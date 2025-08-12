package com.github.bhavuklabs.deepresearch.models;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonInclude;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * Manages the complete research tree structure for a deep research session.
 * Provides tree navigation, node management, and progress tracking capabilities.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResearchTree {

    public enum TreeStatus {
        INITIALIZING,
        GENERATING_QUERIES,
        SEARCHING,
        PROCESSING_RESULTS,
        COMPLETED,
        ERROR,
        CANCELLED
    }

    private final String sessionId;
    private final String rootQuery;
    private final Instant createdAt;
    private volatile Instant completedAt;

    private TreeStatus status;
    private String currentNodeId;
    private final AtomicInteger totalNodesCreated;
    private final AtomicInteger completedNodes;

    // Tree structure
    private final ConcurrentHashMap<String, ResearchNode> nodeMap;
    private final ConcurrentHashMap<String, Set<String>> parentToChildrenMap;
    private final ConcurrentHashMap<String, String> childToParentMap;

    // Configuration
    private final int maxDepth;
    private final int breadthPerLevel;
    private volatile String errorMessage;

    public ResearchTree(String sessionId, String rootQuery, int maxDepth, int breadthPerLevel) {
        this.sessionId = Objects.requireNonNull(sessionId);
        this.rootQuery = Objects.requireNonNull(rootQuery);
        this.maxDepth = Math.max(1, maxDepth);
        this.breadthPerLevel = Math.max(1, breadthPerLevel);

        this.createdAt = Instant.now();
        this.status = TreeStatus.INITIALIZING;
        this.totalNodesCreated = new AtomicInteger(0);
        this.completedNodes = new AtomicInteger(0);

        this.nodeMap = new ConcurrentHashMap<>();
        this.parentToChildrenMap = new ConcurrentHashMap<>();
        this.childToParentMap = new ConcurrentHashMap<>();

        // Create root node
        ResearchNode rootNode = ResearchNode.createRootNode(sessionId, rootQuery);
        addNodeInternal(rootNode);
        this.currentNodeId = rootNode.getId();
    }

    // Node management methods
    public synchronized ResearchNode addChildNode(String parentId, String nodeId, String label, String researchGoal) {
        ResearchNode parentNode = getNode(parentId);
        if (parentNode == null) {
            throw new IllegalArgumentException("Parent node not found: " + parentId);
        }

        // Check depth constraints
        int parentDepth = getNodeDepth(parentId);
        if (parentDepth >= maxDepth) {
            throw new IllegalStateException("Maximum depth reached: " + maxDepth);
        }

        // Check breadth constraints
        Set<String> siblings = parentToChildrenMap.get(parentId);
        if (siblings != null && siblings.size() >= breadthPerLevel) {
            throw new IllegalStateException("Maximum breadth reached for parent " + parentId + ": " + breadthPerLevel);
        }

        ResearchNode childNode = ResearchNode.createChildNode(nodeId, sessionId, parentId, label, researchGoal);
        addNodeInternal(childNode);

        // Update relationships
        parentNode.addChildId(nodeId);
        parentToChildrenMap.computeIfAbsent(parentId, k -> ConcurrentHashMap.newKeySet()).add(nodeId);
        childToParentMap.put(nodeId, parentId);

        totalNodesCreated.incrementAndGet();
        return childNode;
    }

    private void addNodeInternal(ResearchNode node) {
        nodeMap.put(node.getId(), node);
        if (node.isRootNode()) {
            totalNodesCreated.set(1);
        }
    }

    public ResearchNode getNode(String nodeId) {
        return nodeMap.get(nodeId);
    }

    public ResearchNode getRootNode() {
        return getNode("0");
    }

    public ResearchNode getCurrentNode() {
        return currentNodeId != null ? getNode(currentNodeId) : null;
    }

    public void setCurrentNode(String nodeId) {
        if (nodeMap.containsKey(nodeId)) {
            this.currentNodeId = nodeId;
        }
    }

    // Tree navigation methods
    public List<ResearchNode> getChildren(String nodeId) {
        Set<String> childIds = parentToChildrenMap.get(nodeId);
        if (childIds == null || childIds.isEmpty()) {
            return Collections.emptyList();
        }

        return childIds.stream()
            .map(this::getNode)
            .filter(Objects::nonNull)
            .sorted(Comparator.comparing(ResearchNode::getCreatedAt))
            .collect(Collectors.toList());
    }

    public ResearchNode getParent(String nodeId) {
        String parentId = childToParentMap.get(nodeId);
        return parentId != null ? getNode(parentId) : null;
    }

    public List<ResearchNode> getSiblings(String nodeId) {
        String parentId = childToParentMap.get(nodeId);
        if (parentId == null) {
            return Collections.emptyList(); // Root node has no siblings
        }

        return getChildren(parentId).stream()
            .filter(node -> !node.getId().equals(nodeId))
            .collect(Collectors.toList());
    }

    public List<ResearchNode> getPath(String nodeId) {
        List<ResearchNode> path = new ArrayList<>();
        String currentId = nodeId;

        while (currentId != null) {
            ResearchNode node = getNode(currentId);
            if (node == null) break;

            path.add(0, node); // Add at beginning to maintain order
            currentId = childToParentMap.get(currentId);
        }

        return path;
    }

    public int getNodeDepth(String nodeId) {
        return getPath(nodeId).size() - 1; // Root is depth 0
    }

    public int getMaxCurrentDepth() {
        return nodeMap.keySet().stream()
            .mapToInt(this::getNodeDepth)
            .max()
            .orElse(0);
    }

    // Tree traversal methods
    public List<ResearchNode> getAllNodes() {
        return new ArrayList<>(nodeMap.values());
    }

    public List<ResearchNode> getNodesAtDepth(int depth) {
        return nodeMap.values().stream()
            .filter(node -> getNodeDepth(node.getId()) == depth)
            .sorted(Comparator.comparing(ResearchNode::getCreatedAt))
            .collect(Collectors.toList());
    }

    public List<ResearchNode> getLeafNodes() {
        return nodeMap.values().stream()
            .filter(ResearchNode::isLeafNode)
            .sorted(Comparator.comparing(ResearchNode::getCreatedAt))
            .collect(Collectors.toList());
    }

    public List<ResearchNode> getCompletedNodes() {
        return nodeMap.values().stream()
            .filter(ResearchNode::isCompleted)
            .sorted(Comparator.comparing(ResearchNode::getCreatedAt))
            .collect(Collectors.toList());
    }

    public List<ResearchNode> getErrorNodes() {
        return nodeMap.values().stream()
            .filter(ResearchNode::hasError)
            .sorted(Comparator.comparing(ResearchNode::getCreatedAt))
            .collect(Collectors.toList());
    }

    public List<ResearchNode> getProcessingNodes() {
        return nodeMap.values().stream()
            .filter(ResearchNode::isProcessing)
            .sorted(Comparator.comparing(ResearchNode::getCreatedAt))
            .collect(Collectors.toList());
    }

    // Progress tracking methods
    public synchronized void markNodeCompleted(String nodeId) {
        ResearchNode node = getNode(nodeId);
        if (node != null && !node.isCompleted() && !node.hasError()) {
            node.updateStatus(ResearchNode.NodeStatus.NODE_COMPLETE);
            completedNodes.incrementAndGet();

            // Check if entire tree is complete
            if (isTreeCompleted()) {
                this.status = TreeStatus.COMPLETED;
                this.completedAt = Instant.now();
            }
        }
    }

    public synchronized void markNodeError(String nodeId, String errorMessage) {
        ResearchNode node = getNode(nodeId);
        if (node != null) {
            node.setError(errorMessage);
        }
    }

    public boolean isTreeCompleted() {
        return nodeMap.values().stream()
            .allMatch(node -> node.isCompleted() || node.hasError());
    }

    public boolean hasErrors() {
        return nodeMap.values().stream()
            .anyMatch(ResearchNode::hasError);
    }

    public double getCompletionPercentage() {
        int total = totalNodesCreated.get();
        if (total == 0) return 0.0;

        int completed = completedNodes.get();
        return (double) completed / total * 100.0;
    }

    // Research results aggregation
    public List<ResearchNode.ProcessedLearning> getAllLearnings() {
        List<ResearchNode.ProcessedLearning> allLearnings = new ArrayList<>();
        Set<String> seenUrls = new HashSet<>();

        for (ResearchNode node : getCompletedNodes()) {
            for (ResearchNode.ProcessedLearning learning : node.getLearnings()) {
                // Deduplicate by URL
                if (learning.getUrl() != null && !seenUrls.contains(learning.getUrl())) {
                    seenUrls.add(learning.getUrl());
                    allLearnings.add(learning);
                }
            }
        }

        // Sort by relevance score
        allLearnings.sort((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()));
        return allLearnings;
    }

    // Statistics and metadata
    public Map<String, Object> getTreeStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("sessionId", sessionId);
        stats.put("rootQuery", rootQuery);
        stats.put("status", status);
        stats.put("createdAt", createdAt);
        stats.put("completedAt", completedAt);
        stats.put("totalNodes", totalNodesCreated.get());
        stats.put("completedNodes", completedNodes.get());
        stats.put("errorNodes", getErrorNodes().size());
        stats.put("processingNodes", getProcessingNodes().size());
        stats.put("maxDepth", maxDepth);
        stats.put("breadthPerLevel", breadthPerLevel);
        stats.put("currentDepth", getMaxCurrentDepth());
        stats.put("completionPercentage", getCompletionPercentage());

        if (completedAt != null) {
            stats.put("totalDuration", Duration.between(createdAt, completedAt));
        } else {
            stats.put("elapsedDuration", Duration.between(createdAt, Instant.now()));
        }

        return stats;
    }

    // Serialization for WebSocket/API responses
    public Map<String, Object> toTreeView() {
        Map<String, Object> treeView = new HashMap<>();
        treeView.put("sessionId", sessionId);
        treeView.put("status", status);
        treeView.put("rootNode", buildNodeView(getRootNode()));
        treeView.put("statistics", getTreeStatistics());
        return treeView;
    }

    private Map<String, Object> buildNodeView(ResearchNode node) {
        if (node == null) return null;

        Map<String, Object> nodeView = new HashMap<>();
        nodeView.put("id", node.getId());
        nodeView.put("label", node.getLabel());
        nodeView.put("researchGoal", node.getResearchGoal());
        nodeView.put("status", node.getStatus());
        nodeView.put("errorMessage", node.getErrorMessage());
        nodeView.put("learningsCount", node.getLearnings().size());
        nodeView.put("searchResultsCount", node.getSearchResults().size());
        nodeView.put("createdAt", node.getCreatedAt());
        nodeView.put("updatedAt", node.getUpdatedAt());

        // Add children recursively
        List<ResearchNode> children = getChildren(node.getId());
        if (!children.isEmpty()) {
            List<Map<String, Object>> childViews = children.stream()
                .map(this::buildNodeView)
                .collect(Collectors.toList());
            nodeView.put("children", childViews);
        }

        return nodeView;
    }

    // Getters and setters
    public String getSessionId() { return sessionId; }
    public String getRootQuery() { return rootQuery; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getCompletedAt() { return completedAt; }

    public TreeStatus getStatus() { return status; }
    public void setStatus(TreeStatus status) { this.status = status; }

    public String getCurrentNodeId() { return currentNodeId; }

    public int getTotalNodesCreated() { return totalNodesCreated.get(); }
    public int getCompletedNodesCount() { return completedNodes.get(); }

    public int getMaxDepth() { return maxDepth; }
    public int getBreadthPerLevel() { return breadthPerLevel; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    @JsonIgnore
    public Set<String> getAllNodeIds() {
        return new HashSet<>(nodeMap.keySet());
    }

    @Override
    public String toString() {
        return String.format("ResearchTree{sessionId='%s', status=%s, nodes=%d, completed=%d, maxDepth=%d}",
            sessionId, status, totalNodesCreated.get(), completedNodes.get(), maxDepth);
    }
}