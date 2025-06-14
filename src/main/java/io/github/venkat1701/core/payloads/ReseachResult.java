package io.github.venkat1701.core.payloads;

import io.github.venkat1701.reasoning.context.ResearchContext;

public class ReseachResult<T> {

    private final LLMResponse<T> modelOutput;
    private final ResearchContext context;


    public ReseachResult(LLMResponse<T> modelOutput, ResearchContext context) {
        this.modelOutput = modelOutput;
        this.context = context;
    }

    public LLMResponse<T> getModelOutput() { return modelOutput; }
    public ResearchContext getContext() { return context; }
}
