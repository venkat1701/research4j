package com.github.bhavuklabs.pipeline.nodes;

import java.util.concurrent.CompletableFuture;

import com.github.bhavuklabs.pipeline.graph.GraphNode;
import com.github.bhavuklabs.pipeline.models.StructuredResponse;
import com.github.bhavuklabs.pipeline.models.TableResponse;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;
import com.github.bhavuklabs.reasoning.engine.ReasoningEngine;

public class ReasoningExecutionNode implements GraphNode<ResearchAgentState> {

    private final ReasoningEngine reasoningEngine;

    public ReasoningExecutionNode(ReasoningEngine reasoningEngine) {
        this.reasoningEngine = reasoningEngine;
    }

    @Override
    public CompletableFuture<ResearchAgentState> process(ResearchAgentState state) {
        return reasoningEngine.reasonAsync(state.getSelectedReasoning(), state.getResearchContext(), determineOutputType(state))
            .thenApply(state::withResponse);
    }

    private Class<?> determineOutputType(ResearchAgentState state) {
        UserProfile profile = state.getUserProfile();

        return switch (profile.getPreferredFormat()) {
            case JSON -> StructuredResponse.class;
            case TABLE -> TableResponse.class;
            default -> String.class;
        };
    }

    @Override
    public String getName() {
        return "reasoning_execution";
    }

    @Override
    public boolean shouldExecute(ResearchAgentState state) {
        return state.getSelectedReasoning() != null;
    }
}
