package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.util.TypeConversionUtilities;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.logging.Logger;

/**
 * Utilities for converting between Java and WebAssembly types in JNI context.
 *
 * <p>This class provides comprehensive type conversion and validation for all WebAssembly value
 * types including basic types (i32, i64, f32, f64), SIMD types (v128), and reference types
 * (funcref, externref).
 *
 * <p>All conversions are defensive and validate input types to prevent JVM crashes or incorrect
 * behavior. This class delegates to {@link TypeConversionUtilities} for shared functionality and
 * wraps any exceptions in JNI-specific exception types.
 */
public final class JniTypeConverter {

  private static final Logger LOGGER = Logger.getLogger(JniTypeConverter.class.getName());

  /** Size of v128 vector type in bytes. */
  private static final int V128_SIZE_BYTES = TypeConversionUtilities.V128_SIZE_BYTES;

  /** Private constructor to prevent instantiation. */
  private JniTypeConverter() {}

  /**
   * Converts a WebAssembly value type enum to its string representation.
   *
   * @param type the WebAssembly value type
   * @return the type name string
   * @throws IllegalArgumentException if type is null
   */
  public static String typeToString(final WasmValueType type) {
    if (type == null) {
      throw new IllegalArgumentException("type must not be null");
    }
    return type.name().toLowerCase(java.util.Locale.ROOT);
  }

  /**
   * Converts a string representation to WebAssembly value type enum.
   *
   * @param typeString the type name string
   * @return the WebAssembly value type
   * @throws IllegalArgumentException if typeString is null or invalid
   */
  public static WasmValueType stringToType(final String typeString) {
    if (typeString == null) {
      throw new IllegalArgumentException("typeString must not be null");
    }
    try {
      return WasmValueType.valueOf(typeString.toUpperCase(java.util.Locale.ROOT));
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException("Invalid WebAssembly type string: " + typeString, e);
    }
  }

