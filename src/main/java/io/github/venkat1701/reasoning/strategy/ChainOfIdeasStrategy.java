package io.github.venkat1701.reasoning.strategy;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.exceptions.client.LLMClientException;
import io.github.venkat1701.reasoning.ReasoningStrategy;
import io.github.venkat1701.reasoning.context.ResearchContext;

public class ChainOfIdeasStrategy implements ReasoningStrategy {

    private final LLMClient llmClient;
    private final ExecutorService executor;

    public ChainOfIdeasStrategy(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public <T> LLMResponse<T> reason(ResearchContext context, Class<T> outputType) throws LLMClientException {
        // generating ideas in parallel processing
        List<CompletableFuture<String>> ideaFutures = generateIdeasAsync(context);
        List<String> ideas = ideaFutures
            .stream()
            .map(CompletableFuture::join)
            .toList();

        String synthesisPrompt = buildSynthesisPrompt(context, ideas);
        context.setFinalPrompt(synthesisPrompt);
        return llmClient.complete(synthesisPrompt, outputType);
    }

    @Override
    public <T> CompletableFuture<LLMResponse<T>> reasonAsync(ResearchContext context, Class<T> outputType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reason(context, outputType);
            } catch (LLMClientException e) {
                throw new RuntimeException(e);
            }
        }, executor);
    }

    private List<CompletableFuture<String>> generateIdeasAsync(ResearchContext context) {
        List<CompletableFuture<String>> futures = new ArrayList<>();

        String[] perspectives =  {
            "analytical", "creative", "practical", "critical", "innovative"
        };

        for (String perspective : perspectives) {
            CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
                String ideaPrompt = buildIdeaPrompt(context, perspective);
                LLMResponse<String> response = null;
                try {
                    response = llmClient.complete(ideaPrompt, String.class);
                } catch (LLMClientException e) {
                    throw new RuntimeException(e);
                }
                return response.structuredOutput();
            }, executor);
            futures.add(future);
        }

        return futures;
    }

    private String buildSynthesisPrompt(ResearchContext context, List<String> ideas) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Synthesize the following ideas to answer the question: \n\n");
        prompt.append("Question: ").append(context.getConfig().userPrompt()).append("\n\n");

        prompt.append("Generated Ideas: \n");
        for(int i=0; i<ideas.size(); i++) {
            prompt.append(String.format("Idea Set %d:\n%s\n\n", i+1, ideas.get(i)));
        }

        prompt.append("Create a comprehensive answer by combining the best elements from these ideas: ");
        return prompt.toString();
    }

    private String buildIdeaPrompt(ResearchContext context, String perspective) {
        StringBuilder ideaPrompt = new StringBuilder();
        ideaPrompt.append(String.format("From a %s perspective, generate ideas to address:\n", perspective));
        ideaPrompt.append("Question: ").append(context.getConfig().userPrompt()).append("\n\n");

        if(!context.getCitations().isEmpty()) {
            ideaPrompt.append("Reference Information:\n");
            context.getCitations().forEach(citation ->
                    ideaPrompt.append("- ").append(citation.getTitle()).append(": ")
                        .append(citation.getContent())
                        .append("\n")
                );
        }
        ideaPrompt.append("\nProvide 3-5 key ideas from this perspective:");
        return ideaPrompt.toString();
    }

    @Override
    public String getMethodName() {
        return "CHAIN_OF_IDEAS";
    }

    @Override
    public boolean supportsConcurrency() {
        return true;
    }
}
