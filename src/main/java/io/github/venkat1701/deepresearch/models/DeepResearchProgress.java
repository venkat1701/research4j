package io.github.venkat1701.deepresearch.models;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class DeepResearchProgress {

    private final String sessionId;
    private final Instant startTime;
    private volatile Instant endTime;
    private volatile ResearchPhase currentPhase;
    private final AtomicInteger progressPercentage;
    private volatile String currentActivity;
    private final List<String> completedActivities;
    private final List<String> errors;
    private volatile boolean cancelled;

    public DeepResearchProgress(String sessionId) {
        this.sessionId = sessionId;
        this.startTime = Instant.now();
        this.currentPhase = ResearchPhase.INITIAL_ANALYSIS;
        this.progressPercentage = new AtomicInteger(0);
        this.currentActivity = "Initializing deep research...";
        this.completedActivities = new ArrayList<>();
        this.errors = new ArrayList<>();
        this.cancelled = false;
    }

    public synchronized void updateProgress(int percentage, String activity) {
        this.progressPercentage.set(Math.max(0, Math.min(100, percentage)));
        if (activity != null) {
            this.completedActivities.add(this.currentActivity);
            this.currentActivity = activity;
        }

        if (percentage >= 100) {
            this.currentPhase = ResearchPhase.COMPLETED;
            this.endTime = Instant.now();
        }
    }

    public synchronized void setCurrentPhase(ResearchPhase phase) {
        this.currentPhase = phase;
        this.currentActivity = phase.getDescription();
    }

    public synchronized void addError(String error) {
        this.errors.add(error);
    }

    public synchronized void cancel() {
        this.cancelled = true;
        this.endTime = Instant.now();
        this.currentActivity = "Research cancelled";
    }

    public String getSessionId() {
        return sessionId;
    }

    public Instant getStartTime() {
        return startTime;
    }

    public Instant getEndTime() {
        return endTime;
    }

    public ResearchPhase getCurrentPhase() {
        return currentPhase;
    }

    public int getProgressPercentage() {
        return progressPercentage.get();
    }

    public String getCurrentActivity() {
        return currentActivity;
    }

    public List<String> getCompletedActivities() {
        return new ArrayList<>(completedActivities);
    }

    public List<String> getErrors() {
        return new ArrayList<>(errors);
    }

    public boolean isCancelled() {
        return cancelled;
    }

    public boolean isCompleted() {
        return currentPhase == ResearchPhase.COMPLETED;
    }

    public Duration getTotalDuration() {
        Instant end = endTime != null ? endTime : Instant.now();
        return Duration.between(startTime, end);
    }

    public Duration getEstimatedTimeRemaining() {
        if (isCompleted() || isCancelled()) {
            return Duration.ZERO;
        }

        Duration elapsed = getTotalDuration();
        int progress = getProgressPercentage();

        if (progress <= 0) {
            return Duration.ofMinutes(15);
        }

        long totalEstimatedSeconds = (elapsed.getSeconds() * 100) / progress;
        long remainingSeconds = totalEstimatedSeconds - elapsed.getSeconds();

        return Duration.ofSeconds(Math.max(0, remainingSeconds));
    }
}