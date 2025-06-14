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

    public GeminiAiClient(ModelApiConfig config) {
        this.chatModel = GoogleAiGeminiChatModel.builder()
            .modelName(config.getModelName())
            .apiKey(config.getApiKey())
            .build();
    }

    @Override
    public <T> LLMResponse<T> complete(String prompt, Class<T> type) {
        LLMExtractor<T> extractor = AiServices.create(LLMExtractor.class, chatModel);
        T result = extractor.extract(prompt, type);
        return new LLMResponse<>(result.toString(), result);
    }

}
