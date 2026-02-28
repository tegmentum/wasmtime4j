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

import ai.tegmentum.wasmtime4j.exception.ResourceTableException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
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
    void shouldReturnEmptyForNonExistentHandle() throws WasmException {
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

    @Test
    @DisplayName("should throw ResourceTableException when deleting parent with children")
    void shouldThrowWhenDeletingParentWithChildren() throws WasmException {
      final int parent = table.push("parent-entry");
      table.pushChild("child-entry", parent);

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.delete(parent, String.class),
              "Should throw ResourceTableException when parent has children");
      assertEquals(
          ResourceTableException.ErrorKind.HAS_CHILDREN,
          ex.getErrorKind(),
          "Error kind should be HAS_CHILDREN");
      assertTrue(
          ex.getMessage().contains("1 outstanding child"),
          "Message should mention child count, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("should allow deleting parent after all children are deleted")
    void shouldAllowDeletingParentAfterChildrenDeleted() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child1 = table.pushChild("child-1", parent);
      final int child2 = table.pushChild("child-2", parent);

      // Cannot delete parent yet
      assertThrows(
          ResourceTableException.class,
          () -> table.delete(parent, String.class),
          "Should throw when parent still has children");

      // Delete children first
      table.delete(child1, String.class);
      table.delete(child2, String.class);

      // Now parent can be deleted
      final Optional<String> result = table.delete(parent, String.class);
      assertTrue(result.isPresent(), "Parent should be deletable after children removed");
      assertEquals("parent-entry", result.get(), "Should return parent entry");
    }

    @Test
    @DisplayName("should remove child from parent's children set on deletion")
    void shouldRemoveChildFromParentOnDeletion() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.pushChild("child-entry", parent);

      // Delete the child
      table.delete(child, String.class);

      // Parent should now have no children
      final List<Integer> remainingChildren = table.iterChildren(parent);
      assertTrue(
          remainingChildren.isEmpty(),
          "Parent should have no children after child deletion, but has: " + remainingChildren);

      // Parent should now be deletable
      final Optional<String> result = table.delete(parent, String.class);
      assertTrue(result.isPresent(), "Parent should be deletable after child removed");
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
  @DisplayName("size() and isEmpty() tests")
  class SizeTests {

    @Test
    @DisplayName("should return 0 for empty table")
    void shouldReturnZeroForEmptyTable() {
      assertEquals(0, table.size(), "Empty table should have size 0");
    }

    @Test
    @DisplayName("isEmpty should return true for empty table")
    void shouldReturnTrueForEmptyTable() {
      assertTrue(table.isEmpty(), "Empty table should report isEmpty() as true");
    }

    @Test
    @DisplayName("isEmpty should return false after push")
    void shouldReturnFalseAfterPush() throws WasmException {
      table.push("entry");
      assertFalse(table.isEmpty(), "Non-empty table should report isEmpty() as false");
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
      assertTrue(table.isEmpty(), "Table should be empty after all deletions");
    }

    @Test
    @DisplayName("size should include child entries")
    void shouldIncludeChildEntriesInSize() throws WasmException {
      final int parent = table.push("parent");
      table.pushChild("child-1", parent);
      table.pushChild("child-2", parent);
      assertEquals(3, table.size(), "Size should include parent and 2 children");
    }
  }

  @Nested
  @DisplayName("pushChild() tests")
  class PushChildTests {

    @Test
    @DisplayName("should push a child entry and return a positive handle")
    void shouldPushChildAndReturnPositiveHandle() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.pushChild("child-entry", parent);
      assertTrue(child > 0, "Child handle should be positive, got: " + child);
      assertNotEquals(parent, child, "Child handle should differ from parent");
    }

    @Test
    @DisplayName("should allow retrieving child entry by handle")
    void shouldRetrieveChildEntry() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.pushChild("child-entry", parent);
      final Optional<String> result = table.get(child, String.class);
      assertTrue(result.isPresent(), "Child entry should be retrievable");
      assertEquals("child-entry", result.get(), "Child entry value should match");
    }

    @Test
    @DisplayName("should throw for null entry")
    void shouldThrowForNullEntry() throws WasmException {
      final int parent = table.push("parent-entry");
      assertThrows(
          IllegalArgumentException.class,
          () -> table.pushChild(null, parent),
          "Should throw IllegalArgumentException for null child entry");
    }

    @Test
    @DisplayName("should throw ResourceTableException for invalid parent handle")
    void shouldThrowForInvalidParent() {
      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.pushChild("child-entry", 999),
              "Should throw ResourceTableException for invalid parent");
      assertEquals(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          ex.getErrorKind(),
          "Error kind should be NOT_PRESENT");
    }

    @Test
    @DisplayName("should support multiple children for one parent")
    void shouldSupportMultipleChildren() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child1 = table.pushChild("child-1", parent);
      final int child2 = table.pushChild("child-2", parent);
      final int child3 = table.pushChild("child-3", parent);

      assertTrue(table.contains(child1), "Child 1 should exist");
      assertTrue(table.contains(child2), "Child 2 should exist");
      assertTrue(table.contains(child3), "Child 3 should exist");

      assertEquals("child-1", table.get(child1, String.class).orElse(null));
      assertEquals("child-2", table.get(child2, String.class).orElse(null));
      assertEquals("child-3", table.get(child3, String.class).orElse(null));
    }

    @Test
    @DisplayName("should support nested children (child of a child)")
    void shouldSupportNestedChildren() throws WasmException {
      final int grandparent = table.push("grandparent");
      final int parent = table.pushChild("parent", grandparent);
      final int child = table.pushChild("child", parent);

      assertTrue(table.contains(child), "Grandchild should exist");
      assertEquals("child", table.get(child, String.class).orElse(null));

      // Cannot delete parent while it has children
      assertThrows(
          ResourceTableException.class,
          () -> table.delete(parent, String.class),
          "Cannot delete parent that has children");

      // Delete child first, then parent, then grandparent
      table.delete(child, String.class);
      table.delete(parent, String.class);
      table.delete(grandparent, String.class);
      assertTrue(table.isEmpty(), "Table should be empty after full cleanup");
    }
  }

  @Nested
  @DisplayName("iterChildren() tests")
  class IterChildrenTests {

    @Test
    @DisplayName("should return empty list for entry with no children")
    void shouldReturnEmptyForNoChildren() throws WasmException {
      final int parent = table.push("parent-entry");
      final List<Integer> childHandles = table.iterChildren(parent);
      assertTrue(childHandles.isEmpty(), "Should return empty list for entry with no children");
    }

    @Test
    @DisplayName("should return child handles")
    void shouldReturnChildHandles() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child1 = table.pushChild("child-1", parent);
      final int child2 = table.pushChild("child-2", parent);

      final List<Integer> childHandles = table.iterChildren(parent);
      assertEquals(2, childHandles.size(), "Should have 2 children");
      assertTrue(childHandles.contains(child1), "Should contain child 1 handle: " + child1);
      assertTrue(childHandles.contains(child2), "Should contain child 2 handle: " + child2);
    }

    @Test
    @DisplayName("should throw ResourceTableException for invalid parent handle")
    void shouldThrowForInvalidParent() {
      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.iterChildren(999),
              "Should throw ResourceTableException for invalid parent");
      assertEquals(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          ex.getErrorKind(),
          "Error kind should be NOT_PRESENT");
    }

    @Test
    @DisplayName("should return updated list after child deletion")
    void shouldReflectChildDeletion() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child1 = table.pushChild("child-1", parent);
      final int child2 = table.pushChild("child-2", parent);

      // Delete one child
      table.delete(child1, String.class);

      final List<Integer> remaining = table.iterChildren(parent);
      assertEquals(1, remaining.size(), "Should have 1 remaining child");
      assertTrue(remaining.contains(child2), "Should still contain child 2");
      assertFalse(remaining.contains(child1), "Should not contain deleted child 1");
    }

    @Test
    @DisplayName("returned list should be an immutable snapshot")
    void shouldReturnImmutableSnapshot() throws WasmException {
      final int parent = table.push("parent-entry");
      table.pushChild("child-1", parent);

      final List<Integer> childHandles = table.iterChildren(parent);
      assertThrows(
          UnsupportedOperationException.class,
          () -> childHandles.add(999),
          "Returned list should be immutable");
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

    @Test
    @DisplayName("should handle concurrent pushChild safely")
    void shouldHandleConcurrentPushChild() throws Exception {
      final int parent = table.push("parent-entry");
      final int threadCount = 10;
      final int pushesPerThread = 50;
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
                  table.pushChild("child-" + Thread.currentThread().threadId() + "-" + i, parent);
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

      assertEquals(0, errorCount.get(), "No errors should occur during concurrent pushChild");
      final int expectedTotal = 1 + (threadCount * pushesPerThread);
      assertEquals(expectedTotal, table.size(), "Total entries should be parent + all children");
      final List<Integer> childHandles = table.iterChildren(parent);
      assertEquals(
          threadCount * pushesPerThread,
          childHandles.size(),
          "All children should be tracked under parent");
    }
  }

  @Nested
  @DisplayName("addChild() tests")
  class AddChildTests {

    @Test
    @DisplayName("should add existing entry as child of existing parent")
    void shouldAddChildToParent() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.push("child-entry");

      table.addChild(child, parent);

      final List<Integer> children = table.iterChildren(parent);
      assertEquals(1, children.size(), "Parent should have 1 child");
      assertTrue(children.contains(child), "Children should contain the added child handle");
    }

    @Test
    @DisplayName("should prevent parent deletion after addChild")
    void shouldPreventParentDeletionAfterAddChild() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.push("child-entry");

      table.addChild(child, parent);

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.delete(parent, String.class),
              "Should not be able to delete parent with dynamically added child");
      assertEquals(
          ResourceTableException.ErrorKind.HAS_CHILDREN,
          ex.getErrorKind(),
          "Error kind should be HAS_CHILDREN");
    }

    @Test
    @DisplayName("should throw for invalid child handle")
    void shouldThrowForInvalidChild() throws WasmException {
      final int parent = table.push("parent-entry");

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.addChild(999, parent),
              "Should throw for invalid child handle");
      assertEquals(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          ex.getErrorKind(),
          "Error kind should be NOT_PRESENT");
    }

    @Test
    @DisplayName("should throw for invalid parent handle")
    void shouldThrowForInvalidParent() throws WasmException {
      final int child = table.push("child-entry");

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.addChild(child, 999),
              "Should throw for invalid parent handle");
      assertEquals(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          ex.getErrorKind(),
          "Error kind should be NOT_PRESENT");
    }

    @Test
    @DisplayName("should throw if child already has a parent")
    void shouldThrowIfChildAlreadyHasParent() throws WasmException {
      final int parent1 = table.push("parent-1");
      final int parent2 = table.push("parent-2");
      final int child = table.push("child-entry");

      table.addChild(child, parent1);

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.addChild(child, parent2),
              "Should throw when child already has a parent");
      assertEquals(
          ResourceTableException.ErrorKind.HAS_PARENT,
          ex.getErrorKind(),
          "Error kind should be HAS_PARENT");
    }

    @Test
    @DisplayName("should coexist with pushChild entries")
    void shouldCoexistWithPushChild() throws WasmException {
      final int parent = table.push("parent-entry");
      final int pushChildHandle = table.pushChild("push-child", parent);
      final int addChildHandle = table.push("add-child");

      table.addChild(addChildHandle, parent);

      final List<Integer> children = table.iterChildren(parent);
      assertEquals(2, children.size(), "Parent should have 2 children");
      assertTrue(children.contains(pushChildHandle), "Should contain pushChild entry");
      assertTrue(children.contains(addChildHandle), "Should contain addChild entry");
    }
  }

  @Nested
  @DisplayName("removeChild() tests")
  class RemoveChildTests {

    @Test
    @DisplayName("should remove parent-child relationship without deleting either entry")
    void shouldRemoveRelationshipOnly() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.push("child-entry");

      table.addChild(child, parent);
      table.removeChild(child, parent);

      // Both entries should still exist
      assertTrue(table.contains(parent), "Parent should still exist after removeChild");
      assertTrue(table.contains(child), "Child should still exist after removeChild");

      // Parent should have no children
      final List<Integer> children = table.iterChildren(parent);
      assertTrue(children.isEmpty(), "Parent should have no children after removeChild");

      // Parent should now be deletable
      table.delete(parent, String.class);
      assertFalse(table.contains(parent), "Parent should be deletable after removeChild");
    }

    @Test
    @DisplayName("should work with pushChild-created relationships")
    void shouldWorkWithPushChildRelationships() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.pushChild("child-entry", parent);

      table.removeChild(child, parent);

      final List<Integer> children = table.iterChildren(parent);
      assertTrue(children.isEmpty(), "Parent should have no children after removeChild");

      // Parent should now be deletable
      table.delete(parent, String.class);
    }

    @Test
    @DisplayName("should throw for invalid child handle")
    void shouldThrowForInvalidChild() throws WasmException {
      final int parent = table.push("parent-entry");

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.removeChild(999, parent),
              "Should throw for invalid child handle");
      assertEquals(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          ex.getErrorKind(),
          "Error kind should be NOT_PRESENT");
    }

    @Test
    @DisplayName("should throw for invalid parent handle")
    void shouldThrowForInvalidParent() throws WasmException {
      final int child = table.push("child-entry");

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.removeChild(child, 999),
              "Should throw for invalid parent handle");
      assertEquals(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          ex.getErrorKind(),
          "Error kind should be NOT_PRESENT");
    }

    @Test
    @DisplayName("should throw if relationship does not exist")
    void shouldThrowIfRelationshipNotExists() throws WasmException {
      final int parent = table.push("parent-entry");
      final int child = table.push("child-entry");

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.removeChild(child, parent),
              "Should throw when no relationship exists");
      assertEquals(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          ex.getErrorKind(),
          "Error kind should be NOT_PRESENT");
    }

    @Test
    @DisplayName("should allow re-adding child to different parent after removal")
    void shouldAllowReaddingAfterRemoval() throws WasmException {
      final int parent1 = table.push("parent-1");
      final int parent2 = table.push("parent-2");
      final int child = table.push("child-entry");

      // Add to parent1, then remove, then add to parent2
      table.addChild(child, parent1);
      table.removeChild(child, parent1);
      table.addChild(child, parent2);

      assertTrue(
          table.iterChildren(parent1).isEmpty(), "Parent1 should have no children after removal");
      assertEquals(
          1, table.iterChildren(parent2).size(), "Parent2 should have 1 child after re-add");
      assertTrue(
          table.iterChildren(parent2).contains(child), "Parent2 children should contain the child");
    }
  }

  @Nested
  @DisplayName("maxCapacity tests")
  class MaxCapacityTests {

    @Test
    @DisplayName("default max capacity should be Integer.MAX_VALUE")
    void shouldHaveDefaultMaxCapacity() {
      assertEquals(
          Integer.MAX_VALUE,
          table.maxCapacity(),
          "Default max capacity should be Integer.MAX_VALUE");
    }

    @Test
    @DisplayName("should allow setting and getting max capacity")
    void shouldSetAndGetMaxCapacity() {
      table.setMaxCapacity(100);
      assertEquals(100, table.maxCapacity(), "Max capacity should be 100 after setting");
    }

    @Test
    @DisplayName("should throw when push exceeds capacity")
    void shouldThrowWhenPushExceedsCapacity() throws WasmException {
      table.setMaxCapacity(2);
      table.push("entry-1");
      table.push("entry-2");

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.push("entry-3"),
              "Should throw when exceeding capacity");
      assertEquals(
          ResourceTableException.ErrorKind.FULL, ex.getErrorKind(), "Error kind should be FULL");
      assertTrue(
          ex.getMessage().contains("2"),
          "Message should mention capacity, got: " + ex.getMessage());
    }

    @Test
    @DisplayName("should throw when pushChild exceeds capacity")
    void shouldThrowWhenPushChildExceedsCapacity() throws WasmException {
      table.setMaxCapacity(2);
      final int parent = table.push("parent");
      table.pushChild("child-1", parent);

      final ResourceTableException ex =
          assertThrows(
              ResourceTableException.class,
              () -> table.pushChild("child-2", parent),
              "Should throw when pushChild exceeds capacity");
      assertEquals(
          ResourceTableException.ErrorKind.FULL, ex.getErrorKind(), "Error kind should be FULL");
    }

    @Test
    @DisplayName("should allow push after deletion frees space")
    void shouldAllowPushAfterDeletionFreesSpace() throws WasmException {
      table.setMaxCapacity(2);
      final int h1 = table.push("entry-1");
      table.push("entry-2");

      // Delete one to free space
      table.delete(h1, String.class);

      // Should now be able to push again
      final int h3 = table.push("entry-3");
      assertTrue(table.contains(h3), "New entry should exist after freeing space");
      assertEquals(2, table.size(), "Size should be 2");
    }

    @Test
    @DisplayName("should throw for max capacity less than 1")
    void shouldThrowForInvalidMaxCapacity() {
      assertThrows(
          IllegalArgumentException.class,
          () -> table.setMaxCapacity(0),
          "Should throw for maxCapacity of 0");
      assertThrows(
          IllegalArgumentException.class,
          () -> table.setMaxCapacity(-1),
          "Should throw for negative maxCapacity");
    }

    @Test
    @DisplayName("should not evict existing entries when lowering capacity")
    void shouldNotEvictExistingOnLowerCapacity() throws WasmException {
      table.push("entry-1");
      table.push("entry-2");
      table.push("entry-3");

      table.setMaxCapacity(1);

      // Existing entries should still be accessible
      assertEquals(3, table.size(), "Existing entries should not be evicted");

      // But new pushes should fail
      assertThrows(
          ResourceTableException.class,
          () -> table.push("entry-4"),
          "Should not allow new entries when over capacity");
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

    @Test
    @DisplayName("should support children of different types than parent")
    void shouldSupportMixedTypeChildren() throws WasmException {
      final int parent = table.push("string-parent");
      final int intChild = table.pushChild(42, parent);
      final int listChild = table.pushChild(java.util.List.of("x"), parent);

      assertEquals(42, table.get(intChild, Integer.class).orElse(null));
      assertEquals(java.util.List.of("x"), table.get(listChild, java.util.List.class).orElse(null));

      final List<Integer> children = table.iterChildren(parent);
      assertEquals(2, children.size(), "Parent should have 2 children of different types");
    }
  }
}
