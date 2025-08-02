package com.github.bhavuklabs.pipeline.models;

import java.util.List;
import java.util.Map;

public class StructuredResponse {

    public String answer;
    public List<String> keyPoints;
    public List<CitationInfo> citations;
    public Map<String, Object> metadata;
    public String confidence;
}
