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
            recs.add("Search for more specific information about: " + gap);
        }
        return recs;
    }

    private double calculateOverallCoverage() {
        return coverageScores.values()
            .stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);
    }

    public List<String> getGaps() {
        return new ArrayList<>(gaps);
    }

    public Map<String, Double> getCoverageScores() {
        return new HashMap<>(coverageScores);
    }

    public List<String> getRecommendations() {
        return new ArrayList<>(recommendations);
    }

    public double getOverallCoverageScore() {
        return overallCoverageScore;
    }

    public boolean hasGaps() {
        return !gaps.isEmpty();
    }
}