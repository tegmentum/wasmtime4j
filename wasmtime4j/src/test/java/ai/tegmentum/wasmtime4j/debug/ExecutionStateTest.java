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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.ExecutionState.ExecutionStatistics;
import ai.tegmentum.wasmtime4j.debug.ExecutionState.ExecutionStatus;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExecutionState} interface.
 *
 * <p>ExecutionState provides execution state tracking for WebAssembly debugging including status,
 * instruction pointer, stack frames, and statistics.
 */
@DisplayName("ExecutionState Tests")
class ExecutionStateTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionState.class.isInterface(), "ExecutionState should be an interface");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ExecutionState.class.getMethod("getStatus");
      assertNotNull(method, "getStatus method should exist");
      assertEquals(ExecutionStatus.class, method.getReturnType(), "Should return ExecutionStatus");
    }

    @Test
    @DisplayName("should have getInstructionPointer method")
    void shouldHaveGetInstructionPointerMethod() throws NoSuchMethodException {
      final Method method = ExecutionState.class.getMethod("getInstructionPointer");
      assertNotNull(method, "getInstructionPointer method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getStackFrames method")
    void shouldHaveGetStackFramesMethod() throws NoSuchMethodException {
      final Method method = ExecutionState.class.getMethod("getStackFrames");
      assertNotNull(method, "getStackFrames method should exist");
      assertEquals(List.class, method.getReturnType(), "Should return List");
    }

    @Test
    @DisplayName("should have getCurrentModule method")
    void shouldHaveGetCurrentModuleMethod() throws NoSuchMethodException {
      final Method method = ExecutionState.class.getMethod("getCurrentModule");
      assertNotNull(method, "getCurrentModule method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getCurrentFunction method")
    void shouldHaveGetCurrentFunctionMethod() throws NoSuchMethodException {
      final Method method = ExecutionState.class.getMethod("getCurrentFunction");
      assertNotNull(method, "getCurrentFunction method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ExecutionState.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          ExecutionStatistics.class, method.getReturnType(), "Should return ExecutionStatistics");
    }
  }

  @Nested
  @DisplayName("ExecutionStatus Enum Tests")
  class ExecutionStatusEnumTests {

    @Test
    @DisplayName("should have RUNNING value")
    void shouldHaveRunningValue() {
      assertNotNull(ExecutionStatus.valueOf("RUNNING"), "RUNNING should exist");
    }

    @Test
    @DisplayName("should have PAUSED value")
    void shouldHavePausedValue() {
      assertNotNull(ExecutionStatus.valueOf("PAUSED"), "PAUSED should exist");
    }

    @Test
    @DisplayName("should have STOPPED value")
    void shouldHaveStoppedValue() {
      assertNotNull(ExecutionStatus.valueOf("STOPPED"), "STOPPED should exist");
    }

    @Test
    @DisplayName("should have CRASHED value")
    void shouldHaveCrashedValue() {
      assertNotNull(ExecutionStatus.valueOf("CRASHED"), "CRASHED should exist");
    }

    @Test
    @DisplayName("should have COMPLETED value")
    void shouldHaveCompletedValue() {
      assertNotNull(ExecutionStatus.valueOf("COMPLETED"), "COMPLETED should exist");
    }

    @Test
    @DisplayName("should have exactly five values")
    void shouldHaveExactlyFiveValues() {
      assertEquals(
          5, ExecutionStatus.values().length, "Should have exactly 5 ExecutionStatus values");
    }
  }

  @Nested
  @DisplayName("ExecutionStatistics Interface Tests")
  class ExecutionStatisticsInterfaceTests {

    @Test
    @DisplayName("ExecutionStatistics should be a nested interface")
    void executionStatisticsShouldBeNestedInterface() {
      assertTrue(
          ExecutionStatistics.class.isInterface(), "ExecutionStatistics should be an interface");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getInstructionCount method")
    void shouldHaveGetInstructionCountMethod() throws NoSuchMethodException {
      final Method method = ExecutionStatistics.class.getMethod("getInstructionCount");
      assertNotNull(method, "getInstructionCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getExecutionTime method")
    void shouldHaveGetExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = ExecutionStatistics.class.getMethod("getExecutionTime");
      assertNotNull(method, "getExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getFunctionCallCount method")
    void shouldHaveGetFunctionCallCountMethod() throws NoSuchMethodException {
      final Method method = ExecutionStatistics.class.getMethod("getFunctionCallCount");
      assertNotNull(method, "getFunctionCallCount method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock state should return status")
    void mockStateShouldReturnStatus() {
      final MockExecutionState state = new MockExecutionState(ExecutionStatus.RUNNING);

      assertEquals(ExecutionStatus.RUNNING, state.getStatus(), "Status should be RUNNING");
    }

    @Test
    @DisplayName("mock state should return instruction pointer")
    void mockStateShouldReturnInstructionPointer() {
      final MockExecutionState state = new MockExecutionState(ExecutionStatus.PAUSED);
      state.setInstructionPointer(0x1234);

      assertEquals(0x1234, state.getInstructionPointer(), "IP should match");
    }

    @Test
    @DisplayName("mock state should return current module and function")
    void mockStateShouldReturnCurrentModuleAndFunction() {
      final MockExecutionState state = new MockExecutionState(ExecutionStatus.RUNNING);
      state.setCurrentModule("main.wasm");
      state.setCurrentFunction("processData");

      assertEquals("main.wasm", state.getCurrentModule(), "Module should match");
      assertEquals("processData", state.getCurrentFunction(), "Function should match");
    }

    @Test
    @DisplayName("mock state should return stack frames")
    void mockStateShouldReturnStackFrames() {
      final MockExecutionState state = new MockExecutionState(ExecutionStatus.PAUSED);
      state.addStackFrame(new MockStackFrame(0, "main"));
      state.addStackFrame(new MockStackFrame(1, "helper"));

      assertEquals(2, state.getStackFrames().size(), "Should have 2 stack frames");
    }

    @Test
    @DisplayName("mock state should return statistics")
    void mockStateShouldReturnStatistics() {
      final MockExecutionState state = new MockExecutionState(ExecutionStatus.COMPLETED);
      state.setStatistics(new MockExecutionStatistics(1000, 50, 25));

      final ExecutionStatistics stats = state.getStatistics();
      assertNotNull(stats, "Statistics should not be null");
      assertEquals(1000, stats.getInstructionCount(), "Instruction count should match");
      assertEquals(50, stats.getExecutionTime(), "Execution time should match");
      assertEquals(25, stats.getFunctionCallCount(), "Function call count should match");
    }

    @Test
    @DisplayName("mock state should track all execution statuses")
    void mockStateShouldTrackAllExecutionStatuses() {
      for (final ExecutionStatus status : ExecutionStatus.values()) {
        final MockExecutionState state = new MockExecutionState(status);
        assertEquals(status, state.getStatus(), "Status should match: " + status);
      }
    }
  }

  /** Mock implementation of ExecutionState for testing. */
  private static class MockExecutionState implements ExecutionState {
    private final ExecutionStatus status;
    private long instructionPointer;
    private String currentModule;
    private String currentFunction;
    private final List<StackFrame> stackFrames = new ArrayList<>();
    private ExecutionStatistics statistics;

    MockExecutionState(final ExecutionStatus status) {
      this.status = status;
    }

    @Override
    public ExecutionStatus getStatus() {
      return status;
    }

    @Override
    public long getInstructionPointer() {
      return instructionPointer;
    }

    @Override
    public List<StackFrame> getStackFrames() {
      return stackFrames;
    }

    @Override
    public String getCurrentModule() {
      return currentModule;
    }

    @Override
    public String getCurrentFunction() {
      return currentFunction;
    }

    @Override
    public ExecutionStatistics getStatistics() {
      return statistics;
    }

    public void setInstructionPointer(final long instructionPointer) {
      this.instructionPointer = instructionPointer;
    }

    public void setCurrentModule(final String currentModule) {
      this.currentModule = currentModule;
    }

    public void setCurrentFunction(final String currentFunction) {
      this.currentFunction = currentFunction;
    }

    public void addStackFrame(final StackFrame frame) {
      stackFrames.add(frame);
    }

    public void setStatistics(final ExecutionStatistics statistics) {
      this.statistics = statistics;
    }
  }

  /** Mock implementation of ExecutionStatistics for testing. */
  private static class MockExecutionStatistics implements ExecutionStatistics {
    private final long instructionCount;
    private final long executionTime;
    private final long functionCallCount;

    MockExecutionStatistics(
        final long instructionCount, final long executionTime, final long functionCallCount) {
      this.instructionCount = instructionCount;
      this.executionTime = executionTime;
      this.functionCallCount = functionCallCount;
    }

    @Override
    public long getInstructionCount() {
      return instructionCount;
    }

    @Override
    public long getExecutionTime() {
      return executionTime;
    }

    @Override
    public long getFunctionCallCount() {
      return functionCallCount;
    }
  }

  /** Mock implementation of StackFrame for testing. */
  private static class MockStackFrame implements StackFrame {
    private final int frameIndex;
    private final String functionName;

    MockStackFrame(final int frameIndex, final String functionName) {
      this.frameIndex = frameIndex;
      this.functionName = functionName;
    }

    @Override
    public int getFrameIndex() {
      return frameIndex;
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }

    @Override
    public SourceLocation getSourceLocation() {
      return null;
    }

    @Override
    public List<Variable> getVariables() {
      return new ArrayList<>();
    }

    @Override
    public int getDepth() {
      return frameIndex;
    }
  }
}
