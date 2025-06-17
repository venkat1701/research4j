package io.github.venkat1701.pipeline.nodes;

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
                return state.withQueryAnalysis(analysis.structuredOutput());
            } catch (Exception e) {
                return state.withError(e);
            }
        });
    }


    private String buildAnalysisPrompt(ResearchAgentState state) {
        return String.format("""
            Analyze this research query and provide structured analysis:
            
            Query: %s
            User Domain: %s
            User Expertise: %s
            User Preferences: %s
            
            Provide analysis in JSON format with:
            - intent: primary intent (research, comparison, explanation, creative, etc.)
            - complexity: score 1-10
            - topics: main topics identified
            - requires_citations: boolean
            - estimated_time: processing time estimate
            - suggested_reasoning: best reasoning method
            """,
            state.getQuery(),
            state.getUserProfile().getDomain(),
            state.getUserProfile().getExpertiseLevel(),
            String.join(", ", state.getUserProfile().getPreferences())
        );
    }

    @Override
    public String getName() { return "query_analysis"; }

    @Override
    public boolean shouldExecute(ResearchAgentState state) { return true; }
}
