package io.github.venkat1701.reasoning.strategy;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Logger;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.exceptions.client.LLMClientException;
import io.github.venkat1701.reasoning.ReasoningStrategy;
import io.github.venkat1701.reasoning.context.ResearchContext;

public class ChainOfThoughtStrategy implements ReasoningStrategy {

    private static final Logger logger = Logger.getLogger(ChainOfThoughtStrategy.class.getName());

    private final LLMClient llmClient;
    private final ExecutorService executor;

    public ChainOfThoughtStrategy(LLMClient llmClient) {
        if (llmClient == null) {
            throw new IllegalArgumentException("LLM client cannot be null");
        }
        this.llmClient = llmClient;
        this.executor = Executors.newVirtualThreadPerTaskExecutor();
    }

    @Override
    public <T> LLMResponse<T> reason(ResearchContext context, Class<T> outputType) throws LLMClientException {
        if (context == null) {
            throw new IllegalArgumentException("Research context cannot be null");
        }
        if (outputType == null) {
            throw new IllegalArgumentException("Output type cannot be null");
        }

        logger.info("Starting Chain of Thought reasoning for query: " + truncateString(context.getConfig()
            .userPrompt(), 100));

        String chainOfThoughtPrompt = buildComprehensiveChainOfThoughtPrompt(context);
        context.setFinalPrompt(chainOfThoughtPrompt);

        LLMResponse<T> response = llmClient.complete(chainOfThoughtPrompt, outputType);

        logger.info("Chain of Thought reasoning completed successfully");
        return response;
    }

