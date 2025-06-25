package io.github.venkat1701.exceptions.pipeline;

import io.github.venkat1701.exceptions.Research4jException;

public class PipelineException extends Research4jException {

    private final String sessionId;
    private final String currentNode;
    private final String phase;

    public PipelineException(String message, String sessionId, String currentNode) {
        super("PIPELINE_ERROR", message, String.format("session=%s, node=%s", sessionId, currentNode));
        this.sessionId = sessionId;
        this.currentNode = currentNode;
        this.phase = null;
    }

    public PipelineException(String message, String sessionId, String currentNode, String phase) {
        super("PIPELINE_ERROR", message, String.format("session=%s, node=%s, phase=%s", sessionId, currentNode, phase));
        this.sessionId = sessionId;
        this.currentNode = currentNode;
        this.phase = phase;
    }

    public PipelineException(String message, Throwable cause, String sessionId, String currentNode, String phase) {
        super("PIPELINE_ERROR", message, cause, String.format("session=%s, node=%s, phase=%s", sessionId, currentNode, phase));
        this.sessionId = sessionId;
        this.currentNode = currentNode;
        this.phase = phase;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public String getPhase() {
        return phase;
    }
}