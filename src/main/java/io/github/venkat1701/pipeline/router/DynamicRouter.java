package io.github.venkat1701.pipeline.router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.venkat1701.pipeline.graph.GraphNode;
import io.github.venkat1701.pipeline.models.QueryAnalysis;
import io.github.venkat1701.pipeline.state.ResearchAgentState;

public class DynamicRouter {
    private final Map<String, GraphNode<ResearchAgentState>> nodes = new HashMap<>();

    public DynamicRouter() { }

    public void registerNode(String name, GraphNode<ResearchAgentState> node) {
        nodes.put(name, node);
    }

    public Map<String, GraphNode<ResearchAgentState>> getNodes() {
        return nodes;
    }

    public List<String> determineNextNodes(ResearchAgentState state, String currentNode) {
        return switch (currentNode) {
            case "start" -> List.of("query_analysis");
            case "query_analysis" -> {
                QueryAnalysis analysis = (QueryAnalysis) state.getMetadata().get("query_analysis");
                if (analysis != null && analysis.requiresCitations) {
                    yield List.of("citation_fetch");
                } else {
                    yield List.of("reasoning_selection");
                }
            }
            case "citation_fetch" -> List.of("reasoning_selection");
            case "reasoning_selection" -> List.of("reasoning_execution");
            case "reasoning_execution" -> List.of("end");
            default -> List.of("end");
        };
    }

    public boolean shouldRetry(ResearchAgentState state, String nodeName, Exception error) {
        if (error instanceof java.net.SocketTimeoutException) {
            return state.getMetadata().getOrDefault(nodeName + "_retries", 0).equals(0);
        }
        return false;
    }
}
