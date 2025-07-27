package io.github.venkat1701.examples;

import io.github.venkat1701.Research4j;
import io.github.venkat1701.deepresearch.models.DeepResearchResult;

public class ResearchEventHandler {
    private final Research4j research4j;

    public ResearchEventHandler(Research4j research4j) {
        this.research4j = research4j;
    }

    public void handleResearchRequest(String query, String userId) {
        research4j.deepResearch(query)
            .thenAccept(result -> {
                
                System.out.println("Research completed for user: " + userId);
                System.out.println("Quality score: " + result.getMetrics().getQualityScore());

                
                notifyUserOfCompletion(userId, result);
            })
            .exceptionally(throwable -> {
                System.err.println("Research failed for user " + userId + ": " + throwable.getMessage());
                notifyUserOfFailure(userId, throwable);
                return null;
            });
    }

    private void notifyUserOfCompletion(String userId, DeepResearchResult result) {
        
        System.out.println("Notification sent to user " + userId + " about research completion");
    }

    private void notifyUserOfFailure(String userId, Throwable error) {
        
        System.out.println("Error notification sent to user " + userId + ": " + error.getMessage());
    }
}