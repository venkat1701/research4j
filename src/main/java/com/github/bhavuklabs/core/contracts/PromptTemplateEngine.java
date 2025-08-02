package com.github.bhavuklabs.core.contracts;

import com.github.bhavuklabs.reasoning.context.ResearchContext;
import com.github.bhavuklabs.core.payloads.ResearchPromptConfig;

public interface PromptTemplateEngine {
    String render(ResearchPromptConfig config, ResearchContext context);
}
