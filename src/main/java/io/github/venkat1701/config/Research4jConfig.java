package io.github.venkat1701.config;

import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import io.github.venkat1701.citation.enums.CitationSource;
import io.github.venkat1701.core.enums.ModelType;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.enums.ReasoningMethod;

public class Research4jConfig {

    private static final String ENV_PREFIX = "RESEARCH4J_";
    private static final String OPENAI_API_KEY = "OPENAI_API_KEY";
    private static final String GEMINI_API_KEY = "GEMINI_API_KEY";
    private static final String TAVILY_API_KEY = "TAVILY_API_KEY";
    private static final String GOOGLE_SEARCH_API_KEY = "GOOGLE_SEARCH_API_KEY";
    private static final String GOOGLE_CSE_ID = "GOOGLE_CSE_ID";

    private final Map<String, Object> properties;
    private final Map<String, String> apiKeys;

    private Research4jConfig(Builder builder) {
        this.properties = new ConcurrentHashMap<>(builder.properties);
        this.apiKeys = new ConcurrentHashMap<>(builder.apiKeys);

        loadFromEnvironment();
        validate();
    }

    private void loadFromEnvironment() {
        loadApiKeyFromEnv(OPENAI_API_KEY, ModelType.OPENAI.name());
        loadApiKeyFromEnv(GEMINI_API_KEY, ModelType.GEMINI.name());
        loadApiKeyFromEnv(TAVILY_API_KEY, CitationSource.TAVILY.name());
        loadApiKeyFromEnv(GOOGLE_SEARCH_API_KEY, CitationSource.GOOGLE_GEMINI.name());
        loadApiKeyFromEnv(GOOGLE_CSE_ID, "GOOGLE_CSE_ID");

        loadPropertyFromEnv("DEFAULT_MODEL", "defaultModel");
        loadPropertyFromEnv("DEFAULT_CITATION_SOURCE", "defaultCitationSource");
        loadPropertyFromEnv("DEFAULT_REASONING", "defaultReasoningMethod");
        loadPropertyFromEnv("REQUEST_TIMEOUT_SECONDS", "requestTimeout");
        loadPropertyFromEnv("MAX_CITATIONS", "maxCitations");
    }

    private void loadApiKeyFromEnv(String envVar, String key) {
        if (!apiKeys.containsKey(key)) {
            String value = System.getenv(envVar);
            if (value != null && !value.trim()
                .isEmpty()) {
                apiKeys.put(key, value.trim());
            }
        }
    }

    private void loadPropertyFromEnv(String envVar, String key) {
        if (!properties.containsKey(key)) {
            String value = System.getenv(ENV_PREFIX + envVar);
            if (value == null) {
                value = System.getProperty("research4j." + key);
            }
            if (value != null && !value.trim()
                .isEmpty()) {
                properties.put(key, value.trim());
            }
        }
    }

    private void validate() {
        boolean hasLLMProvider = apiKeys.containsKey(ModelType.OPENAI.name()) || apiKeys.containsKey(ModelType.GEMINI.name());

        if (!hasLLMProvider) {
            throw new IllegalStateException(
                "At least one LLM provider must be configured. " + "Set either OPENAI_API_KEY or GEMINI_API_KEY environment variable.");
        }

        if (properties.containsKey("requestTimeout")) {
            try {
                int timeout = Integer.parseInt(properties.get("requestTimeout")
                    .toString());
                if (timeout <= 0) {
                    throw new IllegalArgumentException("Request timeout must be positive");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid request timeout value");
            }
        }

        if (properties.containsKey("maxCitations")) {
            try {
                int maxCitations = Integer.parseInt(properties.get("maxCitations")
                    .toString());
                if (maxCitations <= 0 || maxCitations > 50) {
                    throw new IllegalArgumentException("Max citations must be between 1 and 50");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid max citations value");
            }
        }
    }

    public String getApiKey(String provider) {
        return apiKeys.get(provider);
    }

    public String getApiKey(ModelType modelType) {
        return getApiKey(modelType.name());
    }

    public String getApiKey(CitationSource citationSource) {
        return getApiKey(citationSource.name());
    }

    public String getOpenAiApiKey() {
        return getApiKey(ModelType.OPENAI);
    }

    public String getGeminiApiKey() {
        return getApiKey(ModelType.GEMINI);
    }

    public String getTavilyApiKey() {
        return getApiKey(CitationSource.TAVILY);
    }

    public String getGoogleSearchApiKey() {
        return getApiKey(CitationSource.GOOGLE_GEMINI);
    }

    public String getGoogleCseId() {
        return getApiKey("GOOGLE_CSE_ID");
    }

    public String getDefaultModel() {
        return (String) properties.getOrDefault("defaultModel", "gemini-1.5-flash");
    }

    public CitationSource getDefaultCitationSource() {
        String source = (String) properties.getOrDefault("defaultCitationSource", "GOOGLE_GEMINI");
        return CitationSource.valueOf(source);
    }

    public ReasoningMethod getDefaultReasoningMethod() {
        String method = (String) properties.getOrDefault("defaultReasoningMethod", "CHAIN_OF_THOUGHT");
        return ReasoningMethod.valueOf(method);
    }

    public OutputFormat getDefaultOutputFormat() {
        String format = (String) properties.getOrDefault("defaultOutputFormat", "MARKDOWN");
        return OutputFormat.valueOf(format);
    }

    public Duration getRequestTimeout() {
        int seconds = Integer.parseInt(properties.getOrDefault("requestTimeout", "30")
            .toString());
        return Duration.ofSeconds(seconds);
    }

    public int getMaxCitations() {
        return Integer.parseInt(properties.getOrDefault("maxCitations", "10")
            .toString());
    }

    public int getMaxRetries() {
        return Integer.parseInt(properties.getOrDefault("maxRetries", "3")
            .toString());
    }

    public boolean isDebugEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("debugEnabled", "false")
            .toString());
    }

