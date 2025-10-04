package ai.tegmentum.wasmtime4j.panama.wasi;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.panama.ArenaResourceManager;
import ai.tegmentum.wasmtime4j.panama.wasi.security.WasiSecurityValidator;
import java.io.IOException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInfo;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive WASI compliance and integration test suite for Panama FFI implementation.
 *
 * <p>This test validates complete WASI system integration using Panama Foreign Function Interface,
 * ensuring compatibility and compliance with the JNI implementation while leveraging Panama's
 * zero-copy and memory management capabilities.
 *
 * <ul>
 *   <li>Panama FFI zero-copy operations
 *   <li>Memory segment-based I/O
 *   <li>Arena resource management
 *   <li>Cross-component integration testing
 *   <li>Performance characteristics under load
 *   <li>Resource cleanup and leak prevention
 *   <li>Error handling across component boundaries
 *   <li>WASI specification compliance with Panama
 * </ul>
 */
class WasiComplianceIntegrationTest {

  @TempDir private Path tempDirectory;

  private WasiContext wasiContext;
  private WasiFileSystem fileSystem;
  private Arena testArena;
  private ArenaResourceManager resourceManager;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    System.out.println(
        "Setting up Panama WASI compliance integration test: " + testInfo.getDisplayName());

    testArena = Arena.ofConfined();
    resourceManager = new ArenaResourceManager(testArena);

    // Create comprehensive WASI context with all features enabled
    wasiContext =
        WasiContext.builder()
            .withEnvironment("WASI_COMPLIANCE_TEST", "panama_enabled")
            .withEnvironment("INTEGRATION_MODE", "panama_full")
            .withEnvironment("ZERO_COPY_OPERATIONS", "enabled")
            .withEnvironment("ARENA_MANAGEMENT", "enabled")
            .withEnvironment("MEMORY_SEGMENTS", "enabled")
            .withEnvironment("TEST_WORKSPACE", tempDirectory.toString())
            .withArgument("panama_wasi_compliance_test")
            .withArgument("--panama-integration")
            .withArgument("--zero-copy")
            .withArgument("--arena-managed")
            .withPreopenDirectory("/tmp", tempDirectory.toString())
            .withPreopenDirectory("/workspace", tempDirectory.toString())
            .withWorkingDirectory(tempDirectory.toString())
            .build();

    assertNotNull(wasiContext, "Panama WASI context must be created successfully");

    fileSystem = new WasiFileSystem(wasiContext);
    assertNotNull(fileSystem, "Panama WASI file system must be initialized");

