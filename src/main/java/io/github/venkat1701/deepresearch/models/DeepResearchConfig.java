package io.github.venkat1701.deepresearch.models;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.venkat1701.core.enums.OutputFormat;


public class DeepResearchConfig {

    public static DeepResearchConfig.Builder builder() {
        return new DeepResearchConfig.Builder();
    }

    public enum ResearchDepth {
        BASIC(1, "Basic research with minimal sources"),
        STANDARD(2, "Standard research with moderate depth"),
        COMPREHENSIVE(3, "Comprehensive research with extensive sources"),
        EXPERT(4, "Expert-level research with maximum depth and analysis");

        private final int level;
        private final String description;

        ResearchDepth(int level, String description) {
            this.level = level;
            this.description = description;
        }

        public int getLevel() {
            return level;
        }

        public String getDescription() {
            return description;
        }
    }

    public enum NarrativeStyle {
        ACADEMIC("Academic style with formal tone and citations"),
        BUSINESS("Business-oriented with executive summaries and actionable insights"),
        TECHNICAL("Technical documentation style with detailed implementations"),
        GENERAL("General audience with accessible language and explanations");

        private final String description;

        NarrativeStyle(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    private final ResearchDepth researchDepth;
    private final NarrativeStyle narrativeStyle;
    private final int maxSources;
    private final int maxQuestions;
    private final int maxRounds;
    private final Duration maxProcessingTime;

    private final double minRelevanceScore;
    private final int minWordCount;
    private final int maxWordCount;
    private final boolean enableQualityFiltering;
    private final boolean enableCrossValidation;

    private final boolean enableParallelProcessing;
    private final int parallelThreads;
    private final boolean enableDeepDiveMode;
    private final boolean enableIterativeRefinement;
    private final boolean enableSemanticClustering;

    private final boolean includeExecutiveSummary;
    private final boolean includeMethodology;
    private final boolean includeBibliography;
    private final boolean includeMetrics;
    private final OutputFormat outputFormat;

    private final Set<String> preferredDomains;
    private final Set<String> excludedDomains;
    private final List<String> additionalKeywords;
    private final boolean enableDomainDiversification;

    private final Duration searchRateLimit;
    private final Duration deepSearchRateLimit;
    private final int maxConcurrentSearches;

    private DeepResearchConfig(Builder builder) {
        this.researchDepth = builder.researchDepth;
        this.narrativeStyle = builder.narrativeStyle;
        this.maxSources = builder.maxSources;
        this.maxQuestions = builder.maxQuestions;
        this.maxRounds = builder.maxRounds;
        this.maxProcessingTime = builder.maxProcessingTime;

        this.minRelevanceScore = builder.minRelevanceScore;
        this.minWordCount = builder.minWordCount;
        this.maxWordCount = builder.maxWordCount;
        this.enableQualityFiltering = builder.enableQualityFiltering;
        this.enableCrossValidation = builder.enableCrossValidation;

        this.enableParallelProcessing = builder.enableParallelProcessing;
        this.parallelThreads = builder.parallelThreads;
        this.enableDeepDiveMode = builder.enableDeepDiveMode;
        this.enableIterativeRefinement = builder.enableIterativeRefinement;
        this.enableSemanticClustering = builder.enableSemanticClustering;

        this.includeExecutiveSummary = builder.includeExecutiveSummary;
        this.includeMethodology = builder.includeMethodology;
        this.includeBibliography = builder.includeBibliography;
        this.includeMetrics = builder.includeMetrics;
        this.outputFormat = builder.outputFormat;

        this.preferredDomains = new HashSet<>(builder.preferredDomains);
        this.excludedDomains = new HashSet<>(builder.excludedDomains);
        this.additionalKeywords = new ArrayList<>(builder.additionalKeywords);
        this.enableDomainDiversification = builder.enableDomainDiversification;

        this.searchRateLimit = builder.searchRateLimit;
        this.deepSearchRateLimit = builder.deepSearchRateLimit;
        this.maxConcurrentSearches = builder.maxConcurrentSearches;
    }

    public static DeepResearchConfig basicConfig() {
        return new Builder().researchDepth(ResearchDepth.BASIC)
            .narrativeStyle(NarrativeStyle.GENERAL)
            .maxSources(15)
            .maxQuestions(8)
            .maxRounds(2)
            .maxProcessingTime(Duration.ofMinutes(15))
            .enableQualityFiltering(true)
            .build();
    }

    public static DeepResearchConfig standardConfig() {
        return new Builder().researchDepth(ResearchDepth.STANDARD)
            .narrativeStyle(NarrativeStyle.BUSINESS)
            .maxSources(30)
            .maxQuestions(12)
            .maxRounds(3)
            .maxProcessingTime(Duration.ofMinutes(30))
            .enableQualityFiltering(true)
            .enableParallelProcessing(true)
            .enableIterativeRefinement(true)
            .build();
    }

    public static DeepResearchConfig comprehensiveConfig() {
        return new Builder().researchDepth(ResearchDepth.COMPREHENSIVE)
            .narrativeStyle(NarrativeStyle.ACADEMIC)
            .maxSources(50)
            .maxQuestions(18)
            .maxRounds(3)
            .maxProcessingTime(Duration.ofMinutes(45))
            .enableQualityFiltering(true)
            .enableParallelProcessing(true)
            .enableDeepDiveMode(true)
            .enableIterativeRefinement(true)
            .enableSemanticClustering(true)
            .enableCrossValidation(true)
            .build();
    }

    public static DeepResearchConfig expertConfig() {
        return new Builder().researchDepth(ResearchDepth.EXPERT)
            .narrativeStyle(NarrativeStyle.TECHNICAL)
            .maxSources(75)
            .maxQuestions(24)
            .maxRounds(4)
            .maxProcessingTime(Duration.ofMinutes(60))
            .minWordCount(8000)
            .maxWordCount(15000)
            .enableQualityFiltering(true)
            .enableParallelProcessing(true)
            .enableDeepDiveMode(true)
            .enableIterativeRefinement(true)
            .enableSemanticClustering(true)
            .enableCrossValidation(true)
            .enableDomainDiversification(true)
            .parallelThreads(8)
            .build();
    }

    public static DeepResearchConfig createDefault() {
        return standardConfig();
    }

    public ConfigValidationResult validate() {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        if (maxSources < 1) {
            errors.add("Maximum sources must be at least 1");
        }
        if (maxQuestions < 1) {
            errors.add("Maximum questions must be at least 1");
        }
        if (maxRounds < 1) {
            errors.add("Maximum rounds must be at least 1");
        }
        if (minRelevanceScore < 0.0 || minRelevanceScore > 1.0) {
            errors.add("Minimum relevance score must be between 0.0 and 1.0");
        }

        if (minWordCount > maxWordCount) {
            errors.add("Minimum word count cannot exceed maximum word count");
        }
        if (minWordCount < 0) {
            errors.add("Minimum word count cannot be negative");
        }

        if (enableParallelProcessing && parallelThreads < 1) {
            errors.add("Parallel threads must be at least 1 when parallel processing is enabled");
        }
        if (parallelThreads > 16) {
            warnings.add("High thread count (" + parallelThreads + ") may impact performance");
        }

        if (maxProcessingTime.isNegative() || maxProcessingTime.isZero()) {
            errors.add("Maximum processing time must be positive");
        }
        if (maxProcessingTime.toMinutes() > 120) {
            warnings.add("Very long processing time (" + maxProcessingTime.toMinutes() + " minutes)");
        }

        if (searchRateLimit.isNegative()) {
            errors.add("Search rate limit cannot be negative");
        }
        if (deepSearchRateLimit.isNegative()) {
            errors.add("Deep search rate limit cannot be negative");
        }

        if (researchDepth == ResearchDepth.EXPERT && maxSources > 100) {
            warnings.add("Expert mode with very high source count may be slow");
        }

        return new ConfigValidationResult(errors, warnings, errors.isEmpty());
    }

    public Duration getEstimatedProcessingTime() {
        int baseMinutes = researchDepth.getLevel() * 5;

        baseMinutes += (maxSources / 10) * 2;

        baseMinutes += (maxQuestions / 5) * 3;

        baseMinutes += (maxRounds - 1) * 5;

        if (enableDeepDiveMode) {
            baseMinutes += 10;
        }
        if (enableCrossValidation) {
            baseMinutes += 5;
        }
        if (enableSemanticClustering) {
            baseMinutes += 3;
        }

        if (enableParallelProcessing) {
            baseMinutes = (int) (baseMinutes * 0.7);
        }

        return Duration.ofMinutes(Math.min(baseMinutes, maxProcessingTime.toMinutes()));
    }

    public int getEstimatedMemoryRequirementMB() {
        int baseMB = 100;

        baseMB += maxSources * 2;
        baseMB += maxQuestions * 1;

        if (enableParallelProcessing) {
            baseMB += parallelThreads * 50;
        }

        if (enableSemanticClustering) {
            baseMB += 200;
        }

        return baseMB;
    }

    public DeepResearchConfig withModifications(ConfigModifier modifier) {
        Builder builder = toBuilder();
        modifier.modify(builder);
        return builder.build();
    }

    public Builder toBuilder() {
        return new Builder().researchDepth(researchDepth)
            .narrativeStyle(narrativeStyle)
            .maxSources(maxSources)
            .maxQuestions(maxQuestions)
            .maxRounds(maxRounds)
            .maxProcessingTime(maxProcessingTime)
            .minRelevanceScore(minRelevanceScore)
            .minWordCount(minWordCount)
            .maxWordCount(maxWordCount)
            .enableQualityFiltering(enableQualityFiltering)
            .enableCrossValidation(enableCrossValidation)
            .enableParallelProcessing(enableParallelProcessing)
            .parallelThreads(parallelThreads)
            .enableDeepDiveMode(enableDeepDiveMode)
            .enableIterativeRefinement(enableIterativeRefinement)
            .enableSemanticClustering(enableSemanticClustering)
            .includeExecutiveSummary(includeExecutiveSummary)
            .includeMethodology(includeMethodology)
            .includeBibliography(includeBibliography)
            .includeMetrics(includeMetrics)
            .outputFormat(outputFormat)
            .preferredDomains(preferredDomains)
            .excludedDomains(excludedDomains)
            .additionalKeywords(additionalKeywords)
            .enableDomainDiversification(enableDomainDiversification)
            .searchRateLimit(searchRateLimit)
            .deepSearchRateLimit(deepSearchRateLimit)
            .maxConcurrentSearches(maxConcurrentSearches);
    }

    public Map<String, Object> exportAsMap() {
        Map<String, Object> export = new HashMap<>();

        export.put("researchDepth", researchDepth.name());
        export.put("narrativeStyle", narrativeStyle.name());
        export.put("maxSources", maxSources);
        export.put("maxQuestions", maxQuestions);
        export.put("maxRounds", maxRounds);
        export.put("maxProcessingTimeMinutes", maxProcessingTime.toMinutes());

        export.put("minRelevanceScore", minRelevanceScore);
        export.put("minWordCount", minWordCount);
        export.put("maxWordCount", maxWordCount);
        export.put("enableQualityFiltering", enableQualityFiltering);
        export.put("enableCrossValidation", enableCrossValidation);

        export.put("enableParallelProcessing", enableParallelProcessing);
        export.put("parallelThreads", parallelThreads);
        export.put("enableDeepDiveMode", enableDeepDiveMode);
        export.put("enableIterativeRefinement", enableIterativeRefinement);
        export.put("enableSemanticClustering", enableSemanticClustering);

        export.put("includeExecutiveSummary", includeExecutiveSummary);
        export.put("includeMethodology", includeMethodology);
        export.put("includeBibliography", includeBibliography);
        export.put("includeMetrics", includeMetrics);
        export.put("outputFormat", outputFormat);

        export.put("preferredDomains", new ArrayList<>(preferredDomains));
        export.put("excludedDomains", new ArrayList<>(excludedDomains));
        export.put("additionalKeywords", new ArrayList<>(additionalKeywords));
        export.put("enableDomainDiversification", enableDomainDiversification);

        export.put("searchRateLimitMs", searchRateLimit.toMillis());
        export.put("deepSearchRateLimitMs", deepSearchRateLimit.toMillis());
        export.put("maxConcurrentSearches", maxConcurrentSearches);

        return export;
    }

    public ResearchDepth getResearchDepth() {
        return researchDepth;
    }

    public NarrativeStyle getNarrativeStyle() {
        return narrativeStyle;
    }

    public int getMaxSources() {
        return maxSources;
    }

    public int getMaxQuestions() {
        return maxQuestions;
    }

    public int getMaxRounds() {
        return maxRounds;
    }

    public Duration getMaxProcessingTime() {
        return maxProcessingTime;
    }

    public double getMinRelevanceScore() {
        return minRelevanceScore;
    }

    public int getMinWordCount() {
        return minWordCount;
    }

    public int getMaxWordCount() {
        return maxWordCount;
    }

    public boolean isEnableQualityFiltering() {
        return enableQualityFiltering;
    }

    public boolean isEnableCrossValidation() {
        return enableCrossValidation;
    }

    public boolean isEnableParallelProcessing() {
        return enableParallelProcessing;
    }

    public int getParallelThreads() {
        return parallelThreads;
    }

    public boolean isEnableDeepDiveMode() {
        return enableDeepDiveMode;
    }

    public boolean isEnableIterativeRefinement() {
        return enableIterativeRefinement;
    }

    public boolean isEnableSemanticClustering() {
        return enableSemanticClustering;
    }

    public boolean isIncludeExecutiveSummary() {
        return includeExecutiveSummary;
    }

    public boolean isIncludeMethodology() {
        return includeMethodology;
    }

    public boolean isIncludeBibliography() {
        return includeBibliography;
    }

    public boolean isIncludeMetrics() {
        return includeMetrics;
    }

    public OutputFormat getOutputFormat() {
        return outputFormat;
    }

    public Set<String> getPreferredDomains() {
        return new HashSet<>(preferredDomains);
    }

    public Set<String> getExcludedDomains() {
        return new HashSet<>(excludedDomains);
    }

    public List<String> getAdditionalKeywords() {
        return new ArrayList<>(additionalKeywords);
    }

    public boolean isEnableDomainDiversification() {
        return enableDomainDiversification;
    }

    public Duration getSearchRateLimit() {
        return searchRateLimit;
    }

    public Duration getDeepSearchRateLimit() {
        return deepSearchRateLimit;
    }

    public int getMaxConcurrentSearches() {
        return maxConcurrentSearches;
    }

    public static class Builder {

        private ResearchDepth researchDepth = ResearchDepth.STANDARD;
        private NarrativeStyle narrativeStyle = NarrativeStyle.GENERAL;
        private int maxSources = 30;
        private int maxQuestions = 12;
        private int maxRounds = 3;
        private Duration maxProcessingTime = Duration.ofMinutes(30);

        private double minRelevanceScore = 0.3;
        private int minWordCount = 2000;
        private int maxWordCount = 10000;
        private boolean enableQualityFiltering = true;
        private boolean enableCrossValidation = false;

        private boolean enableParallelProcessing = true;
        private int parallelThreads = 4;
        private boolean enableDeepDiveMode = false;
        private boolean enableIterativeRefinement = true;
        private boolean enableSemanticClustering = false;

        private boolean includeExecutiveSummary = true;
        private boolean includeMethodology = true;
        private boolean includeBibliography = true;
        private boolean includeMetrics = true;
        private OutputFormat outputFormat = OutputFormat.MARKDOWN;

        private Set<String> preferredDomains = new HashSet<>();
        private Set<String> excludedDomains = new HashSet<>();
        private List<String> additionalKeywords = new ArrayList<>();
        private boolean enableDomainDiversification = false;

        private Duration searchRateLimit = Duration.ofMillis(500);
        private Duration deepSearchRateLimit = Duration.ofMillis(1000);
        private int maxConcurrentSearches = 8;

        public Builder researchDepth(ResearchDepth researchDepth) {
            this.researchDepth = researchDepth;
            return this;
        }

        public Builder narrativeStyle(NarrativeStyle narrativeStyle) {
            this.narrativeStyle = narrativeStyle;
            return this;
        }

        public Builder maxSources(int maxSources) {
            this.maxSources = maxSources;
            return this;
        }

        public Builder maxQuestions(int maxQuestions) {
            this.maxQuestions = maxQuestions;
            return this;
        }

        public Builder maxRounds(int maxRounds) {
            this.maxRounds = maxRounds;
            return this;
        }

        public Builder maxProcessingTime(Duration maxProcessingTime) {
            this.maxProcessingTime = maxProcessingTime;
            return this;
        }

        public Builder minRelevanceScore(double minRelevanceScore) {
            this.minRelevanceScore = minRelevanceScore;
            return this;
        }

        public Builder minWordCount(int minWordCount) {
            this.minWordCount = minWordCount;
            return this;
        }

        public Builder maxWordCount(int maxWordCount) {
            this.maxWordCount = maxWordCount;
            return this;
        }

        public Builder enableQualityFiltering(boolean enableQualityFiltering) {
            this.enableQualityFiltering = enableQualityFiltering;
            return this;
        }

        public Builder enableCrossValidation(boolean enableCrossValidation) {
            this.enableCrossValidation = enableCrossValidation;
            return this;
        }

        public Builder enableParallelProcessing(boolean enableParallelProcessing) {
            this.enableParallelProcessing = enableParallelProcessing;
            return this;
        }

        public Builder parallelThreads(int parallelThreads) {
            this.parallelThreads = parallelThreads;
            return this;
        }

        public Builder enableDeepDiveMode(boolean enableDeepDiveMode) {
            this.enableDeepDiveMode = enableDeepDiveMode;
            return this;
        }

        public Builder enableIterativeRefinement(boolean enableIterativeRefinement) {
            this.enableIterativeRefinement = enableIterativeRefinement;
            return this;
        }

        public Builder enableSemanticClustering(boolean enableSemanticClustering) {
            this.enableSemanticClustering = enableSemanticClustering;
            return this;
        }

        public Builder includeExecutiveSummary(boolean includeExecutiveSummary) {
            this.includeExecutiveSummary = includeExecutiveSummary;
            return this;
        }

        public Builder includeMethodology(boolean includeMethodology) {
            this.includeMethodology = includeMethodology;
            return this;
        }

        public Builder includeBibliography(boolean includeBibliography) {
            this.includeBibliography = includeBibliography;
            return this;
        }

        public Builder includeMetrics(boolean includeMetrics) {
            this.includeMetrics = includeMetrics;
            return this;
        }

        public Builder outputFormat(OutputFormat outputFormat) {
            this.outputFormat = outputFormat;
            return this;
        }

        public Builder preferredDomains(Collection<String> preferredDomains) {
            this.preferredDomains = new HashSet<>(preferredDomains);
            return this;
        }

        public Builder excludedDomains(Collection<String> excludedDomains) {
            this.excludedDomains = new HashSet<>(excludedDomains);
            return this;
        }

        public Builder additionalKeywords(Collection<String> additionalKeywords) {
            this.additionalKeywords = new ArrayList<>(additionalKeywords);
            return this;
        }

        public Builder enableDomainDiversification(boolean enableDomainDiversification) {
            this.enableDomainDiversification = enableDomainDiversification;
            return this;
        }

        public Builder searchRateLimit(Duration searchRateLimit) {
            this.searchRateLimit = searchRateLimit;
            return this;
        }

        public Builder deepSearchRateLimit(Duration deepSearchRateLimit) {
            this.deepSearchRateLimit = deepSearchRateLimit;
            return this;
        }

        public Builder maxConcurrentSearches(int maxConcurrentSearches) {
            this.maxConcurrentSearches = maxConcurrentSearches;
            return this;
        }

        public DeepResearchConfig build() {
            return new DeepResearchConfig(this);
        }
    }

    public static class ConfigValidationResult {

        private final List<String> errors;
        private final List<String> warnings;
        private final boolean isValid;

        public ConfigValidationResult(List<String> errors, List<String> warnings, boolean isValid) {
            this.errors = new ArrayList<>(errors);
            this.warnings = new ArrayList<>(warnings);
            this.isValid = isValid;
        }

        public List<String> getErrors() {
            return new ArrayList<>(errors);
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
                report.append("✅ Configuration is valid\n");
            } else {
                report.append("❌ Configuration has errors\n");
            }

            if (!errors.isEmpty()) {
                report.append("\nErrors:\n");
                for (String error : errors) {
                    report.append("- ")
                        .append(error)
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

    @FunctionalInterface
    public interface ConfigModifier {

        void modify(Builder builder);
    }

    @Override
    public String toString() {
        return String.format("DeepResearchConfig{depth=%s, style=%s, maxSources=%d, maxQuestions=%d, maxRounds=%d}", researchDepth, narrativeStyle, maxSources,
            maxQuestions, maxRounds);
    }
}