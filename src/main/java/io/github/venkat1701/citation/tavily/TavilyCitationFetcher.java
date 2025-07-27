package io.github.venkat1701.citation.tavily;

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
import java.util.stream.Collectors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.parser.Parser;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import io.github.venkat1701.citation.CitationFetcher;
import io.github.venkat1701.citation.CitationResult;

public class TavilyCitationFetcher implements CitationFetcher, AutoCloseable {

    private static final Logger logger = Logger.getLogger(TavilyCitationFetcher.class.getName());
    private static final Duration DEFAULT_HTTP_TIMEOUT = Duration.ofSeconds(10);
    private static final int MAX_CONTENT_LENGTH = 8000;
    private static final int MIN_CONTENT_LENGTH = 100;

    private final WebSearchEngine webSearchEngine;
    private final HttpClient httpClient;
    private final ExecutorService executor;
    private volatile boolean closed = false;

    public TavilyCitationFetcher(String apiKey) {
        if (apiKey == null || apiKey.trim().isEmpty()) {
            throw new IllegalArgumentException("Tavily API key cannot be null or empty");
        }

        try {
            this.webSearchEngine = TavilyWebSearchEngine.builder()
                .apiKey(apiKey)
                .includeRawContent(true)
                .build();

            this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

            this.executor = Executors.newVirtualThreadPerTaskExecutor();

            logger.info("TavilyCitationFetcher initialized successfully");

        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize Tavily citation fetcher: " + e.getMessage(), e);
        }
    }

    @Override
    public List<CitationResult> fetch(String query) {
        if (closed) {
            throw new IllegalStateException("Citation fetcher has been closed");
        }

        if (query == null || query.trim().isEmpty()) {
            throw new IllegalArgumentException("Query cannot be null or empty");
        }

        try {
            logger.info("Fetching citations from Tavily for query: " + truncateString(query, 100));

            WebSearchResults results = webSearchEngine.search(query);

            if (results == null || results.results().isEmpty()) {
                logger.warning("No search results found for query: " + query);
                return List.of();
            }

            List<CompletableFuture<CitationResult>> futures = results.results()
                .stream()
                .limit(15) // Limit to prevent too many concurrent requests
                .map(result -> fetchCitationAsync(result, query))
                .collect(Collectors.toList());

            List<CitationResult> citations = futures.stream()
                .map(future -> {
                    try {
                        return future.get(DEFAULT_HTTP_TIMEOUT.toSeconds() + 5, TimeUnit.SECONDS);
                    } catch (Exception e) {
                        logger.warning("Failed to fetch citation: " + e.getMessage());
                        return null;
                    }
                })
                .filter(citation -> citation != null && citation.isValid())
                .collect(Collectors.toList());

            logger.info("Successfully fetched " + citations.size() + " citations from Tavily");
            return citations;

        } catch (Exception e) {
            logger.severe("Error fetching citations from Tavily: " + e.getMessage());
            return List.of(); // Return empty list instead of throwing exception
        }
    }

    private CompletableFuture<CitationResult> fetchCitationAsync(
        dev.langchain4j.web.search.WebSearchOrganicResult result,
        String originalQuery) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                String url = result.url().toString();
                String rawContent = result.content();

                // Enhanced content processing
                String processedContent = enhanceContent(rawContent, url);

                // Calculate relevance score
                double relevanceScore = calculateRelevanceScore(result, originalQuery, processedContent);