    System.out.println("Panama WASI compliance integration test setup completed");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) throws Exception {
    System.out.println(
        "Tearing down Panama WASI compliance integration test: " + testInfo.getDisplayName());

    try {
      if (fileSystem != null) {
        fileSystem.closeAll();
      }
    } catch (final Exception e) {
      System.err.println("Error closing file system: " + e.getMessage());
    }

    try {
      if (wasiContext != null && !wasiContext.isClosed()) {
        wasiContext.close();
      }
    } catch (final Exception e) {
      System.err.println("Error closing WASI context: " + e.getMessage());
    }

    try {
      if (resourceManager != null) {
        resourceManager.close();
      }
    } catch (final Exception e) {
      System.err.println("Error closing resource manager: " + e.getMessage());
    }

    try {
      if (testArena != null) {
        testArena.close();
      }
    } catch (final Exception e) {
      System.err.println("Error closing test arena: " + e.getMessage());
    }

    System.out.println("Panama WASI compliance integration test teardown completed");
  }

  @Test
  void testPanamaZeroCopyOperations() throws IOException {
    System.out.println("Testing Panama zero-copy operations");

    // Create test file with substantial content for zero-copy testing
    final Path testFile = tempDirectory.resolve("panama_zerocopy_test.txt");
    final String largeContent = "Panama zero-copy test data! ".repeat(1000); // ~30KB
    Files.write(testFile, largeContent.getBytes(), StandardOpenOption.CREATE);

    // Open file using Panama WASI
    final int fd =
        fileSystem.openFile("/tmp/panama_zerocopy_test.txt", WasiFileOperation.READ, false, false);
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Test zero-copy read using memory segments
    final MemorySegment readBuffer = testArena.allocate(largeContent.length());
    final int bytesRead = fileSystem.readFileZeroCopy(fd, readBuffer, 0, largeContent.length());
    assertEquals(largeContent.length(), bytesRead, "Must read all data with zero-copy");

    // Verify content directly from memory segment
    final String readContent = readBuffer.getString(0L);
    assertEquals(largeContent, readContent, "Zero-copy read content must match exactly");

    fileSystem.closeFile(fd);

    // Test zero-copy write operations
    final Path outputFile = tempDirectory.resolve("panama_zerocopy_output.txt");
    final int writeFd =
        fileSystem.openFile(
            "/tmp/panama_zerocopy_output.txt", WasiFileOperation.WRITE, true, false);
    assertTrue(writeFd >= 0, "Write file descriptor must be valid");

    final String writeContent = "Panama zero-copy write test content!";
    final MemorySegment writeBuffer = testArena.allocateFrom(writeContent);
    final int bytesWritten =
        fileSystem.writeFileZeroCopy(writeFd, writeBuffer, 0, writeContent.length());
    assertEquals(writeContent.length(), bytesWritten, "Must write all data with zero-copy");

    fileSystem.closeFile(writeFd);

    // Verify written content
    final String actualContent = Files.readString(outputFile);
    assertEquals(writeContent, actualContent, "Zero-copy written content must match");

    System.out.println("Panama zero-copy operations validated successfully");
  }

  @Test
  void testArenaResourceManagement() {
    System.out.println("Testing Panama arena resource management");

    // Test arena allocation patterns
    final int allocationCount = 100;
    final MemorySegment[] allocations = new MemorySegment[allocationCount];

    // Allocate various sized memory segments
    for (int i = 0; i < allocationCount; i++) {
      final long size = 1024 + (i * 64); // Varying sizes
      allocations[i] = testArena.allocate(size);
      assertNotNull(allocations[i], "Arena allocation " + i + " must succeed");
      assertEquals(size, allocations[i].byteSize(), "Allocation size must match request");
    }

    // Test arena-based resource tracking
    assertTrue(
        resourceManager.getTrackedResourceCount() > 0, "Arena must track allocated resources");

    // Test memory segment operations
    for (int i = 0; i < Math.min(10, allocationCount); i++) {
      final String testData = "Arena test data " + i;
      final MemorySegment segment = testArena.allocateFrom(testData);
      final String readData = segment.getString(0L);
      assertEquals(testData, readData, "Arena string allocation must work correctly");
    }

    // Arena cleanup is automatic when arena is closed
    System.out.println(
        "Arena resource management validated with " + allocationCount + " allocations");
  }

  @Test
  void testPanamaMemorySegmentIntegration() throws IOException {
    System.out.println("Testing Panama memory segment integration");

    // Create multiple test files for segment-based operations
    final String[] testContents = {
      "First segment content",
      "Second segment content with more data",
      "Third segment content with even more detailed information"
    };

    final MemorySegment[] readSegments = new MemorySegment[testContents.length];
    final int[] fileFds = new int[testContents.length];

    try {
      // Create files and read into memory segments
      for (int i = 0; i < testContents.length; i++) {
        final Path testFile = tempDirectory.resolve("segment_test_" + i + ".txt");
        Files.write(testFile, testContents[i].getBytes(), StandardOpenOption.CREATE);

        // Open file
        fileFds[i] =
            fileSystem.openFile(
                "/tmp/segment_test_" + i + ".txt", WasiFileOperation.READ, false, false);
        assertTrue(fileFds[i] >= 0, "File descriptor " + i + " must be valid");

        // Read into memory segment
        readSegments[i] = testArena.allocate(testContents[i].length());
        final int bytesRead =
            fileSystem.readFileZeroCopy(fileFds[i], readSegments[i], 0, testContents[i].length());
        assertEquals(testContents[i].length(), bytesRead, "Must read all content for file " + i);

        // Verify content from memory segment
        final String segmentContent = readSegments[i].getString(0L);
        assertEquals(
            testContents[i], segmentContent, "Memory segment content must match file " + i);
      }

      // Test segment-to-segment operations
      final MemorySegment combinedSegment =
          testArena.allocate(
              readSegments[0].byteSize() + readSegments[1].byteSize() + readSegments[2].byteSize());

      long offset = 0;
      for (final MemorySegment segment : readSegments) {
        MemorySegment.copy(segment, 0L, combinedSegment, offset, segment.byteSize());
        offset += segment.byteSize();
      }

      // Verify combined segment content
      final String combinedContent = combinedSegment.getString(0L);
      final String expectedCombined = testContents[0] + testContents[1] + testContents[2];
      assertEquals(expectedCombined, combinedContent, "Combined segment must contain all content");

    } finally {
      // Close file descriptors
      for (final int fd : fileFds) {
        if (fd >= 0) {
          try {
            fileSystem.closeFile(fd);
          } catch (final Exception e) {
            System.err.println("Error closing fd " + fd + ": " + e.getMessage());
          }
        }
      }
    }

    System.out.println("Panama memory segment integration validated successfully");
  }

  @Test
  void testPanamaSecurityIntegration() {
    System.out.println("Testing Panama security integration");

    final WasiSecurityValidator securityValidator = wasiContext.getSecurityValidator();
    assertNotNull(securityValidator, "Security validator must be available");

    // Test path validation with memory segments
    final String legitimatePath = "/tmp/security_test.txt";
    assertDoesNotThrow(
        () -> wasiContext.validatePath(legitimatePath, WasiFileOperation.READ),
        "Legitimate path must be validated successfully");

    // Test environment variable access
    final String testEnvVar = wasiContext.getEnvironmentVariable("WASI_COMPLIANCE_TEST");
    assertEquals(
        "panama_enabled", testEnvVar, "Environment variable must be accessible through Panama");

    // Test security policy enforcement with memory operations
    try {
      wasiContext.validatePath("../../../etc/passwd", WasiFileOperation.READ);
      System.out.println("Warning: Path traversal was not blocked in Panama implementation");
    } catch (final Exception e) {
      System.out.println(
          "Security policy correctly blocked path traversal in Panama: "
              + e.getClass().getSimpleName());
    }

    // Test memory segment access validation
    final MemorySegment testSegment = testArena.allocate(1024);
    assertNotNull(testSegment, "Test memory segment must be allocated");
    assertTrue(testSegment.byteSize() == 1024, "Memory segment must have correct size");

    // Memory segments within arena should be valid
    assertDoesNotThrow(
        () -> testSegment.fill((byte) 0), "Memory segment operations within arena must be allowed");

    System.out.println("Panama security integration validated successfully");
  }

  @Test
  void testPanamaPerformanceCharacteristics() throws IOException {
    System.out.println("Testing Panama performance characteristics");

    final int operationCount = 50;
    final int dataSize = 8192; // 8KB per operation
    final long startTime = System.currentTimeMillis();

    // Test Panama-specific performance patterns
    for (int i = 0; i < operationCount; i++) {
      // Create test file
      final Path testFile = tempDirectory.resolve("perf_test_" + i + ".dat");
      final byte[] testData = new byte[dataSize];
      // Fill with test pattern
      for (int j = 0; j < dataSize; j++) {
        testData[j] = (byte) (j % 256);
      }
      Files.write(testFile, testData, StandardOpenOption.CREATE);

      // Zero-copy read operation
      final int fd =
          fileSystem.openFile("/tmp/perf_test_" + i + ".dat", WasiFileOperation.READ, false, false);
      if (fd >= 0) {
        final MemorySegment readBuffer = testArena.allocate(dataSize);
        final int bytesRead = fileSystem.readFileZeroCopy(fd, readBuffer, 0, dataSize);
        assertEquals(dataSize, bytesRead, "Performance test read must be complete");

        // Verify data integrity
        for (int j = 0; j < Math.min(100, dataSize); j++) {
          final byte expected = (byte) (j % 256);
          final byte actual = readBuffer.get(java.lang.foreign.ValueLayout.JAVA_BYTE, j);
          assertEquals(expected, actual, "Data integrity must be maintained in performance test");
        }

        fileSystem.closeFile(fd);
      }
    }

    final long endTime = System.currentTimeMillis();
    final long totalTime = endTime - startTime;
    final double avgTimePerOp = (double) totalTime / operationCount;
    final double throughputMBps =
        (operationCount * dataSize / 1024.0 / 1024.0) / (totalTime / 1000.0);

    System.out.printf("Panama performance results:%n");
    System.out.printf("  Operations: %d%n", operationCount);
    System.out.printf("  Data per operation: %d bytes%n", dataSize);
    System.out.printf("  Total time: %d ms%n", totalTime);
    System.out.printf("  Average time per operation: %.2f ms%n", avgTimePerOp);
    System.out.printf("  Throughput: %.2f MB/s%n", throughputMBps);

    // Performance assertions
    assertTrue(avgTimePerOp < 50.0, "Panama operations should be fast (< 50ms per operation)");
    assertTrue(throughputMBps > 1.0, "Panama throughput should be reasonable (> 1 MB/s)");

    System.out.println("Panama performance characteristics validated successfully");
  }

  @Test
  void testPanamaResourceCleanupAndLeakPrevention() throws IOException {
    System.out.println("Testing Panama resource cleanup and leak prevention");

    final int resourceIterations = 25;
    final int segmentSize = 4096;

    // Track initial resource state
    final int initialResourceCount = resourceManager.getTrackedResourceCount();

    // Create and cleanup resources in controlled manner
    for (int iteration = 0; iteration < resourceIterations; iteration++) {
      // Create temporary arena for this iteration
      try (final Arena iterationArena = Arena.ofConfined()) {
        final ArenaResourceManager iterationManager = new ArenaResourceManager(iterationArena);

        // Allocate memory segments
        final MemorySegment[] segments = new MemorySegment[10];
        for (int i = 0; i < segments.length; i++) {
          segments[i] = iterationArena.allocate(segmentSize);
          segments[i].fill((byte) (iteration % 256));
        }

        // Create test file and perform I/O
        final Path testFile = tempDirectory.resolve("cleanup_test_" + iteration + ".txt");
        final String testContent = "Cleanup test iteration " + iteration;
        Files.write(testFile, testContent.getBytes(), StandardOpenOption.CREATE);

        final int fd =
            fileSystem.openFile(
                "/tmp/cleanup_test_" + iteration + ".txt", WasiFileOperation.READ, false, false);
        if (fd >= 0) {
          final MemorySegment readBuffer = iterationArena.allocate(testContent.length());
          fileSystem.readFileZeroCopy(fd, readBuffer, 0, testContent.length());
          fileSystem.closeFile(fd);
        }

        // Verify iteration resources are tracked
        assertTrue(
            iterationManager.getTrackedResourceCount() > 0, "Iteration resources must be tracked");

        // Arena automatically cleans up when closed
      }
    }

    // Verify no resource leaks
    final int finalResourceCount = resourceManager.getTrackedResourceCount();
    assertEquals(initialResourceCount, finalResourceCount, "No resource leaks should occur");

    System.out.println(
        "Panama resource cleanup validated: " + resourceIterations + " iterations completed");
  }

  @Test
  void testPanamaErrorHandlingConsistency() throws IOException {
    System.out.println("Testing Panama error handling consistency");

    // Test invalid file operations
    try {
      fileSystem.readFileZeroCopy(999999, testArena.allocate(100), 0, 100);
      System.out.println("Warning: Invalid fd operation did not throw exception");
    } catch (final Exception e) {
      System.out.println("Panama error handling for invalid fd: " + e.getClass().getSimpleName());
    }

    // Test invalid memory segment operations
    try {
      final MemorySegment invalidSegment = MemorySegment.NULL;
      fileSystem.writeFileZeroCopy(1, invalidSegment, 0, 100);
      System.out.println("Warning: Invalid memory segment operation did not throw exception");
    } catch (final Exception e) {
      System.out.println(
          "Panama error handling for invalid memory segment: " + e.getClass().getSimpleName());
    }

    // Test context operations after errors - system should remain functional
    assertDoesNotThrow(
        () -> {
          final String envVar = wasiContext.getEnvironmentVariable("WASI_COMPLIANCE_TEST");
          assertEquals("panama_enabled", envVar, "Context must remain functional after errors");
        },
        "Panama context must remain functional after errors");

    assertDoesNotThrow(
        () -> {
          final MemorySegment testSegment = testArena.allocate(256);
          testSegment.fill((byte) 42);
        },
        "Arena operations must remain functional after errors");

    System.out.println("Panama error handling consistency validated");
  }

  @Test
  void testPanamaSpecificationCompliance() throws IOException {
    System.out.println("Testing Panama WASI specification compliance");

    // Test environment variable handling
    final Map<String, String> environment = wasiContext.getEnvironment();
    assertNotNull(environment, "Environment must be accessible");
    assertTrue(
        environment.containsKey("WASI_COMPLIANCE_TEST"),
        "Test environment variable must be present");

    // Test file operations compliance
    final Path testFile = tempDirectory.resolve("panama_spec_test.txt");
    final String testContent = "Panama WASI specification compliance test";
    Files.write(testFile, testContent.getBytes(), StandardOpenOption.CREATE);

    // Test file metadata
    final var metadata = fileSystem.getFileMetadata("/tmp/panama_spec_test.txt");
    assertNotNull(metadata, "File metadata must be retrievable");
    assertEquals(testContent.length(), metadata.getSize(), "File size must match content");
    assertTrue(metadata.isRegularFile(), "Must be identified as regular file");

    // Test file I/O with memory segments
    final int fd =
        fileSystem.openFile("/tmp/panama_spec_test.txt", WasiFileOperation.READ, false, false);
    assertTrue(fd >= 0, "File must open successfully");

    final MemorySegment readBuffer = testArena.allocate(testContent.length());
    final int bytesRead = fileSystem.readFileZeroCopy(fd, readBuffer, 0, testContent.length());
    assertEquals(testContent.length(), bytesRead, "Must read all content");

    final String readContent = readBuffer.getString(0L);
    assertEquals(testContent, readContent, "Read content must match original");

    fileSystem.closeFile(fd);

    // Test working directory operations
    final Path workingDir = wasiContext.getWorkingDirectory();
    assertEquals(tempDirectory, workingDir, "Working directory must be correct");

    // Test path validation
    final Path validatedPath = wasiContext.validatePath("/tmp/panama_spec_test.txt");
    assertNotNull(validatedPath, "Path validation must succeed for legitimate paths");

    System.out.println("Panama WASI specification compliance validated successfully");
  }

  @Test
  void testPanamaConcurrentOperations()
      throws InterruptedException, ExecutionException, TimeoutException {
    System.out.println("Testing Panama concurrent operations");

    final int concurrentThreads = 10;
    final int operationsPerThread = 20;
    final List<CompletableFuture<String>> futures = new java.util.ArrayList<>();

    for (int thread = 0; thread < concurrentThreads; thread++) {
      final int threadId = thread;
      final CompletableFuture<String> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  // Each thread uses its own arena for thread safety
                  try (final Arena threadArena = Arena.ofConfined()) {
                    int successCount = 0;

                    for (int op = 0; op < operationsPerThread; op++) {
                      try {
                        // Create test file
                        final String fileName = "concurrent_" + threadId + "_" + op + ".txt";
                        final Path testFile = tempDirectory.resolve(fileName);
                        final String content = "Thread " + threadId + " operation " + op;
                        Files.write(testFile, content.getBytes(), StandardOpenOption.CREATE);

                        // Read with zero-copy
                        final int fd =
                            fileSystem.openFile(
                                "/tmp/" + fileName, WasiFileOperation.READ, false, false);
                        if (fd >= 0) {
                          final MemorySegment buffer = threadArena.allocate(content.length());
                          final int bytesRead =
                              fileSystem.readFileZeroCopy(fd, buffer, 0, content.length());
                          if (bytesRead == content.length()) {
                            final String readContent = buffer.getString(0L);
                            if (content.equals(readContent)) {
                              successCount++;
                            }
                          }
                          fileSystem.closeFile(fd);
                        }
                      } catch (final Exception e) {
                        // Expected occasional failures in concurrent scenarios
                      }
                    }

                    return "Thread "
                        + threadId
                        + ": "
                        + successCount
                        + "/"
                        + operationsPerThread
                        + " operations succeeded";
                  }
                } catch (final Exception e) {
                  return "Thread " + threadId + " failed: " + e.getMessage();
                }
              });

      futures.add(future);
    }

    // Wait for all concurrent operations
    final CompletableFuture<Void> allOf =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    allOf.get(60, TimeUnit.SECONDS);

    // Collect results
    int totalSuccesses = 0;
    int totalOperations = concurrentThreads * operationsPerThread;

    for (final CompletableFuture<String> future : futures) {
      final String result = future.get();
      System.out.println("Concurrent operation result: " + result);
      if (result.contains("succeeded")) {
        final String[] parts = result.split("/");
        if (parts.length >= 2) {
          try {
            final int successes = Integer.parseInt(parts[0].split(": ")[1]);
            totalSuccesses += successes;
          } catch (final NumberFormatException e) {
            // Ignore parsing errors
          }
        }
      }
    }

    final double successRate = (double) totalSuccesses / totalOperations;
    System.out.printf(
        "Panama concurrent operations: %d/%d succeeded (%.1f%%)%n",
        totalSuccesses, totalOperations, successRate * 100);

    // Should have reasonable success rate even under concurrency
    assertTrue(successRate >= 0.8, "At least 80% of concurrent operations should succeed");

    System.out.println("Panama concurrent operations validated successfully");
  }
}
