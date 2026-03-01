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

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ResourceLimiter interface.
 *
 * <p>Validates that the interface defines the correct API surface for dynamic callback-based
 * resource limiting.
 */
@DisplayName("ResourceLimiter Interface Tests")
class ResourceLimiterTest {

  @Nested
  @DisplayName("Default Method Behavior Tests")
  class DefaultMethodBehaviorTests {

    @Test
    @DisplayName("memoryGrowFailed default should not throw")
    void memoryGrowFailedDefaultShouldNotThrow() {
      ResourceLimiter limiter =
          new ResourceLimiter() {
            @Override
            public boolean memoryGrowing(
                final long currentBytes, final long desiredBytes, final long maximumBytes) {
              return true;
            }

            @Override
            public boolean tableGrowing(
                final int currentElements, final int desiredElements, final int maximumElements) {
              return true;
            }
          };

      // Should not throw - default implementation is no-op
      limiter.memoryGrowFailed("test error");
      limiter.tableGrowFailed("test error");
    }
  }
}
