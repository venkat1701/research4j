package io.github.venkat1701.deepresearch.langgraph;

import java.util.Map;

public class LangGraph4jResearchWorkflow {
    private final Map<String, java.util.function.Function<LangGraph4jResearchState, LangGraph4jResearchState>> nodes;
    private final Map<String, String> edges;
    private final Map<String, Map<String, String>> conditionalEdges;
    private final int maxIterations;
    private final int parallelism;
    private final java.time.Duration timeout;

    public LangGraph4jResearchWorkflow(
        Map<String, java.util.function.Function<LangGraph4jResearchState, LangGraph4jResearchState>> nodes,
        Map<String, String> edges,
        Map<String, Map<String, String>> conditionalEdges,
        int maxIterations,
        int parallelism,
        java.time.Duration timeout) {
        this.nodes = nodes;
        this.edges = edges;
        this.conditionalEdges = conditionalEdges;
        this.maxIterations = maxIterations;
        this.parallelism = parallelism;
        this.timeout = timeout;
    }

    public LangGraph4jResearchState execute(LangGraph4jResearchState initialState) {
        LangGraph4jResearchState currentState = initialState;
        String currentNode = "query_analysis";
        int iteration = 0;

        while (currentNode != null && iteration < maxIterations) {
            
            if (nodes.containsKey(currentNode)) {
                currentState = nodes.get(currentNode).apply(currentState);
            }

            
            currentNode = getNextNode(currentNode, currentState);
            iteration++;
        }

        return currentState;
    }

    private String getNextNode(String currentNode, LangGraph4jResearchState state) {
        
        if (conditionalEdges.containsKey(currentNode)) {
            
            Map<String, String> transitions = conditionalEdges.get(currentNode);
            return transitions.values().iterator().next();
        }

        
        return edges.get(currentNode);
    }
}