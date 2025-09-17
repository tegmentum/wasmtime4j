package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.GlobalType;
import ai.tegmentum.wasmtime4j.WasmTypeKind;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of GlobalType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly global types using JNI
 * bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniGlobalType implements GlobalType {

  private static final Logger LOGGER = Logger.getLogger(JniGlobalType.class.getName());

  private final WasmValueType valueType;
  private final boolean isMutable;

  /**
   * Creates a new JniGlobalType instance.
   *
   * @param valueType the value type of the global
   * @param isMutable true if the global is mutable, false if immutable
   */
  public JniGlobalType(final WasmValueType valueType, final boolean isMutable) {
    JniValidation.requireNonNull(valueType, "valueType");

    this.valueType = valueType;
    this.isMutable = isMutable;

    LOGGER.fine(
        String.format(
            "Created JniGlobalType: valueType=%s, mutable=%b", valueType, isMutable));
  }

  /**
   * Creates a JniGlobalType from native global type information.
   *
   * @param nativeHandle the native handle to the global type
   * @return the JniGlobalType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static JniGlobalType fromNative(final long nativeHandle) {
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");

    final long[] typeInfo = nativeGetGlobalTypeInfo(nativeHandle);
    if (typeInfo.length < 2) {
      throw new IllegalStateException("Invalid global type info from native");
    }

    final WasmValueType valueType = WasmValueType.fromNativeTypeCode((int) typeInfo[0]);
    final boolean isMutable = typeInfo[1] != 0;

    return new JniGlobalType(valueType, isMutable);
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
    return String.format(
        "GlobalType{valueType=%s, mutable=%b}", valueType, isMutable);
  }

  /**
   * Native method to get global type information.
   *
   * @param nativeHandle the native handle to the global type
   * @return array containing [valueTypeCode, isMutable(0/1)]
   */
  private static native long[] nativeGetGlobalTypeInfo(long nativeHandle);
}