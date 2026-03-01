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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.type.FunctionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultTagType}.
 *
 * <p>Verifies constructor, getFunctionType, equals, hashCode, toString, and null handling.
 */
@DisplayName("DefaultTagType Tests")
class DefaultTagTypeTest {

  private FunctionType createFuncType(final WasmValueType[] params, final WasmValueType[] returns) {
    return new FunctionType(params, returns);
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create with valid function type")
    void shouldCreateWithValidFunctionType() {
      final FunctionType funcType =
          createFuncType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      assertNotNull(tagType, "DefaultTagType should be created successfully");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null function type")
    void shouldThrowForNullFunctionType() {
      assertThrows(
          IllegalArgumentException.class,
          () -> new DefaultTagType(null),
          "Constructor should throw IllegalArgumentException for null funcType");
    }
  }

  @Nested
  @DisplayName("GetFunctionType Tests")
  class GetFunctionTypeTests {

    @Test
    @DisplayName("should return the wrapped function type")
    void shouldReturnWrappedFunctionType() {
      final FunctionType funcType =
          createFuncType(
              new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {WasmValueType.I64});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      assertSame(funcType, tagType.getFunctionType(), "Should return the same FunctionType");
    }

    @Test
    @DisplayName("should return function type with correct param types")
    void shouldReturnFunctionTypeWithCorrectParams() {
      final FunctionType funcType =
          createFuncType(
              new WasmValueType[] {WasmValueType.I32, WasmValueType.F64},
              new WasmValueType[] {WasmValueType.I64});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      final FunctionType returned = tagType.getFunctionType();
      assertEquals(2, returned.getParamTypes().length, "Should have 2 param types");
      assertEquals(1, returned.getReturnTypes().length, "Should have 1 return type");
    }
  }

  @Nested
  @DisplayName("Equality Tests")
  class EqualityTests {

    @Test
    @DisplayName("equal DefaultTagTypes should be equal")
    void equalDefaultTagTypesShouldBeEqual() {
      final FunctionType funcType =
          createFuncType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final DefaultTagType tag1 = new DefaultTagType(funcType);
      final DefaultTagType tag2 = new DefaultTagType(funcType);
      assertEquals(tag1, tag2, "TagTypes with same FunctionType should be equal");
    }

    @Test
    @DisplayName("different DefaultTagTypes should not be equal")
    void differentDefaultTagTypesShouldNotBeEqual() {
      final FunctionType funcType1 =
          createFuncType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final FunctionType funcType2 =
          createFuncType(new WasmValueType[] {WasmValueType.I64}, new WasmValueType[] {});
      final DefaultTagType tag1 = new DefaultTagType(funcType1);
      final DefaultTagType tag2 = new DefaultTagType(funcType2);
      assertNotEquals(tag1, tag2, "TagTypes with different FunctionTypes should not be equal");
    }

    @Test
    @DisplayName("should not equal null")
    void shouldNotEqualNull() {
      final FunctionType funcType = createFuncType(new WasmValueType[] {}, new WasmValueType[] {});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      assertNotEquals(null, tagType, "DefaultTagType should not equal null");
    }

    @Test
    @DisplayName("should not equal different type")
    void shouldNotEqualDifferentType() {
      final FunctionType funcType = createFuncType(new WasmValueType[] {}, new WasmValueType[] {});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      assertNotEquals("not a tag", tagType, "DefaultTagType should not equal String");
    }

    @Test
    @DisplayName("should be reflexive")
    void shouldBeReflexive() {
      final FunctionType funcType =
          createFuncType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      assertEquals(tagType, tagType, "Equality should be reflexive");
    }
  }

  @Nested
  @DisplayName("HashCode Tests")
  class HashCodeTests {

    @Test
    @DisplayName("equal objects should have same hashCode")
    void equalObjectsShouldHaveSameHashCode() {
      final FunctionType funcType =
          createFuncType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final DefaultTagType tag1 = new DefaultTagType(funcType);
      final DefaultTagType tag2 = new DefaultTagType(funcType);
      assertEquals(tag1.hashCode(), tag2.hashCode(), "Equal objects should have same hashCode");
    }

    @Test
    @DisplayName("hashCode should be consistent")
    void hashCodeShouldBeConsistent() {
      final FunctionType funcType =
          createFuncType(new WasmValueType[] {WasmValueType.F32}, new WasmValueType[] {});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      final int hash1 = tagType.hashCode();
      final int hash2 = tagType.hashCode();
      assertEquals(hash1, hash2, "hashCode should be consistent across calls");
    }
  }

  @Nested
  @DisplayName("ToString Tests")
  class ToStringTests {

    @Test
    @DisplayName("toString should contain TagType prefix")
    void toStringShouldContainPrefix() {
      final FunctionType funcType =
          createFuncType(new WasmValueType[] {WasmValueType.I32}, new WasmValueType[] {});
      final DefaultTagType tagType = new DefaultTagType(funcType);
      final String str = tagType.toString();
      assertNotNull(str, "toString should not be null");
      assertTrue(str.contains("TagType"), "toString should contain TagType");
    }
  }
}
