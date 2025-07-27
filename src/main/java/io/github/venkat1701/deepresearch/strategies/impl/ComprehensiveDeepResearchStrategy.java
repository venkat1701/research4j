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


public class ComprehensiveDeepResearchStrategy implements DeepResearchStrategy {

    private static final Logger logger = Logger.getLogger(ComprehensiveDeepResearchStrategy.class.getName());

    private final LLMClient llmClient;
    private final CitationService citationService;
    private final MemoryManager memoryManager;

    public ComprehensiveDeepResearchStrategy(LLMClient llmClient, CitationService citationService, MemoryManager memoryManager) {
        this.llmClient = llmClient;
        this.citationService = citationService;
        this.memoryManager = memoryManager;
    }

    @Override
    public String getStrategyName() {
        return "Comprehensive Deep Research";
    }

    @Override
    public List<CitationResult> enhanceCitations(
        List<CitationResult> citations,
        ResearchQuestion question,
        DeepResearchContext context) throws Research4jException {

        logger.info("Enhancing citations for comprehensive research: " + question.getCategory());

        
        List<CitationResult> enhanced = citations.stream()
            .filter(citation -> citation.getRelevanceScore() >= 0.4)
            .filter(citation -> citation.getContent() != null && citation.getContent().length() > 200)
            .collect(Collectors.toList());

        
        Map<String, Long> domainCounts = enhanced.stream()
            .collect(Collectors.groupingBy(CitationResult::getDomain, Collectors.counting()));

        
        enhanced.forEach(citation -> {
            String domain = citation.getDomain();
            long domainCount = domainCounts.getOrDefault(domain, 0L);

            
            if (domainCount <= 2) {
                double boostedScore = Math.min(1.0, citation.getRelevanceScore() + 0.1);
                citation.setRelevanceScore(boostedScore);
            }
        });

        
        enhanced.sort((c1, c2) -> Double.compare(c2.getRelevanceScore(), c1.getRelevanceScore()));

        
        return enhanced.stream().limit(15).collect(Collectors.toList());
    }

