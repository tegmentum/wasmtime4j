package ai.tegmentum.wasmtime4j.panama.type;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.panama.util.PanamaValidation;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.util.logging.Logger;

/**
 * Panama implementation of ExportDescriptor interface.
 *
 * <p>This class provides detailed export type information using Panama Foreign Function API
 * bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class PanamaExportDescriptor implements ExportDescriptor {

  private static final Logger LOGGER = Logger.getLogger(PanamaExportDescriptor.class.getName());

  private final String name;
  private final WasmType type;

  /**
   * Creates a new PanamaExportDescriptor instance.
   *
   * @param name the field name of the export
   * @param type the type of the export
   */
  public PanamaExportDescriptor(final String name, final WasmType type) {
    PanamaValidation.requireNonNull(name, "name");
    PanamaValidation.requireNonNull(type, "type");

    this.name = name;
    this.type = type;

    LOGGER.fine(String.format("Created PanamaExportDescriptor: name=%s, type=%s", name, type));
  }

  /**
   * Creates a PanamaExportDescriptor from native export information.
   *
   * @param nativeSegment the native memory segment to the export descriptor
   * @param arena the arena for memory allocation
   * @return the PanamaExportDescriptor instance
   * @throws IllegalArgumentException if nativeSegment is invalid
   */
  public static PanamaExportDescriptor fromNative(
      final MemorySegment nativeSegment, final Arena arena) {
    PanamaValidation.requireNonNull(nativeSegment, "nativeSegment");
    PanamaValidation.requireNonNull(arena, "arena");

    // The native segment is expected to contain:
    // [0]: pointer to export name string (null-terminated C string)
    // [8]: type kind ordinal (long)
    // [16]: pointer to type handle (MemorySegment)
    final MemorySegment namePtr =
        nativeSegment.get(ValueLayout.ADDRESS, 0).reinterpret(Long.MAX_VALUE);

    final String name = namePtr.getString(0);
    if (name == null) {
      throw new IllegalStateException("Export name is null from native");
    }

    final int typeKindOrdinal = (int) nativeSegment.get(ValueLayout.JAVA_LONG, 8);
    final MemorySegment typeHandle =
        nativeSegment.get(ValueLayout.ADDRESS, 16).reinterpret(Long.MAX_VALUE);

    final WasmTypeKind typeKind = WasmTypeKind.values()[typeKindOrdinal];
    final WasmType type = createTypeFromNative(typeKind, typeHandle, arena);

    return new PanamaExportDescriptor(name, type);
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
    if (!(obj instanceof ExportDescriptor)) {
      return false;
    }

    final ExportDescriptor other = (ExportDescriptor) obj;
    return name.equals(other.getName()) && type.equals(other.getType());
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(name, type);
  }

  @Override
  public String toString() {
    return String.format("ExportDescriptor{name='%s', type=%s}", name, type);
  }
}
