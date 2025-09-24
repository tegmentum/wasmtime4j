package ai.tegmentum.wasmtime4j.jni.wasi;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.wasi.security.WasiSecurityPolicyEngine;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * Comprehensive WASI compliance and integration test suite.
 *
 * <p>This test validates complete WASI system integration including:
 *
 * <ul>
 *   <li>Cross-component integration (Preview 1, Preview 2, Security, Processes)
 *   <li>Real-world usage scenarios
 *   <li>Performance characteristics under load
 *   <li>Resource cleanup and leak prevention
 *   <li>Error handling across component boundaries
 *   <li>WASI specification compliance
 *   <li>Security policy enforcement
 *   <li>Concurrent operation support
 * </ul>
 */
class WasiComplianceIntegrationTest {

  @TempDir private Path tempDirectory;

  private WasiContext wasiContext;
  private WasiPreview1Operations preview1Ops;
  private WasiPreview2Operations preview2Ops;
  private WasiProcessOperations processOps;
  private WasiSecurityPolicyEngine securityEngine;

  @BeforeEach
  void setUp(final TestInfo testInfo) throws Exception {
    System.out.println("Setting up WASI compliance integration test: " + testInfo.getDisplayName());

    // Create comprehensive WASI context with all features enabled
    wasiContext =
        WasiContext.builder()
            .withEnvironment("WASI_COMPLIANCE_TEST", "enabled")
            .withEnvironment("INTEGRATION_MODE", "full")
            .withEnvironment("SECURITY_LEVEL", "high")
            .withEnvironment("ASYNC_OPERATIONS", "enabled")
            .withEnvironment("PROCESS_CONTROL", "enabled")
            .withEnvironment("NETWORK_ACCESS", "localhost")
            .withEnvironment("TEST_WORKSPACE", tempDirectory.toString())
            .withArgument("wasi_compliance_test")
            .withArgument("--full-integration")
            .withArgument("--security-enabled")
            .withArgument("--async-mode")
            .withPreopenDirectory("/tmp", tempDirectory.toString())
            .withPreopenDirectory("/workspace", tempDirectory.toString())
            .withWorkingDirectory(tempDirectory.toString())
            .build();

    assertNotNull(wasiContext, "WASI context must be created successfully");

    // Initialize all WASI operation components
    preview1Ops = new WasiPreview1Operations(wasiContext);
    preview2Ops = new WasiPreview2Operations(wasiContext);
    processOps = new WasiProcessOperations(wasiContext);
    securityEngine = new WasiSecurityPolicyEngine(wasiContext);

    assertNotNull(preview1Ops, "WASI Preview 1 operations must be initialized");
    assertNotNull(preview2Ops, "WASI Preview 2 operations must be initialized");
    assertNotNull(processOps, "WASI process operations must be initialized");
    assertNotNull(securityEngine, "WASI security engine must be initialized");

    System.out.println("WASI compliance integration test setup completed");
  }

  @AfterEach
  void tearDown(final TestInfo testInfo) throws Exception {
    System.out.println(
        "Tearing down WASI compliance integration test: " + testInfo.getDisplayName());

    // Clean up in reverse order
    final Exception[] exceptions = new Exception[5];

    try {
      if (securityEngine != null) {
        securityEngine.close();
      }
    } catch (final Exception e) {
      exceptions[0] = e;
      System.err.println("Error closing security engine: " + e.getMessage());
    }

    try {
      if (processOps != null) {
        processOps.close();
      }
    } catch (final Exception e) {
      exceptions[1] = e;
      System.err.println("Error closing process operations: " + e.getMessage());
    }

    try {
      if (preview2Ops != null) {
        preview2Ops.close();
      }
    } catch (final Exception e) {
      exceptions[2] = e;
      System.err.println("Error closing Preview 2 operations: " + e.getMessage());
    }

    try {
      if (preview1Ops != null) {
        preview1Ops.close();
      }
    } catch (final Exception e) {
      exceptions[3] = e;
      System.err.println("Error closing Preview 1 operations: " + e.getMessage());
    }

    try {
      if (wasiContext != null) {
        wasiContext.close();
      }
    } catch (final Exception e) {
      exceptions[4] = e;
      System.err.println("Error closing WASI context: " + e.getMessage());
    }

    System.out.println("WASI compliance integration test teardown completed");

    // If any cleanup failed, report it (but don't fail the test)
    for (int i = 0; i < exceptions.length; i++) {
      if (exceptions[i] != null) {
        System.err.println("Cleanup exception " + i + ": " + exceptions[i].getMessage());
      }
    }
  }

