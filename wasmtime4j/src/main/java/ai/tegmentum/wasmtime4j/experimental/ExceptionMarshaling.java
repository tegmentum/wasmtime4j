/*
 * Copyright 2024 Tegmentum Technology, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.experimental;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.MarshalingException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.logging.Logger;

/**
 * Utilities for marshaling WebAssembly exception payloads.
 *
 * <p><strong>EXPERIMENTAL:</strong> This API provides marshaling utilities for converting between
 * Java objects and WebAssembly exception payloads.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Payload serialization and deserialization
 *   <li>Type-safe marshaling operations
 *   <li>Efficient byte-level payload handling
 *   <li>Support for all WebAssembly value types
 *   <li>Validation and error handling
 * </ul>
 *
 * @since 1.0.0
 */
@ExperimentalApi(feature = ExperimentalFeatures.Feature.EXCEPTION_HANDLING)
public final class ExceptionMarshaling {

  private static final Logger LOGGER = Logger.getLogger(ExceptionMarshaling.class.getName());

  // Prevent instantiation
  private ExceptionMarshaling() {}

  /**
   * Marshals a list of Java objects to WebAssembly values for exception payload.
   *
   * @param objects the Java objects to marshal
   * @param expectedTypes the expected WebAssembly value types
   * @return list of WebAssembly values
   * @throws IllegalArgumentException if objects or expectedTypes is null
   * @throws MarshalingException if marshaling fails
   */
  public static List<WasmValue> marshalPayload(
      final List<Object> objects, final List<WasmValueType> expectedTypes) {
    if (objects == null) {
      throw new IllegalArgumentException("Objects list cannot be null");
    }
    if (expectedTypes == null) {
      throw new IllegalArgumentException("Expected types list cannot be null");
    }

    if (objects.size() != expectedTypes.size()) {
      throw new MarshalingException(
          "Objects count ("
              + objects.size()
              + ") doesn't match expected types count ("
              + expectedTypes.size()
              + ")");
    }

    final List<WasmValue> values = new ArrayList<>(objects.size());

    for (int i = 0; i < objects.size(); i++) {
      final Object obj = objects.get(i);
      final WasmValueType expectedType = expectedTypes.get(i);

      try {
        final WasmValue value = marshalSingleValue(obj, expectedType);
        values.add(value);
      } catch (final Exception e) {
        throw new MarshalingException(
            "Failed to marshal object at index " + i + " to type " + expectedType, e);
      }
    }

    LOGGER.fine("Marshaled " + values.size() + " values for exception payload");
    return values;
  }

  /**
   * Unmarshals WebAssembly exception payload to Java objects.
   *
   * @param payload the WebAssembly values to unmarshal
   * @return list of Java objects
   * @throws IllegalArgumentException if payload is null
   * @throws MarshalingException if unmarshaling fails
   */
  public static List<Object> unmarshalPayload(final List<WasmValue> payload) {
    if (payload == null) {
      throw new IllegalArgumentException("Payload cannot be null");
    }

    final List<Object> objects = new ArrayList<>(payload.size());

    for (int i = 0; i < payload.size(); i++) {
      final WasmValue value = payload.get(i);

      try {
        final Object obj = unmarshalSingleValue(value);
        objects.add(obj);
      } catch (final Exception e) {
        throw new MarshalingException(
            "Failed to unmarshal WebAssembly value at index " + i + " of type " + value.getType(),
            e);
      }
    }

    LOGGER.fine("Unmarshaled " + objects.size() + " objects from exception payload");
    return objects;
  }

