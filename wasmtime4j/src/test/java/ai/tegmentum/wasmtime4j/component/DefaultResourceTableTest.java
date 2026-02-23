/*
 * Copyright 2024 Tegmentum AI
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
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

/**
 * Tests for {@link DefaultResourceTable}.
 *
 * @since 1.0.0
 */
@DisplayName("DefaultResourceTable Tests")
class DefaultResourceTableTest {

  private DefaultResourceTable table;

  @BeforeEach
  void setUp() {
    table = new DefaultResourceTable();
  }

  @Nested
  @DisplayName("push() tests")
  class PushTests {

    @Test
    @DisplayName("should push an entry and return a positive handle")
    void shouldPushAndReturnPositiveHandle() throws WasmException {
      final int handle = table.push("test-entry");
      assertTrue(handle > 0, "Handle should be positive, got: " + handle);
    }

    @Test
    @DisplayName("should return unique handles for each push")
    void shouldReturnUniqueHandles() throws WasmException {
      final int handle1 = table.push("entry-1");
      final int handle2 = table.push("entry-2");
      final int handle3 = table.push("entry-3");
      assertNotEquals(handle1, handle2, "Handles should be unique");
      assertNotEquals(handle2, handle3, "Handles should be unique");
      assertNotEquals(handle1, handle3, "Handles should be unique");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null entry")
    void shouldThrowForNullEntry() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.push(null),
          "Should throw IllegalArgumentException for null entry");
    }

