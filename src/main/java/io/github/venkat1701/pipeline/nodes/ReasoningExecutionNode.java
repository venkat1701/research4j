package io.github.venkat1701.pipeline.nodes;

import java.util.concurrent.CompletableFuture;

import io.github.venkat1701.pipeline.graph.GraphNode;
import io.github.venkat1701.pipeline.models.StructuredResponse;
import io.github.venkat1701.pipeline.models.TableResponse;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.pipeline.state.ResearchAgentState;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class ReasoningExecutionNode implements GraphNode<ResearchAgentState> {
    private final ReasoningEngine reasoningEngine;

    public ReasoningExecutionNode(ReasoningEngine reasoningEngine) {
        this.reasoningEngine = reasoningEngine;
    }

    @Override
    public CompletableFuture<ResearchAgentState> process(ResearchAgentState state) {
        return reasoningEngine.reasonAsync(
            state.getSelectedReasoning(),
            state.getResearchContext(),
            determineOutputType(state)
        ).thenApply(state::withResponse);
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
    public String getName() { return "reasoning_execution"; }

    @Override
    public boolean shouldExecute(ResearchAgentState state) {
        return state.getSelectedReasoning() != null;
    }
}
