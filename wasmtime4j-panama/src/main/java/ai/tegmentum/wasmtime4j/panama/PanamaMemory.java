package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmMemory;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
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

  @Override
  public int atomicCompareAndSwapInt(final int offset, final int expected, final int newValue) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic CAS int
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public long atomicCompareAndSwapLong(final int offset, final long expected, final long newValue) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic CAS long
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public int atomicLoadInt(final int offset) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic load int
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public long atomicLoadLong(final int offset) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic load long
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public void atomicStoreInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic store int
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public void atomicStoreLong(final int offset, final long value) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic store long
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public int atomicAddInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic add int
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public long atomicAddLong(final int offset, final long value) {
    if (offset < 0 || offset % 8 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 8-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic add long
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public int atomicAndInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic and int
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public int atomicOrInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic or int
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
  }

  @Override
  public int atomicXorInt(final int offset, final int value) {
    if (offset < 0 || offset % 4 != 0) {
      throw new IllegalArgumentException("Offset must be non-negative and 4-byte aligned");
    }
    ensureNotClosed();
    // TODO: Implement atomic xor int
    throw new UnsupportedOperationException("Atomic operations not yet implemented");
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
    // TODO: Implement atomic fence
    throw new UnsupportedOperationException("Atomic fence not yet implemented");
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
    // TODO: Implement atomic notify
    throw new UnsupportedOperationException("Atomic notify not yet implemented");
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
    // TODO: Implement atomic wait for 32-bit value
    throw new UnsupportedOperationException("Atomic wait32 not yet implemented");
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
    // TODO: Implement atomic wait for 64-bit value
    throw new UnsupportedOperationException("Atomic wait64 not yet implemented");
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
