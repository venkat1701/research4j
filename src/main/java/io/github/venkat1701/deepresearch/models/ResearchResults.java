package io.github.venkat1701.deepresearch.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.venkat1701.citation.CitationResult;

public class ResearchResults {
    private final List<CitationResult> allCitations;
    private final Map<String, List<CitationResult>> categorizedResults;
    private final Map<String, String> insights;
    private final ResearchMetrics metrics;

    public ResearchResults(List<CitationResult> allCitations,
        Map<String, List<CitationResult>> categorizedResults) {
        this.allCitations = new ArrayList<>(allCitations);
        this.categorizedResults = new HashMap<>(categorizedResults);
        this.insights = new HashMap<>();
        this.metrics = new ResearchMetrics(allCitations, categorizedResults);
    }

    public ResearchResults(List<CitationResult> allCitations,
        Map<String, List<CitationResult>> categorizedResults,
        Map<String, String> insights) {
        this.allCitations = new ArrayList<>(allCitations);
        this.categorizedResults = new HashMap<>(categorizedResults);
        this.insights = new HashMap<>(insights);
        this.metrics = new ResearchMetrics(allCitations, categorizedResults);
    }

    public List<CitationResult> getAllCitations() { return new ArrayList<>(allCitations); }
    public Map<String, List<CitationResult>> getCategorizedResults() {
        return new HashMap<>(categorizedResults);
    }
    public Map<String, String> getInsights() { return new HashMap<>(insights); }
    public ResearchMetrics getMetrics() { return metrics; }

    public void addInsight(String key, String insight) {
        insights.put(key, insight);
    }

    public List<CitationResult> getCitationsByCategory(String category) {
        return categorizedResults.getOrDefault(category, new ArrayList<>());
    }
}
