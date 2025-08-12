package com.github.bhavuklabs.deepresearch.executor;

import com.github.bhavuklabs.citation.service.CitationService;
import com.github.bhavuklabs.config.Research4jConfig;
import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;
import com.github.bhavuklabs.deepresearch.models.DeepResearchResult;
import com.github.bhavuklabs.deepresearch.models.ResearchNode;
import com.github.bhavuklabs.deepresearch.models.ResearchTree;
import com.github.bhavuklabs.deepresearch.websocket.ProgressWebSocketBroadcaster;
import com.github.bhavuklabs.pipeline.langgraph.EnhancedLangGraphState;
import com.github.bhavuklabs.pipeline.langgraph.nodes.LangGraphTreeQueryGenerationNode;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.reasoning.engine.ReasoningEngine;
import org.bsc.langgraph4j.CompiledGraph;
import org.bsc.langgraph4j.RunnableConfig;
import org.bsc.langgraph4j.StateGraph;
import org.bsc.langgraph4j.action.AsyncEdgeAction;
import org.bsc.langgraph4j.checkpoint.MemorySaver;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

import static org.bsc.langgraph4j.StateGraph.END;
import static org.bsc.langgraph4j.StateGraph.START;

/**
 * LangGraph4j-based executor for tree-structured deep research.
 * Orchestrates the complete flow from query generation through search and processing.
 */
public class LangGraphDeepResearchExecutor {

    private static final Logger logger = Logger.getLogger(LangGraphDeepResearchExecutor.class.getName());

    private final CompiledGraph<EnhancedLangGraphState> compiledGraph;
    private final MemorySaver checkpointSaver;
    private final ProgressWebSocketBroadcaster progressBroadcaster;
    private final Research4jConfig config;

    // Active research sessions
    private final ConcurrentHashMap<String, ResearchSession> activeSessions;

    // Graph nodes
    private final LangGraphTreeQueryGenerationNode queryGenerationNode;
    // Additional nodes will be added as we implement them

    /**
     * Represents an active research session with its state and configuration
     */
    public static class ResearchSession {
        private final String sessionId;
        private final String originalQuery;
        private final EnhancedLangGraphState.TreeConfig treeConfig;
        private final UserProfile userProfile;
        private volatile ResearchTree researchTree;
        private volatile boolean cancelled = false;

        public ResearchSession(String sessionId, String originalQuery,
            EnhancedLangGraphState.TreeConfig treeConfig, UserProfile userProfile) {
            this.sessionId = sessionId;
            this.originalQuery = originalQuery;
            this.treeConfig = treeConfig;
            this.userProfile = userProfile;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getOriginalQuery() { return originalQuery; }
        public EnhancedLangGraphState.TreeConfig getTreeConfig() { return treeConfig; }
        public UserProfile getUserProfile() { return userProfile; }
        public ResearchTree getResearchTree() { return researchTree; }
        public boolean isCancelled() { return cancelled; }

        // Setters
        public void setResearchTree(ResearchTree researchTree) { this.researchTree = researchTree; }
        public void cancel() { this.cancelled = true; }
    }

    public LangGraphDeepResearchExecutor(Research4jConfig config,
        CitationService citationService,
        ReasoningEngine reasoningEngine,
        LLMClient llmClient,
        ProgressWebSocketBroadcaster progressBroadcaster) {
        this.config = config;
        this.progressBroadcaster = progressBroadcaster;
        this.checkpointSaver = new MemorySaver();
        this.activeSessions = new ConcurrentHashMap<>();

        // Initialize nodes
        this.queryGenerationNode = new LangGraphTreeQueryGenerationNode(llmClient);

        // Build the graph
        this.compiledGraph = buildDeepResearchGraph();

        logger.info("LangGraphDeepResearchExecutor initialized successfully");
    }

