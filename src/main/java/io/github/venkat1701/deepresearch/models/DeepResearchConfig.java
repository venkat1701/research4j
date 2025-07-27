package io.github.venkat1701.deepresearch.models;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.github.venkat1701.core.enums.OutputFormat;

public class DeepResearchConfig {

    public enum ResearchDepth {
        STANDARD,      
        COMPREHENSIVE, 
        EXHAUSTIVE     
    }

    public enum ResearchScope {
        FOCUSED,       
        BROAD,         
        INTERDISCIPLINARY 
    }

    private final ResearchDepth researchDepth;
    private final ResearchScope researchScope;
    private final Duration maxDuration;
    private final int maxSources;
    private final int maxQuestions;
    private final OutputFormat outputFormat;
    private final List<String> focusAreas;
    private final Map<String, Object> customParameters;
    private final boolean enableMemoryPersistence;
    private final boolean enableCrossValidation;
    private final boolean enableRealTimeUpdates;

    private DeepResearchConfig(Builder builder) {
        this.researchDepth = builder.researchDepth;
        this.researchScope = builder.researchScope;
        this.maxDuration = builder.maxDuration;
        this.maxSources = builder.maxSources;
        this.maxQuestions = builder.maxQuestions;
        this.outputFormat = builder.outputFormat;
        this.focusAreas = builder.focusAreas;
        this.customParameters = builder.customParameters;
        this.enableMemoryPersistence = builder.enableMemoryPersistence;
        this.enableCrossValidation = builder.enableCrossValidation;
        this.enableRealTimeUpdates = builder.enableRealTimeUpdates;
    }

    
    public ResearchDepth getResearchDepth() { return researchDepth; }
    public ResearchScope getResearchScope() { return researchScope; }
    public Duration getMaxDuration() { return maxDuration; }
    public int getMaxSources() { return maxSources; }
    public int getMaxQuestions() { return maxQuestions; }
    public OutputFormat getOutputFormat() { return outputFormat; }
    public List<String> getFocusAreas() { return focusAreas; }
    public Map<String, Object> getCustomParameters() { return customParameters; }
    public boolean isMemoryPersistenceEnabled() { return enableMemoryPersistence; }
    public boolean isCrossValidationEnabled() { return enableCrossValidation; }
    public boolean isRealTimeUpdatesEnabled() { return enableRealTimeUpdates; }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {
        private ResearchDepth researchDepth = ResearchDepth.COMPREHENSIVE;
        private ResearchScope researchScope = ResearchScope.BROAD;
        private Duration maxDuration = Duration.ofMinutes(15);
        private int maxSources = 50;
        private int maxQuestions = 12;
        private OutputFormat outputFormat = OutputFormat.MARKDOWN;
        private List<String> focusAreas = List.of();
        private Map<String, Object> customParameters = Map.of();
        private boolean enableMemoryPersistence = true;
        private boolean enableCrossValidation = true;
        private boolean enableRealTimeUpdates = true;

        public Builder researchDepth(ResearchDepth depth) {
            this.researchDepth = depth;
            return this;
        }

        public Builder researchScope(ResearchScope scope) {
            this.researchScope = scope;
            return this;
        }

        public Builder maxDuration(Duration duration) {
            this.maxDuration = duration;
            return this;
        }

        public Builder maxSources(int sources) {
            this.maxSources = sources;
            return this;
        }

        public Builder maxQuestions(int questions) {
            this.maxQuestions = questions;
            return this;
        }

        public Builder outputFormat(OutputFormat format) {
            this.outputFormat = format;
            return this;
        }

        public Builder focusAreas(List<String> areas) {
            this.focusAreas = areas;
            return this;
        }

        public Builder customParameters(Map<String, Object> params) {
            this.customParameters = params;
            return this;
        }

        public Builder enableMemoryPersistence(boolean enable) {
            this.enableMemoryPersistence = enable;
            return this;
        }

        public Builder enableCrossValidation(boolean enable) {
            this.enableCrossValidation = enable;
            return this;
        }

        public Builder enableRealTimeUpdates(boolean enable) {
            this.enableRealTimeUpdates = enable;
            return this;
        }

        public DeepResearchConfig build() {
            return new DeepResearchConfig(this);
        }
    }

    
    public static DeepResearchConfig standardConfig() {
        return builder()
            .researchDepth(ResearchDepth.STANDARD)
            .maxDuration(Duration.ofMinutes(5))
            .maxSources(25)
            .maxQuestions(8)
            .build();
    }

    public static DeepResearchConfig comprehensiveConfig() {
        return builder()
            .researchDepth(ResearchDepth.COMPREHENSIVE)
            .maxDuration(Duration.ofMinutes(15))
            .maxSources(50)
            .maxQuestions(15)
            .build();
    }

    public static DeepResearchConfig exhaustiveConfig() {
        return builder()
            .researchDepth(ResearchDepth.EXHAUSTIVE)
            .researchScope(ResearchScope.INTERDISCIPLINARY)
            .maxDuration(Duration.ofMinutes(30))
            .maxSources(100)
            .maxQuestions(25)
            .build();
    }
}