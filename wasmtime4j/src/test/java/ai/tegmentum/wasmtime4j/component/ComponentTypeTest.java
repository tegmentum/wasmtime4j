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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ComponentType}.
 *
 * @since 1.1.0
 */
@DisplayName("ComponentType")
class ComponentTypeTest {

  @Test
  @DisplayName("has all expected values")
  void hasAllValues() {
    assertEquals(26, ComponentType.values().length, "Should have 26 component type values");
  }

  @Nested
  @DisplayName("isPrimitive")
  class IsPrimitive {

    @Test
    @DisplayName("primitive types return true")
    void primitiveTypesReturnTrue() {
      assertTrue(ComponentType.BOOL.isPrimitive());
      assertTrue(ComponentType.S8.isPrimitive());
      assertTrue(ComponentType.S16.isPrimitive());
      assertTrue(ComponentType.S32.isPrimitive());
      assertTrue(ComponentType.S64.isPrimitive());
      assertTrue(ComponentType.U8.isPrimitive());
      assertTrue(ComponentType.U16.isPrimitive());
      assertTrue(ComponentType.U32.isPrimitive());
      assertTrue(ComponentType.U64.isPrimitive());
      assertTrue(ComponentType.F32.isPrimitive());
      assertTrue(ComponentType.F64.isPrimitive());
      assertTrue(ComponentType.CHAR.isPrimitive());
      assertTrue(ComponentType.STRING.isPrimitive());
    }

    @Test
    @DisplayName("compound types return false")
    void compoundTypesReturnFalse() {
      assertFalse(ComponentType.LIST.isPrimitive());
      assertFalse(ComponentType.RECORD.isPrimitive());
      assertFalse(ComponentType.TUPLE.isPrimitive());
      assertFalse(ComponentType.OWN.isPrimitive());
      assertFalse(ComponentType.FUTURE.isPrimitive());
    }
  }

  @Nested
  @DisplayName("isInteger")
  class IsInteger {

    @Test
    @DisplayName("integer types return true")
    void integerTypesReturnTrue() {
      assertTrue(ComponentType.S8.isInteger());
      assertTrue(ComponentType.S16.isInteger());
      assertTrue(ComponentType.S32.isInteger());
      assertTrue(ComponentType.S64.isInteger());
      assertTrue(ComponentType.U8.isInteger());
      assertTrue(ComponentType.U16.isInteger());
      assertTrue(ComponentType.U32.isInteger());
      assertTrue(ComponentType.U64.isInteger());
    }

    @Test
    @DisplayName("non-integer types return false")
    void nonIntegerTypesReturnFalse() {
      assertFalse(ComponentType.BOOL.isInteger());
      assertFalse(ComponentType.F32.isInteger());
      assertFalse(ComponentType.STRING.isInteger());
      assertFalse(ComponentType.LIST.isInteger());
    }
  }

  @Nested
  @DisplayName("isSigned")
  class IsSigned {

    @Test
    @DisplayName("signed types return true")
    void signedTypesReturnTrue() {
      assertTrue(ComponentType.S8.isSigned());
      assertTrue(ComponentType.S16.isSigned());
      assertTrue(ComponentType.S32.isSigned());
      assertTrue(ComponentType.S64.isSigned());
    }

    @Test
    @DisplayName("unsigned types return false")
    void unsignedTypesReturnFalse() {
      assertFalse(ComponentType.U8.isSigned());
      assertFalse(ComponentType.U16.isSigned());
      assertFalse(ComponentType.U32.isSigned());
      assertFalse(ComponentType.U64.isSigned());
    }
  }

  @Nested
  @DisplayName("isUnsigned")
  class IsUnsigned {

    @Test
    @DisplayName("unsigned types return true")
    void unsignedTypesReturnTrue() {
      assertTrue(ComponentType.U8.isUnsigned());
      assertTrue(ComponentType.U16.isUnsigned());
      assertTrue(ComponentType.U32.isUnsigned());
      assertTrue(ComponentType.U64.isUnsigned());
    }

    @Test
    @DisplayName("signed types return false")
    void signedTypesReturnFalse() {
      assertFalse(ComponentType.S8.isUnsigned());
      assertFalse(ComponentType.S32.isUnsigned());
    }
  }

  @Nested
  @DisplayName("isFloat")
  class IsFloat {

    @Test
    @DisplayName("float types return true")
    void floatTypesReturnTrue() {
      assertTrue(ComponentType.F32.isFloat());
      assertTrue(ComponentType.F64.isFloat());
    }

    @Test
    @DisplayName("non-float types return false")
    void nonFloatTypesReturnFalse() {
      assertFalse(ComponentType.S32.isFloat());
      assertFalse(ComponentType.STRING.isFloat());
    }
  }

  @Nested
  @DisplayName("isCompound")
  class IsCompound {

    @Test
    @DisplayName("compound types return true")
    void compoundTypesReturnTrue() {
      assertTrue(ComponentType.LIST.isCompound());
      assertTrue(ComponentType.RECORD.isCompound());
      assertTrue(ComponentType.TUPLE.isCompound());
      assertTrue(ComponentType.VARIANT.isCompound());
      assertTrue(ComponentType.ENUM.isCompound());
      assertTrue(ComponentType.OPTION.isCompound());
      assertTrue(ComponentType.RESULT.isCompound());
      assertTrue(ComponentType.FLAGS.isCompound());
    }

    @Test
    @DisplayName("non-compound types return false")
    void nonCompoundTypesReturnFalse() {
      assertFalse(ComponentType.S32.isCompound());
      assertFalse(ComponentType.OWN.isCompound());
      assertFalse(ComponentType.FUTURE.isCompound());
    }
  }

  @Nested
  @DisplayName("isResource")
  class IsResource {

    @Test
    @DisplayName("resource types return true")
    void resourceTypesReturnTrue() {
      assertTrue(ComponentType.OWN.isResource());
      assertTrue(ComponentType.BORROW.isResource());
    }

    @Test
    @DisplayName("non-resource types return false")
    void nonResourceTypesReturnFalse() {
      assertFalse(ComponentType.S32.isResource());
      assertFalse(ComponentType.FUTURE.isResource());
    }
  }

  @Nested
  @DisplayName("isAsync")
  class IsAsync {

    @Test
    @DisplayName("async types return true")
    void asyncTypesReturnTrue() {
      assertTrue(ComponentType.FUTURE.isAsync());
      assertTrue(ComponentType.STREAM.isAsync());
      assertTrue(ComponentType.ERROR_CONTEXT.isAsync());
    }

    @Test
    @DisplayName("non-async types return false")
    void nonAsyncTypesReturnFalse() {
      assertFalse(ComponentType.S32.isAsync());
      assertFalse(ComponentType.OWN.isAsync());
      assertFalse(ComponentType.LIST.isAsync());
    }
  }

  @Test
  @DisplayName("valueOf roundtrips all values")
  void valueOfRoundtrips() {
    for (final ComponentType type : ComponentType.values()) {
      assertEquals(type, ComponentType.valueOf(type.name()));
    }
  }
}
