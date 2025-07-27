package io.github.venkat1701.examples;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import io.github.venkat1701.Research4j;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.pipeline.profile.UserProfile;

public class ResearchServiceExample {

    private final Research4j research4j;

    public ResearchServiceExample(Research4j research4j) {
        this.research4j = research4j;
    }

    public CompletableFuture<String> conductTechnicalResearch(String query) {
        return research4j.technicalDeepResearch(query)
            .thenApply(result -> result.getFinalReport());
    }

    public CompletableFuture<String> conductBusinessResearch(String query) {
        UserProfile businessProfile = createBusinessProfile();
        DeepResearchConfig businessConfig = createBusinessConfig();

        return research4j.deepResearch(query, businessProfile, businessConfig)
            .thenApply(result -> result.getFinalReport());
    }

    private UserProfile createBusinessProfile() {
        return new UserProfile("business-user", "business", "intermediate", List.of("strategic", "roi-focused"),
            Map.of("market analysis", 7, "competitive intelligence", 6), List.of(), OutputFormat.TABLE);
    }

    private DeepResearchConfig createBusinessConfig() {
        return DeepResearchConfig.builder()
            .researchDepth(DeepResearchConfig.ResearchDepth.COMPREHENSIVE)
            .researchScope(DeepResearchConfig.ResearchScope.BROAD)
            .maxDuration(Duration.ofMinutes(10))
            .maxSources(30)
            .focusAreas(List.of("market-trends", "competition", "opportunities"))
            .build();
    }
}