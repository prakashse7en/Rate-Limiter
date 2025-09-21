# Leaky Bucket Rate Limiter

A functional implementation of a rate limiter using the **leaky bucket algorithm** in Java. This implementation provides per-user rate limiting with time-based leaking and maintains immutable state throughout operations.

## Problem Description

This project implements a rate limiter where:
- Each user has their own bucket with a fixed capacity
- Requests fill the bucket (add 1 unit)
- The bucket "leaks" at a constant rate over time
- If a request would overflow the bucket, it should be rejected

## Features

- **Functional Approach**: Minimizes mutation and returns new state instances
- **Time-based Leaking**: Buckets leak at a configurable rate per second
- **Per-user Buckets**: Each user_id gets their own independent bucket
- **TTL Support**: Automatic expiry of inactive buckets
- **Thread-safe**: Uses Caffeine cache for concurrent access
- **Edge Case Handling**: Backwards time, overflow scenarios, and large time gaps
- **Comprehensive Testing**: Full test coverage with JUnit 5

## Core Interface

The implementation provides three main functions as specified in the requirements:

```java
// Creates a new rate limiter
LeakyBucketRateLimiter createRateLimiter(int capacity, double leakRate)

// Determines if a request should be allowed
RateLimiterResult allowRequest(String userId, double timestamp)

// Returns current bucket information for debugging  
BucketInfo getBucketState(String userId)
```

## Quick Start

### Prerequisites

- Java 11 or higher
- Maven 3.6+

### Basic Usage

```java

// Create a rate limiter with capacity of 5 and leak rate of 1.0 units/second
LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);

// Check if request is allowed
RateLimiterResult result = limiter.allowRequest("user1", System.currentTimeMillis() / 1000.0);

if (result.isAllowed()) {
    // Process the request
    System.out.println("Request allowed");
    // Use the new limiter state for subsequent requests
    limiter = result.getNewLimiterState();
} else {
    // Reject the request
    System.out.println("Request rate limited");
}
```

## API Documentation

### Core Classes

#### `LeakyBucketRateLimiter`

Main rate limiter class providing functional rate limiting capabilities.

**Constructor:**
```java
LeakyBucketRateLimiter(int capacity, double leakRate)
LeakyBucketRateLimiter(int capacity, double leakRate, Duration ttl)
```

**Key Methods:**
```java
RateLimiterResult allowRequest(String userId, double timestamp)
BucketInfo getBucketState(String userId)
BucketInfo getBucketStateAtTime(String userId, double currentTime)
```

#### `RateLimiterResult`

Immutable result object containing:
- `boolean isAllowed()` - Whether the request was allowed
- `LeakyBucketRateLimiter getNewLimiterState()` - Updated limiter state

#### `BucketInfo`

Immutable bucket state containing:
- `double getCurrentLevel()` - Current bucket fill level
- `int getCapacity()` - Maximum bucket capacity
- `double getLeakRate()` - Rate at which bucket leaks (units/second)
- `double getLastUpdateTime()` - Last update timestamp

### Factory Method

```java
// Basic rate limiter with default 1-minute TTL
LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(capacity, leakRate);

// Rate limiter with custom TTL
LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(capacity, leakRate, Duration.ofMinutes(5));
```

## Test Scenarios

The implementation includes comprehensive tests covering:

### Basic Functionality
```java
// First request from new user
limiter = createRateLimiter(5, 1.0);
result = limiter.allowRequest("user1", 0.0);
// result.isAllowed() == true

// Multiple requests within capacity
for (int i = 0; i < 5; i++) {
    result = limiter.allowRequest("user1", timestamp);
    limiter = result.getNewLimiterState();
    // All should be allowed
}
```

### Burst Handling
```java
// Rapid requests that exceed capacity
limiter = createRateLimiter(3, 1.0);
// First 3 requests allowed, subsequent rejected
for (int i = 0; i < 10; i++) {
    result = limiter.allowRequest("user1", 0.0);
    // Only first 3 are allowed
}
```

