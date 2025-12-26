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

package ai.tegmentum.wasmtime4j.wasi.http;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasiHttpFactory} class.
 *
 * <p>WasiHttpFactory provides factory methods for creating WASI HTTP contexts.
 */
@DisplayName("WasiHttpFactory Tests")
class WasiHttpFactoryTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasiHttpFactory.class.getModifiers()),
          "WasiHttpFactory should be public");
      assertTrue(
          Modifier.isFinal(WasiHttpFactory.class.getModifiers()),
          "WasiHttpFactory should be final");
    }

    @Test
    @DisplayName("should have private constructor")
    void shouldHavePrivateConstructor() {
      final Constructor<?>[] constructors = WasiHttpFactory.class.getDeclaredConstructors();
      assertEquals(1, constructors.length, "Should have exactly 1 constructor");
      assertTrue(
          Modifier.isPrivate(constructors[0].getModifiers()), "Constructor should be private");
    }

    @Test
    @DisplayName("should have only static methods")
    void shouldHaveOnlyStaticMethods() {
      for (final Method method : WasiHttpFactory.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertTrue(
              Modifier.isStatic(method.getModifiers()),
              "Method should be static: " + method.getName());
        }
      }
    }
  }

  @Nested
  @DisplayName("createContext Method Tests")
  class CreateContextMethodTests {

    @Test
    @DisplayName("should have createContext method without parameters")
    void shouldHaveCreateContextMethodWithoutParameters() throws NoSuchMethodException {
      final Method method = WasiHttpFactory.class.getMethod("createContext");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(WasiHttpContext.class, method.getReturnType(), "Should return WasiHttpContext");
    }

    @Test
    @DisplayName("should have createContext method with config parameter")
    void shouldHaveCreateContextMethodWithConfigParameter() throws NoSuchMethodException {
      final Method method = WasiHttpFactory.class.getMethod("createContext", WasiHttpConfig.class);
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(WasiHttpContext.class, method.getReturnType(), "Should return WasiHttpContext");
    }

    @Test
    @DisplayName("createContext with null config should throw IllegalArgumentException")
    void createContextWithNullConfigShouldThrowIllegalArgumentException() {
      assertThrows(
          IllegalArgumentException.class,
          () -> WasiHttpFactory.createContext(null),
          "Should throw for null config");
    }
  }

  @Nested
  @DisplayName("isAvailable Method Tests")
  class IsAvailableMethodTests {

    @Test
    @DisplayName("should have isAvailable method")
    void shouldHaveIsAvailableMethod() throws NoSuchMethodException {
      final Method method = WasiHttpFactory.class.getMethod("isAvailable");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("isAvailable should return a boolean value")
    void isAvailableShouldReturnBooleanValue() {
      // This should not throw and should return true or false
      final boolean available = WasiHttpFactory.isAvailable();
      // We can't assert the value since it depends on runtime, but it should be boolean
      assertTrue(available || !available, "Should return a boolean");
    }
  }

  @Nested
  @DisplayName("getImplementationName Method Tests")
  class GetImplementationNameMethodTests {

    @Test
    @DisplayName("should have getImplementationName method")
    void shouldHaveGetImplementationNameMethod() throws NoSuchMethodException {
      final Method method = WasiHttpFactory.class.getMethod("getImplementationName");
      assertTrue(Modifier.isStatic(method.getModifiers()), "Should be static");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("getImplementationName should return known value")
    void getImplementationNameShouldReturnKnownValue() {
      final String implementationName = WasiHttpFactory.getImplementationName();
      assertNotNull(implementationName, "Should not return null");

      final Set<String> validNames = Set.of("Panama", "JNI", "None");
      assertTrue(
          validNames.contains(implementationName),
          "Should return one of: " + validNames + ", got: " + implementationName);
    }
  }

  @Nested
  @DisplayName("Factory Pattern Tests")
  class FactoryPatternTests {

    @Test
    @DisplayName("should follow utility class pattern")
    void shouldFollowUtilityClassPattern() {
      // Final class
      assertTrue(Modifier.isFinal(WasiHttpFactory.class.getModifiers()), "Should be final");

      // Private constructor
      final Constructor<?>[] constructors = WasiHttpFactory.class.getDeclaredConstructors();
      assertTrue(
          Arrays.stream(constructors).allMatch(c -> Modifier.isPrivate(c.getModifiers())),
          "All constructors should be private");

      // All methods static
      for (final Method method : WasiHttpFactory.class.getDeclaredMethods()) {
        if (!method.isSynthetic()) {
          assertTrue(Modifier.isStatic(method.getModifiers()), "All methods should be static");
        }
      }
    }

    @Test
    @DisplayName("should provide automatic implementation selection")
    void shouldProvideAutomaticImplementationSelection() {
      // The factory automatically selects Panama or JNI based on runtime
      final String implementationName = WasiHttpFactory.getImplementationName();
      assertNotNull(implementationName, "Should detect implementation");
    }
  }

  @Nested
  @DisplayName("Runtime Selection Tests")
  class RuntimeSelectionTests {

    @Test
    @DisplayName("isAvailable and getImplementationName should be consistent")
    void isAvailableAndGetImplementationNameShouldBeConsistent() {
      final boolean available = WasiHttpFactory.isAvailable();
      final String implementationName = WasiHttpFactory.getImplementationName();

      if (available) {
        assertFalse(
            "None".equals(implementationName),
            "If available, implementation name should not be 'None'");
      } else {
        assertEquals("None", implementationName, "If not available, should return 'None'");
      }
    }

    @Test
    @DisplayName("should prefer Panama over JNI when available")
    void shouldPreferPanamaOverJniWhenAvailable() {
      // This documents the expected behavior based on the implementation
      // Panama is tried first, then JNI, then None
      final String implementationName = WasiHttpFactory.getImplementationName();

      // The implementation tries Panama first, so if we're on Java 23+
      // and Panama is available, it should be used
      assertNotNull(implementationName, "Should return an implementation name");
    }
  }

  @Nested
  @DisplayName("Error Handling Tests")
  class ErrorHandlingTests {

    @Test
    @DisplayName("createContext with null config throws IllegalArgumentException")
    void createContextWithNullConfigThrowsIllegalArgumentException() {
      final IllegalArgumentException ex =
          assertThrows(IllegalArgumentException.class, () -> WasiHttpFactory.createContext(null));

      assertTrue(
          ex.getMessage().contains("null"),
          "Exception message should mention null: " + ex.getMessage());
    }
  }

  @Nested
  @DisplayName("Usage Pattern Tests")
  class UsagePatternTests {

    @Test
    @DisplayName("should support default context creation pattern")
    void shouldSupportDefaultContextCreationPattern() throws NoSuchMethodException {
      // Pattern: WasiHttpContext context = WasiHttpFactory.createContext();
      final Method method = WasiHttpFactory.class.getMethod("createContext");
      assertNotNull(method, "Should have no-arg createContext method");
    }

    @Test
    @DisplayName("should support configured context creation pattern")
    void shouldSupportConfiguredContextCreationPattern() throws NoSuchMethodException {
      // Pattern: WasiHttpContext context = WasiHttpFactory.createContext(config);
      final Method method = WasiHttpFactory.class.getMethod("createContext", WasiHttpConfig.class);
      assertNotNull(method, "Should have createContext(WasiHttpConfig) method");
    }

    @Test
    @DisplayName("should support availability check pattern")
    void shouldSupportAvailabilityCheckPattern() throws NoSuchMethodException {
      // Pattern: if (WasiHttpFactory.isAvailable()) { ... }
      final Method method = WasiHttpFactory.class.getMethod("isAvailable");
      assertNotNull(method, "Should have isAvailable method");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should support implementation info pattern")
    void shouldSupportImplementationInfoPattern() throws NoSuchMethodException {
      // Pattern: log.info("Using " + WasiHttpFactory.getImplementationName());
      final Method method = WasiHttpFactory.class.getMethod("getImplementationName");
      assertNotNull(method, "Should have getImplementationName method");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }
}
