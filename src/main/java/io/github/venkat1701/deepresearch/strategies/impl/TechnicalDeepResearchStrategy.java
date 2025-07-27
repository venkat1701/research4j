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

    private static final Set<String> TECHNICAL_KEYWORDS = Set.of("implementation", "architecture", "design", "pattern", "framework", "library", "api", "code",
        "programming", "development", "software", "algorithm", "performance", "scalability", "security", "testing", "deployment", "configuration",
        "integration", "microservices", "database");

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
    public List<CitationResult> enhanceCitations(List<CitationResult> citations, ResearchQuestion question, DeepResearchContext context)
        throws Research4jException {

        logger.info("Enhancing citations for technical research: " + question.getCategory());

        List<CitationResult> enhanced = citations.stream()
            .filter(citation -> citation.getRelevanceScore() >= 0.3)
            .collect(Collectors.toList());

        enhanced.forEach(citation -> {
            double technicalBoost = calculateTechnicalRelevance(citation);
            double newScore = Math.min(1.0, citation.getRelevanceScore() + technicalBoost);
            citation.setRelevanceScore(newScore);
        });

        enhanced.forEach(citation -> {
            if (isAuthoritativeTechnicalSource(citation)) {
                double boostedScore = Math.min(1.0, citation.getRelevanceScore() + 0.15);
                citation.setRelevanceScore(boostedScore);
            }
        });

        enhanced.sort((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()));

        return ensureImplementationCoverage(enhanced, question);
    }

    private double calculateTechnicalRelevance(CitationResult citation) {
        String content = (citation.getTitle() + " " + citation.getContent()).toLowerCase();

        long technicalMatches = TECHNICAL_KEYWORDS.stream()
            .mapToLong(keyword -> countOccurrences(content, keyword))
            .sum();

        if (content.contains("example") || content.contains("tutorial") || content.contains("github") || content.contains("implementation")) {
            technicalMatches += 2;
        }

        if (content.contains("documentation") || content.contains("docs") || citation.getUrl()
            .contains("docs.") || citation.getUrl()
            .contains("/docs/")) {
            technicalMatches += 1;
        }

        return Math.min(0.2, technicalMatches * 0.02);
    }

    private boolean isAuthoritativeTechnicalSource(CitationResult citation) {
        String url = citation.getUrl()
            .toLowerCase();
        String domain = citation.getDomain()
            .toLowerCase();

        if (url.contains("docs.") || url.contains("/docs/") || url.contains("github.com") || url.contains("gitlab.com")) {
            return true;
        }

        Set<String> authoritativeDomains = Set.of("spring.io", "oracle.com", "microsoft.com", "google.com", "apache.org", "eclipse.org", "jetbrains.com",
            "baeldung.com", "stackoverflow.com", "dzone.com", "medium.com");

        return authoritativeDomains.stream()
            .anyMatch(domain::contains);
    }

    private List<CitationResult> ensureImplementationCoverage(List<CitationResult> citations, ResearchQuestion question) {

        long implementationSources = citations.stream()
            .mapToLong(citation -> {
                String content = citation.getContent()
                    .toLowerCase();
                return (content.contains("implementation") || content.contains("example") || content.contains("code") || content.contains("tutorial")) ? 1 : 0;
            })
            .sum();

        if (implementationSources < 3 && question.getCategory()
            .equals("implementation")) {

            try {
                String implementationQuery = question.getQuestion() + " implementation example code";
                List<CitationResult> additionalSources = citationService.search(implementationQuery);

                List<CitationResult> filteredAdditional = additionalSources.stream()
                    .filter(citation -> !citations.contains(citation))
                    .filter(citation -> citation.getContent()
                        .toLowerCase()
                        .contains("implementation") || citation.getContent()
                        .toLowerCase()
                        .contains("example"))
                    .limit(3)
                    .collect(Collectors.toList());

                citations.addAll(filteredAdditional);

            } catch (Exception e) {
                logger.warning("Failed to fetch additional implementation sources: " + e.getMessage());
            }
        }

        return citations.stream()
            .limit(12)
            .collect(Collectors.toList());
    }

    @Override
    public String generateInsights(ResearchQuestion question, List<CitationResult> citations, DeepResearchContext context) throws Research4jException {

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
            """.formatted(question.getQuestion(), question.getCategory(), question.getPriority(), citations.size()));

        for (int i = 0; i < Math.min(citations.size(), 8); i++) {
            CitationResult citation = citations.get(i);
            prompt.append(String.format("""
                
                [TECHNICAL SOURCE %d] %s (Relevance: %.2f)
                Domain: %s | URL: %s
                Technical Content: %s
                """, i + 1, citation.getTitle(), citation.getRelevanceScore(), citation.getDomain(), citation.getUrl(), truncate(citation.getContent(), 400)));
        }

        List<String> relatedTechnicalConcepts = memoryManager.findRelatedConcepts(question.getQuestion(), 0.5);
        if (!relatedTechnicalConcepts.isEmpty()) {
            prompt.append("\n\nRELATED TECHNICAL CONCEPTS:\n");
            prompt.append(String.join(", ", relatedTechnicalConcepts.subList(0, Math.min(6, relatedTechnicalConcepts.size()))));
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

        Map<String, Integer> technicalPatterns = extractTechnicalPatterns(citations);
        if (!technicalPatterns.isEmpty()) {
            insights.append("## Key Technical Patterns\n");
            technicalPatterns.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer> comparingByValue()
                    .reversed())
                .limit(5)
                .forEach(entry -> insights.append("- **")
                    .append(entry.getKey())
                    .append("**: Referenced ")
                    .append(entry.getValue())
                    .append(" times\n"));
            insights.append("\n");
        }

        List<CitationResult> implementationSources = citations.stream()
            .filter(c -> c.getContent()
                .toLowerCase()
                .contains("implementation") || c.getContent()
                .toLowerCase()
                .contains("example"))
            .limit(3)
            .collect(Collectors.toList());

        if (!implementationSources.isEmpty()) {
            insights.append("## Implementation Resources\n");
            implementationSources.forEach(source -> insights.append("- **")
                .append(source.getTitle())
                .append("**: ")
                .append(truncate(source.getSnippet(), 120))
                .append("\n"));
            insights.append("\n");
        }

        insights.append("## Technical Considerations\n");
        insights.append("Multiple implementation approaches and architectural patterns identified. ");
        insights.append("Further analysis of specific technical requirements recommended.\n");

        return insights.toString();
    }

    @Override
    public List<String> identifyCriticalAreas(DeepResearchContext context) {
        List<String> criticalAreas = new ArrayList<>();

        String originalQuery = context.getOriginalQuery()
            .toLowerCase();

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

        Map<String, Long> categoryCounts = context.getResearchQuestions()
            .stream()
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
    }

    @Override
    public List<ResearchQuestion> generateDeepQuestions(String area, DeepResearchContext context) throws Research4jException {
        List<ResearchQuestion> deepQuestions = new ArrayList<>();
        String originalQuery = context.getOriginalQuery();

        switch (area.toLowerCase()) {
            case "implementation architecture" -> {
                deepQuestions.add(new ResearchQuestion("What are the recommended architectural patterns for implementing " + originalQuery + "?",
                    ResearchQuestion.Priority.HIGH, "architecture"));
                deepQuestions.add(
                    new ResearchQuestion("How should the system components be structured for " + originalQuery + "?", ResearchQuestion.Priority.HIGH,
                        "architecture"));
                deepQuestions.add(new ResearchQuestion("What are the key design principles to follow when implementing " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "design"));
            }
            case "performance optimization" -> {
                deepQuestions.add(new ResearchQuestion("What are the performance bottlenecks and optimization strategies for " + originalQuery + "?",
                    ResearchQuestion.Priority.HIGH, "performance"));
                deepQuestions.add(
                    new ResearchQuestion("How can caching be effectively implemented with " + originalQuery + "?", ResearchQuestion.Priority.MEDIUM,
                        "performance"));
                deepQuestions.add(
                    new ResearchQuestion("What monitoring and profiling tools work best with " + originalQuery + "?", ResearchQuestion.Priority.MEDIUM,
                        "monitoring"));
            }
            case "security considerations" -> {
                deepQuestions.add(new ResearchQuestion("What are the security vulnerabilities and mitigation strategies for " + originalQuery + "?",
                    ResearchQuestion.Priority.HIGH, "security"));
                deepQuestions.add(new ResearchQuestion("How should authentication and authorization be implemented with " + originalQuery + "?",
                    ResearchQuestion.Priority.HIGH, "security"));
                deepQuestions.add(
                    new ResearchQuestion("What security testing approaches are recommended for " + originalQuery + "?", ResearchQuestion.Priority.MEDIUM,
                        "testing"));
            }
            case "framework configuration" -> {
                deepQuestions.add(
                    new ResearchQuestion("What are the essential configuration settings for " + originalQuery + "?", ResearchQuestion.Priority.HIGH,
                        "configuration"));
                deepQuestions.add(new ResearchQuestion("How should environment-specific configurations be managed for " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "configuration"));
            }
            case "testing strategies" -> {
                deepQuestions.add(
                    new ResearchQuestion("What testing frameworks and strategies work best with " + originalQuery + "?", ResearchQuestion.Priority.HIGH,
                        "testing"));
                deepQuestions.add(
                    new ResearchQuestion("How should integration testing be implemented for " + originalQuery + "?", ResearchQuestion.Priority.MEDIUM,
                        "testing"));
            }
            case "detailed implementation examples" -> {
                deepQuestions.add(
                    new ResearchQuestion("What are complete, working code examples for implementing " + originalQuery + "?", ResearchQuestion.Priority.HIGH,
                        "implementation"));
                deepQuestions.add(
                    new ResearchQuestion("What are the step-by-step implementation guides for " + originalQuery + "?", ResearchQuestion.Priority.HIGH,
                        "implementation"));
            }
            default -> {

                deepQuestions.add(new ResearchQuestion("What are the technical implementation details for " + area + " in " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "technical"));
                deepQuestions.add(new ResearchQuestion("What tools and libraries are recommended for " + area + " with " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "tools"));
            }
        }

        return deepQuestions;
    }

    @Override
    public Map<String, Set<String>> analyzeCrossReferences(DeepResearchContext context) {
        Map<String, Set<String>> technicalRelationships = new HashMap<>();

        List<ResearchQuestion> questions = context.getResearchQuestions();
        for (ResearchQuestion question : questions) {
            Set<String> relatedConcepts = extractTechnicalConcepts(question.getQuestion());

            for (String concept : relatedConcepts) {
                technicalRelationships.computeIfAbsent(concept, k -> new HashSet<>())
                    .addAll(relatedConcepts.stream()
                        .filter(c -> !c.equals(concept))
                        .collect(Collectors.toSet()));
            }
        }

        Map<String, List<CitationResult>> citationsByTechnicalPattern = new HashMap<>();
        for (ResearchQuestion question : questions) {
            List<CitationResult> citations = context.getCitationsForQuestion(question.getQuestion());
            for (CitationResult citation : citations) {
                Set<String> patterns = extractTechnicalPatterns(List.of(citation)).keySet();
                for (String pattern : patterns) {
                    citationsByTechnicalPattern.computeIfAbsent(pattern, k -> new ArrayList<>())
                        .add(citation);
                }
            }
        }

        for (Map.Entry<String, List<CitationResult>> entry1 : citationsByTechnicalPattern.entrySet()) {
            for (Map.Entry<String, List<CitationResult>> entry2 : citationsByTechnicalPattern.entrySet()) {
                if (!entry1.getKey()
                    .equals(entry2.getKey())) {
                    long sharedCitations = entry1.getValue()
                        .stream()
                        .filter(entry2.getValue()::contains)
                        .count();

                    if (sharedCitations >= 2) {
                        technicalRelationships.computeIfAbsent(entry1.getKey(), k -> new HashSet<>())
                            .add(entry2.getKey());
                    }
                }
            }
        }

        return technicalRelationships;
    }

    @Override
    public List<String> validateConsistency(DeepResearchContext context) {
        List<String> inconsistencies = new ArrayList<>();

        Map<String, String> insights = context.getAllInsights();

        List<String> allInsights = new ArrayList<>(insights.values());
        for (int i = 0; i < allInsights.size(); i++) {
            for (int j = i + 1; j < allInsights.size(); j++) {
                if (containsTechnicalContradiction(allInsights.get(i), allInsights.get(j))) {
                    inconsistencies.add("Potential technical contradiction detected between different implementation approaches");
                }
            }
        }

        List<CitationResult> allCitations = context.getAllCitations();
        Map<String, Set<String>> versionMentions = extractVersionInformation(allCitations);

        for (Map.Entry<String, Set<String>> entry : versionMentions.entrySet()) {
            if (entry.getValue()
                .size() > 1) {
                inconsistencies.add("Multiple versions referenced for " + entry.getKey() + ": " + String.join(", ", entry.getValue()));
            }
        }

        for (CitationResult citation : allCitations) {
            if (citation.getContent()
                .toLowerCase()
                .contains("deprecated") || citation.getContent()
                .toLowerCase()
                .contains("legacy")) {
                inconsistencies.add("Potential use of deprecated technology in source: " + citation.getTitle());
            }
        }

        return inconsistencies;
    }

    @Override
    public String synthesizeKnowledge(DeepResearchContext context) throws Research4jException {
        logger.info("Synthesizing technical knowledge");

        try {
            String synthesisPrompt = buildTechnicalSynthesisPrompt(context);
            LLMResponse<String> response = llmClient.complete(synthesisPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Failed to synthesize technical knowledge via LLM: " + e.getMessage());
            return generateTechnicalFallbackSynthesis(context);
        }
    }

    private String buildTechnicalSynthesisPrompt(DeepResearchContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are a senior software architect creating a comprehensive technical knowledge synthesis.
            Your goal is to create a definitive technical reference that covers all aspects of implementation.
            
            TECHNICAL RESEARCH TOPIC:
            "%s"
            
            RESEARCH OVERVIEW:
            - Technical Questions Analyzed: %d
            - Technical Sources: %d
            - Research Duration: %s
            
            """.formatted(context.getOriginalQuery(), context.getResearchQuestions()
            .size(), context.getAllCitations()
            .size(), java.time.Duration.between(context.getStartTime(), java.time.Instant.now())));

        Map<String, List<ResearchQuestion>> questionsByCategory = context.getResearchQuestions()
            .stream()
            .collect(Collectors.groupingBy(ResearchQuestion::getCategory));

        prompt.append("TECHNICAL INSIGHTS BY CATEGORY:\n\n");

        for (Map.Entry<String, List<ResearchQuestion>> entry : questionsByCategory.entrySet()) {
            String category = entry.getKey();
            List<ResearchQuestion> questions = entry.getValue();

            prompt.append("### ")
                .append(category.toUpperCase())
                .append(" ANALYSIS:\n");

            for (ResearchQuestion question : questions.subList(0, Math.min(2, questions.size()))) {
                String insights = context.getInsightsForQuestion(question.getQuestion());
                if (insights != null) {
                    prompt.append("**Q**: ")
                        .append(question.getQuestion())
                        .append("\n");
                    prompt.append("**Technical Analysis**: ")
                        .append(truncate(insights, 300))
                        .append("\n\n");
                }
            }
        }

        List<CitationResult> allCitations = context.getAllCitations();
        Map<String, Integer> technicalPatterns = extractTechnicalPatterns(allCitations);

        if (!technicalPatterns.isEmpty()) {
            prompt.append("KEY TECHNICAL PATTERNS IDENTIFIED:\n");
            technicalPatterns.entrySet()
                .stream()
                .sorted(Map.Entry.<String, Integer> comparingByValue()
                    .reversed())
                .limit(6)
                .forEach(entry -> prompt.append("- ")
                    .append(entry.getKey())
                    .append(" (")
                    .append(entry.getValue())
                    .append(" references)\n"));
            prompt.append("\n");
        }

        prompt.append("""
            TECHNICAL SYNTHESIS REQUIREMENTS:
            1. Create a comprehensive technical implementation guide
            2. Provide clear architectural recommendations
            3. Include specific code examples and configuration details
            4. Address performance, security, and scalability considerations
            5. Present information in a logical technical hierarchy
            6. Include troubleshooting and common issues
            7. Provide migration and upgrade paths where applicable
            
            TECHNICAL SYNTHESIS STRUCTURE:
            
            ## Executive Technical Summary
            [High-level technical overview and key architectural decisions]
            
            ## Core Architecture & Design
            [Fundamental architectural patterns and design principles]
            
            ## Implementation Strategy
            [Step-by-step implementation approach with technical details]
            
            ## Configuration & Setup
            [Required configurations, dependencies, and environment setup]
            
            ## Code Examples & Patterns
            [Specific implementation examples and coding patterns]
            
            ## Performance & Optimization
            [Performance considerations and optimization strategies]
            
            ## Security Implementation
            [Security patterns and implementation details]
            
            ## Testing & Validation
            [Testing strategies and validation approaches]
            
            ## Deployment & Operations
            [Deployment patterns and operational considerations]
            
            ## Troubleshooting & Common Issues
            [Known issues and their solutions]
            
            Generate your comprehensive technical synthesis:
            """);

        return prompt.toString();
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

    private String buildTechnicalReportPrompt(DeepResearchContext context, String synthesizedKnowledge) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are creating a comprehensive technical implementation guide and reference document.
            This document should serve as the definitive technical resource for implementing the researched technology.
            
            TECHNICAL RESEARCH OVERVIEW:
            Topic: "%s"
            Research Depth: %s
            Technical Sources: %d
            Implementation Questions: %d
            Processing Time: %s
            
            SYNTHESIZED TECHNICAL KNOWLEDGE:
            %s
            
            """.formatted(context.getOriginalQuery(), context.getConfig()
            .getResearchDepth(), context.getAllCitations()
            .size(), context.getResearchQuestions()
            .size(), java.time.Duration.between(context.getStartTime(), java.time.Instant.now()), synthesizedKnowledge));

        Map<String, Object> technicalMetrics = calculateTechnicalMetrics(context);
        prompt.append("TECHNICAL RESEARCH METRICS:\n");
        technicalMetrics.forEach((key, value) -> prompt.append("- ")
            .append(key)
            .append(": ")
            .append(value)
            .append("\n"));
        prompt.append("\n");

        prompt.append("""
            TECHNICAL REPORT REQUIREMENTS:
            1. Comprehensive implementation guide format
            2. Complete code examples with explanations
            3. Architectural diagrams and design patterns
            4. Step-by-step setup and configuration instructions
            5. Production-ready implementation recommendations
            6. Performance tuning and optimization guidance
            7. Security hardening and best practices
            8. Troubleshooting guide and common issues
            9. Migration strategies and upgrade paths
            10. References to authoritative technical sources
            
            TECHNICAL REPORT STRUCTURE:
            
            # Comprehensive Technical Implementation Guide: [Topic]
            
            ## Executive Summary
            [3-4 paragraph technical overview covering architecture, implementation approach, and key benefits]
            
            ## Technology Overview and Architecture
            [Detailed explanation of the technology, its architecture, and core concepts]
            
            ## Prerequisites and Requirements
            [System requirements, dependencies, and prerequisite knowledge]
            
            ## Implementation Guide
            ### Environment Setup
            [Detailed setup instructions with configurations]
            
            ### Core Implementation
            [Step-by-step implementation with complete code examples]
            
            ### Configuration Management
            [Configuration files, properties, and environment-specific settings]
            
            ### Integration Patterns
            [How to integrate with other systems and technologies]
            
            ## Advanced Topics
            ### Performance Optimization
            [Performance tuning, caching strategies, and optimization techniques]
            
            ### Security Implementation
            [Security patterns, authentication, authorization, and hardening]
            
            ### Scalability Considerations
            [Horizontal and vertical scaling strategies]
            
            ### Monitoring and Observability
            [Logging, monitoring, and observability implementation]
            
            ## Testing Strategy
            ### Unit Testing
            [Unit testing approaches and examples]
            
            ### Integration Testing
            [Integration testing strategies and implementation]
            
            ### Performance Testing
            [Performance testing methodologies and tools]
            
            ## Deployment and Operations
            ### Deployment Strategies
            [CI/CD pipelines and deployment patterns]
            
            ### Production Considerations
            [Production deployment checklist and operational concerns]
            
            ### Maintenance and Updates
            [Maintenance procedures and update strategies]
            
            ## Troubleshooting Guide
            ### Common Issues and Solutions
            [Frequently encountered problems and their resolutions]
            
            ### Debugging Techniques
            [Debugging approaches and diagnostic tools]
            
            ### Performance Issues
            [Performance problem identification and resolution]
            
            ## Migration and Upgrade Guide
            [Migration strategies from existing solutions and upgrade paths]
            
            ## Best Practices and Recommendations
            [Industry best practices and expert recommendations]
            
            ## References and Further Reading
            [Key technical sources and additional resources]
            
            Generate your comprehensive technical implementation guide:
            """);

        return prompt.toString();
    }

    private long countOccurrences(String text, String keyword) {
        return text.split("\\b" + keyword + "\\b", -1).length - 1;
    }

    private Set<String> extractTechnicalConcepts(String text) {
        Set<String> concepts = new HashSet<>();
        String[] words = text.toLowerCase()
            .split("\\W+");

        for (String word : words) {
            if (TECHNICAL_KEYWORDS.contains(word)) {
                concepts.add(word);
            }
        }

        return concepts;
    }

    private Map<String, Integer> extractTechnicalPatterns(List<CitationResult> citations) {
        Map<String, Integer> patterns = new HashMap<>();

        Set<String> technicalPatterns = Set.of("mvc", "mvp", "mvvm", "singleton", "factory", "observer", "strategy", "repository", "service", "dao", "dto",
            "rest", "microservices", "event-driven", "cqrs", "saga", "circuit-breaker", "api-gateway", "dependency-injection", "inversion-of-control",
            "aspect-oriented");

        for (CitationResult citation : citations) {
            String content = citation.getTitle()
                .toLowerCase() + " " + citation.getContent()
                .toLowerCase();

            for (String pattern : technicalPatterns) {
                if (content.contains(pattern) || content.contains(pattern.replace("-", " "))) {
                    patterns.merge(pattern, 1, Integer::sum);
                }
            }
        }

        return patterns;
    }

    private boolean containsTechnicalContradiction(String insight1, String insight2) {
        String[] contradictoryPairs = { "synchronous,asynchronous", "stateful,stateless", "sql,nosql", "monolithic,microservices", "pull,push",
            "blocking,non-blocking", "horizontal,vertical", "cache,no-cache" };

        String i1Lower = insight1.toLowerCase();
        String i2Lower = insight2.toLowerCase();

        for (String pair : contradictoryPairs) {
            String[] concepts = pair.split(",");
            if ((i1Lower.contains(concepts[0]) && i2Lower.contains(concepts[1])) || (i1Lower.contains(concepts[1]) && i2Lower.contains(concepts[0]))) {
                return true;
            }
        }

        return false;
    }

    private Map<String, Set<String>> extractVersionInformation(List<CitationResult> citations) {
        Map<String, Set<String>> versionInfo = new HashMap<>();

        for (CitationResult citation : citations) {
            String content = citation.getContent()
                .toLowerCase();

            String[] versionPatterns = { "spring\\s+(\\d+\\.\\d+)", "java\\s+(\\d+)", "spring\\s+boot\\s+(\\d+\\.\\d+)", "hibernate\\s+(\\d+\\.\\d+)",
                "maven\\s+(\\d+\\.\\d+)" };

            for (String pattern : versionPatterns) {
                java.util.regex.Pattern regex = java.util.regex.Pattern.compile(pattern);
                java.util.regex.Matcher matcher = regex.matcher(content);

                while (matcher.find()) {
                    String technology = matcher.group(0)
                        .split("\\s+")[0];
                    String version = matcher.group(1);
                    versionInfo.computeIfAbsent(technology, k -> new HashSet<>())
                        .add(version);
                }
            }
        }

        return versionInfo;
    }

    private Map<String, Object> calculateTechnicalMetrics(DeepResearchContext context) {
        Map<String, Object> metrics = new HashMap<>();

        List<CitationResult> citations = context.getAllCitations();

        long implementationSources = citations.stream()
            .mapToLong(c -> c.getContent()
                .toLowerCase()
                .contains("implementation") || c.getContent()
                .toLowerCase()
                .contains("example") ? 1 : 0)
            .sum();
        metrics.put("Implementation Coverage", implementationSources + "/" + citations.size());

        long authoritativeSources = citations.stream()
            .mapToLong(c -> isAuthoritativeTechnicalSource(c) ? 1 : 0)
            .sum();
        metrics.put("Authoritative Sources", authoritativeSources + "/" + citations.size());

        long codeExamples = citations.stream()
            .mapToLong(c -> c.getContent()
                .toLowerCase()
                .contains("code") || c.getContent()
                .toLowerCase()
                .contains("example") ? 1 : 0)
            .sum();
        metrics.put("Code Examples", codeExamples + "/" + citations.size());

        double avgTechnicalScore = citations.stream()
            .mapToDouble(this::calculateTechnicalRelevance)
            .average()
            .orElse(0.0);
        metrics.put("Technical Depth Score", String.format("%.2f", avgTechnicalScore));

        return metrics;
    }

    private String generateTechnicalFallbackSynthesis(DeepResearchContext context) {
        StringBuilder synthesis = new StringBuilder();

        synthesis.append("## Executive Technical Summary\n\n");
        synthesis.append("Technical analysis of ")
            .append(context.getOriginalQuery())
            .append(" covering ")
            .append(context.getResearchQuestions()
                .size())
            .append(" implementation aspects.\n\n");

        synthesis.append("## Core Architecture & Design\n\n");
        Map<String, Integer> patterns = extractTechnicalPatterns(context.getAllCitations());
        if (!patterns.isEmpty()) {
            synthesis.append("Key technical patterns identified:\n");
            patterns.entrySet()
                .stream()
                .limit(5)
                .forEach(entry -> synthesis.append("- ")
                    .append(entry.getKey())
                    .append("\n"));
        }

        synthesis.append("\n## Implementation Strategy\n\n");
        synthesis.append("Multiple implementation approaches analyzed with focus on practical application.\n");

        return synthesis.toString();
    }

    private String generateTechnicalFallbackReport(DeepResearchContext context, String synthesizedKnowledge) {
        StringBuilder report = new StringBuilder();

        report.append("# Technical Implementation Guide: ")
            .append(context.getOriginalQuery())
            .append("\n\n");

        report.append("## Executive Summary\n\n");
        report.append("Comprehensive technical research covering implementation, architecture, and best practices for ")
            .append(context.getOriginalQuery())
            .append(".\n\n");

        report.append("## Research Overview\n\n");
        report.append("- **Technical Sources**: ")
            .append(context.getAllCitations()
                .size())
            .append("\n");
        report.append("- **Implementation Questions**: ")
            .append(context.getResearchQuestions()
                .size())
            .append("\n");
        report.append("- **Research Duration**: ")
            .append(java.time.Duration.between(context.getStartTime(), java.time.Instant.now()))
            .append("\n\n");

        report.append("## Technical Analysis\n\n");
        report.append(synthesizedKnowledge)
            .append("\n\n");

        report.append("## Implementation Recommendations\n\n");
        report.append("Based on the technical research, multiple viable implementation approaches have been identified ");
        report.append("with specific recommendations for architecture, configuration, and deployment.\n");

        return report.toString();
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }
}