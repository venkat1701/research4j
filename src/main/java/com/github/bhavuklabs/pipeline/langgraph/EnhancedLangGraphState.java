package com.github.bhavuklabs.pipeline.langgraph;

import com.github.bhavuklabs.deepresearch.models.ResearchNode;
import com.github.bhavuklabs.deepresearch.models.ResearchTree;
import com.github.bhavuklabs.deepresearch.websocket.ProgressWebSocketBroadcaster;
import com.github.bhavuklabs.pipeline.langgraph.LangGraphState;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import org.bsc.langgraph4j.serializer.StateSerializer;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Enhanced LangGraphState that supports tree-based deep research operations.
 * Extends the base LangGraphState with tree management capabilities.
 */
public class EnhancedLangGraphState extends LangGraphState {

    // Tree-specific state keys
    public static final String RESEARCH_TREE = "researchTree";
    public static final String CURRENT_NODE_ID = "currentNodeId";
    public static final String PROGRESS_BROADCASTER = "progressBroadcaster";
    public static final String TREE_CONFIG = "treeConfig";
    public static final String GENERATED_QUERIES = "generatedQueries";
    public static final String SEARCH_RESULTS = "searchResults";
    public static final String PROCESSED_LEARNINGS = "processedLearnings";
    public static final String REASONING_DELTA = "reasoningDelta";
    public static final String NODE_ERROR = "nodeError";

    // Tree configuration
    public static class TreeConfig {
        private final int maxDepth;
        private final int breadthPerLevel;
        private final boolean enableWebSocket;

        public TreeConfig(int maxDepth, int breadthPerLevel, boolean enableWebSocket) {
            this.maxDepth = maxDepth;
            this.breadthPerLevel = breadthPerLevel;
            this.enableWebSocket = enableWebSocket;
        }

        public int getMaxDepth() { return maxDepth; }
        public int getBreadthPerLevel() { return breadthPerLevel; }
        public boolean isWebSocketEnabled() { return enableWebSocket; }
    }

    // Generated query structure
    public static class GeneratedQuery {
        private final String nodeId;
        private final String query;
        private final String researchGoal;
        private final String parentNodeId;

        public GeneratedQuery(String nodeId, String query, String researchGoal, String parentNodeId) {
            this.nodeId = nodeId;
            this.query = query;
            this.researchGoal = researchGoal;
            this.parentNodeId = parentNodeId;
        }

        public String getNodeId() { return nodeId; }
        public String getQuery() { return query; }
        public String getResearchGoal() { return researchGoal; }
        public String getParentNodeId() { return parentNodeId; }
    }

    public EnhancedLangGraphState(Map<String, Object> data) {
        super(data);
    }

    // Factory method to create enhanced state from base state
    public static EnhancedLangGraphState fromBaseLangGraphState(LangGraphState baseState, TreeConfig treeConfig) {
        EnhancedLangGraphState enhancedState = new EnhancedLangGraphState(baseState.getData());
        enhancedState.setTreeConfig(treeConfig);
        return enhancedState;
    }

    // Tree management methods
    public ResearchTree getResearchTree() {
        return (ResearchTree) getData().get(RESEARCH_TREE);
    }

    public void setResearchTree(ResearchTree researchTree) {
        getData().put(RESEARCH_TREE, researchTree);
    }

    public ResearchTree createResearchTree(String sessionId, String rootQuery, int maxDepth, int breadthPerLevel) {
        ResearchTree tree = new ResearchTree(sessionId, rootQuery, maxDepth, breadthPerLevel);
        setResearchTree(tree);
        return tree;
    }

    // Current node management
    public String getCurrentNodeId() {
        return (String) getData().get(CURRENT_NODE_ID);
    }

    public void setCurrentNodeId(String nodeId) {
        getData().put(CURRENT_NODE_ID, nodeId);

        // Also update the tree's current node
        ResearchTree tree = getResearchTree();
        if (tree != null) {
            tree.setCurrentNode(nodeId);
        }
    }

    public ResearchNode getCurrentNode() {
        ResearchTree tree = getResearchTree();
        String nodeId = getCurrentNodeId();
        return (tree != null && nodeId != null) ? tree.getNode(nodeId) : null;
    }

    // Progress broadcasting
    public ProgressWebSocketBroadcaster getProgressBroadcaster() {
        return (ProgressWebSocketBroadcaster) getData().get(PROGRESS_BROADCASTER);
    }

    public void setProgressBroadcaster(ProgressWebSocketBroadcaster broadcaster) {
        getData().put(PROGRESS_BROADCASTER, broadcaster);
    }

    // Tree configuration
    public TreeConfig getTreeConfig() {
        return (TreeConfig) getData().get(TREE_CONFIG);
    }

    public void setTreeConfig(TreeConfig config) {
        getData().put(TREE_CONFIG, config);
    }

