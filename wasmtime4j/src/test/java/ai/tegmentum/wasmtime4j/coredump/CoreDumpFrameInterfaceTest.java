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
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link CoreDumpFrame} interface.
 *
 * <p>CoreDumpFrame represents a single stack frame captured in a WebAssembly core dump.
 */
@DisplayName("CoreDumpFrame Interface Tests")
class CoreDumpFrameInterfaceTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(CoreDumpFrame.class.isInterface(), "CoreDumpFrame should be an interface");
    }

    @Test
    @DisplayName("should have getFuncIndex method")
    void shouldHaveGetFuncIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("getFuncIndex");
      assertNotNull(method, "getFuncIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getFuncName method")
    void shouldHaveGetFuncNameMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("getFuncName");
      assertNotNull(method, "getFuncName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getModuleIndex method")
    void shouldHaveGetModuleIndexMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("getModuleIndex");
      assertNotNull(method, "getModuleIndex method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("getModuleName");
      assertNotNull(method, "getModuleName method should exist");
      assertEquals(Optional.class, method.getReturnType(), "Should return Optional");
    }

    @Test
    @DisplayName("should have getOffset method")
    void shouldHaveGetOffsetMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("getOffset");
      assertNotNull(method, "getOffset method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getLocals method")
    void shouldHaveGetLocalsMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("getLocals");
      assertNotNull(method, "getLocals method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getStack method")
    void shouldHaveGetStackMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("getStack");
      assertNotNull(method, "getStack method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have isTrapFrame method")
    void shouldHaveIsTrapFrameMethod() throws NoSuchMethodException {
      final Method method = CoreDumpFrame.class.getMethod("isTrapFrame");
      assertNotNull(method, "isTrapFrame method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }
  }

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("should have exactly eight declared methods")
    void shouldHaveExactlyEightDeclaredMethods() {
      final Method[] methods = CoreDumpFrame.class.getDeclaredMethods();
      assertEquals(8, methods.length, "CoreDumpFrame should have exactly 8 declared methods");
    }
  }
}
