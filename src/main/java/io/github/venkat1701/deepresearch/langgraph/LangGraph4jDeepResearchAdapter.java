
package io.github.venkat1701.deepresearch.langgraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.DeepResearchResult;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;
import io.github.venkat1701.exceptions.Research4jException;
import io.github.venkat1701.pipeline.profile.UserProfile;


public class LangGraph4jDeepResearchAdapter {

    private static final Logger logger = Logger.getLogger(LangGraph4jDeepResearchAdapter.class.getName());

    private final LLMClient llmClient;
    private final CitationService citationService;
    private final LangGraph4jGraphBuilder graphBuilder;
    private final LangGraph4jStateManager stateManager;

    public LangGraph4jDeepResearchAdapter(LLMClient llmClient, CitationService citationService) {
        this.llmClient = llmClient;
        this.citationService = citationService;
        this.graphBuilder = new LangGraph4jGraphBuilder();
        this.stateManager = new LangGraph4jStateManager();

        logger.info("LangGraph4j Deep Research Adapter initialized");
    }

    
    public CompletableFuture<DeepResearchResult> executeOpenDeepResearch(
        String query,
        UserProfile userProfile,
        DeepResearchConfig config) {

        logger.info("Starting LangGraph4j open deep research for: " + query);

        return CompletableFuture.supplyAsync(() -> {
            try {
                
                LangGraph4jResearchWorkflow workflow = createResearchWorkflow(config);

                
                LangGraph4jResearchState initialState = initializeResearchState(query, userProfile, config);

                
                LangGraph4jResearchState finalState = workflow.execute(initialState);

                
                return convertToDeepResearchResult(finalState);

            } catch (Exception e) {
                logger.severe("LangGraph4j open deep research failed: " + e.getMessage());
                throw new RuntimeException("Open deep research execution failed", e);
            }
        });
    }

    
    private LangGraph4jResearchWorkflow createResearchWorkflow(DeepResearchConfig config) {
        LangGraph4jWorkflowBuilder builder = new LangGraph4jWorkflowBuilder();

        
        builder.addNode("query_analysis", this::analyzeQuery)
            .addNode("question_generation", this::generateResearchQuestions)
            .addNode("parallel_research", this::executeParallelResearch)
            .addNode("citation_gathering", this::gatherCitations)
            .addNode("knowledge_synthesis", this::synthesizeKnowledge)
            .addNode("cross_validation", this::crossValidateFindings)
            .addNode("report_generation", this::generateFinalReport);

        
        builder.addEdge("query_analysis", "question_generation")
            .addConditionalEdge("question_generation", this::shouldContinueResearch,
                Map.of(
                    "continue", "parallel_research",
                    "insufficient", "question_generation",
                    "complete", "knowledge_synthesis"
                ))
            .addEdge("parallel_research", "citation_gathering")
            .addEdge("citation_gathering", "knowledge_synthesis")
            .addConditionalEdge("knowledge_synthesis", this::shouldCrossValidate,
                Map.of(
                    "validate", "cross_validation",
                    "complete", "report_generation"
                ))
            .addEdge("cross_validation", "report_generation");

        
        builder.setMaxIterations(getMaxIterations(config.getResearchDepth()))
            .setParallelism(getParallelismLevel(config.getResearchScope()))
            .setTimeout(config.getMaxDuration());

        return builder.build();
    }

    
    private LangGraph4jResearchState initializeResearchState(String query, UserProfile userProfile, DeepResearchConfig config) {
        LangGraph4jResearchState state = new LangGraph4jResearchState();

        state.setOriginalQuery(query);
        state.setUserProfile(userProfile);
        state.setConfig(config);
        state.setStartTime(java.time.Instant.now());
        state.setResearchQuestions(List.of());
        state.setCitations(List.of());
        state.setKnowledgeGraph(new HashMap<>());
        state.setValidationResults(new HashMap<>());
        state.setCurrentPhase("initialization");

        return state;
    }

    

    
    private LangGraph4jResearchState analyzeQuery(LangGraph4jResearchState state) {
        logger.info("LangGraph4j: Analyzing query - " + state.getOriginalQuery());

        try {
            
            String analysisPrompt = buildQueryAnalysisPrompt(state.getOriginalQuery(), state.getUserProfile());
            var analysisResult = llmClient.complete(analysisPrompt, Map.class);

            
            Map<String, Object> analysis = (Map<String, Object>) analysisResult.structuredOutput();
            state.setQueryAnalysis(analysis);
            state.setCurrentPhase("query_analyzed");

            
            int complexityScore = (Integer) analysis.getOrDefault("complexity", 5);
            state.setComplexityScore(complexityScore);

            logger.info("Query analysis completed - Complexity: " + complexityScore);

        } catch (Exception e) {
            logger.warning("Query analysis failed: " + e.getMessage());
            state.addError("query_analysis", e.getMessage());
        }

        return state;
    }

    
    private LangGraph4jResearchState generateResearchQuestions(LangGraph4jResearchState state) {
        logger.info("LangGraph4j: Generating research questions");

        try {
            
            List<ResearchQuestion> questions = generateAdaptiveQuestions(state);
            state.setResearchQuestions(questions);
            state.setCurrentPhase("questions_generated");

            logger.info("Generated " + questions.size() + " research questions");

        } catch (Exception e) {
            logger.warning("Question generation failed: " + e.getMessage());
            state.addError("question_generation", e.getMessage());
        }

        return state;
    }

    
    private LangGraph4jResearchState executeParallelResearch(LangGraph4jResearchState state) {
        logger.info("LangGraph4j: Executing parallel research");

        try {
            List<ResearchQuestion> questions = state.getResearchQuestions();

            
            List<CompletableFuture<List<CitationResult>>> researchTasks = questions.stream()
                .map(question -> CompletableFuture.supplyAsync(() -> {
                    try {
                        return citationService.search(question.getQuestion());
                    } catch (Exception e) {
                        logger.warning("Failed to research question: " + question.getQuestion());
                        return List.<CitationResult>of();
                    }
                }))
                .toList();

            
            CompletableFuture<Void> allTasks = CompletableFuture.allOf(
                researchTasks.toArray(new CompletableFuture[0]));

            allTasks.join(); 

            
            Map<String, List<CitationResult>> researchResults = new HashMap<>();
            for (int i = 0; i < questions.size(); i++) {
                ResearchQuestion question = questions.get(i);
                List<CitationResult> citations = researchTasks.get(i).join();
                researchResults.put(question.getQuestion(), citations);
            }

            state.setResearchResults(researchResults);
            state.setCurrentPhase("parallel_research_completed");

            logger.info("Parallel research completed for " + questions.size() + " questions");

        } catch (Exception e) {
            logger.warning("Parallel research failed: " + e.getMessage());
            state.addError("parallel_research", e.getMessage());
        }

        return state;
    }

    
    private LangGraph4jResearchState gatherCitations(LangGraph4jResearchState state) {
        logger.info("LangGraph4j: Gathering and enhancing citations");

        try {
            
            List<CitationResult> allCitations = state.getResearchResults().values().stream()
                .flatMap(List::stream)
                .distinct()
                .toList();

            
            List<CitationResult> enhancedCitations = enhanceCitationsWithLangGraph(allCitations, state);

            state.setCitations(enhancedCitations);
            state.setCurrentPhase("citations_gathered");

            logger.info("Gathered " + enhancedCitations.size() + " enhanced citations");

        } catch (Exception e) {
            logger.warning("Citation gathering failed: " + e.getMessage());
            state.addError("citation_gathering", e.getMessage());
        }

        return state;
    }

    
    private LangGraph4jResearchState synthesizeKnowledge(LangGraph4jResearchState state) {
        logger.info("LangGraph4j: Synthesizing knowledge");

        try {
            
            Map<String, Object> knowledgeGraph = buildKnowledgeGraph(state);
            state.setKnowledgeGraph(knowledgeGraph);

            
            String synthesis = generateKnowledgeSynthesis(state);
            state.setSynthesis(synthesis);
            state.setCurrentPhase("knowledge_synthesized");

            logger.info("Knowledge synthesis completed");

        } catch (Exception e) {
            logger.warning("Knowledge synthesis failed: " + e.getMessage());
            state.addError("knowledge_synthesis", e.getMessage());
        }

        return state;
    }

    
    private LangGraph4jResearchState crossValidateFindings(LangGraph4jResearchState state) {
        logger.info("LangGraph4j: Cross-validating findings");

        try {
            
            Map<String, Object> validationResults = performCrossValidation(state);
            state.setValidationResults(validationResults);
            state.setCurrentPhase("cross_validated");

            logger.info("Cross-validation completed");

        } catch (Exception e) {
            logger.warning("Cross-validation failed: " + e.getMessage());
            state.addError("cross_validation", e.getMessage());
        }

        return state;
    }

    
    private LangGraph4jResearchState generateFinalReport(LangGraph4jResearchState state) {
        logger.info("LangGraph4j: Generating final report");

        try {
            String finalReport = buildComprehensiveReport(state);
            state.setFinalReport(finalReport);
            state.setCurrentPhase("completed");
            state.setEndTime(java.time.Instant.now());

            logger.info("Final report generation completed");

        } catch (Exception e) {
            logger.warning("Final report generation failed: " + e.getMessage());
            state.addError("report_generation", e.getMessage());
        }

        return state;
    }

    

