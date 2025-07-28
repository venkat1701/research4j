package io.github.venkat1701.deepresearch.results;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.ResearchResults;

/**
 * Deep Research Result - Comprehensive container for all research outputs
 * Represents the final deliverable from the Deep Research process
 * Contains narrative, sources, metadata, and quality metrics
 */
public class DeepResearchResult {

    // Core Result Data
    private final String sessionId;
    private final String originalQuery;
    private final String narrative;
    private final String executiveSummary;
    private final String methodology;

    // Research Data
    private final ResearchResults results;
    private final Map<String, Object> metadata;
    private final List<ResearchInsight> keyInsights;

    // Quality and Performance Metrics
    private final QualityMetrics qualityMetrics;
    private final PerformanceMetrics performanceMetrics;
    private final DeepResearchConfig config;

    // Timestamps
    private final Instant createdAt;
    private final Map<String, Instant> phaseTimestamps;

    public DeepResearchResult(String sessionId,
        String originalQuery,
        String narrative,
        String executiveSummary,
        String methodology,
        ResearchResults results,
        Object qualityMetrics, // Using Object to match the engine call
        DeepResearchConfig config) {
        this.sessionId = sessionId;
        this.originalQuery = originalQuery;
        this.narrative = narrative;
        this.executiveSummary = executiveSummary;
        this.methodology = methodology;
        this.results = results;
        this.config = config;
        this.createdAt = Instant.now();

        // Initialize metrics
        this.qualityMetrics = createQualityMetrics(narrative, results);
        this.performanceMetrics = new PerformanceMetrics(results);

        // Extract key insights
        this.keyInsights = extractKeyInsights(results);

        // Initialize metadata
        this.metadata = new HashMap<>();
        initializeMetadata();

        // Phase timestamps (would be populated during actual execution)
        this.phaseTimestamps = new HashMap<>();
        this.phaseTimestamps.put("creation", createdAt);
    }

    /**
     * Get formatted output ready for presentation
     */
    public String getFormattedOutput() {
        StringBuilder output = new StringBuilder();

        // Title and Executive Summary
        output.append("# Deep Research Report\n\n");
        output.append("**Topic:** ").append(originalQuery).append("\n");
        output.append("**Generated:** ").append(createdAt).append("\n");
        output.append("**Session ID:** ").append(sessionId).append("\n\n");

        output.append("## Executive Summary\n\n");
        output.append(executiveSummary).append("\n\n");

        // Key Insights
        if (!keyInsights.isEmpty()) {
            output.append("## Key Insights\n\n");
            for (int i = 0; i < keyInsights.size(); i++) {
                ResearchInsight insight = keyInsights.get(i);
                output.append(String.format("%d. **%s**: %s\n",
                    i + 1, insight.getTitle(), insight.getContent()));
            }
            output.append("\n");
        }

        // Main Narrative
        output.append("## Comprehensive Analysis\n\n");
        output.append(narrative).append("\n\n");

        // Research Methodology
        output.append("## Research Methodology\n\n");
        output.append(methodology).append("\n\n");

        // Quality Metrics Summary
        output.append("## Research Quality Summary\n\n");
        output.append(getQualityMetricsSummary()).append("\n\n");

        // Source Bibliography
        output.append("## Sources and References\n\n");
        output.append(formatBibliography()).append("\n");

        return output.toString();
    }

    /**
     * Get research summary for quick overview
     */
    public ResearchSummary getSummary() {
        return new ResearchSummary(
            originalQuery,
            keyInsights.size(),
            results.getAllCitations().size(),
            qualityMetrics.getWordCount(),
            qualityMetrics.getOverallScore(),
            createdAt
        );
    }

    /**
     * Get performance analytics
     */
    public PerformanceAnalytics getPerformanceAnalytics() {
        return new PerformanceAnalytics(
            performanceMetrics,
            results.getMetrics(),
            phaseTimestamps
        );
    }

