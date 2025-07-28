package io.github.venkat1701.deepresearch.models;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import io.github.venkat1701.citation.CitationResult;

/**
 * Deep Research Result - Comprehensive container for all research outputs
 * Represents the final deliverable from the Deep Research process
 * Contains narrative, sources, metadata, and quality metrics
 * Unified version supporting multiple implementation patterns
 */
public class DeepResearchResult {

    private final String sessionId;
    private final String originalQuery;

    private final String narrative;
    private final String finalReport;
    private final String executiveSummary;
    private final String methodology;

    private final ResearchResults results;
    private final List<CitationResult> allCitations;
    private final List<ResearchQuestion> researchQuestions;
    private final Map<String, Object> knowledgeMap;
    private final Map<String, Object> metadata;
    private final List<ResearchInsight> keyInsights;

    private final QualityMetrics qualityMetrics;
    private final PerformanceMetrics performanceMetrics;
    private final ResearchMetrics metrics;
    private final DeepResearchConfig config;

    private final Duration processingTime;
    private final String strategyUsed;
    private final Instant createdAt;
    private final Instant completedAt;
    private final Map<String, Instant> phaseTimestamps;

    public DeepResearchResult(String sessionId, String originalQuery, String narrative, String executiveSummary, String methodology, ResearchResults results,
        Object qualityMetrics, DeepResearchConfig config) {

        this.sessionId = sessionId;
        this.originalQuery = originalQuery;
        this.narrative = narrative;
        this.finalReport = narrative;
        this.executiveSummary = executiveSummary;
        this.methodology = methodology;
        this.results = results;
        this.config = config;
        this.createdAt = Instant.now();
        this.completedAt = createdAt;

        this.allCitations = results != null ? new ArrayList<>(results.getAllCitations()) : new ArrayList<>();
        this.researchQuestions = new ArrayList<>();
        this.knowledgeMap = new HashMap<>();
        this.processingTime = Duration.ZERO;
        this.strategyUsed = "Deep Research Engine";

        this.qualityMetrics = createQualityMetrics(narrative, results);
        this.performanceMetrics = new PerformanceMetrics(results);
        this.metrics = calculateResearchMetrics();

        this.keyInsights = extractKeyInsights(results);

        this.metadata = new HashMap<>();
        initializeMetadata();

        this.phaseTimestamps = new HashMap<>();
        this.phaseTimestamps.put("creation", createdAt);
        this.phaseTimestamps.put("completion", completedAt);
    }

    public DeepResearchResult(String sessionId, String originalQuery, String finalReport, List<CitationResult> allCitations,
        List<ResearchQuestion> researchQuestions, Map<String, Object> knowledgeMap, Duration processingTime, String strategyUsed) {

        this.sessionId = sessionId;
        this.originalQuery = originalQuery;
        this.finalReport = finalReport;
        this.narrative = finalReport;
        this.executiveSummary = generateBasicExecutiveSummary(finalReport);
        this.methodology = "Research methodology not specified";
        this.allCitations = new ArrayList<>(allCitations);
        this.researchQuestions = new ArrayList<>(researchQuestions);
        this.knowledgeMap = new HashMap<>(knowledgeMap);
        this.processingTime = processingTime;
        this.strategyUsed = strategyUsed;
        this.completedAt = Instant.now();
        this.createdAt = completedAt;

        this.results = createSyntheticResearchResults(allCitations, knowledgeMap);
        this.config = DeepResearchConfig.createDefault();

        this.metrics = calculateResearchMetrics();
        this.qualityMetrics = createQualityMetrics(finalReport, results);
        this.performanceMetrics = new PerformanceMetrics(results);
        this.keyInsights = extractKeyInsights(results);

        this.metadata = new HashMap<>();
        initializeMetadata();

        this.phaseTimestamps = new HashMap<>();
        this.phaseTimestamps.put("creation", createdAt);
        this.phaseTimestamps.put("completion", completedAt);
    }

