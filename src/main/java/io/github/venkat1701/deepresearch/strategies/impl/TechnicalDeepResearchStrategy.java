package io.github.venkat1701.deepresearch.strategies.impl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;
import io.github.venkat1701.deepresearch.context.MemoryManager;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;
import io.github.venkat1701.deepresearch.strategies.DeepResearchStrategy;
import io.github.venkat1701.exceptions.Research4jException;

public class TechnicalDeepResearchStrategy implements DeepResearchStrategy {

    private static final Logger logger = Logger.getLogger(TechnicalDeepResearchStrategy.class.getName());

    private final LLMClient llmClient;
    private final CitationService citationService;
    private final MemoryManager memoryManager;

    private static final Set<String> TECHNICAL_KEYWORDS = Set.of(
        "implementation", "architecture", "design", "pattern", "framework", "library", "api", "code",
        "programming", "development", "software", "algorithm", "performance", "scalability", "security",
        "testing", "deployment", "configuration", "integration", "microservices", "database"
    );

    public TechnicalDeepResearchStrategy(LLMClient llmClient, CitationService citationService, MemoryManager memoryManager) {
        this.llmClient = llmClient;
        this.citationService = citationService;
        this.memoryManager = memoryManager;
    }

    @Override
    public String getStrategyName() {
        return "Technical Deep Research";
    }

    @Override
    public List<CitationResult> enhanceCitations(
        List<CitationResult> citations,
        ResearchQuestion question,
        DeepResearchContext context) throws Research4jException {

        logger.info("Enhancing citations for technical research: " + question.getCategory());

        try {
            
            List<CitationResult> enhanced = citations.stream()
                .filter(citation -> citation != null && citation.isValid())
                .filter(citation -> citation.getRelevanceScore() >= 0.3)
                .filter(citation -> hasValidContent(citation))
                .collect(Collectors.toList());

            
            enhanced.forEach(citation -> {
                try {
                    double technicalBoost = calculateTechnicalRelevance(citation);
                    double newScore = Math.min(1.0, citation.getRelevanceScore() + technicalBoost);
                    citation.setRelevanceScore(newScore);
                } catch (Exception e) {
                    logger.warning("Error calculating technical relevance for citation: " + e.getMessage());
                }
            });

            
            enhanced.forEach(citation -> {
                try {
                    if (isAuthoritativeTechnicalSource(citation)) {
                        double boostedScore = Math.min(1.0, citation.getRelevanceScore() + 0.15);
                        citation.setRelevanceScore(boostedScore);
                    }
                } catch (Exception e) {
                    logger.warning("Error boosting authoritative source: " + e.getMessage());
                }
            });

            
            enhanced.sort((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()));

            
            List<CitationResult> finalCitations = ensureImplementationCoverage(enhanced, question);

            logger.info("Enhanced " + citations.size() + " citations to " + finalCitations.size() + " technical citations");
            return finalCitations;

        } catch (Exception e) {
            logger.warning("Error enhancing citations: " + e.getMessage());
            
            return citations.stream()
                .filter(citation -> citation != null && citation.isValid())
                .collect(Collectors.toList());
        }
    }

    private boolean hasValidContent(CitationResult citation) {
        return citation.getContent() != null &&
            !citation.getContent().trim().isEmpty() &&
            citation.getContent().length() >= 50;
    }

    private double calculateTechnicalRelevance(CitationResult citation) {
        try {
            String content = safeGetContent(citation);

            long technicalMatches = TECHNICAL_KEYWORDS.stream()
                .mapToLong(keyword -> countOccurrences(content, keyword))
                .sum();

            
            if (content.contains("example") || content.contains("tutorial") ||
                content.contains("github") || content.contains("implementation")) {
                technicalMatches += 2;
            }

            if (content.contains("documentation") || content.contains("docs") ||
                safeGetUrl(citation).contains("docs.") || safeGetUrl(citation).contains("/docs/")) {
                technicalMatches += 1;
            }

            return Math.min(0.2, technicalMatches * 0.02);

        } catch (Exception e) {
            logger.warning("Error calculating technical relevance: " + e.getMessage());
            return 0.0;
        }
    }

