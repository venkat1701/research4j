package io.github.venkat1701.deepresearch.models;

public enum ResearchPhase {
    INITIAL_ANALYSIS("Initial Analysis & Question Generation"),
    MULTI_DIMENSIONAL_RESEARCH("Multi-dimensional Research"),
    DEEP_DIVE("Deep Dive Investigation"),
    CROSS_REFERENCE("Cross-reference Analysis"),
    SYNTHESIS("Knowledge Synthesis"),
    REPORT_GENERATION("Final Report Generation"),
    COMPLETED("Research Completed");

    private final String description;

    ResearchPhase(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}