    /**
     * Get formatted output ready for presentation - merged from both versions
     */
    public String getFormattedOutput() {
        StringBuilder output = new StringBuilder();

        output.append("# Deep Research Report\n\n");
        output.append("**Topic:** ")
            .append(originalQuery)
            .append("\n");
        output.append("**Generated:** ")
            .append(createdAt)
            .append("\n");
        output.append("**Session ID:** ")
            .append(sessionId)
            .append("\n");
        if (strategyUsed != null) {
            output.append("**Strategy Used:** ")
                .append(strategyUsed)
                .append("\n");
        }
        if (processingTime != null && !processingTime.isZero()) {
            output.append("**Processing Time:** ")
                .append(processingTime.toMinutes())
                .append(" minutes\n");
        }
        output.append("\n");

        if (executiveSummary != null && !executiveSummary.trim()
            .isEmpty()) {
            output.append("## Executive Summary\n\n");
            output.append(executiveSummary)
                .append("\n\n");
        }

        output.append("## Research Overview\n\n");
        output.append(String.format("- **Total Sources**: %d\n", getAllCitations().size()));
        if (!researchQuestions.isEmpty()) {
            output.append(String.format("- **Research Questions**: %d\n", researchQuestions.size()));
            output.append(String.format("- **Questions Answered**: %d (%.1f%%)\n", metrics.getAnsweredQuestions(), metrics.getCompletionRate() * 100));
        }
        output.append(String.format("- **Word Count**: %,d words\n", (narrative != null ? narrative : finalReport).split("\\s+").length));

        if (qualityMetrics != null) {
            output.append(String.format("- **Overall Quality Score**: %.1f/10\n", qualityMetrics.getOverallScore() * 10));
        }
        if (metrics != null) {
            output.append(String.format("- **Average Relevance Score**: %.2f/10\n", metrics.getAvgRelevanceScore() * 10));
            output.append(String.format("- **Unique Domains**: %d\n", metrics.getUniqueDomains()));
        }
        output.append("\n");

        if (!keyInsights.isEmpty()) {
            output.append("## Key Insights\n\n");
            for (int i = 0; i < keyInsights.size(); i++) {
                ResearchInsight insight = keyInsights.get(i);
                output.append(String.format("%d. **%s**: %s\n", i + 1, insight.getTitle(), insight.getContent()));
            }
            output.append("\n");
        }

        if (!knowledgeMap.isEmpty()) {
            output.append("## Knowledge Insights\n\n");
            for (Map.Entry<String, Object> entry : knowledgeMap.entrySet()) {
                if (entry.getValue() instanceof String && !entry.getKey()
                    .equals("totalSources") && !entry.getKey()
                    .equals("categories")) {
                    output.append("**")
                        .append(entry.getKey())
                        .append("**: ");
                    output.append(entry.getValue())
                        .append("\n\n");
                }
            }
        }

        output.append("## Comprehensive Analysis\n\n");
        String mainContent = narrative != null ? narrative : finalReport;
        if (mainContent != null) {
            output.append(mainContent)
                .append("\n\n");
        }

        if (!researchQuestions.isEmpty()) {
            output.append("## Research Questions Explored\n\n");
            for (int i = 0; i < researchQuestions.size(); i++) {
                ResearchQuestion question = researchQuestions.get(i);
                output.append(
                    String.format("%d. **%s** (Priority: %s, Category: %s)\n", i + 1, question.getQuestion(), question.getPriority(), question.getCategory()));
                if (question.isResearched()) {
                    output.append("   ✅ Researched\n");
                } else {
                    output.append("   ⏳ Pending\n");
                }
                output.append("\n");
            }
        }

        if (methodology != null && !methodology.trim()
            .isEmpty()) {
            output.append("## Research Methodology\n\n");
            output.append(methodology)
                .append("\n\n");
        }

        if (qualityMetrics != null) {
            output.append("## Research Quality Summary\n\n");
            output.append(getQualityMetricsSummary())
                .append("\n\n");
        }

        output.append("## Sources and References\n\n");
        output.append(formatBibliography())
            .append("\n");

        return output.toString();
    }

    /**
     * Get research summary for quick overview - merged from both versions
     */
    public ResearchSummary getSummary() {
        return new ResearchSummary(originalQuery, keyInsights.size() > 0 ? keyInsights.size() : researchQuestions.size(), getAllCitations().size(),
            (narrative != null ? narrative : finalReport).split("\\s+").length,
            qualityMetrics != null ? qualityMetrics.getOverallScore() : metrics.getQualityScore(), createdAt, strategyUsed);
    }

    /**
     * Get performance analytics - from results version
     */
    public PerformanceAnalytics getPerformanceAnalytics() {
        return new PerformanceAnalytics(performanceMetrics, results != null ? results.getMetrics() : null, phaseTimestamps);
    }

