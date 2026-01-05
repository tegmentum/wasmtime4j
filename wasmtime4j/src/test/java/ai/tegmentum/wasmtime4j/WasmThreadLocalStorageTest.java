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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmThreadLocalStorage interface.
 *
 * <p>WasmThreadLocalStorage provides thread-local storage for WebAssembly threads, offering
 * isolated memory space not shared with other threads. This test verifies the interface structure
 * and method signatures.
 */
@DisplayName("WasmThreadLocalStorage Interface Tests")
class WasmThreadLocalStorageTest {

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
          WasmThreadLocalStorage.class.isInterface(),
          "WasmThreadLocalStorage should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(WasmThreadLocalStorage.class.getModifiers()),
          "WasmThreadLocalStorage should be public");
    }
  }

  // ========================================================================
  // Primitive Storage Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Primitive Storage Methods Tests")
  class PrimitiveStorageMethodsTests {

    @Test
    @DisplayName("should have putInt method")
    void shouldHavePutIntMethod() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putInt", String.class, int.class);
      assertNotNull(method, "putInt method should exist");
      assertEquals(void.class, method.getReturnType(), "putInt should return void");
    }

    @Test
    @DisplayName("should have getInt method")
    void shouldHaveGetIntMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getInt", String.class);
      assertNotNull(method, "getInt method should exist");
      assertEquals(int.class, method.getReturnType(), "getInt should return int");
    }

    @Test
    @DisplayName("should have putLong method")
    void shouldHavePutLongMethod() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putLong", String.class, long.class);
      assertNotNull(method, "putLong method should exist");
      assertEquals(void.class, method.getReturnType(), "putLong should return void");
    }

    @Test
    @DisplayName("should have getLong method")
    void shouldHaveGetLongMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getLong", String.class);
      assertNotNull(method, "getLong method should exist");
      assertEquals(long.class, method.getReturnType(), "getLong should return long");
    }

    @Test
    @DisplayName("should have putFloat method")
    void shouldHavePutFloatMethod() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putFloat", String.class, float.class);
      assertNotNull(method, "putFloat method should exist");
      assertEquals(void.class, method.getReturnType(), "putFloat should return void");
    }

    @Test
    @DisplayName("should have getFloat method")
    void shouldHaveGetFloatMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getFloat", String.class);
      assertNotNull(method, "getFloat method should exist");
      assertEquals(float.class, method.getReturnType(), "getFloat should return float");
    }

    @Test
    @DisplayName("should have putDouble method")
    void shouldHavePutDoubleMethod() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putDouble", String.class, double.class);
      assertNotNull(method, "putDouble method should exist");
      assertEquals(void.class, method.getReturnType(), "putDouble should return void");
    }

    @Test
    @DisplayName("should have getDouble method")
    void shouldHaveGetDoubleMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getDouble", String.class);
      assertNotNull(method, "getDouble method should exist");
      assertEquals(double.class, method.getReturnType(), "getDouble should return double");
    }
  }

  // ========================================================================
  // Object Storage Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Object Storage Methods Tests")
  class ObjectStorageMethodsTests {

    @Test
    @DisplayName("should have putBytes method")
    void shouldHavePutBytesMethod() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putBytes", String.class, byte[].class);
      assertNotNull(method, "putBytes method should exist");
      assertEquals(void.class, method.getReturnType(), "putBytes should return void");
    }

    @Test
    @DisplayName("should have getBytes method")
    void shouldHaveGetBytesMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getBytes", String.class);
      assertNotNull(method, "getBytes method should exist");
      assertEquals(byte[].class, method.getReturnType(), "getBytes should return byte[]");
    }

    @Test
    @DisplayName("should have putString method")
    void shouldHavePutStringMethod() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putString", String.class, String.class);
      assertNotNull(method, "putString method should exist");
      assertEquals(void.class, method.getReturnType(), "putString should return void");
    }

    @Test
    @DisplayName("should have getString method")
    void shouldHaveGetStringMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getString", String.class);
      assertNotNull(method, "getString method should exist");
      assertEquals(String.class, method.getReturnType(), "getString should return String");
    }
  }

  // ========================================================================
  // Management Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Management Methods Tests")
  class ManagementMethodsTests {

    @Test
    @DisplayName("should have remove method")
    void shouldHaveRemoveMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("remove", String.class);
      assertNotNull(method, "remove method should exist");
      assertEquals(boolean.class, method.getReturnType(), "remove should return boolean");
    }

    @Test
    @DisplayName("should have contains method")
    void shouldHaveContainsMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("contains", String.class);
      assertNotNull(method, "contains method should exist");
      assertEquals(boolean.class, method.getReturnType(), "contains should return boolean");
    }

    @Test
    @DisplayName("should have clear method")
    void shouldHaveClearMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("clear");
      assertNotNull(method, "clear method should exist");
      assertEquals(void.class, method.getReturnType(), "clear should return void");
    }

    @Test
    @DisplayName("should have size method")
    void shouldHaveSizeMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "getMemoryUsage should return long");
    }
  }

  // ========================================================================
  // Method Parameters Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameters Tests")
  class MethodParametersTests {

    @Test
    @DisplayName("putInt should have 2 parameters")
    void putIntShouldHave2Parameters() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putInt", String.class, int.class);
      assertEquals(2, method.getParameterCount(), "putInt should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(int.class, method.getParameterTypes()[1], "Second parameter should be int");
    }

    @Test
    @DisplayName("getInt should have 1 String parameter")
    void getIntShouldHave1Parameter() throws NoSuchMethodException {
      final Method method = WasmThreadLocalStorage.class.getMethod("getInt", String.class);
      assertEquals(1, method.getParameterCount(), "getInt should have 1 parameter");
      assertEquals(String.class, method.getParameterTypes()[0], "Parameter should be String");
    }

    @Test
    @DisplayName("putBytes should have 2 parameters")
    void putBytesShouldHave2Parameters() throws NoSuchMethodException {
      final Method method =
          WasmThreadLocalStorage.class.getMethod("putBytes", String.class, byte[].class);
      assertEquals(2, method.getParameterCount(), "putBytes should have 2 parameters");
      assertEquals(String.class, method.getParameterTypes()[0], "First parameter should be String");
      assertEquals(
          byte[].class, method.getParameterTypes()[1], "Second parameter should be byte[]");
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
              "putInt",
              "getInt",
              "putLong",
              "getLong",
              "putFloat",
              "getFloat",
              "putDouble",
              "getDouble",
              "putBytes",
              "getBytes",
              "putString",
              "getString",
              "remove",
              "contains",
              "clear",
              "size",
              "getMemoryUsage");

      Set<String> actualMethods =
          Arrays.stream(WasmThreadLocalStorage.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "WasmThreadLocalStorage should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 17 declared methods")
    void shouldHaveAtLeast17DeclaredMethods() {
      assertTrue(
          WasmThreadLocalStorage.class.getDeclaredMethods().length >= 17,
          "WasmThreadLocalStorage should have at least 17 methods (found "
              + WasmThreadLocalStorage.class.getDeclaredMethods().length
              + ")");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interface")
    void shouldNotExtendAnyInterface() {
      assertEquals(
          0,
          WasmThreadLocalStorage.class.getInterfaces().length,
          "WasmThreadLocalStorage should not extend any interface");
    }
  }
}
