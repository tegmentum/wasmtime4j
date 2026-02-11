package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.ImportDescriptor;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of ImportDescriptor interface.
 *
 * <p>This class provides detailed import type information using JNI bindings to the native Wasmtime
 * library.
 *
 * @since 1.0.0
 */
public final class JniImportDescriptor implements ImportDescriptor {

  private static final Logger LOGGER = Logger.getLogger(JniImportDescriptor.class.getName());

  private final String moduleName;
  private final String name;
  private final WasmType type;

  /**
   * Creates a new JniImportDescriptor instance.
   *
   * @param moduleName the module name of the import
   * @param name the field name of the import
   * @param type the type of the import
   */
  public JniImportDescriptor(final String moduleName, final String name, final WasmType type) {
    JniValidation.requireNonNull(moduleName, "moduleName");
    JniValidation.requireNonNull(name, "name");
    JniValidation.requireNonNull(type, "type");

    this.moduleName = moduleName;
    this.name = name;
    this.type = type;

    LOGGER.fine(
        String.format(
            "Created JniImportDescriptor: module=%s, name=%s, type=%s", moduleName, name, type));
  }

  /**
   * Creates a JniImportDescriptor from native import information.
   *
   * @param nativeHandle the native handle to the import descriptor
   * @return the JniImportDescriptor instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static JniImportDescriptor fromNative(final long nativeHandle) {
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");

    final String[] stringInfo = nativeGetImportStringInfo(nativeHandle);
    if (stringInfo.length < 2) {
      throw new IllegalStateException("Invalid import string info from native");
    }

    final String moduleName = stringInfo[0];
    final String name = stringInfo[1];

    final long[] typeInfo = nativeGetImportTypeInfo(nativeHandle);
    if (typeInfo.length < 2) {
      throw new IllegalStateException("Invalid import type info from native");
    }

    final WasmTypeKind typeKind = WasmTypeKind.values()[(int) typeInfo[0]];
    final long typeHandle = typeInfo[1];

    final WasmType type = createTypeFromNative(typeKind, typeHandle);

    return new JniImportDescriptor(moduleName, name, type);
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

  /**
   * Native method to get import string information.
   *
   * @param nativeHandle the native handle to the import descriptor
   * @return array containing [moduleName, fieldName]
   */
  private static native String[] nativeGetImportStringInfo(long nativeHandle);

  /**
   * Native method to get import type information.
   *
   * @param nativeHandle the native handle to the import descriptor
   * @return array containing [typeKindOrdinal, typeHandle]
   */
  private static native long[] nativeGetImportTypeInfo(long nativeHandle);
}
