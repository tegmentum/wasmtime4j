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
 * Tests for the TraceFilter interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for TraceFilter
 * using reflection-based testing.
 */
@DisplayName("TraceFilter Tests")
class TraceFilterTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("TraceFilter should be an interface")
    void shouldBeAnInterface() {
      assertTrue(TraceFilter.class.isInterface(), "TraceFilter should be an interface");
    }

    @Test
    @DisplayName("TraceFilter should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(TraceFilter.class.getModifiers()), "TraceFilter should be public");
    }

    @Test
    @DisplayName("TraceFilter should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = TraceFilter.class.getInterfaces();
      assertEquals(0, interfaces.length, "TraceFilter should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getName method")
    void shouldHaveGetNameMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getName");
      assertNotNull(method, "getName method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getType");
      assertNotNull(method, "getType method should exist");
      assertEquals(
          TraceFilter.FilterType.class, method.getReturnType(), "Return type should be FilterType");
    }

    @Test
    @DisplayName("should have getPattern method")
    void shouldHaveGetPatternMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getPattern");
      assertNotNull(method, "getPattern method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have setPattern method")
    void shouldHaveSetPatternMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("setPattern", String.class);
      assertNotNull(method, "setPattern method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getAction method")
    void shouldHaveGetActionMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getAction");
      assertNotNull(method, "getAction method should exist");
      assertEquals(
          TraceFilter.FilterAction.class,
          method.getReturnType(),
          "Return type should be FilterAction");
    }

    @Test
    @DisplayName("should have setAction method")
    void shouldHaveSetActionMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("setAction", TraceFilter.FilterAction.class);
      assertNotNull(method, "setAction method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have matches method")
    void shouldHaveMatchesMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("matches", TraceFilter.TraceEvent.class);
      assertNotNull(method, "matches method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have getPriority method")
    void shouldHaveGetPriorityMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getPriority");
      assertNotNull(method, "getPriority method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have setPriority method")
    void shouldHaveSetPriorityMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("setPriority", int.class);
      assertNotNull(method, "setPriority method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getConditions method")
    void shouldHaveGetConditionsMethod() throws NoSuchMethodException {
      Method method = TraceFilter.class.getMethod("getConditions");
      assertNotNull(method, "getConditions method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have addCondition method")
    void shouldHaveAddConditionMethod() throws NoSuchMethodException {
      Method method =
          TraceFilter.class.getMethod("addCondition", TraceFilter.FilterCondition.class);
      assertNotNull(method, "addCondition method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have removeCondition method")
    void shouldHaveRemoveConditionMethod() throws NoSuchMethodException {
      Method method =
          TraceFilter.class.getMethod("removeCondition", TraceFilter.FilterCondition.class);
      assertNotNull(method, "removeCondition method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // FilterType Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("FilterType Enum Tests")
  class FilterTypeTests {

    @Test
    @DisplayName("FilterType should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(TraceFilter.FilterType.class.isEnum(), "FilterType should be an enum");
      assertTrue(
          TraceFilter.FilterType.class.isMemberClass(), "FilterType should be a member class");
    }

    @Test
    @DisplayName("FilterType should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(TraceFilter.FilterType.class.getModifiers()),
          "FilterType should be public");
    }

    @Test
    @DisplayName("FilterType should have 6 values")
    void shouldHaveSixValues() {
      TraceFilter.FilterType[] values = TraceFilter.FilterType.values();
      assertEquals(6, values.length, "FilterType should have 6 values");
    }

    @Test
    @DisplayName("FilterType should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "FUNCTION_NAME", "MODULE_NAME", "EVENT_TYPE", "DURATION", "MEMORY_USAGE", "CUSTOM");
      Set<String> actualNames = new HashSet<>();
      for (TraceFilter.FilterType type : TraceFilter.FilterType.values()) {
        actualNames.add(type.name());
      }
      assertEquals(expectedNames, actualNames, "FilterType should have expected values");
    }
  }

  // ========================================================================
  // FilterAction Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("FilterAction Enum Tests")
  class FilterActionTests {

    @Test
    @DisplayName("FilterAction should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(TraceFilter.FilterAction.class.isEnum(), "FilterAction should be an enum");
      assertTrue(
          TraceFilter.FilterAction.class.isMemberClass(), "FilterAction should be a member class");
    }

    @Test
    @DisplayName("FilterAction should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(TraceFilter.FilterAction.class.getModifiers()),
          "FilterAction should be public");
    }

    @Test
    @DisplayName("FilterAction should have 4 values")
    void shouldHaveFourValues() {
      TraceFilter.FilterAction[] values = TraceFilter.FilterAction.values();
      assertEquals(4, values.length, "FilterAction should have 4 values");
    }

    @Test
    @DisplayName("FilterAction should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("INCLUDE", "EXCLUDE", "MARK", "TRANSFORM");
      Set<String> actualNames = new HashSet<>();
      for (TraceFilter.FilterAction action : TraceFilter.FilterAction.values()) {
        actualNames.add(action.name());
      }
      assertEquals(expectedNames, actualNames, "FilterAction should have expected values");
    }
  }

  // ========================================================================
  // ConditionOperator Nested Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ConditionOperator Enum Tests")
  class ConditionOperatorTests {

    @Test
    @DisplayName("ConditionOperator should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          TraceFilter.ConditionOperator.class.isEnum(), "ConditionOperator should be an enum");
      assertTrue(
          TraceFilter.ConditionOperator.class.isMemberClass(),
          "ConditionOperator should be a member class");
    }

    @Test
    @DisplayName("ConditionOperator should have 10 values")
    void shouldHaveTenValues() {
      TraceFilter.ConditionOperator[] values = TraceFilter.ConditionOperator.values();
      assertEquals(10, values.length, "ConditionOperator should have 10 values");
    }

    @Test
    @DisplayName("ConditionOperator should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "EQUALS",
              "NOT_EQUALS",
              "GREATER_THAN",
              "LESS_THAN",
              "GREATER_THAN_OR_EQUAL",
              "LESS_THAN_OR_EQUAL",
              "CONTAINS",
              "STARTS_WITH",
              "ENDS_WITH",
              "MATCHES");
      Set<String> actualNames = new HashSet<>();
      for (TraceFilter.ConditionOperator op : TraceFilter.ConditionOperator.values()) {
        actualNames.add(op.name());
      }
      assertEquals(expectedNames, actualNames, "ConditionOperator should have expected values");
    }
  }

  // ========================================================================
  // FilterCondition Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("FilterCondition Interface Tests")
  class FilterConditionTests {

    @Test
    @DisplayName("FilterCondition should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(
          TraceFilter.FilterCondition.class.isInterface(),
          "FilterCondition should be an interface");
      assertTrue(
          TraceFilter.FilterCondition.class.isMemberClass(),
          "FilterCondition should be a member class");
    }

    @Test
    @DisplayName("FilterCondition should have getField method")
    void shouldHaveGetFieldMethod() throws NoSuchMethodException {
      Method method = TraceFilter.FilterCondition.class.getMethod("getField");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("FilterCondition should have getOperator method")
    void shouldHaveGetOperatorMethod() throws NoSuchMethodException {
      Method method = TraceFilter.FilterCondition.class.getMethod("getOperator");
      assertEquals(
          TraceFilter.ConditionOperator.class,
          method.getReturnType(),
          "Return type should be ConditionOperator");
    }

    @Test
    @DisplayName("FilterCondition should have getValue method")
    void shouldHaveGetValueMethod() throws NoSuchMethodException {
      Method method = TraceFilter.FilterCondition.class.getMethod("getValue");
      assertEquals(Object.class, method.getReturnType(), "Return type should be Object");
    }

    @Test
    @DisplayName("FilterCondition should have test method")
    void shouldHaveTestMethod() throws NoSuchMethodException {
      Method method =
          TraceFilter.FilterCondition.class.getMethod("test", TraceFilter.TraceEvent.class);
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }
  }

  // ========================================================================
  // TraceEvent Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("TraceEvent Interface Tests")
  class TraceEventTests {

    @Test
    @DisplayName("TraceEvent should be a nested interface")
    void shouldBeANestedInterface() {
      assertTrue(TraceFilter.TraceEvent.class.isInterface(), "TraceEvent should be an interface");
      assertTrue(
          TraceFilter.TraceEvent.class.isMemberClass(), "TraceEvent should be a member class");
    }

    @Test
    @DisplayName("TraceEvent should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = TraceFilter.TraceEvent.class.getMethod("getType");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TraceEvent should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = TraceFilter.TraceEvent.class.getMethod("getTimestamp");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("TraceEvent should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      Method method = TraceFilter.TraceEvent.class.getMethod("getFunctionName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TraceEvent should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      Method method = TraceFilter.TraceEvent.class.getMethod("getModuleName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TraceEvent should have getProperties method")
    void shouldHaveGetPropertiesMethod() throws NoSuchMethodException {
      Method method = TraceFilter.TraceEvent.class.getMethod("getProperties");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("TraceEvent should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = TraceFilter.TraceEvent.class.getMethod("getDuration");
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
    @DisplayName("TraceFilter should have 3 nested enums")
    void shouldHaveThreeNestedEnums() {
      Class<?>[] nestedClasses = TraceFilter.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(3, enumCount, "TraceFilter should have 3 nested enums");
    }

    @Test
    @DisplayName("TraceFilter should have 2 nested interfaces")
    void shouldHaveTwoNestedInterfaces() {
      Class<?>[] nestedClasses = TraceFilter.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(2, interfaceCount, "TraceFilter should have 2 nested interfaces");
    }
  }
}
