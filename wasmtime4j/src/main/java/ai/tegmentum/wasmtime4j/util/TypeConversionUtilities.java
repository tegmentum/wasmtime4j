package ai.tegmentum.wasmtime4j.util;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;

/**
 * Shared utility methods for WebAssembly type conversion and validation.
 *
 * <p>This class provides common type conversion utilities used by both JNI and Panama
 * implementations. All methods are defensive and validate inputs to prevent incorrect behavior.
 *
 * <p>All validation failures throw {@link IllegalArgumentException} which is an unchecked exception
 * that can be caught and wrapped by JNI/Panama-specific exception types.
 *
 * @since 1.0.0
 */
public final class TypeConversionUtilities {

  /** Size of v128 vector type in bytes. */
  public static final int V128_SIZE_BYTES = 16;

  /** Private constructor to prevent instantiation. */
  private TypeConversionUtilities() {
    throw new AssertionError("Utility class should not be instantiated");
  }

  /**
   * Converts a WasmValueType array to an array of native type codes.
   *
   * @param types the types to convert (may be null or empty)
   * @return array of native type codes, or empty array if input is null or empty
   */
  public static int[] toNativeTypes(final WasmValueType[] types) {
    if (types == null || types.length == 0) {
      return new int[0];
    }

    final int[] nativeTypes = new int[types.length];
    for (int i = 0; i < types.length; i++) {
      nativeTypes[i] = types[i].toNativeTypeCode();
    }
    return nativeTypes;
  }

  /**
   * Creates a defensive copy of a WasmValueType array.
   *
   * @param types the types array (may be null)
   * @return a defensive copy or empty array if input is null
   */
  public static WasmValueType[] copyTypes(final WasmValueType[] types) {
    return types == null ? new WasmValueType[0] : types.clone();
  }

  /**
   * Validates that a v128 byte array has the correct size.
   *
   * @param bytes the byte array to validate
   * @throws IllegalArgumentException if bytes is null or has incorrect size
   */
  public static void validateV128Size(final byte[] bytes) {
    if (bytes == null) {
      throw new IllegalArgumentException("v128 bytes cannot be null");
    }
    if (bytes.length != V128_SIZE_BYTES) {
      throw new IllegalArgumentException(
          "v128 must be exactly " + V128_SIZE_BYTES + " bytes, got " + bytes.length);
    }
  }

  /**
   * Validates that parameter types match expected function signature.
   *
   * @param params the parameters to validate
   * @param expectedTypes the expected parameter types
   * @throws IllegalArgumentException if types don't match
   */
  public static void validateParameterTypes(
      final WasmValue[] params, final WasmValueType[] expectedTypes) {
    Validation.requireNonNull(params, "params");
    Validation.requireNonNull(expectedTypes, "expectedTypes");

    if (params.length != expectedTypes.length) {
      throw new IllegalArgumentException(
          "Parameter count mismatch: got " + params.length + ", expected " + expectedTypes.length);
    }

    for (int i = 0; i < params.length; i++) {
      if (params[i] == null) {
        throw new IllegalArgumentException("Parameter at index " + i + " is null");
      }
      final WasmValueType actualType = params[i].getType();
      final WasmValueType expectedType = expectedTypes[i];
      if (actualType != expectedType) {
        throw new IllegalArgumentException(
            "Parameter type mismatch at index "
                + i
                + ": got "
                + actualType
                + ", expected "
                + expectedType);
      }
    }
  }

