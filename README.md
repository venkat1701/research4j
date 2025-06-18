# Research4j

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Java Version](https://img.shields.io/badge/Java-17+-brightgreen.svg)](https://openjdk.java.net/)
[![Build Status](https://img.shields.io/badge/build-passing-brightgreen.svg)](https://github.com/venkat1701/research4j)

**Research4j** is a highly modular and extensible Java framework for building domain-adaptive, large language model (LLM)-powered research agents. It provides plug-and-play capabilities for integrating LLMs, vector stores, reasoning strategies, citation retrieval, and structured output rendering using a clean, layered architecture.



## Features

- **LangGraph4j-based** agent routing and orchestration
- **Pluggable LLM support**: OpenAI, Gemini
- **Modular citation providers**: Tavily, Gemini (grounded)
- **Vector DB support**: Pinecone, PGVector, Qdrant (embedding + retrieval) TBD
- **Multiple reasoning strategies**: Chain of Thought, Chain of Table, Chain of Ideas
- **Structured outputs** via Java POJOs + dynamic rendering (Markdown, JSON, HTML) TBD
- **Personalized research** via `UserProfile` (domain, expertise, verbosity, format)

---

## Architecture

Research4j follows **Clean Architecture** principles with strict separation of concerns:

### Core Components

| Component | Responsibility |
|-----------|----------------|
| `DynamicResearchAgent` | Coordinates the entire query lifecycle |
| `ResearchAgentState` | Immutable state across graph transitions |
| `ReasoningStrategy` | Strategy pattern for CoT, ToT, etc. |
| `UserProfile` | Customizes agent behavior per user |

### Model Layer

| Component | Responsibility |
|-----------|----------------|
| `LLMClient` | Unified interface for OpenAI, Gemini, Claude |
| `EmbeddingStore` | Abstraction over Pinecone, PGVector, Qdrant |

### Citation Layer

| Component | Responsibility |
|-----------|----------------|
| `CitationClient` | Searches relevant sources for a query |
| `TavilyClient`, `PerplexityClient` | Concrete implementations |

### LangGraph Nodes

| Node | Role |
|------|------|
| `QueryAnalysisNode` | Understands query complexity and requirements |
| `CitationFetchNode` | Fetches and ranks relevant sources |
| `ReasoningSelectionNode` | Chooses optimal strategy dynamically |
| `ReasoningExecutionNode` | Executes strategy and formats output |

### Output Rendering

| Component | Description |
|-----------|-------------|
| `OutputFormatter` | Selects appropriate renderer |
| `FormatRenderer` | Interface with implementations for different formats |
| `ResearchSummary`, `ComparativeAnalysis` | Output POJOs with metadata annotations |

---

## Use Cases

- **Domain-specific research assistants** (legal, financial, scientific)
- **Internal analyst tools** for product and business teams
- **Personalized tutoring systems** with adaptive learning
- **Research-heavy SaaS platforms** with intelligent search
- **Enterprise knowledge queries** via LLM-as-a-Service backends

---

## Getting Started

### Prerequisites

- **Java 17+**
- **Maven 3.6+** or **Gradle 7+**
- API keys for your chosen LLM and search providers

### Installation

#### Maven
```xml
<dependency>
    <groupId>io.github.venkat1701</groupId>
    <artifactId>research4j</artifactId>
    <version>0.1.0</version>
</dependency>
```

#### Gradle
```gradle
implementation 'io.github.venkat1701:research4j:0.1.0'
```

### Development Setup

```bash
git clone https://github.com/venkat1701/research4j.git
cd research4j
./mvnw clean install
```

---

## Configuration

### Basic Setup

```java
// Initialize LLM client
LLMClient model = new GeminiClient(geminiApiKey, "gemini-pro");

// Configure citation provider
CitationClient citation = new TavilyClient(tavilyApiKey);

// Set up vector store
EmbeddingStore store = new PineconeStore(pineconeKey, "research-index");

// Choose reasoning strategy
ReasoningStrategy strategy = new TreeOfThoughtStrategy(model);

// Configure output format
OutputFormatter formatter = new MarkdownRenderer();

// Create research agent
DynamicResearchAgent agent = DynamicResearchAgent.builder()
    .llmClient(model)
    .citationClient(citation)
    .embeddingStore(store)
    .reasoningStrategy(strategy)
    .outputFormatter(formatter)
    .build();
```

### User Profile Customization

```java
UserProfile profile = UserProfile.builder()
    .domain("financial-analysis")
    .expertiseLevel(ExpertiseLevel.INTERMEDIATE)
    .verbosity(Verbosity.DETAILED)
    .preferredFormat(OutputFormat.STRUCTURED_MARKDOWN)
    .build();

ResearchResponse response = agent.research("Analyze Q3 earnings for tech sector", profile);
```

---

## Examples

### 1. Basic Citation Fetching

#### Using Tavily Citation Provider

```java
package io.github.venkat1701.examples.citation.tavily;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.tavily.TavilyCitationFetcher;

public class TavilyCitationFetcherExample {
    public static void main(String[] args) {
        TavilyCitationFetcher fetcher = new TavilyCitationFetcher("YOUR_TAVILY_API_KEY");
        var results = fetcher.fetch("Top resources to study system design from");
        
        for (CitationResult result : results) {
            System.out.println("Title: " + result.getTitle());
            System.out.println("URL: " + result.getUrl());
            System.out.println("Snippet: " + result.getSnippet());
            System.out.println("---");
        }
    }
}
```

#### Using Gemini Citation Provider

```java
package io.github.venkat1701.examples.citation.gemini;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.gemini.GeminiCitationFetcher;

public class GeminiCitationFetcherExample {
    public static void main(String[] args) {
        GeminiCitationFetcher fetcher = new GeminiCitationFetcher(
            "YOUR_GEMINI_API_KEY", 
            "YOUR_GOOGLE_CSE_ID"
        );
        var results = fetcher.fetch("Top resources to study system design from");
        
        for (CitationResult result : results) {
            System.out.println("Source: " + result.getTitle());
            System.out.println("Content: " + result.getContent());
            System.out.println("Relevance Score: " + result.getRelevanceScore());
            System.out.println("---");
        }
    }
}
```

### 2. Advanced Research Pipeline

```java
package io.github.venkat1701.examples.pipeline;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

import io.github.venkat1701.citation.config.CitationConfig;
import io.github.venkat1701.citation.enums.CitationSource;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.payloads.ResearchPromptConfig;
import io.github.venkat1701.model.client.GeminiAiClient;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.pipeline.DynamicResearchAgent;
import io.github.venkat1701.pipeline.profile.UserProfile;
import io.github.venkat1701.pipeline.state.ResearchAgentState;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class PipelineExample {
    private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
    private static final String GOOGLE_CSE_ID = "YOUR_GOOGLE_CSE_ID";
    private static final String GOOGLE_SEARCH_API_KEY = "YOUR_GOOGLE_SEARCH_API_KEY";

    public static void main(String[] args) throws ExecutionException, InterruptedException {
        // Configure model API
        ModelApiConfig modelApiConfig = new ModelApiConfig(
            GEMINI_API_KEY,
            null,
            "gemini-1.5-flash"
        );

        // Setup citation service
        CitationConfig citationConfig = new CitationConfig(
            CitationSource.GOOGLE_GEMINI, 
            GOOGLE_SEARCH_API_KEY
        );
        CitationService citationService = new CitationService(citationConfig, GOOGLE_CSE_ID);
        
        // Initialize LLM client and reasoning engine
        LLMClient llmClient = new GeminiAiClient(modelApiConfig);
        ReasoningEngine reasoningEngine = new ReasoningEngine(llmClient);

        // Create dynamic research agent
        DynamicResearchAgent agent = new DynamicResearchAgent(
            citationService,
            reasoningEngine,
            llmClient
        );

        // Configure user profile for personalized research
        UserProfile profile = new UserProfile(
            "user-123",
            "cloud-computing",
            "intermediate",
            List.of("code-heavy", "balanced"),
            Map.of("load balancing", 7, "microservices", 9),
            List.of("what is horizontal scaling?", "CDN basics", "explain system design"),
            OutputFormat.MARKDOWN
        );

        String query = "CQRS Design Pattern";

        // Create comprehensive research prompt
        ResearchPromptConfig promptConfig = new ResearchPromptConfig(
            query,
            """
            You are an expert educator in modern distributed systems and large-scale application design.
            Your task is to generate an educational, markdown-formatted, and source-grounded explanation 
            for the query: "%s".
        
            ### Your Output Must Include:
            1. **Definition:** Provide a concise yet accurate definition.
            2. **Design Process:** Describe each phase clearly.
            3. **Key Pillars:** Cover principles like scalability, availability, reliability.
            4. **Concrete Examples:** Mention real-world systems like Netflix, Uber.
            5. **Modern Trends:** Highlight microservices, serverless, cloud-native design.
            6. **Best Practices:** Include architectural patterns, tradeoffs, dos/don'ts.
            7. **Final Summary:** End with digestible takeaways.
            8. **Summary Table:** Include a markdown table summarizing key points.
        
            ### Guidelines:
            - Use clear section headings (###).
            - Include bullet points and code blocks where helpful.
            - Ground all facts in the fetched citations.
            - Format in clean Markdown for browser rendering.
            - Avoid hallucinations; cite trustworthy sources.
        
            Target audience: intermediate-level learner with basic CS knowledge.
            """.formatted(query),
            String.class,
            OutputFormat.MARKDOWN
        );

        // Execute research query
        ResearchAgentState result = agent
            .processQuery("session-001", query, profile, promptConfig)
            .get();

        // Handle results
        if (result.getError() != null) {
            System.err.println("Error occurred: " + result.getError().getMessage());
            result.getError().printStackTrace();
        } else {
            System.out.println("=== Research Output ===");
            System.out.println(result.getFinalResponse().structuredOutput());
            System.out.println("\n=== Metadata ===");
            result.getMetadata().forEach((k, v) -> System.out.println(k + ": " + v));
        }

        agent.shutdown();
    }
}
```

### 3. Citation-Based Research with Reasoning

```java
package io.github.venkat1701.examples.reasoning;

import java.util.List;

import io.github.venkat1701.citation.CitationResult;
import io.github.venkat1701.citation.config.CitationConfig;
import io.github.venkat1701.citation.enums.CitationSource;
import io.github.venkat1701.citation.service.CitationService;
import io.github.venkat1701.core.contracts.LLMClient;
import io.github.venkat1701.core.enums.OutputFormat;
import io.github.venkat1701.core.enums.ReasoningMethod;
import io.github.venkat1701.core.payloads.LLMResponse;
import io.github.venkat1701.core.payloads.ResearchPromptConfig;
import io.github.venkat1701.model.client.GeminiAiClient;
import io.github.venkat1701.model.config.ModelApiConfig;
import io.github.venkat1701.reasoning.context.ResearchContext;
import io.github.venkat1701.reasoning.engine.ReasoningEngine;

public class CitationResearchExample {
    private static final String GEMINI_API_KEY = "YOUR_GEMINI_API_KEY";
    private static final String GOOGLE_CSE_ID = "YOUR_GOOGLE_CSE_ID";
    private static final String GOOGLE_SEARCH_API_KEY = "YOUR_GOOGLE_SEARCH_API_KEY";

    public static void main(String[] args) {
        try {
            CitationResearchExample example = new CitationResearchExample();
            System.out.println("=== Citation Research with Reasoning Example ===");
            example.runResearchExample();
        } catch(Exception e) {
            System.err.println("Error Running Example: " + e.getMessage());
            e.printStackTrace();
        }
    }

    public void runResearchExample() {
        // 1. Setup Citation Service
        System.out.println("1. Setting up Citation Service");
        CitationConfig config = new CitationConfig(CitationSource.GOOGLE_GEMINI, GOOGLE_SEARCH_API_KEY);
        CitationService service = new CitationService(config, GOOGLE_CSE_ID);

        // 2. Configure LLM Client
        System.out.println("2. Setting up Gemini AI Client");
        ModelApiConfig modelApiConfig = new ModelApiConfig(
            GEMINI_API_KEY,
            null,
            "gemini-1.5-flash"
        );
        LLMClient llmClient = new GeminiAiClient(modelApiConfig);

        // 3. Initialize Reasoning Engine
        System.out.println("3. Setting up Reasoning Engine");
        ReasoningEngine engine = new ReasoningEngine(llmClient);

        String researchQuestion = "What is PostgreSQL and how does it compare to other databases?";
        System.out.println("4. Research Question: " + researchQuestion);

        // 4. Fetch Citations
        System.out.println("5. Fetching citations...");
        List<CitationResult> citations = service.search(researchQuestion);
        System.out.println("   Found " + citations.size() + " citations:");

        // Display top 3 citations
        for (int i = 0; i < Math.min(citations.size(), 3); i++) {
            CitationResult citation = citations.get(i);
            System.out.println("   [" + (i + 1) + "] " + citation.getTitle());
            System.out.println("       URL: " + citation.getUrl());
            System.out.println("       Snippet: " + citation.getSnippet().substring(0, 
                Math.min(100, citation.getSnippet().length())) + "...");
            System.out.println();
        }

        // 5. Create Research Context
        System.out.println("6. Creating Research Context");
        ResearchPromptConfig promptConfig = new ResearchPromptConfig(
            researchQuestion,
            """
            You are a research assistant providing comprehensive and accurate information 
            based on the provided sources. Please:
            
            1. Provide a clear definition of PostgreSQL
            2. Explain its key features and capabilities
            3. Compare it with other popular databases (MySQL, MongoDB, etc.)
            4. Include use cases and when to choose PostgreSQL
            5. Cite your sources appropriately using the provided citations
            
            Format your response in clear, structured Markdown.
            """,
            String.class,
            OutputFormat.MARKDOWN
        );

        ResearchContext context = new ResearchContext(promptConfig);
        context.setCitations(citations);
        context.setReasoningMethod(ReasoningMethod.CHAIN_OF_IDEAS);
        context.setStartTime(System.currentTimeMillis());

        // 6. Apply Reasoning
        System.out.println("7. Applying Chain of Ideas reasoning...");
        LLMResponse<String> result = engine.reason(
            ReasoningMethod.CHAIN_OF_IDEAS,
            context,
            String.class
        );

        context.setEndTime(System.currentTimeMillis());

        // 7. Display Results
        System.out.println("8. Research Results:");
        System.out.println("   Processing time: " + (context.getEndTime() - context.getStartTime()) + "ms");
        System.out.println("   Citations used: " + citations.size());
        System.out.println("   Reasoning method: " + context.getReasoningMethod());
        System.out.println("\n   Final Answer:");
        System.out.println("   " + "─".repeat(50));
        System.out.println(result.rawText());
        System.out.println("   " + "─".repeat(50));

        // Cleanup
        engine.shutdown();
    }
}
```

### 4. Custom User Profile Example

```java
// Create a specialized user profile for financial analysis
UserProfile financialAnalyst = new UserProfile(
    "analyst-001",
    "financial-analysis",
    "expert",
    List.of("quantitative", "detailed", "chart-heavy"),
    Map.of(
        "risk assessment", 9,
        "market trends", 8,
        "regulatory compliance", 7
    ),
    List.of(
        "quarterly earnings analysis",
        "market volatility patterns",
        "ESG impact assessment"
    ),
    OutputFormat.STRUCTURED_MARKDOWN
);

// Research query tailored for financial analysis
String query = "Impact of rising interest rates on tech sector valuations";
ResearchResponse response = agent.research(query, financialAnalyst);
```

### 5. Multi-Format Output Example

```java
// Configure different output formats for different use cases
public class OutputFormatExample {
    public void demonstrateFormats() {
        // JSON format for API responses
        UserProfile apiProfile = UserProfile.builder()
            .preferredFormat(OutputFormat.JSON)
            .build();
        
        // Markdown for documentation
        UserProfile docsProfile = UserProfile.builder()
            .preferredFormat(OutputFormat.MARKDOWN)
            .build();
        
        // HTML for web display
        UserProfile webProfile = UserProfile.builder()
            .preferredFormat(OutputFormat.HTML)
            .build();
        
        String query = "Explain microservices architecture patterns";
        
        // Generate responses in different formats
        ResearchResponse jsonResponse = agent.research(query, apiProfile);
        ResearchResponse markdownResponse = agent.research(query, docsProfile);
        ResearchResponse htmlResponse = agent.research(query, webProfile);
    }
}
```

---

## Project Structure

```
research4j/
├── core/                       # Agent orchestration & state management
│   ├── DynamicResearchAgent.java
│   ├── ResearchAgentState.java
│   ├── ReasoningStrategy.java
│   └── UserProfile.java
│
├── model/                      # LLM clients via LangChain4j
│   ├── LLMClient.java
│   ├── GeminiClient.java
│   ├── OpenAiClient.java
│   └── ClaudeClient.java
│
├── citation/                   # Citation search providers
│   ├── CitationClient.java
│   ├── TavilyClient.java
│   ├── PerplexityClient.java
│   └── GroundedGeminiClient.java
│
├── embedding/                  # Vector store abstractions
│   ├── EmbeddingStore.java
│   ├── PineconeStore.java
│   ├── PgVectorStore.java
│   └── QdrantStore.java
│
├── reasoning/                  # Reasoning strategy implementations
│   ├── ChainOfThoughtStrategy.java
│   ├── TreeOfThoughtStrategy.java
│   ├── ChainOfTableStrategy.java
│   └── ChainOfIdeasStrategy.java
│
├── output/                     # Response formatting & rendering
│   ├── OutputFormatter.java
│   ├── FormatRenderer.java
│   ├── TableRenderer.java
│   ├── MarkdownRenderer.java
│   ├── JsonRenderer.java
│   └── models/
│       ├── ResearchSummary.java
│       └── ComparativeAnalysis.java
│
└── langgraph/                  # LangGraph4j orchestration
    └── ResearchAgentGraph.java
```

---

## Current Limitations

- No built-in UI or hosted deployment layer
- Caching layer not yet implemented
- Response streaming not supported
- Performance dependent on external LLM latency
- Requires paid API keys for full functionality

---

## Roadmap

- [ ] **Observability**: LangFuse integration for prompt tracking and evaluation
- [ ] **Streaming**: Real-time response streaming via SSE/gRPC
- [ ] **DSL**: Domain-specific language for custom reasoning graphs
- [ ] **Dashboard**: Admin interface for live monitoring and evaluation
- [ ] **Local LLMs**: Support for LM Studio, Ollama, and other local models
- [ ] **Caching**: Intelligent caching layer for performance optimization
- [ ] **Multi-modal**: Support for image and document analysis

---

## Contributing

We welcome contributions from the community! Please see our [Contributing Guide](CONTRIBUTING.md) for details on:

- Code of Conduct
- Development setup
- Pull request process
- Issue reporting guidelines

### Development

```bash
# Run tests
./mvnw test

# Run integration tests
./mvnw verify -Pintegration-tests

# Generate documentation
./mvnw javadoc:javadoc
```

---

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

---

## Maintainer

**Venkat**  
GitHub: [@venkat1701](https://github.com/venkat1701)  
Email: [Contact](mailto:contact@venkat1701.dev)

---

## Acknowledgments

- [LangChain4j](https://github.com/langchain4j/langchain4j) team for the excellent Java LLM framework
- [LangGraph4j](https://github.com/langchain4j/langgraph4j) for agent orchestration capabilities
- The open-source community for inspiration and feedback

---

**If Research4j helps your project, please consider giving it a star!**

[Report Bug](https://github.com/venkat1701/research4j/issues) • [Request Feature](https://github.com/venkat1701/research4j/issues) • [Documentation](https://venkat1701.github.io/research4j)
