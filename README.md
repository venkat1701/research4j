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
    <version>1.0.0</version>
</dependency>
```


### Development Setup

```bash
git clone https://github.com/venkat1701/research4j.git
cd research4j
./mvnw clean install
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
        ModelApiConfig modelApiConfig = new ModelApiConfig(
            GEMINI_API_KEY,
            null,
            "gemini-1.5-flash"
        );

        CitationConfig citationConfig = new CitationConfig(CitationSource.GOOGLE_GEMINI, GOOGLE_SEARCH_API_KEY);
        CitationService citationService = new CitationService(citationConfig, GOOGLE_CSE_ID);
        LLMClient llmClient = new GeminiAiClient(modelApiConfig);
        ReasoningEngine reasoningEngine = new ReasoningEngine(llmClient);

        DynamicResearchAgent agent = new DynamicResearchAgent(
            citationService,
            reasoningEngine,
            llmClient
        );

        UserProfile profile = new UserProfile(
            "1",
            "cloud-computing",
            "intermediate",
            List.of("code-heavy", "balanced"),
            Map.of("load balancing", 7),
            List.of("what is horizontal scaling?", "CDN basics", "explain what is system design"),
            OutputFormat.MARKDOWN
        );

        String query = "CQRS Design Pattern";

        ResearchPromptConfig promptConfig = new ResearchPromptConfig(
            query,
            """
                You are an expert educator in modern distributed systems and large-scale application design.
                Your task is to generate an **educational, markdown-formatted, and source-grounded explanation** 
                for the query: "%s".
                
                ### Your Output Must Include:
                1. **Definition:** Provide a concise yet accurate definition.
                2. **Design Process:** Describe each phase clearly – from requirement gathering to maintenance.
                3. **Key Pillars:** Cover principles like scalability, availability, reliability, consistency, performance, and security.
                4. **Concrete Examples:** Mention real-world systems like Netflix, Uber, etc. and how they apply these principles.
                5. **Modern Trends:** Highlight current and emerging trends such as:
                   - Microservices
                   - Serverless
                   - Cloud-native design
                   - API-first systems
                   - AI-augmented systems
                6. **Best Practices:** Incorporate architectural design patterns (e.g., CQRS, event sourcing), tradeoffs, and key dos/don'ts.
                7. **Final Summary:** End with a clean, digestible takeaway section for learners.
                8. **Final Summary Table**: End with a clean, markdown renderable summary table for learners.
                
                ### Additional Guidelines:
                - Use clear section headings (###).
                - Include bullet points and short code blocks where helpful.
                - All facts and examples should be grounded in the citations fetched via the citation engine.
                - Format everything in **clean Markdown**, suitable for rendering in a browser-based education app.
                - Avoid hallucinations; cite concrete, trustworthy sources where applicable.
                
                Target audience is an intermediate-level learner with basic computer science knowledge.
                """.formatted(query),
            String.class,
            OutputFormat.MARKDOWN
        );


        ResearchAgentState result = agent
            .processQuery("session-001", query, profile, promptConfig)
            .get();

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
            System.out.println("=== Synchronous Citation Research Example ===");
            example.runSynchronousExample();

            System.out.println("\n"+"=".repeat(60)+"\n");

        } catch(Exception e) {
            System.err.println("Error Running Example: "+e.getMessage());
            e.printStackTrace();
        }
    }

    public void runSynchronousExample() {
        System.out.println("1. Setting up Citation Service");
        CitationConfig config = new CitationConfig(CitationSource.GOOGLE_GEMINI, GOOGLE_SEARCH_API_KEY);
        CitationService service = new CitationService(config, GOOGLE_CSE_ID);

        System.out.println("2. Setting up Gemini AI Client");
        ModelApiConfig modelApiConfig = new ModelApiConfig(
            GEMINI_API_KEY,
            null,
            "gemini-1.5-flash"
        );

        LLMClient llmClient = new GeminiAiClient(modelApiConfig);

        System.out.println("3. Setting up Reasoning Engine");
        ReasoningEngine engine = new ReasoningEngine(llmClient);

        String researchQuestion = "what is postgres";
        System.out.println("4. Research Question: " + researchQuestion);

        System.out.println("5. Fetching citations...");
        List<CitationResult> citations = service.search(researchQuestion);
        System.out.println("   Found " + citations.size() + " citations:");

        for (int i = 0; i < Math.min(citations.size(), 3); i++) {
            CitationResult citation = citations.get(i);
            System.out.println("   [" + (i + 1) + "] " + citation.getTitle());
            System.out.println("       URL: " + citation.getUrl());
            System.out.println("       Snippet: " + citation.getSnippet());
            System.out.println();
        }

        System.out.println("6. Creating Research Context.");
        ResearchPromptConfig promptConfig = new ResearchPromptConfig(
            researchQuestion,
            "You are a research assistant providing comprehensive and accurate information based on the provided sources. Please cite your sources appropriately.",
            String.class,
            OutputFormat.MARKDOWN
        );

        ResearchContext context = new ResearchContext(promptConfig);
        context.setCitations(citations);
        context.setReasoningMethod(ReasoningMethod.CHAIN_OF_IDEAS);
        context.setStartTime(System.currentTimeMillis());

        System.out.println("7. Applying Chain of Table reasoning...");
        LLMResponse<String> result = engine.reason(
            ReasoningMethod.CHAIN_OF_IDEAS,
            context,
            String.class
        );

        context.setEndTime(System.currentTimeMillis());

        System.out.println("8. Research Results:");
        System.out.println("   Processing time: " + (context.getEndTime() - context.getStartTime()) + "ms");
        System.out.println("   Final Answer:");
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
    OutputFormat.MARKDOWN
);
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
Email: [Contact](mailto:thejeastdev@gmail.com)

---

## Acknowledgments

- [LangChain4j](https://github.com/langchain4j/langchain4j) team for the excellent Java LLM framework
- [LangGraph4j](https://github.com/langchain4j/langgraph4j) for agent orchestration capabilities
- The open-source community for inspiration and feedback

---

**If Research4j helps your project, please consider giving it a star!**

[Report Bug](https://github.com/venkat1701/research4j/issues) • [Request Feature](https://github.com/venkat1701/research4j/issues) • [Documentation](https://venkat1701.github.io/research4j)
