package com.github.bhavuklabs.model.parser.impl;

import com.fasterxml.jackson.databind.ObjectMapper;

import com.github.bhavuklabs.model.parser.OutputParser;

public class JsonOutputParser<T> implements OutputParser<T> {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public T parse(String rawText, Class<T> type) {
        try {
            return this.objectMapper.readValue(rawText, type);
        } catch(Exception e) {
            throw new RuntimeException("Failed to parse LLM output");
        }
    }
}
