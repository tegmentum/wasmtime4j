package ai.tegmentum.wasmtime4j.panama.type;

import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama implementation of GlobalType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly global types using Panama
 * Foreign Function Interface bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaGlobalType implements GlobalType {

  private static final Logger LOGGER = Logger.getLogger(PanamaGlobalType.class.getName());

  private final WasmValueType valueType;
  private final boolean isMutable;
  private final Arena arena;
  private final MemorySegment nativeHandle;

  /**
   * Creates a new PanamaGlobalType instance.
   *
   * @param valueType the value type of the global
   * @param isMutable true if the global is mutable, false if immutable
   * @param arena the memory arena for resource management
   * @param nativeHandle the native handle to the global type
   */
  public PanamaGlobalType(
      final WasmValueType valueType,
      final boolean isMutable,
      final Arena arena,
      final MemorySegment nativeHandle) {
    PanamaValidation.requireNonNull(valueType, "valueType");
    PanamaValidation.requireNonNull(arena, "arena");
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");

    this.valueType = valueType;
    this.isMutable = isMutable;
    this.arena = arena;
    this.nativeHandle = nativeHandle;

    LOGGER.fine(
        String.format("Created PanamaGlobalType: valueType=%s, mutable=%b", valueType, isMutable));
  }

  /**
   * Creates a PanamaGlobalType from native global type information.
   *
   * @param nativeHandle the native handle to the global type
   * @param arena the memory arena for resource management
   * @return the PanamaGlobalType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static PanamaGlobalType fromNative(final MemorySegment nativeHandle, final Arena arena) {
    PanamaValidation.requireValidHandle(nativeHandle, "nativeHandle");
    PanamaValidation.requireNonNull(arena, "arena");

    // Allocate memory for the type info result
    final MemorySegment typeInfoSegment = arena.allocate(16); // 2 longs * 8 bytes

    // Call native function to get global type info
    nativeGetGlobalTypeInfo(nativeHandle, typeInfoSegment);

    final int valueTypeCode = (int) typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
    final WasmValueType valueType = WasmValueType.fromNativeTypeCode(valueTypeCode);
    final boolean isMutable = typeInfoSegment.get(java.lang.foreign.ValueLayout.JAVA_LONG, 8) != 0;

    return new PanamaGlobalType(valueType, isMutable, arena, nativeHandle);
  }

  @Override
  public WasmValueType getValueType() {
    return valueType;
  }

  @Override
  public boolean isMutable() {
    return isMutable;
  }

  @Override
  public WasmTypeKind getKind() {
    return WasmTypeKind.GLOBAL;
  }

  /**
   * Gets the native handle for this global type.
   *
   * @return the native handle
   */
  public MemorySegment getNativeHandle() {
    return nativeHandle;
  }

  /**
   * Gets the memory arena used by this global type.
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
    if (!(obj instanceof GlobalType)) {
      return false;
    }

    final GlobalType other = (GlobalType) obj;
    return valueType == other.getValueType() && isMutable == other.isMutable();
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(valueType, isMutable);
  }

  @Override
  public String toString() {
    return String.format("GlobalType{valueType=%s, mutable=%b}", valueType, isMutable);
  }

  /**
   * Native method to get global type information.
   *
   * @param nativeHandle the native handle to the global type
   * @param resultBuffer the buffer to store the result [valueTypeCode, isMutable(0/1)]
   */
  private static native void nativeGetGlobalTypeInfo(
      MemorySegment nativeHandle, MemorySegment resultBuffer);
}