### Time-based Leaking
```java
// Requests separated by time should be allowed after leaking
limiter = createRateLimiter(3, 1.0);
// Fill bucket, wait 1 second, should allow 1 more request
limiter = fillBucket(limiter, "user1", 0.0);
result = limiter.allowRequest("user1", 1.0); // 1 second later
// result.isAllowed() == true (1 unit leaked)
```

### Multiple Users
```java
// Independent bucket behavior
limiter = createRateLimiter(3, 1.0);
// Fill user1's bucket
// user2 should still be able to make requests
result = limiter.allowRequest("user2", 0.0);
// result.isAllowed() == true
```

## Design Decisions and Trade-offs

### Functional Approach
- **Decision**: Use immutable data structures and return new state instances
- **Benefits**: Thread-safe, predictable behavior, easier to test and reason about
- **Trade-offs**: Higher memory usage due to object creation

### Caffeine Cache
- **Decision**: Use Caffeine for bucket storage with TTL support
- **Benefits**: Automatic expiry, thread-safe, high performance
- **Trade-offs**: Additional dependency, learning curve

### Time Handling
- **Decision**: Handle backwards time by preserving current state
- **Benefits**: Robust against clock adjustments
- **Trade-offs**: May not reflect real-world time accurately in edge cases

### Precision
- **Decision**: Use double for time and bucket levels
- **Benefits**: Supports fractional seconds and precise leaking
- **Trade-offs**: Potential floating-point precision issues in extreme cases

## Edge Cases Handled

1. **First request from new user**: Creates new bucket automatically
2. **Backwards time**: Preserves current state without leaking
3. **Large time gaps**: Prevents bucket level from going negative
4. **Bucket overflow**: Rejects requests that would exceed capacity
5. **Null/empty user IDs**: Throws IllegalArgumentException
6. **Negative timestamps**: Throws IllegalArgumentException
7. **Invalid constructor parameters**: Validates capacity, leak rate, and TTL

## Performance Considerations

- **Memory**: Buckets automatically expire based on TTL (default 1 minute)
- **Cache Size**: Limited to 10,000 buckets to prevent memory leaks
- **Thread Safety**: Caffeine cache handles concurrent access efficiently
- **Time Complexity**: O(1) for bucket lookup and updates

## Dependencies

```xml
<dependency>
    <groupId>com.github.ben-manes.caffeine</groupId>
    <artifactId>caffeine</artifactId>
    <version>3.2.2</version>
</dependency>
<dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.42</version>
</dependency>
```

Test dependencies:
- JUnit 5 (5.10.0)
- Mockito (4.11.0)

## Testing

Run all tests:
```bash
mvn test
```

Generate coverage report:
```bash
mvn jacoco:report
```

View coverage report at: `target/site/jacoco/index.html`

## Example Usage Scenarios

### API Rate Limiting
```java
// 100 requests per minute, leaking at 1.67 req/sec
LeakyBucketRateLimiter apiLimiter = RateLimiterFactory.createRateLimiter(100, 1.67);

public boolean isRequestAllowed(String apiKey) {
    double currentTime = System.currentTimeMillis() / 1000.0;
    RateLimiterResult result = apiLimiter.allowRequest(apiKey, currentTime);
    apiLimiter = result.getNewLimiterState(); // Update state
    return result.isAllowed();
}
```

### User Action Limiting
```java
// 5 actions per user, leaking 1 per minute
LeakyBucketRateLimiter userLimiter = RateLimiterFactory.createRateLimiter(5, 1.0/60);

public boolean canUserPerformAction(String userId) {
    double currentTime = System.currentTimeMillis() / 1000.0;
    RateLimiterResult result = userLimiter.allowRequest(userId, currentTime);
    userLimiter = result.getNewLimiterState();
    return result.isAllowed();
}
```
