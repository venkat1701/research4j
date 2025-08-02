package com.github.bhavuklabs.pipeline.builder;

import java.util.Objects;

import com.github.bhavuklabs.citation.service.CitationService;
import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.pipeline.DynamicResearchAgent;
import com.github.bhavuklabs.reasoning.engine.ReasoningEngine;

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
