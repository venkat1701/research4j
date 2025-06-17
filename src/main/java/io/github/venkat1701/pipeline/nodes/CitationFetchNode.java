package io.github.venkat1701.pipeline.nodes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.pipeline.graph.GraphNode;
import io.github.venkat1701.pipeline.models.QueryAnalysis;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.pipeline.state.ResearchAgentState;

public class CitationFetchNode implements GraphNode<ResearchAgentState> {

    private final CitationService citationService;
    private final ExecutorService executor;

    public CitationFetchNode(CitationService citationService) {
        this.citationService = citationService;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public CompletableFuture<ResearchAgentState> process(ResearchAgentState state) {
        return CompletableFuture.supplyAsync(() -> {
            String enhancedQuery = enhanceQueryForCitations(state);
            List<CitationResult> citations = citationService.search(enhancedQuery);

            List<CitationResult> filteredCitations = filterCitationsForUser(citations, state.getUserProfile());

            return state.withCitations(filteredCitations);
        }, executor);
    }

    private String enhanceQueryForCitations(ResearchAgentState state) {
        UserProfile profile = state.getUserProfile();
        String baseQuery = state.getQuery();

        switch (profile.getDomain()) {
            case "academic":
                return baseQuery + " academic research papers scholarly";
            case "business":
                return baseQuery + " business analysis market research";
            case "technical":
                return baseQuery + " technical documentation engineering";
            default:
                return baseQuery;
        }
    }

    private List<CitationResult> filterCitationsForUser(List<CitationResult> citations, UserProfile profile) {
        return citations.stream()
            .sorted((a, b) -> {
                int scoreA = calculateRelevanceScore(a, profile);
                int scoreB = calculateRelevanceScore(b, profile);
                return Integer.compare(scoreB, scoreA);
            })
            .limit(getMaxCitations(profile))
            .toList();
    }

    private int calculateRelevanceScore(CitationResult citation, UserProfile profile) {
        int score = 0;
        String content = (citation.getContent() + " " + citation.getTitle()).toLowerCase();
        if (content.contains(profile.getDomain())) score += 20;
        for (Map.Entry<String, Integer> topic : profile.getTopicInterests().entrySet()) {
            if (content.contains(topic.getKey())) {
                score += topic.getValue();
            }
        }

        if (profile.getExpertiseLevel().equals("expert") &&
            (content.contains("advanced") || content.contains("technical"))) {
            score += 10;
        } else if (profile.getExpertiseLevel().equals("beginner") &&
            (content.contains("introduction") || content.contains("basics"))) {
            score += 10;
        }

        return score;
    }

    private long getMaxCitations(UserProfile profile) {
        return switch (profile.getExpertiseLevel()) {
            case "expert" -> 15;
            case "intermediate" -> 10;
            case "beginner" -> 5;
            default -> 8;
        };
    }

    @Override
    public String getName() { return "citation_fetch"; }

    @Override
    public boolean shouldExecute(ResearchAgentState state) {
        QueryAnalysis analysis = (QueryAnalysis) state.getMetadata().get("query_analysis");
        return analysis == null || analysis.requiresCitations;
    }
}
