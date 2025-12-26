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
import java.util.Set;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for the ExecutionPolicy interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested enums for
 * ExecutionPolicy using reflection-based testing.
 */
@DisplayName("ExecutionPolicy Tests")
class ExecutionPolicyTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionPolicy should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionPolicy.class.isInterface(), "ExecutionPolicy should be an interface");
    }

    @Test
    @DisplayName("ExecutionPolicy should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionPolicy.class.getModifiers()),
          "ExecutionPolicy should be public");
    }

    @Test
    @DisplayName("ExecutionPolicy should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionPolicy.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionPolicy should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getPolicyName method")
    void shouldHaveGetPolicyNameMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getPolicyName");
      assertNotNull(method, "getPolicyName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have isAllowed method")
    void shouldHaveIsAllowedMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("isAllowed", String.class);
      assertNotNull(method, "isAllowed method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
      assertEquals(1, method.getParameterCount(), "isAllowed should have 1 parameter");
    }

    @Test
    @DisplayName("should have getMaxExecutionTime method")
    void shouldHaveGetMaxExecutionTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getMaxExecutionTime");
      assertNotNull(method, "getMaxExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMaxMemoryUsage method")
    void shouldHaveGetMaxMemoryUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getMaxMemoryUsage");
      assertNotNull(method, "getMaxMemoryUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getMaxCpuUsage method")
    void shouldHaveGetMaxCpuUsageMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getMaxCpuUsage");
      assertNotNull(method, "getMaxCpuUsage method should exist");
      assertEquals(double.class, method.getReturnType(), "Return type should be double");
    }

    @Test
    @DisplayName("should have getRestrictedOperations method")
    void shouldHaveGetRestrictedOperationsMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getRestrictedOperations");
      assertNotNull(method, "getRestrictedOperations method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have getAllowedOperations method")
    void shouldHaveGetAllowedOperationsMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getAllowedOperations");
      assertNotNull(method, "getAllowedOperations method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have getEnforcementLevel method")
    void shouldHaveGetEnforcementLevelMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getEnforcementLevel");
      assertNotNull(method, "getEnforcementLevel method should exist");
      assertEquals(
          ExecutionPolicy.EnforcementLevel.class,
          method.getReturnType(),
          "Return type should be EnforcementLevel");
    }

    @Test
    @DisplayName("should have getViolationHandling method")
    void shouldHaveGetViolationHandlingMethod() throws NoSuchMethodException {
      Method method = ExecutionPolicy.class.getMethod("getViolationHandling");
      assertNotNull(method, "getViolationHandling method should exist");
      assertEquals(
          ExecutionPolicy.ViolationHandling.class,
          method.getReturnType(),
          "Return type should be ViolationHandling");
    }
  }

  // ========================================================================
  // EnforcementLevel Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("EnforcementLevel Enum Tests")
  class EnforcementLevelTests {

    @Test
    @DisplayName("EnforcementLevel should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionPolicy.EnforcementLevel.class.isEnum(), "EnforcementLevel should be an enum");
      assertTrue(
          ExecutionPolicy.EnforcementLevel.class.isMemberClass(),
          "EnforcementLevel should be a member class");
    }

    @Test
    @DisplayName("EnforcementLevel should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionPolicy.EnforcementLevel.class.getModifiers()),
          "EnforcementLevel should be public");
    }

    @Test
    @DisplayName("EnforcementLevel should have 4 values")
    void shouldHaveFourValues() {
      ExecutionPolicy.EnforcementLevel[] values = ExecutionPolicy.EnforcementLevel.values();
      assertEquals(4, values.length, "EnforcementLevel should have 4 values");
    }

    @Test
    @DisplayName("EnforcementLevel should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("ADVISORY", "WARNING", "BLOCKING", "STRICT");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionPolicy.EnforcementLevel level : ExecutionPolicy.EnforcementLevel.values()) {
        actualNames.add(level.name());
      }
      assertEquals(expectedNames, actualNames, "EnforcementLevel should have expected values");
    }

    @Test
    @DisplayName("ADVISORY should have ordinal 0")
    void advisoryShouldHaveOrdinalZero() {
      assertEquals(
          0, ExecutionPolicy.EnforcementLevel.ADVISORY.ordinal(), "ADVISORY should have ordinal 0");
    }

    @Test
    @DisplayName("STRICT should have ordinal 3")
    void strictShouldHaveOrdinalThree() {
      assertEquals(
          3, ExecutionPolicy.EnforcementLevel.STRICT.ordinal(), "STRICT should have ordinal 3");
    }

    @Test
    @DisplayName("EnforcementLevel values should be comparable")
    void valuesShouldBeComparable() {
      assertTrue(
          ExecutionPolicy.EnforcementLevel.ADVISORY.compareTo(
                  ExecutionPolicy.EnforcementLevel.STRICT)
              < 0,
          "ADVISORY should come before STRICT");
    }
  }

  // ========================================================================
  // ViolationHandling Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ViolationHandling Enum Tests")
  class ViolationHandlingTests {

    @Test
    @DisplayName("ViolationHandling should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionPolicy.ViolationHandling.class.isEnum(), "ViolationHandling should be an enum");
      assertTrue(
          ExecutionPolicy.ViolationHandling.class.isMemberClass(),
          "ViolationHandling should be a member class");
    }

    @Test
    @DisplayName("ViolationHandling should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionPolicy.ViolationHandling.class.getModifiers()),
          "ViolationHandling should be public");
    }

    @Test
    @DisplayName("ViolationHandling should have 4 values")
    void shouldHaveFourValues() {
      ExecutionPolicy.ViolationHandling[] values = ExecutionPolicy.ViolationHandling.values();
      assertEquals(4, values.length, "ViolationHandling should have 4 values");
    }

    @Test
    @DisplayName("ViolationHandling should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("IGNORE", "LOG", "EXCEPTION", "TERMINATE");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionPolicy.ViolationHandling handling :
          ExecutionPolicy.ViolationHandling.values()) {
        actualNames.add(handling.name());
      }
      assertEquals(expectedNames, actualNames, "ViolationHandling should have expected values");
    }

    @Test
    @DisplayName("IGNORE should have ordinal 0")
    void ignoreShouldHaveOrdinalZero() {
      assertEquals(
          0, ExecutionPolicy.ViolationHandling.IGNORE.ordinal(), "IGNORE should have ordinal 0");
    }

    @Test
    @DisplayName("TERMINATE should have ordinal 3")
    void terminateShouldHaveOrdinalThree() {
      assertEquals(
          3,
          ExecutionPolicy.ViolationHandling.TERMINATE.ordinal(),
          "TERMINATE should have ordinal 3");
    }

    @Test
    @DisplayName("ViolationHandling values should be comparable")
    void valuesShouldBeComparable() {
      assertTrue(
          ExecutionPolicy.ViolationHandling.IGNORE.compareTo(
                  ExecutionPolicy.ViolationHandling.TERMINATE)
              < 0,
          "IGNORE should come before TERMINATE");
    }
  }

  // ========================================================================
  // Method Count and Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Count Tests")
  class MethodCountTests {

    @Test
    @DisplayName("ExecutionPolicy should have at least 9 methods")
    void shouldHaveAtLeastNineMethods() {
      Method[] methods = ExecutionPolicy.class.getDeclaredMethods();
      assertTrue(
          methods.length >= 9,
          "ExecutionPolicy should have at least 9 methods, found: " + methods.length);
    }

    @Test
    @DisplayName("All methods should be public and abstract")
    void allMethodsShouldBePublicAbstract() {
      Method[] methods = ExecutionPolicy.class.getDeclaredMethods();
      for (Method method : methods) {
        int modifiers = method.getModifiers();
        assertTrue(
            Modifier.isPublic(modifiers) && Modifier.isAbstract(modifiers),
            method.getName() + " should be public and abstract");
      }
    }

    @Test
    @DisplayName("ExecutionPolicy should have 2 nested enums")
    void shouldHaveTwoNestedEnums() {
      Class<?>[] nestedClasses = ExecutionPolicy.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(2, enumCount, "ExecutionPolicy should have 2 nested enums");
    }
  }
}
