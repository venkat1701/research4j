package com.github.bhavuklabs.pipeline.graph;

import java.util.concurrent.CompletableFuture;

public interface GraphNode<T> {

    CompletableFuture<T> process(T state);

    String getName();

    boolean shouldExecute(T state);
}
