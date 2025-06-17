package io.github.venkat1701.pipeline.state;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.core.payloads.ResearchPromptConfig;
import io.github.venkat1701.pipeline.models.QueryAnalysis;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.reasoning.context.ResearchContext;

public class ResearchAgentState {
    private final String sessionId;
    private final String query;
    private final UserProfile userProfile;
    private final ResearchPromptConfig config;

    private List<CitationResult> citations = new ArrayList<>();
    private List<String> processedChunks = new ArrayList<>();
    private ResearchContext researchContext;
    private ReasoningMethod selectedReasoning;
    private LLMResponse<?> finalResponse;

    private final Map<String, Object> metadata = new HashMap<>();
    private final List<String> processingSteps = new ArrayList<>();
    private Instant startTime = Instant.now();
    private Instant endTime;
    private boolean isComplete = false;
    private Exception error;

    public ResearchAgentState(String sessionId, String query, UserProfile userProfile, ResearchPromptConfig config) {
        this.sessionId = sessionId;
        this.query = query;
        this.userProfile = userProfile;
        this.config = config;
        this.researchContext = new ResearchContext(config);
    }
    public ResearchAgentState withCitations(List<CitationResult> citations) {
        ResearchAgentState newState = this.copy();
        newState.citations = new ArrayList<>(citations);
        newState.researchContext.setCitations(citations);
        newState.addProcessingStep("citations_fetched", citations.size());
        return newState;
    }

    public ResearchAgentState withReasoning(ReasoningMethod reasoning) {
        ResearchAgentState newState = this.copy();
        newState.selectedReasoning = reasoning;
        newState.researchContext.setReasoningMethod(reasoning);
        newState.addProcessingStep("reasoning_selected", reasoning.name());
        return newState;
    }

    public ResearchAgentState withQueryAnalysis(QueryAnalysis analysis) {
        ResearchAgentState newState = this.copy();
        newState.metadata.put("query_analysis", analysis);
        newState.metadata.put("complexity_score", analysis.complexityScore);
        newState.metadata.put("detected_intent", analysis.intent);
        newState.addProcessingStep("query_analysis", analysis.intent);
        return newState;
    }


    public ResearchAgentState withResponse(LLMResponse<?> response) {
        ResearchAgentState newState = this.copy();
        newState.finalResponse = response;
        newState.isComplete = true;
        newState.endTime = Instant.now();
        newState.addProcessingStep("response_generated", "complete");
        return newState;
    }

    public ResearchAgentState withError(Exception error) {
        ResearchAgentState newState = this.copy();
        newState.error = error;
        newState.endTime = Instant.now();
        newState.addProcessingStep("error_occurred", error.getMessage());
        return newState;
    }

    public ResearchAgentState copy() {
        ResearchAgentState copy = new ResearchAgentState(sessionId, query, userProfile, config);
        copy.citations = new ArrayList<>(this.citations);
        copy.processedChunks = new ArrayList<>(this.processedChunks);
        copy.researchContext = this.researchContext;
        copy.selectedReasoning = this.selectedReasoning;
        copy.finalResponse = this.finalResponse;
        copy.metadata.putAll(this.metadata);
        copy.processingSteps.addAll(this.processingSteps);
        copy.startTime = this.startTime;
        copy.endTime = this.endTime;
        copy.isComplete = this.isComplete;
        copy.error = this.error;
        return copy;
    }

    private void addProcessingStep(String step, Object value) {
        processingSteps.add(String.format("%s: %s at %s", step, value, Instant.now()));
        metadata.put(step, value);
    }

    public String getSessionId() { return sessionId; }
    public String getQuery() { return query; }
    public UserProfile getUserProfile() { return userProfile; }
    public ResearchPromptConfig getConfig() { return config; }
    public List<CitationResult> getCitations() { return citations; }
    public ResearchContext getResearchContext() { return researchContext; }
    public ReasoningMethod getSelectedReasoning() { return selectedReasoning; }
    public LLMResponse<?> getFinalResponse() { return finalResponse; }
    public Map<String, Object> getMetadata() { return metadata; }
    public List<String> getProcessingSteps() { return processingSteps; }
    public boolean isComplete() { return isComplete; }
    public Exception getError() { return error; }
    public Duration getProcessingTime() {
        return Duration.between(startTime, endTime != null ? endTime : Instant.now());
    }
}
