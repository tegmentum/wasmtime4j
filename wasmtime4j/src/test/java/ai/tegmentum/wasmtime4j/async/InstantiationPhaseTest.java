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

package ai.tegmentum.wasmtime4j.async;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for InstantiationPhase class.
 *
 * <p>Verifies the representation of WebAssembly instance instantiation phases.
 */
@DisplayName("InstantiationPhase Tests")
class InstantiationPhaseTest {

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create phase with all values")
    void shouldCreatePhaseWithAllValues() {
      Instant start = Instant.now().minusSeconds(1);
      Instant end = Instant.now();
      Duration duration = Duration.ofSeconds(1);

      InstantiationPhase phase =
          new InstantiationPhase(
              "compilation", duration, start, end, 1024L * 1024, 50, "Compiled 50 functions", true);

      assertNotNull(phase, "Phase should not be null");
      assertEquals("compilation", phase.getName(), "Name should match");
      assertEquals(duration, phase.getDuration(), "Duration should match");
      assertEquals(start, phase.getStartTime(), "Start time should match");
      assertEquals(end, phase.getEndTime(), "End time should match");
      assertEquals(1024L * 1024, phase.getMemoryAllocated(), "Memory should match");
      assertEquals(50, phase.getFunctionsProcessed(), "Functions should match");
      assertEquals("Compiled 50 functions", phase.getDetails(), "Details should match");
      assertTrue(phase.isSuccessful(), "Phase should be successful");
    }

