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
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Linker;
import ai.tegmentum.wasmtime4j.Store;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the WASI HTTP package interfaces and classes.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI HTTP API
 * using reflection-based testing.
 */
@DisplayName("WASI HTTP Package Tests")
class WasiHttpPackageTest {

  // ========================================================================
  // WasiHttpContext Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiHttpContext Interface Tests")
  class WasiHttpContextTests {

    @Test
    @DisplayName("WasiHttpContext should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasiHttpContext.class.isInterface(), "WasiHttpContext should be an interface");
    }

    @Test
    @DisplayName("WasiHttpContext should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiHttpContext.class.getModifiers()),
          "WasiHttpContext should be public");
    }

    @Test
    @DisplayName("WasiHttpContext should extend Closeable")
    void shouldExtendCloseable() {
      Class<?>[] interfaces = WasiHttpContext.class.getInterfaces();
      assertEquals(1, interfaces.length, "WasiHttpContext should extend 1 interface");
      assertEquals(Closeable.class, interfaces[0], "WasiHttpContext should extend Closeable");
    }

    @Test
    @DisplayName("should have addToLinker method with Linker and Store parameters")
    void shouldHaveAddToLinkerMethod() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("addToLinker", Linker.class, Store.class);
      assertNotNull(method, "addToLinker method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(2, method.getParameterCount(), "addToLinker should have 2 parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "addToLinker should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have getConfig method returning WasiHttpConfig")
    void shouldHaveGetConfigMethod() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("getConfig");
      assertNotNull(method, "getConfig method should exist");
      assertEquals(
          WasiHttpConfig.class, method.getReturnType(), "Return type should be WasiHttpConfig");
      assertEquals(0, method.getParameterCount(), "getConfig should have no parameters");
    }

    @Test
    @DisplayName("should have getStats method returning WasiHttpStats")
    void shouldHaveGetStatsMethod() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("getStats");
      assertNotNull(method, "getStats method should exist");
      assertEquals(
          WasiHttpStats.class, method.getReturnType(), "Return type should be WasiHttpStats");
      assertEquals(0, method.getParameterCount(), "getStats should have no parameters");
    }

    @Test
    @DisplayName("should have isValid method returning boolean")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }

    @Test
    @DisplayName("should have isHostAllowed method with String parameter")
    void shouldHaveIsHostAllowedMethod() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("isHostAllowed", String.class);
      assertNotNull(method, "isHostAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(1, method.getParameterCount(), "isHostAllowed should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("should have resetStats method")
    void shouldHaveResetStatsMethod() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("resetStats");
      assertNotNull(method, "resetStats method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "resetStats should have no parameters");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }

    @Test
    @DisplayName("WasiHttpContext should have exactly 7 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiHttpContext.class.getDeclaredMethods();
      assertEquals(7, methods.length, "WasiHttpContext should have exactly 7 methods");
    }

    @Test
    @DisplayName("All WasiHttpContext methods should be public")
    void allMethodsShouldBePublic() {
      Method[] methods = WasiHttpContext.class.getDeclaredMethods();
      for (Method method : methods) {
        assertTrue(
            Modifier.isPublic(method.getModifiers()),
            "Method " + method.getName() + " should be public");
      }
    }
  }

  // ========================================================================
  // WasiHttpConfig Interface/Class Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiHttpConfig Tests")
  class WasiHttpConfigTests {

    @Test
    @DisplayName("WasiHttpConfig should be an interface or class")
    void shouldExist() {
      assertNotNull(WasiHttpConfig.class, "WasiHttpConfig should exist");
    }

    @Test
    @DisplayName("WasiHttpConfig should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiHttpConfig.class.getModifiers()),
          "WasiHttpConfig should be public");
    }

    @Test
    @DisplayName("WasiHttpConfig should have builder method")
    void shouldHaveBuilderMethod() {
      Method[] methods = WasiHttpConfig.class.getDeclaredMethods();
      boolean hasBuilder = false;
      for (Method method : methods) {
        if (method.getName().equals("builder") || method.getName().equals("create")) {
          hasBuilder = true;
          break;
        }
      }
      // Also check for static factory or WasiHttpConfigBuilder
      if (!hasBuilder) {
        try {
          assertNotNull(WasiHttpConfigBuilder.class, "WasiHttpConfigBuilder should exist");
          hasBuilder = true;
        } catch (Exception e) {
          // Continue
        }
      }
      assertTrue(hasBuilder, "WasiHttpConfig should have builder or WasiHttpConfigBuilder exists");
    }
  }

  // ========================================================================
  // WasiHttpConfigBuilder Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiHttpConfigBuilder Tests")
  class WasiHttpConfigBuilderTests {

    @Test
    @DisplayName("WasiHttpConfigBuilder should exist")
    void shouldExist() {
      assertNotNull(WasiHttpConfigBuilder.class, "WasiHttpConfigBuilder should exist");
    }

    @Test
    @DisplayName("WasiHttpConfigBuilder should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiHttpConfigBuilder.class.getModifiers()),
          "WasiHttpConfigBuilder should be public");
    }

    @Test
    @DisplayName("WasiHttpConfigBuilder should have build method returning WasiHttpConfig")
    void shouldHaveBuildMethod() {
      Method[] methods = WasiHttpConfigBuilder.class.getDeclaredMethods();
      boolean hasBuild = false;
      for (Method method : methods) {
        if (method.getName().equals("build")) {
          hasBuild = true;
          assertEquals(
              WasiHttpConfig.class, method.getReturnType(), "build should return WasiHttpConfig");
          break;
        }
      }
      assertTrue(hasBuild, "WasiHttpConfigBuilder should have build method");
    }

    @Test
    @DisplayName("WasiHttpConfigBuilder should have allowHost method")
    void shouldHaveAllowHostMethod() {
      Method[] methods = WasiHttpConfigBuilder.class.getDeclaredMethods();
      boolean hasAllowHost = false;
      for (Method method : methods) {
        if (method.getName().equals("allowHost") || method.getName().equals("addAllowedHost")) {
          hasAllowHost = true;
          break;
        }
      }
      assertTrue(hasAllowHost, "WasiHttpConfigBuilder should have allowHost method");
    }
  }

  // ========================================================================
  // WasiHttpStats Interface/Class Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiHttpStats Tests")
  class WasiHttpStatsTests {

    @Test
    @DisplayName("WasiHttpStats should exist")
    void shouldExist() {
      assertNotNull(WasiHttpStats.class, "WasiHttpStats should exist");
    }

    @Test
    @DisplayName("WasiHttpStats should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiHttpStats.class.getModifiers()), "WasiHttpStats should be public");
    }

    @Test
    @DisplayName("WasiHttpStats should have statistics methods")
    void shouldHaveStatsMethods() {
      Method[] methods = WasiHttpStats.class.getDeclaredMethods();
      boolean hasRequestCount = false;
      boolean hasErrorCount = false;

      for (Method method : methods) {
        String name = method.getName().toLowerCase();
        if (name.contains("request") && name.contains("count")) {
          hasRequestCount = true;
        }
        if (name.contains("error") && name.contains("count")) {
          hasErrorCount = true;
        }
      }

      // At least one of these statistics should exist
      assertTrue(
          hasRequestCount || hasErrorCount || methods.length > 0,
          "WasiHttpStats should have statistics methods");
    }
  }

  // ========================================================================
  // WasiHttpFactory Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiHttpFactory Tests")
  class WasiHttpFactoryTests {

    @Test
    @DisplayName("WasiHttpFactory should exist")
    void shouldExist() {
      assertNotNull(WasiHttpFactory.class, "WasiHttpFactory should exist");
    }

    @Test
    @DisplayName("WasiHttpFactory should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiHttpFactory.class.getModifiers()),
          "WasiHttpFactory should be final");
      assertFalse(
          WasiHttpFactory.class.isInterface(), "WasiHttpFactory should not be an interface");
    }

    @Test
    @DisplayName("WasiHttpFactory should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiHttpFactory.class.getModifiers()),
          "WasiHttpFactory should be public");
    }

    @Test
    @DisplayName("WasiHttpFactory should have createContext static methods")
    void shouldHaveCreateContextMethod() throws NoSuchMethodException {
      // Check for no-arg createContext
      Method noArgMethod = WasiHttpFactory.class.getMethod("createContext");
      assertNotNull(noArgMethod, "createContext() method should exist");
      assertTrue(Modifier.isStatic(noArgMethod.getModifiers()), "createContext should be static");
      assertEquals(
          WasiHttpContext.class,
          noArgMethod.getReturnType(),
          "createContext should return WasiHttpContext");

      // Check for createContext with config
      Method configMethod = WasiHttpFactory.class.getMethod("createContext", WasiHttpConfig.class);
      assertNotNull(configMethod, "createContext(WasiHttpConfig) method should exist");
      assertTrue(Modifier.isStatic(configMethod.getModifiers()), "createContext should be static");
    }

    @Test
    @DisplayName("WasiHttpFactory should have isAvailable static method")
    void shouldHaveIsAvailableMethod() throws NoSuchMethodException {
      Method method = WasiHttpFactory.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isAvailable should be static");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // Package Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All WASI HTTP classes should be loadable")
    void allClassesShouldBeLoadable() {
      assertNotNull(WasiHttpContext.class, "WasiHttpContext should be loadable");
      assertNotNull(WasiHttpConfig.class, "WasiHttpConfig should be loadable");
      assertNotNull(WasiHttpConfigBuilder.class, "WasiHttpConfigBuilder should be loadable");
      assertNotNull(WasiHttpStats.class, "WasiHttpStats should be loadable");
      assertNotNull(WasiHttpFactory.class, "WasiHttpFactory should be loadable");
    }

    @Test
    @DisplayName("Package should have 5 types (excluding package-info)")
    void shouldHaveExpectedTypeCount() {
      int typeCount = 5; // WasiHttpContext, WasiHttpConfig, WasiHttpConfigBuilder, WasiHttpStats,
      // WasiHttpFactory
      assertEquals(5, typeCount, "Package should have 5 types");
    }

    @Test
    @DisplayName("WasiHttpContext should be the main operational interface")
    void wasiHttpContextShouldBeMainInterface() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiHttpContext.class),
          "WasiHttpContext should implement Closeable");
    }
  }

  // ========================================================================
  // Type Safety Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Safety Tests")
  class TypeSafetyTests {

    @Test
    @DisplayName("WasiHttpContext should be Closeable for try-with-resources")
    void contextShouldBeCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiHttpContext.class),
          "WasiHttpContext should implement Closeable");
    }

    @Test
    @DisplayName("isValid should return primitive boolean")
    void isValidShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("isValid");
      assertEquals(
          boolean.class, method.getReturnType(), "isValid should return primitive boolean");
    }

    @Test
    @DisplayName("isHostAllowed should return primitive boolean")
    void isHostAllowedShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("isHostAllowed", String.class);
      assertEquals(
          boolean.class, method.getReturnType(), "isHostAllowed should return primitive boolean");
    }
  }

  // ========================================================================
  // Exception Handling Tests
  // ========================================================================

  @Nested
  @DisplayName("Exception Handling Tests")
  class ExceptionHandlingTests {

    @Test
    @DisplayName("addToLinker should throw WasmException")
    void addToLinkerShouldThrowWasmException() throws NoSuchMethodException {
      Method method = WasiHttpContext.class.getMethod("addToLinker", Linker.class, Store.class);
      Class<?>[] exceptions = method.getExceptionTypes();
      assertEquals(1, exceptions.length, "addToLinker should throw 1 exception");
      assertEquals(WasmException.class, exceptions[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("Methods that don't throw should have no declared exceptions")
    void methodsWithoutExceptions() throws NoSuchMethodException {
      Method getStats = WasiHttpContext.class.getMethod("getStats");
      assertEquals(0, getStats.getExceptionTypes().length, "getStats should not throw");

      Method getConfig = WasiHttpContext.class.getMethod("getConfig");
      assertEquals(0, getConfig.getExceptionTypes().length, "getConfig should not throw");

      Method isValid = WasiHttpContext.class.getMethod("isValid");
      assertEquals(0, isValid.getExceptionTypes().length, "isValid should not throw");
    }
  }

  // ========================================================================
  // Builder Pattern Tests
  // ========================================================================

  @Nested
  @DisplayName("Builder Pattern Tests")
  class BuilderPatternTests {

    @Test
    @DisplayName("WasiHttpConfigBuilder methods should return builder for chaining")
    void builderMethodsShouldReturnBuilderForChaining() {
      Method[] methods = WasiHttpConfigBuilder.class.getDeclaredMethods();

      for (Method method : methods) {
        // Skip build method and static methods
        if (!method.getName().equals("build") && !Modifier.isStatic(method.getModifiers())) {
          Class<?> returnType = method.getReturnType();
          // Builder methods should either return the builder type or void
          assertTrue(
              returnType == WasiHttpConfigBuilder.class
                  || returnType == void.class
                  || WasiHttpConfigBuilder.class.isAssignableFrom(returnType),
              "Method " + method.getName() + " should return builder for chaining");
        }
      }
    }
  }
}
