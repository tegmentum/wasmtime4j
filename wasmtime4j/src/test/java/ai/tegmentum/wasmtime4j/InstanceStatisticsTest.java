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

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the InstanceStatistics class.
 *
 * <p>InstanceStatistics provides runtime statistics for a WebAssembly instance including function
 * call counts, execution time, memory usage, and other metrics.
 */
@DisplayName("InstanceStatistics Class Tests")
class InstanceStatisticsTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be a class")
    void shouldBeAClass() {
      assertFalse(InstanceStatistics.class.isInterface(), "InstanceStatistics should be a class");
      assertFalse(InstanceStatistics.class.isEnum(), "InstanceStatistics should not be an enum");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(InstanceStatistics.class.getModifiers()),
          "InstanceStatistics should be public");
    }

    @Test
    @DisplayName("should be final")
    void shouldBeFinal() {
      assertTrue(
          Modifier.isFinal(InstanceStatistics.class.getModifiers()),
          "InstanceStatistics should be final");
    }
  }

  // ========================================================================
  // Constructor Tests
  // ========================================================================

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have public constructor with 8 parameters")
    void shouldHavePublicConstructorWith8Parameters() throws NoSuchMethodException {
      Constructor<?> constructor =
          InstanceStatistics.class.getConstructor(
              long.class,
              long.class,
              long.class,
              long.class,
              int.class,
              int.class,
              long.class,
              long.class);
      assertNotNull(constructor, "Constructor with 8 parameters should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }

    @Test
    @DisplayName("constructor should have correct parameter types")
    void constructorShouldHaveCorrectParameterTypes() throws NoSuchMethodException {
      Constructor<?> constructor =
          InstanceStatistics.class.getConstructor(
              long.class,
              long.class,
              long.class,
              long.class,
              int.class,
              int.class,
              long.class,
              long.class);
      Class<?>[] paramTypes = constructor.getParameterTypes();
      assertEquals(8, paramTypes.length, "Constructor should have 8 parameters");
      assertEquals(long.class, paramTypes[0], "1st parameter should be long (functionCallCount)");
      assertEquals(long.class, paramTypes[1], "2nd parameter should be long (totalExecutionTime)");
      assertEquals(
          long.class, paramTypes[2], "3rd parameter should be long (memoryBytesAllocated)");
      assertEquals(long.class, paramTypes[3], "4th parameter should be long (peakMemoryUsage)");
      assertEquals(int.class, paramTypes[4], "5th parameter should be int (activeTableElements)");
      assertEquals(int.class, paramTypes[5], "6th parameter should be int (activeGlobals)");
      assertEquals(long.class, paramTypes[6], "7th parameter should be long (fuelConsumed)");
      assertEquals(long.class, paramTypes[7], "8th parameter should be long (epochTicks)");
    }

    @Test
    @DisplayName("should have only one public constructor")
    void shouldHaveOnlyOnePublicConstructor() {
      long publicConstructors =
          Arrays.stream(InstanceStatistics.class.getConstructors())
              .filter(c -> Modifier.isPublic(c.getModifiers()))
              .count();
      assertEquals(
          1, publicConstructors, "InstanceStatistics should have exactly 1 public constructor");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have private final functionCallCount field")
    void shouldHavePrivateFinalFunctionCallCountField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("functionCallCount");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "functionCallCount field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "functionCallCount field should be final");
      assertEquals(long.class, field.getType(), "functionCallCount field should be long");
    }

    @Test
    @DisplayName("should have private final totalExecutionTime field")
    void shouldHavePrivateFinalTotalExecutionTimeField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("totalExecutionTime");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "totalExecutionTime field should be private");
      assertTrue(
          Modifier.isFinal(field.getModifiers()), "totalExecutionTime field should be final");
      assertEquals(long.class, field.getType(), "totalExecutionTime field should be long");
    }

    @Test
    @DisplayName("should have private final memoryBytesAllocated field")
    void shouldHavePrivateFinalMemoryBytesAllocatedField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("memoryBytesAllocated");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "memoryBytesAllocated field should be private");
      assertTrue(
          Modifier.isFinal(field.getModifiers()), "memoryBytesAllocated field should be final");
      assertEquals(long.class, field.getType(), "memoryBytesAllocated field should be long");
    }

    @Test
    @DisplayName("should have private final peakMemoryUsage field")
    void shouldHavePrivateFinalPeakMemoryUsageField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("peakMemoryUsage");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "peakMemoryUsage field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "peakMemoryUsage field should be final");
      assertEquals(long.class, field.getType(), "peakMemoryUsage field should be long");
    }

    @Test
    @DisplayName("should have private final activeTableElements field")
    void shouldHavePrivateFinalActiveTableElementsField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("activeTableElements");
      assertTrue(
          Modifier.isPrivate(field.getModifiers()), "activeTableElements field should be private");
      assertTrue(
          Modifier.isFinal(field.getModifiers()), "activeTableElements field should be final");
      assertEquals(int.class, field.getType(), "activeTableElements field should be int");
    }

    @Test
    @DisplayName("should have private final activeGlobals field")
    void shouldHavePrivateFinalActiveGlobalsField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("activeGlobals");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "activeGlobals field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "activeGlobals field should be final");
      assertEquals(int.class, field.getType(), "activeGlobals field should be int");
    }

    @Test
    @DisplayName("should have private final fuelConsumed field")
    void shouldHavePrivateFinalFuelConsumedField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("fuelConsumed");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "fuelConsumed field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "fuelConsumed field should be final");
      assertEquals(long.class, field.getType(), "fuelConsumed field should be long");
    }

    @Test
    @DisplayName("should have private final epochTicks field")
    void shouldHavePrivateFinalEpochTicksField() throws NoSuchFieldException {
      Field field = InstanceStatistics.class.getDeclaredField("epochTicks");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "epochTicks field should be private");
      assertTrue(Modifier.isFinal(field.getModifiers()), "epochTicks field should be final");
      assertEquals(long.class, field.getType(), "epochTicks field should be long");
    }

    @Test
    @DisplayName("should have exactly 8 fields")
    void shouldHaveExactly8Fields() {
      Field[] fields = InstanceStatistics.class.getDeclaredFields();
      assertEquals(8, fields.length, "InstanceStatistics should have exactly 8 fields");
    }
  }

  // ========================================================================
  // Accessor Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Accessor Method Tests")
  class AccessorMethodTests {

    @Test
    @DisplayName("should have getFunctionCallCount method")
    void shouldHaveGetFunctionCallCountMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getFunctionCallCount");
      assertNotNull(method, "getFunctionCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "getFunctionCallCount should return long");
    }

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "getTotalExecutionTime should return long");
    }

    @Test
    @DisplayName("should have getMemoryBytesAllocated method")
    void shouldHaveGetMemoryBytesAllocatedMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getMemoryBytesAllocated");
      assertNotNull(method, "getMemoryBytesAllocated method should exist");
      assertEquals(
          long.class, method.getReturnType(), "getMemoryBytesAllocated should return long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "getPeakMemoryUsage should return long");
    }

    @Test
    @DisplayName("should have getActiveTableElements method")
    void shouldHaveGetActiveTableElementsMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getActiveTableElements");
      assertNotNull(method, "getActiveTableElements method should exist");
      assertEquals(int.class, method.getReturnType(), "getActiveTableElements should return int");
    }

    @Test
    @DisplayName("should have getActiveGlobals method")
    void shouldHaveGetActiveGlobalsMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getActiveGlobals");
      assertNotNull(method, "getActiveGlobals method should exist");
      assertEquals(int.class, method.getReturnType(), "getActiveGlobals should return int");
    }

    @Test
    @DisplayName("should have getFuelConsumed method")
    void shouldHaveGetFuelConsumedMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getFuelConsumed");
      assertNotNull(method, "getFuelConsumed method should exist");
      assertEquals(long.class, method.getReturnType(), "getFuelConsumed should return long");
    }

    @Test
    @DisplayName("should have getEpochTicks method")
    void shouldHaveGetEpochTicksMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getEpochTicks");
      assertNotNull(method, "getEpochTicks method should exist");
      assertEquals(long.class, method.getReturnType(), "getEpochTicks should return long");
    }
  }

  // ========================================================================
  // Computed Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Computed Method Tests")
  class ComputedMethodTests {

    @Test
    @DisplayName("should have getAverageExecutionTimePerCall method")
    void shouldHaveGetAverageExecutionTimePerCallMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getAverageExecutionTimePerCall");
      assertNotNull(method, "getAverageExecutionTimePerCall method should exist");
      assertEquals(
          double.class,
          method.getReturnType(),
          "getAverageExecutionTimePerCall should return double");
    }

    @Test
    @DisplayName("should have getFuelConsumptionRate method")
    void shouldHaveGetFuelConsumptionRateMethod() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("getFuelConsumptionRate");
      assertNotNull(method, "getFuelConsumptionRate method should exist");
      assertEquals(
          double.class, method.getReturnType(), "getFuelConsumptionRate should return double");
    }
  }

  // ========================================================================
  // Object Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Methods Tests")
  class ObjectMethodsTests {

    @Test
    @DisplayName("should override toString")
    void shouldOverrideToString() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("toString");
      assertNotNull(method, "toString method should exist");
      assertEquals(
          InstanceStatistics.class,
          method.getDeclaringClass(),
          "toString should be declared in InstanceStatistics");
    }

    @Test
    @DisplayName("should override equals")
    void shouldOverrideEquals() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("equals", Object.class);
      assertNotNull(method, "equals method should exist");
      assertEquals(
          InstanceStatistics.class,
          method.getDeclaringClass(),
          "equals should be declared in InstanceStatistics");
    }

    @Test
    @DisplayName("should override hashCode")
    void shouldOverrideHashCode() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("hashCode");
      assertNotNull(method, "hashCode method should exist");
      assertEquals(
          InstanceStatistics.class,
          method.getDeclaringClass(),
          "hashCode should be declared in InstanceStatistics");
    }

    @Test
    @DisplayName("toString should return String")
    void toStringShouldReturnString() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("toString");
      assertEquals(String.class, method.getReturnType(), "toString should return String");
    }

    @Test
    @DisplayName("equals should return boolean")
    void equalsShouldReturnBoolean() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("equals", Object.class);
      assertEquals(boolean.class, method.getReturnType(), "equals should return boolean");
    }

    @Test
    @DisplayName("hashCode should return int")
    void hashCodeShouldReturnInt() throws NoSuchMethodException {
      final Method method = InstanceStatistics.class.getMethod("hashCode");
      assertEquals(int.class, method.getReturnType(), "hashCode should return int");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have all expected methods")
    void shouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getFunctionCallCount",
              "getTotalExecutionTime",
              "getMemoryBytesAllocated",
              "getPeakMemoryUsage",
              "getActiveTableElements",
              "getActiveGlobals",
              "getFuelConsumed",
              "getEpochTicks",
              "getAverageExecutionTimePerCall",
              "getFuelConsumptionRate",
              "toString",
              "equals",
              "hashCode");

      Set<String> actualMethods =
          Arrays.stream(InstanceStatistics.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "InstanceStatistics should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 13 public methods")
    void shouldHaveExactly13PublicMethods() {
      long publicMethods =
          Arrays.stream(InstanceStatistics.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .count();
      assertEquals(13, publicMethods, "InstanceStatistics should have exactly 13 public methods");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Object directly")
    void shouldExtendObjectDirectly() {
      assertEquals(
          Object.class,
          InstanceStatistics.class.getSuperclass(),
          "InstanceStatistics should extend Object");
    }

    @Test
    @DisplayName("should not implement any interfaces")
    void shouldNotImplementAnyInterfaces() {
      assertEquals(
          0,
          InstanceStatistics.class.getInterfaces().length,
          "InstanceStatistics should not implement any interfaces");
    }
  }

  // ========================================================================
  // Static Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Methods Tests")
  class StaticMethodsTests {

    @Test
    @DisplayName("should have no public static methods")
    void shouldHaveNoPublicStaticMethods() {
      long staticMethods =
          Arrays.stream(InstanceStatistics.class.getDeclaredMethods())
              .filter(m -> Modifier.isPublic(m.getModifiers()))
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "InstanceStatistics should have no public static methods");
    }
  }

  // ========================================================================
  // Nested Classes Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Classes Tests")
  class NestedClassesTests {

    @Test
    @DisplayName("should have no nested classes")
    void shouldHaveNoNestedClasses() {
      assertEquals(
          0,
          InstanceStatistics.class.getDeclaredClasses().length,
          "InstanceStatistics should have no nested classes");
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("long getters should return primitive long")
    void longGettersShouldReturnPrimitiveLong() throws NoSuchMethodException {
      String[] methodNames = {
        "getFunctionCallCount",
        "getTotalExecutionTime",
        "getMemoryBytesAllocated",
        "getPeakMemoryUsage",
        "getFuelConsumed",
        "getEpochTicks"
      };
      for (String name : methodNames) {
        Method method = InstanceStatistics.class.getMethod(name);
        assertEquals(long.class, method.getReturnType(), name + " should return primitive long");
      }
    }

    @Test
    @DisplayName("int getters should return primitive int")
    void intGettersShouldReturnPrimitiveInt() throws NoSuchMethodException {
      String[] methodNames = {"getActiveTableElements", "getActiveGlobals"};
      for (String name : methodNames) {
        Method method = InstanceStatistics.class.getMethod(name);
        assertEquals(int.class, method.getReturnType(), name + " should return primitive int");
      }
    }

    @Test
    @DisplayName("computed methods should return primitive double")
    void computedMethodsShouldReturnPrimitiveDouble() throws NoSuchMethodException {
      String[] methodNames = {"getAverageExecutionTimePerCall", "getFuelConsumptionRate"};
      for (String name : methodNames) {
        Method method = InstanceStatistics.class.getMethod(name);
        assertEquals(
            double.class, method.getReturnType(), name + " should return primitive double");
      }
    }
  }
}
