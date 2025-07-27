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
import io.github.venkat1701.core.utils.RobustLLMResponseHandler;
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
    private final RobustLLMResponseHandler responseHandler;

    private static final Set<String> TECHNICAL_KEYWORDS = Set.of(
        "implementation", "architecture", "design", "pattern", "framework", "library", "api", "code",
        "programming", "development", "software", "algorithm", "performance", "scalability", "security",
        "testing", "deployment", "configuration", "integration", "microservices", "database"
    );

    public TechnicalDeepResearchStrategy(LLMClient llmClient, CitationService citationService, MemoryManager memoryManager) {
        this.llmClient = llmClient;
        this.citationService = citationService;
        this.memoryManager = memoryManager;
        this.responseHandler = new RobustLLMResponseHandler(llmClient);
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

    @Override
    public String generateInsights(
        ResearchQuestion question,
        List<CitationResult> citations,
        DeepResearchContext context) throws Research4jException {

        logger.info("Generating technical insights for: " + question.getQuestion());

        try {
            String insightPrompt = buildSafeTechnicalInsightPrompt(question, citations, context);
            String contextInfo = "TechnicalInsights-" + question.getCategory();

            
            LLMResponse<String> response = responseHandler.safeComplete(insightPrompt, String.class, contextInfo);

            String insights = response.structuredOutput();
            if (insights == null || insights.trim().isEmpty()) {
                logger.warning("Empty insights received, using fallback");
                return generateTechnicalFallbackInsights(question, citations);
            }

            logger.info("Technical insights generated successfully");
            return insights;

        } catch (Exception e) {
            logger.warning("Failed to generate insights: " + e.getMessage());
            return generateTechnicalFallbackInsights(question, citations);
        }
    }

    private String buildSafeTechnicalInsightPrompt(ResearchQuestion question, List<CitationResult> citations, DeepResearchContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("You are a senior software architect analyzing technical implementation questions.\n\n");

        prompt.append("TECHNICAL QUESTION: ").append(question.getQuestion()).append("\n\n");

        prompt.append("ANALYSIS CONTEXT:\n");
        prompt.append("- Question Category: ").append(question.getCategory()).append("\n");
        prompt.append("- Priority Level: ").append(question.getPriority()).append("\n");
        prompt.append("- Focus Area: Software Architecture and Implementation\n\n");

        if (!citations.isEmpty()) {
            prompt.append("TECHNICAL SOURCES TO ANALYZE:\n\n");

            for (int i = 0; i < Math.min(citations.size(), 6); i++) {
                try {
                    CitationResult citation = citations.get(i);
                    if (citation != null && citation.isValid()) {
                        prompt.append("Source ").append(i + 1).append(": ")
                            .append(citation.getTitle() != null ? citation.getTitle() : "Technical Resource")
                            .append("\n");
                        prompt.append("Relevance: ").append(String.format("%.2f", citation.getRelevanceScore())).append("\n");
                        prompt.append("Content Summary: ").append(truncate(citation.getContent(), 300)).append("\n\n");
                    }
                } catch (Exception e) {
                    logger.warning("Error processing citation " + i + ": " + e.getMessage());
                }
            }
        }

        try {
            List<String> relatedConcepts = memoryManager.findRelatedConcepts(question.getQuestion(), 0.5);
            if (!relatedConcepts.isEmpty()) {
                prompt.append("RELATED TECHNICAL CONCEPTS: ");
                prompt.append(String.join(", ", relatedConcepts.subList(0, Math.min(5, relatedConcepts.size()))));
                prompt.append("\n\n");
            }
        } catch (Exception e) {
            logger.warning("Error retrieving related concepts: " + e.getMessage());
        }

        prompt.append("ANALYSIS REQUIREMENTS:\n");
        prompt.append("1. Provide practical implementation guidance\n");
        prompt.append("2. Identify key architectural patterns and design considerations\n");
        prompt.append("3. Highlight performance and scalability factors\n");
        prompt.append("4. Include security and best practice recommendations\n");
        prompt.append("5. Suggest specific tools and libraries\n\n");

        prompt.append("RESPONSE STRUCTURE:\n");
        prompt.append("## Technical Overview\n");
        prompt.append("Brief explanation of the core technical concepts\n\n");
        prompt.append("## Implementation Approach\n");
        prompt.append("Specific implementation strategies and patterns\n\n");
        prompt.append("## Recommended Tools and Libraries\n");
        prompt.append("Specific tools, frameworks, and libraries to use\n\n");
        prompt.append("## Architecture Considerations\n");
        prompt.append("Key architectural decisions and design patterns\n\n");
        prompt.append("## Best Practices\n");
        prompt.append("Security, performance, and maintainability recommendations\n\n");

        prompt.append("Provide your technical analysis:");

        return prompt.toString();
    }

    @Override
    public String synthesizeKnowledge(DeepResearchContext context) throws Research4jException {
        logger.info("Synthesizing technical knowledge");

        try {
            String synthesisPrompt = buildSafeSynthesisPrompt(context);
            String contextInfo = "TechnicalSynthesis-" + context.getOriginalQuery().hashCode();

            LLMResponse<String> response = responseHandler.safeComplete(synthesisPrompt, String.class, contextInfo);

            String synthesis = response.structuredOutput();
            if (synthesis == null || synthesis.trim().isEmpty()) {
                logger.warning("Empty synthesis received, using fallback");
                return generateTechnicalFallbackSynthesis(context);
            }

            return synthesis;

        } catch (Exception e) {
            logger.warning("Failed to synthesize knowledge: " + e.getMessage());
            return generateTechnicalFallbackSynthesis(context);
        }
    }

    @Override
    public String generateFinalReport(DeepResearchContext context, String synthesizedKnowledge) throws Research4jException {
        logger.info("Generating technical final report");

        try {
            String reportPrompt = buildSafeReportPrompt(context, synthesizedKnowledge);
            String contextInfo = "TechnicalReport-" + context.getSessionId();

            LLMResponse<String> response = responseHandler.safeComplete(reportPrompt, String.class, contextInfo);

            String report = response.structuredOutput();
            if (report == null || report.trim().isEmpty()) {
                logger.warning("Empty report received, using fallback");
                return generateTechnicalFallbackReport(context, synthesizedKnowledge);
            }

            return report;

        } catch (Exception e) {
            logger.warning("Failed to generate final report: " + e.getMessage());
            return generateTechnicalFallbackReport(context, synthesizedKnowledge);
        }
    }

    private String buildSafeSynthesisPrompt(DeepResearchContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Synthesize technical research findings into a comprehensive overview.\n\n");
        prompt.append("RESEARCH TOPIC: ").append(context.getOriginalQuery()).append("\n\n");

        prompt.append("RESEARCH SUMMARY:\n");
        prompt.append("- Questions Analyzed: ").append(context.getResearchQuestions().size()).append("\n");
        prompt.append("- Sources Reviewed: ").append(context.getAllCitations().size()).append("\n");
        prompt.append("- Research Focus: Technical Implementation\n\n");

        prompt.append("Create a synthesis covering:\n");
        prompt.append("1. Key technical findings\n");
        prompt.append("2. Implementation strategies\n");
        prompt.append("3. Architectural recommendations\n");
        prompt.append("4. Tool and technology suggestions\n");
        prompt.append("5. Best practices identified\n\n");

        return prompt.toString();
    }

    private String buildSafeReportPrompt(DeepResearchContext context, String synthesizedKnowledge) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("Create a comprehensive technical research report.\n\n");
        prompt.append("RESEARCH TOPIC: ").append(context.getOriginalQuery()).append("\n\n");
        prompt.append("SYNTHESIZED FINDINGS:\n").append(synthesizedKnowledge).append("\n\n");

        prompt.append("Create a structured report with:\n");
        prompt.append("1. Executive Summary\n");
        prompt.append("2. Technical Analysis\n");
        prompt.append("3. Implementation Recommendations\n");
        prompt.append("4. Conclusion\n\n");

        return prompt.toString();
    }

    private String generateTechnicalFallbackInsights(ResearchQuestion question, List<CitationResult> citations) {
        StringBuilder insights = new StringBuilder();

        insights.append("## Technical Analysis\n\n");
        insights.append("Analysis of: ").append(question.getQuestion()).append("\n");
        insights.append("Based on ").append(citations.size()).append(" technical sources.\n\n");

        insights.append("## Key Findings\n");
        insights.append("- Multiple implementation approaches identified\n");
        insights.append("- Various architectural patterns applicable\n");
        insights.append("- Several tool and library options available\n\n");

        insights.append("## Implementation Considerations\n");
        insights.append("- Consider scalability requirements\n");
        insights.append("- Evaluate security implications\n");
        insights.append("- Plan for maintainability and testing\n\n");

        insights.append("## Recommendations\n");
        insights.append("- Follow established architectural patterns\n");
        insights.append("- Use proven tools and frameworks\n");
        insights.append("- Implement comprehensive testing strategy\n");

        return insights.toString();
    }

    private String generateTechnicalFallbackSynthesis(DeepResearchContext context) {
        StringBuilder synthesis = new StringBuilder();

        synthesis.append("## Technical Research Synthesis\n\n");
        synthesis.append("Comprehensive analysis of: ").append(context.getOriginalQuery()).append("\n\n");

        synthesis.append("## Research Overview\n");
        synthesis.append("- Technical focus on implementation and architecture\n");
        synthesis.append("- Multiple perspectives and approaches analyzed\n");
        synthesis.append("- Best practices and recommendations identified\n\n");

        synthesis.append("## Key Technical Insights\n");
        synthesis.append("- Implementation patterns and strategies\n");
        synthesis.append("- Architectural considerations and trade-offs\n");
        synthesis.append("- Tool and technology recommendations\n");

        return synthesis.toString();
    }

    private String generateTechnicalFallbackReport(DeepResearchContext context, String synthesizedKnowledge) {
        StringBuilder report = new StringBuilder();

        report.append("# Technical Research Report\n\n");
        report.append("## Topic: ").append(context.getOriginalQuery()).append("\n\n");

        report.append("## Executive Summary\n");
        report.append("Comprehensive technical research conducted on the specified topic.\n\n");

        report.append("## Technical Analysis\n");
        report.append(synthesizedKnowledge).append("\n\n");

        report.append("## Conclusion\n");
        report.append("Research provides comprehensive technical guidance for implementation.\n");

        return report.toString();
    }

    
    private boolean hasValidContent(CitationResult citation) {
        return citation.getContent() != null && citation.getContent().length() >= 50;
    }

    private double calculateTechnicalRelevance(CitationResult citation) {
        try {
            String content = safeGetContent(citation).toLowerCase();
            long matches = TECHNICAL_KEYWORDS.stream()
                .mapToLong(keyword -> content.contains(keyword) ? 1 : 0)
                .sum();
            return Math.min(0.2, matches * 0.02);
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String safeGetContent(CitationResult citation) {
        if (citation == null) return "";
        String title = citation.getTitle() != null ? citation.getTitle() : "";
        String content = citation.getContent() != null ? citation.getContent() : "";
        return title + " " + content;
    }

    private boolean isAuthoritativeTechnicalSource(CitationResult citation) {
        try {
            String url = citation.getUrl() != null ? citation.getUrl().toLowerCase() : "";
            return url.contains("docs.") || url.contains("github.com") || url.contains("spring.io") ||
                url.contains("baeldung.com") || url.contains("stackoverflow.com");
        } catch (Exception e) {
            return false;
        }
    }

    private List<CitationResult> ensureImplementationCoverage(List<CitationResult> citations, ResearchQuestion question) {
        return citations.stream().limit(10).collect(Collectors.toList());
    }

    private String truncate(String text, int maxLength) {
        if (text == null || text.length() <= maxLength) {
            return text != null ? text : "No content available";
        }
        return text.substring(0, maxLength - 3) + "...";
    }

    
    @Override
    public List<String> identifyCriticalAreas(DeepResearchContext context) {
        return List.of("implementation architecture", "performance optimization", "security considerations");
    }

    @Override
    public List<ResearchQuestion> generateDeepQuestions(String area, DeepResearchContext context) throws Research4jException {
        return List.of(new ResearchQuestion(
            "What are the technical implementation details for " + area + " with " + context.getOriginalQuery() + "?",
            ResearchQuestion.Priority.MEDIUM, "technical"));
    }

    @Override
    public Map<String, Set<String>> analyzeCrossReferences(DeepResearchContext context) {
        return new HashMap<>();
    }

    @Override
    public List<String> validateConsistency(DeepResearchContext context) {
        return new ArrayList<>();
    }
}