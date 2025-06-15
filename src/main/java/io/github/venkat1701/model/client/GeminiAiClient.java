package io.github.venkat1701.model.client;

import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.googleai.GoogleAiGeminiChatModel;
import dev.langchain4j.service.AiServices;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.model.parser.LLMExtractor;

public class GeminiAiClient implements LLMClient {
    private final ChatModel chatModel;
    private final LLMExtractor extractor;

    public GeminiAiClient(ModelApiConfig config) {
        this.chatModel = GoogleAiGeminiChatModel.builder()
            .modelName(config.getModelName())
            .apiKey(config.getApiKey())
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