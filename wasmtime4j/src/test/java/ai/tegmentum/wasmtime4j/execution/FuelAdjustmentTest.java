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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the FuelAdjustment class.
 *
 * <p>This test class verifies the class structure, constructor, fields, and methods for
 * FuelAdjustment using reflection-based testing.
 */
@DisplayName("FuelAdjustment Tests")
class FuelAdjustmentTest {

  // ========================================================================
  // Class Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("FuelAdjustment should be a class")
    void shouldBeAClass() {
      assertFalse(FuelAdjustment.class.isInterface(), "FuelAdjustment should not be an interface");
      assertFalse(FuelAdjustment.class.isEnum(), "FuelAdjustment should not be an enum");
    }

    @Test
    @DisplayName("FuelAdjustment should be a final class")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(FuelAdjustment.class.getModifiers()), "FuelAdjustment should be final");
    }

    @Test
    @DisplayName("FuelAdjustment should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(FuelAdjustment.class.getModifiers()),
          "FuelAdjustment should be public");
    }

    @Test
    @DisplayName("FuelAdjustment should not extend other classes")
    void shouldNotExtendOtherClasses() {
      assertEquals(
          Object.class,
          FuelAdjustment.class.getSuperclass(),
          "FuelAdjustment should extend Object");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("FuelAdjustment should have exactly one constructor")
    void shouldHaveExactlyOneConstructor() {
      Constructor<?>[] constructors = FuelAdjustment.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly one constructor");
    }

    @Test
    @DisplayName("Constructor should accept long and String parameters")
    void constructorShouldAcceptLongAndStringParameters() {
      Constructor<?>[] constructors = FuelAdjustment.class.getDeclaredConstructors();
      Constructor<?> constructor = constructors[0];
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(2, paramTypes.length, "Constructor should have 2 parameters");
      assertEquals(long.class, paramTypes[0], "First parameter should be long");
      assertEquals(String.class, paramTypes[1], "Second parameter should be String");
    }

    @Test
    @DisplayName("Constructor should be public")
    void constructorShouldBePublic() {
      Constructor<?>[] constructors = FuelAdjustment.class.getDeclaredConstructors();
      Constructor<?> constructor = constructors[0];
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("FuelAdjustment should have an amount field")
    void shouldHaveAmountField() throws NoSuchFieldException {
      Field field = FuelAdjustment.class.getDeclaredField("amount");
      assertNotNull(field, "amount field should exist");
    }

    @Test
    @DisplayName("amount field should be of type long")
    void amountFieldShouldBeLong() throws NoSuchFieldException {
      Field field = FuelAdjustment.class.getDeclaredField("amount");
      assertEquals(long.class, field.getType(), "amount should be of type long");
    }

    @Test
    @DisplayName("amount field should be private and final")
    void amountFieldShouldBePrivateFinal() throws NoSuchFieldException {
      Field field = FuelAdjustment.class.getDeclaredField("amount");
      int modifiers = field.getModifiers();
      assertTrue(Modifier.isPrivate(modifiers), "amount should be private");
      assertTrue(Modifier.isFinal(modifiers), "amount should be final");
    }

    @Test
    @DisplayName("FuelAdjustment should have a reason field")
    void shouldHaveReasonField() throws NoSuchFieldException {
      Field field = FuelAdjustment.class.getDeclaredField("reason");
      assertNotNull(field, "reason field should exist");
    }

    @Test
    @DisplayName("reason field should be of type String")
    void reasonFieldShouldBeString() throws NoSuchFieldException {
      Field field = FuelAdjustment.class.getDeclaredField("reason");
      assertEquals(String.class, field.getType(), "reason should be of type String");
    }

    @Test
    @DisplayName("reason field should be private and final")
    void reasonFieldShouldBePrivateFinal() throws NoSuchFieldException {
      Field field = FuelAdjustment.class.getDeclaredField("reason");
      int modifiers = field.getModifiers();
      assertTrue(Modifier.isPrivate(modifiers), "reason should be private");
      assertTrue(Modifier.isFinal(modifiers), "reason should be final");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getAmount method")
    void shouldHaveGetAmountMethod() throws NoSuchMethodException {
      Method method = FuelAdjustment.class.getMethod("getAmount");
      assertNotNull(method, "getAmount method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getAmount should be public");
    }

    @Test
    @DisplayName("should have getReason method")
    void shouldHaveGetReasonMethod() throws NoSuchMethodException {
      Method method = FuelAdjustment.class.getMethod("getReason");
      assertNotNull(method, "getReason method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
      assertTrue(Modifier.isPublic(method.getModifiers()), "getReason should be public");
    }

    @Test
    @DisplayName("FuelAdjustment should have exactly 2 declared methods")
    void shouldHaveExactlyTwoDeclaredMethods() {
      Method[] methods = FuelAdjustment.class.getDeclaredMethods();
      long nonSyntheticCount = Arrays.stream(methods).filter(m -> !m.isSynthetic()).count();
      assertEquals(2, nonSyntheticCount, "Should have exactly 2 declared methods");
    }
  }

  // ========================================================================
  // Instance Tests
  // ========================================================================

  @Nested
  @DisplayName("Instance Tests")
  class InstanceTests {

    @Test
    @DisplayName("FuelAdjustment constructor should work correctly")
    void constructorShouldWorkCorrectly() {
      FuelAdjustment adjustment = new FuelAdjustment(100L, "test reason");
      assertNotNull(adjustment, "Should be able to create FuelAdjustment");
    }

    @Test
    @DisplayName("getAmount should return the amount passed to constructor")
    void getAmountShouldReturnConstructorValue() {
      FuelAdjustment adjustment = new FuelAdjustment(500L, "bonus fuel");
      assertEquals(500L, adjustment.getAmount(), "getAmount should return constructor value");
    }

    @Test
    @DisplayName("getReason should return the reason passed to constructor")
    void getReasonShouldReturnConstructorValue() {
      FuelAdjustment adjustment = new FuelAdjustment(100L, "performance boost");
      assertEquals(
          "performance boost", adjustment.getReason(), "getReason should return constructor value");
    }

    @Test
    @DisplayName("should handle zero amount")
    void shouldHandleZeroAmount() {
      FuelAdjustment adjustment = new FuelAdjustment(0L, "no change");
      assertEquals(0L, adjustment.getAmount(), "Should handle zero amount");
    }

    @Test
    @DisplayName("should handle negative amount")
    void shouldHandleNegativeAmount() {
      FuelAdjustment adjustment = new FuelAdjustment(-50L, "fuel reduction");
      assertEquals(-50L, adjustment.getAmount(), "Should handle negative amount");
    }

    @Test
    @DisplayName("should handle large positive amount")
    void shouldHandleLargePositiveAmount() {
      FuelAdjustment adjustment = new FuelAdjustment(Long.MAX_VALUE, "max fuel");
      assertEquals(Long.MAX_VALUE, adjustment.getAmount(), "Should handle Long.MAX_VALUE");
    }

    @Test
    @DisplayName("should handle large negative amount")
    void shouldHandleLargeNegativeAmount() {
      FuelAdjustment adjustment = new FuelAdjustment(Long.MIN_VALUE, "min fuel");
      assertEquals(Long.MIN_VALUE, adjustment.getAmount(), "Should handle Long.MIN_VALUE");
    }
  }
}
