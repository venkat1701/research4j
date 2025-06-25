package io.github.venkat1701.agent;

import java.time.Duration;
import java.util.List;
import java.util.Map;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.config.Research4jConfig;

public class ResearchResult {

    private final io.github.venkat1701.pipeline.state.ResearchAgentState state;
    private final Research4jConfig config;

    public ResearchResult(io.github.venkat1701.pipeline.state.ResearchAgentState state, Research4jConfig config) {
        this.state = state;
        this.config = config;
    }

    public String getAnswer() {
        return state.getFinalResponse() != null ? state.getFinalResponse()
            .structuredOutput()
            .toString() : "No answer generated";
    }

    public String getRawResponse() {
        return state.getFinalResponse() != null ? state.getFinalResponse()
            .rawText() : "No response generated";
    }

    public List<CitationResult> getCitations() {
        return state.getCitations();
    }

    public Map<String, Object> getMetadata() {
        return state.getMetadata();
    }

    public Duration getProcessingTime() {
        return state.getProcessingTime();
    }

    public boolean hasError() {
        return state.getError() != null;
    }

    public Exception getError() {
        return state.getError();
    }

    public boolean isComplete() {
        return state.isComplete();
    }

    public String getSessionId() {
        return state.getSessionId();
    }

    public String getQuery() {
        return state.getQuery();
    }

    @Override
    public String toString() {
        if (hasError()) {
            return "ResearchResult{error=" + getError().getMessage() + "}";
        }
        return "ResearchResult{" + "answer='" + getAnswer() + "', " + "citations=" + getCitations().size() + ", " + "processingTime=" + getProcessingTime() +
            "}";
    }
}
