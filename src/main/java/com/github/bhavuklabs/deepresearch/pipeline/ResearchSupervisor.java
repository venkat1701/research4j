package com.github.bhavuklabs.deepresearch.pipeline;

import java.util.*;
import java.util.concurrent.*;
import java.util.stream.Collectors;
import java.util.logging.Logger;

import com.github.bhavuklabs.citation.CitationResult;
import com.github.bhavuklabs.citation.service.CitationService;
import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.deepresearch.context.DeepResearchContext;
import com.github.bhavuklabs.deepresearch.models.ContextAwareQueryGenerator;
import com.github.bhavuklabs.deepresearch.models.DeepResearchConfig;
import com.github.bhavuklabs.deepresearch.models.ResearchGapAnalysis;
import com.github.bhavuklabs.deepresearch.models.ResearchQualityAnalyzer;
import com.github.bhavuklabs.deepresearch.models.ResearchQuery;
import com.github.bhavuklabs.deepresearch.models.ResearchQuestion;


public class ResearchSupervisor {

    private static final Logger logger = Logger.getLogger(ResearchSupervisor.class.getName());

    
    private static final int MAX_PARALLEL_SEARCHES = 8;
    private static final int MAX_RESEARCH_ITERATIONS = 5;
    private static final int CONTEXT_WINDOW_LIMIT = 32000;
    private static final int SEARCH_RESULT_LIMIT = 15;
    private static final long SEARCH_RATE_LIMIT_MS = 500;
    private static final long DEEP_SEARCH_RATE_LIMIT_MS = 1000;

    private final LLMClient llmClient;
    private final CitationService citationService;
    private final ExecutorService researchExecutor;
    private final ScheduledExecutorService rateLimitExecutor;
    private final ContextAwareQueryGenerator queryGenerator;
    private final ResearchQualityAnalyzer qualityAnalyzer;

    
    private final Map<String, Long> lastSearchTimes = new ConcurrentHashMap<>();

