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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.Closeable;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasiThreadsContext interface.
 *
 * <p>Tests the interface definition, method signatures, and inheritance structure.
 */
@DisplayName("WasiThreadsContext Interface Tests")
class WasiThreadsContextTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          WasiThreadsContext.class.isInterface(), "WasiThreadsContext should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasiThreadsContext.class.getModifiers()),
          "WasiThreadsContext should be public");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should extend Closeable")
    void shouldExtendCloseable() {
      assertTrue(
          Closeable.class.isAssignableFrom(WasiThreadsContext.class),
          "WasiThreadsContext should extend Closeable");
    }

    @Test
    @DisplayName("should have exactly 1 parent interface")
    void shouldHaveExactlyOneParentInterface() {
      assertEquals(
          1,
          WasiThreadsContext.class.getInterfaces().length,
          "WasiThreadsContext should have exactly 1 parent interface");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have spawn method")
    void shouldHaveSpawnMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("spawn", int.class);
      assertNotNull(method, "spawn method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("spawn should have int parameter")
    void spawnShouldHaveIntParameter() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("spawn", int.class);
      assertEquals(1, method.getParameterCount(), "Should have 1 parameter");
      assertEquals(int.class, method.getParameterTypes()[0], "Parameter should be int");
    }

    @Test
    @DisplayName("spawn should declare WasmException")
    void spawnShouldDeclareWasmException() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("spawn", int.class);
      Class<?>[] exceptionTypes = method.getExceptionTypes();
      boolean declaresWasmException =
          Arrays.stream(exceptionTypes).anyMatch(e -> e.getSimpleName().equals("WasmException"));
      assertTrue(declaresWasmException, "spawn should declare WasmException");
    }

    @Test
    @DisplayName("should have getThreadCount method")
    void shouldHaveGetThreadCountMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("getThreadCount");
      assertNotNull(method, "getThreadCount method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getMaxThreadId method")
    void shouldHaveGetMaxThreadIdMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("getMaxThreadId");
      assertNotNull(method, "getMaxThreadId method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertTrue(Modifier.isAbstract(method.getModifiers()), "Should be abstract");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have close method")
    void shouldHaveCloseMethod() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("close");
      assertNotNull(method, "close method should exist");
      assertTrue(Modifier.isPublic(method.getModifiers()), "Should be public");
      assertEquals(void.class, method.getReturnType(), "Should return void");
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
          Set.of("spawn", "getThreadCount", "isEnabled", "getMaxThreadId", "isValid", "close");

      Set<String> actualMethods =
          Arrays.stream(WasiThreadsContext.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "Should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 6 declared methods")
    void shouldHaveExactly6DeclaredMethods() {
      long methodCount =
          Arrays.stream(WasiThreadsContext.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(6, methodCount, "Should have exactly 6 declared methods");
    }

    @Test
    @DisplayName("all methods should be abstract")
    void allMethodsShouldBeAbstract() {
      boolean allAbstract =
          Arrays.stream(WasiThreadsContext.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isAbstract(m.getModifiers()));
      assertTrue(allAbstract, "All methods should be abstract");
    }

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      boolean allPublic =
          Arrays.stream(WasiThreadsContext.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .allMatch(m -> Modifier.isPublic(m.getModifiers()));
      assertTrue(allPublic, "All methods should be public");
    }

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticCount =
          Arrays.stream(WasiThreadsContext.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticCount, "Should have no static methods");
    }

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultCount =
          Arrays.stream(WasiThreadsContext.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultCount, "Should have no default methods");
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
          WasiThreadsContext.class.getDeclaredClasses().length,
          "WasiThreadsContext should have no nested classes");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          WasiThreadsContext.class.getDeclaredFields().length,
          "WasiThreadsContext should have no declared fields");
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("getThreadCount should have no parameters")
    void getThreadCountShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("getThreadCount");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("isEnabled should have no parameters")
    void isEnabledShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("isEnabled");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("getMaxThreadId should have no parameters")
    void getMaxThreadIdShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("getMaxThreadId");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("isValid should have no parameters")
    void isValidShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("isValid");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }

    @Test
    @DisplayName("close should have no parameters")
    void closeShouldHaveNoParameters() throws NoSuchMethodException {
      Method method = WasiThreadsContext.class.getMethod("close");
      assertEquals(0, method.getParameterCount(), "Should have no parameters");
    }
  }
}
