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

package ai.tegmentum.wasmtime4j.wasi.jni;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiContext;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiPreview1Operations;
import ai.tegmentum.wasmtime4j.wasi.security.WasiSecurityValidator;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Integration tests for JNI WasiPreview1Operations.
 *
 * <p>These tests verify the actual native implementation of WASI Preview 1 operations, including
 * file I/O, environment access, time operations, and random number generation.
 *
 * <p>Test coverage:
 *
 * <ul>
 *   <li>Environment operations: environ_get, environ_sizes_get
 *   <li>Arguments operations: args_get, args_sizes_get
 *   <li>Clock operations: clock_time_get, clock_res_get
 *   <li>Random operations: random_get
 * </ul>
 */
@DisplayName("JNI WasiPreview1Operations Integration Tests")
class JniWasiPreview1OperationsIntegrationTest {

  private static final Logger LOGGER =
      Logger.getLogger(JniWasiPreview1OperationsIntegrationTest.class.getName());

  /**
   * Permissive security validator that allows absolute paths for testing. This is required because
   * the default validator rejects absolute paths.
   */
  private static final WasiSecurityValidator TEST_SECURITY_VALIDATOR =
      WasiSecurityValidator.builder().withAllowAbsolutePaths(true).build();

  /** WASI clock IDs. */
  private static final int CLOCK_REALTIME = 0;

  private static final int CLOCK_MONOTONIC = 1;

  /** Tracks resources for cleanup. */
  private final List<AutoCloseable> resources = new ArrayList<>();

  @TempDir Path tempDir;

  private WasiContext wasiContext;
  private WasiPreview1Operations operations;

  @BeforeAll
  static void loadNativeLibrary() {
    LOGGER.info("Loading native library for WASI operations tests");
    try {
      NativeLibraryLoader.loadLibrary();
      LOGGER.info("Native library loaded successfully");
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library: " + e.getMessage());
      throw new RuntimeException("Native library required for integration tests", e);
    }
  }

  @BeforeEach
  void setUp() throws Exception {
    LOGGER.info("Setting up test resources");

    // Create a preopen directory
    final Path preopenDir = tempDir.resolve("preopen");
    Files.createDirectories(preopenDir);

    // Create WASI context with test configuration
    wasiContext =
        WasiContext.builder()
            .withSecurityValidator(TEST_SECURITY_VALIDATOR)
            .withEnvironment("TEST_VAR", "test_value")
            .withEnvironment("HOME", "/home/test")
            .withEnvironment("PATH", "/usr/bin")
            .withArgument("test_program")
            .withArgument("--verbose")
            .withArgument("-o")
            .withArgument("output.txt")
            .withPreopenDirectory("/", preopenDir.toString())
            .withWorkingDirectory("/")
            .build();
    resources.add(wasiContext);

    // Create operations instance
    operations = new WasiPreview1Operations(wasiContext);

    LOGGER.info("Test setup complete with preopen dir: " + preopenDir);
  }

  @AfterEach
  void tearDown() {
    LOGGER.info("Cleaning up test resources");
    for (final AutoCloseable resource : resources) {
      try {
        resource.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close resource: " + e.getMessage());
      }
    }
    resources.clear();
    wasiContext = null;
    operations = null;
  }

  @Nested
  @DisplayName("Operations Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("should create operations with valid context")
    void shouldCreateOperationsWithValidContext() {
      LOGGER.info("Testing operations constructor with valid context");

      assertNotNull(operations, "Operations should not be null");
      LOGGER.info("Operations instance created successfully");
    }

    @Test
    @DisplayName("should throw on null context")
    void shouldThrowOnNullContext() {
      LOGGER.info("Testing operations constructor with null context");

      assertThrows(
          RuntimeException.class,
          () -> new WasiPreview1Operations(null),
          "Should throw on null context");

      LOGGER.info("Constructor correctly rejected null context");
    }
  }

  @Nested
  @DisplayName("Environment Operations Tests")
  class EnvironmentOperationsTests {

    @Test
    @DisplayName("should get environment sizes correctly")
    void shouldGetEnvironmentSizesCorrectly() {
      LOGGER.info("Testing environ_sizes_get operation");

      final int[] sizes = operations.environSizesGet();

      assertNotNull(sizes, "Sizes array should not be null");
      assertEquals(2, sizes.length, "Sizes array should have 2 elements [count, totalSize]");

      final int count = sizes[0];
      final int totalSize = sizes[1];

      assertEquals(3, count, "Should have 3 environment variables");
      assertTrue(totalSize > 0, "Total size should be positive");

      // Calculate expected size: KEY=value\0 for each
      // TEST_VAR=test_value\0 = 20 bytes
      // HOME=/home/test\0 = 16 bytes
      // PATH=/usr/bin\0 = 14 bytes
      // Total = 50 bytes
      LOGGER.info(String.format("environ_sizes_get: count=%d, totalSize=%d", count, totalSize));
    }

