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

import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link GlobalType} interface.
 *
 * <p>GlobalType represents the type information of a WebAssembly global variable.
 */
@DisplayName("GlobalType Tests")
class GlobalTypeTest {

  /** Test implementation of GlobalType for testing purposes. */
  private static class TestGlobalType implements GlobalType {
    private final WasmValueType valueType;
    private final boolean mutable;

    TestGlobalType(final WasmValueType valueType, final boolean mutable) {
      this.valueType = valueType;
      this.mutable = mutable;
    }

    @Override
    public WasmValueType getValueType() {
      return valueType;
    }

    @Override
    public boolean isMutable() {
      return mutable;
    }
  }

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be public interface")
    void shouldBePublicInterface() {
      assertTrue(Modifier.isPublic(GlobalType.class.getModifiers()), "GlobalType should be public");
      assertTrue(GlobalType.class.isInterface(), "GlobalType should be an interface");
    }

    @Test
    @DisplayName("should extend WasmType")
    void shouldExtendWasmType() {
      assertTrue(
          WasmType.class.isAssignableFrom(GlobalType.class), "GlobalType should extend WasmType");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      final Method method = GlobalType.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "getValueType should return WasmValueType");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      final Method method = GlobalType.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "isMutable should return boolean");
    }
  }

  @Nested
  @DisplayName("Default getKind Method Tests")
  class DefaultGetKindMethodTests {

    @Test
    @DisplayName("getKind should return GLOBAL")
    void getKindShouldReturnGlobal() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.I32, false);

      assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "getKind should return GLOBAL");
    }

    @Test
    @DisplayName("getKind should be a default method")
    void getKindShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = GlobalType.class.getMethod("getKind");
      assertTrue(method.isDefault(), "getKind should be a default method");
    }
  }

  @Nested
  @DisplayName("Value Type Tests")
  class ValueTypeTests {

    @Test
    @DisplayName("should handle I32 value type")
    void shouldHandleI32ValueType() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.I32, false);

      assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
    }

    @Test
    @DisplayName("should handle I64 value type")
    void shouldHandleI64ValueType() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.I64, true);

      assertEquals(WasmValueType.I64, globalType.getValueType(), "Value type should be I64");
    }

    @Test
    @DisplayName("should handle F32 value type")
    void shouldHandleF32ValueType() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.F32, false);

      assertEquals(WasmValueType.F32, globalType.getValueType(), "Value type should be F32");
    }

    @Test
    @DisplayName("should handle F64 value type")
    void shouldHandleF64ValueType() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.F64, true);

      assertEquals(WasmValueType.F64, globalType.getValueType(), "Value type should be F64");
    }

    @Test
    @DisplayName("should handle FUNCREF value type")
    void shouldHandleFuncrefValueType() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.FUNCREF, false);

      assertEquals(
          WasmValueType.FUNCREF, globalType.getValueType(), "Value type should be FUNCREF");
    }

    @Test
    @DisplayName("should handle EXTERNREF value type")
    void shouldHandleExternrefValueType() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.EXTERNREF, true);

      assertEquals(
          WasmValueType.EXTERNREF, globalType.getValueType(), "Value type should be EXTERNREF");
    }
  }

  @Nested
  @DisplayName("Mutability Tests")
  class MutabilityTests {

    @Test
    @DisplayName("should handle immutable global (const)")
    void shouldHandleImmutableGlobal() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.I32, false);

      assertFalse(globalType.isMutable(), "Global should be immutable");
    }

    @Test
    @DisplayName("should handle mutable global (var)")
    void shouldHandleMutableGlobal() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.I32, true);

      assertTrue(globalType.isMutable(), "Global should be mutable");
    }
  }

  @Nested
  @DisplayName("Combined Value Type and Mutability Tests")
  class CombinedTests {

    @Test
    @DisplayName("should handle immutable I32")
    void shouldHandleImmutableI32() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.I32, false);

      assertEquals(WasmValueType.I32, globalType.getValueType(), "Value type should be I32");
      assertFalse(globalType.isMutable(), "Should be immutable");
      assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "Kind should be GLOBAL");
    }

    @Test
    @DisplayName("should handle mutable F64")
    void shouldHandleMutableF64() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.F64, true);

      assertEquals(WasmValueType.F64, globalType.getValueType(), "Value type should be F64");
      assertTrue(globalType.isMutable(), "Should be mutable");
      assertEquals(WasmTypeKind.GLOBAL, globalType.getKind(), "Kind should be GLOBAL");
    }
  }

  @Nested
  @DisplayName("WasmType Integration Tests")
  class WasmTypeIntegrationTests {

    @Test
    @DisplayName("GlobalType should implement WasmType")
    void globalTypeShouldImplementWasmType() {
      final GlobalType globalType = new TestGlobalType(WasmValueType.I32, false);

      assertTrue(globalType instanceof WasmType, "GlobalType should be instance of WasmType");
    }

    @Test
    @DisplayName("getKind should return GLOBAL for all value types")
    void getKindShouldReturnGlobalForAllValueTypes() {
      final GlobalType i32Global = new TestGlobalType(WasmValueType.I32, false);
      final GlobalType i64Global = new TestGlobalType(WasmValueType.I64, true);
      final GlobalType f32Global = new TestGlobalType(WasmValueType.F32, false);
      final GlobalType f64Global = new TestGlobalType(WasmValueType.F64, true);

      assertEquals(WasmTypeKind.GLOBAL, i32Global.getKind(), "I32 global should be GLOBAL kind");
      assertEquals(WasmTypeKind.GLOBAL, i64Global.getKind(), "I64 global should be GLOBAL kind");
      assertEquals(WasmTypeKind.GLOBAL, f32Global.getKind(), "F32 global should be GLOBAL kind");
      assertEquals(WasmTypeKind.GLOBAL, f64Global.getKind(), "F64 global should be GLOBAL kind");
    }
  }

  @Nested
  @DisplayName("All WasmValueType Tests")
  class AllWasmValueTypeTests {

    @Test
    @DisplayName("should work with all WasmValueType values")
    void shouldWorkWithAllWasmValueTypeValues() {
      for (final WasmValueType valueType : WasmValueType.values()) {
        final GlobalType globalType = new TestGlobalType(valueType, false);

        assertEquals(valueType, globalType.getValueType(), "Should handle " + valueType.name());
        assertEquals(
            WasmTypeKind.GLOBAL,
            globalType.getKind(),
            "Kind should be GLOBAL for " + valueType.name());
      }
    }
  }
}
