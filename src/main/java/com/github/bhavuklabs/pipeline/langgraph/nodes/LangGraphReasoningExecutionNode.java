package com.github.bhavuklabs.pipeline.langgraph.nodes;

import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import org.bsc.langgraph4j.action.AsyncNodeAction;

import com.github.bhavuklabs.citation.CitationResult;
import com.github.bhavuklabs.core.enums.ReasoningMethod;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;
import com.github.bhavuklabs.pipeline.langgraph.LangGraphState;
import com.github.bhavuklabs.reasoning.context.ResearchContext;
import com.github.bhavuklabs.reasoning.engine.ReasoningEngine;

public class LangGraphReasoningExecutionNode implements AsyncNodeAction<LangGraphState> {

    private static final Logger logger = Logger.getLogger(LangGraphReasoningExecutionNode.class.getName());

    private final ReasoningEngine reasoningEngine;

    public LangGraphReasoningExecutionNode(ReasoningEngine reasoningEngine) {
        this.reasoningEngine = reasoningEngine;
    }

    @Override
    public CompletableFuture<Map<String, Object>> apply(LangGraphState state) {
        try {
            logger.info("Starting LangGraph4j reasoning execution for session: " + state.getSessionId());

            ReasoningMethod method = state.getSelectedReasoning();
            if (method == null) {
                method = ReasoningMethod.CHAIN_OF_THOUGHT;
            }

            ResearchContext researchContext = buildResearchContextAlternative(state);

            Object response = executeReasoningSync(method, researchContext, state);

            logger.info("Reasoning execution completed successfully");

            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.RESPONSE, response, LangGraphState.RESEARCH_CONTEXT, buildContextString(state)));

        } catch (Exception e) {
            logger.severe("Reasoning execution failed: " + e.getMessage());
            return CompletableFuture.supplyAsync(() -> Map.of(LangGraphState.ERROR, e));
        }
    }

    private ResearchContext buildResearchContextAlternative(LangGraphState state) {

        @SuppressWarnings("unchecked") Map<String, Object> metadata = state.getMetadata();
        ResearchPromptConfig promptConfig = null;

        if (metadata != null && metadata.containsKey("prompt_config")) {
            promptConfig = (ResearchPromptConfig) metadata.get("prompt_config");
        }

        if (promptConfig == null) {
            promptConfig = createBasicPromptConfig(state);
        }

        try {

            return new ResearchContext(promptConfig);
        } catch (Exception e) {

            try {
                return new ResearchContext(promptConfig);
            } catch (Exception e2) {
                logger.warning("Unable to create ResearchContext with standard constructors, using reflection");
                return createResearchContextViaReflection(promptConfig, state);
            }
        }
    }

    private ResearchContext createResearchContextViaReflection(ResearchPromptConfig config, LangGraphState state) {
        try {

            Class<?> contextClass = ResearchContext.class;

            try {
                var constructor = contextClass.getConstructor(ResearchPromptConfig.class);
                return (ResearchContext) constructor.newInstance(config);
            } catch (NoSuchMethodException e) {

                var constructor = contextClass.getConstructor();
                ResearchContext context = (ResearchContext) constructor.newInstance();

                try {
                    var configField = contextClass.getDeclaredField("config");
                    configField.setAccessible(true);
                    configField.set(context, config);
                } catch (Exception fieldException) {
                    logger.warning("Could not set config field: " + fieldException.getMessage());
                }

                return context;
            }
        } catch (Exception e) {
            logger.severe("Failed to create ResearchContext via reflection: " + e.getMessage());
            throw new RuntimeException("Cannot create ResearchContext", e);
        }
    }

    private Object executeReasoningSync(ReasoningMethod method, ResearchContext context, LangGraphState state) {
        try {
            CompletableFuture<LLMResponse<Object>> future = reasoningEngine.reasonAsync(method, context, determineOutputType(state));

            LLMResponse<Object> response = future.get();

            if (response.structuredOutput() != null) {
                return response.structuredOutput();
            } else {
                return response.rawText();
            }

        } catch (Exception e) {
            logger.warning("Reasoning execution error: " + e.getMessage());
            return "Error occurred during reasoning: " + e.getMessage();
        }
    }

    private ResearchPromptConfig createBasicPromptConfig(LangGraphState state) {
        return new ResearchPromptConfig(state.getQuery(),
            "You are a helpful research assistant. Provide a comprehensive response based on the given context and citations.", determineOutputType(state),
            com.github.bhavuklabs.core.enums.OutputFormat.MARKDOWN);
    }

    private String buildContextString(LangGraphState state) {
        StringBuilder context = new StringBuilder();

        context.append("Query: ")
            .append(state.getQuery())
            .append("\n\n");

        List<CitationResult> citations = state.getCitations();
        if (!citations.isEmpty()) {
            context.append("Research Sources:\n");
            for (int i = 0; i < citations.size(); i++) {
                CitationResult citation = citations.get(i);
                context.append(String.format("[%d] %s\n", i + 1, citation.getTitle()));
                if (citation.getContent() != null && !citation.getContent()
                    .trim()
                    .isEmpty()) {
                    context.append(citation.getContent()
                        .substring(0, Math.min(500, citation.getContent()
                            .length())));
                    context.append("...\n");
                }
                context.append("\n");
            }
        }

        return context.toString();
    }

    @SuppressWarnings("unchecked")
    private Class<Object> determineOutputType(LangGraphState state) {
        return (Class<Object>) Object.class;
    }
}