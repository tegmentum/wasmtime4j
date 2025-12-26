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

package ai.tegmentum.wasmtime4j.coredump;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.reflect.Method;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoreDumpGlobal} interface.
 *
 * <p>CoreDumpGlobal represents a global variable captured in a WebAssembly core dump.
 */
@DisplayName("CoreDumpGlobal Interface Tests")
class CoreDumpGlobalInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpGlobal.class.isInterface(), "CoreDumpGlobal should be an interface");
    }

    @Test
    @DisplayName("should have getInstanceIndex method")
    void shouldHaveGetInstanceIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getInstanceIndex");
      assertNotNull(method, "getInstanceIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getGlobalIndex method")
    void shouldHaveGetGlobalIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getGlobalIndex");
      assertNotNull(method, "getGlobalIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getValueType method")
    void shouldHaveGetValueTypeMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getValueType");
      assertNotNull(method, "getValueType method should exist");
      assertEquals(WasmValueType.class, method.getReturnType(), "Should return WasmValueType");
    }

    @Test
    @DisplayName("should have isMutable method")
    void shouldHaveIsMutableMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("isMutable");
      assertNotNull(method, "isMutable method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have getRawValue method")
    void shouldHaveGetRawValueMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getRawValue");
      assertNotNull(method, "getRawValue method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have getI32Value method")
    void shouldHaveGetI32ValueMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getI32Value");
      assertNotNull(method, "getI32Value method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getI64Value method")
    void shouldHaveGetI64ValueMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getI64Value");
      assertNotNull(method, "getI64Value method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getF32Value method")
    void shouldHaveGetF32ValueMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getF32Value");
      assertNotNull(method, "getF32Value method should exist");
      assertEquals(float.class, method.getReturnType(), "Should return float");
    }

    @Test
    @DisplayName("should have getF64Value method")
    void shouldHaveGetF64ValueMethod() throws NoSuchMethodException {
      final Method method = CoreDumpGlobal.class.getMethod("getF64Value");
      assertNotNull(method, "getF64Value method should exist");
      assertEquals(double.class, method.getReturnType(), "Should return double");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly ten declared methods")
    void shouldHaveExactlyTenDeclaredMethods() {
      final Method[] methods = CoreDumpGlobal.class.getDeclaredMethods();
      assertEquals(10, methods.length, "CoreDumpGlobal should have exactly 10 declared methods");
    }
  }
}
