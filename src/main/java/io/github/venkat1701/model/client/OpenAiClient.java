package io.github.venkat1701.model.client;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.service.AiServices;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.model.parser.LLMExtractor;

public class OpenAiClient implements LLMClient {
    private final ChatModel chatModel;
    private final LLMExtractor extractor;

    public OpenAiClient(ModelApiConfig config) {
        this.chatModel = OpenAiChatModel.builder()
            .apiKey(config.getApiKey())
            .baseUrl(config.getBaseUrl())
            .modelName(config.getModelName())
            .build();
        this.extractor = AiServices.create(LLMExtractor.class, chatModel);
    }

    @Override
    public <T> LLMResponse<T> complete(String prompt, Class<T> type) {
        String rawResponse;

        if (type == String.class) {
            rawResponse = extractor.analyze(prompt);
        } else {
            rawResponse = extractor.extractJson(prompt);
        }

        if (type == String.class) {
            @SuppressWarnings("unchecked")
            T result = (T) rawResponse;
            return new LLMResponse<>(rawResponse, result);
        }

        try {
            @SuppressWarnings("unchecked")
            T result = (T) rawResponse;
            return new LLMResponse<>(rawResponse, result);
        } catch (ClassCastException e) {
            @SuppressWarnings("unchecked")
            T result = (T) rawResponse;
            return new LLMResponse<>(rawResponse, result);
        }
    }
}