  /**
   * Serializes WebAssembly exception payload to byte array.
   *
   * @param payload the WebAssembly values to serialize
   * @return serialized byte array
   * @throws IllegalArgumentException if payload is null
   * @throws MarshalingException if serialization fails
   */
  public static byte[] serializePayload(final List<WasmValue> payload) {
    if (payload == null) {
      throw new IllegalArgumentException("Payload cannot be null");
    }

    // Calculate total size needed
    int totalSize = 4; // Size header
    for (final WasmValue value : payload) {
      totalSize += 1; // Type byte
      totalSize += getValueSize(value);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    // Write payload size
    buffer.putInt(payload.size());

    // Write each value
    for (final WasmValue value : payload) {
      serializeValue(buffer, value);
    }

    LOGGER.fine("Serialized exception payload to " + buffer.position() + " bytes");
    return buffer.array();
  }

  /**
   * Deserializes byte array to WebAssembly exception payload.
   *
   * @param data the serialized byte array
   * @return list of WebAssembly values
   * @throws IllegalArgumentException if data is null
   * @throws MarshalingException if deserialization fails
   */
  public static List<WasmValue> deserializePayload(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }

    if (data.length < 4) {
      throw new MarshalingException("Data too short to contain valid payload");
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data);
    buffer.order(ByteOrder.LITTLE_ENDIAN);

    // Read payload size
    final int payloadSize = buffer.getInt();
    if (payloadSize < 0 || payloadSize > 1000) { // Reasonable limit
      throw new MarshalingException("Invalid payload size: " + payloadSize);
    }

    final List<WasmValue> payload = new ArrayList<>(payloadSize);

    // Read each value
    for (int i = 0; i < payloadSize; i++) {
      try {
        final WasmValue value = deserializeValue(buffer);
        payload.add(value);
      } catch (final Exception e) {
        throw new MarshalingException("Failed to deserialize value at index " + i, e);
      }
    }

    LOGGER.fine("Deserialized exception payload with " + payload.size() + " values");
    return payload;
  }

  /**
   * Validates that a payload matches the expected types.
   *
   * @param payload the payload to validate
   * @param expectedTypes the expected types
   * @throws IllegalArgumentException if payload or expectedTypes is null
   * @throws MarshalingException if validation fails
   */
  public static void validatePayload(
      final List<WasmValue> payload, final List<WasmValueType> expectedTypes) {
    if (payload == null) {
      throw new IllegalArgumentException("Payload cannot be null");
    }
    if (expectedTypes == null) {
      throw new IllegalArgumentException("Expected types cannot be null");
    }

    if (payload.size() != expectedTypes.size()) {
      throw new MarshalingException(
          "Payload size ("
              + payload.size()
              + ") doesn't match expected types size ("
              + expectedTypes.size()
              + ")");
    }

    for (int i = 0; i < payload.size(); i++) {
      final WasmValue value = payload.get(i);
      final WasmValueType expectedType = expectedTypes.get(i);

      if (!value.getType().equals(expectedType)) {
        throw new MarshalingException(
            "Value at index "
                + i
                + " has type "
                + value.getType()
                + " but expected "
                + expectedType);
      }
    }

    LOGGER.fine("Validated exception payload with " + payload.size() + " values");
  }

  /**
   * Converts a Java exception to a WebAssembly exception payload.
   *
   * @param throwable the Java exception
   * @param messageTag the exception tag for the message
   * @return WebAssembly exception payload
   * @throws IllegalArgumentException if throwable or messageTag is null
   * @throws MarshalingException if conversion fails
   */
  public static List<WasmValue> convertFromJavaException(
      final Throwable throwable, final ExceptionHandler.ExceptionTag messageTag) {
    if (throwable == null) {
      throw new IllegalArgumentException("Throwable cannot be null");
    }
    if (messageTag == null) {
      throw new IllegalArgumentException("Message tag cannot be null");
    }

    final List<Object> objects =
        List.of(
            throwable.getClass().getSimpleName(),
            throwable.getMessage() != null ? throwable.getMessage() : "",
            System.currentTimeMillis());

    try {
      return marshalPayload(objects, messageTag.getParameterTypes());
    } catch (final Exception e) {
      throw new MarshalingException("Failed to convert Java exception to WebAssembly payload", e);
    }
  }

  /**
   * Converts a WebAssembly exception payload to a Java exception.
   *
   * @param payload the WebAssembly exception payload
   * @param tag the exception tag
   * @return Java exception representation
   * @throws IllegalArgumentException if payload or tag is null
   * @throws MarshalingException if conversion fails
   */
  public static Exception convertToJavaException(
      final List<WasmValue> payload, final ExceptionHandler.ExceptionTag tag) {
    if (payload == null) {
      throw new IllegalArgumentException("Payload cannot be null");
    }
    if (tag == null) {
      throw new IllegalArgumentException("Tag cannot be null");
    }

    try {
      final List<Object> objects = unmarshalPayload(payload);

      final String className =
          objects.size() > 0 ? String.valueOf(objects.get(0)) : "WebAssemblyException";
      final String message =
          objects.size() > 1
              ? String.valueOf(objects.get(1))
              : "WebAssembly exception: " + tag.getName();

      return new RuntimeException(className + ": " + message);
    } catch (final Exception e) {
      throw new MarshalingException("Failed to convert WebAssembly payload to Java exception", e);
    }
  }