  /**
   * Converts an array of WasmValue objects to native function parameters.
   *
   * @param values the WebAssembly values
   * @return array of objects suitable for native function calls
   * @throws IllegalArgumentException if values is null or contains invalid types
   */
  public static Object[] wasmValuesToNativeParams(final WasmValue[] values) {
    Validation.requireNonNull(values, "values");

    final Object[] params = new Object[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        throw new IllegalArgumentException("Parameter at index " + i + " is null");
      }
      params[i] = wasmValueToNativeParam(values[i]);
    }
    return params;
  }

  /**
   * Converts a single WasmValue to a native parameter object.
   *
   * @param value the WebAssembly value
   * @return the native parameter object
   * @throws IllegalArgumentException if value is null or has invalid type
   */
  public static Object wasmValueToNativeParam(final WasmValue value) {
    Validation.requireNonNull(value, "value");

    final WasmValueType type = value.getType();
    switch (type) {
      case I32:
        return value.asInt();
      case I64:
        return value.asLong();
      case F32:
        return value.asFloat();
      case F64:
        return value.asDouble();
      case V128:
        final byte[] v128Bytes = value.asV128();
        if (v128Bytes.length != V128_SIZE_BYTES) {
          throw new IllegalArgumentException(
              "v128 value has invalid size: " + v128Bytes.length + ", expected " + V128_SIZE_BYTES);
        }
        return v128Bytes;
      case FUNCREF:
      case EXTERNREF:
        return value.getValue(); // References are passed as-is
      default:
        throw new IllegalArgumentException("Unsupported WebAssembly type: " + type);
    }
  }

  /**
   * Converts native function results to WasmValue array.
   *
   * @param results the native function results
   * @param expectedTypes the expected return types
   * @return array of WebAssembly values
   * @throws IllegalArgumentException if results or types are invalid
   */
  public static WasmValue[] nativeResultsToWasmValues(
      final Object[] results, final WasmValueType[] expectedTypes) {
    Validation.requireNonNull(results, "results");
    Validation.requireNonNull(expectedTypes, "expectedTypes");

    if (results.length != expectedTypes.length) {
      throw new IllegalArgumentException(
          "Result count mismatch: got " + results.length + ", expected " + expectedTypes.length);
    }

    final WasmValue[] wasmValues = new WasmValue[results.length];
    for (int i = 0; i < results.length; i++) {
      wasmValues[i] = nativeResultToWasmValue(results[i], expectedTypes[i]);
    }
    return wasmValues;
  }

  /**
   * Converts a single native result to WasmValue.
   *
   * @param result the native result object
   * @param expectedType the expected WebAssembly type
   * @return the WebAssembly value
   * @throws IllegalArgumentException if result type doesn't match expected
   */
  public static WasmValue nativeResultToWasmValue(
      final Object result, final WasmValueType expectedType) {
    Validation.requireNonNull(expectedType, "expectedType");

    // If the native code already returned a WasmValue, use it directly
    if (result instanceof WasmValue) {
      final WasmValue wasmValue = (WasmValue) result;
      // Optional type validation (defensive check)
      if (wasmValue.getType() != expectedType) {
        throw new IllegalArgumentException(
            "WasmValue type mismatch: got " + wasmValue.getType() + ", expected " + expectedType);
      }
      return wasmValue;
    }

    switch (expectedType) {
      case I32:
        if (!(result instanceof Integer)) {
          throw new IllegalArgumentException("Expected i32 result, got: " + getTypeName(result));
        }
        return WasmValue.i32((Integer) result);
      case I64:
        if (!(result instanceof Long)) {
          throw new IllegalArgumentException("Expected i64 result, got: " + getTypeName(result));
        }
        return WasmValue.i64((Long) result);
      case F32:
        if (!(result instanceof Float)) {
          throw new IllegalArgumentException("Expected f32 result, got: " + getTypeName(result));
        }
        return WasmValue.f32((Float) result);
      case F64:
        if (!(result instanceof Double)) {
          throw new IllegalArgumentException("Expected f64 result, got: " + getTypeName(result));
        }
        return WasmValue.f64((Double) result);
      case V128:
        if (!(result instanceof byte[])) {
          throw new IllegalArgumentException("Expected v128 result, got: " + getTypeName(result));
        }
        final byte[] v128Bytes = (byte[]) result;
        if (v128Bytes.length != V128_SIZE_BYTES) {
          throw new IllegalArgumentException(
              "v128 result has invalid size: "
                  + v128Bytes.length
                  + ", expected "
                  + V128_SIZE_BYTES);
        }
        return WasmValue.v128(v128Bytes);
      case FUNCREF:
        return WasmValue.funcref(result); // May be null
      case EXTERNREF:
        return WasmValue.externref(result); // May be null
      default:
        throw new IllegalArgumentException("Unsupported return type: " + expectedType);
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
    try {
      TypeConversionUtilities.validateParameterTypes(params, expectedTypes);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /**
   * Validates that a v128 byte array has the correct size.
   *
   * @param bytes the byte array to validate
   * @throws IllegalArgumentException if array size is incorrect
   */
  public static void validateV128Size(final byte[] bytes) {
    try {
      TypeConversionUtilities.validateV128Size(bytes);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /**
   * Gets a descriptive type name for an object (for error messages).
   *
   * @param obj the object (may be null)
   * @return the type name
   */
  private static String getTypeName(final Object obj) {
    return TypeConversionUtilities.getTypeName(obj);
  }

  /**
   * Creates a defensive copy of a WasmValueType array.
   *
   * @param types the types array (may be null)
   * @return a defensive copy or empty array if input is null
   */
  public static WasmValueType[] copyTypes(final WasmValueType[] types) {
    return TypeConversionUtilities.copyTypes(types);
  }

  /**
   * Creates string array representation of WebAssembly types.
   *
   * @param types the WebAssembly value types
   * @return array of type name strings
   * @throws IllegalArgumentException if types contains null elements
   */
  public static String[] typesToStrings(final WasmValueType[] types) {
    if (types == null) {
      throw new IllegalArgumentException("types must not be null");
    }
    final String[] strings = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      if (types[i] == null) {
        throw new IllegalArgumentException("Type at index " + i + " is null");
      }
      strings[i] = types[i].name().toLowerCase(java.util.Locale.ROOT);
    }
    return strings;
  }

  /**
   * Converts string array to WebAssembly value types.
   *
   * @param typeStrings the type name strings
   * @return array of WebAssembly value types
   * @throws IllegalArgumentException if any string is invalid
   */
  public static WasmValueType[] stringsToTypes(final String[] typeStrings) {
    if (typeStrings == null) {
      throw new IllegalArgumentException("typeStrings must not be null");
    }
    final WasmValueType[] types = new WasmValueType[typeStrings.length];
    for (int i = 0; i < typeStrings.length; i++) {
      if (typeStrings[i] == null) {
        throw new IllegalArgumentException("Type string at index " + i + " is null");
      }
      try {
        types[i] = WasmValueType.valueOf(typeStrings[i].toUpperCase(java.util.Locale.ROOT));
      } catch (final IllegalArgumentException e) {
        throw new IllegalArgumentException(
            "Invalid WebAssembly type string: " + typeStrings[i], e);
      }
    }
    return types;
  }

  /**
   * Marshals a FunctionType to byte array for native consumption.
   *
   * <p>The marshalling format is: [param_count: 4 bytes][param_types: param_count bytes]
   * [return_count: 4 bytes][return_types: return_count bytes]
   *
   * <p>Each type is encoded as: i32=0, i64=1, f32=2, f64=3, v128=4, funcref=5, externref=6
   *
   * @param functionType the function type to marshal
   * @return byte array containing marshalled function type
   * @throws IllegalArgumentException if functionType is null
   */
  public static byte[] marshalFunctionType(
      final ai.tegmentum.wasmtime4j.type.FunctionType functionType) {
    Validation.requireNonNull(functionType, "functionType");

    final ai.tegmentum.wasmtime4j.WasmValueType[] paramTypes = functionType.getParamTypes();
    final ai.tegmentum.wasmtime4j.WasmValueType[] returnTypes = functionType.getReturnTypes();

    final int totalSize = 8 + paramTypes.length + returnTypes.length;
    final byte[] data = new byte[totalSize];
    int offset = 0;

    // Write parameter count
    writeInt(data, offset, paramTypes.length);
    offset += 4;

    // Write parameter types
    for (final ai.tegmentum.wasmtime4j.WasmValueType paramType : paramTypes) {
      data[offset++] = encodeValueType(paramType);
    }

    // Write return count
    writeInt(data, offset, returnTypes.length);
    offset += 4;

    // Write return types
    for (final ai.tegmentum.wasmtime4j.WasmValueType returnType : returnTypes) {
      data[offset++] = encodeValueType(returnType);
    }

    return data;
  }

  /**
   * Unmarshals parameters from native byte array.
   *
   * @param paramsData the marshalled parameter data
   * @param expectedTypes the expected parameter types for validation
   * @return array of WasmValue parameters
   * @throws IllegalArgumentException if unmarshalling fails or types don't match
   */
  public static WasmValue[] unmarshalParameters(
      final byte[] paramsData, final ai.tegmentum.wasmtime4j.WasmValueType[] expectedTypes) {
    Validation.requireNonNull(paramsData, "paramsData");
    Validation.requireNonNull(expectedTypes, "expectedTypes");

    if (paramsData.length < 4) {
      throw new IllegalArgumentException("Parameter data too short");
    }

    final int paramCount = readInt(paramsData, 0);
    if (paramCount != expectedTypes.length) {
      throw new IllegalArgumentException(
          "Parameter count mismatch: expected " + expectedTypes.length + ", got " + paramCount);
    }

    final WasmValue[] params = new WasmValue[paramCount];
    int offset = 4;

    for (int i = 0; i < paramCount; i++) {
      final ai.tegmentum.wasmtime4j.WasmValueType expectedType = expectedTypes[i];
      params[i] = unmarshalValue(paramsData, offset, expectedType);
      offset += getValueSize(expectedType);
    }

    return params;
  }

  /**
   * Marshals results to native byte array.
   *
   * @param results the result values to marshal
   * @param buffer the buffer to write results to
   * @throws IllegalArgumentException if marshalling fails
   */
  public static void marshalResults(final WasmValue[] results, final byte[] buffer) {
    Validation.requireNonNull(results, "results");
    Validation.requireNonNull(buffer, "buffer");

    // Write result count
    if (buffer.length < 4) {
      throw new IllegalArgumentException("Result buffer too small for count");
    }

    writeInt(buffer, 0, results.length);
    int offset = 4;

    for (final WasmValue result : results) {
      offset = marshalValue(result, buffer, offset);
    }
  }

  /**
   * Encodes a WasmValueType to byte representation.
   *
   * @param valueType the value type to encode
   * @return byte representation of the type
   */
  private static byte encodeValueType(final ai.tegmentum.wasmtime4j.WasmValueType valueType) {
    return (byte) valueType.toNativeTypeCode();
  }

  /**
   * Gets the size in bytes of a value type when marshalled.
   *
   * @param valueType the value type
   * @return size in bytes
   */
  private static int getValueSize(final ai.tegmentum.wasmtime4j.WasmValueType valueType) {
    try {
      return TypeConversionUtilities.getValueSize(valueType);
    } catch (final IllegalArgumentException e) {
      throw new IllegalArgumentException(e.getMessage(), e);
    }
  }

  /**
   * Unmarshals a single value from byte array.
   *
   * @param data the data array
   * @param offset the offset to read from
   * @param valueType the expected value type
   * @return the unmarshalled WasmValue
   */
  private static WasmValue unmarshalValue(
      final byte[] data, final int offset, final ai.tegmentum.wasmtime4j.WasmValueType valueType) {
    switch (valueType) {
      case I32:
        return WasmValue.i32(readInt(data, offset));
      case I64:
        return WasmValue.i64(readLong(data, offset));
      case F32:
        return WasmValue.f32(Float.intBitsToFloat(readInt(data, offset)));
      case F64:
        return WasmValue.f64(Double.longBitsToDouble(readLong(data, offset)));
      case V128:
        final byte[] v128Data = new byte[16];
        System.arraycopy(data, offset, v128Data, 0, 16);
        return WasmValue.v128(v128Data);
      case FUNCREF:
        return WasmValue.funcref(null);
      case EXTERNREF:
        return WasmValue.externref(null);
      default:
        throw new IllegalArgumentException("Unsupported value type: " + valueType);
    }
  }

  /**
   * Marshals a single value to byte array.
   *
   * @param value the value to marshal
   * @param buffer the buffer to write to
   * @param offset the offset to write at
   * @return the new offset after writing
   */
  private static int marshalValue(final WasmValue value, final byte[] buffer, final int offset) {
    switch (value.getType()) {
      case I32:
        writeInt(buffer, offset, value.asInt());
        return offset + 4;
      case I64:
        writeLong(buffer, offset, value.asLong());
        return offset + 8;
      case F32:
        writeInt(buffer, offset, Float.floatToIntBits(value.asFloat()));
        return offset + 4;
      case F64:
        writeLong(buffer, offset, Double.doubleToLongBits(value.asDouble()));
        return offset + 8;
      case V128:
        final byte[] v128Data = value.asV128();
        System.arraycopy(v128Data, 0, buffer, offset, 16);
        return offset + 16;
      case FUNCREF:
      case EXTERNREF:
        // Both reference types use the same null representation
        writeLong(buffer, offset, 0);
        return offset + 8;
      default:
        throw new IllegalArgumentException("Unsupported value type: " + value.getType());
    }
  }

  /**
   * Writes a 32-bit integer to byte array in little-endian format.
   *
   * @param buffer the buffer to write to
   * @param offset the offset to write at
   * @param value the value to write
   */
  private static void writeInt(final byte[] buffer, final int offset, final int value) {
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
   */
  private static void writeLong(final byte[] buffer, final int offset, final long value) {
    writeInt(buffer, offset, (int) (value & 0xFFFFFFFFL));
    writeInt(buffer, offset + 4, (int) ((value >> 32) & 0xFFFFFFFFL));
  }

  /**
   * Reads a 32-bit integer from byte array in little-endian format.
   *
   * @param buffer the buffer to read from
   * @param offset the offset to read from
   * @return the read integer value
   */
  private static int readInt(final byte[] buffer, final int offset) {
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
   */
  private static long readLong(final byte[] buffer, final int offset) {
    final long low = readInt(buffer, offset) & 0xFFFFFFFFL;
    final long high = readInt(buffer, offset + 4) & 0xFFFFFFFFL;
    return low | (high << 32);
  }

  /**
   * Marshals parameters to native byte array format.
   *
   * <p>The marshalling format is: [param_count: 4 bytes][param_data: variable]
   *
   * @param params the parameters to marshal
   * @return byte array containing marshalled parameters
   * @throws IllegalArgumentException if marshalling fails
   */
  public static byte[] marshalParameters(final WasmValue[] params) {
    Validation.requireNonNull(params, "params");

    // Calculate total size needed
    int totalSize = 4; // 4 bytes for parameter count
    for (final WasmValue param : params) {
      totalSize += getValueSize(param.getType());
    }

    final byte[] buffer = new byte[totalSize];
    int offset = 0;

    // Write parameter count
    writeInt(buffer, offset, params.length);
    offset += 4;

    // Write parameter values
    for (final WasmValue param : params) {
      offset = marshalValue(param, buffer, offset);
    }

    return buffer;
  }

  /**
   * Unmarshals results from native byte array.
   *
   * @param resultBuffer the marshalled result data
   * @param expectedTypes the expected result types for validation
   * @return array of WasmValue results
   * @throws IllegalArgumentException if unmarshalling fails or types don't match
   */
  public static WasmValue[] unmarshalResults(
      final byte[] resultBuffer, final ai.tegmentum.wasmtime4j.WasmValueType[] expectedTypes) {
    Validation.requireNonNull(resultBuffer, "resultBuffer");
    Validation.requireNonNull(expectedTypes, "expectedTypes");

    if (resultBuffer.length < 4) {
      throw new IllegalArgumentException("Result buffer too short");
    }

    final int resultCount = readInt(resultBuffer, 0);
    if (resultCount != expectedTypes.length) {
      throw new IllegalArgumentException(
          "Result count mismatch: got " + resultCount + ", expected " + expectedTypes.length);
    }

    final WasmValue[] results = new WasmValue[resultCount];
    int offset = 4;

    for (int i = 0; i < resultCount; i++) {
      final ai.tegmentum.wasmtime4j.WasmValueType expectedType = expectedTypes[i];
      results[i] = unmarshalValue(resultBuffer, offset, expectedType);
      offset += getValueSize(expectedType);
    }

    return results;
  }
}
