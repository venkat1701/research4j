package io.github.venkat1701.core.contracts;

import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.exceptions.client.LLMClientException;

public interface LLMClient {
    <T> LLMResponse<T> complete(String prompt, Class<T> type) throws LLMClientException;
}
