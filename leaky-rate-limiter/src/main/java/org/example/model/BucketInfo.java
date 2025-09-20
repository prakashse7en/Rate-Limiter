package org.example.model;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * Immutable data structure representing the state of a leaky bucket.
 * Contains current level, capacity, leak rate, and last update timestamp.
 */
@Getter
@EqualsAndHashCode
public class BucketInfo {
    // Getters
    private final double currentLevel;
    private final int capacity;
    private final double leakRate;
    private final double lastUpdateTime;

    public BucketInfo(double currentLevel, int capacity, double leakRate, double lastUpdateTime) {
        this.currentLevel = Math.max(0, currentLevel); // Ensure non-negative
        this.capacity = capacity;
        this.leakRate = leakRate;
        this.lastUpdateTime = lastUpdateTime;
    }

    /**
     * Creates a new BucketInfo with updated level after leaking based on elapsed time.
     * 
     * @param currentTime the current timestamp
     * @return new BucketInfo instance with updated level
     */
    public BucketInfo leak(double currentTime) {
        if (currentTime < lastUpdateTime) {
            // Handle backwards time - keep current state
            return this;
        }
        
        double elapsedTime = currentTime - lastUpdateTime;
        double leakedAmount = leakRate * elapsedTime;
        double newLevel = Math.max(0, currentLevel - leakedAmount);
        
        return new BucketInfo(newLevel, capacity, leakRate, currentTime);
    }

    /**
     * Creates a new BucketInfo with one unit added (for a new request).
     * 
     * @return new BucketInfo instance with incremented level, or null if would overflow
     */
    public BucketInfo addRequest() {
        if (currentLevel >= capacity) {
            return null; // Would overflow
        }
        return new BucketInfo(currentLevel + 1, capacity, leakRate, lastUpdateTime);
    }

    /**
     * Checks if the bucket can accept a new request without overflowing.
     * 
     * @return true if request can be accepted, false otherwise
     */
    public boolean canAcceptRequest() {
        return currentLevel < capacity;
    }

    public double getAvailableCapacity() {
        return capacity - currentLevel;
    }


    @Override
    public String toString() {
        return String.format("BucketInfo{level=%.2f/%d, leakRate=%.2f/s, lastUpdate=%.2f}",
                currentLevel, capacity, leakRate, lastUpdateTime);
    }
}