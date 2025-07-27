package io.github.venkat1701.deepresearch.context;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;


public class InformationHierarchy {

    private final Map<String, HierarchyNode> rootNodes;
    private final Map<String, HierarchyNode> allNodes;
    private final Map<String, Double> importanceScores;

    public InformationHierarchy() {
        this.rootNodes = new ConcurrentHashMap<>();
        this.allNodes = new ConcurrentHashMap<>();
        this.importanceScores = new ConcurrentHashMap<>();
    }

    public void addInformation(String key, String content, String type) {
        addInformation(key, content, type, null, 1.0);
    }

    public void addInformation(String key, String content, String type, String parentKey, double importance) {
        HierarchyNode node = new HierarchyNode(key, content, type, importance);
        allNodes.put(key, node);
        importanceScores.put(key, importance);

        if (parentKey != null && allNodes.containsKey(parentKey)) {
            HierarchyNode parent = allNodes.get(parentKey);
            parent.addChild(node);
            node.setParent(parent);
        } else {
            rootNodes.put(key, node);
        }
    }

    public List<HierarchyNode> getTopLevelNodes() {
        return new ArrayList<>(rootNodes.values());
    }

    public List<HierarchyNode> getMostImportantNodes(int limit) {
        return allNodes.values().stream()
            .sorted((n1, n2) -> Double.compare(n2.getImportance(), n1.getImportance()))
            .limit(limit)
            .collect(java.util.stream.Collectors.toList());
    }

    public HierarchyNode getNode(String key) {
        return allNodes.get(key);
    }

    public static class HierarchyNode {
        private final String key;
        private final String content;
        private final String type;
        private final double importance;
        private final List<HierarchyNode> children;
        private HierarchyNode parent;

        public HierarchyNode(String key, String content, String type, double importance) {
            this.key = key;
            this.content = content;
            this.type = type;
            this.importance = importance;
            this.children = new ArrayList<>();
        }

        public void addChild(HierarchyNode child) {
            children.add(child);
        }

        public void setParent(HierarchyNode parent) {
            this.parent = parent;
        }

        
        public String getKey() { return key; }
        public String getContent() { return content; }
        public String getType() { return type; }
        public double getImportance() { return importance; }
        public List<HierarchyNode> getChildren() { return children; }
        public HierarchyNode getParent() { return parent; }

        public int getDepth() {
            return parent == null ? 0 : parent.getDepth() + 1;
        }

        public boolean isLeaf() {
            return children.isEmpty();
        }
    }
}