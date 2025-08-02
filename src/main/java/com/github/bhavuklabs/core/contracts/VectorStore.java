package com.github.bhavuklabs.core.contracts;

import java.util.List;
import java.util.Map;

public interface VectorStore {
    void add(String id, String content, Map<String, String> metadata);
    List<String> query(String query, int topK);
}
