package io.github.venkat1701;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.github.venkat1701.agent.ResearchResult;
import io.github.venkat1701.agent.ResearchSession;
import io.github.venkat1701.citation.config.CitationConfig;
import io.github.venkat1701.citation.enums.CitationSource;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.config.Research4jConfig;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.ModelType;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.exceptions.citation.CitationException;
import io.github.venkat1701.exceptions.client.LLMClientException;
import io.github.venkat1701.exceptions.config.ConfigurationException;
import io.github.venkat1701.model.client.GeminiAiClient;
import io.github.venkat1701.model.client.OpenAiClient;
import io.github.venkat1701.pipeline.DynamicResearchAgent;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class Research4j implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Research4j.class.getName());

    private final Research4jConfig config;
    private final DynamicResearchAgent agent;
    private final LLMClient llmClient;
    private final CitationService citationService;
    private final ReasoningEngine reasoningEngine;

    private Research4j(Builder builder) throws ConfigurationException {
        try {
            this.config = builder.configBuilder.build();

            this.llmClient = createLLMClient();
            this.citationService = createCitationService();
            this.reasoningEngine = new ReasoningEngine(llmClient);
            this.agent = new DynamicResearchAgent(citationService, reasoningEngine, llmClient);

            logger.info("Research4j initialized successfully");

        } catch (Exception e) {
            throw new ConfigurationException("Failed to initialize Research4j: " + e.getMessage(), e);
        }
    }

    private LLMClient createLLMClient() throws ConfigurationException, LLMClientException {
        if (config.hasApiKey(ModelType.GEMINI)) {
            logger.info("Using Gemini AI client");
            return new GeminiAiClient(config);
        } else if (config.hasApiKey(ModelType.OPENAI)) {
            logger.info("Using OpenAI client");
            return new OpenAiClient(config);
        } else {
            throw new ConfigurationException("No LLM provider configured. Set either GEMINI_API_KEY or OPENAI_API_KEY");
        }
    }

    private CitationService createCitationService() throws ConfigurationException, CitationException {
        CitationSource source = config.getDefaultCitationSource();
        CitationConfig citationConfig;

        switch (source) {
            case GOOGLE_GEMINI -> {
                if (!config.hasApiKey(CitationSource.GOOGLE_GEMINI)) {
                    throw new ConfigurationException("Google Search API key required for Google citation source");
                }
                if (config.getGoogleCseId() == null) {
                    throw new ConfigurationException("Google CSE ID required for Google citation source");
                }
                citationConfig = new CitationConfig(source, config.getGoogleSearchApiKey());
                return new CitationService(citationConfig, config.getGoogleCseId());
            }
            case TAVILY -> {
                if (!config.hasApiKey(CitationSource.TAVILY)) {
                    throw new ConfigurationException("Tavily API key required for Tavily citation source");
                }
                citationConfig = new CitationConfig(source, config.getTavilyApiKey());
                return new CitationService(citationConfig);
            }
            default -> throw new ConfigurationException("Unsupported citation source: " + source);
        }
    }

    public ResearchResult research(String query) {
        return research(query, createDefaultUserProfile());
    }

    public ResearchResult research(String query, UserProfile userProfile) {
        return research(query, userProfile, config.getDefaultOutputFormat());
    }

    public ResearchResult research(String query, OutputFormat outputFormat) {
        return research(query, createDefaultUserProfile(), outputFormat);
    }

    public ResearchResult research(String query, UserProfile userProfile, OutputFormat outputFormat) {
        try {
            var promptConfig = new io.github.venkat1701.core.payloads.ResearchPromptConfig(query,
                "You are a helpful research assistant. Provide comprehensive, accurate, and well-sourced answers.", String.class, outputFormat);

            var result = agent.processQuery(generateSessionId(), query, userProfile, promptConfig)
                .get();

            return new ResearchResult(result, config);

        } catch (Exception e) {
            logger.severe("Research failed: " + e.getMessage());
            throw new RuntimeException("Research failed: " + e.getMessage(), e);
        }
    }

    public ResearchSession createSession() {
        return new ResearchSession(this, generateSessionId());
    }

    public ResearchSession createSession(UserProfile userProfile) {
        return new ResearchSession(this, generateSessionId(), userProfile);
    }

    private UserProfile createDefaultUserProfile() {
        return new UserProfile("default-user", "general", "intermediate", List.of("balanced"), Map.of(), List.of(), config.getDefaultOutputFormat());
    }

    private String generateSessionId() {
        return "session-" + System.currentTimeMillis();
    }

    public Research4jConfig getConfig() {
        return config;
    }

    public boolean isHealthy() {
        try {
            return llmClient instanceof GeminiAiClient ? ((GeminiAiClient) llmClient).isHealthy() :
                llmClient instanceof OpenAiClient ? ((OpenAiClient) llmClient).isHealthy() : false;
        } catch (Exception e) {
            logger.warning("Health check failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        try {
            if (agent != null) {
                agent.shutdown();
            }
            if (llmClient instanceof AutoCloseable) {
                ((AutoCloseable) llmClient).close();
            }
            logger.info("Research4j closed successfully");
        } catch (Exception e) {
            logger.warning("Error during shutdown: " + e.getMessage());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Research4jConfig.Builder configBuilder = Research4jConfig.builder();

        public Builder withOpenAI(String apiKey) {
            configBuilder.openAiApiKey(apiKey);
            return this;
        }

        public Builder withGemini(String apiKey) {
            configBuilder.geminiApiKey(apiKey);
            return this;
        }

        public Builder withGemini(String apiKey, String modelName) {
            configBuilder.geminiApiKey(apiKey)
                .defaultModel(modelName);
            return this;
        }

        public Builder withOpenAI(String apiKey, String modelName) {
            configBuilder.openAiApiKey(apiKey)
                .defaultModel(modelName);
            return this;
        }

        public Builder withTavily(String apiKey) {
            configBuilder.tavilyApiKey(apiKey)
                .defaultCitationSource(CitationSource.TAVILY);
            return this;
        }

        public Builder withGoogleSearch(String searchApiKey, String cseId) {
            configBuilder.googleSearchApiKey(searchApiKey)
                .googleCseId(cseId)
                .defaultCitationSource(CitationSource.GOOGLE_GEMINI);
            return this;
        }

        public Builder defaultReasoning(ReasoningMethod method) {
            configBuilder.defaultReasoningMethod(method);
            return this;
        }

        public Builder defaultOutputFormat(OutputFormat format) {
            configBuilder.defaultOutputFormat(format);
            return this;
        }

        public Builder timeout(Duration timeout) {
            configBuilder.requestTimeout(timeout);
            return this;
        }

        public Builder maxCitations(int maxCitations) {
            configBuilder.maxCitations(maxCitations);
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            configBuilder.maxRetries(maxRetries);
            return this;
        }

        public Builder enableDebug() {
            configBuilder.debugEnabled(true);
            return this;
        }

        public Builder disableCache() {
            configBuilder.cacheEnabled(false);
            return this;
        }

        public Builder config(String key, Object value) {
            configBuilder.property(key, value);
            return this;
        }

        public Research4j build() throws ConfigurationException {
            return new Research4j(this);
        }
    }

    public static Research4j createDefault() throws ConfigurationException {
        return builder().build();
    }

    public static Research4j createWithGemini(String geminiKey, String searchKey, String cseId) throws ConfigurationException {
        return builder().withGemini(geminiKey)
            .withGoogleSearch(searchKey, cseId)
            .build();
    }

    public static Research4j createWithOpenAI(String openaiKey, String tavilyKey) throws ConfigurationException {
        return builder().withOpenAI(openaiKey)
            .withTavily(tavilyKey)
            .build();
    }

    public static Research4j createForAcademicResearch(String geminiKey, String searchKey, String cseId) throws ConfigurationException {
        return builder().withGemini(geminiKey, "gemini-1.5-flash")
            .withGoogleSearch(searchKey, cseId)
            .defaultReasoning(ReasoningMethod.CHAIN_OF_THOUGHT)
            .maxCitations(15)
            .timeout(Duration.ofMinutes(2))
            .build();
    }

    public static Research4j createForBusinessAnalysis(String openaiKey, String tavilyKey) throws ConfigurationException {
        return builder().withOpenAI(openaiKey, "gpt-4")
            .withTavily(tavilyKey)
            .defaultReasoning(ReasoningMethod.CHAIN_OF_TABLE)
            .defaultOutputFormat(OutputFormat.TABLE)
            .maxCitations(12)
            .build();
    }
}

