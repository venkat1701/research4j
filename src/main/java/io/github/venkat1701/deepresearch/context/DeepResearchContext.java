package io.github.venkat1701.deepresearch.context;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;
import io.github.venkat1701.pipeline.profile.UserProfile;


public class DeepResearchContext {

    private final String sessionId;
    private final String originalQuery;
    private final UserProfile userProfile;
    private final DeepResearchConfig config;
    private final Instant startTime;

    
    private final List<ResearchQuestion> researchQuestions;
    private final Map<String, List<CitationResult>> citationsByQuestion;
    private final Map<String, String> insightsByQuestion;
    private final Map<String, Object> knowledgeMap;
    private final Map<String, Set<String>> knowledgeRelationships;
    private final List<String> inconsistencies;

    
    private final ContextualMemory contextualMemory;
    private final ConceptualGraph conceptualGraph;
    private final InformationHierarchy informationHierarchy;
    private final Map<String, Double> conceptImportance;
    private final Map<String, List<String>> semanticClusters;

    
    private volatile String currentFocus;
    private final List<String> processedConcepts;
    private final Map<String, Object> processingMetadata;

    public DeepResearchContext(String sessionId, String originalQuery,
        UserProfile userProfile, DeepResearchConfig config) {
        this.sessionId = sessionId;
        this.originalQuery = originalQuery;
        this.userProfile = userProfile;
        this.config = config;
        this.startTime = Instant.now();

        
        this.researchQuestions = new ArrayList<>();
        this.citationsByQuestion = new ConcurrentHashMap<>();
        this.insightsByQuestion = new ConcurrentHashMap<>();
        this.knowledgeMap = new ConcurrentHashMap<>();
        this.knowledgeRelationships = new ConcurrentHashMap<>();
        this.inconsistencies = new ArrayList<>();

        
        this.contextualMemory = new ContextualMemory();
        this.conceptualGraph = new ConceptualGraph();
        this.informationHierarchy = new InformationHierarchy();
        this.conceptImportance = new ConcurrentHashMap<>();
        this.semanticClusters = new ConcurrentHashMap<>();

        
        this.processedConcepts = new ArrayList<>();
        this.processingMetadata = new ConcurrentHashMap<>();
    }

    
    public String getSessionId() { return sessionId; }
    public String getOriginalQuery() { return originalQuery; }
    public UserProfile getUserProfile() { return userProfile; }
    public DeepResearchConfig getConfig() { return config; }
    public Instant getStartTime() { return startTime; }

    
    public List<ResearchQuestion> getResearchQuestions() {
        return new ArrayList<>(researchQuestions);
    }

    public void addResearchQuestions(List<ResearchQuestion> questions) {
        this.researchQuestions.addAll(questions);

        
        for (ResearchQuestion question : questions) {
            conceptualGraph.addConcept(question.getQuestion(), question.getCategory());
            updateConceptImportance(question.getQuestion(), question.getPriority().getValue());
        }
    }

    
    public void addCitations(String questionKey, List<CitationResult> citations) {
        citationsByQuestion.merge(questionKey, citations, (existing, newList) -> {
            List<CitationResult> merged = new ArrayList<>(existing);
            merged.addAll(newList);
            return merged;
        });

        
        for (CitationResult citation : citations) {
            contextualMemory.addCitation(questionKey, citation);
            conceptualGraph.addRelationship(questionKey, citation.getTitle());
        }
    }

    public List<CitationResult> getAllCitations() {
        return citationsByQuestion.values().stream()
            .flatMap(List::stream)
            .distinct()
            .collect(java.util.stream.Collectors.toList());
    }

    public List<CitationResult> getCitationsForQuestion(String questionKey) {
        return citationsByQuestion.getOrDefault(questionKey, List.of());
    }

    
    public void addInsights(String questionKey, String insights) {
        insightsByQuestion.put(questionKey, insights);

        
        List<String> extractedConcepts = extractConcepts(insights);
        for (String concept : extractedConcepts) {
            conceptualGraph.addConcept(concept, "insight");
            conceptualGraph.addRelationship(questionKey, concept);
            updateConceptImportance(concept, 2.0); 
        }

        
        informationHierarchy.addInformation(questionKey, insights, "insight");
    }