    /**
     * Export result in different formats - merged from both versions
     */
    public Map<String, Object> exportAsMap() {
        Map<String, Object> export = new HashMap<>();

        export.put("sessionId", sessionId);
        export.put("originalQuery", originalQuery);
        export.put("narrative", narrative);
        export.put("finalReport", finalReport);
        export.put("executiveSummary", executiveSummary);
        export.put("methodology", methodology);
        export.put("createdAt", createdAt.toString());
        export.put("completedAt", completedAt.toString());

        if (strategyUsed != null) {
            export.put("strategyUsed", strategyUsed);
        }
        if (processingTime != null) {
            export.put("processingTime", processingTime.toString());
        }

        export.put("totalSources", getAllCitations().size());
        export.put("totalQuestions", researchQuestions.size());
        if (results != null && results.getCategorizedResults() != null) {
            export.put("categories", results.getCategorizedResults()
                .keySet());
        }

        export.put("keyInsights", keyInsights.stream()
            .map(insight -> Map.of("title", insight.getTitle(), "content", insight.getContent(), "confidence", insight.getConfidence()))
            .collect(Collectors.toList()));

        export.put("researchQuestions", researchQuestions.stream()
            .map(q -> Map.of("question", q.getQuestion(), "category", q.getCategory(), "priority", q.getPriority()
                .toString(), "researched", q.isResearched()))
            .collect(Collectors.toList()));

        if (qualityMetrics != null) {
            export.put("qualityScore", qualityMetrics.getOverallScore());
            export.put("wordCount", qualityMetrics.getWordCount());
            export.put("sourceQuality", qualityMetrics.getSourceQualityScore());
        }
        if (metrics != null) {
            export.put("avgRelevanceScore", metrics.getAvgRelevanceScore());
            export.put("completionRate", metrics.getCompletionRate());
            export.put("uniqueDomains", metrics.getUniqueDomains());
        }

        if (config != null) {
            export.put("researchDepth", config.getResearchDepth()
                .toString());
        }
        export.put("metadata", metadata);
        export.put("knowledgeMap", knowledgeMap);

        return export;
    }

    /**
     * Validate result completeness and quality - merged logic from both versions
     */
    public ValidationResult validate() {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String content = narrative != null ? narrative : finalReport;

        if (content == null || content.trim()
            .isEmpty()) {
            issues.add("Content is empty or missing");
        } else {
            int wordCount = content.split("\\s+").length;
            if (wordCount < 1000) {
                warnings.add("Content is shorter than recommended (< 1000 words)");
            }
        }

        int sourceCount = getAllCitations().size();
        if (sourceCount < 5) {
            warnings.add("Fewer than 5 sources used");
        } else if (sourceCount < 10) {
            warnings.add("Fewer than 10 sources used (recommended minimum)");
        }

        if (executiveSummary == null || executiveSummary.trim()
            .isEmpty()) {
            warnings.add("Executive summary is missing");
        }

        if (methodology == null || methodology.trim()
            .isEmpty() || methodology.equals("Research methodology not specified")) {
            warnings.add("Research methodology not documented");
        }

        if (metrics != null && metrics.getCompletionRate() < 0.5) {
            warnings.add("Less than 50% of research questions answered");
        }

        if (processingTime != null && processingTime.toMinutes() > 30) {
            warnings.add("Processing took longer than 30 minutes");
        }

        return new ValidationResult(issues, warnings, issues.isEmpty());
    }

    private QualityMetrics createQualityMetrics(String content, ResearchResults results) {
        int wordCount = content != null ? content.split("\\s+").length : 0;
        double sourceQuality = calculateSourceQuality(getAllCitations());
        double coherenceScore = assessCoherence(content);
        double comprehensivenessScore = assessComprehensiveness(results);

        return new QualityMetrics(wordCount, sourceQuality, coherenceScore, comprehensivenessScore);
    }

    private ResearchMetrics calculateResearchMetrics() {
        int totalSources = getAllCitations().size();
        int totalQuestions = researchQuestions.size();
        int answeredQuestions = (int) researchQuestions.stream()
            .mapToLong(q -> q.isResearched() ? 1 : 0)
            .sum();

        double avgRelevanceScore = getAllCitations().stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);

        Set<String> uniqueDomains = getAllCitations().stream()
            .map(CitationResult::getDomain)
            .collect(Collectors.toSet());

        Duration procTime = processingTime != null ? processingTime : Duration.ZERO;