    @Test
    @DisplayName("should get environment variables correctly")
    void shouldGetEnvironmentVariablesCorrectly() {
      LOGGER.info("Testing environ_get operation");

      // First get sizes
      final int[] sizes = operations.environSizesGet();
      final int count = sizes[0];
      final int totalSize = sizes[1];

      // Allocate buffers
      final ByteBuffer environ = ByteBuffer.allocate(count * 4); // 4 bytes per pointer
      final ByteBuffer environBuf = ByteBuffer.allocate(totalSize);

      final int result = operations.environGet(environ, environBuf);

      assertEquals(WasiPreview1Operations.WASI_ESUCCESS, result, "Should return success");

      LOGGER.info("environ_get completed successfully");
    }

    @Test
    @DisplayName("should throw on null environ buffer")
    void shouldThrowOnNullEnvironBuffer() {
      LOGGER.info("Testing environ_get with null environ buffer");

      final ByteBuffer environBuf = ByteBuffer.allocate(100);

      assertThrows(
          RuntimeException.class,
          () -> operations.environGet(null, environBuf),
          "Should throw on null environ buffer");

      LOGGER.info("Correctly rejected null environ buffer");
    }

    @Test
    @DisplayName("should throw on null environBuf buffer")
    void shouldThrowOnNullEnvironBufBuffer() {
      LOGGER.info("Testing environ_get with null environBuf buffer");

      final ByteBuffer environ = ByteBuffer.allocate(100);

      assertThrows(
          RuntimeException.class,
          () -> operations.environGet(environ, null),
          "Should throw on null environBuf buffer");

      LOGGER.info("Correctly rejected null environBuf buffer");
    }
  }

  @Nested
  @DisplayName("Arguments Operations Tests")
  class ArgumentsOperationsTests {

    @Test
    @DisplayName("should get argument sizes correctly")
    void shouldGetArgumentSizesCorrectly() {
      LOGGER.info("Testing args_sizes_get operation");

      final int[] sizes = operations.argsSizesGet();

      assertNotNull(sizes, "Sizes array should not be null");
      assertEquals(2, sizes.length, "Sizes array should have 2 elements [count, totalSize]");

      final int count = sizes[0];
      final int totalSize = sizes[1];

      assertEquals(4, count, "Should have 4 arguments");
      assertTrue(totalSize > 0, "Total size should be positive");

      // Calculate expected size: string\0 for each
      // test_program\0 = 13 bytes
      // --verbose\0 = 10 bytes
      // -o\0 = 3 bytes
      // output.txt\0 = 11 bytes
      // Total = 37 bytes
      LOGGER.info(String.format("args_sizes_get: count=%d, totalSize=%d", count, totalSize));
    }

    @Test
    @DisplayName("should get arguments correctly")
    void shouldGetArgumentsCorrectly() {
      LOGGER.info("Testing args_get operation");

      // First get sizes
      final int[] sizes = operations.argsSizesGet();
      final int count = sizes[0];
      final int totalSize = sizes[1];

      // Allocate buffers
      final ByteBuffer argv = ByteBuffer.allocate(count * 4); // 4 bytes per pointer
      final ByteBuffer argvBuf = ByteBuffer.allocate(totalSize);

      final int result = operations.argsGet(argv, argvBuf);

      assertEquals(WasiPreview1Operations.WASI_ESUCCESS, result, "Should return success");

      LOGGER.info("args_get completed successfully");
    }

    @Test
    @DisplayName("should throw on null argv buffer")
    void shouldThrowOnNullArgvBuffer() {
      LOGGER.info("Testing args_get with null argv buffer");

      final ByteBuffer argvBuf = ByteBuffer.allocate(100);

      assertThrows(
          RuntimeException.class,
          () -> operations.argsGet(null, argvBuf),
          "Should throw on null argv buffer");

      LOGGER.info("Correctly rejected null argv buffer");
    }

    @Test
    @DisplayName("should throw on null argvBuf buffer")
    void shouldThrowOnNullArgvBufBuffer() {
      LOGGER.info("Testing args_get with null argvBuf buffer");

      final ByteBuffer argv = ByteBuffer.allocate(100);

      assertThrows(
          RuntimeException.class,
          () -> operations.argsGet(argv, null),
          "Should throw on null argvBuf buffer");

      LOGGER.info("Correctly rejected null argvBuf buffer");
    }
  }

  @Nested
  @DisplayName("Clock Operations Tests")
  class ClockOperationsTests {

