package io.github.venkat1701.reasoning;

import java.util.concurrent.CompletableFuture;

import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.reasoning.context.ResearchContext;

public interface ReasoningStrategy {
    <T>LLMResponse<T> reason(ResearchContext context, Class<T> outputType);
    <T> CompletableFuture<LLMResponse<T>> reasonAsync(ResearchContext context, Class<T> outputType);
    String getMethodName();
    boolean supportsConcurrency();
}
