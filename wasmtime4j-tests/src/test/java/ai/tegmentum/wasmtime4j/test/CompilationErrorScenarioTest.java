package ai.tegmentum.wasmtime4j.test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.WasmRuntime;
import ai.tegmentum.wasmtime4j.exception.CompilationException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

/**
 * Comprehensive test suite for WebAssembly compilation error scenarios.
 *
 * <p>This test class verifies proper error handling for various malformed WebAssembly modules,
 * ensuring that appropriate CompilationException instances are thrown with meaningful error
 * messages and proper context preservation.
 */
@DisplayName("Compilation Error Scenario Test Suite")
class CompilationErrorScenarioTest {

  /** Standard WebAssembly magic number and version for reference. */
  private static final byte[] VALID_WASM_HEADER = {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00};

  @ParameterizedTest
  @EnumSource(RuntimeType.class)
  @DisplayName("Invalid magic number throws CompilationException")
  void testInvalidMagicNumber(RuntimeType runtimeType) throws WasmException {
    if (!WasmRuntimeFactory.isRuntimeAvailable(runtimeType)) {
      return; // Skip if runtime not available
    }

    try (WasmRuntime runtime = WasmRuntimeFactory.create(runtimeType)) {
      Engine engine = runtime.createEngine();

      // Test various invalid magic numbers
      byte[][] invalidMagicNumbers = {
        {0x00, 0x00, 0x00, 0x00, 0x01, 0x00, 0x00, 0x00}, // Wrong magic
        {0x6d, 0x73, 0x61, 0x00, 0x01, 0x00, 0x00, 0x00}, // Reversed magic
        {0x00, 0x61, 0x73, 0x6d, 0x02, 0x00, 0x00, 0x00}, // Wrong version
        {'W', 'A', 'S', 'M', 0x01, 0x00, 0x00, 0x00} // ASCII magic
      };

      for (byte[] invalidMagic : invalidMagicNumbers) {
        CompilationException exception =
            assertThrows(
                CompilationException.class,
                () -> runtime.compileModule(engine, invalidMagic),
                "Invalid magic number should throw CompilationException");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");
        assertTrue(
            exception.getMessage().toLowerCase().contains("magic")
                || exception.getMessage().toLowerCase().contains("header")
                || exception.getMessage().toLowerCase().contains("invalid"),
            "Exception message should mention magic/header/invalid: " + exception.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Truncated WebAssembly module throws CompilationException")
  void testTruncatedModule() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test various truncated modules
      byte[][] truncatedModules = {
        {0x00}, // Single byte
        {0x00, 0x61}, // Two bytes
        {0x00, 0x61, 0x73}, // Three bytes
        {0x00, 0x61, 0x73, 0x6d}, // Magic only, no version
        {0x00, 0x61, 0x73, 0x6d, 0x01}, // Partial version
        {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00}, // Partial version
        {0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00} // Missing last version byte
      };

      for (byte[] truncated : truncatedModules) {
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> runtime.compileModule(engine, truncated),
                "Truncated module should throw WasmException: length=" + truncated.length);

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");
        assertTrue(
            exception.getMessage().toLowerCase().contains("truncated")
                || exception.getMessage().toLowerCase().contains("incomplete")
                || exception.getMessage().toLowerCase().contains("unexpected")
                || exception.getMessage().toLowerCase().contains("end"),
            "Exception message should mention truncation/incomplete/unexpected/end: "
                + exception.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Malformed section headers throw CompilationException")
  void testMalformedSectionHeaders() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Valid header + malformed section
      byte[] malformedSection1 = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Valid header
        0x01, // Section type (type section)
        0xFF, 0xFF, 0xFF, 0xFF, 0x0F // Invalid LEB128 size (too large)
      };

      byte[] malformedSection2 = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Valid header
        0x99, // Invalid section type
        0x01, 0x00 // Size = 1, content = 0
      };

      byte[] malformedSection3 = {
        0x00, 0x61, 0x73, 0x6d, 0x01, 0x00, 0x00, 0x00, // Valid header
        0x01, // Section type (type section)
        0x05, // Size = 5
        0x01, 0x02 // Only 2 bytes of content when 5 expected
      };

      byte[][] malformedSections = {malformedSection1, malformedSection2, malformedSection3};

      for (byte[] malformed : malformedSections) {
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> runtime.compileModule(engine, malformed),
                "Malformed section should throw WasmException");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");
        assertTrue(
            exception.getMessage().toLowerCase().contains("section")
                || exception.getMessage().toLowerCase().contains("invalid")
                || exception.getMessage().toLowerCase().contains("malformed"),
            "Exception message should mention section/invalid/malformed: "
                + exception.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Random binary data throws CompilationException")
  void testRandomBinaryData() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Test various non-WASM binary data
      byte[][] randomData = {
        "Hello, World!".getBytes(StandardCharsets.UTF_8), // Text
        {(byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF}, // All ones
        new byte[1024], // All zeros
        {0x7F, 0x45, 0x4C, 0x46}, // ELF magic
        {0x4D, 0x5A}, // PE/DOS magic
        {(byte) 0xCA, (byte) 0xFE, (byte) 0xBA, (byte) 0xBE} // Java class magic
      };

      for (byte[] data : randomData) {
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> runtime.compileModule(engine, data),
                "Random binary data should throw WasmException");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");
        assertTrue(
            exception.getMessage().length() > 10,
            "Exception message should be descriptive: " + exception.getMessage());
      }
    }
  }

  @Test
  @DisplayName("Large malformed modules are handled efficiently")
  void testLargeMalformedModules() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      // Create large malformed module (valid header + large random data)
      byte[] largeModule = new byte[10 * 1024]; // 10KB
      System.arraycopy(VALID_WASM_HEADER, 0, largeModule, 0, VALID_WASM_HEADER.length);
      // Fill rest with pseudo-random data
      for (int i = VALID_WASM_HEADER.length; i < largeModule.length; i++) {
        largeModule[i] = (byte) (i % 256);
      }

      long startTime = System.nanoTime();
      WasmException exception =
          assertThrows(
              WasmException.class,
              () -> runtime.compileModule(engine, largeModule),
              "Large malformed module should throw WasmException");
      long duration = System.nanoTime() - startTime;

      assertNotNull(exception.getMessage(), "Exception should have meaningful message");

      // Error handling should be fast (less than 1 second for 10KB)
      assertTrue(
          duration < TimeUnit.SECONDS.toNanos(1),
          "Error handling should be efficient: " + duration + "ns");
    }
  }

