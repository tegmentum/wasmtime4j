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
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the {@link NnExecutionTarget} enum.
 *
 * <p>Verifies WASI-NN execution target values, name mappings, native codes, and string
 * representations.
 */
@DisplayName("NnExecutionTarget Tests")
class NnExecutionTargetTest {

  @Nested
  @DisplayName("Enum Structure Tests")
  class EnumStructureTests {

    @Test
    @DisplayName("NnExecutionTarget should be an enum")
    void shouldBeAnEnum() {
      assertTrue(NnExecutionTarget.class.isEnum(), "NnExecutionTarget should be an enum");
    }

    @Test
    @DisplayName("NnExecutionTarget should have exactly 3 values")
    void shouldHaveExactlyThreeValues() {
      assertEquals(
          3, NnExecutionTarget.values().length, "Should have exactly 3 execution target values");
    }
  }

  @Nested
  @DisplayName("Enum Values Tests")
  class EnumValuesTests {

    @Test
    @DisplayName("should have CPU value")
    void shouldHaveCpuValue() {
      assertNotNull(NnExecutionTarget.CPU, "CPU should exist");
      assertEquals("CPU", NnExecutionTarget.CPU.name(), "Name should be CPU");
    }

    @Test
    @DisplayName("should have GPU value")
    void shouldHaveGpuValue() {
      assertNotNull(NnExecutionTarget.GPU, "GPU should exist");
      assertEquals("GPU", NnExecutionTarget.GPU.name(), "Name should be GPU");
    }

    @Test
    @DisplayName("should have TPU value")
    void shouldHaveTpuValue() {
      assertNotNull(NnExecutionTarget.TPU, "TPU should exist");
      assertEquals("TPU", NnExecutionTarget.TPU.name(), "Name should be TPU");
    }
  }

  @Nested
  @DisplayName("valueOf Tests")
  class ValueOfTests {

    @Test
    @DisplayName("valueOf should return correct constant for each name")
    void valueOfShouldReturnCorrectConstant() {
      assertEquals(NnExecutionTarget.CPU, NnExecutionTarget.valueOf("CPU"), "Should return CPU");
      assertEquals(NnExecutionTarget.GPU, NnExecutionTarget.valueOf("GPU"), "Should return GPU");
      assertEquals(NnExecutionTarget.TPU, NnExecutionTarget.valueOf("TPU"), "Should return TPU");
    }

