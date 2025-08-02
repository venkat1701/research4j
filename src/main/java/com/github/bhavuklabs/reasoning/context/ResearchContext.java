package com.github.bhavuklabs.reasoning.context;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.github.bhavuklabs.citation.CitationResult;
import com.github.bhavuklabs.core.enums.ReasoningMethod;
import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;

public class ResearchContext {
    private final ResearchPromptConfig config;
    private List<CitationResult> citations = new ArrayList<>();
    private List<String> retrievedChunks = new ArrayList<>();
    private Map<String, String> summaries = new LinkedHashMap<>();
    private String finalPrompt;

    private ReasoningMethod reasoningMethod;
    private Map<String, Object> reasoningMetadata = new ConcurrentHashMap<>();
    private long startTime;
    private long endTime;

    public ResearchContext(ResearchPromptConfig config) {
        this.config = config;
    }

    public ResearchPromptConfig getConfig() {
        return config;
    }
    public List<CitationResult> getCitations() { return citations; }
    public List<String> getRetrievedChunks() { return retrievedChunks; }
    public Map<String, String> getSummaries() { return summaries; }
    public String getFinalPrompt() { return finalPrompt; }
    public void setFinalPrompt(String finalPrompt) {
        this.finalPrompt = finalPrompt;
    }

    public void setCitations(List<CitationResult> citations) {
        this.citations = citations;
    }

    public void setRetrievedChunks(List<String> retrievedChunks) {
        this.retrievedChunks = retrievedChunks;
    }

    public void setSummaries(Map<String, String> summaries) {
        this.summaries = summaries;
    }

    public void setReasoningMethod(ReasoningMethod reasoningMethod) {
        this.reasoningMethod = reasoningMethod;
    }

    public void setReasoningMetadata(Map<String, Object> reasoningMetadata) {
        this.reasoningMetadata = reasoningMetadata;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public ReasoningMethod getReasoningMethod() {
        return reasoningMethod;
    }

    public Map<String, Object> getReasoningMetadata() {
        return reasoningMetadata;
    }

    public long getStartTime() {
        return startTime;
    }

    public long getEndTime() {
        return endTime;
    }
}