    /**
     * Export result in different formats
     */
    public Map<String, Object> exportAsMap() {
        Map<String, Object> export = new HashMap<>();

        export.put("sessionId", sessionId);
        export.put("originalQuery", originalQuery);
        export.put("narrative", narrative);
        export.put("executiveSummary", executiveSummary);
        export.put("methodology", methodology);
        export.put("createdAt", createdAt.toString());

        // Research data
        export.put("totalSources", results.getAllCitations().size());
        export.put("categories", results.getCategorizedResults().keySet());
        export.put("keyInsights", keyInsights.stream()
            .map(insight -> Map.of(
                "title", insight.getTitle(),
                "content", insight.getContent(),
                "confidence", insight.getConfidence()
            ))
            .collect(Collectors.toList()));

        // Quality metrics
        export.put("qualityScore", qualityMetrics.getOverallScore());
        export.put("wordCount", qualityMetrics.getWordCount());
        export.put("sourceQuality", qualityMetrics.getSourceQualityScore());

        // Configuration
        export.put("researchDepth", config.getResearchDepth().toString());
        export.put("metadata", metadata);

        return export;
    }

    /**
     * Validate result completeness and quality
     */
    public ValidationResult validate() {
        List<String> issues = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        // Check narrative completeness
        if (narrative == null || narrative.trim().isEmpty()) {
            issues.add("Narrative is empty or missing");
        } else if (qualityMetrics.getWordCount() < 2000) {
            warnings.add("Narrative is shorter than recommended (< 2000 words)");
        }

        // Check source quality
        if (results.getAllCitations().size() < 10) {
            warnings.add("Fewer than 10 sources used");
        }

        // Check executive summary
        if (executiveSummary == null || executiveSummary.trim().isEmpty()) {
            issues.add("Executive summary is missing");
        }

        // Check methodology documentation
        if (methodology == null || methodology.trim().isEmpty()) {
            warnings.add("Research methodology not documented");
        }

        return new ValidationResult(issues, warnings, issues.isEmpty());
    }

    // Private helper methods

    private QualityMetrics createQualityMetrics(String narrative, ResearchResults results) {
        int wordCount = narrative != null ? narrative.split("\\s+").length : 0;
        double sourceQuality = calculateSourceQuality(results.getAllCitations());
        double coherenceScore = assessCoherence(narrative);
        double comprehensivenessScore = assessComprehensiveness(results);

        return new QualityMetrics(wordCount, sourceQuality, coherenceScore, comprehensivenessScore);
    }

