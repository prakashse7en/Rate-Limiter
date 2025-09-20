package org.example;


import java.time.Duration;

/**
 * Factory class for creating rate limiter instances.
 * Provides static methods that match the interface specified in the requirements.
 */
public class RateLimiterFactory {

    /**
     * Creates a new rate limiter with the specified capacity and leak rate.
     * Uses default TTL of 10 minutes for bucket expiry.
     * 
     * @param capacity maximum bucket size
     * @param leakRate units leaked per second
     * @return new LeakyBucketRateLimiter instance
     */
    public static LeakyBucketRateLimiter createRateLimiter(int capacity, double leakRate) {
        return new LeakyBucketRateLimiter(capacity, leakRate);
    }

    /**
     * Creates a new rate limiter with custom TTL.
     * 
     * @param capacity maximum bucket size
     * @param leakRate units leaked per second
     * @param ttl time-to-live for inactive buckets
     * @return new LeakyBucketRateLimiter instance
     */
    public static LeakyBucketRateLimiter createRateLimiter(int capacity, double leakRate, Duration ttl) {
        return new LeakyBucketRateLimiter(capacity, leakRate, ttl);
    }
}