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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of the WebAssembly memory interface.
 *
 * <p>WebAssembly linear memory provides a resizable array of bytes that can be accessed by both
 * WebAssembly code and host code. This implementation uses Panama FFI with Stream 1 & 2
 * infrastructure for zero-copy access to the underlying native memory through direct MemorySegment
 * operations.
 *
 * <p>This implementation provides efficient memory access through direct memory segment operations,
 * avoiding the overhead of copying data between native and Java heap memory. All operations include
 * comprehensive bounds checking and memory safety validation.
 *
 * @since 1.0.0
 */
public final class PanamaMemory implements WasmMemory {
  private static final Logger LOGGER = Logger.getLogger(PanamaMemory.class.getName());

  // Core infrastructure from Streams 1 & 2
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;
  private final ArenaResourceManager.ManagedNativeResource memoryResource;
  private final PanamaInstance parentInstance;

  // Memory state
  private volatile boolean closed = false;

  /**
   * Creates a new Panama memory instance using Stream 1 & 2 infrastructure.
   *
   * @param memoryPtr the native memory pointer from export
   * @param resourceManager the arena resource manager for lifecycle management
   * @param parentInstance the parent instance that owns this memory
   * @throws WasmException if the memory cannot be created
   */
  public PanamaMemory(
      final MemorySegment memoryPtr,
      final ArenaResourceManager resourceManager,
      final PanamaInstance parentInstance)
      throws WasmException {
    // Defensive parameter validation
    PanamaErrorHandler.requireValidPointer(memoryPtr, "memoryPtr");
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.parentInstance = Objects.requireNonNull(parentInstance, "Parent instance cannot be null");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    if (!nativeFunctions.isInitialized()) {
      throw new WasmException("Native function bindings not initialized");
    }

    try {
      // Create managed resource with cleanup for memory
      // Note: Memory is typically owned by the instance, so cleanup is light
      this.memoryResource =
          resourceManager.manageNativeResource(
              memoryPtr, () -> destroyNativeMemoryInternal(memoryPtr), "Wasmtime Memory");

      LOGGER.fine("Created Panama memory with managed resource");

    } catch (Exception e) {
      throw new WasmException("Failed to create memory wrapper", e);
    }
  }

