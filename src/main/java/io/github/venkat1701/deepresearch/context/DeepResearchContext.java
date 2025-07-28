package io.github.venkat1701.deepresearch.context;

import java.time.Instant;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;

/**
 * Deep Research Context - Manages the complete context for a research session
 * Tracks citations, insights, questions, and configuration throughout the research process
 */
public class DeepResearchContext {

    private final String sessionId;
    private final String originalQuery;
    private final DeepResearchConfig config;
    private final Instant startTime;

    // Research State
    private final List<CitationResult> allCitations;
    private final Map<String, String> allInsights;
    private final List<ResearchQuestion> researchQuestions;
    private final Set<String> exploredTopics;
    private final Map<String, Object> sessionMetadata;

    // Progress Tracking
    private int currentRound;
    private int totalQuestionsGenerated;
    private int totalQuestionsResearched;
    private double averageRelevanceScore;

    // Thread-safe collections for concurrent access
    private final Map<String, String> threadSafeInsights;
    private final List<CitationResult> threadSafeCitations;

    public DeepResearchContext(String sessionId, String originalQuery, DeepResearchConfig config) {
        this.sessionId = sessionId;
        this.originalQuery = originalQuery;
        this.config = config;
        this.startTime = Instant.now();

        // Initialize collections
        this.allCitations = Collections.synchronizedList(new ArrayList<>());
        this.allInsights = new ConcurrentHashMap<>();
        this.researchQuestions = Collections.synchronizedList(new ArrayList<>());
        this.exploredTopics = Collections.synchronizedSet(new HashSet<>());
        this.sessionMetadata = new ConcurrentHashMap<>();

        // Thread-safe views
        this.threadSafeInsights = new ConcurrentHashMap<>();
        this.threadSafeCitations = Collections.synchronizedList(new ArrayList<>());

        // Initialize progress tracking
        this.currentRound = 0;
        this.totalQuestionsGenerated = 0;
        this.totalQuestionsResearched = 0;
        this.averageRelevanceScore = 0.0;

        // Initialize session metadata
        initializeSessionMetadata();
    }

    private void initializeSessionMetadata() {
        sessionMetadata.put("startTime", startTime);
        sessionMetadata.put("originalQuery", originalQuery);
        sessionMetadata.put("researchDepth", config.getResearchDepth());
        sessionMetadata.put("version", "1.0");
        sessionMetadata.put("engine", "DeepResearchEngine");
    }

    // Citation Management
    public synchronized void addCitation(CitationResult citation) {
        if (citation != null && !allCitations.contains(citation)) {
            allCitations.add(citation);
            threadSafeCitations.add(citation);
            updateAverageRelevanceScore();

            // Extract and add topics from citation
            extractTopicsFromCitation(citation);
        }
    }

    public synchronized void addCitations(List<CitationResult> citations) {
        for (CitationResult citation : citations) {
            addCitation(citation);
        }
    }

    // Insight Management
    public void addInsight(String question, String insight) {
        if (question != null && insight != null && !insight.trim().isEmpty()) {
            allInsights.put(question, insight);
            threadSafeInsights.put(question, insight);
        }
    }

    public void addInsights(Map<String, String> insights) {
        for (Map.Entry<String, String> entry : insights.entrySet()) {
            addInsight(entry.getKey(), entry.getValue());
        }
    }

    // Research Question Management
    public synchronized void addResearchQuestion(ResearchQuestion question) {
        if (question != null && !researchQuestions.contains(question)) {
            researchQuestions.add(question);
            totalQuestionsGenerated++;

            // Extract topics from question
            exploredTopics.addAll(question.getKeywords());
        }
    }

    public synchronized void addResearchQuestions(List<ResearchQuestion> questions) {
        for (ResearchQuestion question : questions) {
            addResearchQuestion(question);
        }
    }

    public synchronized void markQuestionAsResearched(ResearchQuestion question) {
        if (question != null && researchQuestions.contains(question)) {
            question.markAsResearched();
            totalQuestionsResearched++;
        }
    }

    // Topic Management
    public void addExploredTopic(String topic) {
        if (topic != null && !topic.trim().isEmpty()) {
            exploredTopics.add(topic.toLowerCase().trim());
        }
    }

