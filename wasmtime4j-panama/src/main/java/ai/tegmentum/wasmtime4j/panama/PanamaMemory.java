package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmMemory;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.nio.ByteBuffer;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Memory.
 *
 * @since 1.0.0
 */
public final class PanamaMemory implements WasmMemory {
  private static final Logger LOGGER = Logger.getLogger(PanamaMemory.class.getName());
  private static final int PAGE_SIZE = 65536; // 64KB
  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeMemory;
  private final String memoryName;
  private final PanamaInstance instance;
  private final PanamaStore store; // For memories created directly by store (instance will be null)
  private volatile boolean closed = false;

  /**
   * Creates a new Panama memory.
   *
   * @param initialPages initial size in pages
   * @param maxPages maximum size in pages (-1 for unlimited)
   */
  public PanamaMemory(final int initialPages, final int maxPages) {
    if (initialPages < 0) {
      throw new IllegalArgumentException("Initial pages cannot be negative");
    }
    this.instance = null;
    this.memoryName = null;

    // TODO: Create native memory via Panama FFI
    throw new UnsupportedOperationException("Creating new memories not yet implemented");
  }

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
    this.nativeMemory = MemorySegment.NULL; // Not used for instance-exported memories
    this.memoryName = memoryName;
    this.instance = instance;
    this.store = null; // Instance-exported memories don't have direct store reference
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
    this.nativeMemory = nativeMemory;
    this.memoryName = null; // Store-created memories don't have a name
    this.instance = null; // Memories created by store don't have an instance
    this.store = store;
    LOGGER.fine("Created memory from store");
  }

  @Override
  public int getSize() {
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot get size: memory not associated with an instance");
    }
    return instance.getMemorySize(this);
  }

  @Override
  public int grow(final int pages) {
    if (pages < 0) {
      throw new IllegalArgumentException("Pages cannot be negative");
    }
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot grow: memory not associated with an instance");
    }
    return instance.growMemory(this, pages);
  }

  @Override
  public int getMaxSize() {
    ensureNotClosed();
    // TODO: Implement max size retrieval
    return -1;
  }

  @Override
  public ByteBuffer getBuffer() {
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot get buffer: memory not associated with an instance");
    }
    return instance.getMemoryBuffer(this);
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
    if (instance == null) {
      throw new IllegalStateException("Cannot read: memory not associated with an instance");
    }
    instance.readMemoryBytes(this, offset, dest, destOffset, length);
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
    if (instance == null) {
      throw new IllegalStateException("Cannot write: memory not associated with an instance");
    }
    instance.writeMemoryBytes(this, offset, src, srcOffset, length);
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

    // Memory initialization from data segments is handled by the runtime during instantiation
    // This is a no-op matching pattern from PanamaTable as Wasmtime manages memory initialization
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

    // Data segments are dropped automatically by the runtime
    // This is a no-op matching pattern from PanamaTable as Wasmtime manages data segments internally
    LOGGER.fine("Dropped data segment: " + dataSegmentIndex);
  }

  @Override
  public boolean isShared() {
    ensureNotClosed();
    // Shared memory check not implemented - return false (non-shared)
    // Matches JNI backend which defaults to false on query failure
    return false;
  }

  @Override
  public ai.tegmentum.wasmtime4j.MemoryType getMemoryType() {
    ensureNotClosed();

    // Return sensible defaults - proper implementation would need store context
    // TODO: Enhance with actual memory type information when store context is available
    final long minimum = 1L;
    final Long maximum = null; // unlimited
    final boolean is64Bit = false; // 32-bit
    final boolean isShared = false; // not shared

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaMemoryType(
        minimum, maximum, is64Bit, isShared, arena, nativeMemory);
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
    if (instance != null && memoryName != null) {
      // For instance-exported memories, look up by name
      final MemorySegment nameSegment = arena.allocateFrom(memoryName);
      final PanamaStore actualStore = getPanamaStore();
      return NATIVE_BINDINGS.instanceGetMemoryByName(
          instance.getNativeInstance(), actualStore.getNativeStore(), nameSegment);
    } else {
      // For store-created memories, use cached pointer
      return nativeMemory;
    }
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
      throw new RuntimeException("Atomic CAS i32 failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic CAS i64 failed with error code: " + errorCode);
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
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicLoadI32(memPtr, storePtr, offset, resultOut);

    if (errorCode != 0) {
      throw new RuntimeException("Atomic load i32 failed with error code: " + errorCode);
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
    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicLoadI64(memPtr, storePtr, offset, resultOut);

    if (errorCode != 0) {
      throw new RuntimeException("Atomic load i64 failed with error code: " + errorCode);
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

    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicStoreI32(memPtr, storePtr, offset, value);

    if (errorCode != 0) {
      throw new RuntimeException("Atomic store i32 failed with error code: " + errorCode);
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

    final int errorCode =
        NATIVE_BINDINGS.memoryAtomicStoreI64(memPtr, storePtr, offset, value);

    if (errorCode != 0) {
      throw new RuntimeException("Atomic store i64 failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic add i32 failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic add i64 failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic and i32 failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic or i32 failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic xor i32 failed with error code: " + errorCode);
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
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
    ensureNotClosed();
    // TODO: Implement 64-bit offset bytes read
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
    ensureNotClosed();
    // TODO: Implement 64-bit offset bytes write
  }

  @Override
  public long getSize64() {
    ensureNotClosed();
    // TODO: Implement 64-bit size retrieval
    return 0L;
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
      throw new RuntimeException("Atomic fence failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic notify failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic wait32 failed with error code: " + errorCode);
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
      throw new RuntimeException("Atomic wait64 failed with error code: " + errorCode);
    }

    return resultOut.get(ValueLayout.JAVA_INT, 0);
  }

  /** Closes the memory and releases resources. */
  public void close() {
    if (closed) {
      return;
    }

    try {
      // TODO: Destroy native memory if created by store
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama memory");
    } catch (final Exception e) {
      LOGGER.warning("Error closing memory: " + e.getMessage());
    }
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
   * Gets the native memory pointer.
   *
   * @return native memory segment
   */
  public MemorySegment getNativeMemory() {
    return nativeMemory;
  }

  /**
   * Ensures the memory is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Memory has been closed");
    }
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
}
