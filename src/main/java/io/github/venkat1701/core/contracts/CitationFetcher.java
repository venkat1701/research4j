package io.github.venkat1701.core.contracts;

import java.util.List;

import io.github.venkat1701.core.payloads.CitationSource;

public interface CitationFetcher {
    List<CitationSource> fetchCitation(String query);
}
