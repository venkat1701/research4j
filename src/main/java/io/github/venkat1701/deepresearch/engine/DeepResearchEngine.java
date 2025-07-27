package io.github.venkat1701.deepresearch.engine;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;
import io.github.venkat1701.deepresearch.context.MemoryManager;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.DeepResearchProgress;
import io.github.venkat1701.deepresearch.models.DeepResearchResult;
import io.github.venkat1701.deepresearch.models.ResearchPhase;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;
import io.github.venkat1701.deepresearch.strategies.DeepResearchStrategy;
import io.github.venkat1701.deepresearch.strategies.impl.ComprehensiveDeepResearchStrategy;
import io.github.venkat1701.deepresearch.strategies.impl.TechnicalDeepResearchStrategy;
import io.github.venkat1701.exceptions.Research4jException;
import io.github.venkat1701.pipeline.profile.UserProfile;


public class DeepResearchEngine {

    private static final Logger logger = Logger.getLogger(DeepResearchEngine.class.getName());

    private final LLMClient llmClient;
    private final CitationService citationService;
    private final ExecutorService executor;
    private final MemoryManager memoryManager;

    
    private final Map<String, DeepResearchStrategy> strategies;

    
    private final Map<String, DeepResearchContext> activeSessions;

    
    private final Map<String, DeepResearchProgress> progressTracker;

    public DeepResearchEngine(LLMClient llmClient, CitationService citationService) {
        this.llmClient = llmClient;
        this.citationService = citationService;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.memoryManager = new MemoryManager();
        this.strategies = initializeStrategies();
        this.activeSessions = new ConcurrentHashMap<>();
        this.progressTracker = new ConcurrentHashMap<>();

        logger.info("Deep Research Engine initialized with " + strategies.size() + " research strategies");
    }

    private Map<String, DeepResearchStrategy> initializeStrategies() {
        Map<String, DeepResearchStrategy> strategies = new HashMap<>();

        strategies.put("comprehensive", new ComprehensiveDeepResearchStrategy(llmClient, citationService, memoryManager));
        strategies.put("technical", new TechnicalDeepResearchStrategy(llmClient, citationService, memoryManager));



        return strategies;
    }

    
    public CompletableFuture<DeepResearchResult> startDeepResearch(
        String query,
        UserProfile userProfile,
        DeepResearchConfig config) {

        String sessionId = generateSessionId();
        logger.info("Starting deep research session: " + sessionId + " for query: " + truncate(query, 100));

        return CompletableFuture.supplyAsync(() -> {
            try {
                
                DeepResearchContext context = new DeepResearchContext(sessionId, query, userProfile, config);
                activeSessions.put(sessionId, context);

                
                DeepResearchProgress progress = new DeepResearchProgress(sessionId);
                progressTracker.put(sessionId, progress);

                
                DeepResearchStrategy strategy = selectOptimalStrategy(query, userProfile, config);
                logger.info("Selected strategy: " + strategy.getStrategyName() + " for session: " + sessionId);

                
                return executeDeepResearch(context, strategy, progress);

            } catch (Exception e) {
                logger.severe("Deep research failed for session " + sessionId + ": " + e.getMessage());
                throw new RuntimeException("Deep research execution failed", e);
            }
        }, executor);
    }

    
    private DeepResearchResult executeDeepResearch(
        DeepResearchContext context,
        DeepResearchStrategy strategy,
        DeepResearchProgress progress) throws Research4jException {

        logger.info("Executing deep research with strategy: " + strategy.getStrategyName());

        try {
            
            progress.setCurrentPhase(ResearchPhase.INITIAL_ANALYSIS);
            List<ResearchQuestion> initialQuestions = generateInitialResearchQuestions(context);
            context.addResearchQuestions(initialQuestions);
            progress.updateProgress(20, "Generated " + initialQuestions.size() + " initial research questions");

            
            progress.setCurrentPhase(ResearchPhase.MULTI_DIMENSIONAL_RESEARCH);
            executeMultiDimensionalResearch(context, strategy, progress);

            
            progress.setCurrentPhase(ResearchPhase.DEEP_DIVE);
            executeDeepDiveResearch(context, strategy, progress);

            
            progress.setCurrentPhase(ResearchPhase.CROSS_REFERENCE);
            executeCrossReferenceAnalysis(context, strategy, progress);

            
            progress.setCurrentPhase(ResearchPhase.SYNTHESIS);
            String synthesizedKnowledge = synthesizeKnowledge(context, strategy);
            progress.updateProgress(90, "Knowledge synthesis completed");

            
            progress.setCurrentPhase(ResearchPhase.REPORT_GENERATION);
            String finalReport = generateFinalReport(context, strategy, synthesizedKnowledge);
            progress.updateProgress(100, "Deep research completed");

            
            DeepResearchResult result = new DeepResearchResult(
                context.getSessionId(),
                context.getOriginalQuery(),
                finalReport,
                context.getAllCitations(),
                context.getResearchQuestions(),
                context.getKnowledgeMap(),
                progress.getTotalDuration(),
                strategy.getStrategyName()
            );

            
            memoryManager.storeResearchResult(context.getSessionId(), result);

            logger.info("Deep research completed successfully for session: " + context.getSessionId());
            return result;

        } finally {
            
            activeSessions.remove(context.getSessionId());

            
            CompletableFuture.delayedExecutor(1, TimeUnit.HOURS, executor)
                .execute(() -> progressTracker.remove(context.getSessionId()));
        }
    }

    
    private List<ResearchQuestion> generateInitialResearchQuestions(DeepResearchContext context)
        throws Research4jException {

        logger.info("Generating initial research questions for: " + context.getOriginalQuery());

        String questionPrompt = buildQuestionGenerationPrompt(context);

        try {
            LLMResponse<String> response = llmClient.complete(questionPrompt, String.class);
            return parseResearchQuestions(response.structuredOutput(), context);

        } catch (Exception e) {
            logger.warning("Failed to generate questions via LLM, using fallback strategy");
            return generateFallbackQuestions(context);
        }
    }

