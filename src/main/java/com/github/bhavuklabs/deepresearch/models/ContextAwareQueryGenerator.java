package com.github.bhavuklabs.deepresearch.models;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.deepresearch.context.DeepResearchContext;


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
                
                Requirements:
                1. Create queries that explore different aspects
                2. Use specific terminology and avoid generic terms
                3. Target authoritative sources
                4. Include implementation, performance, and case study angles
                5. Return one query per line
                
                Queries:
                """, count, baseQuery, context != null ? context : "General research");

            LLMResponse<String> response = llmClient.complete(prompt, String.class);

            List<String> queries = Arrays.stream(response.structuredOutput().split("\n"))
                .map(String::trim)
                .filter(query -> !query.isEmpty())
                .filter(query -> !query.toLowerCase().startsWith("query"))
                .limit(count)
                .collect(Collectors.toList());

            if (queries.isEmpty()) {
                return generateFallbackQueries(baseQuery, count);
            }

            logger.info("Generated " + queries.size() + " contextual queries");
            return queries;

        } catch (Exception e) {
            logger.warning("Contextual query generation failed: " + e.getMessage());
            return generateFallbackQueries(baseQuery, count);
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
            return generateFallbackAdvancedQueries(question);
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

            } else if (line.startsWith("RATIONALE:") && currentQuery != null) {
                currentRationale.append(line.substring(10).trim());

            } else if (currentQuery != null && !line.isEmpty() &&
                !line.startsWith("QUERY:") && !line.startsWith("TYPE:") &&
                !line.startsWith("PRIORITY:") && !line.startsWith("RATIONALE:")) {
                
                if (currentRationale.length() > 0) {
                    currentRationale.append(" ");
                }
                currentRationale.append(line);
            }
        }

        
        if (currentQuery != null) {
            currentQuery.setRationale(currentRationale.toString().trim());
            queries.add(currentQuery);
        }

        logger.info("Parsed " + queries.size() + " advanced queries from response");
        return queries;
    }

    
    public List<ResearchQuery> generateCategorySpecificQueries(ResearchQuestion question,
        DeepResearchContext context) {
        List<ResearchQuery> queries = new ArrayList<>();
        String baseQuery = question.getQuestion();
        String category = question.getCategory().toLowerCase();

        switch (category) {
            case "implementation":
                queries.add(new ResearchQuery(baseQuery + " implementation guide", "Implementation", "High",
                    "Detailed implementation steps and code examples"));
                queries.add(new ResearchQuery(baseQuery + " code example tutorial", "Tutorial", "High",
                    "Practical coding examples and walkthroughs"));
                queries.add(new ResearchQuery(baseQuery + " best practices patterns", "Best-Practices", "Medium",
                    "Industry standard practices and design patterns"));
                queries.add(new ResearchQuery(baseQuery + " configuration setup", "Configuration", "Medium",
                    "Setup and configuration guidelines"));
                break;

            case "performance":
                queries.add(new ResearchQuery(baseQuery + " performance benchmarks", "Performance", "High",
                    "Performance metrics and benchmark comparisons"));
                queries.add(new ResearchQuery(baseQuery + " optimization techniques", "Optimization", "High",
                    "Performance optimization strategies"));
                queries.add(new ResearchQuery(baseQuery + " scalability analysis", "Scalability", "Medium",
                    "Scalability considerations and analysis"));
                queries.add(new ResearchQuery(baseQuery + " load testing results", "Testing", "Medium",
                    "Load testing methodologies and results"));
                break;

            case "case-study":
                queries.add(new ResearchQuery(baseQuery + " real world case studies", "Case-Study", "High",
                    "Real-world implementation examples"));
                queries.add(new ResearchQuery(baseQuery + " success stories applications", "Success-Story", "High",
                    "Successful applications and outcomes"));
                queries.add(new ResearchQuery(baseQuery + " lessons learned challenges", "Lessons-Learned", "Medium",
                    "Challenges faced and lessons learned"));
                queries.add(new ResearchQuery(baseQuery + " industry adoption examples", "Industry", "Medium",
                    "Industry adoption patterns and examples"));
                break;

            case "technical":
                queries.add(new ResearchQuery(baseQuery + " technical specification", "Specification", "High",
                    "Technical specifications and requirements"));
                queries.add(new ResearchQuery(baseQuery + " architecture design", "Architecture", "High",
                    "Architectural design and patterns"));
                queries.add(new ResearchQuery(baseQuery + " API documentation", "Documentation", "Medium",
                    "API documentation and interfaces"));
                queries.add(new ResearchQuery(baseQuery + " integration patterns", "Integration", "Medium",
                    "Integration approaches and patterns"));
                break;

            case "analysis":
                queries.add(new ResearchQuery(baseQuery + " comparative analysis", "Comparison", "High",
                    "Comparative analysis of alternatives"));
                queries.add(new ResearchQuery(baseQuery + " pros cons evaluation", "Evaluation", "High",
                    "Advantages and disadvantages analysis"));
                queries.add(new ResearchQuery(baseQuery + " market research trends", "Market-Research", "Medium",
                    "Market trends and research insights"));
                queries.add(new ResearchQuery(baseQuery + " expert opinions", "Expert-Opinion", "Medium",
                    "Expert analysis and opinions"));
                break;

            default:
                queries.add(new ResearchQuery(baseQuery + " comprehensive guide", "General", "High",
                    "Comprehensive overview and guide"));
                queries.add(new ResearchQuery(baseQuery + " overview summary", "Overview", "Medium",
                    "High-level overview and summary"));
                queries.add(new ResearchQuery(baseQuery + " detailed analysis", "Analysis", "Medium",
                    "Detailed analysis and examination"));
        }

        return queries;
    }

    
    public List<ResearchQuery> generateDomainSpecificQueries(String query, String detectedDomain) {
        List<ResearchQuery> domainQueries = new ArrayList<>();

        switch (detectedDomain.toLowerCase()) {
            case "software":
            case "programming":
                domainQueries.add(new ResearchQuery(query + " programming tutorial", "Tutorial", "High",
                    "Programming tutorials and guides"));
                domainQueries.add(new ResearchQuery(query + " source code examples", "Code", "High",
                    "Source code examples and repositories"));
                domainQueries.add(new ResearchQuery(query + " framework library", "Framework", "Medium",
                    "Related frameworks and libraries"));
                break;

            case "business":
            case "management":
                domainQueries.add(new ResearchQuery(query + " business strategy", "Strategy", "High",
                    "Business strategy and approaches"));
                domainQueries.add(new ResearchQuery(query + " ROI cost benefit", "Financial", "High",
                    "Return on investment and cost analysis"));
                domainQueries.add(new ResearchQuery(query + " market analysis", "Market", "Medium",
                    "Market analysis and trends"));
                break;

            case "science":
            case "research":
                domainQueries.add(new ResearchQuery(query + " research papers studies", "Academic", "High",
                    "Academic research and studies"));
                domainQueries.add(new ResearchQuery(query + " experimental results", "Experimental", "High",
                    "Experimental results and data"));
                domainQueries.add(new ResearchQuery(query + " peer review analysis", "Peer-Review", "Medium",
                    "Peer-reviewed analysis and findings"));
                break;

            default:
                domainQueries.add(new ResearchQuery(query + " expert analysis", "Expert", "High",
                    "Expert analysis and insights"));
                domainQueries.add(new ResearchQuery(query + " comprehensive study", "Study", "Medium",
                    "Comprehensive studies and research"));
        }

        return domainQueries;
    }

    
    public List<ResearchQuery> generateGapFillingQueries(List<String> identifiedGaps,
        String originalQuery,
        DeepResearchContext context) {
        List<ResearchQuery> gapQueries = new ArrayList<>();

        for (String gap : identifiedGaps) {
            String gapQuery = createGapFillingQuery(gap, originalQuery);
            if (gapQuery != null && !gapQuery.trim().isEmpty()) {
                ResearchQuery query = new ResearchQuery(gapQuery, "Gap-Fill", "Medium",
                    "Filling identified research gap: " + gap);
                gapQueries.add(query);
            }
        }

        return gapQueries;
    }

    
    private String createGapFillingQuery(String gap, String originalQuery) {
        String gapLower = gap.toLowerCase();

        if (gapLower.contains("implementation") || gapLower.contains("code")) {
            return originalQuery + " implementation example code";
        } else if (gapLower.contains("performance") || gapLower.contains("benchmark")) {
            return originalQuery + " performance benchmark comparison";
        } else if (gapLower.contains("case study") || gapLower.contains("real world")) {
            return originalQuery + " case study real world application";
        } else if (gapLower.contains("tutorial") || gapLower.contains("guide")) {
            return originalQuery + " step by step tutorial guide";
        } else if (gapLower.contains("comparison") || gapLower.contains("alternative")) {
            return originalQuery + " comparison alternatives analysis";
        } else if (gapLower.contains("documentation") || gapLower.contains("specification")) {
            return originalQuery + " official documentation specification";
        } else if (gapLower.contains("recent") || gapLower.contains("latest")) {
            return originalQuery + " latest 2024 2025 recent";
        } else if (gapLower.contains("academic") || gapLower.contains("research")) {
            return originalQuery + " academic research paper study";
        } else {
            return originalQuery + " comprehensive detailed analysis";
        }
    }

    
    private List<String> generateFallbackQueries(String baseQuery, int count) {
        List<String> fallback = new ArrayList<>();

        
        fallback.add(baseQuery);
        fallback.add(baseQuery + " guide");
        fallback.add(baseQuery + " tutorial");
        fallback.add(baseQuery + " example");
        fallback.add(baseQuery + " best practices");
        fallback.add(baseQuery + " implementation");
        fallback.add(baseQuery + " analysis");
        fallback.add(baseQuery + " overview");
        fallback.add(baseQuery + " case study");
        fallback.add(baseQuery + " documentation");

        return fallback.stream()
            .limit(count)
            .collect(Collectors.toList());
    }

    
    private List<ResearchQuery> generateFallbackAdvancedQueries(ResearchQuestion question) {
        List<ResearchQuery> fallback = new ArrayList<>();
        String base = question.getQuestion();

        fallback.add(new ResearchQuery(base + " implementation guide", "Implementation", "High",
            "Implementation guidance and examples"));
        fallback.add(new ResearchQuery(base + " best practices", "Best-Practices", "Medium",
            "Industry best practices and standards"));
        fallback.add(new ResearchQuery(base + " case studies", "Case-Study", "Medium",
            "Real-world case studies and applications"));
        fallback.add(new ResearchQuery(base + " performance analysis", "Performance", "Medium",
            "Performance characteristics and analysis"));

        return fallback;
    }

    
    public List<ResearchQuery> enhanceQueriesWithDomainTerms(List<ResearchQuery> queries,
        String domain,
        DeepResearchContext context) {
        return queries.stream()
            .map(query -> enhanceQueryWithDomain(query, domain))
            .collect(Collectors.toList());
    }

    
    private ResearchQuery enhanceQueryWithDomain(ResearchQuery query, String domain) {
        String originalQuery = query.getQuery();
        String enhancedQuery = originalQuery;

        switch (domain.toLowerCase()) {
            case "java":
                enhancedQuery += " Java Spring Boot";
                break;
            case "python":
                enhancedQuery += " Python Django Flask";
                break;
            case "javascript":
                enhancedQuery += " JavaScript Node.js React";
                break;
            case "cloud":
                enhancedQuery += " AWS Azure Google Cloud";
                break;
            case "database":
                enhancedQuery += " SQL NoSQL MongoDB PostgreSQL";
                break;
            case "microservices":
                enhancedQuery += " microservices Docker Kubernetes";
                break;
            default:
                
                break;
        }

        query.setQuery(enhancedQuery);
        return query;
    }

    
    public List<ResearchQuery> validateAndFilterQueries(List<ResearchQuery> queries,
        DeepResearchContext context) {
        return queries.stream()
            .filter(query -> isValidQuery(query))
            .filter(query -> isRelevantQuery(query, context))
            .collect(Collectors.toList());
    }

    
    private boolean isValidQuery(ResearchQuery query) {
        if (query == null || query.getQuery() == null) {
            return false;
        }

        String queryText = query.getQuery().trim();

        
        if (queryText.length() < 5) {
            return false;
        }

        
        if (queryText.matches("^[\\s\\p{Punct}]*$")) {
            return false;
        }

        
        if (queryText.length() > 200) {
            return false;
        }

        return true;
    }

    
    private boolean isRelevantQuery(ResearchQuery query, DeepResearchContext context) {
        String queryText = query.getQuery().toLowerCase();
        String originalQuery = context.getOriginalQuery().toLowerCase();

        
        String[] originalWords = originalQuery.split("\\s+");
        String[] queryWords = queryText.split("\\s+");

        long commonWords = Arrays.stream(originalWords)
            .filter(word -> word.length() > 3)
            .mapToLong(word -> Arrays.stream(queryWords).anyMatch(qWord -> qWord.contains(word)) ? 1 : 0)
            .sum();

        
        return commonWords > 0;
    }
}