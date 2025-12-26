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
 * Tests for the ExecutionTraceData interface.
 *
 * <p>This test class verifies the interface structure, methods, nested types, and enums for
 * ExecutionTraceData using reflection-based testing.
 */
@DisplayName("ExecutionTraceData Tests")
class ExecutionTraceDataTest {

  // ========================================================================
  // Interface Structure Tests
  // ========================================================================

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("ExecutionTraceData should be an interface")
    void shouldBeAnInterface() {
      assertTrue(
          ExecutionTraceData.class.isInterface(), "ExecutionTraceData should be an interface");
    }

    @Test
    @DisplayName("ExecutionTraceData should be a public interface")
    void shouldBePublic() {
      assertTrue(
          Modifier.isPublic(ExecutionTraceData.class.getModifiers()),
          "ExecutionTraceData should be public");
    }

    @Test
    @DisplayName("ExecutionTraceData should not extend other interfaces")
    void shouldNotExtendOtherInterfaces() {
      Class<?>[] interfaces = ExecutionTraceData.class.getInterfaces();
      assertEquals(0, interfaces.length, "ExecutionTraceData should not extend other interfaces");
    }
  }

  // ========================================================================
  // Method Tests
  // ========================================================================

  @Nested
  @DisplayName("Method Tests")
  class MethodTests {

    @Test
    @DisplayName("should have getTraceId method")
    void shouldHaveGetTraceIdMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getTraceId");
      assertNotNull(method, "getTraceId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getExecutionId method")
    void shouldHaveGetExecutionIdMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getExecutionId");
      assertNotNull(method, "getExecutionId method should exist");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getEndTime method")
    void shouldHaveGetEndTimeMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getEndTime");
      assertNotNull(method, "getEndTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getDuration");
      assertNotNull(method, "getDuration method should exist");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("should have getEvents method")
    void shouldHaveGetEventsMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getEvents");
      assertNotNull(method, "getEvents method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have getMetadata method")
    void shouldHaveGetMetadataMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getMetadata");
      assertNotNull(method, "getMetadata method should exist");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ExecutionTraceData.TraceStatistics.class,
          method.getReturnType(),
          "Return type should be TraceStatistics");
    }

    @Test
    @DisplayName("should have filter method")
    void shouldHaveFilterMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("filter", TraceFilter.class);
      assertNotNull(method, "filter method should exist");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }

    @Test
    @DisplayName("should have export method")
    void shouldHaveExportMethod() throws NoSuchMethodException {
      Method method =
          ExecutionTraceData.class.getMethod("export", ExecutionTraceData.ExportFormat.class);
      assertNotNull(method, "export method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Return type should be byte[]");
    }

    @Test
    @DisplayName("should have getSamplingConfig method")
    void shouldHaveGetSamplingConfigMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getSamplingConfig");
      assertNotNull(method, "getSamplingConfig method should exist");
      assertEquals(
          ExecutionTraceData.SamplingConfig.class,
          method.getReturnType(),
          "Return type should be SamplingConfig");
    }

    @Test
    @DisplayName("should have getCompressionInfo method")
    void shouldHaveGetCompressionInfoMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.class.getMethod("getCompressionInfo");
      assertNotNull(method, "getCompressionInfo method should exist");
      assertEquals(
          ExecutionTraceData.CompressionInfo.class,
          method.getReturnType(),
          "Return type should be CompressionInfo");
    }
  }

  // ========================================================================
  // EventType Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("EventType Enum Tests")
  class EventTypeTests {

    @Test
    @DisplayName("EventType should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(ExecutionTraceData.EventType.class.isEnum(), "EventType should be an enum");
      assertTrue(
          ExecutionTraceData.EventType.class.isMemberClass(), "EventType should be a member class");
    }

    @Test
    @DisplayName("EventType should have 8 values")
    void shouldHaveEightValues() {
      ExecutionTraceData.EventType[] values = ExecutionTraceData.EventType.values();
      assertEquals(8, values.length, "EventType should have 8 values");
    }

    @Test
    @DisplayName("EventType should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames =
          Set.of(
              "FUNCTION_ENTRY",
              "FUNCTION_EXIT",
              "MEMORY_ALLOCATION",
              "MEMORY_DEALLOCATION",
              "INSTRUCTION_EXECUTION",
              "EXCEPTION_THROWN",
              "EXCEPTION_HANDLED",
              "CUSTOM");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionTraceData.EventType type : ExecutionTraceData.EventType.values()) {
        actualNames.add(type.name());
      }
      assertEquals(expectedNames, actualNames, "EventType should have expected values");
    }
  }

  // ========================================================================
  // ExportFormat Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("ExportFormat Enum Tests")
  class ExportFormatTests {

    @Test
    @DisplayName("ExportFormat should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(ExecutionTraceData.ExportFormat.class.isEnum(), "ExportFormat should be an enum");
    }

    @Test
    @DisplayName("ExportFormat should have 4 values")
    void shouldHaveFourValues() {
      ExecutionTraceData.ExportFormat[] values = ExecutionTraceData.ExportFormat.values();
      assertEquals(4, values.length, "ExportFormat should have 4 values");
    }

    @Test
    @DisplayName("ExportFormat should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("JSON", "BINARY", "CHROME_TRACE", "FLAME_GRAPH");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionTraceData.ExportFormat format : ExecutionTraceData.ExportFormat.values()) {
        actualNames.add(format.name());
      }
      assertEquals(expectedNames, actualNames, "ExportFormat should have expected values");
    }
  }

  // ========================================================================
  // SamplingStrategy Enum Tests
  // ========================================================================

  @Nested
  @DisplayName("SamplingStrategy Enum Tests")
  class SamplingStrategyTests {

    @Test
    @DisplayName("SamplingStrategy should be a nested enum")
    void shouldBeANestedEnum() {
      assertTrue(
          ExecutionTraceData.SamplingStrategy.class.isEnum(), "SamplingStrategy should be an enum");
    }

    @Test
    @DisplayName("SamplingStrategy should have 4 values")
    void shouldHaveFourValues() {
      ExecutionTraceData.SamplingStrategy[] values = ExecutionTraceData.SamplingStrategy.values();
      assertEquals(4, values.length, "SamplingStrategy should have 4 values");
    }

    @Test
    @DisplayName("SamplingStrategy should have expected values")
    void shouldHaveExpectedValues() {
      Set<String> expectedNames = Set.of("RANDOM", "SYSTEMATIC", "STRATIFIED", "ADAPTIVE");
      Set<String> actualNames = new HashSet<>();
      for (ExecutionTraceData.SamplingStrategy strategy :
          ExecutionTraceData.SamplingStrategy.values()) {
        actualNames.add(strategy.name());
      }
      assertEquals(expectedNames, actualNames, "SamplingStrategy should have expected values");
    }
  }

  // ========================================================================
  // Nested Interface Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Interface Tests")
  class NestedInterfaceTests {

    @Test
    @DisplayName("TraceEvent should be a nested interface")
    void traceEventShouldBeNestedInterface() {
      assertTrue(
          ExecutionTraceData.TraceEvent.class.isInterface(), "TraceEvent should be an interface");
      assertTrue(
          ExecutionTraceData.TraceEvent.class.isMemberClass(),
          "TraceEvent should be a member class");
    }

    @Test
    @DisplayName("TraceStatistics should be a nested interface")
    void traceStatisticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionTraceData.TraceStatistics.class.isInterface(),
          "TraceStatistics should be an interface");
    }

    @Test
    @DisplayName("SamplingConfig should be a nested interface")
    void samplingConfigShouldBeNestedInterface() {
      assertTrue(
          ExecutionTraceData.SamplingConfig.class.isInterface(),
          "SamplingConfig should be an interface");
    }

    @Test
    @DisplayName("CompressionInfo should be a nested interface")
    void compressionInfoShouldBeNestedInterface() {
      assertTrue(
          ExecutionTraceData.CompressionInfo.class.isInterface(),
          "CompressionInfo should be an interface");
    }

    @Test
    @DisplayName("FunctionCallStatistics should be a nested interface")
    void functionCallStatisticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionTraceData.FunctionCallStatistics.class.isInterface(),
          "FunctionCallStatistics should be an interface");
    }

    @Test
    @DisplayName("StackFrame should be a nested interface")
    void stackFrameShouldBeNestedInterface() {
      assertTrue(
          ExecutionTraceData.StackFrame.class.isInterface(), "StackFrame should be an interface");
    }
  }

  // ========================================================================
  // TraceEvent Interface Method Tests
  // ========================================================================

  @Nested
  @DisplayName("TraceEvent Interface Method Tests")
  class TraceEventMethodTests {

    @Test
    @DisplayName("TraceEvent should have getId method")
    void shouldHaveGetIdMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getId");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TraceEvent should have getType method")
    void shouldHaveGetTypeMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getType");
      assertEquals(
          ExecutionTraceData.EventType.class,
          method.getReturnType(),
          "Return type should be EventType");
    }

    @Test
    @DisplayName("TraceEvent should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getTimestamp");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("TraceEvent should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getFunctionName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TraceEvent should have getModuleName method")
    void shouldHaveGetModuleNameMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getModuleName");
      assertEquals(String.class, method.getReturnType(), "Return type should be String");
    }

    @Test
    @DisplayName("TraceEvent should have getThreadId method")
    void shouldHaveGetThreadIdMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getThreadId");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("TraceEvent should have getData method")
    void shouldHaveGetDataMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getData");
      assertEquals(Map.class, method.getReturnType(), "Return type should be Map");
    }

    @Test
    @DisplayName("TraceEvent should have getDuration method")
    void shouldHaveGetDurationMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getDuration");
      assertEquals(long.class, method.getReturnType(), "Return type should be long");
    }

    @Test
    @DisplayName("TraceEvent should have getStackTrace method")
    void shouldHaveGetStackTraceMethod() throws NoSuchMethodException {
      Method method = ExecutionTraceData.TraceEvent.class.getMethod("getStackTrace");
      assertEquals(List.class, method.getReturnType(), "Return type should be List");
    }
  }

  // ========================================================================
  // Nested Type Count Tests
  // ========================================================================

  @Nested
  @DisplayName("Nested Type Count Tests")
  class NestedTypeCountTests {

    @Test
    @DisplayName("ExecutionTraceData should have 3 nested enums")
    void shouldHaveThreeNestedEnums() {
      Class<?>[] nestedClasses = ExecutionTraceData.class.getDeclaredClasses();
      int enumCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isEnum()) {
          enumCount++;
        }
      }
      assertEquals(3, enumCount, "ExecutionTraceData should have 3 nested enums");
    }

    @Test
    @DisplayName("ExecutionTraceData should have 6 nested interfaces")
    void shouldHaveSixNestedInterfaces() {
      Class<?>[] nestedClasses = ExecutionTraceData.class.getDeclaredClasses();
      int interfaceCount = 0;
      for (Class<?> nested : nestedClasses) {
        if (nested.isInterface() && !nested.isAnnotation()) {
          interfaceCount++;
        }
      }
      assertEquals(6, interfaceCount, "ExecutionTraceData should have 6 nested interfaces");
    }
  }
}
