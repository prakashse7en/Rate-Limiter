package org.example;

import org.example.model.BucketInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("BucketInfo Tests")
public class BucketInfoTest {

    @Nested
    @DisplayName("Constructor Tests")
    class ConstructorTests {

        @Test
        @DisplayName("Should create bucket with valid parameters")
        void shouldCreateBucketWithValidParameters() {
            BucketInfo bucket = new BucketInfo(2.5, 10, 1.0, 5.0);
            
            assertEquals(2.5, bucket.getCurrentLevel(), 0.001);
            assertEquals(10, bucket.getCapacity());
            assertEquals(1.0, bucket.getLeakRate(), 0.001);
            assertEquals(5.0, bucket.getLastUpdateTime(), 0.001);
        }

        @Test
        @DisplayName("Should handle negative current level by setting to zero")
        void shouldHandleNegativeCurrentLevelBySettingToZero() {
            BucketInfo bucket = new BucketInfo(-1.0, 10, 1.0, 5.0);
            
            assertEquals(0.0, bucket.getCurrentLevel(), 0.001, 
                "Negative current level should be set to 0");
        }
    }

    @Nested
    @DisplayName("Leaking Tests")
    class LeakingTests {

        @Test
        @DisplayName("Should leak correctly with positive time elapsed")
        void shouldLeakCorrectlyWithPositiveTimeElapsed() {
            BucketInfo bucket = new BucketInfo(5.0, 10, 2.0, 0.0);
            
            BucketInfo leaked = bucket.leak(2.5); // 2.5 seconds elapsed
            
            assertEquals(0.0, leaked.getCurrentLevel(), 0.001, 
                "Should leak 5.0 units (2.0 * 2.5 seconds)");
            assertEquals(2.5, leaked.getLastUpdateTime(), 0.001, 
                "Last update time should be updated");
        }

        @Test
        @DisplayName("Should not leak below zero")
        void shouldNotLeakBelowZero() {
            BucketInfo bucket = new BucketInfo(3.0, 10, 2.0, 0.0);
            
            BucketInfo leaked = bucket.leak(10.0); // Should leak way more than available
            
            assertEquals(0.0, leaked.getCurrentLevel(), 0.001, 
                "Should not leak below zero");
        }

        @Test
        @DisplayName("Should handle backwards time by not leaking")
        void shouldHandleBackwardsTimeByNotLeaking() {
            BucketInfo bucket = new BucketInfo(5.0, 10, 2.0, 10.0);
            
            BucketInfo leaked = bucket.leak(5.0); // Go back 5 seconds
            
            assertEquals(5.0, leaked.getCurrentLevel(), 0.001, 
                "Should not leak with backwards time");
            assertEquals(10.0, leaked.getLastUpdateTime(), 0.001, 
                "Last update time should remain unchanged");
        }

        @Test
        @DisplayName("Should handle same time by not leaking")
        void shouldHandleSameTimeByNotLeaking() {
            BucketInfo bucket = new BucketInfo(3.0, 10, 2.0, 5.0);
            
            BucketInfo leaked = bucket.leak(5.0); // Same time
            
            assertEquals(3.0, leaked.getCurrentLevel(), 0.001, 
                "Should not leak with same time");
            assertEquals(5.0, leaked.getLastUpdateTime(), 0.001, 
                "Last update time should remain unchanged");
        }

        @Test
        @DisplayName("Should handle fractional leaking")
        void shouldHandleFractionalLeaking() {
            BucketInfo bucket = new BucketInfo(4.0, 10, 3.0, 0.0);
            
            BucketInfo leaked = bucket.leak(0.5); // 0.5 seconds
            
            assertEquals(2.5, leaked.getCurrentLevel(), 0.001, 
                "Should leak 1.5 units (3.0 * 0.5)");
        }
    }

    @Nested
    @DisplayName("Add Request Tests")
    class AddRequestTests {

        @Test
        @DisplayName("Should add request when bucket has capacity")
        void shouldAddRequestWhenBucketHasCapacity() {
            BucketInfo bucket = new BucketInfo(3.0, 5, 1.0, 0.0);
            
            BucketInfo updated = bucket.addRequest();
            
            assertNotNull(updated, "Should return updated bucket");
            assertEquals(4.0, updated.getCurrentLevel(), 0.001, 
                "Level should increase by 1");
        }

        @Test
        @DisplayName("Should reject request when bucket is at capacity")
        void shouldRejectRequestWhenBucketAtCapacity() {
            BucketInfo bucket = new BucketInfo(5.0, 5, 1.0, 0.0);
            
            BucketInfo updated = bucket.addRequest();
            
            assertNull(updated, "Should return null when bucket is full");
        }

