package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.permission.WasiResourceLimits;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Comprehensive tests for WASI resource usage tracking and statistics collection.
 *
 * <p>These tests verify that the resource usage tracker correctly monitors memory allocations,
 * file system operations, CPU usage, and enforces resource limits while providing detailed
 * statistics and metrics.
 *
 * @since 1.0.0
 */
final class WasiResourceUsageTrackerTest {

  private WasiResourceUsageTracker tracker;
  private WasiResourceLimits defaultLimits;

  @BeforeEach
  void setUp() {
    defaultLimits =
        WasiResourceLimits.builder()
            .withMaxMemoryBytes(1024 * 1024) // 1 MB
            .withMaxFileDescriptors(100)
            .withMaxDiskReadsPerSecond(1000)
            .withMaxDiskWritesPerSecond(1000)
            .withMaxExecutionTime(Duration.ofSeconds(10))
            .build();

    tracker = new WasiResourceUsageTracker(defaultLimits, true); // Enable detailed tracking
  }

  @Test
  void testContextRegistration() {
    // Initially no contexts tracked
    assertEquals(0, tracker.getTrackedContextCount());

    // Register a context
    tracker.registerContext("test-context");
    assertEquals(1, tracker.getTrackedContextCount());

    // Verify global statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        tracker.getGlobalUsage();
    assertEquals(1, globalStats.getContextsRegistered());
    assertEquals(0, globalStats.getContextsUnregistered());
    assertEquals(1, globalStats.getActiveContexts());

    // Unregister the context
    tracker.unregisterContext("test-context");
    assertEquals(0, tracker.getTrackedContextCount());

    // Verify updated statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot updatedGlobalStats =
        tracker.getGlobalUsage();
    assertEquals(1, updatedGlobalStats.getContextsRegistered());
    assertEquals(1, updatedGlobalStats.getContextsUnregistered());
    assertEquals(0, updatedGlobalStats.getActiveContexts());
  }

  @Test
  void testMemoryAllocationTracking() {
    tracker.registerContext("memory-context");

    // Record memory allocation
    tracker.recordMemoryAllocation("memory-context", 1000);

    // Get context usage
    final WasiResourceUsageTracker.ContextResourceUsageSnapshot contextUsage =
        tracker.getContextUsage("memory-context");

    assertEquals(1000, contextUsage.getMemoryUsed());
    assertEquals(1000, contextUsage.getTotalMemoryAllocated());
    assertEquals(0, contextUsage.getTotalMemoryDeallocated());
    assertEquals(1, contextUsage.getMemoryAllocations());

    // Record memory deallocation
    tracker.recordMemoryDeallocation("memory-context", 500);

    final WasiResourceUsageTracker.ContextResourceUsageSnapshot updatedUsage =
        tracker.getContextUsage("memory-context");

    assertEquals(500, updatedUsage.getMemoryUsed());
    assertEquals(1000, updatedUsage.getTotalMemoryAllocated());
    assertEquals(500, updatedUsage.getTotalMemoryDeallocated());
    assertEquals(1, updatedUsage.getMemoryDeallocations());

    // Verify global statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        tracker.getGlobalUsage();
    assertEquals(500, globalStats.getCurrentMemoryUsage());
    assertEquals(1000, globalStats.getTotalMemoryAllocated());
    assertEquals(500, globalStats.getTotalMemoryDeallocated());
  }

  @Test
  void testMemoryLimitEnforcement() {
    tracker.registerContext("limit-context");

    // Allocate memory within limit
    tracker.recordMemoryAllocation("limit-context", 512 * 1024); // 512 KB

    // Attempt to exceed memory limit
    assertThrows(
        IllegalStateException.class,
        () -> tracker.recordMemoryAllocation("limit-context", 600 * 1024)); // Would exceed 1 MB

    // Verify global violation statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        tracker.getGlobalUsage();
    assertEquals(1, globalStats.getMemoryLimitViolations());
  }