    @Test
    @DisplayName("should increment size after push")
    void shouldIncrementSizeAfterPush() throws WasmException {
      assertEquals(0, table.size(), "Initial size should be 0");
      table.push("entry-1");
      assertEquals(1, table.size(), "Size should be 1 after first push");
      table.push("entry-2");
      assertEquals(2, table.size(), "Size should be 2 after second push");
    }
  }

  @Nested
  @DisplayName("get() tests")
  class GetTests {

    @Test
    @DisplayName("should retrieve a pushed entry by handle")
    void shouldRetrievePushedEntry() throws WasmException {
      final String entry = "test-entry";
      final int handle = table.push(entry);
      final Optional<String> result = table.get(handle, String.class);
      assertTrue(result.isPresent(), "Entry should be present");
      assertEquals(entry, result.get(), "Retrieved entry should match pushed entry");
    }

    @Test
    @DisplayName("should return empty for invalid handle")
    void shouldReturnEmptyForInvalidHandle() {
      final Optional<String> result = table.get(999, String.class);
      assertFalse(result.isPresent(), "Should return empty for invalid handle");
    }

    @Test
    @DisplayName("should throw ClassCastException for wrong type")
    void shouldThrowForWrongType() throws WasmException {
      final int handle = table.push("string-entry");
      assertThrows(
          ClassCastException.class,
          () -> table.get(handle, Integer.class),
          "Should throw ClassCastException for wrong type");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null class")
    void shouldThrowForNullClass() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.get(1, null),
          "Should throw IllegalArgumentException for null class");
    }
  }

  @Nested
  @DisplayName("delete() tests")
  class DeleteTests {

    @Test
    @DisplayName("should delete and return a pushed entry")
    void shouldDeleteAndReturnEntry() throws WasmException {
      final String entry = "test-entry";
      final int handle = table.push(entry);
      final Optional<String> result = table.delete(handle, String.class);
      assertTrue(result.isPresent(), "Deleted entry should be present");
      assertEquals(entry, result.get(), "Deleted entry should match pushed entry");
    }

    @Test
    @DisplayName("should not find entry after deletion")
    void shouldNotFindAfterDeletion() throws WasmException {
      final int handle = table.push("test-entry");
      table.delete(handle, String.class);
      assertFalse(table.contains(handle), "Entry should not exist after deletion");
      final Optional<String> result = table.get(handle, String.class);
      assertFalse(result.isPresent(), "Get should return empty after deletion");
    }

    @Test
    @DisplayName("should return empty for non-existent handle")
    void shouldReturnEmptyForNonExistentHandle() {
      final Optional<String> result = table.delete(999, String.class);
      assertFalse(result.isPresent(), "Should return empty for non-existent handle");
    }

    @Test
    @DisplayName("should decrement size after deletion")
    void shouldDecrementSizeAfterDeletion() throws WasmException {
      final int handle = table.push("test-entry");
      assertEquals(1, table.size(), "Size should be 1 after push");
      table.delete(handle, String.class);
      assertEquals(0, table.size(), "Size should be 0 after deletion");
    }

    @Test
    @DisplayName("should throw IllegalArgumentException for null class")
    void shouldThrowForNullClass() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.delete(1, null),
          "Should throw IllegalArgumentException for null class");
    }
  }

  @Nested
  @DisplayName("contains() tests")
  class ContainsTests {

    @Test
    @DisplayName("should return true for existing entry")
    void shouldReturnTrueForExistingEntry() throws WasmException {
      final int handle = table.push("test-entry");
      assertTrue(table.contains(handle), "Should contain pushed entry");
    }

    @Test
    @DisplayName("should return false for non-existent handle")
    void shouldReturnFalseForNonExistentHandle() {
      assertFalse(table.contains(0), "Should not contain handle 0");
      assertFalse(table.contains(-1), "Should not contain negative handle");
      assertFalse(table.contains(999), "Should not contain non-existent handle");
    }
  }

  @Nested
  @DisplayName("size() tests")
  class SizeTests {

    @Test
    @DisplayName("should return 0 for empty table")
    void shouldReturnZeroForEmptyTable() {
      assertEquals(0, table.size(), "Empty table should have size 0");
    }

    @Test
    @DisplayName("should track size correctly through push and delete")
    void shouldTrackSizeCorrectly() throws WasmException {
      final int h1 = table.push("a");
      final int h2 = table.push("b");
      final int h3 = table.push("c");
      assertEquals(3, table.size(), "Size should be 3 after 3 pushes");

      table.delete(h2, String.class);
      assertEquals(2, table.size(), "Size should be 2 after 1 deletion");

      table.delete(h1, String.class);
      table.delete(h3, String.class);
      assertEquals(0, table.size(), "Size should be 0 after all deletions");
    }
  }

  @Nested
  @DisplayName("concurrency tests")
  class ConcurrencyTests {

    @Test
    @DisplayName("should handle concurrent pushes safely")
    void shouldHandleConcurrentPushes() throws Exception {
      final int threadCount = 10;
      final int pushesPerThread = 100;
      final ExecutorService executor = Executors.newFixedThreadPool(threadCount);
      final CountDownLatch startLatch = new CountDownLatch(1);
      final CountDownLatch doneLatch = new CountDownLatch(threadCount);
      final AtomicInteger errorCount = new AtomicInteger(0);

      for (int t = 0; t < threadCount; t++) {
        executor.submit(
            () -> {
              try {
                startLatch.await();
                for (int i = 0; i < pushesPerThread; i++) {
                  table.push("entry-" + Thread.currentThread().threadId() + "-" + i);
                }
              } catch (final Exception e) {
                errorCount.incrementAndGet();
              } finally {
                doneLatch.countDown();
              }
            });
      }

      startLatch.countDown();
      doneLatch.await();
      executor.shutdown();

      assertEquals(0, errorCount.get(), "No errors should occur during concurrent pushes");
      assertEquals(
          threadCount * pushesPerThread,
          table.size(),
          "All entries should be present after concurrent pushes");
    }
  }

  @Nested
  @DisplayName("mixed type tests")
  class MixedTypeTests {

    @Test
    @DisplayName("should store and retrieve different types")
    void shouldStoreDifferentTypes() throws WasmException {
      final int stringHandle = table.push("hello");
      final int intHandle = table.push(42);
      final int listHandle = table.push(java.util.List.of("a", "b"));

      assertEquals("hello", table.get(stringHandle, String.class).orElse(null));
      assertEquals(42, table.get(intHandle, Integer.class).orElse(null));
      assertEquals(
          java.util.List.of("a", "b"), table.get(listHandle, java.util.List.class).orElse(null));
    }
  }
}
