package com.github.bhavuklabs.pipeline.executor.impl;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;
import org.bsc.langgraph4j.serializer.StateSerializer;

import com.github.bhavuklabs.citation.service.CitationService;
import com.github.bhavuklabs.config.Research4jConfig;
import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;
import com.github.bhavuklabs.pipeline.executor.GraphExecutor;
import com.github.bhavuklabs.pipeline.langgraph.LangGraphState;
import com.github.bhavuklabs.pipeline.langgraph.nodes.LangGraphCitationFetchNode;
import com.github.bhavuklabs.pipeline.langgraph.nodes.LangGraphQueryAnalysisNode;
import com.github.bhavuklabs.pipeline.langgraph.nodes.LangGraphReasoningExecutionNode;
import com.github.bhavuklabs.pipeline.langgraph.nodes.LangGraphReasoningSelectionNode;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;
import com.github.bhavuklabs.reasoning.engine.ReasoningEngine;

public class LangGraphExecutor implements GraphExecutor {

    private static final Logger logger = Logger.getLogger(LangGraphExecutor.class.getName());

    private final CompiledGraph<LangGraphState> compiledGraph;
    private final MemorySaver checkpointSaver;
    private final Research4jConfig config;

    private final LangGraphQueryAnalysisNode queryAnalysisNode;
    private final LangGraphCitationFetchNode citationFetchNode;
    private final LangGraphReasoningSelectionNode reasoningSelectionNode;
    private final LangGraphReasoningExecutionNode reasoningExecutionNode;

    public LangGraphExecutor(Research4jConfig config, CitationService citationService, ReasoningEngine reasoningEngine, LLMClient llmClient) {

        this.config = config;
        this.checkpointSaver = new MemorySaver();

        this.queryAnalysisNode = new LangGraphQueryAnalysisNode(llmClient);
        this.citationFetchNode = new LangGraphCitationFetchNode(citationService);
        this.reasoningSelectionNode = new LangGraphReasoningSelectionNode(llmClient);
        this.reasoningExecutionNode = new LangGraphReasoningExecutionNode(reasoningEngine);

        this.compiledGraph = buildGraph();

        logger.info("LangGraph4j executor initialized successfully");
    }

    private CompiledGraph<LangGraphState> buildGraph() {
        try {
            StateGraph<LangGraphState> stateGraph = new StateGraph<>((StateSerializer) LangGraphState.SCHEMA);

            stateGraph.addNode("query_analysis", queryAnalysisNode)
                .addNode("citation_fetch", citationFetchNode)
                .addNode("reasoning_selection", reasoningSelectionNode)
                .addNode("reasoning_execution", reasoningExecutionNode);

            stateGraph

                .addEdge(START, "query_analysis")

                .addConditionalEdges("query_analysis", AsyncEdgeAction.edge_async(this::shouldFetchCitations),
                    Map.of("fetch_citations", "citation_fetch", "skip_citations", "reasoning_selection"))

                .addEdge("citation_fetch", "reasoning_selection")

                .addEdge("reasoning_selection", "reasoning_execution")

                .addEdge("reasoning_execution", END);

            return stateGraph.compile();

        } catch (Exception e) {
            logger.severe("Failed to build LangGraph4j graph: " + e.getMessage());
            throw new RuntimeException("Graph compilation failed", e);
        }
    }

    private String shouldFetchCitations(LangGraphState state) {
        var queryAnalysis = state.getQueryAnalysis();
        if (queryAnalysis != null && queryAnalysis.requiresCitations) {
            logger.info("Query analysis indicates citations are needed");
            return "fetch_citations";
        }
        logger.info("Query analysis indicates citations can be skipped");
        return "skip_citations";
    }

    @Override
    public CompletableFuture<ResearchAgentState> processQuery(String sessionId, String query, UserProfile userProfile, ResearchPromptConfig config) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Processing query using LangGraph4j executor");

                Map<String, Object> initialData = Map.of(LangGraphState.SESSION_ID, sessionId, LangGraphState.QUERY, query, LangGraphState.USER_PROFILE,
                    userProfile, LangGraphState.METADATA, Map.of("prompt_config", config));

                LangGraphState initialState = new LangGraphState(initialData);

                RunnableConfig runnableConfig = RunnableConfig.builder()
                    .checkPointId(sessionId)
                    .build();

                Optional<LangGraphState> finalState = compiledGraph.invoke(initialData, runnableConfig);

                ResearchAgentState result = finalState.get()
                    .toLegacyState();

                logger.info("LangGraph4j execution completed successfully");
                return result;

            } catch (Exception e) {
                logger.severe("Error in LangGraph4j executor: " + e.getMessage());
                throw new RuntimeException("LangGraph4j execution failed", e);
            }
        });
    }

    @Override
    public boolean isHealthy() {
        try {

            return compiledGraph != null && queryAnalysisNode != null && citationFetchNode != null && reasoningSelectionNode != null &&
                reasoningExecutionNode != null;
        } catch (Exception e) {
            logger.warning("Health check failed for LangGraph4j executor: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down LangGraph4j executor");

    }

    @Override
    public String getExecutorType() {
        return "LANGGRAPH4J";
    }

    public Map<String, Object> getCheckpointHistory(String threadId) {
        try {

            return Map.of("thread_id", threadId, "status", "available");
        } catch (Exception e) {
            logger.warning("Failed to retrieve checkpoint history: " + e.getMessage());
            return Map.of("error", e.getMessage());
        }
    }
}