package io.github.venkat1701.citation.service;


import static io.github.venkat1701.citation.enums.CitationSource.TAVILY;

import java.util.List;

import io.github.venkat1701.citation.CitationFetcher;
import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.config.CitationConfig;
import io.github.venkat1701.citation.gemini.GeminiCitationFetcher;
import io.github.venkat1701.citation.tavily.TavilyCitationFetcher;
import io.github.venkat1701.exceptions.citation.CitationException;

public class CitationService {
    private final CitationFetcher citationFetcher;

    public CitationService(CitationConfig citationConfig) throws CitationException {
        switch(citationConfig.getCitationSource()) {
            case TAVILY -> this.citationFetcher = new TavilyCitationFetcher(citationConfig.getApiKey());
            case GOOGLE_GEMINI -> this.citationFetcher = new GeminiCitationFetcher(citationConfig.getApiKey(), "");
            default -> throw new IllegalArgumentException("Unsupported CitationSource: " + citationConfig.getCitationSource());
        }
    }

    public CitationService(CitationConfig citationConfig, String CSI) throws CitationException {
        switch(citationConfig.getCitationSource()) {
            case TAVILY -> this.citationFetcher = new TavilyCitationFetcher(citationConfig.getApiKey());
            case GOOGLE_GEMINI -> this.citationFetcher = new GeminiCitationFetcher(citationConfig.getApiKey(), CSI);
            default -> throw new IllegalArgumentException("Unsupported CitationSource: " + citationConfig.getCitationSource());
        }
    }

    public List<CitationResult> search(String query) throws CitationException {
        return this.citationFetcher.fetch(query);
    }
}