  @Override
  public int getSize() {
    ensureNotClosed();

    try {
      // Get memory size through optimized FFI call
      MethodHandle memorySize =
          nativeFunctions.getFunction(
              "wasmtime_memory_size",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG, ValueLayout.ADDRESS // memory
                  ));

      long sizeInBytes = (long) memorySize.invoke(memoryResource.getNativePointer());
      return (int) (sizeInBytes / 65536L); // Convert bytes to pages

    } catch (Throwable e) {
      // WasmMemory interface doesn't throw exceptions, return -1 for error
      return -1;
    }
  }

  @Override
  public int getMaxSize() {
    // TODO: Implement max size query - return -1 for unlimited for now
    return -1;
  }

  @Override
  public int grow(final int pages) {
    ensureNotClosed();

    if (pages < 0) {
      return -1;
    }

    try {
      // Get current size before growth
      int previousSize = getSize();

      // Grow memory through optimized FFI call
      MethodHandle memoryGrow =
          nativeFunctions.getFunction(
              "wasmtime_memory_grow",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT // delta_pages
                  ));

      boolean success = (boolean) memoryGrow.invoke(memoryResource.getNativePointer(), pages);

      if (success) {
        LOGGER.fine("Successfully grew memory by " + pages + " pages");
        return previousSize;
      } else {
        return -1;
      }

    } catch (Throwable e) {
      return -1;
    }
  }

  @Override
  public byte readByte(final int offset) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety - get size in bytes
      int currentSizePages = getSize();
      long memorySize = currentSizePages * 65536L;
      if (offset >= memorySize) {
        throw new IndexOutOfBoundsException(
            "Offset " + offset + " exceeds memory size " + memorySize);
      }

      // Direct memory access with zero-copy
      return memoryData.get(ValueLayout.JAVA_BYTE, offset);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory read failed", e);
    }
  }

  @Override
  public void writeByte(final int offset, final byte value) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety - get size in bytes
      int currentSizePages = getSize();
      long memorySize = currentSizePages * 65536L;
      if (offset >= memorySize) {
        throw new IndexOutOfBoundsException(
            "Offset " + offset + " exceeds memory size " + memorySize);
      }

      // Direct memory access with zero-copy
      memoryData.set(ValueLayout.JAVA_BYTE, offset, value);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory write failed", e);
    }
  }

  @Override
  public void readBytes(
      final int offset, final byte[] dest, final int destOffset, final int length) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }
    Objects.requireNonNull(dest, "Destination array cannot be null");
    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("Destination offset cannot be negative: " + destOffset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
    }

    if (destOffset + length > dest.length) {
      throw new IndexOutOfBoundsException(
          "Buffer overflow: destOffset="
              + destOffset
              + ", length="
              + length
              + ", dest.length="
              + dest.length);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety - get size in bytes
      int currentSizePages = getSize();
      long memorySize = currentSizePages * 65536L;
      if (offset + length > memorySize) {
        throw new IndexOutOfBoundsException(
            "Read overflow: offset="
                + offset
                + ", length="
                + length
                + ", memory size="
                + memorySize);
      }

      // Zero-copy bulk memory read using MemorySegment
      MemorySegment bufferSegment = MemorySegment.ofArray(dest);
      MemorySegment sourceSegment = memoryData.asSlice(offset, length);
      bufferSegment.asSlice(destOffset, length).copyFrom(sourceSegment);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory read failed", e);
    }
  }

  @Override
  public void writeBytes(
      final int offset, final byte[] src, final int srcOffset, final int length) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }
    Objects.requireNonNull(src, "Source array cannot be null");
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("Source offset cannot be negative: " + srcOffset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative: " + length);
    }

    if (srcOffset + length > src.length) {
      throw new IndexOutOfBoundsException(
          "Buffer overflow: srcOffset="
              + srcOffset
              + ", length="
              + length
              + ", src.length="
              + src.length);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety - get size in bytes
      int currentSizePages = getSize();
      long memorySize = currentSizePages * 65536L;
      if (offset + length > memorySize) {
        throw new IndexOutOfBoundsException(
            "Write overflow: offset="
                + offset
                + ", length="
                + length
                + ", memory size="
                + memorySize);
      }

      // Zero-copy bulk memory write using MemorySegment
      MemorySegment bufferSegment = MemorySegment.ofArray(src);
      MemorySegment sourceSegment = bufferSegment.asSlice(srcOffset, length);
      MemorySegment targetSegment = memoryData.asSlice(offset, length);
      targetSegment.copyFrom(sourceSegment);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory write failed", e);
    }
  }

  @Override
  public ByteBuffer getBuffer() {
    ensureNotClosed();

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Create a ByteBuffer view of the memory segment for compatibility
      // Note: This creates a direct ByteBuffer that shares the same memory
      return memoryData.asByteBuffer();

    } catch (Throwable e) {
      throw new RuntimeException("ByteBuffer creation failed", e);
    }
  }

  /**
   * Gets a direct MemorySegment view of the WebAssembly memory.
   *
   * <p>This provides zero-copy access to the underlying native memory, allowing for efficient bulk
   * operations without data copying. This is the preferred method for high-performance memory
   * operations.
   *
   * @return a MemorySegment representing the WebAssembly memory
   * @throws WasmException if the memory segment cannot be created
   */
  public MemorySegment asMemorySegment() throws WasmException {
    ensureNotClosed();

    try {
      // Return direct memory access - this is the zero-copy advantage of Panama
      return getDirectMemoryAccess();

    } catch (Throwable e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "MemorySegment access",
              "memory=" + memoryResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  /**
   * Closes this memory instance and releases associated resources.
   *
   * <p>After calling this method, the memory instance becomes invalid and should not be used. This
   * method is idempotent and can be called multiple times safely.
   */
  public void close() {
    if (closed) {
      return;
    }

    synchronized (this) {
      if (closed) {
        return;
      }

      try {
        // Close the managed native resource (automatic cleanup)
        memoryResource.close();

        LOGGER.fine("Closed Panama memory");

      } catch (Exception e) {
        LOGGER.warning("Failed to close memory: " + e.getMessage());
      } finally {
        closed = true;
      }
    }
  }

  /**
   * Gets the native memory handle for this memory.
   *
   * @return the native memory handle
   * @throws IllegalStateException if the memory is closed
   */
  public MemorySegment getMemoryHandle() {
    ensureNotClosed();
    return memoryResource.getNativePointer();
  }

  /**
   * Gets the parent instance that owns this memory.
   *
   * @return the parent instance
   */
  public PanamaInstance getParentInstance() {
    ensureNotClosed();
    return parentInstance;
  }

  // Private helper methods for memory operations

  /**
   * Gets direct access to the memory data through FFI calls. This is the core zero-copy operation
   * that provides MemorySegment access.
   */
  private MemorySegment getDirectMemoryAccess() throws Throwable {
    // Get memory data pointer and size through FFI
    MemorySegment memoryPtr = getMemoryDataPointer();
    int memoryPages = getSize();
    long memorySize = memoryPages * 65536L;

    // Create memory segment view with proper size
    return memoryPtr.reinterpret(memorySize);
  }

  /** Gets the memory data pointer through FFI calls. */
  private MemorySegment getMemoryDataPointer() throws Throwable {
    // Call wasmtime_memory_data through cached method handle
    MethodHandle memoryData =
        nativeFunctions.getFunction(
            "wasmtime_memory_data",
            FunctionDescriptor.of(
                ValueLayout.ADDRESS, ValueLayout.ADDRESS // memory
                ));

    MemorySegment dataPtr = (MemorySegment) memoryData.invoke(memoryResource.getNativePointer());
    PanamaErrorHandler.requireValidPointer(dataPtr, "memory data pointer");

    return dataPtr;
  }

  /**
   * Internal cleanup method called by managed resource.
   *
   * @param memoryPtr the native memory pointer to destroy
   */
  private void destroyNativeMemoryInternal(final MemorySegment memoryPtr) {
    try {
      // Memory is typically owned by the instance, so no specific cleanup needed
      LOGGER.fine("Destroying native memory: " + memoryPtr);

    } catch (Exception e) {
      LOGGER.warning("Error during memory cleanup: " + e.getMessage());
      // Don't throw exceptions from cleanup methods
    }
  }

  /**
   * Ensures that this memory instance is not closed.
   *
   * @throws IllegalStateException if the memory is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Memory has been closed");
    }
  }
}