    @Override
    public <T> CompletableFuture<LLMResponse<T>> reasonAsync(ResearchContext context, Class<T> outputType) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return reason(context, outputType);
            } catch (LLMClientException e) {
                logger.severe("Async Chain of Thought reasoning failed: " + e.getMessage());
                throw new RuntimeException("Chain of Thought reasoning failed", e);
            }
        }, executor);
    }

    private String buildComprehensiveChainOfThoughtPrompt(ResearchContext context) {
        StringBuilder prompt = new StringBuilder();

        prompt.append("""
            You are a senior software architect and technical expert with deep expertise in enterprise software development.
            You must provide comprehensive technical responses with working code examples.
            
            MANDATORY RESPONSE STRUCTURE:
            1. Begin with <think></think> tags containing your step-by-step reasoning process
            2. Follow with detailed technical implementation including complete, working code examples
            3. Include all necessary imports, configurations, dependencies, and setup instructions
            4. Provide real-world usage patterns and best practices
            
            """);

        prompt.append("TECHNICAL RESEARCH QUESTION:\n");
        prompt.append("\"")
            .append(context.getConfig()
                .userPrompt())
            .append("\"\n\n");

        if (!context.getCitations()
            .isEmpty()) {
            prompt.append("AVAILABLE TECHNICAL SOURCES:\n");
            prompt.append("Extract practical implementation details from these ")
                .append(context.getCitations()
                    .size())
                .append(" sources:\n\n");

            for (int i = 0; i < Math.min(context.getCitations()
                .size(), 8); i++) {
                CitationResult citation = context.getCitations()
                    .get(i);
                prompt.append(String.format("""
                    [SOURCE %d] %s
                    URL: %s
                    Relevance: %.2f
                    Key Content: %s
                    
                    """, i + 1, citation.getTitle(), citation.getUrl(), citation.getRelevanceScore(), truncateString(citation.getContent(), 400)));
            }
        } else {
            prompt.append("KNOWLEDGE BASE RESEARCH:\n");
            prompt.append("Use your comprehensive knowledge to provide accurate, well-informed responses with practical examples.\n\n");
        }

        String domainRequirements = generateDomainRequirements(context.getConfig()
            .userPrompt());
        prompt.append(domainRequirements);

        prompt.append("""
            
            RESPONSE FORMAT REQUIREMENTS:
            
            <think>
            Step 1: Analyze the technical question and break down core components needed
            Step 2: Review available sources and extract key implementation patterns  
            Step 3: Plan the complete code structure and identify required dependencies
            Step 4: Consider architectural decisions, best practices, and potential challenges
            Step 5: Outline the comprehensive implementation approach with examples
            </think>
            
            # Complete Technical Implementation Guide
            
            ## Overview
            [Brief explanation of the core concept and its technical importance]
            
            ## Architecture & Design
            [Detailed architectural explanation with component relationships and design decisions]
            
            ## Complete Implementation
            
            ### Dependencies and Setup
            ```xml
            [Complete pom.xml dependencies with versions]
            ```
            
            ### Core Implementation Classes
            ```java
            [Complete working Java classes with all imports, annotations, and logic]
            ```
            
            ### Configuration Files
            ```yaml
            [Complete application.yml with all necessary configuration]
            ```
            
            ### Testing Implementation
            ```java
            [Complete unit tests and integration tests with assertions]
            ```
            
            ## Real-World Usage Examples
            [Practical usage scenarios with complete code examples]
            
            ## Best Practices & Production Considerations
            [Performance optimization, security considerations, monitoring, and deployment guidance]
            
            ## Common Issues & Troubleshooting
            [Typical problems and their solutions with code fixes]
            
            CRITICAL REQUIREMENTS:
            - Every section must include complete, runnable code examples
            - No placeholder code or incomplete snippets allowed
            - Include all necessary imports and dependencies
            - Show real-world usage patterns and error handling
            - Provide production-ready implementations
            
            Begin your comprehensive technical analysis:
            """);

        return prompt.toString();
    }

    private String generateDomainRequirements(String query) {
        String queryLower = query.toLowerCase();
        StringBuilder requirements = new StringBuilder();

        requirements.append("DOMAIN-SPECIFIC IMPLEMENTATION REQUIREMENTS:\n\n");

        if (queryLower.contains("cqrs") || queryLower.contains("command query")) {
            requirements.append("""
                🎯 CQRS PATTERN IMPLEMENTATION - MUST INCLUDE:
                - Complete Command handler classes with @CommandHandler annotations
                - Complete Query handler classes with @QueryHandler annotations
                - Event store configuration and setup code
                - Command and Query DTO implementations
                - Aggregate root classes with domain logic
                - Separate read/write database configurations
                - Spring Boot application setup with CQRS framework
                - Complete Maven dependencies for CQRS implementation
                - Unit tests for commands, queries, and event handlers
                - Integration tests demonstrating full CQRS flow
                
                """);
        }

        if (queryLower.contains("microservices") || queryLower.contains("service")) {
            requirements.append("""
                🔧 MICROSERVICES ARCHITECTURE - MUST INCLUDE:
                - Complete Spring Boot microservice implementation
                - Service discovery setup (Eureka Server and Client)
                - API Gateway configuration with routing rules
                - Inter-service communication (RestTemplate/WebClient/Feign)
                - Docker containerization with multi-stage Dockerfile
                - Kubernetes deployment manifests
                - Circuit breaker implementation (Resilience4j)
                - Distributed configuration management
                - Health check endpoints and monitoring
                - Service-to-service authentication and security
                
                """);
        }

        if (queryLower.contains("spring boot") || queryLower.contains("spring")) {
            requirements.append("""
                🌱 SPRING BOOT APPLICATION - MUST INCLUDE:
                - Main application class with @SpringBootApplication
                - REST controllers with @RestController and proper mappings
                - Service layer with @Service and business logic
                - Data access layer with @Repository and JPA entities
                - Configuration classes with @Configuration and @Bean
                - Complete application.yml with profiles and properties
                - Exception handling with @ControllerAdvice
                - Security configuration with Spring Security
                - Complete Maven/Gradle build configuration
                - Comprehensive testing with @SpringBootTest
                
                """);
        }

        if (queryLower.contains("database") || queryLower.contains("jpa")) {
            requirements.append("""
                🗃️ DATABASE INTEGRATION - MUST INCLUDE:
                - JPA entity classes with proper annotations
                - Repository interfaces with custom query methods
                - Database migration scripts (Flyway/Liquibase)
                - Transaction management configuration
                - Connection pooling setup (HikariCP)
                - Database testing with @DataJpaTest
                - Performance optimization techniques
                - Caching configuration (Redis/Hazelcast)
                
                """);
        }

        return requirements.toString();
    }

    private String truncateString(String str, int maxLength) {
        if (str == null) {
            return "No content available";
        }
        if (str.length() <= maxLength) {
            return str;
        }
        return str.substring(0, maxLength - 3) + "...";
    }

    @Override
    public String getMethodName() {
        return "CHAIN_OF_THOUGHT";
    }

    @Override
    public boolean supportsConcurrency() {
        return true;
    }

    public void shutdown() {
        try {
            executor.shutdown();
            if (!executor.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread()
                .interrupt();
        }
    }
}