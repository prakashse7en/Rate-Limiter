package org.example;

import org.example.model.BucketInfo;
import org.example.model.RateLimiterResult;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("Leaky Bucket Rate Limiter Tests")
public class LeakyBucketRateLimiterTest {

    @Nested
    @DisplayName("Basic Functionality Tests")
    class BasicFunctionalityTests {

        @Test
        @DisplayName("Should allow first request from new user")
        void shouldAllowFirstRequestFromNewUser() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            RateLimiterResult result = limiter.allowRequest("user1", 0.0);
            
            assertTrue(result.isAllowed(), "First request should be allowed");
            assertNotNull(result.getNewLimiterState(), "New limiter state should not be null");
            
            BucketInfo bucket = result.getNewLimiterState().getBucketState("user1");
            assertNotNull(bucket, "Bucket should exist after first request");
            assertEquals(1.0, bucket.getCurrentLevel(), 0.001, "Bucket level should be 1 after first request");
        }

        @Test
        @DisplayName("Should allow multiple requests within capacity")
        void shouldAllowMultipleRequestsWithinCapacity() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            double timestamp = 0.0;
            
            // Make 5 requests in quick succession
            for (int i = 1; i <= 5; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", timestamp);
                limiter = result.getNewLimiterState();
                
                assertTrue(result.isAllowed(), "Request " + i + " should be allowed");
                
                BucketInfo bucket = limiter.getBucketState("user1");
                assertEquals(i, bucket.getCurrentLevel(), 0.001, 
                    "Bucket level should be " + i + " after " + i + " requests");
            }
        }

        @Test
        @DisplayName("Should reject request when bucket is at capacity")
        void shouldRejectRequestWhenBucketAtCapacity() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(3, 1.0);
            double timestamp = 0.0;
            
            // Fill bucket to capacity
            for (int i = 0; i < 3; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", timestamp);
                limiter = result.getNewLimiterState();
                assertTrue(result.isAllowed());
            }
            
            // Next request should be rejected
            RateLimiterResult result = limiter.allowRequest("user1", timestamp);
            assertFalse(result.isAllowed(), "Request should be rejected when bucket is full");
            
            // Bucket level should remain at capacity
            BucketInfo bucket = result.getNewLimiterState().getBucketState("user1");
            assertEquals(3.0, bucket.getCurrentLevel(), 0.001, "Bucket level should remain at capacity");
        }
    }

    @Nested
    @DisplayName("Time-based Leaking Tests")
    class TimeBasedLeakingTests {

        @Test
        @DisplayName("Should leak over time at specified rate")
        void shouldLeakOverTimeAtSpecifiedRate() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 2.0); // 2 units/second
            
            // Fill bucket with 4 requests
            for (int i = 0; i < 4; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", 0.0);
                limiter = result.getNewLimiterState();
            }
            
            // Check bucket state after 1 second (should leak 2 units)
            BucketInfo bucketAfter1Sec = limiter.getBucketStateAtTime("user1", 1.0);
            assertEquals(2.0, bucketAfter1Sec.getCurrentLevel(), 0.001, 
                "Bucket should have 2 units after 1 second with leak rate 2.0/s");
            
            // Check bucket state after 2 seconds (should be empty)
            BucketInfo bucketAfter2Sec = limiter.getBucketStateAtTime("user1", 2.0);
            assertEquals(0.0, bucketAfter2Sec.getCurrentLevel(), 0.001, 
                "Bucket should be empty after 2 seconds");
        }

        @Test
        @DisplayName("Should allow requests after sufficient leaking")
        void shouldAllowRequestsAfterSufficientLeaking() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(3, 1.0);
            
            // Fill bucket to capacity at t=0
            for (int i = 0; i < 3; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", 0.0);
                limiter = result.getNewLimiterState();
            }
            
            // Try request immediately - should be rejected
            RateLimiterResult immediateResult = limiter.allowRequest("user1", 0.0);
            assertFalse(immediateResult.isAllowed());
            
            // Try request after 1 second - should be allowed (1 unit leaked)
            RateLimiterResult delayedResult = limiter.allowRequest("user1", 1.0);
            assertTrue(delayedResult.isAllowed(), "Request should be allowed after 1 second of leaking");
        }

        @Test
        @DisplayName("Should handle fractional time and leaking")
        void shouldHandleFractionalTimeAndLeaking() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 4.0); // 4 units/second
            
            // Add 4 requests at t=0
            for (int i = 0; i < 4; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", 0.0);
                limiter = result.getNewLimiterState();
            }
            
            // After 0.5 seconds, should leak 2 units (4 * 0.5)
            BucketInfo bucket = limiter.getBucketStateAtTime("user1", 0.5);
            assertEquals(2.0, bucket.getCurrentLevel(), 0.001, 
                "Should have 2 units remaining after 0.5 seconds");
        }
    }

    @Nested
    @DisplayName("Multiple Users Tests")
    class MultipleUsersTests {

        @Test
        @DisplayName("Should handle multiple users independently")
        void shouldHandleMultipleUsersIndependently() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(3, 1.0);
            
            // Fill user1's bucket
            for (int i = 0; i < 3; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", 0.0);
                limiter = result.getNewLimiterState();
            }
            
            // user2 should still be able to make requests
            RateLimiterResult user2Result = limiter.allowRequest("user2", 0.0);
            assertTrue(user2Result.isAllowed(), "user2 should be allowed even when user1 is at capacity");
            
            // Verify independent bucket states
            limiter = user2Result.getNewLimiterState();
            BucketInfo user1Bucket = limiter.getBucketState("user1");
            BucketInfo user2Bucket = limiter.getBucketState("user2");
            
            assertEquals(3.0, user1Bucket.getCurrentLevel(), 0.001);
            assertEquals(1.0, user2Bucket.getCurrentLevel(), 0.001);
        }

        @Test
        @DisplayName("Should maintain separate bucket states for different users")
        void shouldMaintainSeparateBucketStatesForDifferentUsers() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            // Different users making different numbers of requests
            limiter = limiter.allowRequest("user1", 0.0).getNewLimiterState(); // 1 request
            limiter = limiter.allowRequest("user2", 0.0).getNewLimiterState(); // 1 request
            limiter = limiter.allowRequest("user2", 0.0).getNewLimiterState(); // user2: 2 requests
            limiter = limiter.allowRequest("user3", 0.0).getNewLimiterState(); // 1 request
            
            assertEquals(1.0, limiter.getBucketState("user1").getCurrentLevel(), 0.001);
            assertEquals(2.0, limiter.getBucketState("user2").getCurrentLevel(), 0.001);
            assertEquals(1.0, limiter.getBucketState("user3").getCurrentLevel(), 0.001);
        }
    }

    @Nested
    @DisplayName("Edge Cases Tests")
    class EdgeCasesTests {

        @Test
        @DisplayName("Should handle backwards time gracefully")
        void shouldHandleBackwardsTimeGracefully() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            // Make request at t=10
            RateLimiterResult result1 = limiter.allowRequest("user1", 10.0);
            limiter = result1.getNewLimiterState();
            
            // Make request at t=5 (backwards)
            RateLimiterResult result2 = limiter.allowRequest("user1", 5.0);
            assertTrue(result2.isAllowed(), "Request should be allowed even with backwards time");
            
            // Bucket level should not decrease due to backwards time
            BucketInfo bucket = result2.getNewLimiterState().getBucketState("user1");
            assertEquals(2.0, bucket.getCurrentLevel(), 0.001, 
                "Bucket level should increase normally despite backwards time");
        }

        @Test
        @DisplayName("Should handle very large time gaps")
        void shouldHandleVeryLargeTimeGaps() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            // Fill bucket
            for (int i = 0; i < 5; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", 0.0);
                limiter = result.getNewLimiterState();
            }
            
            // Wait a very long time (should completely empty bucket)
            BucketInfo bucket = limiter.getBucketStateAtTime("user1", 1000000.0);
            assertEquals(0.0, bucket.getCurrentLevel(), 0.001, 
                "Bucket should be empty after very long time gap");
        }

        @Test
        @DisplayName("Should handle zero leak rate edge case")
        void shouldHandleZeroLeakRateEdgeCase() {
            // Note: Constructor prevents zero leak rate, but test the concept
            assertThrows(IllegalArgumentException.class, () -> {
                RateLimiterFactory.createRateLimiter(5, 0.0);
            }, "Should throw exception for zero leak rate");
        }

        @Test
        @DisplayName("Should handle null or empty user ID")
        void shouldHandleNullOrEmptyUserId() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            assertThrows(IllegalArgumentException.class, () -> {
                limiter.allowRequest(null, 0.0);
            }, "Should throw exception for null user ID");
            
            assertThrows(IllegalArgumentException.class, () -> {
                limiter.allowRequest("", 0.0);
            }, "Should throw exception for empty user ID");
            
            assertThrows(IllegalArgumentException.class, () -> {
                limiter.allowRequest("   ", 0.0);
            }, "Should throw exception for whitespace-only user ID");
        }

        @Test
        @DisplayName("Should handle negative timestamps")
        void shouldHandleNegativeTimestamps() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            assertThrows(IllegalArgumentException.class, () -> {
                limiter.allowRequest("user1", -1.0);
            }, "Should throw exception for negative timestamp");
        }
    }

    @Nested
    @DisplayName("Burst Handling Tests")
    class BurstHandlingTests {

        @Test
        @DisplayName("Should reject burst requests exceeding capacity")
        void shouldRejectBurstRequestsExceedingCapacity() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(3, 1.0);
            double timestamp = 0.0;
            
            int allowedRequests = 0;
            int rejectedRequests = 0;
            
            // Try to make 10 rapid requests
            for (int i = 0; i < 10; i++) {
                RateLimiterResult result = limiter.allowRequest("user1", timestamp);
                limiter = result.getNewLimiterState();
                
                if (result.isAllowed()) {
                    allowedRequests++;
                } else {
                    rejectedRequests++;
                }
            }
            
            assertEquals(3, allowedRequests, "Should allow exactly 3 requests (capacity)");
            assertEquals(7, rejectedRequests, "Should reject 7 requests");
        }

        @Test
        @DisplayName("Should handle burst followed by normal requests after leaking")
        void shouldHandleBurstFollowedByNormalRequestsAfterLeaking() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(2, 1.0);
            
            // Initial burst - fill bucket
            RateLimiterResult result1 = limiter.allowRequest("user1", 0.0);
            limiter = result1.getNewLimiterState();
            assertTrue(result1.isAllowed());
            
            RateLimiterResult result2 = limiter.allowRequest("user1", 0.0);
            limiter = result2.getNewLimiterState();
            assertTrue(result2.isAllowed());
            
            // Third request should be rejected
            RateLimiterResult result3 = limiter.allowRequest("user1", 0.0);
            limiter = result3.getNewLimiterState();
            assertFalse(result3.isAllowed());
            
            // After 1 second, one unit should have leaked - allow one more request
            RateLimiterResult result4 = limiter.allowRequest("user1", 1.0);
            assertTrue(result4.isAllowed(), "Should allow request after leaking");
        }
    }

    @Nested
    @DisplayName("TTL and Cache Expiry Tests")
    class TTLAndCacheExpiryTests {

        @Test
        @DisplayName("Should create limiter with custom TTL")
        void shouldCreateLimiterWithCustomTTL() {
            Duration customTTL = Duration.ofMinutes(1);
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0, customTTL);
            
            assertNotNull(limiter, "Should create limiter with custom TTL");
        }

        @Test
        @DisplayName("Should handle TTL expiry")
        void shouldHandleTTLExpiry() throws InterruptedException {
            // Create limiter with very short TTL for testing
            Duration shortTTL = Duration.ofMinutes(1);
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0, shortTTL);
            
            // Make request
            RateLimiterResult result = limiter.allowRequest("user1", 0.0);
            limiter = result.getNewLimiterState();
            
            // Verify bucket exists
            assertNotNull(limiter.getBucketState("user1"));
            
            // Wait for TTL to expire
            Thread.sleep(70000);
            limiter.cleanUp(); // Trigger cache cleanup
            
            // Bucket should be expired and removed
            assertNull(limiter.getBucketState("user1"), "Bucket should be expired after TTL");
        }

        @Test
        @DisplayName("Should track active bucket count")
        void shouldTrackActiveBucketCount() {
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            assertEquals(0, limiter.getActiveBucketCount(), "Should start with 0 buckets");
            
            RateLimiterResult result1 = limiter.allowRequest("user1", 0.0);
            limiter = result1.getNewLimiterState();
            assertTrue(limiter.getActiveBucketCount() >= 1, "Should have at least 1 bucket");
            
            RateLimiterResult result2 = limiter.allowRequest("user2", 0.0);
            limiter = result2.getNewLimiterState();
            assertTrue(limiter.getActiveBucketCount() >= 2, "Should have at least 2 buckets");
        }
    }

    @Nested
    @DisplayName("Constructor Validation Tests")
    class ConstructorValidationTests {

        @ParameterizedTest
        @ValueSource(ints = {0, -1, -10})
        @DisplayName("Should reject non-positive capacity")
        void shouldRejectNonPositiveCapacity(int capacity) {
            assertThrows(IllegalArgumentException.class, () -> {
                RateLimiterFactory.createRateLimiter(capacity, 1.0);
            }, "Should throw exception for capacity: " + capacity);
        }

        @ParameterizedTest
        @ValueSource(doubles = {0.0, -1.0, -0.1})
        @DisplayName("Should reject non-positive leak rate")
        void shouldRejectNonPositiveLeakRate(double leakRate) {
            assertThrows(IllegalArgumentException.class, () -> {
                RateLimiterFactory.createRateLimiter(5, leakRate);
            }, "Should throw exception for leak rate: " + leakRate);
        }

        @Test
        @DisplayName("Should reject null TTL")
        void shouldRejectNullTTL() {
            assertThrows(IllegalArgumentException.class, () -> {
                RateLimiterFactory.createRateLimiter(5, 1.0, null);
            }, "Should throw exception for null TTL");
        }

        @Test
        @DisplayName("Should reject zero or negative TTL")
        void shouldRejectZeroOrNegativeTTL() {
            assertThrows(IllegalArgumentException.class, () -> {
                RateLimiterFactory.createRateLimiter(5, 1.0, Duration.ZERO);
            }, "Should throw exception for zero TTL");
            
            assertThrows(IllegalArgumentException.class, () -> {
                RateLimiterFactory.createRateLimiter(5, 1.0, Duration.ofSeconds(-1));
            }, "Should throw exception for negative TTL");
        }
    }

    @Nested
    @DisplayName("Integration Tests")
    class IntegrationTests {

        @Test
        @DisplayName("Should handle realistic usage scenario")
        void shouldHandleRealisticUsageScenario() {
            // Scenario: API with 100 requests/minute capacity, leaking at 1.67 req/sec
            LeakyBucketRateLimiter limiter = RateLimiterFactory.createRateLimiter(100, 1.67);
            
            double currentTime = 0.0;
            
            // Simulate 50 requests in first second (should all be allowed)
            for (int i = 0; i < 50; i++) {
                RateLimiterResult result = limiter.allowRequest("api_user", currentTime);
                limiter = result.getNewLimiterState();
                assertTrue(result.isAllowed(), "Request " + (i+1) + " should be allowed");
            }
            
            // Simulate another 50 requests (should all be allowed)
            for (int i = 0; i < 50; i++) {
                RateLimiterResult result = limiter.allowRequest("api_user", currentTime);
                limiter = result.getNewLimiterState();
                assertTrue(result.isAllowed(), "Request " + (i+51) + " should be allowed");
            }
            
            // 101st request should be rejected
            RateLimiterResult result = limiter.allowRequest("api_user", currentTime);
            assertFalse(result.isAllowed(), "101st request should be rejected");
            
            // After 1 second, should leak ~1.67 units, allowing 1 more request
            currentTime = 1.0;
            result = limiter.allowRequest("api_user", currentTime);
            assertTrue(result.isAllowed(), "Should allow request after 1 second of leaking");
        }

        @Test
        @DisplayName("Should demonstrate functional immutability")
        void shouldDemonstrateFunctionalImmutability() {
            LeakyBucketRateLimiter originalLimiter = RateLimiterFactory.createRateLimiter(5, 1.0);
            
            // Make request and get new state
            RateLimiterResult result = originalLimiter.allowRequest("user1", 0.0);
            LeakyBucketRateLimiter newLimiter = result.getNewLimiterState();
            
            // Original limiter should be unchanged
            assertNull(originalLimiter.getBucketState("user1"), 
                "Original limiter should not have bucket state");
            
            // New limiter should have the updated state
            assertNotNull(newLimiter.getBucketState("user1"), 
                "New limiter should have bucket state");
            
            // They should be different instances
            assertNotSame(originalLimiter, newLimiter, 
                "Should return different limiter instances");
        }
    }
}