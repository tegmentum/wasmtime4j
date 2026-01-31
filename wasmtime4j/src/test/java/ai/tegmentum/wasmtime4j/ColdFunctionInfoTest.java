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
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Tests for {@link ColdFunctionInfo} cold function data accessors. */
@DisplayName("ColdFunctionInfo")
final class ColdFunctionInfoTest {

  @Nested
  @DisplayName("builder defaults")
  final class BuilderDefaultTests {

    @Test
    @DisplayName("should build with default values")
    void shouldBuildWithDefaults() {
      final ColdFunctionInfo info = ColdFunctionInfo.builder("myFunc").build();
      assertEquals("myFunc", info.getFunctionName(), "Function name should be myFunc");
      assertEquals(0, info.getTotalCallCount(), "Default call count should be 0");
      assertEquals(Duration.ZERO, info.getTotalExecutionTime(), "Default total time should be ZERO");
      assertEquals(
          Duration.ZERO, info.getAverageExecutionTime(), "Default avg time should be ZERO");
      assertEquals(0.0, info.getCallFrequency(), 0.001, "Default frequency should be 0.0");
      assertEquals(0, info.getLastCallTimestamp(), "Default last call timestamp should be 0");
      assertFalse(info.isCandidate(), "Default candidate should be false");
      assertEquals("", info.getReason(), "Default reason should be empty");
    }
  }

  @Nested
  @DisplayName("builder with custom values")
  final class BuilderCustomValuesTests {

    @Test
    @DisplayName("should set total call count")
    void shouldSetTotalCallCount() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func1").totalCallCount(42).build();
      assertEquals(42, info.getTotalCallCount(), "Total call count should be 42");
    }

    @Test
    @DisplayName("should set total execution time")
    void shouldSetTotalExecutionTime() {
      final Duration execTime = Duration.ofMillis(500);
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func2").totalExecutionTime(execTime).build();
      assertEquals(execTime, info.getTotalExecutionTime(), "Total execution time should match");
    }

    @Test
    @DisplayName("should set average execution time")
    void shouldSetAverageExecutionTime() {
      final Duration avgTime = Duration.ofMillis(10);
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func3").averageExecutionTime(avgTime).build();
      assertEquals(avgTime, info.getAverageExecutionTime(), "Average execution time should match");
    }

    @Test
    @DisplayName("should set call frequency")
    void shouldSetCallFrequency() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func4").callFrequency(0.5).build();
      assertEquals(0.5, info.getCallFrequency(), 0.001, "Call frequency should be 0.5");
    }

    @Test
    @DisplayName("should set last call timestamp")
    void shouldSetLastCallTimestamp() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func5").lastCallTimestamp(123456789L).build();
      assertEquals(
          123456789L, info.getLastCallTimestamp(), "Last call timestamp should match");
    }

    @Test
    @DisplayName("should set candidate flag")
    void shouldSetCandidateFlag() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func6").candidate(true).build();
      assertTrue(info.isCandidate(), "Candidate flag should be true");
    }

    @Test
    @DisplayName("should set reason string")
    void shouldSetReason() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func7").reason("low frequency").build();
      assertEquals("low frequency", info.getReason(), "Reason should match");
    }
  }

  @Nested
  @DisplayName("candidate factory method")
  final class CandidateFactoryTests {

    @Test
    @DisplayName("should create candidate with computed average time")
    void shouldCreateCandidateWithComputedAvgTime() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.candidate("coldFunc", 10, Duration.ofMillis(1000), 0.1, "rarely called");
      assertEquals("coldFunc", info.getFunctionName(), "Function name should be coldFunc");
      assertEquals(10, info.getTotalCallCount(), "Total call count should be 10");
      assertEquals(
          Duration.ofMillis(1000),
          info.getTotalExecutionTime(),
          "Total execution time should be 1000ms");
      assertEquals(
          Duration.ofMillis(100),
          info.getAverageExecutionTime(),
          "Average should be total/count = 100ms");
      assertEquals(0.1, info.getCallFrequency(), 0.001, "Call frequency should be 0.1");
      assertTrue(info.isCandidate(), "Should be marked as candidate");
      assertEquals("rarely called", info.getReason(), "Reason should match");
    }

    @Test
    @DisplayName("should handle zero call count gracefully")
    void shouldHandleZeroCallCount() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.candidate("neverCalled", 0, Duration.ofMillis(0), 0.0, "never called");
      assertEquals(Duration.ZERO, info.getAverageExecutionTime(), "Avg time should be ZERO for 0 calls");
    }

    @Test
    @DisplayName("should set lastCallTimestamp to current nanoTime")
    void shouldSetLastCallTimestamp() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.candidate("testFunc", 5, Duration.ofMillis(50), 0.5, "test");
      assertTrue(
          info.getLastCallTimestamp() > 0,
          "Last call timestamp should be positive (set from System.nanoTime())");
    }
  }

  @Nested
  @DisplayName("getTimePercentage")
  final class GetTimePercentageTests {

    @Test
    @DisplayName("should calculate correct time percentage")
    void shouldCalculateCorrectPercentage() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func")
              .totalExecutionTime(Duration.ofMillis(100))
              .build();
      final double percentage = info.getTimePercentage(Duration.ofMillis(1000));
      assertEquals(10.0, percentage, 0.001, "100ms / 1000ms should be 10%");
    }

    @Test
    @DisplayName("should return 0 for zero total program time")
    void shouldReturnZeroForZeroProgramTime() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func")
              .totalExecutionTime(Duration.ofMillis(100))
              .build();
      final double percentage = info.getTimePercentage(Duration.ZERO);
      assertEquals(0.0, percentage, 0.001, "Should be 0% when total program time is zero");
    }

    @Test
    @DisplayName("should return 100% when function time equals program time")
    void shouldReturn100PercentWhenEqual() {
      final Duration time = Duration.ofMillis(500);
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func").totalExecutionTime(time).build();
      final double percentage = info.getTimePercentage(time);
      assertEquals(100.0, percentage, 0.001, "Should be 100% when times are equal");
    }
  }

  @Nested
  @DisplayName("toString")
  final class ToStringTests {

    @Test
    @DisplayName("should include function name in toString")
    void shouldIncludeFunctionName() {
      final ColdFunctionInfo info = ColdFunctionInfo.builder("myFunc").build();
      final String str = info.toString();
      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("myFunc"), "toString should include function name");
    }

    @Test
    @DisplayName("should include candidate status in toString")
    void shouldIncludeCandidateStatus() {
      final ColdFunctionInfo info =
          ColdFunctionInfo.builder("func").candidate(true).build();
      final String str = info.toString();
      assertTrue(str.contains("true"), "toString should include candidate=true");
    }
  }
}
