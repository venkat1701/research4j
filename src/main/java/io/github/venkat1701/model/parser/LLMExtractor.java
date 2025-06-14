package io.github.venkat1701.model.parser;

public interface LLMExtractor<T> {
    T extract(String text, Class<T> targetType);
}