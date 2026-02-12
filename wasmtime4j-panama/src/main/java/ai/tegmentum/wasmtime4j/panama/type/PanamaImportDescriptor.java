package ai.tegmentum.wasmtime4j.panama.type;

import ai.tegmentum.wasmtime4j.ImportDescriptor;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Panama implementation of ImportDescriptor interface.
 *
 * <p>This class provides detailed import type information using Panama Foreign Function API
 * bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaImportDescriptor implements ImportDescriptor {

  private static final Logger LOGGER = Logger.getLogger(PanamaImportDescriptor.class.getName());

  private final String moduleName;
  private final String name;
  private final WasmType type;

  /**
   * Creates a new PanamaImportDescriptor instance.
   *
   * @param moduleName the module name of the import
   * @param name the field name of the import
   * @param type the type of the import
   */
  public PanamaImportDescriptor(final String moduleName, final String name, final WasmType type) {
    PanamaValidation.requireNonNull(moduleName, "moduleName");
    PanamaValidation.requireNonNull(name, "name");
    PanamaValidation.requireNonNull(type, "type");

    this.moduleName = moduleName;
    this.name = name;
    this.type = type;

    LOGGER.fine(
        String.format(
            "Created PanamaImportDescriptor: module=%s, name=%s, type=%s", moduleName, name, type));
  }

  /**
   * Creates a PanamaImportDescriptor from native import information.
   *
   * @param nativeSegment the native memory segment to the import descriptor
   * @return the PanamaImportDescriptor instance
   * @throws IllegalArgumentException if nativeSegment is invalid
   */
  public static PanamaImportDescriptor fromNative(
      final MemorySegment nativeSegment, final Arena arena) {
    PanamaValidation.requireNonNull(nativeSegment, "nativeSegment");
    PanamaValidation.requireNonNull(arena, "arena");

    // The native segment is expected to contain:
    // [0]: pointer to module name string (null-terminated C string)
    // [8]: pointer to field name string (null-terminated C string)
    // [16]: type kind ordinal (long)
    // [24]: pointer to type handle (MemorySegment)
    final MemorySegment moduleNamePtr =
        nativeSegment.get(ValueLayout.ADDRESS, 0).reinterpret(Long.MAX_VALUE);
    final MemorySegment fieldNamePtr =
        nativeSegment.get(ValueLayout.ADDRESS, 8).reinterpret(Long.MAX_VALUE);

    final String moduleName = moduleNamePtr.getString(0);
    final String name = fieldNamePtr.getString(0);

    final int typeKindOrdinal = (int) nativeSegment.get(ValueLayout.JAVA_LONG, 16);
    final MemorySegment typeHandle =
        nativeSegment.get(ValueLayout.ADDRESS, 24).reinterpret(Long.MAX_VALUE);

    final WasmTypeKind typeKind = WasmTypeKind.values()[typeKindOrdinal];
    final WasmType type = createTypeFromNative(typeKind, typeHandle, arena);

    return new PanamaImportDescriptor(moduleName, name, type);
  }

  /**
   * Creates a WasmType implementation from native type information.
   *
   * @param typeKind the kind of the type
   * @param typeSegment the native memory segment to the type
   * @param arena the arena for memory allocation
   * @return the appropriate WasmType implementation
   */
  private static WasmType createTypeFromNative(
      final WasmTypeKind typeKind, final MemorySegment typeSegment, final Arena arena) {
    switch (typeKind) {
      case FUNCTION:
        return PanamaFuncType.fromNative(typeSegment, arena);
      case GLOBAL:
        return PanamaGlobalType.fromNative(typeSegment, arena);
      case MEMORY:
        return PanamaMemoryType.fromNative(typeSegment, arena);
      case TABLE:
        return PanamaTableType.fromNative(typeSegment, arena);
      default:
        throw new IllegalArgumentException("Unknown type kind: " + typeKind);
    }
  }

  @Override
  public String getModuleName() {
    return moduleName;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public WasmType getType() {
    return type;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof ImportDescriptor)) {
      return false;
    }

    final ImportDescriptor other = (ImportDescriptor) obj;
    return moduleName.equals(other.getModuleName())
        && name.equals(other.getName())
        && type.equals(other.getType());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(moduleName, name, type);
  }

  @Override
  public String toString() {
    return String.format(
        "ImportDescriptor{module='%s', name='%s', type=%s}", moduleName, name, type);
  }
}