  @Test
  void testFileSystemOperationTracking() {
    tracker.registerContext("fs-context");

    // Record file system operations
    tracker.recordFileSystemOperation("fs-context", WasiFileOperation.READ, 1024, 1000000); // 1ms
    tracker.recordFileSystemOperation("fs-context", WasiFileOperation.WRITE, 512, 2000000); // 2ms
    tracker.recordFileSystemOperation("fs-context", WasiFileOperation.OPEN, 0, 500000); // 0.5ms
    tracker.recordFileSystemOperation("fs-context", WasiFileOperation.CLOSE, 0, 300000); // 0.3ms

    // Get context usage
    final WasiResourceUsageTracker.ContextResourceUsageSnapshot contextUsage =
        tracker.getContextUsage("fs-context");

    assertEquals(1, contextUsage.getFileReadOperations());
    assertEquals(1, contextUsage.getFileWriteOperations());
    assertEquals(1, contextUsage.getFileOpenOperations());
    assertEquals(1, contextUsage.getFileCloseOperations());
    assertEquals(4, contextUsage.getTotalFileOperations());
    assertEquals(1024, contextUsage.getTotalBytesRead());
    assertEquals(512, contextUsage.getTotalBytesWritten());
    assertEquals(3800000, contextUsage.getTotalFileSystemDuration()); // 3.8ms total

    // Verify global statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        tracker.getGlobalUsage();
    assertEquals(4, globalStats.getTotalFileOperations());
    assertEquals(1024, globalStats.getTotalBytesRead());
    assertEquals(512, globalStats.getTotalBytesWritten());
  }

  @Test
  void testCpuTimeTracking() {
    tracker.registerContext("cpu-context");

    // Record CPU time usage
    tracker.recordCpuTime("cpu-context", 1000000000); // 1 second

    // Get context usage
    final WasiResourceUsageTracker.ContextResourceUsageSnapshot contextUsage =
        tracker.getContextUsage("cpu-context");

    assertEquals(1000000000, contextUsage.getTotalCpuTime());

    // Verify global statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        tracker.getGlobalUsage();
    assertEquals(1000000000, globalStats.getTotalCpuTime());
  }

  @Test
  void testCpuTimeLimitEnforcement() {
    // Create tracker with strict CPU limit
    final WasiResourceLimits strictLimits =
        WasiResourceLimits.builder()
            .withMaxCpuTime(Duration.ofSeconds(2))
            .build();

    final WasiResourceUsageTracker strictTracker =
        new WasiResourceUsageTracker(strictLimits);

    strictTracker.registerContext("cpu-limit-context");

    // Record CPU time within limit
    strictTracker.recordCpuTime("cpu-limit-context", 1000000000); // 1 second

    // Attempt to exceed CPU limit
    assertThrows(
        IllegalStateException.class,
        () ->
            strictTracker.recordCpuTime(
                "cpu-limit-context", 1500000000)); // Would exceed 2 seconds

    // Verify global violation statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        strictTracker.getGlobalUsage();
    assertEquals(1, globalStats.getCpuLimitViolations());
  }

  @Test
  void testExecutionTimeTracking() {
    tracker.registerContext("exec-context");

    // Record execution time
    tracker.recordExecutionTime("exec-context", 5000000000L); // 5 seconds

    // Get context usage
    final WasiResourceUsageTracker.ContextResourceUsageSnapshot contextUsage =
        tracker.getContextUsage("exec-context");

    assertEquals(5000000000L, contextUsage.getTotalExecutionTime());

    // Verify global statistics
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        tracker.getGlobalUsage();
    assertEquals(5000000000L, globalStats.getTotalExecutionTime());
  }

  @Test
  void testDiskRateLimitEnforcement() {
    // Create tracker with strict disk I/O limits
    final WasiResourceLimits strictLimits =
        WasiResourceLimits.builder()
            .withMaxDiskReadsPerSecond(5)
            .withMaxDiskWritesPerSecond(3)
            .build();

    final WasiResourceUsageTracker strictTracker =
        new WasiResourceUsageTracker(strictLimits);

    strictTracker.registerContext("rate-limit-context");

    // Perform operations within rate limit
    for (int i = 0; i < 3; i++) {
      strictTracker.recordFileSystemOperation(
          "rate-limit-context", WasiFileOperation.READ, 1024, 1000000);
    }

    // Should be able to perform write operations
    for (int i = 0; i < 2; i++) {
      strictTracker.recordFileSystemOperation(
          "rate-limit-context", WasiFileOperation.WRITE, 512, 1000000);
    }

    // Attempting to exceed rate limits should trigger violations
    // Note: This is a simplified test - in practice, rate limiting would be time-based
    for (int i = 0; i < 10; i++) {
      try {
        strictTracker.recordFileSystemOperation(
            "rate-limit-context", WasiFileOperation.READ, 1024, 1000000);
      } catch (final IllegalStateException e) {
        // Expected when rate limit is exceeded
        break;
      }
    }

    // Verify some violation occurred
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        strictTracker.getGlobalUsage();
    assertTrue(
        globalStats.getDiskReadLimitViolations() + globalStats.getDiskWriteLimitViolations() > 0);
  }

