package ai.tegmentum.wasmtime4j.jni.util;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.exception.JniValidationException;
import java.util.logging.Logger;

/**
 * Utilities for converting between Java and WebAssembly types in JNI context.
 *
 * <p>This class provides comprehensive type conversion and validation for all WebAssembly value
 * types including basic types (i32, i64, f32, f64), SIMD types (v128), and reference types
 * (funcref, externref).
 *
 * <p>All conversions are defensive and validate input types to prevent JVM crashes or incorrect
 * behavior.
 */
public final class JniTypeConverter {

  private static final Logger LOGGER = Logger.getLogger(JniTypeConverter.class.getName());

  /** Size of v128 vector type in bytes. */
  private static final int V128_SIZE_BYTES = 16;

  /** Private constructor to prevent instantiation. */
  private JniTypeConverter() {}

  /**
   * Converts a WebAssembly value type enum to its string representation.
   *
   * @param type the WebAssembly value type
   * @return the type name string
   * @throws JniValidationException if type is null
   */
  public static String typeToString(final WasmValueType type) {
    JniValidation.requireNonNull(type, "type");
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
        throw new JniValidationException("Unknown WebAssembly type: " + type);
    }
  }

  /**
   * Converts a string representation to WebAssembly value type enum.
   *
   * @param typeString the type name string
   * @return the WebAssembly value type
   * @throws JniValidationException if typeString is null or invalid
   */
  public static WasmValueType stringToType(final String typeString) {
    JniValidation.requireNonNull(typeString, "typeString");
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
        throw new JniValidationException("Invalid WebAssembly type string: " + typeString);
    }
  }

  /**
   * Converts an array of WasmValue objects to native function parameters.
   *
   * @param values the WebAssembly values
   * @return array of objects suitable for native function calls
   * @throws JniValidationException if values is null or contains invalid types
   */
  public static Object[] wasmValuesToNativeParams(final WasmValue[] values) {
    JniValidation.requireNonNull(values, "values");

    final Object[] params = new Object[values.length];
    for (int i = 0; i < values.length; i++) {
      if (values[i] == null) {
        throw new JniValidationException("Parameter at index " + i + " is null");
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
   * @throws JniValidationException if value is null or has invalid type
   */
  public static Object wasmValueToNativeParam(final WasmValue value) {
    JniValidation.requireNonNull(value, "value");

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
          throw new JniValidationException(
              "v128 value has invalid size: " + v128Bytes.length + ", expected " + V128_SIZE_BYTES);
        }
        return v128Bytes;
      case FUNCREF:
      case EXTERNREF:
        return value.getValue(); // References are passed as-is
      default:
        throw new JniValidationException("Unsupported WebAssembly type: " + type);
    }
  }

  /**
   * Converts native function results to WasmValue array.
   *
   * @param results the native function results
   * @param expectedTypes the expected return types
   * @return array of WebAssembly values
   * @throws JniValidationException if results or types are invalid
   */
  public static WasmValue[] nativeResultsToWasmValues(
      final Object[] results, final WasmValueType[] expectedTypes) {
    JniValidation.requireNonNull(results, "results");
    JniValidation.requireNonNull(expectedTypes, "expectedTypes");

    if (results.length != expectedTypes.length) {
      throw new JniValidationException(
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
   * @throws JniValidationException if result type doesn't match expected
   */
  public static WasmValue nativeResultToWasmValue(
      final Object result, final WasmValueType expectedType) {
    JniValidation.requireNonNull(expectedType, "expectedType");

    switch (expectedType) {
      case I32:
        if (!(result instanceof Integer)) {
          throw new JniValidationException("Expected i32 result, got: " + getTypeName(result));
        }
        return WasmValue.i32((Integer) result);
      case I64:
        if (!(result instanceof Long)) {
          throw new JniValidationException("Expected i64 result, got: " + getTypeName(result));
        }
        return WasmValue.i64((Long) result);
      case F32:
        if (!(result instanceof Float)) {
          throw new JniValidationException("Expected f32 result, got: " + getTypeName(result));
        }
        return WasmValue.f32((Float) result);
      case F64:
        if (!(result instanceof Double)) {
          throw new JniValidationException("Expected f64 result, got: " + getTypeName(result));
        }
        return WasmValue.f64((Double) result);
      case V128:
        if (!(result instanceof byte[])) {
          throw new JniValidationException("Expected v128 result, got: " + getTypeName(result));
        }
        final byte[] v128Bytes = (byte[]) result;
        if (v128Bytes.length != V128_SIZE_BYTES) {
          throw new JniValidationException(
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
        throw new JniValidationException("Unsupported return type: " + expectedType);
    }
  }

  /**
   * Validates that parameter types match expected function signature.
   *
   * @param params the parameters to validate
   * @param expectedTypes the expected parameter types
   * @throws JniValidationException if types don't match
   */
  public static void validateParameterTypes(
      final WasmValue[] params, final WasmValueType[] expectedTypes) {
    JniValidation.requireNonNull(params, "params");
    JniValidation.requireNonNull(expectedTypes, "expectedTypes");

    if (params.length != expectedTypes.length) {
      throw new JniValidationException(
          "Parameter count mismatch: got " + params.length + ", expected " + expectedTypes.length);
    }

    for (int i = 0; i < params.length; i++) {
      if (params[i] == null) {
        throw new JniValidationException("Parameter at index " + i + " is null");
      }
      final WasmValueType actualType = params[i].getType();
      final WasmValueType expectedType = expectedTypes[i];
      if (actualType != expectedType) {
        throw new JniValidationException(
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
   * Validates that a v128 byte array has the correct size.
   *
   * @param bytes the byte array to validate
   * @throws JniValidationException if array size is incorrect
   */
  public static void validateV128Size(final byte[] bytes) {
    if (bytes == null) {
      throw new JniValidationException("v128 bytes cannot be null");
    }
    if (bytes.length != V128_SIZE_BYTES) {
      throw new JniValidationException(
          "v128 must be exactly " + V128_SIZE_BYTES + " bytes, got " + bytes.length);
    }
  }

  /**
   * Gets a descriptive type name for an object (for error messages).
   *
   * @param obj the object (may be null)
   * @return the type name
   */
  private static String getTypeName(final Object obj) {
    if (obj == null) {
      return "null";
    }
    if (obj instanceof byte[]) {
      return "byte[" + ((byte[]) obj).length + "]";
    }
    return obj.getClass().getSimpleName();
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
   * Creates string array representation of WebAssembly types.
   *
   * @param types the WebAssembly value types
   * @return array of type name strings
   * @throws JniValidationException if types contains null elements
   */
  public static String[] typesToStrings(final WasmValueType[] types) {
    JniValidation.requireNonNull(types, "types");

    final String[] strings = new String[types.length];
    for (int i = 0; i < types.length; i++) {
      if (types[i] == null) {
        throw new JniValidationException("Type at index " + i + " is null");
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
   * @throws JniValidationException if any string is invalid
   */
  public static WasmValueType[] stringsToTypes(final String[] typeStrings) {
    JniValidation.requireNonNull(typeStrings, "typeStrings");

    final WasmValueType[] types = new WasmValueType[typeStrings.length];
    for (int i = 0; i < typeStrings.length; i++) {
      if (typeStrings[i] == null) {
        throw new JniValidationException("Type string at index " + i + " is null");
      }
      types[i] = stringToType(typeStrings[i]);
    }
    return types;
  }
}
