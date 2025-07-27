package io.github.venkat1701.deepresearch.models;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

public class ResearchQuestion {

    public enum Priority {
        LOW(1), MEDIUM(2), HIGH(3), CRITICAL(4);

        private final int value;
        Priority(int value) { this.value = value; }
        public int getValue() { return value; }
    }

    private final String question;
    private final Priority priority;
    private final String category;
    private final Instant createdAt;
    private boolean researched;
    private List<String> insights;
    private List<String> relatedQuestions;
    private double confidenceScore;

    public ResearchQuestion(String question, Priority priority, String category) {
        this.question = question;
        this.priority = priority;
        this.category = category;
        this.createdAt = Instant.now();
        this.researched = false;
        this.insights = new ArrayList<>();
        this.relatedQuestions = new ArrayList<>();
        this.confidenceScore = 0.0;
    }

    
    public String getQuestion() { return question; }
    public Priority getPriority() { return priority; }
    public String getCategory() { return category; }
    public Instant getCreatedAt() { return createdAt; }
    public boolean isResearched() { return researched; }
    public void setResearched(boolean researched) { this.researched = researched; }
    public List<String> getInsights() { return insights; }
    public void addInsight(String insight) { this.insights.add(insight); }
    public List<String> getRelatedQuestions() { return relatedQuestions; }
    public void addRelatedQuestion(String question) { this.relatedQuestions.add(question); }
    public double getConfidenceScore() { return confidenceScore; }
    public void setConfidenceScore(double score) { this.confidenceScore = score; }
}