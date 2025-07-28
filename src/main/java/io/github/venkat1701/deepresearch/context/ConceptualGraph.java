package io.github.venkat1701.deepresearch.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;


public class ConceptualGraph {

    private final Map<String, ConceptNode> concepts;
    private final Map<String, Set<String>> relationships;
    private final Map<String, Double> relationshipWeights;

    public ConceptualGraph() {
        this.concepts = new ConcurrentHashMap<>();
        this.relationships = new ConcurrentHashMap<>();
        this.relationshipWeights = new ConcurrentHashMap<>();
    }

    public void addConcept(String conceptId, String category) {
        concepts.put(conceptId, new ConceptNode(conceptId, category));
        relationships.computeIfAbsent(conceptId, k -> new HashSet<>());
    }

    public void addRelationship(String concept1, String concept2) {
        addRelationship(concept1, concept2, 1.0);
    }

    public void addRelationship(String concept1, String concept2, double weight) {
        relationships.computeIfAbsent(concept1, k -> new HashSet<>()).add(concept2);
        relationships.computeIfAbsent(concept2, k -> new HashSet<>()).add(concept1);

        String relationshipKey = concept1 + ":" + concept2;
        relationshipWeights.put(relationshipKey, weight);
    }

    public List<String> getRelatedConcepts(String conceptId) {
        return new ArrayList<>(relationships.getOrDefault(conceptId, Set.of()));
    }

    public int getConceptCount() {
        return concepts.size();
    }

    public Map<String, Integer> getConceptsByCategory() {
        Map<String, Integer> categoryCounts = new HashMap<>();
        for (ConceptNode node : concepts.values()) {
            categoryCounts.merge(node.getCategory(), 1, Integer::sum);
        }
        return categoryCounts;
    }

    public List<String> findShortestPath(String startConcept, String endConcept) {

        if (!concepts.containsKey(startConcept) || !concepts.containsKey(endConcept)) {
            return List.of();
        }

        Map<String, String> parent = new HashMap<>();
        Set<String> visited = new HashSet<>();
        java.util.Queue<String> queue = new java.util.LinkedList<>();

        queue.offer(startConcept);
        visited.add(startConcept);
        parent.put(startConcept, null);

        while (!queue.isEmpty()) {
            String current = queue.poll();

            if (current.equals(endConcept)) {
                return reconstructPath(parent, startConcept, endConcept);
            }

            for (String neighbor : relationships.getOrDefault(current, Set.of())) {
                if (!visited.contains(neighbor)) {
                    visited.add(neighbor);
                    parent.put(neighbor, current);
                    queue.offer(neighbor);
                }
            }
        }

        return List.of();
    }

    private List<String> reconstructPath(Map<String, String> parent, String start, String end) {
        List<String> path = new ArrayList<>();
        String current = end;

        while (current != null) {
            path.add(0, current);
            current = parent.get(current);
        }

        return path.get(0).equals(start) ? path : List.of();
    }

    public static class ConceptNode {
        private final String id;
        private final String category;
        private final Map<String, Object> properties;

        public ConceptNode(String id, String category) {
            this.id = id;
            this.category = category;
            this.properties = new HashMap<>();
        }

        public String getId() { return id; }
        public String getCategory() { return category; }
        public Map<String, Object> getProperties() { return properties; }

        public void addProperty(String key, Object value) {
            properties.put(key, value);
        }
    }
}