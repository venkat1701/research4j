package io.github.venkat1701.model.parser;

public interface OutputParser<T> {
    T parse(String rawText, Class<T> type);
}
