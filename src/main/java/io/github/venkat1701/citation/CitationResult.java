package io.github.venkat1701.citation;

public class CitationResult {
    private String title;
    private String snippet;
    private String content;
    private String url;

    public CitationResult(String title, String snippet, String content, String url) {
        this.title = title;
        this.snippet = snippet;
        this.content = content;
        this.url = url;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public String getSnippet() {
        return snippet;
    }

    public void setSnippet(String snippet) {
        this.snippet = snippet;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    @Override
    public String toString() {
        return "CitationResult{" + "title='" + title + '\'' + ", snippet='" + snippet + '\'' + ", content='" + content + '\'' + ", url='" + url + '\'' + '}';
    }
}
