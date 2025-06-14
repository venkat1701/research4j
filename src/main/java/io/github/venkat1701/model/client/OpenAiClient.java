package io.github.venkat1701.model.client;

import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.model.openai.OpenAiLanguageModel;
import dev.langchain4j.service.AiServices;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.model.parser.LLMExtractor;

public class OpenAiClient implements LLMClient {
    private final OpenAiChatModel model;

    public OpenAiClient(ModelApiConfig config) {
        this.model = OpenAiChatModel
            .builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .modelName(config.getModelName())
            .build();
    }

    @Override
    public <T> LLMResponse<T> complete(String prompt, Class<T> type) {
        LLMExtractor<T> extractor = AiServices.create(LLMExtractor.class, model);
        T result = extractor.extract(prompt, type);
        return new LLMResponse<>(result.toString(), result);
    }
}
