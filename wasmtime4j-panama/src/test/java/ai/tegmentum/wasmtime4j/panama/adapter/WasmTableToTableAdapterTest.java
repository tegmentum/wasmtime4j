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

package ai.tegmentum.wasmtime4j.panama.adapter;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Table;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.WasmTable;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmTableToTableAdapter} class.
 *
 * <p>WasmTableToTableAdapter bridges the gap between the WasmTable interface and the Table
 * interface used by the Caller interface, handling type conversions and method signature
 * differences.
 */
@DisplayName("WasmTableToTableAdapter Tests")
class WasmTableToTableAdapterTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasmTableToTableAdapter.class.getModifiers()),
          "WasmTableToTableAdapter should be public");
      assertTrue(
          Modifier.isFinal(WasmTableToTableAdapter.class.getModifiers()),
          "WasmTableToTableAdapter should be final");
    }

    @Test
    @DisplayName("should implement Table interface")
    void shouldImplementTableInterface() {
      assertTrue(
          Table.class.isAssignableFrom(WasmTableToTableAdapter.class),
          "WasmTableToTableAdapter should implement Table");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with WasmTable parameter")
    void shouldHaveConstructorWithWasmTable() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasmTableToTableAdapter.class.getConstructor(WasmTable.class);
      assertNotNull(constructor, "Constructor with WasmTable should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Size Method Tests")
  class SizeMethodTests {

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasmTableToTableAdapter.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getMaxSize method")
    void shouldHaveGetMaxSizeMethod() throws NoSuchMethodException {
      final Method method = WasmTableToTableAdapter.class.getMethod("getMaxSize");
      assertNotNull(method, "getMaxSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Grow Method Tests")
  class GrowMethodTests {

    @Test
    @DisplayName("should have grow method")
    void shouldHaveGrowMethod() throws NoSuchMethodException {
      final Method method =
          WasmTableToTableAdapter.class.getMethod("grow", long.class, Object.class);
      assertNotNull(method, "grow method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have growAsync method")
    void shouldHaveGrowAsyncMethod() throws NoSuchMethodException {
      final Method method =
          WasmTableToTableAdapter.class.getMethod("growAsync", long.class, Object.class);
      assertNotNull(method, "growAsync method should exist");
      assertEquals(
          CompletableFuture.class, method.getReturnType(), "Should return CompletableFuture");
    }
  }

  @Nested
  @DisplayName("Element Access Method Tests")
  class ElementAccessMethodTests {

    @Test
    @DisplayName("should have get method")
    void shouldHaveGetMethod() throws NoSuchMethodException {
      final Method method = WasmTableToTableAdapter.class.getMethod("get", long.class);
      assertNotNull(method, "get method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("should have set method")
    void shouldHaveSetMethod() throws NoSuchMethodException {
      final Method method =
          WasmTableToTableAdapter.class.getMethod("set", long.class, Object.class);
      assertNotNull(method, "set method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have fill method")
    void shouldHaveFillMethod() throws NoSuchMethodException {
      final Method method =
          WasmTableToTableAdapter.class.getMethod("fill", long.class, Object.class, long.class);
      assertNotNull(method, "fill method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have copy method")
    void shouldHaveCopyMethod() throws NoSuchMethodException {
      final Method method =
          WasmTableToTableAdapter.class.getMethod(
              "copy", long.class, Table.class, long.class, long.class);
      assertNotNull(method, "copy method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Type Method Tests")
  class TypeMethodTests {

    @Test
    @DisplayName("should have getElementType method")
    void shouldHaveGetElementTypeMethod() throws NoSuchMethodException {
      final Method method = WasmTableToTableAdapter.class.getMethod("getElementType");
      assertNotNull(method, "getElementType method should exist");
      assertEquals(
          Table.TableElementType.class, method.getReturnType(), "Should return TableElementType");
    }

    @Test
    @DisplayName("should have getTableType method")
    void shouldHaveGetTableTypeMethod() throws NoSuchMethodException {
      final Method method = WasmTableToTableAdapter.class.getMethod("getTableType");
      assertNotNull(method, "getTableType method should exist");
      assertEquals(TableType.class, method.getReturnType(), "Should return TableType");
    }
  }

  @Nested
  @DisplayName("State Method Tests")
  class StateMethodTests {

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasmTableToTableAdapter.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDelegate method")
    void shouldHaveGetDelegateMethod() throws NoSuchMethodException {
      final Method method = WasmTableToTableAdapter.class.getMethod("getDelegate");
      assertNotNull(method, "getDelegate method should exist");
      assertEquals(WasmTable.class, method.getReturnType(), "Should return WasmTable");
    }
  }
}
