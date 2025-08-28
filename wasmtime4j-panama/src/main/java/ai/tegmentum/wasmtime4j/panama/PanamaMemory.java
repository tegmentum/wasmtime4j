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

import ai.tegmentum.wasmtime4j.Memory;
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
public final class PanamaMemory implements Memory, AutoCloseable {
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
  public long size() throws WasmException {
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
      return sizeInBytes;

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory size query", "memory=" + memoryResource.getNativePointer(), e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public long pages() throws WasmException {
    ensureNotClosed();

    try {
      // WebAssembly memory pages are 64KB (65536 bytes)
      long sizeInBytes = size();
      return sizeInBytes / 65536L;

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory pages query", "memory=" + memoryResource.getNativePointer(), e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public boolean grow(final long pages) throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    PanamaErrorHandler.requireNonNegative(pages, "pages");

    try {
      // Grow memory through optimized FFI call
      MethodHandle memoryGrow =
          nativeFunctions.getFunction(
              "wasmtime_memory_grow",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_LONG // delta_pages
                  ));

      boolean success = (boolean) memoryGrow.invoke(memoryResource.getNativePointer(), pages);

      if (success) {
        LOGGER.fine("Successfully grew memory by " + pages + " pages");
      }

      return success;

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory growth",
              "memory=" + memoryResource.getNativePointer() + ", pages=" + pages,
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public byte readByte(final long offset) throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    PanamaErrorHandler.requireNonNegative(offset, "offset");

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      long memorySize = size();
      if (offset >= memorySize) {
        throw new IndexOutOfBoundsException(
            "Offset " + offset + " exceeds memory size " + memorySize);
      }

      // Direct memory access with zero-copy
      return memoryData.get(ValueLayout.JAVA_BYTE, offset);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory byte read",
              "offset=" + offset + ", memory=" + memoryResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public void writeByte(final long offset, final byte value) throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    PanamaErrorHandler.requireNonNegative(offset, "offset");

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      long memorySize = size();
      if (offset >= memorySize) {
        throw new IndexOutOfBoundsException(
            "Offset " + offset + " exceeds memory size " + memorySize);
      }

      // Direct memory access with zero-copy
      memoryData.set(ValueLayout.JAVA_BYTE, offset, value);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory byte write",
              "offset="
                  + offset
                  + ", value="
                  + value
                  + ", memory="
                  + memoryResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public void read(final long offset, final byte[] buffer) throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    PanamaErrorHandler.requireNonNegative(offset, "offset");
    Objects.requireNonNull(buffer, "Buffer cannot be null");

    read(offset, buffer, 0, buffer.length);
  }

  @Override
  public void read(final long offset, final byte[] buffer, final int bufferOffset, final int length)
      throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    PanamaErrorHandler.requireNonNegative(offset, "offset");
    Objects.requireNonNull(buffer, "Buffer cannot be null");
    PanamaErrorHandler.requireNonNegative(bufferOffset, "bufferOffset");
    PanamaErrorHandler.requireNonNegative(length, "length");

    if (bufferOffset + length > buffer.length) {
      throw new IndexOutOfBoundsException(
          "Buffer overflow: bufferOffset="
              + bufferOffset
              + ", length="
              + length
              + ", buffer.length="
              + buffer.length);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      long memorySize = size();
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
      MemorySegment bufferSegment = MemorySegment.ofArray(buffer);
      MemorySegment sourceSegment = memoryData.asSlice(offset, length);
      bufferSegment.asSlice(bufferOffset, length).copyFrom(sourceSegment);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory bulk read",
              "offset="
                  + offset
                  + ", bufferOffset="
                  + bufferOffset
                  + ", length="
                  + length
                  + ", memory="
                  + memoryResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public void write(final long offset, final byte[] buffer) throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    PanamaErrorHandler.requireNonNegative(offset, "offset");
    Objects.requireNonNull(buffer, "Buffer cannot be null");

    write(offset, buffer, 0, buffer.length);
  }

  @Override
  public void write(
      final long offset, final byte[] buffer, final int bufferOffset, final int length)
      throws WasmException {
    ensureNotClosed();

    // Parameter validation with defensive programming
    PanamaErrorHandler.requireNonNegative(offset, "offset");
    Objects.requireNonNull(buffer, "Buffer cannot be null");
    PanamaErrorHandler.requireNonNegative(bufferOffset, "bufferOffset");
    PanamaErrorHandler.requireNonNegative(length, "length");

    if (bufferOffset + length > buffer.length) {
      throw new IndexOutOfBoundsException(
          "Buffer overflow: bufferOffset="
              + bufferOffset
              + ", length="
              + length
              + ", buffer.length="
              + buffer.length);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      long memorySize = size();
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
      MemorySegment bufferSegment = MemorySegment.ofArray(buffer);
      MemorySegment sourceSegment = bufferSegment.asSlice(bufferOffset, length);
      MemorySegment targetSegment = memoryData.asSlice(offset, length);
      targetSegment.copyFrom(sourceSegment);

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "Memory bulk write",
              "offset="
                  + offset
                  + ", bufferOffset="
                  + bufferOffset
                  + ", length="
                  + length
                  + ", memory="
                  + memoryResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public ByteBuffer asByteBuffer() throws WasmException {
    ensureNotClosed();

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Create a ByteBuffer view of the memory segment for compatibility
      // Note: This creates a direct ByteBuffer that shares the same memory
      return memoryData.asByteBuffer();

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "ByteBuffer creation", "memory=" + memoryResource.getNativePointer(), e.getMessage());
      throw new WasmException(detailedMessage, e);
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

    } catch (Exception e) {
      String detailedMessage =
          PanamaErrorHandler.createDetailedErrorMessage(
              "MemorySegment access",
              "memory=" + memoryResource.getNativePointer(),
              e.getMessage());
      throw new WasmException(detailedMessage, e);
    }
  }

  @Override
  public void close() throws WasmException {
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
        throw new WasmException("Failed to close memory", e);
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
  private MemorySegment getDirectMemoryAccess() throws Exception {
    // Get memory data pointer and size through FFI
    MemorySegment memoryPtr = getMemoryDataPointer();
    long memorySize = size();

    // Create memory segment view with proper size
    return memoryPtr.reinterpret(memorySize);
  }

  /** Gets the memory data pointer through FFI calls. */
  private MemorySegment getMemoryDataPointer() throws Exception {
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
