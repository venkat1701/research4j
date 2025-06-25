## Changelog

### Version 1.0.1 (Current)

#### Added
- **Enhanced citation fetching** using Gemini and executors for improved performance
- **Plug-and-play research4j autoclouseable class** for better resource management
- **ResearchResult and ResearchSession** for structured output and session management
- **Type safe response handling** with comprehensive error management
- **Better exception handling patch** with structured exception hierarchy
- **Refactored exception handling** for more robust error management
- **Pruned unrequired classes** for cleaner codebase
- **Centralized configuration system** with environment variable support
- **Relevance score and related fields** for better citation quality assessment

#### Changed
- **Version bump from 1.0.0 to 1.0.1** with stability improvements
- **Updated citation system** for better source integration
- **Improved reasoning engine** with parallel processing capabilities

#### Fixed
- **Exception handling issues** in pipeline execution
- **Resource cleanup** in concurrent operations
- **Configuration validation** for required API keys

### Version 1.0.0 (Initial Release)

#### Added
- **Core Research4j framework** with dynamic research agent
- **Multi-LLM support** (OpenAI GPT, Google Gemini)
- **Citation integration** (Google Search, Tavily)
- **Reasoning strategies** (Chain-of-Thought, Chain-of-Table, Chain-of-Ideas)
- **User profile system** for personalized research
- **Session management** for context-aware queries
- **Comprehensive configuration** via builder pattern and environment variables
- **Asynchronous processing** with virtual threads
- **Auto-closeable resources** with proper cleanup
- **Structured exception hierarchy** for robust error handling

#### Architecture
- **Graph-based pipeline** with dynamic node routing
- **Strategy pattern** for reasoning methodologies
- **Factory pattern** for LLM and citation clients
- **Builder pattern** for configuration management
- **Observer pattern** for pipeline monitoring

#### Core Components
- `Research4j`: Main facade and entry point
- `DynamicResearchAgent`: Pipeline orchestrator
- `ReasoningEngine`: Strategy execution engine
- `CitationService`: Multi-provider citation abstraction
- `Research4jConfig`: Centralized configuration management
- `ResearchResult`: Comprehensive result wrapper
- `ResearchSession`: Stateful query sessions

---

**Research4j** - Empowering intelligent research automation through advanced AI integration and robust software architecture.