package com.github.bhavuklabs.pipeline.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.github.bhavuklabs.core.enums.OutputFormat;

public class UserProfile {

    private final String userId;
    private final String domain;
    private final String expertiseLevel;

    private final List<String> preferences;
    private final Map<String, Integer> topicInterests;
    private final List<String> previousQueries;
    private final OutputFormat preferredFormat;

    public UserProfile(String userId, String domain, String expertiseLevel, List<String> preferences, Map<String, Integer> topicInterests,
        List<String> previousQueries, OutputFormat preferredFormat) {
        this.userId = userId;
        this.domain = domain != null ? domain : "general";
        this.expertiseLevel = expertiseLevel != null ? expertiseLevel : "intermediate";
        this.preferences = preferences != null ? preferences : List.of("balanced");
        this.topicInterests = topicInterests != null ? topicInterests : new HashMap<>();
        this.previousQueries = previousQueries != null ? previousQueries : new ArrayList<>();
        this.preferredFormat = preferredFormat != null ? preferredFormat : OutputFormat.MARKDOWN;
    }

    public String getUserId() {
        return userId;
    }

    public String getDomain() {
        return domain;
    }

    public String getExpertiseLevel() {
        return expertiseLevel;
    }

    public List<String> getPreferences() {
        return preferences;
    }

    public Map<String, Integer> getTopicInterests() {
        return topicInterests;
    }

    public List<String> getPreviousQueries() {
        return previousQueries;
    }

    public OutputFormat getPreferredFormat() {
        return preferredFormat;
    }

    public boolean hasPreference(String preference) {
        return preferences.contains(preference);
    }

    public int getTopicWeight(String topic) {
        return topicInterests.getOrDefault(topic.toLowerCase(), 0);
    }
}