    @Test
    @DisplayName("valueOf should throw for invalid name")
    void valueOfShouldThrowForInvalidName() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnExecutionTarget.valueOf("INVALID"),
          "Should throw for invalid enum name");
    }
  }

  @Nested
  @DisplayName("values() Tests")
  class ValuesTests {

    @Test
    @DisplayName("values() should return all enum constants")
    void valuesShouldReturnAllEnumConstants() {
      final NnExecutionTarget[] values = NnExecutionTarget.values();
      final Set<NnExecutionTarget> valueSet = new HashSet<>(Arrays.asList(values));

      assertTrue(valueSet.contains(NnExecutionTarget.CPU), "Should contain CPU");
      assertTrue(valueSet.contains(NnExecutionTarget.GPU), "Should contain GPU");
      assertTrue(valueSet.contains(NnExecutionTarget.TPU), "Should contain TPU");
    }

    @Test
    @DisplayName("values() should return new array each time")
    void valuesShouldReturnNewArrayEachTime() {
      final NnExecutionTarget[] first = NnExecutionTarget.values();
      final NnExecutionTarget[] second = NnExecutionTarget.values();

      assertTrue(first != second, "Should return new array each time");
      assertEquals(first.length, second.length, "Arrays should have same length");
    }
  }

  @Nested
  @DisplayName("GetWasiName Tests")
  class GetWasiNameTests {

    @Test
    @DisplayName("CPU should have wasi name 'cpu'")
    void cpuShouldHaveCorrectWasiName() {
      assertEquals("cpu", NnExecutionTarget.CPU.getWasiName(), "CPU wasi name should be 'cpu'");
    }

    @Test
    @DisplayName("GPU should have wasi name 'gpu'")
    void gpuShouldHaveCorrectWasiName() {
      assertEquals("gpu", NnExecutionTarget.GPU.getWasiName(), "GPU wasi name should be 'gpu'");
    }

    @Test
    @DisplayName("TPU should have wasi name 'tpu'")
    void tpuShouldHaveCorrectWasiName() {
      assertEquals("tpu", NnExecutionTarget.TPU.getWasiName(), "TPU wasi name should be 'tpu'");
    }
  }

  @Nested
  @DisplayName("GetDisplayName Tests")
  class GetDisplayNameTests {

    @Test
    @DisplayName("CPU should have display name 'CPU'")
    void cpuShouldHaveCorrectDisplayName() {
      assertEquals(
          "CPU", NnExecutionTarget.CPU.getDisplayName(), "CPU display name should be 'CPU'");
    }

    @Test
    @DisplayName("GPU should have display name 'GPU'")
    void gpuShouldHaveCorrectDisplayName() {
      assertEquals(
          "GPU", NnExecutionTarget.GPU.getDisplayName(), "GPU display name should be 'GPU'");
    }

    @Test
    @DisplayName("TPU should have display name 'TPU'")
    void tpuShouldHaveCorrectDisplayName() {
      assertEquals(
          "TPU", NnExecutionTarget.TPU.getDisplayName(), "TPU display name should be 'TPU'");
    }
  }

  @Nested
  @DisplayName("FromWasiName Tests")
  class FromWasiNameTests {

    @Test
    @DisplayName("should resolve valid wasi names to correct constants")
    void shouldResolveValidWasiNames() {
      assertEquals(
          NnExecutionTarget.CPU, NnExecutionTarget.fromWasiName("cpu"), "Should resolve 'cpu'");
      assertEquals(
          NnExecutionTarget.GPU, NnExecutionTarget.fromWasiName("gpu"), "Should resolve 'gpu'");
      assertEquals(
          NnExecutionTarget.TPU, NnExecutionTarget.fromWasiName("tpu"), "Should resolve 'tpu'");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid wasi name")
    void shouldThrowForInvalidWasiName() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> NnExecutionTarget.fromWasiName("fpga"),
              "Should throw for invalid wasi name");
      assertTrue(
          exception.getMessage().contains("fpga"),
          "Exception message should mention the invalid name: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("GetNativeCode Tests")
  class GetNativeCodeTests {

    @Test
    @DisplayName("getNativeCode should return ordinal for each constant")
    void getNativeCodeShouldReturnOrdinal() {
      for (final NnExecutionTarget target : NnExecutionTarget.values()) {
        assertEquals(
            target.ordinal(),
            target.getNativeCode(),
            "getNativeCode() should return ordinal() for " + target);
      }
    }
  }

  @Nested
  @DisplayName("FromNativeCode Tests")
  class FromNativeCodeTests {

    @Test
    @DisplayName("should resolve valid native codes to correct constants")
    void shouldResolveValidNativeCodes() {
      for (final NnExecutionTarget target : NnExecutionTarget.values()) {
        assertSame(
            target,
            NnExecutionTarget.fromNativeCode(target.getNativeCode()),
            "Should resolve native code " + target.getNativeCode() + " to " + target);
      }
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for invalid native code")
    void shouldThrowForInvalidNativeCode() {
      assertThrows(
          IllegalArgumentException.class,
          () -> NnExecutionTarget.fromNativeCode(-1),
          "Should throw for negative native code");
      assertThrows(
          IllegalArgumentException.class,
          () -> NnExecutionTarget.fromNativeCode(3),
          "Should throw for out-of-range native code 3");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should return displayName")
    void toStringShouldReturnDisplayName() {
      for (final NnExecutionTarget target : NnExecutionTarget.values()) {
        assertEquals(
            target.getDisplayName(),
            target.toString(),
            "toString() for " + target + " should return displayName");
      }
    }
  }
}
