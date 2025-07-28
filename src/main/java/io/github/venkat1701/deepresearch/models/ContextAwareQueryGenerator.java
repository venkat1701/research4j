package io.github.venkat1701.deepresearch.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;

public class ContextAwareQueryGenerator {
    private static final Logger logger = Logger.getLogger(ContextAwareQueryGenerator.class.getName());
    private final LLMClient llmClient;

    public ContextAwareQueryGenerator(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    public List<String> generateContextualQueries(String baseQuery, String context, int count) {
        try {
            String prompt = String.format("""
                    Generate %d diverse research queries based on: "%s"
                    
                    Context: %s
                    
                    Create queries that explore different aspects and use specific terminology.
                    Return one query per line.
                    """, count, baseQuery, context);

            LLMResponse<String> response = llmClient.complete(prompt, String.class);
            return Arrays.stream(response.structuredOutput().split("\n"))
                .map(String::trim)
                .filter(query -> !query.isEmpty())
                .limit(count)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.warning("Contextual query generation failed: " + e.getMessage());
            return List.of(baseQuery + " implementation", baseQuery + " examples");
        }
    }

    public List<ResearchQuery> generateAdvancedQueries(ResearchQuestion question,
        DeepResearchContext context,
        DeepResearchConfig config) {
        try {
            String prompt = buildAdvancedQueryPrompt(question, context, config);
            LLMResponse<String> response = llmClient.complete(prompt, String.class);
            return parseAdvancedQueries(response.structuredOutput());

        } catch (Exception e) {
            logger.warning("Advanced query generation failed: " + e.getMessage());
            return generateFallbackQueries(question);
        }
    }

    private String buildAdvancedQueryPrompt(ResearchQuestion question,
        DeepResearchContext context,
        DeepResearchConfig config) {
        return String.format("""
                Generate advanced research queries for: "%s"
                
                Research Context:
                - Category: %s
                - Priority: %s
                - Depth: %s
                - Existing Sources: %d
                
                Create 4-6 specialized queries targeting:
                1. Technical implementations and specifications
                2. Performance benchmarks and metrics
                3. Case studies and real-world applications
                4. Expert analysis and industry insights
                5. Comparative analysis and alternatives
                
                Format each query as:
                QUERY: [search terms]
                TYPE: [category]
                PRIORITY: [High/Medium/Low]
                RATIONALE: [why this query is important]
                
                Generate queries:
                """,
            question.getQuestion(),
            question.getCategory(),
            question.getPriority(),
            config.getResearchDepth(),
            context.getAllCitations().size()
        );
    }

    private List<ResearchQuery> parseAdvancedQueries(String response) {
        List<ResearchQuery> queries = new ArrayList<>();
        String[] lines = response.split("\n");

        ResearchQuery currentQuery = null;
        for (String line : lines) {
            line = line.trim();

            if (line.startsWith("QUERY:")) {
                if (currentQuery != null) {
                    queries.add(currentQuery);
                }
                String queryText = line.substring(6).trim();
                currentQuery = new ResearchQuery(queryText, "General", "Medium", "");
            } else if (line.startsWith("TYPE:") && currentQuery != null) {
                currentQuery.setType(line.substring(5).trim());
            } else if (line.startsWith("PRIORITY:") && currentQuery != null) {
                currentQuery.setPriority(line.substring(9).trim());
            } else if (line.startsWith("RATIONALE:") && currentQuery != null) {
                currentQuery.setRationale(line.substring(10).trim());
            }
        }

        if (currentQuery != null) {
            queries.add(currentQuery);
        }

        return queries;
    }

    private List<ResearchQuery> generateFallbackQueries(ResearchQuestion question) {
        List<ResearchQuery> fallback = new ArrayList<>();
        String base = question.getQuestion();

        fallback.add(new ResearchQuery(base + " implementation guide", "Implementation", "High", ""));
        fallback.add(new ResearchQuery(base + " best practices", "Best-Practices", "Medium", ""));
        fallback.add(new ResearchQuery(base + " case studies", "Case-Study", "Medium", ""));

        return fallback;
    }
}

/**
 * Research Quality Analyzer - Evaluates and filters research results
 */


/**
 * Research Orchestrator - Coordinates multiple research strategies
 */
