package com.github.bhavuklabs.deepresearch.models;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NarrativeSection {

    private final String title;
    private String focus;
    private int targetLength;
    private String priority;
    private List<String> dependencies;
    private Map<String, Object> metadata;

    public NarrativeSection(String title) {
        this.title = title;
        this.focus = "";
        this.targetLength = 1000;
        this.priority = "Medium";
        this.dependencies = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public NarrativeSection(String title, String focus, int targetLength, String priority) {
        this.title = title;
        this.focus = focus;
        this.targetLength = targetLength;
        this.priority = priority;
        this.dependencies = new ArrayList<>();
        this.metadata = new HashMap<>();
    }

    public String getTitle() {
        return title;
    }

    public String getFocus() {
        return focus;
    }

    public int getTargetLength() {
        return targetLength;
    }

    public String getPriority() {
        return priority;
    }

    public List<String> getDependencies() {
        return new ArrayList<>(dependencies);
    }

    public Map<String, Object> getMetadata() {
        return new HashMap<>(metadata);
    }

    public void setFocus(String focus) {
        this.focus = focus;
    }

    public void setTargetLength(int targetLength) {
        this.targetLength = targetLength;
    }

    public void setPriority(String priority) {
        this.priority = priority;
    }

    public void addDependency(String dependency) {
        this.dependencies.add(dependency);
    }

    public void setMetadata(String key, Object value) {
        this.metadata.put(key, value);
    }

    @Override
    public String toString() {
        return String.format("NarrativeSection{title='%s', focus='%s', targetLength=%d, priority='%s'}", title, focus, targetLength, priority);
    }
}
