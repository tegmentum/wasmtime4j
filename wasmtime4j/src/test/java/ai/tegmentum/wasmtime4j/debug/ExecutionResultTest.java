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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.ExecutionResult.ExecutionStatistics;
import ai.tegmentum.wasmtime4j.debug.ExecutionResult.ExecutionStatus;
import java.lang.reflect.Method;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ExecutionResult} interface.
 *
 * <p>ExecutionResult provides the result of WebAssembly function execution including status, result
 * value, execution time, error information, and execution statistics.
 */
@DisplayName("ExecutionResult Tests")
class ExecutionResultTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ExecutionResult.class.isInterface(), "ExecutionResult should be an interface");
    }

    @Test
    @DisplayName("should have getStatus method")
    void shouldHaveGetStatusMethod() throws NoSuchMethodException {
      final Method method = ExecutionResult.class.getMethod("getStatus");
      assertEquals(ExecutionStatus.class, method.getReturnType(), "Should return ExecutionStatus");
    }

    @Test
    @DisplayName("should have getResult method")
    void shouldHaveGetResultMethod() throws NoSuchMethodException {
      final Method method = ExecutionResult.class.getMethod("getResult");
      assertEquals(Object.class, method.getReturnType(), "Should return Object");
    }

    @Test
    @DisplayName("should have getExecutionTime method")
    void shouldHaveGetExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = ExecutionResult.class.getMethod("getExecutionTime");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getErrorMessage method")
    void shouldHaveGetErrorMessageMethod() throws NoSuchMethodException {
      final Method method = ExecutionResult.class.getMethod("getErrorMessage");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getException method")
    void shouldHaveGetExceptionMethod() throws NoSuchMethodException {
      final Method method = ExecutionResult.class.getMethod("getException");
      assertEquals(Throwable.class, method.getReturnType(), "Should return Throwable");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = ExecutionResult.class.getMethod("getStatistics");
      assertEquals(
          ExecutionStatistics.class, method.getReturnType(), "Should return ExecutionStatistics");
    }

    @Test
    @DisplayName("should have exactly six methods")
    void shouldHaveExactlySixMethods() {
      final Method[] methods = ExecutionResult.class.getDeclaredMethods();
      assertEquals(6, methods.length, "ExecutionResult should have exactly 6 methods");
    }
  }

  @Nested
  @DisplayName("ExecutionStatus Enum Tests")
  class ExecutionStatusEnumTests {

    @Test
    @DisplayName("should have SUCCESS value")
    void shouldHaveSuccessValue() {
      assertNotNull(ExecutionStatus.valueOf("SUCCESS"), "SUCCESS should exist");
    }

    @Test
    @DisplayName("should have ERROR value")
    void shouldHaveErrorValue() {
      assertNotNull(ExecutionStatus.valueOf("ERROR"), "ERROR should exist");
    }

    @Test
    @DisplayName("should have CANCELLED value")
    void shouldHaveCancelledValue() {
      assertNotNull(ExecutionStatus.valueOf("CANCELLED"), "CANCELLED should exist");
    }

    @Test
    @DisplayName("should have TIMEOUT value")
    void shouldHaveTimeoutValue() {
      assertNotNull(ExecutionStatus.valueOf("TIMEOUT"), "TIMEOUT should exist");
    }

    @Test
    @DisplayName("should have INTERRUPTED value")
    void shouldHaveInterruptedValue() {
      assertNotNull(ExecutionStatus.valueOf("INTERRUPTED"), "INTERRUPTED should exist");
    }

    @Test
    @DisplayName("should have exactly five values")
    void shouldHaveExactlyFiveValues() {
      assertEquals(
          5, ExecutionStatus.values().length, "ExecutionStatus should have exactly 5 values");
    }
  }

  @Nested
  @DisplayName("ExecutionStatistics Nested Interface Tests")
  class ExecutionStatisticsInterfaceTests {

    @Test
    @DisplayName("should have ExecutionStatistics nested interface")
    void shouldHaveExecutionStatisticsNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : ExecutionResult.class.getDeclaredClasses()) {
        if ("ExecutionStatistics".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have ExecutionStatistics nested interface");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getInstructionCount method")
    void executionStatisticsShouldHaveGetInstructionCountMethod() throws NoSuchMethodException {
      final Method method = ExecutionStatistics.class.getMethod("getInstructionCount");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getMemoryAllocations method")
    void executionStatisticsShouldHaveGetMemoryAllocationsMethod() throws NoSuchMethodException {
      final Method method = ExecutionStatistics.class.getMethod("getMemoryAllocations");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getFunctionCalls method")
    void executionStatisticsShouldHaveGetFunctionCallsMethod() throws NoSuchMethodException {
      final Method method = ExecutionStatistics.class.getMethod("getFunctionCalls");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("ExecutionStatistics should have getPeakMemoryUsage method")
    void executionStatisticsShouldHaveGetPeakMemoryUsageMethod() throws NoSuchMethodException {
      final Method method = ExecutionStatistics.class.getMethod("getPeakMemoryUsage");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return successful execution result")
    void mockShouldReturnSuccessfulExecutionResult() {
      final MockExecutionStatistics stats = new MockExecutionStatistics(1000L, 10L, 50L, 65536L);
      final MockExecutionResult result =
          new MockExecutionResult(ExecutionStatus.SUCCESS, 42, 500_000L, null, null, stats);

      assertEquals(ExecutionStatus.SUCCESS, result.getStatus(), "Status should be SUCCESS");
      assertEquals(42, result.getResult(), "Result should be 42");
      assertEquals(500_000L, result.getExecutionTime(), "Execution time should match");
      assertNull(result.getErrorMessage(), "Error message should be null");
      assertNull(result.getException(), "Exception should be null");
      assertNotNull(result.getStatistics(), "Statistics should not be null");
    }

    @Test
    @DisplayName("mock should return error execution result")
    void mockShouldReturnErrorExecutionResult() {
      final RuntimeException exception = new RuntimeException("Test error");
      final MockExecutionResult result =
          new MockExecutionResult(
              ExecutionStatus.ERROR, null, 100_000L, "Execution failed", exception, null);

      assertEquals(ExecutionStatus.ERROR, result.getStatus(), "Status should be ERROR");
      assertNull(result.getResult(), "Result should be null on error");
      assertEquals("Execution failed", result.getErrorMessage(), "Error message should match");
      assertEquals(exception, result.getException(), "Exception should match");
    }

    @Test
    @DisplayName("mock should return cancelled execution result")
    void mockShouldReturnCancelledExecutionResult() {
      final MockExecutionResult result =
          new MockExecutionResult(
              ExecutionStatus.CANCELLED, null, 0L, "Execution was cancelled", null, null);

      assertEquals(ExecutionStatus.CANCELLED, result.getStatus(), "Status should be CANCELLED");
    }

    @Test
    @DisplayName("mock should return timeout execution result")
    void mockShouldReturnTimeoutExecutionResult() {
      final MockExecutionResult result =
          new MockExecutionResult(
              ExecutionStatus.TIMEOUT,
              null,
              5_000_000_000L,
              "Execution timed out after 5s",
              null,
              null);

      assertEquals(ExecutionStatus.TIMEOUT, result.getStatus(), "Status should be TIMEOUT");
      assertEquals(
          5_000_000_000L, result.getExecutionTime(), "Execution time should be timeout limit");
    }

    @Test
    @DisplayName("mock should return interrupted execution result")
    void mockShouldReturnInterruptedExecutionResult() {
      final InterruptedException exception = new InterruptedException("Thread interrupted");
      final MockExecutionResult result =
          new MockExecutionResult(
              ExecutionStatus.INTERRUPTED,
              null,
              250_000L,
              "Thread was interrupted",
              exception,
              null);

      assertEquals(ExecutionStatus.INTERRUPTED, result.getStatus(), "Status should be INTERRUPTED");
      assertTrue(
          result.getException() instanceof InterruptedException,
          "Exception should be InterruptedException");
    }

    @Test
    @DisplayName("execution statistics should track all metrics")
    void executionStatisticsShouldTrackAllMetrics() {
      final MockExecutionStatistics stats =
          new MockExecutionStatistics(10000L, 100L, 500L, 131072L);

      assertEquals(10000L, stats.getInstructionCount(), "Instruction count should match");
      assertEquals(100L, stats.getMemoryAllocations(), "Memory allocations should match");
      assertEquals(500L, stats.getFunctionCalls(), "Function calls should match");
      assertEquals(131072L, stats.getPeakMemoryUsage(), "Peak memory usage should match");
    }

    @Test
    @DisplayName("all status values should be iterable")
    void allStatusValuesShouldBeIterable() {
      int count = 0;
      for (final ExecutionStatus status : ExecutionStatus.values()) {
        assertNotNull(status, "Status should not be null");
        count++;
      }
      assertEquals(5, count, "Should have iterated over 5 status values");
    }
  }

  /** Mock implementation of ExecutionResult for testing. */
  private static class MockExecutionResult implements ExecutionResult {
    private final ExecutionStatus status;
    private final Object result;
    private final long executionTime;
    private final String errorMessage;
    private final Throwable exception;
    private final ExecutionStatistics statistics;

    MockExecutionResult(
        final ExecutionStatus status,
        final Object result,
        final long executionTime,
        final String errorMessage,
        final Throwable exception,
        final ExecutionStatistics statistics) {
      this.status = status;
      this.result = result;
      this.executionTime = executionTime;
      this.errorMessage = errorMessage;
      this.exception = exception;
      this.statistics = statistics;
    }

    @Override
    public ExecutionStatus getStatus() {
      return status;
    }

    @Override
    public Object getResult() {
      return result;
    }

    @Override
    public long getExecutionTime() {
      return executionTime;
    }

    @Override
    public String getErrorMessage() {
      return errorMessage;
    }

    @Override
    public Throwable getException() {
      return exception;
    }

    @Override
    public ExecutionStatistics getStatistics() {
      return statistics;
    }
  }

  /** Mock implementation of ExecutionStatistics for testing. */
  private static class MockExecutionStatistics implements ExecutionStatistics {
    private final long instructionCount;
    private final long memoryAllocations;
    private final long functionCalls;
    private final long peakMemoryUsage;

    MockExecutionStatistics(
        final long instructionCount,
        final long memoryAllocations,
        final long functionCalls,
        final long peakMemoryUsage) {
      this.instructionCount = instructionCount;
      this.memoryAllocations = memoryAllocations;
      this.functionCalls = functionCalls;
      this.peakMemoryUsage = peakMemoryUsage;
    }

    @Override
    public long getInstructionCount() {
      return instructionCount;
    }

    @Override
    public long getMemoryAllocations() {
      return memoryAllocations;
    }

    @Override
    public long getFunctionCalls() {
      return functionCalls;
    }

    @Override
    public long getPeakMemoryUsage() {
      return peakMemoryUsage;
    }
  }
}
