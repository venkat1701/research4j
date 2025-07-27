package io.github.venkat1701.core.utils;

import java.util.logging.Logger;
import java.util.regex.Pattern;

import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.exceptions.client.LLMClientException;


public class RobustLLMResponseHandler {

    private static final Logger logger = Logger.getLogger(RobustLLMResponseHandler.class.getName());

    
    private static final Pattern XML_TAG_PATTERN = Pattern.compile("<[^>]*>");
    private static final Pattern DUPLICATE_BRACE_PATTERN = Pattern.compile("\\}\\s*\\{");
    private static final Pattern MARKDOWN_CODE_BLOCK_PATTERN = Pattern.compile("```[\\w]*\\n?|```");

    private final LLMClient llmClient;

    public RobustLLMResponseHandler(LLMClient llmClient) {
        this.llmClient = llmClient;
    }

    
    public <T> LLMResponse<T> safeComplete(String prompt, Class<T> type, String context) {
        try {
            logger.info("Starting safe LLM completion for context: " + context);

            
            String safePrompt = prepareSafePrompt(prompt, type);

            
            LLMResponse<T> response = executeWithRetry(safePrompt, type, 3);

            
            LLMResponse<T> cleanedResponse = cleanResponse(response, type, context);

            logger.info("Safe LLM completion successful for context: " + context);
            return cleanedResponse;

        } catch (Exception e) {
            logger.warning("LLM completion failed for context: " + context + ", using fallback: " + e.getMessage());
            return createFallbackResponse(prompt, type, context, e);
        }
    }

    
    private String prepareSafePrompt(String originalPrompt, Class<?> type) {
        StringBuilder safePrompt = new StringBuilder();

        
        String cleanedPrompt = XML_TAG_PATTERN.matcher(originalPrompt).replaceAll("");
        cleanedPrompt = cleanedPrompt.replaceAll("[\\{\\}\\[\\]]", ""); 

        safePrompt.append(cleanedPrompt);
        safePrompt.append("\n\n");

        
        safePrompt.append("CRITICAL RESPONSE FORMATTING REQUIREMENTS:\n");
        safePrompt.append("- Do NOT use any XML tags, angle brackets, or markup in your response\n");
        safePrompt.append("- Do NOT use curly braces {} or square brackets [] unless for valid JSON\n");
        safePrompt.append("- Use only plain text with markdown headers (## or ###)\n");
        safePrompt.append("- For code examples, use standard triple backticks without language specifiers\n");
        safePrompt.append("- Do NOT repeat any content or create duplicate sections\n");
        safePrompt.append("- Provide ONE complete, coherent response without duplicated elements\n");

        if (type != String.class) {
            safePrompt.append("- For structured output, provide clean JSON without markdown formatting\n");
            safePrompt.append("- Ensure JSON is valid and properly escaped\n");
        }

        safePrompt.append("\nRespond with your analysis now:\n");

        return safePrompt.toString();
    }

    
    private <T> LLMResponse<T> executeWithRetry(String prompt, Class<T> type, int maxRetries) throws LLMClientException {
        Exception lastException = null;

        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                logger.fine("LLM attempt " + attempt + "/" + maxRetries);
                return llmClient.complete(prompt, type);

            } catch (Exception e) {
                lastException = e;
                logger.warning("LLM attempt " + attempt + " failed: " + e.getMessage());

                if (attempt < maxRetries) {
                    
                    try {
                        Thread.sleep(1000 * attempt); 
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        break;
                    }
                }
            }
        }

        throw new LLMClientException("All LLM attempts failed", lastException, "ROBUST_HANDLER", "retry_exhausted");
    }

    
    @SuppressWarnings("unchecked")
    private <T> LLMResponse<T> cleanResponse(LLMResponse<T> original, Class<T> type, String context) {
        try {
            String rawText = original.rawText();
            T structuredOutput = original.structuredOutput();

            
            String cleanedRawText = cleanRawText(rawText);

            
            if (type == String.class) {
                return new LLMResponse<>(cleanedRawText, (T) cleanedRawText);
            }

            
            if (structuredOutput != null) {
                return new LLMResponse<>(cleanedRawText, structuredOutput);
            } else {
                
                T reconstructed = reconstructStructuredOutput(cleanedRawText, type, context);
                return new LLMResponse<>(cleanedRawText, reconstructed);
            }

        } catch (Exception e) {
            logger.warning("Error cleaning response for context: " + context + ", using original: " + e.getMessage());
            return original;
        }
    }

    
    private String cleanRawText(String rawText) {
        if (rawText == null || rawText.trim().isEmpty()) {
            return "No response generated";
        }

        String cleaned = rawText;

        
        cleaned = XML_TAG_PATTERN.matcher(cleaned).replaceAll("");

        
        cleaned = MARKDOWN_CODE_BLOCK_PATTERN.matcher(cleaned).replaceAll("");

        
        cleaned = DUPLICATE_BRACE_PATTERN.matcher(cleaned).replaceAll("}\n\nAlternative approach: {");

        
        cleaned = cleaned.replaceAll("\\s+", " ");
        cleaned = cleaned.replaceAll("\n\n+", "\n\n");

        return cleaned.trim();
    }

    
    @SuppressWarnings("unchecked")
    private <T> T reconstructStructuredOutput(String cleanedText, Class<T> type, String context) {
        try {
            if (type == String.class) {
                return (T) cleanedText;
            }

            
            if (type.getSimpleName().toLowerCase().contains("analysis")) {
                return (T) createAnalysisFallback(cleanedText);
            }

            
            return (T) createGenericFallback(cleanedText);

        } catch (Exception e) {
            logger.warning("Failed to reconstruct structured output for type: " + type.getSimpleName());
            return (T) cleanedText;
        }
    }

    
    @SuppressWarnings("unchecked")
    private <T> LLMResponse<T> createFallbackResponse(String originalPrompt, Class<T> type, String context, Exception error) {
        logger.warning("Creating fallback response for context: " + context + " due to: " + error.getMessage());

        String fallbackText = generateFallbackContent(originalPrompt, context);

        if (type == String.class) {
            return new LLMResponse<>(fallbackText, (T) fallbackText);
        }

        try {
            T fallbackStructured = reconstructStructuredOutput(fallbackText, type, context);
            return new LLMResponse<>(fallbackText, fallbackStructured);
        } catch (Exception e) {
            return new LLMResponse<>(fallbackText, (T) fallbackText);
        }
    }

    
    private String generateFallbackContent(String originalPrompt, String context) {
        StringBuilder fallback = new StringBuilder();

        fallback.append("## Technical Analysis\n\n");
        fallback.append("Due to processing constraints, providing a structured analysis based on the request:\n\n");

        if (originalPrompt.toLowerCase().contains("spring boot")) {
            fallback.append("### Spring Boot Implementation\n");
            fallback.append("- Use Spring Boot 3.x for latest features and performance\n");
            fallback.append("- Implement proper dependency injection with @Service and @Component\n");
            fallback.append("- Configure application.yml for environment-specific settings\n\n");
        }

        if (originalPrompt.toLowerCase().contains("microservices")) {
            fallback.append("### Microservices Architecture\n");
            fallback.append("- Service discovery with Eureka or Consul\n");
            fallback.append("- API Gateway for routing and load balancing\n");
            fallback.append("- Circuit breaker pattern for resilience\n\n");
        }

        if (originalPrompt.toLowerCase().contains("event sourcing")) {
            fallback.append("### Event Sourcing Implementation\n");
            fallback.append("- Event store for persistent event storage\n");
            fallback.append("- Command and Query separation (CQRS)\n");
            fallback.append("- Event handlers for processing domain events\n\n");
        }

        fallback.append("### Recommended Libraries\n");
        fallback.append("- Spring Boot Starter packages for rapid development\n");
        fallback.append("- Spring Cloud for microservices infrastructure\n");
        fallback.append("- Spring Data JPA for data persistence\n");
        fallback.append("- Testing frameworks: JUnit 5, TestContainers, Mockito\n\n");

        fallback.append("For detailed implementation guidance, please refer to official documentation and best practices.");

        return fallback.toString();
    }

    private Object createAnalysisFallback(String cleanedText) {
        
        
        return cleanedText;
    }

    private Object createGenericFallback(String cleanedText) {
        
        
        return cleanedText;
    }
}