    private String safeGetContent(CitationResult citation) {
        if (citation == null) return "";
        String title = citation.getTitle() != null ? citation.getTitle() : "";
        String content = citation.getContent() != null ? citation.getContent() : "";
        return (title + " " + content).toLowerCase();
    }

    private String safeGetUrl(CitationResult citation) {
        return citation != null && citation.getUrl() != null ? citation.getUrl().toLowerCase() : "";
    }

    private boolean isAuthoritativeTechnicalSource(CitationResult citation) {
        try {
            String url = safeGetUrl(citation);
            String domain = citation.getDomain() != null ? citation.getDomain().toLowerCase() : "";

            if (url.contains("docs.") || url.contains("/docs/") ||
                url.contains("github.com") || url.contains("gitlab.com")) {
                return true;
            }

            Set<String> authoritativeDomains = Set.of(
                "spring.io", "oracle.com", "microsoft.com", "google.com", "apache.org",
                "eclipse.org", "jetbrains.com", "baeldung.com", "stackoverflow.com",
                "dzone.com", "medium.com"
            );

            return authoritativeDomains.stream().anyMatch(domain::contains);

        } catch (Exception e) {
            logger.warning("Error checking authoritative source: " + e.getMessage());
            return false;
        }
    }

    private List<CitationResult> ensureImplementationCoverage(List<CitationResult> citations, ResearchQuestion question) {
        try {
            long implementationSources = citations.stream()
                .mapToLong(citation -> {
                    try {
                        String content = safeGetContent(citation);
                        return (content.contains("implementation") || content.contains("example") ||
                            content.contains("code") || content.contains("tutorial")) ? 1 : 0;
                    } catch (Exception e) {
                        return 0;
                    }
                })
                .sum();

            if (implementationSources < 3 && "implementation".equals(question.getCategory())) {
                try {
                    String implementationQuery = question.getQuestion() + " implementation example code";
                    List<CitationResult> additionalSources = citationService.search(implementationQuery);

                    List<CitationResult> filteredAdditional = additionalSources.stream()
                        .filter(citation -> citation != null && citation.isValid())
                        .filter(citation -> !citations.contains(citation))
                        .filter(citation -> {
                            String content = safeGetContent(citation);
                            return content.contains("implementation") || content.contains("example");
                        })
                        .limit(3)
                        .collect(Collectors.toList());

                    citations.addAll(filteredAdditional);
                    logger.info("Added " + filteredAdditional.size() + " additional implementation sources");

                } catch (Exception e) {
                    logger.warning("Failed to fetch additional implementation sources: " + e.getMessage());
                }
            }

            return citations.stream()
                .limit(12)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.warning("Error ensuring implementation coverage: " + e.getMessage());
            return citations;
        }
    }

