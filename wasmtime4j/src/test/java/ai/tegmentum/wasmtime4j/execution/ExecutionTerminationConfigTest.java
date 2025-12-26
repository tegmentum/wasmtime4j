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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionTerminationConfig interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ExecutionTerminationConfig using reflection-based testing.
 */
@DisplayName("ExecutionTerminationConfig Tests")
class ExecutionTerminationConfigTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionTerminationConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionTerminationConfig.class.isInterface(),
          "ExecutionTerminationConfig should be an interface");
    }

    @Test
    @DisplayName("ExecutionTerminationConfig should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionTerminationConfig.class.getModifiers()),
          "ExecutionTerminationConfig should be public");
    }

    @Test
    @DisplayName("ExecutionTerminationConfig should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionTerminationConfig.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "ExecutionTerminationConfig should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getTerminationTimeout method")
    void shouldHaveGetTerminationTimeoutMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.class.getMethod("getTerminationTimeout");
      assertNotNull(method, "getTerminationTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setTerminationTimeout method")
    void shouldHaveSetTerminationTimeoutMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTerminationConfig.class.getMethod("setTerminationTimeout", long.class);
      assertNotNull(method, "setTerminationTimeout method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getStrategy method")
    void shouldHaveGetStrategyMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.class.getMethod("getStrategy");
      assertNotNull(method, "getStrategy method should exist");
      assertEquals(
          ExecutionTerminationConfig.TerminationStrategy.class,
          method.getReturnType(),
          "Return type should be TerminationStrategy");
    }

    @Test
    @DisplayName("should have setStrategy method")
    void shouldHaveSetStrategyMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTerminationConfig.class.getMethod(
              "setStrategy", ExecutionTerminationConfig.TerminationStrategy.class);
      assertNotNull(method, "setStrategy method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isForcedTerminationEnabled method")
    void shouldHaveIsForcedTerminationEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.class.getMethod("isForcedTerminationEnabled");
      assertNotNull(method, "isForcedTerminationEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setForcedTerminationEnabled method")
    void shouldHaveSetForcedTerminationEnabledMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTerminationConfig.class.getMethod("setForcedTerminationEnabled", boolean.class);
      assertNotNull(method, "setForcedTerminationEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getCleanupTimeout method")
    void shouldHaveGetCleanupTimeoutMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.class.getMethod("getCleanupTimeout");
      assertNotNull(method, "getCleanupTimeout method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setCleanupTimeout method")
    void shouldHaveSetCleanupTimeoutMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.class.getMethod("setCleanupTimeout", long.class);
      assertNotNull(method, "setCleanupTimeout method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getHandlers method")
    void shouldHaveGetHandlersMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.class.getMethod("getHandlers");
      assertNotNull(method, "getHandlers method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have addHandler method")
    void shouldHaveAddHandlerMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTerminationConfig.class.getMethod(
              "addHandler", ExecutionTerminationConfig.TerminationHandler.class);
      assertNotNull(method, "addHandler method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have removeHandler method")
    void shouldHaveRemoveHandlerMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTerminationConfig.class.getMethod(
              "removeHandler", ExecutionTerminationConfig.TerminationHandler.class);
      assertNotNull(method, "removeHandler method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // TerminationStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("TerminationStrategy Enum Tests")
  class TerminationStrategyTests {

    @Test
    @DisplayName("TerminationStrategy should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionTerminationConfig.TerminationStrategy.class.isEnum(),
          "TerminationStrategy should be an enum");
      assertTrue(
          ExecutionTerminationConfig.TerminationStrategy.class.isMemberClass(),
          "TerminationStrategy should be a member class");
    }

    @Test
    @DisplayName("TerminationStrategy should have 4 values")
    void shouldHaveFourValues() {
      ExecutionTerminationConfig.TerminationStrategy[] values =
          ExecutionTerminationConfig.TerminationStrategy.values();
      assertEquals(4, values.length, "TerminationStrategy should have 4 values");
    }

    @Test
    @DisplayName("TerminationStrategy should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("GRACEFUL", "IMMEDIATE", "PROGRESSIVE", "COOPERATIVE");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionTerminationConfig.TerminationStrategy strategy :
          ExecutionTerminationConfig.TerminationStrategy.values()) {
        actualNames.add(strategy.name());
      }
      assertEquals(expectedNames, actualNames, "TerminationStrategy should have expected values");
    }
  }

  // ========================================================================
  // TerminationHandler Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("TerminationHandler Interface Tests")
  class TerminationHandlerTests {

    @Test
    @DisplayName("TerminationHandler should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionTerminationConfig.TerminationHandler.class.isInterface(),
          "TerminationHandler should be an interface");
      assertTrue(
          ExecutionTerminationConfig.TerminationHandler.class.isMemberClass(),
          "TerminationHandler should be a member class");
    }

    @Test
    @DisplayName("TerminationHandler should have onTermination method")
    void shouldHaveOnTerminationMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTerminationConfig.TerminationHandler.class.getMethod(
              "onTermination", ExecutionTerminationConfig.TerminationContext.class);
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("TerminationHandler should have getPriority method")
    void shouldHaveGetPriorityMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.TerminationHandler.class.getMethod("getPriority");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }
  }

  // ========================================================================
  // TerminationContext Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("TerminationContext Interface Tests")
  class TerminationContextTests {

    @Test
    @DisplayName("TerminationContext should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          ExecutionTerminationConfig.TerminationContext.class.isInterface(),
          "TerminationContext should be an interface");
      assertTrue(
          ExecutionTerminationConfig.TerminationContext.class.isMemberClass(),
          "TerminationContext should be a member class");
    }

    @Test
    @DisplayName("TerminationContext should have getExecutionId method")
    void shouldHaveGetExecutionIdMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTerminationConfig.TerminationContext.class.getMethod("getExecutionId");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TerminationContext should have getReason method")
    void shouldHaveGetReasonMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.TerminationContext.class.getMethod("getReason");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TerminationContext should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = ExecutionTerminationConfig.TerminationContext.class.getMethod("getTimestamp");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ExecutionTerminationConfig should have 1 nested enum")
    void shouldHaveOneNestedEnum() {
      Class<?>[] nestedClasses = ExecutionTerminationConfig.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(1, enumCount, "ExecutionTerminationConfig should have 1 nested enum");
    }

    @Test
    @DisplayName("ExecutionTerminationConfig should have 2 nested interfaces")
    void shouldHaveTwoNestedInterfaces() {
      Class<?>[] nestedClasses = ExecutionTerminationConfig.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(2, interfaceCount, "ExecutionTerminationConfig should have 2 nested interfaces");
    }
  }
}
