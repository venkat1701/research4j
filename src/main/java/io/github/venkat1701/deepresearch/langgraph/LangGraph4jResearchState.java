package io.github.venkat1701.deepresearch.langgraph;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.ResearchQuestion;
import io.github.venkat1701.pipeline.profile.UserProfile;

public class LangGraph4jResearchState {
    private String originalQuery;
    private UserProfile userProfile;
    private DeepResearchConfig config;
    private java.time.Instant startTime;
    private java.time.Instant endTime;
    private String currentPhase;
    private int complexityScore;

    private Map<String, Object> queryAnalysis = new HashMap<>();
    private List<ResearchQuestion> researchQuestions = List.of();
    private Map<String, List<CitationResult>> researchResults = new HashMap<>();
    private List<CitationResult> citations = List.of();
    private Map<String, Object> knowledgeGraph = new HashMap<>();
    private Map<String, Object> validationResults = new HashMap<>();
    private String synthesis;
    private String finalReport;
    private Map<String, String> errors = new HashMap<>();

    
    public String getOriginalQuery() { return originalQuery; }
    public void setOriginalQuery(String originalQuery) { this.originalQuery = originalQuery; }

    public UserProfile getUserProfile() { return userProfile; }
    public void setUserProfile(UserProfile userProfile) { this.userProfile = userProfile; }

    public DeepResearchConfig getConfig() { return config; }
    public void setConfig(DeepResearchConfig config) { this.config = config; }

    public java.time.Instant getStartTime() { return startTime; }
    public void setStartTime(java.time.Instant startTime) { this.startTime = startTime; }

    public java.time.Instant getEndTime() { return endTime; }
    public void setEndTime(java.time.Instant endTime) { this.endTime = endTime; }

    public String getCurrentPhase() { return currentPhase; }
    public void setCurrentPhase(String currentPhase) { this.currentPhase = currentPhase; }

    public int getComplexityScore() { return complexityScore; }
    public void setComplexityScore(int complexityScore) { this.complexityScore = complexityScore; }

    public Map<String, Object> getQueryAnalysis() { return queryAnalysis; }
    public void setQueryAnalysis(Map<String, Object> queryAnalysis) { this.queryAnalysis = queryAnalysis; }

    public List<ResearchQuestion> getResearchQuestions() { return researchQuestions; }
    public void setResearchQuestions(List<ResearchQuestion> researchQuestions) { this.researchQuestions = researchQuestions; }

    public Map<String, List<CitationResult>> getResearchResults() { return researchResults; }
    public void setResearchResults(Map<String, List<CitationResult>> researchResults) { this.researchResults = researchResults; }

    public List<CitationResult> getCitations() { return citations; }
    public void setCitations(List<CitationResult> citations) { this.citations = citations; }

    public Map<String, Object> getKnowledgeGraph() { return knowledgeGraph; }
    public void setKnowledgeGraph(Map<String, Object> knowledgeGraph) { this.knowledgeGraph = knowledgeGraph; }

    public Map<String, Object> getValidationResults() { return validationResults; }
    public void setValidationResults(Map<String, Object> validationResults) { this.validationResults = validationResults; }

    public String getSynthesis() { return synthesis; }
    public void setSynthesis(String synthesis) { this.synthesis = synthesis; }

    public String getFinalReport() { return finalReport; }
    public void setFinalReport(String finalReport) { this.finalReport = finalReport; }

    public void addError(String phase, String error) {
        this.errors.put(phase, error);
    }

    public Map<String, String> getErrors() { return errors; }
}