    private String buildQuestionGenerationPrompt(DeepResearchContext context) {
        return String.format("""
            You are an expert research strategist tasked with generating comprehensive research questions 
            for deep investigation of complex topics.
            
            ORIGINAL RESEARCH TOPIC:
            "%s"
            
            USER CONTEXT:
            - Domain: %s
            - Expertise Level: %s
            - Research Depth: %s
            
            TASK: Generate 8-12 diverse research questions that will enable comprehensive understanding of this topic.
            
            QUESTION CATEGORIES TO COVER:
            1. Fundamental Concepts (What is...? How does...?)
            2. Historical Context (When did...? How did it evolve...?)
            3. Current State (What's the current...? How is it being used...?)
            4. Comparative Analysis (How does X compare to Y...?)
            5. Implementation Details (How to implement...? What are the steps...?)
            6. Challenges & Solutions (What are the main challenges...? How to solve...?)
            7. Future Trends (What's the future of...? What are emerging...?)
            8. Best Practices (What are the best practices...? What should be avoided...?)
            
            FORMAT: Return each question on a new line, prefixed with "Q:" followed by the question.
            
            REQUIREMENTS:
            - Questions should be specific and actionable
            - Cover different aspects and perspectives
            - Appropriate for the user's expertise level
            - Enable building comprehensive knowledge
            
            Generate the research questions:
            """,
            context.getOriginalQuery(),
            context.getUserProfile() != null ? context.getUserProfile().getDomain() : "general",
            context.getUserProfile() != null ? context.getUserProfile().getExpertiseLevel() : "intermediate",
            context.getConfig().getResearchDepth().name()
        );
    }

    private List<ResearchQuestion> parseResearchQuestions(String response, DeepResearchContext context) {
        List<ResearchQuestion> questions = new ArrayList<>();

        String[] lines = response.split("\n");
        for (String line : lines) {
            line = line.trim();
            if (line.startsWith("Q:")) {
                String question = line.substring(2).trim();
                if (!question.isEmpty()) {
                    questions.add(new ResearchQuestion(
                        question,
                        determineQuestionPriority(question, context),
                        categorizeQuestion(question)
                    ));
                }
            }
        }

        if (questions.isEmpty()) {
            logger.warning("No questions parsed from LLM response, using fallback");
            return generateFallbackQuestions(context);
        }

        return questions;
    }

    private List<ResearchQuestion> generateFallbackQuestions(DeepResearchContext context) {
        List<ResearchQuestion> fallbackQuestions = new ArrayList<>();
        String query = context.getOriginalQuery();

        fallbackQuestions.add(new ResearchQuestion("What is " + query + "?", ResearchQuestion.Priority.HIGH, "fundamental"));
        fallbackQuestions.add(new ResearchQuestion("How does " + query + " work?", ResearchQuestion.Priority.HIGH, "technical"));
        fallbackQuestions.add(new ResearchQuestion("What are the benefits of " + query + "?", ResearchQuestion.Priority.MEDIUM, "analysis"));
        fallbackQuestions.add(new ResearchQuestion("What are the challenges with " + query + "?", ResearchQuestion.Priority.MEDIUM, "analysis"));
        fallbackQuestions.add(new ResearchQuestion("How to implement " + query + "?", ResearchQuestion.Priority.HIGH, "implementation"));
        fallbackQuestions.add(new ResearchQuestion("What are best practices for " + query + "?", ResearchQuestion.Priority.MEDIUM, "best-practices"));

        return fallbackQuestions;
    }

