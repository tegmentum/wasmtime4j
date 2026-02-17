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

package ai.tegmentum.wasmtime4j.jni.wasi.clocks;

import static org.junit.jupiter.api.Assertions.assertEquals;

import ai.tegmentum.wasmtime4j.wasi.clocks.DateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link JniWasiWallClock} class.
 *
 * <p>JniWasiWallClock provides JNI-based access to WASI Preview 2 wall clock operations.
 */
@DisplayName("JniWasiWallClock Tests")
class JniWasiWallClockTest {

  @Nested
  @DisplayName("Return Type Compatibility Tests")
  class ReturnTypeCompatibilityTests {

    @Test
    @DisplayName("DateTime should be valid for wall clock operations")
    void dateTimeShouldBeValidForWallClockOperations() {
      // Verify DateTime can be constructed with valid values
      final DateTime dt = new DateTime(1234567890L, 500000000);
      assertEquals(1234567890L, dt.getSeconds(), "Seconds should match");
      assertEquals(500000000, dt.getNanoseconds(), "Nanoseconds should match");
    }

    @Test
    @DisplayName("DateTime should represent resolution correctly")
    void dateTimeShouldRepresentResolutionCorrectly() {
      // Common resolution is 1 nanosecond (0 seconds, 1 nanosecond)
      final DateTime resolution = new DateTime(0L, 1);
      assertEquals(0L, resolution.getSeconds(), "Resolution seconds should be 0");
      assertEquals(1, resolution.getNanoseconds(), "Resolution nanoseconds should be 1");
    }
  }
}
