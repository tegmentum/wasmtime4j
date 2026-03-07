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
package ai.tegmentum.wasmtime4j.jni.type;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.type.FuncType;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Unit tests for JNI type system classes.
 *
 * <p>Tests JniFuncType, JniGlobalType, JniMemoryType, and JniTableType without requiring native
 * code.
 */
@DisplayName("JNI Type System Tests")
class JniTypeSystemTest {

  @Nested
  @DisplayName("JniFuncType Tests")
  class JniFuncTypeTests {

    @Test
    @DisplayName("should create FuncType with valid params and results")
    void shouldCreateFuncTypeWithValidParamsAndResults() {
      final List<WasmValueType> params = Arrays.asList(WasmValueType.I32, WasmValueType.I64);
      final List<WasmValueType> results = Collections.singletonList(WasmValueType.F64);

      final JniFuncType funcType = new JniFuncType(params, results);

      assertEquals(params, funcType.getParams());
      assertEquals(results, funcType.getResults());
      assertEquals(WasmTypeKind.FUNCTION, funcType.getKind());
    }

    @Test
    @DisplayName("should create FuncType with empty params and results")
    void shouldCreateFuncTypeWithEmptyParamsAndResults() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.emptyList());

      assertTrue(funcType.getParams().isEmpty());
      assertTrue(funcType.getResults().isEmpty());
    }

    @Test
    @DisplayName("should create FuncType from arrays")
    void shouldCreateFuncTypeFromArrays() {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.F32};
      final WasmValueType[] results = {WasmValueType.I64};

      final JniFuncType funcType = new JniFuncType(params, results);

      assertEquals(
          List.of(WasmValueType.I32, WasmValueType.F32),
          funcType.getParams());
      assertEquals(
          List.of(WasmValueType.I64),
          funcType.getResults());
    }

    @Test
    @DisplayName("should reject null params list")
    void shouldRejectNullParamsList() {
      assertThrows(
          IllegalArgumentException.class, () -> new JniFuncType(null, Collections.emptyList()));
    }

    @Test
    @DisplayName("should reject null results list")
    void shouldRejectNullResultsList() {
      assertThrows(
          IllegalArgumentException.class, () -> new JniFuncType(Collections.emptyList(), null));
    }

    @Test
    @DisplayName("should reject null element in params list")
    void shouldRejectNullElementInParamsList() {
      final List<WasmValueType> paramsWithNull = Arrays.asList(WasmValueType.I32, null);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniFuncType(paramsWithNull, Collections.emptyList()));

      assertTrue(
          exception.getMessage().contains("Parameter type at index 1 is null"),
          "Expected message to contain: Parameter type at index 1 is null");
    }

    @Test
    @DisplayName("should reject null element in results list")
    void shouldRejectNullElementInResultsList() {
      final List<WasmValueType> resultsWithNull = Arrays.asList(null, WasmValueType.F64);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniFuncType(Collections.emptyList(), resultsWithNull));

      assertTrue(
          exception.getMessage().contains("Result type at index 0 is null"),
          "Expected message to contain: Result type at index 0 is null");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final JniFuncType funcType1 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType2 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType3 =
          new JniFuncType(Arrays.asList(WasmValueType.F32), Arrays.asList(WasmValueType.I64));

      assertEquals(funcType1, funcType2);
      assertNotEquals(funcType1, funcType3);
      assertNotEquals(null, funcType1);
      assertNotEquals("not a FuncType", funcType1);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniFuncType funcType1 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType2 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));

      assertEquals(funcType1.hashCode(), funcType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniFuncType funcType =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.F64));

      final String result = funcType.toString();

      assertTrue(result.contains("FuncType"), "Expected string to contain: FuncType");
      assertTrue(result.contains("I32"), "Expected string to contain: I32");
      assertTrue(result.contains("F64"), "Expected string to contain: F64");
    }

    @Test
    @DisplayName("params list should be immutable")
    void paramsListShouldBeImmutable() {
      final JniFuncType funcType =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Collections.emptyList());

      assertThrows(
          UnsupportedOperationException.class, () -> funcType.getParams().add(WasmValueType.I64));
    }

    @Test
    @DisplayName("results list should be immutable")
    void resultsListShouldBeImmutable() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Arrays.asList(WasmValueType.F32));

      assertThrows(
          UnsupportedOperationException.class, () -> funcType.getResults().add(WasmValueType.F64));
    }
  }

  @Nested
  @DisplayName("JniGlobalType Tests")
  class JniGlobalTypeTests {

    @Test
    @DisplayName("should create mutable GlobalType")
    void shouldCreateMutableGlobalType() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.I32, true);

      assertEquals(WasmValueType.I32, globalType.getValueType());
      assertTrue(globalType.isMutable());
      assertEquals(WasmTypeKind.GLOBAL, globalType.getKind());
    }

    @Test
    @DisplayName("should create immutable GlobalType")
    void shouldCreateImmutableGlobalType() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.F64, false);

      assertEquals(WasmValueType.F64, globalType.getValueType());
      assertFalse(globalType.isMutable());
    }

    @Test
    @DisplayName("should reject null value type")
    void shouldRejectNullValueType() {
      assertThrows(IllegalArgumentException.class, () -> new JniGlobalType(null, true));
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I32, true);
      final JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I32, true);
      final JniGlobalType globalType3 = new JniGlobalType(WasmValueType.I32, false);
      final JniGlobalType globalType4 = new JniGlobalType(WasmValueType.I64, true);

      assertEquals(globalType1, globalType2);
      assertNotEquals(globalType1, globalType3);
      assertNotEquals(globalType1, globalType4);
      assertNotEquals(null, globalType1);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I64, false);
      final JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I64, false);

      assertEquals(globalType1.hashCode(), globalType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.F32, true);

      final String result = globalType.toString();

      assertTrue(result.contains("GlobalType"), "Expected string to contain: GlobalType");
      assertTrue(result.contains("F32"), "Expected string to contain: F32");
      assertTrue(result.contains("true"), "Expected string to contain: true");
    }
  }

  @Nested
  @DisplayName("JniMemoryType Tests")
  class JniMemoryTypeTests {

    @Test
    @DisplayName("should create MemoryType with minimum only")
    void shouldCreateMemoryTypeWithMinimumOnly() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);

      assertEquals(1, memoryType.getMinimum());
      assertTrue(memoryType.getMaximum().isEmpty());
      assertFalse(memoryType.is64Bit());
      assertFalse(memoryType.isShared());
      assertEquals(WasmTypeKind.MEMORY, memoryType.getKind());
    }

    @Test
    @DisplayName("should create MemoryType with maximum")
    void shouldCreateMemoryTypeWithMaximum() {
      final JniMemoryType memoryType = new JniMemoryType(1, 10L, false, false);

      assertEquals(1, memoryType.getMinimum());
      assertTrue(memoryType.getMaximum().isPresent());
      assertEquals(10L, memoryType.getMaximum().get());
    }

    @Test
    @DisplayName("should create 64-bit shared MemoryType")
    void shouldCreate64BitSharedMemoryType() {
      final JniMemoryType memoryType = new JniMemoryType(0, 100L, true, true);

      assertTrue(memoryType.is64Bit());
      assertTrue(memoryType.isShared());
    }

    @Test
    @DisplayName("should reject negative minimum")
    void shouldRejectNegativeMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new JniMemoryType(-1, null, false, false));

      assertTrue(
          exception.getMessage().contains("cannot be negative"),
          "Expected message to contain: cannot be negative");
    }

    @Test
    @DisplayName("should reject maximum less than minimum")
    void shouldRejectMaximumLessThanMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new JniMemoryType(10, 5L, false, false));

      assertTrue(
          exception.getMessage().contains("cannot be less than minimum"),
          "Expected message to contain: cannot be less than minimum");
    }

    @Test
    @DisplayName("should allow maximum equal to minimum")
    void shouldAllowMaximumEqualToMinimum() {
      assertDoesNotThrow(() -> new JniMemoryType(5, 5L, false, false));
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, 10L, true, false);
      final JniMemoryType memoryType2 = new JniMemoryType(1, 10L, true, false);
      final JniMemoryType memoryType3 = new JniMemoryType(2, 10L, true, false);

      assertEquals(memoryType1, memoryType2);
      assertNotEquals(memoryType1, memoryType3);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, 10L, false, true);
      final JniMemoryType memoryType2 = new JniMemoryType(1, 10L, false, true);

      assertEquals(memoryType1.hashCode(), memoryType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniMemoryType memoryType = new JniMemoryType(1, 10L, true, false);

      final String result = memoryType.toString();

      assertTrue(result.contains("MemoryType"), "Expected string to contain: MemoryType");
      assertTrue(result.contains("min=1"), "Expected string to contain: min=1");
      assertTrue(result.contains("max=10"), "Expected string to contain: max=10");
      assertTrue(result.contains("64bit=true"), "Expected string to contain: 64bit=true");
    }

    @Test
    @DisplayName("should show unlimited in toString when no maximum")
    void shouldShowUnlimitedInToStringWhenNoMaximum() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);

      final String result = memoryType.toString();

      assertTrue(result.contains("unlimited"), "Expected string to contain: unlimited");
    }
  }

  @Nested
  @DisplayName("JniTableType Tests")
  class JniTableTypeTests {

    @Test
    @DisplayName("should create TableType with funcref element type")
    void shouldCreateTableTypeWithFuncrefElementType() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, null);

      assertEquals(WasmValueType.FUNCREF, tableType.getElementType());
      assertEquals(1, tableType.getMinimum());
      assertTrue(tableType.getMaximum().isEmpty());
      assertEquals(WasmTypeKind.TABLE, tableType.getKind());
    }

    @Test
    @DisplayName("should create TableType with externref element type")
    void shouldCreateTableTypeWithExternrefElementType() {
      final JniTableType tableType = new JniTableType(WasmValueType.EXTERNREF, 0, 100L);

      assertEquals(WasmValueType.EXTERNREF, tableType.getElementType());
      assertTrue(tableType.getMaximum().isPresent());
      assertEquals(100L, tableType.getMaximum().get());
    }

    @Test
    @DisplayName("should reject null element type")
    void shouldRejectNullElementType() {
      assertThrows(IllegalArgumentException.class, () -> new JniTableType(null, 0, null));
    }

    @Test
    @DisplayName("should reject non-reference element type")
    void shouldRejectNonReferenceElementType() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new JniTableType(WasmValueType.I32, 0, null));

      assertTrue(
          exception.getMessage().contains("reference type"),
          "Expected message to contain: reference type");
    }

    @Test
    @DisplayName("should reject negative minimum")
    void shouldRejectNegativeMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniTableType(WasmValueType.FUNCREF, -1, null));

      assertTrue(
          exception.getMessage().contains("cannot be negative"),
          "Expected message to contain: cannot be negative");
    }

    @Test
    @DisplayName("should reject maximum less than minimum")
    void shouldRejectMaximumLessThanMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniTableType(WasmValueType.FUNCREF, 10, 5L));

      assertTrue(
          exception.getMessage().contains("cannot be less than minimum"),
          "Expected message to contain: cannot be less than minimum");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);
      final JniTableType tableType3 = new JniTableType(WasmValueType.EXTERNREF, 1, 10L);

      assertEquals(tableType1, tableType2);
      assertNotEquals(tableType1, tableType3);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 0, 50L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 0, 50L);

      assertEquals(tableType1.hashCode(), tableType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, 100L);

      final String result = tableType.toString();

      assertTrue(result.contains("TableType"), "Expected string to contain: TableType");
      assertTrue(result.contains("FUNCREF"), "Expected string to contain: FUNCREF");
      assertTrue(result.contains("min=1"), "Expected string to contain: min=1");
      assertTrue(result.contains("max=100"), "Expected string to contain: max=100");
    }
  }

  @Nested
  @DisplayName("Interface Compatibility Tests")
  class InterfaceCompatibilityTests {

    @Test
    @DisplayName("JniFuncType should implement FuncType interface")
    void jniFuncTypeShouldImplementFuncTypeInterface() {
      final FuncType funcType =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));

      assertInstanceOf(FuncType.class, funcType);
      assertEquals(1, funcType.getParams().size());
      assertEquals(1, funcType.getResults().size());
    }

    @Test
    @DisplayName("JniGlobalType should implement GlobalType interface")
    void jniGlobalTypeShouldImplementGlobalTypeInterface() {
      final GlobalType globalType = new JniGlobalType(WasmValueType.F32, true);

      assertInstanceOf(GlobalType.class, globalType);
      assertEquals(WasmValueType.F32, globalType.getValueType());
    }

    @Test
    @DisplayName("JniMemoryType should implement MemoryType interface")
    void jniMemoryTypeShouldImplementMemoryTypeInterface() {
      final MemoryType memoryType = new JniMemoryType(1, 10L, false, false);

      assertInstanceOf(MemoryType.class, memoryType);
      assertEquals(1, memoryType.getMinimum());
    }

    @Test
    @DisplayName("JniTableType should implement TableType interface")
    void jniTableTypeShouldImplementTableTypeInterface() {
      final TableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, 100L);

      assertInstanceOf(TableType.class, tableType);
      assertEquals(WasmValueType.FUNCREF, tableType.getElementType());
    }
  }
}
