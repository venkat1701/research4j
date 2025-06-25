package io.github.venkat1701.model.client;

import java.time.Duration;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.ObjectMapper;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import io.github.venkat1701.config.Research4jConfig;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.exceptions.client.LLMClientException;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.model.parser.LLMExtractor;

public class GeminiAiClient implements LLMClient, AutoCloseable {

    private static final Logger logger = Logger.getLogger(GeminiAiClient.class.getName());
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private final ChatModel chatModel;
    private final LLMExtractor extractor;
    private final String modelName;
    private final Duration timeout;

    public GeminiAiClient(ModelApiConfig config) throws LLMClientException {
        this(config, Duration.ofSeconds(30));
    }

    public GeminiAiClient(ModelApiConfig config, Duration timeout) throws LLMClientException {
        validateConfig(config);

        this.modelName = config.getModelName();
        this.timeout = timeout;

        try {
            this.chatModel = GoogleAiGeminiChatModel.builder()
                .modelName(config.getModelName())
                .apiKey(config.getApiKey())
                .timeout(timeout)
                .build();

            this.extractor = AiServices.create(LLMExtractor.class, chatModel);

            logger.info("GeminiAiClient initialized with model: " + modelName);

        } catch (Exception e) {
            throw new LLMClientException("Failed to initialize Gemini AI client: " + e.getMessage(), e, "GEMINI", "initialization");
        }
    }

    public GeminiAiClient(Research4jConfig config) throws LLMClientException {
        this(new ModelApiConfig(config.getGeminiApiKey(), null, config.getDefaultModel()), config.getRequestTimeout());
    }

    private void validateConfig(ModelApiConfig config) {
        if (config == null) {
            throw new IllegalArgumentException("ModelApiConfig cannot be null");
        }
        if (config.getApiKey() == null || config.getApiKey()
            .trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Gemini API key cannot be null or empty");
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
            logger.fine("Processing prompt with Gemini model: " + modelName);

            String rawResponse = generateResponse(prompt, type);
            T structuredOutput = parseResponse(rawResponse, type);

            return new LLMResponse<>(rawResponse, structuredOutput);

        } catch (Exception e) {
            String errorMsg = String.format("Failed to complete prompt with Gemini model %s: %s", modelName, e.getMessage());
            logger.severe(errorMsg);
            throw new LLMClientException(errorMsg, e, "GEMINI", "completion");
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
            throw new LLMClientException("Failed to generate response: " + e.getMessage(), e, "GEMINI", "generation");
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
            logger.warning("Failed to parse as JSON, returning raw response: " + e.getMessage());

            if (type == String.class) {
                return (T) rawResponse;
            }

            try {
                return (T) rawResponse;
            } catch (ClassCastException cce) {
                throw new LLMClientException(String.format("Cannot parse response to type %s: %s", type.getSimpleName(), e.getMessage()), e, "GEMINI",
                    "parsing");
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
            cleaned = "{\"text\": \"" + cleaned.replace("\"", "\\\"") + "\"}";
        }

        return cleaned;
    }

    public String getModelName() {
        return modelName;
    }

    public Duration getTimeout() {
        return timeout;
    }

    @Override
    public void close() {
        try {
            logger.info("GeminiAiClient closed");
        } catch (Exception e) {
            logger.warning("Error during GeminiAiClient cleanup: " + e.getMessage());
        }
    }

    public boolean isHealthy() {
        try {
            LLMResponse<String> response = complete("Hello", String.class);
            return response != null && response.rawText() != null;
        } catch (Exception e) {
            logger.warning("Health check failed for Gemini client: " + e.getMessage());
            return false;
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private String apiKey;
        private String modelName = "gemini-1.5-flash";
        private Duration timeout = Duration.ofSeconds(30);

        public Builder apiKey(String apiKey) {
            this.apiKey = apiKey;
            return this;
        }

        public Builder modelName(String modelName) {
            this.modelName = modelName;
            return this;
        }

        public Builder timeout(Duration timeout) {
            this.timeout = timeout;
            return this;
        }

        public GeminiAiClient build() throws LLMClientException {
            ModelApiConfig config = new ModelApiConfig(apiKey, null, modelName);
            return new GeminiAiClient(config, timeout);
        }
    }
}