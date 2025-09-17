package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.ExportDescriptor;
import ai.tegmentum.wasmtime4j.WasmType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of ExportDescriptor interface.
 *
 * <p>This class provides detailed export type information using JNI bindings to the native
 * Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniExportDescriptor implements ExportDescriptor {

  private static final Logger LOGGER = Logger.getLogger(JniExportDescriptor.class.getName());

  private final String name;
  private final WasmType type;

  /**
   * Creates a new JniExportDescriptor instance.
   *
   * @param name the field name of the export
   * @param type the type of the export
   */
  public JniExportDescriptor(final String name, final WasmType type) {
    JniValidation.requireNonNull(name, "name");
    JniValidation.requireNonNull(type, "type");

    this.name = name;
    this.type = type;

    LOGGER.fine(
        String.format("Created JniExportDescriptor: name=%s, type=%s", name, type));
  }

  /**
   * Creates a JniExportDescriptor from native export information.
   *
   * @param nativeHandle the native handle to the export descriptor
   * @return the JniExportDescriptor instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static JniExportDescriptor fromNative(final long nativeHandle) {
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");

    final String name = nativeGetExportName(nativeHandle);
    if (name == null) {
      throw new IllegalStateException("Export name is null from native");
    }

    final long[] typeInfo = nativeGetExportTypeInfo(nativeHandle);
    if (typeInfo.length < 2) {
      throw new IllegalStateException("Invalid export type info from native");
    }

    final WasmTypeKind typeKind = WasmTypeKind.values()[(int) typeInfo[0]];
    final long typeHandle = typeInfo[1];

    final WasmType type = createTypeFromNative(typeKind, typeHandle);

    return new JniExportDescriptor(name, type);
  }

  /**
   * Creates a WasmType implementation from native type information.
   *
   * @param typeKind the kind of the type
   * @param typeHandle the native handle to the type
   * @return the appropriate WasmType implementation
   */
  private static WasmType createTypeFromNative(final WasmTypeKind typeKind, final long typeHandle) {
    switch (typeKind) {
      case FUNCTION:
        return JniFuncType.fromNative(typeHandle);
      case GLOBAL:
        return JniGlobalType.fromNative(typeHandle);
      case MEMORY:
        return JniMemoryType.fromNative(typeHandle);
      case TABLE:
        return JniTableType.fromNative(typeHandle);
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

  /**
   * Native method to get export name.
   *
   * @param nativeHandle the native handle to the export descriptor
   * @return the export name
   */
  private static native String nativeGetExportName(long nativeHandle);

  /**
   * Native method to get export type information.
   *
   * @param nativeHandle the native handle to the export descriptor
   * @return array containing [typeKindOrdinal, typeHandle]
   */
  private static native long[] nativeGetExportTypeInfo(long nativeHandle);
}