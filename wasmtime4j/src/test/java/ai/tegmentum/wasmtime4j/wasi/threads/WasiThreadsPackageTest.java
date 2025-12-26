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

package ai.tegmentum.wasmtime4j.wasi.threads;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the WASI Threads package interfaces and classes.
 *
 * <p>This test class verifies the interface structure and method signatures for the WASI threads
 * API using reflection-based testing.
 */
@DisplayName("WASI Threads Package Tests")
class WasiThreadsPackageTest {

  // ========================================================================
  // WasiThreadsContext Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiThreadsContext Interface Tests")
  class WasiThreadsContextTests {

    @Test
    @DisplayName("WasiThreadsContext should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiThreadsContext.class.isInterface(), "WasiThreadsContext should be an interface");
    }

    @Test
    @DisplayName("WasiThreadsContext should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsContext.class.getModifiers()),
          "WasiThreadsContext should be public");
    }

    @Test
    @DisplayName("WasiThreadsContext should extend Closeable")
    void shouldExtendCloseable() {
      Class<?>[] interfaces = WasiThreadsContext.class.getInterfaces();
      assertEquals(1, interfaces.length, "WasiThreadsContext should extend 1 interface");
      assertEquals(Closeable.class, interfaces[0], "WasiThreadsContext should extend Closeable");
    }

    @Test
    @DisplayName("should have spawn method with int parameter returning int")
    void shouldHaveSpawnMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("spawn", int.class);
      assertNotNull(method, "spawn method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(1, method.getParameterCount(), "spawn should have 1 parameter");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "spawn should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("should have getThreadCount method returning int")
    void shouldHaveGetThreadCountMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("getThreadCount");
      assertNotNull(method, "getThreadCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(0, method.getParameterCount(), "getThreadCount should have no parameters");
    }

    @Test
    @DisplayName("should have isEnabled method returning boolean")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isEnabled should have no parameters");
    }

    @Test
    @DisplayName("should have getMaxThreadId method returning int")
    void shouldHaveGetMaxThreadIdMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("getMaxThreadId");
      assertNotNull(method, "getMaxThreadId method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
      assertEquals(0, method.getParameterCount(), "getMaxThreadId should have no parameters");
    }

