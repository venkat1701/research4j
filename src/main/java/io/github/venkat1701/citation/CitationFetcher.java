package io.github.venkat1701.citation;

import java.util.List;

import io.github.venkat1701.exceptions.citation.CitationException;

public interface CitationFetcher {
    List<CitationResult> fetch(String query) throws CitationException;
}