    /**
     * Start a new tree-based deep research session
     */
    public CompletableFuture<String> startDeepResearch(String query,
        UserProfile userProfile,
        int maxDepth,
        int breadthPerLevel,
        boolean enableWebSocket) {

        String sessionId = UUID.randomUUID().toString();

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting deep research session: " + sessionId + " for query: " + query);

                // Create tree configuration
                EnhancedLangGraphState.TreeConfig treeConfig =
                    new EnhancedLangGraphState.TreeConfig(maxDepth, breadthPerLevel, enableWebSocket);

                // Create and register session
                ResearchSession session = new ResearchSession(sessionId, query, treeConfig, userProfile);
                activeSessions.put(sessionId, session);

                // Create initial state
                EnhancedLangGraphState initialState = createInitialState(sessionId, query, treeConfig, userProfile);

                // Initialize research tree
                ResearchTree tree = initialState.createResearchTree(sessionId, query, maxDepth, breadthPerLevel);
                session.setResearchTree(tree);

                // Set current node to root
                initialState.setCurrentNodeId("0");

                // Start the graph execution asynchronously
                executeResearchGraph(sessionId, initialState);

                return sessionId;

            } catch (Exception e) {
                logger.severe("Failed to start deep research: " + e.getMessage());
                activeSessions.remove(sessionId);
                throw new RuntimeException("Failed to start deep research", e);
            }
        });
    }

    /**
     * Get the current status of a research session
     */
    public CompletableFuture<Map<String, Object>> getResearchStatus(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            ResearchSession session = activeSessions.get(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("Research session not found: " + sessionId);
            }

            ResearchTree tree = session.getResearchTree();
            if (tree == null) {
                throw new IllegalStateException("Research tree not initialized for session: " + sessionId);
            }

            return tree.getTreeStatistics();
        });
    }

    /**
     * Get the complete tree structure for a research session
     */
    public CompletableFuture<Map<String, Object>> getResearchTree(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            ResearchSession session = activeSessions.get(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("Research session not found: " + sessionId);
            }

            ResearchTree tree = session.getResearchTree();
            if (tree == null) {
                throw new IllegalStateException("Research tree not initialized for session: " + sessionId);
            }

            return tree.toTreeView();
        });
    }

    /**
     * Retry a failed node
     */
    public CompletableFuture<Boolean> retryNode(String sessionId, String nodeId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResearchSession session = activeSessions.get(sessionId);
                if (session == null || session.isCancelled()) {
                    return false;
                }

                ResearchTree tree = session.getResearchTree();
                ResearchNode node = tree.getNode(nodeId);

                if (node == null || !node.hasError()) {
                    return false;
                }

                logger.info("Retrying node: " + nodeId + " in session: " + sessionId);

                // Reset node status
                node.updateStatus(ResearchNode.NodeStatus.GENERATING_QUERY);
                node.setErrorMessage(null);

                // Create state for retry
                EnhancedLangGraphState retryState = createRetryState(session, nodeId);

                // Execute graph starting from the retry node
                executeResearchGraph(sessionId, retryState);

                return true;

            } catch (Exception e) {
                logger.severe("Failed to retry node " + nodeId + ": " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Cancel an active research session
     */
    public CompletableFuture<Boolean> cancelResearch(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                ResearchSession session = activeSessions.get(sessionId);
                if (session == null) {
                    return false;
                }

                logger.info("Cancelling research session: " + sessionId);

                session.cancel();
                ResearchTree tree = session.getResearchTree();
                if (tree != null) {
                    tree.setStatus(ResearchTree.TreeStatus.CANCELLED);
                }

                // Close WebSocket connections
                progressBroadcaster.closeAllConnections(sessionId);

                // Remove from active sessions after a delay (allow final messages)
                CompletableFuture.delayedExecutor(5, java.util.concurrent.TimeUnit.SECONDS)
                    .execute(() -> activeSessions.remove(sessionId));

                return true;

            } catch (Exception e) {
                logger.warning("Error cancelling research session " + sessionId + ": " + e.getMessage());
                return false;
            }
        });
    }

    /**
     * Get final results for a completed research session
     */
    public CompletableFuture<DeepResearchResult> getResearchResults(String sessionId) {
        return CompletableFuture.supplyAsync(() -> {
            ResearchSession session = activeSessions.get(sessionId);
            if (session == null) {
                throw new IllegalArgumentException("Research session not found: " + sessionId);
            }

            ResearchTree tree = session.getResearchTree();
            if (tree == null || !tree.isTreeCompleted()) {
                throw new IllegalStateException("Research not completed for session: " + sessionId);
            }

            // Convert tree results to DeepResearchResult
            return convertTreeToResult(tree);
        });
    }

    private CompiledGraph<EnhancedLangGraphState> buildDeepResearchGraph() {
        try {
            StateGraph<EnhancedLangGraphState> stateGraph = new StateGraph<>(EnhancedLangGraphState.SCHEMA);

            // Add nodes
            stateGraph.addNode("query_generation", queryGenerationNode);
            // TODO: Add other nodes as they are implemented
            // .addNode("search_execution", searchExecutionNode)
            // .addNode("result_processing", resultProcessingNode)
            // .addNode("tree_completion", treeCompletionNode)

            // Define the flow
            stateGraph.addEdge(START, "query_generation");

            // Conditional edge based on query generation result
            stateGraph.addConditionalEdges("query_generation",
                AsyncEdgeAction.edge_async(this::shouldContinueToSearch),
                Map.of(
                    "search", END, // TODO: Change to "search_execution" when implemented
                    "complete", END,
                    "error", END
                ));

            return stateGraph.compile();

        } catch (Exception e) {
            logger.severe("Failed to build deep research graph: " + e.getMessage());
            throw new RuntimeException("Graph compilation failed", e);
        }
    }

    private String shouldContinueToSearch(EnhancedLangGraphState state) {
        Map<String, Object> result = (Map<String, Object>) state.getData().get("result");
        if (result == null) {
            return "error";
        }

        Boolean success = (Boolean) result.get("success");
        if (success == null || !success) {
            return "error";
        }

        Boolean skipped = (Boolean) result.get("skipped");
        if (skipped != null && skipped) {
            return "complete";
        }

        return "search";
    }

    private void executeResearchGraph(String sessionId, EnhancedLangGraphState initialState) {
        CompletableFuture.runAsync(() -> {
            try {
                logger.info("Executing research graph for session: " + sessionId);

                RunnableConfig config = RunnableConfig.builder()
                    .withConfigurable(Map.of("thread_id", sessionId))
                    .build();

                // Execute the graph
                EnhancedLangGraphState finalState = compiledGraph.invoke(initialState, config);

                // Handle completion
                handleResearchCompletion(sessionId, finalState);

            } catch (Exception e) {
                logger.severe("Error executing research graph for session " + sessionId + ": " + e.getMessage());
                handleResearchError(sessionId, e);
            }
        });
    }

    private void handleResearchCompletion(String sessionId, EnhancedLangGraphState finalState) {
        try {
            ResearchSession session = activeSessions.get(sessionId);
            if (session == null || session.isCancelled()) {
                return;
            }

            ResearchTree tree = finalState.getResearchTree();
            if (tree != null) {
                tree.setStatus(ResearchTree.TreeStatus.COMPLETED);

                // Broadcast completion
                finalState.broadcastTreeComplete();

                logger.info("Research completed successfully for session: " + sessionId);
            }

        } catch (Exception e) {
            logger.warning("Error handling research completion: " + e.getMessage());
        }
    }

    private void handleResearchError(String sessionId, Exception error) {
        try {
            ResearchSession session = activeSessions.get(sessionId);
            if (session == null) {
                return;
            }

            ResearchTree tree = session.getResearchTree();
            if (tree != null) {
                tree.setStatus(ResearchTree.TreeStatus.ERROR);
                tree.setErrorMessage(error.getMessage());
            }

            // Broadcast error
            progressBroadcaster.broadcastError(sessionId, null, error.getMessage());

            logger.severe("Research failed for session " + sessionId + ": " + error.getMessage());

        } catch (Exception e) {
            logger.severe("Error handling research error: " + e.getMessage());
        }
    }

    private EnhancedLangGraphState createInitialState(String sessionId, String query,
        EnhancedLangGraphState.TreeConfig treeConfig,
        UserProfile userProfile) {

        Map<String, Object> initialData = Map.of(
            EnhancedLangGraphState.SESSION_ID, sessionId,
            EnhancedLangGraphState.QUERY, query,
            EnhancedLangGraphState.USER_PROFILE, userProfile,
            EnhancedLangGraphState.TREE_CONFIG, treeConfig,
            EnhancedLangGraphState.PROGRESS_BROADCASTER, progressBroadcaster
        );

        return new EnhancedLangGraphState(initialData);
    }

    private EnhancedLangGraphState createRetryState(ResearchSession session, String nodeId) {
        EnhancedLangGraphState retryState = createInitialState(
            session.getSessionId(),
            session.getOriginalQuery(),
            session.getTreeConfig(),
            session.getUserProfile()
        );

        retryState.setResearchTree(session.getResearchTree());
        retryState.setCurrentNodeId(nodeId);

        return retryState;
    }

    private DeepResearchResult convertTreeToResult(ResearchTree tree) {
        // Convert ResearchTree to DeepResearchResult format
        // This is a simplified conversion - you might want to enhance this based on your needs

        DeepResearchResult.Builder builder = DeepResearchResult.builder()
            .sessionId(tree.getSessionId())
            .originalQuery(tree.getRootQuery())
            .researchDepth(tree.getMaxCurrentDepth())