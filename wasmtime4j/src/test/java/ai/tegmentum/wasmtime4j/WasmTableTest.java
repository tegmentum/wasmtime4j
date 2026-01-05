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
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive test suite for the WasmTable interface.
 *
 * <p>WasmTable represents a WebAssembly table - resizable arrays of references that can be accessed
 * by WebAssembly code. This test verifies the interface structure and method signatures.
 */
@DisplayName("WasmTable Interface Tests")
class WasmTableTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmTable.class.isInterface(), "WasmTable should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(Modifier.isPublic(WasmTable.class.getModifiers()), "WasmTable should be public");
    }
  }

  // ========================================================================
  // Abstract Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Methods Tests")
  class AbstractMethodsTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getSize should return int");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          WasmValueType.class, method.getReturnType(), "getType should return WasmValueType");
    }

    @Test
    @DisplayName("should have getTableType method")
    void shouldHaveGetTableTypeMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("getTableType");
      assertNotNull(method, "getTableType method should exist");
      assertEquals(TableType.class, method.getReturnType(), "getTableType should return TableType");
    }

    @Test
    @DisplayName("should have grow method")
    void shouldHaveGrowMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("grow", int.class, Object.class);
      assertNotNull(method, "grow method should exist");
      assertEquals(int.class, method.getReturnType(), "grow should return int");
    }

    @Test
    @DisplayName("should have getMaxSize method")
    void shouldHaveGetMaxSizeMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("getMaxSize");
      assertNotNull(method, "getMaxSize method should exist");
      assertEquals(int.class, method.getReturnType(), "getMaxSize should return int");
    }

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("get", int.class);
      assertNotNull(method, "get method should exist");
      assertEquals(Object.class, method.getReturnType(), "get should return Object");
    }

    @Test
    @DisplayName("should have set method")
    void shouldHaveSetMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("set", int.class, Object.class);
      assertNotNull(method, "set method should exist");
      assertEquals(void.class, method.getReturnType(), "set should return void");
    }

    @Test
    @DisplayName("should have getElementType method")
    void shouldHaveGetElementTypeMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("getElementType");
      assertNotNull(method, "getElementType method should exist");
      assertEquals(
          WasmValueType.class,
          method.getReturnType(),
          "getElementType should return WasmValueType");
    }

    @Test
    @DisplayName("should have fill method")
    void shouldHaveFillMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("fill", int.class, int.class, Object.class);
      assertNotNull(method, "fill method should exist");
      assertEquals(void.class, method.getReturnType(), "fill should return void");
    }

    @Test
    @DisplayName("should have copy method for same table")
    void shouldHaveCopyMethodSameTable() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("copy", int.class, int.class, int.class);
      assertNotNull(method, "copy method should exist");
      assertEquals(void.class, method.getReturnType(), "copy should return void");
    }

    @Test
    @DisplayName("should have copy method for different table")
    void shouldHaveCopyMethodDifferentTable() throws NoSuchMethodException {
      final Method method =
          WasmTable.class.getMethod("copy", int.class, WasmTable.class, int.class, int.class);
      assertNotNull(method, "copy method should exist");
      assertEquals(void.class, method.getReturnType(), "copy should return void");
    }

    @Test
    @DisplayName("should have init method")
    void shouldHaveInitMethod() throws NoSuchMethodException {
      final Method method =
          WasmTable.class.getMethod("init", int.class, int.class, int.class, int.class);
      assertNotNull(method, "init method should exist");
      assertEquals(void.class, method.getReturnType(), "init should return void");
    }

    @Test
    @DisplayName("should have dropElementSegment method")
    void shouldHaveDropElementSegmentMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("dropElementSegment", int.class);
      assertNotNull(method, "dropElementSegment method should exist");
      assertEquals(void.class, method.getReturnType(), "dropElementSegment should return void");
    }
  }

  // ========================================================================
  // Default Methods Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Methods Tests")
  class DefaultMethodsTests {

    @Test
    @DisplayName("should have size default method")
    void shouldHaveSizeDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("size");
      assertNotNull(method, "size method should exist");
      assertTrue(method.isDefault(), "size should be a default method");
      assertEquals(int.class, method.getReturnType(), "size should return int");
    }

    @Test
    @DisplayName("should have supports64BitAddressing default method")
    void shouldHaveSupports64BitAddressingDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("supports64BitAddressing");
      assertNotNull(method, "supports64BitAddressing method should exist");
      assertTrue(method.isDefault(), "supports64BitAddressing should be a default method");
      assertEquals(
          boolean.class, method.getReturnType(), "supports64BitAddressing should return boolean");
    }

    @Test
    @DisplayName("should have getSize64 default method")
    void shouldHaveGetSize64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("getSize64");
      assertNotNull(method, "getSize64 method should exist");
      assertTrue(method.isDefault(), "getSize64 should be a default method");
      assertEquals(long.class, method.getReturnType(), "getSize64 should return long");
    }

    @Test
    @DisplayName("should have get64 default method")
    void shouldHaveGet64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("get64", long.class);
      assertNotNull(method, "get64 method should exist");
      assertTrue(method.isDefault(), "get64 should be a default method");
      assertEquals(WasmValue.class, method.getReturnType(), "get64 should return WasmValue");
    }

    @Test
    @DisplayName("should have set64 default method")
    void shouldHaveSet64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("set64", long.class, WasmValue.class);
      assertNotNull(method, "set64 method should exist");
      assertTrue(method.isDefault(), "set64 should be a default method");
      assertEquals(void.class, method.getReturnType(), "set64 should return void");
    }

    @Test
    @DisplayName("should have grow64 default method")
    void shouldHaveGrow64DefaultMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("grow64", long.class, WasmValue.class);
      assertNotNull(method, "grow64 method should exist");
      assertTrue(method.isDefault(), "grow64 should be a default method");
      assertEquals(long.class, method.getReturnType(), "grow64 should return long");
    }

    @Test
    @DisplayName("should have fill64 default method")
    void shouldHaveFill64DefaultMethod() throws NoSuchMethodException {
      final Method method =
          WasmTable.class.getMethod("fill64", long.class, long.class, WasmValue.class);
      assertNotNull(method, "fill64 method should exist");
      assertTrue(method.isDefault(), "fill64 should be a default method");
      assertEquals(void.class, method.getReturnType(), "fill64 should return void");
    }

    @Test
    @DisplayName("should have copy64 default method")
    void shouldHaveCopy64DefaultMethod() throws NoSuchMethodException {
      final Method method =
          WasmTable.class.getMethod("copy64", long.class, WasmTable.class, long.class, long.class);
      assertNotNull(method, "copy64 method should exist");
      assertTrue(method.isDefault(), "copy64 should be a default method");
      assertEquals(void.class, method.getReturnType(), "copy64 should return void");
    }

    @Test
    @DisplayName("should have init64 default method")
    void shouldHaveInit64DefaultMethod() throws NoSuchMethodException {
      final Method method =
          WasmTable.class.getMethod("init64", long.class, int.class, long.class, long.class);
      assertNotNull(method, "init64 method should exist");
      assertTrue(method.isDefault(), "init64 should be a default method");
      assertEquals(void.class, method.getReturnType(), "init64 should return void");
    }

    @Test
    @DisplayName("should have growAsync default method")
    void shouldHaveGrowAsyncDefaultMethod() throws NoSuchMethodException {
      final Method method = WasmTable.class.getMethod("growAsync", int.class, Object.class);
      assertNotNull(method, "growAsync method should exist");
      assertTrue(method.isDefault(), "growAsync should be a default method");
      assertEquals(
          CompletableFuture.class,
          method.getReturnType(),
          "growAsync should return CompletableFuture");
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
              "getSize",
              "size",
              "getType",
              "getTableType",
              "grow",
              "getMaxSize",
              "get",
              "set",
              "getElementType",
              "fill",
              "copy",
              "init",
              "dropElementSegment",
              "supports64BitAddressing",
              "getSize64",
              "get64",
              "set64",
              "grow64",
              "fill64",
              "copy64",
              "init64",
              "growAsync");

      Set<String> actualMethods =
          Arrays.stream(WasmTable.class.getDeclaredMethods())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(actualMethods.contains(expected), "WasmTable should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have at least 22 declared methods")
    void shouldHaveAtLeast22DeclaredMethods() {
      assertTrue(
          WasmTable.class.getDeclaredMethods().length >= 22,
          "WasmTable should have at least 22 methods");
    }

    @Test
    @DisplayName("should have at least 10 default methods")
    void shouldHaveAtLeast10DefaultMethods() {
      long defaultMethodCount =
          Arrays.stream(WasmTable.class.getDeclaredMethods()).filter(Method::isDefault).count();
      assertTrue(defaultMethodCount >= 10, "WasmTable should have at least 10 default methods");
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
          0, WasmTable.class.getInterfaces().length, "WasmTable should not extend any interface");
    }
  }
}
