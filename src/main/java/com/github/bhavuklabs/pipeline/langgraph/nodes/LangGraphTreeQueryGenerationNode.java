package com.github.bhavuklabs.pipeline.langgraph.nodes;

import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.deepresearch.models.ResearchNode;
import com.github.bhavuklabs.deepresearch.models.ResearchTree;
import com.github.bhavuklabs.deepresearch.websocket.ProgressWebSocketBroadcaster;
import com.github.bhavuklabs.pipeline.langgraph.EnhancedLangGraphState;

import org.bsc.langgraph4j.action.AsyncNodeAction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

/**
 * LangGraph4j node for generating search queries in a tree-based deep research flow.
 * This node creates child nodes with specific research queries based on the current node's context.
 */
public class LangGraphTreeQueryGenerationNode implements AsyncNodeAction<EnhancedLangGraphState> {

    private static final Logger logger = Logger.getLogger(LangGraphTreeQueryGenerationNode.class.getName());

    private final LLMClient llmClient;

    // Query generation prompt template
    private static final String QUERY_GENERATION_PROMPT = """
        You are a research assistant tasked with generating specific search queries for deep research.
        
        Current Research Context:
        - Original Query: {originalQuery}
        - Current Research Goal: {researchGoal}
        - Research Depth: {currentDepth} of {maxDepth}
        - Breadth Required: {breadthPerLevel} queries
        - Previous Learnings: {previousLearnings}
        
        Based on the research context, generate {breadthPerLevel} specific and focused search queries that will help answer the research goal.
        Each query should:
        1. Be specific and actionable
        2. Build upon previous learnings
        3. Explore different aspects or angles of the research goal
        4. Be suitable for web search
        5. Avoid duplicating previous searches
        
        For each query, also provide a brief research goal explaining what aspect you're trying to research.
        
        Respond with a JSON array in this exact format:
        {
          "queries": [
            {
              "query": "specific search query here",
              "researchGoal": "what this query aims to discover"
            }
          ]
        }
        
        Important: Generate exactly {breadthPerLevel} unique, high-quality queries.
        """;

