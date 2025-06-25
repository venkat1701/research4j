package io.github.venkat1701.examples;

import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import io.github.venkat1701.Research4j;
import io.github.venkat1701.agent.ResearchResult;
import io.github.venkat1701.agent.ResearchSession;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.pipeline.profile.UserProfile;

public class Research4jExamples {

    private static final Logger logger = Logger.getLogger(Research4jExamples.class.getName());

    public static void main(String[] args) {
        System.out.println("=== Research4j Examples ===\n");

        try {
            programmaticConfigExample();
            sessionBasedExample();
            customUserProfileExample();
            reasoningMethodsExample();
            businessAnalysisExample();
            academicResearchExample();

        } catch (Exception e) {
            System.err.println("Example failed: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public static void simpleEnvironmentExample() {
        System.out.println("1. Simple Environment Example");
        System.out.println("Using environment variables for configuration...");

        try (Research4j research = Research4j.createDefault()) {

            if (!research.isHealthy()) {
                System.out.println("Service not healthy - check your API keys in environment variables");
                return;
            }

            ResearchResult result = research.research("What is machine learning?");

            if (result.hasError()) {
                System.out.println("Error: " + result.getError()
                    .getMessage());
            } else {
                System.out.println("Research completed in " + result.getProcessingTime());
                System.out.println("Answer: " + truncate(result.getAnswer(), 200));
                System.out.println("Citations: " + result.getCitations()
                    .size());
            }

        } catch (Exception e) {
            System.out.println("Configuration error: " + e.getMessage());
            System.out.println("Make sure to set your API keys as environment variables:");
            System.out.println("   - GEMINI_API_KEY or OPENAI_API_KEY");
            System.out.println("   - GOOGLE_SEARCH_API_KEY and GOOGLE_CSE_ID");
            System.out.println("   - OR TAVILY_API_KEY");
        }

        System.out.println();
    }

    public static void programmaticConfigExample() {
        System.out.println("2. Programmatic Configuration Example");
        System.out.println("Setting API keys programmatically...");

        String geminiKey = "your-gemini-api-key";
        String searchKey = "your-google-search-api-key";
        String cseId = "your-google-cse-id";

        if (isPlaceholder(geminiKey) || isPlaceholder(searchKey) || isPlaceholder(cseId)) {
            System.out.println("Replace placeholder API keys with real ones to run this example");
            System.out.println();
            return;
        }

        try (Research4j research = Research4j.createWithGemini(geminiKey, searchKey, cseId)) {

            ResearchResult result = research.research("How does CQRS pattern work in microservices?", OutputFormat.MARKDOWN);

            if (result.hasError()) {
                System.out.println("Error: " + result.getError()
                    .getMessage());
            } else {
                System.out.println("Research completed successfully");
                System.out.println("Answer preview: " + truncate(result.getAnswer(), 150));
                System.out.println("Citations found: " + result.getCitations()
                    .size());
            }

        } catch (Exception e) {
            System.out.println("Example failed: " + e.getMessage());
        }

        System.out.println();
    }

    public static void sessionBasedExample() {
        System.out.println("3. Session-Based Research Example");
        System.out.println("Conducting multiple related queries in a session...");

        try (Research4j research = Research4j.createDefault()) {

            if (!research.isHealthy()) {
                System.out.println("Service not healthy");
                return;
            }

            try (ResearchSession session = research.createSession()) {

                String[] queries = { "What is blockchain technology?", "How does Bitcoin use blockchain?", "What are smart contracts?" };

                for (String query : queries) {
                    System.out.println("Researching: " + query);

                    ResearchResult result = session.query(query);

                    if (result.hasError()) {
                        System.out.println("Error: " + result.getError()
                            .getMessage());
                    } else {
                        System.out.println("Completed in " + result.getProcessingTime());
                        System.out.println(truncate(result.getAnswer(), 100));
                    }

                    System.out.println();
                }
            }

        } catch (Exception e) {
            System.out.println("Session example failed: " + e.getMessage());
        }

        System.out.println();
    }

    public static void customUserProfileExample() {
        System.out.println("4. Custom User Profile Example");
        System.out.println("Using custom user profiles for personalized research...");

        try (Research4j research = Research4j.createDefault()) {

            if (!research.isHealthy()) {
                System.out.println("Service not healthy");
                return;
            }

            UserProfile developerProfile = new UserProfile("dev-001", "software-engineering", "expert", List.of("technical", "code-heavy", "detailed"),
                Map.of("java", 9, "microservices", 8, "distributed systems", 9, "performance", 7),
                List.of("How to optimize database queries?", "Best practices for API design", "Microservices vs monoliths"), OutputFormat.MARKDOWN);

            ResearchResult result = research.research("Best practices for API rate limiting", developerProfile);

            if (result.hasError()) {
                System.out.println("Error: " + result.getError()
                    .getMessage());
            } else {
                System.out.println("Personalized research completed");
                System.out.println("Profile: " + developerProfile.getDomain() + " (" + developerProfile.getExpertiseLevel() + ")");
                System.out.println("Answer: " + truncate(result.getAnswer(), 200));
                System.out.println("Citations: " + result.getCitations()
                    .size());
            }

        } catch (Exception e) {
            System.out.println("Custom profile example failed: " + e.getMessage());
        }

        System.out.println();
    }

    public static void reasoningMethodsExample() {
        System.out.println("5. Different Reasoning Methods Example");
        System.out.println("Comparing different reasoning approaches...");

        try (Research4j research = Research4j.builder()
            .defaultReasoning(ReasoningMethod.CHAIN_OF_TABLE)
            .build()) {

            if (!research.isHealthy()) {
                System.out.println("Service not healthy");
                return;
            }

            String query = "Compare React vs Vue vs Angular frameworks";

            ResearchResult result = research.research(query, OutputFormat.TABLE);

            if (result.hasError()) {
                System.out.println("Error: " + result.getError()
                    .getMessage());
            } else {
                System.out.println("Comparison research completed");
                System.out.println("Reasoning: CHAIN_OF_TABLE (optimized for comparisons)");
                System.out.println("Format: TABLE");
                System.out.println("Result: " + truncate(result.getAnswer(), 200));
            }

        } catch (Exception e) {
            System.out.println("Reasoning methods example failed: " + e.getMessage());
        }

        System.out.println();
    }

    public static void businessAnalysisExample() {
        System.out.println("6. Business Analysis Configuration Example");
        System.out.println("Using configuration optimized for business analysis...");

        String openaiKey = "your-openai-api-key";
        String tavilyKey = "your-tavily-api-key";

        if (isPlaceholder(openaiKey) || isPlaceholder(tavilyKey)) {
            System.out.println("Replace placeholder API keys to run this example");
            System.out.println();
            return;
        }

        try (Research4j research = Research4j.createForBusinessAnalysis(openaiKey, tavilyKey)) {

            UserProfile businessAnalyst = new UserProfile("analyst-001", "business-analysis", "expert",
                List.of("quantitative", "chart-heavy", "executive-summary"), Map.of("market analysis", 9, "financial metrics", 8, "competitive analysis", 9),
                List.of(), OutputFormat.TABLE);

            ResearchResult result = research.research("Market analysis of electric vehicle industry 2024", businessAnalyst);

            if (result.hasError()) {
                System.out.println("Error: " + result.getError()
                    .getMessage());
            } else {
                System.out.println("Business analysis completed");
                System.out.println("Optimized for: Business Analysis");
                System.out.println("Reasoning: CHAIN_OF_TABLE");
                System.out.println("Format: TABLE");
                System.out.println("Result: " + truncate(result.getAnswer(), 200));
            }

        } catch (Exception e) {
            System.out.println("Business analysis example failed: " + e.getMessage());
        }

        System.out.println();
    }

    public static void academicResearchExample() {
        System.out.println("7. Academic Research Configuration Example");
        System.out.println("Using configuration optimized for academic research...");

        String geminiKey = "your-gemini-api-key";
        String searchKey = "your-google-search-api-key";
        String cseId = "your-google-cse-id";

        if (isPlaceholder(geminiKey) || isPlaceholder(searchKey) || isPlaceholder(cseId)) {
            System.out.println("Replace placeholder API keys to run this example");
            System.out.println();
            return;
        }

        try (Research4j research = Research4j.createForAcademicResearch(geminiKey, searchKey, cseId)) {

            UserProfile researcher = new UserProfile("researcher-001", "academic", "expert", List.of("detailed", "citation-heavy", "peer-reviewed"),
                Map.of("machine learning", 9, "natural language processing", 8, "deep learning", 9), List.of(), OutputFormat.MARKDOWN);

            ResearchResult result = research.research("Recent advances in transformer architecture for NLP", researcher);

            if (result.hasError()) {
                System.out.println("Error: " + result.getError()
                    .getMessage());
            } else {
                System.out.println("Academic research completed");
                System.out.println("Optimized for: Academic Research");
                System.out.println("Reasoning: CHAIN_OF_THOUGHT");
                System.out.println("Max Citations: 15");
                System.out.println("Timeout: 2 minutes");
                System.out.println("Result: " + truncate(result.getAnswer(), 200));
                System.out.println("Citations found: " + result.getCitations()
                    .size());
            }

        } catch (Exception e) {
            System.out.println("Academic research example failed: " + e.getMessage());
        }

        System.out.println();
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "null";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength) + "...";
    }

    private static boolean isPlaceholder(String value) {
        return value == null || value.startsWith("your-") || value.equals("API_KEY") || value.equals("CSE_ID");
    }

    public static void configurationValidationExample() {
        System.out.println("Configuration Validation Example");
        System.out.println("Showing how to validate configuration before use...");

        try {
            Research4j research = Research4j.builder()
                .withGemini("invalid-key")
                .build();

            if (research.isHealthy()) {
                System.out.println("Configuration is valid and service is healthy");
            } else {
                System.out.println("Configuration is invalid or service is unhealthy");
            }

            research.close();

        } catch (Exception e) {
            System.out.println("Configuration validation failed: " + e.getMessage());
            System.out.println("This is expected behavior for invalid configurations");
        }

        System.out.println();
    }
}