    @Test
    @DisplayName("should have isValid method returning boolean")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isValid should have no parameters");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
      assertEquals(0, method.getParameterCount(), "close should have no parameters");
    }

    @Test
    @DisplayName("WasiThreadsContext should have exactly 6 declared methods")
    void shouldHaveExactMethodCount() {
      Method[] methods = WasiThreadsContext.class.getDeclaredMethods();
      assertEquals(6, methods.length, "WasiThreadsContext should have exactly 6 methods");
    }

    @Test
    @DisplayName("All WasiThreadsContext methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = WasiThreadsContext.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            "Method " + method.getName() + " should be public and abstract");
      }
    }
  }

  // ========================================================================
  // WasiThreadsContextBuilder Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiThreadsContextBuilder Interface Tests")
  class WasiThreadsContextBuilderTests {

    @Test
    @DisplayName("WasiThreadsContextBuilder should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiThreadsContextBuilder.class.isInterface(),
          "WasiThreadsContextBuilder should be an interface");
    }

    @Test
    @DisplayName("WasiThreadsContextBuilder should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsContextBuilder.class.getModifiers()),
          "WasiThreadsContextBuilder should be public");
    }

    @Test
    @DisplayName("should have build method returning WasiThreadsContext")
    void shouldHaveBuildMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContextBuilder.class.getMethod("build");
      assertNotNull(method, "build method should exist");
      assertEquals(
          WasiThreadsContext.class,
          method.getReturnType(),
          "Return type should be WasiThreadsContext");
      assertEquals(0, method.getParameterCount(), "build should have no parameters");

      Class<?>[] exceptionTypes = method.getExceptionTypes();
      assertEquals(1, exceptionTypes.length, "build should throw 1 exception");
      assertEquals(WasmException.class, exceptionTypes[0], "Should throw WasmException");
    }

    @Test
    @DisplayName("All WasiThreadsContextBuilder methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = WasiThreadsContextBuilder.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        // Skip static methods (like create())
        if (!Modifier.isStatic(modifiers)) {
          assertTrue(
              Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
              "Method " + method.getName() + " should be public and abstract");
        }
      }
    }
  }

  // ========================================================================
  // WasiThreadsFactory Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiThreadsFactory Class Tests")
  class WasiThreadsFactoryTests {

    @Test
    @DisplayName("WasiThreadsFactory should be a final class")
    void shouldBeAFinalClass() {
      assertTrue(
          Modifier.isFinal(WasiThreadsFactory.class.getModifiers()),
          "WasiThreadsFactory should be final");
      assertFalse(
          WasiThreadsFactory.class.isInterface(), "WasiThreadsFactory should not be an interface");
    }

    @Test
    @DisplayName("WasiThreadsFactory should be a public class")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsFactory.class.getModifiers()),
          "WasiThreadsFactory should be public");
    }

    @Test
    @DisplayName("should have createBuilder static method")
    void shouldHaveCreateBuilderMethod() throws NoSuchMethodException {
      Method method = WasiThreadsFactory.class.getMethod("createBuilder");
      assertNotNull(method, "createBuilder method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "createBuilder should be static");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "createBuilder should return WasiThreadsContextBuilder");
    }

    @Test
    @DisplayName("should have isSupported static method")
    void shouldHaveIsSupportedMethod() throws NoSuchMethodException {
      Method method = WasiThreadsFactory.class.getMethod("isSupported");
      assertNotNull(method, "isSupported method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "isSupported should be static");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // WasiThreadsProvider Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("WasiThreadsProvider Interface Tests")
  class WasiThreadsProviderTests {

    @Test
    @DisplayName("WasiThreadsProvider should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiThreadsProvider.class.isInterface(), "WasiThreadsProvider should be an interface");
    }

    @Test
    @DisplayName("WasiThreadsProvider should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsProvider.class.getModifiers()),
          "WasiThreadsProvider should be public");
    }

    @Test
    @DisplayName("should have isAvailable method returning boolean")
    void shouldHaveIsAvailableMethod() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("isAvailable");
      assertNotNull(method, "isAvailable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(0, method.getParameterCount(), "isAvailable should have no parameters");
    }

    @Test
    @DisplayName("should have createBuilder method")
    void shouldHaveCreateBuilderMethod() throws NoSuchMethodException {
      Method method = WasiThreadsProvider.class.getMethod("createBuilder");
      assertNotNull(method, "createBuilder method should exist");
      assertEquals(
          WasiThreadsContextBuilder.class,
          method.getReturnType(),
          "createBuilder should return WasiThreadsContextBuilder");
    }

    @Test
    @DisplayName("All WasiThreadsProvider methods should be public")
    void allMethodsShouldBePublic() {
      Method[] methods = WasiThreadsProvider.class.getDeclaredMethods();

      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers), "Method " + method.getName() + " should be public");
      }
    }
  }

  // ========================================================================
  // Package Completeness Tests
  // ========================================================================

  @Nested
  @DisplayName("Package Completeness Tests")
  class PackageCompletenessTests {

    @Test
    @DisplayName("All WASI Threads classes should be loadable")
    void allClassesShouldBeLoadable() {
      // Verify all expected classes can be loaded
      assertNotNull(WasiThreadsContext.class, "WasiThreadsContext should be loadable");
      assertNotNull(
          WasiThreadsContextBuilder.class, "WasiThreadsContextBuilder should be loadable");
      assertNotNull(WasiThreadsFactory.class, "WasiThreadsFactory should be loadable");
      assertNotNull(WasiThreadsProvider.class, "WasiThreadsProvider should be loadable");
    }

    @Test
    @DisplayName("Package should contain exactly 4 public types (excluding package-info)")
    void packageShouldContainExpectedTypes() {
      // Count public types in the package
      int publicTypeCount = 0;
      if (Modifier.isPublic(WasiThreadsContext.class.getModifiers())) {
        publicTypeCount++;
      }
      if (Modifier.isPublic(WasiThreadsContextBuilder.class.getModifiers())) {
        publicTypeCount++;
      }
      if (Modifier.isPublic(WasiThreadsFactory.class.getModifiers())) {
        publicTypeCount++;
      }
      if (Modifier.isPublic(WasiThreadsProvider.class.getModifiers())) {
        publicTypeCount++;
      }

      assertEquals(4, publicTypeCount, "Package should have 4 public types");
    }

    @Test
    @DisplayName("WasiThreadsContext should be the main operational interface")
    void wasiThreadsContextShouldBeMainInterface() {
      // Verify WasiThreadsContext has the expected operational methods
      Method[] methods = WasiThreadsContext.class.getDeclaredMethods();
      assertTrue(methods.length >= 5, "WasiThreadsContext should have at least 5 methods");

      // Verify key methods exist
      boolean hasSpawn = false;
      boolean hasGetThreadCount = false;
      boolean hasIsEnabled = false;

      for (Method method : methods) {
        if (method.getName().equals("spawn")) {
          hasSpawn = true;
        }
        if (method.getName().equals("getThreadCount")) {
          hasGetThreadCount = true;
        }
        if (method.getName().equals("isEnabled")) {
          hasIsEnabled = true;
        }
      }

      assertTrue(hasSpawn, "Should have spawn method");
      assertTrue(hasGetThreadCount, "Should have getThreadCount method");
      assertTrue(hasIsEnabled, "Should have isEnabled method");
    }
  }

  // ========================================================================
  // Type Safety Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Safety Tests")
  class TypeSafetyTests {

    @Test
    @DisplayName("WasiThreadsContext should be Closeable for try-with-resources")
    void contextShouldBeCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiThreadsContext.class),
          "WasiThreadsContext should implement Closeable");
    }

    @Test
    @DisplayName("spawn method should return primitive int for thread ID")
    void spawnShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("spawn", int.class);
      assertEquals(int.class, method.getReturnType(), "spawn should return primitive int");
      assertFalse(
          method.getReturnType().equals(Integer.class), "spawn should not return boxed Integer");
    }

    @Test
    @DisplayName("getThreadCount should return primitive int")
    void getThreadCountShouldReturnPrimitiveInt() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("getThreadCount");
      assertEquals(int.class, method.getReturnType(), "getThreadCount should return primitive int");
    }

    @Test
    @DisplayName("Boolean methods should return primitive boolean")
    void booleanMethodsShouldReturnPrimitiveBoolean() throws NoSuchMethodException {
      Method isEnabled = WasiThreadsContext.class.getMethod("isEnabled");
      assertEquals(
          boolean.class, isEnabled.getReturnType(), "isEnabled should return primitive boolean");

      Method isValid = WasiThreadsContext.class.getMethod("isValid");
      assertEquals(
          boolean.class, isValid.getReturnType(), "isValid should return primitive boolean");
    }
  }
}
