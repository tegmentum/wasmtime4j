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
 * Tests for the ExecutionTracingConfig interface.
 *
 * <p>This test class verifies the interface structure, methods, and nested types for
 * ExecutionTracingConfig using reflection-based testing.
 */
@DisplayName("ExecutionTracingConfig Tests")
class ExecutionTracingConfigTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionTracingConfig should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionTracingConfig.class.isInterface(),
          "ExecutionTracingConfig should be an interface");
    }

    @Test
    @DisplayName("ExecutionTracingConfig should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionTracingConfig.class.getModifiers()),
          "ExecutionTracingConfig should be public");
    }

    @Test
    @DisplayName("ExecutionTracingConfig should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionTracingConfig.class.getInterfaces();
      assertEquals(
          0, interfaces.length, "ExecutionTracingConfig should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have isEnabled method")
    void shouldHaveIsEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("isEnabled");
      assertNotNull(method, "isEnabled method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Return type should be boolean");
    }

    @Test
    @DisplayName("should have setEnabled method")
    void shouldHaveSetEnabledMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("setEnabled", boolean.class);
      assertNotNull(method, "setEnabled method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getOutputDestination method")
    void shouldHaveGetOutputDestinationMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("getOutputDestination");
      assertNotNull(method, "getOutputDestination method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have setOutputDestination method")
    void shouldHaveSetOutputDestinationMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("setOutputDestination", String.class);
      assertNotNull(method, "setOutputDestination method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getBufferSize method")
    void shouldHaveGetBufferSizeMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("getBufferSize");
      assertNotNull(method, "getBufferSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Return type should be int");
    }

    @Test
    @DisplayName("should have setBufferSize method")
    void shouldHaveSetBufferSizeMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("setBufferSize", int.class);
      assertNotNull(method, "setBufferSize method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getFilters method")
    void shouldHaveGetFiltersMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("getFilters");
      assertNotNull(method, "getFilters method should exist");
      assertEquals(Set.class, method.getReturnType(), "Return type should be Set");
    }

    @Test
    @DisplayName("should have addFilter method")
    void shouldHaveAddFilterMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("addFilter", TraceFilter.class);
      assertNotNull(method, "addFilter method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have removeFilter method")
    void shouldHaveRemoveFilterMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("removeFilter", TraceFilter.class);
      assertNotNull(method, "removeFilter method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getFormat method")
    void shouldHaveGetFormatMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("getFormat");
      assertNotNull(method, "getFormat method should exist");
      assertEquals(
          ExecutionTracingConfig.TraceFormat.class,
          method.getReturnType(),
          "Return type should be TraceFormat");
    }

    @Test
    @DisplayName("should have setFormat method")
    void shouldHaveSetFormatMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTracingConfig.class.getMethod(
              "setFormat", ExecutionTracingConfig.TraceFormat.class);
      assertNotNull(method, "setFormat method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }

    @Test
    @DisplayName("should have getFlushInterval method")
    void shouldHaveGetFlushIntervalMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("getFlushInterval");
      assertNotNull(method, "getFlushInterval method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have setFlushInterval method")
    void shouldHaveSetFlushIntervalMethod() throws NoSuchMethodException {
      Method method = ExecutionTracingConfig.class.getMethod("setFlushInterval", long.class);
      assertNotNull(method, "setFlushInterval method should exist");
      assertEquals(void.class, method.getReturnType(), "Return type should be void");
    }
  }

  // ========================================================================
  // TraceFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("TraceFormat Enum Tests")
  class TraceFormatTests {

    @Test
    @DisplayName("TraceFormat should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionTracingConfig.TraceFormat.class.isEnum(), "TraceFormat should be an enum");
      assertTrue(
          ExecutionTracingConfig.TraceFormat.class.isMemberClass(),
          "TraceFormat should be a member class");
    }

    @Test
    @DisplayName("TraceFormat should be public")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionTracingConfig.TraceFormat.class.getModifiers()),
          "TraceFormat should be public");
    }

    @Test
    @DisplayName("TraceFormat should have 4 values")
    void shouldHaveFourValues() {
      ExecutionTracingConfig.TraceFormat[] values = ExecutionTracingConfig.TraceFormat.values();
      assertEquals(4, values.length, "TraceFormat should have 4 values");
    }

    @Test
    @DisplayName("TraceFormat should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("JSON", "BINARY", "TEXT", "CHROME_TRACE");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionTracingConfig.TraceFormat format :
          ExecutionTracingConfig.TraceFormat.values()) {
        actualNames.add(format.name());
      }
      assertEquals(expectedNames, actualNames, "TraceFormat should have expected values");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ExecutionTracingConfig should have 1 nested enum")
    void shouldHaveOneNestedEnum() {
      Class<?>[] nestedClasses = ExecutionTracingConfig.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(1, enumCount, "ExecutionTracingConfig should have 1 nested enum");
    }

    @Test
    @DisplayName("ExecutionTracingConfig should have 0 nested interfaces")
    void shouldHaveZeroNestedInterfaces() {
      Class<?>[] nestedClasses = ExecutionTracingConfig.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(0, interfaceCount, "ExecutionTracingConfig should have 0 nested interfaces");
    }
  }
}
