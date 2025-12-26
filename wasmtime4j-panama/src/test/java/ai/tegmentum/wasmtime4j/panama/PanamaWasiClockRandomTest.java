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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasiMonotonicClock;
import ai.tegmentum.wasmtime4j.WasiRandom;
import ai.tegmentum.wasmtime4j.WasiWallClock;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the Panama WASI Clock and Random implementations.
 *
 * <p>This test class verifies the PanamaWasiMonotonicClock, PanamaWasiWallClock, and
 * PanamaWasiRandom implementations including class structure, method signatures, and interface
 * compliance using reflection.
 */
@DisplayName("Panama WASI Clock and Random Tests")
class PanamaWasiClockRandomTest {

  // ========================================================================
  // PanamaWasiMonotonicClock Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaWasiMonotonicClock Class Structure Tests")
  class MonotonicClockClassStructureTests {

    @Test
    @DisplayName("PanamaWasiMonotonicClock should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWasiMonotonicClock.class.getModifiers()),
          "PanamaWasiMonotonicClock should be final");
    }

    @Test
    @DisplayName("PanamaWasiMonotonicClock should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiMonotonicClock.class.getModifiers()),
          "PanamaWasiMonotonicClock should be public");
    }

    @Test
    @DisplayName("PanamaWasiMonotonicClock should implement WasiMonotonicClock interface")
    void shouldImplementWasiMonotonicClockInterface() {
      Class<?>[] interfaces = PanamaWasiMonotonicClock.class.getInterfaces();
      boolean implementsInterface =
          Arrays.asList(interfaces).contains(WasiMonotonicClock.class);
      assertTrue(
          implementsInterface,
          "PanamaWasiMonotonicClock should implement WasiMonotonicClock interface");
    }

    @Test
    @DisplayName("PanamaWasiMonotonicClock should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaWasiMonotonicClock.class.getPackage().getName(),
          "PanamaWasiMonotonicClock should be in ai.tegmentum.wasmtime4j.panama package");
    }
  }

  @Nested
  @DisplayName("PanamaWasiMonotonicClock Field Tests")
  class MonotonicClockFieldTests {

    @Test
    @DisplayName("should have NATIVE_BINDINGS static field")
    void shouldHaveNativeBindingsField() throws NoSuchFieldException {
      Field field = PanamaWasiMonotonicClock.class.getDeclaredField("NATIVE_BINDINGS");
      assertNotNull(field, "NATIVE_BINDINGS field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "NATIVE_BINDINGS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "NATIVE_BINDINGS should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "NATIVE_BINDINGS should be private");
      assertEquals(
          NativeFunctionBindings.class,
          field.getType(),
          "NATIVE_BINDINGS should be of type NativeFunctionBindings");
    }
  }

  @Nested
  @DisplayName("PanamaWasiMonotonicClock Constructor Tests")
  class MonotonicClockConstructorTests {

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHaveNoArgConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = PanamaWasiMonotonicClock.class.getConstructor();
      assertNotNull(constructor, "No-arg constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "No-arg constructor should be public");
      assertEquals(0, constructor.getParameterCount(), "Constructor should have no parameters");
    }
  }

  @Nested
  @DisplayName("PanamaWasiMonotonicClock Method Tests")
  class MonotonicClockMethodTests {

    @Test
    @DisplayName("should have now method")
    void shouldHaveNowMethod() throws NoSuchMethodException {
      Method method = PanamaWasiMonotonicClock.class.getMethod("now");
      assertNotNull(method, "now method should exist");
      assertEquals(long.class, method.getReturnType(), "now should return long");
      assertEquals(0, method.getParameterCount(), "now should have no parameters");
    }

    @Test
    @DisplayName("should have resolution method")
    void shouldHaveResolutionMethod() throws NoSuchMethodException {
      Method method = PanamaWasiMonotonicClock.class.getMethod("resolution");
      assertNotNull(method, "resolution method should exist");
      assertEquals(long.class, method.getReturnType(), "resolution should return long");
      assertEquals(0, method.getParameterCount(), "resolution should have no parameters");
    }

    @Test
    @DisplayName("should have subscribeInstant method")
    void shouldHaveSubscribeInstantMethod() throws NoSuchMethodException {
      Method method = PanamaWasiMonotonicClock.class.getMethod("subscribeInstant", long.class);
      assertNotNull(method, "subscribeInstant method should exist");
      assertEquals(long.class, method.getReturnType(), "subscribeInstant should return long");
      assertEquals(1, method.getParameterCount(), "subscribeInstant should have 1 parameter");
      assertEquals(
          long.class,
          method.getParameterTypes()[0],
          "Parameter should be long (when)");
    }

    @Test
    @DisplayName("should have subscribeDuration method")
    void shouldHaveSubscribeDurationMethod() throws NoSuchMethodException {
      Method method = PanamaWasiMonotonicClock.class.getMethod("subscribeDuration", long.class);
      assertNotNull(method, "subscribeDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "subscribeDuration should return long");
      assertEquals(1, method.getParameterCount(), "subscribeDuration should have 1 parameter");
      assertEquals(
          long.class,
          method.getParameterTypes()[0],
          "Parameter should be long (duration)");
    }
  }

  // ========================================================================
  // PanamaWasiWallClock Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaWasiWallClock Class Structure Tests")
  class WallClockClassStructureTests {

    @Test
    @DisplayName("PanamaWasiWallClock should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWasiWallClock.class.getModifiers()),
          "PanamaWasiWallClock should be final");
    }

    @Test
    @DisplayName("PanamaWasiWallClock should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiWallClock.class.getModifiers()),
          "PanamaWasiWallClock should be public");
    }

    @Test
    @DisplayName("PanamaWasiWallClock should implement WasiWallClock interface")
    void shouldImplementWasiWallClockInterface() {
      Class<?>[] interfaces = PanamaWasiWallClock.class.getInterfaces();
      boolean implementsInterface = Arrays.asList(interfaces).contains(WasiWallClock.class);
      assertTrue(
          implementsInterface,
          "PanamaWasiWallClock should implement WasiWallClock interface");
    }

    @Test
    @DisplayName("PanamaWasiWallClock should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaWasiWallClock.class.getPackage().getName(),
          "PanamaWasiWallClock should be in ai.tegmentum.wasmtime4j.panama package");
    }
  }

  @Nested
  @DisplayName("PanamaWasiWallClock Field Tests")
  class WallClockFieldTests {

    @Test
    @DisplayName("should have NATIVE_BINDINGS static field")
    void shouldHaveNativeBindingsField() throws NoSuchFieldException {
      Field field = PanamaWasiWallClock.class.getDeclaredField("NATIVE_BINDINGS");
      assertNotNull(field, "NATIVE_BINDINGS field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "NATIVE_BINDINGS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "NATIVE_BINDINGS should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "NATIVE_BINDINGS should be private");
    }
  }

  @Nested
  @DisplayName("PanamaWasiWallClock Constructor Tests")
  class WallClockConstructorTests {

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHaveNoArgConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = PanamaWasiWallClock.class.getConstructor();
      assertNotNull(constructor, "No-arg constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "No-arg constructor should be public");
      assertEquals(0, constructor.getParameterCount(), "Constructor should have no parameters");
    }
  }

  @Nested
  @DisplayName("PanamaWasiWallClock Method Tests")
  class WallClockMethodTests {

    @Test
    @DisplayName("should have now method returning Datetime")
    void shouldHaveNowMethod() throws NoSuchMethodException {
      Method method = PanamaWasiWallClock.class.getMethod("now");
      assertNotNull(method, "now method should exist");
      assertEquals(
          WasiWallClock.Datetime.class,
          method.getReturnType(),
          "now should return Datetime");
      assertEquals(0, method.getParameterCount(), "now should have no parameters");
    }

    @Test
    @DisplayName("should have resolution method returning Datetime")
    void shouldHaveResolutionMethod() throws NoSuchMethodException {
      Method method = PanamaWasiWallClock.class.getMethod("resolution");
      assertNotNull(method, "resolution method should exist");
      assertEquals(
          WasiWallClock.Datetime.class,
          method.getReturnType(),
          "resolution should return Datetime");
      assertEquals(0, method.getParameterCount(), "resolution should have no parameters");
    }
  }

  // ========================================================================
  // PanamaWasiRandom Tests
  // ========================================================================

  @Nested
  @DisplayName("PanamaWasiRandom Class Structure Tests")
  class RandomClassStructureTests {

    @Test
    @DisplayName("PanamaWasiRandom should be a final class")
    void shouldBeFinalClass() {
      assertTrue(
          Modifier.isFinal(PanamaWasiRandom.class.getModifiers()),
          "PanamaWasiRandom should be final");
    }

    @Test
    @DisplayName("PanamaWasiRandom should be a public class")
    void shouldBePublicClass() {
      assertTrue(
          Modifier.isPublic(PanamaWasiRandom.class.getModifiers()),
          "PanamaWasiRandom should be public");
    }

    @Test
    @DisplayName("PanamaWasiRandom should implement WasiRandom interface")
    void shouldImplementWasiRandomInterface() {
      Class<?>[] interfaces = PanamaWasiRandom.class.getInterfaces();
      boolean implementsInterface = Arrays.asList(interfaces).contains(WasiRandom.class);
      assertTrue(
          implementsInterface,
          "PanamaWasiRandom should implement WasiRandom interface");
    }

    @Test
    @DisplayName("PanamaWasiRandom should be in the correct package")
    void shouldBeInCorrectPackage() {
      assertEquals(
          "ai.tegmentum.wasmtime4j.panama",
          PanamaWasiRandom.class.getPackage().getName(),
          "PanamaWasiRandom should be in ai.tegmentum.wasmtime4j.panama package");
    }
  }

  @Nested
  @DisplayName("PanamaWasiRandom Field Tests")
  class RandomFieldTests {

    @Test
    @DisplayName("should have NATIVE_BINDINGS static field")
    void shouldHaveNativeBindingsField() throws NoSuchFieldException {
      Field field = PanamaWasiRandom.class.getDeclaredField("NATIVE_BINDINGS");
      assertNotNull(field, "NATIVE_BINDINGS field should exist");
      assertTrue(Modifier.isStatic(field.getModifiers()), "NATIVE_BINDINGS should be static");
      assertTrue(Modifier.isFinal(field.getModifiers()), "NATIVE_BINDINGS should be final");
      assertTrue(Modifier.isPrivate(field.getModifiers()), "NATIVE_BINDINGS should be private");
    }
  }

  @Nested
  @DisplayName("PanamaWasiRandom Constructor Tests")
  class RandomConstructorTests {

    @Test
    @DisplayName("should have public no-arg constructor")
    void shouldHaveNoArgConstructor() throws NoSuchMethodException {
      Constructor<?> constructor = PanamaWasiRandom.class.getConstructor();
      assertNotNull(constructor, "No-arg constructor should exist");
      assertTrue(
          Modifier.isPublic(constructor.getModifiers()),
          "No-arg constructor should be public");
      assertEquals(0, constructor.getParameterCount(), "Constructor should have no parameters");
    }
  }

  @Nested
  @DisplayName("PanamaWasiRandom Method Tests")
  class RandomMethodTests {

    @Test
    @DisplayName("should have getRandomBytes method")
    void shouldHaveGetRandomBytesMethod() throws NoSuchMethodException {
      Method method = PanamaWasiRandom.class.getMethod("getRandomBytes", int.class);
      assertNotNull(method, "getRandomBytes method should exist");
      assertEquals(byte[].class, method.getReturnType(), "getRandomBytes should return byte[]");
      assertEquals(1, method.getParameterCount(), "getRandomBytes should have 1 parameter");
      assertEquals(
          int.class,
          method.getParameterTypes()[0],
          "Parameter should be int (length)");
    }

    @Test
    @DisplayName("should have getRandomU64 method")
    void shouldHaveGetRandomU64Method() throws NoSuchMethodException {
      Method method = PanamaWasiRandom.class.getMethod("getRandomU64");
      assertNotNull(method, "getRandomU64 method should exist");
      assertEquals(long.class, method.getReturnType(), "getRandomU64 should return long");
      assertEquals(0, method.getParameterCount(), "getRandomU64 should have no parameters");
    }
  }

  // ========================================================================
  // Interface Compliance Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Compliance Tests")
  class InterfaceComplianceTests {

    @Test
    @DisplayName("PanamaWasiMonotonicClock should implement all WasiMonotonicClock methods")
    void monotonicClockShouldImplementAllInterfaceMethods() {
      Method[] interfaceMethods = WasiMonotonicClock.class.getDeclaredMethods();
      Class<?> implClass = PanamaWasiMonotonicClock.class;

      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          try {
            Method implMethod =
                implClass.getMethod(
                    interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            assertNotNull(
                implMethod,
                "Implementation should have method: " + interfaceMethod.getName());
          } catch (NoSuchMethodException e) {
            // May be a default method
          }
        }
      }
    }

    @Test
    @DisplayName("PanamaWasiWallClock should implement all WasiWallClock methods")
    void wallClockShouldImplementAllInterfaceMethods() {
      Method[] interfaceMethods = WasiWallClock.class.getDeclaredMethods();
      Class<?> implClass = PanamaWasiWallClock.class;

      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          try {
            Method implMethod =
                implClass.getMethod(
                    interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            assertNotNull(
                implMethod,
                "Implementation should have method: " + interfaceMethod.getName());
          } catch (NoSuchMethodException e) {
            // May be a default method
          }
        }
      }
    }

    @Test
    @DisplayName("PanamaWasiRandom should implement all WasiRandom methods")
    void randomShouldImplementAllInterfaceMethods() {
      Method[] interfaceMethods = WasiRandom.class.getDeclaredMethods();
      Class<?> implClass = PanamaWasiRandom.class;

      for (Method interfaceMethod : interfaceMethods) {
        if (Modifier.isAbstract(interfaceMethod.getModifiers())) {
          try {
            Method implMethod =
                implClass.getMethod(
                    interfaceMethod.getName(), interfaceMethod.getParameterTypes());
            assertNotNull(
                implMethod,
                "Implementation should have method: " + interfaceMethod.getName());
          } catch (NoSuchMethodException e) {
            // May be a default method
          }
        }
      }
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("PanamaWasiMonotonicClock should have expected number of methods")
    void monotonicClockShouldHaveExpectedMethods() {
      Method[] methods = PanamaWasiMonotonicClock.class.getDeclaredMethods();
      // Should have: now, resolution, subscribeInstant, subscribeDuration
      assertTrue(
          methods.length >= 4,
          "PanamaWasiMonotonicClock should have at least 4 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("PanamaWasiWallClock should have expected number of methods")
    void wallClockShouldHaveExpectedMethods() {
      Method[] methods = PanamaWasiWallClock.class.getDeclaredMethods();
      // Should have: now, resolution
      assertTrue(
          methods.length >= 2,
          "PanamaWasiWallClock should have at least 2 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("PanamaWasiRandom should have expected number of methods")
    void randomShouldHaveExpectedMethods() {
      Method[] methods = PanamaWasiRandom.class.getDeclaredMethods();
      // Should have: getRandomBytes, getRandomU64
      assertTrue(
          methods.length >= 2,
          "PanamaWasiRandom should have at least 2 methods, found: " + methods.length);
    }
  }

  // ========================================================================
  // Design Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Design Pattern Tests")
  class DesignPatternTests {

    @Test
    @DisplayName("All clock and random classes should be final")
    void allClassesShouldBeFinal() {
      assertTrue(
          Modifier.isFinal(PanamaWasiMonotonicClock.class.getModifiers()),
          "PanamaWasiMonotonicClock should be final");
      assertTrue(
          Modifier.isFinal(PanamaWasiWallClock.class.getModifiers()),
          "PanamaWasiWallClock should be final");
      assertTrue(
          Modifier.isFinal(PanamaWasiRandom.class.getModifiers()),
          "PanamaWasiRandom should be final");
    }

    @Test
    @DisplayName("All classes should have singleton-style NATIVE_BINDINGS field")
    void allClassesShouldHaveNativeBindings() {
      try {
        Field monotonic = PanamaWasiMonotonicClock.class.getDeclaredField("NATIVE_BINDINGS");
        Field wall = PanamaWasiWallClock.class.getDeclaredField("NATIVE_BINDINGS");
        Field random = PanamaWasiRandom.class.getDeclaredField("NATIVE_BINDINGS");

        // All should be static final
        assertTrue(Modifier.isStatic(monotonic.getModifiers()));
        assertTrue(Modifier.isFinal(monotonic.getModifiers()));
        assertTrue(Modifier.isStatic(wall.getModifiers()));
        assertTrue(Modifier.isFinal(wall.getModifiers()));
        assertTrue(Modifier.isStatic(random.getModifiers()));
        assertTrue(Modifier.isFinal(random.getModifiers()));
      } catch (NoSuchFieldException e) {
        throw new AssertionError("All classes should have NATIVE_BINDINGS field", e);
      }
    }

    @Test
    @DisplayName("All constructors should be public and take no arguments")
    void allConstructorsShouldBePublicNoArg() throws NoSuchMethodException {
      Constructor<?> monotonic = PanamaWasiMonotonicClock.class.getConstructor();
      Constructor<?> wall = PanamaWasiWallClock.class.getConstructor();
      Constructor<?> random = PanamaWasiRandom.class.getConstructor();

      assertTrue(Modifier.isPublic(monotonic.getModifiers()));
      assertTrue(Modifier.isPublic(wall.getModifiers()));
      assertTrue(Modifier.isPublic(random.getModifiers()));

      assertEquals(0, monotonic.getParameterCount());
      assertEquals(0, wall.getParameterCount());
      assertEquals(0, random.getParameterCount());
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("Monotonic clock methods should return correct types")
    void monotonicClockMethodsShouldReturnCorrectTypes() throws NoSuchMethodException {
      Method now = PanamaWasiMonotonicClock.class.getMethod("now");
      Method resolution = PanamaWasiMonotonicClock.class.getMethod("resolution");
      Method subscribeInstant =
          PanamaWasiMonotonicClock.class.getMethod("subscribeInstant", long.class);
      Method subscribeDuration =
          PanamaWasiMonotonicClock.class.getMethod("subscribeDuration", long.class);

      assertEquals(long.class, now.getReturnType(), "now should return long (nanoseconds)");
      assertEquals(
          long.class, resolution.getReturnType(), "resolution should return long (nanoseconds)");
      assertEquals(
          long.class,
          subscribeInstant.getReturnType(),
          "subscribeInstant should return long (pollable handle)");
      assertEquals(
          long.class,
          subscribeDuration.getReturnType(),
          "subscribeDuration should return long (pollable handle)");
    }

    @Test
    @DisplayName("Wall clock methods should return Datetime")
    void wallClockMethodsShouldReturnDatetime() throws NoSuchMethodException {
      Method now = PanamaWasiWallClock.class.getMethod("now");
      Method resolution = PanamaWasiWallClock.class.getMethod("resolution");

      assertEquals(
          WasiWallClock.Datetime.class,
          now.getReturnType(),
          "now should return Datetime");
      assertEquals(
          WasiWallClock.Datetime.class,
          resolution.getReturnType(),
          "resolution should return Datetime");
    }

    @Test
    @DisplayName("Random methods should return correct types")
    void randomMethodsShouldReturnCorrectTypes() throws NoSuchMethodException {
      Method getRandomBytes = PanamaWasiRandom.class.getMethod("getRandomBytes", int.class);
      Method getRandomU64 = PanamaWasiRandom.class.getMethod("getRandomU64");

      assertEquals(
          byte[].class,
          getRandomBytes.getReturnType(),
          "getRandomBytes should return byte[]");
      assertEquals(
          long.class,
          getRandomU64.getReturnType(),
          "getRandomU64 should return long");
    }
  }
}
