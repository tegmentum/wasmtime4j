/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ResourceLimiterAsync interface.
 *
 * <p>Validates that the interface defines the correct async API surface for dynamic callback-based
 * resource limiting using CompletableFuture.
 */
@DisplayName("ResourceLimiterAsync Interface Tests")
class ResourceLimiterAsyncTest {

  /**
   * Creates a permissive ResourceLimiterAsync that allows all growth requests.
   *
   * @return a ResourceLimiterAsync that always returns true
   */
  private ResourceLimiterAsync createPermissiveLimiter() {
    return new ResourceLimiterAsync() {
      @Override
      public CompletableFuture<Boolean> memoryGrowing(
          final long currentBytes, final long desiredBytes, final long maximumBytes) {
        return CompletableFuture.completedFuture(true);
      }

      @Override
      public CompletableFuture<Boolean> tableGrowing(
          final int currentElements, final int desiredElements, final int maximumElements) {
        return CompletableFuture.completedFuture(true);
      }
    };
  }

  @Nested
  @DisplayName("Default Method Behavior Tests")
  class DefaultMethodBehaviorTests {

    @Test
    @DisplayName("memoryGrowFailed default should not throw")
    void memoryGrowFailedDefaultShouldNotThrow() {
      ResourceLimiterAsync limiter = createPermissiveLimiter();

      // Should not throw - default implementation is no-op
      limiter.memoryGrowFailed("test error: out of memory");
    }

    @Test
    @DisplayName("tableGrowFailed default should not throw")
    void tableGrowFailedDefaultShouldNotThrow() {
      ResourceLimiterAsync limiter = createPermissiveLimiter();

      // Should not throw - default implementation is no-op
      limiter.tableGrowFailed("test error: table limit exceeded");
    }

    @Test
    @DisplayName("memoryGrowFailed default should accept null error")
    void memoryGrowFailedDefaultShouldAcceptNull() {
      ResourceLimiterAsync limiter = createPermissiveLimiter();

      // Default no-op should handle null gracefully
      limiter.memoryGrowFailed(null);
    }

    @Test
    @DisplayName("tableGrowFailed default should accept null error")
    void tableGrowFailedDefaultShouldAcceptNull() {
      ResourceLimiterAsync limiter = createPermissiveLimiter();

      // Default no-op should handle null gracefully
      limiter.tableGrowFailed(null);
    }
  }

  @Nested
  @DisplayName("memoryGrowing Async Tests")
  class MemoryGrowingAsyncTests {

    @Test
    @DisplayName("memoryGrowing should allow growth when under limit")
    void memoryGrowingShouldAllowGrowthWhenUnderLimit()
        throws ExecutionException, InterruptedException {
      long maxAllowed = 10L * 1024 * 1024; // 10 MB
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(desiredBytes <= maxAllowed);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }
          };

