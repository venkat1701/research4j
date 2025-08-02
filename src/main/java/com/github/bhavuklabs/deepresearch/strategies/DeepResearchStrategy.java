package com.github.bhavuklabs.deepresearch.strategies;

import java.util.List;
import java.util.Map;
import java.util.Set;

import com.github.bhavuklabs.citation.CitationResult;
import com.github.bhavuklabs.deepresearch.context.DeepResearchContext;
import com.github.bhavuklabs.deepresearch.models.ResearchQuestion;
import com.github.bhavuklabs.exceptions.Research4jException;


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