  /**
   * Gets the size in bytes of a value type when marshalled.
   *
   * @param valueType the value type
   * @return size in bytes
   * @throws IllegalArgumentException if valueType is null or unsupported
   */
  public static int getValueSize(final WasmValueType valueType) {
    Validation.requireNonNull(valueType, "valueType");
    switch (valueType) {
      case I32:
      case F32:
        return 4;
      case I64:
      case F64:
      case FUNCREF:
      case EXTERNREF:
        return 8;
      case V128:
        return 16;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + valueType);
    }
  }

  /**
   * Writes a 32-bit integer to byte array in little-endian format.
   *
   * @param buffer the buffer to write to
   * @param offset the offset to write at
   * @param value the value to write
   * @throws IllegalArgumentException if buffer is null or offset is invalid
   */
  public static void writeInt(final byte[] buffer, final int offset, final int value) {
    Validation.requireNonNull(buffer, "buffer");
    Validation.requireNonNegative(offset, "offset");
    Validation.require(offset + 4 <= buffer.length, "Buffer overflow: need 4 bytes at offset");

    buffer[offset] = (byte) (value & 0xFF);
    buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
    buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  /**
   * Writes a 64-bit long to byte array in little-endian format.
   *
   * @param buffer the buffer to write to
   * @param offset the offset to write at
   * @param value the value to write
   * @throws IllegalArgumentException if buffer is null or offset is invalid
   */
  public static void writeLong(final byte[] buffer, final int offset, final long value) {
    Validation.requireNonNull(buffer, "buffer");
    Validation.requireNonNegative(offset, "offset");
    Validation.require(offset + 8 <= buffer.length, "Buffer overflow: need 8 bytes at offset");

    writeIntUnchecked(buffer, offset, (int) (value & 0xFFFFFFFFL));
    writeIntUnchecked(buffer, offset + 4, (int) ((value >> 32) & 0xFFFFFFFFL));
  }

  /**
   * Reads a 32-bit integer from byte array in little-endian format.
   *
   * @param buffer the buffer to read from
   * @param offset the offset to read from
   * @return the read integer value
   * @throws IllegalArgumentException if buffer is null or offset is invalid
   */
  public static int readInt(final byte[] buffer, final int offset) {
    Validation.requireNonNull(buffer, "buffer");
    Validation.requireNonNegative(offset, "offset");
    Validation.require(offset + 4 <= buffer.length, "Buffer underflow: need 4 bytes at offset");

    return (buffer[offset] & 0xFF)
        | ((buffer[offset + 1] & 0xFF) << 8)
        | ((buffer[offset + 2] & 0xFF) << 16)
        | ((buffer[offset + 3] & 0xFF) << 24);
  }

  /**
   * Reads a 64-bit long from byte array in little-endian format.
   *
   * @param buffer the buffer to read from
   * @param offset the offset to read from
   * @return the read long value
   * @throws IllegalArgumentException if buffer is null or offset is invalid
   */
  public static long readLong(final byte[] buffer, final int offset) {
    Validation.requireNonNull(buffer, "buffer");
    Validation.requireNonNegative(offset, "offset");
    Validation.require(offset + 8 <= buffer.length, "Buffer underflow: need 8 bytes at offset");

    final long low = readIntUnchecked(buffer, offset) & 0xFFFFFFFFL;
    final long high = readIntUnchecked(buffer, offset + 4) & 0xFFFFFFFFL;
    return low | (high << 32);
  }

  /**
   * Gets a descriptive type name for an object (for error messages).
   *
   * @param obj the object (may be null)
   * @return the type name
   */
  public static String getTypeName(final Object obj) {
    if (obj == null) {
      return "null";
    }
    if (obj instanceof byte[]) {
      return "byte[" + ((byte[]) obj).length + "]";
    }
    return obj.getClass().getSimpleName();
  }

  /**
   * Internal helper to write int without validation (after bounds already checked).
   *
   * @param buffer the buffer
   * @param offset the offset
   * @param value the value
   */
  private static void writeIntUnchecked(final byte[] buffer, final int offset, final int value) {
    buffer[offset] = (byte) (value & 0xFF);
    buffer[offset + 1] = (byte) ((value >> 8) & 0xFF);
    buffer[offset + 2] = (byte) ((value >> 16) & 0xFF);
    buffer[offset + 3] = (byte) ((value >> 24) & 0xFF);
  }

  /**
   * Internal helper to read int without validation (after bounds already checked).
   *
   * @param buffer the buffer
   * @param offset the offset
   * @return the read value
   */
  private static int readIntUnchecked(final byte[] buffer, final int offset) {
    return (buffer[offset] & 0xFF)
        | ((buffer[offset + 1] & 0xFF) << 8)
        | ((buffer[offset + 2] & 0xFF) << 16)
        | ((buffer[offset + 3] & 0xFF) << 24);
  }
}
