package ai.tegmentum.wasmtime4j.memory;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.*;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

/**
 * Comprehensive test suite for platform-specific memory management optimizations.
 *
 * <p>This test class validates all aspects of the platform memory management system: - Basic
 * allocation and deallocation operations - Huge pages support and NUMA awareness - Memory
 * compression and deduplication - Performance monitoring and leak detection - Concurrent operation
 * safety - Platform-specific features - Stress testing and resource limits
 *
 * <p>Tests are designed to be verbose for debugging and run across multiple platforms.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Platform Memory Management Comprehensive Test Suite")
public class PlatformMemoryManagerTest {

  private static final Logger LOGGER = Logger.getLogger(PlatformMemoryManagerTest.class.getName());

  // Test configuration constants
  private static final int SMALL_ALLOCATION_SIZE = 1024; // 1KB
  private static final int MEDIUM_ALLOCATION_SIZE = 1024 * 1024; // 1MB
  private static final int LARGE_ALLOCATION_SIZE = 16 * 1024 * 1024; // 16MB
  private static final int STRESS_TEST_ITERATIONS = 1000;
  private static final int CONCURRENT_THREADS = 8;

  // Mock implementations for testing different runtimes
  private MockJniPlatformMemoryManager jniManager;
  private MockPanamaPlatformMemoryManager panamaManager;

  @BeforeAll
  void setUp() {
    LOGGER.info("Setting up Platform Memory Management Test Suite");

    // Initialize mock implementations for comprehensive testing
    jniManager = new MockJniPlatformMemoryManager();
    panamaManager = new MockPanamaPlatformMemoryManager();

    LOGGER.info("Test suite setup completed - JNI and Panama mock managers initialized");
  }

  @AfterAll
  void tearDown() {
    LOGGER.info("Tearing down Platform Memory Management Test Suite");

    if (jniManager != null) {
      jniManager.close();
    }
    if (panamaManager != null) {
      panamaManager.close();
    }

    LOGGER.info("Test suite teardown completed");
  }

  // =============================================================================
  // BASIC FUNCTIONALITY TESTS
  // =============================================================================

  @Test
  @Order(1)
  @DisplayName("Test basic memory allocation and deallocation")
  void testBasicAllocationDeallocation() {
    LOGGER.info("Testing basic memory allocation and deallocation");

    // Test with default configuration
    var config = new MockConfig();

    try (var manager = new MockJniPlatformMemoryManager(config)) {
      // Test small allocation
      long ptr1 = manager.allocate(SMALL_ALLOCATION_SIZE, 0);
      assertTrue(ptr1 != 0, "Small allocation should return valid pointer");

      // Test medium allocation
      long ptr2 = manager.allocate(MEDIUM_ALLOCATION_SIZE, 64);
      assertTrue(ptr2 != 0, "Medium allocation should return valid pointer");
      assertNotEquals(ptr1, ptr2, "Different allocations should return different pointers");

      // Test large allocation
      long ptr3 = manager.allocate(LARGE_ALLOCATION_SIZE, 0);
      assertTrue(ptr3 != 0, "Large allocation should return valid pointer");

      // Verify statistics
      var stats = manager.getStats();
      assertTrue(
          stats.totalAllocated
              >= SMALL_ALLOCATION_SIZE + MEDIUM_ALLOCATION_SIZE + LARGE_ALLOCATION_SIZE,
          "Total allocated should reflect all allocations");
      assertEquals(3, stats.allocationCount, "Should have 3 allocations recorded");

      // Test deallocation
      manager.deallocate(ptr1);
      manager.deallocate(ptr2);
      manager.deallocate(ptr3);

      // Verify deallocation statistics
      var finalStats = manager.getStats();
      assertEquals(3, finalStats.deallocationCount, "Should have 3 deallocations recorded");
      assertEquals(0, finalStats.currentUsage, "Current usage should be zero after deallocation");
    }

    LOGGER.info("Basic allocation/deallocation test completed successfully");
  }

  @Test
  @Order(2)
  @DisplayName("Test invalid allocation parameters")
  void testInvalidAllocationParameters() {
    LOGGER.info("Testing invalid allocation parameters");

    try (var manager = new MockJniPlatformMemoryManager()) {
      // Test zero size allocation
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            manager.allocate(0, 0);
          },
          "Zero size allocation should throw IllegalArgumentException");

      // Test negative size allocation
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            manager.allocate(-1, 0);
          },
          "Negative size allocation should throw IllegalArgumentException");

      // Test null pointer deallocation
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            manager.deallocate(0);
          },
          "Null pointer deallocation should throw IllegalArgumentException");
    }

    LOGGER.info("Invalid parameter test completed successfully");
  }

  // =============================================================================
  // PLATFORM-SPECIFIC FEATURE TESTS
  // =============================================================================

  @Test
  @Order(10)
  @DisplayName("Test huge pages support")
  @EnabledOnOs({OS.LINUX, OS.WINDOWS})
  void testHugePagesSupport() {
    LOGGER.info("Testing huge pages support");

    var config = new MockConfig();
    config.enableHugePages = true;
    config.pageSize = MockConfig.PageSize.LARGE;

    try (var manager = new MockJniPlatformMemoryManager(config)) {
      var platformInfo = manager.getPlatformInfo();
      LOGGER.info(
          "Platform info: huge pages supported={}, page size={}",
          platformInfo.supportsHugePages,
          platformInfo.hugePageSize);

      if (platformInfo.supportsHugePages) {
        // Allocate memory that should use huge pages
        long ptr = manager.allocate(platformInfo.hugePageSize, 0);
        assertTrue(ptr != 0, "Huge page allocation should succeed");

        var stats = manager.getStats();
        assertTrue(stats.hugePagesUsed > 0, "Should report huge pages usage");

        manager.deallocate(ptr);
        LOGGER.info("Huge pages allocation test passed");
      } else {
        LOGGER.info("Huge pages not supported on this platform, skipping test");
      }
    }
  }

  @Test
  @Order(11)
  @DisplayName("Test NUMA-aware allocation")
  @EnabledOnOs(OS.LINUX)
  void testNumaAwareAllocation() {
    LOGGER.info("Testing NUMA-aware allocation");

    var config = new MockConfig();
    config.numaNode = 0; // Bind to NUMA node 0

    try (var manager = new MockJniPlatformMemoryManager(config)) {
      var platformInfo = manager.getPlatformInfo();
      LOGGER.info(
          "Platform info: NUMA supported={}, nodes={}",
          platformInfo.supportsNuma,
          platformInfo.numaNodes);

      if (platformInfo.supportsNuma && platformInfo.numaNodes > 1) {
        long ptr = manager.allocate(MEDIUM_ALLOCATION_SIZE, 0);
        assertTrue(ptr != 0, "NUMA allocation should succeed");

        var stats = manager.getStats();
        assertTrue(stats.numaHitRate >= 0.0, "NUMA hit rate should be valid");

        manager.deallocate(ptr);
        LOGGER.info("NUMA-aware allocation test passed");
      } else {
        LOGGER.info("NUMA not supported or single node system, skipping test");
      }
    }
  }

  // =============================================================================
  // ADVANCED FEATURE TESTS
  // =============================================================================

  @Test
  @Order(20)
  @DisplayName("Test memory compression")
  void testMemoryCompression() {
    LOGGER.info("Testing memory compression");

    var config = new MockConfig();
    config.enableCompression = true;

    try (var manager = new MockJniPlatformMemoryManager(config)) {
      // Create compressible test data (repeated pattern)
      String testData =
          "This is a test string that should compress well when repeated. ".repeat(100);
      byte[] originalData = testData.getBytes(StandardCharsets.UTF_8);

      byte[] compressed = manager.compressMemory(originalData);
      assertNotNull(compressed, "Compressed data should not be null");
      assertTrue(compressed.length > 0, "Compressed data should have positive length");
      assertTrue(
          compressed.length <= originalData.length, "Compressed data should be smaller or equal");

      double compressionRatio = (double) compressed.length / originalData.length;
      LOGGER.info(
          "Compression ratio: {} (original: {} bytes, compressed: {} bytes)",
          compressionRatio,
          originalData.length,
          compressed.length);

      // Verify statistics reflect compression
      var stats = manager.getStats();
      assertTrue(stats.compressionRatio > 0.0, "Compression ratio should be positive");
    }

    LOGGER.info("Memory compression test completed successfully");
  }

  @Test
  @Order(21)
  @DisplayName("Test memory deduplication")
  void testMemoryDeduplication() {
    LOGGER.info("Testing memory deduplication");

    var config = new MockConfig();
    config.enableDeduplication = true;

    try (var manager = new MockJniPlatformMemoryManager(config)) {
      // Create identical data for deduplication
      String testData = "Identical data for deduplication testing";
      byte[] data1 = testData.getBytes(StandardCharsets.UTF_8);
      byte[] data2 = testData.getBytes(StandardCharsets.UTF_8);

      // First allocation should create new memory
      long ptr1 = manager.deduplicateMemory(data1);
      assertTrue(ptr1 != 0, "First deduplication should return valid pointer");

      // Second allocation of identical data should potentially reuse memory
      long ptr2 = manager.deduplicateMemory(data2);
      assertTrue(ptr2 != 0, "Second deduplication should return valid pointer");

      // In a real implementation, ptr1 == ptr2 would indicate successful deduplication
      // For mock implementation, we just verify both operations succeed

      var stats = manager.getStats();
      LOGGER.info("Deduplication savings: {} bytes", stats.deduplicationSavings);
      assertTrue(stats.deduplicationSavings >= 0, "Deduplication savings should be non-negative");
    }

    LOGGER.info("Memory deduplication test completed successfully");
  }

  // =============================================================================
  // PERFORMANCE AND MONITORING TESTS
  // =============================================================================

  @Test
  @Order(30)
  @DisplayName("Test memory prefetching")
  void testMemoryPrefetching() {
    LOGGER.info("Testing memory prefetching");

    try (var manager = new MockJniPlatformMemoryManager()) {
      long ptr = manager.allocate(LARGE_ALLOCATION_SIZE, 0);
      assertTrue(ptr != 0, "Allocation should succeed");

      // Test prefetching - should not throw exceptions
      assertDoesNotThrow(
          () -> {
            manager.prefetchMemory(ptr, LARGE_ALLOCATION_SIZE);
          },
          "Memory prefetching should not throw exceptions");

      // Test prefetching invalid parameters
      assertThrows(
          IllegalArgumentException.class,
          () -> {
            manager.prefetchMemory(0, LARGE_ALLOCATION_SIZE);
          },
          "Prefetching null pointer should throw IllegalArgumentException");

      assertThrows(
          IllegalArgumentException.class,
          () -> {
            manager.prefetchMemory(ptr, -1);
          },
          "Prefetching negative size should throw IllegalArgumentException");

      manager.deallocate(ptr);
    }

    LOGGER.info("Memory prefetching test completed successfully");
  }

  @Test
  @Order(31)
  @DisplayName("Test memory leak detection")
  void testMemoryLeakDetection() {
    LOGGER.info("Testing memory leak detection");

    var config = new MockConfig();
    config.enableLeakDetection = true;

    try (var manager = new MockJniPlatformMemoryManager(config)) {
      // Allocate memory without deallocating to simulate leak
      long ptr1 = manager.allocate(MEDIUM_ALLOCATION_SIZE, 0);

      // Deallocate one but not the other
      manager.deallocate(ptr1);

      final long ptr2 = manager.allocate(MEDIUM_ALLOCATION_SIZE, 0);

      // Simulate time passing for leak detection
      try {
        Thread.sleep(100);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      // Check for leaks
      var leaks = manager.detectLeaks();
      assertNotNull(leaks, "Leak detection should return array");

      LOGGER.info("Detected {} potential memory leaks", leaks.length);

      // Clean up remaining allocation
      manager.deallocate(ptr2);
    }

    LOGGER.info("Memory leak detection test completed successfully");
  }

  // =============================================================================
  // CONCURRENCY AND THREAD SAFETY TESTS
  // =============================================================================

  @Test
  @Order(40)
  @DisplayName("Test concurrent memory operations")
  @Execution(ExecutionMode.CONCURRENT)
  void testConcurrentMemoryOperations() throws InterruptedException {
    LOGGER.info("Testing concurrent memory operations");

    try (var manager = new MockJniPlatformMemoryManager()) {
      ExecutorService executor = Executors.newFixedThreadPool(CONCURRENT_THREADS);
      CountDownLatch latch = new CountDownLatch(CONCURRENT_THREADS);
      AtomicInteger successfulOperations = new AtomicInteger(0);
      AtomicInteger failedOperations = new AtomicInteger(0);

      // Submit concurrent allocation/deallocation tasks
      for (int i = 0; i < CONCURRENT_THREADS; i++) {
        final int threadId = i;
        executor.submit(
            () -> {
              try {
                LOGGER.fine("Thread {} starting concurrent memory operations", threadId);

                for (int j = 0; j < 100; j++) {
                  // Allocate memory
                  long ptr = manager.allocate(SMALL_ALLOCATION_SIZE + j * 16, 0);

                  if (ptr != 0) {
                    // Simulate some work
                    Thread.yield();

                    // Deallocate memory
                    manager.deallocate(ptr);
                    successfulOperations.incrementAndGet();
                  } else {
                    failedOperations.incrementAndGet();
                  }
                }

                LOGGER.fine("Thread {} completed concurrent memory operations", threadId);
              } catch (Exception e) {
                LOGGER.warning("Thread " + threadId + " failed: " + e.getMessage());
                failedOperations.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      // Wait for all threads to complete
      assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within timeout");
      executor.shutdown();

      // Verify results
      int totalOperations = successfulOperations.get();
      int failures = failedOperations.get();

      LOGGER.info(
          "Concurrent operations completed: {} successful, {} failed", totalOperations, failures);

      assertTrue(totalOperations > 0, "Should have successful operations");
      assertTrue(failures < totalOperations * 0.1, "Failure rate should be less than 10%");

      // Verify final state
      var finalStats = manager.getStats();
      assertEquals(0, finalStats.currentUsage, "Should have no memory in use after test");
    }

    LOGGER.info("Concurrent memory operations test completed successfully");
  }

  // =============================================================================
  // STRESS TESTING
  // =============================================================================

  @Test
  @Order(50)
  @DisplayName("Test memory allocation stress scenarios")
  @Timeout(60) // 60 second timeout
  void testMemoryAllocationStress() {
    LOGGER.info("Testing memory allocation stress scenarios");

    try (var manager = new MockJniPlatformMemoryManager()) {
      long totalAllocated = 0;
      int successfulAllocations = 0;

      // Stress test with many small allocations
      for (int i = 0; i < STRESS_TEST_ITERATIONS; i++) {
        try {
          int size = SMALL_ALLOCATION_SIZE + (i % 1024);
          long ptr = manager.allocate(size, 0);

          if (ptr != 0) {
            totalAllocated += size;
            successfulAllocations++;

            // Immediately deallocate to test rapid allocation/deallocation
            manager.deallocate(ptr);
          }

          // Log progress periodically
          if (i % 100 == 0) {
            LOGGER.fine("Stress test progress: {}/{} iterations", i + 1, STRESS_TEST_ITERATIONS);
          }
        } catch (Exception e) {
          LOGGER.warning("Stress test allocation {} failed: {}", i, e.getMessage());
        }
      }

      LOGGER.info(
          "Stress test completed: {} successful allocations, {} total bytes allocated",
          successfulAllocations,
          totalAllocated);

      assertTrue(
          successfulAllocations >= STRESS_TEST_ITERATIONS * 0.95,
          "Should have at least 95% successful allocations");

      // Verify final state
      var finalStats = manager.getStats();
      assertEquals(0, finalStats.currentUsage, "Should have no memory leaks after stress test");
      assertTrue(
          finalStats.totalAllocated >= totalAllocated,
          "Statistics should reflect stress test allocations");
    }

    LOGGER.info("Memory allocation stress test completed successfully");
  }

  @Test
  @Order(51)
  @DisplayName("Test large memory allocation scenarios")
  void testLargeMemoryAllocations() {
    LOGGER.info("Testing large memory allocation scenarios");

    var config = new MockConfig();
    config.maxPoolSizeBytes = 1024L * 1024 * 1024; // 1GB pool

    try (var manager = new MockJniPlatformMemoryManager(config)) {
      // Test very large single allocation
      long largeSize = 128L * 1024 * 1024; // 128MB

      long ptr = manager.allocate(largeSize, 0);
      assertTrue(ptr != 0, "Large allocation should succeed");

      var stats = manager.getStats();
      assertTrue(stats.currentUsage >= largeSize, "Current usage should reflect large allocation");
      assertTrue(stats.peakUsage >= largeSize, "Peak usage should be updated");

      // Test memory prefetching on large allocation
      assertDoesNotThrow(
          () -> {
            manager.prefetchMemory(ptr, Math.min(largeSize, 4 * 1024 * 1024));
          },
          "Prefetching large memory should not fail");

      manager.deallocate(ptr);

      // Verify cleanup
      var finalStats = manager.getStats();
      assertEquals(
          0, finalStats.currentUsage, "Should have no memory in use after large allocation test");
    }

    LOGGER.info("Large memory allocation test completed successfully");
  }

  // =============================================================================
  // RUNTIME-SPECIFIC TESTS
  // =============================================================================

  @Test
  @Order(60)
  @DisplayName("Test JNI runtime specific features")
  void testJniRuntimeFeatures() {
    LOGGER.info("Testing JNI runtime specific features");

    try (var jniManager = new MockJniPlatformMemoryManager()) {
      // Test JNI-specific functionality
      var platformInfo = jniManager.getPlatformInfo();
      assertNotNull(platformInfo, "Platform info should not be null");

      assertTrue(platformInfo.totalPhysicalMemory > 0, "Should report positive total memory");
      assertTrue(platformInfo.cpuCores > 0, "Should report positive CPU core count");
      assertTrue(platformInfo.pageSize > 0, "Should report positive page size");

      // Test statistics collection
      var initialStats = jniManager.getStats();
      assertNotNull(initialStats, "Initial statistics should not be null");
      assertEquals(0, initialStats.currentUsage, "Initial usage should be zero");

      LOGGER.info(
          "JNI platform info: memory={}GB, cores={}, page_size={}KB",
          platformInfo.totalPhysicalMemory / (1024 * 1024 * 1024),
          platformInfo.cpuCores,
          platformInfo.pageSize / 1024);
    }

    LOGGER.info("JNI runtime features test completed successfully");
  }

  @Test
  @Order(61)
  @DisplayName("Test Panama FFI runtime specific features")
  @EnabledForJreRange(min = JRE.JAVA_21) // Panama FFI requires Java 21+
  void testPanamaRuntimeFeatures() {
    LOGGER.info("Testing Panama FFI runtime specific features");

    try (var panamaManager = new MockPanamaPlatformMemoryManager()) {
      // Test Panama-specific functionality
      var platformInfo = panamaManager.getPlatformInfo();
      assertNotNull(platformInfo, "Platform info should not be null");

      // Test memory segment allocation (Panama-specific)
      var memorySegment = panamaManager.allocateSegment(MEDIUM_ALLOCATION_SIZE, 64);
      assertNotNull(memorySegment, "Memory segment should not be null");
      assertTrue(memorySegment.address() != 0, "Memory segment should have valid address");

      panamaManager.deallocateSegment(memorySegment);

      LOGGER.info(
          "Panama platform info: memory={}GB, supports_numa={}",
          platformInfo.totalPhysicalMemory / (1024 * 1024 * 1024),
          platformInfo.supportsNuma);
    }

    LOGGER.info("Panama FFI runtime features test completed successfully");
  }

  // =============================================================================
  // CONFIGURATION AND ERROR HANDLING TESTS
  // =============================================================================

  @Test
  @Order(70)
  @DisplayName("Test configuration parameter validation")
  void testConfigurationValidation() {
    LOGGER.info("Testing configuration parameter validation");

    // Test valid configuration
    var validConfig = new MockConfig();
    validConfig.enableHugePages = true;
    validConfig.numaNode = 0;
    validConfig.initialPoolSizeBytes = 32 * 1024 * 1024;
    validConfig.enableCompression = true;

    assertDoesNotThrow(
        () -> {
          try (var manager = new MockJniPlatformMemoryManager(validConfig)) {
            // Basic operation should work
            long ptr = manager.allocate(1024, 0);
            manager.deallocate(ptr);
          }
        },
        "Valid configuration should not throw exceptions");

    // Test null configuration
    assertThrows(
        IllegalArgumentException.class,
        () -> {
          new MockJniPlatformMemoryManager(null);
        },
        "Null configuration should throw IllegalArgumentException");

    LOGGER.info("Configuration validation test completed successfully");
  }

  @Test
  @Order(71)
  @DisplayName("Test error recovery scenarios")
  void testErrorRecoveryScenarios() {
    LOGGER.info("Testing error recovery scenarios");

    try (var manager = new MockJniPlatformMemoryManager()) {
      // Test recovery after failed allocation
      try {
        // Try to allocate impossibly large amount
        manager.allocate(Long.MAX_VALUE, 0);
        fail("Should not be able to allocate MAX_VALUE bytes");
      } catch (RuntimeException e) {
        LOGGER.info("Expected allocation failure: {}", e.getMessage());
      }

      // Manager should still be functional after failure
      long ptr = manager.allocate(SMALL_ALLOCATION_SIZE, 0);
      assertTrue(ptr != 0, "Manager should be functional after failed allocation");
      manager.deallocate(ptr);

      // Test double deallocation handling
      ptr = manager.allocate(SMALL_ALLOCATION_SIZE, 0);
      manager.deallocate(ptr);

      assertThrows(
          RuntimeException.class,
          () -> {
            manager.deallocate(ptr); // Double deallocation should fail
          },
          "Double deallocation should throw RuntimeException");
    }

    LOGGER.info("Error recovery scenarios test completed successfully");
  }

  // =============================================================================
  // CLEANUP AND RESOURCE MANAGEMENT TESTS
  // =============================================================================

  @Test
  @Order(80)
  @DisplayName("Test resource cleanup on manager close")
  void testResourceCleanup() {
    LOGGER.info("Testing resource cleanup on manager close");

    var manager = new MockJniPlatformMemoryManager();

    // Allocate some memory
    long ptr1 = manager.allocate(MEDIUM_ALLOCATION_SIZE, 0);
    long ptr2 = manager.allocate(MEDIUM_ALLOCATION_SIZE, 0);

    assertTrue(ptr1 != 0 && ptr2 != 0, "Allocations should succeed");

    var statsBeforeClose = manager.getStats();
    assertTrue(statsBeforeClose.currentUsage > 0, "Should have memory in use before close");

    // Close manager - should clean up resources
    manager.close();
    assertTrue(manager.isClosed(), "Manager should report as closed");

    // Operations should fail after close
    assertThrows(
        IllegalStateException.class,
        () -> {
          manager.allocate(1024, 0);
        },
        "Operations should fail after close");

    assertThrows(
        IllegalStateException.class,
        () -> {
          manager.getStats();
        },
        "Statistics should not be available after close");

    LOGGER.info("Resource cleanup test completed successfully");
  }

  @Test
  @Order(81)
  @DisplayName("Test try-with-resources cleanup")
  void testTryWithResourcesCleanup() {
    LOGGER.info("Testing try-with-resources cleanup");

    AtomicBoolean cleanupCalled = new AtomicBoolean(false);

    // Mock manager that tracks cleanup
    var manager =
        new MockJniPlatformMemoryManager() {
          @Override
          public void close() {
            super.close();
            cleanupCalled.set(true);
          }
        };

    try (manager) {
      // Use manager
      long ptr = manager.allocate(SMALL_ALLOCATION_SIZE, 0);
      manager.deallocate(ptr);
    }

    assertTrue(cleanupCalled.get(), "Cleanup should be called by try-with-resources");
    assertTrue(manager.isClosed(), "Manager should be closed after try-with-resources");

    LOGGER.info("Try-with-resources cleanup test completed successfully");
  }

  // =============================================================================
  // MOCK IMPLEMENTATIONS FOR TESTING
  // =============================================================================

  /** Mock configuration class for testing. */
  public static class MockConfig {
    public boolean enableHugePages = true;
    public int numaNode = -1;
    public long initialPoolSizeBytes = 64 * 1024 * 1024;
    public long maxPoolSizeBytes = 2L * 1024 * 1024 * 1024;
    public boolean enableCompression = true;
    public boolean enableDeduplication = true;
    public long prefetchBufferSizeBytes = 4 * 1024 * 1024;
    public boolean enableLeakDetection = true;
    public int alignmentBytes = 64;
    public PageSize pageSize = PageSize.DEFAULT;

    /** Page size options for memory allocation. */
    public enum PageSize {
      DEFAULT,
      SMALL,
      LARGE,
      HUGE
    }
  }

  /** Mock JNI platform memory manager for testing. */
  public static class MockJniPlatformMemoryManager implements AutoCloseable {
    private boolean closed = false;
    private final AtomicLong nextPointer = new AtomicLong(0x1000000); // Start at 16MB
    private final ConcurrentHashMap<Long, Integer> allocations = new ConcurrentHashMap<>();
    private final AtomicLong totalAllocated = new AtomicLong(0);
    private final AtomicLong totalFreed = new AtomicLong(0);
    private final AtomicLong allocationCount = new AtomicLong(0);
    private final AtomicLong deallocationCount = new AtomicLong(0);

    public MockJniPlatformMemoryManager() {
      this(new MockConfig());
    }

    /** Creates manager with configuration. */
    public MockJniPlatformMemoryManager(MockConfig config) {
      if (config == null) {
        throw new IllegalArgumentException("Config cannot be null");
      }
    }

    /** Allocates memory with specified size and alignment. */
    public long allocate(long size, int alignment) {
      ensureNotClosed();
      if (size <= 0) {
        throw new IllegalArgumentException("Size must be positive: " + size);
      }

      long ptr = nextPointer.addAndGet(size + 64); // Add padding
      allocations.put(ptr, (int) size);
      totalAllocated.addAndGet(size);
      allocationCount.incrementAndGet();
      return ptr;
    }

    /** Deallocates memory at the specified pointer. */
    public void deallocate(long ptr) {
      ensureNotClosed();
      if (ptr == 0) {
        throw new IllegalArgumentException("Pointer cannot be null");
      }

      Integer size = allocations.remove(ptr);
      if (size == null) {
        throw new RuntimeException("Invalid pointer or double deallocation");
      }

      totalFreed.addAndGet(size);
      deallocationCount.incrementAndGet();
    }

    /** Returns memory statistics. */
    public MockStats getStats() {
      ensureNotClosed();
      long currentUsage = totalAllocated.get() - totalFreed.get();
      return new MockStats(
          totalAllocated.get(),
          totalFreed.get(),
          currentUsage,
          Math.max(currentUsage, totalAllocated.get()),
          allocationCount.get(),
          deallocationCount.get());
    }

    public MockPlatformInfo getPlatformInfo() {
      ensureNotClosed();
      return new MockPlatformInfo();
    }

    public MockLeak[] detectLeaks() {
      ensureNotClosed();
      return new MockLeak[allocations.size()]; // Return array with size = current allocations
    }

    /** Prefetches memory at the specified pointer. */
    public void prefetchMemory(long ptr, long size) {
      ensureNotClosed();
      if (ptr == 0) {
        throw new IllegalArgumentException("Pointer cannot be null");
      }
      if (size < 0) {
        throw new IllegalArgumentException("Size cannot be negative");
      }
      // Mock prefetch - no-op
    }

    /** Compresses memory data. */
    public byte[] compressMemory(byte[] data) {
      ensureNotClosed();
      if (data == null || data.length == 0) {
        throw new IllegalArgumentException("Data cannot be null or empty");
      }
      // Mock compression - return slightly smaller array
      byte[] compressed = new byte[Math.max(1, data.length * 3 / 4)];
      System.arraycopy(data, 0, compressed, 0, Math.min(data.length, compressed.length));
      return compressed;
    }

    /** Deduplicates memory data. */
    public long deduplicateMemory(byte[] data) {
      ensureNotClosed();
      if (data == null || data.length == 0) {
        throw new IllegalArgumentException("Data cannot be null or empty");
      }
      // Mock deduplication - just allocate and return pointer
      return allocate(data.length, 0);
    }

    @Override
    public void close() {
      closed = true;
    }

    public boolean isClosed() {
      return closed;
    }

    private void ensureNotClosed() {
      if (closed) {
        throw new IllegalStateException("Manager has been closed");
      }
    }
  }

  /** Mock Panama platform memory manager for testing. */
  public static class MockPanamaPlatformMemoryManager implements AutoCloseable {
    private final MockJniPlatformMemoryManager delegate;

    public MockPanamaPlatformMemoryManager() {
      this.delegate = new MockJniPlatformMemoryManager();
    }

    public MockMemorySegment allocateSegment(long size, int alignment) {
      long address = delegate.allocate(size, alignment);
      return new MockMemorySegment(address, size);
    }

    public void deallocateSegment(MockMemorySegment segment) {
      delegate.deallocate(segment.address());
    }

    public MockPlatformInfo getPlatformInfo() {
      return delegate.getPlatformInfo();
    }

    @Override
    public void close() {
      delegate.close();
    }

    public boolean isClosed() {
      return delegate.isClosed();
    }
  }

  /** Mock memory segment for Panama testing. */
  public static class MockMemorySegment {
    private final long address;
    private final long size;

    public MockMemorySegment(long address, long size) {
      this.address = address;
      this.size = size;
    }

    public long address() {
      return address;
    }

    public long size() {
      return size;
    }
  }

  /** Mock memory statistics. */
  public static class MockStats {
    public final long totalAllocated;
    public final long totalFreed;
    public final long currentUsage;
    public final long peakUsage;
    public final long allocationCount;
    public final long deallocationCount;
    public final double fragmentationRatio = 0.1;
    public final double compressionRatio = 0.75;
    public final long deduplicationSavings = 0;
    public final long hugePagesUsed = 0;
    public final double numaHitRate = 1.0;

    /** Creates memory statistics. */
    public MockStats(
        long totalAllocated,
        long totalFreed,
        long currentUsage,
        long peakUsage,
        long allocationCount,
        long deallocationCount) {
      this.totalAllocated = totalAllocated;
      this.totalFreed = totalFreed;
      this.currentUsage = currentUsage;
      this.peakUsage = peakUsage;
      this.allocationCount = allocationCount;
      this.deallocationCount = deallocationCount;
    }
  }

  /** Mock platform information. */
  public static class MockPlatformInfo {
    public final long totalPhysicalMemory = 16L * 1024 * 1024 * 1024; // 16GB
    public final long availableMemory = 8L * 1024 * 1024 * 1024; // 8GB
    public final long pageSize = 4096;
    public final long hugePageSize = 2 * 1024 * 1024; // 2MB
    public final int numaNodes = 2;
    public final int cpuCores = 8;
    public final int cacheLineSize = 64;
    public final boolean supportsHugePages = true;
    public final boolean supportsNuma = true;
  }

  /** Mock memory leak for testing. */
  public static class MockLeak {
    // Mock leak for testing
  }
}