    public ResearchSupervisor(LLMClient llmClient,
        CitationService citationService,
        ExecutorService researchExecutor) {
        this.llmClient = llmClient;
        this.citationService = citationService;
        this.researchExecutor = researchExecutor;
        this.rateLimitExecutor = Executors.newScheduledThreadPool(2);
        this.queryGenerator = new ContextAwareQueryGenerator(llmClient);
        this.qualityAnalyzer = new ResearchQualityAnalyzer();

        logger.info("ResearchSupervisor initialized with " + MAX_PARALLEL_SEARCHES + " parallel search capacity");
    }

    
    public CompletableFuture<List<CitationResult>> executeComprehensiveResearch(
        ResearchQuestion question,
        String isolatedContext,
        DeepResearchConfig config,
        DeepResearchContext globalContext) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Executing comprehensive research for: " +
                    truncateString(question.getQuestion(), 80));

                
                List<ResearchQuery> researchQueries = generateMultiDimensionalQueries(
                    question, isolatedContext, config);

                if (researchQueries.isEmpty()) {
                    logger.warning("No research queries generated for question: " + question.getQuestion());
                    return executeBasicSearch(question.getQuestion());
                }

                
                List<CitationResult> initialResults = executeParallelSearches(researchQueries);

                if (initialResults.isEmpty()) {
                    logger.warning("No initial results found, trying basic search");
                    return executeBasicSearch(question.getQuestion());
                }

                
                List<CitationResult> refinedResults = executeIterativeRefinement(
                    initialResults, question, isolatedContext, config);

                
                List<CitationResult> qualityResults = qualityAnalyzer.filterAndRankResults(
                    refinedResults, question, config);

                
                List<CitationResult> enhancedResults = enhanceResultsWithContext(
                    qualityResults, question, globalContext);

                logger.info("Comprehensive research completed: " + enhancedResults.size() +
                    " high-quality sources identified for question: " + truncateString(question.getQuestion(), 50));

                return enhancedResults;

            } catch (Exception e) {
                logger.severe("Comprehensive research failed for question '" +
                    truncateString(question.getQuestion(), 50) + "': " + e.getMessage());
                return executeEmergencyFallbackResearch(question, isolatedContext);
            }
        }, researchExecutor);
    }

    
    public CompletableFuture<List<CitationResult>> executeDeepDiveResearch(
        ResearchQuestion question,
        DeepResearchContext context,
        Set<String> exploredTopics) {

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.info("Executing deep dive research for: " +
                    truncateString(question.getQuestion(), 80));

                
                List<ResearchQuery> deepQueries = generateDeepDiveQueries(
                    question, context, exploredTopics);

                if (deepQueries.isEmpty()) {
                    logger.warning("No deep dive queries generated, falling back to standard research");
                    return executeStandardResearch(question, context);
                }

                
                List<CitationResult> deepResults = executeMultiStepResearch(deepQueries, context);

                
                List<CitationResult> validatedResults = validateCrossReferences(
                    deepResults, question, context);

                
                Map<String, List<CitationResult>> clusteredResults = clusterResultsBySemantics(
                    validatedResults, question);

                
                List<CitationResult> gapFillingResults = identifyAndFillResearchGaps(
                    clusteredResults, question, context);

                
                List<CitationResult> synthesizedResults = synthesizeDeepResults(
                    validatedResults, gapFillingResults, question);

                logger.info("Deep dive research completed: " + synthesizedResults.size() +
                    " comprehensive sources identified");

                return synthesizedResults;

            } catch (Exception e) {
                logger.severe("Deep dive research failed: " + e.getMessage());
                return executeStandardResearch(question, context);
            }
        }, researchExecutor);
    }

    
    private List<ResearchQuery> generateMultiDimensionalQueries(ResearchQuestion question,
        String isolatedContext,
        DeepResearchConfig config) {
        try {
            String queryGenerationPrompt = buildQueryGenerationPrompt(question, isolatedContext, config);

            
            List<String> promptChunks = chunkPromptForContext(queryGenerationPrompt);
            List<ResearchQuery> allQueries = new ArrayList<>();

            for (String chunk : promptChunks) {
                try {
                    LLMResponse<String> response = llmClient.complete(chunk, String.class);
                    List<ResearchQuery> chunkQueries = parseResearchQueries(response.structuredOutput(), question);
                    allQueries.addAll(chunkQueries);
                } catch (Exception e) {
                    logger.warning("Failed to generate queries for chunk: " + e.getMessage());
                }
            }

            
            List<ResearchQuery> finalQueries = prioritizeAndDeduplicateQueries(allQueries, config);

            logger.info("Generated " + finalQueries.size() + " research queries for question: " +
                truncateString(question.getQuestion(), 50));

            return finalQueries;

        } catch (Exception e) {
            logger.warning("Multi-dimensional query generation failed: " + e.getMessage());
            return generateFallbackQueries(question, isolatedContext);
        }
    }

    
    private String buildQueryGenerationPrompt(ResearchQuestion question,
        String isolatedContext,
        DeepResearchConfig config) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Generate comprehensive research queries for the question: \"")
            .append(question.getQuestion()).append("\"\n\n");

        prompt.append("RESEARCH CONTEXT:\n");
        prompt.append("- Category: ").append(question.getCategory()).append("\n");
        prompt.append("- Priority: ").append(question.getPriority()).append("\n");
        prompt.append("- Research Depth: ").append(config.getResearchDepth()).append("\n");
        prompt.append("- Complexity Score: ").append(String.format("%.2f", question.getComplexityScore())).append("\n\n");

        if (isolatedContext != null && !isolatedContext.trim().isEmpty()) {
            prompt.append("ISOLATED CONTEXT:\n");
            prompt.append(truncateString(isolatedContext, 1000)).append("\n\n");
        }

        prompt.append("Generate 4-6 diverse research queries that:\n");
        prompt.append("1. Target specific implementations, case studies, and technical documentation\n");
        prompt.append("2. Include domain-specific terminology and technical specifications\n");
        prompt.append("3. Focus on authoritative sources (academic, industry, government)\n");
        prompt.append("4. Explore different aspects: implementation, performance, challenges, examples\n");
        prompt.append("5. Avoid generic terms that would return broad, unfocused results\n");
        prompt.append("6. Include quantitative research (metrics, benchmarks, studies)\n\n");

        prompt.append("Query Format:\n");
        prompt.append("QUERY: [Specific search query with technical terms]\n");
        prompt.append("TYPE: [Implementation/Performance/Case-Study/Technical/Academic]\n");
        prompt.append("PRIORITY: [High/Medium/Low]\n");
        prompt.append("EXPECTED_SOURCES: [Type of sources expected]\n");
        prompt.append("RATIONALE: [Why this query is important]\n\n");

        prompt.append("Generate targeted research queries:\n");

        return prompt.toString();
    }

    
    private List<CitationResult> executeParallelSearches(List<ResearchQuery> queries) {
        try {
            logger.info("Executing parallel searches for " + queries.size() + " queries");

            
            Map<String, List<ResearchQuery>> prioritizedQueries = queries.stream()
                .collect(Collectors.groupingBy(ResearchQuery::getPriority));

            List<CompletableFuture<List<CitationResult>>> searchFutures = new ArrayList<>();

            
            if (prioritizedQueries.containsKey("High")) {
                for (ResearchQuery query : prioritizedQueries.get("High")) {
                    searchFutures.add(executeRateLimitedSearch(query, SEARCH_RATE_LIMIT_MS));
                }
            }

            
            for (String priority : Arrays.asList("Medium", "Low")) {
                if (prioritizedQueries.containsKey(priority)) {
                    for (ResearchQuery query : prioritizedQueries.get(priority)) {
                        searchFutures.add(executeRateLimitedSearch(query, SEARCH_RATE_LIMIT_MS));
                    }
                }
            }

            
            List<CitationResult> allResults = searchFutures.stream()
                .map(future -> {
                    try {
                        return future.get(30, TimeUnit.SECONDS); 
                    } catch (Exception e) {
                        logger.warning("Search future failed or timed out: " + e.getMessage());
                        return new ArrayList<CitationResult>();
                    }
                })
                .flatMap(List::stream)
                .collect(Collectors.toList());

            
            List<CitationResult> uniqueResults = removeDuplicateCitations(allResults);
            uniqueResults.sort((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()));

            logger.info("Parallel search execution completed: " + uniqueResults.size() + " unique results");
            return uniqueResults;

        } catch (Exception e) {
            logger.warning("Parallel search execution failed: " + e.getMessage());
            return executeSequentialSearches(queries);
        }
    }

    
    private CompletableFuture<List<CitationResult>> executeRateLimitedSearch(ResearchQuery query,
        long rateLimitMs) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                
                String threadKey = Thread.currentThread().getName();
                long lastSearchTime = lastSearchTimes.getOrDefault(threadKey, 0L);
                long currentTime = System.currentTimeMillis();
                long timeSinceLastSearch = currentTime - lastSearchTime;

                if (timeSinceLastSearch < rateLimitMs) {
                    Thread.sleep(rateLimitMs - timeSinceLastSearch);
                }

                lastSearchTimes.put(threadKey, System.currentTimeMillis());

                
                List<CitationResult> results = executeSearchWithRetry(query);

                
                List<CitationResult> validResults = results.stream()
                    .filter(result -> result != null && result.isValid())
                    .filter(result -> result.getRelevanceScore() >= 0.3)
                    .filter(result -> result.getContent() != null && result.getContent().length() > 150)
                    .filter(this::isValidSource)
                    .limit(SEARCH_RESULT_LIMIT)
                    .collect(Collectors.toList());

                logger.info("Rate-limited search completed for query '" +
                    truncateString(query.getQuery(), 50) + "': " +
                    validResults.size() + " valid results");

                return validResults;

            } catch (Exception e) {
                logger.warning("Rate-limited search failed for query '" + query.getQuery() + "': " + e.getMessage());
                return new ArrayList<>();
            }
        }, researchExecutor);
    }

    
    private List<CitationResult> executeSearchWithRetry(ResearchQuery query) {
        int maxRetries = 3;
        int retryCount = 0;
        Exception lastException = null;

        while (retryCount < maxRetries) {
            try {
                logger.fine("Executing search (attempt " + (retryCount + 1) + "): " + query.getQuery());

                List<CitationResult> results = citationService.search(query.getQuery());

                if (results != null && !results.isEmpty()) {
                    return results;
                }

                
                if (retryCount < maxRetries - 1) {
                    String modifiedQuery = enhanceQuery(query.getQuery(), retryCount);
                    results = citationService.search(modifiedQuery);

                    if (results != null && !results.isEmpty()) {
                        return results;
                    }
                }

            } catch (Exception e) {
                lastException = e;
                logger.warning("Search attempt " + (retryCount + 1) + " failed for query '" +
                    query.getQuery() + "': " + e.getMessage());

                
                try {
                    Thread.sleep((long) Math.pow(2, retryCount) * 1000);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }

            retryCount++;
        }

        logger.warning("All search attempts failed for query: " + query.getQuery() +
            (lastException != null ? " - " + lastException.getMessage() : ""));
        return new ArrayList<>();
    }

    
    private String enhanceQuery(String originalQuery, int retryAttempt) {
        switch (retryAttempt) {
            case 0:
                return originalQuery + " tutorial";
            case 1:
                return originalQuery + " guide";
            case 2:
                return originalQuery.replace(" ", " AND ");
            default:
                return originalQuery;
        }
    }

    
    private List<CitationResult> removeDuplicateCitations(List<CitationResult> citations) {
        Map<String, CitationResult> uniqueCitations = new LinkedHashMap<>();
        Set<String> seenTitles = new HashSet<>();

        for (CitationResult citation : citations) {
            if (citation == null || !citation.isValid()) {
                continue;
            }

            String url = citation.getUrl();
            String title = citation.getTitle();

            
            if (url != null && uniqueCitations.containsKey(url)) {
                continue;
            }

            
            boolean similarTitleExists = seenTitles.stream()
                .anyMatch(seenTitle -> calculateTitleSimilarity(title, seenTitle) > 0.8);

            if (!similarTitleExists) {
                uniqueCitations.put(url != null ? url : title + "_" + System.nanoTime(), citation);
                if (title != null) {
                    seenTitles.add(title);
                }
            }
        }

        logger.info("Removed duplicates: " + citations.size() + " -> " + uniqueCitations.size() + " unique citations");
        return new ArrayList<>(uniqueCitations.values());
    }

    
    private double calculateTitleSimilarity(String title1, String title2) {
        if (title1 == null || title2 == null) {
            return 0.0;
        }

        String[] words1 = title1.toLowerCase().split("\\W+");
        String[] words2 = title2.toLowerCase().split("\\W+");

        Set<String> set1 = new HashSet<>(Arrays.asList(words1));
        Set<String> set2 = new HashSet<>(Arrays.asList(words2));

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        Set<String> union = new HashSet<>(set1);
        union.addAll(set2);

        return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();
    }

    
    private boolean isValidSource(CitationResult citation) {
        if (citation == null || citation.getUrl() == null) {
            return false;
        }

        String url = citation.getUrl().toLowerCase();
        String domain = citation.getDomain().toLowerCase();

        
        String[] blacklistedDomains = {
            "ads", "spam", "clickbait", "pinterest", "instagram",
            "facebook", "twitter", "tiktok", "reddit.com/r/", "quora"
        };

        for (String blacklisted : blacklistedDomains) {
            if (url.contains(blacklisted) || domain.contains(blacklisted)) {
                return false;
            }
        }

        
        String[] authoritativeDomains = {
            ".edu", ".gov", ".org", "wikipedia", "github", "stackoverflow",
            "medium", "docs.", "blog.", "research", "arxiv", "ieee"
        };

        boolean isAuthoritative = Arrays.stream(authoritativeDomains)
            .anyMatch(auth -> url.contains(auth) || domain.contains(auth));

        
        return isAuthoritative || citation.getRelevanceScore() >= 0.6;
    }

    
    private List<CitationResult> executeIterativeRefinement(List<CitationResult> initialResults,
        ResearchQuestion question,
        String isolatedContext,
        DeepResearchConfig config) {
        try {
            List<CitationResult> currentResults = new ArrayList<>(initialResults);

            logger.info("Starting iterative refinement with " + currentResults.size() + " initial results");

            for (int iteration = 0; iteration < MAX_RESEARCH_ITERATIONS; iteration++) {
                logger.info("Research refinement iteration " + (iteration + 1));

                
                ResearchGapAnalysis gapAnalysis = analyzeResearchGaps(currentResults, question);

                if (gapAnalysis.getGaps().isEmpty() || gapAnalysis.getOverallCoverageScore() > 0.8) {
                    logger.info("Research gaps filled or high coverage achieved, refinement complete");
                    break;
                }

                
                List<ResearchQuery> refinementQueries = generateGapFillingQueries(
                    gapAnalysis, question, isolatedContext);

                if (refinementQueries.isEmpty()) {
                    logger.info("No refinement queries generated, stopping iteration");
                    break;
                }

                
                List<CitationResult> refinementResults = executeParallelSearches(refinementQueries);

                if (refinementResults.isEmpty()) {
                    logger.info("No refinement results found, stopping iteration");
                    break;
                }

                
                currentResults = mergeAndDeduplicateResults(currentResults, refinementResults);

                
                if (qualityAnalyzer.isResearchSufficient(currentResults, question, config)) {
                    logger.info("Research quality threshold achieved at iteration " + (iteration + 1));
                    break;
                }

                logger.info("Iteration " + (iteration + 1) + " completed: " + currentResults.size() + " total results");
            }

            logger.info("Iterative refinement completed with " + currentResults.size() + " final results");
            return currentResults;

        } catch (Exception e) {
            logger.warning("Iterative refinement failed: " + e.getMessage());
            return initialResults;
        }
    }

    
    private ResearchGapAnalysis analyzeResearchGaps(List<CitationResult> results, ResearchQuestion question) {
        List<String> gaps = new ArrayList<>();
        Map<String, Double> coverageScores = new HashMap<>();

        
        List<String> expectedAreas = getExpectedCoverageAreas(question);

        for (String area : expectedAreas) {
            double coverage = calculateAreaCoverage(results, area);
            coverageScores.put(area, coverage);

            if (coverage < 0.5) { 
                gaps.add("Insufficient coverage of " + area);
            }
        }

        
        Set<String> domains = results.stream()
            .map(CitationResult::getDomain)
            .collect(Collectors.toSet());

        if (domains.size() < 3) {
            gaps.add("Limited source diversity (only " + domains.size() + " domains)");
        }

        
        long recentResults = results.stream()
            .mapToLong(citation -> {
                
                return citation.getRetrievedAt().isAfter(
                    java.time.LocalDateTime.now().minusMonths(6)) ? 1 : 0;
            })
            .sum();

        if (recentResults < results.size() * 0.3) {
            gaps.add("Limited recent sources");
        }

        return new ResearchGapAnalysis(gaps, coverageScores);
    }

    
    private List<String> getExpectedCoverageAreas(ResearchQuestion question) {
        String category = question.getCategory().toLowerCase();

        switch (category) {
            case "implementation":
                return Arrays.asList("code examples", "tutorials", "documentation", "best practices");
            case "performance":
                return Arrays.asList("benchmarks", "optimization", "metrics", "comparison");
            case "case-study":
                return Arrays.asList("real world examples", "success stories", "lessons learned", "applications");
            case "technical":
                return Arrays.asList("specifications", "architecture", "design patterns", "technical details");
            default:
                return Arrays.asList("overview", "examples", "analysis", "recommendations");
        }
    }

    
    private double calculateAreaCoverage(List<CitationResult> results, String area) {
        if (results.isEmpty()) {
            return 0.0;
        }

        long relevantResults = results.stream()
            .mapToLong(citation -> {
                String content = (citation.getTitle() + " " + citation.getContent()).toLowerCase();
                return content.contains(area.toLowerCase()) ? 1 : 0;
            })
            .sum();

        return (double) relevantResults / results.size();
    }

    
    private List<ResearchQuery> generateGapFillingQueries(ResearchGapAnalysis gapAnalysis,
        ResearchQuestion question,
        String context) {
        List<ResearchQuery> gapQueries = new ArrayList<>();

        for (String gap : gapAnalysis.getGaps()) {
            try {
                String gapQuery = generateQueryForGap(gap, question, context);
                if (gapQuery != null && !gapQuery.trim().isEmpty()) {
                    ResearchQuery query = new ResearchQuery(gapQuery, "Gap-Fill", "Medium", "");
                    query.setRationale("Filling research gap: " + gap);
                    gapQueries.add(query);
                }
            } catch (Exception e) {
                logger.warning("Failed to generate gap-filling query for: " + gap + " - " + e.getMessage());
            }
        }

        return gapQueries;
    }

    
    private String generateQueryForGap(String gap, ResearchQuestion question, String context) {
        String baseQuery = question.getQuestion();

        if (gap.contains("code examples")) {
            return baseQuery + " code example implementation";
        } else if (gap.contains("tutorials")) {
            return baseQuery + " step by step tutorial";
        } else if (gap.contains("benchmarks")) {
            return baseQuery + " performance benchmark comparison";
        } else if (gap.contains("real world examples")) {
            return baseQuery + " case study real world application";
        } else if (gap.contains("specifications")) {
            return baseQuery + " technical specification documentation";
        } else if (gap.contains("recent sources")) {
            return baseQuery + " 2024 2025 latest";
        } else if (gap.contains("source diversity")) {
            return baseQuery + " academic research paper";
        } else {
            return baseQuery + " comprehensive guide";
        }
    }

    
    private List<CitationResult> mergeAndDeduplicateResults(List<CitationResult> existing,
        List<CitationResult> newResults) {
        List<CitationResult> combined = new ArrayList<>(existing);
        combined.addAll(newResults);

        return removeDuplicateCitations(combined);
    }

    
    private List<ResearchQuery> generateDeepDiveQueries(ResearchQuestion question,
        DeepResearchContext context,
        Set<String> exploredTopics) {
        List<ResearchQuery> deepQueries = new ArrayList<>();

        try {
            
            if (question.requiresDeepResearch()) {
                List<String> searchQueries = question.generateSearchQueries();

                for (String searchQuery : searchQueries) {
                    
                    boolean isNewTopic = exploredTopics.stream()
                        .noneMatch(topic -> searchQuery.toLowerCase().contains(topic.toLowerCase()));

                    if (isNewTopic) {
                        ResearchQuery deepQuery = new ResearchQuery(
                            searchQuery, "Deep-Dive", "High", "Expert sources");
                        deepQuery.setRationale("Deep dive analysis for complex question");
                        deepQueries.add(deepQuery);
                    }
                }
            }

            
            String originalQuery = context.getOriginalQuery();
            deepQueries.add(new ResearchQuery(
                originalQuery + " " + question.getQuestion() + " advanced analysis",
                "Deep-Analysis", "High", ""));

        } catch (Exception e) {
            logger.warning("Failed to generate deep dive queries: " + e.getMessage());
        }

        return deepQueries.stream().limit(6).collect(Collectors.toList());
    }

    private List<CitationResult> executeMultiStepResearch(List<ResearchQuery> queries,
        DeepResearchContext context) {
        List<CitationResult> allResults = new ArrayList<>();

        for (ResearchQuery query : queries) {
            try {
                List<CitationResult> queryResults = executeSearchWithRetry(query);
                allResults.addAll(queryResults);

                
                Thread.sleep(DEEP_SEARCH_RATE_LIMIT_MS);

            } catch (Exception e) {
                logger.warning("Multi-step search failed for query: " + query.getQuery() + " - " + e.getMessage());
            }
        }

        return removeDuplicateCitations(allResults);
    }

    private List<CitationResult> validateCrossReferences(List<CitationResult> results,
        ResearchQuestion question,
        DeepResearchContext context) {
        
        return results.stream()
            .filter(result -> result.getRelevanceScore() >= 0.4)
            .filter(this::isValidSource)
            .collect(Collectors.toList());
    }

    private Map<String, List<CitationResult>> clusterResultsBySemantics(List<CitationResult> results,
        ResearchQuestion question) {
        Map<String, List<CitationResult>> clusters = new HashMap<>();

        for (CitationResult result : results) {
            String cluster = determineSemanticCluster(result, question);
            clusters.computeIfAbsent(cluster, k -> new ArrayList<>()).add(result);
        }

        return clusters;
    }

    private String determineSemanticCluster(CitationResult result, ResearchQuestion question) {
        String content = (result.getTitle() + " " + result.getContent()).toLowerCase();
        String category = question.getCategory().toLowerCase();

        
        if (content.contains("implement") || content.contains("code")) {
            return "implementation";
        } else if (content.contains("performance") || content.contains("benchmark")) {
            return "performance";
        } else if (content.contains("example") || content.contains("case")) {
            return "examples";
        } else if (content.contains("theory") || content.contains("concept")) {
            return "theory";
        } else {
            return category;
        }
    }

    private List<CitationResult> identifyAndFillResearchGaps(Map<String, List<CitationResult>> clusteredResults,
        ResearchQuestion question,
        DeepResearchContext context) {
        List<CitationResult> gapFillingResults = new ArrayList<>();

        
        Set<String> expectedClusters = Set.of("implementation", "theory", "examples", "performance");
        Set<String> actualClusters = clusteredResults.keySet();

        for (String expectedCluster : expectedClusters) {
            if (!actualClusters.contains(expectedCluster) ||
                clusteredResults.get(expectedCluster).size() < 2) {

                
                try {
                    String gapQuery = question.getQuestion() + " " + expectedCluster;
                    List<CitationResult> gapResults = citationService.search(gapQuery);
                    gapFillingResults.addAll(gapResults.stream()
                        .limit(3)
                        .collect(Collectors.toList()));

                } catch (Exception e) {
                    logger.warning("Failed to fill gap for cluster: " + expectedCluster);
                }
            }
        }

        return gapFillingResults;
    }

    private List<CitationResult> synthesizeDeepResults(List<CitationResult> validatedResults,
        List<CitationResult> gapFillingResults,
        ResearchQuestion question) {
        List<CitationResult> combined = new ArrayList<>();
        combined.addAll(validatedResults);
        combined.addAll(gapFillingResults);

        
        List<CitationResult> unique = removeDuplicateCitations(combined);
        unique.sort((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()));

        
        int resultLimit = question.requiresDeepResearch() ? 20 : 15;
        return unique.stream().limit(resultLimit).collect(Collectors.toList());
    }

    
    private List<CitationResult> executeStandardResearch(ResearchQuestion question,
        DeepResearchContext context) {
        try {
            return citationService.search(question.getQuestion());
        } catch (Exception e) {
            logger.warning("Standard research failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<CitationResult> executeBasicSearch(String query) {
        try {
            return citationService.search(query);
        } catch (Exception e) {
            logger.warning("Basic search failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<CitationResult> enhanceResultsWithContext(List<CitationResult> results,
        ResearchQuestion question,
        DeepResearchContext context) {
        
        return results.stream()
            .map(result -> {
                
                double contextBoost = calculateContextualRelevance(result, question, context);
                result.setRelevanceScore(Math.min(1.0, result.getRelevanceScore() + contextBoost));
                return result;
            })
            .collect(Collectors.toList());
    }

    private double calculateContextualRelevance(CitationResult result,
        ResearchQuestion question,
        DeepResearchContext context) {
        double boost = 0.0;

        String content = (result.getTitle() + " " + result.getContent()).toLowerCase();
        String originalQuery = context.getOriginalQuery().toLowerCase();

        
        if (content.contains(originalQuery)) {
            boost += 0.1;
        }

        
        String category = question.getCategory().toLowerCase();
        if (content.contains(category)) {
            boost += 0.05;
        }

        return boost;
    }

    private List<CitationResult> executeEmergencyFallbackResearch(ResearchQuestion question,
        String context) {
        try {
            logger.info("Executing emergency fallback research for: " + question.getQuestion());
            List<CitationResult> results = citationService.search(question.getQuestion());

            if (results.isEmpty() && question.getKeywords().size() > 1) {
                
                String keywordQuery = String.join(" ", question.getKeywords());
                results = citationService.search(keywordQuery);
            }

            return results;

        } catch (Exception e) {
            logger.severe("Emergency fallback research failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private List<CitationResult> executeSequentialSearches(List<ResearchQuery> queries) {
        List<CitationResult> allResults = new ArrayList<>();

        for (ResearchQuery query : queries) {
            try {
                List<CitationResult> queryResults = citationService.search(query.getQuery());
                allResults.addAll(queryResults);

                
                Thread.sleep(SEARCH_RATE_LIMIT_MS);

            } catch (Exception e) {
                logger.warning("Sequential search failed for query: " + query.getQuery());
            }
        }

        return removeDuplicateCitations(allResults);
    }

    
    private List<String> chunkPromptForContext(String prompt) {
        if (countTokens(prompt) <= CONTEXT_WINDOW_LIMIT) {
            return List.of(prompt);
        }

        List<String> chunks = new ArrayList<>();
        int chunkSize = CONTEXT_WINDOW_LIMIT * 4; 

        for (int i = 0; i < prompt.length(); i += chunkSize) {
            int end = Math.min(i + chunkSize, prompt.length());
            chunks.add(prompt.substring(i, end));
        }

        return chunks;
    }

    private int countTokens(String text) {
        return text != null ? (int) Math.ceil(text.length() / 4.0) : 0;
    }

    private List<ResearchQuery> parseResearchQueries(String response, ResearchQuestion question) {
        List<ResearchQuery> queries = new ArrayList<>();
        String[] lines = response.split("\n");

        ResearchQuery currentQuery = null;
        StringBuilder currentRationale = new StringBuilder();

        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("QUERY:")) {
                
                if (currentQuery != null) {
                    currentQuery.setRationale(currentRationale.toString().trim());
                    queries.add(currentQuery);
                }

                
                String queryText = line.substring(6).trim();
                if (!queryText.isEmpty()) {
                    currentQuery = new ResearchQuery(queryText, "General", "Medium", "");
                    currentRationale = new StringBuilder();
                }

            } else if (line.startsWith("TYPE:") && currentQuery != null) {
                currentQuery.setType(line.substring(5).trim());

            } else if (line.startsWith("PRIORITY:") && currentQuery != null) {
                currentQuery.setPriority(line.substring(9).trim());

            } else if (line.startsWith("EXPECTED_SOURCES:") && currentQuery != null) {
                currentQuery.setExpectedSources(line.substring(17).trim());

            } else if (line.startsWith("RATIONALE:")) {
                currentRationale.append(line.substring(10).trim());

            } else if (currentQuery != null && !line.isEmpty() &&
                !line.startsWith("QUERY:") && !line.startsWith("TYPE:") &&
                !line.startsWith("PRIORITY:") && !line.startsWith("EXPECTED_SOURCES:")) {
                currentRationale.append(" ").append(line);
            }
        }

        
        if (currentQuery != null) {
            currentQuery.setRationale(currentRationale.toString().trim());
            queries.add(currentQuery);
        }

        return queries;
    }

    private List<ResearchQuery> prioritizeAndDeduplicateQueries(List<ResearchQuery> queries,
        DeepResearchConfig config) {
        
        Map<String, ResearchQuery> uniqueQueries = new LinkedHashMap<>();
        for (ResearchQuery query : queries) {
            uniqueQueries.putIfAbsent(query.getQuery().toLowerCase(), query);
        }

        
        return uniqueQueries.values().stream()
            .sorted((q1, q2) -> {
                
                Map<String, Integer> priorityOrder = Map.of("High", 3, "Medium", 2, "Low", 1);
                return Integer.compare(
                    priorityOrder.getOrDefault(q2.getPriority(), 0),
                    priorityOrder.getOrDefault(q1.getPriority(), 0)
                );
            })
            .limit(MAX_PARALLEL_SEARCHES)
            .collect(Collectors.toList());
    }

    private List<ResearchQuery> generateFallbackQueries(ResearchQuestion question, String context) {
        List<ResearchQuery> fallbackQueries = new ArrayList<>();
        String baseQuery = question.getQuestion();

        fallbackQueries.add(new ResearchQuery(baseQuery, "Basic", "High", ""));
        fallbackQueries.add(new ResearchQuery(baseQuery + " guide", "Tutorial", "Medium", ""));
        fallbackQueries.add(new ResearchQuery(baseQuery + " example", "Example", "Medium", ""));

        return fallbackQueries;
    }

    private String truncateString(String str, int maxLength) {
        if (str == null || str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    
    public void shutdown() {
        try {
            researchExecutor.shutdown();
            rateLimitExecutor.shutdown();

            if (!researchExecutor.awaitTermination(30, TimeUnit.SECONDS)) {
                researchExecutor.shutdownNow();
            }
            if (!rateLimitExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                rateLimitExecutor.shutdownNow();
            }

            logger.info("ResearchSupervisor shutdown completed");

        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            logger.warning("Shutdown interrupted");
        }
    }
}