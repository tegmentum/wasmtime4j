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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;
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
 * @since 1.0.0
 */
public final class DefaultResourceTable implements ResourceTable {

  private final ConcurrentHashMap<Integer, Object> entries = new ConcurrentHashMap<>();
  private final AtomicInteger nextHandle = new AtomicInteger(1);

  @Override
  public int push(final Object entry) throws WasmException {
    if (entry == null) {
      throw new IllegalArgumentException("entry cannot be null");
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

  @Override
  public <T> Optional<T> delete(final int index, final Class<T> clazz) {
    if (clazz == null) {
      throw new IllegalArgumentException("clazz cannot be null");
    }
    final Object entry = entries.remove(index);
    if (entry == null) {
      return Optional.empty();
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
}
