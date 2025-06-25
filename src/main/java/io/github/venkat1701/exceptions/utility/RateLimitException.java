package io.github.venkat1701.exceptions.utility;

import io.github.venkat1701.exceptions.Research4jException;

public class RateLimitException extends Research4jException {

    private final String provider;
    private final long retryAfterSeconds;

    public RateLimitException(String message, String provider, long retryAfterSeconds) {
        super("RATE_LIMIT_ERROR", message, String.format("provider=%s, retryAfter=%d", provider, retryAfterSeconds));
        this.provider = provider;
        this.retryAfterSeconds = retryAfterSeconds;
    }

    public String getProvider() {
        return provider;
    }

    public long getRetryAfterSeconds() {
        return retryAfterSeconds;
    }
}
