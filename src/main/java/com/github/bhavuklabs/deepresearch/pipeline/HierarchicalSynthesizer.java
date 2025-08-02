package com.github.bhavuklabs.deepresearch.pipeline;

import java.util.*;
import java.util.logging.Logger;

import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.deepresearch.context.DeepResearchContext;


public class HierarchicalSynthesizer {

    private static final Logger logger = Logger.getLogger(HierarchicalSynthesizer.class.getName());

    private final LLMClient llmClient;

    public HierarchicalSynthesizer(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    
    public String synthesizeHierarchically(Map<String, String> sectionContents,
        DeepResearchContext context) {
        try {
            logger.info("Starting hierarchical synthesis for " + sectionContents.size() + " sections");

            
            Map<String, String> groupSyntheses = synthesizeRelatedSections(sectionContents);

            
            String finalSynthesis = synthesizeAcrossGroups(groupSyntheses, context);

            logger.info("Hierarchical synthesis completed");
            return finalSynthesis;

        } catch (Exception e) {
            logger.warning("Hierarchical synthesis failed: " + e.getMessage());
            
            return String.join("\n\n", sectionContents.values());
        }
    }

    
    private Map<String, String> synthesizeRelatedSections(Map<String, String> sections) {
        Map<String, List<String>> groups = groupRelatedSections(sections);
        Map<String, String> syntheses = new HashMap<>();

        for (Map.Entry<String, List<String>> group : groups.entrySet()) {
            String groupSynthesis = synthesizeSectionGroup(group.getValue(), group.getKey());
            syntheses.put(group.getKey(), groupSynthesis);
            logger.info("Synthesized group: " + group.getKey() +
                " (" + group.getValue().size() + " sections)");
        }

        return syntheses;
    }

    
    private Map<String, List<String>> groupRelatedSections(Map<String, String> sections) {
        Map<String, List<String>> groups = new HashMap<>();

        for (Map.Entry<String, String> section : sections.entrySet()) {
            String groupKey = determineGroupKey(section.getKey());
            groups.computeIfAbsent(groupKey, k -> new ArrayList<>()).add(section.getValue());
        }

        return groups;
    }

    
    private String determineGroupKey(String sectionTitle) {
        String lower = sectionTitle.toLowerCase();

        if (lower.contains("implementation") || lower.contains("technical") ||
            lower.contains("code") || lower.contains("develop")) {
            return "implementation";
        }
        if (lower.contains("performance") || lower.contains("optimization") ||
            lower.contains("benchmark") || lower.contains("speed")) {
            return "performance";
        }
        if (lower.contains("example") || lower.contains("case") ||
            lower.contains("application") || lower.contains("use case")) {
            return "examples";
        }
        if (lower.contains("architecture") || lower.contains("design") ||
            lower.contains("pattern") || lower.contains("structure")) {
            return "architecture";
        }
        if (lower.contains("security") || lower.contains("safety") ||
            lower.contains("privacy") || lower.contains("risk")) {
            return "security";
        }
        if (lower.contains("comparison") || lower.contains("analysis") ||
            lower.contains("evaluation") || lower.contains("assessment")) {
            return "analysis";
        }

        return "overview";
    }

    
    private String synthesizeSectionGroup(List<String> groupSections, String groupType) {
        if (groupSections.size() == 1) {
            return groupSections.get(0);
        }

        try {
            
            String synthesisPrompt = buildGroupSynthesisPrompt(groupSections, groupType);

            LLMResponse<String> response = llmClient.complete(synthesisPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Group synthesis failed for " + groupType + ": " + e.getMessage());
            
            return String.join("\n\n", groupSections);
        }
    }

    
    private String buildGroupSynthesisPrompt(List<String> sections, String groupType) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("""
            Synthesize the following %s-related sections into a cohesive narrative:
            
            GROUP TYPE: %s
            SECTIONS TO SYNTHESIZE: %d
            
            SYNTHESIS REQUIREMENTS:
            1. Maintain all key technical details and insights
            2. Eliminate redundancy while preserving unique information
            3. Create smooth transitions between concepts
            4. Organize content logically within the %s theme
            5. Preserve all quantitative data and specific examples
            6. Maintain professional, authoritative tone
            
            SECTIONS TO SYNTHESIZE:
            
            """, groupType, groupType, sections.size(), groupType));

        for (int i = 0; i < sections.size(); i++) {
            prompt.append(String.format("--- SECTION %d ---\n", i + 1));
            prompt.append(sections.get(i));
            prompt.append("\n\n");
        }

        prompt.append("""
            
            Provide the synthesized content that combines all sections while maintaining coherence:
            """);

        return prompt.toString();
    }

    
    private String synthesizeAcrossGroups(Map<String, String> groupSyntheses,
        DeepResearchContext context) {
        StringBuilder synthesis = new StringBuilder();

        
        String[] preferredOrder = {
            "overview", "architecture", "implementation",
            "examples", "performance", "security", "analysis"
        };

        
        for (String groupKey : preferredOrder) {
            if (groupSyntheses.containsKey(groupKey)) {
                synthesis.append(createGroupTransition(groupKey));
                synthesis.append(groupSyntheses.get(groupKey)).append("\n\n");
            }
        }

        
        for (Map.Entry<String, String> group : groupSyntheses.entrySet()) {
            if (!Arrays.asList(preferredOrder).contains(group.getKey())) {
                synthesis.append(createGroupTransition(group.getKey()));
                synthesis.append(group.getValue()).append("\n\n");
            }
        }

        return applyFinalCoherenceEnhancement(synthesis.toString(), context);
    }

    
    private String createGroupTransition(String groupKey) {
        return switch (groupKey.toLowerCase()) {
            case "overview" -> "## Research Overview and Context\n\n";
            case "architecture" -> "## Architectural Foundations and Design Principles\n\n";
            case "implementation" -> "## Implementation Strategies and Technical Approaches\n\n";
            case "examples" -> "## Practical Applications and Case Studies\n\n";
            case "performance" -> "## Performance Analysis and Optimization\n\n";
            case "security" -> "## Security Considerations and Best Practices\n\n";
            case "analysis" -> "## Comparative Analysis and Evaluation\n\n";
            default -> "## " + capitalize(groupKey) + " Analysis\n\n";
        };
    }

    
    private String applyFinalCoherenceEnhancement(String content, DeepResearchContext context) {
        try {
            if (content.length() < 5000) {
                
                return content;
            }

            String enhancementPrompt = String.format("""
                Enhance the coherence and flow of this comprehensive research synthesis:
                
                RESEARCH TOPIC: %s
                CONTENT LENGTH: %d characters
                
                ENHANCEMENT REQUIREMENTS:
                1. Ensure smooth transitions between major sections
                2. Eliminate any remaining redundancy
                3. Strengthen logical argument flow
                4. Enhance readability while maintaining technical depth
                5. Verify consistent terminology throughout
                6. Improve paragraph transitions and connectivity
                
                CONTENT TO ENHANCE:
                %s
                
                Return the enhanced, coherent version:
                """,
                context.getOriginalQuery(),
                content.length(),
                truncateForPrompt(content, 15000)
            );

            LLMResponse<String> response = llmClient.complete(enhancementPrompt, String.class);
            return response.structuredOutput();

        } catch (Exception e) {
            logger.warning("Final coherence enhancement failed: " + e.getMessage());
            return content;
        }
    }

    
    public Map<String, String> synthesizeIterativeInsights(Map<String, List<String>> iterativeResults,
        DeepResearchContext context) {
        Map<String, String> synthesizedInsights = new HashMap<>();

        for (Map.Entry<String, List<String>> entry : iterativeResults.entrySet()) {
            String questionKey = entry.getKey();
            List<String> insights = entry.getValue();

            if (insights.size() == 1) {
                synthesizedInsights.put(questionKey, insights.get(0));
                continue;
            }

            try {
                String synthesisPrompt = buildInsightSynthesisPrompt(questionKey, insights);
                LLMResponse<String> response = llmClient.complete(synthesisPrompt, String.class);
                synthesizedInsights.put(questionKey, response.structuredOutput());

            } catch (Exception e) {
                logger.warning("Insight synthesis failed for: " + questionKey);
                
                synthesizedInsights.put(questionKey, String.join(" ", insights));
            }
        }

        return synthesizedInsights;
    }

    
    private String buildInsightSynthesisPrompt(String question, List<String> insights) {
        StringBuilder prompt = new StringBuilder();

        prompt.append(String.format("""
            Synthesize multiple research insights for the question: "%s"
            
            SYNTHESIS GOALS:
            1. Combine complementary information from all insights
            2. Resolve any contradictions by identifying the most reliable sources
            3. Create a comprehensive, unified response
            4. Maintain all factual details and quantitative data
            5. Eliminate redundancy while preserving unique perspectives
            
            INSIGHTS TO SYNTHESIZE (%d total):
            
            """, question, insights.size()));

        for (int i = 0; i < insights.size(); i++) {
            prompt.append(String.format("INSIGHT %d:\n%s\n\n", i + 1, insights.get(i)));
        }

        prompt.append("Provide the synthesized, comprehensive insight:");

        return prompt.toString();
    }

    
    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1).toLowerCase();
    }

    private String truncateForPrompt(String content, int maxLength) {
        if (content.length() <= maxLength) {
            return content;
        }
        return content.substring(0, maxLength - 3) + "...";
    }
}