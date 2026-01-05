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

import ai.tegmentum.wasmtime4j.Global;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmGlobalToGlobalAdapter} class.
 *
 * <p>WasmGlobalToGlobalAdapter bridges the gap between the WasmGlobal interface and the Global
 * interface used by the Caller interface, handling type conversions and method signature
 * differences.
 */
@DisplayName("WasmGlobalToGlobalAdapter Tests")
class WasmGlobalToGlobalAdapterTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be public final class")
    void shouldBePublicFinalClass() {
      assertTrue(
          Modifier.isPublic(WasmGlobalToGlobalAdapter.class.getModifiers()),
          "WasmGlobalToGlobalAdapter should be public");
      assertTrue(
          Modifier.isFinal(WasmGlobalToGlobalAdapter.class.getModifiers()),
          "WasmGlobalToGlobalAdapter should be final");
    }

    @Test
    @DisplayName("should implement Global interface")
    void shouldImplementGlobalInterface() {
      assertTrue(
          Global.class.isAssignableFrom(WasmGlobalToGlobalAdapter.class),
          "WasmGlobalToGlobalAdapter should implement Global");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have constructor with WasmGlobal parameter")
    void shouldHaveConstructorWithWasmGlobal() throws NoSuchMethodException {
      final Constructor<?> constructor =
          WasmGlobalToGlobalAdapter.class.getConstructor(WasmGlobal.class);
      assertNotNull(constructor, "Constructor with WasmGlobal should exist");
      assertTrue(Modifier.isPublic(constructor.getModifiers()), "Constructor should be public");
    }
  }

  @Nested
  @DisplayName("Value Getter Method Tests")
  class ValueGetterMethodTests {

    @Test
    @DisplayName("should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("getValue");
      assertNotNull(method, "getValue method should exist");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("should have getIntValue method")
    void shouldHaveGetIntValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("getIntValue");
      assertNotNull(method, "getIntValue method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getLongValue method")
    void shouldHaveGetLongValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("getLongValue");
      assertNotNull(method, "getLongValue method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFloatValue method")
    void shouldHaveGetFloatValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("getFloatValue");
      assertNotNull(method, "getFloatValue method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have getDoubleValue method")
    void shouldHaveGetDoubleValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("getDoubleValue");
      assertNotNull(method, "getDoubleValue method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Value Setter Method Tests")
  class ValueSetterMethodTests {

    @Test
    @DisplayName("should have setValue method")
    void shouldHaveSetValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("setValue", Object.class);
      assertNotNull(method, "setValue method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setIntValue method")
    void shouldHaveSetIntValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("setIntValue", int.class);
      assertNotNull(method, "setIntValue method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setLongValue method")
    void shouldHaveSetLongValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("setLongValue", long.class);
      assertNotNull(method, "setLongValue method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setFloatValue method")
    void shouldHaveSetFloatValueMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("setFloatValue", float.class);
      assertNotNull(method, "setFloatValue method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have setDoubleValue method")
    void shouldHaveSetDoubleValueMethod() throws NoSuchMethodException {
      final Method method =
          WasmGlobalToGlobalAdapter.class.getMethod("setDoubleValue", double.class);
      assertNotNull(method, "setDoubleValue method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("Type and State Method Tests")
  class TypeAndStateMethodTests {

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(
          Global.GlobalValueType.class, method.getReturnType(), "Should return GlobalValueType");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have isValid method")
    void shouldHaveIsValidMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("isValid");
      assertNotNull(method, "isValid method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getDelegate method")
    void shouldHaveGetDelegateMethod() throws NoSuchMethodException {
      final Method method = WasmGlobalToGlobalAdapter.class.getMethod("getDelegate");
      assertNotNull(method, "getDelegate method should exist");
      assertEquals(WasmGlobal.class, method.getReturnType(), "Should return WasmGlobal");
    }
  }
}
