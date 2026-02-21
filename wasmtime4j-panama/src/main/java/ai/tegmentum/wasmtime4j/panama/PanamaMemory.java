package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Memory.
 *
 * @since 1.0.0
 */
public final class PanamaMemory implements WasmMemory {
  private static final Logger LOGGER = Logger.getLogger(PanamaMemory.class.getName());
  private static final int PAGE_SIZE = 65536; // 64KB
  private static final NativeMemoryBindings NATIVE_BINDINGS = NativeMemoryBindings.getInstance();
  private static final NativeInstanceBindings INSTANCE_BINDINGS =
      NativeInstanceBindings.getInstance();

  // Buffer pool constants for optimized memory operations
  private static final int SMALL_BUFFER_SIZE = 4096; // 4KB
  private static final int MEDIUM_BUFFER_SIZE = 65536; // 64KB
  private static final long BUFFER_CACHE_VALIDITY_MS = 100; // Cache validity in milliseconds

  // Performance threshold: use ByteBuffer for small ops, MemorySegment.copy for large
  private static final int BULK_OPERATION_THRESHOLD = 1024;

  private final Arena arena;
  private final MemorySegment nativeMemory;
  private final String memoryName;
  private final PanamaInstance instance;
  private final PanamaStore store; // For memories created directly by store (instance will be null)
  private final NativeResourceHandle resourceHandle;

  // Performance optimization: cached memory pointer to avoid repeated lookups
  private volatile MemorySegment cachedMemoryPointer;

  // Performance optimization: reusable buffers to avoid per-operation allocations
  private final Arena bufferArena;
  private volatile MemorySegment smallBuffer;
  private volatile MemorySegment mediumBuffer;

  // Performance optimization: cached ByteBuffer for getBuffer() calls
  private volatile ByteBuffer cachedByteBuffer;
  private volatile long cachedByteBufferSize;
  private volatile long lastByteBufferCheck;

  // Performance optimization: direct zero-copy access to WASM linear memory
  private volatile MemorySegment directMemorySegment;
  private volatile long directMemorySize;
  private volatile ByteBuffer directByteBuffer;

  /**
   * Package-private constructor for wrapping an exported memory.
   *
   * @param memoryName the name of the memory export
   * @param instance the instance that owns this memory
   */
  PanamaMemory(final String memoryName, final PanamaInstance instance) {
    if (memoryName == null || memoryName.isEmpty()) {
      throw new IllegalArgumentException("Memory name cannot be null or empty");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    this.arena = Arena.ofShared();
    this.bufferArena = Arena.ofShared();
    this.nativeMemory = MemorySegment.NULL; // Not used for instance-exported memories
    this.memoryName = memoryName;
    this.instance = instance;
    this.store = null; // Instance-exported memories don't have direct store reference
    this.resourceHandle = createResourceHandle();
    LOGGER.fine("Created memory wrapper for export: " + memoryName);
  }

  /**
   * Package-private constructor for memories created directly by a store.
   *
   * @param nativeMemory the native memory pointer from Wasmtime
   * @param store the store that owns this memory
   */
  PanamaMemory(final MemorySegment nativeMemory, final PanamaStore store) {
    if (nativeMemory == null || nativeMemory.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native memory pointer cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    this.arena = Arena.ofShared();
    this.bufferArena = Arena.ofShared();
    this.nativeMemory = nativeMemory;
    this.memoryName = null; // Store-created memories don't have a name
    this.instance = null; // Memories created by store don't have an instance
    this.store = store;
    this.resourceHandle = createResourceHandle();
    LOGGER.fine("Created memory from store");
  }

  /**
   * Package-private constructor for standalone shared memories created from an Engine.
   *
   * <p>Standalone shared memory does not require a Store or Instance. Operations that need
   * a Store context (read, write, grow) will throw {@link IllegalStateException}.
   * Store-independent operations (getSize, getMemoryType, isShared) will work.
   *
   * @param nativeMemory the native memory pointer from Wasmtime
   */
  PanamaMemory(final MemorySegment nativeMemory) {
    if (nativeMemory == null || nativeMemory.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native memory pointer cannot be null");
    }
    this.arena = Arena.ofShared();
    this.bufferArena = Arena.ofShared();
    this.nativeMemory = nativeMemory;
    this.memoryName = null;
    this.instance = null;
    this.store = null;
    this.resourceHandle = createResourceHandle();
    LOGGER.fine("Created standalone shared memory");
  }

  @Override
  public int getSize() {
    ensureNotClosed();
    if (instance != null) {
      final long size = instance.getMemorySize(this);
      return (int) Math.min(size, Integer.MAX_VALUE);
    }
    // Memory created directly by store - use native memory pointer
    if (nativeMemory != null && !nativeMemory.equals(MemorySegment.NULL) && store != null) {
      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment sizeOutPtr = tempArena.allocate(ValueLayout.JAVA_LONG);
        final int result =
            NATIVE_BINDINGS.panamaMemorySizePages(nativeMemory, store.getNativeStore(), sizeOutPtr);
        if (result == 0) {
          return (int) Math.min(sizeOutPtr.get(ValueLayout.JAVA_LONG, 0), Integer.MAX_VALUE);
        }
        throw new IllegalStateException(
            "Failed to get memory size: " + PanamaErrorMapper.getErrorDescription(result));
      }
    }
    throw new IllegalStateException("Cannot get size: memory not associated with an instance");
  }

  @Override
  public int grow(final int pages) {
    if (pages < 0) {
      throw new IllegalArgumentException("Pages cannot be negative");
    }
    ensureNotClosed();

    long result;
    if (instance != null) {
      result = instance.growMemory(this, (long) pages);
    } else if (nativeMemory != null && !nativeMemory.equals(MemorySegment.NULL) && store != null) {
      // Memory created directly by store - use native memory pointer
      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment previousPagesOutPtr = tempArena.allocate(ValueLayout.JAVA_LONG);
        final int growResult =
            NATIVE_BINDINGS.panamaMemoryGrow(
                nativeMemory, store.getNativeStore(), (long) pages, previousPagesOutPtr);
        if (growResult == 0) {
          result = previousPagesOutPtr.get(ValueLayout.JAVA_LONG, 0);
        } else {
          result = -1L; // Grow failed
        }
      }
    } else {
      throw new IllegalStateException("Cannot grow: memory not associated with an instance");
    }

    // Invalidate cached direct memory segment since grow may relocate memory
    if (result >= 0) {
      invalidateDirectMemoryCache();
    }

    return (int) Math.min(result, Integer.MAX_VALUE);
  }

