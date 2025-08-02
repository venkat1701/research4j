package com.github.bhavuklabs.model.parser;

public interface OutputParser<T> {
    T parse(String rawText, Class<T> type);
}
