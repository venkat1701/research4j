package com.github.bhavuklabs.pipeline.langgraph.nodes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bsc.langgraph4j.action.AsyncNodeAction;

import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.pipeline.langgraph.LangGraphState;
import com.github.bhavuklabs.pipeline.models.QueryAnalysis;

public class LangGraphQueryAnalysisNode implements AsyncNodeAction<LangGraphState> {

    private static final Logger logger = Logger.getLogger(LangGraphQueryAnalysisNode.class.getName());

    private final LLMClient llmClient;

    public LangGraphQueryAnalysisNode(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(LangGraphState state) {
        try {
            logger.info("Starting LangGraph4j query analysis for session: " + state.getSessionId());

            String query = state.getQuery();
            if (query == null || query.trim()
                .isEmpty()) {
                throw new IllegalArgumentException("Query cannot be null or empty");
            }

            String analysisPrompt = buildAnalysisPrompt(state);
            LLMResponse<QueryAnalysis> response = llmClient.complete(analysisPrompt, QueryAnalysis.class);

            QueryAnalysis analysis;
            if (response != null && response.structuredOutput() != null) {
                analysis = response.structuredOutput();
                validateAndEnhanceAnalysis(analysis, state);
            } else {
                logger.warning("LLM returned null analysis, using fallback");
                analysis = createFallbackAnalysis(state);
            }

            logger.info("Query analysis completed - Intent: " + analysis.intent + ", Complexity: " + analysis.complexityScore + ", Citations needed: " +
                analysis.requiresCitations);

            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.QUERY_ANALYSIS, analysis));

        } catch (Exception e) {
            logger.severe("Query analysis failed: " + e.getMessage());

            QueryAnalysis fallbackAnalysis = createFallbackAnalysis(state);
            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.QUERY_ANALYSIS, fallbackAnalysis));
        }
    }

    private String buildAnalysisPrompt(LangGraphState state) {
        Object userProfile = state.getUserProfile();
        String domain = "general";
        String expertise = "intermediate";

        if (userProfile != null) {

        }

        return String.format("""
            Analyze the following research query and provide a structured assessment:
            
            Query: "%s"
            User Domain: %s
            User Expertise: %s
            
            Provide analysis in JSON format with these fields:
            - intent: one of "research", "comparison", "explanation", "creative", "analysis"
            - complexityScore: integer from 1-10
            - topics: array of relevant topic strings
            - requiresCitations: boolean indicating if external sources are needed
            - estimatedTime: string like "1-2 minutes"
            - suggestedReasoning: one of "CHAIN_OF_THOUGHT", "CHAIN_OF_TABLE", "CHAIN_OF_IDEAS"
            
            Return only valid JSON without additional formatting.
            """, state.getQuery()
            .replace("\"", "\\\""), domain, expertise);
    }

    private QueryAnalysis createFallbackAnalysis(LangGraphState state) {
        String query = state.getQuery()
            .toLowerCase();
        QueryAnalysis analysis = new QueryAnalysis();

        analysis.intent = detectIntent(query);
        analysis.complexityScore = calculateComplexity(query);
        analysis.topics = extractBasicTopics(query);
        analysis.requiresCitations = shouldRequireCitations(query);
        analysis.estimatedTime = estimateTime(analysis.complexityScore);
        analysis.suggestedReasoning = selectReasoning(analysis.intent);

        return analysis;
    }

    private String detectIntent(String query) {
        if (query.contains("compare") || query.contains("vs") || query.contains("difference")) {
            return "comparison";
        }
        if (query.contains("how to") || query.contains("tutorial") || query.contains("guide")) {
            return "creative";
        }
        if (query.contains("analyze") || query.contains("evaluate") || query.contains("assess")) {
            return "analysis";
        }
        if (query.contains("what is") || query.contains("explain") || query.contains("define")) {
            return "explanation";
        }
        return "research";
    }

    private int calculateComplexity(String query) {
        int score = 3;
        if (query.length() > 200) {
            score += 2;
        }
        if (query.split("\\s+").length > 15) {
            score += 1;
        }
        if (query.matches(".*\\b(algorithm|architecture|implementation)\\b.*")) {
            score += 2;
        }
        return Math.min(10, Math.max(1, score));
    }

    private List<String> extractBasicTopics(String query) {
        List<String> topics = new java.util.ArrayList<>();
        if (query.matches(".*\\b(java|python|programming|software)\\b.*")) {
            topics.add("software development");
        }
        if (query.matches(".*\\b(ai|machine learning|neural network)\\b.*")) {
            topics.add("artificial intelligence");
        }
        if (query.matches(".*\\b(business|market|strategy)\\b.*")) {
            topics.add("business");
        }
        return topics.isEmpty() ? List.of("general") : topics;
    }

    private boolean shouldRequireCitations(String query) {
        return query.length() > 50 || query.matches(".*\\b(research|study|evidence|data|statistics)\\b.*");
    }

    private String estimateTime(int complexity) {
        return switch (complexity) {
            case 1, 2, 3 -> "30 seconds - 1 minute";
            case 4, 5 -> "1-2 minutes";
            case 6, 7 -> "2-3 minutes";
            default -> "3-5 minutes";
        };
    }

    private String selectReasoning(String intent) {
        return switch (intent) {
            case "comparison" -> "CHAIN_OF_TABLE";
            case "creative" -> "CHAIN_OF_IDEAS";
            default -> "CHAIN_OF_THOUGHT";
        };
    }

    private void validateAndEnhanceAnalysis(QueryAnalysis analysis, LangGraphState state) {
        if (analysis.intent == null) {
            analysis.intent = detectIntent(state.getQuery()
                .toLowerCase());
        }
        if (analysis.complexityScore < 1 || analysis.complexityScore > 10) {
            analysis.complexityScore = calculateComplexity(state.getQuery()
                .toLowerCase());
        }
        if (analysis.topics == null || analysis.topics.isEmpty()) {
            analysis.topics = extractBasicTopics(state.getQuery()
                .toLowerCase());
        }
    }
}