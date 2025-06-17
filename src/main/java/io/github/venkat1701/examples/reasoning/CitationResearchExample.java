package io.github.venkat1701.examples.reasoning;

import java.util.List;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.config.CitationConfig;
import io.github.venkat1701.citation.enums.CitationSource;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.core.payloads.ResearchPromptConfig;
import io.github.venkat1701.model.client.GeminiAiClient;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.reasoning.context.ResearchContext;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class CitationResearchExample {

    private static final String GEMINI_API_KEY = "API_KEY";
    private static final String GOOGLE_CSE_ID = "CSE_ID";
    private static final String GOOGLE_SEARCH_API_KEY = "SEARCH_API_KEY";

    public static void main(String[] args) {
        try {
            CitationResearchExample example = new CitationResearchExample();
            System.out.println("=== Synchronous Citation Research Example ===");
            example.runSynchronousExample();

            System.out.println("\n"+"=".repeat(60)+"\n");

        } catch(Exception e) {
            System.err.println("Error Running Example: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public void runSynchronousExample() {
        System.out.println("1. Setting up Citation Service");
        CitationConfig config = new CitationConfig(CitationSource.GOOGLE_GEMINI, GOOGLE_SEARCH_API_KEY);
        CitationService service = new CitationService(config, GOOGLE_CSE_ID);

        System.out.println("2. Setting up Gemini AI Client");
        ModelApiConfig modelApiConfig = new ModelApiConfig(
            GEMINI_API_KEY,
            null,
            "gemini-1.5-flash"
        );

        LLMClient llmClient = new GeminiAiClient(modelApiConfig);

        System.out.println("3. Setting up Reasoning Engine");
        ReasoningEngine engine = new ReasoningEngine(llmClient);

        String researchQuestion = "what is postgres";
        System.out.println("4. Research Question: " + researchQuestion);

        System.out.println("5. Fetching citations...");
        List<CitationResult> citations = service.search(researchQuestion);
        System.out.println("   Found " + citations.size() + " citations:");

        for (int i = 0; i < Math.min(citations.size(), 3); i++) {
            CitationResult citation = citations.get(i);
            System.out.println("   [" + (i + 1) + "] " + citation.getTitle());
            System.out.println("       URL: " + citation.getUrl());
            System.out.println("       Snippet: " + citation.getSnippet());
            System.out.println();
        }

        System.out.println("6. Creating Research Context.");
        ResearchPromptConfig promptConfig = new ResearchPromptConfig(
            researchQuestion,
            "You are a research assistant providing comprehensive and accurate information based on the provided sources. Please cite your sources appropriately.",
            String.class,
            OutputFormat.MARKDOWN
        );

        ResearchContext context = new ResearchContext(promptConfig);
        context.setCitations(citations);
        context.setReasoningMethod(ReasoningMethod.CHAIN_OF_IDEAS);
        context.setStartTime(System.currentTimeMillis());

        System.out.println("7. Applying Chain of Table reasoning...");
        LLMResponse<String> result = engine.reason(
            ReasoningMethod.CHAIN_OF_IDEAS,
            context,
            String.class
        );

        context.setEndTime(System.currentTimeMillis());

        System.out.println("8. Research Results:");
        System.out.println("   Processing time: " + (context.getEndTime() - context.getStartTime()) + "ms");
        System.out.println("   Final Answer:");
        System.out.println("   " + "─".repeat(50));
        System.out.println(result.rawText());
        System.out.println("   " + "─".repeat(50));

        // Cleanup
        engine.shutdown();

    }
}