    @Override
    public String generateInsights(
        ResearchQuestion question,
        List<CitationResult> citations,
        DeepResearchContext context) throws Research4jException {

        logger.info("Generating technical insights for: " + question.getQuestion());

        try {
            String insightPrompt = buildTechnicalInsightPrompt(question, citations, context);
            LLMResponse<String> response = llmClient.complete(insightPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Failed to generate technical insights via LLM: " + e.getMessage());
            return generateTechnicalFallbackInsights(question, citations);
        }
    }

    private String buildTechnicalInsightPrompt(ResearchQuestion question, List<CitationResult> citations, DeepResearchContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are a senior software architect and technical expert conducting deep technical research.
            Your expertise spans software architecture, design patterns, implementation strategies, and best practices.
            
            TECHNICAL RESEARCH QUESTION:
            "%s"
            
            QUESTION CONTEXT:
            - Category: %s
            - Priority: %s
            - Technical Domain: Software Development & Architecture
            
            TECHNICAL SOURCES ANALYZED (%d total):
            """.formatted(
            question.getQuestion(),
            question.getCategory(),
            question.getPriority(),
            citations.size()
        ));

        
        for (int i = 0; i < Math.min(citations.size(), 8); i++) {
            try {
                CitationResult citation = citations.get(i);
                if (citation != null && citation.isValid()) {
                    prompt.append(String.format("""
                        
                        [TECHNICAL SOURCE %d] %s (Relevance: %.2f)
                        Domain: %s | URL: %s
                        Technical Content: %s
                        """,
                        i + 1,
                        citation.getTitle() != null ? citation.getTitle() : "Unknown Title",
                        citation.getRelevanceScore(),
                        citation.getDomain() != null ? citation.getDomain() : "Unknown Domain",
                        citation.getUrl() != null ? citation.getUrl() : "No URL",
                        truncate(citation.getContent(), 400)
                    ));
                }
            } catch (Exception e) {
                logger.warning("Error adding citation " + i + " to prompt: " + e.getMessage());
            }
        }

        
        try {
            List<String> relatedTechnicalConcepts = memoryManager.findRelatedConcepts(question.getQuestion(), 0.5);
            if (!relatedTechnicalConcepts.isEmpty()) {
                prompt.append("\n\nRELATED TECHNICAL CONCEPTS:\n");
                prompt.append(String.join(", ", relatedTechnicalConcepts.subList(0, Math.min(6, relatedTechnicalConcepts.size()))));
            }
        } catch (Exception e) {
            logger.warning("Error adding related concepts: " + e.getMessage());
        }

        prompt.append("""
            
            TECHNICAL ANALYSIS REQUIREMENTS:
            1. Focus on practical implementation details and code examples
            2. Identify architectural patterns and design considerations
            3. Analyze technical trade-offs and performance implications
            4. Provide specific configuration and setup requirements
            5. Include security considerations and best practices
            6. Consider scalability and maintainability aspects
            7. Reference authoritative technical documentation
            
            TECHNICAL RESPONSE FORMAT:
            
            ## Technical Overview
            [High-level technical explanation of concepts]
            
            ## Implementation Details
            [Specific implementation approaches and code patterns]
            
            ## Architecture & Design
            [Architectural considerations and design patterns]
            
            ## Configuration & Setup
            [Required configurations, dependencies, and setup steps]
            
            ## Performance & Scalability
            [Performance considerations and scalability factors]
            
            ## Security & Best Practices
            [Security implications and recommended best practices]
            
            ## Technical Trade-offs
            [Analysis of different approaches and their trade-offs]
            
            ## Code Examples & Patterns
            [Specific code examples and implementation patterns]
            
            Generate your comprehensive technical analysis:
            """);

        return prompt.toString();
    }

    private String generateTechnicalFallbackInsights(ResearchQuestion question, List<CitationResult> citations) {
        StringBuilder insights = new StringBuilder();

        insights.append("## Technical Overview\n");
        insights.append("Analysis of ")
            .append(question.getQuestion())
            .append(" based on ")
            .append(citations.size())
            .append(" technical sources.\n\n");

        
        try {
            Map<String, Integer> technicalPatterns = extractTechnicalPatterns(citations);
            if (!technicalPatterns.isEmpty()) {
                insights.append("## Key Technical Patterns\n");
                technicalPatterns.entrySet().stream()
                    .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                    .limit(5)
                    .forEach(entry -> insights.append("- **")
                        .append(entry.getKey())
                        .append("**: Referenced ")
                        .append(entry.getValue())
                        .append(" times\n"));
                insights.append("\n");
            }
        } catch (Exception e) {
            logger.warning("Error extracting technical patterns: " + e.getMessage());
        }

        
        try {
            List<CitationResult> implementationSources = citations.stream()
                .filter(c -> c != null && hasValidContent(c))
                .filter(c -> {
                    String content = safeGetContent(c);
                    return content.contains("implementation") || content.contains("example");
                })
                .limit(3)
                .collect(Collectors.toList());

            if (!implementationSources.isEmpty()) {
                insights.append("## Implementation Resources\n");
                implementationSources.forEach(source -> {
                    try {
                        insights.append("- **")
                            .append(source.getTitle() != null ? source.getTitle() : "Unknown Source")
                            .append("**: ")
                            .append(truncate(source.getSnippet(), 120))
                            .append("\n");
                    } catch (Exception e) {
                        logger.warning("Error adding implementation source: " + e.getMessage());
                    }
                });
                insights.append("\n");
            }
        } catch (Exception e) {
            logger.warning("Error processing implementation sources: " + e.getMessage());
        }

        insights.append("## Technical Considerations\n");
        insights.append("Multiple implementation approaches and architectural patterns identified. ");
        insights.append("Further analysis of specific technical requirements recommended.\n");

        return insights.toString();
    }

    @Override
    public List<String> identifyCriticalAreas(DeepResearchContext context) {
        List<String> criticalAreas = new ArrayList<>();

        try {
            String originalQuery = context.getOriginalQuery().toLowerCase();

            
            criticalAreas.add("implementation architecture");
            criticalAreas.add("performance optimization");
            criticalAreas.add("security considerations");

            
            if (originalQuery.contains("microservices") || originalQuery.contains("distributed")) {
                criticalAreas.add("distributed systems patterns");
                criticalAreas.add("service communication");
                criticalAreas.add("data consistency");
            }

            if (originalQuery.contains("spring") || originalQuery.contains("framework")) {
                criticalAreas.add("framework configuration");
                criticalAreas.add("dependency injection");
                criticalAreas.add("testing strategies");
            }

            if (originalQuery.contains("database") || originalQuery.contains("data")) {
                criticalAreas.add("data access patterns");
                criticalAreas.add("database optimization");
                criticalAreas.add("transaction management");
            }

            if (originalQuery.contains("api") || originalQuery.contains("rest")) {
                criticalAreas.add("api design patterns");
                criticalAreas.add("authentication and authorization");
                criticalAreas.add("api documentation");
            }

            
            Map<String, Long> categoryCounts = context.getResearchQuestions().stream()
                .collect(Collectors.groupingBy(ResearchQuestion::getCategory, Collectors.counting()));

            if (categoryCounts.getOrDefault("implementation", 0L) < 2) {
                criticalAreas.add("detailed implementation examples");
            }

            if (categoryCounts.getOrDefault("best-practices", 0L) < 1) {
                criticalAreas.add("industry best practices");
            }

            logger.info("Identified " + criticalAreas.size() + " critical technical areas");
            return criticalAreas.stream()
                .distinct()
                .limit(6)
                .collect(Collectors.toList());

        } catch (Exception e) {
            logger.warning("Error identifying critical areas: " + e.getMessage());
            return List.of("implementation details", "best practices", "security considerations");
        }
    }

    @Override
    public List<ResearchQuestion> generateDeepQuestions(String area, DeepResearchContext context) throws Research4jException {
        List<ResearchQuestion> deepQuestions = new ArrayList<>();
        String originalQuery = context.getOriginalQuery();

        try {
            switch (area.toLowerCase()) {
                case "implementation architecture" -> {
                    deepQuestions.add(new ResearchQuestion(
                        "What are the recommended architectural patterns for implementing " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "architecture"));
                    deepQuestions.add(new ResearchQuestion(
                        "How should the system components be structured for " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "architecture"));
                    deepQuestions.add(new ResearchQuestion(
                        "What are the key design principles to follow when implementing " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "design"));
                }
                case "performance optimization" -> {
                    deepQuestions.add(new ResearchQuestion(
                        "What are the performance bottlenecks and optimization strategies for " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "performance"));
                    deepQuestions.add(new ResearchQuestion(
                        "How can caching be effectively implemented with " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "performance"));
                    deepQuestions.add(new ResearchQuestion(
                        "What monitoring and profiling tools work best with " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "monitoring"));
                }
                case "security considerations" -> {
                    deepQuestions.add(new ResearchQuestion(
                        "What are the security vulnerabilities and mitigation strategies for " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "security"));
                    deepQuestions.add(new ResearchQuestion(
                        "How should authentication and authorization be implemented with " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "security"));
                    deepQuestions.add(new ResearchQuestion(
                        "What security testing approaches are recommended for " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "testing"));
                }
                case "framework configuration" -> {
                    deepQuestions.add(new ResearchQuestion(
                        "What are the essential configuration settings for " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "configuration"));
                    deepQuestions.add(new ResearchQuestion(
                        "How should environment-specific configurations be managed for " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "configuration"));
                }
                case "testing strategies" -> {
                    deepQuestions.add(new ResearchQuestion(
                        "What testing frameworks and strategies work best with " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "testing"));
                    deepQuestions.add(new ResearchQuestion(
                        "How should integration testing be implemented for " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "testing"));
                }
                case "detailed implementation examples" -> {
                    deepQuestions.add(new ResearchQuestion(
                        "What are complete, working code examples for implementing " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "implementation"));
                    deepQuestions.add(new ResearchQuestion(
                        "What are the step-by-step implementation guides for " + originalQuery + "?",
                        ResearchQuestion.Priority.HIGH, "implementation"));
                }
                default -> {
                    
                    deepQuestions.add(new ResearchQuestion(
                        "What are the technical implementation details for " + area + " in " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "technical"));
                    deepQuestions.add(new ResearchQuestion(
                        "What tools and libraries are recommended for " + area + " with " + originalQuery + "?",
                        ResearchQuestion.Priority.MEDIUM, "tools"));
                }
            }

            return deepQuestions;

        } catch (Exception e) {
            logger.warning("Error generating deep questions for area '" + area + "': " + e.getMessage());
            
            return List.of(
                new ResearchQuestion(
                    "What are the key technical aspects of " + area + " for " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "technical")
            );
        }
    }

    @Override
    public Map<String, Set<String>> analyzeCrossReferences(DeepResearchContext context) {
        Map<String, Set<String>> technicalRelationships = new HashMap<>();

        try {
            List<ResearchQuestion> questions = context.getResearchQuestions();

            
            for (ResearchQuestion question : questions) {
                try {
                    Set<String> relatedConcepts = extractTechnicalConcepts(question.getQuestion());

                    for (String concept : relatedConcepts) {
                        technicalRelationships.computeIfAbsent(concept, k -> new HashSet<>())
                            .addAll(relatedConcepts.stream()
                                .filter(c -> !c.equals(concept))
                                .collect(Collectors.toSet()));
                    }
                } catch (Exception e) {
                    logger.warning("Error analyzing question relationships: " + e.getMessage());
                }
            }

            
            Map<String, List<CitationResult>> citationsByTechnicalPattern = new HashMap<>();
            for (ResearchQuestion question : questions) {
                try {
                    List<CitationResult> citations = context.getCitationsForQuestion(question.getQuestion());
                    for (CitationResult citation : citations) {
                        if (citation != null && citation.isValid()) {
                            Set<String> patterns = extractTechnicalPatterns(List.of(citation)).keySet();
                            for (String pattern : patterns) {
                                citationsByTechnicalPattern.computeIfAbsent(pattern, k -> new ArrayList<>())
                                    .add(citation);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Error analyzing citation relationships: " + e.getMessage());
                }
            }

            
            for (Map.Entry<String, List<CitationResult>> entry1 : citationsByTechnicalPattern.entrySet()) {
                for (Map.Entry<String, List<CitationResult>> entry2 : citationsByTechnicalPattern.entrySet()) {
                    if (!entry1.getKey().equals(entry2.getKey())) {
                        try {
                            long sharedCitations = entry1.getValue().stream()
                                .filter(entry2.getValue()::contains)
                                .count();

                            if (sharedCitations >= 2) {
                                technicalRelationships.computeIfAbsent(entry1.getKey(), k -> new HashSet<>())
                                    .add(entry2.getKey());
                            }
                        } catch (Exception e) {
                            logger.warning("Error finding shared citations: " + e.getMessage());
                        }
                    }
                }
            }

            return technicalRelationships;

        } catch (Exception e) {
            logger.warning("Error analyzing cross references: " + e.getMessage());
            return new HashMap<>();
        }
    }

    @Override
    public List<String> validateConsistency(DeepResearchContext context) {
        List<String> inconsistencies = new ArrayList<>();

        try {
            Map<String, String> insights = context.getAllInsights();

            
            List<String> allInsights = new ArrayList<>(insights.values());
            for (int i = 0; i < allInsights.size(); i++) {
                for (int j = i + 1; j < allInsights.size(); j++) {
                    try {
                        if (containsTechnicalContradiction(allInsights.get(i), allInsights.get(j))) {
                            inconsistencies.add("Potential technical contradiction detected between different implementation approaches");
                        }
                    } catch (Exception e) {
                        logger.warning("Error checking technical contradictions: " + e.getMessage());
                    }
                }
            }

            
            try {
                List<CitationResult> allCitations = context.getAllCitations();
                Map<String, Set<String>> versionMentions = extractVersionInformation(allCitations);

                for (Map.Entry<String, Set<String>> entry : versionMentions.entrySet()) {
                    if (entry.getValue().size() > 1) {
                        inconsistencies.add("Multiple versions referenced for " + entry.getKey() +
                            ": " + String.join(", ", entry.getValue()));
                    }
                }
            } catch (Exception e) {
                logger.warning("Error checking version consistency: " + e.getMessage());
            }

            
            try {
                for (CitationResult citation : context.getAllCitations()) {
                    if (citation != null && hasValidContent(citation)) {
                        String content = safeGetContent(citation);
                        if (content.contains("deprecated") || content.contains("legacy")) {
                            inconsistencies.add("Potential use of deprecated technology in source: " +
                                (citation.getTitle() != null ? citation.getTitle() : "Unknown"));
                        }
                    }
                }
            } catch (Exception e) {
                logger.warning("Error checking for deprecated technologies: " + e.getMessage());
            }

            return inconsistencies;

        } catch (Exception e) {
            logger.warning("Error validating consistency: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    @Override
    public String synthesizeKnowledge(DeepResearchContext context) throws Research4jException {
        logger.info("Synthesizing technical knowledge");

        try {
            String synthesisPrompt = buildTechnicalSynthesisPrompt(context);
            LLMResponse<String> response = llmClient.complete(synthesisPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Failed to synthesize knowledge via LLM: " + e.getMessage());
            return generateTechnicalFallbackSynthesis(context);
        }
    }

    @Override
    public String generateFinalReport(DeepResearchContext context, String synthesizedKnowledge) throws Research4jException {
        logger.info("Generating technical final report");

        try {
            String reportPrompt = buildTechnicalReportPrompt(context, synthesizedKnowledge);
            LLMResponse<String> response = llmClient.complete(reportPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Failed to generate technical final report via LLM: " + e.getMessage());
            return generateTechnicalFallbackReport(context, synthesizedKnowledge);
        }
    }

    
    private long countOccurrences(String text, String keyword) {
        try {
            if (text == null || keyword == null) return 0;
            return text.split("\\b" + java.util.regex.Pattern.quote(keyword) + "\\b", -1).length - 1;
        } catch (Exception e) {
            logger.warning("Error counting occurrences: " + e.getMessage());
            return 0;
        }
    }

    private Set<String> extractTechnicalConcepts(String text) {
        Set<String> concepts = new HashSet<>();
        try {
            if (text != null) {
                String[] words = text.toLowerCase().split("\\W+");
                for (String word : words) {
                    if (TECHNICAL_KEYWORDS.contains(word)) {
                        concepts.add(word);
                    }
                }
            }
        } catch (Exception e) {
            logger.warning("Error extracting technical concepts: " + e.getMessage());
        }
        return concepts;
    }

    private Map<String, Integer> extractTechnicalPatterns(List<CitationResult> citations) {
        Map<String, Integer> patterns = new HashMap<>();

        try {
            Set<String> technicalPatterns = Set.of(
                "mvc", "mvp", "mvvm", "singleton", "factory", "observer", "strategy", "repository",
                "service", "dao", "dto", "rest", "microservices", "event-driven", "cqrs", "saga",
                "circuit-breaker", "api-gateway", "dependency-injection", "inversion-of-control",
                "aspect-oriented"
            );

            for (CitationResult citation : citations) {
                try {
                    if (citation != null && hasValidContent(citation)) {
                        String content = safeGetContent(citation);

                        for (String pattern : technicalPatterns) {
                            if (content.contains(pattern) || content.contains(pattern.replace("-", " "))) {
                                patterns.merge(pattern, 1, Integer::sum);
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Error processing citation for patterns: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warning("Error extracting technical patterns: " + e.getMessage());
        }

        return patterns;
    }

    private boolean containsTechnicalContradiction(String insight1, String insight2) {
        try {
            String[] contradictoryPairs = {
                "synchronous,asynchronous", "stateful,stateless", "sql,nosql",
                "monolithic,microservices", "pull,push", "blocking,non-blocking",
                "horizontal,vertical", "cache,no-cache"
            };

            String i1Lower = insight1.toLowerCase();
            String i2Lower = insight2.toLowerCase();

            for (String pair : contradictoryPairs) {
                String[] concepts = pair.split(",");
                if ((i1Lower.contains(concepts[0]) && i2Lower.contains(concepts[1])) ||
                    (i1Lower.contains(concepts[1]) && i2Lower.contains(concepts[0]))) {
                    return true;
                }
            }

            return false;

        } catch (Exception e) {
            logger.warning("Error checking technical contradictions: " + e.getMessage());
            return false;
        }
    }

    private Map<String, Set<String>> extractVersionInformation(List<CitationResult> citations) {
        Map<String, Set<String>> versionInfo = new HashMap<>();

        try {
            for (CitationResult citation : citations) {
                try {
                    if (citation != null && hasValidContent(citation)) {
                        String content = safeGetContent(citation);

                        String[] versionPatterns = {
                            "spring\\s+(\\d+\\.\\d+)", "java\\s+(\\d+)", "spring\\s+boot\\s+(\\d+\\.\\d+)",
                            "hibernate\\s+(\\d+\\.\\d+)", "maven\\s+(\\d+\\.\\d+)"
                        };

                        for (String pattern : versionPatterns) {
                            try {
                                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
                                java.util.regex.Matcher matcher = regex.matcher(content);

                                while (matcher.find()) {
                                    String technology = matcher.group(0).split("\\s+")[0];
                                    String version = matcher.group(1);
                                    versionInfo.computeIfAbsent(technology, k -> new HashSet<>()).add(version);
                                }
                            } catch (Exception e) {
                                logger.warning("Error processing version pattern: " + e.getMessage());
                            }
                        }
                    }
                } catch (Exception e) {
                    logger.warning("Error processing citation for versions: " + e.getMessage());
                }
            }
        } catch (Exception e) {
            logger.warning("Error extracting version information: " + e.getMessage());
        }

        return versionInfo;
    }

    private String buildTechnicalSynthesisPrompt(DeepResearchContext context) {
        
        return "Technical synthesis prompt for: " + context.getOriginalQuery();
    }

    private String buildTechnicalReportPrompt(DeepResearchContext context, String synthesizedKnowledge) {
        
        return "Technical report for: " + context.getOriginalQuery() + "\n\n" + synthesizedKnowledge;
    }

    private String generateTechnicalFallbackSynthesis(DeepResearchContext context) {
        return "## Technical Analysis\nFallback synthesis for: " + context.getOriginalQuery();
    }

    private String generateTechnicalFallbackReport(DeepResearchContext context, String synthesizedKnowledge) {
        return "# Technical Report: " + context.getOriginalQuery() + "\n\n" + synthesizedKnowledge;
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "No content available";
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}