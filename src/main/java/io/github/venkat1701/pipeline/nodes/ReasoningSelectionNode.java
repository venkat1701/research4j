package io.github.venkat1701.pipeline.nodes;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.pipeline.graph.GraphNode;
import io.github.venkat1701.pipeline.models.QueryAnalysis;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.pipeline.state.ResearchAgentState;

public class ReasoningSelectionNode implements GraphNode<ResearchAgentState> {
    private final LLMClient llmClient;

    public ReasoningSelectionNode(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public CompletableFuture<ResearchAgentState> process(ResearchAgentState state) {
        return CompletableFuture.supplyAsync(() -> {
            ReasoningMethod selectedMethod = selectOptimalReasoning(state);
            return state.withReasoning(selectedMethod);
        });
    }

    private ReasoningMethod selectOptimalReasoning(ResearchAgentState state) {
        QueryAnalysis analysis = (QueryAnalysis) state.getMetadata().get("query_analysis");
        UserProfile profile = state.getUserProfile();
        String query = state.getQuery().toLowerCase();

        Map<ReasoningMethod, Integer> scores = new HashMap<>();
        scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, 0);
        scores.put(ReasoningMethod.CHAIN_OF_IDEAS, 0);
        scores.put(ReasoningMethod.CHAIN_OF_TABLE, 0);

        if (analysis != null) {
            switch (analysis.intent) {
                case "comparison" -> scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 30);
                case "creative" -> scores.put(ReasoningMethod.CHAIN_OF_IDEAS, scores.get(ReasoningMethod.CHAIN_OF_IDEAS) + 30);
                case "analysis" -> scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, scores.get(ReasoningMethod.CHAIN_OF_THOUGHT) + 30);
            }
        }

        if (query.contains("compare") || query.contains("versus") || query.contains("difference")) {
            scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 20);
        }
        if (query.contains("creative") || query.contains("idea") || query.contains("brainstorm")) {
            scores.put(ReasoningMethod.CHAIN_OF_IDEAS, scores.get(ReasoningMethod.CHAIN_OF_IDEAS) + 20);
        }

        if (profile.hasPreference("detailed")) {
            scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, scores.get(ReasoningMethod.CHAIN_OF_THOUGHT) + 15);
        }
        if (profile.hasPreference("visual") || profile.getPreferredFormat() == OutputFormat.TABLE) {
            scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 15);
        }

        switch (profile.getDomain()) {
            case "business" -> scores.put(ReasoningMethod.CHAIN_OF_TABLE, scores.get(ReasoningMethod.CHAIN_OF_TABLE) + 10);
            case "academic" -> scores.put(ReasoningMethod.CHAIN_OF_THOUGHT, scores.get(ReasoningMethod.CHAIN_OF_THOUGHT) + 10);
            case "creative" -> scores.put(ReasoningMethod.CHAIN_OF_IDEAS, scores.get(ReasoningMethod.CHAIN_OF_IDEAS) + 10);
        }

        return scores.entrySet().stream()
            .max(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(ReasoningMethod.CHAIN_OF_THOUGHT);
    }

    @Override
    public String getName() { return "reasoning_selection"; }

    @Override
    public boolean shouldExecute(ResearchAgentState state) { return true; }
}

