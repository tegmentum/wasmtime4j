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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.ValType;
import ai.tegmentum.wasmtime4j.type.ValTypes;
import java.lang.reflect.Constructor;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ValTypes}.
 *
 * <p>Verifies utility class structure, factory methods, and type creation.
 */
@DisplayName("ValTypes Tests")
class ValTypesTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(ValTypes.class.getModifiers()), "ValTypes should be a final class");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() throws Exception {
      final Constructor<?>[] constructors = ValTypes.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }
  }

  @Nested
  @DisplayName("Factory Method Tests")
  class FactoryMethodTests {

    @Test
    @DisplayName("i32() should return an I32 ValType")
    void i32ShouldReturnI32ValType() {
      final ValType type = ValTypes.i32();
      assertNotNull(type, "i32() should not return null");
      assertEquals(WasmValueType.I32, type.getValueType(), "Should be I32 type");
    }

    @Test
    @DisplayName("i64() should return an I64 ValType")
    void i64ShouldReturnI64ValType() {
      final ValType type = ValTypes.i64();
      assertNotNull(type, "i64() should not return null");
      assertEquals(WasmValueType.I64, type.getValueType(), "Should be I64 type");
    }

    @Test
    @DisplayName("f32() should return an F32 ValType")
    void f32ShouldReturnF32ValType() {
      final ValType type = ValTypes.f32();
      assertNotNull(type, "f32() should not return null");
      assertEquals(WasmValueType.F32, type.getValueType(), "Should be F32 type");
    }

    @Test
    @DisplayName("f64() should return an F64 ValType")
    void f64ShouldReturnF64ValType() {
      final ValType type = ValTypes.f64();
      assertNotNull(type, "f64() should not return null");
      assertEquals(WasmValueType.F64, type.getValueType(), "Should be F64 type");
    }

    @Test
    @DisplayName("v128() should return a V128 ValType")
    void v128ShouldReturnV128ValType() {
      final ValType type = ValTypes.v128();
      assertNotNull(type, "v128() should not return null");
      assertEquals(WasmValueType.V128, type.getValueType(), "Should be V128 type");
    }

    @Test
    @DisplayName("funcref() should return a FUNCREF ValType")
    void funcrefShouldReturnFuncrefValType() {
      final ValType type = ValTypes.funcref();
      assertNotNull(type, "funcref() should not return null");
      assertEquals(WasmValueType.FUNCREF, type.getValueType(), "Should be FUNCREF type");
    }

    @Test
    @DisplayName("externref() should return an EXTERNREF ValType")
    void externrefShouldReturnExternrefValType() {
      final ValType type = ValTypes.externref();
      assertNotNull(type, "externref() should not return null");
      assertEquals(WasmValueType.EXTERNREF, type.getValueType(), "Should be EXTERNREF type");
    }
  }

  @Nested
  @DisplayName("From Method Tests")
  class FromMethodTests {

    @Test
    @DisplayName("from(I32) should return I32 ValType")
    void fromI32ShouldReturnI32ValType() {
      final ValType type = ValTypes.from(WasmValueType.I32);
      assertNotNull(type, "from(I32) should not return null");
      assertEquals(WasmValueType.I32, type.getValueType(), "Should be I32 type");
    }

    @Test
    @DisplayName("from(I64) should return I64 ValType")
    void fromI64ShouldReturnI64ValType() {
      final ValType type = ValTypes.from(WasmValueType.I64);
      assertNotNull(type, "from(I64) should not return null");
      assertEquals(WasmValueType.I64, type.getValueType(), "Should be I64 type");
    }

    @Test
    @DisplayName("from(F32) should return F32 ValType")
    void fromF32ShouldReturnF32ValType() {
      final ValType type = ValTypes.from(WasmValueType.F32);
      assertNotNull(type, "from(F32) should not return null");
      assertEquals(WasmValueType.F32, type.getValueType(), "Should be F32 type");
    }

    @Test
    @DisplayName("from(F64) should return F64 ValType")
    void fromF64ShouldReturnF64ValType() {
      final ValType type = ValTypes.from(WasmValueType.F64);
      assertNotNull(type, "from(F64) should not return null");
      assertEquals(WasmValueType.F64, type.getValueType(), "Should be F64 type");
    }

    @Test
    @DisplayName("from(FUNCREF) should return FUNCREF ValType")
    void fromFuncrefShouldReturnFuncrefValType() {
      final ValType type = ValTypes.from(WasmValueType.FUNCREF);
      assertNotNull(type, "from(FUNCREF) should not return null");
      assertEquals(WasmValueType.FUNCREF, type.getValueType(), "Should be FUNCREF type");
    }

    @Test
    @DisplayName("from(EXTERNREF) should return EXTERNREF ValType")
    void fromExternrefShouldReturnExternrefValType() {
      final ValType type = ValTypes.from(WasmValueType.EXTERNREF);
      assertNotNull(type, "from(EXTERNREF) should not return null");
      assertEquals(WasmValueType.EXTERNREF, type.getValueType(), "Should be EXTERNREF type");
    }
  }

  @Nested
  @DisplayName("Consistency Tests")
  class ConsistencyTests {

    @Test
    @DisplayName("from(I32) should match i32()")
    void fromI32ShouldMatchI32Factory() {
      final ValType fromType = ValTypes.from(WasmValueType.I32);
      final ValType directType = ValTypes.i32();
      assertEquals(
          fromType.getValueType(),
          directType.getValueType(),
          "from(I32) and i32() should return same value type");
    }

    @Test
    @DisplayName("from(F64) should match f64()")
    void fromF64ShouldMatchF64Factory() {
      final ValType fromType = ValTypes.from(WasmValueType.F64);
      final ValType directType = ValTypes.f64();
      assertEquals(
          fromType.getValueType(),
          directType.getValueType(),
          "from(F64) and f64() should return same value type");
    }
  }
}
