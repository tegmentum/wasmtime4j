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

import ai.tegmentum.wasmtime4j.exception.WitMarshallingException;
import ai.tegmentum.wasmtime4j.exception.WitRangeException;
import ai.tegmentum.wasmtime4j.exception.WitValueException;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;

/**
 * Deserializes WIT values from binary format used for native marshalling.
 *
 * <p>This class provides deserialization of WIT values from byte arrays received through JNI
 * boundaries from native code. The format must match {@link WitValueSerializer} exactly.
 *
 * @since 1.0.0
 */
public final class WitValueDeserializer {

  private static final int BOOL_SIZE = 1;
  private static final int S32_SIZE = 4;
  private static final int S64_SIZE = 8;
  private static final int FLOAT64_SIZE = 8;
  private static final int CHAR_SIZE = 4;
  private static final int STRING_LENGTH_SIZE = 4;

  /** Private constructor to prevent instantiation. */
  private WitValueDeserializer() {}

  /**
   * Deserializes a WIT value from binary format.
   *
   * @param typeDiscriminator the type discriminator (1-7)
   * @param data the serialized byte array
   * @return the deserialized WIT value
   * @throws WitValueException if deserialization fails
   */
  public static WitValue deserialize(final int typeDiscriminator, final byte[] data)
      throws WitValueException {
    if (data == null) {
      throw new WitValueException(
          "Cannot deserialize null data", WitValueException.ErrorCode.NULL_VALUE);
    }

    switch (typeDiscriminator) {
      case 1:
        return deserializeBool(data);
      case 2:
        return deserializeS32(data);
      case 3:
        return deserializeS64(data);
      case 4:
        return deserializeFloat64(data);
      case 5:
        return deserializeChar(data);
      case 6:
        return deserializeString(data);
      case 7:
        return deserializeRecord(data);
      default:
        throw new WitValueException(
            "Invalid type discriminator: " + typeDiscriminator,
            WitValueException.ErrorCode.INVALID_FORMAT);
    }
  }

  /**
   * Deserializes a boolean value.
   *
   * @param data the serialized bytes
   * @return the boolean value
   * @throws WitValueException if data is invalid
   */
  private static WitBool deserializeBool(final byte[] data) throws WitValueException {
    if (data.length != BOOL_SIZE) {
      throw new WitValueException(
          "Invalid bool data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    return WitBool.of(data[0] != 0);
  }

  /**
   * Deserializes a signed 32-bit integer value.
   *
   * @param data the serialized bytes
   * @return the integer value
   * @throws WitValueException if data is invalid
   */
  private static WitS32 deserializeS32(final byte[] data) throws WitValueException {
    if (data.length != S32_SIZE) {
      throw new WitValueException(
          "Invalid s32 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitS32.of(buffer.getInt());
  }

  /**
   * Deserializes a signed 64-bit integer value.
   *
   * @param data the serialized bytes
   * @return the long value
   * @throws WitValueException if data is invalid
   */
  private static WitS64 deserializeS64(final byte[] data) throws WitValueException {
    if (data.length != S64_SIZE) {
      throw new WitValueException(
          "Invalid s64 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitS64.of(buffer.getLong());
  }

  /**
   * Deserializes a 64-bit floating-point value.
   *
   * @param data the serialized bytes
   * @return the double value
   * @throws WitValueException if data is invalid
   */
  private static WitFloat64 deserializeFloat64(final byte[] data) throws WitValueException {
    if (data.length != FLOAT64_SIZE) {
      throw new WitValueException(
          "Invalid float64 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitFloat64.of(buffer.getDouble());
  }

  /**
   * Deserializes a Unicode character value.
   *
   * @param data the serialized bytes
   * @return the character value
   * @throws WitValueException if data is invalid
   */
  private static WitChar deserializeChar(final byte[] data) throws WitValueException {
    if (data.length != CHAR_SIZE) {
      throw new WitValueException(
          "Invalid char data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final int codepoint = buffer.getInt();
    try {
      return WitChar.of(codepoint);
    } catch (final WitRangeException e) {
      throw new WitValueException(
          "Invalid codepoint: " + codepoint, WitValueException.ErrorCode.RANGE_ERROR, e);
    }
  }

  /**
   * Deserializes a string value.
   *
   * @param data the serialized bytes
   * @return the string value
   * @throws WitValueException if data is invalid
   */
  private static WitString deserializeString(final byte[] data) throws WitValueException {
    if (data.length < STRING_LENGTH_SIZE) {
      throw new WitValueException(
          "Invalid string data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final int length = buffer.getInt();

    if (length < 0) {
      throw new WitValueException(
          "Invalid string length: " + length, WitValueException.ErrorCode.INVALID_FORMAT);
    }

    if (data.length != STRING_LENGTH_SIZE + length) {
      throw new WitValueException(
          String.format(
              "String data size mismatch: expected %d, got %d",
              STRING_LENGTH_SIZE + length, data.length),
          WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final byte[] utf8Bytes = new byte[length];
    buffer.get(utf8Bytes);

    final String value = new String(utf8Bytes, StandardCharsets.UTF_8);
    try {
      return WitString.of(value);
    } catch (final WitMarshallingException e) {
      throw new WitValueException(
          "Failed to create WitString", WitValueException.ErrorCode.MARSHALLING_ERROR, e);
    }
  }

  /**
   * Deserializes a record value.
   *
   * @param data the serialized bytes
   * @return the record value
   * @throws WitValueException if data is invalid
   */
  private static WitRecord deserializeRecord(final byte[] data) throws WitValueException {
    if (data.length < 4) {
      throw new WitValueException(
          "Record data too short for field count", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final int fieldCount = buffer.getInt();

    if (fieldCount < 0) {
      throw new WitValueException(
          "Invalid field count: " + fieldCount, WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final java.util.Map<String, WitValue> fields =
        new java.util.LinkedHashMap<>(fieldCount);

    for (int i = 0; i < fieldCount; i++) {
      if (buffer.remaining() < 8) { // Need at least discriminator + length
        throw new WitValueException(
            "Record data truncated at field " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final int discriminator = buffer.getInt();
      final int length = buffer.getInt();

      if (length < 0) {
        throw new WitValueException(
            "Invalid field data length: " + length, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      if (buffer.remaining() < length) {
        throw new WitValueException(
            "Field data truncated at field " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final byte[] fieldData = new byte[length];
      buffer.get(fieldData);

      final WitValue fieldValue = deserialize(discriminator, fieldData);
      // Field names are not preserved in serialization, use index as name
      fields.put("field" + i, fieldValue);
    }

    return WitRecord.of(fields);
  }
}
