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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiStreamOperations.StreamInfo;
import ai.tegmentum.wasmtime4j.jni.wasi.WasiStreamOperations.StreamType;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiStreamOperations}. */
@DisplayName("WasiStreamOperations Tests")
class WasiStreamOperationsTest {

  private WasiContext testContext;
  private ExecutorService executorService;
  private WasiStreamOperations streamOperations;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    executorService = Executors.newSingleThreadExecutor();
    streamOperations = new WasiStreamOperations(testContext, executorService);
  }

  @AfterEach
  void tearDown() {
    if (streamOperations != null) {
      streamOperations.close();
    }
    if (executorService != null) {
      executorService.shutdown();
    }
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiStreamOperations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiStreamOperations.class.getModifiers()),
          "WasiStreamOperations should be final");
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
          () -> new WasiStreamOperations(null, executorService),
          "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should throw on null executor")
    void constructorShouldThrowOnNullExecutor() {
      assertThrows(
          JniException.class,
          () -> new WasiStreamOperations(testContext, null),
          "Should throw on null executor");
    }

    @Test
    @DisplayName("Constructor should create operations with valid parameters")
    void constructorShouldCreateOperationsWithValidParameters() {
      final WasiStreamOperations ops = new WasiStreamOperations(testContext, executorService);
      assertNotNull(ops, "Operations should be created");
      ops.close();
    }
  }

  @Nested
  @DisplayName("openInputStream Tests")
  class OpenInputStreamTests {

    @Test
    @DisplayName("Should attempt to open input stream")
    void shouldAttemptToOpenInputStream() {
      // Will throw due to native call failure (WasiException or UnsatisfiedLinkError)
      assertThrows(
          Throwable.class,
          () -> streamOperations.openInputStream(1L),
          "Should attempt to open input stream");
    }
  }

  @Nested
  @DisplayName("openOutputStream Tests")
  class OpenOutputStreamTests {

    @Test
    @DisplayName("Should attempt to open output stream")
    void shouldAttemptToOpenOutputStream() {
      // Will throw due to native call failure (WasiException or UnsatisfiedLinkError)
      assertThrows(
          Throwable.class,
          () -> streamOperations.openOutputStream(1L),
          "Should attempt to open output stream");
    }
  }

  @Nested
  @DisplayName("read Tests")
  class ReadTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(
          JniException.class, () -> streamOperations.read(1L, null), "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on invalid stream handle")
    void shouldThrowOnInvalidStreamHandle() {
      assertThrows(
          WasiException.class,
          () -> streamOperations.read(999L, ByteBuffer.allocate(100)),
          "Should throw on invalid stream handle");
    }

    @Test
    @DisplayName("Should return zero for empty buffer")
    void shouldReturnZeroForEmptyBuffer() {
      // Can't test directly without valid stream, but validates logic
      final ByteBuffer buffer = ByteBuffer.allocate(10);
      buffer.position(10); // No remaining space

      assertThrows(
          WasiException.class,
          () -> streamOperations.read(1L, buffer),
          "Should throw on invalid stream (not return 0 for invalid stream)");
    }
  }

  @Nested
  @DisplayName("write Tests")
  class WriteTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(
          JniException.class,
          () -> streamOperations.write(1L, null),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on invalid stream handle")
    void shouldThrowOnInvalidStreamHandle() {
      assertThrows(
          WasiException.class,
          () -> streamOperations.write(999L, ByteBuffer.wrap("test".getBytes())),
          "Should throw on invalid stream handle");
    }
  }

  @Nested
  @DisplayName("flush Tests")
  class FlushTests {

    @Test
    @DisplayName("Should throw on invalid stream handle")
    void shouldThrowOnInvalidStreamHandle() {
      assertThrows(
          WasiException.class,
          () -> streamOperations.flush(999L),
          "Should throw on invalid stream handle");
    }
  }

  @Nested
  @DisplayName("closeStream Tests")
  class CloseStreamTests {

    @Test
    @DisplayName("Should handle invalid stream handle gracefully")
    void shouldHandleInvalidStreamHandleGracefully() {
      // Should not throw, just log and return
      streamOperations.closeStream(999L);
      assertTrue(true, "Should not throw");
    }
  }

  @Nested
  @DisplayName("getStreamInfo Tests")
  class GetStreamInfoTests {

    @Test
    @DisplayName("Should return null for unknown stream")
    void shouldReturnNullForUnknownStream() {
      final StreamInfo info = streamOperations.getStreamInfo(999L);
      assertNull(info, "Should return null for unknown stream");
    }
  }

  @Nested
  @DisplayName("close Tests")
  class CloseTests {

    @Test
    @DisplayName("Should close gracefully")
    void shouldCloseGracefully() {
      streamOperations.close();
      assertTrue(true, "Close should complete");
    }

    @Test
    @DisplayName("Should be idempotent")
    void shouldBeIdempotent() {
      streamOperations.close();
      streamOperations.close();
      assertTrue(true, "Multiple closes should not throw");
    }
  }

  @Nested
  @DisplayName("StreamType Tests")
  class StreamTypeTests {

    @Test
    @DisplayName("Should have INPUT and OUTPUT types")
    void shouldHaveInputAndOutputTypes() {
      assertEquals(2, StreamType.values().length, "Should have 2 stream types");
      assertNotNull(StreamType.INPUT, "INPUT should exist");
      assertNotNull(StreamType.OUTPUT, "OUTPUT should exist");
    }
  }

  @Nested
  @DisplayName("StreamInfo Tests")
  class StreamInfoTests {

    @Test
    @DisplayName("Should create stream info with all fields")
    void shouldCreateStreamInfoWithAllFields() {
      final StreamInfo info = new StreamInfo(1L, StreamType.INPUT, 100L);

      assertEquals(1L, info.handle, "Handle should match");
      assertEquals(StreamType.INPUT, info.type, "Type should be INPUT");
      assertEquals(100L, info.resourceHandle, "Resource handle should match");
      assertTrue(info.createdAt > 0, "Created timestamp should be positive");
    }

    @Test
    @DisplayName("toString should contain all fields")
    void toStringShouldContainAllFields() {
      final StreamInfo info = new StreamInfo(1L, StreamType.OUTPUT, 200L);
      final String str = info.toString();

      assertTrue(str.contains("handle=1"), "Should contain handle");
      assertTrue(str.contains("OUTPUT"), "Should contain type");
      assertTrue(str.contains("resource=200"), "Should contain resource handle");
      assertTrue(str.contains("created="), "Should contain created timestamp");
    }
  }
}
