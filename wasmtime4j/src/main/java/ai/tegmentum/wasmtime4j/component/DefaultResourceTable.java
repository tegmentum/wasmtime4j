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

import ai.tegmentum.wasmtime4j.exception.ResourceTableException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Default implementation of {@link ResourceTable} using a {@link ConcurrentHashMap}.
 *
 * <p>This is a pure Java implementation with no native dependencies, shared by both JNI and Panama
 * runtime implementations. It provides thread-safe resource handle management for the Component
 * Model.
 *
 * <p>Resource handles are monotonically increasing integers starting from 1. Handle 0 is reserved
 * as an invalid handle. Deleted handles are not reused to prevent use-after-free bugs.
 *
 * <p>Supports parent-child resource relationships. A child resource is associated with a parent
 * handle, and the parent cannot be deleted while it has outstanding children. This mirrors
 * Wasmtime's {@code ResourceTable} parent-child semantics.
 *
 * @since 1.0.0
 */
public final class DefaultResourceTable implements ResourceTable {

  private final ConcurrentHashMap<Integer, Object> entries = new ConcurrentHashMap<>();
  private final AtomicInteger nextHandle = new AtomicInteger(1);
  private volatile int maxCapacity = Integer.MAX_VALUE;

  /** Maps parent handle to the set of its child handles. */
  private final ConcurrentHashMap<Integer, Set<Integer>> children = new ConcurrentHashMap<>();

  /** Maps child handle to its parent handle. */
  private final ConcurrentHashMap<Integer, Integer> childToParent = new ConcurrentHashMap<>();

  @Override
  public int maxCapacity() {
    return maxCapacity;
  }

  @Override
  public void setMaxCapacity(final int maxCapacity) {
    if (maxCapacity < 1) {
      throw new IllegalArgumentException("maxCapacity must be at least 1, got: " + maxCapacity);
    }
    this.maxCapacity = maxCapacity;
  }

  @Override
  public int push(final Object entry) throws WasmException {
    if (entry == null) {
      throw new IllegalArgumentException("entry cannot be null");
    }
    if (entries.size() >= maxCapacity) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.FULL,
          "Resource table is at capacity (" + maxCapacity + ")");
    }
    final int handle = nextHandle.getAndIncrement();
    entries.put(handle, entry);
    return handle;
  }

  @Override
  public <T> Optional<T> get(final int index, final Class<T> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("clazz cannot be null");
    }
    final Object entry = entries.get(index);
    if (entry == null) {
      return Optional.empty();
    }
    return Optional.of(clazz.cast(entry));
  }

  /**
   * Removes a resource entry from the table and returns it.
   *
   * <p>After removal, the index is no longer valid and may be reused for future entries. If the
   * entry is a parent with outstanding child resources, the deletion fails with a {@link
   * ResourceTableException} of kind {@link ResourceTableException.ErrorKind#HAS_CHILDREN}.
   *
   * <p>If the entry is a child resource, it is automatically removed from its parent's children
   * set.
   *
   * @param <T> the expected type of the resource entry
   * @param index the resource handle index
   * @param clazz the expected class of the entry
   * @return an Optional containing the removed entry, or empty if the index was invalid
   * @throws ResourceTableException if the entry has outstanding children
   * @throws ClassCastException if the entry is not of the expected type
   * @throws IllegalArgumentException if clazz is null
   */
  @Override
  public <T> Optional<T> delete(final int index, final Class<T> clazz) throws WasmException {
    if (clazz == null) {
      throw new IllegalArgumentException("clazz cannot be null");
    }

    // Check for outstanding children before deletion
    final Set<Integer> childSet = children.get(index);
    if (childSet != null && !childSet.isEmpty()) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.HAS_CHILDREN,
          "Cannot delete resource "
              + index
              + ": has "
              + childSet.size()
              + " outstanding child resource(s)");
    }

    final Object entry = entries.remove(index);
    if (entry == null) {
      return Optional.empty();
    }

    // Clean up parent-child relationships
    children.remove(index);
    final Integer parentHandle = childToParent.remove(index);
    if (parentHandle != null) {
      final Set<Integer> parentChildren = children.get(parentHandle);
      if (parentChildren != null) {
        parentChildren.remove(index);
      }
    }

    return Optional.of(clazz.cast(entry));
  }

  @Override
  public boolean contains(final int index) {
    return entries.containsKey(index);
  }

  @Override
  public int size() {
    return entries.size();
  }

  @Override
  public int pushChild(final Object entry, final int parentHandle) throws WasmException {
    if (entry == null) {
      throw new IllegalArgumentException("entry cannot be null");
    }
    if (!entries.containsKey(parentHandle)) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          "Parent resource " + parentHandle + " not found in table");
    }
    if (entries.size() >= maxCapacity) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.FULL,
          "Resource table is at capacity (" + maxCapacity + ")");
    }

    final int childHandle = nextHandle.getAndIncrement();
    entries.put(childHandle, entry);

    // Register parent-child relationship
    children.computeIfAbsent(parentHandle, k -> ConcurrentHashMap.newKeySet()).add(childHandle);
    childToParent.put(childHandle, parentHandle);

    return childHandle;
  }

  @Override
  public List<Integer> iterChildren(final int parentHandle) throws WasmException {
    if (!entries.containsKey(parentHandle)) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          "Parent resource " + parentHandle + " not found in table");
    }

    final Set<Integer> childSet = children.get(parentHandle);
    if (childSet == null || childSet.isEmpty()) {
      return Collections.emptyList();
    }
    return List.copyOf(childSet);
  }

  @Override
  public void addChild(final int childHandle, final int parentHandle) throws WasmException {
    if (!entries.containsKey(childHandle)) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          "Child resource " + childHandle + " not found in table");
    }
    if (!entries.containsKey(parentHandle)) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          "Parent resource " + parentHandle + " not found in table");
    }
    if (childToParent.containsKey(childHandle)) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.HAS_PARENT,
          "Child resource "
              + childHandle
              + " already has parent "
              + childToParent.get(childHandle));
    }

    children.computeIfAbsent(parentHandle, k -> ConcurrentHashMap.newKeySet()).add(childHandle);
    childToParent.put(childHandle, parentHandle);
  }

  @Override
  public void removeChild(final int childHandle, final int parentHandle) throws WasmException {
    if (!entries.containsKey(childHandle)) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          "Child resource " + childHandle + " not found in table");
    }
    if (!entries.containsKey(parentHandle)) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          "Parent resource " + parentHandle + " not found in table");
    }

    final Integer actualParent = childToParent.get(childHandle);
    if (actualParent == null || actualParent != parentHandle) {
      throw new ResourceTableException(
          ResourceTableException.ErrorKind.NOT_PRESENT,
          "Child resource " + childHandle + " is not a child of parent resource " + parentHandle);
    }

    childToParent.remove(childHandle);
    final Set<Integer> parentChildren = children.get(parentHandle);
    if (parentChildren != null) {
      parentChildren.remove(childHandle);
    }
  }
}
