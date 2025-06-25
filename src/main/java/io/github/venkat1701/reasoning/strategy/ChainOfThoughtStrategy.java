package io.github.venkat1701.reasoning.strategy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.exceptions.client.LLMClientException;
import io.github.venkat1701.reasoning.ReasoningStrategy;
import io.github.venkat1701.reasoning.context.ResearchContext;

public class ChainOfThoughtStrategy implements ReasoningStrategy {

    private final LLMClient llmClient;
    private final ExecutorService executor;

    public ChainOfThoughtStrategy(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public <T> LLMResponse<T> reason(ResearchContext context, Class<T> outputType) throws LLMClientException {
        String cotPrompt = this.buildChainOfThoughtPrompt(context);
        context.setFinalPrompt(cotPrompt);
        return llmClient.complete(cotPrompt, outputType);
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

    @Override
    public String getMethodName() {
        return "CHAIN_OF_THOUGHT";
    }

    @Override
    public boolean supportsConcurrency() {
        return true;
    }

    private String buildChainOfThoughtPrompt(ResearchContext context) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("Let's think step by step to the answer the following questions: \n\n");
        prompt.append("Question: ").append(context.getConfig().userPrompt()).append("\n\n");

        if(!context.getCitations().isEmpty()) {
            prompt.append("Available information: \n\n");
            for(int i=0; i<context.getCitations().size(); i++) {
                CitationResult citation = context.getCitations().get(i);
                prompt.append(
                    String.format("[%d] %s\nContent: %s\nSource: %s\n\n",i+1, citation.getTitle(), citation.getContent(), citation.getUrl())
                );
            }
        }

        prompt.append("Please follow these steps:\n");
        prompt.append("1. First, analyze the question and identify key components\n");
        prompt.append("2. Then, examine the available information sources\n");
        prompt.append("3. Connect relevant information to answer parts of the question\n");
        prompt.append("4. Synthesize your findings into a comprehensive answer\n");
        prompt.append("5. Conclude with your final answer\n\n");

        if(context.getConfig().systemInstruction() != null) {
            prompt.append("System Instructions: ")
                .append(context.getConfig().systemInstruction())
                .append("\n\n");
        }

        return prompt.toString();
    }
}
