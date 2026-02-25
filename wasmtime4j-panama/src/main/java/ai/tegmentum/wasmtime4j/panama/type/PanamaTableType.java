package ai.tegmentum.wasmtime4j.panama.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama implementation of TableType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly table types using Panama
 * Foreign Function Interface bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaTableType implements TableType {

  private static final Logger LOGGER = Logger.getLogger(PanamaTableType.class.getName());

  private final WasmValueType elementType;
  private final long minimum;
  private final Optional<Long> maximum;
  private final boolean is64;
  private final Arena arena;
  private final MemorySegment nativeHandle;

  /**
   * Creates a new PanamaTableType instance.
   *
   * @param elementType the element type stored in the table
   * @param minimum the minimum number of elements
   * @param maximum the maximum number of elements (null if unlimited)
   * @param arena the memory arena for resource management
   * @param nativeHandle the native handle to the table type
   */
  public PanamaTableType(
      final WasmValueType elementType,
      final long minimum,
      final Long maximum,
      final Arena arena,
      final MemorySegment nativeHandle) {
    this(elementType, minimum, maximum, false, arena, nativeHandle);
  }

  /**
   * Creates a new PanamaTableType instance with 64-bit index support.
   *
   * @param elementType the element type stored in the table
   * @param minimum the minimum number of elements
   * @param maximum the maximum number of elements (null if unlimited)
   * @param is64 true if this table uses 64-bit indices
   * @param arena the memory arena for resource management
   * @param nativeHandle the native handle to the table type
   */
  public PanamaTableType(
      final WasmValueType elementType,
      final long minimum,
      final Long maximum,
      final boolean is64,
      final Arena arena,
      final MemorySegment nativeHandle) {
    Validation.requireNonNull(elementType, "elementType");
    if (minimum < 0) {
      throw new IllegalArgumentException("Minimum element count cannot be negative: " + minimum);
    }
    if (maximum != null && maximum < minimum) {
      throw new IllegalArgumentException(
          "Maximum element count cannot be less than minimum: " + maximum + " < " + minimum);
    }
    if (!elementType.isReference()) {
      throw new IllegalArgumentException(
          "Table element type must be a reference type: " + elementType);
    }
    Validation.requireNonNull(arena, "arena");
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");

    this.elementType = elementType;
    this.minimum = minimum;
    this.maximum = Optional.ofNullable(maximum);
    this.is64 = is64;
    this.arena = arena;
    this.nativeHandle = nativeHandle;

    LOGGER.fine(
        String.format(
            "Created PanamaTableType: element=%s, min=%d, max=%s, is64=%s",
            elementType, minimum, maximum, is64));
  }

  /**
   * Creates a PanamaTableType from type information without a native handle.
   *
   * <p>This factory method is used when type information is parsed from JSON or other sources where
   * a native handle is not available.
   *
   * @param elementType the element type stored in the table
   * @param minimum the minimum number of elements
   * @param maximum the maximum number of elements (null if unlimited)
   * @return the PanamaTableType instance
   */
  public static PanamaTableType of(
      final WasmValueType elementType, final long minimum, final Long maximum) {
    Validation.requireNonNull(elementType, "elementType");
    if (minimum < 0) {
      throw new IllegalArgumentException("Minimum element count cannot be negative: " + minimum);
    }
    if (maximum != null && maximum < minimum) {
      throw new IllegalArgumentException(
          "Maximum element count cannot be less than minimum: " + maximum + " < " + minimum);
    }
    if (!elementType.isReference()) {
      throw new IllegalArgumentException(
          "Table element type must be a reference type: " + elementType);
    }
    return new PanamaTableType(elementType, minimum, maximum, false);
  }

  /**
   * Private constructor for creating type descriptors without native handles.
   *
   * @param elementType the element type stored in the table
   * @param minimum the minimum number of elements
   * @param maximum the maximum number of elements (null if unlimited)
   */
  private PanamaTableType(
      final WasmValueType elementType, final long minimum, final Long maximum, final boolean is64) {
    this.elementType = elementType;
    this.minimum = minimum;
    this.maximum = Optional.ofNullable(maximum);
    this.is64 = is64;
    this.arena = null;
    this.nativeHandle = MemorySegment.NULL;

    LOGGER.fine(
        String.format(
            "Created PanamaTableType (no native handle): element=%s, min=%d, max=%s",
            elementType, minimum, maximum));
  }

  /**
   * Creates a PanamaTableType from native table type information.
   *
   * @param nativeHandle the native handle to the table type
   * @param arena the memory arena for resource management
   * @return the PanamaTableType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static PanamaTableType fromNative(final MemorySegment nativeHandle, final Arena arena) {
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");
    Validation.requireNonNull(arena, "arena");

    // Allocate memory for the type info result
    final MemorySegment typeInfoSegment = arena.allocate(32); // 4 longs * 8 bytes

    // Call native function to get table type info
    nativeGetTableTypeInfo(nativeHandle, typeInfoSegment);

    final int elementTypeCode =
        (int) typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
    final WasmValueType elementType = WasmValueType.fromNativeTypeCode(elementTypeCode);
    final long minimum = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 8);
    final long maxValue = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 16);
    final Long maximum = maxValue == -1 ? null : maxValue;
    final boolean is64 = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 24) != 0;

    return new PanamaTableType(elementType, minimum, maximum, is64, arena, nativeHandle);
  }

  @Override
  public WasmValueType getElementType() {
    return elementType;
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
    return is64;
  }

  @Override
  public WasmTypeKind getKind() {
    return WasmTypeKind.TABLE;
  }

  /**
   * Gets the native handle for this table type.
   *
   * @return the native handle
   */
  public MemorySegment getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Gets the memory arena used by this table type.
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
    if (!(obj instanceof TableType)) {
      return false;
    }

    final TableType other = (TableType) obj;
    return elementType == other.getElementType()
        && minimum == other.getMinimum()
        && maximum.equals(other.getMaximum());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(elementType, minimum, maximum);
  }

  @Override
  public String toString() {
    return String.format(
        "TableType{element=%s, min=%d, max=%s}",
        elementType, minimum, maximum.map(String::valueOf).orElse("unlimited"));
  }

  /**
   * Native method to get table type information.
   *
   * @param nativeHandle the native handle to the table type
   * @param resultBuffer the buffer to store the result [elementTypeCode, minimum, maximum(-1 if
   *     unlimited)]
   */
  private static native void nativeGetTableTypeInfo(
      MemorySegment nativeHandle, MemorySegment resultBuffer);
}
