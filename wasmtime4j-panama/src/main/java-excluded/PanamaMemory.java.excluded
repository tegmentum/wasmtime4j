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

import ai.tegmentum.wasmtime4j.Memory64Instruction;
import ai.tegmentum.wasmtime4j.Memory64InstructionHandler;
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

  // Memory64 instruction support
  private final Memory64InstructionHandler instructionHandler = new Memory64InstructionHandler();

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
    ensureNotClosed();

    try {
      // Get maximum memory size through optimized FFI call
      MethodHandle memoryMaxSize =
          nativeFunctions.getFunction(
              "wasmtime_memory_max_size",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG, ValueLayout.ADDRESS // memory
                  ));

      long maxSizePages = (long) memoryMaxSize.invoke(memoryResource.getNativePointer());
      if (maxSizePages == -1 || maxSizePages > Integer.MAX_VALUE) {
        return -1; // Unlimited or exceeds 32-bit range
      }
      return (int) maxSizePages;

    } catch (Throwable e) {
      // Return -1 for unlimited if query fails
      return -1;
    }
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

  // Bulk Memory Operations Implementation

  @Override
  public void copy(final int destOffset, final int srcOffset, final int length) {
    ensureNotClosed();

    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("destOffset cannot be negative: " + destOffset);
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("srcOffset cannot be negative: " + srcOffset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("length cannot be negative: " + length);
    }

    try {
      // Get direct memory access for zero-copy bulk operations
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      int currentSizePages = getSize();
      long memorySize = currentSizePages * 65536L;
      if (destOffset + length > memorySize || srcOffset + length > memorySize) {
        throw new IndexOutOfBoundsException(
            "Copy operation exceeds memory bounds: dest="
                + destOffset
                + ", src="
                + srcOffset
                + ", length="
                + length
                + ", memory size="
                + memorySize);
      }

      // Perform efficient memory copy using MemorySegment
      MemorySegment sourceSegment = memoryData.asSlice(srcOffset, length);
      MemorySegment destSegment = memoryData.asSlice(destOffset, length);
      destSegment.copyFrom(sourceSegment);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory copy failed", e);
    }
  }

  @Override
  public void fill(final int offset, final byte value, final int length) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("offset cannot be negative: " + offset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("length cannot be negative: " + length);
    }

    try {
      // Get direct memory access
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      int currentSizePages = getSize();
      long memorySize = currentSizePages * 65536L;
      if (offset + length > memorySize) {
        throw new IndexOutOfBoundsException(
            "Fill operation exceeds memory bounds: offset="
                + offset
                + ", length="
                + length
                + ", memory size="
                + memorySize);
      }

      // Perform efficient memory fill using MemorySegment
      MemorySegment fillSegment = memoryData.asSlice(offset, length);
      fillSegment.fill(value);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory fill failed", e);
    }
  }

  @Override
  public void init(
      final int destOffset, final int dataSegmentIndex, final int srcOffset, final int length) {
    ensureNotClosed();

    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("destOffset cannot be negative: " + destOffset);
    }
    if (dataSegmentIndex < 0) {
      throw new IndexOutOfBoundsException(
          "dataSegmentIndex cannot be negative: " + dataSegmentIndex);
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("srcOffset cannot be negative: " + srcOffset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("length cannot be negative: " + length);
    }

    try {
      // Call memory init through FFI
      MethodHandle memoryInit =
          nativeFunctions.getFunction(
              "wasmtime_memory_init",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // dest_offset
                  ValueLayout.JAVA_INT, // data_segment_index
                  ValueLayout.JAVA_INT, // src_offset
                  ValueLayout.JAVA_INT // length
                  ));

      memoryInit.invoke(
          memoryResource.getNativePointer(), destOffset, dataSegmentIndex, srcOffset, length);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory init failed", e);
    }
  }

  @Override
  public void dropDataSegment(final int dataSegmentIndex) {
    ensureNotClosed();

    if (dataSegmentIndex < 0) {
      throw new IndexOutOfBoundsException(
          "dataSegmentIndex cannot be negative: " + dataSegmentIndex);
    }

    try {
      // Call data drop through FFI
      MethodHandle dataDrop =
          nativeFunctions.getFunction(
              "wasmtime_data_drop",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // memory (or store context)
                  ValueLayout.JAVA_INT // data_segment_index
                  ));

      dataDrop.invoke(memoryResource.getNativePointer(), dataSegmentIndex);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Data segment drop failed", e);
    }
  }

  // Shared Memory Operations Implementation

  @Override
  public boolean isShared() {
    ensureNotClosed();

    try {
      // Call wasmtime_memory_is_shared through FFI
      MethodHandle isSharedHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_is_shared",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS // memory
                  ));

      return (boolean) isSharedHandle.invoke(memoryResource.getNativePointer());

    } catch (Throwable e) {
      // Default to non-shared if query fails
      return false;
    }
  }

  @Override
  public int atomicCompareAndSwapInt(final int offset, final int expected, final int newValue) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle casHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_cas_i32",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT, // expected
                  ValueLayout.JAVA_INT // new_value
                  ));

      return (int) casHandle.invoke(memoryResource.getNativePointer(), offset, expected, newValue);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic compare-and-swap failed", e);
    }
  }

  @Override
  public long atomicCompareAndSwapLong(final int offset, final long expected, final long newValue) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateMemoryBounds(offset, 8);

    try {
      MethodHandle casHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_cas_i64",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_LONG, // expected
                  ValueLayout.JAVA_LONG // new_value
                  ));

      return (long) casHandle.invoke(memoryResource.getNativePointer(), offset, expected, newValue);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic compare-and-swap failed", e);
    }
  }

  @Override
  public int atomicLoadInt(final int offset) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle loadHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_load_i32",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT // offset
                  ));

      return (int) loadHandle.invoke(memoryResource.getNativePointer(), offset);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic load failed", e);
    }
  }

  @Override
  public long atomicLoadLong(final int offset) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateMemoryBounds(offset, 8);

    try {
      MethodHandle loadHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_load_i64",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT // offset
                  ));

      return (long) loadHandle.invoke(memoryResource.getNativePointer(), offset);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic load failed", e);
    }
  }

  @Override
  public void atomicStoreInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle storeHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_store_i32",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT // value
                  ));

      storeHandle.invoke(memoryResource.getNativePointer(), offset, value);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic store failed", e);
    }
  }

  @Override
  public void atomicStoreLong(final int offset, final long value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateMemoryBounds(offset, 8);

    try {
      MethodHandle storeHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_store_i64",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_LONG // value
                  ));

      storeHandle.invoke(memoryResource.getNativePointer(), offset, value);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic store failed", e);
    }
  }

  @Override
  public int atomicAddInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle addHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_add_i32",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT // value
                  ));

      return (int) addHandle.invoke(memoryResource.getNativePointer(), offset, value);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic add failed", e);
    }
  }

  @Override
  public long atomicAddLong(final int offset, final long value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateMemoryBounds(offset, 8);

    try {
      MethodHandle addHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_add_i64",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_LONG // value
                  ));

      return (long) addHandle.invoke(memoryResource.getNativePointer(), offset, value);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic add failed", e);
    }
  }

  @Override
  public int atomicAndInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle andHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_and_i32",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT // value
                  ));

      return (int) andHandle.invoke(memoryResource.getNativePointer(), offset, value);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic AND failed", e);
    }
  }

  @Override
  public int atomicOrInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle orHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_or_i32",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT // value
                  ));

      return (int) orHandle.invoke(memoryResource.getNativePointer(), offset, value);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic OR failed", e);
    }
  }

  @Override
  public int atomicXorInt(final int offset, final int value) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle xorHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_xor_i32",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT // value
                  ));

      return (int) xorHandle.invoke(memoryResource.getNativePointer(), offset, value);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic XOR failed", e);
    }
  }

  @Override
  public void atomicFence() {
    ensureNotClosed();
    checkSharedMemory();

    try {
      MethodHandle fenceHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_fence",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS // memory
                  ));

      fenceHandle.invoke(memoryResource.getNativePointer());

    } catch (Throwable e) {
      throw new RuntimeException("Atomic fence failed", e);
    }
  }

  @Override
  public int atomicNotify(final int offset, final int count) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative: " + count);
    }

    try {
      MethodHandle notifyHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_notify",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT // count
                  ));

      return (int) notifyHandle.invoke(memoryResource.getNativePointer(), offset, count);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic notify failed", e);
    }
  }

  @Override
  public int atomicWait32(final int offset, final int expected, final long timeoutNanos) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 4);
    validateMemoryBounds(offset, 4);

    try {
      MethodHandle waitHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_wait32",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_INT, // expected
                  ValueLayout.JAVA_LONG // timeout_nanos
                  ));

      return (int)
          waitHandle.invoke(memoryResource.getNativePointer(), offset, expected, timeoutNanos);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic wait failed", e);
    }
  }

  @Override
  public int atomicWait64(final int offset, final long expected, final long timeoutNanos) {
    ensureNotClosed();
    checkSharedMemory();
    checkAligned(offset, 8);
    validateMemoryBounds(offset, 8);

    try {
      MethodHandle waitHandle =
          nativeFunctions.getFunction(
              "wasmtime_memory_atomic_wait64",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_INT,
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_INT, // offset
                  ValueLayout.JAVA_LONG, // expected
                  ValueLayout.JAVA_LONG // timeout_nanos
                  ));

      return (int)
          waitHandle.invoke(memoryResource.getNativePointer(), offset, expected, timeoutNanos);

    } catch (Throwable e) {
      throw new RuntimeException("Atomic wait failed", e);
    }
  }

  // Helper methods for shared memory operations

  /**
   * Checks if this memory is shared, throwing an exception if not.
   *
   * @throws IllegalStateException if this memory is not shared
   */
  private void checkSharedMemory() {
    if (!isShared()) {
      throw new IllegalStateException("Operation requires shared memory");
    }
  }

  /**
   * Checks if an offset is properly aligned for the given alignment requirement.
   *
   * @param offset the offset to check
   * @param alignment the required alignment (must be a power of 2)
   * @throws IllegalArgumentException if offset is not properly aligned
   */
  private void checkAligned(final int offset, final int alignment) {
    if ((offset & (alignment - 1)) != 0) {
      throw new IllegalArgumentException(
          String.format("Offset %d is not aligned to %d-byte boundary", offset, alignment));
    }
  }

  /**
   * Validates memory bounds for atomic operations.
   *
   * @param offset the memory offset
   * @param size the operation size in bytes
   * @throws IndexOutOfBoundsException if the operation would exceed memory bounds
   */
  private void validateMemoryBounds(final int offset, final int size) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }

    try {
      int currentSizePages = getSize();
      long memorySize = currentSizePages * 65536L;
      if (offset + size > memorySize) {
        throw new IndexOutOfBoundsException(
            "Operation at offset "
                + offset
                + " with size "
                + size
                + " exceeds memory size "
                + memorySize);
      }
    } catch (Exception e) {
      throw new IndexOutOfBoundsException("Failed to validate memory bounds: " + e.getMessage());
    }
  }

  // 64-bit Memory Operations Implementation

  @Override
  public long getSize64() {
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
      return sizeInBytes / 65536L; // Convert bytes to pages

    } catch (Throwable e) {
      // Return current size from 32-bit method as fallback
      return getSize();
    }
  }

  @Override
  public long getMaxSize64() {
    ensureNotClosed();

    try {
      // Get maximum memory size through optimized FFI call
      MethodHandle memoryMaxSize =
          nativeFunctions.getFunction(
              "wasmtime_memory_max_size",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_LONG, ValueLayout.ADDRESS // memory
                  ));

      return (long) memoryMaxSize.invoke(memoryResource.getNativePointer());

    } catch (Throwable e) {
      // Return -1 for unlimited if query fails
      return -1;
    }
  }

  @Override
  public long grow64(final long pages) {
    ensureNotClosed();

    if (pages < 0) {
      return -1;
    }

    try {
      // Get current size before growth
      long previousSize = getSize64();

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
        return previousSize;
      } else {
        return -1;
      }

    } catch (Throwable e) {
      return -1;
    }
  }

  @Override
  public byte readByte64(final long offset) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety - get size in bytes
      long currentSizePages = getSize64();
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
  public void writeByte64(final long offset, final byte value) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative: " + offset);
    }

    try {
      // Get direct memory access through FFI
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety - get size in bytes
      long currentSizePages = getSize64();
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
  public void readBytes64(
      final long offset, final byte[] dest, final int destOffset, final int length) {
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
      long currentSizePages = getSize64();
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
  public void writeBytes64(
      final long offset, final byte[] src, final int srcOffset, final int length) {
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
      long currentSizePages = getSize64();
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
  public void copy64(final long destOffset, final long srcOffset, final long length) {
    ensureNotClosed();

    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("destOffset cannot be negative: " + destOffset);
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("srcOffset cannot be negative: " + srcOffset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("length cannot be negative: " + length);
    }

    try {
      // Get direct memory access for zero-copy bulk operations
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      long currentSizePages = getSize64();
      long memorySize = currentSizePages * 65536L;
      if (destOffset + length > memorySize || srcOffset + length > memorySize) {
        throw new IndexOutOfBoundsException(
            "Copy operation exceeds memory bounds: dest="
                + destOffset
                + ", src="
                + srcOffset
                + ", length="
                + length
                + ", memory size="
                + memorySize);
      }

      // Perform efficient memory copy using MemorySegment
      MemorySegment sourceSegment = memoryData.asSlice(srcOffset, length);
      MemorySegment destSegment = memoryData.asSlice(destOffset, length);
      destSegment.copyFrom(sourceSegment);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory copy failed", e);
    }
  }

  @Override
  public void fill64(final long offset, final byte value, final long length) {
    ensureNotClosed();

    if (offset < 0) {
      throw new IndexOutOfBoundsException("offset cannot be negative: " + offset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("length cannot be negative: " + length);
    }

    try {
      // Get direct memory access
      MemorySegment memoryData = getDirectMemoryAccess();

      // Bounds checking for memory safety
      long currentSizePages = getSize64();
      long memorySize = currentSizePages * 65536L;
      if (offset + length > memorySize) {
        throw new IndexOutOfBoundsException(
            "Fill operation exceeds memory bounds: offset="
                + offset
                + ", length="
                + length
                + ", memory size="
                + memorySize);
      }

      // Perform efficient memory fill using MemorySegment
      MemorySegment fillSegment = memoryData.asSlice(offset, length);
      fillSegment.fill(value);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory fill failed", e);
    }
  }

  @Override
  public void init64(
      final long destOffset, final int dataSegmentIndex, final long srcOffset, final long length) {
    ensureNotClosed();

    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("destOffset cannot be negative: " + destOffset);
    }
    if (dataSegmentIndex < 0) {
      throw new IndexOutOfBoundsException(
          "dataSegmentIndex cannot be negative: " + dataSegmentIndex);
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("srcOffset cannot be negative: " + srcOffset);
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("length cannot be negative: " + length);
    }

    try {
      // Call memory init through FFI with 64-bit parameters
      MethodHandle memoryInit =
          nativeFunctions.getFunction(
              "wasmtime_memory_init64",
              FunctionDescriptor.ofVoid(
                  ValueLayout.ADDRESS, // memory
                  ValueLayout.JAVA_LONG, // dest_offset
                  ValueLayout.JAVA_INT, // data_segment_index
                  ValueLayout.JAVA_LONG, // src_offset
                  ValueLayout.JAVA_LONG // length
                  ));

      memoryInit.invoke(
          memoryResource.getNativePointer(), destOffset, dataSegmentIndex, srcOffset, length);

    } catch (IndexOutOfBoundsException e) {
      throw e;
    } catch (Throwable e) {
      throw new RuntimeException("Memory init failed", e);
    }
  }

  @Override
  public boolean supports64BitAddressing() {
    ensureNotClosed();

    try {
      // Check if this memory supports 64-bit addressing through FFI
      MethodHandle supports64Bit =
          nativeFunctions.getFunction(
              "wasmtime_memory_supports_64bit",
              FunctionDescriptor.of(
                  ValueLayout.JAVA_BOOLEAN, ValueLayout.ADDRESS // memory
                  ));

      return (boolean) supports64Bit.invoke(memoryResource.getNativePointer());

    } catch (Throwable e) {
      // Default to 32-bit if query fails
      return false;
    }
  }

  @Override
  public long getSizeInBytes64() {
    return getSize64() * 65536L;
  }

  @Override
  public long getMaxSizeInBytes64() {
    final long maxPages = getMaxSize64();
    return maxPages == -1 ? -1 : maxPages * 65536L;
  }

  // Memory64 Instruction Support

  /**
   * Executes a Memory64 instruction by opcode.
   *
   * @param opcode the instruction opcode
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeMemory64Instruction(final int opcode, final long offset, final long value) {
    return instructionHandler.executeByOpcode(opcode, this, offset, value);
  }

  /**
   * Executes a Memory64 instruction by mnemonic.
   *
   * @param mnemonic the instruction mnemonic
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeMemory64Instruction(
      final String mnemonic, final long offset, final long value) {
    return instructionHandler.executeByMnemonic(mnemonic, this, offset, value);
  }

  /**
   * Executes a Memory64 instruction directly.
   *
   * @param instruction the instruction to execute
   * @param offset the memory offset (64-bit)
   * @param value the value for store operations (ignored for loads)
   * @return the result value for load operations, or 0 for store/control operations
   * @throws UnsupportedOperationException if the instruction or memory doesn't support 64-bit
   * @throws IndexOutOfBoundsException if the operation is out of bounds
   * @throws IllegalArgumentException if parameters are invalid
   */
  public long executeMemory64Instruction(
      final Memory64Instruction instruction, final long offset, final long value) {
    return instructionHandler.executeInstruction(instruction, this, offset, value);
  }

  /**
   * Checks if a Memory64 instruction is supported by this memory implementation.
   *
   * @param opcode the instruction opcode
   * @return true if the instruction is supported
   */
  public boolean isMemory64InstructionSupported(final int opcode) {
    return supports64BitAddressing() && instructionHandler.isInstructionSupported(opcode);
  }

  /**
   * Checks if a Memory64 instruction is supported by this memory implementation.
   *
   * @param mnemonic the instruction mnemonic
   * @return true if the instruction is supported
   */
  public boolean isMemory64InstructionSupported(final String mnemonic) {
    return supports64BitAddressing() && instructionHandler.isInstructionSupported(mnemonic);
  }

  /**
   * Gets execution statistics for Memory64 instructions.
   *
   * @return the execution statistics
   */
  public Memory64InstructionHandler.ExecutionStatistics getMemory64InstructionStatistics() {
    return instructionHandler.getStatistics();
  }
}
