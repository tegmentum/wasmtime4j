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

package ai.tegmentum.wasmtime4j.ref;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link HeapType} interface.
 *
 * <p>HeapType represents a WebAssembly heap type for reference types.
 */
@DisplayName("HeapType Interface Tests")
class HeapTypeInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(HeapType.class.isInterface(), "HeapType should be an interface");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(WasmValueType.class, method.getReturnType(), "Should return WasmValueType");
    }

    @Test
    @DisplayName("should have isNullable method")
    void shouldHaveIsNullableMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("isNullable");
      assertNotNull(method, "isNullable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isBottom method")
    void shouldHaveIsBottomMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("isBottom");
      assertNotNull(method, "isBottom method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isSubtypeOf method")
    void shouldHaveIsSubtypeOfMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("isSubtypeOf", HeapType.class);
      assertNotNull(method, "isSubtypeOf method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getTypeName method")
    void shouldHaveGetTypeNameMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("getTypeName");
      assertNotNull(method, "getTypeName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have asNullable method")
    void shouldHaveAsNullableMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("asNullable");
      assertNotNull(method, "asNullable method should exist");
      assertEquals(HeapType.class, method.getReturnType(), "Should return HeapType");
    }

    @Test
    @DisplayName("should have asNonNullable method")
    void shouldHaveAsNonNullableMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("asNonNullable");
      assertNotNull(method, "asNonNullable method should exist");
      assertEquals(HeapType.class, method.getReturnType(), "Should return HeapType");
    }
  }

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("isBottom should be a default method")
    void isBottomShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("isBottom");
      assertTrue(method.isDefault(), "isBottom should be a default method");
    }

    @Test
    @DisplayName("asNullable should be a default method")
    void asNullableShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("asNullable");
      assertTrue(method.isDefault(), "asNullable should be a default method");
    }

    @Test
    @DisplayName("asNonNullable should be a default method")
    void asNonNullableShouldBeDefaultMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("asNonNullable");
      assertTrue(method.isDefault(), "asNonNullable should be a default method");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Tests")
  class StaticFactoryMethodTests {

    @Test
    @DisplayName("should have static none method")
    void shouldHaveStaticNoneMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("none");
      assertNotNull(method, "none method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "none should be static");
      assertEquals(HeapType.class, method.getReturnType(), "Should return HeapType");
    }

    @Test
    @DisplayName("should have static nofunc method")
    void shouldHaveStaticNofuncMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("nofunc");
      assertNotNull(method, "nofunc method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "nofunc should be static");
      assertEquals(HeapType.class, method.getReturnType(), "Should return HeapType");
    }

    @Test
    @DisplayName("should have static noextern method")
    void shouldHaveStaticNoexternMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("noextern");
      assertNotNull(method, "noextern method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "noextern should be static");
      assertEquals(HeapType.class, method.getReturnType(), "Should return HeapType");
    }

    @Test
    @DisplayName("should have static funcNull method")
    void shouldHaveStaticFuncNullMethod() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("funcNull");
      assertNotNull(method, "funcNull method should exist");
      assertTrue(Modifier.isStatic(method.getModifiers()), "funcNull should be static");
      assertEquals(FuncRef.class, method.getReturnType(), "Should return FuncRef");
    }
  }

  @Nested
  @DisplayName("Static Factory Method Behavior Tests")
  class StaticFactoryMethodBehaviorTests {

    @Test
    @DisplayName("none should return a HeapType")
    void noneShouldReturnHeapType() {
      final HeapType none = HeapType.none();
      assertNotNull(none, "none() should return a non-null HeapType");
    }

    @Test
    @DisplayName("nofunc should return a HeapType")
    void nofuncShouldReturnHeapType() {
      final HeapType nofunc = HeapType.nofunc();
      assertNotNull(nofunc, "nofunc() should return a non-null HeapType");
    }

    @Test
    @DisplayName("noextern should return a HeapType")
    void noexternShouldReturnHeapType() {
      final HeapType noextern = HeapType.noextern();
      assertNotNull(noextern, "noextern() should return a non-null HeapType");
    }

    @Test
    @DisplayName("funcNull should return a null FuncRef")
    void funcNullShouldReturnNullFuncRef() {
      final FuncRef nullRef = HeapType.funcNull();
      assertNotNull(nullRef, "funcNull() should return a non-null FuncRef");
      assertTrue(nullRef.isNull(), "funcNull() should return a null reference");
    }
  }

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("isSubtypeOf should have one parameter")
    void isSubtypeOfShouldHaveOneParameter() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("isSubtypeOf", HeapType.class);
      assertEquals(1, method.getParameterCount(), "isSubtypeOf should have one parameter");
      assertEquals(HeapType.class, method.getParameterTypes()[0], "Parameter should be HeapType");
    }
  }

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("getValueType should not be default")
    void getValueTypeShouldNotBeDefault() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("getValueType");
      assertFalse(method.isDefault(), "getValueType should not be a default method");
    }

    @Test
    @DisplayName("isNullable should not be default")
    void isNullableShouldNotBeDefault() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("isNullable");
      assertFalse(method.isDefault(), "isNullable should not be a default method");
    }

    @Test
    @DisplayName("isSubtypeOf should not be default")
    void isSubtypeOfShouldNotBeDefault() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("isSubtypeOf", HeapType.class);
      assertFalse(method.isDefault(), "isSubtypeOf should not be a default method");
    }

    @Test
    @DisplayName("getTypeName should not be default")
    void getTypeNameShouldNotBeDefault() throws NoSuchMethodException {
      final Method method = HeapType.class.getMethod("getTypeName");
      assertFalse(method.isDefault(), "getTypeName should not be a default method");
    }
  }
}
