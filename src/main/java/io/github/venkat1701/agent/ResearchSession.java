package io.github.venkat1701.agent;

import java.util.List;
import java.util.Map;

import io.github.venkat1701.Research4j;
import io.github.venkat1701.core.enums.OutputFormat;
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

    public String getSessionId() {
        return sessionId;
    }

    public UserProfile getUserProfile() {
        return userProfile;
    }

    @Override
    public void close() {
    }
}