  @Override
  public int getMaxSize() {
    ensureNotClosed();
    if (instance != null) {
      return instance.getMemoryMaxSize(this);
    }
    // Memory created directly by store - use native memory pointer
    if (nativeMemory != null && !nativeMemory.equals(MemorySegment.NULL) && store != null) {
      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment maxOutPtr = tempArena.allocate(ValueLayout.JAVA_LONG);
        final int result =
            NATIVE_BINDINGS.panamaMemoryGetMaximum(nativeMemory, store.getNativeStore(), maxOutPtr);
        if (result == 0) {
          final long maxPages = maxOutPtr.get(ValueLayout.JAVA_LONG, 0);
          // -1 means unlimited, return Integer.MAX_VALUE for API compatibility
          return maxPages < 0 ? Integer.MAX_VALUE : (int) maxPages;
        }
        throw new IllegalStateException(
            "Failed to get memory max size: " + PanamaErrorMapper.getErrorDescription(result));
      }
    }
    throw new IllegalStateException("Cannot get max size: memory not associated with an instance");
  }

  @Override
  @SuppressFBWarnings(
      value = "EI_EXPOSE_BUF",
      justification = "Returns duplicate() of buffer, not the internal reference")
  public ByteBuffer getBuffer() {
    ensureNotClosed();

    final long currentTime = System.currentTimeMillis();

    // Performance optimization: use cached ByteBuffer if still valid
    if (cachedByteBuffer != null
        && (currentTime - lastByteBufferCheck) < BUFFER_CACHE_VALIDITY_MS) {
      // Check if memory size changed (due to grow)
      final long currentSizeBytes = (long) getSize() * PAGE_SIZE;
      if (currentSizeBytes == cachedByteBufferSize) {
        return cachedByteBuffer.duplicate();
      }
    }

    // Get current memory size
    final long sizeBytes = (long) getSize() * PAGE_SIZE;
    if (sizeBytes > Integer.MAX_VALUE) {
      throw new IllegalStateException("Memory too large for ByteBuffer: " + sizeBytes + " bytes");
    }

    // Read entire memory into ByteBuffer
    final byte[] data = new byte[(int) sizeBytes];
    if (sizeBytes > 0) {
      readBytes(0, data, 0, (int) sizeBytes);
    }

    cachedByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    cachedByteBufferSize = sizeBytes;
    lastByteBufferCheck = currentTime;

    return cachedByteBuffer.duplicate();
  }

  @Override
  public byte readByte(final int offset) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    ensureNotClosed();
    final ByteBuffer buffer = getBuffer();
    if (offset >= buffer.limit()) {
      throw new IndexOutOfBoundsException("Offset " + offset + " is out of bounds");
    }
    return buffer.get(offset);
  }

  @Override
  public void writeByte(final int offset, final byte value) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    ensureNotClosed();
    final ByteBuffer buffer = getBuffer();
    if (offset >= buffer.limit()) {
      throw new IndexOutOfBoundsException("Offset " + offset + " is out of bounds");
    }
    buffer.put(offset, value);
  }

  @Override
  public void readBytes(
      final int offset, final byte[] dest, final int destOffset, final int length) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    if (dest == null) {
      throw new IllegalArgumentException("Destination array cannot be null");
    }
    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("Destination offset cannot be negative");
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative");
    }
    if (destOffset + length > dest.length) {
      throw new IndexOutOfBoundsException(
          "Destination array bounds exceeded: destOffset="
              + destOffset
              + ", length="
              + length
              + ", array length="
              + dest.length);
    }
    ensureNotClosed();

    // Bounds check against cached size (initializes direct memory if needed)
    final MemorySegment directMem = getDirectMemorySegment();
    if ((long) offset + length > directMemorySize) {
      throw new IndexOutOfBoundsException(
          "Memory access out of bounds: offset="
              + offset
              + ", length="
              + length
              + ", size="
              + directMemorySize);
    }

    // Performance optimization: use ByteBuffer for small operations (JVM intrinsics)
    // MemorySegment.copy is better for large bulk transfers
    if (length < BULK_OPERATION_THRESHOLD) {
      final ByteBuffer buffer = getDirectByteBuffer();
      // Use absolute get to avoid position() overhead (Java 13+)
      buffer.get(offset, dest, destOffset, length);
    } else {
      // Use MemorySegment.copy for large operations
      MemorySegment.copy(directMem, ValueLayout.JAVA_BYTE, offset, dest, destOffset, length);
    }
  }

  @Override
  public void writeBytes(
      final int offset, final byte[] src, final int srcOffset, final int length) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    if (src == null) {
      throw new IllegalArgumentException("Source array cannot be null");
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("Source offset cannot be negative");
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative");
    }
    if (srcOffset + length > src.length) {
      throw new IndexOutOfBoundsException(
          "Source array bounds exceeded: srcOffset="
              + srcOffset
              + ", length="
              + length
              + ", array length="
              + src.length);
    }
    ensureNotClosed();

    // Bounds check against cached size (initializes direct memory if needed)
    final MemorySegment directMem = getDirectMemorySegment();
    if ((long) offset + length > directMemorySize) {
      throw new IndexOutOfBoundsException(
          "Memory access out of bounds: offset="
              + offset
              + ", length="
              + length
              + ", size="
              + directMemorySize);
    }

    // Performance optimization: use ByteBuffer for small operations (JVM intrinsics)
    // MemorySegment.copy is better for large bulk transfers
    if (length < BULK_OPERATION_THRESHOLD) {
      final ByteBuffer buffer = getDirectByteBuffer();
      // Use absolute put to avoid position() overhead (Java 13+)
      buffer.put(offset, src, srcOffset, length);
    } else {
      // Use MemorySegment.copy for large operations
      MemorySegment.copy(src, srcOffset, directMem, ValueLayout.JAVA_BYTE, offset, length);
    }
  }

  @Override
  public void copy(final int destOffset, final int srcOffset, final int length) {
    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("Destination offset cannot be negative");
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("Source offset cannot be negative");
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative");
    }
    ensureNotClosed();
    if (length == 0) {
      return;
    }
    final ByteBuffer buffer = getBuffer();
    if (srcOffset + length > buffer.limit()) {
      throw new IndexOutOfBoundsException(
          "Source range [" + srcOffset + ", " + (srcOffset + length) + ") is out of bounds");
    }
    if (destOffset + length > buffer.limit()) {
      throw new IndexOutOfBoundsException(
          "Destination range [" + destOffset + ", " + (destOffset + length) + ") is out of bounds");
    }

    // Handle overlapping regions correctly using a temp buffer
    final byte[] temp = new byte[length];
    for (int i = 0; i < length; i++) {
      temp[i] = buffer.get(srcOffset + i);
    }
    for (int i = 0; i < length; i++) {
      buffer.put(destOffset + i, temp[i]);
    }
  }

  @Override
  public void fill(final int offset, final byte value, final int length) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative");
    }
    ensureNotClosed();
    if (length == 0) {
      return;
    }
    final ByteBuffer buffer = getBuffer();
    if (offset + length > buffer.limit()) {
      throw new IndexOutOfBoundsException(
          "Range [" + offset + ", " + (offset + length) + ") is out of bounds");
    }
    for (int i = 0; i < length; i++) {
      buffer.put(offset + i, value);
    }
  }

  @Override
  public void init(
      final int destOffset, final int dataSegmentIndex, final int srcOffset, final int length) {
    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("Destination offset cannot be negative");
    }
    if (dataSegmentIndex < 0) {
      throw new IllegalArgumentException("Data segment index cannot be negative");
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("Source offset cannot be negative");
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative");
    }
    ensureNotClosed();

    if (length == 0) {
      return; // Nothing to do
    }

    if (instance == null) {
      throw new IllegalStateException("Cannot init: memory not associated with an instance");
    }

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();
    final MemorySegment instancePtr = instance.getNativeInstance();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Instance pointer is null");
    }

    final int result =
        NATIVE_BINDINGS.panamaMemoryInit(
            memPtr, storePtr, instancePtr, destOffset, dataSegmentIndex, srcOffset, length);

    if (result != 0) {
      throw new RuntimeException(
          "Failed to initialize memory from data segment: "
              + PanamaErrorMapper.getErrorDescription(result));
    }

    LOGGER.fine(
        "Initialized memory range ["
            + destOffset
            + ", "
            + (destOffset + length)
            + ") from data segment "
            + dataSegmentIndex
            + " at offset "
            + srcOffset);
  }

  @Override
  public void dropDataSegment(final int dataSegmentIndex) {
    if (dataSegmentIndex < 0) {
      throw new IllegalArgumentException("Data segment index cannot be negative");
    }
    ensureNotClosed();

    if (instance == null) {
      throw new IllegalStateException(
          "Cannot drop data segment: memory not associated with an instance");
    }

    final MemorySegment instancePtr = instance.getNativeInstance();

    if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Instance pointer is null");
    }

    final int result = NATIVE_BINDINGS.dataSegmentDrop(instancePtr, dataSegmentIndex);

    if (result != 0) {
      throw new RuntimeException(
          "Failed to drop data segment: " + PanamaErrorMapper.getErrorDescription(result));
    }

    LOGGER.fine("Dropped data segment: " + dataSegmentIndex);
  }

  @Override
  public boolean isShared() {
    ensureNotClosed();
    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment isSharedOut = localArena.allocate(ValueLayout.JAVA_INT);
      final PanamaStore panamaStore = getPanamaStore();
      final MemorySegment memPtr = getMemoryPointer();
      final MemorySegment storePtr = panamaStore.getNativeStore();
      final int result = NATIVE_BINDINGS.panamaMemoryIsShared(memPtr, storePtr, isSharedOut);
      if (result != 0) {
        throw new RuntimeException(
            "Failed to query memory shared status: "
                + PanamaErrorMapper.getErrorDescription(result));
      }
      return isSharedOut.get(ValueLayout.JAVA_INT, 0) != 0;
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.type.MemoryType getMemoryType() {
    ensureNotClosed();

    // Get type characteristics from native calls
    final boolean is64Bit = supports64BitAddressing();
    final boolean sharedFlag = isShared();

    // Get memory pointer and store pointer
    final MemorySegment memPtr = getMemoryPointer();
    final PanamaStore panamaStore = getPanamaStore();
    final MemorySegment storePtr = panamaStore.getNativeStore();

    // Get minimum from native
    final MemorySegment minimumOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int minResult = NATIVE_BINDINGS.panamaMemoryGetMinimum(memPtr, storePtr, minimumOut);
    if (minResult != 0) {
      throw new IllegalStateException(
          "Failed to get memory minimum: " + PanamaErrorMapper.getErrorDescription(minResult));
    }
    final long minimum = minimumOut.get(ValueLayout.JAVA_LONG, 0);

    // Get maximum from native (-1 means unlimited)
    final MemorySegment maximumOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int maxResult = NATIVE_BINDINGS.panamaMemoryGetMaximum(memPtr, storePtr, maximumOut);
    if (maxResult != 0) {
      throw new IllegalStateException(
          "Failed to get memory maximum: " + PanamaErrorMapper.getErrorDescription(maxResult));
    }
    final long maxValue = maximumOut.get(ValueLayout.JAVA_LONG, 0);
    final Long maximum = maxValue == -1 ? null : maxValue;

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaMemoryType(
        minimum, maximum, is64Bit, sharedFlag);
  }

  /**
   * Helper method to get the PanamaStore from either store or instance.
   *
   * @return PanamaStore instance
   */
  private PanamaStore getPanamaStore() {
    if (store != null) {
      return store;
    }
    if (instance != null) {
      return (PanamaStore) instance.getStore();
    }
    throw new IllegalStateException("No store available");
  }

  /**
   * Helper method to get the memory pointer for atomic operations.
   *
   * @return memory segment pointer
   */
  private MemorySegment getMemoryPointer() {
    // Performance optimization: return cached pointer if available
    if (cachedMemoryPointer != null) {
      return cachedMemoryPointer;
    }

    if (instance != null && memoryName != null) {
      // For instance-exported memories, look up by name and cache
      final MemorySegment nameSegment = arena.allocateFrom(memoryName);
      final PanamaStore actualStore = getPanamaStore();
      cachedMemoryPointer =
          INSTANCE_BINDINGS.instanceGetMemoryByName(
              instance.getNativeInstance(), actualStore.getNativeStore(), nameSegment);
      return cachedMemoryPointer;
    } else {
      // For store-created memories, use the native pointer directly
      cachedMemoryPointer = nativeMemory;
      return nativeMemory;
    }
  }

  /**
   * Gets a reusable buffer from the pool, or allocates a new one for large requests.
   *
   * @param size the required buffer size
   * @return a memory segment buffer of at least the requested size
   */
  private MemorySegment getReusableBuffer(final int size) {
    if (size <= SMALL_BUFFER_SIZE) {
      if (smallBuffer == null) {
        smallBuffer = bufferArena.allocate(SMALL_BUFFER_SIZE);
      }
      return smallBuffer;
    } else if (size <= MEDIUM_BUFFER_SIZE) {
      if (mediumBuffer == null) {
        mediumBuffer = bufferArena.allocate(MEDIUM_BUFFER_SIZE);
      }
      return mediumBuffer;
    }
    // Large buffers: allocate fresh (will be collected when no longer referenced)
    return bufferArena.allocate(size);
  }

  /**
   * Gets a direct zero-copy memory segment for the WASM linear memory.
   *
   * <p>This method uses the native panamaMemoryGetData function to obtain a raw pointer to the WASM
   * linear memory, then creates a MemorySegment that directly maps to it. This enables zero-copy
   * read/write operations.
   *
   * <p>The segment is cached and only refreshed when needed (e.g., after memory.grow()).
   *
   * @return a MemorySegment directly mapping the WASM linear memory
   */
  private MemorySegment getDirectMemorySegment() {
    // Return cached segment if available
    if (directMemorySegment != null) {
      return directMemorySegment;
    }

    final MemorySegment memPtr = getMemoryPointer();
    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment storePtr = getNativeStorePointer();

    // Allocate output pointers for data pointer and size
    final MemorySegment dataPtrOut = arena.allocate(ValueLayout.ADDRESS);
    final MemorySegment sizeOut = arena.allocate(ValueLayout.JAVA_LONG);

    final int result = NATIVE_BINDINGS.panamaMemoryGetData(memPtr, storePtr, dataPtrOut, sizeOut);

    if (result != 0) {
      throw new RuntimeException(
          "Failed to get memory data pointer: " + PanamaErrorMapper.getErrorDescription(result));
    }

    final long rawPtr = dataPtrOut.get(ValueLayout.ADDRESS, 0).address();
    final long size = sizeOut.get(ValueLayout.JAVA_LONG, 0);

    if (rawPtr == 0) {
      throw new IllegalStateException("Memory data pointer is null");
    }

    // Create a MemorySegment that directly maps to the WASM linear memory
    directMemorySegment = MemorySegment.ofAddress(rawPtr).reinterpret(size);
    directMemorySize = size;

    return directMemorySegment;
  }

  /**
   * Gets a direct ByteBuffer that maps to the WASM linear memory.
   *
   * <p>This provides zero-copy access to the WASM memory through a DirectByteBuffer, which can be
   * faster than MemorySegment.copy() for small operations.
   *
   * @return a ByteBuffer directly mapping the WASM linear memory
   */
  private ByteBuffer getDirectByteBuffer() {
    // Return cached ByteBuffer if available and valid
    if (directByteBuffer != null) {
      return directByteBuffer;
    }

    // Get the direct memory segment first
    final MemorySegment directMem = getDirectMemorySegment();

    // Create a ByteBuffer view of the memory segment
    directByteBuffer = directMem.asByteBuffer().order(ByteOrder.LITTLE_ENDIAN);

    return directByteBuffer;
  }

  /**
   * Invalidates the cached direct memory segment.
   *
   * <p>This must be called after any operation that may invalidate the memory pointer, such as
   * memory.grow().
   */
  private void invalidateDirectMemoryCache() {
    directMemorySegment = null;
    directMemorySize = 0;
    directByteBuffer = null;
    cachedByteBuffer = null;
    cachedByteBufferSize = 0;
  }

  @Override
  public int atomicCompareAndSwapInt(final int offset, final int expected, final int newValue) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicCompareAndSwapI32(
            memPtr, storePtr, offset, expected, newValue, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public long atomicCompareAndSwapLong(final int offset, final long expected, final long newValue) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicCompareAndSwapI64(
            memPtr, storePtr, offset, expected, newValue, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_LONG, 0);
  }

  @Override
  public int atomicLoadInt(final int offset) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode = NATIVE_BINDINGS.memoryAtomicLoadI32(memPtr, storePtr, offset, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public long atomicLoadLong(final int offset) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int errorCode = NATIVE_BINDINGS.memoryAtomicLoadI64(memPtr, storePtr, offset, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_LONG, 0);
  }

  @Override
  public void atomicStoreInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final int errorCode = NATIVE_BINDINGS.memoryAtomicStoreI32(memPtr, storePtr, offset, value);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }
  }

  @Override
  public void atomicStoreLong(final int offset, final long value) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final int errorCode = NATIVE_BINDINGS.memoryAtomicStoreI64(memPtr, storePtr, offset, value);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }
  }

  @Override
  public int atomicAddInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicAddI32(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public long atomicAddLong(final int offset, final long value) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicAddI64(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_LONG, 0);
  }

  @Override
  public int atomicAndInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicAndI32(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public int atomicOrInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicOrI32(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public int atomicXorInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicXorI32(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public long atomicAndLong(final int offset, final long value) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicAndI64(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_LONG, 0);
  }

  @Override
  public long atomicOrLong(final int offset, final long value) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicOrI64(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_LONG, 0);
  }

  @Override
  public long atomicXorLong(final int offset, final long value) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_LONG);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicXorI64(memPtr, storePtr, offset, value, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_LONG, 0);
  }

  @Override
  public void readBytes64(
      final long offset, final byte[] dest, final int destOffset, final int length) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    if (dest == null) {
      throw new IllegalArgumentException("Destination array cannot be null");
    }
    if (destOffset < 0) {
      throw new IndexOutOfBoundsException("Destination offset cannot be negative");
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative");
    }
    if (destOffset + length > dest.length) {
      throw new IndexOutOfBoundsException(
          "Destination array bounds exceeded: destOffset="
              + destOffset
              + ", length="
              + length
              + ", array length="
              + dest.length);
    }
    ensureNotClosed();

    if (length == 0) {
      return;
    }

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment buffer = localArena.allocate(length);
      final int result =
          NATIVE_BINDINGS.panamaMemoryReadBytes(memPtr, storePtr, offset, length, buffer);

      if (result != 0) {
        throw new RuntimeException(
            "Failed to read memory bytes: " + PanamaErrorMapper.getErrorDescription(result));
      }

      // Copy from native buffer to destination array
      for (int i = 0; i < length; i++) {
        dest[destOffset + i] = buffer.get(ValueLayout.JAVA_BYTE, i);
      }
    }
  }

  @Override
  public void writeBytes64(
      final long offset, final byte[] src, final int srcOffset, final int length) {
    if (offset < 0) {
      throw new IndexOutOfBoundsException("Offset cannot be negative");
    }
    if (src == null) {
      throw new IllegalArgumentException("Source array cannot be null");
    }
    if (srcOffset < 0) {
      throw new IndexOutOfBoundsException("Source offset cannot be negative");
    }
    if (length < 0) {
      throw new IndexOutOfBoundsException("Length cannot be negative");
    }
    if (srcOffset + length > src.length) {
      throw new IndexOutOfBoundsException(
          "Source array bounds exceeded: srcOffset="
              + srcOffset
              + ", length="
              + length
              + ", array length="
              + src.length);
    }
    ensureNotClosed();

    if (length == 0) {
      return;
    }

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment buffer = localArena.allocate(length);

      // Copy from source array to native buffer
      for (int i = 0; i < length; i++) {
        buffer.set(ValueLayout.JAVA_BYTE, i, src[srcOffset + i]);
      }

      final int result =
          NATIVE_BINDINGS.panamaMemoryWriteBytes(memPtr, storePtr, offset, length, buffer);

      if (result != 0) {
        throw new RuntimeException(
            "Failed to write memory bytes: " + PanamaErrorMapper.getErrorDescription(result));
      }
    }
  }

  @Override
  public long getSize64() {
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment sizeOut = localArena.allocate(ValueLayout.JAVA_LONG);
      final int result = NATIVE_BINDINGS.panamaMemorySizePages64(memPtr, storePtr, sizeOut);

      if (result != 0) {
        throw new RuntimeException(
            "Failed to get memory size: " + PanamaErrorMapper.getErrorDescription(result));
      }

      return sizeOut.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public long grow64(final long pages) {
    if (pages < 0) {
      throw new IllegalArgumentException("Pages cannot be negative");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment previousPagesOut = localArena.allocate(ValueLayout.JAVA_LONG);
      final int result =
          NATIVE_BINDINGS.panamaMemoryGrow64(memPtr, storePtr, pages, previousPagesOut);

      if (result != 0) {
        return -1; // Growth failed
      }

      return previousPagesOut.get(ValueLayout.JAVA_LONG, 0);
    }
  }

  @Override
  public boolean supports64BitAddressing() {
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      return false; // Conservative default
    }

    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment is64BitOut = localArena.allocate(ValueLayout.JAVA_INT);
      final int result = NATIVE_BINDINGS.panamaMemoryIs64Bit(memPtr, storePtr, is64BitOut);

      if (result != 0) {
        return false; // Conservative default on error
      }

      return is64BitOut.get(ValueLayout.JAVA_INT, 0) != 0;
    }
  }

  @Override
  public void atomicFence() {
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final int errorCode = NATIVE_BINDINGS.memoryAtomicFence(memPtr, storePtr);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }
  }

  @Override
  public int atomicNotify(final int offset, final int count) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicNotify(memPtr, storePtr, offset, count, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public int atomicWait32(final int offset, final int expected, final long timeoutNanos) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    if (timeoutNanos < 0 && timeoutNanos != -1) {
      throw new IllegalArgumentException("Timeout must be non-negative or -1 for infinite");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicWait32(
            memPtr, storePtr, offset, expected, timeoutNanos, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public int atomicWait64(final int offset, final long expected, final long timeoutNanos) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    if (timeoutNanos < 0 && timeoutNanos != -1) {
      throw new IllegalArgumentException("Timeout must be non-negative or -1 for infinite");
    }
    ensureNotClosed();

    final PanamaStore actualStore = getPanamaStore();
    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment memPtr = getMemoryPointer();

    if (memPtr == null || memPtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Memory pointer is null");
    }

    final MemorySegment resultOut = arena.allocate(ValueLayout.JAVA_INT);
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicWait64(
            memPtr, storePtr, offset, expected, timeoutNanos, resultOut);

    if (errorCode != 0) {
      throwAtomicOperationError(errorCode, "Atomic operation failed");
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  @Override
  public long growAsync(final long pages) throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (pages < 0) {
      throw new IllegalArgumentException("Pages cannot be negative");
    }
    ensureNotClosed();

    if (instance != null) {
      return instance.growMemoryAsync(this, pages);
    } else if (nativeMemory != null && !nativeMemory.equals(MemorySegment.NULL) && store != null) {
      try (final Arena tempArena = Arena.ofConfined()) {
        final MemorySegment previousPagesOutPtr = tempArena.allocate(ValueLayout.JAVA_LONG);
        final int growResult =
            NATIVE_BINDINGS.panamaMemoryGrowAsync(
                nativeMemory, store.getNativeStore(), pages, previousPagesOutPtr);
        if (growResult == 0) {
          return previousPagesOutPtr.get(ValueLayout.JAVA_LONG, 0);
        }
        return -1L; // Grow failed
      }
    }
    throw new IllegalStateException("Cannot grow async: memory not associated with an instance");
  }

  /** Closes the memory and releases resources. */
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the memory name.
   *
   * @return memory export name
   */
  String getMemoryName() {
    return memoryName;
  }

  /**
   * Gets the instance this memory was exported from, if any.
   *
   * @return the instance, or null if this memory was created directly by a store
   */
  PanamaInstance getSourceInstance() {
    return instance;
  }

  /**
   * Gets the native memory pointer.
   *
   * <p>For instance-exported memories, this method looks up the memory pointer by name. For
   * store-created memories, it returns the stored native pointer.
   *
   * @return native memory segment, or NULL if the memory pointer cannot be obtained
   */
  public MemorySegment getNativeMemory() {
    // Use getMemoryPointer to properly resolve instance-exported memories
    return getMemoryPointer();
  }

  /**
   * Checks if this memory is an instance-exported memory.
   *
   * <p>Instance-exported memories are created by calling getMemory() on an Instance. Store-created
   * memories are created directly by the Store.
   *
   * @return true if this memory was exported from an instance, false if store-created
   */
  boolean isInstanceExported() {
    return instance != null && memoryName != null;
  }

  /**
   * Gets the instance that owns this memory (for instance-exported memories).
   *
   * @return the owning instance, or null if this is a store-created memory
   */
  PanamaInstance getOwningInstance() {
    return instance;
  }

  /**
   * Gets the export name of this memory (for instance-exported memories).
   *
   * @return the export name, or null if this is a store-created memory
   */
  String getExportName() {
    return memoryName;
  }

  /**
   * Ensures the memory is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Creates the resource handle with the memory's cleanup logic.
   *
   * @return the resource handle
   */
  private NativeResourceHandle createResourceHandle() {
    final MemorySegment capturedNativeMemory = this.nativeMemory;
    final PanamaInstance capturedInstance = this.instance;
    final Arena capturedArena = this.arena;
    final Arena capturedBufferArena = this.bufferArena;
    return new NativeResourceHandle(
        "PanamaMemory",
        () -> {
          // Destroy native memory handles to prevent leaks.
          // For instance-exported memories, cachedMemoryPointer holds the ValidatedMemory
          // allocated by instanceGetMemoryByName. For store-created memories, nativeMemory
          // holds the ValidatedMemory from creation. Both must be properly freed.
          if (instance != null
              && cachedMemoryPointer != null
              && !cachedMemoryPointer.equals(MemorySegment.NULL)) {
            // Instance-exported memory: destroy the cached lookup result
            NATIVE_BINDINGS.memoryDestroy(cachedMemoryPointer);
          } else if (instance == null
              && nativeMemory != null
              && !nativeMemory.equals(MemorySegment.NULL)) {
            // Store-created memory: destroy the creation result
            NATIVE_BINDINGS.memoryDestroy(nativeMemory);
          }
          cachedMemoryPointer = null;
          smallBuffer = null;
          mediumBuffer = null;
          cachedByteBuffer = null;
          directMemorySegment = null;
          directMemorySize = 0;
          directByteBuffer = null;
          bufferArena.close();
          arena.close();
        },
        this,
        () -> {
          // Safety net: destroy store-created native memory and close arenas.
          // For instance-exported memories, cachedMemoryPointer is lazily set and cannot
          // be captured at construction time. The instance owns the underlying memory.
          if (capturedInstance == null
              && capturedNativeMemory != null
              && !capturedNativeMemory.equals(MemorySegment.NULL)) {
            NATIVE_BINDINGS.memoryDestroy(capturedNativeMemory);
          }
          if (capturedBufferArena != null && capturedBufferArena.scope().isAlive()) {
            capturedBufferArena.close();
          }
          if (capturedArena != null && capturedArena.scope().isAlive()) {
            capturedArena.close();
          }
        });
  }

  /**
   * Gets the native store pointer from either the instance or direct store reference.
   *
   * @return native store segment
   * @throws IllegalStateException if store is not available or not a PanamaStore
   */
  private MemorySegment getNativeStorePointer() {
    if (store != null) {
      // Memory was created directly by store
      return store.getNativeStore();
    }
    if (instance == null || instance.getStore() == null) {
      throw new IllegalStateException("Instance or store is null");
    }
    if (!(instance.getStore() instanceof PanamaStore)) {
      throw new IllegalStateException("Store is not a PanamaStore");
    }
    return ((PanamaStore) instance.getStore()).getNativeStore();
  }


  /**
   * Throws an appropriate exception for a failed atomic operation based on the native error code
   * and error message.
   *
   * @param errorCode the native error code
   * @param operation description of the failed operation
   */
  private static void throwAtomicOperationError(final int errorCode, final String operation) {
    final String nativeMessage = PanamaErrorMapper.retrieveNativeErrorMessage();
    final String message;
    if (nativeMessage != null && !nativeMessage.isEmpty()) {
      message = operation + ": " + nativeMessage;
    } else {
      message = operation + ": " + PanamaErrorMapper.getErrorDescription(errorCode);
    }
    if (nativeMessage != null && nativeMessage.contains("shared memory")) {
      throw new UnsupportedOperationException(message);
    }
    throw new RuntimeException(message);
  }
}
