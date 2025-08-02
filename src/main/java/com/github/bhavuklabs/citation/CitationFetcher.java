package com.github.bhavuklabs.citation;

import java.util.List;

import com.github.bhavuklabs.exceptions.citation.CitationException;

public interface CitationFetcher {
    List<CitationResult> fetch(String query) throws CitationException;
}
