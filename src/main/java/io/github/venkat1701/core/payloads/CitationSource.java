package io.github.venkat1701.core.payloads;

public class CitationSource {
    private final String title;
    private final String url;
    private final String rawContent;

    public CitationSource(String title, String url, String rawContent) {
        this.title = title;
        this.url = url;
        this.rawContent = rawContent;
    }

    public String getTitle() { return title; }
    public String getUrl() { return url; }
    public String getRawContent() { return rawContent; }
}
