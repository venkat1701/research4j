package com.github.bhavuklabs.deepresearch.models;

import java.util.HashMap;
import java.util.Map;

public class ResearchQuery {
    private String query;
    private String type;
    private String priority;
    private String expectedSources;
    private String rationale;
    private Map<String, Object> parameters;

    public ResearchQuery(String query, String type, String priority, String expectedSources) {
        this.query = query;
        this.type = type;
        this.priority = priority;
        this.expectedSources = expectedSources;
        this.rationale = "";
        this.parameters = new HashMap<>();
    }

    
    public String getQuery() { return query; }
    public String getType() { return type; }
    public String getPriority() { return priority; }
    public String getExpectedSources() { return expectedSources; }
    public String getRationale() { return rationale; }
    public Map<String, Object> getParameters() { return new HashMap<>(parameters); }

    
    public void setQuery(String query) { this.query = query; }
    public void setType(String type) { this.type = type; }
    public void setPriority(String priority) { this.priority = priority; }
    public void setExpectedSources(String expectedSources) { this.expectedSources = expectedSources; }
    public void setRationale(String rationale) { this.rationale = rationale; }
    public void setParameter(String key, Object value) { this.parameters.put(key, value); }

    @Override
    public String toString() {
        return String.format("ResearchQuery{query='%s', type='%s', priority='%s'}",
            query, type, priority);
    }
}

