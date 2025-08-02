package com.github.bhavuklabs.core.contracts;

import com.github.bhavuklabs.core.payloads.LLMResponse;
import com.github.bhavuklabs.exceptions.client.LLMClientException;

public interface LLMClient {
    <T> LLMResponse<T> complete(String prompt, Class<T> type) throws LLMClientException;
}