        @Test
        @DisplayName("Should reject request when bucket would overflow")
        void shouldRejectRequestWhenBucketWouldOverflow() {
            BucketInfo bucket = new BucketInfo(4.9, 5, 1.0, 0.0);
            
            BucketInfo updated = bucket.addRequest();
            
            assertNotNull(updated, "Should accept request just under capacity");
            assertEquals(5.9, updated.getCurrentLevel(), 0.001);
            
            // But next request should be rejected
            BucketInfo nextUpdate = updated.addRequest();
            assertNull(nextUpdate, "Should reject request that would exceed capacity");
        }
    }

    @Nested
    @DisplayName("Capacity Check Tests")
    class CapacityCheckTests {

        @Test
        @DisplayName("Should correctly identify when request can be accepted")
        void shouldCorrectlyIdentifyWhenRequestCanBeAccepted() {
            BucketInfo bucket = new BucketInfo(3.0, 5, 1.0, 0.0);
            
            assertTrue(bucket.canAcceptRequest(), "Should accept request when under capacity");
        }

        @Test
        @DisplayName("Should correctly identify when request cannot be accepted")
        void shouldCorrectlyIdentifyWhenRequestCannotBeAccepted() {
            BucketInfo bucket = new BucketInfo(5.0, 5, 1.0, 0.0);
            
            assertFalse(bucket.canAcceptRequest(), "Should reject request when at capacity");
        }

        @Test
        @DisplayName("Should calculate available capacity correctly")
        void shouldCalculateAvailableCapacityCorrectly() {
            BucketInfo bucket = new BucketInfo(3.5, 10, 1.0, 0.0);
            
            assertEquals(6.5, bucket.getAvailableCapacity(), 0.001, 
                "Available capacity should be capacity - current level");
        }
    }

    @Nested
    @DisplayName("Equality and Hash Tests")
    class EqualityAndHashTests {

        @Test
        @DisplayName("Should be equal when all fields match")
        void shouldBeEqualWhenAllFieldsMatch() {
            BucketInfo bucket1 = new BucketInfo(3.0, 5, 1.0, 10.0);
            BucketInfo bucket2 = new BucketInfo(3.0, 5, 1.0, 10.0);
            
            assertEquals(bucket1, bucket2, "Buckets with same values should be equal");
            assertEquals(bucket1.hashCode(), bucket2.hashCode(), 
                "Equal buckets should have same hash code");
        }

        @Test
        @DisplayName("Should not be equal when fields differ")
        void shouldNotBeEqualWhenFieldsDiffer() {
            BucketInfo bucket1 = new BucketInfo(3.0, 5, 1.0, 10.0);
            BucketInfo bucket2 = new BucketInfo(4.0, 5, 1.0, 10.0); // Different level
            BucketInfo bucket3 = new BucketInfo(3.0, 6, 1.0, 10.0); // Different capacity
            BucketInfo bucket4 = new BucketInfo(3.0, 5, 2.0, 10.0); // Different leak rate
            BucketInfo bucket5 = new BucketInfo(3.0, 5, 1.0, 11.0); // Different timestamp
            
            assertNotEquals(bucket1, bucket2, "Should not be equal with different level");
            assertNotEquals(bucket1, bucket3, "Should not be equal with different capacity");
            assertNotEquals(bucket1, bucket4, "Should not be equal with different leak rate");
            assertNotEquals(bucket1, bucket5, "Should not be equal with different timestamp");
        }

        @Test
        @DisplayName("Should handle null and different types in equals")
        void shouldHandleNullAndDifferentTypesInEquals() {
            BucketInfo bucket = new BucketInfo(3.0, 5, 1.0, 10.0);
            
            assertNotEquals(bucket, null, "Should not be equal to null");
            assertNotEquals(bucket, "string", "Should not be equal to different type");
            assertEquals(bucket, bucket, "Should be equal to itself");
        }
    }

    @Nested
    @DisplayName("String Representation Tests")
    class StringRepresentationTests {

        @Test
        @DisplayName("Should provide meaningful string representation")
        void shouldProvideMeaningfulStringRepresentation() {
            BucketInfo bucket = new BucketInfo(3.75, 10, 2.5, 15.25);
            
            String str = bucket.toString();
            
            assertTrue(str.contains("3.75"), "Should contain current level");
            assertTrue(str.contains("10"), "Should contain capacity");
            assertTrue(str.contains("2.50"), "Should contain leak rate");
            assertTrue(str.contains("15.25"), "Should contain last update time");
        }
    }
}