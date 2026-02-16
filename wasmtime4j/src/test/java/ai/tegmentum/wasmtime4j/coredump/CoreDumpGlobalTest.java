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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoreDumpGlobal} class.
 *
 * <p>CoreDumpGlobal is the default implementation of the CoreDumpGlobal interface.
 */
@DisplayName("CoreDumpGlobal Tests")
class CoreDumpGlobalTest {

  @Nested
  @DisplayName("Builder Tests")
  class BuilderTests {

    @Test
    @DisplayName("should create builder via static method")
    void shouldCreateBuilderViaStaticMethod() {
      final CoreDumpGlobal.Builder builder = CoreDumpGlobal.builder();
      assertNotNull(builder, "Builder should not be null");
    }

    @Test
    @DisplayName("should build with instanceIndex")
    void shouldBuildWithInstanceIndex() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().instanceIndex(3).valueType(WasmValueType.I32).build();
      assertEquals(3, global.getInstanceIndex(), "InstanceIndex should match");
    }

    @Test
    @DisplayName("should build with globalIndex")
    void shouldBuildWithGlobalIndex() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().globalIndex(5).valueType(WasmValueType.I32).build();
      assertEquals(5, global.getGlobalIndex(), "GlobalIndex should match");
    }

    @Test
    @DisplayName("should build with name")
    void shouldBuildWithName() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().name("counter").valueType(WasmValueType.I32).build();
      assertTrue(global.getName().isPresent(), "Name should be present");
      assertEquals("counter", global.getName().get(), "Name should match");
    }

    @Test
    @DisplayName("should build without name")
    void shouldBuildWithoutName() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().valueType(WasmValueType.I32).build();
      assertFalse(global.getName().isPresent(), "Name should not be present");
    }

    @Test
    @DisplayName("should build with mutable flag")
    void shouldBuildWithMutableFlag() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().mutable(true).valueType(WasmValueType.I32).build();
      assertTrue(global.isMutable(), "Mutable should be true");
    }

    @Test
    @DisplayName("should build with mutable false by default")
    void shouldBuildWithMutableFalseByDefault() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().valueType(WasmValueType.I32).build();
      assertFalse(global.isMutable(), "Mutable should default to false");
    }

    @Test
    @DisplayName("should throw when valueType is null")
    void shouldThrowWhenValueTypeIsNull() {
      assertThrows(
          NullPointerException.class,
          () -> CoreDumpGlobal.builder().build(),
          "Should throw when valueType is null");
    }
  }

  @Nested
  @DisplayName("I32 Value Tests")
  class I32ValueTests {

    @Test
    @DisplayName("should build with i32 value")
    void shouldBuildWithI32Value() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().i32Value(42).build();
      assertEquals(WasmValueType.I32, global.getValueType(), "ValueType should be I32");
      assertEquals(42, global.getI32Value(), "I32 value should match");
    }

    @Test
    @DisplayName("should handle negative i32 value")
    void shouldHandleNegativeI32Value() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().i32Value(-100).build();
      assertEquals(-100, global.getI32Value(), "Negative I32 value should match");
    }

    @Test
    @DisplayName("should handle max i32 value")
    void shouldHandleMaxI32Value() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().i32Value(Integer.MAX_VALUE).build();
      assertEquals(Integer.MAX_VALUE, global.getI32Value(), "Max I32 value should match");
    }

    @Test
    @DisplayName("should handle min i32 value")
    void shouldHandleMinI32Value() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().i32Value(Integer.MIN_VALUE).build();
      assertEquals(Integer.MIN_VALUE, global.getI32Value(), "Min I32 value should match");
    }

    @Test
    @DisplayName("should throw when getting i32 from wrong type")
    void shouldThrowWhenGettingI32FromWrongType() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().i64Value(100L).build();
      assertThrows(
          IllegalStateException.class,
          global::getI32Value,
          "Should throw when value type is not I32");
    }
  }

  @Nested
  @DisplayName("I64 Value Tests")
  class I64ValueTests {

    @Test
    @DisplayName("should build with i64 value")
    void shouldBuildWithI64Value() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().i64Value(123456789L).build();
      assertEquals(WasmValueType.I64, global.getValueType(), "ValueType should be I64");
      assertEquals(123456789L, global.getI64Value(), "I64 value should match");
    }

    @Test
    @DisplayName("should handle large i64 value")
    void shouldHandleLargeI64Value() {
      final long largeValue = Long.MAX_VALUE - 1000;
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().i64Value(largeValue).build();
      assertEquals(largeValue, global.getI64Value(), "Large I64 value should match");
    }

    @Test
    @DisplayName("should throw when getting i64 from wrong type")
    void shouldThrowWhenGettingI64FromWrongType() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().i32Value(100).build();
      assertThrows(
          IllegalStateException.class,
          global::getI64Value,
          "Should throw when value type is not I64");
    }
  }

  @Nested
  @DisplayName("F32 Value Tests")
  class F32ValueTests {

    @Test
    @DisplayName("should build with f32 value")
    void shouldBuildWithF32Value() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().f32Value(3.14f).build();
      assertEquals(WasmValueType.F32, global.getValueType(), "ValueType should be F32");
      assertEquals(3.14f, global.getF32Value(), 0.001f, "F32 value should match");
    }

    @Test
    @DisplayName("should handle negative f32 value")
    void shouldHandleNegativeF32Value() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().f32Value(-2.5f).build();
      assertEquals(-2.5f, global.getF32Value(), 0.001f, "Negative F32 value should match");
    }

    @Test
    @DisplayName("should throw when getting f32 from wrong type")
    void shouldThrowWhenGettingF32FromWrongType() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().i32Value(100).build();
      assertThrows(
          IllegalStateException.class,
          global::getF32Value,
          "Should throw when value type is not F32");
    }
  }

  @Nested
  @DisplayName("F64 Value Tests")
  class F64ValueTests {

    @Test
    @DisplayName("should build with f64 value")
    void shouldBuildWithF64Value() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().f64Value(3.141592653589793).build();
      assertEquals(WasmValueType.F64, global.getValueType(), "ValueType should be F64");
      assertEquals(3.141592653589793, global.getF64Value(), 0.000001, "F64 value should match");
    }

    @Test
    @DisplayName("should handle very small f64 value")
    void shouldHandleVerySmallF64Value() {
      final double smallValue = 1.0e-300;
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().f64Value(smallValue).build();
      assertEquals(smallValue, global.getF64Value(), 1.0e-310, "Very small F64 value should match");
    }

    @Test
    @DisplayName("should throw when getting f64 from wrong type")
    void shouldThrowWhenGettingF64FromWrongType() {
      final CoreDumpGlobal global = CoreDumpGlobal.builder().i32Value(100).build();
      assertThrows(
          IllegalStateException.class,
          global::getF64Value,
          "Should throw when value type is not F64");
    }
  }

  @Nested
  @DisplayName("Raw Value Tests")
  class RawValueTests {

    @Test
    @DisplayName("should build with raw value")
    void shouldBuildWithRawValue() {
      final byte[] rawValue = {0x2A, 0x00, 0x00, 0x00}; // 42 in little-endian
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().valueType(WasmValueType.I32).rawValue(rawValue).build();
      assertArrayEquals(rawValue, global.getRawValue(), "Raw value should match");
    }

    @Test
    @DisplayName("should return copy of raw value")
    void shouldReturnCopyOfRawValue() {
      final byte[] original = {0x01, 0x02, 0x03, 0x04};
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().valueType(WasmValueType.I32).rawValue(original).build();
      final byte[] retrieved = global.getRawValue();
      retrieved[0] = (byte) 0xFF;
      assertArrayEquals(original, global.getRawValue(), "Internal data should not be modified");
    }

    @Test
    @DisplayName("should return empty array for null raw value")
    void shouldReturnEmptyArrayForNullRawValue() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder().valueType(WasmValueType.I32).rawValue(null).build();
      assertEquals(0, global.getRawValue().length, "Raw value should be empty");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("should return meaningful string representation")
    void shouldReturnMeaningfulStringRepresentation() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder()
              .instanceIndex(0)
              .globalIndex(1)
              .name("counter")
              .i32Value(42)
              .mutable(true)
              .build();
      final String str = global.toString();
      assertTrue(str.contains("CoreDumpGlobal"), "Should contain class name");
      assertTrue(str.contains("counter"), "Should contain name");
      assertTrue(str.contains("I32"), "Should contain value type");
      assertTrue(str.contains("mutable=true"), "Should contain mutable flag");
    }
  }

  @Nested
  @DisplayName("Full Integration Tests")
  class FullIntegrationTests {

    @Test
    @DisplayName("should build complete i32 global")
    void shouldBuildCompleteI32Global() {
      final CoreDumpGlobal global =
          CoreDumpGlobal.builder()
              .instanceIndex(0)
              .globalIndex(0)
              .name("stack_pointer")
              .i32Value(65536)
              .mutable(true)
              .build();

      assertEquals(0, global.getInstanceIndex(), "InstanceIndex should match");
      assertEquals(0, global.getGlobalIndex(), "GlobalIndex should match");
      assertEquals("stack_pointer", global.getName().get(), "Name should match");
      assertEquals(WasmValueType.I32, global.getValueType(), "ValueType should be I32");
      assertEquals(65536, global.getI32Value(), "Value should match");
      assertTrue(global.isMutable(), "Should be mutable");
    }

    @Test
    @DisplayName("should correctly encode and decode all types")
    void shouldCorrectlyEncodeAndDecodeAllTypes() {
      // Test I32
      final CoreDumpGlobal i32Global =
          CoreDumpGlobal.builder().i32Value(12345).build();
      assertEquals(12345, i32Global.getI32Value(), "I32 should roundtrip correctly");

      // Test I64
      final CoreDumpGlobal i64Global =
          CoreDumpGlobal.builder().i64Value(9876543210L).build();
      assertEquals(9876543210L, i64Global.getI64Value(), "I64 should roundtrip correctly");

      // Test F32
      final CoreDumpGlobal f32Global =
          CoreDumpGlobal.builder().f32Value(1.5f).build();
      assertEquals(1.5f, f32Global.getF32Value(), 0.0001f, "F32 should roundtrip correctly");

      // Test F64
      final CoreDumpGlobal f64Global =
          CoreDumpGlobal.builder().f64Value(2.718281828).build();
      assertEquals(
          2.718281828, f64Global.getF64Value(), 0.000001, "F64 should roundtrip correctly");
    }
  }
}