    @Test
    @DisplayName("should get realtime clock time")
    void shouldGetRealtimeClockTime() {
      LOGGER.info("Testing clock_time_get for realtime clock");

      final long time = operations.clockTimeGet(CLOCK_REALTIME, 1);

      assertTrue(time > 0, "Realtime clock should return positive value");

      // Time should be in nanoseconds since Unix epoch
      // Reasonable range: after year 2000 (946684800 seconds)
      final long minExpected = 946684800L * 1_000_000_000L;
      assertTrue(time > minExpected, "Time should be after year 2000");

      LOGGER.info("clock_time_get (realtime): " + time + " nanoseconds");
    }

    @Test
    @DisplayName("should get monotonic clock time")
    void shouldGetMonotonicClockTime() {
      LOGGER.info("Testing clock_time_get for monotonic clock");

      final long time1 = operations.clockTimeGet(CLOCK_MONOTONIC, 1);
      assertTrue(time1 >= 0, "Monotonic clock should return non-negative value");

      // Wait a tiny bit and get time again
      try {
        Thread.sleep(1);
      } catch (final InterruptedException e) {
        Thread.currentThread().interrupt();
      }

      final long time2 = operations.clockTimeGet(CLOCK_MONOTONIC, 1);
      assertTrue(time2 >= time1, "Monotonic clock should be non-decreasing");

      LOGGER.info(String.format("clock_time_get (monotonic): t1=%d, t2=%d", time1, time2));
    }

    @Test
    @DisplayName("should get realtime clock resolution")
    void shouldGetRealtimeClockResolution() {
      LOGGER.info("Testing clock_res_get for realtime clock");

      final long resolution = operations.clockResGet(CLOCK_REALTIME);

      assertTrue(resolution > 0, "Clock resolution should be positive");

      // Resolution should be reasonable (nanoseconds to milliseconds range)
      // Most systems have at least microsecond resolution
      assertTrue(resolution <= 1_000_000_000L, "Resolution should be at most 1 second");

      LOGGER.info("clock_res_get (realtime): " + resolution + " nanoseconds");
    }

    @Test
    @DisplayName("should get monotonic clock resolution")
    void shouldGetMonotonicClockResolution() {
      LOGGER.info("Testing clock_res_get for monotonic clock");

      final long resolution = operations.clockResGet(CLOCK_MONOTONIC);

      assertTrue(resolution > 0, "Clock resolution should be positive");
      assertTrue(resolution <= 1_000_000_000L, "Resolution should be at most 1 second");

      LOGGER.info("clock_res_get (monotonic): " + resolution + " nanoseconds");
    }
  }

  @Nested
  @DisplayName("Random Operations Tests")
  class RandomOperationsTests {

    @Test
    @DisplayName("should generate random bytes")
    void shouldGenerateRandomBytes() {
      LOGGER.info("Testing random_get operation");

      final ByteBuffer buffer = ByteBuffer.allocate(32);
      operations.randomGet(buffer);

      // Buffer position should not change (data is put at current position)
      assertEquals(32, buffer.position(), "Buffer should have 32 bytes written");

      // Rewind to read
      buffer.rewind();
      final byte[] randomBytes = new byte[32];
      buffer.get(randomBytes);

      // Check that not all bytes are zero (extremely unlikely for random data)
      boolean hasNonZero = false;
      for (final byte b : randomBytes) {
        if (b != 0) {
          hasNonZero = true;
          break;
        }
      }
      assertTrue(hasNonZero, "Random bytes should contain non-zero values");

      LOGGER.info("random_get generated 32 bytes successfully");
    }

    @Test
    @DisplayName("should generate different random bytes each time")
    void shouldGenerateDifferentRandomBytesEachTime() {
      LOGGER.info("Testing random_get produces different results");

      final ByteBuffer buffer1 = ByteBuffer.allocate(16);
      final ByteBuffer buffer2 = ByteBuffer.allocate(16);

      operations.randomGet(buffer1);
      operations.randomGet(buffer2);

      buffer1.rewind();
      buffer2.rewind();

      final byte[] bytes1 = new byte[16];
      final byte[] bytes2 = new byte[16];
      buffer1.get(bytes1);
      buffer2.get(bytes2);

      // These should be different (with extremely high probability)
      boolean different = false;
      for (int i = 0; i < 16; i++) {
        if (bytes1[i] != bytes2[i]) {
          different = true;
          break;
        }
      }
      assertTrue(different, "Two random_get calls should produce different results");

      LOGGER.info("random_get produced different results as expected");
    }

    @Test
    @DisplayName("should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      LOGGER.info("Testing random_get with null buffer");

      assertThrows(
          RuntimeException.class, () -> operations.randomGet(null), "Should throw on null buffer");

      LOGGER.info("Correctly rejected null buffer");
    }

