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

package ai.tegmentum.wasmtime4j.async;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Interface for custom async stack allocation.
 *
 * <p>StackCreator allows customization of how async stacks are allocated for WebAssembly
 * async execution. This is an advanced feature that enables control over stack memory
 * allocation, which can be useful for:
 *
 * <ul>
 *   <li>Custom memory management strategies</li>
 *   <li>Stack pooling for reduced allocation overhead</li>
 *   <li>Guard page customization</li>
 *   <li>Integration with custom memory allocators</li>
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * StackCreator creator = new PooledStackCreator(100, 1024 * 1024);
 *
 * EngineConfig config = new EngineConfig()
 *     .asyncSupport(true)
 *     .stackCreator(creator);
 *
 * Engine engine = Engine.create(config);
 * }</pre>
 *
 * <p><b>Warning:</b> Incorrect stack management can cause crashes or security issues.
 * Only implement this interface if you understand the implications of custom stack
 * allocation.
 *
 * @since 1.1.0
 */
public interface StackCreator {

  /**
   * Creates a new async stack with the specified size.
   *
   * <p>The implementation should allocate a contiguous block of memory for the stack.
   * The returned AsyncStack must remain valid until {@link AsyncStack#release()} is called.
   *
   * @param size the required stack size in bytes
   * @return an AsyncStack representing the allocated stack
   * @throws WasmException if stack allocation fails
   */
  AsyncStack createStack(long size) throws WasmException;

  /**
   * Gets the default stack size for this creator.
   *
   * <p>This is used when no specific size is requested.
   *
   * @return the default stack size in bytes
   */
  default long getDefaultStackSize() {
    return 2 * 1024 * 1024; // 2MB default
  }

  /**
   * Gets the maximum stack size this creator supports.
   *
   * @return the maximum stack size in bytes
   */
  default long getMaxStackSize() {
    return 8 * 1024 * 1024; // 8MB default maximum
  }

  /**
   * Gets the minimum stack size this creator supports.
   *
   * @return the minimum stack size in bytes
   */
  default long getMinStackSize() {
    return 64 * 1024; // 64KB minimum
  }

  /**
   * Called when the StackCreator is no longer needed.
   *
   * <p>Implementations should release any resources held by the creator.
   */
  default void close() {
    // Default implementation does nothing
  }

  /**
   * Creates a default system stack creator.
   *
   * @return a default StackCreator implementation
   */
  static StackCreator defaultCreator() {
    return new DefaultStackCreator();
  }

  /**
   * Represents an allocated async stack.
   */
  interface AsyncStack {

    /**
     * Gets the base address of the stack.
     *
     * @return the stack base address
     */
    long getBaseAddress();

    /**
     * Gets the top address of the stack (base + size).
     *
     * @return the stack top address
     */
    long getTopAddress();

    /**
     * Gets the size of the stack in bytes.
     *
     * @return the stack size
     */
    long getSize();

    /**
     * Gets the guard page size in bytes.
     *
     * <p>Guard pages are used to detect stack overflow.
     *
     * @return the guard page size, or 0 if no guard pages
     */
    default long getGuardSize() {
      return 4096; // One page default
    }

    /**
     * Checks if this stack is still valid and usable.
     *
     * @return true if the stack is valid
     */
    boolean isValid();

    /**
     * Releases this stack, returning its memory.
     *
     * <p>After calling this method, the stack should not be used.
     */
    void release();

    /**
     * Gets an optional identifier for this stack.
     *
     * <p>This can be useful for debugging and tracking.
     *
     * @return an optional stack identifier
     */
    default Optional<String> getId() {
      return Optional.empty();
    }
  }

  /**
   * Default stack creator using system memory allocation.
   */
  final class DefaultStackCreator implements StackCreator {

    @Override
    public AsyncStack createStack(final long size) throws WasmException {
      if (size < getMinStackSize() || size > getMaxStackSize()) {
        throw new WasmException("Invalid stack size: " + size
            + " (must be between " + getMinStackSize() + " and " + getMaxStackSize() + ")");
      }
      return new DefaultAsyncStack(size);
    }

    private static final class DefaultAsyncStack implements AsyncStack {
      private final long size;
      private final long baseAddress;
      private boolean valid;

      DefaultAsyncStack(final long size) {
        this.size = size;
        // In a real implementation, this would allocate actual memory
        // For now, we use a placeholder address
        this.baseAddress = System.identityHashCode(this);
        this.valid = true;
      }

      @Override
      public long getBaseAddress() {
        return baseAddress;
      }

      @Override
      public long getTopAddress() {
        return baseAddress + size;
      }

      @Override
      public long getSize() {
        return size;
      }

      @Override
      public boolean isValid() {
        return valid;
      }

      @Override
      public void release() {
        valid = false;
      }
    }
  }
}