    @Override
    public String generateInsights(
        ResearchQuestion question,
        List<CitationResult> citations,
        DeepResearchContext context) throws Research4jException {

        logger.info("Generating comprehensive insights for: " + question.getQuestion());

        try {
            String insightPrompt = buildInsightPrompt(question, citations, context);
            LLMResponse<String> response = llmClient.complete(insightPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Failed to generate insights via LLM: " + e.getMessage());
            return generateFallbackInsights(question, citations);
        }
    }

    private String buildInsightPrompt(ResearchQuestion question, List<CitationResult> citations, DeepResearchContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are a senior research analyst conducting comprehensive research analysis.
            Your task is to generate deep insights by synthesizing information from multiple sources.
            
            RESEARCH QUESTION:
            "%s"
            
            QUESTION CATEGORY: %s
            QUESTION PRIORITY: %s
            
            AVAILABLE SOURCES (%d total):
            """.formatted(
            question.getQuestion(),
            question.getCategory(),
            question.getPriority(),
            citations.size()
        ));

        
        for (int i = 0; i < Math.min(citations.size(), 10); i++) {
            CitationResult citation = citations.get(i);
            prompt.append(String.format("""
                
                [SOURCE %d] %s (Relevance: %.2f)
                Domain: %s
                Content: %s
                """,
                i + 1,
                citation.getTitle(),
                citation.getRelevanceScore(),
                citation.getDomain(),
                truncate(citation.getContent(), 300)
            ));
        }

        
        List<String> relatedConcepts = context.getRelatedConcepts(question.getQuestion());
        if (!relatedConcepts.isEmpty()) {
            prompt.append("\n\nRELATED CONCEPTS FROM PREVIOUS RESEARCH:\n");
            prompt.append(String.join(", ", relatedConcepts.subList(0, Math.min(5, relatedConcepts.size()))));
        }

        prompt.append("""
            
            ANALYSIS REQUIREMENTS:
            1. Synthesize information from all sources to answer the research question
            2. Identify key themes, patterns, and relationships
            3. Note any contradictions or gaps in the information
            4. Provide specific examples and evidence where possible
            5. Consider multiple perspectives and viewpoints
            6. Highlight the most important findings and implications
            
            FORMAT YOUR RESPONSE AS:
            
            ## Key Findings
            [3-5 main findings with supporting evidence]
            
            ## Analysis & Synthesis
            [Detailed analysis connecting different sources and concepts]
            
            ## Implications & Significance
            [What this means and why it matters]
            
            ## Research Gaps & Questions
            [Areas needing further investigation]
            
            Generate your comprehensive analysis:
            """);

        return prompt.toString();
    }

    private String generateFallbackInsights(ResearchQuestion question, List<CitationResult> citations) {
        StringBuilder insights = new StringBuilder();

        insights.append("## Key Findings\n");
        insights.append("Based on ").append(citations.size()).append(" sources:\n");

        
        Map<String, Integer> themes = extractThemes(citations);
        themes.entrySet().stream()
            .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
            .limit(3)
            .forEach(entry -> insights.append("- ").append(entry.getKey()).append(" (mentioned ").append(entry.getValue()).append(" times)\n"));

        insights.append("\n## Analysis & Synthesis\n");
        insights.append("The research reveals multiple perspectives on ").append(question.getQuestion()).append(".\n");

        
        citations.stream().limit(3).forEach(citation ->
            insights.append("- ").append(citation.getTitle()).append(": ").append(truncate(citation.getSnippet(), 100)).append("\n"));

        insights.append("\n## Implications & Significance\n");
        insights.append("This research area shows significant depth and complexity requiring further investigation.\n");

        return insights.toString();
    }

    @Override
    public List<String> identifyCriticalAreas(DeepResearchContext context) {
        List<String> criticalAreas = new ArrayList<>();

        
        Map<String, Long> categoryCounts = context.getResearchQuestions().stream()
            .collect(Collectors.groupingBy(ResearchQuestion::getCategory, Collectors.counting()));

        
        if (categoryCounts.getOrDefault("implementation", 0L) < 2) {
            criticalAreas.add("implementation details");
        }

        if (categoryCounts.getOrDefault("comparative", 0L) < 1) {
            criticalAreas.add("comparative analysis");
        }

        if (categoryCounts.getOrDefault("best-practices", 0L) < 1) {
            criticalAreas.add("best practices and guidelines");
        }

        
        Map<String, Integer> citationCoverage = new HashMap<>();
        context.getResearchQuestions().forEach(q -> {
            int citationCount = context.getCitationsForQuestion(q.getQuestion()).size();
            citationCoverage.put(q.getCategory(), citationCoverage.getOrDefault(q.getCategory(), 0) + citationCount);
        });

        citationCoverage.entrySet().stream()
            .filter(entry -> entry.getValue() < 5)
            .map(entry -> entry.getKey() + " evidence")
            .forEach(criticalAreas::add);

        
        criticalAreas.add("future trends and developments");
        criticalAreas.add("challenges and limitations");

        logger.info("Identified " + criticalAreas.size() + " critical areas for deep dive");
        return criticalAreas.stream().distinct().limit(5).collect(Collectors.toList());
    }

    @Override
    public List<ResearchQuestion> generateDeepQuestions(String area, DeepResearchContext context) throws Research4jException {
        List<ResearchQuestion> deepQuestions = new ArrayList<>();
        String originalQuery = context.getOriginalQuery();

        switch (area.toLowerCase()) {
            case "implementation details" -> {
                deepQuestions.add(new ResearchQuestion("What are the step-by-step implementation approaches for " + originalQuery + "?",
                    ResearchQuestion.Priority.HIGH, "implementation"));
                deepQuestions.add(new ResearchQuestion("What are the technical requirements and dependencies for " + originalQuery + "?",
                    ResearchQuestion.Priority.HIGH, "technical"));
                deepQuestions.add(new ResearchQuestion("What are common implementation pitfalls and how to avoid them with " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "best-practices"));
            }
            case "comparative analysis" -> {
                deepQuestions.add(
                    new ResearchQuestion("How does " + originalQuery + " compare to alternative approaches?", ResearchQuestion.Priority.HIGH,
                        "comparative"));
                deepQuestions.add(new ResearchQuestion("What are the trade-offs between " + originalQuery + " and competing solutions?",
                    ResearchQuestion.Priority.MEDIUM, "analysis"));
            }
            case "best practices and guidelines" -> {
                deepQuestions.add(new ResearchQuestion("What are industry best practices for " + originalQuery + "?", ResearchQuestion.Priority.HIGH,
                    "best-practices"));
                deepQuestions.add(new ResearchQuestion("What guidelines should be followed when working with " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "best-practices"));
            }
            case "future trends and developments" -> {
                deepQuestions.add(new ResearchQuestion("What are the emerging trends and future developments in " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "trends"));
                deepQuestions.add(
                    new ResearchQuestion("How is " + originalQuery + " expected to evolve in the next 2-3 years?", ResearchQuestion.Priority.MEDIUM,
                        "trends"));
            }
            case "challenges and limitations" -> {
                deepQuestions.add(
                    new ResearchQuestion("What are the main challenges and limitations of " + originalQuery + "?", ResearchQuestion.Priority.HIGH,
                        "analysis"));
                deepQuestions.add(new ResearchQuestion("How can the limitations of " + originalQuery + " be addressed or mitigated?",
                    ResearchQuestion.Priority.MEDIUM, "analysis"));
            }
            default -> {
                
                deepQuestions.add(new ResearchQuestion("What specific aspects of " + area + " are most important for " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "analysis"));
                deepQuestions.add(new ResearchQuestion("How does " + area + " impact the practical application of " + originalQuery + "?",
                    ResearchQuestion.Priority.MEDIUM, "practical"));
            }
        }

        return deepQuestions;
    }

    @Override
    public Map<String, Set<String>> analyzeCrossReferences(DeepResearchContext context) {
        Map<String, Set<String>> relationships = new HashMap<>();

        
        List<ResearchQuestion> questions = context.getResearchQuestions();
        for (ResearchQuestion q1 : questions) {
            Set<String> related = new HashSet<>();

            for (ResearchQuestion q2 : questions) {
                if (!q1.equals(q2) && areQuestionsRelated(q1, q2)) {
                    related.add(q2.getQuestion());
                }
            }

            if (!related.isEmpty()) {
                relationships.put(q1.getQuestion(), related);
            }
        }

        
        Map<String, List<CitationResult>> citationsByQuestion = new HashMap<>();
        for (ResearchQuestion question : questions) {
            List<CitationResult> citations = context.getCitationsForQuestion(question.getQuestion());
            if (!citations.isEmpty()) {
                citationsByQuestion.put(question.getQuestion(), citations);
            }
        }

        
        for (Map.Entry<String, List<CitationResult>> entry1 : citationsByQuestion.entrySet()) {
            for (Map.Entry<String, List<CitationResult>> entry2 : citationsByQuestion.entrySet()) {
                if (!entry1.getKey().equals(entry2.getKey())) {
                    Set<String> sharedSources = findSharedSources(entry1.getValue(), entry2.getValue());
                    if (sharedSources.size() >= 2) { 
                        relationships.computeIfAbsent(entry1.getKey(), k -> new HashSet<>())
                            .add(entry2.getKey());
                    }
                }
            }
        }

        return relationships;
    }

    @Override
    public List<String> validateConsistency(DeepResearchContext context) {
        List<String> inconsistencies = new ArrayList<>();

        
        Map<String, String> insights = context.getAllInsights();

        for (Map.Entry<String, String> entry1 : insights.entrySet()) {
            for (Map.Entry<String, String> entry2 : insights.entrySet()) {
                if (!entry1.getKey().equals(entry2.getKey())) {
                    if (containsContradiction(entry1.getValue(), entry2.getValue())) {
                        inconsistencies.add("Potential contradiction between insights for '" + entry1.getKey() + "' and '" + entry2.getKey() + "'");
                    }
                }
            }
        }

        
        List<CitationResult> allCitations = context.getAllCitations();
        Map<String, List<Double>> scoresByDomain = allCitations.stream()
            .collect(Collectors.groupingBy(CitationResult::getDomain,
                Collectors.mapping(CitationResult::getRelevanceScore, Collectors.toList())));

        for (Map.Entry<String, List<Double>> entry : scoresByDomain.entrySet()) {
            List<Double> scores = entry.getValue();
            if (scores.size() > 1) {
                double variance = calculateVariance(scores);
                if (variance > 0.3) { 
                    inconsistencies.add("High variance in source quality for domain: " + entry.getKey());
                }
            }
        }

        return inconsistencies;
    }

    @Override
    public String synthesizeKnowledge(DeepResearchContext context) throws Research4jException {
        logger.info("Synthesizing comprehensive knowledge");

        try {
            String synthesisPrompt = buildSynthesisPrompt(context);
            LLMResponse<String> response = llmClient.complete(synthesisPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Failed to synthesize knowledge via LLM: " + e.getMessage());
            return generateFallbackSynthesis(context);
        }
    }

    private String buildSynthesisPrompt(DeepResearchContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are a senior research synthesizer tasked with creating a comprehensive knowledge synthesis.
            Your role is to integrate all research findings into a coherent, authoritative understanding.
            
            ORIGINAL RESEARCH QUERY:
            "%s"
            
            RESEARCH SUMMARY:
            - Research Questions Explored: %d
            - Total Sources Analyzed: %d
            - Research Duration: %s
            - Strategy Used: %s
            
            """.formatted(
            context.getOriginalQuery(),
            context.getResearchQuestions().size(),
            context.getAllCitations().size(),
            java.time.Duration.between(context.getStartTime(), java.time.Instant.now()),
            getStrategyName()
        ));

        
        Map<String, List<ResearchQuestion>> questionsByCategory = context.getResearchQuestions()
            .stream()
            .collect(Collectors.groupingBy(ResearchQuestion::getCategory));

        prompt.append("INSIGHTS BY RESEARCH CATEGORY:\n\n");
        for (Map.Entry<String, List<ResearchQuestion>> entry : questionsByCategory.entrySet()) {
            prompt.append("### ")
                .append(entry.getKey().toUpperCase())
                .append(" INSIGHTS:\n");

            for (ResearchQuestion question : entry.getValue().subList(0, Math.min(3, entry.getValue().size()))) {
                String insights = context.getInsightsForQuestion(question.getQuestion());
                if (insights != null) {
                    prompt.append("Q: ").append(question.getQuestion()).append("\n");
                    prompt.append("A: ").append(truncate(insights, 200)).append("\n\n");
                }
            }
        }

        
        List<String> topConcepts = context.getMostImportantConcepts(8);
        if (!topConcepts.isEmpty()) {
            prompt.append("KEY CONCEPTS IDENTIFIED:\n");
            prompt.append(String.join(", ", topConcepts)).append("\n\n");
        }

        
        Map<String, Set<String>> relationships = context.getKnowledgeRelationships();
        if (!relationships.isEmpty()) {
            prompt.append("CONCEPT RELATIONSHIPS:\n");
            relationships.entrySet().stream().limit(5).forEach(entry ->
                prompt.append("- ").append(entry.getKey())
                    .append(" connects to: ").append(String.join(", ", entry.getValue()))
                    .append("\n"));
            prompt.append("\n");
        }

        prompt.append("""
            SYNTHESIS REQUIREMENTS:
            1. Create a unified, comprehensive understanding of the research topic
            2. Identify and explain the most important findings and insights
            3. Show how different aspects connect and relate to each other
            4. Resolve any apparent contradictions with reasoned analysis
            5. Present the information in a logical, hierarchical structure
            6. Include practical implications and applications
            7. Maintain academic rigor while being accessible
            
            SYNTHESIS STRUCTURE:
            
            ## Executive Summary
            [2-3 paragraphs summarizing the most important findings]
            
            ## Core Concepts and Foundations
            [Fundamental concepts and principles]
            
            ## Key Findings and Analysis
            [Major discoveries and insights organized thematically]
            
            ## Interconnections and Relationships
            [How different aspects relate and influence each other]
            
            ## Practical Implications
            [Real-world applications and significance]
            
            ## Critical Considerations
            [Limitations, challenges, and important caveats]
            
            Generate your comprehensive knowledge synthesis:
            """);

        return prompt.toString();
    }