        return new ResearchMetrics(totalSources, totalQuestions, answeredQuestions, avgRelevanceScore, uniqueDomains.size(), procTime);
    }

    private double calculateSourceQuality(List<CitationResult> citations) {
        if (citations.isEmpty()) {
            return 0.0;
        }
        return citations.stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);
    }

    private double assessCoherence(String content) {
        if (content == null || content.trim()
            .isEmpty()) {
            return 0.0;
        }

        int sections = content.split("##").length - 1;
        int paragraphs = content.split("\n\n").length;

        double structureScore = Math.min(sections / 8.0, 1.0);
        double lengthScore = Math.min(content.length() / 40000.0, 1.0);

        return (structureScore + lengthScore) / 2.0;
    }

    private double assessComprehensiveness(ResearchResults results) {
        if (results == null) {
            return 0.0;
        }

        int categories = results.getCategorizedResults() != null ? results.getCategorizedResults()
            .size() : 0;
        int sources = getAllCitations().size();
        int insights = results.getInsights() != null ? results.getInsights()
            .size() : 0;

        double categoryScore = Math.min(categories / 6.0, 1.0);
        double sourceScore = Math.min(sources / 30.0, 1.0);
        double insightScore = Math.min(insights / 15.0, 1.0);

        return (categoryScore + sourceScore + insightScore) / 3.0;
    }

    private List<ResearchInsight> extractKeyInsights(ResearchResults results) {
        List<ResearchInsight> insights = new ArrayList<>();

        if (results == null || results.getInsights() == null) {
            return insights;
        }

        Map<String, String> allInsights = results.getInsights();
        List<Map.Entry<String, String>> sortedInsights = allInsights.entrySet()
            .stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue()
                .length(), e1.getValue()
                .length()))
            .limit(5)
            .collect(Collectors.toList());

        for (Map.Entry<String, String> entry : sortedInsights) {
            String title = generateInsightTitle(entry.getKey());
            double confidence = calculateInsightConfidence(entry.getValue(), results);
            insights.add(new ResearchInsight(title, entry.getValue(), confidence));
        }

        return insights;
    }

    private String generateInsightTitle(String question) {
        String[] words = question.split("\\s+");
        if (words.length <= 6) {
            return question;
        }

        StringBuilder title = new StringBuilder();
        int count = 0;
        for (String word : words) {
            if (word.length() > 3 && count < 5) {
                title.append(word)
                    .append(" ");
                count++;
            }
        }

        return title.toString()
            .trim();
    }

    private double calculateInsightConfidence(String insight, ResearchResults results) {
        double lengthScore = Math.min(insight.length() / 500.0, 1.0);
        double sourceScore = calculateSourceQuality(getAllCitations());
        return (lengthScore + sourceScore) / 2.0;
    }

    private void initializeMetadata() {
        metadata.put("version", "1.0");
        metadata.put("engine", "DeepResearchEngine");

        String content = narrative != null ? narrative : finalReport;
        if (content != null) {
            metadata.put("totalSections", content.split("##").length - 1);
        }

        metadata.put("averageSourceRelevance", calculateSourceQuality(getAllCitations()));
        metadata.put("researchComplexity", determineResearchComplexity());

        if (results != null && results.getCategorizedResults() != null) {
            metadata.put("categories", results.getCategorizedResults()
                .keySet());
        }
    }

    private String determineResearchComplexity() {
        int sources = getAllCitations().size();
        int categories = results != null && results.getCategorizedResults() != null ? results.getCategorizedResults()
            .size() : 0;

        if (sources >= 40 && categories >= 6) {
            return "Expert";
        }
        if (sources >= 25 && categories >= 4) {
            return "Comprehensive";
        }
        if (sources >= 15 && categories >= 3) {
            return "Standard";
        }
        return "Basic";
    }

    private String getQualityMetricsSummary() {
        if (qualityMetrics == null) {
            return "Quality metrics not available";
        }

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("- **Overall Quality Score**: %.1f/10\n", qualityMetrics.getOverallScore() * 10));
        summary.append(String.format("- **Word Count**: %,d words\n", qualityMetrics.getWordCount()));
        summary.append(String.format("- **Sources Analyzed**: %,d\n", getAllCitations().size()));

        if (results != null && results.getCategorizedResults() != null) {
            summary.append(String.format("- **Research Categories**: %d\n", results.getCategorizedResults()
                .size()));
        }

        summary.append(String.format("- **Average Source Quality**: %.1f/10\n", qualityMetrics.getSourceQualityScore() * 10));

        if (config != null) {
            summary.append(String.format("- **Research Depth**: %s\n", config.getResearchDepth()));
        }

        return summary.toString();
    }

    private String formatBibliography() {
        StringBuilder bibliography = new StringBuilder();

        if (results != null && results.getCategorizedResults() != null && !results.getCategorizedResults()
            .isEmpty()) {
            Map<String, List<CitationResult>> categorized = results.getCategorizedResults();
            for (Map.Entry<String, List<CitationResult>> category : categorized.entrySet()) {
                bibliography.append("### ")
                    .append(capitalize(category.getKey()))
                    .append("\n\n");
                List<CitationResult> citations = category.getValue();
                for (int i = 0; i < citations.size(); i++) {
                    CitationResult citation = citations.get(i);
                    bibliography.append(String.format("%d. **%s**\n", i + 1, citation.getTitle()));
                    bibliography.append(String.format("   - Source: %s\n", citation.getUrl()));
                    bibliography.append(String.format("   - Relevance Score: %.1f/10\n", citation.getRelevanceScore() * 10));
                    bibliography.append("\n");
                }
            }
        } else {

            Map<String, List<CitationResult>> citationsByDomain = getAllCitations().stream()
                .collect(Collectors.groupingBy(CitationResult::getDomain));

            int counter = 1;
            for (Map.Entry<String, List<CitationResult>> domainEntry : citationsByDomain.entrySet()) {
                bibliography.append("### ")
                    .append(capitalize(domainEntry.getKey()))
                    .append("\n\n");
                for (CitationResult citation : domainEntry.getValue()) {
                    bibliography.append(String.format("%d. **%s**\n", counter++, citation.getTitle()));
                    bibliography.append(String.format("   - URL: %s\n", citation.getUrl()));
                    bibliography.append(String.format("   - Relevance Score: %.1f/10\n", citation.getRelevanceScore() * 10));
                    bibliography.append("\n");
                }
            }
        }

        return bibliography.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1)
            .toUpperCase() + str.substring(1)
            .toLowerCase();
    }

    private String generateBasicExecutiveSummary(String content) {
        if (content == null || content.trim()
            .isEmpty()) {
            return "Executive summary not available.";
        }

        String[] sentences = content.split("\\.");
        StringBuilder summary = new StringBuilder("Executive Summary: ");

        int sentenceCount = Math.min(3, sentences.length);
        for (int i = 0; i < sentenceCount; i++) {
            summary.append(sentences[i].trim())
                .append(". ");
        }

        return summary.toString();
    }

    private ResearchResults createSyntheticResearchResults(List<CitationResult> citations, Map<String, Object> knowledgeMap) {
        Map<String, List<CitationResult>> categorized = citations.stream()
            .collect(Collectors.groupingBy(CitationResult::getDomain));

        Map<String, String> insights = new HashMap<>();
        for (Map.Entry<String, Object> entry : knowledgeMap.entrySet()) {
            if (entry.getValue() instanceof String) {
                insights.put(entry.getKey(), (String) entry.getValue());
            }
        }

        return new ResearchResults(citations, categorized, insights);
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public String getNarrative() {
        return narrative;
    }

    public String getFinalReport() {
        return finalReport;
    }

    public String getExecutiveSummary() {
        return executiveSummary;
    }

    public String getMethodology() {
        return methodology;
    }

    public ResearchResults getResults() {
        return results;
    }

    public List<CitationResult> getAllCitations() {
        return new ArrayList<>(allCitations);
    }

    public List<ResearchQuestion> getResearchQuestions() {
        return new ArrayList<>(researchQuestions);
    }

    public Map<String, Object> getKnowledgeMap() {
        return new HashMap<>(knowledgeMap);
    }

    public Duration getProcessingTime() {
        return processingTime;
    }

    public String getStrategyUsed() {
        return strategyUsed;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getCompletedAt() {
        return completedAt;
    }

    public QualityMetrics getQualityMetrics() {
        return qualityMetrics;
    }

    public PerformanceMetrics getPerformanceMetrics() {
        return performanceMetrics;
    }

    public ResearchMetrics getMetrics() {
        return metrics;
    }

    public DeepResearchConfig getConfig() {
        return config;
    }

    public List<ResearchInsight> getKeyInsights() {
        return new ArrayList<>(keyInsights);
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public Map<String, Instant> getPhaseTimestamps() {
        return new HashMap<>(phaseTimestamps);
    }

    public static class QualityMetrics {

        private final int wordCount;
        private final double sourceQualityScore;
        private final double coherenceScore;
        private final double comprehensivenessScore;
        private final double overallScore;

        public QualityMetrics(int wordCount, double sourceQualityScore, double coherenceScore, double comprehensivenessScore) {
            this.wordCount = wordCount;
            this.sourceQualityScore = sourceQualityScore;
            this.coherenceScore = coherenceScore;
            this.comprehensivenessScore = comprehensivenessScore;
            this.overallScore = (sourceQualityScore + coherenceScore + comprehensivenessScore) / 3.0;
        }

        public int getWordCount() {
            return wordCount;
        }

        public double getSourceQualityScore() {
            return sourceQualityScore;
        }

        public double getCoherenceScore() {
            return coherenceScore;
        }

        public double getComprehensivenessScore() {
            return comprehensivenessScore;
        }

        public double getOverallScore() {
            return overallScore;
        }
    }

    public static class PerformanceMetrics {

        private final int totalSources;
        private final int totalCategories;
        private final double averageRelevanceScore;
        private final Map<String, Integer> categoryDistribution;

        public PerformanceMetrics(ResearchResults results) {
            if (results != null) {
                this.totalSources = results.getAllCitations()
                    .size();
                this.totalCategories = results.getCategorizedResults() != null ? results.getCategorizedResults()
                    .size() : 0;
                this.averageRelevanceScore = results.getAllCitations()
                    .stream()
                    .mapToDouble(CitationResult::getRelevanceScore)
                    .average()
                    .orElse(0.0);
                this.categoryDistribution = results.getCategorizedResults() != null ? results.getCategorizedResults()
                    .entrySet()
                    .stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, entry -> entry.getValue()
                        .size())) : new HashMap<>();
            } else {
                this.totalSources = 0;
                this.totalCategories = 0;
                this.averageRelevanceScore = 0.0;
                this.categoryDistribution = new HashMap<>();
            }
        }

        public int getTotalSources() {
            return totalSources;
        }

        public int getTotalCategories() {
            return totalCategories;
        }

        public double getAverageRelevanceScore() {
            return averageRelevanceScore;
        }

        public Map<String, Integer> getCategoryDistribution() {
            return new HashMap<>(categoryDistribution);
        }
    }

    public static class ResearchMetrics {

        private final int totalSources;
        private final int totalQuestions;
        private final int answeredQuestions;
        private final double avgRelevanceScore;
        private final int uniqueDomains;
        private final Duration processingTime;

        public ResearchMetrics(int totalSources, int totalQuestions, int answeredQuestions, double avgRelevanceScore, int uniqueDomains,
            Duration processingTime) {
            this.totalSources = totalSources;
            this.totalQuestions = totalQuestions;
            this.answeredQuestions = answeredQuestions;
            this.avgRelevanceScore = avgRelevanceScore;
            this.uniqueDomains = uniqueDomains;
            this.processingTime = processingTime;
        }

        public int getTotalSources() {
            return totalSources;
        }

        public int getTotalQuestions() {
            return totalQuestions;
        }

        public int getAnsweredQuestions() {
            return answeredQuestions;
        }

        public double getAvgRelevanceScore() {
            return avgRelevanceScore;
        }

        public int getUniqueDomains() {
            return uniqueDomains;
        }

        public Duration getProcessingTime() {
            return processingTime;
        }

        public double getCompletionRate() {
            return totalQuestions > 0 ? (double) answeredQuestions / totalQuestions : 0.0;
        }

        public double getQualityScore() {
            double relevanceWeight = avgRelevanceScore * 0.4;
            double completionWeight = getCompletionRate() * 0.3;
            double diversityWeight = Math.min(1.0, uniqueDomains / 10.0) * 0.2;
            double efficiencyWeight = processingTime != null ? Math.max(0.0, 1.0 - (processingTime.toMinutes() / 30.0)) * 0.1 : 0.0;

            return relevanceWeight + completionWeight + diversityWeight + efficiencyWeight;
        }
    }

    public static class ResearchInsight {

        private final String title;
        private final String content;
        private final double confidence;
        private final Instant extractedAt;

        public ResearchInsight(String title, String content, double confidence) {
            this.title = title;
            this.content = content;
            this.confidence = confidence;
            this.extractedAt = Instant.now();
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public double getConfidence() {
            return confidence;
        }

        public Instant getExtractedAt() {
            return extractedAt;
        }
    }

    public static class ResearchSummary {

        private final String query;
        private final int insightCount;
        private final int sourceCount;
        private final int wordCount;
        private final double qualityScore;
        private final Instant createdAt;
        private final String strategy;

        public ResearchSummary(String query, int insightCount, int sourceCount, int wordCount, double qualityScore, Instant createdAt, String strategy) {
            this.query = query;
            this.insightCount = insightCount;
            this.sourceCount = sourceCount;
            this.wordCount = wordCount;
            this.qualityScore = qualityScore;
            this.createdAt = createdAt;
            this.strategy = strategy;
        }

        public String getQuery() {
            return query;
        }

        public int getInsightCount() {
            return insightCount;
        }

        public int getSourceCount() {
            return sourceCount;
        }

        public int getWordCount() {
            return wordCount;
        }

        public double getQualityScore() {
            return qualityScore;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }

        public String getStrategy() {
            return strategy;
        }

        @Override
        public String toString() {
            return String.format("Research Summary: '%s' - %d sources, %d insights, %,d words (Quality: %.1f/10) [%s]", query, sourceCount, insightCount,
                wordCount, qualityScore * 10, strategy != null ? strategy : "N/A");
        }
    }

    public static class PerformanceAnalytics {

        private final PerformanceMetrics performanceMetrics;
        private final Object researchMetrics;
        private final Map<String, Instant> phaseTimestamps;

        public PerformanceAnalytics(PerformanceMetrics performanceMetrics, Object researchMetrics, Map<String, Instant> phaseTimestamps) {
            this.performanceMetrics = performanceMetrics;
            this.researchMetrics = researchMetrics;
            this.phaseTimestamps = phaseTimestamps != null ? new HashMap<>(phaseTimestamps) : new HashMap<>();
        }

        public PerformanceMetrics getPerformanceMetrics() {
            return performanceMetrics;
        }

        public Object getResearchMetrics() {
            return researchMetrics;
        }

        public Map<String, Instant> getPhaseTimestamps() {
            return new HashMap<>(phaseTimestamps);
        }

        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("# Performance Analytics Report\n\n");

            if (performanceMetrics != null) {
                report.append(String.format("- Total Sources: %d\n", performanceMetrics.getTotalSources()));
                report.append(String.format("- Categories: %d\n", performanceMetrics.getTotalCategories()));
                report.append(String.format("- Average Relevance: %.2f\n", performanceMetrics.getAverageRelevanceScore()));
            }

            if (!phaseTimestamps.isEmpty()) {
                report.append("\n## Phase Timestamps\n");
                for (Map.Entry<String, Instant> phase : phaseTimestamps.entrySet()) {
                    report.append(String.format("- %s: %s\n", phase.getKey(), phase.getValue()));
                }
            }

            return report.toString();
        }
    }

    public static class ValidationResult {

        private final List<String> issues;
        private final List<String> warnings;
        private final boolean isValid;

        public ValidationResult(List<String> issues, List<String> warnings, boolean isValid) {
            this.issues = new ArrayList<>(issues);
            this.warnings = new ArrayList<>(warnings);
            this.isValid = isValid;
        }

        public List<String> getIssues() {
            return new ArrayList<>(issues);
        }

        public List<String> getWarnings() {
            return new ArrayList<>(warnings);
        }

        public boolean isValid() {
            return isValid;
        }

        public boolean hasWarnings() {
            return !warnings.isEmpty();
        }

        public String getReport() {
            StringBuilder report = new StringBuilder();

            if (isValid) {
                report.append("✅ Validation PASSED\n");
            } else {
                report.append("❌ Validation FAILED\n");
            }

            if (!issues.isEmpty()) {
                report.append("\nIssues:\n");
                for (String issue : issues) {
                    report.append("- ")
                        .append(issue)
                        .append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                report.append("\nWarnings:\n");
                for (String warning : warnings) {
                    report.append("- ")
                        .append(warning)
                        .append("\n");
                }
            }

            return report.toString();
        }
    }

    /**
     * Check if this result contains comprehensive research data
     */
    public boolean isComprehensive() {
        return getAllCitations().size() >= 15 && (narrative != null ? narrative.split("\\s+").length : 0) >= 2000 &&
            (qualityMetrics != null ? qualityMetrics.getOverallScore() >= 0.7 : false);
    }

    /**
     * Get research depth assessment
     */
    public String getResearchDepthAssessment() {
        int sources = getAllCitations().size();
        int wordCount = (narrative != null ? narrative : finalReport).split("\\s+").length;
        double qualityScore = qualityMetrics != null ? qualityMetrics.getOverallScore() : 0.0;

        if (sources >= 30 && wordCount >= 5000 && qualityScore >= 0.8) {
            return "Expert-Level Research";
        } else if (sources >= 20 && wordCount >= 3000 && qualityScore >= 0.7) {
            return "Comprehensive Research";
        } else if (sources >= 10 && wordCount >= 1500 && qualityScore >= 0.6) {
            return "Standard Research";
        } else {
            return "Basic Research";
        }
    }

    /**
     * Get citation statistics
     */
    public Map<String, Object> getCitationStatistics() {
        Map<String, Object> stats = new HashMap<>();
        List<CitationResult> citations = getAllCitations();

        stats.put("totalCitations", citations.size());

        if (!citations.isEmpty()) {
            stats.put("averageRelevanceScore", citations.stream()
                .mapToDouble(CitationResult::getRelevanceScore)
                .average()
                .orElse(0.0));

            Map<String, Long> domainCounts = citations.stream()
                .collect(Collectors.groupingBy(CitationResult::getDomain, Collectors.counting()));
            stats.put("domainDistribution", domainCounts);
            stats.put("uniqueDomains", domainCounts.size());

            stats.put("highQualitySources", citations.stream()
                .mapToLong(c -> c.getRelevanceScore() >= 0.8 ? 1 : 0)
                .sum());
        } else {
            stats.put("averageRelevanceScore", 0.0);
            stats.put("domainDistribution", new HashMap<>());
            stats.put("uniqueDomains", 0);
            stats.put("highQualitySources", 0);
        }

        return stats;
    }

    /**
     * Generate compact summary for dashboards or APIs
     */
    public Map<String, Object> getCompactSummary() {
        Map<String, Object> compact = new HashMap<>();

        compact.put("sessionId", sessionId);
        compact.put("query", originalQuery);
        compact.put("status", isValid() ? "completed" : "incomplete");
        compact.put("sources", getAllCitations().size());
        compact.put("wordCount", (narrative != null ? narrative : finalReport).split("\\s+").length);
        compact.put("qualityScore", qualityMetrics != null ? Math.round(qualityMetrics.getOverallScore() * 100.0) / 10.0 : 0.0);
        compact.put("createdAt", createdAt);
        compact.put("processingTime", processingTime != null ? processingTime.toMinutes() : 0);
        compact.put("researchDepth", getResearchDepthAssessment());

        return compact;
    }

    /**
     * Check if the result is valid (no critical issues)
     */
    private boolean isValid() {
        ValidationResult validation = validate();
        return validation.isValid();
    }

    /**
     * Generate a research report card with letter grades
     */
    public String generateReportCard() {
        StringBuilder card = new StringBuilder();
        card.append("📊 Research Report Card\n");
        card.append("=".repeat(30))
            .append("\n\n");

        String content = narrative != null ? narrative : finalReport;
        int wordCount = content != null ? content.split("\\s+").length : 0;
        String contentGrade = wordCount >= 5000 ? "A" : wordCount >= 3000 ? "B" : wordCount >= 1500 ? "C" : wordCount >= 500 ? "D" : "F";
        card.append(String.format("📝 Content Quality: %s (%,d words)\n", contentGrade, wordCount));

        int sourceCount = getAllCitations().size();
        double avgRelevance = getAllCitations().stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);
        String sourceGrade = sourceCount >= 25 && avgRelevance >= 0.8 ? "A" : sourceCount >= 15 && avgRelevance >= 0.7 ? "B" :
            sourceCount >= 10 && avgRelevance >= 0.6 ? "C" : sourceCount >= 5 && avgRelevance >= 0.5 ? "D" : "F";
        card.append(String.format("📚 Source Quality: %s (%d sources, %.1f avg relevance)\n", sourceGrade, sourceCount, avgRelevance * 10));

        String depthAssessment = getResearchDepthAssessment();
        String depthGrade =
            depthAssessment.startsWith("Expert") ? "A" : depthAssessment.startsWith("Comprehensive") ? "B" : depthAssessment.startsWith("Standard") ? "C" : "D";
        card.append(String.format("🔍 Research Depth: %s (%s)\n", depthGrade, depthAssessment));

        double overallScore = qualityMetrics != null ? qualityMetrics.getOverallScore() : 0.0;
        String overallGrade =
            overallScore >= 0.9 ? "A+" : overallScore >= 0.8 ? "A" : overallScore >= 0.7 ? "B" : overallScore >= 0.6 ? "C" : overallScore >= 0.5 ? "D" : "F";
        card.append(String.format("🎯 Overall Grade: %s (%.1f/10)\n", overallGrade, overallScore * 10));

        return card.toString();
    }
}