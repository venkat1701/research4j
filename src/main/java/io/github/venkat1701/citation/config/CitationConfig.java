package io.github.venkat1701.citation.config;

import io.github.venkat1701.citation.enums.CitationSource;

public class CitationConfig {
    private final CitationSource citationSource;
    private final String apiKey;

    public CitationConfig(CitationSource source, String apiKey) {
        this.citationSource = source;
        this.apiKey = apiKey;
    }

    public CitationSource getCitationSource() {
        return citationSource;
    }

    public String getApiKey() {
        return apiKey;
    }
}