    private double calculateSourceQuality(List<CitationResult> citations) {
        if (citations.isEmpty()) return 0.0;

        return citations.stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);
    }

    private double assessCoherence(String narrative) {
        if (narrative == null || narrative.trim().isEmpty()) return 0.0;

        // Simple coherence assessment based on structure
        int sections = narrative.split("##").length - 1;
        int paragraphs = narrative.split("\n\n").length;

        // Basic coherence score based on structure and length
        double structureScore = Math.min(sections / 8.0, 1.0); // Target 8+ sections
        double lengthScore = Math.min(narrative.length() / 40000.0, 1.0); // Target 40k+ chars

        return (structureScore + lengthScore) / 2.0;
    }

    private double assessComprehensiveness(ResearchResults results) {
        int categories = results.getCategorizedResults().size();
        int sources = results.getAllCitations().size();
        int insights = results.getInsights().size();

        // Comprehensiveness based on breadth and depth
        double categoryScore = Math.min(categories / 6.0, 1.0); // Target 6+ categories
        double sourceScore = Math.min(sources / 30.0, 1.0); // Target 30+ sources
        double insightScore = Math.min(insights / 15.0, 1.0); // Target 15+ insights

        return (categoryScore + sourceScore + insightScore) / 3.0;
    }

    private List<ResearchInsight> extractKeyInsights(ResearchResults results) {
        List<ResearchInsight> insights = new ArrayList<>();

        // Extract top insights from research results
        Map<String, String> allInsights = results.getInsights();

        // Sort insights by quality/relevance (simplified approach)
        List<Map.Entry<String, String>> sortedInsights = allInsights.entrySet().stream()
            .sorted((e1, e2) -> Integer.compare(e2.getValue().length(), e1.getValue().length()))
            .limit(5) // Top 5 insights
            .collect(Collectors.toList());

        for (Map.Entry<String, String> entry : sortedInsights) {
            String title = generateInsightTitle(entry.getKey());
            double confidence = calculateInsightConfidence(entry.getValue(), results);
            insights.add(new ResearchInsight(title, entry.getValue(), confidence));
        }

        return insights;
    }

    private String generateInsightTitle(String question) {
        // Generate a concise title from the research question
        String[] words = question.split("\\s+");
        if (words.length <= 6) {
            return question;
        }

        // Take first few meaningful words
        StringBuilder title = new StringBuilder();
        int count = 0;
        for (String word : words) {
            if (word.length() > 3 && count < 5) {
                title.append(word).append(" ");
                count++;
            }
        }

        return title.toString().trim();
    }

    private double calculateInsightConfidence(String insight, ResearchResults results) {
        // Simple confidence calculation based on insight length and source quality
        double lengthScore = Math.min(insight.length() / 500.0, 1.0);
        double sourceScore = calculateSourceQuality(results.getAllCitations());

        return (lengthScore + sourceScore) / 2.0;
    }

    private void initializeMetadata() {
        metadata.put("version", "1.0");
        metadata.put("engine", "DeepResearchEngine");
        metadata.put("totalSections", narrative.split("##").length - 1);
        metadata.put("averageSourceRelevance", calculateSourceQuality(results.getAllCitations()));
        metadata.put("researchComplexity", determineResearchComplexity());
        metadata.put("categories", results.getCategorizedResults().keySet());
    }

    private String determineResearchComplexity() {
        int sources = results.getAllCitations().size();
        int categories = results.getCategorizedResults().size();

        if (sources >= 40 && categories >= 6) return "Expert";
        if (sources >= 25 && categories >= 4) return "Comprehensive";
        if (sources >= 15 && categories >= 3) return "Standard";
        return "Basic";
    }

    private String getQualityMetricsSummary() {
        StringBuilder summary = new StringBuilder();

        summary.append(String.format("- **Overall Quality Score**: %.1f/10\n",
            qualityMetrics.getOverallScore() * 10));
        summary.append(String.format("- **Word Count**: %,d words\n",
            qualityMetrics.getWordCount()));
        summary.append(String.format("- **Sources Analyzed**: %,d\n",
            results.getAllCitations().size()));
        summary.append(String.format("- **Research Categories**: %d\n",
            results.getCategorizedResults().size()));
        summary.append(String.format("- **Average Source Quality**: %.1f/10\n",
            qualityMetrics.getSourceQualityScore() * 10));
        summary.append(String.format("- **Research Depth**: %s\n",
            config.getResearchDepth()));

        return summary.toString();
    }

    private String formatBibliography() {
        StringBuilder bibliography = new StringBuilder();

        Map<String, List<CitationResult>> categorized = results.getCategorizedResults();

        for (Map.Entry<String, List<CitationResult>> category : categorized.entrySet()) {
            bibliography.append("### ").append(capitalize(category.getKey())).append("\n\n");

            List<CitationResult> citations = category.getValue();
            for (int i = 0; i < citations.size(); i++) {
                CitationResult citation = citations.get(i);
                bibliography.append(String.format("%d. **%s**\n", i + 1, citation.getTitle()));
                bibliography.append(String.format("   - Source: %s\n", citation.getUrl()));
                bibliography.append(String.format("   - Relevance Score: %.1f/10\n",
                    citation.getRelevanceScore() * 10));
                bibliography.append("\n");
            }
        }

        return bibliography.toString();
    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) return str;
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    // Getters
    public String getSessionId() { return sessionId; }
    public String getOriginalQuery() { return originalQuery; }
    public String getNarrative() { return narrative; }
    public String getExecutiveSummary() { return executiveSummary; }
    public String getMethodology() { return methodology; }
    public ResearchResults getResults() { return results; }
    public QualityMetrics getQualityMetrics() { return qualityMetrics; }
    public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
    public DeepResearchConfig getConfig() { return config; }
    public Instant getCreatedAt() { return createdAt; }
    public List<ResearchInsight> getKeyInsights() { return new ArrayList<>(keyInsights); }
    public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }

    // Supporting Classes

    public static class QualityMetrics {
        private final int wordCount;
        private final double sourceQualityScore;
        private final double coherenceScore;
        private final double comprehensivenessScore;
        private final double overallScore;

        public QualityMetrics(int wordCount, double sourceQualityScore,
            double coherenceScore, double comprehensivenessScore) {
            this.wordCount = wordCount;
            this.sourceQualityScore = sourceQualityScore;
            this.coherenceScore = coherenceScore;
            this.comprehensivenessScore = comprehensivenessScore;
            this.overallScore = (sourceQualityScore + coherenceScore + comprehensivenessScore) / 3.0;
        }

        public int getWordCount() { return wordCount; }
        public double getSourceQualityScore() { return sourceQualityScore; }
        public double getCoherenceScore() { return coherenceScore; }
        public double getComprehensivenessScore() { return comprehensivenessScore; }
        public double getOverallScore() { return overallScore; }
    }

    public static class PerformanceMetrics {
        private final int totalSources;
        private final int totalCategories;
        private final double averageRelevanceScore;
        private final Map<String, Integer> categoryDistribution;

        public PerformanceMetrics(ResearchResults results) {
            this.totalSources = results.getAllCitations().size();
            this.totalCategories = results.getCategorizedResults().size();
            this.averageRelevanceScore = results.getAllCitations().stream()
                .mapToDouble(CitationResult::getRelevanceScore)
                .average()
                .orElse(0.0);
            this.categoryDistribution = results.getCategorizedResults().entrySet().stream()
                .collect(Collectors.toMap(
                    Map.Entry::getKey,
                    entry -> entry.getValue().size()
                ));
        }

        public int getTotalSources() { return totalSources; }
        public int getTotalCategories() { return totalCategories; }
        public double getAverageRelevanceScore() { return averageRelevanceScore; }
        public Map<String, Integer> getCategoryDistribution() {
            return new HashMap<>(categoryDistribution);
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

        public String getTitle() { return title; }
        public String getContent() { return content; }
        public double getConfidence() { return confidence; }
        public Instant getExtractedAt() { return extractedAt; }
    }

    public static class ResearchSummary {
        private final String query;
        private final int insightCount;
        private final int sourceCount;
        private final int wordCount;
        private final double qualityScore;
        private final Instant createdAt;

        public ResearchSummary(String query, int insightCount, int sourceCount,
            int wordCount, double qualityScore, Instant createdAt) {
            this.query = query;
            this.insightCount = insightCount;
            this.sourceCount = sourceCount;
            this.wordCount = wordCount;
            this.qualityScore = qualityScore;
            this.createdAt = createdAt;
        }

        public String getQuery() { return query; }
        public int getInsightCount() { return insightCount; }
        public int getSourceCount() { return sourceCount; }
        public int getWordCount() { return wordCount; }
        public double getQualityScore() { return qualityScore; }
        public Instant getCreatedAt() { return createdAt; }

        @Override
        public String toString() {
            return String.format("Research Summary: '%s' - %d sources, %d insights, %,d words (Quality: %.1f/10)",
                query, sourceCount, insightCount, wordCount, qualityScore * 10);
        }
    }

    public static class PerformanceAnalytics {
        private final PerformanceMetrics performanceMetrics;
        private final Object researchMetrics; // From ResearchResults.getMetrics()
        private final Map<String, Instant> phaseTimestamps;

        public PerformanceAnalytics(PerformanceMetrics performanceMetrics,
            Object researchMetrics,
            Map<String, Instant> phaseTimestamps) {
            this.performanceMetrics = performanceMetrics;
            this.researchMetrics = researchMetrics;
            this.phaseTimestamps = new HashMap<>(phaseTimestamps);
        }

        public PerformanceMetrics getPerformanceMetrics() { return performanceMetrics; }
        public Object getResearchMetrics() { return researchMetrics; }
        public Map<String, Instant> getPhaseTimestamps() { return new HashMap<>(phaseTimestamps); }

        public String generateReport() {
            StringBuilder report = new StringBuilder();
            report.append("# Performance Analytics Report\n\n");
            report.append(String.format("- Total Sources: %d\n", performanceMetrics.getTotalSources()));
            report.append(String.format("- Categories: %d\n", performanceMetrics.getTotalCategories()));
            report.append(String.format("- Average Relevance: %.2f\n", performanceMetrics.getAverageRelevanceScore()));

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

        public List<String> getIssues() { return new ArrayList<>(issues); }
        public List<String> getWarnings() { return new ArrayList<>(warnings); }
        public boolean isValid() { return isValid; }
        public boolean hasWarnings() { return !warnings.isEmpty(); }

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
                    report.append("- ").append(issue).append("\n");
                }
            }

            if (!warnings.isEmpty()) {
                report.append("\nWarnings:\n");
                for (String warning : warnings) {
                    report.append("- ").append(warning).append("\n");
                }
            }

            return report.toString();
        }
    }
}