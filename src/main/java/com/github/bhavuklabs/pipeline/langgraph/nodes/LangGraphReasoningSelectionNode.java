package com.github.bhavuklabs.pipeline.langgraph.nodes;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bsc.langgraph4j.action.AsyncNodeAction;

import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.enums.ReasoningMethod;
import com.github.bhavuklabs.pipeline.langgraph.LangGraphState;
import com.github.bhavuklabs.pipeline.models.QueryAnalysis;

public class LangGraphReasoningSelectionNode implements AsyncNodeAction<LangGraphState> {

    private static final Logger logger = Logger.getLogger(LangGraphReasoningSelectionNode.class.getName());

    private final LLMClient llmClient;

    public LangGraphReasoningSelectionNode(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(LangGraphState state) {
        try {
            logger.info("Starting LangGraph4j reasoning selection for session: " + state.getSessionId());

            ReasoningMethod selectedMethod = selectOptimalReasoning(state);

            logger.info("Selected reasoning method: " + selectedMethod);

            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.SELECTED_REASONING, selectedMethod));

        } catch (Exception e) {
            logger.warning("Reasoning selection failed, using default: " + e.getMessage());

            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.SELECTED_REASONING, ReasoningMethod.CHAIN_OF_THOUGHT));
        }
    }

    private ReasoningMethod selectOptimalReasoning(LangGraphState state) {
        QueryAnalysis analysis = state.getQueryAnalysis();
        String query = state.getQuery()
            .toLowerCase();

        if (analysis != null && analysis.suggestedReasoning != null) {
            try {
                return ReasoningMethod.valueOf(analysis.suggestedReasoning);
            } catch (IllegalArgumentException e) {
                logger.warning("Invalid reasoning method in analysis: " + analysis.suggestedReasoning);
            }
        }

        if (query.contains("compare") || query.contains("table") || query.contains("vs")) {
            return ReasoningMethod.CHAIN_OF_TABLE;
        }

        if (query.contains("creative") || query.contains("brainstorm") || query.contains("ideas")) {
            return ReasoningMethod.CHAIN_OF_IDEAS;
        }

        if (analysis != null && analysis.complexityScore >= 8) {
            return ReasoningMethod.TREE_OF_THOUGHT;
        }

        return ReasoningMethod.CHAIN_OF_THOUGHT;
    }
}