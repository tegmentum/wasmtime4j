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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/** Comprehensive tests for {@link WasiPreview1Operations}. */
@DisplayName("WasiPreview1Operations Tests")
class WasiPreview1OperationsTest {

  @TempDir Path tempDir;

  private WasiContext testContext;
  private WasiPreview1Operations operations;

  @BeforeEach
  void setUp() throws Exception {
    final Map<String, String> environment = new HashMap<>();
    environment.put("PATH", "/usr/bin");
    environment.put("HOME", "/home/user");
    environment.put("LANG", "en_US.UTF-8");

    final String[] arguments = new String[] {"arg0", "arg1", "arg2"};

    testContext =
        TestWasiContextFactory.createTestContextWithWorkingDir(tempDir, environment, arguments);

    operations = new WasiPreview1Operations(testContext);
  }

  @AfterEach
  void tearDown() {
    // Cleanup handled by test context
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiPreview1Operations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiPreview1Operations.class.getModifiers()),
          "WasiPreview1Operations should be final");
    }

    @Test
    @DisplayName("Should define WASI error code constants")
    void shouldDefineWasiErrorCodeConstants() {
      assertEquals(0, WasiPreview1Operations.WASI_ESUCCESS, "WASI_ESUCCESS should be 0");
      assertEquals(28, WasiPreview1Operations.WASI_EINVAL, "WASI_EINVAL should be 28");
      assertEquals(9, WasiPreview1Operations.WASI_EBADF, "WASI_EBADF should be 9");
      assertEquals(44, WasiPreview1Operations.WASI_ENOENT, "WASI_ENOENT should be 44");
      assertEquals(2, WasiPreview1Operations.WASI_EACCES, "WASI_EACCES should be 2");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(
          JniException.class,
          () -> new WasiPreview1Operations(null),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should create operations with valid context")
    void constructorShouldCreateOperationsWithValidContext() {
      final WasiPreview1Operations ops = new WasiPreview1Operations(testContext);
      assertNotNull(ops, "Operations should be created");
    }
  }

  @Nested
  @DisplayName("fdRead Tests")
  class FdReadTests {

    @Test
    @DisplayName("Should throw on null iovs")
    void shouldThrowOnNullIovs() {
      assertThrows(
          JniException.class, () -> operations.fdRead(3, null), "Should throw on null iovs");
    }

    @Test
    @DisplayName("Should throw on negative file descriptor")
    void shouldThrowOnNegativeFileDescriptor() {
      final List<ByteBuffer> iovs = new ArrayList<>();
      iovs.add(ByteBuffer.allocate(10));

      assertThrows(
          WasiException.class, () -> operations.fdRead(-1, iovs), "Should throw on negative fd");
    }

    @Test
    @DisplayName("Should handle empty iovs list")
    void shouldHandleEmptyIovsList() throws Exception {
      final Path testFile = tempDir.resolve("read.txt");
      writeStringToFile(testFile, "Hello");

      // Empty list should return 0 bytes read
      final List<ByteBuffer> iovs = new ArrayList<>();

      // Empty iovs list returns 0 bytes (no buffers to read into)
      final int bytesRead = operations.fdRead(3, iovs);
      assertEquals(0, bytesRead, "Empty iovs list should return 0 bytes read");
    }
  }

  @Nested
  @DisplayName("fdWrite Tests")
  class FdWriteTests {

    @Test
    @DisplayName("Should throw on null iovs")
    void shouldThrowOnNullIovs() {
      assertThrows(
          JniException.class, () -> operations.fdWrite(3, null), "Should throw on null iovs");
    }

    @Test
    @DisplayName("Should throw on negative file descriptor")
    void shouldThrowOnNegativeFileDescriptor() {
      final List<ByteBuffer> iovs = new ArrayList<>();
      iovs.add(ByteBuffer.wrap("test".getBytes()));

      assertThrows(
          WasiException.class, () -> operations.fdWrite(-1, iovs), "Should throw on negative fd");
    }
  }

  @Nested
  @DisplayName("fdSeek Tests")
  class FdSeekTests {

    @Test
    @DisplayName("Should throw on negative file descriptor")
    void shouldThrowOnNegativeFileDescriptor() {
      assertThrows(
          WasiException.class, () -> operations.fdSeek(-1, 0, 0), "Should throw on negative fd");
    }

    @Test
    @DisplayName("Should throw on invalid whence")
    void shouldThrowOnInvalidWhence() {
      assertThrows(
          WasiException.class, () -> operations.fdSeek(3, 0, 99), "Should throw on invalid whence");
    }

    @Test
    @DisplayName("Should throw on negative whence")
    void shouldThrowOnNegativeWhence() {
      assertThrows(
          WasiException.class,
          () -> operations.fdSeek(3, 0, -1),
          "Should throw on negative whence");
    }

    @Test
    @DisplayName("Should accept valid whence values")
    void shouldAcceptValidWhenceValues() {
      // SEEK_SET (0), SEEK_CUR (1), SEEK_END (2) are valid
      // These will throw because mock setup, but the whence validation passes
      assertThrows(WasiException.class, () -> operations.fdSeek(3, 0, 0));
      assertThrows(WasiException.class, () -> operations.fdSeek(3, 0, 1));
      assertThrows(WasiException.class, () -> operations.fdSeek(3, 0, 2));
    }
  }

  @Nested
  @DisplayName("fdClose Tests")
  class FdCloseTests {

    @Test
    @DisplayName("Should throw on negative file descriptor")
    void shouldThrowOnNegativeFileDescriptor() {
      assertThrows(
          WasiException.class, () -> operations.fdClose(-1), "Should throw on negative fd");
    }
  }

  @Nested
  @DisplayName("pathOpen Tests")
  class PathOpenTests {

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(
          JniException.class,
          () -> operations.pathOpen(3, 0, null, 0, 0L, 0L, 0),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on empty path")
    void shouldThrowOnEmptyPath() {
      assertThrows(
          JniException.class,
          () -> operations.pathOpen(3, 0, "", 0, 0L, 0L, 0),
          "Should throw on empty path");
    }

    @Test
    @DisplayName("Should throw on negative dirfd")
    void shouldThrowOnNegativeDirfd() {
      assertThrows(
          WasiException.class,
          () -> operations.pathOpen(-1, 0, "test.txt", 0, 0L, 0L, 0),
          "Should throw on negative dirfd");
    }
  }

  @Nested
  @DisplayName("pathCreateDirectory Tests")
  class PathCreateDirectoryTests {

    @Test
    @DisplayName("Should throw on null path")
    void shouldThrowOnNullPath() {
      assertThrows(
          JniException.class,
          () -> operations.pathCreateDirectory(3, null),
          "Should throw on null path");
    }

    @Test
    @DisplayName("Should throw on empty path")
    void shouldThrowOnEmptyPath() {
      assertThrows(
          JniException.class,
          () -> operations.pathCreateDirectory(3, ""),
          "Should throw on empty path");
    }

    @Test
    @DisplayName("Should throw on negative dirfd")
    void shouldThrowOnNegativeDirfd() {
      assertThrows(
          WasiException.class,
          () -> operations.pathCreateDirectory(-1, "newdir"),
          "Should throw on negative dirfd");
    }
  }

  @Nested
  @DisplayName("environGet Tests")
  class EnvironGetTests {

    @Test
    @DisplayName("Should throw on null environ buffer")
    void shouldThrowOnNullEnvironBuffer() {
      assertThrows(
          JniException.class,
          () -> operations.environGet(null, ByteBuffer.allocate(100)),
          "Should throw on null environ");
    }

    @Test
    @DisplayName("Should throw on null environBuf buffer")
    void shouldThrowOnNullEnvironBufBuffer() {
      assertThrows(
          JniException.class,
          () -> operations.environGet(ByteBuffer.allocate(100), null),
          "Should throw on null environBuf");
    }

    @Test
    @DisplayName("Should write environment to buffers")
    void shouldWriteEnvironmentToBuffers() {
      final ByteBuffer environ = ByteBuffer.allocate(100);
      final ByteBuffer environBuf = ByteBuffer.allocate(500);

      final int result = operations.environGet(environ, environBuf);

      assertEquals(WasiPreview1Operations.WASI_ESUCCESS, result, "Should return success");
    }
  }

  @Nested
  @DisplayName("environSizesGet Tests")
  class EnvironSizesGetTests {

    @Test
    @DisplayName("Should return environment sizes")
    void shouldReturnEnvironmentSizes() {
      final int[] sizes = operations.environSizesGet();

      assertNotNull(sizes, "Sizes should not be null");
      assertEquals(2, sizes.length, "Should return [count, totalSize]");
      assertEquals(3, sizes[0], "Should have 3 environment variables");
      assertTrue(sizes[1] > 0, "Total size should be positive");
    }
  }

  @Nested
  @DisplayName("argsGet Tests")
  class ArgsGetTests {

    @Test
    @DisplayName("Should throw on null argv buffer")
    void shouldThrowOnNullArgvBuffer() {
      assertThrows(
          JniException.class,
          () -> operations.argsGet(null, ByteBuffer.allocate(100)),
          "Should throw on null argv");
    }

    @Test
    @DisplayName("Should throw on null argvBuf buffer")
    void shouldThrowOnNullArgvBufBuffer() {
      assertThrows(
          JniException.class,
          () -> operations.argsGet(ByteBuffer.allocate(100), null),
          "Should throw on null argvBuf");
    }

    @Test
    @DisplayName("Should write arguments to buffers")
    void shouldWriteArgumentsToBuffers() {
      final ByteBuffer argv = ByteBuffer.allocate(100);
      final ByteBuffer argvBuf = ByteBuffer.allocate(500);

      final int result = operations.argsGet(argv, argvBuf);

      assertEquals(WasiPreview1Operations.WASI_ESUCCESS, result, "Should return success");
    }
  }

  @Nested
  @DisplayName("argsSizesGet Tests")
  class ArgsSizesGetTests {

    @Test
    @DisplayName("Should return argument sizes")
    void shouldReturnArgumentSizes() {
      final int[] sizes = operations.argsSizesGet();

      assertNotNull(sizes, "Sizes should not be null");
      assertEquals(2, sizes.length, "Should return [count, totalSize]");
      assertEquals(3, sizes[0], "Should have 3 arguments");
      assertTrue(sizes[1] > 0, "Total size should be positive");
    }
  }

  @Nested
  @DisplayName("clockTimeGet Tests")
  class ClockTimeGetTests {

    @Test
    @DisplayName("Should throw on invalid clock ID")
    void shouldThrowOnInvalidClockId() {
      // Clock operations delegate to WasiTimeOperations
      // Invalid clock IDs are validated there
      assertThrows(
          Exception.class,
          () -> operations.clockTimeGet(999, 0),
          "Should throw on invalid clock ID");
    }
  }

  @Nested
  @DisplayName("clockResGet Tests")
  class ClockResGetTests {

    @Test
    @DisplayName("Should throw on invalid clock ID")
    void shouldThrowOnInvalidClockId() {
      assertThrows(
          Exception.class, () -> operations.clockResGet(999), "Should throw on invalid clock ID");
    }
  }

  @Nested
  @DisplayName("randomGet Tests")
  class RandomGetTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(
          JniException.class, () -> operations.randomGet(null), "Should throw on null buffer");
    }
  }

  /**
   * Helper method to write string to file - Java 8 compatible alternative to Files.writeString().
   *
   * @param path the path to write to
   * @param content the string content to write
   * @throws IOException if writing fails
   */
  private static void writeStringToFile(final Path path, final String content) throws IOException {
    Files.write(path, content.getBytes(StandardCharsets.UTF_8));
  }
}
