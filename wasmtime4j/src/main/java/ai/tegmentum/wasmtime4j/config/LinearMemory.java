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
 * Represents a custom linear memory allocation for WebAssembly.
 *
 * <p>Implementations of this interface provide raw native memory that Wasmtime will use directly
 * for WebAssembly linear memory. This is an advanced feature for users who need control over memory
 * allocation strategies (e.g., using huge pages, memory-mapped files, or custom allocators).
 *
 * <p><strong>Safety Requirements:</strong>
 *
 * <ul>
 *   <li>All memory returned by {@link #basePointer()} MUST be native/direct memory, not JVM heap
 *       memory. Use {@code sun.misc.Unsafe.allocateMemory()}, {@code ByteBuffer.allocateDirect()},
 *       or {@code MemorySegment.allocateNative()} to allocate.
 *   <li>Memory MUST be page-aligned (typically 4096 or 65536 byte alignment).
 *   <li>Memory MUST be zero-filled when first allocated.
 *   <li>Guard pages MUST be set up correctly to prevent out-of-bounds access.
 *   <li>The base pointer MUST NOT change after allocation unless {@code grow_to} is called and no
 *       reserved size was specified.
 *   <li>Implementations MUST be thread-safe.
 * </ul>
 *
 * @since 1.1.0
 */
public interface LinearMemory extends AutoCloseable {

  /**
   * Returns the current size of the linear memory in bytes.
   *
   * @return the current byte size
   */
  long byteSize();

  /**
   * Returns the maximum capacity of the linear memory in bytes.
   *
   * <p>This is the total amount of memory that can be used without relocating the base pointer. It
   * may be larger than {@link #byteSize()} if memory was reserved ahead of time.
   *
   * @return the byte capacity
   */
  long byteCapacity();

  /**
   * Grows the linear memory to the specified new size.
   *
   * <p>The new size must be larger than the current size. The newly allocated region must be
   * zero-filled.
   *
   * @param newSize the new size in bytes
   * @throws WasmException if the memory cannot be grown to the requested size
   */
  void growTo(long newSize) throws WasmException;

  /**
   * Returns a raw pointer to the base of the linear memory.
   *
   * <p><strong>WARNING:</strong> This MUST return a pointer to native memory (allocated via {@code
   * Unsafe.allocateMemory()}, {@code ByteBuffer.allocateDirect()}, or equivalent). Returning a
   * pointer to JVM heap memory WILL cause JVM crashes due to garbage collector relocation.
   *
   * @return the base pointer as a native address
   */
  long basePointer();

  /**
   * Releases the native memory owned by this linear memory.
   *
   * <p>After calling close, the memory region is invalid and must not be accessed.
   */
  @Override
  void close();
}
