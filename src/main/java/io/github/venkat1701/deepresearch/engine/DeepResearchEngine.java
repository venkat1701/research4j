package io.github.venkat1701.deepresearch.engine;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;
import java.time.Instant;
import java.time.Duration;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;
import io.github.venkat1701.deepresearch.models.ResearchResults;
import io.github.venkat1701.deepresearch.pipeline.ContextAwareChunker;
import io.github.venkat1701.deepresearch.pipeline.HierarchicalSynthesizer;
import io.github.venkat1701.deepresearch.pipeline.NarrativeBuilder;
import io.github.venkat1701.deepresearch.pipeline.ResearchSupervisor;
import io.github.venkat1701.deepresearch.models.DeepResearchResult;
import io.github.venkat1701.exceptions.client.LLMClientException;


public class DeepResearchEngine {

    private static final Logger logger = Logger.getLogger(DeepResearchEngine.class.getName());

    
    private static final int MAX_RESEARCH_ROUNDS = 3;
    private static final int MAX_QUESTIONS_PER_ROUND = 12;
    private static final int TARGET_NARRATIVE_WORDS = 8000;
    private static final long SESSION_TIMEOUT_MINUTES = 45;

    
    private final LLMClient llmClient;
    private final CitationService citationService;
    private final ResearchSupervisor researchSupervisor;
    private final NarrativeBuilder narrativeBuilder;
    private final HierarchicalSynthesizer hierarchicalSynthesizer;
    private final ContextAwareChunker contextChunker;
    private final ExecutorService mainExecutor;
    private final ScheduledExecutorService scheduledExecutor;

    
    private final Map<String, DeepResearchSession> activeSessions;
    private final ResearchMetricsCollector metricsCollector;