                return CitationResult.builder()
                    .title(cleanText(result.title()))
                    .snippet(cleanText(result.snippet()))
                    .content(processedContent)
                    .url(url)
                    .relevanceScore(relevanceScore)
                    .retrievedAt(LocalDateTime.now())
                    .language(detectLanguage(processedContent))
                    .build();

            } catch (Exception e) {
                logger.warning("Failed to process citation result from URL: " + result.url() + " - " + e.getMessage());

                // Return basic citation with available data
                try {
                    return CitationResult.builder()
                        .title(cleanText(result.title() != null ? result.title() : "Unknown Title"))
                        .snippet(cleanText(result.snippet() != null ? result.snippet() : "No snippet available"))
                        .content(cleanText(result.content() != null ? result.content() : "No content available"))
                        .url(result.url().toString())
                        .relevanceScore(0.3)
                        .retrievedAt(LocalDateTime.now())
                        .language("unknown")
                        .build();
                } catch (Exception ex) {
                    logger.severe("Failed to create fallback citation: " + ex.getMessage());
                    return null;
                }
            }
        }, executor);
    }

    private String enhanceContent(String rawContent, String url) {
        if (rawContent == null || rawContent.trim().isEmpty()) {
            // Try to fetch content from URL if raw content is empty
            return fetchContentFromUrl(url);
        }

        try {
            // Safe HTML parsing with proper error handling
            String cleanedContent = cleanHtmlContent(rawContent);

            if (cleanedContent.length() < MIN_CONTENT_LENGTH) {
                // Try to fetch more content from URL
                String fetchedContent = fetchContentFromUrl(url);
                if (fetchedContent.length() > cleanedContent.length()) {
                    return fetchedContent;
                }
            }

            return cleanedContent;

        } catch (Exception e) {
            logger.warning("Error enhancing content from " + url + ": " + e.getMessage());
            return cleanText(rawContent);
        }
    }

    private String cleanHtmlContent(String htmlContent) {
        if (htmlContent == null || htmlContent.trim().isEmpty()) {
            return "No content available";
        }

        try {
            // Use XML parser mode to handle malformed HTML better
            Document doc = Jsoup.parse(htmlContent, "", Parser.xmlParser());

            // Remove problematic elements that might cause duplicates
            doc.select("script, style, nav, footer, header, aside, .advertisement, .ads").remove();
            doc.select("[class*=ad], [id*=ad], [class*=social], [id*=social]").remove();

            String textContent = doc.text();

            if (textContent.length() > MAX_CONTENT_LENGTH) {
                textContent = truncateAtSentence(textContent, MAX_CONTENT_LENGTH);
            }

            return cleanText(textContent);

        } catch (Exception e) {
            logger.warning("Error parsing HTML content: " + e.getMessage());
            // Fallback: strip HTML tags manually
            return cleanText(htmlContent.replaceAll("<[^>]+>", " ").replaceAll("\\s+", " "));
        }
    }

    private String fetchContentFromUrl(String url) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(DEFAULT_HTTP_TIMEOUT)
                .header("User-Agent", "Research4j/2.0 Academic Research Bot")
                .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200) {
                return cleanHtmlContent(response.body());
            } else {
                logger.warning("HTTP " + response.statusCode() + " for URL: " + url);
                return "Content not available (HTTP " + response.statusCode() + ")";
            }

        } catch (Exception e) {
            logger.warning("Failed to fetch content from URL " + url + ": " + e.getMessage());
            return "Content not available";
        }
    }

    private String cleanText(String text) {
        if (text == null) {
            return "No content available";
        }

        return text.replaceAll("\\s+", " ")
            .replaceAll("[\\r\\n]+", " ")
            .replaceAll("\\t+", " ")
            .trim();
    }

    private String truncateAtSentence(String text, int maxLength) {
        if (text.length() <= maxLength) {
            return text;
        }

        String truncated = text.substring(0, maxLength);
        int lastSentence = Math.max(
            truncated.lastIndexOf(". "),
            Math.max(truncated.lastIndexOf("! "), truncated.lastIndexOf("? "))
        );

        if (lastSentence > maxLength * 0.7) {
            return truncated.substring(0, lastSentence + 1);
        }

        return truncated + "...";
    }

    private double calculateRelevanceScore(
        dev.langchain4j.web.search.WebSearchOrganicResult result,
        String originalQuery,
        String content) {

        try {
            double score = 0.5; // Base score

            String queryLower = originalQuery.toLowerCase();
            String titleLower = result.title() != null ? result.title().toLowerCase() : "";
            String snippetLower = result.snippet() != null ? result.snippet().toLowerCase() : "";
            String contentLower = content.toLowerCase();

            // Title relevance
            if (titleLower.contains(queryLower)) {
                score += 0.3;
            } else {
                String[] queryWords = queryLower.split("\\s+");
                long titleMatches = java.util.Arrays.stream(queryWords)
                    .mapToLong(word -> titleLower.contains(word) ? 1 : 0)
                    .sum();
                score += (titleMatches / (double) queryWords.length) * 0.2;
            }

            // Content relevance
            if (!contentLower.isEmpty()) {
                String[] queryWords = queryLower.split("\\s+");
                long contentMatches = java.util.Arrays.stream(queryWords)
                    .mapToLong(word -> contentLower.contains(word) ? 1 : 0)
                    .sum();
                score += (contentMatches / (double) queryWords.length) * 0.2;
            }

            // Content quality indicators
            if (content.length() > MIN_CONTENT_LENGTH * 3) {
                score += 0.1;
            }

            return Math.min(1.0, Math.max(0.1, score));

        } catch (Exception e) {
            logger.warning("Error calculating relevance score: " + e.getMessage());
            return 0.3;
        }
    }

    private String detectLanguage(String content) {
        if (content == null || content.trim().isEmpty()) {
            return "unknown";
        }

        String contentLower = content.toLowerCase();
        String[] englishIndicators = {" the ", " and ", " is ", " of ", " to ", " in ", " that ", " for "};

        long englishMatches = java.util.Arrays.stream(englishIndicators)
            .mapToLong(indicator -> contentLower.contains(indicator) ? 1 : 0)
            .sum();

        return englishMatches >= 3 ? "en" : "unknown";
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) return "null";
        return str.length() <= maxLength ? str : str.substring(0, maxLength) + "...";
    }

    @Override
    public void close() {
        if (closed) return;

        closed = true;
        try {
            executor.shutdown();
            if (!executor.awaitTermination(10, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
            logger.info("TavilyCitationFetcher closed successfully");
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}