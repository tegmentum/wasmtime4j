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
import static org.junit.jupiter.api.Assertions.assertFalse;
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
 * Comprehensive test suite for the ExecutionState interface.
 *
 * <p>ExecutionState provides monitoring capabilities for WebAssembly execution state including
 * pointers, statistics, and timing information.
 */
@DisplayName("ExecutionState Interface Tests")
class ExecutionStateTest {

  // ========================================================================
  // Type Definition Tests
  // ========================================================================

  @Nested
  @DisplayName("Type Definition Tests")
  class TypeDefinitionTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionState.class.isInterface(), "ExecutionState should be an interface");
    }

    @Test
    @DisplayName("should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionState.class.getModifiers()),
          "ExecutionState should be public");
    }

    @Test
    @DisplayName("should not be final")
    void shouldNotBeFinal() {
      assertFalse(
          Modifier.isFinal(ExecutionState.class.getModifiers()),
          "ExecutionState should not be final (interfaces cannot be final)");
    }
  }

  // ========================================================================
  // Inheritance Tests
  // ========================================================================

  @Nested
  @DisplayName("Inheritance Tests")
  class InheritanceTests {

    @Test
    @DisplayName("should not extend any interfaces")
    void shouldNotExtendAnyInterfaces() {
      assertEquals(
          0,
          ExecutionState.class.getInterfaces().length,
          "ExecutionState should not extend any interfaces");
    }
  }

  // ========================================================================
  // Abstract Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Abstract Method Tests")
  class AbstractMethodTests {

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.execution.ExecutionStatus.class,
          method.getReturnType(),
          "Should return ExecutionStatus");
      assertFalse(method.isDefault(), "getStatus should be abstract");
    }

    @Test
    @DisplayName("should have getInstructionPointer method")
    void shouldHaveGetInstructionPointerMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getInstructionPointer");
      assertNotNull(method, "getInstructionPointer method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getInstructionPointer should be abstract");
    }

    @Test
    @DisplayName("should have getStackPointer method")
    void shouldHaveGetStackPointerMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStackPointer");
      assertNotNull(method, "getStackPointer method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getStackPointer should be abstract");
    }

    @Test
    @DisplayName("should have getFramePointer method")
    void shouldHaveGetFramePointerMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getFramePointer");
      assertNotNull(method, "getFramePointer method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getFramePointer should be abstract");
    }

    @Test
    @DisplayName("should have getCurrentFunction method")
    void shouldHaveGetCurrentFunctionMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getCurrentFunction");
      assertNotNull(method, "getCurrentFunction method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
      assertFalse(method.isDefault(), "getCurrentFunction should be abstract");
    }

    @Test
    @DisplayName("should have getContext method")
    void shouldHaveGetContextMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getContext");
      assertNotNull(method, "getContext method should exist");
      assertEquals(
          ai.tegmentum.wasmtime4j.execution.ExecutionContext.class,
          method.getReturnType(),
          "Should return ExecutionContext");
      assertFalse(method.isDefault(), "getContext should be abstract");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ExecutionState.ExecutionStatistics.class,
          method.getReturnType(),
          "Should return ExecutionStatistics");
      assertFalse(method.isDefault(), "getStatistics should be abstract");
    }

    @Test
    @DisplayName("should have getStackDepth method")
    void shouldHaveGetStackDepthMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStackDepth");
      assertNotNull(method, "getStackDepth method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
      assertFalse(method.isDefault(), "getStackDepth should be abstract");
    }

    @Test
    @DisplayName("should have getMemoryUsage method")
    void shouldHaveGetMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getMemoryUsage");
      assertNotNull(method, "getMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getMemoryUsage should be abstract");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getStartTime should be abstract");
    }

    @Test
    @DisplayName("should have getElapsedTime method")
    void shouldHaveGetElapsedTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getElapsedTime");
      assertNotNull(method, "getElapsedTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
      assertFalse(method.isDefault(), "getElapsedTime should be abstract");
    }

    @Test
    @DisplayName("should have getLastError method")
    void shouldHaveGetLastErrorMethod() throws NoSuchMethodException {
      Method method = ExecutionState.class.getMethod("getLastError");
      assertNotNull(method, "getLastError method should exist");
      assertEquals(Throwable.class, method.getReturnType(), "Should return Throwable");
      assertFalse(method.isDefault(), "getLastError should be abstract");
    }

    @Test
    @DisplayName("should have exactly 12 abstract methods")
    void shouldHaveExactly12AbstractMethods() {
      long abstractMethods =
          Arrays.stream(ExecutionState.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> !m.isDefault())
              .filter(m -> Modifier.isAbstract(m.getModifiers()))
              .count();
      assertEquals(12, abstractMethods, "ExecutionState should have exactly 12 abstract methods");
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
              "getStatus",
              "getInstructionPointer",
              "getStackPointer",
              "getFramePointer",
              "getCurrentFunction",
              "getContext",
              "getStatistics",
              "getStackDepth",
              "getMemoryUsage",
              "getStartTime",
              "getElapsedTime",
              "getLastError");

      Set<String> actualMethods =
          Arrays.stream(ExecutionState.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected), "ExecutionState should have method: " + expected);
      }
    }

    @Test
    @DisplayName("should have exactly 12 declared methods")
    void shouldHaveExactly12DeclaredMethods() {
      long methodCount =
          Arrays.stream(ExecutionState.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(12, methodCount, "ExecutionState should have exactly 12 declared methods");
    }
  }

  // ========================================================================
  // Default Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Default Method Tests")
  class DefaultMethodTests {

    @Test
    @DisplayName("should have no default methods")
    void shouldHaveNoDefaultMethods() {
      long defaultMethods =
          Arrays.stream(ExecutionState.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(Method::isDefault)
              .count();
      assertEquals(0, defaultMethods, "ExecutionState should have no default methods");
    }
  }

  // ========================================================================
  // Static Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Static Method Tests")
  class StaticMethodTests {

    @Test
    @DisplayName("should have no static methods")
    void shouldHaveNoStaticMethods() {
      long staticMethods =
          Arrays.stream(ExecutionState.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .filter(m -> Modifier.isStatic(m.getModifiers()))
              .count();
      assertEquals(0, staticMethods, "ExecutionState should have no static methods");
    }
  }

  // ========================================================================
  // Field Tests
  // ========================================================================

  @Nested
  @DisplayName("Field Tests")
  class FieldTests {

    @Test
    @DisplayName("should have no declared fields")
    void shouldHaveNoDeclaredFields() {
      assertEquals(
          0,
          ExecutionState.class.getDeclaredFields().length,
          "ExecutionState should have no declared fields");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("should have ExecutionStatistics nested interface")
    void shouldHaveExecutionStatisticsNestedInterface() {
      Class<?>[] declaredClasses = ExecutionState.class.getDeclaredClasses();
      assertEquals(1, declaredClasses.length, "ExecutionState should have 1 nested class");
      assertEquals(
          "ExecutionStatistics",
          declaredClasses[0].getSimpleName(),
          "Nested interface should be ExecutionStatistics");
      assertTrue(declaredClasses[0].isInterface(), "ExecutionStatistics should be an interface");
    }

    @Test
    @DisplayName("ExecutionStatistics should be public")
    void executionStatisticsShouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionState.ExecutionStatistics.class.getModifiers()),
          "ExecutionStatistics should be public");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getInstructionsExecuted method")
    void executionStatisticsShouldHaveGetInstructionsExecutedMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ExecutionStatistics.class.getMethod("getInstructionsExecuted");
      assertNotNull(method, "getInstructionsExecuted method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getFunctionCalls method")
    void executionStatisticsShouldHaveGetFunctionCallsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ExecutionStatistics.class.getMethod("getFunctionCalls");
      assertNotNull(method, "getFunctionCalls method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getMemoryAllocations method")
    void executionStatisticsShouldHaveGetMemoryAllocationsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ExecutionStatistics.class.getMethod("getMemoryAllocations");
      assertNotNull(method, "getMemoryAllocations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getIoOperations method")
    void executionStatisticsShouldHaveGetIoOperationsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ExecutionStatistics.class.getMethod("getIoOperations");
      assertNotNull(method, "getIoOperations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getSystemCalls method")
    void executionStatisticsShouldHaveGetSystemCallsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ExecutionStatistics.class.getMethod("getSystemCalls");
      assertNotNull(method, "getSystemCalls method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getExceptions method")
    void executionStatisticsShouldHaveGetExceptionsMethod() throws NoSuchMethodException {
      Method method = ExecutionState.ExecutionStatistics.class.getMethod("getExceptions");
      assertNotNull(method, "getExceptions method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have exactly 6 declared methods")
    void executionStatisticsShouldHave6DeclaredMethods() {
      long methodCount =
          Arrays.stream(ExecutionState.ExecutionStatistics.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .count();
      assertEquals(6, methodCount, "ExecutionStatistics should have exactly 6 declared methods");
    }

    @Test
    @DisplayName("ExecutionStatistics should have all expected methods")
    void executionStatisticsShouldHaveAllExpectedMethods() {
      Set<String> expectedMethods =
          Set.of(
              "getInstructionsExecuted",
              "getFunctionCalls",
              "getMemoryAllocations",
              "getIoOperations",
              "getSystemCalls",
              "getExceptions");

      Set<String> actualMethods =
          Arrays.stream(ExecutionState.ExecutionStatistics.class.getDeclaredMethods())
              .filter(m -> !m.isSynthetic())
              .map(Method::getName)
              .collect(Collectors.toSet());

      for (String expected : expectedMethods) {
        assertTrue(
            actualMethods.contains(expected),
            "ExecutionStatistics should have method: " + expected);
      }
    }
  }

  // ========================================================================
  // Method Parameter Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Parameter Tests")
  class MethodParameterTests {

    @Test
    @DisplayName("all methods should have no parameters")
    void allMethodsShouldHaveNoParameters() {
      Arrays.stream(ExecutionState.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertEquals(
                      0,
                      m.getParameterCount(),
                      "Method " + m.getName() + " should have no parameters"));
    }
  }

  // ========================================================================
  // Method Visibility Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Visibility Tests")
  class MethodVisibilityTests {

    @Test
    @DisplayName("all methods should be public")
    void allMethodsShouldBePublic() {
      Arrays.stream(ExecutionState.class.getDeclaredMethods())
          .filter(m -> !m.isSynthetic())
          .forEach(
              m ->
                  assertTrue(
                      Modifier.isPublic(m.getModifiers()),
                      "Method " + m.getName() + " should be public"));
    }
  }

  // ========================================================================
  // Return Type Tests
  // ========================================================================

  @Nested
  @DisplayName("Return Type Tests")
  class ReturnTypeTests {

    @Test
    @DisplayName("pointer methods should return long")
    void pointerMethodsShouldReturnLong() throws NoSuchMethodException {
      assertEquals(
          long.class,
          ExecutionState.class.getMethod("getInstructionPointer").getReturnType(),
          "getInstructionPointer should return long");
      assertEquals(
          long.class,
          ExecutionState.class.getMethod("getStackPointer").getReturnType(),
          "getStackPointer should return long");
      assertEquals(
          long.class,
          ExecutionState.class.getMethod("getFramePointer").getReturnType(),
          "getFramePointer should return long");
    }

    @Test
    @DisplayName("time methods should return long")
    void timeMethodsShouldReturnLong() throws NoSuchMethodException {
      assertEquals(
          long.class,
          ExecutionState.class.getMethod("getStartTime").getReturnType(),
          "getStartTime should return long");
      assertEquals(
          long.class,
          ExecutionState.class.getMethod("getElapsedTime").getReturnType(),
          "getElapsedTime should return long");
    }

    @Test
    @DisplayName("getStackDepth should return int")
    void getStackDepthShouldReturnInt() throws NoSuchMethodException {
      assertEquals(
          int.class,
          ExecutionState.class.getMethod("getStackDepth").getReturnType(),
          "getStackDepth should return int");
    }
  }
}
