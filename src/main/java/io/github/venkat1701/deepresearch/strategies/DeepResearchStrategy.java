package io.github.venkat1701.deepresearch.strategies;

import java.util.List;
import java.util.Map;
import java.util.Set;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.deepresearch.context.DeepResearchContext;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;
import io.github.venkat1701.exceptions.Research4jException;


public interface DeepResearchStrategy {

    
    String getStrategyName();

    
    List<CitationResult> enhanceCitations(
        List<CitationResult> citations,
        ResearchQuestion question,
        DeepResearchContext context) throws Research4jException;

    
    String generateInsights(
        ResearchQuestion question,
        List<CitationResult> citations,
        DeepResearchContext context) throws Research4jException;

    
    List<String> identifyCriticalAreas(DeepResearchContext context);

    
    List<ResearchQuestion> generateDeepQuestions(
        String area,
        DeepResearchContext context) throws Research4jException;

    
    Map<String, Set<String>> analyzeCrossReferences(DeepResearchContext context);

    
    List<String> validateConsistency(DeepResearchContext context);

    
    String synthesizeKnowledge(DeepResearchContext context) throws Research4jException;

    
    String generateFinalReport(
        DeepResearchContext context,
        String synthesizedKnowledge) throws Research4jException;
}