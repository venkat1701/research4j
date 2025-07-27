package io.github.venkat1701.deepresearch.langgraph;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class LangGraph4jWorkflowBuilder {
    private Map<String, Function<LangGraph4jResearchState, LangGraph4jResearchState>> nodes = new HashMap<>();
    private Map<String, String> edges = new HashMap<>();
    private Map<String, Map<String, String>> conditionalEdges = new HashMap<>();
    private int maxIterations = 20;
    private int parallelism = 5;
    private java.time.Duration timeout = java.time.Duration.ofMinutes(30);

    public LangGraph4jWorkflowBuilder addNode(String name, java.util.function.Function<LangGraph4jResearchState, LangGraph4jResearchState> function) {
        nodes.put(name, function);
        return this;
    }

    public LangGraph4jWorkflowBuilder addEdge(String from, String to) {
        edges.put(from, to);
        return this;
    }

    public LangGraph4jWorkflowBuilder addConditionalEdge(String from,
        java.util.function.Function<LangGraph4jResearchState, String> condition,
        Map<String, String> transitions) {
        conditionalEdges.put(from, transitions);
        return this;
    }

    public LangGraph4jWorkflowBuilder setMaxIterations(int maxIterations) {
        this.maxIterations = maxIterations;
        return this;
    }

    public LangGraph4jWorkflowBuilder setParallelism(int parallelism) {
        this.parallelism = parallelism;
        return this;
    }

    public LangGraph4jWorkflowBuilder setTimeout(java.time.Duration timeout) {
        this.timeout = timeout;
        return this;
    }

    public LangGraph4jResearchWorkflow build() {
        return new LangGraph4jResearchWorkflow(nodes, edges, conditionalEdges, maxIterations, parallelism, timeout);
    }
}