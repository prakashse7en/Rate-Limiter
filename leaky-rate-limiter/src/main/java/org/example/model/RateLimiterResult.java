package org.example.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;
import org.example.LeakyBucketRateLimiter;

/**
 * Immutable result object representing the outcome of a rate limiting decision.
 * Contains whether the request was allowed and the new state of the rate limiter.
 */
@EqualsAndHashCode
@Getter
public class RateLimiterResult {

    private final boolean allowed;
    private final LeakyBucketRateLimiter newLimiterState;

    public RateLimiterResult(boolean allowed, LeakyBucketRateLimiter newLimiterState) {
        this.allowed = allowed;
        this.newLimiterState = newLimiterState;
    }


    @Override
    public String toString() {
        return String.format("RateLimiterResult{allowed=%s}", allowed);
    }
}