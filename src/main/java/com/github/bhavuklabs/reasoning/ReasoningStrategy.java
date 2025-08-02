package com.github.bhavuklabs.reasoning;

import java.util.concurrent.CompletableFuture;

import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.exceptions.client.LLMClientException;
import com.github.bhavuklabs.reasoning.context.ResearchContext;

public interface ReasoningStrategy {
    <T>LLMResponse<T> reason(ResearchContext context, Class<T> outputType) throws LLMClientException;
    <T> CompletableFuture<LLMResponse<T>> reasonAsync(ResearchContext context, Class<T> outputType);
    String getMethodName();
    boolean supportsConcurrency();
}
