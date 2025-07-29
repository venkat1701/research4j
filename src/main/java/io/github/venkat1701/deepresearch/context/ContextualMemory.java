package io.github.venkat1701.deepresearch.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import io.github.venkat1701.citation.CitationResult;

public class ContextualMemory {

    private final Map<String, MemoryNode> memoryNodes;
    private final Map<String, List<String>> contextualConnections;
    private final Map<String, Double> relevanceScores;
    private final Map<String, Instant> accessTimestamps;

    public ContextualMemory() {
        this.memoryNodes = new ConcurrentHashMap<>();
        this.contextualConnections = new ConcurrentHashMap<>();
        this.relevanceScores = new ConcurrentHashMap<>();
        this.accessTimestamps = new ConcurrentHashMap<>();
    }

    public void addCitation(String context, CitationResult citation) {
        String nodeId = generateNodeId(context, citation.getTitle());
        MemoryNode node = new MemoryNode(nodeId, "citation", citation.getContent(), citation.getRelevanceScore());
        node.addMetadata("url", citation.getUrl());
        node.addMetadata("domain", citation.getDomain());
        node.addMetadata("retrievedAt", citation.getRetrievedAt()
            .toString());

        memoryNodes.put(nodeId, node);
        relevanceScores.put(nodeId, citation.getRelevanceScore());
        accessTimestamps.put(nodeId, Instant.now());

        contextualConnections.computeIfAbsent(context, k -> new ArrayList<>())
            .add(nodeId);
    }

    public void updateKnowledge(String key, Object value) {
        String nodeId = "knowledge_" + key;
        MemoryNode node = new MemoryNode(nodeId, "knowledge", value.toString(), 1.0);
        memoryNodes.put(nodeId, node);
        relevanceScores.put(nodeId, 1.0);
        accessTimestamps.put(nodeId, Instant.now());
    }

    public List<MemoryNode> getRelatedMemories(String context, int limit) {
        List<String> nodeIds = contextualConnections.getOrDefault(context, List.of());
        return nodeIds.stream()
            .limit(limit)
            .map(memoryNodes::get)
            .filter(node -> node != null)
            .peek(node -> accessTimestamps.put(node.getId(), Instant.now()))
            .collect(java.util.stream.Collectors.toList());
    }

    public List<MemoryNode> searchMemories(String query, int limit) {
        String queryLower = query.toLowerCase();
        return memoryNodes.values()
            .stream()
            .filter(node -> node.getContent()
                .toLowerCase()
                .contains(queryLower))
            .sorted((n1, n2) -> Double.compare(relevanceScores.getOrDefault(n2.getId(), 0.0), relevanceScores.getOrDefault(n1.getId(), 0.0)))
            .limit(limit)
            .peek(node -> accessTimestamps.put(node.getId(), Instant.now()))
            .collect(java.util.stream.Collectors.toList());
    }

    private String generateNodeId(String context, String title) {
        return context.hashCode() + "_" + title.hashCode();
    }

    public static class MemoryNode {

        private final String id;
        private final String type;
        private final String content;
        private final double relevance;
        private final Map<String, Object> metadata;
        private final Instant createdAt;

        public MemoryNode(String id, String type, String content, double relevance) {
            this.id = id;
            this.type = type;
            this.content = content;
            this.relevance = relevance;
            this.metadata = new HashMap<>();
            this.createdAt = Instant.now();
        }

        public void addMetadata(String key, Object value) {
            metadata.put(key, value);
        }

        public String getId() {
            return id;
        }

        public String getType() {
            return type;
        }

        public String getContent() {
            return content;
        }

        public double getRelevance() {
            return relevance;
        }

        public Map<String, Object> getMetadata() {
            return metadata;
        }

        public Instant getCreatedAt() {
            return createdAt;
        }
    }
}