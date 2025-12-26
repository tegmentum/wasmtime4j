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

import java.lang.reflect.Method;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link WasmCoreDump} interface.
 *
 * <p>WasmCoreDump represents a captured state of WebAssembly execution at the time of a trap.
 */
@DisplayName("WasmCoreDump Interface Tests")
class WasmCoreDumpInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(WasmCoreDump.class.isInterface(), "WasmCoreDump should be an interface");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getFrames method")
    void shouldHaveGetFramesMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getFrames");
      assertNotNull(method, "getFrames method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getModules method")
    void shouldHaveGetModulesMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getModules");
      assertNotNull(method, "getModules method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getInstances method")
    void shouldHaveGetInstancesMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getInstances");
      assertNotNull(method, "getInstances method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getGlobals method")
    void shouldHaveGetGlobalsMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getGlobals");
      assertNotNull(method, "getGlobals method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getMemories method")
    void shouldHaveGetMemoriesMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getMemories");
      assertNotNull(method, "getMemories method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have serialize method")
    void shouldHaveSerializeMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("serialize");
      assertNotNull(method, "serialize method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getTrapMessage method")
    void shouldHaveGetTrapMessageMethod() throws NoSuchMethodException {
      final Method method = WasmCoreDump.class.getMethod("getTrapMessage");
      assertNotNull(method, "getTrapMessage method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly nine declared methods")
    void shouldHaveExactlyNineDeclaredMethods() {
      final Method[] methods = WasmCoreDump.class.getDeclaredMethods();
      assertEquals(9, methods.length, "WasmCoreDump should have exactly 9 declared methods");
    }
  }
}
