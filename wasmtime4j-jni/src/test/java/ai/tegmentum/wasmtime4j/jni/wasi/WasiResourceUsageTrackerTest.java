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

package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiResourceUsageTracker.ContextResourceUsageSnapshot;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiResourceUsageTracker.GlobalResourceUsageSnapshot;
import ai.tegmentum.wasmtime4j.jni.wasi.permission.WasiResourceLimits;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for {@link WasiResourceUsageTracker}.
 */
@DisplayName("WasiResourceUsageTracker Tests")
class WasiResourceUsageTrackerTest {

  private WasiResourceLimits unlimitedLimits;
  private WasiResourceUsageTracker tracker;

  @BeforeEach
  void setUp() {
    unlimitedLimits = WasiResourceLimits.unlimitedLimits();
    tracker = new WasiResourceUsageTracker(unlimitedLimits, false);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiResourceUsageTracker should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiResourceUsageTracker.class.getModifiers()),
          "WasiResourceUsageTracker should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Should throw on null resource limits")
    void shouldThrowOnNullResourceLimits() {
      assertThrows(JniException.class,
          () -> new WasiResourceUsageTracker(null),
          "Should throw on null resource limits");
    }

    @Test
    @DisplayName("Should create tracker with resource limits")
    void shouldCreateTrackerWithResourceLimits() {
      final WasiResourceUsageTracker newTracker = new WasiResourceUsageTracker(unlimitedLimits);

      assertNotNull(newTracker, "Tracker should be created");
      assertFalse(newTracker.isDetailedTrackingEnabled(), "Detailed tracking should be disabled");
    }

    @Test
    @DisplayName("Should create tracker with detailed tracking")
    void shouldCreateTrackerWithDetailedTracking() {
      final WasiResourceUsageTracker newTracker = new WasiResourceUsageTracker(unlimitedLimits, true);

      assertNotNull(newTracker, "Tracker should be created");
      assertTrue(newTracker.isDetailedTrackingEnabled(), "Detailed tracking should be enabled");
    }
  }

  @Nested
  @DisplayName("registerContext Tests")
  class RegisterContextTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.registerContext(null),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.registerContext(""),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should register context successfully")
    void shouldRegisterContextSuccessfully() {
      tracker.registerContext("ctx1");

      assertEquals(1, tracker.getTrackedContextCount(), "Should have 1 tracked context");
    }
  }

  @Nested
  @DisplayName("unregisterContext Tests")
  class UnregisterContextTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.unregisterContext(null),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.unregisterContext(""),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should unregister context successfully")
    void shouldUnregisterContextSuccessfully() {
      tracker.registerContext("ctx1");
      assertEquals(1, tracker.getTrackedContextCount(), "Should have 1 tracked context");

      tracker.unregisterContext("ctx1");
      assertEquals(0, tracker.getTrackedContextCount(), "Should have 0 tracked contexts");
    }

    @Test
    @DisplayName("Should handle unregistering non-existent context gracefully")
    void shouldHandleUnregisteringNonExistentContextGracefully() {
      assertDoesNotThrow(
          () -> tracker.unregisterContext("nonexistent"),
          "Should handle non-existent context gracefully");
    }
  }

  @Nested
  @DisplayName("recordMemoryAllocation Tests")
  class RecordMemoryAllocationTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordMemoryAllocation(null, 100),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordMemoryAllocation("", 100),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on negative bytes")
    void shouldThrowOnNegativeBytes() {
      tracker.registerContext("ctx1");

      assertThrows(JniException.class,
          () -> tracker.recordMemoryAllocation("ctx1", -1),
          "Should throw on negative bytes");
    }

    @Test
    @DisplayName("Should throw for unknown context")
    void shouldThrowForUnknownContext() {
      assertThrows(IllegalArgumentException.class,
          () -> tracker.recordMemoryAllocation("unknown", 100),
          "Should throw for unknown context");
    }

    @Test
    @DisplayName("Should record memory allocation")
    void shouldRecordMemoryAllocation() {
      tracker.registerContext("ctx1");
      tracker.recordMemoryAllocation("ctx1", 1024);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(1024, usage.getMemoryUsed(), "Should record memory allocation");
    }

    @Test
    @DisplayName("Should accumulate memory allocations")
    void shouldAccumulateMemoryAllocations() {
      tracker.registerContext("ctx1");
      tracker.recordMemoryAllocation("ctx1", 1024);
      tracker.recordMemoryAllocation("ctx1", 2048);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(3072, usage.getMemoryUsed(), "Should accumulate allocations");
    }

    @Test
    @DisplayName("Should throw on memory limit exceeded")
    void shouldThrowOnMemoryLimitExceeded() {
      final WasiResourceLimits memoryLimits = WasiResourceLimits.builder()
          .withMaxMemoryBytes(1000L)
          .build();

      final WasiResourceUsageTracker limitedTracker =
          new WasiResourceUsageTracker(memoryLimits, false);
      limitedTracker.registerContext("ctx1");

      assertThrows(IllegalStateException.class,
          () -> limitedTracker.recordMemoryAllocation("ctx1", 2000),
          "Should throw on memory limit exceeded");
    }
  }

  @Nested
  @DisplayName("recordMemoryDeallocation Tests")
  class RecordMemoryDeallocationTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordMemoryDeallocation(null, 100),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordMemoryDeallocation("", 100),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on negative bytes")
    void shouldThrowOnNegativeBytes() {
      tracker.registerContext("ctx1");

      assertThrows(JniException.class,
          () -> tracker.recordMemoryDeallocation("ctx1", -1),
          "Should throw on negative bytes");
    }

    @Test
    @DisplayName("Should record memory deallocation")
    void shouldRecordMemoryDeallocation() {
      tracker.registerContext("ctx1");
      tracker.recordMemoryAllocation("ctx1", 1024);
      tracker.recordMemoryDeallocation("ctx1", 512);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(512, usage.getMemoryUsed(), "Should reduce memory usage");
    }
  }

  @Nested
  @DisplayName("recordFileSystemOperation Tests")
  class RecordFileSystemOperationTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordFileSystemOperation(null, WasiFileOperation.READ, 100, 1000),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordFileSystemOperation("", WasiFileOperation.READ, 100, 1000),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on null operation")
    void shouldThrowOnNullOperation() {
      tracker.registerContext("ctx1");

      assertThrows(JniException.class,
          () -> tracker.recordFileSystemOperation("ctx1", null, 100, 1000),
          "Should throw on null operation");
    }

    @Test
    @DisplayName("Should throw on negative bytes")
    void shouldThrowOnNegativeBytes() {
      tracker.registerContext("ctx1");

      assertThrows(JniException.class,
          () -> tracker.recordFileSystemOperation("ctx1", WasiFileOperation.READ, -1, 1000),
          "Should throw on negative bytes");
    }

    @Test
    @DisplayName("Should throw on negative duration")
    void shouldThrowOnNegativeDuration() {
      tracker.registerContext("ctx1");

      assertThrows(JniException.class,
          () -> tracker.recordFileSystemOperation("ctx1", WasiFileOperation.READ, 100, -1),
          "Should throw on negative duration");
    }

    @Test
    @DisplayName("Should record read operation")
    void shouldRecordReadOperation() {
      tracker.registerContext("ctx1");
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.READ, 1024, 1000);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(1, usage.getFileReadOperations(), "Should record read operation");
      assertEquals(1024, usage.getTotalBytesRead(), "Should record bytes read");
    }

    @Test
    @DisplayName("Should record write operation")
    void shouldRecordWriteOperation() {
      tracker.registerContext("ctx1");
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.WRITE, 2048, 2000);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(1, usage.getFileWriteOperations(), "Should record write operation");
      assertEquals(2048, usage.getTotalBytesWritten(), "Should record bytes written");
    }

    @Test
    @DisplayName("Should record open operation")
    void shouldRecordOpenOperation() {
      tracker.registerContext("ctx1");
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.OPEN, 0, 500);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(1, usage.getFileOpenOperations(), "Should record open operation");
    }

    @Test
    @DisplayName("Should record close operation")
    void shouldRecordCloseOperation() {
      tracker.registerContext("ctx1");
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.CLOSE, 0, 100);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(1, usage.getFileCloseOperations(), "Should record close operation");
    }
  }

  @Nested
  @DisplayName("recordCpuTime Tests")
  class RecordCpuTimeTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordCpuTime(null, 1000),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordCpuTime("", 1000),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on negative CPU time")
    void shouldThrowOnNegativeCpuTime() {
      tracker.registerContext("ctx1");

      assertThrows(JniException.class,
          () -> tracker.recordCpuTime("ctx1", -1),
          "Should throw on negative CPU time");
    }

    @Test
    @DisplayName("Should record CPU time")
    void shouldRecordCpuTime() {
      tracker.registerContext("ctx1");
      tracker.recordCpuTime("ctx1", 1000000);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(1000000, usage.getTotalCpuTime(), "Should record CPU time");
    }

    @Test
    @DisplayName("Should throw on CPU time limit exceeded")
    void shouldThrowOnCpuTimeLimitExceeded() {
      final WasiResourceLimits cpuLimits = WasiResourceLimits.builder()
          .withMaxCpuTime(Duration.ofMillis(1))
          .build();

      final WasiResourceUsageTracker limitedTracker =
          new WasiResourceUsageTracker(cpuLimits, false);
      limitedTracker.registerContext("ctx1");

      assertThrows(IllegalStateException.class,
          () -> limitedTracker.recordCpuTime("ctx1", 10_000_000_000L), // 10 seconds
          "Should throw on CPU time limit exceeded");
    }
  }

  @Nested
  @DisplayName("recordExecutionTime Tests")
  class RecordExecutionTimeTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordExecutionTime(null, 1000),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.recordExecutionTime("", 1000),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw on negative execution time")
    void shouldThrowOnNegativeExecutionTime() {
      tracker.registerContext("ctx1");

      assertThrows(JniException.class,
          () -> tracker.recordExecutionTime("ctx1", -1),
          "Should throw on negative execution time");
    }

    @Test
    @DisplayName("Should record execution time")
    void shouldRecordExecutionTime() {
      tracker.registerContext("ctx1");
      tracker.recordExecutionTime("ctx1", 5000000);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      assertEquals(5000000, usage.getTotalExecutionTime(), "Should record execution time");
    }
  }

  @Nested
  @DisplayName("getContextUsage Tests")
  class GetContextUsageTests {

    @Test
    @DisplayName("Should throw on null context ID")
    void shouldThrowOnNullContextId() {
      assertThrows(JniException.class,
          () -> tracker.getContextUsage(null),
          "Should throw on null context ID");
    }

    @Test
    @DisplayName("Should throw on empty context ID")
    void shouldThrowOnEmptyContextId() {
      assertThrows(JniException.class,
          () -> tracker.getContextUsage(""),
          "Should throw on empty context ID");
    }

    @Test
    @DisplayName("Should throw for unknown context")
    void shouldThrowForUnknownContext() {
      assertThrows(IllegalArgumentException.class,
          () -> tracker.getContextUsage("unknown"),
          "Should throw for unknown context");
    }

    @Test
    @DisplayName("Should return context usage snapshot")
    void shouldReturnContextUsageSnapshot() {
      tracker.registerContext("ctx1");

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");

      assertNotNull(usage, "Usage should not be null");
      assertEquals("ctx1", usage.getContextId(), "Context ID should match");
      assertNotNull(usage.getCreationTime(), "Creation time should not be null");
    }
  }

  @Nested
  @DisplayName("getGlobalUsage Tests")
  class GetGlobalUsageTests {

    @Test
    @DisplayName("Should return global usage snapshot")
    void shouldReturnGlobalUsageSnapshot() {
      final GlobalResourceUsageSnapshot usage = tracker.getGlobalUsage();

      assertNotNull(usage, "Global usage should not be null");
    }

    @Test
    @DisplayName("Should track global statistics")
    void shouldTrackGlobalStatistics() {
      tracker.registerContext("ctx1");
      tracker.recordMemoryAllocation("ctx1", 1024);
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.READ, 512, 100);

      final GlobalResourceUsageSnapshot usage = tracker.getGlobalUsage();

      assertEquals(1, usage.getContextsRegistered(), "Should count registered contexts");
      assertEquals(1024, usage.getTotalMemoryAllocated(), "Should sum memory allocations");
      assertEquals(1, usage.getTotalFileReadOperations(), "Should count read operations");
    }
  }

  @Nested
  @DisplayName("ContextResourceUsageSnapshot Tests")
  class ContextResourceUsageSnapshotTests {

    @Test
    @DisplayName("Should calculate uptime")
    void shouldCalculateUptime() throws InterruptedException {
      tracker.registerContext("ctx1");
      Thread.sleep(100); // Wait a bit

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      final Duration uptime = usage.getUptime();

      assertTrue(uptime.toMillis() >= 100, "Uptime should be at least 100ms");
    }

    @Test
    @DisplayName("Should calculate total file operations")
    void shouldCalculateTotalFileOperations() {
      tracker.registerContext("ctx1");
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.READ, 100, 50);
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.WRITE, 200, 60);
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.OPEN, 0, 10);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");

      assertEquals(3, usage.getTotalFileOperations(), "Should sum all file operations");
    }

    @Test
    @DisplayName("toString should contain key fields")
    void toStringShouldContainKeyFields() {
      tracker.registerContext("ctx1");
      tracker.recordMemoryAllocation("ctx1", 1024);

      final ContextResourceUsageSnapshot usage = tracker.getContextUsage("ctx1");
      final String str = usage.toString();

      assertTrue(str.contains("ctx1"), "Should contain context ID");
      assertTrue(str.contains("memory="), "Should contain memory");
    }
  }

  @Nested
  @DisplayName("GlobalResourceUsageSnapshot Tests")
  class GlobalResourceUsageSnapshotTests {

    @Test
    @DisplayName("Should calculate active contexts")
    void shouldCalculateActiveContexts() {
      tracker.registerContext("ctx1");
      tracker.registerContext("ctx2");
      tracker.unregisterContext("ctx1");

      final GlobalResourceUsageSnapshot usage = tracker.getGlobalUsage();

      assertEquals(2, usage.getContextsRegistered(), "Should count all registered");
      assertEquals(1, usage.getContextsUnregistered(), "Should count unregistered");
      assertEquals(1, usage.getActiveContexts(), "Should calculate active correctly");
    }

    @Test
    @DisplayName("Should calculate current memory usage")
    void shouldCalculateCurrentMemoryUsage() {
      tracker.registerContext("ctx1");
      tracker.recordMemoryAllocation("ctx1", 2048);
      tracker.recordMemoryDeallocation("ctx1", 512);

      final GlobalResourceUsageSnapshot usage = tracker.getGlobalUsage();

      assertEquals(2048, usage.getTotalMemoryAllocated(), "Should track allocations");
      assertEquals(512, usage.getTotalMemoryDeallocated(), "Should track deallocations");
      assertEquals(1536, usage.getCurrentMemoryUsage(), "Should calculate current usage");
    }

    @Test
    @DisplayName("Should calculate total file operations")
    void shouldCalculateTotalFileOperations() {
      tracker.registerContext("ctx1");
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.READ, 100, 50);
      tracker.recordFileSystemOperation("ctx1", WasiFileOperation.WRITE, 200, 60);

      final GlobalResourceUsageSnapshot usage = tracker.getGlobalUsage();

      assertEquals(1, usage.getTotalFileReadOperations(), "Should count reads");
      assertEquals(1, usage.getTotalFileWriteOperations(), "Should count writes");
      assertEquals(2, usage.getTotalFileOperations(), "Should sum all operations");
    }

    @Test
    @DisplayName("Should calculate total limit violations")
    void shouldCalculateTotalLimitViolations() {
      final WasiResourceLimits memoryLimits = WasiResourceLimits.builder()
          .withMaxMemoryBytes(100L)
          .build();

      final WasiResourceUsageTracker limitedTracker =
          new WasiResourceUsageTracker(memoryLimits, false);
      limitedTracker.registerContext("ctx1");

      // Try to exceed limit
      try {
        limitedTracker.recordMemoryAllocation("ctx1", 200);
      } catch (IllegalStateException ignored) {
        // Expected
      }

      final GlobalResourceUsageSnapshot usage = limitedTracker.getGlobalUsage();

      assertTrue(usage.getMemoryLimitViolations() >= 1, "Should track memory violations");
      assertTrue(usage.getTotalLimitViolations() >= 1, "Should sum all violations");
    }

    @Test
    @DisplayName("toString should contain key fields")
    void toStringShouldContainKeyFields() {
      tracker.registerContext("ctx1");
      tracker.recordMemoryAllocation("ctx1", 1024);

      final GlobalResourceUsageSnapshot usage = tracker.getGlobalUsage();
      final String str = usage.toString();

      assertTrue(str.contains("contexts="), "Should contain contexts");
      assertTrue(str.contains("memory="), "Should contain memory");
      assertTrue(str.contains("violations="), "Should contain violations");
    }
  }

  @Nested
  @DisplayName("getResourceLimits Tests")
  class GetResourceLimitsTests {

    @Test
    @DisplayName("Should return resource limits")
    void shouldReturnResourceLimits() {
      final WasiResourceLimits limits = tracker.getResourceLimits();

      assertNotNull(limits, "Resource limits should not be null");
      assertEquals(unlimitedLimits, limits, "Should return same limits instance");
    }
  }

  @Nested
  @DisplayName("getTrackedContextCount Tests")
  class GetTrackedContextCountTests {

    @Test
    @DisplayName("Should return 0 initially")
    void shouldReturnZeroInitially() {
      assertEquals(0, tracker.getTrackedContextCount(), "Should have 0 tracked contexts initially");
    }

    @Test
    @DisplayName("Should count tracked contexts correctly")
    void shouldCountTrackedContextsCorrectly() {
      tracker.registerContext("ctx1");
      assertEquals(1, tracker.getTrackedContextCount(), "Should have 1 tracked context");

      tracker.registerContext("ctx2");
      assertEquals(2, tracker.getTrackedContextCount(), "Should have 2 tracked contexts");

      tracker.unregisterContext("ctx1");
      assertEquals(1, tracker.getTrackedContextCount(), "Should have 1 tracked context");
    }
  }
}
