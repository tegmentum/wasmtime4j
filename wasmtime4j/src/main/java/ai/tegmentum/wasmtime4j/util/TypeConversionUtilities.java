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
   * Converts a WebAssembly value type enum to its string representation.
   *
   * @param type the WebAssembly value type
   * @return the type name string (e.g., "i32", "i64", "f32", "f64", "v128", "funcref", "externref")
   * @throws IllegalArgumentException if type is null or unknown
   */
  public static String typeToString(final WasmValueType type) {
    Validation.requireNonNull(type, "type");
    switch (type) {
      case I32:
        return "i32";
      case I64:
        return "i64";
      case F32:
        return "f32";
      case F64:
        return "f64";
      case V128:
        return "v128";
      case FUNCREF:
        return "funcref";
      case EXTERNREF:
        return "externref";
      default:
        throw new IllegalArgumentException("Unknown WebAssembly type: " + type);
    }
  }

  /**
   * Converts a string representation to WebAssembly value type enum.
   *
   * @param typeString the type name string (case-insensitive)
   * @return the WebAssembly value type
   * @throws IllegalArgumentException if typeString is null or invalid
   */
  public static WasmValueType stringToType(final String typeString) {
    Validation.requireNonNull(typeString, "typeString");
    switch (typeString.toLowerCase()) {
      case "i32":
        return WasmValueType.I32;
      case "i64":
        return WasmValueType.I64;
      case "f32":
        return WasmValueType.F32;
      case "f64":
        return WasmValueType.F64;
      case "v128":
        return WasmValueType.V128;
      case "funcref":
        return WasmValueType.FUNCREF;
      case "externref":
        return WasmValueType.EXTERNREF;
      default:
        throw new IllegalArgumentException("Invalid WebAssembly type string: " + typeString);
    }
  }

  /**
   * Creates string array representation of WebAssembly types.
   *
   * @param types the WebAssembly value types
   * @return array of type name strings
   * @throws IllegalArgumentException if types is null or contains null elements
   */
  public static String[] typesToStrings(final WasmValueType[] types) {
    Validation.requireNonNull(types, "types");

    final String[] strings = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      if (types[i] == null) {
        throw new IllegalArgumentException("Type at index " + i + " is null");
      }
      strings[i] = typeToString(types[i]);
    }
    return strings;
  }

  /**
   * Converts string array to WebAssembly value types.
   *
   * @param typeStrings the type name strings
   * @return array of WebAssembly value types
   * @throws IllegalArgumentException if typeStrings is null or contains invalid strings
   */
  public static WasmValueType[] stringsToTypes(final String[] typeStrings) {
    Validation.requireNonNull(typeStrings, "typeStrings");

    final WasmValueType[] types = new WasmValueType[typeStrings.length];
    for (int i = 0; i < typeStrings.length; i++) {
      if (typeStrings[i] == null) {
        throw new IllegalArgumentException("Type string at index " + i + " is null");
      }
      types[i] = stringToType(typeStrings[i]);
    }
    return types;
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
   * Encodes a WasmValueType to byte representation for native marshalling.
   *
   * <p>The encoding is: i32=0, i64=1, f32=2, f64=3, v128=4, funcref=5, externref=6
   *
   * @param valueType the value type to encode
   * @return byte representation of the type
   * @throws IllegalArgumentException if valueType is null or unsupported
   */
  public static byte encodeValueType(final WasmValueType valueType) {
    Validation.requireNonNull(valueType, "valueType");
    switch (valueType) {
      case I32:
        return 0;
      case I64:
        return 1;
      case F32:
        return 2;
      case F64:
        return 3;
      case V128:
        return 4;
      case FUNCREF:
        return 5;
      case EXTERNREF:
        return 6;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + valueType);
    }
  }

  /**
   * Decodes a byte value to WasmValueType.
   *
   * @param encoded the encoded byte value
   * @return the decoded WasmValueType
   * @throws IllegalArgumentException if encoded value is invalid
   */
  public static WasmValueType decodeValueType(final byte encoded) {
    switch (encoded) {
      case 0:
        return WasmValueType.I32;
      case 1:
        return WasmValueType.I64;
      case 2:
        return WasmValueType.F32;
      case 3:
        return WasmValueType.F64;
      case 4:
        return WasmValueType.V128;
      case 5:
        return WasmValueType.FUNCREF;
      case 6:
        return WasmValueType.EXTERNREF;
      default:
        throw new IllegalArgumentException("Invalid encoded value type: " + encoded);
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
