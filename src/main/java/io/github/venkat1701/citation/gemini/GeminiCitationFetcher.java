package io.github.venkat1701.citation.gemini;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import io.github.venkat1701.citation.CitationFetcher;
import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.config.Research4jConfig;
import io.github.venkat1701.exceptions.citation.CitationException;

public class GeminiCitationFetcher implements CitationFetcher, AutoCloseable {

    private static final Logger logger = Logger.getLogger(GeminiCitationFetcher.class.getName());
    private static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(5);
    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(2);
    private static final int MAX_CONTENT_LENGTH = 10000;

    private final WebSearchEngine webSearchEngine;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private final Duration httpTimeout;
    private final int maxResults;
    private volatile boolean closed = false;

    public GeminiCitationFetcher(String apiKey, String cseId) throws CitationException {
        this(apiKey, cseId, DEFAULT_HTTP_TIMEOUT, 10);
    }

    public GeminiCitationFetcher(String apiKey, String cseId, Duration httpTimeout, int maxResults) throws CitationException {
        validateInputs(apiKey, cseId);

        this.httpTimeout = httpTimeout != null ? httpTimeout : DEFAULT_HTTP_TIMEOUT;
        this.maxResults = Math.max(1, Math.min(maxResults, 20));

        try {
            this.webSearchEngine = GoogleCustomWebSearchEngine.builder()
                .apiKey(apiKey)
                .csi(cseId)
                .includeImages(false)
                .logRequests(false)
                .logResponses(false)
                .build();

            this.httpClient = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            this.executor = Executors.newVirtualThreadPerTaskExecutor();

            logger.info("GeminiCitationFetcher initialized with timeout: " + this.httpTimeout);

        } catch (Exception e) {
            throw new CitationException("Failed to initialize Gemini citation fetcher: " + e.getMessage(), e, "initialization", "GEMINI");
        }
    }

    public GeminiCitationFetcher(Research4jConfig config) throws CitationException {
        this(config.getGoogleSearchApiKey(), config.getGoogleCseId(), config.getRequestTimeout(), config.getMaxCitations());
    }

    private void validateInputs(String apiKey, String cseId) {
        if (apiKey == null || apiKey.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Google Search API key cannot be null or empty");
        }
        if (cseId == null || cseId.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Google CSE ID cannot be null or empty");
        }
    }