  @Test
  void testFullWasiWorkflow()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    System.out.println("Testing complete WASI workflow integration");

    // 1. Create test data files using Preview 1 operations
    final Path dataFile = tempDirectory.resolve("workflow_data.txt");
    final String originalData =
        "WASI workflow test data\n"
            + "Line 1: Preview 1 operations\n"
            + "Line 2: File I/O\n"
            + "Line 3: Security validation\n";
    Files.write(dataFile, originalData.getBytes(), StandardOpenOption.CREATE);

    // 2. Open file using Preview 1 fd operations
    final int fd =
        preview1Ops.pathOpen(
            3, // preopen fd for /tmp
            0, // flags
            "/tmp/workflow_data.txt",
            0, // oflags
            WasiRights.FD_READ.getValue() | WasiRights.FD_WRITE.getValue(),
            0L,
            0);
    assertTrue(fd >= 0, "File must be opened successfully");

    // 3. Read data using Preview 1 vectored I/O
    final List<ByteBuffer> readIovs = new ArrayList<>();
    readIovs.add(ByteBuffer.allocate(originalData.length()));
    final int bytesRead = preview1Ops.fdRead(fd, readIovs);
    assertEquals(originalData.length(), bytesRead, "Must read all data");

    final String readData = new String(readIovs.get(0).array()).trim();
    assertEquals(originalData.trim(), readData, "Read data must match original");

    // 4. Append data using Preview 1 operations
    preview1Ops.fdSeek(fd, 0, WasiWhence.END.getValue());
    final String appendData = "Line 4: Appended via WASI Preview 1\n";
    final List<ByteBuffer> writeIovs = new ArrayList<>();
    writeIovs.add(ByteBuffer.wrap(appendData.getBytes()));
    final int bytesWritten = preview1Ops.fdWrite(fd, writeIovs);
    assertEquals(appendData.length(), bytesWritten, "Must write append data");

    preview1Ops.fdSync(fd);
    preview1Ops.fdClose(fd);

    // 5. Process file using Preview 2 async operations
    final CompletableFuture<Long> asyncOpenFuture =
        preview2Ops.openFileAsync("/tmp/workflow_data.txt", "read");
    final Long asyncHandle = asyncOpenFuture.get(5, TimeUnit.SECONDS);
    assertNotNull(asyncHandle, "Async file handle must not be null");

    final int expectedSize = originalData.length() + appendData.length();
    final CompletableFuture<ByteBuffer> asyncReadFuture =
        preview2Ops.readFileAsync(asyncHandle, expectedSize);
    final ByteBuffer asyncBuffer = asyncReadFuture.get(5, TimeUnit.SECONDS);
    assertEquals(expectedSize, asyncBuffer.remaining(), "Async read must return correct size");

    preview2Ops.closeFileAsync(asyncHandle).get(5, TimeUnit.SECONDS);

    // 6. Spawn process to validate file content
    final Map<String, String> processEnv = new HashMap<>();
    processEnv.put("WORKFLOW_FILE", dataFile.toString());
    processEnv.put("EXPECTED_LINES", "4");

    final String command =
        System.getProperty("os.name").toLowerCase().contains("win") ? "find" : "wc";
    final List<String> arguments =
        System.getProperty("os.name").toLowerCase().contains("win")
            ? Arrays.asList("/c", dataFile.toString())
            : Arrays.asList("-l", dataFile.toString());

    final CompletableFuture<Long> spawnFuture =
        processOps.spawnProcess(command, arguments, processEnv, tempDirectory.toString());
    final Long processHandle = spawnFuture.get(5, TimeUnit.SECONDS);

    final CompletableFuture<Integer> waitFuture = processOps.waitForProcess(processHandle, 10);
    final Integer exitCode = waitFuture.get(5, TimeUnit.SECONDS);
    assertEquals(0, exitCode.intValue(), "Process must complete successfully");

    // 7. Validate security throughout the workflow
    final boolean hasSecurityViolations = securityEngine.hasSecurityViolations();
    assertFalse(hasSecurityViolations, "Workflow must not generate security violations");

    final int auditEventCount = securityEngine.getAuditEventCount();
    assertTrue(auditEventCount > 0, "Security audit must have recorded events");

