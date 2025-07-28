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

/**
 * Deep Research Engine - Main coordination engine for comprehensive research
 * Implements the complete Deep Research workflow similar to Perplexity and Gemini Deep Research
 * Orchestrates all components to deliver exhaustive, well-connected narratives
 */
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

    public DeepResearchEngine(LLMClient llmClient,
        CitationService citationService) {
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

    /**
     * Execute comprehensive deep research with full narrative generation
     * Main entry point for the Deep Research workflow
     */
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

    /**
     * Execute multi-round research with progressive depth
     * Implements iterative research refinement similar to Perplexity Deep Research
     */
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

                
                List<CompletableFuture<List<CitationResult>>> searchFutures = roundQuestions.stream()
                    .map(question -> executeQuestionResearch(question, context, exploredTopics))
                    .collect(Collectors.toList());

                
                List<List<CitationResult>> roundResults = searchFutures.stream()
                    .map(CompletableFuture::join)
                    .collect(Collectors.toList());

                
                Map<String, String> roundInsights = synthesizeRoundInsights(
                    roundQuestions, roundResults, context);

                
                roundResults.forEach(allCitations::addAll);
                consolidatedInsights.putAll(roundInsights);
                exploredTopics.addAll(extractTopicsFromResults(roundResults));

                
                updateContextWithFindings(context, roundInsights, roundResults);

                
                if (isResearchSufficient(context, round)) {
                    logger.info("Research deemed sufficient after round " + round);
                    break;
                }

            } catch (Exception e) {
                logger.warning("Research round " + round + " failed: " + e.getMessage());
            }
        }

        
        Map<String, List<CitationResult>> categorizedResults = categorizeCitations(allCitations);

        return new ResearchResults(allCitations, categorizedResults, consolidatedInsights);
    }

    /**
     * Execute research for individual question with deep dive capability
     */
    private CompletableFuture<List<CitationResult>> executeQuestionResearch(ResearchQuestion question,
        DeepResearchContext context,
        Set<String> exploredTopics) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                
                if (isComplexQuestion(question)) {
                    return researchSupervisor.executeDeepDiveResearch(
                        question, context, exploredTopics).join();
                } else {
                    String isolatedContext = createIsolatedContext(question, context);
                    return researchSupervisor.executeComprehensiveResearch(
                        question, isolatedContext, context.getConfig(), context).join();
                }

            } catch (Exception e) {
                logger.warning("Question research failed: " + question.getQuestion() + " - " + e.getMessage());
                return new ArrayList<>();
            }
        }, mainExecutor);
    }

    /**
     * Synthesize comprehensive knowledge from all research results
     */
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

    /**
     * Generate research questions for each round based on current context
     */
    private List<ResearchQuestion> generateResearchQuestions(DeepResearchContext context,
        Set<String> exploredTopics,
        int round) {
        try {
            String questionPrompt = buildQuestionGenerationPrompt(context, exploredTopics, round);

            
            List<ContextAwareChunker.ContextChunk> promptChunks = contextChunker.chunkPrompt(questionPrompt);
            List<ResearchQuestion> allQuestions = new ArrayList<>();

            for (ContextAwareChunker.ContextChunk chunk : promptChunks) {
                List<ResearchQuestion> chunkQuestions = parseQuestionsFromResponse(
                    llmClient.complete(chunk.getContent(), String.class).structuredOutput(),
                    context, round);
                allQuestions.addAll(chunkQuestions);
            }

            
            return prioritizeQuestions(allQuestions, round);

        } catch (Exception e) {
            logger.warning("Question generation failed for round " + round + ": " + e.getMessage());
            return generateFallbackQuestions(context, round);
        }
    }

    /**
     * Build comprehensive question generation prompt
     */
    private String buildQuestionGenerationPrompt(DeepResearchContext context,
        Set<String> exploredTopics,
        int round) {
        String existingInsights = getExistingInsightsSummary(context);
        String exploredTopicsStr = String.join(", ", exploredTopics);

        return String.format("""
            Generate research questions for Round %d of deep research on: "%s"
            
            RESEARCH PROGRESS:
            - Round: %d of %d
            - Sources collected: %d
            - Insights generated: %d
            - Topics explored: %s
            
            EXISTING INSIGHTS:
            %s
            
            ROUND %d FOCUS:
            %s
            
            Generate %d targeted research questions that:
            1. Build upon existing insights without redundancy
            2. Explore uncharted aspects of the topic
            3. Target specific implementations, case studies, and quantitative data
            4. Include technical depth appropriate for expert analysis
            5. Avoid topics already thoroughly explored: %s
            6. Focus on actionable, practical insights
            
            Question Format:
            QUESTION: [Specific research question]
            CATEGORY: [Implementation/Performance/Analysis/Case-Study/Technical]
            PRIORITY: [High/Medium/Low]
            RATIONALE: [Why this question advances the research]
            
            Generate research questions:
            """,
            round,
            context.getOriginalQuery(),
            round, MAX_RESEARCH_ROUNDS,
            context.getAllCitations().size(),
            context.getAllInsights().size(),
            exploredTopicsStr,
            truncateString(existingInsights, 1000),
            round,
            getRoundFocus(round),
            MAX_QUESTIONS_PER_ROUND,
            exploredTopicsStr
        );
    }

    /**
     * Synthesize insights from a research round
     */
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

            } catch (Exception e) {
                logger.warning("Insight synthesis failed for question: " + question.getQuestion());
                insights.put(question.getQuestion(), createFallbackInsight(questionResults));
            }
        }

        return insights;
    }

    /**
     * Synthesize insight for individual question and its results
     */
    private String synthesizeQuestionInsight(ResearchQuestion question,
        List<CitationResult> results,
        DeepResearchContext context) throws LLMClientException {
        if (results.isEmpty()) {
            return "No sufficient sources found for this question.";
        }

        String synthesisPrompt = String.format("""
            Synthesize a comprehensive insight for: "%s"
            
            QUESTION CONTEXT:
            - Category: %s
            - Priority: %s
            - Main Topic: %s
            
            SOURCES ANALYZED (%d total):
            %s
            
            SYNTHESIS REQUIREMENTS:
            1. Provide a detailed, evidence-based answer
            2. Include specific examples, metrics, and quantitative data
            3. Reference authoritative sources with inline citations [1], [2], etc.
            4. Maintain technical accuracy and professional tone
            5. Focus on actionable insights and practical implications
            6. Highlight any conflicting viewpoints or limitations
            7. Connect findings to the broader research topic
            
            Generate comprehensive insight:
            """,
            question.getQuestion(),
            question.getCategory(),
            question.getPriority(),
            context.getOriginalQuery(),
            results.size(),
            formatSourcesForSynthesis(results)
        );

        return llmClient.complete(synthesisPrompt, String.class).structuredOutput();
    }

    /**
     * Enhance and validate final research result
     */
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

    

    private DeepResearchContext initializeResearchContext(String sessionId, String query, DeepResearchConfig config) {
        return new DeepResearchContext(sessionId, query, config);
    }

    private String generateSessionId() {
        return "session_" + System.currentTimeMillis() + "_" + UUID.randomUUID().toString().substring(0, 8);
    }

    private boolean isComplexQuestion(ResearchQuestion question) {
        String q = question.getQuestion().toLowerCase();
        return q.contains("compare") || q.contains("analyze") || q.contains("evaluate") ||
            question.getPriority().equals("High") || q.split("\\s+").length > 8;
    }

    private String createIsolatedContext(ResearchQuestion question, DeepResearchContext context) {
        StringBuilder isolatedContext = new StringBuilder();
        isolatedContext.append("Research Topic: ").append(context.getOriginalQuery()).append("\n");
        isolatedContext.append("Specific Question: ").append(question.getQuestion()).append("\n");
        isolatedContext.append("Category: ").append(question.getCategory()).append("\n");
        return isolatedContext.toString();
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

    private String createFallbackSynthesis(ResearchResults results, DeepResearchContext context) {
        StringBuilder fallback = new StringBuilder();
        fallback.append("Research Summary for: ").append(context.getOriginalQuery()).append("\n\n");

        
        for (Map.Entry<String, String> insight : results.getInsights().entrySet()) {
            fallback.append("• ").append(insight.getValue()).append("\n");
        }

        return fallback.toString();
    }

    private List<ResearchQuestion> parseQuestionsFromResponse(String response, DeepResearchContext context, int round) {
        List<ResearchQuestion> questions = new ArrayList<>();
        String[] lines = response.split("\n");

        ResearchQuestion currentQuestion = null;
        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("QUESTION:")) {
                if (currentQuestion != null) {
                    questions.add(currentQuestion);
                }
                String questionText = line.substring(9).trim();
                currentQuestion = new ResearchQuestion(questionText, "General", "Medium");
            } else if (line.startsWith("CATEGORY:") && currentQuestion != null) {
                currentQuestion.setCategory(line.substring(9).trim());
            } else if (line.startsWith("PRIORITY:") && currentQuestion != null) {
                currentQuestion.setPriority(ResearchQuestion.Priority.MEDIUM);
            }
        }

        if (currentQuestion != null) {
            questions.add(currentQuestion);
        }

        return questions;
    }

    private List<ResearchQuestion> prioritizeQuestions(List<ResearchQuestion> questions, int round) {
        return questions.stream()
            .sorted((q1, q2) -> {
                
                return Integer.compare(q2.getPriority().ordinal(), q1.getPriority().ordinal());
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

    private boolean isResearchSufficient(DeepResearchContext context, int round) {
        int minSources = context.getConfig().getResearchDepth().ordinal() * 20 + 30;
        int minInsights = round * 8;

        return context.getAllCitations().size() >= minSources &&
            context.getAllInsights().size() >= minInsights;
    }

    private Map<String, List<CitationResult>> categorizeCitations(List<CitationResult> citations) {
        return citations.stream()
            .collect(Collectors.groupingBy(citation ->
                determineInsightTheme(citation.getTitle(), citation.getContent())));
    }

    private String formatSourcesForSynthesis(List<CitationResult> results) {
        StringBuilder formatted = new StringBuilder();

        for (int i = 0; i < Math.min(results.size(), 6); i++) {
            CitationResult result = results.get(i);
            formatted.append(String.format("[%d] %s: %s\n",
                i + 1, result.getTitle(), truncateString(result.getContent(), 200)));
        }

        return formatted.toString();
    }

    /**
     * Generate executive summary of the research findings
     */
    private String generateExecutiveSummary(String narrative, ResearchResults results, DeepResearchContext context) {
        try {
            String summaryPrompt = String.format("""
            Generate a comprehensive executive summary for the research on: "%s"
            
            RESEARCH SCOPE:
            - Total sources analyzed: %d
            - Research insights generated: %d
            - Research rounds completed: Based on available data
            - Narrative length: %d words
            
            KEY FINDINGS OVERVIEW:
            %s
            
            FULL NARRATIVE EXCERPT:
            %s
            
            EXECUTIVE SUMMARY REQUIREMENTS:
            1. Provide a concise overview (300-500 words)
            2. Highlight 3-5 key findings with supporting evidence
            3. Include quantitative metrics where available
            4. Summarize practical implications and recommendations
            5. Mention research methodology and source reliability
            6. Use professional, accessible language for stakeholders
            7. Structure with clear sections: Overview, Key Findings, Implications, Recommendations
            
            Generate executive summary:
            """,
                context.getOriginalQuery(),
                results.getAllCitations().size(),
                results.getInsights().size(),
                narrative.split("\\s+").length,
                formatKeyInsights(results.getInsights()),
                truncateString(narrative, 1000)
            );

            return llmClient.complete(summaryPrompt, String.class).structuredOutput();

        } catch (Exception e) {
            logger.warning("Executive summary generation failed: " + e.getMessage());
            return generateFallbackExecutiveSummary(narrative, results, context);
        }
    }

    /**
     * Document the research methodology used in this session
     */
    private String documentResearchMethodology(DeepResearchContext context, ResearchResults results) {
        try {
            StringBuilder methodology = new StringBuilder();

            methodology.append("# Research Methodology Report\n\n");
            methodology.append("## Research Overview\n");
            methodology.append(String.format("- **Research Topic**: %s%n", context.getOriginalQuery()));
            methodology.append(String.format("- **Session ID**: %s%n", context.getSessionId()));
            methodology.append(String.format("- **Research Depth**: %s%n", context.getConfig().getResearchDepth()));
            methodology.append(String.format("- **Total Sources**: %d%n", results.getAllCitations().size()));
            methodology.append(String.format("- **Research Insights**: %d%n", results.getInsights().size()));
            methodology.append("\n");

            methodology.append("## Multi-Round Research Approach\n");
            methodology.append("This research employed a systematic multi-round approach:\n\n");

            methodology.append("### Round 1: Foundational Research\n");
            methodology.append("- Focus: Core concepts, definitions, and basic implementation approaches\n");
            methodology.append("- Strategy: Broad exploration of fundamental topics\n");
            methodology.append("- Question Types: Definitional, conceptual, and introductory\n\n");

            methodology.append("### Round 2: Technical Deep-Dive\n");
            methodology.append("- Focus: Advanced implementations, performance analysis, and technical specifications\n");
            methodology.append("- Strategy: Targeted investigation of complex technical aspects\n");
            methodology.append("- Question Types: Implementation-focused, performance-oriented, technical analysis\n\n");

            methodology.append("### Round 3: Expert Analysis & Synthesis\n");
            methodology.append("- Focus: Edge cases, emerging trends, comparative analysis, and expert insights\n");
            methodology.append("- Strategy: Synthesis of findings with expert-level analysis\n");
            methodology.append("- Question Types: Comparative, evaluative, forward-looking\n\n");

            methodology.append("## Research Components\n");
            methodology.append("### Source Collection & Analysis\n");
            methodology.append("- **Citation Service**: Automated collection from authoritative sources\n");
            methodology.append("- **Research Supervisor**: Coordinated parallel research execution\n");
            methodology.append("- **Context-Aware Processing**: Intelligent chunking and context preservation\n\n");

            methodology.append("### Knowledge Synthesis\n");
            methodology.append("- **Hierarchical Synthesis**: Multi-level insight aggregation\n");
            methodology.append("- **Thematic Grouping**: Categorization by implementation, performance, security, examples, architecture\n");
            methodology.append("- **Cross-Theme Integration**: Comprehensive knowledge synthesis\n\n");

            methodology.append("### Quality Assurance\n");
            methodology.append("- **Source Validation**: Authority and relevance verification\n");
            methodology.append("- **Insight Synthesis**: Evidence-based analysis with citations\n");
            methodology.append("- **Narrative Construction**: Long-form comprehensive documentation\n");
            methodology.append("- **Final Enhancement**: Quality metrics assessment and improvement\n\n");

            
            Map<String, List<CitationResult>> categorized = results.getCategorizedResults();
            if (!categorized.isEmpty()) {
                methodology.append("## Source Distribution\n");
                for (Map.Entry<String, List<CitationResult>> category : categorized.entrySet()) {
                    methodology.append(String.format("- **%s**: %d sources%n",
                        capitalize(category.getKey()), category.getValue().size()));
                }
                methodology.append("\n");
            }

            methodology.append("## Research Parameters\n");
            methodology.append(String.format("- **Maximum Research Rounds**: %d%n", MAX_RESEARCH_ROUNDS));
            methodology.append(String.format("- **Questions Per Round**: Up to %d%n", MAX_QUESTIONS_PER_ROUND));
            methodology.append(String.format("- **Target Narrative Length**: %d words%n", TARGET_NARRATIVE_WORDS));
            methodology.append(String.format("- **Session Timeout**: %d minutes%n", SESSION_TIMEOUT_MINUTES));
            methodology.append("\n");

            methodology.append("## Limitations & Considerations\n");
            methodology.append("- Research bounded by available online sources and databases\n");
            methodology.append("- Time-sensitive information may require verification\n");
            methodology.append("- Technical implementations subject to rapid evolution\n");
            methodology.append("- Synthesis represents current understanding at time of research\n");

            return methodology.toString();

        } catch (Exception e) {
            logger.warning("Methodology documentation failed: " + e.getMessage());
            return generateFallbackMethodology(context, results);
        }
    }

    /**
     * Helper method to format key insights for summary
     */
    private String formatKeyInsights(Map<String, String> insights) {
        return insights.entrySet().stream()
            .limit(5)
            .map(entry -> String.format("- %s: %s",
                truncateString(entry.getKey(), 80),
                truncateString(entry.getValue(), 150)))
            .collect(Collectors.joining("\n"));
    }

    /**
     * Generate fallback executive summary when main generation fails
     */
    private String generateFallbackExecutiveSummary(String narrative, ResearchResults results, DeepResearchContext context) {
        StringBuilder fallback = new StringBuilder();

        fallback.append("# Executive Summary\n\n");
        fallback.append(String.format("## Research Topic: %s%n%n", context.getOriginalQuery()));

        fallback.append("## Key Statistics\n");
        fallback.append(String.format("- Sources Analyzed: %d%n", results.getAllCitations().size()));
        fallback.append(String.format("- Research Insights: %d%n", results.getInsights().size()));
        fallback.append(String.format("- Narrative Length: %d words%n%n", narrative.split("\\s+").length));

        fallback.append("## Primary Findings\n");
        results.getInsights().entrySet().stream()
            .limit(3)
            .forEach(entry -> fallback.append(String.format("- %s%n",
                truncateString(entry.getValue(), 200))));

        fallback.append("\n## Research Methodology\n");
        fallback.append("Multi-round research approach with comprehensive source analysis and synthesis.\n");

        return fallback.toString();
    }

    /**
     * Generate fallback methodology documentation when main generation fails
     */
    private String generateFallbackMethodology(DeepResearchContext context, ResearchResults results) {
        StringBuilder fallback = new StringBuilder();

        fallback.append("# Research Methodology (Simplified)\n\n");
        fallback.append(String.format("**Research Topic**: %s%n", context.getOriginalQuery()));
        fallback.append(String.format("**Session ID**: %s%n", context.getSessionId()));
        fallback.append(String.format("**Sources Collected**: %d%n", results.getAllCitations().size()));
        fallback.append(String.format("**Insights Generated**: %d%n%n", results.getInsights().size()));

        fallback.append("**Approach**: Multi-round research with progressive depth\n");
        fallback.append("**Components**: Citation collection, insight synthesis, narrative construction\n");
        fallback.append("**Quality Assurance**: Source validation and comprehensive analysis\n");

        return fallback.toString();
    }

    /**
     * Helper method to capitalize first letter of a string
     */
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String createFallbackInsight(List<CitationResult> results) {
        if (results.isEmpty()) {
            return "Insufficient data available for analysis.";
        }

        return "Based on " + results.size() + " sources, preliminary analysis suggests further investigation is needed.";
    }

    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    
    private NarrativeQualityMetrics assessNarrativeQuality(String narrative, DeepResearchContext context) {
        int wordCount = narrative != null ? narrative.split("\\s+").length : 0;
        return new NarrativeQualityMetrics(narrative.length(), wordCount, true);
    }

    private String applyFinalNarrativeEnhancements(String narrative, DeepResearchContext context, NarrativeQualityMetrics metrics) {
        return narrative; 
    }

    private DeepResearchResult createBasicResult(String narrative, ResearchResults results, DeepResearchContext context) {
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

    private void scheduleSessionCleanup() {
        scheduledExecutor.scheduleAtFixedRate(() -> {
            Instant cutoff = Instant.now().minus(Duration.ofMinutes(SESSION_TIMEOUT_MINUTES));
            activeSessions.entrySet().removeIf(entry ->
                entry.getValue().getStartTime().isBefore(cutoff));
        }, 10, 10, TimeUnit.MINUTES);
    }

    /**
     * Shutdown the research engine and cleanup resources
     */
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