package io.github.venkat1701.exceptions;

public class Research4jException extends Exception{

    private final String errorCode;
    private final Object context;

    public Research4jException(String message) {
        super(message);
        this.errorCode = "RESEARCH4j Error";
        this.context = null;
    }

    public Research4jException(String message, Throwable cause) {
        super(message, cause);
        this.errorCode = "RESEARCH4j Error";
        this.context = null;
    }

    public Research4jException(String errorCode, String message) {
        super(message);
        this.errorCode = errorCode;
        this.context = null;
    }

    public Research4jException(String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = null;
    }

    public Research4jException(String errorCode, String message, Object context) {
        super(message);
        this.errorCode = errorCode;
        this.context = context;
    }

    public Research4jException(String errorCode, String message, Throwable cause, Object context) {
        super(message, cause);
        this.errorCode = errorCode;
        this.context = context;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public Object getContext() {
        return context;
    }

    @Override
    public String toString() {
        return String.format("Research4jException[%s]: %s", errorCode, getMessage());
    }
}
