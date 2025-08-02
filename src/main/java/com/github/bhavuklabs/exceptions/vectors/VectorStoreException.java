package com.github.bhavuklabs.exceptions.vectors;

import com.github.bhavuklabs.exceptions.Research4jException;

public class VectorStoreException extends Research4jException {

    private final String operation;
    private final String storeType;

    public VectorStoreException(String message, String operation, String storeType) {
        super("VECTOR_STORE_ERROR", message, String.format("operation=%s, store=%s", operation, storeType));
        this.operation = operation;
        this.storeType = storeType;
    }

    public VectorStoreException(String message, Throwable cause, String operation, String storeType) {
        super("VECTOR_STORE_ERROR", message, cause, String.format("operation=%s, store=%s", operation, storeType));
        this.operation = operation;
        this.storeType = storeType;
    }

    public String getOperation() {
        return operation;
    }

    public String getStoreType() {
        return storeType;
    }
}