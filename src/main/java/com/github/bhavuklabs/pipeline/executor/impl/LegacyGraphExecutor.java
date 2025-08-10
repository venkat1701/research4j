package com.github.bhavuklabs.pipeline.executor.impl;

import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;
import com.github.bhavuklabs.pipeline.DynamicResearchAgent;
import com.github.bhavuklabs.pipeline.executor.GraphExecutor;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;

public class LegacyGraphExecutor implements GraphExecutor {

    private static final Logger logger = Logger.getLogger(LegacyGraphExecutor.class.getName());

    private final DynamicResearchAgent dynamicAgent;

    public LegacyGraphExecutor(DynamicResearchAgent dynamicAgent) {
        this.dynamicAgent = dynamicAgent;
    }

    @Override
    public CompletableFuture<ResearchAgentState> processQuery(String sessionId, String query, UserProfile userProfile, ResearchPromptConfig config) {

        logger.info("Processing query using legacy graph executor");
        return dynamicAgent.processQuery(sessionId, query, userProfile, config);
    }

    @Override
    public boolean isHealthy() {

        return dynamicAgent != null;
    }

    @Override
    public void shutdown() {
        logger.info("Shutting down legacy graph executor");

    }

    @Override
    public String getExecutorType() {
        return "LEGACY_CUSTOM";
    }
}