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

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.debug.MemoryInfo.MemoryStatistics;
import java.lang.reflect.Method;
import java.util.Arrays;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link MemoryInfo} interface.
 *
 * <p>MemoryInfo provides information about WebAssembly linear memory including base address, size,
 * page counts, and memory statistics.
 */
@DisplayName("MemoryInfo Tests")
class MemoryInfoTest {

  @Nested
  @DisplayName("Interface Structure Tests")
  class InterfaceStructureTests {

    @Test
    @DisplayName("should be an interface")
    void shouldBeAnInterface() {
      assertTrue(MemoryInfo.class.isInterface(), "MemoryInfo should be an interface");
    }

    @Test
    @DisplayName("should have getBaseAddress method")
    void shouldHaveGetBaseAddressMethod() throws NoSuchMethodException {
      final Method method = MemoryInfo.class.getMethod("getBaseAddress");
      assertNotNull(method, "getBaseAddress method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getSize method")
    void shouldHaveGetSizeMethod() throws NoSuchMethodException {
      final Method method = MemoryInfo.class.getMethod("getSize");
      assertNotNull(method, "getSize method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("should have getPageCount method")
    void shouldHaveGetPageCountMethod() throws NoSuchMethodException {
      final Method method = MemoryInfo.class.getMethod("getPageCount");
      assertNotNull(method, "getPageCount method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have getMaxPages method")
    void shouldHaveGetMaxPagesMethod() throws NoSuchMethodException {
      final Method method = MemoryInfo.class.getMethod("getMaxPages");
      assertNotNull(method, "getMaxPages method should exist");
      assertEquals(int.class, method.getReturnType(), "Should return int");
    }

    @Test
    @DisplayName("should have isShared method")
    void shouldHaveIsSharedMethod() throws NoSuchMethodException {
      final Method method = MemoryInfo.class.getMethod("isShared");
      assertNotNull(method, "isShared method should exist");
      assertEquals(boolean.class, method.getReturnType(), "Should return boolean");
    }

    @Test
    @DisplayName("should have readMemory method")
    void shouldHaveReadMemoryMethod() throws NoSuchMethodException {
      final Method method = MemoryInfo.class.getMethod("readMemory", long.class, int.class);
      assertNotNull(method, "readMemory method should exist");
      assertEquals(byte[].class, method.getReturnType(), "Should return byte[]");
      assertEquals(2, method.getParameterCount(), "Should have 2 parameters");
    }

    @Test
    @DisplayName("should have getStatistics method")
    void shouldHaveGetStatisticsMethod() throws NoSuchMethodException {
      final Method method = MemoryInfo.class.getMethod("getStatistics");
      assertNotNull(method, "getStatistics method should exist");
      assertEquals(
          MemoryStatistics.class, method.getReturnType(), "Should return MemoryStatistics");
    }

    @Test
    @DisplayName("should have exactly seven methods")
    void shouldHaveExactlySevenMethods() {
      final Method[] methods = MemoryInfo.class.getDeclaredMethods();
      assertEquals(7, methods.length, "MemoryInfo should have exactly 7 methods");
    }
  }

  @Nested
  @DisplayName("MemoryStatistics Nested Interface Tests")
  class MemoryStatisticsInterfaceTests {

    @Test
    @DisplayName("should have MemoryStatistics nested interface")
    void shouldHaveMemoryStatisticsNestedInterface() {
      boolean hasMemoryStatistics = false;
      for (final Class<?> inner : MemoryInfo.class.getDeclaredClasses()) {
        if ("MemoryStatistics".equals(inner.getSimpleName()) && inner.isInterface()) {
          hasMemoryStatistics = true;
          break;
        }
      }
      assertTrue(hasMemoryStatistics, "Should have MemoryStatistics nested interface");
    }

    @Test
    @DisplayName("MemoryStatistics should have getTotalAllocations method")
    void memoryStatisticsShouldHaveGetTotalAllocationsMethod() throws NoSuchMethodException {
      final Method method = MemoryStatistics.class.getMethod("getTotalAllocations");
      assertNotNull(method, "getTotalAllocations method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryStatistics should have getCurrentUsage method")
    void memoryStatisticsShouldHaveGetCurrentUsageMethod() throws NoSuchMethodException {
      final Method method = MemoryStatistics.class.getMethod("getCurrentUsage");
      assertNotNull(method, "getCurrentUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryStatistics should have getPeakUsage method")
    void memoryStatisticsShouldHaveGetPeakUsageMethod() throws NoSuchMethodException {
      final Method method = MemoryStatistics.class.getMethod("getPeakUsage");
      assertNotNull(method, "getPeakUsage method should exist");
      assertEquals(long.class, method.getReturnType(), "Should return long");
    }

    @Test
    @DisplayName("MemoryStatistics should have exactly three methods")
    void memoryStatisticsShouldHaveExactlyThreeMethods() {
      final Method[] methods = MemoryStatistics.class.getDeclaredMethods();
      assertEquals(3, methods.length, "MemoryStatistics should have exactly 3 methods");
    }
  }

  @Nested
  @DisplayName("Mock Implementation Tests")
  class MockImplementationTests {

    @Test
    @DisplayName("mock memory info should return base address")
    void mockMemoryInfoShouldReturnBaseAddress() {
      final MockMemoryInfo info = new MockMemoryInfo(0x1000L, 65536L, 1, 16, false);

      assertEquals(0x1000L, info.getBaseAddress(), "Base address should match");
    }

    @Test
    @DisplayName("mock memory info should return size")
    void mockMemoryInfoShouldReturnSize() {
      final MockMemoryInfo info = new MockMemoryInfo(0x1000L, 65536L, 1, 16, false);

      assertEquals(65536L, info.getSize(), "Size should match");
    }

    @Test
    @DisplayName("mock memory info should return page count")
    void mockMemoryInfoShouldReturnPageCount() {
      final MockMemoryInfo info = new MockMemoryInfo(0x1000L, 65536L, 1, 16, false);

      assertEquals(1, info.getPageCount(), "Page count should match");
    }

    @Test
    @DisplayName("mock memory info should return max pages")
    void mockMemoryInfoShouldReturnMaxPages() {
      final MockMemoryInfo info = new MockMemoryInfo(0x1000L, 65536L, 1, 16, false);

      assertEquals(16, info.getMaxPages(), "Max pages should match");
    }

    @Test
    @DisplayName("mock memory info should return shared status")
    void mockMemoryInfoShouldReturnSharedStatus() {
      final MockMemoryInfo sharedInfo = new MockMemoryInfo(0L, 0L, 0, 0, true);
      final MockMemoryInfo nonSharedInfo = new MockMemoryInfo(0L, 0L, 0, 0, false);

      assertTrue(sharedInfo.isShared(), "Should be shared");
      assertFalse(nonSharedInfo.isShared(), "Should not be shared");
    }

    @Test
    @DisplayName("mock memory info should support unlimited max pages")
    void mockMemoryInfoShouldSupportUnlimitedMaxPages() {
      final MockMemoryInfo info = new MockMemoryInfo(0L, 0L, 0, -1, false);

      assertEquals(-1, info.getMaxPages(), "Max pages should be -1 for unlimited");
    }

    @Test
    @DisplayName("mock memory info should read memory content")
    void mockMemoryInfoShouldReadMemoryContent() {
      final MockMemoryInfo info = new MockMemoryInfo(0L, 256L, 1, 1, false);
      final byte[] testData = {0x01, 0x02, 0x03, 0x04};
      info.setMemoryContent(testData);

      final byte[] result = info.readMemory(0L, 4);

      assertArrayEquals(testData, result, "Memory content should match");
    }

    @Test
    @DisplayName("mock memory info should return statistics")
    void mockMemoryInfoShouldReturnStatistics() {
      final MockMemoryInfo info = new MockMemoryInfo(0L, 65536L, 1, 16, false);
      info.setStatistics(100, 32768, 65536);

      final MemoryStatistics stats = info.getStatistics();

      assertNotNull(stats, "Statistics should not be null");
      assertEquals(100, stats.getTotalAllocations(), "Total allocations should match");
      assertEquals(32768, stats.getCurrentUsage(), "Current usage should match");
      assertEquals(65536, stats.getPeakUsage(), "Peak usage should match");
    }

    @Test
    @DisplayName("mock statistics should track usage correctly")
    void mockStatisticsShouldTrackUsageCorrectly() {
      final MockMemoryStatistics stats = new MockMemoryStatistics(50, 16384, 32768);

      assertEquals(50, stats.getTotalAllocations(), "Allocations should match");
      assertEquals(16384, stats.getCurrentUsage(), "Current usage should match");
      assertEquals(32768, stats.getPeakUsage(), "Peak usage should match");
    }
  }

  /** Mock implementation of MemoryInfo for testing. */
  private static class MockMemoryInfo implements MemoryInfo {
    private final long baseAddress;
    private final long size;
    private final int pageCount;
    private final int maxPages;
    private final boolean shared;
    private byte[] memoryContent = new byte[0];
    private MockMemoryStatistics statistics = new MockMemoryStatistics(0, 0, 0);

    MockMemoryInfo(
        final long baseAddress,
        final long size,
        final int pageCount,
        final int maxPages,
        final boolean shared) {
      this.baseAddress = baseAddress;
      this.size = size;
      this.pageCount = pageCount;
      this.maxPages = maxPages;
      this.shared = shared;
    }

    @Override
    public long getBaseAddress() {
      return baseAddress;
    }

    @Override
    public long getSize() {
      return size;
    }

    @Override
    public int getPageCount() {
      return pageCount;
    }

    @Override
    public int getMaxPages() {
      return maxPages;
    }

    @Override
    public boolean isShared() {
      return shared;
    }

    @Override
    public byte[] readMemory(final long address, final int length) {
      if (memoryContent.length == 0) {
        return new byte[length];
      }
      return Arrays.copyOf(memoryContent, Math.min(length, memoryContent.length));
    }

    public void setMemoryContent(final byte[] content) {
      this.memoryContent = Arrays.copyOf(content, content.length);
    }

    @Override
    public MemoryStatistics getStatistics() {
      return statistics;
    }

    public void setStatistics(
        final long totalAllocations, final long currentUsage, final long peakUsage) {
      this.statistics = new MockMemoryStatistics(totalAllocations, currentUsage, peakUsage);
    }
  }

  /** Mock implementation of MemoryStatistics for testing. */
  private static class MockMemoryStatistics implements MemoryStatistics {
    private final long totalAllocations;
    private final long currentUsage;
    private final long peakUsage;

    MockMemoryStatistics(
        final long totalAllocations, final long currentUsage, final long peakUsage) {
      this.totalAllocations = totalAllocations;
      this.currentUsage = currentUsage;
      this.peakUsage = peakUsage;
    }

    @Override
    public long getTotalAllocations() {
      return totalAllocations;
    }

    @Override
    public long getCurrentUsage() {
      return currentUsage;
    }

    @Override
    public long getPeakUsage() {
      return peakUsage;
    }
  }
}
