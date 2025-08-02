package com.github.bhavuklabs.pipeline.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.enums.OutputFormat;
import com.github.bhavuklabs.core.enums.ReasoningMethod;
import com.github.bhavuklabs.pipeline.graph.GraphNode;
import com.github.bhavuklabs.pipeline.models.QueryAnalysis;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;

public class ReasoningSelectionNode implements GraphNode<ResearchAgentState> {

    private final LLMClient llmClient;

    public ReasoningSelectionNode(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public CompletableFuture<ResearchAgentState> process(ResearchAgentState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ReasoningMethod selectedMethod = selectOptimalReasoning(state);
                return state.withReasoning(selectedMethod);
            } catch (Exception e) {
                return state.withReasoning(ReasoningMethod.CHAIN_OF_THOUGHT);
            }
        });
    }

    private ReasoningMethod selectOptimalReasoning(ResearchAgentState state) {
        QueryAnalysis analysis = (QueryAnalysis) state.getMetadata()
            .get("query_analysis");
        UserProfile profile = state.getUserProfile();
        String query = state.getQuery() != null ? state.getQuery()
            .toLowerCase() : "";

        Map<ReasoningMethod, Integer> scores = new HashMap<>();
        scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, 10);
        scores.put(ReasoningMethod.CHAIN_OF_IDEAS, 10);
        scores.put(ReasoningMethod.CHAIN_OF_TABLE, 10);

        if (analysis != null && analysis.intent != null) {
            switch (analysis.intent) {
                case "comparison" -> scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 30);
                case "creative" -> scores.put(ReasoningMethod.CHAIN_OF_IDEAS, scores.get(ReasoningMethod.CHAIN_OF_IDEAS) + 30);
                case "analysis", "research" -> scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, scores.get(ReasoningMethod.CHAIN_OF_THOUGHT) + 30);
            }
        }

        if (query.contains("compare") || query.contains("versus") || query.contains("difference")) {
            scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 20);
        }
        if (query.contains("creative") || query.contains("idea") || query.contains("brainstorm")) {
            scores.put(ReasoningMethod.CHAIN_OF_IDEAS, scores.get(ReasoningMethod.CHAIN_OF_IDEAS) + 20);
        }
        if (query.contains("analyze") || query.contains("explain") || query.contains("why")) {
            scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, scores.get(ReasoningMethod.CHAIN_OF_THOUGHT) + 20);
        }

        if (profile != null) {
            if (profile.getPreferences() != null && profile.hasPreference("detailed")) {
                scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, scores.get(ReasoningMethod.CHAIN_OF_THOUGHT) + 15);
            }
            if ((profile.getPreferences() != null && profile.hasPreference("visual")) || profile.getPreferredFormat() == OutputFormat.TABLE) {
                scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 15);
            }
            if (profile.getDomain() != null) {
                switch (profile.getDomain()) {
                    case "business" -> scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 10);
                    case "academic" -> scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, scores.get(ReasoningMethod.CHAIN_OF_THOUGHT) + 10);
                    case "creative" -> scores.put(ReasoningMethod.CHAIN_OF_IDEAS, scores.get(ReasoningMethod.CHAIN_OF_IDEAS) + 10);
                }
            }
        }

        return scores.entrySet()
            .stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(ReasoningMethod.CHAIN_OF_THOUGHT);
    }

    @Override
    public String getName() {
        return "reasoning_selection";
    }

    @Override
    public boolean shouldExecute(ResearchAgentState state) {
        return state != null && !state.isComplete();
    }
}