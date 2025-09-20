package org.example;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import lombok.Getter;
import org.example.model.BucketInfo;
import org.example.model.RateLimiterResult;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Functional implementation of a leaky bucket rate limiter.
 * 
 * Features:
 * - Per-user independent buckets
 * - Time-based leaking at configurable rate
 * - TTL-based automatic expiry (default 10 minutes)
 * - Immutable functional approach
 * - Thread-safe using Caffeine cache
 */
@Getter
public class LeakyBucketRateLimiter {
    // Getters
    private final int capacity;
    private final double leakRate;
    private final Cache<String, BucketInfo> buckets;

    /**
     * Creates a new rate limiter with default TTL of 10 minutes.
     * 
     * @param capacity maximum bucket size
     * @param leakRate units leaked per second
     */
    public LeakyBucketRateLimiter(int capacity, double leakRate) {
        this(capacity, leakRate, Duration.ofMinutes(1));
    }

    /**
     * Creates a new rate limiter with custom TTL.
     * 
     * @param capacity maximum bucket size
     * @param leakRate units leaked per second  
     * @param ttl time-to-live for inactive buckets
     */
    public LeakyBucketRateLimiter(int capacity, double leakRate, Duration ttl) {
        if (capacity <= 0) {
            throw new IllegalArgumentException("Capacity must be positive");
        }
        if (leakRate <= 0) {
            throw new IllegalArgumentException("Leak rate must be positive");
        }
        if (ttl == null || ttl.isNegative() || ttl.isZero()) {
            throw new IllegalArgumentException("TTL must be positive");
        }

        this.capacity = capacity;
        this.leakRate = leakRate;
        this.buckets = Caffeine.newBuilder()
                .expireAfterWrite(ttl)
                .maximumSize(10000) // Prevent memory leaks with a reasonable limit
                .build();
    }

    /**
     * Copy constructor for functional updates.
     */
    private LeakyBucketRateLimiter(int capacity, double leakRate, Cache<String, BucketInfo> buckets) {
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.buckets = buckets;
    }

    /**
     * Determines if a request should be allowed for the given user.
     * Returns a new rate limiter instance with updated state.
     * 
     * @param userId the user identifier
     * @param timestamp current time in seconds (Unix epoch)
     * @return RateLimiterResult containing decision and new state
     */
    public RateLimiterResult allowRequest(String userId, double timestamp) {
        if (userId == null || userId.trim().isEmpty()) {
            throw new IllegalArgumentException("User ID cannot be null or empty");
        }
        if (timestamp < 0) {
            throw new IllegalArgumentException("Timestamp cannot be negative");
        }

        // Create a new cache instance for functional approach
        Cache<String, BucketInfo> newCache = Caffeine.newBuilder()
                .expireAfterWrite(Duration.ofMinutes(1))
                .maximumSize(10000)
                .build();
        
        // Copy existing buckets to new cache
        newCache.putAll(buckets.asMap());

        // Get current bucket state (or create new one)
        BucketInfo currentBucket = newCache.getIfPresent(userId);
        if (currentBucket == null) {
            // First request from this user
            currentBucket = new BucketInfo(0, capacity, leakRate, timestamp);
        }

        // Apply leaking based on elapsed time
        BucketInfo leakedBucket = currentBucket.leak(timestamp);

        // Try to add the request
        BucketInfo updatedBucket = leakedBucket.addRequest();
        
        if (updatedBucket == null) {
            // Request rejected - bucket would overflow
            newCache.put(userId, leakedBucket); // Update timestamp but not level
            return new RateLimiterResult(false, new LeakyBucketRateLimiter(capacity, leakRate, newCache));
        } else {
            // Request allowed
            newCache.put(userId, updatedBucket);
            return new RateLimiterResult(true, new LeakyBucketRateLimiter(capacity, leakRate, newCache));
        }
    }

    /**
     * Gets the current bucket state for debugging purposes.
     * 
     * @param userId the user identifier
     * @return BucketInfo or null if user doesn't exist
     */
    public BucketInfo getBucketState(String userId) {
        if (userId == null || userId.trim().isEmpty()) {
            return null;
        }
        return buckets.getIfPresent(userId);
    }

    /**
     * Gets the current bucket state with leaking applied up to the specified time.
     * 
     * @param userId the user identifier
     * @param currentTime current timestamp for leaking calculation
     * @return BucketInfo with leaking applied, or null if user doesn't exist
     */
    public BucketInfo getBucketStateAtTime(String userId, double currentTime) {
        BucketInfo bucket = getBucketState(userId);
        return bucket != null ? bucket.leak(currentTime) : null;
    }

    /**
     * Gets all active buckets (for testing/debugging).
     * 
     * @return map of user IDs to their bucket states
     */
    public Map<String, BucketInfo> getAllBuckets() {
        return new ConcurrentHashMap<>(buckets.asMap());
    }

    /**
     * Gets the number of active buckets.
     * 
     * @return count of active buckets
     */
    public long getActiveBucketCount() {
        return buckets.estimatedSize();
    }

    /**
     * Triggers cache cleanup to remove expired entries.
     * Useful for testing TTL behavior.
     */
    public void cleanUp() {
        buckets.cleanUp();
    }

    @Override
    public String toString() {
        return String.format("LeakyBucketRateLimiter{capacity=%d, leakRate=%.2f/s, activeBuckets=%d}",
                capacity, leakRate, buckets.estimatedSize());
    }
}