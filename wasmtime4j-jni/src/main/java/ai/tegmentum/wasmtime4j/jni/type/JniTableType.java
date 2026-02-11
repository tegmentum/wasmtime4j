package ai.tegmentum.wasmtime4j.jni.type;

import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * JNI implementation of TableType interface.
 *
 * <p>This class provides type introspection capabilities for WebAssembly table types using JNI
 * bindings to the native Wasmtime library.
 *
 * @since 1.0.0
 */
public final class JniTableType implements TableType {

  private static final Logger LOGGER = Logger.getLogger(JniTableType.class.getName());

  private final WasmValueType elementType;
  private final long minimum;
  private final Optional<Long> maximum;

  /**
   * Creates a new JniTableType instance.
   *
   * @param elementType the element type stored in the table
   * @param minimum the minimum number of elements
   * @param maximum the maximum number of elements (null if unlimited)
   */
  public JniTableType(final WasmValueType elementType, final long minimum, final Long maximum) {
    JniValidation.requireNonNull(elementType, "elementType");
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

    this.elementType = elementType;
    this.minimum = minimum;
    this.maximum = Optional.ofNullable(maximum);

    LOGGER.fine(
        String.format(
            "Created JniTableType: element=%s, min=%d, max=%s", elementType, minimum, maximum));
  }

  /**
   * Creates a JniTableType from native table type information.
   *
   * @param nativeHandle the native handle to the table type
   * @return the JniTableType instance
   * @throws IllegalArgumentException if nativeHandle is invalid
   */
  public static JniTableType fromNative(final long nativeHandle) {
    JniValidation.requireValidHandle(nativeHandle, "nativeHandle");

    final long[] typeInfo = nativeGetTableTypeInfo(nativeHandle);
    if (typeInfo.length < 3) {
      throw new IllegalStateException("Invalid table type info from native");
    }

    final WasmValueType elementType = WasmValueType.fromNativeTypeCode((int) typeInfo[0]);
    final long minimum = typeInfo[1];
    final Long maximum = typeInfo[2] == -1 ? null : typeInfo[2];

    return new JniTableType(elementType, minimum, maximum);
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
  public WasmTypeKind getKind() {
    return WasmTypeKind.TABLE;
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
   * @return array containing [elementTypeCode, minimum, maximum(-1 if unlimited)]
   */
  private static native long[] nativeGetTableTypeInfo(long nativeHandle);
}