    public void addExploredTopics(Collection<String> topics) {
        for (String topic : topics) {
            addExploredTopic(topic);
        }
    }

    // Progress Tracking
    public void incrementRound() {
        this.currentRound++;
        sessionMetadata.put("currentRound", currentRound);
    }

    public void updateProgress(int questionsGenerated, int questionsResearched) {
        this.totalQuestionsGenerated += questionsGenerated;
        this.totalQuestionsResearched += questionsResearched;

        sessionMetadata.put("totalQuestionsGenerated", totalQuestionsGenerated);
        sessionMetadata.put("totalQuestionsResearched", totalQuestionsResearched);
        sessionMetadata.put("completionRate", getCompletionRate());
    }

    // Analytics and Metrics
    public double getCompletionRate() {
        return totalQuestionsGenerated > 0 ?
            (double) totalQuestionsResearched / totalQuestionsGenerated : 0.0;
    }

    public Map<String, Integer> getCategoryDistribution() {
        Map<String, Integer> distribution = new HashMap<>();

        synchronized (researchQuestions) {
            for (ResearchQuestion question : researchQuestions) {
                String category = question.getCategory();
                distribution.merge(category, 1, Integer::sum);
            }
        }

        return distribution;
    }

    public Map<String, Integer> getDomainDistribution() {
        Map<String, Integer> distribution = new HashMap<>();

        synchronized (allCitations) {
            for (CitationResult citation : allCitations) {
                String domain = citation.getDomain();
                distribution.merge(domain, 1, Integer::sum);
            }
        }

        return distribution;
    }

    public int getUniqueDomainsCount() {
        return getDomainDistribution().size();
    }

    public List<ResearchQuestion> getHighPriorityQuestions() {
        synchronized (researchQuestions) {
            return researchQuestions.stream()
                .filter(q -> q.getPriority() == ResearchQuestion.Priority.HIGH)
                .toList();
        }
    }

    public List<ResearchQuestion> getUnresearchedQuestions() {
        synchronized (researchQuestions) {
            return researchQuestions.stream()
                .filter(q -> !q.isResearched())
                .toList();
        }
    }

    // Context Summary
    public ContextSummary generateSummary() {
        return new ContextSummary(
            sessionId,
            originalQuery,
            currentRound,
            allCitations.size(),
            allInsights.size(),
            researchQuestions.size(),
            totalQuestionsResearched,
            exploredTopics.size(),
            averageRelevanceScore,
            getCompletionRate(),
            getUniqueDomainsCount(),
            startTime,
            Instant.now()
        );
    }

    // Export context for external use
    public Map<String, Object> exportContext() {
        Map<String, Object> export = new HashMap<>();

        export.put("sessionId", sessionId);
        export.put("originalQuery", originalQuery);
        export.put("config", config);
        export.put("startTime", startTime);
        export.put("currentRound", currentRound);

        // Statistics
        export.put("totalCitations", allCitations.size());
        export.put("totalInsights", allInsights.size());
        export.put("totalQuestions", researchQuestions.size());
        export.put("completionRate", getCompletionRate());
        export.put("averageRelevanceScore", averageRelevanceScore);
        export.put("uniqueDomains", getUniqueDomainsCount());

        // Data snapshots (for export only)
        export.put("exploredTopics", new ArrayList<>(exploredTopics));
        export.put("categoryDistribution", getCategoryDistribution());
        export.put("domainDistribution", getDomainDistribution());
        export.put("sessionMetadata", new HashMap<>(sessionMetadata));

        return export;
    }

    // Helper methods
    private void updateAverageRelevanceScore() {
        synchronized (allCitations) {
            this.averageRelevanceScore = allCitations.stream()
                .mapToDouble(CitationResult::getRelevanceScore)
                .average()
                .orElse(0.0);
        }
        sessionMetadata.put("averageRelevanceScore", averageRelevanceScore);
    }

