package io.github.venkat1701.citation.tavily;

import java.util.List;

import dev.langchain4j.web.search.WebSearchEngine;
import dev.langchain4j.web.search.WebSearchResults;
import dev.langchain4j.web.search.tavily.TavilyWebSearchEngine;
import io.github.venkat1701.citation.CitationFetcher;
import io.github.venkat1701.citation.CitationResult;

public class TavilyCitationFetcher implements CitationFetcher {

    private final WebSearchEngine webSearchEngine;

    public TavilyCitationFetcher(String apiKey) {
        this.webSearchEngine = TavilyWebSearchEngine.builder()
            .apiKey(apiKey)
            .includeRawContent(true)
            .build();
    }

    @Override
    public List<CitationResult> fetch(String query) {
        WebSearchResults results = this.webSearchEngine.search(query);
        return results
            .results()
            .stream()
            .map(result -> new CitationResult(
                result.title(),
                result.snippet(),
                result.content(),
                result.url().toString()
            )).toList();
    }
}