  @Test
  void testContextUsageSnapshot() {
    tracker.registerContext("snapshot-context");

    // Record various operations
    tracker.recordMemoryAllocation("snapshot-context", 2048);
    tracker.recordMemoryDeallocation("snapshot-context", 512);
    tracker.recordFileSystemOperation(
        "snapshot-context", WasiFileOperation.READ, 1024, 1000000);
    tracker.recordFileSystemOperation(
        "snapshot-context", WasiFileOperation.WRITE, 256, 500000);
    tracker.recordCpuTime("snapshot-context", 2000000000);
    tracker.recordExecutionTime("snapshot-context", 3000000000L);

    // Get snapshot
    final WasiResourceUsageTracker.ContextResourceUsageSnapshot snapshot =
        tracker.getContextUsage("snapshot-context");

    // Verify snapshot data
    assertEquals("snapshot-context", snapshot.getContextId());
    assertNotNull(snapshot.getCreationTime());
    assertNotNull(snapshot.getUptime());
    assertEquals(1536, snapshot.getMemoryUsed()); // 2048 - 512
    assertEquals(2048, snapshot.getTotalMemoryAllocated());
    assertEquals(512, snapshot.getTotalMemoryDeallocated());
    assertEquals(1, snapshot.getFileReadOperations());
    assertEquals(1, snapshot.getFileWriteOperations());
    assertEquals(2, snapshot.getTotalFileOperations());
    assertEquals(1024, snapshot.getTotalBytesRead());
    assertEquals(256, snapshot.getTotalBytesWritten());
    assertEquals(1500000, snapshot.getTotalFileSystemDuration());
    assertEquals(2000000000, snapshot.getTotalCpuTime());
    assertEquals(3000000000L, snapshot.getTotalExecutionTime());

    // Verify string representation
    final String snapshotString = snapshot.toString();
    assertNotNull(snapshotString);
    assertTrue(snapshotString.contains("snapshot-context"));
    assertTrue(snapshotString.contains("memory=1536"));
    assertTrue(snapshotString.contains("fileOps=2"));
  }

  @Test
  void testGlobalResourceUsageSnapshot() {
    // Register multiple contexts and perform operations
    tracker.registerContext("global-context1");
    tracker.registerContext("global-context2");

    // Context 1 operations
    tracker.recordMemoryAllocation("global-context1", 1024);
    tracker.recordFileSystemOperation(
        "global-context1", WasiFileOperation.READ, 512, 1000000);
    tracker.recordCpuTime("global-context1", 1000000000);

    // Context 2 operations
    tracker.recordMemoryAllocation("global-context2", 2048);
    tracker.recordFileSystemOperation(
        "global-context2", WasiFileOperation.WRITE, 256, 500000);
    tracker.recordCpuTime("global-context2", 2000000000);

    // Get global snapshot
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalSnapshot =
        tracker.getGlobalUsage();

    // Verify global aggregated data
    assertEquals(2, globalSnapshot.getContextsRegistered());
    assertEquals(0, globalSnapshot.getContextsUnregistered());
    assertEquals(2, globalSnapshot.getActiveContexts());
    assertEquals(3072, globalSnapshot.getCurrentMemoryUsage()); // 1024 + 2048
    assertEquals(3072, globalSnapshot.getTotalMemoryAllocated());
    assertEquals(0, globalSnapshot.getTotalMemoryDeallocated());
    assertEquals(2, globalSnapshot.getTotalFileOperations());
    assertEquals(1, globalSnapshot.getTotalFileReadOperations());
    assertEquals(1, globalSnapshot.getTotalFileWriteOperations());
    assertEquals(512, globalSnapshot.getTotalBytesRead());
    assertEquals(256, globalSnapshot.getTotalBytesWritten());
    assertEquals(3000000000L, globalSnapshot.getTotalCpuTime()); // 1s + 2s
    assertEquals(0, globalSnapshot.getTotalLimitViolations());

    // Verify string representation
    final String globalString = globalSnapshot.toString();
    assertNotNull(globalString);
    assertTrue(globalString.contains("contexts=2"));
    assertTrue(globalString.contains("memory=3072"));
    assertTrue(globalString.contains("fileOps=2"));
    assertTrue(globalString.contains("violations=0"));
  }