    public LangGraphTreeQueryGenerationNode(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(EnhancedLangGraphState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting tree-based query generation for session: " + state.getSessionId());

                // Validate state
                state.validateTreeState();

                ResearchTree tree = state.getResearchTree();
                String currentNodeId = state.getCurrentNodeId();
                ResearchNode currentNode = state.getCurrentNode();

                if (currentNode == null) {
                    throw new IllegalStateException("No current node found for query generation");
                }

                // Check if we've reached maximum depth
                EnhancedLangGraphState.TreeConfig treeConfig = state.getTreeConfig();
                if (treeConfig != null) {
                    int currentDepth = tree.getNodeDepth(currentNodeId);
                    if (currentDepth >= treeConfig.getMaxDepth()) {
                        logger.info("Maximum depth reached for node " + currentNodeId + ", skipping query generation");
                        return createSkipResult("Maximum depth reached");
                    }
                }

                // Broadcast start of query generation
                state.broadcastProgress("generating_query_reasoning", Map.of(
                    "nodeId", currentNodeId,
                    "status", "starting"
                ));

                // Generate queries using LLM
                List<EnhancedLangGraphState.GeneratedQuery> generatedQueries = generateQueries(
                    state, tree, currentNode, treeConfig);

                // Create child nodes for each generated query
                List<ResearchNode> childNodes = createChildNodes(tree, currentNode, generatedQueries);

                // Update state with generated queries
                state.setGeneratedQueries(generatedQueries);

                // Broadcast generated queries
                for (EnhancedLangGraphState.GeneratedQuery query : generatedQueries) {
                    state.broadcastNodeProgress(query.getNodeId(), "generating_query", Map.of(
                        "nodeId", query.getNodeId(),
                        "parentNodeId", query.getParentNodeId(),
                        "query", query.getQuery(),
                        "researchGoal", query.getResearchGoal()
                    ));
                }

                logger.info("Generated " + generatedQueries.size() + " queries for node " + currentNodeId);

                return createSuccessResult(generatedQueries, childNodes);

            } catch (Exception e) {
                logger.severe("Error in tree query generation: " + e.getMessage());
                return handleError(state, e);
            }
        });
    }

    private List<EnhancedLangGraphState.GeneratedQuery> generateQueries(
        EnhancedLangGraphState state, ResearchTree tree, ResearchNode currentNode,
        EnhancedLangGraphState.TreeConfig treeConfig) {

        try {
            // Prepare context for query generation
            String originalQuery = tree.getRootQuery();
            String researchGoal = currentNode.getResearchGoal();
            int currentDepth = tree.getNodeDepth(currentNode.getId());
            int maxDepth = treeConfig != null ? treeConfig.getMaxDepth() : 3;
            int breadthPerLevel = treeConfig != null ? treeConfig.getBreadthPerLevel() : 3;

            // Gather previous learnings from the current path
            List<String> previousLearnings = collectPreviousLearnings(tree, currentNode);

            // Build the prompt
            String prompt = QUERY_GENERATION_PROMPT
                .replace("{originalQuery}", originalQuery)
                .replace("{researchGoal}", researchGoal != null ? researchGoal : originalQuery)
                .replace("{currentDepth}", String.valueOf(currentDepth))
                .replace("{maxDepth}", String.valueOf(maxDepth))
                .replace("{breadthPerLevel}", String.valueOf(breadthPerLevel))
                .replace("{previousLearnings}", String.join(", ", previousLearnings));

            // Stream reasoning updates
            StringBuilder reasoningBuffer = new StringBuilder();

            // Make LLM call with streaming
            LLMResponse response = llmClient.complete(prompt, (chunk) -> {
                // Handle streaming reasoning
                if (chunk != null && chunk.contains("reasoning")) {
                    String delta = extractReasoningDelta(chunk);
                    if (delta != null && !delta.isEmpty()) {
                        reasoningBuffer.append(delta);
                        currentNode.appendGenerateQueriesReasoning(delta);

                        // Broadcast reasoning delta
                        state.broadcastNodeProgress(currentNode.getId(), "generating_query_reasoning", Map.of(
                            "nodeId", currentNode.getId(),
                            "delta", delta
                        ));
                    }
                }
            });

            // Parse the response JSON
            List<EnhancedLangGraphState.GeneratedQuery> queries = parseQueryResponse(
                response.getContent(), currentNode.getId(), breadthPerLevel);

            logger.info("Successfully generated " + queries.size() + " queries for node " + currentNode.getId());
            return queries;

        } catch (Exception e) {
            logger.severe("Failed to generate queries: " + e.getMessage());
            throw new RuntimeException("Query generation failed", e);
        }
    }

    private List<String> collectPreviousLearnings(ResearchTree tree, ResearchNode currentNode) {
        List<String> learnings = new ArrayList<>();

        // Get the path from root to current node
        List<ResearchNode> path = tree.getPath(currentNode.getId());

        // Collect learnings from all nodes in the path
        for (ResearchNode node : path) {
            for (ResearchNode.ProcessedLearning learning : node.getLearnings()) {
                if (learning.getLearning() != null && !learning.getLearning().isEmpty()) {
                    learnings.add(learning.getLearning());
                }
            }
        }

        // Limit to most recent/relevant learnings to avoid prompt bloat
        return learnings.size() > 10 ? learnings.subList(learnings.size() - 10, learnings.size()) : learnings;
    }

    private List<EnhancedLangGraphState.GeneratedQuery> parseQueryResponse(String responseContent,
        String parentNodeId,
        int expectedCount) {
        List<EnhancedLangGraphState.GeneratedQuery> queries = new ArrayList<>();

        try {
            // Simple JSON parsing for the response
            // In a production system, you'd want to use a proper JSON library like Jackson
            String jsonContent = extractJsonFromResponse(responseContent);

            if (jsonContent == null) {
                throw new RuntimeException("No valid JSON found in LLM response");
            }

            // Parse queries from JSON (simplified implementation)
            List<Map<String, String>> parsedQueries = parseQueriesJson(jsonContent);

            for (int i = 0; i < parsedQueries.size() && i < expectedCount; i++) {
                Map<String, String> queryData = parsedQueries.get(i);
                String childNodeId = EnhancedLangGraphState.generateChildNodeId(parentNodeId, i);

                String query = queryData.get("query");
                String researchGoal = queryData.get("researchGoal");

                if (query != null && !query.trim().isEmpty() && !"undefined".equals(query)) {
                    queries.add(new EnhancedLangGraphState.GeneratedQuery(
                        childNodeId, query.trim(), researchGoal, parentNodeId));
                }
            }

        } catch (Exception e) {
            logger.warning("Failed to parse query response, using fallback: " + e.getMessage());
            // Fallback: create generic queries
            queries = createFallbackQueries(parentNodeId, expectedCount);
        }

        return queries;
    }

    private List<ResearchNode> createChildNodes(ResearchTree tree, ResearchNode parentNode,
        List<EnhancedLangGraphState.GeneratedQuery> queries) {
        List<ResearchNode> childNodes = new ArrayList<>();

        for (EnhancedLangGraphState.GeneratedQuery query : queries) {
            try {
                ResearchNode childNode = tree.addChildNode(
                    parentNode.getId(),
                    query.getNodeId(),
                    query.getQuery(),
                    query.getResearchGoal()
                );

                childNodes.add(childNode);
                logger.fine("Created child node: " + childNode.getId() + " with query: " + query.getQuery());

            } catch (Exception e) {
                logger.warning("Failed to create child node for query: " + query.getQuery() + " - " + e.getMessage());
            }
        }

        return childNodes;
    }

    private String extractJsonFromResponse(String response) {
        if (response == null) return null;

        // Look for JSON content between ```json and ``` or { and }
        int jsonStart = response.indexOf("{");
        int jsonEnd = response.lastIndexOf("}");

        if (jsonStart >= 0 && jsonEnd > jsonStart) {
            return response.substring(jsonStart, jsonEnd + 1);
        }

        return null;
    }

    private List<Map<String, String>> parseQueriesJson(String json) {
        List<Map<String, String>> queries = new ArrayList<>();

        try {
            // Simple JSON parsing implementation
            // In production, use Jackson or another proper JSON library

            if (json.contains("\"queries\"")) {
                String queriesSection = extractQueriesSection(json);
                queries = extractQueryObjects(queriesSection);
            }

        } catch (Exception e) {
            logger.warning("JSON parsing failed: " + e.getMessage());
        }

        return queries;
    }

    private String extractQueriesSection(String json) {
        int start = json.indexOf("\"queries\"");
        if (start < 0) return "";

        int arrayStart = json.indexOf("[", start);
        if (arrayStart < 0) return "";

        int arrayEnd = json.lastIndexOf("]");
        if (arrayEnd < arrayStart) return "";

        return json.substring(arrayStart + 1, arrayEnd);
    }

    private List<Map<String, String>> extractQueryObjects(String queriesSection) {
        List<Map<String, String>> queries = new ArrayList<>();

        // Split by objects (simplified approach)
        String[] objects = queriesSection.split("\\},\\s*\\{");

        for (String obj : objects) {
            obj = obj.replace("{", "").replace("}", "").trim();
            if (obj.isEmpty()) continue;

            Map<String, String> queryMap = new HashMap<>();
            String[] pairs = obj.split(",");

            for (String pair : pairs) {
                String[] keyValue = pair.split(":");
                if (keyValue.length == 2) {
                    String key = keyValue[0].trim().replace("\"", "");
                    String value = keyValue[1].trim().replace("\"", "");
                    queryMap.put(key, value);
                }
            }

            if (queryMap.containsKey("query")) {
                queries.add(queryMap);
            }
        }

        return queries;
    }

    private List<EnhancedLangGraphState.GeneratedQuery> createFallbackQueries(String parentNodeId, int count) {
        List<EnhancedLangGraphState.GeneratedQuery> fallbackQueries = new ArrayList<>();

        for (int i = 0; i < count; i++) {
            String childNodeId = EnhancedLangGraphState.generateChildNodeId(parentNodeId, i);
            String query = "Research query " + (i + 1);
            String researchGoal = "Explore aspect " + (i + 1) + " of the research topic";

            fallbackQueries.add(new EnhancedLangGraphState.GeneratedQuery(
                childNodeId, query, researchGoal, parentNodeId));
        }

        return fallbackQueries;
    }

    private String extractReasoningDelta(String chunk) {
        // Extract reasoning content from streaming chunk
        if (chunk == null) return null;

        // This is a simplified implementation
        // In practice, you'd parse the actual streaming JSON structure
        if (chunk.contains("reasoning")) {
            return chunk; // Return the entire chunk for now
        }

        return null;
    }

    private Map<String, Object> createSuccessResult(List<EnhancedLangGraphState.GeneratedQuery> queries,
        List<ResearchNode> childNodes) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("generatedQueries", queries);
        result.put("childNodes", childNodes);
        result.put("nextAction", "search"); // Next step is to search for each query
        return result;
    }

    private Map<String, Object> createSkipResult(String reason) {
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("skipped", true);
        result.put("reason", reason);
        result.put("nextAction", "complete"); // Skip to completion
        return result;
    }

    private Map<String, Object> handleError(EnhancedLangGraphState state, Exception e) {
        String currentNodeId = state.getCurrentNodeId();
        String errorMessage = e.getMessage();

        // Update node with error
        ResearchNode currentNode = state.getCurrentNode();
        if (currentNode != null) {
            currentNode.setError(errorMessage);
            state.setNodeError(currentNodeId, errorMessage);
        }

        // Broadcast error
        state.broadcastError(currentNodeId, errorMessage);

        Map<String, Object> result = new HashMap<>();
        result.put("success", false);
        result.put("error", errorMessage);
        result.put("nodeId", currentNodeId);
        return result;
    }
}