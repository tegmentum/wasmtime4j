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
package ai.tegmentum.wasmtime4j.component;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link VecBuffer} class.
 *
 * <p>VecBuffer batches ComponentVal instances for stream I/O operations.
 */
@DisplayName("VecBuffer Tests")
class VecBufferTest {

  @Nested
  @DisplayName("Factory Tests")
  class FactoryTests {

    @Test
    @DisplayName("should create with valid capacity")
    void shouldCreateWithValidCapacity() {
      final VecBuffer buffer = VecBuffer.withCapacity(10);
      assertTrue(buffer.isEmpty(), "New buffer should be empty");
      assertEquals(0, buffer.size(), "New buffer size should be 0");
    }

    @Test
    @DisplayName("should create with zero capacity")
    void shouldCreateWithZeroCapacity() {
      final VecBuffer buffer = VecBuffer.withCapacity(0);
      assertTrue(buffer.isEmpty());
      assertEquals(0, buffer.size());
    }

    @Test
    @DisplayName("should throw on negative capacity")
    void shouldThrowOnNegativeCapacity() {
      final IllegalArgumentException exception =
          assertThrows(IllegalArgumentException.class, () -> VecBuffer.withCapacity(-1));
      assertTrue(
          exception.getMessage().contains("-1"),
          "Exception should mention the invalid capacity: " + exception.getMessage());
    }
  }

  @Nested
  @DisplayName("Push Tests")
  class PushTests {

    @Test
    @DisplayName("should push values and track size")
    void shouldPushValuesAndTrackSize() {
      final VecBuffer buffer = VecBuffer.withCapacity(4);
      buffer.push(ComponentVal.s32(1));
      assertEquals(1, buffer.size());
      assertFalse(buffer.isEmpty());

      buffer.push(ComponentVal.s32(2));
      buffer.push(ComponentVal.s32(3));
      assertEquals(3, buffer.size());
    }

    @Test
    @DisplayName("should throw on null push")
    void shouldThrowOnNullPush() {
      final VecBuffer buffer = VecBuffer.withCapacity(4);
      assertThrows(IllegalArgumentException.class, () -> buffer.push(null));
    }

    @Test
    @DisplayName("should grow beyond initial capacity")
    void shouldGrowBeyondInitialCapacity() {
      final VecBuffer buffer = VecBuffer.withCapacity(2);
      buffer.push(ComponentVal.s32(1));
      buffer.push(ComponentVal.s32(2));
      buffer.push(ComponentVal.s32(3)); // Beyond initial capacity
      assertEquals(3, buffer.size());
    }
  }

  @Nested
  @DisplayName("Drain Tests")
  class DrainTests {

    @Test
    @DisplayName("should drain all values and empty the buffer")
    void shouldDrainAllValuesAndEmptyBuffer() {
      final VecBuffer buffer = VecBuffer.withCapacity(4);
      buffer.push(ComponentVal.s32(10));
      buffer.push(ComponentVal.string("hello"));
      buffer.push(ComponentVal.bool(true));

      final List<ComponentVal> drained = buffer.drain();
      assertEquals(3, drained.size(), "Drained list should have 3 elements");
      assertTrue(buffer.isEmpty(), "Buffer should be empty after drain");
      assertEquals(0, buffer.size());

      // Verify values in order
      assertEquals(10, drained.get(0).asS32());
      assertEquals("hello", drained.get(1).asString());
      assertTrue(drained.get(2).asBool());
    }

    @Test
    @DisplayName("drain on empty buffer should return empty list")
    void drainOnEmptyBufferShouldReturnEmptyList() {
      final VecBuffer buffer = VecBuffer.withCapacity(4);
      final List<ComponentVal> drained = buffer.drain();
      assertTrue(drained.isEmpty(), "Drain on empty buffer should return empty list");
    }

    @Test
    @DisplayName("drained list should be unmodifiable")
    void drainedListShouldBeUnmodifiable() {
      final VecBuffer buffer = VecBuffer.withCapacity(4);
      buffer.push(ComponentVal.s32(1));
      final List<ComponentVal> drained = buffer.drain();
      assertThrows(UnsupportedOperationException.class, () -> drained.add(ComponentVal.s32(2)));
    }

    @Test
    @DisplayName("should support push after drain")
    void shouldSupportPushAfterDrain() {
      final VecBuffer buffer = VecBuffer.withCapacity(4);
      buffer.push(ComponentVal.s32(1));
      buffer.drain();

      buffer.push(ComponentVal.s32(2));
      assertEquals(1, buffer.size());
      final List<ComponentVal> drained = buffer.drain();
      assertEquals(1, drained.size());
      assertEquals(2, drained.get(0).asS32());
    }
  }
}
