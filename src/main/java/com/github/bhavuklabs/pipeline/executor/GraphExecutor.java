package com.github.bhavuklabs.pipeline.executor;

import java.util.concurrent.CompletableFuture;

import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;
import com.github.bhavuklabs.pipeline.profile.UserProfile;
import com.github.bhavuklabs.pipeline.state.ResearchAgentState;

public interface GraphExecutor {

    CompletableFuture<ResearchAgentState> processQuery(String sessionId, String query, UserProfile userProfile, ResearchPromptConfig config);

    boolean isHealthy();

    void shutdown();

    String getExecutorType();
}