    private String shouldContinueResearch(LangGraph4jResearchState state) {
        List<ResearchQuestion> questions = state.getResearchQuestions();

        if (questions.isEmpty()) {
            return "insufficient";
        }

        if (questions.size() < getMinQuestions(state.getConfig().getResearchDepth())) {
            return "insufficient";
        }

        if (questions.size() >= state.getConfig().getMaxQuestions()) {
            return "complete";
        }

        return "continue";
    }

    private String shouldCrossValidate(LangGraph4jResearchState state) {
        if (!state.getConfig().isCrossValidationEnabled()) {
            return "complete";
        }

        if (state.getComplexityScore() >= 7) {
            return "validate";
        }

        if (state.getCitations().size() >= 20) {
            return "validate";
        }

        return "complete";
    }

    

    private String buildQueryAnalysisPrompt(String query, UserProfile userProfile) {
        return String.format("""
            Analyze the following research query and provide a comprehensive analysis:
            
            QUERY: "%s"
            
            USER CONTEXT:
            - Domain: %s
            - Expertise: %s
            
            Provide analysis in JSON format with:
            - complexity (1-10 scale)
            - primary_domain
            - research_type (exploratory/explanatory/comparative/technical)
            - estimated_questions_needed (5-25)
            - key_concepts (list)
            - research_depth_required (standard/comprehensive/exhaustive)
            
            JSON:
            """,
            query,
            userProfile.getDomain(),
            userProfile.getExpertiseLevel()
        );
    }

