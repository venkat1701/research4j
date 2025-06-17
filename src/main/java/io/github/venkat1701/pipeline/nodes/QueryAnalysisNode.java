package io.github.venkat1701.pipeline.nodes;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.pipeline.graph.GraphNode;
import io.github.venkat1701.pipeline.models.QueryAnalysis;
import io.github.venkat1701.pipeline.state.ResearchAgentState;

public class QueryAnalysisNode implements GraphNode<ResearchAgentState> {

    private final LLMClient llmClient;

    public QueryAnalysisNode(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    @Override
    public CompletableFuture<ResearchAgentState> process(ResearchAgentState state) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                String analysisPrompt = buildAnalysisPrompt(state);
                LLMResponse<QueryAnalysis> analysis = llmClient.complete(analysisPrompt, QueryAnalysis.class);

                if (analysis == null || analysis.structuredOutput() == null) {
                    QueryAnalysis defaultAnalysis = createDefaultAnalysis(state);
                    return state.withQueryAnalysis(defaultAnalysis);
                }

                return state.withQueryAnalysis(analysis.structuredOutput());
            } catch (Exception e) {
                QueryAnalysis fallbackAnalysis = createDefaultAnalysis(state);
                return state.withQueryAnalysis(fallbackAnalysis);
            }
        });
    }

    private String buildAnalysisPrompt(ResearchAgentState state) {
        String userDomain = state.getUserProfile() != null ? state.getUserProfile().getDomain() : "general";
        String userExpertise = state.getUserProfile() != null ? state.getUserProfile().getExpertiseLevel() : "intermediate";
        String userPrefs = state.getUserProfile() != null && state.getUserProfile().getPreferences() != null ?
            String.join(", ", state.getUserProfile().getPreferences()) : "balanced";

        return String.format("""
            Analyze this research query and provide structured analysis in valid JSON format.
            
            Query: "%s"
            User Domain: %s
            User Expertise: %s
            User Preferences: %s
            
            Return JSON with these exact field names:
            {
                "intent": "research|comparison|explanation|creative|analysis",
                "complexityScore": 1-10,
                "topics": ["topic1", "topic2"],
                "requiresCitations": true|false,
                "estimatedTime": "time estimate",
                "suggestedReasoning": "CHAIN_OF_THOUGHT|CHAIN_OF_IDEAS|CHAIN_OF_TABLE"
            }
            
            Only return valid JSON, no other text.
            """,
            state.getQuery().replace("\"", "\\\""),
            userDomain,
            userExpertise,
            userPrefs
        );
    }

    private QueryAnalysis createDefaultAnalysis(ResearchAgentState state) {
        QueryAnalysis analysis = new QueryAnalysis();
        analysis.intent = "research";
        analysis.complexityScore = 5;
        analysis.topics = List.of("general");
        analysis.requiresCitations = true;
        analysis.estimatedTime = "2-3 minutes";
        analysis.suggestedReasoning = "CHAIN_OF_THOUGHT";
        return analysis;
    }

    @Override
    public String getName() {
        return "query_analysis";
    }

    @Override
    public boolean shouldExecute(ResearchAgentState state) {
        return state != null && state.getQuery() != null && !state.getQuery().trim().isEmpty();
    }
}