    // Generated queries management
    @SuppressWarnings("unchecked")
    public List<GeneratedQuery> getGeneratedQueries() {
        return (List<GeneratedQuery>) getData().getOrDefault(GENERATED_QUERIES, new ArrayList<>());
    }

    public void setGeneratedQueries(List<GeneratedQuery> queries) {
        getData().put(GENERATED_QUERIES, queries);
    }

    public void addGeneratedQuery(GeneratedQuery query) {
        List<GeneratedQuery> queries = getGeneratedQueries();
        queries.add(query);
        setGeneratedQueries(queries);
    }

    // Search results management
    @SuppressWarnings("unchecked")
    public Map<String, Object> getSearchResults() {
        return (Map<String, Object>) getData().getOrDefault(SEARCH_RESULTS, new HashMap<>());
    }

    public void setSearchResults(Map<String, Object> searchResults) {
        getData().put(SEARCH_RESULTS, searchResults);
    }

    public void setNodeSearchResults(String nodeId, Object searchResults) {
        Map<String, Object> allResults = getSearchResults();
        allResults.put(nodeId, searchResults);
        setSearchResults(allResults);
    }

    public Object getNodeSearchResults(String nodeId) {
        return getSearchResults().get(nodeId);
    }

    // Processed learnings management
    @SuppressWarnings("unchecked")
    public Map<String, List<ResearchNode.ProcessedLearning>> getProcessedLearnings() {
        return (Map<String, List<ResearchNode.ProcessedLearning>>)
            getData().getOrDefault(PROCESSED_LEARNINGS, new HashMap<>());
    }

    public void setProcessedLearnings(Map<String, List<ResearchNode.ProcessedLearning>> learnings) {
        getData().put(PROCESSED_LEARNINGS, learnings);
    }

    public void setNodeLearnings(String nodeId, List<ResearchNode.ProcessedLearning> learnings) {
        Map<String, List<ResearchNode.ProcessedLearning>> allLearnings = getProcessedLearnings();
        allLearnings.put(nodeId, learnings);
        setProcessedLearnings(allLearnings);
    }

    public List<ResearchNode.ProcessedLearning> getNodeLearnings(String nodeId) {
        return getProcessedLearnings().getOrDefault(nodeId, new ArrayList<>());
    }

    // Reasoning delta management (for streaming reasoning)
    @SuppressWarnings("unchecked")
    public Map<String, String> getReasoningDeltas() {
        return (Map<String, String>) getData().getOrDefault(REASONING_DELTA, new HashMap<>());
    }

    public void setReasoningDeltas(Map<String, String> deltas) {
        getData().put(REASONING_DELTA, deltas);
    }

    public void appendReasoningDelta(String nodeId, String deltaType, String delta) {
        Map<String, String> deltas = getReasoningDeltas();
        String key = nodeId + ":" + deltaType;
        String existing = deltas.getOrDefault(key, "");
        deltas.put(key, existing + delta);
        setReasoningDeltas(deltas);
    }

    public String getReasoningDelta(String nodeId, String deltaType) {
        Map<String, String> deltas = getReasoningDeltas();
        String key = nodeId + ":" + deltaType;
        return deltas.getOrDefault(key, "");
    }

    // Node error management
    @SuppressWarnings("unchecked")
    public Map<String, String> getNodeErrors() {
        return (Map<String, String>) getData().getOrDefault(NODE_ERROR, new HashMap<>());
    }

    public void setNodeErrors(Map<String, String> errors) {
        getData().put(NODE_ERROR, errors);
    }

    public void setNodeError(String nodeId, String error) {
        Map<String, String> errors = getNodeErrors();
        errors.put(nodeId, error);
        setNodeErrors(errors);
    }

    public String getNodeError(String nodeId) {
        return getNodeErrors().get(nodeId);
    }

    public boolean hasNodeError(String nodeId) {
        return getNodeErrors().containsKey(nodeId);
    }

    // Utility methods for tree operations
    public boolean isTreeInitialized() {
        return getResearchTree() != null;
    }

    public boolean hasCurrentNode() {
        return getCurrentNodeId() != null && getCurrentNode() != null;
    }

    public int getTreeDepth() {
        ResearchTree tree = getResearchTree();
        return tree != null ? tree.getMaxCurrentDepth() : 0;
    }

    public int getTotalNodes() {
        ResearchTree tree = getResearchTree();
        return tree != null ? tree.getTotalNodesCreated() : 0;
    }

    public boolean isTreeCompleted() {
        ResearchTree tree = getResearchTree();
        return tree != null && tree.isTreeCompleted();
    }

    public double getTreeCompletionPercentage() {
        ResearchTree tree = getResearchTree();
        return tree != null ? tree.getCompletionPercentage() : 0.0;
    }

