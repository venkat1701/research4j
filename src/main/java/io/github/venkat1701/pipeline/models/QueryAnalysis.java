package io.github.venkat1701.pipeline.models;

import java.util.List;

public class QueryAnalysis {

    public String intent;
    public int complexityScore;
    public List<String> topics;
    public boolean requiresCitations;
    public String estimatedTime;
    public String suggestedReasoning;
}
