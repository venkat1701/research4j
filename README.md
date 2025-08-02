# Research4j

**Intelligent Research Automation Library with Dynamic Reasoning and Citation Management**

[![Java](https://img.shields.io/badge/Java-17%2B-orange)](https://openjdk.java.net/)
[![License](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Build Status](https://img.shields.io/badge/Build-Passing-green.svg)](https://github.com/venkat1701/research4j)

Research4j is a comprehensive Java library that automates research workflows through intelligent query analysis, dynamic citation fetching, adaptive reasoning strategies, and seamless LLM integration. Built with a modular architecture, it provides a robust solution for applications requiring automated research capabilities.

## Key Features

### Intelligent Research Pipeline
- **Dynamic Query Analysis**: Automatically analyzes query complexity, intent, and requirements
- **Adaptive Reasoning Selection**: Chooses optimal reasoning strategies (Chain-of-Thought, Chain-of-Table, Chain-of-Ideas)
- **Multi-Source Citation Fetching**: Integrates with Google Search, Tavily, and other research APIs
- **User Profile Personalization**: Tailors responses based on expertise level and preferences

### Robust Architecture
- **Asynchronous Processing**: Virtual thread-based execution for high performance
- **Auto-Closeable Resources**: Proper resource management with automatic cleanup
- **Comprehensive Error Handling**: Structured exception hierarchy with retry logic
- **Session Management**: Stateful research sessions for related queries

### Flexible Integration
- **Multi-LLM Support**: OpenAI GPT, Google Gemini with extensible client architecture
- **Configurable Output Formats**: Markdown, JSON, Table formats
- **Environment-based Configuration**: Seamless deployment with environment variables
- **Builder Pattern**: Fluent API for easy configuration

## Quick Start

### Prerequisites

- **Java 21+** (Virtual threads support required)
- **API Keys** for at least one LLM provider and citation source

### Installation

```xml
<dependency>
    <groupId>io.github.venkat1701</groupId>
    <artifactId>research4j</artifactId>
    <version>1.0.1</version>
</dependency>
```

### Setting up your CSE and API Keys
In order to use this, you need to setup your own programmable search engine. This is required to get the citations and materials to perform reasoning on.
#### Setting up the Programmable Search Engine
1. Visit https://programmablesearchengine.google.com/
2. Create your Custom Search Engine and get the CSE ID from the dashboard.
3. Paste that into your CSE_ID

#### Getting the Google Search API Key
1. Go to this website https://developers.google.com/custom-search/v1/introduction and generate a Programmable Search Engine API key.
2. Paste that into the GOOGLE_SEARCH_API_KEY environmental variable.

### Basic Usage

```java
import com.github.bhavuklabs.Research4j;
import agent.com.github.bhavuklabs.ResearchResult;

// Environment-based configuration (recommended)
try(Research4j research = Research4j.createDefault()){
ResearchResult result = research.research("What is machine learning?");
    
    System.out.

println("Answer: "+result.getAnswer());
    System.out.

println("Citations: "+result.getCitations().

size());
    System.out.

println("Processing Time: "+result.getProcessingTime());
    }
```

## Configuration Guide

### Environment Variables

Set these environment variables for automatic configuration:

```bash
# LLM Providers (at least one required)
export GEMINI_API_KEY="your-gemini-api-key"
export OPENAI_API_KEY="your-openai-api-key"

# Citation Sources (at least one required)
export TAVILY_API_KEY="your-tavily-api-key"
export GOOGLE_SEARCH_API_KEY="your-google-search-api-key"
export GOOGLE_CSE_ID="your-google-cse-id"

# Optional Configuration
export RESEARCH4J_DEFAULT_MODEL="gemini-1.5-flash"
export RESEARCH4J_MAX_CITATIONS="15"
export RESEARCH4J_REQUEST_TIMEOUT_SECONDS="60"
```

### Programmatic Configuration

```java
// Gemini + Google Search Configuration
Research4j research = Research4j.builder()
    .withGemini("your-gemini-key", "gemini-1.5-flash")
    .withGoogleSearch("search-api-key", "cse-id")
    .defaultReasoning(ReasoningMethod.CHAIN_OF_THOUGHT)
    .defaultOutputFormat(OutputFormat.MARKDOWN)
    .maxCitations(15)
    .timeout(Duration.ofMinutes(2))
    .enableDebug()
    .build();

// OpenAI + Tavily Configuration
Research4j research = Research4j.builder()
    .withOpenAI("your-openai-key", "gpt-4")
    .withTavily("your-tavily-key")
    .defaultReasoning(ReasoningMethod.CHAIN_OF_TABLE)
    .maxRetries(3)
    .build();
```

## Advanced Usage

### Session-Based Research

```java
try (Research4j research = Research4j.createDefault();
     ResearchSession session = research.createSession()) {
    
    // Related queries in same session maintain context
    ResearchResult blockchain = session.query("What is blockchain?");
    ResearchResult bitcoin = session.query("How does Bitcoin use blockchain?");
    ResearchResult smartContracts = session.query("What are smart contracts?");
}
```

### Custom User Profiles

```java
import profile.pipeline.com.github.bhavuklabs.UserProfile;
import enums.core.com.github.bhavuklabs.OutputFormat;

UserProfile developerProfile = new UserProfile("dev-001",                          // User ID
    "software-engineering",             // Domain
    "expert",                          // Expertise level
    List.of("technical", "detailed"),  // Preferences
    Map.of(                           // Topic interests (weighted)
        "java", 9, "microservices", 8, "distributed systems", 9), List.of(),                        // Previous queries
    OutputFormat.MARKDOWN             // Preferred format
);

ResearchResult result = research.research("Best practices for API rate limiting", developerProfile);
```

### Specialized Configurations

```java
// Academic Research Configuration
Research4j academicResearch = Research4j.createForAcademicResearch(
    "gemini-key", "search-key", "cse-id"
);

// Business Analysis Configuration
Research4j businessAnalysis = Research4j.createForBusinessAnalysis(
    "openai-key", "tavily-key"
);
```

## Architecture Overview

### Core Components

#### Research4j (Main Entry Point)
The primary facade that orchestrates the entire research pipeline:

```java
public class Research4j implements AutoCloseable {
    private final Research4jConfig config;
    private final DynamicResearchAgent agent;
    private final LLMClient llmClient;
    private final CitationService citationService;
    private final ReasoningEngine reasoningEngine;
}
```

#### DynamicResearchAgent (Pipeline Orchestrator)
Manages the research workflow through a graph-based pipeline:

- **QueryAnalysisNode**: Analyzes incoming queries for complexity and intent
- **CitationFetchNode**: Retrieves relevant sources based on query analysis
- **ReasoningSelectionNode**: Chooses optimal reasoning strategy
- **ReasoningExecutionNode**: Executes the selected reasoning approach

#### ReasoningEngine (Strategy Pattern Implementation)
Supports multiple reasoning methodologies:

- **Chain-of-Thought**: Step-by-step analytical reasoning
- **Chain-of-Table**: Structured tabular analysis for comparisons
- **Chain-of-Ideas**: Creative parallel idea generation

#### CitationService (Multi-Provider Abstraction)
Unified interface for multiple citation sources:

```java
public interface CitationFetcher {
    List<CitationResult> fetch(String query) throws CitationException;
}
```

### Data Flow Architecture

```
User Query → Query Analysis → Citation Fetch → Reasoning Selection → 
Reasoning Execution → Response Generation → ResearchResult
```

### Configuration Management

The library employs a layered configuration approach:

1. **Environment Variables**: Primary configuration source
2. **Programmatic Configuration**: Override environment settings
3. **Default Values**: Fallback for unspecified settings

```java
public class Research4jConfig {
    // Environment variable loading with fallbacks
    private void loadFromEnvironment() {
        loadApiKeyFromEnv(OPENAI_API_KEY, ModelType.OPENAI.name());
        loadApiKeyFromEnv(GEMINI_API_KEY, ModelType.GEMINI.name());
        // ... additional configuration loading
    }
}
```

## API Reference

### Core Classes

#### Research4j

Primary interface for research operations:

```java
// Basic research
ResearchResult research(String query)
ResearchResult research(String query, UserProfile userProfile)
ResearchResult research(String query, OutputFormat outputFormat)
ResearchResult research(String query, UserProfile userProfile, OutputFormat outputFormat)

// Session management
ResearchSession createSession()
ResearchSession createSession(UserProfile userProfile)

// Health checks
boolean isHealthy()
```

#### ResearchResult

Contains comprehensive research output:

```java
public class ResearchResult {
    String getAnswer()                    // Formatted answer
    String getRawResponse()               // Raw LLM response
    List<CitationResult> getCitations()  // Source citations
    Map<String, Object> getMetadata()    // Processing metadata
    Duration getProcessingTime()          // Execution time
    boolean hasError()                    // Error status
    Exception getError()                  // Error details if any
}
```

#### CitationResult

Structured citation information:

```java
public class CitationResult {
    String getTitle()                     // Source title
    String getSnippet()                   // Brief excerpt
    String getContent()                   // Full content
    String getUrl()                       // Source URL
    double getRelevanceScore()            // 0.0-1.0 relevance
    LocalDateTime getRetrievedAt()        // Fetch timestamp
    String getDomain()                    // Source domain
    int getWordCount()                    // Content length
}
```

### Configuration Options

#### Research4jConfig.Builder

```java
Research4jConfig.Builder builder()
    .openAiApiKey(String apiKey)
    .geminiApiKey(String apiKey)
    .tavilyApiKey(String apiKey)
    .googleSearchApiKey(String searchKey)
    .googleCseId(String cseId)
    .defaultModel(String model)
    .defaultCitationSource(CitationSource source)
    .defaultReasoningMethod(ReasoningMethod method)
    .defaultOutputFormat(OutputFormat format)
    .requestTimeout(Duration timeout)
    .maxCitations(int maxCitations)
    .maxRetries(int maxRetries)
    .debugEnabled(boolean enabled)
    .cacheEnabled(boolean enabled)
    .build()
```

## Error Handling

### Exception Hierarchy

Research4j provides a comprehensive exception hierarchy for robust error handling:

```java
Research4jException (Base)
├── ConfigurationException        // Configuration issues
├── LLMClientException           // LLM provider errors
├── CitationException            // Citation fetch errors
├── PipelineException            // Pipeline execution errors
├── ReasoningException           // Reasoning strategy errors
├── AuthenticationException      // API authentication errors
├── RateLimitException          // Rate limiting errors
└── VectorStoreException        // Vector database errors
```

### Error Handling Best Practices

```java
try (Research4j research = Research4j.createDefault()) {
    ResearchResult result = research.research("complex query");
    
    if (result.hasError()) {
        Exception error = result.getError();
        if (error instanceof RateLimitException) {
            RateLimitException rateLimit = (RateLimitException) error;
            System.out.println("Rate limited by: " + rateLimit.getProvider());
            System.out.println("Retry after: " + rateLimit.getRetryAfterSeconds());
        }
    }
} catch (ConfigurationException e) {
    System.err.println("Configuration error: " + e.getMessage());
} catch (Research4jException e) {
    System.err.println("Research error [" + e.getErrorCode() + "]: " + e.getMessage());
}
```

## Performance Considerations

### Async Processing

Research4j leverages Java's virtual threads for optimal performance:

```java
// Internal async processing example
private CompletableFuture<CitationResult> fetchCitationAsync(WebSearchResult result) {
    return CompletableFuture.supplyAsync(() -> {
        // Citation processing logic
    }, virtualThreadExecutor);
}
```

### Resource Management

Proper resource cleanup is handled automatically:

```java
@Override
public void close() {
    try {
        if (agent != null) agent.shutdown();
        if (llmClient instanceof AutoCloseable) {
            ((AutoCloseable) llmClient).close();
        }
    } catch (Exception e) {
        logger.warning("Error during shutdown: " + e.getMessage());
    }
}
```

### Optimization Tips

1. **Reuse Research4j instances** when possible
2. **Use sessions** for related queries to leverage context
3. **Configure appropriate timeouts** based on your use case
4. **Monitor health status** for long-running applications
5. **Implement proper retry logic** for transient failures

## Testing

### Unit Testing Example

```java
@Test
public void testBasicResearch() throws Exception {
    try (Research4j research = Research4j.builder()
            .withGemini("test-key")
            .withTavily("test-key")
            .build()) {
        
        ResearchResult result = research.research("test query");
        
        assertNotNull(result);
        assertFalse(result.hasError());
        assertNotNull(result.getAnswer());
    }
}
```

### Integration Testing

```java
@Test
@EnabledIf("hasValidApiKeys")
public void testFullPipeline() throws Exception {
    try (Research4j research = Research4j.createDefault()) {
        assertTrue(research.isHealthy());
        
        ResearchResult result = research.research("machine learning basics");
        
        assertFalse(result.hasError());
        assertTrue(result.getCitations().size() > 0);
        assertTrue(result.getProcessingTime().toSeconds() < 60);
    }
}
```

## Production Deployment

### Docker Configuration

```dockerfile
FROM openjdk:17-jre-slim

COPY research4j-app.jar /app/
COPY application.properties /app/

ENV GEMINI_API_KEY=${GEMINI_API_KEY}
ENV GOOGLE_SEARCH_API_KEY=${GOOGLE_SEARCH_API_KEY}
ENV GOOGLE_CSE_ID=${GOOGLE_CSE_ID}

WORKDIR /app
ENTRYPOINT ["java", "-jar", "research4j-app.jar"]
```

### Monitoring and Logging

```java
// Health check endpoint
@GetMapping("/health")
public ResponseEntity<Map<String, Object>> health() {
    Map<String, Object> status = new HashMap<>();
    status.put("healthy", research4j.isHealthy());
    status.put("timestamp", Instant.now());
    
    return ResponseEntity.ok(status);
}

// Metrics collection
@EventListener
public void onResearchComplete(ResearchResult result) {
    meterRegistry.counter("research.completed").increment();
    meterRegistry.timer("research.duration")
        .record(result.getProcessingTime());
}
```

### Configuration Management

```yaml
# application.yml
research4j:
  llm:
    provider: gemini
    model: gemini-1.5-flash
    timeout: 60s
  citation:
    provider: google
    max-results: 15
  reasoning:
    default-method: CHAIN_OF_THOUGHT
  performance:
    max-retries: 3
    enable-cache: true
```

## Contributing

### Development Setup

```bash
git clone https://github.com/venkat1701/research4j.git
cd research4j
./mvnw clean install
```

### Code Style Guidelines

- Follow standard Java conventions
- Use meaningful variable and method names
- Implement comprehensive error handling
- Write unit tests for new features
- Document public APIs with Javadoc

### Pull Request Process

1. Fork the repository
2. Create feature branch (`git checkout -b feature/new-feature`)
3. Commit changes (`git commit -am 'Add new feature'`)
4. Push to branch (`git push origin feature/new-feature`)
5. Create Pull Request

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Support

- **Documentation**: [GitHub Wiki](https://github.com/venkat1701/research4j/wiki)
- **Issues**: [GitHub Issues](https://github.com/venkat1701/research4j/issues)
- **Discussions**: [GitHub Discussions](https://github.com/venkat1701/research4j/discussions)
