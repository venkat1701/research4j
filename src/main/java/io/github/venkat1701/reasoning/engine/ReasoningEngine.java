package io.github.venkat1701.reasoning.engine;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.exceptions.client.LLMClientException;
import io.github.venkat1701.reasoning.ReasoningStrategy;
import io.github.venkat1701.reasoning.context.ResearchContext;
import io.github.venkat1701.reasoning.strategy.ChainOfIdeasStrategy;
import io.github.venkat1701.reasoning.strategy.ChainOfTableStrategy;
import io.github.venkat1701.reasoning.strategy.ChainOfThoughtStrategy;

public class ReasoningEngine {

    private final Map<ReasoningMethod, ReasoningStrategy> strategies;
    private final ExecutorService executor;
    private final LLMClient llmClient;

    public ReasoningEngine(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
        this.strategies = initializeStrategies();
    }

    private Map<ReasoningMethod, ReasoningStrategy> initializeStrategies() {
        Map<ReasoningMethod, ReasoningStrategy> strategies = new HashMap<>();
        strategies.put(ReasoningMethod.CHAIN_OF_THOUGHT, new ChainOfThoughtStrategy(llmClient));
        strategies.put(ReasoningMethod.CHAIN_OF_TABLE, new ChainOfTableStrategy(llmClient));
        strategies.put(ReasoningMethod.CHAIN_OF_IDEAS, new ChainOfIdeasStrategy(llmClient));
        return strategies;
    }

    public <T> LLMResponse<T> reason(ReasoningMethod reasoningMethod, ResearchContext context, Class<T> outputType) throws LLMClientException {
        ReasoningStrategy strategy = strategies.get(reasoningMethod);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for " + reasoningMethod);
        }
        return strategy.reason(context, outputType);
    }

    public <T> CompletableFuture<LLMResponse<T>> reasonAsync(ReasoningMethod method, ResearchContext context, Class<T> outputType) {
        ReasoningStrategy strategy = strategies.get(method);
        if (strategy == null) {
            throw new IllegalArgumentException("No strategy found for " + method);
        }
        return strategy.reasonAsync(context, outputType);
    }

    public <T> CompletableFuture<LLMResponse<T>> reasonWithMultipleStrategies(List<ReasoningMethod> methods, ResearchContext context, Class<T> outputType) {
        List<CompletableFuture<LLMResponse<T>>> futures = methods.stream()
            .map(method -> reasonAsync(method, context, outputType))
            .toList();

        return CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
            .thenApply(v -> {
                List<LLMResponse<T>> results = futures.stream()
                    .map(CompletableFuture::join)
                    .toList();

                try {
                    return combineResults(results, context, outputType);
                } catch (LLMClientException e) {
                    throw new RuntimeException(e);
                }
            });
    }

    public <T> CompletableFuture<LLMResponse<T>> reasonWithAutoStrategy(ResearchContext context, Class<T> outputType) {
        ReasoningMethod selectedMethod = selectOptimalStrategy(context);
        return reasonAsync(selectedMethod, context, outputType);
    }

    private ReasoningMethod selectOptimalStrategy(ResearchContext context) {
        String prompt = context.getConfig()
            .userPrompt()
            .toLowerCase();

        if (prompt.contains("how does") && (prompt.contains("work") || prompt.contains("implement"))) {
            return ReasoningMethod.CHAIN_OF_THOUGHT;
        }

        if (prompt.contains("cqrs") || prompt.contains("microservices") || prompt.contains("spring boot") || prompt.contains("implementation") ||
            prompt.contains("code") || prompt.contains("example")) {
            return ReasoningMethod.CHAIN_OF_THOUGHT;
        }

        if (prompt.contains("compare") || prompt.contains("versus") || prompt.contains("table") || prompt.contains("data")) {
            return ReasoningMethod.CHAIN_OF_TABLE;
        }

        if (prompt.contains("creative") || prompt.contains("brainstorm") || prompt.contains("idea")) {
            return ReasoningMethod.CHAIN_OF_IDEAS;
        }

        return ReasoningMethod.CHAIN_OF_THOUGHT;
    }

    private <T> LLMResponse<T> combineResults(List<LLMResponse<T>> results, ResearchContext context, Class<T> outputType) throws LLMClientException {
        StringBuilder combinedPrompt = new StringBuilder();

        combinedPrompt.append("""
            You are an expert technical research assistant specializing in software architecture and implementation.
            
            Your task is to synthesize multiple analyses into a unified, comprehensive response that includes:
            1. Clear technical explanations
            2. Complete working code examples
            3. Practical implementation guidance
            4. Best practices and considerations
            
            """);

        combinedPrompt.append("ORIGINAL QUESTION:\n")
            .append(context.getConfig()
                .userPrompt())
            .append("\n\n");

        combinedPrompt.append("MULTIPLE ANALYSIS RESULTS:\n");
        for (int i = 0; i < results.size(); i++) {
            combinedPrompt.append(String.format("Analysis %d:\n%s\n\n", i + 1, results.get(i)
                .rawText()));
        }

        combinedPrompt.append("""
            SYNTHESIS REQUIREMENTS:
            - Integrate the best insights from all analyses
            - Resolve any contradictions with clear reasoning
            - Include complete code examples from the most comprehensive analysis
            - Ensure technical accuracy and practical applicability
            - Structure the response for maximum clarity and usefulness
            
            Provide your comprehensive synthesis:
            """);

        return llmClient.complete(combinedPrompt.toString(), outputType);
    }

    public List<ReasoningMethod> getAvailableStrategies() {
        return new ArrayList<>(strategies.keySet());
    }

    public boolean supportsParallelProcessing(ReasoningMethod method) {
        ReasoningStrategy strategy = strategies.get(method);
        return strategy != null && strategy.supportsConcurrency();
    }

    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread()
                .interrupt();
        }
    }
}