      CompletableFuture<Boolean> result = limiter.memoryGrowing(0L, 65536L, Long.MAX_VALUE);
      assertNotNull(result, "memoryGrowing should return a non-null CompletableFuture");
      assertTrue(
          result.get(),
          "memoryGrowing should allow growth from 0 to 65536 bytes (under 10 MB limit)");
    }

    @Test
    @DisplayName("memoryGrowing should deny growth when over limit")
    void memoryGrowingShouldDenyGrowthWhenOverLimit()
        throws ExecutionException, InterruptedException {
      long maxAllowed = 10L * 1024 * 1024; // 10 MB
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(desiredBytes <= maxAllowed);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }
          };

      long overLimit = 20L * 1024 * 1024; // 20 MB
      CompletableFuture<Boolean> result =
          limiter.memoryGrowing(maxAllowed, overLimit, Long.MAX_VALUE);
      assertNotNull(result, "memoryGrowing should return a non-null CompletableFuture");
      assertFalse(
          result.get(),
          "memoryGrowing should deny growth from 10 MB to 20 MB (exceeds 10 MB limit)");
    }

    @Test
    @DisplayName("memoryGrowing should receive correct parameter values")
    void memoryGrowingShouldReceiveCorrectParameters()
        throws ExecutionException, InterruptedException {
      long expectedCurrent = 65536L;
      long expectedDesired = 131072L;
      long expectedMaximum = 1048576L;

      long[] capturedParams = new long[3];
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              capturedParams[0] = currentBytes;
              capturedParams[1] = desiredBytes;
              capturedParams[2] = maximumBytes;
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }
          };

      limiter.memoryGrowing(expectedCurrent, expectedDesired, expectedMaximum).get();

      assertEquals(
          expectedCurrent,
          capturedParams[0],
          "currentBytes parameter should be " + expectedCurrent + " but was " + capturedParams[0]);
      assertEquals(
          expectedDesired,
          capturedParams[1],
          "desiredBytes parameter should be " + expectedDesired + " but was " + capturedParams[1]);
      assertEquals(
          expectedMaximum,
          capturedParams[2],
          "maximumBytes parameter should be " + expectedMaximum + " but was " + capturedParams[2]);
    }

    @Test
    @DisplayName("memoryGrowing should handle Long.MAX_VALUE for unbounded maximum")
    void memoryGrowingShouldHandleUnboundedMaximum()
        throws ExecutionException, InterruptedException {
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(maximumBytes == Long.MAX_VALUE);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }
          };

      CompletableFuture<Boolean> result = limiter.memoryGrowing(0L, 65536L, Long.MAX_VALUE);
      assertTrue(
          result.get(),
          "memoryGrowing should correctly handle Long.MAX_VALUE as unbounded maximum");
    }
  }

  @Nested
  @DisplayName("tableGrowing Async Tests")
  class TableGrowingAsyncTests {

    @Test
    @DisplayName("tableGrowing should allow growth when under limit")
    void tableGrowingShouldAllowGrowthWhenUnderLimit()
        throws ExecutionException, InterruptedException {
      int maxElements = 10000;
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(desiredElements <= maxElements);
            }
          };

      CompletableFuture<Boolean> result = limiter.tableGrowing(0, 100, Integer.MAX_VALUE);
      assertNotNull(result, "tableGrowing should return a non-null CompletableFuture");
      assertTrue(
          result.get(),
          "tableGrowing should allow growth from 0 to 100 elements (under 10000 limit)");
    }

    @Test
    @DisplayName("tableGrowing should deny growth when over limit")
    void tableGrowingShouldDenyGrowthWhenOverLimit()
        throws ExecutionException, InterruptedException {
      int maxElements = 10000;
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(desiredElements <= maxElements);
            }
          };

      CompletableFuture<Boolean> result =
          limiter.tableGrowing(maxElements, 20000, Integer.MAX_VALUE);
      assertNotNull(result, "tableGrowing should return a non-null CompletableFuture");
      assertFalse(
          result.get(),
          "tableGrowing should deny growth from 10000 to 20000 elements (exceeds limit)");
    }

    @Test
    @DisplayName("tableGrowing should receive correct parameter values")
    void tableGrowingShouldReceiveCorrectParameters()
        throws ExecutionException, InterruptedException {
      int expectedCurrent = 50;
      int expectedDesired = 100;
      int expectedMaximum = 500;

      int[] capturedParams = new int[3];
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              capturedParams[0] = currentElements;
              capturedParams[1] = desiredElements;
              capturedParams[2] = maximumElements;
              return CompletableFuture.completedFuture(true);
            }
          };

      limiter.tableGrowing(expectedCurrent, expectedDesired, expectedMaximum).get();

      assertEquals(
          expectedCurrent,
          capturedParams[0],
          "currentElements parameter should be "
              + expectedCurrent
              + " but was "
              + capturedParams[0]);
      assertEquals(
          expectedDesired,
          capturedParams[1],
          "desiredElements parameter should be "
              + expectedDesired
              + " but was "
              + capturedParams[1]);
      assertEquals(
          expectedMaximum,
          capturedParams[2],
          "maximumElements parameter should be "
              + expectedMaximum
              + " but was "
              + capturedParams[2]);
    }

    @Test
    @DisplayName("tableGrowing should handle Integer.MAX_VALUE for unbounded maximum")
    void tableGrowingShouldHandleUnboundedMaximum()
        throws ExecutionException, InterruptedException {
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(maximumElements == Integer.MAX_VALUE);
            }
          };

      CompletableFuture<Boolean> result = limiter.tableGrowing(0, 10, Integer.MAX_VALUE);
      assertTrue(
          result.get(),
          "tableGrowing should correctly handle Integer.MAX_VALUE as unbounded maximum");
    }
  }

  @Nested
  @DisplayName("Custom Failure Callback Tests")
  class CustomFailureCallbackTests {

    @Test
    @DisplayName("custom memoryGrowFailed should capture error message")
    void customMemoryGrowFailedShouldCaptureError() {
      List<String> capturedErrors = new ArrayList<>();
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public void memoryGrowFailed(final String error) {
              capturedErrors.add(error);
            }
          };

      String errorMsg = "allocation failed: system out of memory";
      limiter.memoryGrowFailed(errorMsg);

      assertEquals(
          1,
          capturedErrors.size(),
          "memoryGrowFailed should have been called exactly once, but was called "
              + capturedErrors.size()
              + " times");
      assertEquals(
          errorMsg,
          capturedErrors.get(0),
          "memoryGrowFailed should capture the exact error message '"
              + errorMsg
              + "' but got '"
              + capturedErrors.get(0)
              + "'");
    }

    @Test
    @DisplayName("custom tableGrowFailed should capture error message")
    void customTableGrowFailedShouldCaptureError() {
      List<String> capturedErrors = new ArrayList<>();
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public void tableGrowFailed(final String error) {
              capturedErrors.add(error);
            }
          };

      String errorMsg = "table growth failed: internal error";
      limiter.tableGrowFailed(errorMsg);

      assertEquals(
          1,
          capturedErrors.size(),
          "tableGrowFailed should have been called exactly once, but was called "
              + capturedErrors.size()
              + " times");
      assertEquals(
          errorMsg,
          capturedErrors.get(0),
          "tableGrowFailed should capture the exact error message '"
              + errorMsg
              + "' but got '"
              + capturedErrors.get(0)
              + "'");
    }
  }

  @Nested
  @DisplayName("Async CompletableFuture Behavior Tests")
  class AsyncCompletableFutureBehaviorTests {

    @Test
    @DisplayName("memoryGrowing should work with supplyAsync for true async execution")
    void memoryGrowingShouldWorkWithSupplyAsync() throws ExecutionException, InterruptedException {
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.supplyAsync(() -> desiredBytes <= 1048576L);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }
          };

      CompletableFuture<Boolean> result = limiter.memoryGrowing(0L, 65536L, Long.MAX_VALUE);
      assertNotNull(
          result, "memoryGrowing with supplyAsync should return a non-null CompletableFuture");
      assertTrue(
          result.get(),
          "memoryGrowing with supplyAsync should allow growth of 65536 bytes (under 1 MB limit)");
    }

    @Test
    @DisplayName("tableGrowing should work with supplyAsync for true async execution")
    void tableGrowingShouldWorkWithSupplyAsync() throws ExecutionException, InterruptedException {
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(true);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.supplyAsync(() -> desiredElements <= 5000);
            }
          };

      CompletableFuture<Boolean> result = limiter.tableGrowing(0, 100, Integer.MAX_VALUE);
      assertNotNull(
          result, "tableGrowing with supplyAsync should return a non-null CompletableFuture");
      assertTrue(
          result.get(),
          "tableGrowing with supplyAsync should allow growth to 100 elements (under 5000 limit)");
    }

    @Test
    @DisplayName("memoryGrowing should support chaining with thenApply")
    void memoryGrowingShouldSupportChaining() throws ExecutionException, InterruptedException {
      ResourceLimiterAsync limiter =
          new ResourceLimiterAsync() {
            @Override
            public CompletableFuture<Boolean> memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return CompletableFuture.completedFuture(desiredBytes)
                  .thenApply(desired -> desired <= 1048576L);
            }

            @Override
            public CompletableFuture<Boolean> tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return CompletableFuture.completedFuture(true);
            }
          };

      CompletableFuture<Boolean> allowed = limiter.memoryGrowing(0L, 512L, Long.MAX_VALUE);
      assertTrue(
          allowed.get(),
          "Chained memoryGrowing should allow growth of 512 bytes (under 1 MB limit)");

      CompletableFuture<Boolean> denied = limiter.memoryGrowing(0L, 2097152L, Long.MAX_VALUE);
      assertFalse(
          denied.get(), "Chained memoryGrowing should deny growth of 2 MB (exceeds 1 MB limit)");
    }
  }
}