    public boolean isCacheEnabled() {
        return Boolean.parseBoolean(properties.getOrDefault("cacheEnabled", "true")
            .toString());
    }

    public Object getProperty(String key) {
        return properties.get(key);
    }

    public String getProperty(String key, String defaultValue) {
        return (String) properties.getOrDefault(key, defaultValue);
    }

    public boolean hasApiKey(String provider) {
        return apiKeys.containsKey(provider) && apiKeys.get(provider) != null && !apiKeys.get(provider)
            .trim()
            .isEmpty();
    }

    public boolean hasApiKey(ModelType modelType) {
        return hasApiKey(modelType.name());
    }

    public boolean hasApiKey(CitationSource citationSource) {
        return hasApiKey(citationSource.name());
    }

    public Set<String> getAvailableProviders() {
        return Collections.unmodifiableSet(apiKeys.keySet());
    }

    public Map<String, Object> getAllProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Map<String, Object> properties = new HashMap<>();
        private final Map<String, String> apiKeys = new HashMap<>();

        public Builder openAiApiKey(String apiKey) {
            this.apiKeys.put(ModelType.OPENAI.name(), apiKey);
            return this;
        }

        public Builder geminiApiKey(String apiKey) {
            this.apiKeys.put(ModelType.GEMINI.name(), apiKey);
            return this;
        }

        public Builder tavilyApiKey(String apiKey) {
            this.apiKeys.put(CitationSource.TAVILY.name(), apiKey);
            return this;
        }

        public Builder googleSearchApiKey(String apiKey) {
            this.apiKeys.put(CitationSource.GOOGLE_GEMINI.name(), apiKey);
            return this;
        }

        public Builder googleCseId(String cseId) {
            this.apiKeys.put("GOOGLE_CSE_ID", cseId);
            return this;
        }

        public Builder apiKey(String provider, String apiKey) {
            this.apiKeys.put(provider, apiKey);
            return this;
        }

        public Builder defaultModel(String model) {
            this.properties.put("defaultModel", model);
            return this;
        }

        public Builder defaultCitationSource(CitationSource source) {
            this.properties.put("defaultCitationSource", source.name());
            return this;
        }

        public Builder defaultReasoningMethod(ReasoningMethod method) {
            this.properties.put("defaultReasoningMethod", method.name());
            return this;
        }

        public Builder defaultOutputFormat(OutputFormat format) {
            this.properties.put("defaultOutputFormat", format.name());
            return this;
        }

        public Builder requestTimeout(Duration timeout) {
            this.properties.put("requestTimeout", String.valueOf(timeout.getSeconds()));
            return this;
        }

        public Builder maxCitations(int maxCitations) {
            if (maxCitations <= 0 || maxCitations > 50) {
                throw new IllegalArgumentException("Max citations must be between 1 and 50");
            }
            this.properties.put("maxCitations", String.valueOf(maxCitations));
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            if (maxRetries < 0 || maxRetries > 10) {
                throw new IllegalArgumentException("Max retries must be between 0 and 10");
            }
            this.properties.put("maxRetries", String.valueOf(maxRetries));
            return this;
        }

        public Builder debugEnabled(boolean enabled) {
            this.properties.put("debugEnabled", String.valueOf(enabled));
            return this;
        }

        public Builder cacheEnabled(boolean enabled) {
            this.properties.put("cacheEnabled", String.valueOf(enabled));
            return this;
        }

        public Builder property(String key, Object value) {
            this.properties.put(key, value);
            return this;
        }

        public Research4jConfig build() {
            return new Research4jConfig(this);
        }
    }

    public static Research4jConfig createDefault() {
        return builder().build();
    }

    public static Research4jConfig createWithOpenAI(String apiKey) {
        return builder().openAiApiKey(apiKey)
            .defaultModel("gpt-4")
            .build();
    }

    public static Research4jConfig createWithGemini(String apiKey) {
        return builder().geminiApiKey(apiKey)
            .defaultModel("gemini-2.0-flash")
            .build();
    }

    public static Research4jConfig createWithGeminiAndGoogle(String geminiKey, String searchKey, String cseId) {
        return builder().geminiApiKey(geminiKey)
            .googleSearchApiKey(searchKey)
            .googleCseId(cseId)
            .defaultCitationSource(CitationSource.GOOGLE_GEMINI)
            .build();
    }
}