    private void extractTopicsFromCitation(CitationResult citation) {
        // Extract keywords from title and content
        String text = (citation.getTitle() + " " + citation.getContent()).toLowerCase();
        String[] words = text.split("\\W+");

        for (String word : words) {
            if (word.length() > 4 && !isStopWord(word)) {
                exploredTopics.add(word);
            }
        }
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of(
            "the", "and", "for", "are", "but", "not", "you", "all", "can", "had", "her", "was", "one",
            "our", "out", "day", "get", "has", "him", "his", "how", "its", "may", "new", "now", "old",
            "see", "two", "way", "who", "with", "that", "this", "from", "they", "know", "want", "been",
            "good", "much", "some", "time", "very", "when", "come", "here", "just", "like", "long",
            "make", "many", "over", "such", "take", "than", "them", "well", "will"
        );
        return stopWords.contains(word.toLowerCase());
    }

    // Getters
    public String getSessionId() {
        return sessionId;
    }

    public String getOriginalQuery() {
        return originalQuery;
    }

    public DeepResearchConfig getConfig() {
        return config;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public List<CitationResult> getAllCitations() {
        synchronized (allCitations) {
            return new ArrayList<>(allCitations);
        }
    }

    public Map<String, String> getAllInsights() {
        return new HashMap<>(allInsights);
    }

    public List<ResearchQuestion> getResearchQuestions() {
        synchronized (researchQuestions) {
            return new ArrayList<>(researchQuestions);
        }
    }

    public Set<String> getExploredTopics() {
        return new HashSet<>(exploredTopics);
    }

    public int getCurrentRound() {
        return currentRound;
    }

    public int getTotalQuestionsGenerated() {
        return totalQuestionsGenerated;
    }

    public int getTotalQuestionsResearched() {
        return totalQuestionsResearched;
    }

    public double getAverageRelevanceScore() {
        return averageRelevanceScore;
    }

    public Map<String, Object> getSessionMetadata() {
        return new HashMap<>(sessionMetadata);
    }

    // Supporting Classes
    public static class ContextSummary {
        private final String sessionId;
        private final String originalQuery;
        private final int currentRound;
        private final int totalCitations;
        private final int totalInsights;
        private final int totalQuestions;
        private final int researchedQuestions;
        private final int exploredTopics;
        private final double averageRelevanceScore;
        private final double completionRate;
        private final int uniqueDomains;
        private final Instant startTime;
        private final Instant summaryTime;

        public ContextSummary(String sessionId, String originalQuery, int currentRound,
            int totalCitations, int totalInsights, int totalQuestions,
            int researchedQuestions, int exploredTopics, double averageRelevanceScore,
            double completionRate, int uniqueDomains, Instant startTime, Instant summaryTime) {
            this.sessionId = sessionId;
            this.originalQuery = originalQuery;
            this.currentRound = currentRound;
            this.totalCitations = totalCitations;
            this.totalInsights = totalInsights;
            this.totalQuestions = totalQuestions;
            this.researchedQuestions = researchedQuestions;
            this.exploredTopics = exploredTopics;
            this.averageRelevanceScore = averageRelevanceScore;
            this.completionRate = completionRate;
            this.uniqueDomains = uniqueDomains;
            this.startTime = startTime;
            this.summaryTime = summaryTime;
        }

        // Getters
        public String getSessionId() { return sessionId; }
        public String getOriginalQuery() { return originalQuery; }
        public int getCurrentRound() { return currentRound; }
        public int getTotalCitations() { return totalCitations; }
        public int getTotalInsights() { return totalInsights; }
        public int getTotalQuestions() { return totalQuestions; }
        public int getResearchedQuestions() { return researchedQuestions; }
        public int getExploredTopics() { return exploredTopics; }
        public double getAverageRelevanceScore() { return averageRelevanceScore; }
        public double getCompletionRate() { return completionRate; }
        public int getUniqueDomains() { return uniqueDomains; }
        public Instant getStartTime() { return startTime; }
        public Instant getSummaryTime() { return summaryTime; }

        @Override
        public String toString() {
            return String.format(
                "Context Summary [%s]: Round %d, %d citations, %d insights, %d/%d questions (%.1f%% complete)",
                sessionId, currentRound, totalCitations, totalInsights,
                researchedQuestions, totalQuestions, completionRate * 100
            );
        }
    }
}