    // WebSocket broadcasting helper methods
    public void broadcastProgress(String messageType, Object data) {
        ProgressWebSocketBroadcaster broadcaster = getProgressBroadcaster();
        if (broadcaster != null) {
            String nodeId = getCurrentNodeId();
            broadcaster.broadcastProgress(getSessionId(), nodeId, messageType, data);
        }
    }

    public void broadcastNodeProgress(String nodeId, String messageType, Object data) {
        ProgressWebSocketBroadcaster broadcaster = getProgressBroadcaster();
        if (broadcaster != null) {
            broadcaster.broadcastProgress(getSessionId(), nodeId, messageType, data);
        }
    }

    public void broadcastError(String nodeId, String error) {
        ProgressWebSocketBroadcaster broadcaster = getProgressBroadcaster();
        if (broadcaster != null) {
            Map<String, Object> errorData = Map.of("message", error, "nodeId", nodeId);
            broadcaster.broadcastProgress(getSessionId(), nodeId, "error", errorData);
        }
    }

    public void broadcastTreeComplete() {
        ProgressWebSocketBroadcaster broadcaster = getProgressBroadcaster();
        if (broadcaster != null) {
            ResearchTree tree = getResearchTree();
            Map<String, Object> completeData = Map.of(
                "learnings", tree != null ? tree.getAllLearnings() : new ArrayList<>(),
                "statistics", tree != null ? tree.getTreeStatistics() : new HashMap<>()
            );
            broadcaster.broadcastProgress(getSessionId(), null, "complete", completeData);
        }
    }

    // Node ID generation utilities (matching frontend pattern)
    public static String generateChildNodeId(String parentId, int childIndex) {
        if ("0".equals(parentId)) {
            return "0-" + childIndex;
        }
        return parentId + "-" + childIndex;
    }

    public static String getParentNodeId(String nodeId) {
        if ("0".equals(nodeId)) {
            return null; // Root has no parent
        }

        int lastDashIndex = nodeId.lastIndexOf('-');
        if (lastDashIndex > 0) {
            return nodeId.substring(0, lastDashIndex);
        }

        return "0"; // Default to root as parent
    }

    public static int getNodeDepth(String nodeId) {
        if ("0".equals(nodeId)) {
            return 0;
        }
        return (int) nodeId.chars().filter(c -> c == '-').count();
    }

    // State validation methods
    public boolean isValidForTreeOperation() {
        return isTreeInitialized() && getSessionId() != null;
    }

    public boolean isValidForNodeOperation() {
        return isValidForTreeOperation() && hasCurrentNode();
    }

    public void validateTreeState() {
        if (!isValidForTreeOperation()) {
            throw new IllegalStateException("Tree state is not properly initialized");
        }
    }

    public void validateNodeState() {
        if (!isValidForNodeOperation()) {
            throw new IllegalStateException("Node state is not properly initialized");
        }
    }

    // State snapshot for debugging/monitoring
    public Map<String, Object> createStateSnapshot() {
        Map<String, Object> snapshot = new HashMap<>();
        snapshot.put("sessionId", getSessionId());
        snapshot.put("currentNodeId", getCurrentNodeId());
        snapshot.put("treeInitialized", isTreeInitialized());
        snapshot.put("hasCurrentNode", hasCurrentNode());
        snapshot.put("treeDepth", getTreeDepth());
        snapshot.put("totalNodes", getTotalNodes());
        snapshot.put("completionPercentage", getTreeCompletionPercentage());
        snapshot.put("isTreeCompleted", isTreeCompleted());

        ResearchTree tree = getResearchTree();
        if (tree != null) {
            snapshot.put("treeStatus", tree.getStatus());
            snapshot.put("errorNodes", tree.getErrorNodes().size());
            snapshot.put("processingNodes", tree.getProcessingNodes().size());
        }

        return snapshot;
    }

    // Clone method for state transitions
    public EnhancedLangGraphState copy() {
        // Create a deep copy of the data map
        Map<String, Object> copiedData = new ConcurrentHashMap<>();

        // Copy all data from parent state
        copiedData.putAll(getData());

        return new EnhancedLangGraphState(copiedData);
    }

    @Override
    public String toString() {
        return String.format("EnhancedLangGraphState{sessionId='%s', currentNodeId='%s', treeNodes=%d, completion=%.1f%%}",
            getSessionId(), getCurrentNodeId(), getTotalNodes(), getTreeCompletionPercentage());
    }

    // StateSerializer for LangGraph4j compatibility
    public static final StateSerializer<EnhancedLangGraphState> SCHEMA = new StateSerializer<EnhancedLangGraphState>() {
        @Override
        public Map<String, Object> write(EnhancedLangGraphState state) {
            return state.getData();
        }

        @Override
        public EnhancedLangGraphState read(Map<String, Object> data) {
            return new EnhancedLangGraphState(data);
        }
    };
}