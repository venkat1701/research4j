package com.github.bhavuklabs.core.payloads;

public record LLMResponse<T>(String rawText, T structuredOutput) {

}
