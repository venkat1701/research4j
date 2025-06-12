package io.github.venkat1701.model.config;

public class ModelApiConfig {
    private final String apiKey;
    private final String baseUrl;
    private final String modelName;

    public ModelApiConfig(String apiKey, String baseUrl, String modelName) {
        this.apiKey = apiKey;
        this.baseUrl = baseUrl;
        this.modelName = modelName;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public String getModelName() {
        return modelName;
    }
}
