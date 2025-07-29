package io.github.venkat1701.deepresearch.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ResearchGapAnalysis {
    private final List<String> gaps;
    private final Map<String, Double> coverageScores;
    private final List<String> recommendations;
    private final double overallCoverageScore;

    public ResearchGapAnalysis(List<String> gaps, Map<String, Double> coverageScores) {
        this.gaps = new ArrayList<>(gaps);
        this.coverageScores = new HashMap<>(coverageScores);
        this.recommendations = generateRecommendations();
        this.overallCoverageScore = calculateOverallCoverage();
    }

    private List<String> generateRecommendations() {
        List<String> recs = new ArrayList<>();
        for (String gap : gaps) {
            if (gap.contains("code examples")) {
                recs.add("Search for implementation tutorials and code repositories");
            } else if (gap.contains("recent sources")) {
                recs.add("Include year qualifiers (2024, 2025) in search terms");
            } else if (gap.contains("diversity")) {
                recs.add("Search across different source types (academic, industry, documentation)");
            } else {
                recs.add("Search for more specific information about: " + gap);
            }
        }
        return recs;
    }

    private double calculateOverallCoverage() {
        if (coverageScores.isEmpty()) {
            return 0.0;
        }
        return coverageScores.values().stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    public List<String> getGaps() { return new ArrayList<>(gaps); }
    public Map<String, Double> getCoverageScores() { return new HashMap<>(coverageScores); }
    public List<String> getRecommendations() { return new ArrayList<>(recommendations); }
    public double getOverallCoverageScore() { return overallCoverageScore; }
    public boolean hasGaps() { return !gaps.isEmpty(); }
}