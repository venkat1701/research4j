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

public class ChainOfTableStrategy implements ReasoningStrategy {

    private final LLMClient llmClient;
    private final ExecutorService executor;

    public ChainOfTableStrategy(LLMClient llmClient) {
        this.llmClient = llmClient;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public <T> LLMResponse<T> reason(ResearchContext context, Class<T> outputType) throws LLMClientException {
        String tablePrompt = buildTablePrompt(context);
        context.setFinalPrompt(tablePrompt);
        return llmClient.complete(tablePrompt, outputType);
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

    private String buildTablePrompt(ResearchContext context) {
        StringBuilder tablePrompt = new StringBuilder();
        tablePrompt.append("Let's solve this systematically using a table-based approach:\n\n");
        tablePrompt.append("Question: ").append(context.getConfig().userPrompt()).append("\n\n");

        if(!context.getCitations().isEmpty()) {
            tablePrompt.append("Information Table:\n");
            tablePrompt.append("| Source | Key Information | Relevance | Reliability |\n");
            tablePrompt.append("| ------ | --------------- | --------- | ----------- |\n");

           for(CitationResult citation : context.getCitations()) {
               tablePrompt.append(String.format("| %s | %s | TBD | TBD |\n", citation.getTitle(), citation.getContent()));
           }
        }
        tablePrompt.append("\n");

        tablePrompt.append("Analysis Framework:\n");
        tablePrompt.append("| Step | Task | Output |\n");
        tablePrompt.append("|------|------|--------|\n");
        tablePrompt.append("| 1 | Identify key concepts | List concepts |\n");
        tablePrompt.append("| 2 | Map concepts to sources | Concept-source mapping |\n");
        tablePrompt.append("| 3 | Evaluate evidence strength | Evidence scores |\n");
        tablePrompt.append("| 4 | Synthesize findings | Integrated answer |\n");
        tablePrompt.append("| 5 | Validate conclusions | Final verification |\n\n");
        if(context.getConfig().systemInstruction() != null) {
            tablePrompt.append("\n\nSystem Instructions: ").append(context.getConfig().systemInstruction()).append("\n");
        }

        return tablePrompt.toString();
    }

    @Override
    public String getMethodName() {
        return "CHAIN_OF_TABLE";
    }

    @Override
    public boolean supportsConcurrency() {
        return true;
    }
}
