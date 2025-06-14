package io.github.venkat1701.core.contracts;

import io.github.venkat1701.reasoning.context.ResearchContext;
import io.github.venkat1701.core.payloads.ResearchPromptConfig;

public interface PromptTemplateEngine {
    String render(ResearchPromptConfig config, ResearchContext context);
}
