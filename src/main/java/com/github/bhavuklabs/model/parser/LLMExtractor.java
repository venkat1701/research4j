package com.github.bhavuklabs.model.parser;

import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.V;

public interface LLMExtractor {

    @UserMessage("Extract and convert the following text into a String: {{text}}")
    String extractString(@V("text") String text);

    @UserMessage("Extract and convert the following text into JSON format: {{text}}")
    String extractJson(@V("text") String text);

    @UserMessage("Analyze and respond to the following text: {{text}}")
    String analyze(@V("text") String text);
}