  @Test
  void testUnknownContextHandling() {
    // Attempt operations on unknown context
    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordMemoryAllocation("unknown-context", 1024));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            tracker.recordFileSystemOperation(
                "unknown-context", WasiFileOperation.READ, 512, 1000000));

    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordCpuTime("unknown-context", 1000000000));

    assertThrows(
        IllegalArgumentException.class, () -> tracker.getContextUsage("unknown-context"));
  }

  @Test
  void testDetailedTrackingFlag() {
    // Test with detailed tracking disabled
    final WasiResourceUsageTracker basicTracker =
        new WasiResourceUsageTracker(defaultLimits, false);

    assertFalse(basicTracker.isDetailedTrackingEnabled());

    basicTracker.registerContext("basic-context");
    basicTracker.recordMemoryAllocation("basic-context", 1024);
    basicTracker.recordFileSystemOperation(
        "basic-context", WasiFileOperation.READ, 512, 1000000);

    // Basic tracking should still work, but some detailed metrics might not be collected
    final WasiResourceUsageTracker.ContextResourceUsageSnapshot usage =
        basicTracker.getContextUsage("basic-context");
    assertEquals(1024, usage.getMemoryUsed());
    assertEquals(512, usage.getTotalBytesRead());

    // Test with detailed tracking enabled
    assertTrue(tracker.isDetailedTrackingEnabled());
  }

  @Test
  void testResourceLimitsConfiguration() {
    final WasiResourceLimits limits = tracker.getResourceLimits();

    assertNotNull(limits);
    assertEquals(1024 * 1024, limits.getMaxMemoryBytes());
    assertEquals(100, limits.getMaxFileDescriptors());
    assertEquals(1000, limits.getMaxDiskReadsPerSecond());
    assertEquals(1000, limits.getMaxDiskWritesPerSecond());
    assertEquals(Duration.ofSeconds(10), limits.getMaxExecutionTime());
  }

  @Test
  void testInputValidation() {
    tracker.registerContext("validation-context");

    // Test null/invalid inputs
    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordMemoryAllocation(null, 1024));
    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordMemoryAllocation("", 1024));
    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordMemoryAllocation("validation-context", -1));

    assertThrows(
        IllegalArgumentException.class,
        () ->
            tracker.recordFileSystemOperation(
                null, WasiFileOperation.READ, 512, 1000000));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            tracker.recordFileSystemOperation(
                "validation-context", null, 512, 1000000));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            tracker.recordFileSystemOperation(
                "validation-context", WasiFileOperation.READ, -1, 1000000));
    assertThrows(
        IllegalArgumentException.class,
        () ->
            tracker.recordFileSystemOperation(
                "validation-context", WasiFileOperation.READ, 512, -1));

    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordCpuTime(null, 1000000000));
    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordCpuTime("validation-context", -1));

    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordExecutionTime(null, 1000000000));
    assertThrows(
        IllegalArgumentException.class,
        () -> tracker.recordExecutionTime("validation-context", -1));
  }

  @Test
  void testConcurrentContextOperations() throws InterruptedException {
    final int threadCount = 5;
    final Thread[] threads = new Thread[threadCount];

    // Create threads that perform operations on different contexts
    for (int i = 0; i < threadCount; i++) {
      final int threadId = i;
      threads[i] =
          new Thread(
              () -> {
                final String contextId = "concurrent-context-" + threadId;
                tracker.registerContext(contextId);

                // Perform various operations
                for (int j = 0; j < 100; j++) {
                  tracker.recordMemoryAllocation(contextId, 100);
                  tracker.recordFileSystemOperation(
                      contextId, WasiFileOperation.READ, 50, 100000);
                  tracker.recordCpuTime(contextId, 1000000);
                  tracker.recordExecutionTime(contextId, 2000000);

                  if (j % 2 == 0) {
                    tracker.recordMemoryDeallocation(contextId, 50);
                  }
                }

                tracker.unregisterContext(contextId);
              });
    }

    // Start all threads
    for (final Thread thread : threads) {
      thread.start();
    }

    // Wait for all threads to complete
    for (final Thread thread : threads) {
      thread.join();
    }

    // Verify final global state
    final WasiResourceUsageTracker.GlobalResourceUsageSnapshot globalStats =
        tracker.getGlobalUsage();
    assertEquals(threadCount, globalStats.getContextsRegistered());
    assertEquals(threadCount, globalStats.getContextsUnregistered());
    assertEquals(0, globalStats.getActiveContexts());
    assertTrue(globalStats.getTotalMemoryAllocated() > 0);
    assertTrue(globalStats.getTotalFileOperations() > 0);
    assertTrue(globalStats.getTotalCpuTime() > 0);
  }
}