    public DeepResearchEngine(LLMClient llmClient, CitationService citationService) {
        this.llmClient = llmClient;
        this.citationService = citationService;

        
        this.mainExecutor = Executors.newFixedThreadPool(8);
        this.scheduledExecutor = Executors.newScheduledThreadPool(4);

        
        this.hierarchicalSynthesizer = new HierarchicalSynthesizer(llmClient);
        this.contextChunker = new ContextAwareChunker(32000);
        this.narrativeBuilder = new NarrativeBuilder(llmClient, hierarchicalSynthesizer, mainExecutor);
        this.researchSupervisor = new ResearchSupervisor(llmClient, citationService, mainExecutor);

        
        this.activeSessions = new ConcurrentHashMap<>();
        this.metricsCollector = new ResearchMetricsCollector();

        
        scheduleSessionCleanup();

        logger.info("DeepResearchEngine initialized successfully");
    }

    
    public CompletableFuture<DeepResearchResult> executeDeepResearch(String originalQuery,
        DeepResearchConfig config) {
        return CompletableFuture.supplyAsync(() -> {
            String sessionId = generateSessionId();
            Instant startTime = Instant.now();

            try {
                logger.info("Starting Deep Research session: " + sessionId + " for query: " + originalQuery);

                
                DeepResearchContext context = initializeResearchContext(sessionId, originalQuery, config);
                activeSessions.put(sessionId, new DeepResearchSession(context, startTime));

                
                ResearchResults comprehensiveResults = executeMultiRoundResearch(context);

                
                String synthesizedKnowledge = synthesizeComprehensiveKnowledge(comprehensiveResults, context);

                
                String comprehensiveNarrative = narrativeBuilder.buildComprehensiveNarrative(
                    context, synthesizedKnowledge);

                
                DeepResearchResult finalResult = enhanceAndValidateResult(
                    comprehensiveNarrative, comprehensiveResults, context);

                
                Duration totalDuration = Duration.between(startTime, Instant.now());
                metricsCollector.recordSession(sessionId, totalDuration, finalResult);
                activeSessions.remove(sessionId);

                logger.info("Deep Research completed for session: " + sessionId +
                    " in " + totalDuration.toMinutes() + " minutes");

                return finalResult;

            } catch (Exception e) {
                logger.severe("Deep Research failed for session: " + sessionId + " - " + e.getMessage());
                activeSessions.remove(sessionId);
                return createFallbackResult(originalQuery, e);
            }
        }, mainExecutor);
    }

    
    private ResearchResults executeMultiRoundResearch(DeepResearchContext context) {
        logger.info("Executing multi-round research for session: " + context.getSessionId());

        List<CitationResult> allCitations = new ArrayList<>();
        Map<String, String> consolidatedInsights = new HashMap<>();
        Set<String> exploredTopics = new HashSet<>();

        for (int round = 1; round <= MAX_RESEARCH_ROUNDS; round++) {
            logger.info("Research Round " + round + " - generating questions and executing searches");

            try {
                
                List<ResearchQuestion> roundQuestions = generateResearchQuestions(
                    context, exploredTopics, round);

                if (roundQuestions.isEmpty()) {
                    logger.warning("No research questions generated for round " + round);
                    continue;
                }

                
                List<CompletableFuture<List<CitationResult>>> searchFutures = roundQuestions.stream()
                    .map(question -> executeQuestionResearchSafely(question, context, exploredTopics))
                    .collect(Collectors.toList());

                
                List<List<CitationResult>> roundResults = searchFutures.stream()
                    .map(future -> {
                        try {
                            return future.get(30, TimeUnit.SECONDS); 
                        } catch (Exception e) {
                            logger.warning("Question research timed out: " + e.getMessage());
                            return new ArrayList<CitationResult>();
                        }
                    })
                    .collect(Collectors.toList());

                
                Map<String, String> roundInsights = synthesizeRoundInsights(
                    roundQuestions, roundResults, context);

                
                roundResults.forEach(allCitations::addAll);
                consolidatedInsights.putAll(roundInsights);
                exploredTopics.addAll(extractTopicsFromResults(roundResults));

                
                updateContextWithFindings(context, roundInsights, roundResults);

                logger.info("Round " + round + " completed: " +
                    roundResults.stream().mapToInt(List::size).sum() + " new citations, " +
                    roundInsights.size() + " insights");

                
                if (isResearchSufficient(context, round, allCitations.size())) {
                    logger.info("Research deemed sufficient after round " + round);
                    break;
                }

            } catch (Exception e) {
                logger.warning("Research round " + round + " failed: " + e.getMessage());
                
            }
        }

        
        Map<String, List<CitationResult>> categorizedResults = categorizeCitations(allCitations);

        logger.info("Multi-round research completed: " + allCitations.size() + " total citations, " +
            consolidatedInsights.size() + " insights, " + categorizedResults.size() + " categories");

        return new ResearchResults(allCitations, categorizedResults, consolidatedInsights);
    }

    
    private List<ResearchQuestion> generateResearchQuestions(DeepResearchContext context,
        Set<String> exploredTopics,
        int round) {
        try {
            String questionPrompt = buildQuestionGenerationPrompt(context, exploredTopics, round);

            
            List<ContextAwareChunker.ContextChunk> promptChunks = contextChunker.chunkPrompt(questionPrompt);
            List<ResearchQuestion> allQuestions = new ArrayList<>();

            for (ContextAwareChunker.ContextChunk chunk : promptChunks) {
                try {
                    String response = llmClient.complete(chunk.getContent(), String.class).structuredOutput();
                    List<ResearchQuestion> chunkQuestions = parseQuestionsFromResponse(response, context, round);
                    allQuestions.addAll(chunkQuestions);
                } catch (Exception e) {
                    logger.warning("Failed to generate questions for chunk: " + e.getMessage());
                }
            }

            
            List<ResearchQuestion> prioritizedQuestions = prioritizeQuestions(allQuestions, round);

            
            for (ResearchQuestion question : prioritizedQuestions) {
                context.addResearchQuestion(question);
            }

            logger.info("Generated " + prioritizedQuestions.size() + " research questions for round " + round);
            return prioritizedQuestions;

        } catch (Exception e) {
            logger.warning("Question generation failed for round " + round + ": " + e.getMessage());
            return generateFallbackQuestions(context, round);
        }
    }

    
    private CompletableFuture<List<CitationResult>> executeQuestionResearchSafely(ResearchQuestion question,
        DeepResearchContext context,
        Set<String> exploredTopics) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Researching question: " + truncateString(question.getQuestion(), 100));

                
                List<String> searchQueries = generateSearchQueries(question, context);
                List<CitationResult> questionResults = new ArrayList<>();

                
                for (String query : searchQueries) {
                    try {
                        List<CitationResult> queryResults = citationService.search(query);

                        
                        List<CitationResult> filteredResults = queryResults.stream()
                            .filter(citation -> citation != null && citation.isValid())
                            .filter(citation -> citation.getRelevanceScore() >= 0.3)
                            .filter(citation -> citation.getContent() != null &&
                                citation.getContent().length() > 100)
                            .limit(8) 
                            .collect(Collectors.toList());

                        questionResults.addAll(filteredResults);

                        
                        Thread.sleep(500);

                    } catch (Exception e) {
                        logger.warning("Search failed for query '" + query + "': " + e.getMessage());
                    }
                }

                
                List<CitationResult> uniqueResults = removeDuplicateCitations(questionResults);
                uniqueResults.sort((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()));

                
                context.markQuestionAsResearched(question);

