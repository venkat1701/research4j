package com.github.bhavuklabs.core.payloads;

import com.github.bhavuklabs.reasoning.context.ResearchContext;

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
