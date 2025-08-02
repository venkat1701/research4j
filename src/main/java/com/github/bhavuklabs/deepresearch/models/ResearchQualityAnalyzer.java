package com.github.bhavuklabs.deepresearch.models;

import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.bhavuklabs.citation.CitationResult;

public class ResearchQualityAnalyzer {
    private static final Logger logger = Logger.getLogger(ResearchQualityAnalyzer.class.getName());

    public List<CitationResult> filterAndRankResults(List<CitationResult> results,
        ResearchQuestion question,
        DeepResearchConfig config) {
        if (results == null || results.isEmpty()) {
            return new java.util.ArrayList<>();
        }

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
        if (results == null || results.isEmpty()) {
            return false;
        }

        int minSources = config.getResearchDepth().ordinal() * 5 + 10;
        double avgQuality = results.stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);

        boolean hasMinSources = results.size() >= minSources;
        boolean hasGoodQuality = avgQuality >= 0.6;
        boolean hasContentDiversity = hasContentDiversity(results);

        logger.info("Research sufficiency check - Sources: " + results.size() + "/" + minSources +
            ", Quality: " + String.format("%.2f", avgQuality) + ", Diversity: " + hasContentDiversity);

        return hasMinSources && hasGoodQuality && hasContentDiversity;
    }

    private boolean hasContentDiversity(List<CitationResult> results) {
        if (results.size() < 3) {
            return false;
        }

        
        long uniqueDomains = results.stream()
            .map(CitationResult::getDomain)
            .distinct()
            .count();

        return uniqueDomains >= Math.min(3, results.size() / 2);
    }

    private boolean isHighQualitySource(CitationResult result) {
        if (result == null || !result.isValid()) {
            return false;
        }

        
        if (result.getRelevanceScore() < 0.3) {
            return false;
        }

        
        if (result.getContent() == null || result.getContent().length() < 150) {
            return false;
        }

        
        String domain = result.getDomain();
        if (domain != null) {
            String domainLower = domain.toLowerCase();
            String[] lowQualityDomains = {"ads", "spam", "pinterest", "instagram", "tiktok"};
            for (String lowQuality : lowQualityDomains) {
                if (domainLower.contains(lowQuality)) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean isRelevantToQuestion(CitationResult result, ResearchQuestion question) {
        if (result == null || question == null) {
            return false;
        }

        String questionLower = question.getQuestion().toLowerCase();
        String titleLower = result.getTitle() != null ? result.getTitle().toLowerCase() : "";
        String contentLower = result.getContent() != null ? result.getContent().toLowerCase() : "";
        String combined = titleLower + " " + contentLower;

        String[] questionWords = questionLower.split("\\W+");
        long matches = Arrays.stream(questionWords)
            .filter(word -> word.length() > 3)
            .mapToLong(word -> combined.contains(word) ? 1 : 0)
            .sum();

        return matches >= 2 || questionWords.length <= 2;
    }

    private int determineResultLimit(DeepResearchConfig config) {
        return switch (config.getResearchDepth()) {
            case BASIC -> 8;
            case STANDARD -> 15;
            case COMPREHENSIVE -> 25;
            case EXPERT -> 40;
        };
    }
}
