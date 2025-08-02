package com.github.bhavuklabs.exceptions.citation;

import com.github.bhavuklabs.exceptions.Research4jException;

public class CitationException extends Research4jException {

    private final String query;
    private final String provider;

    public CitationException(String message, String query) {
        super("CITATION_ERROR", message, query);
        this.query = query;
        this.provider = null;
    }

    public CitationException(String message, String query, String provider) {
        super("CITATION_ERROR", message, String.format("query=%s, provider=%s", query, provider));
        this.query = query;
        this.provider = provider;
    }

    public CitationException(String message, Throwable cause, String query, String provider) {
        super("CITATION_ERROR", message, cause, String.format("query=%s, provider=%s", query, provider));
        this.query = query;
        this.provider = provider;
    }

    public String getQuery() {
        return query;
    }

    public String getProvider() {
        return provider;
    }
}