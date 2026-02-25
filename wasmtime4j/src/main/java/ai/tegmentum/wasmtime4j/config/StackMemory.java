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

/**
 * Represents a custom fiber stack allocation for async WebAssembly execution.
 *
 * <p>Implementations of this interface provide raw native memory for fiber stacks used by
 * Wasmtime's async support. This is an advanced feature for users who need control over stack
 * memory allocation.
 *
 * <p><strong>Safety Requirements:</strong>
 *
 * <ul>
 *   <li>All memory must be native/direct memory, not JVM heap memory.
 *   <li>The top of the stack must be page-aligned.
 *   <li>At least one guard page must exist at the bottom of the stack.
 *   <li>The usable range must not overlap with the guard range.
 *   <li>Implementations must be thread-safe.
 * </ul>
 *
 * @since 1.1.0
 */
public interface StackMemory extends AutoCloseable {

  /**
   * Returns a pointer to the top of the stack.
   *
   * <p>Stacks grow downward, so the top is the highest address. This must be page-aligned.
   *
   * @return the raw pointer to the top of the stack
   */
  long top();

  /**
   * Returns the start address of the usable stack range (exclusive of guard pages).
   *
   * @return the start address of the usable range
   */
  long rangeStart();

  /**
   * Returns the end address of the usable stack range (exclusive of guard pages).
   *
   * @return the end address of the usable range
   */
  long rangeEnd();

  /**
   * Returns the start address of the guard page range.
   *
   * @return the start of the guard range as a raw pointer
   */
  long guardRangeStart();

  /**
   * Returns the end address of the guard page range.
   *
   * @return the end of the guard range as a raw pointer
   */
  long guardRangeEnd();

  /**
   * Releases the native memory owned by this stack.
   *
   * <p>After calling close, the stack memory is invalid and must not be accessed.
   */
  @Override
  void close();
}
