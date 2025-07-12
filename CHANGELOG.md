# Change Log

## [Unreleased] - 2024-12-xx

### Added
* RESEARCH4J-001 MAJOR Enhanced relevance calculation system with multi-factor scoring algorithm
* RESEARCH4J-002 MAJOR Dynamic graph traversal with adaptive node revisiting capabilities
* RESEARCH4J-003 MAJOR Unlimited citation fetching with quality-driven stopping criteria
* RESEARCH4J-004 MINOR Relevance-weighted randomization for diverse source selection
* RESEARCH4J-005 MAJOR Enhanced Chain of Thought strategy with mandatory code generation
* RESEARCH4J-006 MINOR Domain-specific scoring for CQRS, microservices, and Spring Boot queries
* RESEARCH4J-007 MINOR Technical content detection with code pattern recognition
* RESEARCH4J-008 PATCH Comprehensive citation quality assessment metrics
* RESEARCH4J-009 PATCH Information quality tracking and metadata management
* RESEARCH4J-010 PATCH Enhanced reasoning selection logic for technical queries

### Changed
* RESEARCH4J-011 MAJOR Reasoning engine strategy initialization to use enhanced implementations
* RESEARCH4J-012 MAJOR Citation fetcher to support unlimited batch processing with quality thresholds
* RESEARCH4J-013 MAJOR Dynamic router to implement LangGraph-style conditional node traversal
* RESEARCH4J-014 MINOR Query analysis node to provide more accurate intent detection and complexity scoring
* RESEARCH4J-015 MINOR System instruction generation to emphasize code-focused responses
* RESEARCH4J-016 PATCH Response structure to mandate `<think></think>` reasoning tags

### Fixed
* RESEARCH4J-017 MAJOR Relevance calculation returning static 0.5 scores for all citations
* RESEARCH4J-018 MAJOR Sequential pipeline execution preventing dynamic information gathering
* RESEARCH4J-019 MAJOR Limited citation fetching with arbitrary 5-source restriction
* RESEARCH4J-020 MINOR Missing code examples in technical responses
* RESEARCH4J-021 PATCH Router logic preventing beneficial node revisiting
* RESEARCH4J-022 PATCH Citation diversity issues causing top-heavy result selection

## [1.0.1] - 2024-11-28

### Added
* RESEARCH4J-023 MINOR Enhanced citation fetching using Gemini and executors for improved performance
* RESEARCH4J-024 MINOR Plug-and-play research4j AutoCloseable class for better resource management
* RESEARCH4J-025 MINOR ResearchResult and ResearchSession for structured output and session management
* RESEARCH4J-026 PATCH Type safe response handling with comprehensive error management
* RESEARCH4J-027 PATCH Better exception handling patch with structured exception hierarchy
* RESEARCH4J-028 PATCH Refactored exception handling for more robust error management
* RESEARCH4J-029 PATCH Pruned unrequired classes for cleaner codebase
* RESEARCH4J-030 MINOR Centralized configuration system with environment variable support
* RESEARCH4J-031 PATCH Relevance score and related fields for better citation quality assessment

### Changed
* RESEARCH4J-032 PATCH Version bump from 1.0.0 to 1.0.1 with stability improvements
* RESEARCH4J-033 MINOR Updated citation system for better source integration
* RESEARCH4J-034 MINOR Improved reasoning engine with parallel processing capabilities

### Fixed
* RESEARCH4J-035 PATCH Exception handling issues in pipeline execution
* RESEARCH4J-036 PATCH Resource cleanup in concurrent operations
* RESEARCH4J-037 PATCH Configuration validation for required API keys

## [1.0.0] - 2024-10-15

Initial release of Research4j - AI-powered research automation framework.

### Added
* RESEARCH4J-038 MAJOR Core Research4j framework with dynamic research agent
* RESEARCH4J-039 MAJOR Multi-LLM support (OpenAI GPT, Google Gemini)
* RESEARCH4J-040 MAJOR Citation integration (Google Search, Tavily)
* RESEARCH4J-041 MAJOR Reasoning strategies (Chain-of-Thought, Chain-of-Table, Chain-of-Ideas)
* RESEARCH4J-042 MINOR User profile system for personalized research
* RESEARCH4J-043 MINOR Session management for context-aware queries
* RESEARCH4J-044 MINOR Comprehensive configuration via builder pattern and environment variables
* RESEARCH4J-045 MINOR Asynchronous processing with virtual threads
* RESEARCH4J-046 PATCH Auto-closeable resources with proper cleanup
* RESEARCH4J-047 PATCH Structured exception hierarchy for robust error handling

### Architecture Components
* RESEARCH4J-048 MAJOR Graph-based pipeline with dynamic node routing
* RESEARCH4J-049 MAJOR Strategy pattern for reasoning methodologies
* RESEARCH4J-050 MINOR Factory pattern for LLM and citation clients
* RESEARCH4J-051 MINOR Builder pattern for configuration management
* RESEARCH4J-052 PATCH Observer pattern for pipeline monitoring

### Core Components
* RESEARCH4J-053 MAJOR `Research4j`: Main facade and entry point
* RESEARCH4J-054 MAJOR `DynamicResearchAgent`: Pipeline orchestrator
* RESEARCH4J-055 MAJOR `ReasoningEngine`: Strategy execution engine
* RESEARCH4J-056 MAJOR `CitationService`: Multi-provider citation abstraction
* RESEARCH4J-057 MINOR `Research4jConfig`: Centralized configuration management
* RESEARCH4J-058 MINOR `ResearchResult`: Comprehensive result wrapper
* RESEARCH4J-059 MINOR `ResearchSession`: Stateful query sessions

---

## Migration Guide

### Upgrading from 1.0.1 to Unreleased

**Breaking Changes:**
- Enhanced relevance calculation may produce different citation rankings
- Dynamic routing may result in longer processing times for complex queries
- Code-focused responses will include mandatory `<think></think>` tags

**Configuration Updates:**
```java
// Add new enhanced configuration options
Research4j research = Research4j.builder()
    .withGemini(apiKey)
    .withGoogleSearch(searchKey, cseId)
    .config("enhancedRelevanceEnabled", true)
    .config("adaptiveRoutingEnabled", true)
    .config("unlimitedCitations", true)
    .build();
```

**API Changes:**
- No breaking API changes - all existing code remains compatible
- New features are opt-in via configuration
- Enhanced responses provide richer content automatically

### Performance Considerations

**Enhanced Features Impact:**
- Initial query processing may be 20-30% longer due to quality assessment
- Citation fetching can gather 3-5x more sources for complex queries
- Response quality significantly improved with working code examples
- Memory usage may increase due to larger citation sets

**Optimization Recommendations:**
- Use caching for frequently accessed domains
- Configure quality thresholds based on use case requirements
- Monitor citation fetch rounds for performance tuning
- Implement circuit breakers for external API calls

---

**Research4j** - Empowering intelligent research automation through advanced AI integration and robust software architecture.