    @Override
    public String generateFinalReport(DeepResearchContext context, String synthesizedKnowledge) throws Research4jException {
        logger.info("Generating comprehensive final report");

        try {
            String reportPrompt = buildReportPrompt(context, synthesizedKnowledge);
            LLMResponse<String> response = llmClient.complete(reportPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Failed to generate final report via LLM: " + e.getMessage());
            return generateFallbackReport(context, synthesizedKnowledge);
        }
    }

    private String buildReportPrompt(DeepResearchContext context, String synthesizedKnowledge) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are creating a comprehensive research report that serves as the definitive resource on this topic.
            This report should be publication-quality and serve as a complete reference document.
            
            RESEARCH OVERVIEW:
            Original Query: "%s"
            Research Depth: %s
            Total Sources: %d
            Research Questions: %d
            Processing Time: %s
            
            SYNTHESIZED KNOWLEDGE:
            %s
            
            """.formatted(
            context.getOriginalQuery(),
            context.getConfig().getResearchDepth(),
            context.getAllCitations().size(),
            context.getResearchQuestions().size(),
            java.time.Duration.between(context.getStartTime(), java.time.Instant.now()),
            synthesizedKnowledge
        ));

        
        Map<String, Object> metrics = calculateResearchMetrics(context);
        prompt.append("RESEARCH QUALITY METRICS:\n");
        prompt.append("- Source Diversity: ").append(metrics.get("sourceDiversity")).append("\n");
        prompt.append("- Average Relevance: ").append(metrics.get("averageRelevance")).append("\n");
        prompt.append("- Question Coverage: ").append(metrics.get("questionCoverage")).append("\n\n");

        prompt.append("""
            FINAL REPORT REQUIREMENTS:
            1. Professional, publication-ready format
            2. Complete coverage of all important aspects
            3. Clear section hierarchy and organization
            4. Executive summary for quick understanding
            5. Detailed analysis with supporting evidence
            6. Actionable recommendations and conclusions
            7. Proper acknowledgment of sources and limitations
            
            REPORT STRUCTURE:
            
            # [Dynamic Title Based on Research Topic]
            
            ## Executive Summary
            [Comprehensive 3-4 paragraph summary of entire research]
            
            ## Introduction and Background
            [Context, importance, and scope of the research]
            
            ## Methodology
            [Research approach, sources analyzed, and analytical framework]
            
            ## Findings and Analysis
            ### [Major Theme 1]
            [Detailed analysis with evidence]
            
            ### [Major Theme 2]
            [Detailed analysis with evidence]
            
            ### [Additional themes as needed]
            
            ## Synthesis and Integration
            [How all findings connect and what they mean together]
            
            ## Practical Applications and Implications
            [Real-world use cases, recommendations, and actionable insights]
            
            ## Challenges and Limitations
            [Honest assessment of constraints and areas for future research]
            
            ## Conclusions and Recommendations
            [Key takeaways and suggested next steps]
            
            ## Sources and References
            [Acknowledgment of key sources used in research]
            
            Generate your comprehensive final report:
            """);

        return prompt.toString();
    }

    
    private boolean areQuestionsRelated(ResearchQuestion q1, ResearchQuestion q2) {
        
        if (q1.getCategory().equals(q2.getCategory())) {
            return true;
        }

        String[] words1 = q1.getQuestion().toLowerCase().split("\\W+");
        String[] words2 = q2.getQuestion().toLowerCase().split("\\W+");

        Set<String> set1 = Set.of(words1);
        Set<String> set2 = Set.of(words2);

        Set<String> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);

        return intersection.size() >= 2; 
    }

    private Set<String> findSharedSources(List<CitationResult> list1, List<CitationResult> list2) {
        Set<String> sources1 = list1.stream()
            .map(CitationResult::getUrl)
            .collect(Collectors.toSet());
        Set<String> sources2 = list2.stream()
            .map(CitationResult::getUrl)
            .collect(Collectors.toSet());

        sources1.retainAll(sources2);
        return sources1;
    }

    private boolean containsContradiction(String text1, String text2) {
        
        String[] contradictoryPairs = {
            "beneficial,harmful", "increase,decrease", "effective,ineffective",
            "recommended,discouraged", "secure,insecure", "fast,slow"
        };

        String t1Lower = text1.toLowerCase();
        String t2Lower = text2.toLowerCase();

        for (String pair : contradictoryPairs) {
            String[] words = pair.split(",");
            if ((t1Lower.contains(words[0]) && t2Lower.contains(words[1])) ||
                (t1Lower.contains(words[1]) && t2Lower.contains(words[0]))) {
                return true;
            }
        }

        return false;
    }

    private double calculateVariance(List<Double> values) {
        if (values.size() <= 1) return 0.0;

        double mean = values.stream()
            .mapToDouble(Double::doubleValue)
            .average()
            .orElse(0.0);

        return values.stream()
            .mapToDouble(val -> Math.pow(val - mean, 2))
            .average()
            .orElse(0.0);
    }

    private Map<String, Integer> extractThemes(List<CitationResult> citations) {
        Map<String, Integer> themeCounts = new HashMap<>();

        for (CitationResult citation : citations) {
            String title = citation.getTitle().toLowerCase();
            String[] words = title.split("\\W+");

            for (String word : words) {
                if (word.length() > 4 && !isStopWord(word)) {
                    themeCounts.merge(word, 1, Integer::sum);
                }
            }
        }

        return themeCounts.entrySet().stream()
            .filter(entry -> entry.getValue() > 1)
            .collect(Collectors.toMap(
                Map.Entry::getKey,
                Map.Entry::getValue
            ));
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "this", "that", "with", "have", "will", "from", "they", "been",
            "said", "each", "which", "their", "time", "about", "using", "based"
        );
        return stopWords.contains(word);
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    private String generateFallbackSynthesis(DeepResearchContext context) {
        StringBuilder synthesis = new StringBuilder();

        synthesis.append("## Executive Summary\n\n");
        synthesis.append("This research examined ")
            .append(context.getOriginalQuery())
            .append(" through ")
            .append(context.getResearchQuestions().size())
            .append(" research questions and ")
            .append(context.getAllCitations().size())
            .append(" sources.\n\n");

        synthesis.append("## Key Findings\n\n");
        Map<String, List<ResearchQuestion>> byCategory = context.getResearchQuestions()
            .stream()
            .collect(Collectors.groupingBy(ResearchQuestion::getCategory));

        for (Map.Entry<String, List<ResearchQuestion>> entry : byCategory.entrySet()) {
            synthesis.append("### ").append(entry.getKey()).append("\n");
            synthesis.append("Investigated ")
                .append(entry.getValue().size())
                .append(" aspects of this category.\n\n");
        }

        return synthesis.toString();
    }

    private String generateFallbackReport(DeepResearchContext context, String synthesizedKnowledge) {
        StringBuilder report = new StringBuilder();

        report.append("# Comprehensive Research Report: ")
            .append(context.getOriginalQuery())
            .append("\n\n");

        report.append("## Executive Summary\n\n");
        report.append("This comprehensive research investigated ")
            .append(context.getOriginalQuery())
            .append(" using advanced deep research methodology.\n\n");

        report.append("## Research Overview\n\n");
        report.append("- **Total Sources Analyzed**: ")
            .append(context.getAllCitations().size()).append("\n");
        report.append("- **Research Questions**: ")
            .append(context.getResearchQuestions().size()).append("\n");
        report.append("- **Research Duration**: ")
            .append(java.time.Duration.between(context.getStartTime(), java.time.Instant.now()))
            .append("\n\n");

        report.append("## Synthesized Knowledge\n\n");
        report.append(synthesizedKnowledge).append("\n\n");

        report.append("## Conclusions\n\n");
        report.append("This research provides comprehensive coverage of ")
            .append(context.getOriginalQuery())
            .append(" with insights from multiple perspectives and authoritative sources.\n");

        return report.toString();
    }

    private Map<String, Object> calculateResearchMetrics(DeepResearchContext context) {
        Map<String, Object> metrics = new HashMap<>();

        List<CitationResult> citations = context.getAllCitations();

        
        Set<String> uniqueDomains = citations.stream()
            .map(CitationResult::getDomain)
            .collect(Collectors.toSet());
        metrics.put("sourceDiversity", uniqueDomains.size());

        
        double avgRelevance = citations.stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average()
            .orElse(0.0);
        metrics.put("averageRelevance", String.format("%.2f", avgRelevance));

        
        long answeredQuestions = context.getResearchQuestions().stream()
            .mapToLong(q -> q.isResearched() ? 1 : 0)
            .sum();
        double coverage = (double) answeredQuestions / context.getResearchQuestions().size();
        metrics.put("questionCoverage", String.format("%.1f%%", coverage * 100));

        return metrics;
    }
}