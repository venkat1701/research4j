package io.github.venkat1701.deepresearch.models;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.venkat1701.citation.CitationResult;

public class ResearchQualityAnalyzer {
    private static final Logger logger = Logger.getLogger(ResearchQualityAnalyzer.class.getName());

    public List<CitationResult> filterAndRankResults(List<CitationResult> results,
        ResearchQuestion question,
        DeepResearchConfig config) {
        return results.stream()
            .filter(this::isHighQualitySource)
            .filter(result -> isRelevantToQuestion(result, question))
            .sorted((r1, r2) -> Double.compare(r2.getRelevanceScore(), r1.getRelevanceScore()))
            .limit(determineResultLimit(config))
            .collect(Collectors.toList());
    }

    public boolean isResearchSufficient(List<CitationResult> results,
        ResearchQuestion question,
        DeepResearchConfig config) {
        int minSources = config.getResearchDepth().ordinal() * 5 + 10;
        double avgQuality = results.stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);

        return results.size() >= minSources && avgQuality >= 0.6;
    }

    private boolean isHighQualitySource(CitationResult result) {
        if (result.getRelevanceScore() < 0.4) return false;
        if (result.getContent() == null || result.getContent().length() < 200) return false;

        String domain = result.getDomain().toLowerCase();
        return !domain.contains("forum") &&
            !domain.contains("reddit") &&
            !domain.contains("quora");
    }

    private boolean isRelevantToQuestion(CitationResult result, ResearchQuestion question) {
        String questionLower = question.getQuestion().toLowerCase();
        String contentLower = (result.getTitle() + " " + result.getContent()).toLowerCase();

        String[] questionWords = questionLower.split("\\W+");
        long matches = Arrays.stream(questionWords)
            .filter(word -> word.length() > 3)
            .mapToLong(word -> contentLower.contains(word) ? 1 : 0)
            .sum();

        return matches >= 2;
    }

    private int determineResultLimit(DeepResearchConfig config) {
        return switch (config.getResearchDepth()) {
            case STANDARD -> 15;
            case COMPREHENSIVE -> 25;
            case BASIC -> 5;
            case EXPERT -> 45;
        };
    }
}