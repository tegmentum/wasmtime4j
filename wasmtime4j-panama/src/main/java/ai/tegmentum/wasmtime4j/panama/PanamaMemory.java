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
          "Failed to initialize memory from data segment, error code: " + result);
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
      throw new RuntimeException("Failed to drop data segment, error code: " + result);
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
        // On failure, return false as safe default
        return false;
      }
      return isSharedOut.get(ValueLayout.JAVA_INT, 0) != 0;
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.MemoryType getMemoryType() {
    ensureNotClosed();

    // Get actual memory type information from native calls
    final long minimum = 1L;
    final Long maximum = null; // unlimited - would need MemoryType limits API
    final boolean is64Bit = supports64BitAddressing();
    final boolean sharedFlag = isShared();

    return new ai.tegmentum.wasmtime4j.panama.type.PanamaMemoryType(
        minimum, maximum, is64Bit, sharedFlag, arena, nativeMemory);
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
    final int errorCode = NATIVE_BINDINGS.memoryAtomicLoadI32(memPtr, storePtr, offset, resultOut);

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
    final int errorCode = NATIVE_BINDINGS.memoryAtomicLoadI64(memPtr, storePtr, offset, resultOut);

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

    final int errorCode = NATIVE_BINDINGS.memoryAtomicStoreI32(memPtr, storePtr, offset, value);

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

    final int errorCode = NATIVE_BINDINGS.memoryAtomicStoreI64(memPtr, storePtr, offset, value);

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
        throw new RuntimeException("Failed to read memory bytes, error code: " + result);
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
        throw new RuntimeException("Failed to write memory bytes, error code: " + result);
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
        throw new RuntimeException("Failed to get memory size, error code: " + result);
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
