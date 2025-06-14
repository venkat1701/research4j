package io.github.venkat1701.core.payloads;

import io.github.venkat1701.core.enums.OutputFormat;

public record ResearchPromptConfig(String userPrompt, String systemInstruction, Class<?> outputType, OutputFormat outputFormat) {

}
