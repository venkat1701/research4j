package io.github.venkat1701.examples.pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.github.venkat1701.citation.config.CitationConfig;
import io.github.venkat1701.citation.enums.CitationSource;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.payloads.ResearchPromptConfig;
import io.github.venkat1701.model.client.GeminiAiClient;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.pipeline.DynamicResearchAgent;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.pipeline.state.ResearchAgentState;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class PipelineExample {
    private static final String GEMINI_API_KEY = "API_KEY";
    private static final String GOOGLE_CSE_ID = "CSE_ID";
    private static final String GOOGLE_SEARCH_API_KEY = "SEARCH_API_KEY";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        ModelApiConfig modelApiConfig = new ModelApiConfig(
            GEMINI_API_KEY,
            null,
            "gemini-1.5-flash"
        );

        CitationConfig citationConfig = new CitationConfig(CitationSource.GOOGLE_GEMINI, GOOGLE_SEARCH_API_KEY);
        CitationService citationService = new CitationService(citationConfig, GOOGLE_CSE_ID);
        LLMClient llmClient = new GeminiAiClient(modelApiConfig);
        ReasoningEngine reasoningEngine = new ReasoningEngine(llmClient);

        DynamicResearchAgent agent = new DynamicResearchAgent(
            citationService,
            reasoningEngine,
            llmClient
        );

        UserProfile profile = new UserProfile(
            "1",
            "cloud-computing",
            "intermediate",
            List.of("code-heavy", "balanced"),
            Map.of("load balancing", 7),
            List.of("what is horizontal scaling?", "CDN basics", "explain what is system design"),
            OutputFormat.MARKDOWN
        );

        String query = "CQRS Design Pattern";

        ResearchPromptConfig promptConfig = new ResearchPromptConfig(
            query,
            """
            You are an expert educator in modern distributed systems and large-scale application design.
            Your task is to generate an **educational, markdown-formatted, and source-grounded explanation** 
            for the query: "%s".
        
            ### Your Output Must Include:
            1. **Definition:** Provide a concise yet accurate definition.
            2. **Design Process:** Describe each phase clearly – from requirement gathering to maintenance.
            3. **Key Pillars:** Cover principles like scalability, availability, reliability, consistency, performance, and security.
            4. **Concrete Examples:** Mention real-world systems like Netflix, Uber, etc. and how they apply these principles.
            5. **Modern Trends:** Highlight current and emerging trends such as:
               - Microservices
               - Serverless
               - Cloud-native design
               - API-first systems
               - AI-augmented systems
            6. **Best Practices:** Incorporate architectural design patterns (e.g., CQRS, event sourcing), tradeoffs, and key dos/don'ts.
            7. **Final Summary:** End with a clean, digestible takeaway section for learners.
            8. **Final Summary Table**: End with a clean, markdown renderable summary table for learners.
        
            ### Additional Guidelines:
            - Use clear section headings (###).
            - Include bullet points and short code blocks where helpful.
            - All facts and examples should be grounded in the citations fetched via the citation engine.
            - Format everything in **clean Markdown**, suitable for rendering in a browser-based education app.
            - Avoid hallucinations; cite concrete, trustworthy sources where applicable.
        
            Target audience is an intermediate-level learner with basic computer science knowledge.
            """.formatted(query),
            String.class,
            OutputFormat.MARKDOWN
        );


        ResearchAgentState result = agent
            .processQuery("session-001", query, profile, promptConfig)
            .get();

        if (result.getError() != null) {
            System.err.println("Error occurred: " + result.getError().getMessage());
            result.getError().printStackTrace();
        } else {
            System.out.println("=== Research Output ===");
            System.out.println(result.getFinalResponse().structuredOutput());
            System.out.println("\n=== Metadata ===");
            result.getMetadata().forEach((k, v) -> System.out.println(k + ": " + v));
        }

        agent.shutdown();
    }
}