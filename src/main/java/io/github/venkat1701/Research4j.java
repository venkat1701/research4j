package io.github.venkat1701;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.logging.Logger;

import io.github.venkat1701.agent.ResearchResult;
import io.github.venkat1701.agent.ResearchSession;
import io.github.venkat1701.citation.config.CitationConfig;
import io.github.venkat1701.citation.enums.CitationSource;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.config.Research4jConfig;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.ModelType;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.deepresearch.engine.DeepResearchEngine;
import io.github.venkat1701.deepresearch.models.DeepResearchConfig;
import io.github.venkat1701.deepresearch.models.DeepResearchProgress;
import io.github.venkat1701.deepresearch.models.DeepResearchResult;
import io.github.venkat1701.exceptions.citation.CitationException;
import io.github.venkat1701.exceptions.client.LLMClientException;
import io.github.venkat1701.exceptions.config.ConfigurationException;
import io.github.venkat1701.model.client.GeminiAiClient;
import io.github.venkat1701.model.client.OpenAiClient;
import io.github.venkat1701.pipeline.DynamicResearchAgent;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class Research4j implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(Research4j.class.getName());

    private final Research4jConfig config;
    private final DynamicResearchAgent agent;
    private final LLMClient llmClient;
    private final CitationService citationService;
    private final ReasoningEngine reasoningEngine;

    
    private final DeepResearchEngine deepResearchEngine;

    private Research4j(Builder builder) throws ConfigurationException {
        try {
            this.config = builder.configBuilder.build();
            this.llmClient = createLLMClient();
            this.citationService = createCitationService();
            this.reasoningEngine = new ReasoningEngine(llmClient);
            this.agent = new DynamicResearchAgent(citationService, reasoningEngine, llmClient);

            
            this.deepResearchEngine = new DeepResearchEngine(llmClient, citationService);

            logger.info("Research4j initialized successfully with provider: " + (config.hasApiKey(ModelType.GEMINI) ? "Gemini" : "OpenAI"));
            logger.info("Deep Research capabilities enabled");
        } catch (Exception e) {
            throw new ConfigurationException("Failed to initialize Research4j: " + e.getMessage(), e);
        }
    }

    private LLMClient createLLMClient() throws ConfigurationException, LLMClientException {
        if (config.hasApiKey(ModelType.GEMINI)) {
            logger.info("Initializing Gemini AI client with model: " + config.getDefaultModel());
            return new GeminiAiClient(config);
        } else if (config.hasApiKey(ModelType.OPENAI)) {
            logger.info("Initializing OpenAI client with model: " + config.getDefaultModel());
            return new OpenAiClient(config);
        } else {
            throw new ConfigurationException("No LLM provider configured. Please set either GEMINI_API_KEY or OPENAI_API_KEY environment variable, " +
                "or configure them programmatically using the builder pattern.");
        }
    }

    private CitationService createCitationService() throws ConfigurationException, CitationException {
        CitationSource source = config.getDefaultCitationSource();
        CitationConfig citationConfig;

        switch (source) {
            case GOOGLE_GEMINI -> {
                validateGoogleSearchConfig();
                citationConfig = new CitationConfig(source, config.getGoogleSearchApiKey());
                return new CitationService(citationConfig, config.getGoogleCseId());
            }
            case TAVILY -> {
                if (!config.hasApiKey(CitationSource.TAVILY)) {
                    throw new ConfigurationException(
                        "Tavily API key required for Tavily citation source. " + "Set TAVILY_API_KEY environment variable or configure via builder.");
                }
                citationConfig = new CitationConfig(source, config.getTavilyApiKey());
                return new CitationService(citationConfig);
            }
            default -> throw new ConfigurationException("Unsupported citation source: " + source);
        }
    }

    private void validateGoogleSearchConfig() throws ConfigurationException {
        if (!config.hasApiKey(CitationSource.GOOGLE_GEMINI)) {
            throw new ConfigurationException(
                "Google Search API key required for Google citation source. " + "Set GOOGLE_SEARCH_API_KEY environment variable or configure via builder.");
        }
        if (config.getGoogleCseId() == null || config.getGoogleCseId()
            .trim()
            .isEmpty()) {
            throw new ConfigurationException(
                "Google Custom Search Engine ID required for Google citation source. " + "Set GOOGLE_CSE_ID environment variable or configure via builder.");
        }
    }

    
    public ResearchResult research(String query) {
        validateQuery(query);
        return research(query, createDefaultUserProfile());
    }

    public ResearchResult research(String query, UserProfile userProfile) {
        validateQuery(query);
        return research(query, userProfile, config.getDefaultOutputFormat());
    }

    public ResearchResult research(String query, OutputFormat outputFormat) {
        validateQuery(query);
        return research(query, createDefaultUserProfile(), outputFormat);
    }

    public ResearchResult research(String query, UserProfile userProfile, OutputFormat outputFormat) {
        validateQuery(query);
        validateUserProfile(userProfile);
        validateOutputFormat(outputFormat);

        try {
            logger.info("Starting standard research for query: " + truncateQuery(query));

            var promptConfig = new io.github.venkat1701.core.payloads.ResearchPromptConfig(query, buildSystemInstruction(userProfile, outputFormat),
                determineOutputType(outputFormat), outputFormat);

            var result = agent.processQuery(generateSessionId(), query, userProfile, promptConfig)
                .get();

            logger.info("Standard research completed successfully in " + result.getProcessingTime());
            return new ResearchResult(result, config);

        } catch (Exception e) {
            logger.severe("Standard research failed for query: " + truncateQuery(query) + " - " + e.getMessage());
            throw new RuntimeException("Research processing failed: " + e.getMessage(), e);
        }
    }

    

    
    public CompletableFuture<DeepResearchResult> deepResearch(String query) {
        return deepResearch(query, createDefaultUserProfile(), DeepResearchConfig.comprehensiveConfig());
    }

    
    public CompletableFuture<DeepResearchResult> deepResearch(String query, UserProfile userProfile) {
        return deepResearch(query, userProfile, DeepResearchConfig.comprehensiveConfig());
    }

    
    public CompletableFuture<DeepResearchResult> deepResearch(String query, DeepResearchConfig deepConfig) {
        return deepResearch(query, createDefaultUserProfile(), deepConfig);
    }

    
    public CompletableFuture<DeepResearchResult> deepResearch(String query, UserProfile userProfile, DeepResearchConfig deepConfig) {
        validateQuery(query);
        validateUserProfile(userProfile);

        if (deepConfig == null) {
            deepConfig = DeepResearchConfig.comprehensiveConfig();
        }

        logger.info("Starting deep research for query: " + truncateQuery(query));
        logger.info("Deep research configuration: " + deepConfig.getResearchDepth() + " depth, " +
            deepConfig.getMaxSources() + " max sources, " + deepConfig.getMaxDuration());

        return deepResearchEngine.startDeepResearch(query, userProfile, deepConfig);
    }

    
    public CompletableFuture<DeepResearchResult> quickDeepResearch(String query) {
        return deepResearch(query, createDefaultUserProfile(), DeepResearchConfig.standardConfig());
    }

    
    public CompletableFuture<DeepResearchResult> exhaustiveDeepResearch(String query) {
        return deepResearch(query, createDefaultUserProfile(), DeepResearchConfig.exhaustiveConfig());
    }

    
    public CompletableFuture<DeepResearchResult> technicalDeepResearch(String query) {
        UserProfile techProfile = new UserProfile(
            "tech-researcher",
            "software-engineering",
            "expert",
            List.of("technical", "implementation", "code-heavy"),
            Map.of("software architecture", 9, "implementation", 9, "best practices", 8),
            List.of(),
            OutputFormat.MARKDOWN
        );

        DeepResearchConfig techConfig = DeepResearchConfig.builder()
            .researchDepth(DeepResearchConfig.ResearchDepth.COMPREHENSIVE)
            .researchScope(DeepResearchConfig.ResearchScope.FOCUSED)
            .maxDuration(Duration.ofMinutes(20))
            .maxSources(60)
            .maxQuestions(15)
            .focusAreas(List.of("implementation", "architecture", "best-practices", "security", "performance"))
            .build();

        return deepResearch(query, techProfile, techConfig);
    }

    
    public CompletableFuture<DeepResearchResult> academicDeepResearch(String query) {
        UserProfile academicProfile = new UserProfile(
            "academic-researcher",
            "academic",
            "expert",
            List.of("comprehensive", "citation-heavy", "peer-reviewed"),
            Map.of("research methodology", 9, "academic writing", 8, "peer review", 9),
            List.of(),
            OutputFormat.MARKDOWN
        );

        DeepResearchConfig academicConfig = DeepResearchConfig.builder()
            .researchDepth(DeepResearchConfig.ResearchDepth.EXHAUSTIVE)
            .researchScope(DeepResearchConfig.ResearchScope.INTERDISCIPLINARY)
            .maxDuration(Duration.ofMinutes(25))
            .maxSources(80)
            .maxQuestions(20)
            .enableCrossValidation(true)
            .build();

        return deepResearch(query, academicProfile, academicConfig);
    }

    
    public DeepResearchProgress getDeepResearchProgress(String sessionId) {
        return deepResearchEngine.getProgress(sessionId);
    }

    
    public Map<String, DeepResearchProgress> getAllActiveDeepResearch() {
        return deepResearchEngine.getAllActiveResearch();
    }

    
    public boolean cancelDeepResearch(String sessionId) {
        return deepResearchEngine.cancelResearch(sessionId);
    }

    
    public io.github.venkat1701.deepresearch.context.MemoryManager getMemoryManager() {
        return deepResearchEngine.getMemoryManager();
    }

    
    public ResearchSession createSession() {
        return new ResearchSession(this, generateSessionId());
    }

    public ResearchSession createSession(UserProfile userProfile) {
        validateUserProfile(userProfile);
        return new ResearchSession(this, generateSessionId(), userProfile);
    }

    
    public ResearchSession createEnhancedSession() {
        return new ResearchSession(this, generateSessionId());
    }

    
    public ResearchSession createEnhancedSession(UserProfile userProfile) {
        validateUserProfile(userProfile);
        return new ResearchSession(this, generateSessionId(), userProfile);
    }

    

    
    public static Research4j createForSoftwareDevelopment(String geminiKey, String tavilyKey) throws ConfigurationException {
        return builder()
            .withGemini(geminiKey, "gemini-2.0-flash")
            .withTavily(tavilyKey)
            .defaultReasoning(ReasoningMethod.CHAIN_OF_THOUGHT)
            .defaultOutputFormat(OutputFormat.MARKDOWN)
            .maxCitations(20)
            .timeout(Duration.ofMinutes(3))
            .build();
    }

    
    public static Research4j createForAcademicResearch(String geminiKey, String searchKey, String cseId) throws ConfigurationException {
        return builder().withGemini(geminiKey, "gemini-1.5-flash")
            .withGoogleSearch(searchKey, cseId)
            .defaultReasoning(ReasoningMethod.CHAIN_OF_THOUGHT)
            .maxCitations(15)
            .timeout(Duration.ofMinutes(2))
            .build();
    }

    
    public static Research4j createForBusinessAnalysis(String openaiKey, String tavilyKey) throws ConfigurationException {
        return builder().withOpenAI(openaiKey, "gpt-4")
            .withTavily(tavilyKey)
            .defaultReasoning(ReasoningMethod.CHAIN_OF_TABLE)
            .defaultOutputFormat(OutputFormat.TABLE)
            .maxCitations(12)
            .build();
    }

    
    public static Research4j createForDeepResearch(String geminiKey, String searchKey, String cseId) throws ConfigurationException {
        return builder()
            .withGemini(geminiKey, "gemini-1.5-pro")
            .withGoogleSearch(searchKey, cseId)
            .defaultReasoning(ReasoningMethod.CHAIN_OF_THOUGHT)
            .maxCitations(25)
            .timeout(Duration.ofMinutes(5))
            .enableDebug()
            .build();
    }

    
    private String buildSystemInstruction(UserProfile userProfile, OutputFormat outputFormat) {
        StringBuilder instruction = new StringBuilder();

        instruction.append("You are an elite AI research assistant with expertise across multiple domains. ");
        instruction.append("Your primary objective is to provide comprehensive, accurate, and well-researched answers ");
        instruction.append("that demonstrate deep understanding and critical analysis.\n\n");

        if (userProfile != null) {
            instruction.append("USER PROFILE CONTEXT:\n");
            instruction.append("- Domain: ")
                .append(userProfile.getDomain())
                .append("\n");
            instruction.append("- Expertise Level: ")
                .append(userProfile.getExpertiseLevel())
                .append("\n");
            instruction.append("- Preferences: ")
                .append(String.join(", ", userProfile.getPreferences()))
                .append("\n\n");
        }

        instruction.append("OUTPUT FORMAT REQUIREMENTS:\n");
        switch (outputFormat) {
            case MARKDOWN -> instruction.append(
                "- Use clear Markdown formatting with proper headers, lists, and emphasis\n" + "- Structure content with logical sections and subsections\n" +
                    "- Include code blocks for technical examples when relevant\n");
            case JSON -> instruction.append("- Provide response in valid JSON format only\n" + "- Structure data logically with appropriate nesting\n" +
                "- Ensure all JSON is properly escaped and parseable\n");
            case TABLE -> instruction.append("- Present information in well-structured table format\n" + "- Use clear headers and logical row organization\n" +
                "- Include summary sections where appropriate\n");
        }

        instruction.append("\nRESEARCH METHODOLOGY:\n");
        instruction.append("1. Analyze the query thoroughly to understand all implicit and explicit requirements\n");
        instruction.append("2. Synthesize information from provided citations with your knowledge base\n");
        instruction.append("3. Apply critical thinking to evaluate source reliability and information quality\n");
        instruction.append("4. Present findings with logical progression and clear reasoning\n");
        instruction.append("5. Acknowledge limitations or uncertainties when present\n");
        instruction.append("6. Prioritize accuracy over speculation while maintaining comprehensiveness\n\n");

        instruction.append("QUALITY STANDARDS:\n");
        instruction.append("- Ensure factual accuracy and cite sources appropriately\n");
        instruction.append("- Maintain professional tone while being accessible\n");
        instruction.append("- Provide sufficient detail without overwhelming the reader\n");
        instruction.append("- Use examples and analogies to clarify complex concepts\n");
        instruction.append("- Structure responses for optimal readability and comprehension\n");

        instruction.append("CODE REQUIREMENTS:\n");
        instruction.append("- Include complete, runnable code examples\n");
        instruction.append("- Provide all necessary imports and dependencies\n");
        instruction.append("- Show configuration files (application.yml, pom.xml)\n");
        instruction.append("- Include unit tests and integration examples\n");
        instruction.append("- Demonstrate real-world usage patterns\n\n");

        return instruction.toString();
    }

    private Class<?> determineOutputType(OutputFormat format) {
        return switch (format) {
            case JSON -> Map.class;
            case TABLE -> List.class;
            default -> String.class;
        };
    }

    private UserProfile createDefaultUserProfile() {
        return new UserProfile("default-user", "general", "intermediate", List.of("balanced", "comprehensive"), Map.of("general knowledge", 7, "analysis", 8),
            List.of(), config.getDefaultOutputFormat());
    }

    private String generateSessionId() {
        return "research-session-" + System.currentTimeMillis() + "-" + Integer.toHexString((int) (Math.random() * 0x10000));
    }

    private void validateQuery(String query) {
        if (query == null || query.trim()
            .isEmpty()) {
            throw new IllegalArgumentException("Research query cannot be null or empty");
        }
        if (query.length() > 10000) {
            throw new IllegalArgumentException("Research query exceeds maximum length of 10,000 characters");
        }
    }

    private void validateUserProfile(UserProfile userProfile) {
        if (userProfile == null) {
            throw new IllegalArgumentException("User profile cannot be null");
        }
    }

    private void validateOutputFormat(OutputFormat outputFormat) {
        if (outputFormat == null) {
            throw new IllegalArgumentException("Output format cannot be null");
        }
    }

    private String truncateQuery(String query) {
        if (query == null) {
            return "null";
        }
        return query.length() > 100 ? query.substring(0, 100) + "..." : query;
    }

    public Research4jConfig getConfig() {
        return config;
    }

    public boolean isHealthy() {
        try {
            boolean llmHealthy = llmClient instanceof GeminiAiClient ? ((GeminiAiClient) llmClient).isHealthy() :
                llmClient instanceof OpenAiClient ? ((OpenAiClient) llmClient).isHealthy() : false;

            if (!llmHealthy) {
                logger.warning("LLM client health check failed");
                return false;
            }

            return true;

        } catch (Exception e) {
            logger.warning("Health check failed: " + e.getMessage());
            return false;
        }
    }

    @Override
    public void close() {
        try {
            if (agent != null) {
                agent.shutdown();
            }
            if (reasoningEngine != null) {
                reasoningEngine.shutdown();
            }
            if (deepResearchEngine != null) {
                deepResearchEngine.shutdown();
            }
            if (llmClient instanceof AutoCloseable) {
                ((AutoCloseable) llmClient).close();
            }
            logger.info("Research4j closed successfully");
        } catch (Exception e) {
            logger.warning("Error during shutdown: " + e.getMessage());
        }
    }

    public static Builder builder() {
        return new Builder();
    }

    public static class Builder {

        private final Research4jConfig.Builder configBuilder = Research4jConfig.builder();

        public Builder withOpenAI(String apiKey) {
            configBuilder.openAiApiKey(apiKey);
            return this;
        }

        public Builder withGemini(String apiKey) {
            configBuilder.geminiApiKey(apiKey);
            return this;
        }

        public Builder withGemini(String apiKey, String modelName) {
            configBuilder.geminiApiKey(apiKey)
                .defaultModel(modelName);
            return this;
        }

        public Builder withOpenAI(String apiKey, String modelName) {
            configBuilder.openAiApiKey(apiKey)
                .defaultModel(modelName);
            return this;
        }

        public Builder withTavily(String apiKey) {
            configBuilder.tavilyApiKey(apiKey)
                .defaultCitationSource(CitationSource.TAVILY);
            return this;
        }

        public Builder withGoogleSearch(String searchApiKey, String cseId) {
            configBuilder.googleSearchApiKey(searchApiKey)
                .googleCseId(cseId)
                .defaultCitationSource(CitationSource.GOOGLE_GEMINI);
            return this;
        }

        public Builder defaultReasoning(ReasoningMethod method) {
            configBuilder.defaultReasoningMethod(method);
            return this;
        }

        public Builder defaultOutputFormat(OutputFormat format) {
            configBuilder.defaultOutputFormat(format);
            return this;
        }

        public Builder timeout(Duration timeout) {
            configBuilder.requestTimeout(timeout);
            return this;
        }

        public Builder maxCitations(int maxCitations) {
            configBuilder.maxCitations(maxCitations);
            return this;
        }

        public Builder maxRetries(int maxRetries) {
            configBuilder.maxRetries(maxRetries);
            return this;
        }

        public Builder enableDebug() {
            configBuilder.debugEnabled(true);
            return this;
        }

        public Builder disableCache() {
            configBuilder.cacheEnabled(false);
            return this;
        }

        public Builder config(String key, Object value) {
            configBuilder.property(key, value);
            return this;
        }

        public Research4j build() throws ConfigurationException {
            return new Research4j(this);
        }
    }

    public static Research4j createDefault() throws ConfigurationException {
        return builder().build();
    }

    public static Research4j createWithGemini(String geminiKey, String searchKey, String cseId) throws ConfigurationException {
        return builder().withGemini(geminiKey)
            .withGoogleSearch(searchKey, cseId)
            .build();
    }

    public static Research4j createWithOpenAI(String openaiKey, String tavilyKey) throws ConfigurationException {
        return builder().withOpenAI(openaiKey)
            .withTavily(tavilyKey)
            .build();
    }
}