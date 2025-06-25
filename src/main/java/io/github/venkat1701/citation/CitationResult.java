package io.github.venkat1701.citation;

import java.time.LocalDateTime;

public class CitationResult {

    private String title;
    private String snippet;
    private String content;
    private String url;
    private double relevanceScore;
    private LocalDateTime retrievedAt;
    private String language;
    private String domain;
    private int wordCount;

    public CitationResult() {
        this.retrievedAt = LocalDateTime.now();
        this.relevanceScore = 0.0;
        this.language = "unknown";
    }

    public CitationResult(String title, String snippet, String content, String url) {
        this();
        this.title = title;
        this.snippet = snippet;
        this.content = content;
        this.url = url;
        this.wordCount = content != null ? content.split("\\s+").length : 0;
        this.domain = extractDomain(url);
    }

    public CitationResult(String title, String snippet, String content, String url, double relevanceScore) {
        this(title, snippet, content, url);
        this.relevanceScore = relevanceScore;
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final CitationResult result = new CitationResult();

        public Builder title(String title) {
            result.title = title;
            return this;
        }

        public Builder snippet(String snippet) {
            result.snippet = snippet;
            return this;
        }

        public Builder content(String content) {
            result.content = content;
            if (content != null) {
                result.wordCount = content.split("\\s+").length;
            }
            return this;
        }

        public Builder url(String url) {
            result.url = url;
            result.domain = extractDomain(url);
            return this;
        }

        public Builder relevanceScore(double relevanceScore) {
            result.relevanceScore = Math.max(0.0, Math.min(1.0, relevanceScore));
            return this;
        }

        public Builder language(String language) {
            result.language = language;
            return this;
        }

        public Builder retrievedAt(LocalDateTime retrievedAt) {
            result.retrievedAt = retrievedAt;
            return this;
        }

        public CitationResult build() {
            if (result.title == null || result.title.trim()
                .isEmpty()) {
                throw new IllegalArgumentException("Title cannot be null or empty");
            }
            if (result.url == null || result.url.trim()
                .isEmpty()) {
                throw new IllegalArgumentException("URL cannot be null or empty");
            }
            return result;
        }
    }

    private static String extractDomain(String url) {
        if (url == null || url.isEmpty()) {
            return "unknown";
        }
        try {
            String domain = url.replaceAll("https?://", "")
                .replaceAll("www\\.", "")
                .split("/")[0];
            return domain.toLowerCase();
        } catch (Exception e) {
            return "unknown";
        }
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
        this.wordCount = content != null ? content.split("\\s+").length : 0;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
        this.domain = extractDomain(url);
    }

    public double getRelevanceScore() {
        return relevanceScore;
    }

    public void setRelevanceScore(double relevanceScore) {
        this.relevanceScore = Math.max(0.0, Math.min(1.0, relevanceScore));
    }

    public LocalDateTime getRetrievedAt() {
        return retrievedAt;
    }

    public void setRetrievedAt(LocalDateTime retrievedAt) {
        this.retrievedAt = retrievedAt;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public String getDomain() {
        return domain;
    }

    public void setDomain(String domain) {
        this.domain = domain;
    }

    public int getWordCount() {
        return wordCount;
    }

    public boolean isValid() {
        return title != null && !title.trim()
            .isEmpty() && url != null && !url.trim()
            .isEmpty();
    }

    public boolean hasContent() {
        return content != null && !content.trim()
            .isEmpty();
    }

    public boolean isHighRelevance() {
        return relevanceScore >= 0.7;
    }
}