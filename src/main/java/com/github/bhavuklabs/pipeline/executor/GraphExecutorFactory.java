package com.github.bhavuklabs.pipeline.executor;

import java.util.logging.Logger;

import com.github.bhavuklabs.citation.service.CitationService;
import com.github.bhavuklabs.config.Research4jConfig;
import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.enums.GraphEngineType;
import com.github.bhavuklabs.pipeline.DynamicResearchAgent;
import com.github.bhavuklabs.pipeline.executor.impl.LangGraphExecutor;
import com.github.bhavuklabs.pipeline.executor.impl.LegacyGraphExecutor;
import com.github.bhavuklabs.reasoning.engine.ReasoningEngine;

public class GraphExecutorFactory {

    private static final Logger logger = Logger.getLogger(GraphExecutorFactory.class.getName());

    public static GraphExecutor create(Research4jConfig config, CitationService citationService, ReasoningEngine reasoningEngine, LLMClient llmClient) {

        GraphEngineType engineType = config.getGraphEngine();
        logger.info("Creating graph executor of type: " + engineType);

        return switch (engineType) {
            case LEGACY_CUSTOM -> createLegacyExecutor(citationService, reasoningEngine, llmClient);
            case LANGGRAPH4J -> createLangGraphExecutor(config, citationService, reasoningEngine, llmClient);
        };
    }

    private static GraphExecutor createLegacyExecutor(CitationService citationService, ReasoningEngine reasoningEngine, LLMClient llmClient) {

        logger.info("Initializing legacy DynamicResearchAgent");
        DynamicResearchAgent dynamicAgent = new DynamicResearchAgent(citationService, reasoningEngine, llmClient);

        return new LegacyGraphExecutor(dynamicAgent);
    }

    private static GraphExecutor createLangGraphExecutor(Research4jConfig config, CitationService citationService, ReasoningEngine reasoningEngine,
        LLMClient llmClient) {

        try {
            logger.info("Initializing LangGraph4j executor");
            return new LangGraphExecutor(config, citationService, reasoningEngine, llmClient);

        } catch (Exception e) {
            logger.severe("Failed to create LangGraph4j executor, falling back to legacy: " + e.getMessage());

            return createLegacyExecutor(citationService, reasoningEngine, llmClient);
        }
    }
}