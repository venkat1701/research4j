package io.github.venkat1701.core.contracts;

import io.github.venkat1701.core.payloads.ResearchContext;

public interface ReasoningStrategy {
    ResearchContext reason(ResearchContext context);
}
