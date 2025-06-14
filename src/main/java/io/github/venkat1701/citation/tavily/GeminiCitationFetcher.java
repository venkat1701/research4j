package io.github.venkat1701.citation.tavily;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.google.customsearch.GoogleCustomWebSearchEngine;
import io.github.venkat1701.citation.CitationFetcher;
import io.github.venkat1701.citation.CitationResult;

public class GeminiCitationFetcher implements CitationFetcher {

    private final WebSearchEngine webSearchEngine;

    public GeminiCitationFetcher(String apiKey, String csi) {
        this.webSearchEngine = GoogleCustomWebSearchEngine
            .builder()
            .apiKey(apiKey)
            .csi(csi)
            .includeImages(true)
            .build();
    }


    @Override
    public List<CitationResult> fetch(String query) {
        WebSearchResults results = this.webSearchEngine.search(query);
        ExecutorService executor = Executors.newVirtualThreadPerTaskExecutor();
        try {
            return results.results()
                .stream()
                .map(result -> executor.submit(
                    () -> {
                        String url = result.url().toString();
                        String content = fetchHtmlComponent(url);
                        return new CitationResult(
                            result.title(),
                            result.snippet(),
                            content,
                            url
                        );
                    }
                ))
                .map(future -> {
                    try {
                        return future.get();
                    } catch (Exception e) {
                        return new CitationResult("Error", "", "There was an error processing the given URL", "");
                    }
                })
                .toList();
        } finally {
            executor.shutdown();
        }
    }

    private String fetchHtmlComponent(String url) {
        try {
            HttpClient httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(1000))
                .build();
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(url))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            Document doc = Jsoup.parse(response.body());
            return doc.text();
        } catch (Exception e) {
            return "Error";
        }
    }


}