  /**
   * Marshals a single Java object to a WebAssembly value.
   *
   * @param obj the Java object
   * @param expectedType the expected WebAssembly type
   * @return the WebAssembly value
   * @throws MarshalingException if marshaling fails
   */
  private static WasmValue marshalSingleValue(final Object obj, final WasmValueType expectedType) {
    if (obj == null) {
      throw new MarshalingException("Cannot marshal null object to " + expectedType);
    }

    switch (expectedType) {
      case I32:
        if (obj instanceof Integer) {
          return WasmValue.i32((Integer) obj);
        } else if (obj instanceof Number) {
          return WasmValue.i32(((Number) obj).intValue());
        } else if (obj instanceof Boolean) {
          return WasmValue.i32((Boolean) obj ? 1 : 0);
        }
        break;

      case I64:
        if (obj instanceof Long) {
          return WasmValue.i64((Long) obj);
        } else if (obj instanceof Number) {
          return WasmValue.i64(((Number) obj).longValue());
        }
        break;

      case F32:
        if (obj instanceof Float) {
          return WasmValue.f32((Float) obj);
        } else if (obj instanceof Number) {
          return WasmValue.f32(((Number) obj).floatValue());
        }
        break;

      case F64:
        if (obj instanceof Double) {
          return WasmValue.f64((Double) obj);
        } else if (obj instanceof Number) {
          return WasmValue.f64(((Number) obj).doubleValue());
        }
        break;

      case EXTERNREF:
        return WasmValue.externRef(obj);

      default:
        throw new MarshalingException("Unsupported type for marshaling: " + expectedType);
    }

    throw new MarshalingException(
        "Cannot marshal object of type " + obj.getClass().getSimpleName() + " to " + expectedType);
  }

  /**
   * Unmarshals a WebAssembly value to a Java object.
   *
   * @param value the WebAssembly value
   * @return the Java object
   */
  private static Object unmarshalSingleValue(final WasmValue value) {
    switch (value.getType()) {
      case I32:
        return value.i32();
      case I64:
        return value.i64();
      case F32:
        return value.f32();
      case F64:
        return value.f64();
      case EXTERNREF:
        return value.externRef();
      case FUNCREF:
        return value.funcRef();
      default:
        throw new MarshalingException("Unsupported type for unmarshaling: " + value.getType());
    }
  }

  /**
   * Gets the serialized size of a WebAssembly value.
   *
   * @param value the value
   * @return the size in bytes
   */
  private static int getValueSize(final WasmValue value) {
    switch (value.getType()) {
      case I32:
      case F32:
        return 4;
      case I64:
      case F64:
        return 8;
      case V128:
        return 16;
      case FUNCREF:
      case EXTERNREF:
        return 8; // Handle size
      default:
        throw new MarshalingException("Unsupported type for size calculation: " + value.getType());
    }
  }

  /**
   * Serializes a single WebAssembly value to a byte buffer.
   *
   * @param buffer the byte buffer
   * @param value the value to serialize
   */
  private static void serializeValue(final ByteBuffer buffer, final WasmValue value) {
    // Write type byte
    buffer.put((byte) value.getType().ordinal());

    // Write value data
    switch (value.getType()) {
      case I32:
        buffer.putInt(value.i32());
        break;
      case I64:
        buffer.putLong(value.i64());
        break;
      case F32:
        buffer.putFloat(value.f32());
        break;
      case F64:
        buffer.putDouble(value.f64());
        break;
      case FUNCREF:
      case EXTERNREF:
        // For references, we store a placeholder handle
        buffer.putLong(Objects.hashCode(value));
        break;
      default:
        throw new MarshalingException("Unsupported type for serialization: " + value.getType());
    }
  }

  /**
   * Deserializes a single WebAssembly value from a byte buffer.
   *
   * @param buffer the byte buffer
   * @return the deserialized value
   */
  private static WasmValue deserializeValue(final ByteBuffer buffer) {
    // Read type byte
    final byte typeByte = buffer.get();
    final WasmValueType type = WasmValueType.values()[typeByte];

    // Read value data
    switch (type) {
      case I32:
        return WasmValue.i32(buffer.getInt());
      case I64:
        return WasmValue.i64(buffer.getLong());
      case F32:
        return WasmValue.f32(buffer.getFloat());
      case F64:
        return WasmValue.f64(buffer.getDouble());
      case FUNCREF:
        buffer.getLong(); // Skip handle
        return WasmValue.funcRef(null);
      case EXTERNREF:
        buffer.getLong(); // Skip handle
        return WasmValue.externRef(null);
      default:
        throw new MarshalingException("Unsupported type for deserialization: " + type);
    }
  }
}