    private ResearchQuestion.Priority determineQuestionPriority(String question, DeepResearchContext context) {
        String questionLower = question.toLowerCase();

        if (questionLower.contains("what is") || questionLower.contains("how does") ||
            questionLower.contains("how to")) {
            return ResearchQuestion.Priority.HIGH;
        }

        if (questionLower.contains("best practices") || questionLower.contains("implementation") ||
            questionLower.contains("example")) {
            return ResearchQuestion.Priority.HIGH;
        }

        if (questionLower.contains("challenge") || questionLower.contains("problem") ||
            questionLower.contains("compare")) {
            return ResearchQuestion.Priority.MEDIUM;
        }

        return ResearchQuestion.Priority.MEDIUM;
    }

    private String categorizeQuestion(String question) {
        String questionLower = question.toLowerCase();

        if (questionLower.contains("what is") || questionLower.contains("define")) {
            return "fundamental";
        }
        if (questionLower.contains("how does") || questionLower.contains("how to")) {
            return "technical";
        }
        if (questionLower.contains("compare") || questionLower.contains("versus")) {
            return "comparative";
        }
        if (questionLower.contains("implement") || questionLower.contains("code")) {
            return "implementation";
        }
        if (questionLower.contains("best practice") || questionLower.contains("should")) {
            return "best-practices";
        }
        if (questionLower.contains("future") || questionLower.contains("trend")) {
            return "trends";
        }

        return "analysis";
    }

    
    private void executeMultiDimensionalResearch(
        DeepResearchContext context,
        DeepResearchStrategy strategy,
        DeepResearchProgress progress) throws Research4jException {

        logger.info("Executing multi-dimensional research for " + context.getResearchQuestions().size() + " questions");

        List<ResearchQuestion> prioritizedQuestions = context.getResearchQuestions().stream()
            .sorted((q1, q2) -> q2.getPriority().compareTo(q1.getPriority()))
            .collect(Collectors.toList());

        int totalQuestions = prioritizedQuestions.size();
        int processedQuestions = 0;

        
        List<List<ResearchQuestion>> batches = partitionQuestions(prioritizedQuestions, 3);

        for (List<ResearchQuestion> batch : batches) {
            List<CompletableFuture<Void>> batchFutures = batch.stream()
                .map(question -> CompletableFuture.runAsync(() -> {
                    try {
                        researchSingleQuestion(question, context, strategy);
                    } catch (Exception e) {
                        logger.warning("Failed to research question: " + question.getQuestion() + " - " + e.getMessage());
                    }
                }, executor))
                .collect(Collectors.toList());

            
            CompletableFuture.allOf(batchFutures.toArray(new CompletableFuture[0])).join();

            processedQuestions += batch.size();
            int progressPercent = 20 + (int)(((double)processedQuestions / totalQuestions) * 30); 
            progress.updateProgress(progressPercent,
                "Researched " + processedQuestions + "/" + totalQuestions + " questions");
        }
    }

    private List<List<ResearchQuestion>> partitionQuestions(List<ResearchQuestion> questions, int batchSize) {
        List<List<ResearchQuestion>> batches = new ArrayList<>();
        for (int i = 0; i < questions.size(); i += batchSize) {
            batches.add(questions.subList(i, Math.min(i + batchSize, questions.size())));
        }
        return batches;
    }

    private void researchSingleQuestion(
        ResearchQuestion question,
        DeepResearchContext context,
        DeepResearchStrategy strategy) throws Research4jException {

        logger.info("Researching question: " + truncate(question.getQuestion(), 80));

        try {
            
            List<CitationResult> questionCitations = citationService.search(question.getQuestion());

            
            List<CitationResult> enhancedCitations = strategy.enhanceCitations(questionCitations, question, context);

            
            context.addCitations(question.getQuestion(), enhancedCitations);

            
            String insights = strategy.generateInsights(question, enhancedCitations, context);
            context.addInsights(question.getQuestion(), insights);

            
            memoryManager.updateKnowledge(question.getQuestion(), insights, enhancedCitations);

        } catch (Exception e) {
            logger.warning("Error researching question '" + question.getQuestion() + "': " + e.getMessage());
            throw new Research4jException("Failed to research question", e);
        }
    }

    
    private void executeDeepDiveResearch(
        DeepResearchContext context,
        DeepResearchStrategy strategy,
        DeepResearchProgress progress) throws Research4jException {

        logger.info("Executing deep dive research");

        
        List<String> criticalAreas = identifyCriticalAreas(context, strategy);
        progress.updateProgress(55, "Identified " + criticalAreas.size() + " critical areas for deep dive");

        int processedAreas = 0;
        for (String area : criticalAreas) {
            try {
                
                List<ResearchQuestion> deepQuestions = generateDeepQuestions(area, context, strategy);
                context.addResearchQuestions(deepQuestions);

                
                for (ResearchQuestion deepQuestion : deepQuestions) {
                    researchSingleQuestion(deepQuestion, context, strategy);
                }

                processedAreas++;
                int progressPercent = 55 + (int)(((double)processedAreas / criticalAreas.size()) * 20); 
                progress.updateProgress(progressPercent,
                    "Completed deep dive " + processedAreas + "/" + criticalAreas.size());

            } catch (Exception e) {
                logger.warning("Error in deep dive for area '" + area + "': " + e.getMessage());
            }
        }
    }

