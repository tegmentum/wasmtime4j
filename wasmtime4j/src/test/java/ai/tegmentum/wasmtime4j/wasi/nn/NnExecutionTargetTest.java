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

package ai.tegmentum.wasmtime4j.wasi.nn;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link NnExecutionTarget} enum.
 *
 * <p>NnExecutionTarget represents hardware accelerators supported by WASI-NN per the WASI-NN
 * specification.
 */
@DisplayName("NnExecutionTarget Tests")
class NnExecutionTargetTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnExecutionTarget.class.isEnum(), "NnExecutionTarget should be an enum");
    }

    @Test
    @DisplayName("should have exactly 3 values")
    void shouldHaveExactlyThreeValues() {
      final NnExecutionTarget[] values = NnExecutionTarget.values();
      assertEquals(3, values.length, "Should have exactly 3 execution targets");
    }

    @Test
    @DisplayName("should have CPU value")
    void shouldHaveCpuValue() {
      assertNotNull(NnExecutionTarget.valueOf("CPU"), "Should have CPU");
    }

    @Test
    @DisplayName("should have GPU value")
    void shouldHaveGpuValue() {
      assertNotNull(NnExecutionTarget.valueOf("GPU"), "Should have GPU");
    }

    @Test
    @DisplayName("should have TPU value")
    void shouldHaveTpuValue() {
      assertNotNull(NnExecutionTarget.valueOf("TPU"), "Should have TPU");
    }
  }

  @Nested
  @DisplayName("getWasiName Method Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("should return cpu for CPU")
    void shouldReturnCpuForCpu() {
      assertEquals("cpu", NnExecutionTarget.CPU.getWasiName(), "CPU WASI name");
    }

    @Test
    @DisplayName("should return gpu for GPU")
    void shouldReturnGpuForGpu() {
      assertEquals("gpu", NnExecutionTarget.GPU.getWasiName(), "GPU WASI name");
    }

    @Test
    @DisplayName("should return tpu for TPU")
    void shouldReturnTpuForTpu() {
      assertEquals("tpu", NnExecutionTarget.TPU.getWasiName(), "TPU WASI name");
    }
  }

  @Nested
  @DisplayName("getDisplayName Method Tests")
  class GetDisplayNameTests {

    @Test
    @DisplayName("should return CPU for CPU")
    void shouldReturnCpuForCpuDisplayName() {
      assertEquals("CPU", NnExecutionTarget.CPU.getDisplayName(), "CPU display name");
    }

    @Test
    @DisplayName("should return GPU for GPU")
    void shouldReturnGpuForGpuDisplayName() {
      assertEquals("GPU", NnExecutionTarget.GPU.getDisplayName(), "GPU display name");
    }

    @Test
    @DisplayName("should return TPU for TPU")
    void shouldReturnTpuForTpuDisplayName() {
      assertEquals("TPU", NnExecutionTarget.TPU.getDisplayName(), "TPU display name");
    }
  }

  @Nested
  @DisplayName("fromWasiName Method Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should parse cpu")
    void shouldParseCpu() {
      assertEquals(
          NnExecutionTarget.CPU, NnExecutionTarget.fromWasiName("cpu"), "Should parse cpu");
    }

    @Test
    @DisplayName("should parse gpu")
    void shouldParseGpu() {
      assertEquals(
          NnExecutionTarget.GPU, NnExecutionTarget.fromWasiName("gpu"), "Should parse gpu");
    }

    @Test
    @DisplayName("should parse tpu")
    void shouldParseTpu() {
      assertEquals(
          NnExecutionTarget.TPU, NnExecutionTarget.fromWasiName("tpu"), "Should parse tpu");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for unknown name")
    void shouldThrowIllegalArgumentExceptionForUnknownName() {
      final IllegalArgumentException ex =
          assertThrows(
              IllegalArgumentException.class, () -> NnExecutionTarget.fromWasiName("unknown"));

      assertTrue(ex.getMessage().contains("Unknown"), "Exception should mention Unknown");
    }

    @Test
    @DisplayName("should be case sensitive")
    void shouldBeCaseSensitive() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnExecutionTarget.fromWasiName("CPU"),
          "Should be case sensitive");
    }

    @Test
    @DisplayName("should throw for npu")
    void shouldThrowForNpu() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnExecutionTarget.fromWasiName("npu"),
          "NPU not yet supported");
    }
  }

  @Nested
  @DisplayName("getNativeCode Method Tests")
  class GetNativeCodeTests {

    @Test
    @DisplayName("should return ordinal as native code")
    void shouldReturnOrdinalAsNativeCode() {
      for (final NnExecutionTarget target : NnExecutionTarget.values()) {
        assertEquals(
            target.ordinal(),
            target.getNativeCode(),
            target.name() + " native code should be ordinal");
      }
    }

    @Test
    @DisplayName("should have unique native codes")
    void shouldHaveUniqueNativeCodes() {
      final Set<Integer> codes = new HashSet<>();
      for (final NnExecutionTarget target : NnExecutionTarget.values()) {
        assertTrue(codes.add(target.getNativeCode()), "Native code should be unique: " + target);
      }
    }

    @Test
    @DisplayName("should have CPU as code 0")
    void shouldHaveCpuAsCode0() {
      assertEquals(0, NnExecutionTarget.CPU.getNativeCode(), "CPU should be code 0");
    }
  }

  @Nested
  @DisplayName("fromNativeCode Method Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should parse all valid codes")
    void shouldParseAllValidCodes() {
      for (final NnExecutionTarget expected : NnExecutionTarget.values()) {
        final NnExecutionTarget actual = NnExecutionTarget.fromNativeCode(expected.ordinal());
        assertEquals(expected, actual, "Should parse code " + expected.ordinal());
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for negative code")
    void shouldThrowIllegalArgumentExceptionForNegativeCode() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> NnExecutionTarget.fromNativeCode(-1));

      assertTrue(ex.getMessage().contains("Invalid"), "Exception should mention Invalid");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for code out of range")
    void shouldThrowIllegalArgumentExceptionForCodeOutOfRange() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> NnExecutionTarget.fromNativeCode(100));

      assertTrue(ex.getMessage().contains("Invalid"), "Exception should mention Invalid");
    }

    @Test
    @DisplayName("should round trip from target to code and back")
    void shouldRoundTripFromTargetToCodeAndBack() {
      for (final NnExecutionTarget original : NnExecutionTarget.values()) {
        final int code = original.getNativeCode();
        final NnExecutionTarget roundTripped = NnExecutionTarget.fromNativeCode(code);
        assertEquals(original, roundTripped, "Should round trip: " + original);
      }
    }
  }

  @Nested
  @DisplayName("toString Method Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return display name")
    void shouldReturnDisplayName() {
      for (final NnExecutionTarget target : NnExecutionTarget.values()) {
        assertEquals(
            target.getDisplayName(),
            target.toString(),
            target.name() + " toString should return display name");
      }
    }

    @Test
    @DisplayName("should return human-readable string")
    void shouldReturnHumanReadableString() {
      assertEquals("CPU", NnExecutionTarget.CPU.toString(), "CPU toString");
      assertEquals("GPU", NnExecutionTarget.GPU.toString(), "GPU toString");
      assertEquals("TPU", NnExecutionTarget.TPU.toString(), "TPU toString");
    }
  }

  @Nested
  @DisplayName("WASI-NN Specification Compliance Tests")
  class WasiNnSpecificationComplianceTests {

    @Test
    @DisplayName("should cover all WASI-NN execution targets")
    void shouldCoverAllWasiNnExecutionTargets() {
      // Per WASI-NN specification: execution_target enum
      final String[] expectedTargets = {"cpu", "gpu", "tpu"};

      for (final String expectedName : expectedTargets) {
        assertNotNull(
            NnExecutionTarget.fromWasiName(expectedName), "Should have target: " + expectedName);
      }

      assertEquals(
          expectedTargets.length,
          NnExecutionTarget.values().length,
          "Should have exact count of targets");
    }

    @Test
    @DisplayName("should have CPU as always available target")
    void shouldHaveCpuAsAlwaysAvailableTarget() {
      // CPU execution should always be available per spec comment
      assertNotNull(NnExecutionTarget.CPU, "CPU should always be available");
      assertEquals("cpu", NnExecutionTarget.CPU.getWasiName(), "CPU WASI name");
    }

    @Test
    @DisplayName("should have GPU as hardware accelerator")
    void shouldHaveGpuAsHardwareAccelerator() {
      // GPU requires GPU hardware and drivers per spec comment
      assertNotNull(NnExecutionTarget.GPU, "GPU should be available");
      assertEquals("gpu", NnExecutionTarget.GPU.getWasiName(), "GPU WASI name");
    }

    @Test
    @DisplayName("should have TPU as hardware accelerator")
    void shouldHaveTpuAsHardwareAccelerator() {
      // TPU requires TPU hardware per spec comment
      assertNotNull(NnExecutionTarget.TPU, "TPU should be available");
      assertEquals("tpu", NnExecutionTarget.TPU.getWasiName(), "TPU WASI name");
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support fallback execution target selection")
    void shouldSupportFallbackExecutionTargetSelection() {
      // Simulate selecting execution target with fallback to CPU
      final boolean gpuAvailable = false;
      final boolean tpuAvailable = false;

      final NnExecutionTarget target;
      if (tpuAvailable) {
        target = NnExecutionTarget.TPU;
      } else if (gpuAvailable) {
        target = NnExecutionTarget.GPU;
      } else {
        target = NnExecutionTarget.CPU;
      }

      assertEquals(NnExecutionTarget.CPU, target, "Should fallback to CPU");
    }

    @Test
    @DisplayName("should support switch statement")
    void shouldSupportSwitchStatement() {
      final NnExecutionTarget target = NnExecutionTarget.GPU;

      final String accelerationType;
      switch (target) {
        case CPU:
          accelerationType = "software";
          break;
        case GPU:
          accelerationType = "graphics card";
          break;
        case TPU:
          accelerationType = "tensor processing unit";
          break;
        default:
          accelerationType = "unknown";
      }

      assertEquals("graphics card", accelerationType, "GPU should map to graphics card");
    }

    @Test
    @DisplayName("should support execution target comparison")
    void shouldSupportExecutionTargetComparison() {
      final NnExecutionTarget preferred = NnExecutionTarget.GPU;
      final NnExecutionTarget fallback = NnExecutionTarget.CPU;

      final boolean usingAccelerator = preferred != NnExecutionTarget.CPU;

      assertTrue(usingAccelerator, "GPU should be considered an accelerator");
      assertEquals(NnExecutionTarget.CPU, fallback, "CPU is the fallback");
    }
  }
}
