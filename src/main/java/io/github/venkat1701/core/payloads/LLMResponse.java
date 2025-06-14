package io.github.venkat1701.core.payloads;

public record LLMResponse<T>(String rawText, T structuredOutput) {

}
