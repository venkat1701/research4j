package com.github.bhavuklabs.exceptions.reasoning;

import com.github.bhavuklabs.exceptions.Research4jException;

public class ReasoningException extends Research4jException {

    private final String reasoningMethod;
    private final String stage;

    public ReasoningException(String message, Throwable cause, String reasoningMethod, String stage) {
        super("REASONING_ERROR", message, cause, String.format("method=%s, stage=%s", reasoningMethod, stage));
        this.reasoningMethod = reasoningMethod;
        this.stage = stage;
    }

    public String getReasoningMethod() {
        return reasoningMethod;
    }

    public String getStage() {
        return stage;
    }
}