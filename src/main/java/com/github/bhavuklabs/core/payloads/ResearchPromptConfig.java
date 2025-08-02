package com.github.bhavuklabs.core.payloads;

import com.github.bhavuklabs.core.enums.OutputFormat;

public record ResearchPromptConfig(String userPrompt, String systemInstruction, Class<?> outputType, OutputFormat outputFormat) {

}
