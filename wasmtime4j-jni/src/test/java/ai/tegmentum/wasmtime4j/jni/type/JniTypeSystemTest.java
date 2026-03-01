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

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

      assertThat(funcType.getParams()).isEqualTo(params);
      assertThat(funcType.getResults()).isEqualTo(results);
      assertThat(funcType.getKind()).isEqualTo(WasmTypeKind.FUNCTION);
    }

    @Test
    @DisplayName("should create FuncType with empty params and results")
    void shouldCreateFuncTypeWithEmptyParamsAndResults() {
      final JniFuncType funcType =
          new JniFuncType(Collections.emptyList(), Collections.emptyList());

      assertThat(funcType.getParams()).isEmpty();
      assertThat(funcType.getResults()).isEmpty();
    }

    @Test
    @DisplayName("should create FuncType from arrays")
    void shouldCreateFuncTypeFromArrays() {
      final WasmValueType[] params = {WasmValueType.I32, WasmValueType.F32};
      final WasmValueType[] results = {WasmValueType.I64};

      final JniFuncType funcType = new JniFuncType(params, results);

      assertThat(funcType.getParams()).containsExactly(WasmValueType.I32, WasmValueType.F32);
      assertThat(funcType.getResults()).containsExactly(WasmValueType.I64);
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

      assertThat(exception.getMessage()).contains("Parameter type at index 1 is null");
    }

    @Test
    @DisplayName("should reject null element in results list")
    void shouldRejectNullElementInResultsList() {
      final List<WasmValueType> resultsWithNull = Arrays.asList(null, WasmValueType.F64);

      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniFuncType(Collections.emptyList(), resultsWithNull));

      assertThat(exception.getMessage()).contains("Result type at index 0 is null");
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

      assertThat(funcType1).isEqualTo(funcType2);
      assertThat(funcType1).isNotEqualTo(funcType3);
      assertThat(funcType1).isNotEqualTo(null);
      assertThat(funcType1).isNotEqualTo("not a FuncType");
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniFuncType funcType1 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));
      final JniFuncType funcType2 =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.I64));

      assertThat(funcType1.hashCode()).isEqualTo(funcType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniFuncType funcType =
          new JniFuncType(Arrays.asList(WasmValueType.I32), Arrays.asList(WasmValueType.F64));

      final String result = funcType.toString();

      assertThat(result).contains("FuncType");
      assertThat(result).contains("I32");
      assertThat(result).contains("F64");
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

      assertThat(globalType.getValueType()).isEqualTo(WasmValueType.I32);
      assertThat(globalType.isMutable()).isTrue();
      assertThat(globalType.getKind()).isEqualTo(WasmTypeKind.GLOBAL);
    }

    @Test
    @DisplayName("should create immutable GlobalType")
    void shouldCreateImmutableGlobalType() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.F64, false);

      assertThat(globalType.getValueType()).isEqualTo(WasmValueType.F64);
      assertThat(globalType.isMutable()).isFalse();
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

      assertThat(globalType1).isEqualTo(globalType2);
      assertThat(globalType1).isNotEqualTo(globalType3);
      assertThat(globalType1).isNotEqualTo(globalType4);
      assertThat(globalType1).isNotEqualTo(null);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniGlobalType globalType1 = new JniGlobalType(WasmValueType.I64, false);
      final JniGlobalType globalType2 = new JniGlobalType(WasmValueType.I64, false);

      assertThat(globalType1.hashCode()).isEqualTo(globalType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniGlobalType globalType = new JniGlobalType(WasmValueType.F32, true);

      final String result = globalType.toString();

      assertThat(result).contains("GlobalType");
      assertThat(result).contains("F32");
      assertThat(result).contains("true");
    }
  }

  @Nested
  @DisplayName("JniMemoryType Tests")
  class JniMemoryTypeTests {

    @Test
    @DisplayName("should create MemoryType with minimum only")
    void shouldCreateMemoryTypeWithMinimumOnly() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);

      assertThat(memoryType.getMinimum()).isEqualTo(1);
      assertThat(memoryType.getMaximum()).isEmpty();
      assertThat(memoryType.is64Bit()).isFalse();
      assertThat(memoryType.isShared()).isFalse();
      assertThat(memoryType.getKind()).isEqualTo(WasmTypeKind.MEMORY);
    }

    @Test
    @DisplayName("should create MemoryType with maximum")
    void shouldCreateMemoryTypeWithMaximum() {
      final JniMemoryType memoryType = new JniMemoryType(1, 10L, false, false);

      assertThat(memoryType.getMinimum()).isEqualTo(1);
      assertThat(memoryType.getMaximum()).isPresent().contains(10L);
    }

    @Test
    @DisplayName("should create 64-bit shared MemoryType")
    void shouldCreate64BitSharedMemoryType() {
      final JniMemoryType memoryType = new JniMemoryType(0, 100L, true, true);

      assertThat(memoryType.is64Bit()).isTrue();
      assertThat(memoryType.isShared()).isTrue();
    }

    @Test
    @DisplayName("should reject negative minimum")
    void shouldRejectNegativeMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new JniMemoryType(-1, null, false, false));

      assertThat(exception.getMessage()).contains("cannot be negative");
    }

    @Test
    @DisplayName("should reject maximum less than minimum")
    void shouldRejectMaximumLessThanMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class, () -> new JniMemoryType(10, 5L, false, false));

      assertThat(exception.getMessage()).contains("cannot be less than minimum");
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

      assertThat(memoryType1).isEqualTo(memoryType2);
      assertThat(memoryType1).isNotEqualTo(memoryType3);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniMemoryType memoryType1 = new JniMemoryType(1, 10L, false, true);
      final JniMemoryType memoryType2 = new JniMemoryType(1, 10L, false, true);

      assertThat(memoryType1.hashCode()).isEqualTo(memoryType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniMemoryType memoryType = new JniMemoryType(1, 10L, true, false);

      final String result = memoryType.toString();

      assertThat(result).contains("MemoryType");
      assertThat(result).contains("min=1");
      assertThat(result).contains("max=10");
      assertThat(result).contains("64bit=true");
    }

    @Test
    @DisplayName("should show unlimited in toString when no maximum")
    void shouldShowUnlimitedInToStringWhenNoMaximum() {
      final JniMemoryType memoryType = new JniMemoryType(1, null, false, false);

      final String result = memoryType.toString();

      assertThat(result).contains("unlimited");
    }
  }

  @Nested
  @DisplayName("JniTableType Tests")
  class JniTableTypeTests {

    @Test
    @DisplayName("should create TableType with funcref element type")
    void shouldCreateTableTypeWithFuncrefElementType() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, null);

      assertThat(tableType.getElementType()).isEqualTo(WasmValueType.FUNCREF);
      assertThat(tableType.getMinimum()).isEqualTo(1);
      assertThat(tableType.getMaximum()).isEmpty();
      assertThat(tableType.getKind()).isEqualTo(WasmTypeKind.TABLE);
    }

    @Test
    @DisplayName("should create TableType with externref element type")
    void shouldCreateTableTypeWithExternrefElementType() {
      final JniTableType tableType = new JniTableType(WasmValueType.EXTERNREF, 0, 100L);

      assertThat(tableType.getElementType()).isEqualTo(WasmValueType.EXTERNREF);
      assertThat(tableType.getMaximum()).isPresent().contains(100L);
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

      assertThat(exception.getMessage()).contains("reference type");
    }

    @Test
    @DisplayName("should reject negative minimum")
    void shouldRejectNegativeMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniTableType(WasmValueType.FUNCREF, -1, null));

      assertThat(exception.getMessage()).contains("cannot be negative");
    }

    @Test
    @DisplayName("should reject maximum less than minimum")
    void shouldRejectMaximumLessThanMinimum() {
      final IllegalArgumentException exception =
          assertThrows(
              IllegalArgumentException.class,
              () -> new JniTableType(WasmValueType.FUNCREF, 10, 5L));

      assertThat(exception.getMessage()).contains("cannot be less than minimum");
    }

    @Test
    @DisplayName("should implement equals correctly")
    void shouldImplementEqualsCorrectly() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 1, 10L);
      final JniTableType tableType3 = new JniTableType(WasmValueType.EXTERNREF, 1, 10L);

      assertThat(tableType1).isEqualTo(tableType2);
      assertThat(tableType1).isNotEqualTo(tableType3);
    }

    @Test
    @DisplayName("should implement hashCode correctly")
    void shouldImplementHashCodeCorrectly() {
      final JniTableType tableType1 = new JniTableType(WasmValueType.FUNCREF, 0, 50L);
      final JniTableType tableType2 = new JniTableType(WasmValueType.FUNCREF, 0, 50L);

      assertThat(tableType1.hashCode()).isEqualTo(tableType2.hashCode());
    }

    @Test
    @DisplayName("should implement toString correctly")
    void shouldImplementToStringCorrectly() {
      final JniTableType tableType = new JniTableType(WasmValueType.FUNCREF, 1, 100L);

      final String result = tableType.toString();

      assertThat(result).contains("TableType");
      assertThat(result).contains("FUNCREF");
      assertThat(result).contains("min=1");
      assertThat(result).contains("max=100");
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

      assertThat(funcType).isInstanceOf(FuncType.class);
      assertThat(funcType.getParams()).hasSize(1);
      assertThat(funcType.getResults()).hasSize(1);
    }

    @Test
    @DisplayName("JniGlobalType should implement GlobalType interface")
    void jniGlobalTypeShouldImplementGlobalTypeInterface() {
      final GlobalType globalType = new JniGlobalType(WasmValueType.F32, true);

      assertThat(globalType).isInstanceOf(GlobalType.class);
      assertThat(globalType.getValueType()).isEqualTo(WasmValueType.F32);
    }

    @Test
    @DisplayName("JniMemoryType should implement MemoryType interface")
    void jniMemoryTypeShouldImplementMemoryTypeInterface() {
      final MemoryType memoryType = new JniMemoryType(1, 10L, false, false);

      assertThat(memoryType).isInstanceOf(MemoryType.class);
      assertThat(memoryType.getMinimum()).isEqualTo(1);
    }

    @Test
    @DisplayName("JniTableType should implement TableType interface")
    void jniTableTypeShouldImplementTableTypeInterface() {
      final TableType tableType = new JniTableType(WasmValueType.FUNCREF, 0, 100L);

      assertThat(tableType).isInstanceOf(TableType.class);
      assertThat(tableType.getElementType()).isEqualTo(WasmValueType.FUNCREF);
    }
  }
}