    @Override
    public List<CitationResult> fetch(String query) throws CitationException {
        if (closed) {
            throw new IllegalStateException("Citation fetcher has been closed");
        }

        if (query == null || query.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        try {
            logger.info("Fetching citations for query: " + query);

            WebSearchResults results = webSearchEngine.search(query);

            if (results == null || results.results()
                .isEmpty()) {
                logger.warning("No search results found for query: " + query);
                return List.of();
            }

            List<CompletableFuture<CitationResult>> futures = results.results()
                .stream()
                .limit(maxResults)
                .map(result -> fetchCitationAsync(result, query))
                .toList();

            return futures.stream()
                .map(this::safeGet)
                .filter(citation -> citation != null && citation.isValid())
                .toList();

        } catch (Exception e) {
            logger.severe("Failed to fetch citations: " + e.getMessage());
            throw new CitationException("Citation fetch failed: " + e.getMessage(), e, query, "GEMINI");
        }
    }

    private CompletableFuture<CitationResult> fetchCitationAsync(dev.langchain4j.web.search.WebSearchOrganicResult result, String originalQuery) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = result.url()
                    .toString();
                String content = fetchContentWithTimeout(url);
                double relevanceScore = calculateRelevanceScore(result, originalQuery);

                return CitationResult.builder()
                    .title(result.title())
                    .snippet(result.snippet())
                    .content(content)
                    .url(url)
                    .relevanceScore(relevanceScore)
                    .retrievedAt(LocalDateTime.now())
                    .language(detectLanguage(content))
                    .build();

            } catch (Exception e) {
                logger.warning("Failed to fetch content from URL: " + result.url() + " - " + e.getMessage());

                return CitationResult.builder()
                    .title(result.title())
                    .snippet(result.snippet())
                    .content(result.snippet())
                    .url(result.url()
                        .toString())
                    .relevanceScore(0.5)
                    .retrievedAt(LocalDateTime.now())
                    .language("unknown")
                    .build();
            }
        }, executor);
    }

    private String fetchContentWithTimeout(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(httpTimeout)
                .header("User-Agent", "Research4j/1.0 (+https://github.com/venkat1701/research4j)")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                logger.warning("HTTP " + response.statusCode() + " for URL: " + url);
                return "Content not available (HTTP " + response.statusCode() + ")";
            }

            String htmlContent = response.body();
            if (htmlContent == null || htmlContent.trim()
                .isEmpty()) {
                return "No content available";
            }

            Document doc = Jsoup.parse(htmlContent);

            doc.select("script, style, nav, footer, header")
                .remove();

            String textContent = doc.body()
                .text();

            if (textContent.length() > MAX_CONTENT_LENGTH) {
                textContent = textContent.substring(0, MAX_CONTENT_LENGTH) + "...";
            }

            return textContent.trim();

        } catch (java.net.SocketTimeoutException e) {
            logger.warning("Timeout fetching content from: " + url);
            return "Content not available (timeout)";
        } catch (java.net.ConnectException e) {
            logger.warning("Connection failed for: " + url);
            return "Content not available (connection failed)";
        } catch (Exception e) {
            logger.warning("Error fetching content from " + url + ": " + e.getMessage());
            return "Content not available (error: " + e.getClass()
                .getSimpleName() + ")";
        }
    }

    private double calculateRelevanceScore(dev.langchain4j.web.search.WebSearchOrganicResult result, String query) {
        try {
            double score = 0.0;
            String queryLower = query.toLowerCase();
            String titleLower = result.title()
                .toLowerCase();
            String snippetLower = result.snippet()
                .toLowerCase();

            if (titleLower.contains(queryLower)) {
                score += 0.4;
            } else {
                String[] queryTerms = queryLower.split("\\s+");
                int titleMatches = 0;
                for (String term : queryTerms) {
                    if (titleLower.contains(term)) {
                        titleMatches++;
                    }
                }
                score += (0.4 * titleMatches) / queryTerms.length;
            }

            if (snippetLower.contains(queryLower)) {
                score += 0.35;
            } else {
                String[] queryTerms = queryLower.split("\\s+");
                int snippetMatches = 0;
                for (String term : queryTerms) {
                    if (snippetLower.contains(term)) {
                        snippetMatches++;
                    }
                }
                score += (0.35 * snippetMatches) / queryTerms.length;
            }

            String url = result.url()
                .toString()
                .toLowerCase();
            if (url.contains("wikipedia") || url.contains("edu") || url.contains("gov")) {
                score += 0.15;
            } else if (url.contains("stackoverflow") || url.contains("github")) {
                score += 0.10;
            } else if (url.contains("medium") || url.contains("blog")) {
                score += 0.05;
            }
            if (url.contains("https://")) {
                score += 0.05;
            }
            if (!url.contains("ads") && !url.contains("spam")) {
                score += 0.05;
            }

            return Math.min(1.0, Math.max(0.0, score));

        } catch (Exception e) {
            logger.warning("Error calculating relevance score: " + e.getMessage());
            return 0.5;
        }
    }

    private String detectLanguage(String content) {
        if (content == null || content.trim()
            .isEmpty()) {
            return "unknown";
        }

        String contentLower = content.toLowerCase();

        if (contentLower.contains(" the ") || contentLower.contains(" and ") || contentLower.contains(" is ") || contentLower.contains(" of ")) {
            return "en";
        }

        return "unknown";
    }

    private CitationResult safeGet(CompletableFuture<CitationResult> future) {
        try {
            return future.get(httpTimeout.toSeconds() + 5, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warning("Failed to get citation result: " + e.getMessage());
            return null;
        }
    }

    public boolean isHealthy() {
        if (closed) {
            return false;
        }

        try {
            List<CitationResult> results = fetch("test query");
            return true;
        } catch (Exception e) {
            logger.warning("Health check failed: " + e.getMessage());
            return false;
        }
    }

    public Duration getHttpTimeout() {
        return httpTimeout;
    }

    public int getMaxResults() {
        return maxResults;
    }

    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (closed) {
            return;
        }

        closed = true;

        try {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
                if (!executor.awaitTermination(5, TimeUnit.SECONDS)) {
                    logger.warning("Executor did not terminate cleanly");
                }
            }

            logger.info("GeminiCitationFetcher closed successfully");

        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread()
                .interrupt();
            logger.warning("Interrupted during shutdown");
        } catch (Exception e) {
            logger.warning("Error during shutdown: " + e.getMessage());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String apiKey;
        private String cseId;
        private Duration httpTimeout = DEFAULT_HTTP_TIMEOUT;
        private int maxResults = 10;

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder cseId(String cseId) {
            this.cseId = cseId;
            return this;
        }

        public Builder httpTimeout(Duration timeout) {
            this.httpTimeout = timeout;
            return this;
        }

        public Builder maxResults(int maxResults) {
            this.maxResults = maxResults;
            return this;
        }

        public GeminiCitationFetcher build() throws CitationException {
            return new GeminiCitationFetcher(apiKey, cseId, httpTimeout, maxResults);
        }
    }
}