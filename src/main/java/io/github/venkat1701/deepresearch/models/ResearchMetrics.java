package io.github.venkat1701.deepresearch.models;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.venkat1701.citation.CitationResult;

public class ResearchMetrics {
    private final int totalSources;
    private final int totalCategories;
    private final double averageRelevanceScore;
    private final Map<String, Integer> sourceDistribution;
    private final long executionTime;

    public ResearchMetrics(List<CitationResult> citations,
        Map<String, List<CitationResult>> categorized) {
        this.totalSources = citations.size();
        this.totalCategories = categorized.size();
        this.averageRelevanceScore = calculateAverageRelevance(citations);
        this.sourceDistribution = calculateSourceDistribution(categorized);
        this.executionTime = System.currentTimeMillis(); // Placeholder
    }

    private double calculateAverageRelevance(List<CitationResult> citations) {
        return citations.stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);
    }

    private Map<String, Integer> calculateSourceDistribution(Map<String, List<CitationResult>> categorized) {
        Map<String, Integer> distribution = new HashMap<>();
        for (Map.Entry<String, List<CitationResult>> entry : categorized.entrySet()) {
            distribution.put(entry.getKey(), entry.getValue().size());
        }
        return distribution;
    }

    public int getTotalSources() { return totalSources; }
    public int getTotalCategories() { return totalCategories; }
    public double getAverageRelevanceScore() { return averageRelevanceScore; }
    public Map<String, Integer> getSourceDistribution() { return new HashMap<>(sourceDistribution); }
    public long getExecutionTime() { return executionTime; }
}
