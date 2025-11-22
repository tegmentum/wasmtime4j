/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.exception.WitValueException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Serializes WIT values to binary format for native marshalling.
 *
 * <p>This class provides efficient serialization of WIT values to byte arrays suitable for passing
 * through JNI boundaries to native code. The serialization format is optimized for performance and
 * minimal overhead.
 *
 * <p>Format for primitive values:
 *
 * <ul>
 *   <li>bool → 1 byte (0 or 1)
 *   <li>s32 → 4 bytes (little-endian)
 *   <li>s64 → 8 bytes (little-endian)
 *   <li>float64 → 8 bytes (little-endian IEEE 754)
 *   <li>char → 4 bytes (little-endian Unicode codepoint)
 *   <li>string → 4 bytes length + UTF-8 bytes
 *   <li>record → 4 bytes field count + (discriminator + length + data) for each field
 * </ul>
 *
 * @since 1.0.0
 */
public final class WitValueSerializer {

  private static final int BOOL_SIZE = 1;
  private static final int S32_SIZE = 4;
  private static final int S64_SIZE = 8;
  private static final int FLOAT64_SIZE = 8;
  private static final int CHAR_SIZE = 4;
  private static final int STRING_LENGTH_SIZE = 4;

  /** Private constructor to prevent instantiation. */
  private WitValueSerializer() {}

  /**
   * Serializes a WIT value to binary format.
   *
   * @param value the WIT value to serialize
   * @return byte array containing serialized value
   * @throws WitValueException if value cannot be serialized
   */
  public static byte[] serialize(final WitValue value) throws WitValueException {
    if (value == null) {
      throw new WitValueException(
          "Cannot serialize null value", WitValueException.ErrorCode.NULL_VALUE);
    }

    // Handle composite types first
    if (value instanceof WitRecord) {
      return serializeRecord((WitRecord) value);
    }

    // Handle primitive types
    if (!(value instanceof WitPrimitiveValue)) {
      throw new WitValueException(
          "Unsupported value type for serialization: " + value.getClass().getName(),
          WitValueException.ErrorCode.UNSUPPORTED_OPERATION);
    }

    final WitPrimitiveValue primitive = (WitPrimitiveValue) value;

    if (primitive instanceof WitBool) {
      return serializeBool((WitBool) primitive);
    } else if (primitive instanceof WitS32) {
      return serializeS32((WitS32) primitive);
    } else if (primitive instanceof WitS64) {
      return serializeS64((WitS64) primitive);
    } else if (primitive instanceof WitFloat64) {
      return serializeFloat64((WitFloat64) primitive);
    } else if (primitive instanceof WitChar) {
      return serializeChar((WitChar) primitive);
    } else if (primitive instanceof WitString) {
      return serializeString((WitString) primitive);
    } else {
      throw new WitValueException(
          "Unsupported primitive type: " + primitive.getClass().getName(),
          WitValueException.ErrorCode.UNSUPPORTED_OPERATION);
    }
  }

  /**
   * Serializes a boolean value.
   *
   * @param value the boolean value
   * @return serialized bytes
   */
  private static byte[] serializeBool(final WitBool value) {
    final ByteBuffer buffer = ByteBuffer.allocate(BOOL_SIZE);
    buffer.put((byte) (value.getValue() ? 1 : 0));
    return buffer.array();
  }

  /**
   * Serializes a signed 32-bit integer value.
   *
   * @param value the integer value
   * @return serialized bytes
   */
  private static byte[] serializeS32(final WitS32 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(S32_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes a signed 64-bit integer value.
   *
   * @param value the long value
   * @return serialized bytes
   */
  private static byte[] serializeS64(final WitS64 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(S64_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes a 64-bit floating-point value.
   *
   * @param value the double value
   * @return serialized bytes
   */
  private static byte[] serializeFloat64(final WitFloat64 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(FLOAT64_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putDouble(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes a Unicode character value.
   *
   * @param value the character value
   * @return serialized bytes
   */
  private static byte[] serializeChar(final WitChar value) {
    final ByteBuffer buffer = ByteBuffer.allocate(CHAR_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(value.getCodepoint());
    return buffer.array();
  }

  /**
   * Serializes a string value.
   *
   * @param value the string value
   * @return serialized bytes
   */
  private static byte[] serializeString(final WitString value) {
    final byte[] utf8Bytes = value.getValue().getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(STRING_LENGTH_SIZE + utf8Bytes.length).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(utf8Bytes.length);
    buffer.put(utf8Bytes);
    return buffer.array();
  }

  /**
   * Serializes a record value.
   *
   * @param record the record value
   * @return serialized bytes
   * @throws WitValueException if serialization fails
   */
  private static byte[] serializeRecord(final WitRecord record) throws WitValueException {
    final var fields = record.getFields();

    // Calculate total size: 4 bytes for count + all field data
    int totalSize = 4; // field count
    final java.util.List<byte[]> fieldData = new java.util.ArrayList<>(fields.size());

    for (final java.util.Map.Entry<String, WitValue> entry : fields.entrySet()) {
      final WitValue fieldValue = entry.getValue();
      final int discriminator = getTypeDiscriminator(fieldValue);
      final byte[] data = serialize(fieldValue);

      totalSize += 4; // discriminator
      totalSize += 4; // length
      totalSize += data.length; // field data

      fieldData.add(data);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write field count
    buffer.putInt(fields.size());

    // Write each field
    int fieldIndex = 0;
    for (final java.util.Map.Entry<String, WitValue> entry : fields.entrySet()) {
      final WitValue fieldValue = entry.getValue();
      final byte[] data = fieldData.get(fieldIndex++);

      buffer.putInt(getTypeDiscriminator(fieldValue));
      buffer.putInt(data.length);
      buffer.put(data);
    }

    return buffer.array();
  }

  /**
   * Gets the type discriminator for a WIT value.
   *
   * <p>Type discriminators are used by the native layer to identify the value type:
   *
   * <ul>
   *   <li>1 = bool
   *   <li>2 = s32
   *   <li>3 = s64
   *   <li>4 = float64
   *   <li>5 = char
   *   <li>6 = string
   *   <li>7 = record
   * </ul>
   *
   * @param value the WIT value
   * @return type discriminator
   * @throws WitValueException if value type is not supported
   */
  public static int getTypeDiscriminator(final WitValue value) throws WitValueException {
    if (value == null) {
      throw new WitValueException(
          "Cannot get type discriminator for null value", WitValueException.ErrorCode.NULL_VALUE);
    }

    if (value instanceof WitBool) {
      return 1;
    } else if (value instanceof WitS32) {
      return 2;
    } else if (value instanceof WitS64) {
      return 3;
    } else if (value instanceof WitFloat64) {
      return 4;
    } else if (value instanceof WitChar) {
      return 5;
    } else if (value instanceof WitString) {
      return 6;
    } else if (value instanceof WitRecord) {
      return 7;
    } else {
      throw new WitValueException(
          "Unsupported value type: " + value.getClass().getName(),
          WitValueException.ErrorCode.UNSUPPORTED_OPERATION);
    }
  }
}
