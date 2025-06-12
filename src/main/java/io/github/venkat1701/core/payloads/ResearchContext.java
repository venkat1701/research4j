package io.github.venkat1701.core.payloads;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class ResearchContext {
    private final ResearchPromptConfig config;
    private List<CitationSource> citations = new ArrayList<>();
    private List<String> retrievedChunks = new ArrayList<>();
    private Map<String, String> summaries = new LinkedHashMap<>();
    private String finalPrompt;

    public ResearchContext(ResearchPromptConfig config) {
        this.config = config;
    }

    public ResearchPromptConfig getConfig() {
        return config;
    }
    public List<CitationSource> getCitations() { return citations; }
    public List<String> getRetrievedChunks() { return retrievedChunks; }
    public Map<String, String> getSummaries() { return summaries; }
    public String getFinalPrompt() { return finalPrompt; }
    public void setFinalPrompt(String finalPrompt) {
        this.finalPrompt = finalPrompt;
    }

    public void setCitations(List<CitationSource> citations) {
        this.citations = citations;
    }

    public void setRetrievedChunks(List<String> retrievedChunks) {
        this.retrievedChunks = retrievedChunks;
    }

    public void setSummaries(Map<String, String> summaries) {
        this.summaries = summaries;
    }
}
