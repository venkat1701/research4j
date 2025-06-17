package io.github.venkat1701.pipeline;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.ResearchPromptConfig;
import io.github.venkat1701.pipeline.graph.GraphNode;
import io.github.venkat1701.pipeline.nodes.CitationFetchNode;
import io.github.venkat1701.pipeline.nodes.QueryAnalysisNode;
import io.github.venkat1701.pipeline.nodes.ReasoningExecutionNode;
import io.github.venkat1701.pipeline.nodes.ReasoningSelectionNode;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.pipeline.router.DynamicRouter;
import io.github.venkat1701.pipeline.state.ResearchAgentState;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class DynamicResearchAgent {
    private final CitationService citationService;
    private final ReasoningEngine reasoningEngine;
    private final LLMClient llmClient;
    private final DynamicRouter router;
    private final ExecutorService executor;

    private final QueryAnalysisNode queryAnalysisNode;
    private final CitationFetchNode citationFetchNode;
    private final ReasoningSelectionNode reasoningSelectionNode;
    private final ReasoningExecutionNode reasoningExecutionNode;

    public DynamicResearchAgent(CitationService citationService,
        ReasoningEngine reasoningEngine,
        LLMClient llmClient) {
        this.citationService = citationService;
        this.reasoningEngine = reasoningEngine;
        this.llmClient = llmClient;
        this.router = new DynamicRouter();
        this.executor = Executors.newVirtualThreadPerTaskExecutor();

        this.queryAnalysisNode = new QueryAnalysisNode(llmClient);
        this.citationFetchNode = new CitationFetchNode(citationService);
        this.reasoningSelectionNode = new ReasoningSelectionNode(llmClient);
        this.reasoningExecutionNode = new ReasoningExecutionNode(reasoningEngine);

        router.registerNode("query_analysis", queryAnalysisNode);
        router.registerNode("citation_fetch", citationFetchNode);
        router.registerNode("reasoning_selection", reasoningSelectionNode);
        router.registerNode("reasoning_execution", reasoningExecutionNode);
    }

    public CompletableFuture<ResearchAgentState> processQuery(String sessionId,
        String query,
        UserProfile userProfile,
        ResearchPromptConfig config) {

        ResearchAgentState initialState = new ResearchAgentState(sessionId, query, userProfile, config);
        return executeGraph(initialState, "start");
    }

    private CompletableFuture<ResearchAgentState> executeGraph(ResearchAgentState state, String currentNode) {
        if ("end".equals(currentNode) || state.isComplete()) {
            return CompletableFuture.completedFuture(state);
        }

        List<String> nextNodes = router.determineNextNodes(state, currentNode);
        if (nextNodes.isEmpty()) {
            return CompletableFuture.completedFuture(state);
        }

        if (nextNodes.size() == 1) {
            String nextNode = nextNodes.get(0);
            GraphNode<ResearchAgentState> node = router.getNodes()
                .get(nextNode);

            if (node != null && node.shouldExecute(state)) {
                return node.process(state)
                    .thenCompose(newState -> executeGraph(newState, nextNode))
                    .exceptionally(throwable -> state.withError((Exception) throwable));
            } else {
                return executeGraph(state, nextNode);
            }
        } else {
            List<CompletableFuture<ResearchAgentState>> futures = nextNodes.stream()
                .map(nodeName -> {
                    GraphNode<ResearchAgentState> node = router.getNodes()
                        .get(nodeName);
                    if (node != null && node.shouldExecute(state)) {
                        return node.process(state);
                    }
                    return CompletableFuture.completedFuture(state);
                })
                .toList();

            return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(v -> {
                    ResearchAgentState mergedState = state;
                    for (CompletableFuture<ResearchAgentState> future : futures) {
                        ResearchAgentState nodeResult = future.join();
                        mergedState = mergeStates(mergedState, nodeResult);
                    }
                    return mergedState;
                })
                .thenCompose(mergedState -> executeGraph(mergedState, "reasoning_execution"));
        }
    }

    private ResearchAgentState mergeStates(ResearchAgentState state1, ResearchAgentState state2) {
        ResearchAgentState merged = state1.copy();
        merged.getMetadata()
            .putAll(state2.getMetadata());
        if (!state2.getCitations().isEmpty()) {
            merged = merged.withCitations(state2.getCitations());
        }
        return merged;
    }

    public void shutdown() {
        executor.shutdown();
        reasoningEngine.shutdown();
    }


}
