package com.github.bhavuklabs.pipeline.langgraph;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.bsc.langgraph4j.state.AgentState;
import org.bsc.langgraph4j.state.Channel;
import org.bsc.langgraph4j.state.Channels;

import com.github.bhavuklabs.citation.CitationResult;
import com.github.bhavuklabs.core.enums.ReasoningMethod;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.pipeline.models.QueryAnalysis;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;

public class LangGraphState extends AgentState {

    public static final String SESSION_ID = "session_id";
    public static final String QUERY = "query";
    public static final String USER_PROFILE = "user_profile";
    public static final String QUERY_ANALYSIS = "query_analysis";
    public static final String CITATIONS = "citations";
    public static final String SELECTED_REASONING = "selected_reasoning";
    public static final String RESEARCH_CONTEXT = "research_context";
    public static final String RESPONSE = "response";
    public static final String ERROR = "error";
    public static final String METADATA = "metadata";

    public static final Map<String, Channel<?>> SCHEMA = Map.of(SESSION_ID, Channels.base(() -> "SESSION_ID"), QUERY, Channels.base(() -> "QUERY"),
        USER_PROFILE, Channels.base(() -> "USER_PROFILE"), QUERY_ANALYSIS, Channels.base(() -> "QUERY_ANALYSIS"), CITATIONS, Channels.appender(ArrayList::new),
        SELECTED_REASONING, Channels.base(() -> "SELECTED_REASONING"), RESEARCH_CONTEXT, Channels.base(() -> "RESEARCH_CONTEXT"), RESPONSE,
        Channels.base(() -> "RESPONSE"), ERROR, Channels.base(() -> "ERROR"), METADATA, Channels.base(() -> Map.of()));

    private ResearchAgentState legacyState;

    public LangGraphState(Map<String, Object> initData) {
        super(initData);
    }

    public static LangGraphState fromLegacyState(ResearchAgentState legacyState) {
        Map<String, Object> initData = Map.of(SESSION_ID, legacyState.getSessionId() != null ? legacyState.getSessionId() : "", QUERY,
            legacyState.getQuery() != null ? legacyState.getQuery() : "", USER_PROFILE, legacyState.getUserProfile(), RESEARCH_CONTEXT,
            legacyState.getResearchContext() != null ? legacyState.getResearchContext() : "", METADATA,
            legacyState.getMetadata() != null ? legacyState.getMetadata() : Map.of());

        LangGraphState state = new LangGraphState(initData);
        state.legacyState = legacyState;
        return state;
    }

    public ResearchAgentState toLegacyState() {
        if (legacyState != null) {

            return legacyState.withQueryAnalysis(getQueryAnalysis())
                .withCitations(getCitations())
                .withReasoning(getSelectedReasoning())
                .withResponse(getResponse());
        }

        return new ResearchAgentState(getSessionId(), getQuery(), (UserProfile) getUserProfile(), null).withQueryAnalysis(getQueryAnalysis())
            .withCitations(getCitations())
            .withReasoning(getSelectedReasoning())
            .withResponse(getResponse());
    }

    public String getSessionId() {
        return this.<String> value(SESSION_ID)
            .orElse("");
    }

    public String getQuery() {
        return this.<String> value(QUERY)
            .orElse("");
    }

    public Object getUserProfile() {
        return this.value(USER_PROFILE)
            .orElse(null);
    }

    public QueryAnalysis getQueryAnalysis() {
        return this.<QueryAnalysis> value(QUERY_ANALYSIS)
            .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public List<CitationResult> getCitations() {
        return this.<List<CitationResult>> value(CITATIONS)
            .orElse(new ArrayList<>());
    }

    public ReasoningMethod getSelectedReasoning() {
        return this.<ReasoningMethod> value(SELECTED_REASONING)
            .orElse(null);
    }

    public String getResearchContext() {
        return this.<String> value(RESEARCH_CONTEXT)
            .orElse("");
    }

    public LLMResponse<?> getResponse() {
        return (LLMResponse<?>) this.value(RESPONSE)
            .orElse(null);
    }

    public Exception getError() {
        return this.<Exception> value(ERROR)
            .orElse(null);
    }

    @SuppressWarnings("unchecked")
    public Map<String, Object> getMetadata() {
        return this.<Map<String, Object>> value(METADATA)
            .orElse(Map.of());
    }

    public LangGraphState withQueryAnalysis(QueryAnalysis analysis) {
        Map<String, Object> updates = Map.of(QUERY_ANALYSIS, analysis);
        return new LangGraphState(mergeState(updates));
    }

    public LangGraphState withCitations(List<CitationResult> citations) {
        Map<String, Object> updates = Map.of(CITATIONS, citations);
        return new LangGraphState(mergeState(updates));
    }

    public LangGraphState withSelectedReasoning(ReasoningMethod reasoning) {
        Map<String, Object> updates = Map.of(SELECTED_REASONING, reasoning);
        return new LangGraphState(mergeState(updates));
    }

    public LangGraphState withResponse(Object response) {
        Map<String, Object> updates = Map.of(RESPONSE, response);
        return new LangGraphState(mergeState(updates));
    }

    public LangGraphState withError(Exception error) {
        Map<String, Object> updates = Map.of(ERROR, error);
        return new LangGraphState(mergeState(updates));
    }

    private Map<String, Object> mergeState(Map<String, Object> updates) {
        Map<String, Object> newState = new java.util.HashMap<>(this.data());
        newState.putAll(updates);
        return newState;
    }
}