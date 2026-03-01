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

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Interface for providing custom JIT code memory management for Wasmtime.
 *
 * <p>When set on an {@link EngineConfig} via {@link
 * EngineConfig#withCustomCodeMemory(CustomCodeMemory)}, this interface allows the embedder to
 * control the W^X (write XOR execute) permission transitions for JIT-compiled code memory.
 *
 * <p>This is useful for environments that require custom memory protection policies, such as
 * sealing, signing, or auditing executable memory transitions.
 *
 * <p>Implementations must be thread-safe since Wasmtime may call these methods from multiple
 * threads concurrently.
 *
 * @since 1.1.0
 */
public interface CustomCodeMemory {

  /**
   * Returns the required alignment for code memory regions.
   *
   * <p>Wasmtime guarantees that all pointers passed to {@link #publishExecutable} and {@link
   * #unpublishExecutable} are aligned to this value.
   *
   * @return the required alignment in bytes (must be a power of 2)
   */
  long requiredAlignment();

  /**
   * Transitions a code memory region from writable to executable.
   *
   * <p>After this call, the memory region starting at {@code ptr} with length {@code len} must be
   * executable but may no longer be writable.
   *
   * @param ptr the base address of the code memory region
   * @param len the length of the region in bytes
   * @throws WasmException if the permission transition fails
   */
  void publishExecutable(long ptr, long len) throws WasmException;

  /**
   * Transitions a code memory region from executable to writable.
   *
   * <p>After this call, the memory region starting at {@code ptr} with length {@code len} must be
   * writable but may no longer be executable.
   *
   * @param ptr the base address of the code memory region
   * @param len the length of the region in bytes
   * @throws WasmException if the permission transition fails
   */
  void unpublishExecutable(long ptr, long len) throws WasmException;
}
