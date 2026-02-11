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

package ai.tegmentum.wasmtime4j.panama;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.component.ComponentDebugInfo;
import ai.tegmentum.wasmtime4j.ExecutionState;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link PanamaComponentDebugInfo} class.
 *
 * <p>PanamaComponentDebugInfo provides debug information for WebAssembly components.
 */
@DisplayName("PanamaComponentDebugInfo Tests")
class PanamaComponentDebugInfoTest {

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("should be package-private final class")
    void shouldBePackagePrivateFinalClass() {
      // Not public - package-private
      assertTrue(
          !Modifier.isPublic(PanamaComponentDebugInfo.class.getModifiers()),
          "PanamaComponentDebugInfo should be package-private");
      assertTrue(
          Modifier.isFinal(PanamaComponentDebugInfo.class.getModifiers()),
          "PanamaComponentDebugInfo should be final");
    }

    @Test
    @DisplayName("should implement ComponentDebugInfo interface")
    void shouldImplementComponentDebugInfoInterface() {
      assertTrue(
          ComponentDebugInfo.class.isAssignableFrom(PanamaComponentDebugInfo.class),
          "PanamaComponentDebugInfo should implement ComponentDebugInfo");
    }
  }

  @Nested
  @DisplayName("ComponentDebugInfo Method Tests")
  class ComponentDebugInfoMethodTests {

    @Test
    @DisplayName("should have getComponentId method")
    void shouldHaveGetComponentIdMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getComponentId");
      assertNotNull(method, "getComponentId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getComponentName method")
    void shouldHaveGetComponentNameMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getComponentName");
      assertNotNull(method, "getComponentName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getSymbols method")
    void shouldHaveGetSymbolsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getSymbols");
      assertNotNull(method, "getSymbols method should exist");
      assertEquals(
          ComponentDebugInfo.DebugSymbols.class,
          method.getReturnType(),
          "Should return DebugSymbols");
    }

    @Test
    @DisplayName("should have getSourceMaps method")
    void shouldHaveGetSourceMapsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getSourceMaps");
      assertNotNull(method, "getSourceMaps method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getExecutionState method")
    void shouldHaveGetExecutionStateMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getExecutionState");
      assertNotNull(method, "getExecutionState method should exist");
      assertEquals(ExecutionState.class, method.getReturnType(), "Should return ExecutionState");
    }

    @Test
    @DisplayName("should have getVariables method")
    void shouldHaveGetVariablesMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getVariables");
      assertNotNull(method, "getVariables method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getFunctions method")
    void shouldHaveGetFunctionsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getFunctions");
      assertNotNull(method, "getFunctions method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getMemoryLayout method")
    void shouldHaveGetMemoryLayoutMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getMemoryLayout");
      assertNotNull(method, "getMemoryLayout method should exist");
      assertEquals(
          ComponentDebugInfo.MemoryLayout.class,
          method.getReturnType(),
          "Should return MemoryLayout");
    }

    @Test
    @DisplayName("should have getStackTrace method")
    void shouldHaveGetStackTraceMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getStackTrace");
      assertNotNull(method, "getStackTrace method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getBreakpoints method")
    void shouldHaveGetBreakpointsMethod() throws NoSuchMethodException {
      final Method method = PanamaComponentDebugInfo.class.getMethod("getBreakpoints");
      assertNotNull(method, "getBreakpoints method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should have package-private constructor with two String parameters")
    void shouldHavePackagePrivateConstructor() {
      boolean hasExpectedConstructor = false;
      for (var constructor : PanamaComponentDebugInfo.class.getDeclaredConstructors()) {
        if (constructor.getParameterCount() == 2
            && constructor.getParameterTypes()[0] == String.class
            && constructor.getParameterTypes()[1] == String.class) {
          hasExpectedConstructor = true;
          break;
        }
      }
      assertTrue(hasExpectedConstructor, "Should have constructor with two String parameters");
    }
  }

  @Nested
  @DisplayName("Inner Class Tests")
  class InnerClassTests {

    @Test
    @DisplayName("should have inner classes for debug information")
    void shouldHaveInnerClasses() {
      Class<?>[] declaredClasses = PanamaComponentDebugInfo.class.getDeclaredClasses();
      assertTrue(declaredClasses.length > 0, "Should have inner classes");
    }
  }
}
