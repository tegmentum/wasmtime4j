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

package ai.tegmentum.wasmtime4j.execution;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionResult interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ExecutionResult using reflection-based testing.
 */
@DisplayName("ExecutionResult Tests")
class ExecutionResultTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionResult should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionResult.class.isInterface(), "ExecutionResult should be an interface");
    }

    @Test
    @DisplayName("ExecutionResult should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionResult.class.getModifiers()),
          "ExecutionResult should be public");
    }

    @Test
    @DisplayName("ExecutionResult should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionResult.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionResult should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getRequestId method")
    void shouldHaveGetRequestIdMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getRequestId");
      assertNotNull(method, "getRequestId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(
          ExecutionStatus.class, method.getReturnType(), "Return type should be ExecutionStatus");
    }

    @Test
    @DisplayName("should have getResult method")
    void shouldHaveGetResultMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getResult");
      assertNotNull(method, "getResult method should exist");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("should have getError method")
    void shouldHaveGetErrorMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getError");
      assertNotNull(method, "getError method should exist");
      assertEquals(Throwable.class, method.getReturnType(), "Return type should be Throwable");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ExecutionStatistics.class,
          method.getReturnType(),
          "Return type should be ExecutionStatistics");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getPeakMemoryUsage method")
    void shouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getPeakMemoryUsage");
      assertNotNull(method, "getPeakMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getFinalMemoryUsage method")
    void shouldHaveGetFinalMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getFinalMemoryUsage");
      assertNotNull(method, "getFinalMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getWarnings method")
    void shouldHaveGetWarningsMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getWarnings");
      assertNotNull(method, "getWarnings method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have isSuccessful method")
    void shouldHaveIsSuccessfulMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("isSuccessful");
      assertNotNull(method, "isSuccessful method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have isTerminated method")
    void shouldHaveIsTerminatedMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("isTerminated");
      assertNotNull(method, "isTerminated method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getTerminationReason method")
    void shouldHaveGetTerminationReasonMethod() throws NoSuchMethodException {
      Method method = ExecutionResult.class.getMethod("getTerminationReason");
      assertNotNull(method, "getTerminationReason method should exist");
      assertEquals(
          ExecutionResult.TerminationReason.class,
          method.getReturnType(),
          "Return type should be TerminationReason");
    }
  }

  // ========================================================================
  // TerminationReason Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("TerminationReason Enum Tests")
  class TerminationReasonTests {

    @Test
    @DisplayName("TerminationReason should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionResult.TerminationReason.class.isEnum(), "TerminationReason should be an enum");
      assertTrue(
          ExecutionResult.TerminationReason.class.isMemberClass(),
          "TerminationReason should be a member class");
    }

    @Test
    @DisplayName("TerminationReason should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionResult.TerminationReason.class.getModifiers()),
          "TerminationReason should be public");
    }

    @Test
    @DisplayName("TerminationReason should have 8 values")
    void shouldHaveEightValues() {
      ExecutionResult.TerminationReason[] values = ExecutionResult.TerminationReason.values();
      assertEquals(8, values.length, "TerminationReason should have 8 values");
    }

    @Test
    @DisplayName("TerminationReason should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "COMPLETED",
              "TIMEOUT",
              "MEMORY_LIMIT",
              "FUEL_LIMIT",
              "INSTRUCTION_LIMIT",
              "USER_REQUESTED",
              "SYSTEM_ERROR",
              "SECURITY_VIOLATION");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionResult.TerminationReason reason : ExecutionResult.TerminationReason.values()) {
        actualNames.add(reason.name());
      }
      assertEquals(expectedNames, actualNames, "TerminationReason should have expected values");
    }

    @Test
    @DisplayName("COMPLETED should have ordinal 0")
    void completedShouldHaveOrdinalZero() {
      assertEquals(
          0,
          ExecutionResult.TerminationReason.COMPLETED.ordinal(),
          "COMPLETED should have ordinal 0");
    }

    @Test
    @DisplayName("All TerminationReason values should have unique ordinals")
    void allValuesShouldHaveUniqueOrdinals() {
      ExecutionResult.TerminationReason[] values = ExecutionResult.TerminationReason.values();
      Set<Integer> ordinals = new HashSet<>();
      for (ExecutionResult.TerminationReason reason : values) {
        ordinals.add(reason.ordinal());
      }
      assertEquals(values.length, ordinals.size(), "All values should have unique ordinals");
    }
  }

  // ========================================================================
  // Method Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ExecutionResult should have at least 14 methods")
    void shouldHaveAtLeastFourteenMethods() {
      Method[] methods = ExecutionResult.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 14,
          "ExecutionResult should have at least 14 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("All methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ExecutionResult.class.getDeclaredMethods();
      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            method.getName() + " should be public and abstract");
      }
    }
  }
}
