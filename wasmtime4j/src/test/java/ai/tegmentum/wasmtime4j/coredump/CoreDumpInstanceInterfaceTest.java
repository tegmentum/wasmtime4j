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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoreDumpInstance} interface.
 *
 * <p>CoreDumpInstance represents instance information captured in a WebAssembly core dump.
 */
@DisplayName("CoreDumpInstance Interface Tests")
class CoreDumpInstanceInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpInstance.class.isInterface(), "CoreDumpInstance should be an interface");
    }

    @Test
    @DisplayName("should have getIndex method")
    void shouldHaveGetIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpInstance.class.getMethod("getIndex");
      assertNotNull(method, "getIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getModuleIndex method")
    void shouldHaveGetModuleIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpInstance.class.getMethod("getModuleIndex");
      assertNotNull(method, "getModuleIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      final Method method = CoreDumpInstance.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getMemoryCount method")
    void shouldHaveGetMemoryCountMethod() throws NoSuchMethodException {
      final Method method = CoreDumpInstance.class.getMethod("getMemoryCount");
      assertNotNull(method, "getMemoryCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getGlobalCount method")
    void shouldHaveGetGlobalCountMethod() throws NoSuchMethodException {
      final Method method = CoreDumpInstance.class.getMethod("getGlobalCount");
      assertNotNull(method, "getGlobalCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getTableCount method")
    void shouldHaveGetTableCountMethod() throws NoSuchMethodException {
      final Method method = CoreDumpInstance.class.getMethod("getTableCount");
      assertNotNull(method, "getTableCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly six declared methods")
    void shouldHaveExactlySixDeclaredMethods() {
      final Method[] methods = CoreDumpInstance.class.getDeclaredMethods();
      assertEquals(6, methods.length, "CoreDumpInstance should have exactly 6 declared methods");
    }
  }
}
