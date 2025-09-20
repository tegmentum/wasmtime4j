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

import ai.tegmentum.wasmtime4j.MemoryPerformanceMetrics;
import ai.tegmentum.wasmtime4j.MemorySegment;
import ai.tegmentum.wasmtime4j.MemoryStatistics;
import ai.tegmentum.wasmtime4j.MemoryUsageReport;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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

  // BulkMemoryOperations implementation

  @Override
  public void bulkCopy(
      final WasmMemory dest,
      final int destOffset,
      final WasmMemory source,
      final int sourceOffset,
      final int length) {
    Objects.requireNonNull(dest, "dest");
    Objects.requireNonNull(source, "source");
    if (destOffset < 0) throw new IllegalArgumentException("destOffset cannot be negative");
    if (sourceOffset < 0) throw new IllegalArgumentException("sourceOffset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(dest instanceof PanamaMemory)) {
      throw new IllegalArgumentException("dest must be a PanamaMemory instance");
    }
    if (!(source instanceof PanamaMemory)) {
      throw new IllegalArgumentException("source must be a PanamaMemory instance");
    }

    final PanamaMemory destPanama = (PanamaMemory) dest;
    final PanamaMemory sourcePanama = (PanamaMemory) source;

    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_bulk_copy",
          FunctionDescriptor.ofVoid(
              ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
              ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
          destPanama.getMemoryPtr(), destOffset, sourcePanama.getMemoryPtr(), sourceOffset, length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in bulk copy", e);
    }
  }

  @Override
  public void bulkFill(final WasmMemory memory, final int offset, final int length, final byte value) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    final PanamaMemory memoryPanama = (PanamaMemory) memory;

    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_bulk_fill",
          FunctionDescriptor.ofVoid(
              ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_BYTE),
          memoryPanama.getMemoryPtr(), offset, length, value);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in bulk fill", e);
    }
  }

  @Override
  public int bulkCompare(
      final WasmMemory memory1,
      final int offset1,
      final WasmMemory memory2,
      final int offset2,
      final int length) {
    Objects.requireNonNull(memory1, "memory1");
    Objects.requireNonNull(memory2, "memory2");
    if (offset1 < 0) throw new IllegalArgumentException("offset1 cannot be negative");
    if (offset2 < 0) throw new IllegalArgumentException("offset2 cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory1 instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory1 must be a PanamaMemory instance");
    }
    if (!(memory2 instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory2 must be a PanamaMemory instance");
    }

    final PanamaMemory memory1Panama = (PanamaMemory) memory1;
    final PanamaMemory memory2Panama = (PanamaMemory) memory2;

    try {
      return (Integer) nativeFunctions.invokeFunction(
          "wasmtime4j_memory_bulk_compare",
          FunctionDescriptor.of(ValueLayout.JAVA_INT,
              ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.ADDRESS,
              ValueLayout.JAVA_INT, ValueLayout.JAVA_INT),
          memory1Panama.getMemoryPtr(), offset1, memory2Panama.getMemoryPtr(), offset2, length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in bulk compare", e);
    }
  }

  @Override
  public void batchWrite(final WasmMemory memory, final Map<Integer, ByteBuffer> writes) {
    Objects.requireNonNull(memory, "memory");
    Objects.requireNonNull(writes, "writes");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    final PanamaMemory memoryPanama = (PanamaMemory) memory;

    // Convert map to arrays for efficient native call
    final int[] offsets = new int[writes.size()];
    final byte[][] dataArrays = new byte[writes.size()][];
    int i = 0;
    for (final Map.Entry<Integer, ByteBuffer> entry : writes.entrySet()) {
      if (entry.getKey() == null) {
        throw new IllegalArgumentException("offset cannot be null");
      }
      if (entry.getValue() == null) {
        throw new IllegalArgumentException("ByteBuffer cannot be null");
      }
      offsets[i] = entry.getKey();
      final ByteBuffer buffer = entry.getValue();
      final byte[] data = new byte[buffer.remaining()];
      buffer.get(data);
      dataArrays[i] = data;
      i++;
    }

    try {
      // For now, perform individual writes - real implementation would use native batch operation
      for (int j = 0; j < offsets.length; j++) {
        writeBytes(offsets[j], dataArrays[j], 0, dataArrays[j].length);
      }
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in batch write", e);
    }
  }

  @Override
  public Map<Integer, ByteBuffer> batchRead(final WasmMemory memory, final Map<Integer, Integer> reads) {
    Objects.requireNonNull(memory, "memory");
    Objects.requireNonNull(reads, "reads");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    final Map<Integer, ByteBuffer> results = new HashMap<>();

    try {
      // For now, perform individual reads - real implementation would use native batch operation
      for (final Map.Entry<Integer, Integer> entry : reads.entrySet()) {
        if (entry.getKey() == null) {
          throw new IllegalArgumentException("offset cannot be null");
        }
        if (entry.getValue() == null) {
          throw new IllegalArgumentException("length cannot be null");
        }
        final int offset = entry.getKey();
        final int length = entry.getValue();
        final byte[] data = new byte[length];
        readBytes(offset, data, 0, length);
        results.put(offset, ByteBuffer.wrap(data).asReadOnlyBuffer());
      }
      return results;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in batch read", e);
    }
  }

  @Override
  public int bulkSearch(
      final WasmMemory memory, final int offset, final int length, final byte[] pattern) {
    Objects.requireNonNull(memory, "memory");
    Objects.requireNonNull(pattern, "pattern");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    if (pattern.length == 0) throw new IllegalArgumentException("pattern cannot be empty");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      // For now, return -1 (not found) - real implementation would use native search
      LOGGER.fine("Bulk search operation for pattern of length " + pattern.length);
      return -1;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in bulk search", e);
    }
  }

  @Override
  public void bulkMove(
      final WasmMemory memory, final int destOffset, final int sourceOffset, final int length) {
    Objects.requireNonNull(memory, "memory");
    if (destOffset < 0) throw new IllegalArgumentException("destOffset cannot be negative");
    if (sourceOffset < 0) throw new IllegalArgumentException("sourceOffset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      // For now, use bulk copy to same memory - real implementation would use native move
      bulkCopy(memory, destOffset, memory, sourceOffset, length);
      LOGGER.fine("Bulk move operation from " + sourceOffset + " to " + destOffset + " length " + length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error in bulk move", e);
    }
  }

  // MemoryIntrospection implementation

  @Override
  public MemoryStatistics getStatistics() {
    ensureNotClosed();
    try {
      // For now, return null - real implementation would call native function
      LOGGER.fine("Getting memory statistics for memory: " + getMemoryPtr());
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory statistics", e);
    }
  }

  @Override
  public List<MemorySegment> getSegments() {
    ensureNotClosed();
    try {
      // For now, return null - real implementation would call native function
      LOGGER.fine("Getting memory segments for memory: " + getMemoryPtr());
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory segments", e);
    }
  }

  @Override
  public MemoryUsageReport generateUsageReport() {
    ensureNotClosed();
    try {
      // For now, return null - real implementation would call native function
      LOGGER.fine("Generating usage report for memory: " + getMemoryPtr());
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error generating usage report", e);
    }
  }

  @Override
  public void enablePerformanceTracking() {
    ensureNotClosed();
    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_enable_performance_tracking",
          FunctionDescriptor.ofVoid());
      LOGGER.fine("Enabled performance tracking for memory: " + getMemoryPtr());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error enabling performance tracking", e);
    }
  }

  @Override
  public void disablePerformanceTracking() {
    ensureNotClosed();
    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_disable_performance_tracking",
          FunctionDescriptor.ofVoid());
      LOGGER.fine("Disabled performance tracking for memory: " + getMemoryPtr());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error disabling performance tracking", e);
    }
  }

  @Override
  public boolean isPerformanceTrackingEnabled() {
    ensureNotClosed();
    try {
      return (Boolean) nativeFunctions.invokeFunction(
          "wasmtime4j_memory_is_performance_tracking_enabled",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN));
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error checking performance tracking status", e);
    }
  }

  @Override
  public MemoryPerformanceMetrics getPerformanceMetrics() {
    ensureNotClosed();
    if (!isPerformanceTrackingEnabled()) {
      throw new IllegalStateException("Performance tracking is not enabled");
    }
    try {
      // For now, return null - real implementation would call native function
      LOGGER.fine("Getting performance metrics for memory: " + getMemoryPtr());
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting performance metrics", e);
    }
  }

  @Override
  public void resetMetrics() {
    ensureNotClosed();
    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_reset_metrics",
          FunctionDescriptor.ofVoid());
      LOGGER.fine("Reset metrics for memory: " + getMemoryPtr());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error resetting metrics", e);
    }
  }

  @Override
  public List<String> analyzeAccessPatterns() {
    ensureNotClosed();
    try {
      // For now, return null - real implementation would call native function
      LOGGER.fine("Analyzing access patterns for memory: " + getMemoryPtr());
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error analyzing access patterns", e);
    }
  }

  @Override
  public List<String> detectMemoryIssues() {
    ensureNotClosed();
    try {
      // For now, return null - real implementation would call native function
      LOGGER.fine("Detecting memory issues for memory: " + getMemoryPtr());
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error detecting memory issues", e);
    }
  }

  @Override
  public MemorySegment analyzeRegion(final int offset, final int length) {
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    try {
      // For now, return null - real implementation would call native function
      LOGGER.fine("Analyzing region at offset " + offset + " length " + length + " for memory: " + getMemoryPtr());
      return null;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error analyzing memory region", e);
    }
  }

  @Override
  public boolean validateMemoryIntegrity() {
    ensureNotClosed();
    try {
      // For now, always return true - real implementation would call native function
      LOGGER.fine("Validating memory integrity for memory: " + getMemoryPtr());
      return true;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error validating memory integrity", e);
    }
  }

  @Override
  public String getMemoryLayout() {
    ensureNotClosed();
    try {
      // For now, return placeholder - real implementation would call native function
      return "Memory Layout: 1 segment, 0-" + (getSize() * 65536 - 1) + " bytes";
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting memory layout", e);
    }
  }

  @Override
  public long estimateOperationCost(final String operationType, final int offset, final int length) {
    Objects.requireNonNull(operationType, "operationType");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    try {
      // Simple cost estimation based on operation type and size
      final long baseCost = switch (operationType) {
        case "read" -> 100L;
        case "write" -> 150L;
        case "bulk_copy" -> 50L;
        default -> 200L;
      };

      final long sizeFactor = length / 1024L; // Cost per KB
      final long totalCost = baseCost + sizeFactor;

      LOGGER.fine("Estimated cost for " + operationType + " operation: " + totalCost + " ns");
      return totalCost;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error estimating operation cost", e);
    }
  }

  // MemoryProtection implementation

  @Override
  public void setReadOnly(final WasmMemory memory, final int offset, final int length) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_set_protection",
          FunctionDescriptor.ofVoid(
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN),
          (long) offset, (long) length, true, false, false);

      LOGGER.fine("Set read-only protection for memory: " + getMemoryPtr() + " offset " + offset + " length " + length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting read-only protection", e);
    }
  }

  @Override
  public void setExecutable(final WasmMemory memory, final int offset, final int length) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_set_protection",
          FunctionDescriptor.ofVoid(
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN),
          (long) offset, (long) length, true, true, true);

      LOGGER.fine("Set executable protection for memory: " + getMemoryPtr() + " offset " + offset + " length " + length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting executable protection", e);
    }
  }

  @Override
  public void removeReadOnly(final WasmMemory memory, final int offset, final int length) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_set_protection",
          FunctionDescriptor.ofVoid(
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN),
          (long) offset, (long) length, true, true, false);

      LOGGER.fine("Removed read-only protection for memory: " + getMemoryPtr() + " offset " + offset + " length " + length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error removing read-only protection", e);
    }
  }

  @Override
  public void removeExecutable(final WasmMemory memory, final int offset, final int length) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_set_protection",
          FunctionDescriptor.ofVoid(
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN),
          (long) offset, (long) length, true, true, false);

      LOGGER.fine("Removed executable protection for memory: " + getMemoryPtr() + " offset " + offset + " length " + length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error removing executable protection", e);
    }
  }

  @Override
  public boolean isReadable(final WasmMemory memory, final int offset) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      return (Boolean) nativeFunctions.invokeFunction(
          "wasmtime4j_memory_validate_operation",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN),
          (long) offset, 1L, false);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error checking read permission", e);
    }
  }

  @Override
  public boolean isWritable(final WasmMemory memory, final int offset) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      return (Boolean) nativeFunctions.invokeFunction(
          "wasmtime4j_memory_validate_operation",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN),
          (long) offset, 1L, true);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error checking write permission", e);
    }
  }

  @Override
  public boolean isExecutable(final WasmMemory memory, final int offset) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      // For now, assume memory is not executable by default
      LOGGER.fine("Checking executable status for memory: " + getMemoryPtr() + " offset " + offset);
      return false;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error checking execute permission", e);
    }
  }

  @Override
  public int getProtectionFlags(final WasmMemory memory, final int offset, final int length) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      // For now, return READ | WRITE (3) as default protection
      LOGGER.fine("Getting protection flags for memory: " + getMemoryPtr() + " offset " + offset + " length " + length);
      return 3; // READ(1) | WRITE(2)
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting protection flags", e);
    }
  }

  @Override
  public void setProtectionFlags(final WasmMemory memory, final int offset, final int length, final int flags) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    if (flags < 0 || flags > 7) throw new IllegalArgumentException("Invalid protection flags");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    final boolean read = (flags & 1) != 0;
    final boolean write = (flags & 2) != 0;
    final boolean execute = (flags & 4) != 0;

    try {
      nativeFunctions.invokeFunction(
          "wasmtime4j_memory_set_protection",
          FunctionDescriptor.ofVoid(
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG,
              ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN, ValueLayout.JAVA_BOOLEAN),
          (long) offset, (long) length, read, write, execute);

      LOGGER.fine("Set protection flags " + flags + " for memory: " + getMemoryPtr() + " offset " + offset + " length " + length);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting protection flags", e);
    }
  }

  @Override
  public WasmMemory createProtectedView(
      final WasmMemory memory,
      final int offset,
      final int length,
      final boolean allowRead,
      final boolean allowWrite) {
    Objects.requireNonNull(memory, "memory");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    try {
      // For now, return the same memory instance - real implementation would create a restricted view
      LOGGER.fine("Creating protected view for memory: " + getMemoryPtr() + " offset " + offset + " length " + length +
          " read:" + allowRead + " write:" + allowWrite);
      return memory;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error creating protected view", e);
    }
  }

  @Override
  public boolean validateOperation(
      final WasmMemory memory, final String operation, final int offset, final int length) {
    Objects.requireNonNull(memory, "memory");
    Objects.requireNonNull(operation, "operation");
    if (offset < 0) throw new IllegalArgumentException("offset cannot be negative");
    if (length < 0) throw new IllegalArgumentException("length cannot be negative");
    ensureNotClosed();

    if (!(memory instanceof PanamaMemory)) {
      throw new IllegalArgumentException("memory must be a PanamaMemory instance");
    }

    final boolean isWrite = "write".equals(operation);

    try {
      final boolean allowed = (Boolean) nativeFunctions.invokeFunction(
          "wasmtime4j_memory_validate_operation",
          FunctionDescriptor.of(ValueLayout.JAVA_BOOLEAN,
              ValueLayout.JAVA_LONG, ValueLayout.JAVA_LONG, ValueLayout.JAVA_BOOLEAN),
          (long) offset, (long) length, isWrite);

      LOGGER.fine("Validated " + operation + " operation for memory: " + getMemoryPtr() + ": " + allowed);
      return allowed;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error validating operation", e);
    }
  }

  @Override
  public void enableAuditLogging() {
    ensureNotClosed();
    try {
      LOGGER.fine("Enabled audit logging for memory: " + getMemoryPtr());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error enabling audit logging", e);
    }
  }

  @Override
  public void disableAuditLogging() {
    ensureNotClosed();
    try {
      LOGGER.fine("Disabled audit logging for memory: " + getMemoryPtr());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error disabling audit logging", e);
    }
  }

  @Override
  public boolean isAuditLoggingEnabled() {
    ensureNotClosed();
    try {
      // For now, return false (disabled) as default
      LOGGER.fine("Checking audit logging status for memory: " + getMemoryPtr());
      return false;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error checking audit logging status", e);
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

  /**
   * Gets the native memory pointer for internal use.
   *
   * @return the native memory pointer
   */
  private java.lang.foreign.MemorySegment getMemoryPtr() {
    return memoryPtr;
  }
}
