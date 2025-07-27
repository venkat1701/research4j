package io.github.venkat1701.model.client;

import java.time.Duration;
import java.util.logging.Logger;

import com.fasterxml.jackson.databind.JsonNode;
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
        if (config.getApiKey() == null || config.getApiKey().trim().isEmpty()) {
            throw new IllegalArgumentException("Gemini API key cannot be null or empty");
        }
        if (config.getModelName() == null || config.getModelName().trim().isEmpty()) {
            throw new IllegalArgumentException("Model name cannot be null or empty");
        }
    }

    @Override
    public <T> LLMResponse<T> complete(String prompt, Class<T> type) throws LLMClientException {
        if (prompt == null || prompt.trim().isEmpty()) {
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
        enhancedPrompt.append("\n\nIMPORTANT: Respond with valid JSON format only. ");
        enhancedPrompt.append("The response should be parseable as ").append(type.getSimpleName()).append(". ");
        enhancedPrompt.append("Do not include any text outside the JSON structure. ");
        enhancedPrompt.append("Ensure all JSON properties are properly quoted and escaped. ");
        enhancedPrompt.append("Do not use markdown code blocks or any formatting around the JSON.");
        return enhancedPrompt.toString();
    }

    @SuppressWarnings("unchecked")
    private <T> T parseResponse(String rawResponse, Class<T> type) throws LLMClientException {
        if (type == String.class) {
            return (T) cleanResponseText(rawResponse);
        }

        try {
            String cleanedResponse = cleanJsonResponse(rawResponse);

            
            JsonNode jsonNode = objectMapper.readTree(cleanedResponse);

            
            return objectMapper.treeToValue(jsonNode, type);

        } catch (Exception e) {
            logger.warning("Failed to parse as JSON, attempting recovery: " + e.getMessage());

            try {
                
                String extractedJson = extractJsonFromText(rawResponse);
                if (extractedJson != null) {
                    JsonNode jsonNode = objectMapper.readTree(extractedJson);
                    return objectMapper.treeToValue(jsonNode, type);
                }
            } catch (Exception ex) {
                logger.warning("JSON extraction also failed: " + ex.getMessage());
            }

            
            if (type == String.class) {
                return (T) cleanResponseText(rawResponse);
            }

            
            try {
                String fallbackJson = createFallbackJson(rawResponse, type);
                JsonNode jsonNode = objectMapper.readTree(fallbackJson);
                return objectMapper.treeToValue(jsonNode, type);
            } catch (Exception fallbackException) {
                throw new LLMClientException(
                    String.format("Cannot parse response to type %s. Original error: %s", type.getSimpleName(), e.getMessage()),
                    e, "GEMINI", "parsing");
            }
        }
    }

    private String cleanJsonResponse(String response) {
        if (response == null || response.trim().isEmpty()) {
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
            cleaned = "{\"text\": \"" + escapeJsonString(cleaned) + "\"}";
        }

        return cleaned;
    }

    private String extractJsonFromText(String text) {
        if (text == null) return null;

        
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        
        start = text.indexOf('[');
        end = text.lastIndexOf(']');

        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }

        return null;
    }

    private String createFallbackJson(String rawResponse, Class<?> type) {
        String cleanedText = cleanResponseText(rawResponse);

        
        if (type.getSimpleName().toLowerCase().contains("analysis")) {
            return String.format("""
                {
                    "intent": "research",
                    "complexityScore": 5,
                    "topics": ["general"],
                    "requiresCitations": true,
                    "estimatedTime": "2-3 minutes",
                    "suggestedReasoning": "CHAIN_OF_THOUGHT"
                }
                """);
        }

        
        return String.format("{\"content\": \"%s\"}", escapeJsonString(cleanedText));
    }

    private String cleanResponseText(String text) {
        if (text == null) return "No response generated";

        return text.replaceAll("\\s+", " ")
            .replaceAll("[\\r\\n]+", " ")
            .trim();
    }

    private String escapeJsonString(String text) {
        if (text == null) return "";

        return text.replace("\\", "\\\\")
            .replace("\"", "\\\"")
            .replace("\r", "\\r")
            .replace("\n", "\\n")
            .replace("\t", "\\t");
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