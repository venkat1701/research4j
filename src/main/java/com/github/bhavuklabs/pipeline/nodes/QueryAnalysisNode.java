package com.github.bhavuklabs.pipeline.nodes;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.pipeline.graph.GraphNode;
import com.github.bhavuklabs.pipeline.models.QueryAnalysis;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;

public class QueryAnalysisNode implements GraphNode<ResearchAgentState> {

    private static final Logger logger = Logger.getLogger(QueryAnalysisNode.class.getName());

    private final LLMClient llmClient;

    public QueryAnalysisNode(LLMClient llmClient) {
        if (llmClient == null) {
            throw new IllegalArgumentException("LLM client cannot be null");
        }
        this.llmClient = llmClient;
    }

    @Override
    public CompletableFuture<ResearchAgentState> process(ResearchAgentState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Starting query analysis for session: " + state.getSessionId());

                String analysisPrompt = buildComprehensiveAnalysisPrompt(state);
                LLMResponse<QueryAnalysis> analysisResponse = llmClient.complete(analysisPrompt, QueryAnalysis.class);

                if (analysisResponse == null || analysisResponse.structuredOutput() == null) {
                    logger.warning("LLM returned null analysis, using enhanced fallback");
                    QueryAnalysis fallbackAnalysis = createEnhancedFallbackAnalysis(state);
                    return state.withQueryAnalysis(fallbackAnalysis);
                }

                QueryAnalysis analysis = analysisResponse.structuredOutput();
                validateAndEnhanceAnalysis(analysis, state);
                logger.info("Query analysis completed - Intent: " + analysis.intent + ", Complexity: " + analysis.complexityScore + ", Citations needed: " +
                    analysis.requiresCitations);

                return state.withQueryAnalysis(analysis);

            } catch (Exception e) {
                logger.warning("Query analysis failed, using enhanced fallback: " + e.getMessage());
                QueryAnalysis fallbackAnalysis = createEnhancedFallbackAnalysis(state);
                return state.withQueryAnalysis(fallbackAnalysis);
            }
        });
    }

    private String buildComprehensiveAnalysisPrompt(ResearchAgentState state) {
        String userDomain = state.getUserProfile() != null ? state.getUserProfile()
            .getDomain() : "general";
        String userExpertise = state.getUserProfile() != null ? state.getUserProfile()
            .getExpertiseLevel() : "intermediate";
        String userPrefs = state.getUserProfile() != null && state.getUserProfile()
            .getPreferences() != null ? String.join(", ", state.getUserProfile()
            .getPreferences()) : "balanced, comprehensive";

        return String.format("""
            You are an expert research query analyst with deep expertise in information science, cognitive psychology, and AI-assisted research methodologies. Your task is to perform comprehensive analysis of research queries to optimize processing strategies and resource allocation.
            
            QUERY TO ANALYZE:
            "%s"
            
            USER CONTEXT:
            - Domain Expertise: %s
            - Skill Level: %s  
            - Research Preferences: %s
            
            ANALYSIS FRAMEWORK:
            You must analyze this query across multiple dimensions and provide structured output in EXACTLY this JSON format:
            
            {
                "intent": "[INTENT_TYPE]",
                "complexityScore": [1-10],
                "topics": ["topic1", "topic2", "topic3"],
                "requiresCitations": [true/false],
                "estimatedTime": "[TIME_ESTIMATE]",
                "suggestedReasoning": "[REASONING_METHOD]"
            }
            
            INTENT CLASSIFICATION GUIDELINES:
            - "research": Seeking comprehensive information, understanding, or knowledge synthesis
            - "comparison": Evaluating differences, similarities, or relative merits between entities
            - "explanation": Requesting detailed clarification of concepts, processes, or phenomena
            - "creative": Generating ideas, brainstorming, or innovative problem-solving
            - "analysis": Deep examination, evaluation, or critical assessment of topics
            
            COMPLEXITY SCORING CRITERIA (1-10):
            1-2: Basic factual queries requiring simple lookups
            3-4: Straightforward questions with clear, established answers
            5-6: Moderate complexity requiring synthesis of multiple sources
            7-8: Advanced topics requiring deep analysis and expert knowledge
            9-10: Highly complex, multi-faceted questions requiring extensive research
            
            COMPLEXITY ASSESSMENT FACTORS:
            - Technical depth and specialization required
            - Number of concepts that need integration
            - Ambiguity and interpretation requirements  
            - Cross-disciplinary knowledge needs
            - Current vs historical information requirements
            - Subjective vs objective answer nature
            
            TOPIC EXTRACTION GUIDELINES:
            - Identify 2-5 main topics/domains relevant to the query
            - Use specific, searchable terms rather than generic categories
            - Consider both explicit and implicit topics
            - Include technical terminology when appropriate
            - Prioritize topics that would benefit from citation support
            
            CITATION REQUIREMENTS DECISION LOGIC:
            Citations ARE required when:
            - Query asks for current/recent information or statistics
            - Topic involves disputed or evolving knowledge
            - User requests evidence, sources, or verification
            - Complex analysis benefiting from authoritative sources
            - Comparison requiring factual data points
            - Technical topics where accuracy is critical
            
            Citations are NOT required when:
            - Query is purely creative or hypothetical
            - Asking for general explanations of well-established concepts
            - Personal opinion or subjective assessment requests
            - Simple definitional queries
            - Mathematical or logical problems
            
            TIME ESTIMATION GUIDELINES:
            - "30 seconds - 1 minute": Simple factual or definitional queries
            - "1-2 minutes": Standard research with moderate complexity
            - "2-3 minutes": Complex analysis requiring multiple sources
            - "3-5 minutes": Comprehensive research with extensive synthesis
            - "5+ minutes": Highly complex, multi-faceted research projects
            
            REASONING METHOD SELECTION:
            - "CHAIN_OF_THOUGHT": Best for analytical, explanatory, and research queries requiring step-by-step reasoning
            - "CHAIN_OF_TABLE": Optimal for comparisons, data analysis, and structured information presentation
            - "CHAIN_OF_IDEAS": Ideal for creative, brainstorming, and innovative problem-solving queries
            - "TREE_OF_THOUGHT": Reserved for extremely complex multi-path reasoning (use sparingly)
            
            ADDITIONAL ANALYSIS CONSIDERATIONS:
            - User expertise level should influence complexity assessment
            - Domain context affects topic relevance and citation needs
            - Research preferences guide reasoning method selection
            - Query ambiguity may require multiple interpretation paths
            
            CRITICAL REQUIREMENTS:
            1. Return ONLY valid JSON - no additional text, explanations, or formatting
            2. Ensure all field names match exactly as specified
            3. Use only the specified values for categorical fields
            4. Provide realistic and actionable recommendations
            5. Consider user context in all assessment decisions
            
            Analyze the query now and return the structured JSON analysis:
            """, state.getQuery()
            .replace("\"", "\\\""), userDomain, userExpertise, userPrefs);
    }

    private QueryAnalysis createEnhancedFallbackAnalysis(ResearchAgentState state) {
        String query = state.getQuery()
            .toLowerCase();
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.intent = detectIntent(query);
        analysis.complexityScore = calculateComplexityScore(query, state);
        analysis.topics = extractTopics(query, state);
        analysis.requiresCitations = shouldRequireCitations(query, analysis.intent, state);
        analysis.estimatedTime = estimateProcessingTime(analysis.complexityScore, analysis.requiresCitations);
        analysis.suggestedReasoning = selectReasoningMethod(query, analysis.intent, state);

        logger.info("Enhanced fallback analysis created: " + analysis.intent + " (complexity: " + analysis.complexityScore + ")");
        return analysis;
    }

    private String detectIntent(String query) {
        if (query.contains("compare") || query.contains("versus") || query.contains("vs") || query.contains("difference") || query.contains("better") ||
            query.contains("pros and cons")) {
            return "comparison";
        }

        if (query.contains("how to") || query.contains("tutorial") || query.contains("guide") || query.contains("step by step") ||
            query.contains("implementation")) {
            return "creative";
        }

        if (query.contains("analyze") || query.contains("evaluate") || query.contains("assess") || query.contains("best practices") ||
            query.contains("architecture")) {
            return "analysis";
        }

        if (query.contains("what is") || query.contains("explain") || query.contains("how does") || query.contains("why") || query.contains("define")) {
            return "explanation";
        }

        return "research";
    }

    private int calculateComplexityScore(String query, ResearchAgentState state) {
        int score = 3;

        if (query.length() > 200) {
            score += 2;
        } else if (query.length() > 100) {
            score += 1;
        }

        if (query.matches(".*\\b(algorithm|implementation|architecture|framework|methodology)\\b.*")) {
            score += 2;
        }
        if (query.matches(".*\\b(optimization|performance|scalability|security|integration)\\b.*")) {
            score += 1;
        }

        long conceptCount = query.split("\\band\\b|\\bor\\b|,").length;
        if (conceptCount > 3) {
            score += 2;
        } else if (conceptCount > 2) {
            score += 1;
        }

        if (query.matches(".*\\b(comprehensive|detailed|in-depth|thorough|extensive)\\b.*")) {
            score += 1;
        }
        if (query.matches(".*\\b(latest|recent|current|modern|state-of-the-art)\\b.*")) {
            score += 1;
        }

        if (state.getUserProfile() != null) {
            String expertise = state.getUserProfile()
                .getExpertiseLevel();
            if ("expert".equals(expertise)) {
                score += 1;
            } else if ("beginner".equals(expertise)) {
                score = Math.max(1, score - 1);
            }
        }

        return Math.min(10, Math.max(1, score));
    }

    private List<String> extractTopics(String query, ResearchAgentState state) {
        List<String> topics = new java.util.ArrayList<>();

        if (query.matches(".*\\b(java|python|javascript|react|angular|vue|spring|microservices)\\b.*")) {
            topics.add("software development");
        }
        if (query.matches(".*\\b(machine learning|ai|artificial intelligence|neural network|deep learning)\\b.*")) {
            topics.add("artificial intelligence");
        }
        if (query.matches(".*\\b(cloud|aws|azure|kubernetes|docker|devops)\\b.*")) {
            topics.add("cloud computing");
        }

        if (query.matches(".*\\b(market|business|strategy|finance|investment|startup)\\b.*")) {
            topics.add("business");
        }
        if (query.matches(".*\\b(management|leadership|productivity|workflow|process)\\b.*")) {
            topics.add("management");
        }

        if (query.matches(".*\\b(research|study|paper|publication|academic|scientific)\\b.*")) {
            topics.add("academic research");
        }

        if (state.getUserProfile() != null && state.getUserProfile()
            .getDomain() != null) {
            String domain = state.getUserProfile()
                .getDomain();
            if (!domain.equals("general") && !topics.contains(domain)) {
                topics.add(domain);
            }
        }

        if (topics.isEmpty()) {
            topics.add("general knowledge");
            if (query.length() > 50) {
                topics.add("comprehensive analysis");
            }
        }

        return topics;
    }

    private boolean shouldRequireCitations(String query, String intent, ResearchAgentState state) {

        if ("comparison".equals(intent) || "analysis".equals(intent)) {
            return true;
        }

        if ("creative".equals(intent)) {
            return false;
        }

        if (query.matches(".*\\b(latest|recent|current|statistics|data|evidence|sources|studies)\\b.*")) {
            return true;
        }

        if (query.matches(".*\\b(how many|what percentage|according to|research shows|studies indicate)\\b.*")) {
            return true;
        }

        if (query.length() > 100 || query.split("\\s+").length > 15) {
            return true;
        }

        if (state.getUserProfile() != null && "expert".equals(state.getUserProfile()
            .getExpertiseLevel())) {
            return true;
        }

        return true;
    }

    private String estimateProcessingTime(int complexityScore, boolean requiresCitations) {
        int baseTime = complexityScore <= 3 ? 1 : complexityScore <= 6 ? 2 : complexityScore <= 8 ? 3 : 4;

        if (requiresCitations) {
            baseTime += 1;
        }

        return switch (baseTime) {
            case 1 -> "30 seconds - 1 minute";
            case 2 -> "1-2 minutes";
            case 3 -> "2-3 minutes";
            case 4 -> "3-4 minutes";
            default -> "4-5 minutes";
        };
    }

    private String selectReasoningMethod(String query, String intent, ResearchAgentState state) {
        switch (intent) {
            case "comparison" -> {
                return "CHAIN_OF_TABLE";
            }
            case "creative" -> {
                return "CHAIN_OF_IDEAS";
            }
            case "analysis", "research", "explanation" -> {
                return "CHAIN_OF_THOUGHT";
            }
        }
        if (query.matches(".*\\b(table|chart|data|statistics|compare|versus)\\b.*")) {
            return "CHAIN_OF_TABLE";
        }
        if (query.matches(".*\\b(creative|brainstorm|generate|design|ideas?)\\b.*")) {
            return "CHAIN_OF_IDEAS";
        }
        if (state.getUserProfile() != null && state.getUserProfile()
            .getPreferences() != null) {
            List<String> prefs = state.getUserProfile()
                .getPreferences();
            if (prefs.contains("visual") || prefs.contains("structured")) {
                return "CHAIN_OF_TABLE";
            }
            if (prefs.contains("creative") || prefs.contains("innovative")) {
                return "CHAIN_OF_IDEAS";
            }
        }

        return "CHAIN_OF_THOUGHT";
    }

    private void validateAndEnhanceAnalysis(QueryAnalysis analysis, ResearchAgentState state) {

        if (analysis.intent == null || !isValidIntent(analysis.intent)) {
            analysis.intent = detectIntent(state.getQuery()
                .toLowerCase());
        }
        if (analysis.complexityScore < 1 || analysis.complexityScore > 10) {
            analysis.complexityScore = calculateComplexityScore(state.getQuery()
                .toLowerCase(), state);
        }
        if (analysis.topics == null || analysis.topics.isEmpty()) {
            analysis.topics = extractTopics(state.getQuery()
                .toLowerCase(), state);
        }
        if (analysis.suggestedReasoning == null || !isValidReasoningMethod(analysis.suggestedReasoning)) {
            analysis.suggestedReasoning = selectReasoningMethod(state.getQuery()
                .toLowerCase(), analysis.intent, state);
        }
        if (analysis.estimatedTime == null || analysis.estimatedTime.trim()
            .isEmpty()) {
            analysis.estimatedTime = estimateProcessingTime(analysis.complexityScore, analysis.requiresCitations);
        }
    }

    private boolean isValidIntent(String intent) {
        return List.of("research", "comparison", "explanation", "creative", "analysis")
            .contains(intent);
    }

    private boolean isValidReasoningMethod(String method) {
        return List.of("CHAIN_OF_THOUGHT", "CHAIN_OF_TABLE", "CHAIN_OF_IDEAS", "TREE_OF_THOUGHT")
            .contains(method);
    }

    @Override
    public String getName() {
        return "query_analysis";
    }

    @Override
    public boolean shouldExecute(ResearchAgentState state) {
        boolean shouldExecute = state != null && state.getQuery() != null && !state.getQuery()
            .trim()
            .isEmpty() && !state.getMetadata()
            .containsKey("query_analysis");

        if (!shouldExecute) {
            logger.info("Query analysis node skipped - conditions not met or already completed");
        }

        return shouldExecute;
    }
}