package com.github.bhavuklabs.pipeline.langgraph.adapters;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bsc.langgraph4j.action.NodeAction;

import com.github.bhavuklabs.pipeline.graph.GraphNode;
import com.github.bhavuklabs.pipeline.langgraph.LangGraphState;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;

public class NodeAdapter implements NodeAction<LangGraphState> {

    private static final Logger logger = Logger.getLogger(NodeAdapter.class.getName());

    private final GraphNode<ResearchAgentState> legacyNode;

    public NodeAdapter(GraphNode<ResearchAgentState> legacyNode) {
        this.legacyNode = legacyNode;
    }

    @Override
    public Map<String, Object> apply(LangGraphState state) {
        try {

            ResearchAgentState legacyState = state.toLegacyState();

            if (!legacyNode.shouldExecute(legacyState)) {
                logger.info("Legacy node " + legacyNode.getName() + " chose not to execute");
                return Map.of();
            }

            CompletableFuture<ResearchAgentState> future = legacyNode.process(legacyState);
            ResearchAgentState result = future.join();

            return convertLegacyStateToUpdates(legacyState, result);

        } catch (Exception e) {
            logger.severe("Error in NodeAdapter for " + legacyNode.getName() + ": " + e.getMessage());
            return Map.of(LangGraphState.ERROR, e);
        }
    }

    private Map<String, Object> convertLegacyStateToUpdates(ResearchAgentState original, ResearchAgentState updated) {

        Map<String, Object> updates = new java.util.HashMap<>();

        if (updated.getMetadata()
            .containsKey("query_analysis") && !original.getMetadata()
            .containsKey("query_analysis")) {
            updates.put(LangGraphState.QUERY_ANALYSIS, updated.getMetadata()
                .get("query_analysis"));
        }

        if (updated.getCitations() != null && !updated.getCitations()
            .equals(original.getCitations())) {
            updates.put(LangGraphState.CITATIONS, updated.getCitations());
        }

        if (updated.getSelectedReasoning() != null && !updated.getSelectedReasoning()
            .equals(original.getSelectedReasoning())) {
            updates.put(LangGraphState.SELECTED_REASONING, updated.getSelectedReasoning());
        }

        if (updated.getFinalResponse() != null && !updated.getFinalResponse()
            .equals(original.getFinalResponse())) {
            updates.put(LangGraphState.RESPONSE, updated.getFinalResponse());
        }

        if (updated.getError() != null) {
            updates.put(LangGraphState.ERROR, updated.getError());
        }

        if (updated.getResearchContext() != null && !updated.getResearchContext()
            .equals(original.getResearchContext())) {
            updates.put(LangGraphState.RESEARCH_CONTEXT, updated.getResearchContext());
        }

        return updates;
    }

    public String getName() {
        return legacyNode.getName();
    }
}