                logger.info("Question research completed: " + uniqueResults.size() + " citations found");
                return uniqueResults.stream().limit(15).collect(Collectors.toList());

            } catch (Exception e) {
                logger.warning("Question research failed: " + question.getQuestion() + " - " + e.getMessage());
                return new ArrayList<>();
            }
        }, mainExecutor);
    }

    
    private List<String> generateSearchQueries(ResearchQuestion question, DeepResearchContext context) {
        List<String> queries = new ArrayList<>();

        
        queries.add(question.getQuestion());

        
        String category = question.getCategory().toLowerCase();
        String baseQuery = question.getQuestion();

        switch (category) {
            case "implementation":
                queries.add(baseQuery + " implementation guide");
                queries.add(baseQuery + " code example");
                queries.add(baseQuery + " tutorial");
                break;
            case "performance":
                queries.add(baseQuery + " performance analysis");
                queries.add(baseQuery + " benchmark");
                queries.add(baseQuery + " optimization");
                break;
            case "case-study":
                queries.add(baseQuery + " case study");
                queries.add(baseQuery + " real world example");
                queries.add(baseQuery + " use case");
                break;
            case "technical":
                queries.add(baseQuery + " technical specification");
                queries.add(baseQuery + " architecture");
                queries.add(baseQuery + " design pattern");
                break;
            default:
                queries.add(baseQuery + " best practices");
                queries.add(baseQuery + " overview");
        }

        
        String originalQuery = context.getOriginalQuery();
        if (!baseQuery.toLowerCase().contains(originalQuery.toLowerCase())) {
            queries.add(originalQuery + " " + baseQuery);
        }

        return queries.stream().distinct().limit(5).collect(Collectors.toList());
    }

    
    private List<CitationResult> removeDuplicateCitations(List<CitationResult> citations) {
        Map<String, CitationResult> uniqueCitations = new HashMap<>();

        for (CitationResult citation : citations) {
            String key = citation.getUrl();
            if (key != null && !uniqueCitations.containsKey(key)) {
                uniqueCitations.put(key, citation);
            }
        }

        return new ArrayList<>(uniqueCitations.values());
    }

    
    private String buildQuestionGenerationPrompt(DeepResearchContext context,
        Set<String> exploredTopics,
        int round) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Generate targeted research questions for Round ").append(round)
            .append(" of deep research on: \"").append(context.getOriginalQuery()).append("\"\n\n");

        prompt.append("RESEARCH PROGRESS:\n");
        prompt.append("- Round: ").append(round).append(" of ").append(MAX_RESEARCH_ROUNDS).append("\n");
        prompt.append("- Sources collected: ").append(context.getAllCitations().size()).append("\n");
        prompt.append("- Insights generated: ").append(context.getAllInsights().size()).append("\n");
        prompt.append("- Questions already explored: ").append(context.getResearchQuestions().size()).append("\n");

        if (!exploredTopics.isEmpty()) {
            prompt.append("- Topics explored: ").append(String.join(", ",
                exploredTopics.stream().limit(10).collect(Collectors.toList()))).append("\n");
        }

        prompt.append("\nEXISTING INSIGHTS OVERVIEW:\n");
        String existingInsights = getExistingInsightsSummary(context);
        prompt.append(existingInsights).append("\n\n");

        prompt.append("ROUND ").append(round).append(" FOCUS:\n");
        prompt.append(getRoundFocus(round)).append("\n\n");

        prompt.append("Generate ").append(MAX_QUESTIONS_PER_ROUND).append(" targeted research questions that:\n");
        prompt.append("1. Build upon existing insights without redundancy\n");
        prompt.append("2. Explore uncharted aspects of the topic\n");
        prompt.append("3. Target specific implementations, case studies, and quantitative data\n");
        prompt.append("4. Include technical depth appropriate for expert analysis\n");
        prompt.append("5. Focus on actionable, practical insights\n");
        prompt.append("6. Avoid topics already thoroughly explored\n\n");

        prompt.append("Question Format:\n");
        prompt.append("QUESTION: [Specific research question]\n");
        prompt.append("CATEGORY: [Implementation/Performance/Analysis/Case-Study/Technical]\n");
        prompt.append("PRIORITY: [High/Medium/Low]\n");
        prompt.append("RATIONALE: [Why this question advances the research]\n\n");

        prompt.append("Generate research questions:\n");

        return prompt.toString();
    }

    
    private List<ResearchQuestion> parseQuestionsFromResponse(String response,
        DeepResearchContext context,
        int round) {
        List<ResearchQuestion> questions = new ArrayList<>();
        String[] lines = response.split("\n");

        ResearchQuestion currentQuestion = null;
        StringBuilder currentRationale = new StringBuilder();

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("QUESTION:")) {
                
                if (currentQuestion != null) {
                    currentQuestion.setRationale(currentRationale.toString().trim());
                    questions.add(currentQuestion);
                }

                
                String questionText = line.substring(9).trim();
                if (!questionText.isEmpty()) {
                    currentQuestion = new ResearchQuestion(questionText, "General", "Medium");
                    currentRationale = new StringBuilder();
                }

            } else if (line.startsWith("CATEGORY:") && currentQuestion != null) {
                String category = line.substring(9).trim();
                if (!category.isEmpty()) {
                    currentQuestion.setCategory(category);
                }

            } else if (line.startsWith("PRIORITY:") && currentQuestion != null) {
                String priority = line.substring(9).trim();
                try {
                    ResearchQuestion.Priority priorityEnum = ResearchQuestion.Priority.valueOf(priority.toUpperCase());
                    currentQuestion.setPriority(priorityEnum);
                } catch (Exception e) {
                    
                }

            } else if (line.startsWith("RATIONALE:")) {
                String rationale = line.substring(10).trim();
                currentRationale.append(rationale);

            } else if (currentQuestion != null && !line.isEmpty() &&
                !line.startsWith("QUESTION:") && !line.startsWith("CATEGORY:") &&
                !line.startsWith("PRIORITY:")) {
                
                currentRationale.append(" ").append(line);
            }
        }

        
        if (currentQuestion != null) {
            currentQuestion.setRationale(currentRationale.toString().trim());
            questions.add(currentQuestion);
        }

        logger.info("Parsed " + questions.size() + " questions from response");
        return questions;
    }

    
    private Map<String, String> synthesizeRoundInsights(List<ResearchQuestion> questions,
        List<List<CitationResult>> results,
        DeepResearchContext context) {
        Map<String, String> insights = new HashMap<>();

        for (int i = 0; i < Math.min(questions.size(), results.size()); i++) {
            ResearchQuestion question = questions.get(i);
            List<CitationResult> questionResults = results.get(i);

            try {
                String insight = synthesizeQuestionInsight(question, questionResults, context);
                insights.put(question.getQuestion(), insight);

                
                context.addInsight(question.getQuestion(), insight);

            } catch (Exception e) {
                logger.warning("Insight synthesis failed for question: " + question.getQuestion() +
                    " - " + e.getMessage());
                
                insights.put(question.getQuestion(), createFallbackInsight(questionResults));
            }
        }

        return insights;
    }

    
    private String synthesizeQuestionInsight(ResearchQuestion question,
        List<CitationResult> results,
        DeepResearchContext context) throws LLMClientException {
        if (results.isEmpty()) {
            return "No sufficient sources found for this question.";
        }

        
        StringBuilder prompt = new StringBuilder();
        prompt.append("Synthesize a comprehensive insight for: \"").append(question.getQuestion()).append("\"\n\n");

        prompt.append("QUESTION CONTEXT:\n");
        prompt.append("- Category: ").append(question.getCategory()).append("\n");
        prompt.append("- Priority: ").append(question.getPriority()).append("\n");
        prompt.append("- Main Topic: ").append(context.getOriginalQuery()).append("\n\n");

        prompt.append("SOURCES ANALYZED (").append(results.size()).append(" total):\n");
        for (int i = 0; i < Math.min(results.size(), 6); i++) {
            CitationResult citation = results.get(i);
            prompt.append("Source ").append(i + 1).append(": ").append(citation.getTitle()).append("\n");
            prompt.append("Relevance: ").append(String.format("%.2f", citation.getRelevanceScore())).append("\n");
            prompt.append("Content: ").append(truncateString(citation.getContent(), 300)).append("\n\n");
        }

        prompt.append("SYNTHESIS REQUIREMENTS:\n");
        prompt.append("1. Provide a detailed, evidence-based answer\n");
        prompt.append("2. Include specific examples, metrics, and quantitative data\n");
        prompt.append("3. Reference sources appropriately\n");
        prompt.append("4. Maintain technical accuracy and professional tone\n");
        prompt.append("5. Focus on actionable insights and practical implications\n");
        prompt.append("6. Highlight any conflicting viewpoints or limitations\n");
        prompt.append("7. Connect findings to the broader research topic\n\n");

        prompt.append("Generate comprehensive insight (300-500 words):\n");

        return llmClient.complete(prompt.toString(), String.class).structuredOutput();
    }

    

    private String synthesizeComprehensiveKnowledge(ResearchResults results, DeepResearchContext context) {
        try {
            logger.info("Synthesizing comprehensive knowledge from " +
                results.getAllCitations().size() + " sources");

            
            Map<String, List<String>> thematicInsights = groupInsightsByTheme(results.getInsights());

            
            Map<String, String> synthesizedThemes = new HashMap<>();
            for (Map.Entry<String, List<String>> themeEntry : thematicInsights.entrySet()) {
                String themeKey = themeEntry.getKey();
                List<String> themeInsights = themeEntry.getValue();

                String themeSynthesis = hierarchicalSynthesizer.synthesizeIterativeInsights(
                    Map.of(themeKey, themeInsights), context).get(themeKey);
                synthesizedThemes.put(themeKey, themeSynthesis);
            }

            
            return hierarchicalSynthesizer.synthesizeHierarchically(synthesizedThemes, context);

        } catch (Exception e) {
            logger.warning("Knowledge synthesis failed: " + e.getMessage());
            return createFallbackSynthesis(results, context);
        }
    }

    private Map<String, List<String>> groupInsightsByTheme(Map<String, String> insights) {
        Map<String, List<String>> thematicGroups = new HashMap<>();

        for (Map.Entry<String, String> entry : insights.entrySet()) {
            String theme = determineInsightTheme(entry.getKey(), entry.getValue());
            thematicGroups.computeIfAbsent(theme, k -> new ArrayList<>()).add(entry.getValue());
        }

        return thematicGroups;
    }

    private String determineInsightTheme(String question, String insight) {
        String combined = (question + " " + insight).toLowerCase();

        if (combined.contains("implement") || combined.contains("code") || combined.contains("develop")) {
            return "implementation";
        }
        if (combined.contains("performance") || combined.contains("speed") || combined.contains("benchmark")) {
            return "performance";
        }
        if (combined.contains("security") || combined.contains("privacy") || combined.contains("safe")) {
            return "security";
        }
        if (combined.contains("example") || combined.contains("case") || combined.contains("application")) {
            return "examples";
        }
        if (combined.contains("architecture") || combined.contains("design") || combined.contains("pattern")) {
            return "architecture";
        }

        return "general";
    }

    
    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    private String getRoundFocus(int round) {
        return switch (round) {
            case 1 -> "Foundational concepts, definitions, and basic implementation approaches";
            case 2 -> "Advanced implementations, performance optimization, and technical deep-dives";
            case 3 -> "Edge cases, emerging trends, comparative analysis, and expert insights";
            default -> "Comprehensive analysis and synthesis";
        };
    }

    private String getExistingInsightsSummary(DeepResearchContext context) {
        return context.getAllInsights().entrySet().stream()
            .limit(3)
            .map(entry -> "- " + truncateString(entry.getValue(), 150))
            .collect(Collectors.joining("\n"));
    }

    private Set<String> extractTopicsFromResults(List<List<CitationResult>> roundResults) {
        Set<String> topics = new HashSet<>();

        for (List<CitationResult> results : roundResults) {
            for (CitationResult result : results) {
                
                String[] titleWords = result.getTitle().toLowerCase().split("\\W+");
                for (String word : titleWords) {
                    if (word.length() > 4) {
                        topics.add(word);
                    }
                }
            }
        }

        return topics;
    }

    private void updateContextWithFindings(DeepResearchContext context,
        Map<String, String> insights,
        List<List<CitationResult>> results) {
        
        for (Map.Entry<String, String> insight : insights.entrySet()) {
            context.addInsight(insight.getKey(), insight.getValue());
        }

        
        for (List<CitationResult> resultList : results) {
            for (CitationResult result : resultList) {
                context.addCitation(result);
            }
        }
    }

    private boolean isResearchSufficient(DeepResearchContext context, int round, int totalSources) {
        int minSources = context.getConfig().getResearchDepth().ordinal() * 20 + 30;
        int minInsights = round * 8;

        return totalSources >= minSources && context.getAllInsights().size() >= minInsights;
    }

    private Map<String, List<CitationResult>> categorizeCitations(List<CitationResult> citations) {
        return citations.stream()
            .collect(Collectors.groupingBy(citation ->
                determineInsightTheme(citation.getTitle(), citation.getContent())));
    }

    private List<ResearchQuestion> prioritizeQuestions(List<ResearchQuestion> questions, int round) {
        return questions.stream()
            .sorted((q1, q2) -> {
                
                int priorityCompare = Integer.compare(q2.getPriority().ordinal(), q1.getPriority().ordinal());
                if (priorityCompare != 0) return priorityCompare;

                return Double.compare(q2.getComplexityScore(), q1.getComplexityScore());
            })
            .limit(MAX_QUESTIONS_PER_ROUND)
            .collect(Collectors.toList());
    }

    private List<ResearchQuestion> generateFallbackQuestions(DeepResearchContext context, int round) {
        List<ResearchQuestion> fallback = new ArrayList<>();
        String baseQuery = context.getOriginalQuery();

        fallback.add(new ResearchQuestion(baseQuery + " implementation approaches", "Implementation", "High"));
        fallback.add(new ResearchQuestion(baseQuery + " performance analysis", "Performance", "Medium"));
        fallback.add(new ResearchQuestion(baseQuery + " real-world applications", "Case-Study", "Medium"));

        return fallback;
    }

    private String createFallbackInsight(List<CitationResult> results) {
        if (results.isEmpty()) {
            return "Insufficient data available for analysis.";
        }

        StringBuilder fallback = new StringBuilder();
        fallback.append("Based on ").append(results.size()).append(" sources, ");

        
        Map<String, Long> domainCounts = results.stream()
            .collect(Collectors.groupingBy(CitationResult::getDomain, Collectors.counting()));

        if (!domainCounts.isEmpty()) {
            String topDomain = domainCounts.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse("various sources");
            fallback.append("primarily from ").append(topDomain).append(", ");
        }

        fallback.append("preliminary analysis suggests this topic requires further investigation with more specific search terms and authoritative sources.");

        return fallback.toString();
    }

    private String createFallbackSynthesis(ResearchResults results, DeepResearchContext context) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("Research Summary for: ").append(context.getOriginalQuery()).append("\n\n");

        
        for (Map.Entry<String, String> insight : results.getInsights().entrySet()) {
            fallback.append("• ").append(insight.getValue()).append("\n");
        }

        return fallback.toString();
    }

    private DeepResearchResult enhanceAndValidateResult(String narrative,
        ResearchResults results,
        DeepResearchContext context) {
        try {
            
            NarrativeQualityMetrics qualityMetrics = assessNarrativeQuality(narrative, context);

            
            String enhancedNarrative = narrative;
            if (qualityMetrics.needsEnhancement()) {
                enhancedNarrative = applyFinalNarrativeEnhancements(narrative, context, qualityMetrics);
            }

            
            String executiveSummary = generateExecutiveSummary(enhancedNarrative, results, context);

            
            String methodology = documentResearchMethodology(context, results);

            
            return new DeepResearchResult(
                context.getSessionId(),
                context.getOriginalQuery(),
                enhancedNarrative,
                executiveSummary,
                methodology,
                results,
                qualityMetrics,
                context.getConfig()
            );

        } catch (Exception e) {
            logger.warning("Result enhancement failed: " + e.getMessage());
            return createBasicResult(narrative, results, context);
        }
    }

    private NarrativeQualityMetrics assessNarrativeQuality(String narrative, DeepResearchContext context) {
        int wordCount = narrative != null ? narrative.split("\\s+").length : 0;
        boolean isComplete = wordCount >= TARGET_NARRATIVE_WORDS * 0.8;
        return new NarrativeQualityMetrics(narrative.length(), wordCount, isComplete);
    }

    private String applyFinalNarrativeEnhancements(String narrative,
        DeepResearchContext context,
        NarrativeQualityMetrics metrics) {
        
        return narrative; 
    }

    private String generateExecutiveSummary(String narrative,
        ResearchResults results,
        DeepResearchContext context) {
        try {
            StringBuilder prompt = new StringBuilder();
            prompt.append("Generate a comprehensive executive summary for the research on: \"")
                .append(context.getOriginalQuery()).append("\"\n\n");

            prompt.append("RESEARCH SCOPE:\n");
            prompt.append("- Total sources analyzed: ").append(results.getAllCitations().size()).append("\n");
            prompt.append("- Research insights generated: ").append(results.getInsights().size()).append("\n");
            prompt.append("- Narrative length: ").append(narrative.split("\\s+").length).append(" words\n\n");

            prompt.append("KEY FINDINGS OVERVIEW:\n");
            prompt.append(formatKeyInsights(results.getInsights())).append("\n\n");

            prompt.append("EXECUTIVE SUMMARY REQUIREMENTS:\n");
            prompt.append("1. Provide a concise overview (300-500 words)\n");
            prompt.append("2. Highlight 3-5 key findings with supporting evidence\n");
            prompt.append("3. Include quantitative metrics where available\n");
            prompt.append("4. Summarize practical implications and recommendations\n");
            prompt.append("5. Use professional, accessible language for stakeholders\n\n");

            prompt.append("Generate executive summary:\n");

            return llmClient.complete(prompt.toString(), String.class).structuredOutput();

        } catch (Exception e) {
            logger.warning("Executive summary generation failed: " + e.getMessage());
            return generateFallbackExecutiveSummary(narrative, results, context);
        }
    }

    private String formatKeyInsights(Map<String, String> insights) {
        return insights.entrySet().stream()
            .limit(5)
            .map(entry -> String.format("- %s: %s",
                truncateString(entry.getKey(), 80),
                truncateString(entry.getValue(), 150)))
            .collect(Collectors.joining("\n"));
    }

    private String generateFallbackExecutiveSummary(String narrative,
        ResearchResults results,
        DeepResearchContext context) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("# Executive Summary\n\n");
        fallback.append("## Research Topic: ").append(context.getOriginalQuery()).append("\n\n");

        fallback.append("## Key Statistics\n");
        fallback.append("- Sources Analyzed: ").append(results.getAllCitations().size()).append("\n");
        fallback.append("- Research Insights: ").append(results.getInsights().size()).append("\n");
        fallback.append("- Narrative Length: ").append(narrative.split("\\s+").length).append(" words\n\n");

        fallback.append("## Primary Findings\n");
        results.getInsights().entrySet().stream()
            .limit(3)
            .forEach(entry -> fallback.append("- ").append(truncateString(entry.getValue(), 200)).append("\n"));

        return fallback.toString();
    }

    private String documentResearchMethodology(DeepResearchContext context, ResearchResults results) {
        StringBuilder methodology = new StringBuilder();

        methodology.append("# Research Methodology Report\n\n");
        methodology.append("## Research Overview\n");
        methodology.append("- **Research Topic**: ").append(context.getOriginalQuery()).append("\n");
        methodology.append("- **Session ID**: ").append(context.getSessionId()).append("\n");
        methodology.append("- **Research Depth**: ").append(context.getConfig().getResearchDepth()).append("\n");
        methodology.append("- **Total Sources**: ").append(results.getAllCitations().size()).append("\n");
        methodology.append("- **Research Insights**: ").append(results.getInsights().size()).append("\n\n");

        methodology.append("## Multi-Round Research Approach\n");
        methodology.append("This research employed a systematic multi-round approach:\n\n");

        methodology.append("### Round 1: Foundational Research\n");
        methodology.append("- Focus: Core concepts, definitions, and basic implementation approaches\n");
        methodology.append("- Strategy: Broad exploration of fundamental topics\n\n");

        methodology.append("### Round 2: Technical Deep-Dive\n");
        methodology.append("- Focus: Advanced implementations, performance analysis, and technical specifications\n");
        methodology.append("- Strategy: Targeted investigation of complex technical aspects\n\n");

        methodology.append("### Round 3: Expert Analysis & Synthesis\n");
        methodology.append("- Focus: Edge cases, emerging trends, comparative analysis, and expert insights\n");
        methodology.append("- Strategy: Synthesis of findings with expert-level analysis\n\n");

        methodology.append("## Research Parameters\n");
        methodology.append("- **Maximum Research Rounds**: ").append(MAX_RESEARCH_ROUNDS).append("\n");
        methodology.append("- **Questions Per Round**: Up to ").append(MAX_QUESTIONS_PER_ROUND).append("\n");
        methodology.append("- **Target Narrative Length**: ").append(TARGET_NARRATIVE_WORDS).append(" words\n");
        methodology.append("- **Session Timeout**: ").append(SESSION_TIMEOUT_MINUTES).append(" minutes\n");

        return methodology.toString();
    }

    private DeepResearchResult createBasicResult(String narrative,
        ResearchResults results,
        DeepResearchContext context) {
        return new DeepResearchResult(
            context.getSessionId(),
            context.getOriginalQuery(),
            narrative,
            "Summary not available",
            "Methodology not available",
            results,
            new NarrativeQualityMetrics(0, 0, false),
            context.getConfig()
        );
    }

    private DeepResearchResult createFallbackResult(String originalQuery, Exception error) {
        return new DeepResearchResult(
            "fallback_session",
            originalQuery,
            "Research failed: " + error.getMessage(),
            "Error occurred",
            "Error occurred",
            new ResearchResults(new ArrayList<>(), new HashMap<>()),
            new NarrativeQualityMetrics(0, 0, false),
            DeepResearchConfig.createDefault()
        );
    }

    
    private DeepResearchContext initializeResearchContext(String sessionId, String query, DeepResearchConfig config) {
        return new DeepResearchContext(sessionId, query, config);
    }

    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private void scheduleSessionCleanup() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            Instant cutoff = Instant.now().minus(Duration.ofMinutes(SESSION_TIMEOUT_MINUTES));
            activeSessions.entrySet().removeIf(entry ->
                entry.getValue().getStartTime().isBefore(cutoff));
        }, 10, 10, TimeUnit.MINUTES);
    }

    
    public void shutdown() {
        try {
            logger.info("Shutting down DeepResearchEngine...");

            researchSupervisor.shutdown();
            mainExecutor.shutdown();
            scheduledExecutor.shutdown();

            if (!mainExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                mainExecutor.shutdownNow();
            }
            if (!scheduledExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                scheduledExecutor.shutdownNow();
            }

            activeSessions.clear();
            logger.info("DeepResearchEngine shutdown completed");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Shutdown interrupted");
        }
    }

    
    private static class DeepResearchSession {
        private final DeepResearchContext context;
        private final Instant startTime;

        public DeepResearchSession(DeepResearchContext context, Instant startTime) {
            this.context = context;
            this.startTime = startTime;
        }

        public DeepResearchContext getContext() { return context; }
        public Instant getStartTime() { return startTime; }
    }

    private static class ResearchMetricsCollector {
        private final Map<String, Object> metrics = new ConcurrentHashMap<>();

        public void recordSession(String sessionId, Duration duration, DeepResearchResult result) {
            metrics.put(sessionId + "_duration", duration.toMinutes());
            metrics.put(sessionId + "_sources", result.getResults().getAllCitations().size());
            metrics.put(sessionId + "_words", result.getNarrative().split("\\s+").length);
        }

        public Map<String, Object> getMetrics() {
            return new HashMap<>(metrics);
        }
    }

    private static class NarrativeQualityMetrics {
        private final int characterCount;
        private final int wordCount;
        private final boolean isComplete;

        public NarrativeQualityMetrics(int characterCount, int wordCount, boolean isComplete) {
            this.characterCount = characterCount;
            this.wordCount = wordCount;
            this.isComplete = isComplete;
        }

        public boolean needsEnhancement() {
            return wordCount < TARGET_NARRATIVE_WORDS * 0.8 || !isComplete;
        }

        public int getCharacterCount() { return characterCount; }
        public int getWordCount() { return wordCount; }
        public boolean isComplete() { return isComplete; }
    }
}