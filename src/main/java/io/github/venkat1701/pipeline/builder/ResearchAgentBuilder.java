package io.github.venkat1701.pipeline.builder;

import java.util.Objects;

import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.pipeline.DynamicResearchAgent;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class ResearchAgentBuilder {

    private CitationService citationService;
    private ReasoningEngine reasoningEngine;
    private LLMClient llmClient;

    public ResearchAgentBuilder withCitationService(CitationService service) {
        this.citationService = service;
        return this;
    }

    public ResearchAgentBuilder withReasoningEngine(ReasoningEngine engine) {
        this.reasoningEngine = engine;
        return this;
    }

    public ResearchAgentBuilder withLLMClient(LLMClient client) {
        this.llmClient = client;
        return this;
    }

    public DynamicResearchAgent build() {
        Objects.requireNonNull(citationService, "CitationService is required");
        Objects.requireNonNull(reasoningEngine, "ReasoningEngine is required");
        Objects.requireNonNull(llmClient, "LLMClient is required");

        return new DynamicResearchAgent(citationService, reasoningEngine, llmClient);
    }
}