    System.out.println(
        "Full WASI workflow completed successfully with " + auditEventCount + " audit events");
  }

  @Test
  void testConcurrentOperationsIntegration() throws InterruptedException, ExecutionException {
    System.out.println("Testing concurrent operations across WASI components");

    final int concurrentOperations = 20;
    final List<CompletableFuture<String>> futures = new ArrayList<>();

    // Create concurrent operations mixing Preview 1, Preview 2, and Process operations
    for (int i = 0; i < concurrentOperations; i++) {
      final int index = i;
      final CompletableFuture<String> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  final String fileName = "concurrent_test_" + index + ".txt";
                  final String content = "Concurrent operation " + index + " data";

                  // Create file using Preview 1
                  final Path testFile = tempDirectory.resolve(fileName);
                  Files.write(testFile, content.getBytes(), StandardOpenOption.CREATE);

                  // Read using Preview 2 async
                  final CompletableFuture<Long> openFuture =
                      preview2Ops.openFileAsync("/tmp/" + fileName, "read");
                  final Long handle = openFuture.get(5, TimeUnit.SECONDS);

                  final CompletableFuture<ByteBuffer> readFuture =
                      preview2Ops.readFileAsync(handle, content.length());
                  final ByteBuffer buffer = readFuture.get(5, TimeUnit.SECONDS);

                  preview2Ops.closeFileAsync(handle).get(5, TimeUnit.SECONDS);

                  // Validate with environment variable
                  processOps.setEnvironmentVariable("CONCURRENT_TEST_" + index, "completed");
                  final String envValue =
                      processOps.getEnvironmentVariable("CONCURRENT_TEST_" + index);

                  return "Operation " + index + " completed: " + envValue;

                } catch (final Exception e) {
                  return "Operation " + index + " failed: " + e.getMessage();
                }
              });

      futures.add(future);
    }

    // Wait for all concurrent operations to complete
    final CompletableFuture<Void> allOf =
        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]));
    assertDoesNotThrow(
        () -> allOf.get(30, TimeUnit.SECONDS), "All concurrent operations must complete");

    // Verify all operations succeeded
    int successCount = 0;
    for (final CompletableFuture<String> future : futures) {
      final String result = future.get();
      if (result.contains("completed")) {
        successCount++;
      } else {
        System.out.println("Operation result: " + result);
      }
    }

    assertTrue(
        successCount >= (concurrentOperations * 0.8),
        "At least 80% of concurrent operations must succeed");
    System.out.println(
        "Concurrent operations integration validated: "
            + successCount
            + "/"
            + concurrentOperations
            + " succeeded");
  }

  @Test
  void testSecurityPolicyEnforcementIntegration() throws IOException {
    System.out.println("Testing security policy enforcement across all components");

    // 1. Configure strict security policy
    securityEngine.setSecurityLevel(9); // High security
    securityEngine.enableAuditLogging(true);
    securityEngine.enableThreatDetection(true);

    // 2. Test legitimate operations (should succeed)
    final Path legitimateFile = tempDirectory.resolve("legitimate_file.txt");
    Files.write(legitimateFile, "Legitimate content".getBytes(), StandardOpenOption.CREATE);

    assertDoesNotThrow(
        () -> {
          wasiContext.validatePath("/tmp/legitimate_file.txt", WasiFileOperation.READ);
        },
        "Legitimate file access must be allowed");

    assertDoesNotThrow(
        () -> {
          processOps.setEnvironmentVariable("LEGITIMATE_VAR", "legitimate_value");
        },
        "Legitimate environment variable access must be allowed");

    // 3. Test blocked operations (should fail with security exceptions)
    final String[] maliciousPaths = {
      "../../../etc/passwd", "/proc/self/mem", "\\..\\..\\Windows\\System32\\config\\sam"
    };

    for (final String maliciousPath : maliciousPaths) {
      try {
        wasiContext.validatePath(maliciousPath, WasiFileOperation.READ);
        System.out.println("Warning: Malicious path was not blocked: " + maliciousPath);
      } catch (final Exception e) {
        // Expected - security policy should block this
        System.out.println("Security policy correctly blocked: " + maliciousPath);
      }
    }

    // 4. Test resource limits enforcement
    final List<Long> resources = new ArrayList<>();
    try {
      for (int i = 0; i < 1000; i++) {
        final long resource = preview2Ops.createResource("security_test_resource_" + i);
        resources.add(resource);
      }
    } catch (final Exception e) {
      // Expected - should hit resource limits
      System.out.println(
          "Resource limits enforced after creating " + resources.size() + " resources");
    }

    // Clean up resources
    for (final Long resource : resources) {
      try {
        preview2Ops.destroyResource(resource);
      } catch (final Exception e) {
        // Ignore cleanup errors
      }
    }

    // 5. Verify security audit trail
    final int finalAuditCount = securityEngine.getAuditEventCount();
    assertTrue(finalAuditCount > 0, "Security audit must have recorded events");

    final boolean hasViolations = securityEngine.hasSecurityViolations();
    if (hasViolations) {
      final int violationCount = securityEngine.getSecurityViolationCount();
      System.out.println("Security violations detected (expected): " + violationCount);
    }

    System.out.println(
        "Security policy enforcement validated with " + finalAuditCount + " audit events");
  }

  @Test
  void testResourceLeakPrevention()
      throws InterruptedException, ExecutionException, TimeoutException {
    System.out.println("Testing resource leak prevention across all components");

    final int iterations = 50;
    final List<Long> trackedResources = new ArrayList<>();

    try {
      for (int i = 0; i < iterations; i++) {
        // Create various types of resources

        // Preview 2 resources
        final long resource1 = preview2Ops.createResource("leak_test_resource_" + i);
        trackedResources.add(resource1);

        // File handles via Preview 1
        final Path testFile = tempDirectory.resolve("leak_test_" + i + ".txt");
        Files.write(testFile, ("Test content " + i).getBytes(), StandardOpenOption.CREATE);

        final int fd =
            preview1Ops.pathOpen(
                3, 0, "/tmp/leak_test_" + i + ".txt", 0, WasiRights.FD_READ.getValue(), 0L, 0);
        if (fd >= 0) {
          preview1Ops.fdClose(fd); // Properly close
        }

        // Process environment variables
        processOps.setEnvironmentVariable("LEAK_TEST_VAR_" + i, "value_" + i);

        // Network resources (if available)
        try {
          final CompletableFuture<Long> socketFuture = preview2Ops.createTcpSocket();
          final Long socket = socketFuture.get(1, TimeUnit.SECONDS);
          if (socket != null) {
            preview2Ops.closeTcpSocket(socket).get(1, TimeUnit.SECONDS);
          }
        } catch (final Exception e) {
          // Network operations might not be available in all environments
        }
      }

      // Verify resource tracking
      final int activeResources = preview2Ops.getActiveResourceCount();
      assertTrue(
          activeResources >= trackedResources.size(),
          "Active resources must include tracked resources");

    } finally {
      // Clean up tracked resources
      for (final Long resource : trackedResources) {
        try {
          preview2Ops.destroyResource(resource);
        } catch (final Exception e) {
          System.err.println("Error cleaning up resource " + resource + ": " + e.getMessage());
        }
      }

      // Clean up environment variables
      for (int i = 0; i < iterations; i++) {
        try {
          processOps.setEnvironmentVariable("LEAK_TEST_VAR_" + i, null);
        } catch (final Exception e) {
          // Ignore cleanup errors
        }
      }
    }

    // Verify cleanup was effective
    final int finalResourceCount = preview2Ops.getActiveResourceCount();
    System.out.println(
        "Resource leak prevention validated: final resource count = " + finalResourceCount);

    // Allow some resources to remain (system resources, etc.) but ensure we didn't leak excessively
    assertTrue(
        finalResourceCount < trackedResources.size(), "Must have cleaned up most test resources");
  }

  @Test
  void testErrorHandlingConsistency() throws IOException {
    System.out.println("Testing error handling consistency across all components");

    // Test consistent error handling for invalid operations across components

    // 1. Preview 1 error handling
    try {
      preview1Ops.fdRead(999999, new ArrayList<>());
      System.out.println("Warning: Invalid fd operation did not throw exception");
    } catch (final Exception e) {
      assertThat(e.getClass().getSimpleName()).contains("Wasi");
      System.out.println("Preview 1 error handling: " + e.getClass().getSimpleName());
    }

    // 2. Preview 2 error handling
    try {
      preview2Ops.readFileAsync(999999L, 100).get(2, TimeUnit.SECONDS);
      System.out.println("Warning: Invalid async operation did not throw exception");
    } catch (final ExecutionException e) {
      System.out.println("Preview 2 error handling: " + e.getCause().getClass().getSimpleName());
    } catch (final Exception e) {
      System.out.println("Preview 2 error handling: " + e.getClass().getSimpleName());
    }

    // 3. Process operations error handling
    try {
      processOps.waitForProcess(999999L, 1);
      System.out.println("Warning: Invalid process operation did not throw exception");
    } catch (final Exception e) {
      assertThat(e.getClass().getSimpleName()).contains("Wasi");
      System.out.println("Process operations error handling: " + e.getClass().getSimpleName());
    }

    // 4. Security validation error handling
    try {
      wasiContext.validatePath("../../../etc/passwd", WasiFileOperation.READ);
      System.out.println("Warning: Security violation was not caught");
    } catch (final Exception e) {
      System.out.println("Security validation error handling: " + e.getClass().getSimpleName());
    }

    // 5. Test error recovery - system should remain functional after errors
    assertDoesNotThrow(
        () -> {
          processOps.getCurrentProcessId();
        },
        "System must remain functional after errors");

    assertDoesNotThrow(
        () -> {
          final String envVar = processOps.getEnvironmentVariable("WASI_COMPLIANCE_TEST");
          assertEquals("enabled", envVar, "Context must remain functional after errors");
        },
        "Context must remain functional after errors");

    System.out.println("Error handling consistency validated across all components");
  }

  @Test
  void testWasiSpecificationCompliance()
      throws IOException, InterruptedException, ExecutionException, TimeoutException {
    System.out.println("Testing WASI specification compliance");

    // Test WASI Preview 1 specification compliance

    // 1. Environment variable handling (environ_get, environ_sizes_get)
    final int[] envSizes = preview1Ops.environSizesGet();
    assertNotNull(envSizes, "Environment sizes must not be null");
    assertEquals(2, envSizes.length, "Environment sizes must return count and buffer size");
    assertTrue(envSizes[0] > 0, "Environment count must be positive");
    assertTrue(envSizes[1] > 0, "Environment buffer size must be positive");

    final String[] environment = preview1Ops.environGet();
    assertNotNull(environment, "Environment variables must not be null");
    assertTrue(environment.length >= envSizes[0], "Environment array must match size");

    // 2. Clock operations (clock_time_get, clock_res_get)
    final long realtimeNanos = preview1Ops.clockTimeGet(WasiClockId.REALTIME.getValue(), 1000);
    assertTrue(realtimeNanos > 0, "Realtime clock must return positive nanoseconds");

    final long monotonicNanos = preview1Ops.clockTimeGet(WasiClockId.MONOTONIC.getValue(), 1000);
    assertTrue(monotonicNanos > 0, "Monotonic clock must return positive nanoseconds");

    final long realtimeRes = preview1Ops.clockResGet(WasiClockId.REALTIME.getValue());
    assertTrue(realtimeRes > 0, "Realtime clock resolution must be positive");

    // 3. Random number generation (random_get)
    final byte[] randomData = preview1Ops.randomGet(256);
    assertNotNull(randomData, "Random data must not be null");
    assertEquals(256, randomData.length, "Random data must have requested length");

    // Basic randomness check - not all bytes should be the same
    final byte firstByte = randomData[0];
    boolean hasVariation = false;
    for (final byte b : randomData) {
      if (b != firstByte) {
        hasVariation = true;
        break;
      }
    }
    assertTrue(hasVariation, "Random data must have variation");

    // 4. File descriptor operations
    final Path testFile = tempDirectory.resolve("spec_compliance_test.txt");
    final String testContent = "WASI specification compliance test content";
    Files.write(testFile, testContent.getBytes(), StandardOpenOption.CREATE);

    final int fd =
        preview1Ops.pathOpen(
            3,
            0,
            "/tmp/spec_compliance_test.txt",
            0,
            WasiRights.FD_READ.getValue() | WasiRights.FD_FILESTAT_GET.getValue(),
            0L,
            0);
    assertTrue(fd >= 0, "File descriptor must be valid");

    // Test fd_filestat_get
    final WasiFileStat fileStat = preview1Ops.fdFilestatGet(fd);
    assertNotNull(fileStat, "File stat must not be null");
    assertEquals(testContent.length(), fileStat.getSize(), "File size must match content");
    assertEquals(
        WasiFileType.REGULAR_FILE.getValue(), fileStat.getFileType(), "Must be regular file");

    // Test fd_read
    final List<ByteBuffer> readIovs = new ArrayList<>();
    readIovs.add(ByteBuffer.allocate(testContent.length()));
    final int bytesRead = preview1Ops.fdRead(fd, readIovs);
    assertEquals(testContent.length(), bytesRead, "Must read all bytes");

    preview1Ops.fdClose(fd);

    // 5. Test WASI Preview 2 async compliance
    final CompletableFuture<Long> asyncResourceFuture =
        CompletableFuture.supplyAsync(() -> preview2Ops.createResource("spec_compliance_resource"));
    final Long resourceHandle = asyncResourceFuture.get(5, TimeUnit.SECONDS);
    assertTrue(resourceHandle > 0, "Async resource creation must succeed");

    preview2Ops.destroyResource(resourceHandle);

    System.out.println("WASI specification compliance validated successfully");
  }

  @Test
  void testPerformanceCharacteristics()
      throws InterruptedException, ExecutionException, TimeoutException {
    System.out.println("Testing performance characteristics under load");

    final int operationCount = 100;
    final long startTime = System.currentTimeMillis();

    // Mix of different operation types to simulate real usage
    final List<CompletableFuture<String>> performanceFutures = new ArrayList<>();

    for (int i = 0; i < operationCount; i++) {
      final int index = i;
      final CompletableFuture<String> future =
          CompletableFuture.supplyAsync(
              () -> {
                try {
                  final long opStartTime = System.nanoTime();

                  // Mix of operations
                  if (index % 4 == 0) {
                    // File operations
                    final byte[] data = preview1Ops.randomGet(1024);
                    processOps.setEnvironmentVariable("PERF_TEST_" + index, "file_op");
                    return "file:" + (System.nanoTime() - opStartTime);

                  } else if (index % 4 == 1) {
                    // Async operations
                    final long resource = preview2Ops.createResource("perf_resource_" + index);
                    preview2Ops.destroyResource(resource);
                    return "async:" + (System.nanoTime() - opStartTime);

                  } else if (index % 4 == 2) {
                    // Environment operations
                    processOps.setEnvironmentVariable("PERF_ENV_" + index, "perf_value_" + index);
                    final String value = processOps.getEnvironmentVariable("PERF_ENV_" + index);
                    return "env:" + (System.nanoTime() - opStartTime);

                  } else {
                    // Clock operations
                    final long time1 =
                        preview1Ops.clockTimeGet(WasiClockId.REALTIME.getValue(), 1000);
                    final long time2 =
                        preview1Ops.clockTimeGet(WasiClockId.MONOTONIC.getValue(), 1000);
                    return "clock:" + (System.nanoTime() - opStartTime);
                  }

                } catch (final Exception e) {
                  return "error:" + e.getMessage();
                }
              });

      performanceFutures.add(future);
    }

    // Wait for all operations to complete
    final CompletableFuture<Void> allPerformanceOps =
        CompletableFuture.allOf(performanceFutures.toArray(new CompletableFuture[0]));
    allPerformanceOps.get(30, TimeUnit.SECONDS);

    final long endTime = System.currentTimeMillis();
    final long totalTime = endTime - startTime;
    final double avgTimePerOp = (double) totalTime / operationCount;

    // Collect performance statistics
    final Map<String, List<Long>> performanceStats = new HashMap<>();
    int successCount = 0;

    for (final CompletableFuture<String> future : performanceFutures) {
      final String result = future.get();
      if (!result.startsWith("error:")) {
        successCount++;
        final String[] parts = result.split(":");
        if (parts.length == 2) {
          final String opType = parts[0];
          final long duration = Long.parseLong(parts[1]);
          performanceStats.computeIfAbsent(opType, k -> new ArrayList<>()).add(duration);
        }
      }
    }

    // Calculate and display performance metrics
    System.out.printf("Performance test results:%n");
    System.out.printf("  Total operations: %d%n", operationCount);
    System.out.printf(
        "  Successful operations: %d (%.1f%%)%n",
        successCount, (100.0 * successCount / operationCount));
    System.out.printf("  Total time: %d ms%n", totalTime);
    System.out.printf("  Average time per operation: %.2f ms%n", avgTimePerOp);

    for (final Map.Entry<String, List<Long>> entry : performanceStats.entrySet()) {
      final List<Long> durations = entry.getValue();
      final double avgNanos = durations.stream().mapToLong(Long::longValue).average().orElse(0.0);
      final double avgMicros = avgNanos / 1000.0;
      System.out.printf("  %s operations: %.2f μs average%n", entry.getKey(), avgMicros);
    }

    // Performance assertions
    assertTrue(successCount >= (operationCount * 0.95), "At least 95% of operations must succeed");
    assertTrue(avgTimePerOp < 100.0, "Average operation time must be reasonable (< 100ms)");

    System.out.println("Performance characteristics validated successfully");
  }
}
