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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentSwapResult} class.
 *
 * <p>ComponentSwapResult provides detailed information about the outcome of a component hot-swap
 * operation.
 */
@DisplayName("ComponentSwapResult Tests")
class ComponentSwapResultTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(ComponentSwapResult.class.getModifiers()),
          "ComponentSwapResult should be public");
      assertTrue(
          Modifier.isFinal(ComponentSwapResult.class.getModifiers()),
          "ComponentSwapResult should be final");
      assertFalse(
          ComponentSwapResult.class.isInterface(),
          "ComponentSwapResult should not be an interface");
    }
  }

  @Nested
  @DisplayName("SwapStatus Enum Tests")
  class SwapStatusEnumTests {

    @Test
    @DisplayName("SwapStatus enum should have expected values")
    void swapStatusEnumShouldHaveExpectedValues() {
      final ComponentSwapResult.SwapStatus[] values = ComponentSwapResult.SwapStatus.values();
      assertEquals(6, values.length, "Should have 6 status values");
    }

    @Test
    @DisplayName("SUCCESS should be first value")
    void successShouldBeFirstValue() {
      assertEquals(
          0, ComponentSwapResult.SwapStatus.SUCCESS.ordinal(), "SUCCESS should have ordinal 0");
    }

    @Test
    @DisplayName("SUCCESS_WITH_WARNINGS should have ordinal 1")
    void successWithWarningsShouldHaveOrdinal1() {
      assertEquals(
          1,
          ComponentSwapResult.SwapStatus.SUCCESS_WITH_WARNINGS.ordinal(),
          "SUCCESS_WITH_WARNINGS should have ordinal 1");
    }

    @Test
    @DisplayName("FAILED should have ordinal 2")
    void failedShouldHaveOrdinal2() {
      assertEquals(
          2, ComponentSwapResult.SwapStatus.FAILED.ordinal(), "FAILED should have ordinal 2");
    }

    @Test
    @DisplayName("CANCELLED should have ordinal 3")
    void cancelledShouldHaveOrdinal3() {
      assertEquals(
          3, ComponentSwapResult.SwapStatus.CANCELLED.ordinal(), "CANCELLED should have ordinal 3");
    }

    @Test
    @DisplayName("ROLLED_BACK should have ordinal 4")
    void rolledBackShouldHaveOrdinal4() {
      assertEquals(
          4,
          ComponentSwapResult.SwapStatus.ROLLED_BACK.ordinal(),
          "ROLLED_BACK should have ordinal 4");
    }

    @Test
    @DisplayName("IN_PROGRESS should have ordinal 5")
    void inProgressShouldHaveOrdinal5() {
      assertEquals(
          5,
          ComponentSwapResult.SwapStatus.IN_PROGRESS.ordinal(),
          "IN_PROGRESS should have ordinal 5");
    }
  }

  @Nested
  @DisplayName("StatePreservationResult Nested Class Tests")
  class StatePreservationResultNestedClassTests {

    @Test
    @DisplayName("should have StatePreservationResult nested class")
    void shouldHaveStatePreservationResultNestedClass() {
      final var nestedClasses = ComponentSwapResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("StatePreservationResult")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "StatePreservationResult should be a class");
          assertTrue(Modifier.isStatic(nestedClass.getModifiers()), "Should be static");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Should be final");
          break;
        }
      }
      assertTrue(found, "Should have StatePreservationResult nested class");
    }

    @Test
    @DisplayName("StatePreservationResult.success() should create successful result")
    void successShouldCreateSuccessfulResult() {
      final ComponentSwapResult.StatePreservationResult result =
          ComponentSwapResult.StatePreservationResult.success();

      assertTrue(result.isSuccessful(), "Should be successful");
      assertEquals("State preserved successfully", result.getMessage(), "Message should match");
      assertTrue(result.getPreservedState().isEmpty(), "Preserved state should be empty");
    }

    @Test
    @DisplayName("StatePreservationResult.success(Map) should create successful result with state")
    void successWithMapShouldCreateSuccessfulResultWithState() {
      final Map<String, Object> state = Map.of("key", "value");
      final ComponentSwapResult.StatePreservationResult result =
          ComponentSwapResult.StatePreservationResult.success(state);

      assertTrue(result.isSuccessful(), "Should be successful");
      assertEquals("State preserved successfully", result.getMessage(), "Message should match");
      assertEquals(1, result.getPreservedState().size(), "Should have 1 state entry");
      assertEquals("value", result.getPreservedState().get("key"), "State value should match");
    }

    @Test
    @DisplayName("StatePreservationResult.failed(message) should create failed result")
    void failedShouldCreateFailedResult() {
      final ComponentSwapResult.StatePreservationResult result =
          ComponentSwapResult.StatePreservationResult.failed("Test failure");

      assertFalse(result.isSuccessful(), "Should not be successful");
      assertEquals("Test failure", result.getMessage(), "Message should match");
      assertTrue(result.getPreservedState().isEmpty(), "Preserved state should be empty");
    }
  }

  @Nested
  @DisplayName("Builder Nested Class Tests")
  class BuilderNestedClassTests {

    @Test
    @DisplayName("should have Builder nested class")
    void shouldHaveBuilderNestedClass() {
      final var nestedClasses = ComponentSwapResult.class.getDeclaredClasses();
      boolean found = false;
      for (var nestedClass : nestedClasses) {
        if (nestedClass.getSimpleName().equals("Builder")) {
          found = true;
          assertFalse(nestedClass.isInterface(), "Builder should be a class");
          assertTrue(Modifier.isStatic(nestedClass.getModifiers()), "Should be static");
          assertTrue(Modifier.isFinal(nestedClass.getModifiers()), "Should be final");
          break;
        }
      }
      assertTrue(found, "Should have Builder nested class");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have builder static method")
    void shouldHaveBuilderStaticMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("builder");
      assertNotNull(method, "builder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "builder should be static");
      assertEquals(
          ComponentSwapResult.Builder.class, method.getReturnType(), "Should return Builder");
    }

    @Test
    @DisplayName("should have success static method")
    void shouldHaveSuccessStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentSwapResult.class.getMethod(
              "success",
              Component.class,
              Component.class,
              Instant.class,
              Instant.class);
      assertNotNull(method, "success method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "success should be static");
      assertEquals(
          ComponentSwapResult.class, method.getReturnType(), "Should return ComponentSwapResult");
    }

    @Test
    @DisplayName("should have failure static method")
    void shouldHaveFailureStaticMethod() throws NoSuchMethodException {
      final Method method =
          ComponentSwapResult.class.getMethod(
              "failure",
              Component.class,
              Component.class,
              Instant.class,
              Instant.class,
              Exception.class);
      assertNotNull(method, "failure method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "failure should be static");
      assertEquals(
          ComponentSwapResult.class, method.getReturnType(), "Should return ComponentSwapResult");
    }
  }

  @Nested
  @DisplayName("Getter Method Tests")
  class GetterMethodTests {

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          ComponentSwapResult.SwapStatus.class, method.getReturnType(), "Should return SwapStatus");
    }

    @Test
    @DisplayName("should have getOldComponent method")
    void shouldHaveGetOldComponentMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getOldComponent");
      assertNotNull(method, "getOldComponent method should exist");
      assertEquals(Component.class, method.getReturnType(), "Should return Component");
    }

    @Test
    @DisplayName("should have getNewComponent method")
    void shouldHaveGetNewComponentMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getNewComponent");
      assertNotNull(method, "getNewComponent method should exist");
      assertEquals(Component.class, method.getReturnType(), "Should return Component");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getEndTime method")
    void shouldHaveGetEndTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getEndTime");
      assertNotNull(method, "getEndTime method should exist");
      assertEquals(Instant.class, method.getReturnType(), "Should return Instant");
    }

    @Test
    @DisplayName("should have getTotalTime method")
    void shouldHaveGetTotalTimeMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getTotalTime");
      assertNotNull(method, "getTotalTime method should exist");
      assertEquals(Duration.class, method.getReturnType(), "Should return Duration");
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getError method")
    void shouldHaveGetErrorMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getError");
      assertNotNull(method, "getError method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getMetrics method")
    void shouldHaveGetMetricsMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getMetrics");
      assertNotNull(method, "getMetrics method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getStatePreservation method")
    void shouldHaveGetStatePreservationMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getStatePreservation");
      assertNotNull(method, "getStatePreservation method should exist");
      assertEquals(
          ComponentSwapResult.StatePreservationResult.class,
          method.getReturnType(),
          "Should return StatePreservationResult");
    }

    @Test
    @DisplayName("should have isRollbackPerformed method")
    void shouldHaveIsRollbackPerformedMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("isRollbackPerformed");
      assertNotNull(method, "isRollbackPerformed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRollbackComponent method")
    void shouldHaveGetRollbackComponentMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getRollbackComponent");
      assertNotNull(method, "getRollbackComponent method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }
  }

  @Nested
  @DisplayName("Convenience Method Tests")
  class ConvenienceMethodTests {

    @Test
    @DisplayName("should have isSuccessful method")
    void shouldHaveIsSuccessfulMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("isSuccessful");
      assertNotNull(method, "isSuccessful method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isFailed method")
    void shouldHaveIsFailedMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("isFailed");
      assertNotNull(method, "isFailed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getSummary method")
    void shouldHaveGetSummaryMethod() throws NoSuchMethodException {
      final Method method = ComponentSwapResult.class.getMethod("getSummary");
      assertNotNull(method, "getSummary method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Builder Behavior Tests")
  class BuilderBehaviorTests {

    @Test
    @DisplayName("builder should create result with all fields")
    void builderShouldCreateResultWithAllFields() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS)
              .startTime(startTime)
              .endTime(endTime)
              .statePreservation(ComponentSwapResult.StatePreservationResult.success())
              .build();

      assertNotNull(result, "Result should not be null");
      assertEquals(
          ComponentSwapResult.SwapStatus.SUCCESS, result.getStatus(), "Status should match");
      assertEquals(startTime, result.getStartTime(), "Start time should match");
      assertEquals(endTime, result.getEndTime(), "End time should match");
    }

    @Test
    @DisplayName("builder should calculate total time")
    void builderShouldCalculateTotalTime() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(500);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS)
              .startTime(startTime)
              .endTime(endTime)
              .build();

      assertEquals(Duration.ofMillis(500), result.getTotalTime(), "Total time should be 500ms");
    }

    @Test
    @DisplayName("builder should support warnings")
    void builderShouldSupportWarnings() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS_WITH_WARNINGS)
              .startTime(startTime)
              .endTime(endTime)
              .warnings(List.of("Warning 1", "Warning 2"))
              .build();

      assertEquals(2, result.getWarnings().size(), "Should have 2 warnings");
      assertEquals("Warning 1", result.getWarnings().get(0), "First warning should match");
      assertEquals("Warning 2", result.getWarnings().get(1), "Second warning should match");
    }

    @Test
    @DisplayName("builder should support error")
    void builderShouldSupportError() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);
      final Exception error = new RuntimeException("Test error");

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.FAILED)
              .startTime(startTime)
              .endTime(endTime)
              .error(error)
              .build();

      assertTrue(result.getError().isPresent(), "Error should be present");
      assertEquals(error, result.getError().get(), "Error should match");
    }

    @Test
    @DisplayName("builder should support metrics")
    void builderShouldSupportMetrics() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS)
              .startTime(startTime)
              .endTime(endTime)
              .metrics(Map.of("operations", 42, "bytesProcessed", 1024))
              .build();

      assertEquals(2, result.getMetrics().size(), "Should have 2 metrics");
      assertEquals(42, result.getMetrics().get("operations"), "Operations metric should match");
      assertEquals(
          1024, result.getMetrics().get("bytesProcessed"), "BytesProcessed metric should match");
    }

    @Test
    @DisplayName("builder should support rollback")
    void builderShouldSupportRollback() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.ROLLED_BACK)
              .startTime(startTime)
              .endTime(endTime)
              .rollbackPerformed(true)
              .build();

      assertTrue(result.isRollbackPerformed(), "Rollback should be performed");
    }
  }

  @Nested
  @DisplayName("isSuccessful Behavior Tests")
  class IsSuccessfulBehaviorTests {

    @Test
    @DisplayName("SUCCESS status should be successful")
    void successStatusShouldBeSuccessful() {
      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS)
              .startTime(Instant.now())
              .endTime(Instant.now())
              .build();

      assertTrue(result.isSuccessful(), "SUCCESS should be successful");
      assertFalse(result.isFailed(), "SUCCESS should not be failed");
    }

    @Test
    @DisplayName("SUCCESS_WITH_WARNINGS status should be successful")
    void successWithWarningsStatusShouldBeSuccessful() {
      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS_WITH_WARNINGS)
              .startTime(Instant.now())
              .endTime(Instant.now())
              .build();

      assertTrue(result.isSuccessful(), "SUCCESS_WITH_WARNINGS should be successful");
      assertFalse(result.isFailed(), "SUCCESS_WITH_WARNINGS should not be failed");
    }

    @Test
    @DisplayName("FAILED status should be failed")
    void failedStatusShouldBeFailed() {
      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.FAILED)
              .startTime(Instant.now())
              .endTime(Instant.now())
              .build();

      assertFalse(result.isSuccessful(), "FAILED should not be successful");
      assertTrue(result.isFailed(), "FAILED should be failed");
    }

    @Test
    @DisplayName("ROLLED_BACK status should be failed")
    void rolledBackStatusShouldBeFailed() {
      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.ROLLED_BACK)
              .startTime(Instant.now())
              .endTime(Instant.now())
              .build();

      assertFalse(result.isSuccessful(), "ROLLED_BACK should not be successful");
      assertTrue(result.isFailed(), "ROLLED_BACK should be failed");
    }
  }

  @Nested
  @DisplayName("getSummary Tests")
  class GetSummaryTests {

    @Test
    @DisplayName("summary should contain status")
    void summaryShouldContainStatus() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS)
              .startTime(startTime)
              .endTime(endTime)
              .build();

      final String summary = result.getSummary();
      assertTrue(summary.contains("success"), "Summary should contain status");
      assertTrue(summary.contains("100"), "Summary should contain duration");
    }

    @Test
    @DisplayName("summary should mention warnings if present")
    void summaryShouldMentionWarningsIfPresent() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS_WITH_WARNINGS)
              .startTime(startTime)
              .endTime(endTime)
              .warnings(List.of("warn1", "warn2"))
              .build();

      final String summary = result.getSummary();
      assertTrue(summary.contains("2"), "Summary should mention warning count");
      assertTrue(summary.contains("warning"), "Summary should mention warnings");
    }

    @Test
    @DisplayName("summary should mention rollback if performed")
    void summaryShouldMentionRollbackIfPerformed() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.ROLLED_BACK)
              .startTime(startTime)
              .endTime(endTime)
              .rollbackPerformed(true)
              .build();

      final String summary = result.getSummary();
      assertTrue(summary.contains("rollback"), "Summary should mention rollback");
    }
  }

  @Nested
  @DisplayName("Immutability Tests")
  class ImmutabilityTests {

    @Test
    @DisplayName("warnings list should be immutable")
    void warningsListShouldBeImmutable() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS)
              .startTime(startTime)
              .endTime(endTime)
              .warnings(List.of("warning1"))
              .build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getWarnings().add("newWarning"),
          "Warnings list should be immutable");
    }

    @Test
    @DisplayName("metrics map should be immutable")
    void metricsMapShouldBeImmutable() {
      final Instant startTime = Instant.now();
      final Instant endTime = startTime.plusMillis(100);

      final ComponentSwapResult result =
          ComponentSwapResult.builder()
              .status(ComponentSwapResult.SwapStatus.SUCCESS)
              .startTime(startTime)
              .endTime(endTime)
              .metrics(Map.of("key", "value"))
              .build();

      assertThrows(
          UnsupportedOperationException.class,
          () -> result.getMetrics().put("newKey", "newValue"),
          "Metrics map should be immutable");
    }
  }
}