    @Test
    @DisplayName("should create phase with zero values")
    void shouldCreatePhaseWithZeroValues() {
      Instant now = Instant.now();

      InstantiationPhase phase =
          new InstantiationPhase("init", Duration.ZERO, now, now, 0L, 0, "", true);

      assertEquals("init", phase.getName(), "Name should match");
      assertEquals(Duration.ZERO, phase.getDuration(), "Duration should be zero");
      assertEquals(0L, phase.getMemoryAllocated(), "Memory should be 0");
      assertEquals(0, phase.getFunctionsProcessed(), "Functions should be 0");
      assertEquals("", phase.getDetails(), "Details should be empty");
    }
  }

  @Nested
  @DisplayName("Successful Factory Tests")
  class SuccessfulFactoryTests {

    @Test
    @DisplayName("should create successful phase with name and duration")
    void shouldCreateSuccessfulPhaseWithNameAndDuration() {
      Duration duration = Duration.ofMillis(500);

      InstantiationPhase phase = InstantiationPhase.successful("linking", duration);

      assertNotNull(phase, "Phase should not be null");
      assertEquals("linking", phase.getName(), "Name should be 'linking'");
      assertEquals(duration, phase.getDuration(), "Duration should match");
      assertTrue(phase.isSuccessful(), "Phase should be successful");
      assertEquals(0L, phase.getMemoryAllocated(), "Memory should be 0");
      assertEquals(0, phase.getFunctionsProcessed(), "Functions should be 0");
      assertEquals("", phase.getDetails(), "Details should be empty");
    }

    @Test
    @DisplayName("should set start and end times correctly")
    void shouldSetStartAndEndTimesCorrectly() {
      Duration duration = Duration.ofSeconds(2);
      Instant beforeCreate = Instant.now();

      InstantiationPhase phase = InstantiationPhase.successful("test", duration);

      assertNotNull(phase.getStartTime(), "Start time should not be null");
      assertNotNull(phase.getEndTime(), "End time should not be null");
      // End time should be around now
      assertTrue(
          !phase.getEndTime().isBefore(beforeCreate),
          "End time should be after or at beforeCreate");
      final Instant afterCreate = Instant.now();
      assertTrue(
          !phase.getEndTime().isAfter(afterCreate.plusMillis(100)),
          "End time should be at or before afterCreate");
    }
  }

  @Nested
  @DisplayName("Failed Factory Tests")
  class FailedFactoryTests {

    @Test
    @DisplayName("should create failed phase with error details")
    void shouldCreateFailedPhaseWithErrorDetails() {
      Duration duration = Duration.ofMillis(100);
      String errorDetails = "Memory limit exceeded";

      InstantiationPhase phase = InstantiationPhase.failed("allocation", duration, errorDetails);

      assertNotNull(phase, "Phase should not be null");
      assertEquals("allocation", phase.getName(), "Name should be 'allocation'");
      assertEquals(duration, phase.getDuration(), "Duration should match");
      assertFalse(phase.isSuccessful(), "Phase should NOT be successful");
      assertEquals(errorDetails, phase.getDetails(), "Details should contain error");
    }
  }

  @Nested
  @DisplayName("WithMetrics Factory Tests")
  class WithMetricsFactoryTests {

    @Test
    @DisplayName("should create phase with metrics")
    void shouldCreatePhaseWithMetrics() {
      Duration duration = Duration.ofMillis(250);
      long memoryAllocated = 2L * 1024 * 1024;
      int functionsProcessed = 100;

      InstantiationPhase phase =
          InstantiationPhase.withMetrics(
              "compilation", duration, memoryAllocated, functionsProcessed);

      assertNotNull(phase, "Phase should not be null");
      assertEquals("compilation", phase.getName(), "Name should be 'compilation'");
      assertEquals(duration, phase.getDuration(), "Duration should match");
      assertEquals(memoryAllocated, phase.getMemoryAllocated(), "Memory should match");
      assertEquals(functionsProcessed, phase.getFunctionsProcessed(), "Functions should match");
      assertTrue(phase.isSuccessful(), "Phase should be successful");
      assertEquals("", phase.getDetails(), "Details should be empty");
    }
  }

  @Nested
  @DisplayName("GetMemoryAllocationRate Tests")
  class GetMemoryAllocationRateTests {

    @Test
    @DisplayName("should calculate memory allocation rate")
    void shouldCalculateMemoryAllocationRate() {
      Duration duration = Duration.ofSeconds(2);
      long memoryAllocated = 10L * 1024 * 1024; // 10 MB

      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", duration, memoryAllocated, 0);

      double rate = phase.getMemoryAllocationRate();
      double expectedRate = 5L * 1024 * 1024; // 5 MB/s

      assertEquals(expectedRate, rate, 1000.0, "Rate should be ~5MB/s");
    }

    @Test
    @DisplayName("should return zero rate for zero duration")
    void shouldReturnZeroRateForZeroDuration() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", Duration.ZERO, 1024L * 1024, 0);

      assertEquals(
          0.0, phase.getMemoryAllocationRate(), 0.001, "Rate should be 0 for zero duration");
    }
  }

  @Nested
  @DisplayName("GetFunctionProcessingRate Tests")
  class GetFunctionProcessingRateTests {

    @Test
    @DisplayName("should calculate function processing rate")
    void shouldCalculateFunctionProcessingRate() {
      Duration duration = Duration.ofSeconds(2);
      int functionsProcessed = 100;

      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", duration, 0L, functionsProcessed);

      double rate = phase.getFunctionProcessingRate();
      assertEquals(50.0, rate, 1.0, "Rate should be ~50 functions/s");
    }

    @Test
    @DisplayName("should return zero rate for zero duration")
    void shouldReturnZeroRateForZeroDuration() {
      InstantiationPhase phase = InstantiationPhase.withMetrics("test", Duration.ZERO, 0L, 100);

      assertEquals(
          0.0, phase.getFunctionProcessingRate(), 0.001, "Rate should be 0 for zero duration");
    }
  }

  @Nested
  @DisplayName("HasMemoryAllocation Tests")
  class HasMemoryAllocationTests {

    @Test
    @DisplayName("should return true when memory allocated")
    void shouldReturnTrueWhenMemoryAllocated() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", Duration.ofMillis(100), 1024L, 0);

      assertTrue(phase.hasMemoryAllocation(), "Should have memory allocation");
    }

    @Test
    @DisplayName("should return false when no memory allocated")
    void shouldReturnFalseWhenNoMemoryAllocated() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", Duration.ofMillis(100), 0L, 10);

      assertFalse(phase.hasMemoryAllocation(), "Should NOT have memory allocation");
    }
  }

  @Nested
  @DisplayName("HasFunctionProcessing Tests")
  class HasFunctionProcessingTests {

    @Test
    @DisplayName("should return true when functions processed")
    void shouldReturnTrueWhenFunctionsProcessed() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", Duration.ofMillis(100), 0L, 50);

      assertTrue(phase.hasFunctionProcessing(), "Should have function processing");
    }

    @Test
    @DisplayName("should return false when no functions processed")
    void shouldReturnFalseWhenNoFunctionsProcessed() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", Duration.ofMillis(100), 1024L, 0);

      assertFalse(phase.hasFunctionProcessing(), "Should NOT have function processing");
    }
  }

  @Nested
  @DisplayName("HasDetails Tests")
  class HasDetailsTests {

    @Test
    @DisplayName("should return true when details present")
    void shouldReturnTrueWhenDetailsPresent() {
      InstantiationPhase phase =
          InstantiationPhase.failed("test", Duration.ofMillis(100), "Error occurred");

      assertTrue(phase.hasDetails(), "Should have details");
    }

    @Test
    @DisplayName("should return false when no details")
    void shouldReturnFalseWhenNoDetails() {
      InstantiationPhase phase = InstantiationPhase.successful("test", Duration.ofMillis(100));

      assertFalse(phase.hasDetails(), "Should NOT have details");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should produce non-null toString output")
    void shouldProduceNonNullToStringOutput() {
      InstantiationPhase phase = InstantiationPhase.successful("test", Duration.ofMillis(100));

      assertNotNull(phase.toString(), "toString should not return null");
    }

    @Test
    @DisplayName("should include class name in toString")
    void shouldIncludeClassNameInToString() {
      InstantiationPhase phase = InstantiationPhase.successful("test", Duration.ofMillis(100));

      assertTrue(
          phase.toString().contains("InstantiationPhase"), "toString should contain class name");
    }

    @Test
    @DisplayName("should include name in toString")
    void shouldIncludeNameInToString() {
      InstantiationPhase phase =
          InstantiationPhase.successful("compilation", Duration.ofMillis(100));

      assertTrue(phase.toString().contains("compilation"), "toString should contain name");
    }

    @Test
    @DisplayName("should include successful status in toString")
    void shouldIncludeSuccessfulStatusInToString() {
      InstantiationPhase phase = InstantiationPhase.successful("test", Duration.ofMillis(100));

      assertTrue(
          phase.toString().contains("successful=true"), "toString should contain successful");
    }

    @Test
    @DisplayName("should include memory when present")
    void shouldIncludeMemoryWhenPresent() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", Duration.ofMillis(100), 2L * 1024 * 1024, 0);

      String str = phase.toString();
      assertTrue(str.contains("memory="), "toString should contain memory");
    }

    @Test
    @DisplayName("should include functions when present")
    void shouldIncludeFunctionsWhenPresent() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics("test", Duration.ofMillis(100), 0L, 50);

      String str = phase.toString();
      assertTrue(str.contains("functions=50"), "toString should contain functions");
    }

    @Test
    @DisplayName("should include details when present")
    void shouldIncludeDetailsWhenPresent() {
      InstantiationPhase phase =
          InstantiationPhase.failed("test", Duration.ofMillis(100), "Some error");

      String str = phase.toString();
      assertTrue(str.contains("details="), "toString should contain details");
      assertTrue(str.contains("Some error"), "toString should contain error message");
    }
  }

  @Nested
  @DisplayName("Integration Tests")
  class IntegrationTests {

    @Test
    @DisplayName("should represent typical compilation phase")
    void shouldRepresentTypicalCompilationPhase() {
      InstantiationPhase phase =
          InstantiationPhase.withMetrics(
              "compilation",
              Duration.ofMillis(500),
              5L * 1024 * 1024, // 5 MB
              100 // 100 functions
              );

      assertEquals("compilation", phase.getName());
      assertEquals(Duration.ofMillis(500), phase.getDuration());
      assertTrue(phase.isSuccessful());
      assertTrue(phase.hasMemoryAllocation());
      assertTrue(phase.hasFunctionProcessing());

      // Verify rates are reasonable
      double memRate = phase.getMemoryAllocationRate();
      assertTrue(memRate > 0, "Memory rate should be positive");

      double funcRate = phase.getFunctionProcessingRate();
      assertTrue(funcRate > 0, "Function rate should be positive");
    }

    @Test
    @DisplayName("should represent failed instantiation phase")
    void shouldRepresentFailedInstantiationPhase() {
      InstantiationPhase phase =
          InstantiationPhase.failed(
              "linking", Duration.ofMillis(50), "Import 'env.memory' not found");

      assertEquals("linking", phase.getName());
      assertEquals(Duration.ofMillis(50), phase.getDuration());
      assertFalse(phase.isSuccessful());
      assertTrue(phase.hasDetails());
      assertEquals("Import 'env.memory' not found", phase.getDetails());
    }

    @Test
    @DisplayName("should represent quick initialization phase")
    void shouldRepresentQuickInitializationPhase() {
      InstantiationPhase phase =
          InstantiationPhase.successful("initialization", Duration.ofMillis(1));

      assertEquals("initialization", phase.getName());
      assertEquals(Duration.ofMillis(1), phase.getDuration());
      assertTrue(phase.isSuccessful());
      assertFalse(phase.hasMemoryAllocation());
      assertFalse(phase.hasFunctionProcessing());
      assertFalse(phase.hasDetails());
    }
  }
}
