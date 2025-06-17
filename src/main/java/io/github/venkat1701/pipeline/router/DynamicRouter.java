package io.github.venkat1701.pipeline.router;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.github.venkat1701.pipeline.graph.GraphNode;
import io.github.venkat1701.pipeline.models.QueryAnalysis;
import io.github.venkat1701.pipeline.state.ResearchAgentState;

public class DynamicRouter {

    private static final Logger logger = Logger.getLogger(DynamicRouter.class.getName());
    private static final int MAX_RETRIES = 3;
    private static final String RETRY_COUNT_KEY = "_retry_count";

    private final Map<String, GraphNode<ResearchAgentState>> nodes = new HashMap<>();

    public DynamicRouter() {
        logger.info("DynamicRouter initialized");
    }

    public void registerNode(String name, GraphNode<ResearchAgentState> node) {
        if (name == null || name.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Node name cannot be null or empty");
        }
        if (node == null) {
            throw new IllegalArgumentException("Node cannot be null");
        }

        nodes.put(name, node);
        logger.info("Registered node: " + name);
    }

    public Map<String, GraphNode<ResearchAgentState>> getNodes() {
        return new HashMap<>(nodes);
    }

    public List<String> determineNextNodes(ResearchAgentState state, String currentNode) {
        if (state == null) {
            logger.warning("State is null, returning end node");
            return List.of("end");
        }

        if (currentNode == null) {
            logger.warning("Current node is null, starting from beginning");
            return List.of("query_analysis");
        }

        logger.info("Determining next nodes from: " + currentNode);

        return switch (currentNode) {
            case "start" -> {
                logger.info("Starting pipeline with query analysis");
                yield List.of("query_analysis");
            }

            case "query_analysis" -> {
                QueryAnalysis analysis = getQueryAnalysis(state);
                if (analysis != null && analysis.requiresCitations) {
                    logger.info("Citations required, proceeding to citation fetch");
                    yield List.of("citation_fetch");
                } else {
                    logger.info("No citations required, proceeding to reasoning selection");
                    yield List.of("reasoning_selection");
                }
            }

            case "citation_fetch" -> {
                logger.info("Citations fetched, proceeding to reasoning selection");
                yield List.of("reasoning_selection");
            }

            case "reasoning_selection" -> {
                if (state.getSelectedReasoning() != null) {
                    logger.info("Reasoning selected: " + state.getSelectedReasoning() + ", proceeding to execution");
                    yield List.of("reasoning_execution");
                } else {
                    logger.warning("No reasoning selected, ending pipeline");
                    yield List.of("end");
                }
            }

            case "reasoning_execution" -> {
                if (state.getFinalResponse() != null) {
                    logger.info("Response generated, pipeline complete");
                    yield List.of("end");
                } else {
                    logger.warning("No response generated, retrying reasoning selection");
                    yield List.of("reasoning_selection");
                }
            }

            case "end" -> {
                logger.info("Pipeline complete");
                yield List.of();
            }

            default -> {
                logger.warning("Unknown node: " + currentNode + ", ending pipeline");
                yield List.of("end");
            }
        };
    }

    private QueryAnalysis getQueryAnalysis(ResearchAgentState state) {
        try {
            Object analysis = state.getMetadata()
                .get("query_analysis");
            if (analysis instanceof QueryAnalysis) {
                return (QueryAnalysis) analysis;
            }
        } catch (Exception e) {
            logger.warning("Error retrieving query analysis: " + e.getMessage());
        }
        return null;
    }

    public boolean shouldRetry(ResearchAgentState state, String nodeName, Exception error) {
        if (state == null || nodeName == null || error == null) {
            return false;
        }

        String retryKey = nodeName + RETRY_COUNT_KEY;
        int currentRetries = (Integer) state.getMetadata()
            .getOrDefault(retryKey, 0);

        if (currentRetries >= MAX_RETRIES) {
            logger.warning("Max retries reached for node: " + nodeName);
            return false;
        }

        boolean shouldRetry = isRetryableError(error);

        if (shouldRetry) {
            state.getMetadata()
                .put(retryKey, currentRetries + 1);
            logger.info("Retrying node: " + nodeName + " (attempt " + (currentRetries + 1) + "/" + MAX_RETRIES + ")");
        } else {
            logger.info("Non-retryable error for node: " + nodeName + " - " + error.getClass()
                .getSimpleName());
        }

        return shouldRetry;
    }

    private boolean isRetryableError(Exception error) {
        if (error instanceof java.net.SocketTimeoutException || error instanceof java.net.ConnectException || error instanceof java.io.IOException) {
            return true;
        }

        if (error.getMessage() != null) {
            String message = error.getMessage()
                .toLowerCase();
            if (message.contains("timeout") || message.contains("connection reset") || message.contains("service unavailable") ||
                message.contains("rate limit")) {
                return true;
            }
        }

        return false;
    }

    public boolean isValidTransition(String fromNode, String toNode) {
        if (fromNode == null || toNode == null) {
            return false;
        }

        Map<String, List<String>> validTransitions = Map.of("start", List.of("query_analysis"), "query_analysis",
            List.of("citation_fetch", "reasoning_selection"), "citation_fetch", List.of("reasoning_selection"), "reasoning_selection",
            List.of("reasoning_execution"), "reasoning_execution", List.of("end", "reasoning_selection"), // Allow retry
            "end", List.of());

        return validTransitions.getOrDefault(fromNode, List.of())
            .contains(toNode);
    }

    public void validateRouting(ResearchAgentState state) {
        if (state == null) {
            throw new IllegalStateException("State cannot be null for routing validation");
        }

        List<String> requiredNodes = List.of("query_analysis", "citation_fetch", "reasoning_selection", "reasoning_execution");

        for (String requiredNode : requiredNodes) {
            if (!nodes.containsKey(requiredNode)) {
                throw new IllegalStateException("Required node not registered: " + requiredNode);
            }
        }

        logger.info("Routing validation passed for session: " + state.getSessionId());
    }

    public void resetRetryCount(ResearchAgentState state, String nodeName) {
        if (state != null && nodeName != null) {
            String retryKey = nodeName + RETRY_COUNT_KEY;
            state.getMetadata()
                .remove(retryKey);
            logger.info("Reset retry count for node: " + nodeName);
        }
    }

    public int getRetryCount(ResearchAgentState state, String nodeName) {
        if (state == null || nodeName == null) {
            return 0;
        }

        String retryKey = nodeName + RETRY_COUNT_KEY;
        return (Integer) state.getMetadata()
            .getOrDefault(retryKey, 0);
    }
}