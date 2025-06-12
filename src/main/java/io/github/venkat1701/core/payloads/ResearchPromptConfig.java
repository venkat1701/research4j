package io.github.venkat1701.core.payloads;

import io.github.venkat1701.core.enums.OutputFormat;

public class ResearchPromptConfig {
    private final String userPrompt;
    private final String systemInstruction;
    private final Class<?> outputType;
    private final OutputFormat outputFormat;

    public ResearchPromptConfig(String userPrompt, String systemInstruction, Class<?> outputType, OutputFormat outputFormat) {
        this.userPrompt = userPrompt;
        this.systemInstruction = systemInstruction;
        this.outputType = outputType;
        this.outputFormat = outputFormat;
    }

    public String getUserPrompt() { return userPrompt;}
    public String getSystemInstruction() { return systemInstruction;}
    public Class<?> getOutputType() { return outputType;}
    public OutputFormat getOutputFormat() { return outputFormat;}

}
