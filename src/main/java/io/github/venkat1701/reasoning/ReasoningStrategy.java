package io.github.venkat1701.reasoning;

import java.util.List;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.core.payloads.LLMResponse;

public interface ReasoningStrategy {
    <T>LLMResponse<T> apply(String userPrompt, List<CitationResult> citations, Class<T> outputType);
}