  @Test
  @DisplayName("Concurrent compilation errors are handled safely")
  void testConcurrentCompilationErrors() throws InterruptedException, WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      final int threadCount = 10;
      final CountDownLatch latch = new CountDownLatch(threadCount);
      final AtomicInteger exceptionCount = new AtomicInteger(0);
      final AtomicInteger unexpectedExceptionCount = new AtomicInteger(0);

      ExecutorService executor = Executors.newFixedThreadPool(threadCount);

      for (int i = 0; i < threadCount; i++) {
        final int threadIndex = i;
        executor.submit(
            () -> {
              try {
                // Each thread tries to compile different malformed data
                byte[] malformedData = {
                  0x00,
                  0x61,
                  0x73,
                  0x6d,
                  0x01,
                  0x00,
                  0x00,
                  0x00,
                  (byte) threadIndex,
                  (byte) (threadIndex * 2),
                  (byte) (threadIndex * 3)
                };

                runtime.compileModule(engine, malformedData);
                // Should not reach here
              } catch (WasmException e) {
                exceptionCount.incrementAndGet();
              } catch (Exception e) {
                unexpectedExceptionCount.incrementAndGet();
              } finally {
                latch.countDown();
              }
            });
      }

      assertTrue(latch.await(30, TimeUnit.SECONDS), "All threads should complete within 30 seconds");
      executor.shutdown();

      assertTrue(exceptionCount.get() >= threadCount / 2, "Most threads should throw WasmException");
      assertTrue(
          unexpectedExceptionCount.get() == 0, "No unexpected exceptions should occur");
    }
  }

  @Test
  @DisplayName("Error messages are consistent across multiple attempts")
  void testErrorMessageConsistency() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      byte[] consistentMalformedData = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05};

      String firstMessage = null;
      for (int i = 0; i < 5; i++) {
        WasmException exception =
            assertThrows(
                WasmException.class,
                () -> runtime.compileModule(engine, consistentMalformedData),
                "Consistent malformed data should throw WasmException");

        assertNotNull(exception.getMessage(), "Exception should have meaningful message");

        if (firstMessage == null) {
          firstMessage = exception.getMessage();
        } else {
          assertTrue(
              firstMessage.equals(exception.getMessage()),
              "Error messages should be consistent across attempts. First: '"
                  + firstMessage
                  + "', Current: '"
                  + exception.getMessage()
                  + "'");
        }
      }
    }
  }

  @Test
  @DisplayName("Resource cleanup happens after compilation errors")
  void testResourceCleanupAfterErrors() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      byte[] malformedData = {0x00, 0x01, 0x02, 0x03};

      // Trigger multiple compilation errors
      for (int i = 0; i < 100; i++) {
        assertThrows(
            WasmException.class,
            () -> runtime.compileModule(engine, malformedData),
            "Malformed data should throw WasmException");
      }

      // Runtime should still be valid after errors
      assertTrue(runtime.isValid(), "Runtime should remain valid after compilation errors");

      // Should be able to create new engines
      assertDoesNotThrow(
          () -> runtime.createEngine(), "Should be able to create engine after errors");
    }
  }

  @Test
  @DisplayName("Memory usage remains stable during repeated compilation errors")
  void testMemoryUsageDuringErrors() throws WasmException {
    try (WasmRuntime runtime = WasmRuntimeFactory.create()) {
      Engine engine = runtime.createEngine();

      byte[] malformedData = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07};

      // Force garbage collection and measure initial memory
      System.gc();
      Thread.yield();
      long initialMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Trigger many compilation errors
      for (int i = 0; i < 1000; i++) {
        assertThrows(
            WasmException.class,
            () -> runtime.compileModule(engine, malformedData),
            "Malformed data should throw WasmException");

        // Periodic garbage collection
        if (i % 100 == 0) {
          System.gc();
          Thread.yield();
        }
      }

      // Force garbage collection and measure final memory
      System.gc();
      Thread.yield();
      long finalMemory = Runtime.getRuntime().totalMemory() - Runtime.getRuntime().freeMemory();

      // Memory usage should not increase dramatically (allow 50MB increase)
      long memoryIncrease = finalMemory - initialMemory;
      assertTrue(
          memoryIncrease < 50 * 1024 * 1024,
          "Memory usage should remain stable. Initial: "
              + initialMemory
              + ", Final: "
              + finalMemory
              + ", Increase: "
              + memoryIncrease);
    }
  }
}