    @Test
    @DisplayName("should handle empty buffer")
    void shouldHandleEmptyBuffer() {
      LOGGER.info("Testing random_get with empty buffer");

      final ByteBuffer buffer = ByteBuffer.allocate(0);

      // Empty buffer should complete without error
      operations.randomGet(buffer);

      assertEquals(0, buffer.position(), "Position should remain at 0");
      LOGGER.info("random_get handled empty buffer gracefully");
    }

    @Test
    @DisplayName("should generate large amount of random bytes")
    void shouldGenerateLargeAmountOfRandomBytes() {
      LOGGER.info("Testing random_get with large buffer");

      final int size = 4096;
      final ByteBuffer buffer = ByteBuffer.allocate(size);

      operations.randomGet(buffer);

      assertEquals(size, buffer.position(), "Buffer should have " + size + " bytes written");

      LOGGER.info("random_get generated " + size + " bytes successfully");
    }
  }

  @Nested
  @DisplayName("Error Code Tests")
  class ErrorCodeTests {

    @Test
    @DisplayName("should define success error code")
    void shouldDefineSuccessErrorCode() {
      LOGGER.info("Testing WASI error code constants");

      assertEquals(0, WasiPreview1Operations.WASI_ESUCCESS, "WASI_ESUCCESS should be 0");
      LOGGER.info("WASI_ESUCCESS = " + WasiPreview1Operations.WASI_ESUCCESS);
    }

    @Test
    @DisplayName("should define invalid argument error code")
    void shouldDefineInvalidArgumentErrorCode() {
      assertEquals(28, WasiPreview1Operations.WASI_EINVAL, "WASI_EINVAL should be 28");
      LOGGER.info("WASI_EINVAL = " + WasiPreview1Operations.WASI_EINVAL);
    }

    @Test
    @DisplayName("should define bad file descriptor error code")
    void shouldDefineBadFileDescriptorErrorCode() {
      assertEquals(9, WasiPreview1Operations.WASI_EBADF, "WASI_EBADF should be 9");
      LOGGER.info("WASI_EBADF = " + WasiPreview1Operations.WASI_EBADF);
    }

    @Test
    @DisplayName("should define no such file error code")
    void shouldDefineNoSuchFileErrorCode() {
      assertEquals(44, WasiPreview1Operations.WASI_ENOENT, "WASI_ENOENT should be 44");
      LOGGER.info("WASI_ENOENT = " + WasiPreview1Operations.WASI_ENOENT);
    }

    @Test
    @DisplayName("should define permission denied error code")
    void shouldDefinePermissionDeniedErrorCode() {
      assertEquals(2, WasiPreview1Operations.WASI_EACCES, "WASI_EACCES should be 2");
      LOGGER.info("WASI_EACCES = " + WasiPreview1Operations.WASI_EACCES);
    }
  }

  @Nested
  @DisplayName("Context with Empty Configuration Tests")
  class EmptyConfigurationTests {

    @Test
    @DisplayName("should handle context with no environment variables")
    void shouldHandleContextWithNoEnvironmentVariables() throws Exception {
      LOGGER.info("Testing operations with empty environment");

      final Path preopenDir = tempDir.resolve("empty-env");
      Files.createDirectories(preopenDir);

      final WasiContext emptyContext =
          WasiContext.builder()
              .withSecurityValidator(TEST_SECURITY_VALIDATOR)
              .withPreopenDirectory("/", preopenDir.toString())
              .build();
      resources.add(emptyContext);

      final WasiPreview1Operations emptyOps = new WasiPreview1Operations(emptyContext);

      final int[] sizes = emptyOps.environSizesGet();
      assertEquals(0, sizes[0], "Should have 0 environment variables");
      assertEquals(0, sizes[1], "Total size should be 0");

      LOGGER.info("Empty environment handled correctly");
    }

    @Test
    @DisplayName("should handle context with no arguments")
    void shouldHandleContextWithNoArguments() throws Exception {
      LOGGER.info("Testing operations with no arguments");

      final Path preopenDir = tempDir.resolve("no-args");
      Files.createDirectories(preopenDir);

      final WasiContext noArgsContext =
          WasiContext.builder()
              .withSecurityValidator(TEST_SECURITY_VALIDATOR)
              .withPreopenDirectory("/", preopenDir.toString())
              .build();
      resources.add(noArgsContext);

      final WasiPreview1Operations noArgsOps = new WasiPreview1Operations(noArgsContext);

      final int[] sizes = noArgsOps.argsSizesGet();
      assertEquals(0, sizes[0], "Should have 0 arguments");
      assertEquals(0, sizes[1], "Total size should be 0");

      LOGGER.info("No arguments handled correctly");
    }
  }
}
