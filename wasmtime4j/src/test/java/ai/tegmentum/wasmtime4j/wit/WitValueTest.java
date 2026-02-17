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

package ai.tegmentum.wasmtime4j.wit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WitValue} abstract class.
 *
 * <p>WitValue is the base class for all WebAssembly Interface Type (WIT) values.
 */
@DisplayName("WitValue Tests")
class WitValueTest {

  @Nested
  @DisplayName("isCompatibleWith Behavior Tests")
  class IsCompatibleWithBehaviorTests {

    @Test
    @DisplayName("should return false for null target type")
    void shouldReturnFalseForNullTargetType() {
      // Use WitBool as a concrete implementation
      final WitValue value = WitBool.of(true);

      final boolean result = value.isCompatibleWith(null);

      assertFalse(result, "isCompatibleWith should return false for null target type");
    }

    @Test
    @DisplayName("should return true for same type")
    void shouldReturnTrueForSameType() {
      final WitValue value = WitBool.of(true);
      final WitType sameType = WitType.createBool();

      final boolean result = value.isCompatibleWith(sameType);

      assertTrue(
          result, "isCompatibleWith should return true when target type equals value's type");
    }

    @Test
    @DisplayName("should return false for different type")
    void shouldReturnFalseForDifferentType() {
      final WitValue value = WitBool.of(true);
      final WitType differentType = WitType.createString();

      final boolean result = value.isCompatibleWith(differentType);

      assertFalse(
          result,
          "isCompatibleWith should return false when target type differs from value's type");
    }
  }

  @Nested
  @DisplayName("getType Behavior Tests")
  class GetTypeBehaviorTests {

    @Test
    @DisplayName("should return non-null type for concrete implementation")
    void shouldReturnNonNullTypeForConcreteImplementation() {
      final WitValue value = WitBool.of(false);

      final WitType type = value.getType();

      assertNotNull(type, "getType should never return null for valid WitValue");
    }

    @Test
    @DisplayName("should return consistent type for same value")
    void shouldReturnConsistentTypeForSameValue() {
      final WitValue value = WitBool.of(true);

      final WitType type1 = value.getType();
      final WitType type2 = value.getType();

      assertEquals(type1, type2, "getType should return the same type instance on repeated calls");
    }

    @Test
    @DisplayName("should return bool type for WitBool value")
    void shouldReturnBoolTypeForWitBoolValue() {
      final WitValue value = WitBool.of(true);
      final WitType expectedType = WitType.createBool();

      final WitType actualType = value.getType();

      assertEquals(expectedType, actualType, "WitBool should have bool type");
    }
  }

  @Nested
  @DisplayName("toJava Behavior Tests")
  class ToJavaBehaviorTests {

    @Test
    @DisplayName("should return non-null Java value for concrete implementation")
    void shouldReturnNonNullJavaValueForConcreteImplementation() {
      final WitValue value = WitBool.of(true);

      final Object javaValue = value.toJava();

      assertNotNull(javaValue, "toJava should not return null for valid WitValue");
    }

    @Test
    @DisplayName("should return Boolean for WitBool true value")
    void shouldReturnBooleanForWitBoolTrueValue() {
      final WitValue value = WitBool.of(true);

      final Object javaValue = value.toJava();

      assertEquals(Boolean.TRUE, javaValue, "WitBool.of(true).toJava() should return Boolean.TRUE");
    }

    @Test
    @DisplayName("should return Boolean for WitBool false value")
    void shouldReturnBooleanForWitBoolFalseValue() {
      final WitValue value = WitBool.of(false);

      final Object javaValue = value.toJava();

      assertEquals(
          Boolean.FALSE, javaValue, "WitBool.of(false).toJava() should return Boolean.FALSE");
    }
  }
}
