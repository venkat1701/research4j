package io.github.venkat1701.examples.citation.gemini;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.tavily.GeminiCitationFetcher;

public class GeminiCitationFetcherExample {
    public static void main(String[] args) {
        GeminiCitationFetcher fetcher = new GeminiCitationFetcher("API_KEY", "CSI_KEY");
        var results = fetcher.fetch("Top resources to study system design from");
        for (CitationResult result : results) {
            System.out.println(result);
        }
    }
}
