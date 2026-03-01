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
package ai.tegmentum.wasmtime4j.config;

import ai.tegmentum.wasmtime4j.type.MemoryType;
import java.util.OptionalLong;

/**
 * Factory interface for creating custom linear memory allocations for WebAssembly.
 *
 * <p>When set on an {@link EngineConfig} via {@link EngineConfig#withHostMemory(MemoryCreator)},
 * Wasmtime will call this creator instead of using its default memory allocation strategy whenever
 * a new linear memory is needed.
 *
 * <p>This is an advanced feature for users who need control over memory allocation strategies
 * (e.g., using huge pages, memory-mapped files, or custom allocators).
 *
 * <p>Implementations must be thread-safe since Wasmtime may call {@link #newMemory} from multiple
 * threads concurrently.
 *
 * @since 1.1.0
 */
public interface MemoryCreator {

  /**
   * Creates a new linear memory allocation.
   *
   * <p>Wasmtime calls this method when it needs a new linear memory for a module instantiation.
   *
   * @param type the memory type descriptor
   * @param minimumBytes the minimum size in bytes that the memory must be
   * @param maximumBytes the maximum size in bytes, or empty if unlimited
   * @param reservedSizeBytes the size to reserve for future growth without relocating, or empty if
   *     unspecified. When specified, the implementation should reserve this much address space so
   *     that {@code grow_to} never needs to relocate the base pointer.
   * @param guardSizeBytes the number of bytes to reserve as guard pages after the memory region
   * @return a new linear memory allocation
   */
  LinearMemory newMemory(
      MemoryType type,
      long minimumBytes,
      OptionalLong maximumBytes,
      OptionalLong reservedSizeBytes,
      long guardSizeBytes);
}
