package com.github.bhavuklabs.deepresearch.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.fasterxml.jackson.annotation.JsonInclude;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ResearchNode {

    public enum NodeStatus {
        GENERATING_QUERY,
        GENERATED_QUERY,
        SEARCHING,
        SEARCH_COMPLETE,
        PROCESSING_SEARCH_RESULT,
        PROCESSING_SEARCH_RESULT_REASONING,
        GENERATING_QUERY_REASONING,
        NODE_COMPLETE,
        ERROR
    }

    private final String id;
    private final String sessionId;
    private String parentId;
    private String label;
    private String researchGoal;
    private NodeStatus status;
    private String errorMessage;

    private String generateQueriesReasoning;
    private String generateLearningsReasoning;
    private List<WebSearchResult> searchResults;
    private List<ProcessedLearning> learnings;

    private final Instant createdAt;
    private Instant updatedAt;
    private final ConcurrentMap<String, Object> metadata;

    @JsonIgnore
    private final List<String> childIds;

    public ResearchNode(String id, String sessionId, String label) {
        this.id = Objects.requireNonNull(id, "Node ID cannot be null");
        this.sessionId = Objects.requireNonNull(sessionId, "Session ID cannot be null");
        this.label = Objects.requireNonNull(label, "Label cannot be null");
        this.status = NodeStatus.GENERATING_QUERY;
        this.createdAt = Instant.now();
        this.updatedAt = Instant.now();
        this.metadata = new ConcurrentHashMap<>();
        this.childIds = new ArrayList<>();
        this.searchResults = new ArrayList<>();
        this.learnings = new ArrayList<>();
    }

    // Root node constructor
    public static ResearchNode createRootNode(String sessionId, String query) {
        ResearchNode root = new ResearchNode("0", sessionId, "Start");
        root.setResearchGoal(query);
        root.setStatus(NodeStatus.NODE_COMPLETE);
        return root;
    }

    // Child node constructor with parent relationship
    public static ResearchNode createChildNode(String nodeId, String sessionId, String parentId,
        String label, String researchGoal) {
        ResearchNode child = new ResearchNode(nodeId, sessionId, label);
        child.setParentId(parentId);
        child.setResearchGoal(researchGoal);
        return child;
    }

    /**
     * ProcessedLearning represents a learning extracted from search results
     * Maps to the frontend ProcessedSearchResult.learnings structure
     */
    public static class ProcessedLearning {
        private String url;
        private String title;
        private String learning;
        private double relevanceScore;

        public ProcessedLearning() {}

        public ProcessedLearning(String url, String learning) {
            this.url = url;
            this.learning = learning;
            this.relevanceScore = 1.0;
        }

        public ProcessedLearning(String url, String title, String learning, double relevanceScore) {
            this.url = url;
            this.title = title;
            this.learning = learning;
            this.relevanceScore = relevanceScore;
        }

        // Getters and Setters
        public String getUrl() { return url; }
        public void setUrl(String url) { this.url = url; }

        public String getTitle() { return title; }
        public void setTitle(String title) { this.title = title; }

        public String getLearning() { return learning; }
        public void setLearning(String learning) { this.learning = learning; }

        public double getRelevanceScore() { return relevanceScore; }
        public void setRelevanceScore(double relevanceScore) { this.relevanceScore = relevanceScore; }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof ProcessedLearning)) return false;
            ProcessedLearning that = (ProcessedLearning) o;
            return Objects.equals(url, that.url) && Objects.equals(learning, that.learning);
        }

        @Override
        public int hashCode() {
            return Objects.hash(url, learning);
        }
    }

    // Node relationship methods
    public void addChildId(String childId) {
        if (!childIds.contains(childId)) {
            childIds.add(childId);
            touch();
        }
    }

    public void removeChildId(String childId) {
        if (childIds.remove(childId)) {
            touch();
        }
    }

    public boolean isRootNode() {
        return "0".equals(id);
    }

    public boolean isLeafNode() {
        return childIds.isEmpty();
    }

    public boolean hasChildren() {
        return !childIds.isEmpty();
    }

    public boolean hasParent() {
        return parentId != null;
    }

    // Progress and status methods
    public void updateStatus(NodeStatus newStatus) {
        this.status = newStatus;
        touch();
    }

    public void setError(String errorMessage) {
        this.status = NodeStatus.ERROR;
        this.errorMessage = errorMessage;
        touch();
    }

    public boolean isCompleted() {
        return status == NodeStatus.NODE_COMPLETE;
    }

    public boolean hasError() {
        return status == NodeStatus.ERROR;
    }

    public boolean isProcessing() {
        return status != NodeStatus.NODE_COMPLETE &&
            status != NodeStatus.ERROR;
    }

    // Reasoning methods
    public void appendGenerateQueriesReasoning(String delta) {
        if (generateQueriesReasoning == null) {
            generateQueriesReasoning = delta;
        } else {
            generateQueriesReasoning += delta;
        }
        touch();
    }

    public void appendGenerateLearningsReasoning(String delta) {
        if (generateLearningsReasoning == null) {
            generateLearningsReasoning = delta;
        } else {
            generateLearningsReasoning += delta;
        }
        touch();
    }

    // Search results methods
    public void setSearchResults(List<WebSearchResult> searchResults) {
        this.searchResults = new ArrayList<>(searchResults);
        touch();
    }

    public void addSearchResult(WebSearchResult result) {
        if (searchResults == null) {
            searchResults = new ArrayList<>();
        }
        searchResults.add(result);
        touch();
    }

    // Learnings methods
    public void setLearnings(List<ProcessedLearning> learnings) {
        this.learnings = new ArrayList<>(learnings);
        touch();
    }

    public void addLearning(ProcessedLearning learning) {
        if (learnings == null) {
            learnings = new ArrayList<>();
        }
        // Avoid duplicates based on URL and content
        if (!learnings.contains(learning)) {
            learnings.add(learning);
            touch();
        }
    }

    // Metadata methods
    public void setMetadata(String key, Object value) {
        metadata.put(key, value);
        touch();
    }

    public Object getMetadata(String key) {
        return metadata.get(key);
    }

    @SuppressWarnings("unchecked")
    public <T> T getMetadata(String key, Class<T> type) {
        Object value = metadata.get(key);
        if (type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    // Utility methods
    private void touch() {
        this.updatedAt = Instant.now();
    }

    public ResearchNode copy() {
        ResearchNode copy = new ResearchNode(this.id, this.sessionId, this.label);
        copy.parentId = this.parentId;
        copy.researchGoal = this.researchGoal;
        copy.status = this.status;
        copy.errorMessage = this.errorMessage;
        copy.generateQueriesReasoning = this.generateQueriesReasoning;
        copy.generateLearningsReasoning = this.generateLearningsReasoning;

        if (this.searchResults != null) {
            copy.searchResults = new ArrayList<>(this.searchResults);
        }
        if (this.learnings != null) {
            copy.learnings = new ArrayList<>(this.learnings);
        }

        copy.childIds.addAll(this.childIds);
        copy.metadata.putAll(this.metadata);

        return copy;
    }

    // Getters and Setters
    public String getId() { return id; }
    public String getSessionId() { return sessionId; }

    public String getParentId() { return parentId; }
    public void setParentId(String parentId) {
        this.parentId = parentId;
        touch();
    }

    public String getLabel() { return label; }
    public void setLabel(String label) {
        this.label = label;
        touch();
    }

    public String getResearchGoal() { return researchGoal; }
    public void setResearchGoal(String researchGoal) {
        this.researchGoal = researchGoal;
        touch();
    }

    public NodeStatus getStatus() { return status; }
    public void setStatus(NodeStatus status) {
        this.status = status;
        touch();
    }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
        touch();
    }

    public String getGenerateQueriesReasoning() { return generateQueriesReasoning; }
    public void setGenerateQueriesReasoning(String generateQueriesReasoning) {
        this.generateQueriesReasoning = generateQueriesReasoning;
        touch();
    }

    public String getGenerateLearningsReasoning() { return generateLearningsReasoning; }
    public void setGenerateLearningsReasoning(String generateLearningsReasoning) {
        this.generateLearningsReasoning = generateLearningsReasoning;
        touch();
    }

    public List<WebSearchResult> getSearchResults() {
        return searchResults != null ? new ArrayList<>(searchResults) : new ArrayList<>();
    }

    public List<ProcessedLearning> getLearnings() {
        return learnings != null ? new ArrayList<>(learnings) : new ArrayList<>();
    }

    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }

    public List<String> getChildIds() {
        return new ArrayList<>(childIds);
    }

    public ConcurrentMap<String, Object> getMetadataMap() {
        return new ConcurrentHashMap<>(metadata);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof ResearchNode)) return false;
        ResearchNode that = (ResearchNode) o;
        return Objects.equals(id, that.id) && Objects.equals(sessionId, that.sessionId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id, sessionId);
    }

    @Override
    public String toString() {
        return String.format("ResearchNode{id='%s', sessionId='%s', label='%s', status=%s, parentId='%s', childCount=%d}",
            id, sessionId, label, status, parentId, childIds.size());
    }
}
