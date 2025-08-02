package com.github.bhavuklabs.pipeline;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.github.bhavuklabs.citation.service.CitationService;
import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;
import com.github.bhavuklabs.pipeline.graph.GraphNode;
import com.github.bhavuklabs.pipeline.nodes.CitationFetchNode;
import com.github.bhavuklabs.pipeline.nodes.QueryAnalysisNode;
import com.github.bhavuklabs.pipeline.nodes.ReasoningExecutionNode;
import com.github.bhavuklabs.pipeline.nodes.ReasoningSelectionNode;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.pipeline.router.DynamicRouter;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;
import com.github.bhavuklabs.reasoning.engine.ReasoningEngine;

public class DynamicResearchAgent {

    private static final Logger logger = Logger.getLogger(DynamicResearchAgent.class.getName());
    private final CitationService citationService;
    private final ReasoningEngine reasoningEngine;
    private final LLMClient llmClient;
    private final DynamicRouter router;
    private final ExecutorService executor;

    private final QueryAnalysisNode queryAnalysisNode;
    private final CitationFetchNode citationFetchNode;
    private final ReasoningSelectionNode reasoningSelectionNode;
    private final ReasoningExecutionNode reasoningExecutionNode;

    public DynamicResearchAgent(CitationService citationService, ReasoningEngine reasoningEngine, LLMClient llmClient) {
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

    public CompletableFuture<ResearchAgentState> executeNode(String nodeName, ResearchAgentState state) {
        GraphNode<ResearchAgentState> node = router.getNodes()
            .get(nodeName);

        if (node == null) {
            logger.warning("Node not found: " + nodeName);
            return CompletableFuture.completedFuture(state.withError(new IllegalStateException("Node not found: " + nodeName)));
        }

        if (!node.shouldExecute(state)) {
            logger.info("Node " + nodeName + " decided not to execute, skipping");
            return CompletableFuture.completedFuture(state);
        }

        logger.info("Executing node: " + nodeName);
        return node.process(state)
            .handle((result, throwable) -> {
                if (throwable != null) {
                    logger.severe("Error in node " + nodeName + ": " + throwable.getMessage());
                    if (router.shouldRetry(state, nodeName, (Exception) throwable)) {
                        return executeNode(nodeName, state).join();
                    } else {
                        return state.withError((Exception) throwable);
                    }
                }
                return result;
            });
    }

    public CompletableFuture<ResearchAgentState> executePipeline(ResearchAgentState initialState) {
        CompletableFuture<ResearchAgentState> pipeline = CompletableFuture.completedFuture(initialState);
        String currentNode = "start";

        while (!currentNode.equals("end")) {
            final String nodeToExecute = currentNode;

            pipeline = pipeline.thenCompose(state -> {
                if (state.getError() != null) {
                    return CompletableFuture.completedFuture(state);
                }

                List<String> nextNodes = router.determineNextNodes(state, nodeToExecute);

                if (nextNodes.isEmpty()) {
                    return CompletableFuture.completedFuture(state);
                }

                String nextNode = nextNodes.get(0);
                return executeNode(nextNode, state);
            });

            ResearchAgentState currentState = pipeline.join();
            if (currentState.getError() != null) {
                break;
            }

            List<String> nextNodes = router.determineNextNodes(currentState, currentNode);
            if (nextNodes.isEmpty()) {
                break;
            }

            currentNode = nextNodes.get(0);
        }

        return pipeline;
    }

    public CompletableFuture<ResearchAgentState> processQuery(String sessionId, String query, UserProfile userProfile, ResearchPromptConfig config) {

        ResearchAgentState initialState = new ResearchAgentState(sessionId, query, userProfile, config);
        return executeGraph(initialState, "start");
    }

    private CompletableFuture<ResearchAgentState> executeGraph(ResearchAgentState state, String currentNode) {
        if ("end".equals(currentNode) || state.isComplete()) {
            return CompletableFuture.completedFuture(state);
        }

        List<String> nextNodes = router.determineNextNodes(state, currentNode);
        if (nextNodes.isEmpty()) {
            return CompletableFuture.completedFuture(state.withError(new IllegalStateException("No next nodes found for: " + currentNode)));
        }

        if (nextNodes.size() == 1) {
            String nextNode = nextNodes.get(0);
            GraphNode<ResearchAgentState> node = router.getNodes()
                .get(nextNode);

            if (node != null && node.shouldExecute(state)) {
                return node.process(state)
                    .thenCompose(newState -> {
                        if (newState.getError() != null) {
                            return CompletableFuture.completedFuture(newState);
                        }
                        return executeGraph(newState, nextNode);
                    })
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
                        try {
                            ResearchAgentState nodeResult = future.join();
                            if (nodeResult.getError() != null) {
                                return nodeResult; // Return first error encountered
                            }
                            mergedState = mergeStates(mergedState, nodeResult);
                        } catch (Exception e) {
                            return state.withError(e);
                        }
                    }
                    return mergedState;
                })
                .thenCompose(mergedState -> {
                    if (mergedState.getError() != null) {
                        return CompletableFuture.completedFuture(mergedState);
                    }
                    return executeGraph(mergedState, "reasoning_selection");
                });
        }
    }

    private ResearchAgentState mergeStates(ResearchAgentState state1, ResearchAgentState state2) {
        ResearchAgentState merged = state1.copy();
        ResearchAgentState finalMerged = merged;
        state2.getMetadata()
            .forEach((key, value) -> {
                if (value != null) {
                    finalMerged.getMetadata()
                        .put(key, value);
                }
            });

        if (state2.getCitations() != null && !state2.getCitations()
            .isEmpty()) {
            merged = merged.withCitations(state2.getCitations());
        }

        return merged;
    }

    public void shutdown() {
        try {
            citationFetchNode.shutdown();
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread()
                .interrupt();
        } finally {
            reasoningEngine.shutdown();
        }
    }
}