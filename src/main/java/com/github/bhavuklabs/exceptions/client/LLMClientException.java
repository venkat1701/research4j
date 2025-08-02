package com.github.bhavuklabs.exceptions.client;

import com.github.bhavuklabs.exceptions.Research4jException;

public class LLMClientException extends Research4jException {

    private final String modelType;
    private final String operation;

    public LLMClientException(String message, String modelType) {
        super("LLM_ERROR", message, modelType);
        this.modelType = modelType;
        this.operation = null;
    }

    public LLMClientException(String message, String modelType, String operation) {
        super("LLM_ERROR", message, String.format("model=%s, operation=%s", modelType, operation));
        this.modelType = modelType;
        this.operation = operation;
    }

    public LLMClientException(String message, Throwable cause, String modelType, String operation) {
        super("LLM_ERROR", message, cause, String.format("model=%s, operation=%s", modelType, operation));
        this.modelType = modelType;
        this.operation = operation;
    }

    public String getModelType() {
        return modelType;
    }

    public String getOperation() {
        return operation;
    }
}
