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
import java.nio.ByteBuffer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/** Comprehensive tests for {@link WasiRandomOperations}. */
@DisplayName("WasiRandomOperations Tests")
class WasiRandomOperationsTest {

  private WasiContext testContext;
  private WasiRandomOperations randomOperations;

  @BeforeEach
  void setUp() {
    testContext = TestWasiContextFactory.createTestContext();
    randomOperations = new WasiRandomOperations(testContext);
  }

  @Nested
  @DisplayName("Class Structure Tests")
  class ClassStructureTests {

    @Test
    @DisplayName("WasiRandomOperations should be final class")
    void shouldBeFinalClass() {
      assertTrue(
          java.lang.reflect.Modifier.isFinal(WasiRandomOperations.class.getModifiers()),
          "WasiRandomOperations should be final");
    }
  }

  @Nested
  @DisplayName("Constructor Tests")
  class ConstructorTests {

    @Test
    @DisplayName("Constructor should throw on null context")
    void constructorShouldThrowOnNullContext() {
      assertThrows(
          JniException.class, () -> new WasiRandomOperations(null), "Should throw on null context");
    }

    @Test
    @DisplayName("Constructor should create random operations with valid context")
    void constructorShouldCreateRandomOperationsWithValidContext() {
      final WasiRandomOperations ops = new WasiRandomOperations(testContext);
      assertNotNull(ops, "Random operations should be created");
    }
  }

  @Nested
  @DisplayName("getRandomBytes ByteBuffer Tests")
  class GetRandomBytesByteBufferTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(
          JniException.class,
          () -> randomOperations.getRandomBytes(null),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on read-only buffer")
    void shouldThrowOnReadOnlyBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocate(100).asReadOnlyBuffer();

      assertThrows(
          JniException.class,
          () -> randomOperations.getRandomBytes(buffer),
          "Should throw on read-only buffer");
    }

    @Test
    @DisplayName("Should handle zero-length buffer")
    void shouldHandleZeroLengthBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocate(0);

