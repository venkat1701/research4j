package io.github.venkat1701.core.payloads;

public class LLMResponse<T> {
    private final String rawText;
    private final T structuredOutput;

    public LLMResponse(String rawText, T structuredOutput) {
        this.rawText = rawText;
        this.structuredOutput = structuredOutput;
    }

    public String getRawText() { return rawText; }
    public T getStructuredOutput() { return structuredOutput; }
}
