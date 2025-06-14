package io.github.venkat1701.examples.citation.tavily;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.tavily.TavilyCitationFetcher;

public class TavilyCitationFetcherExample {

    public static void main(String[] args) {
        TavilyCitationFetcher fetcher = new TavilyCitationFetcher("API_KEY");
        var results = fetcher.fetch("Top resources to study system design from");
        for (CitationResult result : results) {
            System.out.println(result);
        }
    }
}
