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

import ai.tegmentum.wasmtime4j.debug.ProfilingData.FunctionStatistics;
import ai.tegmentum.wasmtime4j.debug.ProfilingData.MemoryProfilingData;
import ai.tegmentum.wasmtime4j.debug.ProfilingData.MemoryProfilingData.AllocationStatistics;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link ProfilingData} interface.
 *
 * <p>ProfilingData provides profiling information for WebAssembly execution including function
 * statistics, memory usage, and allocation data.
 */
@DisplayName("ProfilingData Tests")
class ProfilingDataTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(ProfilingData.class.isInterface(), "ProfilingData should be an interface");
    }

    @Test
    @DisplayName("should have getSessionId method")
    void shouldHaveGetSessionIdMethod() throws NoSuchMethodException {
      final Method method = ProfilingData.class.getMethod("getSessionId");
      assertNotNull(method, "getSessionId method should exist");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("should have getTotalExecutionTime method")
    void shouldHaveGetTotalExecutionTimeMethod() throws NoSuchMethodException {
      final Method method = ProfilingData.class.getMethod("getTotalExecutionTime");
      assertNotNull(method, "getTotalExecutionTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getFunctionStatistics method")
    void shouldHaveGetFunctionStatisticsMethod() throws NoSuchMethodException {
      final Method method = ProfilingData.class.getMethod("getFunctionStatistics");
      assertNotNull(method, "getFunctionStatistics method should exist");
      assertEquals(Map.class, method.getReturnType(), "Should return Map");
    }

    @Test
    @DisplayName("should have getMemoryData method")
    void shouldHaveGetMemoryDataMethod() throws NoSuchMethodException {
      final Method method = ProfilingData.class.getMethod("getMemoryData");
      assertNotNull(method, "getMemoryData method should exist");
      assertEquals(
          MemoryProfilingData.class, method.getReturnType(), "Should return MemoryProfilingData");
    }

    @Test
    @DisplayName("should have getStartTime method")
    void shouldHaveGetStartTimeMethod() throws NoSuchMethodException {
      final Method method = ProfilingData.class.getMethod("getStartTime");
      assertNotNull(method, "getStartTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getEndTime method")
    void shouldHaveGetEndTimeMethod() throws NoSuchMethodException {
      final Method method = ProfilingData.class.getMethod("getEndTime");
      assertNotNull(method, "getEndTime method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have exactly six methods")
    void shouldHaveExactlySixMethods() {
      final Method[] methods = ProfilingData.class.getDeclaredMethods();
      assertEquals(6, methods.length, "ProfilingData should have exactly 6 methods");
    }
  }

  @Nested
  @DisplayName("FunctionStatistics Nested Interface Tests")
  class FunctionStatisticsInterfaceTests {

    @Test
    @DisplayName("should have FunctionStatistics nested interface")
    void shouldHaveFunctionStatisticsNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : ProfilingData.class.getDeclaredClasses()) {
        if ("FunctionStatistics".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have FunctionStatistics nested interface");
    }

    @Test
    @DisplayName("FunctionStatistics should have getFunctionName method")
    void functionStatisticsShouldHaveGetFunctionNameMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getFunctionName");
      assertEquals(String.class, method.getReturnType(), "Should return String");
    }

    @Test
    @DisplayName("FunctionStatistics should have getCallCount method")
    void functionStatisticsShouldHaveGetCallCountMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getCallCount");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("FunctionStatistics should have getTotalTime method")
    void functionStatisticsShouldHaveGetTotalTimeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getTotalTime");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("FunctionStatistics should have getAverageTime method")
    void functionStatisticsShouldHaveGetAverageTimeMethod() throws NoSuchMethodException {
      final Method method = FunctionStatistics.class.getMethod("getAverageTime");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("MemoryProfilingData Nested Interface Tests")
  class MemoryProfilingDataInterfaceTests {

    @Test
    @DisplayName("should have MemoryProfilingData nested interface")
    void shouldHaveMemoryProfilingDataNestedInterface() {
      boolean found = false;
      for (final Class<?> inner : ProfilingData.class.getDeclaredClasses()) {
        if ("MemoryProfilingData".equals(inner.getSimpleName()) && inner.isInterface()) {
          found = true;
          break;
        }
      }
      assertTrue(found, "Should have MemoryProfilingData nested interface");
    }

    @Test
    @DisplayName("MemoryProfilingData should have getPeakUsage method")
    void memoryProfilingDataShouldHaveGetPeakUsageMethod() throws NoSuchMethodException {
      final Method method = MemoryProfilingData.class.getMethod("getPeakUsage");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryProfilingData should have getCurrentUsage method")
    void memoryProfilingDataShouldHaveGetCurrentUsageMethod() throws NoSuchMethodException {
      final Method method = MemoryProfilingData.class.getMethod("getCurrentUsage");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryProfilingData should have getAllocationStats method")
    void memoryProfilingDataShouldHaveGetAllocationStatsMethod() throws NoSuchMethodException {
      final Method method = MemoryProfilingData.class.getMethod("getAllocationStats");
      assertEquals(
          AllocationStatistics.class, method.getReturnType(), "Should return AllocationStatistics");
    }
  }

  @Nested
  @DisplayName("AllocationStatistics Nested Interface Tests")
  class AllocationStatisticsInterfaceTests {

    @Test
    @DisplayName("AllocationStatistics should have getTotalAllocations method")
    void allocationStatisticsShouldHaveGetTotalAllocationsMethod() throws NoSuchMethodException {
      final Method method = AllocationStatistics.class.getMethod("getTotalAllocations");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AllocationStatistics should have getTotalDeallocations method")
    void allocationStatisticsShouldHaveGetTotalDeallocationsMethod() throws NoSuchMethodException {
      final Method method = AllocationStatistics.class.getMethod("getTotalDeallocations");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AllocationStatistics should have getBytesAllocated method")
    void allocationStatisticsShouldHaveGetBytesAllocatedMethod() throws NoSuchMethodException {
      final Method method = AllocationStatistics.class.getMethod("getBytesAllocated");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("AllocationStatistics should have getBytesDeallocated method")
    void allocationStatisticsShouldHaveGetBytesDeallocatedMethod() throws NoSuchMethodException {
      final Method method = AllocationStatistics.class.getMethod("getBytesDeallocated");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock should return session ID")
    void mockShouldReturnSessionId() {
      final MockProfilingData data = new MockProfilingData("session-123");

      assertEquals("session-123", data.getSessionId(), "Session ID should match");
    }

    @Test
    @DisplayName("mock should return total execution time")
    void mockShouldReturnTotalExecutionTime() {
      final MockProfilingData data = new MockProfilingData("s1");
      data.setTotalExecutionTime(1_000_000_000L);

      assertEquals(
          1_000_000_000L,
          data.getTotalExecutionTime(),
          "Total execution time should be 1 second in nanos");
    }

    @Test
    @DisplayName("mock should return function statistics")
    void mockShouldReturnFunctionStatistics() {
      final MockProfilingData data = new MockProfilingData("s1");
      data.addFunctionStats("main", new MockFunctionStatistics("main", 100, 500_000L, 5000L));

      final Map<String, FunctionStatistics> stats = data.getFunctionStatistics();

      assertEquals(1, stats.size(), "Should have 1 function");
      assertEquals(100, stats.get("main").getCallCount(), "Call count should match");
    }

    @Test
    @DisplayName("mock should return memory data")
    void mockShouldReturnMemoryData() {
      final MockProfilingData data = new MockProfilingData("s1");
      data.setMemoryData(new MockMemoryProfilingData(65536L, 32768L, null));

      final MemoryProfilingData memData = data.getMemoryData();

      assertEquals(65536L, memData.getPeakUsage(), "Peak usage should match");
      assertEquals(32768L, memData.getCurrentUsage(), "Current usage should match");
    }

    @Test
    @DisplayName("mock should return start and end times")
    void mockShouldReturnStartAndEndTimes() {
      final MockProfilingData data = new MockProfilingData("s1");
      data.setStartTime(1000L);
      data.setEndTime(2000L);

      assertEquals(1000L, data.getStartTime(), "Start time should match");
      assertEquals(2000L, data.getEndTime(), "End time should match");
    }

    @Test
    @DisplayName("function statistics should calculate average time")
    void functionStatisticsShouldCalculateAverageTime() {
      final MockFunctionStatistics stats = new MockFunctionStatistics("test", 10, 1000L, 100L);

      assertEquals("test", stats.getFunctionName(), "Function name should match");
      assertEquals(10, stats.getCallCount(), "Call count should match");
      assertEquals(1000L, stats.getTotalTime(), "Total time should match");
      assertEquals(100L, stats.getAverageTime(), "Average time should match");
    }

    @Test
    @DisplayName("allocation statistics should track allocations and deallocations")
    void allocationStatisticsShouldTrackAllocationsAndDeallocations() {
      final MockAllocationStatistics stats = new MockAllocationStatistics(50, 30, 10000L, 6000L);

      assertEquals(50, stats.getTotalAllocations(), "Allocations should match");
      assertEquals(30, stats.getTotalDeallocations(), "Deallocations should match");
      assertEquals(10000L, stats.getBytesAllocated(), "Bytes allocated should match");
      assertEquals(6000L, stats.getBytesDeallocated(), "Bytes deallocated should match");
    }
  }

  /** Mock implementation of ProfilingData for testing. */
  private static class MockProfilingData implements ProfilingData {
    private final String sessionId;
    private long totalExecutionTime;
    private final Map<String, FunctionStatistics> functionStats = new HashMap<>();
    private MemoryProfilingData memoryData;
    private long startTime;
    private long endTime;

    MockProfilingData(final String sessionId) {
      this.sessionId = sessionId;
    }

    @Override
    public String getSessionId() {
      return sessionId;
    }

    @Override
    public long getTotalExecutionTime() {
      return totalExecutionTime;
    }

    public void setTotalExecutionTime(final long time) {
      this.totalExecutionTime = time;
    }

    @Override
    public Map<String, FunctionStatistics> getFunctionStatistics() {
      return new HashMap<>(functionStats);
    }

    public void addFunctionStats(final String name, final FunctionStatistics stats) {
      functionStats.put(name, stats);
    }

    @Override
    public MemoryProfilingData getMemoryData() {
      return memoryData;
    }

    public void setMemoryData(final MemoryProfilingData data) {
      this.memoryData = data;
    }

    @Override
    public long getStartTime() {
      return startTime;
    }

    public void setStartTime(final long time) {
      this.startTime = time;
    }

    @Override
    public long getEndTime() {
      return endTime;
    }

    public void setEndTime(final long time) {
      this.endTime = time;
    }
  }

  /** Mock implementation of FunctionStatistics for testing. */
  private static class MockFunctionStatistics implements FunctionStatistics {
    private final String functionName;
    private final long callCount;
    private final long totalTime;
    private final long averageTime;

    MockFunctionStatistics(
        final String functionName,
        final long callCount,
        final long totalTime,
        final long averageTime) {
      this.functionName = functionName;
      this.callCount = callCount;
      this.totalTime = totalTime;
      this.averageTime = averageTime;
    }

    @Override
    public String getFunctionName() {
      return functionName;
    }

    @Override
    public long getCallCount() {
      return callCount;
    }

    @Override
    public long getTotalTime() {
      return totalTime;
    }

    @Override
    public long getAverageTime() {
      return averageTime;
    }
  }

  /** Mock implementation of MemoryProfilingData for testing. */
  private static class MockMemoryProfilingData implements MemoryProfilingData {
    private final long peakUsage;
    private final long currentUsage;
    private final AllocationStatistics allocationStats;

    MockMemoryProfilingData(
        final long peakUsage, final long currentUsage, final AllocationStatistics allocationStats) {
      this.peakUsage = peakUsage;
      this.currentUsage = currentUsage;
      this.allocationStats = allocationStats;
    }

    @Override
    public long getPeakUsage() {
      return peakUsage;
    }

    @Override
    public long getCurrentUsage() {
      return currentUsage;
    }

    @Override
    public AllocationStatistics getAllocationStats() {
      return allocationStats;
    }
  }

  /** Mock implementation of AllocationStatistics for testing. */
  private static class MockAllocationStatistics implements AllocationStatistics {
    private final long totalAllocations;
    private final long totalDeallocations;
    private final long bytesAllocated;
    private final long bytesDeallocated;

    MockAllocationStatistics(
        final long totalAllocations,
        final long totalDeallocations,
        final long bytesAllocated,
        final long bytesDeallocated) {
      this.totalAllocations = totalAllocations;
      this.totalDeallocations = totalDeallocations;
      this.bytesAllocated = bytesAllocated;
      this.bytesDeallocated = bytesDeallocated;
    }

    @Override
    public long getTotalAllocations() {
      return totalAllocations;
    }

    @Override
    public long getTotalDeallocations() {
      return totalDeallocations;
    }

    @Override
    public long getBytesAllocated() {
      return bytesAllocated;
    }

    @Override
    public long getBytesDeallocated() {
      return bytesDeallocated;
    }
  }
}
