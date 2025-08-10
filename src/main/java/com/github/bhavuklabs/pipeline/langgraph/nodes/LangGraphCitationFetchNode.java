package com.github.bhavuklabs.pipeline.langgraph.nodes;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bsc.langgraph4j.action.AsyncNodeAction;

import com.github.bhavuklabs.citation.CitationResult;
import com.github.bhavuklabs.citation.service.CitationService;
import com.github.bhavuklabs.exceptions.citation.CitationException;
import com.github.bhavuklabs.pipeline.langgraph.LangGraphState;
import com.github.bhavuklabs.pipeline.models.QueryAnalysis;

public class LangGraphCitationFetchNode implements AsyncNodeAction<LangGraphState> {

    private static final Logger logger = Logger.getLogger(LangGraphCitationFetchNode.class.getName());

    private final CitationService citationService;

    public LangGraphCitationFetchNode(CitationService citationService) {
        this.citationService = citationService;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(LangGraphState state) {
        try {
            logger.info("Starting LangGraph4j citation fetch for session: " + state.getSessionId());

            String query = state.getQuery();
            QueryAnalysis analysis = state.getQueryAnalysis();

            if (analysis != null && !analysis.requiresCitations) {
                logger.info("Analysis indicates citations not required, skipping fetch");
                return CompletableFuture.supplyAsync(() -> Map.of());
            }

            int targetCitations = determineTargetCitations(analysis);

            List<CitationResult> citations = fetchCitations(query, targetCitations);

            logger.info("Fetched " + citations.size() + " citations for query");

            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.CITATIONS, citations));

        } catch (Exception e) {
            logger.severe("Citation fetch failed: " + e.getMessage());

            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.CITATIONS, new ArrayList<CitationResult>()));
        }
    }

    private int determineTargetCitations(QueryAnalysis analysis) {
        if (analysis == null) {
            return 5;
        }

        return switch (analysis.complexityScore) {
            case 1, 2, 3 -> 3;
            case 4, 5, 6 -> 5;
            case 7, 8 -> 8;
            default -> 10;
        };
    }

    private List<CitationResult> fetchCitations(String query, int targetCount) {
        try {

            List<CitationResult> citations = citationService.search(query);

            if (citations == null) {
                return new ArrayList<>();
            }

            return citations.stream()
                .filter(citation -> citation.getRelevanceScore() >= 0.3)
                .sorted((a, b) -> Double.compare(b.getRelevanceScore(), a.getRelevanceScore()))
                .limit(targetCount)
                .collect(java.util.stream.Collectors.toList());

        } catch (CitationException e) {
            logger.warning("Citation service error: " + e.getMessage());
            return new ArrayList<>();
        } catch (Exception e) {
            logger.warning("Error fetching citations: " + e.getMessage());
            return new ArrayList<>();
        }
    }
}