    private List<ResearchQuestion> generateAdaptiveQuestions(LangGraph4jResearchState state) throws Research4jException {
        try {
            Map<String, Object> analysis = state.getQueryAnalysis();
            String researchType = (String) analysis.get("research_type");
            int estimatedQuestions = (Integer) analysis.getOrDefault("estimated_questions_needed", 10);

            String questionPrompt = buildAdaptiveQuestionPrompt(state.getOriginalQuery(), researchType, estimatedQuestions);
            var response = llmClient.complete(questionPrompt, String.class);

            return parseResearchQuestions(response.structuredOutput());

        } catch (Exception e) {
            throw new Research4jException("Failed to generate adaptive questions", e);
        }
    }

    private String buildAdaptiveQuestionPrompt(String originalQuery, String researchType, int targetQuestions) {
        return String.format("""
            Generate %d comprehensive research questions for %s research on: "%s"
            
            Research Type: %s
            
            Generate questions that cover:
            1. Fundamental understanding
            2. Implementation details
            3. Comparative analysis
            4. Best practices
            5. Challenges and solutions
            6. Future implications
            
            Format each question as: "Q: [question]"
            """,
            targetQuestions, researchType, originalQuery, researchType
        );
    }

    private List<ResearchQuestion> parseResearchQuestions(String response) {
        return response.lines()
            .filter(line -> line.startsWith("Q:"))
            .map(line -> line.substring(2).trim())
            .map(question -> new ResearchQuestion(question, ResearchQuestion.Priority.MEDIUM, "generated"))
            .toList();
    }

    private List<CitationResult> enhanceCitationsWithLangGraph(List<CitationResult> citations, LangGraph4jResearchState state) {
        
        return citations.stream()
            .filter(citation -> citation.getRelevanceScore() >= 0.4)
            .map(this::enhanceCitationMetadata)
            .sorted((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()))
            .limit(state.getConfig().getMaxSources())
            .toList();
    }

    private CitationResult enhanceCitationMetadata(CitationResult citation) {
        
        double enhancedScore = citation.getRelevanceScore();

        
        if (citation.getDomain().contains("edu") || citation.getDomain().contains("org")) {
            enhancedScore = Math.min(1.0, enhancedScore + 0.1);
        }

        
        if (citation.getRetrievedAt().isAfter(java.time.LocalDateTime.now().minusMonths(6))) {
            enhancedScore = Math.min(1.0, enhancedScore + 0.05);
        }

        citation.setRelevanceScore(enhancedScore);
        return citation;
    }

    private Map<String, Object> buildKnowledgeGraph(LangGraph4jResearchState state) {
        Map<String, Object> knowledgeGraph = new HashMap<>();

        
        Map<String, List<String>> concepts = new HashMap<>();
        List<String> entities = new java.util.ArrayList<>();

        
        for (ResearchQuestion question : state.getResearchQuestions()) {
            String[] words = question.getQuestion().toLowerCase().split("\\W+");
            for (String word : words) {
                if (word.length() > 4) {
                    entities.add(word);
                }
            }
        }

        knowledgeGraph.put("entities", entities);
        knowledgeGraph.put("concepts", concepts);
        knowledgeGraph.put("relationships", new HashMap<String, Object>());

        return knowledgeGraph;
    }

