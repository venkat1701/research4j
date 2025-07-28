package io.github.venkat1701.deepresearch.context;

import java.time.Instant;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.deepresearch.models.DeepResearchResult;


public class MemoryManager {

    private static final Logger logger = Logger.getLogger(MemoryManager.class.getName());


    private final Map<String, DeepResearchResult> researchHistory;
    private final Map<String, Object> knowledgeBase;
    private final Map<String, List<CitationResult>> citationCache;
    private final Map<String, Map<String, Double>> conceptRelationships;
    private final Map<String, Instant> lastAccessed;


    private final ExecutorService memoryExecutor;
    private volatile boolean shutdown = false;


    private final int maxMemoryEntries;
    private final long memoryRetentionHours;

    public MemoryManager() {
        this.researchHistory = new ConcurrentHashMap<>();
        this.knowledgeBase = new ConcurrentHashMap<>();
        this.citationCache = new ConcurrentHashMap<>();
        this.conceptRelationships = new ConcurrentHashMap<>();
        this.lastAccessed = new ConcurrentHashMap<>();

        this.memoryExecutor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r, "MemoryManager-Cleanup");
            t.setDaemon(true);
            return t;
        });

        this.maxMemoryEntries = 1000;
        this.memoryRetentionHours = 24;


        startMemoryCleanupTask();

        logger.info("MemoryManager initialized with retention: " + memoryRetentionHours + " hours");
    }


    public void storeResearchResult(String sessionId, DeepResearchResult result) {
        researchHistory.put(sessionId, result);
        lastAccessed.put(sessionId, Instant.now());


        extractAndStoreKnowledge(result);

        logger.info("Stored research result for session: " + sessionId);
    }


    public DeepResearchResult getResearchResult(String sessionId) {
        DeepResearchResult result = researchHistory.get(sessionId);
        if (result != null) {
            lastAccessed.put(sessionId, Instant.now());
        }
        return result;
    }


    public void updateKnowledge(String concept, String knowledge, List<CitationResult> sources) {
        knowledgeBase.put(concept, knowledge);
        if (sources != null && !sources.isEmpty()) {
            citationCache.put(concept, sources);
        }
        lastAccessed.put(concept, Instant.now());


        updateConceptRelationships(concept, knowledge);

        logger.fine("Updated knowledge for concept: " + concept);
    }


    public String getKnowledge(String concept) {
        Object knowledge = knowledgeBase.get(concept);
        if (knowledge != null) {
            lastAccessed.put(concept, Instant.now());
            return knowledge.toString();
        }
        return null;
    }


    public List<CitationResult> getCachedCitations(String concept) {
        List<CitationResult> citations = citationCache.get(concept);
        if (citations != null) {
            lastAccessed.put(concept, Instant.now());
        }
        return citations;
    }


    public List<String> findRelatedConcepts(String concept, double minSimilarity) {
        Map<String, Double> relationships = conceptRelationships.get(concept);
        if (relationships == null) {
            return List.of();
        }

        return relationships.entrySet().stream()
            .filter(entry -> entry.getValue() >= minSimilarity)
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .map(Map.Entry::getKey)
            .limit(10)
            .collect(java.util.stream.Collectors.toList());
    }


    public Map<String, Object> getMemoryStatistics() {
        Map<String, Object> stats = new HashMap<>();
        stats.put("researchHistorySize", researchHistory.size());
        stats.put("knowledgeBaseSize", knowledgeBase.size());
        stats.put("citationCacheSize", citationCache.size());
        stats.put("conceptRelationshipsSize", conceptRelationships.size());
        stats.put("lastCleanup", getLastCleanupTime());
        stats.put("memoryRetentionHours", memoryRetentionHours);

        return stats;
    }


    public Map<String, String> searchKnowledge(String query) {
        Map<String, String> results = new HashMap<>();
        String queryLower = query.toLowerCase();

        for (Map.Entry<String, Object> entry : knowledgeBase.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue().toString();

            if (key.toLowerCase().contains(queryLower) ||
                value.toLowerCase().contains(queryLower)) {
                results.put(key, value);
                lastAccessed.put(key, Instant.now());
            }
        }

        return results;
    }


    public void clearSession(String sessionId) {
        researchHistory.remove(sessionId);
        lastAccessed.remove(sessionId);
        logger.info("Cleared session data: " + sessionId);
    }


    public void clearAllMemory() {
        researchHistory.clear();
        knowledgeBase.clear();
        citationCache.clear();
        conceptRelationships.clear();
        lastAccessed.clear();
        logger.warning("Cleared all memory data");
    }

    private void extractAndStoreKnowledge(DeepResearchResult result) {

        for (var question : result.getResearchQuestions()) {
            String concept = extractMainConcept(question.getQuestion());
            updateKnowledge(concept, question.getMetadata().toString(), null);
        }


        for (CitationResult citation : result.getAllCitations()) {
            String concept = extractMainConcept(citation.getTitle());
            citationCache.put(concept, List.of(citation));
        }
    }

    private void updateConceptRelationships(String concept, String knowledge) {

        Map<String, Double> relationships = conceptRelationships.computeIfAbsent(concept, k -> new ConcurrentHashMap<>());


        for (String otherConcept : knowledgeBase.keySet()) {
            if (!otherConcept.equals(concept)) {
                double similarity = calculateSimilarity(knowledge, knowledgeBase.get(otherConcept).toString());
                if (similarity > 0.3) {
                    relationships.put(otherConcept, similarity);
                }
            }
        }
    }

    private double calculateSimilarity(String text1, String text2) {
        try {

            String[] words1 = text1.toLowerCase().split("\\W+");
            String[] words2 = text2.toLowerCase().split("\\W+");


            Set<String> set1 = new HashSet<>(Arrays.asList(words1));
            Set<String> set2 = new HashSet<>(Arrays.asList(words2));


            Set<String> intersection = new HashSet<>(set1);
            intersection.retainAll(set2);


            Set<String> union = new HashSet<>(set1);
            union.addAll(set2);


            return union.isEmpty() ? 0.0 : (double) intersection.size() / union.size();

        } catch (Exception e) {
            logger.warning("Error calculating similarity: " + e.getMessage());
            return 0.0;
        }
    }

    private String extractMainConcept(String text) {

        String[] words = text.toLowerCase().split("\\W+");
        for (String word : words) {
            if (word.length() > 4 && !isStopWord(word)) {
                return word;
            }
        }
        return text.length() > 20 ? text.substring(0, 20) : text;
    }

    private boolean isStopWord(String word) {
        java.util.Set<String> stopWords = java.util.Set.of("what", "how", "when", "where", "why", "which", "that", "this", "with", "from", "they", "been", "said", "each", "their", "time", "about");
        return stopWords.contains(word);
    }

    private void startMemoryCleanupTask() {
        memoryExecutor.submit(() -> {
            while (!shutdown) {
                try {
                    Thread.sleep(TimeUnit.HOURS.toMillis(1));
                    performMemoryCleanup();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    logger.warning("Memory cleanup error: " + e.getMessage());
                }
            }
        });
    }

    private void performMemoryCleanup() {
        Instant cutoff = Instant.now().minusSeconds(memoryRetentionHours * 3600);
        AtomicInteger removedEntries = new AtomicInteger();


        lastAccessed.entrySet().removeIf(entry -> {
            if (entry.getValue().isBefore(cutoff)) {
                String key = entry.getKey();
                researchHistory.remove(key);
                knowledgeBase.remove(key);
                citationCache.remove(key);
                conceptRelationships.remove(key);
                return true;
            }
            return false;
        });


        if (researchHistory.size() > maxMemoryEntries) {

            researchHistory.entrySet().stream()
                .sorted(Map.Entry.<String, DeepResearchResult>comparingByValue(
                    (r1, r2) -> r1.getCompletedAt().compareTo(r2.getCompletedAt())))
                .limit(researchHistory.size() - maxMemoryEntries)
                .forEach(entry -> {
                    String key = entry.getKey();
                    researchHistory.remove(key);
                    lastAccessed.remove(key);
                    removedEntries.getAndIncrement();
                });
        }

        if (removedEntries.get() > 0) {
            logger.info("Memory cleanup completed: removed " + removedEntries + " old entries");
        }
    }

    private Instant getLastCleanupTime() {

        return Instant.now().minusSeconds(3600);
    }

    public void shutdown() {
        shutdown = true;
        memoryExecutor.shutdown();
        try {
            if (!memoryExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
                memoryExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            memoryExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        logger.info("MemoryManager shutdown completed");
    }
}