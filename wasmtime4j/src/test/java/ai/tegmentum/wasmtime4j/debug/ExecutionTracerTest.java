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

package ai.tegmentum.wasmtime4j.debug;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.ExecutionTracer.TraceEvent;
import ai.tegmentum.wasmtime4j.debug.ExecutionTracer.TraceEventType;
import ai.tegmentum.wasmtime4j.debug.ExecutionTracer.TraceFilter;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExecutionTracer} interface.
 *
 * <p>ExecutionTracer provides execution tracing for WebAssembly debugging.
 */
@DisplayName("ExecutionTracer Tests")
class ExecutionTracerTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionTracer.class.isInterface(), "ExecutionTracer should be an interface");
    }

    @Test
    @DisplayName("should have startTracing method")
    void shouldHaveStartTracingMethod() throws NoSuchMethodException {
      final Method method = ExecutionTracer.class.getMethod("startTracing");
      assertNotNull(method, "startTracing method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have stopTracing method")
    void shouldHaveStopTracingMethod() throws NoSuchMethodException {
      final Method method = ExecutionTracer.class.getMethod("stopTracing");
      assertNotNull(method, "stopTracing method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have getTraceEvents method")
    void shouldHaveGetTraceEventsMethod() throws NoSuchMethodException {
      final Method method = ExecutionTracer.class.getMethod("getTraceEvents");
      assertNotNull(method, "getTraceEvents method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have clearTrace method")
    void shouldHaveClearTraceMethod() throws NoSuchMethodException {
      final Method method = ExecutionTracer.class.getMethod("clearTrace");
      assertNotNull(method, "clearTrace method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }

    @Test
    @DisplayName("should have isTracing method")
    void shouldHaveIsTracingMethod() throws NoSuchMethodException {
      final Method method = ExecutionTracer.class.getMethod("isTracing");
      assertNotNull(method, "isTracing method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have setTraceFilter method")
    void shouldHaveSetTraceFilterMethod() throws NoSuchMethodException {
      final Method method = ExecutionTracer.class.getMethod("setTraceFilter", TraceFilter.class);
      assertNotNull(method, "setTraceFilter method should exist");
      assertEquals(void.class, method.getReturnType(), "Should return void");
    }
  }

  @Nested
  @DisplayName("TraceEventType Enum Tests")
  class TraceEventTypeEnumTests {

    @Test
    @DisplayName("should have FUNCTION_CALL value")
    void shouldHaveFunctionCallValue() {
      assertNotNull(TraceEventType.valueOf("FUNCTION_CALL"), "FUNCTION_CALL should exist");
    }

    @Test
    @DisplayName("should have FUNCTION_RETURN value")
    void shouldHaveFunctionReturnValue() {
      assertNotNull(TraceEventType.valueOf("FUNCTION_RETURN"), "FUNCTION_RETURN should exist");
    }

    @Test
    @DisplayName("should have INSTRUCTION value")
    void shouldHaveInstructionValue() {
      assertNotNull(TraceEventType.valueOf("INSTRUCTION"), "INSTRUCTION should exist");
    }

    @Test
    @DisplayName("should have MEMORY_ACCESS value")
    void shouldHaveMemoryAccessValue() {
      assertNotNull(TraceEventType.valueOf("MEMORY_ACCESS"), "MEMORY_ACCESS should exist");
    }

    @Test
    @DisplayName("should have EXCEPTION value")
    void shouldHaveExceptionValue() {
      assertNotNull(TraceEventType.valueOf("EXCEPTION"), "EXCEPTION should exist");
    }

    @Test
    @DisplayName("should have exactly five values")
    void shouldHaveExactlyFiveValues() {
      assertEquals(
          5, TraceEventType.values().length, "Should have exactly 5 TraceEventType values");
    }
  }

  @Nested
  @DisplayName("TraceEvent Interface Tests")
  class TraceEventInterfaceTests {

    @Test
    @DisplayName("TraceEvent should be a nested interface")
    void traceEventShouldBeNestedInterface() {
      assertTrue(TraceEvent.class.isInterface(), "TraceEvent should be an interface");
    }

    @Test
    @DisplayName("TraceEvent should have getEventType method")
    void shouldHaveGetEventTypeMethod() throws NoSuchMethodException {
      final Method method = TraceEvent.class.getMethod("getEventType");
      assertNotNull(method, "getEventType method should exist");
      assertEquals(TraceEventType.class, method.getReturnType(), "Should return TraceEventType");
    }

    @Test
    @DisplayName("TraceEvent should have getTimestamp method")
    void shouldHaveGetTimestampMethod() throws NoSuchMethodException {
      final Method method = TraceEvent.class.getMethod("getTimestamp");
      assertNotNull(method, "getTimestamp method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("TraceEvent should have getInstructionAddress method")
    void shouldHaveGetInstructionAddressMethod() throws NoSuchMethodException {
      final Method method = TraceEvent.class.getMethod("getInstructionAddress");
      assertNotNull(method, "getInstructionAddress method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("TraceEvent should have getFunctionName method")
    void shouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = TraceEvent.class.getMethod("getFunctionName");
      assertNotNull(method, "getFunctionName method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("TraceEvent should have getEventData method")
    void shouldHaveGetEventDataMethod() throws NoSuchMethodException {
      final Method method = TraceEvent.class.getMethod("getEventData");
      assertNotNull(method, "getEventData method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }
  }

  @Nested
  @DisplayName("TraceFilter Interface Tests")
  class TraceFilterInterfaceTests {

    @Test
    @DisplayName("TraceFilter should be a nested interface")
    void traceFilterShouldBeNestedInterface() {
      assertTrue(TraceFilter.class.isInterface(), "TraceFilter should be an interface");
    }

    @Test
    @DisplayName("TraceFilter should have shouldTrace method")
    void shouldHaveShouldTraceMethod() throws NoSuchMethodException {
      final Method method =
          TraceFilter.class.getMethod("shouldTrace", TraceEventType.class, String.class);
      assertNotNull(method, "shouldTrace method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("TraceFilter should have getMaxBufferSize method")
    void shouldHaveGetMaxBufferSizeMethod() throws NoSuchMethodException {
      final Method method = TraceFilter.class.getMethod("getMaxBufferSize");
      assertNotNull(method, "getMaxBufferSize method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock tracer should start and stop tracing")
    void mockTracerShouldStartAndStopTracing() {
      final MockExecutionTracer tracer = new MockExecutionTracer();

      assertFalse(tracer.isTracing(), "Should not be tracing initially");

      tracer.startTracing();
      assertTrue(tracer.isTracing(), "Should be tracing after start");

      tracer.stopTracing();
      assertFalse(tracer.isTracing(), "Should not be tracing after stop");
    }

    @Test
    @DisplayName("mock tracer should return trace events")
    void mockTracerShouldReturnTraceEvents() {
      final MockExecutionTracer tracer = new MockExecutionTracer();
      tracer.addEvent(new MockTraceEvent(TraceEventType.FUNCTION_CALL, "main"));
      tracer.addEvent(new MockTraceEvent(TraceEventType.FUNCTION_RETURN, "main"));

      final List<TraceEvent> events = tracer.getTraceEvents();
      assertEquals(2, events.size(), "Should have 2 events");
    }

    @Test
    @DisplayName("mock tracer should clear trace events")
    void mockTracerShouldClearTraceEvents() {
      final MockExecutionTracer tracer = new MockExecutionTracer();
      tracer.addEvent(new MockTraceEvent(TraceEventType.INSTRUCTION, "func"));
      assertEquals(1, tracer.getTraceEvents().size(), "Should have 1 event");

      tracer.clearTrace();
      assertEquals(0, tracer.getTraceEvents().size(), "Should have 0 events after clear");
    }

    @Test
    @DisplayName("mock trace event should return correct values")
    void mockTraceEventShouldReturnCorrectValues() {
      final MockTraceEvent event = new MockTraceEvent(TraceEventType.MEMORY_ACCESS, "readMemory");

      assertEquals(TraceEventType.MEMORY_ACCESS, event.getEventType(), "Event type should match");
      assertEquals("readMemory", event.getFunctionName(), "Function name should match");
      assertTrue(event.getTimestamp() > 0, "Timestamp should be positive");
    }
  }

  /** Mock implementation of ExecutionTracer for testing. */
  private static class MockExecutionTracer implements ExecutionTracer {
    private final List<TraceEvent> events = new ArrayList<>();
    private boolean tracing;
    private TraceFilter filter;

    @Override
    public void startTracing() {
      tracing = true;
    }

    @Override
    public void stopTracing() {
      tracing = false;
    }

    @Override
    public List<TraceEvent> getTraceEvents() {
      return new ArrayList<>(events);
    }

    @Override
    public void clearTrace() {
      events.clear();
    }

    @Override
    public boolean isTracing() {
      return tracing;
    }

    @Override
    public void setTraceFilter(final TraceFilter filter) {
      this.filter = filter;
    }

    public void addEvent(final TraceEvent event) {
      events.add(event);
    }
  }

  /** Mock implementation of TraceEvent for testing. */
  private static class MockTraceEvent implements TraceEvent {
    private final TraceEventType eventType;
    private final String functionName;
    private final long timestamp;

    MockTraceEvent(final TraceEventType eventType, final String functionName) {
      this.eventType = eventType;
      this.functionName = functionName;
      this.timestamp = System.nanoTime();
    }

    @Override
    public TraceEventType getEventType() {
      return eventType;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public long getInstructionAddress() {
      return 0x1000;
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }

    @Override
    public String getEventData() {
      return "";
    }
  }
}