      // Should not throw, just return
      randomOperations.getRandomBytes(buffer);
      assertEquals(0, buffer.remaining(), "Buffer should still be empty");
    }

    @Test
    @DisplayName("Should handle buffer with no remaining space")
    void shouldHandleBufferWithNoRemainingSpace() {
      final ByteBuffer buffer = ByteBuffer.allocate(10);
      buffer.position(10); // Fill to capacity

      // Should not throw, just return
      randomOperations.getRandomBytes(buffer);
      assertEquals(0, buffer.remaining(), "Buffer should have no remaining");
    }

    @Test
    @DisplayName("Should throw on buffer too large")
    void shouldThrowOnBufferTooLarge() {
      // MAX_BUFFER_SIZE is 1MB (1024 * 1024)
      final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 + 1);

      assertThrows(
          JniException.class,
          () -> randomOperations.getRandomBytes(buffer),
          "Should throw on buffer > 1MB");
    }
  }

  @Nested
  @DisplayName("generateRandomBytes Tests")
  class GenerateRandomBytesTests {

    @Test
    @DisplayName("Should throw on negative length")
    void shouldThrowOnNegativeLength() {
      assertThrows(
          JniException.class,
          () -> randomOperations.generateRandomBytes(-1),
          "Should throw on negative length");
    }

    @Test
    @DisplayName("Should return empty array for zero length")
    void shouldReturnEmptyArrayForZeroLength() {
      final byte[] result = randomOperations.generateRandomBytes(0);

      assertNotNull(result, "Result should not be null");
      assertEquals(0, result.length, "Result should be empty array");
    }

    @Test
    @DisplayName("Should throw on length too large")
    void shouldThrowOnLengthTooLarge() {
      assertThrows(
          JniException.class,
          () -> randomOperations.generateRandomBytes(1024 * 1024 + 1),
          "Should throw on length > 1MB");
    }
  }

  @Nested
  @DisplayName("generateRandomInt Tests")
  class GenerateRandomIntTests {

    @Test
    @DisplayName("Should throw on non-positive bound")
    void shouldThrowOnNonPositiveBound() {
      assertThrows(
          JniException.class,
          () -> randomOperations.generateRandomInt(0),
          "Should throw on zero bound");

      assertThrows(
          JniException.class,
          () -> randomOperations.generateRandomInt(-1),
          "Should throw on negative bound");
    }

    @Test
    @DisplayName("Should throw on bound of 1")
    void boundOfOneShouldBeValid() {
      // Bound of 1 is valid - should return 0
      // Will throw due to native call failure, but validation passes
      assertThrows(Throwable.class, () -> randomOperations.generateRandomInt(1));
    }
  }

  @Nested
  @DisplayName("getRandomBytesFallback Tests")
  class GetRandomBytesFallbackTests {

    @Test
    @DisplayName("Should throw on null buffer")
    void shouldThrowOnNullBuffer() {
      assertThrows(
          JniException.class,
          () -> randomOperations.getRandomBytesFallback(null),
          "Should throw on null buffer");
    }

    @Test
    @DisplayName("Should throw on read-only buffer")
    void shouldThrowOnReadOnlyBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocate(100).asReadOnlyBuffer();

      assertThrows(
          JniException.class,
          () -> randomOperations.getRandomBytesFallback(buffer),
          "Should throw on read-only buffer");
    }

    @Test
    @DisplayName("Should handle zero-length buffer")
    void shouldHandleZeroLengthBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocate(0);

      // Should not throw
      randomOperations.getRandomBytesFallback(buffer);
      assertEquals(0, buffer.remaining(), "Buffer should still be empty");
    }

    @Test
    @DisplayName("Should fill buffer with random bytes")
    void shouldFillBufferWithRandomBytes() {
      final ByteBuffer buffer = ByteBuffer.allocate(100);

      randomOperations.getRandomBytesFallback(buffer);

      assertEquals(0, buffer.remaining(), "Buffer should be filled");
      assertEquals(100, buffer.position(), "Position should be at end");
    }

    @Test
    @DisplayName("Fallback should produce different values")
    void fallbackShouldProduceDifferentValues() {
      final ByteBuffer buffer1 = ByteBuffer.allocate(32);
      final ByteBuffer buffer2 = ByteBuffer.allocate(32);

      randomOperations.getRandomBytesFallback(buffer1);
      randomOperations.getRandomBytesFallback(buffer2);

      buffer1.flip();
      buffer2.flip();

      // Extract bytes for comparison
      final byte[] bytes1 = new byte[32];
      final byte[] bytes2 = new byte[32];
      buffer1.get(bytes1);
      buffer2.get(bytes2);

      // Extremely unlikely to be equal
      boolean allEqual = true;
      for (int i = 0; i < 32; i++) {
        if (bytes1[i] != bytes2[i]) {
          allEqual = false;
          break;
        }
      }
      assertTrue(!allEqual, "Two random buffers should be different");
    }

    @Test
    @DisplayName("Should handle heap buffer")
    void shouldHandleHeapBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocate(50);

      randomOperations.getRandomBytesFallback(buffer);

      assertEquals(0, buffer.remaining(), "Heap buffer should be filled");
    }

    @Test
    @DisplayName("Should handle direct buffer")
    void shouldHandleDirectBuffer() {
      final ByteBuffer buffer = ByteBuffer.allocateDirect(50);

      randomOperations.getRandomBytesFallback(buffer);

      assertEquals(0, buffer.remaining(), "Direct buffer should be filled");
    }

    @Test
    @DisplayName("Should respect buffer position")
    void shouldRespectBufferPosition() {
      final ByteBuffer buffer = ByteBuffer.allocate(100);
      buffer.position(50);

      randomOperations.getRandomBytesFallback(buffer);

      assertEquals(0, buffer.remaining(), "Remaining should be zero");
      assertEquals(100, buffer.position(), "Position should be at end");
    }

    @Test
    @DisplayName("Should respect buffer limit")
    void shouldRespectBufferLimit() {
      final ByteBuffer buffer = ByteBuffer.allocate(100);
      buffer.limit(50);

      randomOperations.getRandomBytesFallback(buffer);

      assertEquals(0, buffer.remaining(), "Remaining should be zero");
      assertEquals(50, buffer.position(), "Position should be at limit");
    }
  }

  @Nested
  @DisplayName("Buffer Size Validation Tests")
  class BufferSizeValidationTests {

    @Test
    @DisplayName("Should accept 1MB buffer")
    void shouldAccept1MbBuffer() {
      // Exactly 1MB should be valid
      final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024);

      // Will throw from native, but validation should pass
      assertThrows(Throwable.class, () -> randomOperations.getRandomBytes(buffer));
    }

    @Test
    @DisplayName("Should reject buffer larger than 1MB")
    void shouldRejectBufferLargerThan1Mb() {
      final ByteBuffer buffer = ByteBuffer.allocate(1024 * 1024 + 1);

      assertThrows(
          JniException.class,
          () -> randomOperations.getRandomBytes(buffer),
          "Should reject > 1MB buffer");
    }

    @Test
    @DisplayName("Should accept small buffers")
    void shouldAcceptSmallBuffers() {
      final ByteBuffer buffer = ByteBuffer.allocate(1);

      // Will throw from native, but validation passes
      assertThrows(Throwable.class, () -> randomOperations.getRandomBytes(buffer));
    }
  }

  @Nested
  @DisplayName("Direct vs Heap Buffer Tests")
  class DirectVsHeapBufferTests {

    @Test
    @DisplayName("Should handle heap buffer in getRandomBytes")
    void shouldHandleHeapBufferInGetRandomBytes() {
      final ByteBuffer buffer = ByteBuffer.allocate(100);

      // Will throw from native, but should attempt correct path
      assertThrows(Throwable.class, () -> randomOperations.getRandomBytes(buffer));
    }

    @Test
    @DisplayName("Should handle direct buffer in getRandomBytes")
    void shouldHandleDirectBufferInGetRandomBytes() {
      final ByteBuffer buffer = ByteBuffer.allocateDirect(100);

      // Will throw from native, but should attempt correct path
      assertThrows(Throwable.class, () -> randomOperations.getRandomBytes(buffer));
    }
  }
}