    public String getInsightsForQuestion(String questionKey) {
        return insightsByQuestion.get(questionKey);
    }

    public Map<String, String> getAllInsights() {
        return new HashMap<>(insightsByQuestion);
    }

    
    public Map<String, Object> getKnowledgeMap() {
        return new HashMap<>(knowledgeMap);
    }

    public void updateKnowledgeMap(String key, Object value) {
        knowledgeMap.put(key, value);

        
        contextualMemory.updateKnowledge(key, value);
    }

    
    public Map<String, Set<String>> getKnowledgeRelationships() {
        return new HashMap<>(knowledgeRelationships);
    }

    public void setKnowledgeRelationships(Map<String, Set<String>> relationships) {
        this.knowledgeRelationships.clear();
        this.knowledgeRelationships.putAll(relationships);

        
        for (Map.Entry<String, Set<String>> entry : relationships.entrySet()) {
            for (String related : entry.getValue()) {
                conceptualGraph.addRelationship(entry.getKey(), related);
            }
        }
    }

    
    public void addInconsistencies(List<String> inconsistencies) {
        this.inconsistencies.addAll(inconsistencies);
    }

    public List<String> getInconsistencies() {
        return new ArrayList<>(inconsistencies);
    }

    
    public ContextualMemory getContextualMemory() {
        return contextualMemory;
    }

    public ConceptualGraph getConceptualGraph() {
        return conceptualGraph;
    }

    public InformationHierarchy getInformationHierarchy() {
        return informationHierarchy;
    }

    public Map<String, Double> getConceptImportance() {
        return new HashMap<>(conceptImportance);
    }

    public Map<String, List<String>> getSemanticClusters() {
        return new HashMap<>(semanticClusters);
    }

    public void updateSemanticClusters(Map<String, List<String>> clusters) {
        this.semanticClusters.clear();
        this.semanticClusters.putAll(clusters);
    }

    
    public String getCurrentFocus() {
        return currentFocus;
    }

    public void setCurrentFocus(String focus) {
        this.currentFocus = focus;
        this.processedConcepts.add(focus);
    }

    public List<String> getProcessedConcepts() {
        return new ArrayList<>(processedConcepts);
    }

    public Map<String, Object> getProcessingMetadata() {
        return new HashMap<>(processingMetadata);
    }

    public void addProcessingMetadata(String key, Object value) {
        this.processingMetadata.put(key, value);
    }

    
    private void updateConceptImportance(String concept, double importance) {
        conceptImportance.merge(concept, importance, Double::sum);
    }

    private List<String> extractConcepts(String text) {
        
        List<String> concepts = new ArrayList<>();
        String[] words = text.toLowerCase().split("\\W+");

        for (String word : words) {
            if (word.length() > 4 && !isStopWord(word)) {
                concepts.add(word);
            }
        }

        return concepts;
    }

    private boolean isStopWord(String word) {
        Set<String> stopWords = Set.of("this", "that", "with", "have", "will", "from", "they", "been", "said", "each", "which", "their", "time", "about");
        return stopWords.contains(word);
    }

    
    public double getContextualRelevance(String concept) {
        return conceptImportance.getOrDefault(concept, 0.0);
    }

    public List<String> getRelatedConcepts(String concept) {
        return conceptualGraph.getRelatedConcepts(concept);
    }

    public List<String> getMostImportantConcepts(int limit) {
        return conceptImportance.entrySet().stream()
            .sorted(Map.Entry.<String, Double>comparingByValue().reversed())
            .limit(limit)
            .map(Map.Entry::getKey)
            .collect(java.util.stream.Collectors.toList());
    }

    public Map<String, Object> getContextSummary() {
        Map<String, Object> summary = new HashMap<>();
        summary.put("sessionId", sessionId);
        summary.put("originalQuery", originalQuery);
        summary.put("totalQuestions", researchQuestions.size());
        summary.put("totalCitations", getAllCitations().size());
        summary.put("totalConcepts", conceptualGraph.getConceptCount());
        summary.put("averageRelevance", getAllCitations().stream()
            .mapToDouble(CitationResult::getRelevanceScore)
            .average().orElse(0.0));
        summary.put("topConcepts", getMostImportantConcepts(5));
        summary.put("processingDuration", java.time.Duration.between(startTime, Instant.now()));

        return summary;
    }
}