    private String generateKnowledgeSynthesis(LangGraph4jResearchState state) throws Research4jException {
        try {
            String synthesisPrompt = buildSynthesisPrompt(state);
            var response = llmClient.complete(synthesisPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            throw new Research4jException("Failed to generate knowledge synthesis", e);
        }
    }

    private String buildSynthesisPrompt(LangGraph4jResearchState state) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Synthesize the following research findings into a comprehensive analysis:\n\n");
        prompt.append("ORIGINAL QUERY: ").append(state.getOriginalQuery()).append("\n\n");

        prompt.append("RESEARCH QUESTIONS EXPLORED:\n");
        for (ResearchQuestion question : state.getResearchQuestions()) {
            prompt.append("- ").append(question.getQuestion()).append("\n");
        }

        prompt.append("\nKEY SOURCES: ").append(state.getCitations().size()).append(" authoritative sources\n\n");

        prompt.append("Create a comprehensive synthesis covering:\n");
        prompt.append("1. Executive summary\n");
        prompt.append("2. Key findings and insights\n");
        prompt.append("3. Interconnections and relationships\n");
        prompt.append("4. Practical implications\n");
        prompt.append("5. Areas for further research\n\n");

        return prompt.toString();
    }

    private Map<String, Object> performCrossValidation(LangGraph4jResearchState state) {
        Map<String, Object> validationResults = new HashMap<>();

        
        long uniqueDomains = state.getCitations().stream()
            .map(CitationResult::getDomain)
            .distinct()
            .count();
        validationResults.put("source_diversity", uniqueDomains);

        
        double avgRelevance = state.getCitations().stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);
        validationResults.put("average_relevance", avgRelevance);

        
        int questionsAnswered = state.getResearchQuestions().size();
        int sourcesGathered = state.getCitations().size();
        double coverage = Math.min(1.0, (double) sourcesGathered / (questionsAnswered * 3));
        validationResults.put("coverage_score", coverage);

        
        double overallScore = (uniqueDomains / 10.0 * 0.3) + (avgRelevance * 0.4) + (coverage * 0.3);
        validationResults.put("overall_validation_score", Math.min(1.0, overallScore));

        return validationResults;
    }

    private String buildComprehensiveReport(LangGraph4jResearchState state) {
        StringBuilder report = new StringBuilder();

        report.append("# LangGraph4j Deep Research Report\n\n");
        report.append("## Research Overview\n\n");
        report.append("**Query**: ").append(state.getOriginalQuery()).append("\n");
        report.append("**Research Duration**: ").append(
            java.time.Duration.between(state.getStartTime(), state.getEndTime())).append("\n");
        report.append("**Questions Explored**: ").append(state.getResearchQuestions().size()).append("\n");
        report.append("**Sources Analyzed**: ").append(state.getCitations().size()).append("\n\n");

        if (state.getValidationResults() != null && !state.getValidationResults().isEmpty()) {
            report.append("## Research Quality Metrics\n\n");
            state.getValidationResults().forEach((key, value) ->
                report.append("- **").append(key).append("**: ").append(value).append("\n"));
            report.append("\n");
        }

        report.append("## Knowledge Synthesis\n\n");
        report.append(state.getSynthesis()).append("\n\n");

        report.append("## Research Questions Explored\n\n");
        for (ResearchQuestion question : state.getResearchQuestions()) {
            report.append("### ").append(question.getQuestion()).append("\n");
            report.append("*Category*: ").append(question.getCategory()).append("\n\n");
        }

        return report.toString();
    }

    private DeepResearchResult convertToDeepResearchResult(LangGraph4jResearchState finalState) {
        java.time.Duration processingTime = java.time.Duration.between(
            finalState.getStartTime(), finalState.getEndTime());

        return new DeepResearchResult(
            "langgraph4j-" + System.currentTimeMillis(),
            finalState.getOriginalQuery(),
            finalState.getFinalReport(),
            finalState.getCitations(),
            finalState.getResearchQuestions(),
            finalState.getKnowledgeGraph(),
            processingTime,
            "LangGraph4j Open Deep Research"
        );
    }

    
    private int getMaxIterations(DeepResearchConfig.ResearchDepth depth) {
        return switch (depth) {
            case STANDARD -> 10;
            case COMPREHENSIVE -> 15;
            case EXHAUSTIVE -> 25;
        };
    }

    private int getParallelismLevel(DeepResearchConfig.ResearchScope scope) {
        return switch (scope) {
            case FOCUSED -> 3;
            case BROAD -> 5;
            case INTERDISCIPLINARY -> 8;
        };
    }

    private int getMinQuestions(DeepResearchConfig.ResearchDepth depth) {
        return switch (depth) {
            case STANDARD -> 5;
            case COMPREHENSIVE -> 8;
            case EXHAUSTIVE -> 12;
        };
    }
}










