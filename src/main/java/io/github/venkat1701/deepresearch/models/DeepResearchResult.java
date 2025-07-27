package io.github.venkat1701.deepresearch.models;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.venkat1701.citation.CitationResult;

public class DeepResearchResult {

    private final String sessionId;
    private final String originalQuery;
    private final String finalReport;
    private final List<CitationResult> allCitations;
    private final List<ResearchQuestion> researchQuestions;
    private final Map<String, Object> knowledgeMap;
    private final Duration processingTime;
    private final String strategyUsed;
    private final Instant completedAt;
    private final ResearchMetrics metrics;

    public DeepResearchResult(
        String sessionId,
        String originalQuery,
        String finalReport,
        List<CitationResult> allCitations,
        List<ResearchQuestion> researchQuestions,
        Map<String, Object> knowledgeMap,
        Duration processingTime,
        String strategyUsed) {

        this.sessionId = sessionId;
        this.originalQuery = originalQuery;
        this.finalReport = finalReport;
        this.allCitations = allCitations;
        this.researchQuestions = researchQuestions;
        this.knowledgeMap = knowledgeMap;
        this.processingTime = processingTime;
        this.strategyUsed = strategyUsed;
        this.completedAt = Instant.now();
        this.metrics = calculateMetrics();
    }

    private ResearchMetrics calculateMetrics() {
        int totalSources = allCitations.size();
        int totalQuestions = researchQuestions.size();
        int answeredQuestions = (int) researchQuestions.stream().mapToLong(q -> q.isResearched() ? 1 : 0).sum();

        double avgRelevanceScore = allCitations.stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);

        Set<String> uniqueDomains = Set.copyOf(allCitations.stream()
            .map(CitationResult::getDomain)
            .toList());

        return new ResearchMetrics(
            totalSources,
            totalQuestions,
            answeredQuestions,
            avgRelevanceScore,
            uniqueDomains.size(),
            processingTime
        );
    }

    
    public String getSessionId() { return sessionId; }
    public String getOriginalQuery() { return originalQuery; }
    public String getFinalReport() { return finalReport; }
    public List<CitationResult> getAllCitations() { return allCitations; }
    public List<ResearchQuestion> getResearchQuestions() { return researchQuestions; }
    public Map<String, Object> getKnowledgeMap() { return knowledgeMap; }
    public Duration getProcessingTime() { return processingTime; }
    public String getStrategyUsed() { return strategyUsed; }
    public Instant getCompletedAt() { return completedAt; }
    public ResearchMetrics getMetrics() { return metrics; }

    public static class ResearchMetrics {
        private final int totalSources;
        private final int totalQuestions;
        private final int answeredQuestions;
        private final double avgRelevanceScore;
        private final int uniqueDomains;
        private final Duration processingTime;

        public ResearchMetrics(int totalSources, int totalQuestions, int answeredQuestions,
            double avgRelevanceScore, int uniqueDomains, Duration processingTime) {
            this.totalSources = totalSources;
            this.totalQuestions = totalQuestions;
            this.answeredQuestions = answeredQuestions;
            this.avgRelevanceScore = avgRelevanceScore;
            this.uniqueDomains = uniqueDomains;
            this.processingTime = processingTime;
        }

        public int getTotalSources() { return totalSources; }
        public int getTotalQuestions() { return totalQuestions; }
        public int getAnsweredQuestions() { return answeredQuestions; }
        public double getAvgRelevanceScore() { return avgRelevanceScore; }
        public int getUniqueDomains() { return uniqueDomains; }
        public Duration getProcessingTime() { return processingTime; }

        public double getCompletionRate() {
            return totalQuestions > 0 ? (double) answeredQuestions / totalQuestions : 0.0;
        }

        public double getQualityScore() {
            
            double relevanceWeight = avgRelevanceScore * 0.4;
            double completionWeight = getCompletionRate() * 0.3;
            double diversityWeight = Math.min(1.0, uniqueDomains / 10.0) * 0.2;
            double efficiencyWeight = Math.max(0.0, 1.0 - (processingTime.toMinutes() / 30.0)) * 0.1;

            return relevanceWeight + completionWeight + diversityWeight + efficiencyWeight;
        }
    }
}