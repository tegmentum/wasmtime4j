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

package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Modifier;
import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NetworkOptimizationResult} class.
 *
 * <p>NetworkOptimizationResult contains metrics and outcomes from network optimization attempts.
 * Has a 3-param success constructor, a 5-param full constructor, and a failure() static factory.
 * No equals/hashCode.
 */
@DisplayName("NetworkOptimizationResult Tests")
class NetworkOptimizationResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(NetworkOptimizationResult.class.getModifiers()),
          "NetworkOptimizationResult should be public");
      assertTrue(
          Modifier.isFinal(NetworkOptimizationResult.class.getModifiers()),
          "NetworkOptimizationResult should be final");
    }
  }

  @Nested
  @DisplayName("Three-Parameter Constructor Tests")
  class ThreeParamConstructorTests {

    @Test
    @DisplayName("should create successful result with 3 parameters")
    void shouldCreateSuccessfulResultWithThreeParams() {
      final NetworkOptimizationResult result =
          new NetworkOptimizationResult(1024L, Duration.ofMillis(50), "compression");

      assertTrue(result.isSuccessful(), "3-param constructor should create successful result");
      assertEquals(1024L, result.getBytesReduced(), "Bytes reduced should be 1024");
      assertEquals(Duration.ofMillis(50), result.getTimeSaved(),
          "Time saved should be 50ms");
      assertEquals("compression", result.getOptimizationStrategy(),
          "Strategy should be 'compression'");
      assertNull(result.getErrorMessage(),
          "Error message should be null for successful result");
    }

    @Test
    @DisplayName("should handle zero bytes reduced")
    void shouldHandleZeroBytesReduced() {
      final NetworkOptimizationResult result =
          new NetworkOptimizationResult(0L, Duration.ofMillis(10), "caching");

      assertTrue(result.isSuccessful(), "Result should be successful");
      assertEquals(0L, result.getBytesReduced(), "Bytes reduced should be 0");
    }

    @Test
    @DisplayName("should throw NullPointerException for null timeSaved")
    void shouldThrowNpeForNullTimeSaved() {
      assertThrows(
          NullPointerException.class,
          () -> new NetworkOptimizationResult(100L, null, "strategy"),
          "Null timeSaved should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null optimizationStrategy")
    void shouldThrowNpeForNullStrategy() {
      assertThrows(
          NullPointerException.class,
          () -> new NetworkOptimizationResult(100L, Duration.ofMillis(10), null),
          "Null strategy should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Five-Parameter Constructor Tests")
  class FiveParamConstructorTests {

    @Test
    @DisplayName("should create result with all five parameters")
    void shouldCreateResultWithAllFiveParams() {
      final NetworkOptimizationResult result = new NetworkOptimizationResult(
          true, 2048L, Duration.ofSeconds(1), "deduplication", null);

      assertTrue(result.isSuccessful(), "Result should be successful");
      assertEquals(2048L, result.getBytesReduced(), "Bytes reduced should be 2048");
      assertEquals(Duration.ofSeconds(1), result.getTimeSaved(),
          "Time saved should be 1 second");
      assertEquals("deduplication", result.getOptimizationStrategy(),
          "Strategy should be 'deduplication'");
      assertNull(result.getErrorMessage(), "Error message should be null");
    }

    @Test
    @DisplayName("should create failed result with error message")
    void shouldCreateFailedResultWithErrorMessage() {
      final NetworkOptimizationResult result = new NetworkOptimizationResult(
          false, 0L, Duration.ZERO, "none", "Network timeout");

      assertFalse(result.isSuccessful(), "Result should not be successful");
      assertEquals(0L, result.getBytesReduced(), "Bytes reduced should be 0");
      assertEquals("Network timeout", result.getErrorMessage(),
          "Error message should be 'Network timeout'");
    }

    @Test
    @DisplayName("should allow nullable errorMessage")
    void shouldAllowNullableErrorMessage() {
      final NetworkOptimizationResult result = new NetworkOptimizationResult(
          true, 512L, Duration.ofMillis(100), "minification", null);

      assertNull(result.getErrorMessage(), "Error message should be null");
    }

    @Test
    @DisplayName("should throw NullPointerException for null timeSaved in 5-param constructor")
    void shouldThrowNpeForNullTimeSavedInFiveParamConstructor() {
      assertThrows(
          NullPointerException.class,
          () -> new NetworkOptimizationResult(true, 100L, null, "strategy", null),
          "Null timeSaved should throw NullPointerException");
    }

    @Test
    @DisplayName("should throw NullPointerException for null strategy in 5-param constructor")
    void shouldThrowNpeForNullStrategyInFiveParamConstructor() {
      assertThrows(
          NullPointerException.class,
          () -> new NetworkOptimizationResult(true, 100L, Duration.ZERO, null, null),
          "Null strategy should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Failure Factory Tests")
  class FailureFactoryTests {

    @Test
    @DisplayName("failure should create unsuccessful result")
    void failureShouldCreateUnsuccessfulResult() {
      final NetworkOptimizationResult result =
          NetworkOptimizationResult.failure("Connection refused");

      assertNotNull(result, "Failure result should not be null");
      assertFalse(result.isSuccessful(), "Failure result should not be successful");
    }

    @Test
    @DisplayName("failure should set error message")
    void failureShouldSetErrorMessage() {
      final NetworkOptimizationResult result =
          NetworkOptimizationResult.failure("DNS resolution failed");

      assertEquals("DNS resolution failed", result.getErrorMessage(),
          "Error message should match");
    }

    @Test
    @DisplayName("failure should set bytes reduced to zero")
    void failureShouldSetBytesReducedToZero() {
      final NetworkOptimizationResult result =
          NetworkOptimizationResult.failure("timeout");

      assertEquals(0L, result.getBytesReduced(),
          "Bytes reduced should be 0 for failure");
    }

    @Test
    @DisplayName("failure should set time saved to Duration.ZERO")
    void failureShouldSetTimeSavedToZero() {
      final NetworkOptimizationResult result =
          NetworkOptimizationResult.failure("timeout");

      assertEquals(Duration.ZERO, result.getTimeSaved(),
          "Time saved should be Duration.ZERO for failure");
    }

    @Test
    @DisplayName("failure should set optimization strategy to 'none'")
    void failureShouldSetStrategyToNone() {
      final NetworkOptimizationResult result =
          NetworkOptimizationResult.failure("timeout");

      assertEquals("none", result.getOptimizationStrategy(),
          "Optimization strategy should be 'none' for failure");
    }

    @Test
    @DisplayName("failure should throw NullPointerException for null error message")
    void failureShouldThrowNpeForNullErrorMessage() {
      assertThrows(
          NullPointerException.class,
          () -> NetworkOptimizationResult.failure(null),
          "Null error message should throw NullPointerException");
    }
  }

  @Nested
  @DisplayName("Getter Tests")
  class GetterTests {

    @Test
    @DisplayName("isSuccessful should reflect constructor value")
    void isSuccessfulShouldReflectConstructorValue() {
      final NetworkOptimizationResult successResult =
          new NetworkOptimizationResult(512L, Duration.ofMillis(25), "gzip");
      final NetworkOptimizationResult failResult =
          NetworkOptimizationResult.failure("error");

      assertTrue(successResult.isSuccessful(),
          "3-param constructor result should be successful");
      assertFalse(failResult.isSuccessful(),
          "Failure factory result should not be successful");
    }

    @Test
    @DisplayName("getBytesReduced should return constructor value")
    void getBytesReducedShouldReturnConstructorValue() {
      final NetworkOptimizationResult result =
          new NetworkOptimizationResult(999L, Duration.ofMillis(1), "test");

      assertEquals(999L, result.getBytesReduced(), "Bytes reduced should be 999");
    }

    @Test
    @DisplayName("getTimeSaved should return constructor value")
    void getTimeSavedShouldReturnConstructorValue() {
      final Duration expected = Duration.ofSeconds(5);
      final NetworkOptimizationResult result =
          new NetworkOptimizationResult(100L, expected, "test");

      assertEquals(expected, result.getTimeSaved(), "Time saved should match");
    }

    @Test
    @DisplayName("getOptimizationStrategy should return constructor value")
    void getOptimizationStrategyShouldReturnConstructorValue() {
      final NetworkOptimizationResult result =
          new NetworkOptimizationResult(100L, Duration.ZERO, "brotli");

      assertEquals("brotli", result.getOptimizationStrategy(),
          "Optimization strategy should be 'brotli'");
    }

    @Test
    @DisplayName("getErrorMessage should return constructor value")
    void getErrorMessageShouldReturnConstructorValue() {
      final NetworkOptimizationResult result = new NetworkOptimizationResult(
          false, 0L, Duration.ZERO, "none", "Custom error");

      assertEquals("Custom error", result.getErrorMessage(),
          "Error message should be 'Custom error'");
    }
  }
}
