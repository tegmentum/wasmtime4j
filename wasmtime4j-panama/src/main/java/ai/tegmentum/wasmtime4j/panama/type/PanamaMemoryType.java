package ai.tegmentum.wasmtime4j.panama.type;

import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama implementation of MemoryType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly memory types using Panama
 * Foreign Function Interface bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaMemoryType implements MemoryType {

  private static final Logger LOGGER = Logger.getLogger(PanamaMemoryType.class.getName());

  private final long minimum;
  private final Optional<Long> maximum;
  private final boolean is64Bit;
  private final boolean isShared;
  private final Arena arena;
  private final MemorySegment nativeHandle;

  /**
   * Creates a new PanamaMemoryType instance with just the type values.
   *
   * <p>This constructor is used when the type information is already known and no native handle is
   * needed for further operations.
   *
   * @param minimum the minimum number of memory pages
   * @param maximum the maximum number of memory pages (null if unlimited)
   * @param is64Bit true if this is 64-bit addressable memory
   * @param isShared true if this is shared memory
   */
  public PanamaMemoryType(
      final long minimum, final Long maximum, final boolean is64Bit, final boolean isShared) {
    if (minimum < 0) {
      throw new IllegalArgumentException("Minimum page count cannot be negative: " + minimum);
    }
    if (maximum != null && maximum < minimum) {
      throw new IllegalArgumentException(
          "Maximum page count cannot be less than minimum: " + maximum + " < " + minimum);
    }

    this.minimum = minimum;
    this.maximum = Optional.ofNullable(maximum);
    this.is64Bit = is64Bit;
    this.isShared = isShared;
    this.arena = null;
    this.nativeHandle = null;

    LOGGER.fine(
        String.format(
            "Created PanamaMemoryType: min=%d, max=%s, 64bit=%b, shared=%b",
            minimum, maximum, is64Bit, isShared));
  }

  /**
   * Creates a new PanamaMemoryType instance with native handle.
   *
   * @param minimum the minimum number of memory pages
   * @param maximum the maximum number of memory pages (null if unlimited)
   * @param is64Bit true if this is 64-bit addressable memory
   * @param isShared true if this is shared memory
   * @param arena the memory arena for resource management
   * @param nativeHandle the native handle to the memory type
   */
  public PanamaMemoryType(
      final long minimum,
      final Long maximum,
      final boolean is64Bit,
      final boolean isShared,
      final Arena arena,
      final MemorySegment nativeHandle) {
    if (minimum < 0) {
      throw new IllegalArgumentException("Minimum page count cannot be negative: " + minimum);
    }
    if (maximum != null && maximum < minimum) {
      throw new IllegalArgumentException(
          "Maximum page count cannot be less than minimum: " + maximum + " < " + minimum);
    }
    PanamaValidation.requireNonNull(arena, "arena");
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");

    this.minimum = minimum;
    this.maximum = Optional.ofNullable(maximum);
    this.is64Bit = is64Bit;
    this.isShared = isShared;
    this.arena = arena;
    this.nativeHandle = nativeHandle;

    LOGGER.fine(
        String.format(
            "Created PanamaMemoryType: min=%d, max=%s, 64bit=%b, shared=%b",
            minimum, maximum, is64Bit, isShared));
  }

  /**
   * Creates a PanamaMemoryType from native memory type information.
   *
   * @param nativeHandle the native handle to the memory type
   * @param arena the memory arena for resource management
   * @return the PanamaMemoryType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static PanamaMemoryType fromNative(final MemorySegment nativeHandle, final Arena arena) {
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");
    PanamaValidation.requireNonNull(arena, "arena");

    // Allocate memory for the type info result
    final MemorySegment typeInfoSegment = arena.allocate(32); // 4 longs * 8 bytes

    // Call native function to get memory type info
    nativeGetMemoryTypeInfo(nativeHandle, typeInfoSegment);

    final long minimum = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
    final long maxValue = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 8);
    final Long maximum = maxValue == -1 ? null : maxValue;
    final boolean is64Bit = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 16) != 0;
    final boolean isShared = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 24) != 0;

    return new PanamaMemoryType(minimum, maximum, is64Bit, isShared, arena, nativeHandle);
  }

  @Override
  public long getMinimum() {
    return minimum;
  }

  @Override
  public Optional<Long> getMaximum() {
    return maximum;
  }

  @Override
  public boolean is64Bit() {
    return is64Bit;
  }

  @Override
  public boolean isShared() {
    return isShared;
  }

  @Override
  public WasmTypeKind getKind() {
    return WasmTypeKind.MEMORY;
  }

  /**
   * Gets the native handle for this memory type.
   *
   * @return the native handle
   */
  public MemorySegment getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Gets the memory arena used by this memory type.
   *
   * @return the memory arena
   */
  public Arena getArena() {
    return arena;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof MemoryType)) {
      return false;
    }

    final MemoryType other = (MemoryType) obj;
    return minimum == other.getMinimum()
        && maximum.equals(other.getMaximum())
        && is64Bit == other.is64Bit()
        && isShared == other.isShared();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(minimum, maximum, is64Bit, isShared);
  }

  @Override
  public String toString() {
    return String.format(
        "MemoryType{min=%d, max=%s, 64bit=%b, shared=%b}",
        minimum, maximum.map(String::valueOf).orElse("unlimited"), is64Bit, isShared);
  }

  /**
   * Native method to get memory type information.
   *
   * @param nativeHandle the native handle to the memory type
   * @param resultBuffer the buffer to store the result [minimum, maximum(-1 if unlimited),
   *     is64Bit(0/1), isShared(0/1)]
   */
  private static native void nativeGetMemoryTypeInfo(
      MemorySegment nativeHandle, MemorySegment resultBuffer);
}