    private List<String> identifyCriticalAreas(DeepResearchContext context, DeepResearchStrategy strategy) {
        
        return strategy.identifyCriticalAreas(context);
    }

    private List<ResearchQuestion> generateDeepQuestions(
        String area,
        DeepResearchContext context,
        DeepResearchStrategy strategy) throws Research4jException {

        return strategy.generateDeepQuestions(area, context);
    }

    
    private void executeCrossReferenceAnalysis(
        DeepResearchContext context,
        DeepResearchStrategy strategy,
        DeepResearchProgress progress) throws Research4jException {

        logger.info("Executing cross-reference analysis");

        
        Map<String, Set<String>> relationships = strategy.analyzeCrossReferences(context);
        context.setKnowledgeRelationships(relationships);

        
        List<String> inconsistencies = strategy.validateConsistency(context);
        if (!inconsistencies.isEmpty()) {
            logger.warning("Found " + inconsistencies.size() + " potential inconsistencies");
            context.addInconsistencies(inconsistencies);
        }

        progress.updateProgress(85, "Cross-reference analysis completed");
    }

    
    private String synthesizeKnowledge(DeepResearchContext context, DeepResearchStrategy strategy)
        throws Research4jException {

        logger.info("Synthesizing knowledge from " + context.getAllCitations().size() + " sources");

        return strategy.synthesizeKnowledge(context);
    }

    
    private String generateFinalReport(
        DeepResearchContext context,
        DeepResearchStrategy strategy,
        String synthesizedKnowledge) throws Research4jException {

        logger.info("Generating final research report");

        return strategy.generateFinalReport(context, synthesizedKnowledge);
    }

    
    private DeepResearchStrategy selectOptimalStrategy(
        String query,
        UserProfile userProfile,
        DeepResearchConfig config) {

        String queryLower = query.toLowerCase();
        String domain = userProfile != null ? userProfile.getDomain() : "general";

        
        if (domain.contains("software") || domain.contains("technical") || domain.contains("engineering") ||
            queryLower.contains("implement") || queryLower.contains("code") || queryLower.contains("architecture")) {
            return strategies.get("technical");
        }

        
        if (domain.contains("academic") || domain.contains("research") ||
            queryLower.contains("research") || queryLower.contains("study") || queryLower.contains("analysis")) {
            return strategies.get("academic");
        }

        
        if (domain.contains("business") || domain.contains("management") ||
            queryLower.contains("business") || queryLower.contains("strategy") || queryLower.contains("market")) {
            return strategies.get("business");
        }

        
        return strategies.get("comprehensive");
    }

    
    public DeepResearchProgress getProgress(String sessionId) {
        return progressTracker.get(sessionId);
    }

    
    public Map<String, DeepResearchProgress> getAllActiveResearch() {
        return new HashMap<>(progressTracker);
    }

    
    public boolean cancelResearch(String sessionId) {
        DeepResearchContext context = activeSessions.remove(sessionId);
        DeepResearchProgress progress = progressTracker.remove(sessionId);

        if (context != null || progress != null) {
            logger.info("Cancelled research session: " + sessionId);
            return true;
        }

        return false;
    }

    
    public MemoryManager getMemoryManager() {
        return memoryManager;
    }

    private String generateSessionId() {
        return "deep-research-" + System.currentTimeMillis() + "-" +
            Integer.toHexString((int)(Math.random() * 0x10000));
    }

    private String truncate(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    
    public void shutdown() {
        try {
            
            activeSessions.keySet().forEach(this::cancelResearch);

            
            executor.shutdown();
            if (!executor.awaitTermination(30, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }

            
            memoryManager.shutdown();

            logger.info("Deep Research Engine shutdown completed");

        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
            logger.warning("Deep Research Engine shutdown interrupted");
        }
    }
}