package io.github.venkat1701.agent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.github.venkat1701.Research4j;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.DeepResearchProgress;
import io.github.venkat1701.deepresearch.models.DeepResearchResult;
import io.github.venkat1701.pipeline.profile.UserProfile;


public class ResearchSession implements AutoCloseable {

    private final Research4j research4j;
    private final String sessionId;
    private final UserProfile userProfile;

    public ResearchSession(Research4j research4j, String sessionId) {
        this(research4j, sessionId, createDefaultProfile());
    }

    public ResearchSession(Research4j research4j, String sessionId, UserProfile userProfile) {
        this.research4j = research4j;
        this.sessionId = sessionId;
        this.userProfile = userProfile;
    }

    private static UserProfile createDefaultProfile() {
        return new UserProfile("session-user", "general", "intermediate", List.of("balanced"), Map.of(), List.of(), OutputFormat.MARKDOWN);
    }

    
    public ResearchResult query(String query) {
        return research4j.research(query, userProfile);
    }

    public ResearchResult query(String query, OutputFormat format) {
        return research4j.research(query, userProfile, format);
    }

    
    public CompletableFuture<DeepResearchResult> deepQuery(String query) {
        return research4j.deepResearch(query, userProfile);
    }

    public CompletableFuture<DeepResearchResult> deepQuery(String query, DeepResearchConfig config) {
        return research4j.deepResearch(query, userProfile, config);
    }

    public CompletableFuture<DeepResearchResult> quickDeepQuery(String query) {
        return research4j.deepResearch(query, userProfile, DeepResearchConfig.standardConfig());
    }

    public CompletableFuture<DeepResearchResult> comprehensiveDeepQuery(String query) {
        return research4j.deepResearch(query, userProfile, DeepResearchConfig.comprehensiveConfig());
    }

    public CompletableFuture<DeepResearchResult> exhaustiveDeepQuery(String query) {
        return research4j.deepResearch(query, userProfile, DeepResearchConfig.exhaustiveConfig());
    }

    
    public DeepResearchProgress getDeepResearchProgress(String researchSessionId) {
        return research4j.getDeepResearchProgress(researchSessionId);
    }

    public boolean cancelDeepResearch(String researchSessionId) {
        return research4j.cancelDeepResearch(researchSessionId);
    }

    
    public String getSessionId() {
        return sessionId;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    @Override
    public void close() {
        
    }

    public Research4j getResearch4j() {
        return research4j;
    }


}