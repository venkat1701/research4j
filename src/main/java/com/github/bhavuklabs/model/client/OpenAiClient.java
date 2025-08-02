package com.github.bhavuklabs.model.client;

import java.time.Duration;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import com.github.bhavuklabs.config.Research4jConfig;
import com.github.bhavuklabs.core.contracts.LLMClient;
import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.exceptions.client.LLMClientException;
import com.github.bhavuklabs.model.config.ModelApiConfig;
import com.github.bhavuklabs.model.parser.LLMExtractor;

public class OpenAiClient implements LLMClient, AutoCloseable {

    private static final Logger logger = Logger.getLogger(OpenAiClient.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatModel chatModel;
    private final LLMExtractor extractor;
    private final String modelName;
    private final Duration timeout;
    private final String baseUrl;

    public OpenAiClient(ModelApiConfig config) throws LLMClientException {
        this(config, Duration.ofSeconds(30));
    }

    public OpenAiClient(ModelApiConfig config, Duration timeout) throws LLMClientException {
        validateConfig(config);

        this.modelName = config.getModelName();
        this.baseUrl = config.getBaseUrl();
        this.timeout = timeout;

        try {
            OpenAiChatModel.OpenAiChatModelBuilder builder = OpenAiChatModel.builder()
                .apiKey(config.getApiKey())
                .modelName(config.getModelName())
                .timeout(timeout);

            if (config.getBaseUrl() != null && !config.getBaseUrl()
                .trim()
                .isEmpty()) {
                builder.baseUrl(config.getBaseUrl());
            }

            this.chatModel = builder.build();
            this.extractor = AiServices.create(LLMExtractor.class, chatModel);

            logger.info("OpenAiClient initialized with model: " + modelName);

        } catch (Exception e) {
            throw new LLMClientException("Failed to initialize OpenAI client: " + e.getMessage(), e, "OPENAI", "initialization");
        }
    }

    public OpenAiClient(Research4jConfig config) throws LLMClientException {
        this(new ModelApiConfig(config.getOpenAiApiKey(), null, config.getDefaultModel()), config.getRequestTimeout());
    }

    private void validateConfig(ModelApiConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ModelApiConfig cannot be null");
        }
        if (config.getApiKey() == null || config.getApiKey()
            .trim()
            .isEmpty()) {
            throw new IllegalArgumentException("OpenAI API key cannot be null or empty");
        }
        if (config.getModelName() == null || config.getModelName()
            .trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
    }

    @Override
    public <T> LLMResponse<T> complete(String prompt, Class<T> type) throws LLMClientException {
        if (prompt == null || prompt.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Prompt cannot be null or empty");
        }
        if (type == null) {
            throw new IllegalArgumentException("Type cannot be null");
        }

        try {
            logger.fine("Processing prompt with OpenAI model: " + modelName);

            String rawResponse = generateResponse(prompt, type);
            T structuredOutput = parseResponse(rawResponse, type);

            return new LLMResponse<>(rawResponse, structuredOutput);

        } catch (Exception e) {
            String errorMsg = String.format("Failed to complete prompt with OpenAI model %s: %s", modelName, e.getMessage());
            logger.severe(errorMsg);
            throw new LLMClientException(errorMsg, e, "OPENAI", "completion");
        }
    }

    private String generateResponse(String prompt, Class<?> type) throws LLMClientException {
        try {
            if (type == String.class) {
                return extractor.analyze(prompt);
            } else {
                String jsonPrompt = enhancePromptForJson(prompt, type);
                return extractor.extractJson(jsonPrompt);
            }
        } catch (Exception e) {
            throw new LLMClientException("Failed to generate response: " + e.getMessage(), e, "OPENAI", "generation");
        }
    }

    private String enhancePromptForJson(String originalPrompt, Class<?> type) {
        StringBuilder enhancedPrompt = new StringBuilder();
        enhancedPrompt.append(originalPrompt);
        enhancedPrompt.append("\n\nPlease respond with valid JSON format only. ");
        enhancedPrompt.append("The response should be parseable as ")
            .append(type.getSimpleName())
            .append(".");
        enhancedPrompt.append(" Do not include any text outside the JSON structure.");
        enhancedPrompt.append(" Ensure the JSON is well-formed and complete.");
        return enhancedPrompt.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResponse(String rawResponse, Class<T> type) throws LLMClientException {
        if (type == String.class) {
            return (T) rawResponse;
        }

        try {
            String cleanedResponse = cleanJsonResponse(rawResponse);
            return objectMapper.readValue(cleanedResponse, type);

        } catch (Exception e) {
            logger.warning("Failed to parse as JSON, attempting fallback: " + e.getMessage());

            if (type == String.class) {
                return (T) rawResponse;
            }

            try {
                String fallbackJson = String.format("{\"text\": \"%s\"}", rawResponse.replace("\"", "\\\"")
                    .replace("\n", "\\n"));
                return objectMapper.readValue(fallbackJson, type);
            } catch (Exception fallbackException) {
                throw new LLMClientException(String.format("Cannot parse response to type %s. Original error: %s", type.getSimpleName(), e.getMessage()), e,
                    "OPENAI", "parsing");
            }
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null) {
            return "{}";
        }

        String cleaned = response.trim();
        if (cleaned.startsWith("```json")) {
            cleaned = cleaned.substring(7);
        } else if (cleaned.startsWith("```")) {
            cleaned = cleaned.substring(3);
        }

        if (cleaned.endsWith("```")) {
            cleaned = cleaned.substring(0, cleaned.length() - 3);
        }

        cleaned = cleaned.trim();

        if (!cleaned.startsWith("{") && !cleaned.startsWith("[")) {
            cleaned = "{\"content\": \"" + cleaned.replace("\"", "\\\"") + "\"}";
        }

        return cleaned;
    }

    public String getModelName() {
        return modelName;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public void close() {
        try {
            logger.info("OpenAiClient closed");
        } catch (Exception e) {
            logger.warning("Error during OpenAiClient cleanup: " + e.getMessage());
        }
    }

    public boolean isHealthy() {
        try {
            LLMResponse<String> response = complete("Hello", String.class);
            return response != null && response.rawText() != null;
        } catch (Exception e) {
            logger.warning("Health check failed for OpenAI client: " + e.getMessage());
            return false;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String apiKey;
        private String modelName = "gpt-4";
        private String baseUrl;
        private Duration timeout = Duration.ofSeconds(30);

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder baseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public OpenAiClient build() throws LLMClientException {
            ModelApiConfig config = new ModelApiConfig(apiKey, baseUrl, modelName);
            return new OpenAiClient(config, timeout);
        }
    }
}