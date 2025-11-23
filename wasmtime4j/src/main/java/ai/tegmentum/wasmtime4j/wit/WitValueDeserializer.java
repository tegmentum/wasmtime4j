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
  private static final int S8_SIZE = 1;
  private static final int S16_SIZE = 2;
  private static final int S32_SIZE = 4;
  private static final int S64_SIZE = 8;
  private static final int U8_SIZE = 1;
  private static final int U16_SIZE = 2;
  private static final int U32_SIZE = 4;
  private static final int U64_SIZE = 8;
  private static final int FLOAT32_SIZE = 4;
  private static final int FLOAT64_SIZE = 8;
  private static final int CHAR_SIZE = 4;
  private static final int STRING_LENGTH_SIZE = 4;

  /** Private constructor to prevent instantiation. */
  private WitValueDeserializer() {}

  /**
   * Deserializes a WIT value from binary format.
   *
   * @param typeDiscriminator the type discriminator (1-16)
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
      case 9:
        return deserializeU32(data);
      case 10:
        return deserializeU64(data);
      case 11:
        return deserializeList(data);
      case 12:
        return deserializeVariant(data);
      case 13:
        return deserializeEnum(data);
      case 14:
        return deserializeOption(data);
      case 15:
        return deserializeResult(data);
      case 16:
        return deserializeFlags(data);
      case 17:
        return deserializeS8(data);
      case 18:
        return deserializeS16(data);
      case 19:
        return deserializeU8(data);
      case 20:
        return deserializeU16(data);
      case 21:
        return deserializeFloat32(data);
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
   * Deserializes a signed 8-bit integer value.
   *
   * @param data the serialized bytes
   * @return the byte value
   * @throws WitValueException if data is invalid
   */
  private static WitS8 deserializeS8(final byte[] data) throws WitValueException {
    if (data.length != S8_SIZE) {
      throw new WitValueException(
          "Invalid s8 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    return WitS8.of(data[0]);
  }

  /**
   * Deserializes a signed 16-bit integer value.
   *
   * @param data the serialized bytes
   * @return the short value
   * @throws WitValueException if data is invalid
   */
  private static WitS16 deserializeS16(final byte[] data) throws WitValueException {
    if (data.length != S16_SIZE) {
      throw new WitValueException(
          "Invalid s16 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitS16.of(buffer.getShort());
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
   * Deserializes an unsigned 8-bit integer value.
   *
   * @param data the serialized bytes
   * @return the unsigned byte value
   * @throws WitValueException if data is invalid
   */
  private static WitU8 deserializeU8(final byte[] data) throws WitValueException {
    if (data.length != U8_SIZE) {
      throw new WitValueException(
          "Invalid u8 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    return WitU8.of(data[0]);
  }

  /**
   * Deserializes an unsigned 16-bit integer value.
   *
   * @param data the serialized bytes
   * @return the unsigned short value
   * @throws WitValueException if data is invalid
   */
  private static WitU16 deserializeU16(final byte[] data) throws WitValueException {
    if (data.length != U16_SIZE) {
      throw new WitValueException(
          "Invalid u16 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitU16.of(buffer.getShort());
  }

  /**
   * Deserializes an unsigned 32-bit integer value.
   *
   * @param data the serialized bytes
   * @return the unsigned integer value
   * @throws WitValueException if data is invalid
   */
  private static WitU32 deserializeU32(final byte[] data) throws WitValueException {
    if (data.length != U32_SIZE) {
      throw new WitValueException(
          "Invalid u32 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitU32.of(buffer.getInt());
  }

  /**
   * Deserializes an unsigned 64-bit integer value.
   *
   * @param data the serialized bytes
   * @return the unsigned long value
   * @throws WitValueException if data is invalid
   */
  private static WitU64 deserializeU64(final byte[] data) throws WitValueException {
    if (data.length != U64_SIZE) {
      throw new WitValueException(
          "Invalid u64 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitU64.of(buffer.getLong());
  }

  /**
   * Deserializes a 32-bit floating-point value.
   *
   * @param data the serialized bytes
   * @return the float value
   * @throws WitValueException if data is invalid
   */
  private static WitFloat32 deserializeFloat32(final byte[] data) throws WitValueException {
    if (data.length != FLOAT32_SIZE) {
      throw new WitValueException(
          "Invalid float32 data size: " + data.length, WitValueException.ErrorCode.INVALID_FORMAT);
    }
    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    return WitFloat32.of(buffer.getFloat());
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

    final java.util.Map<String, WitValue> fields = new java.util.LinkedHashMap<>(fieldCount);

    for (int i = 0; i < fieldCount; i++) {
      if (buffer.remaining() < 4) { // Need at least field name length
        throw new WitValueException(
            "Record data truncated at field " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      // Read field name
      final int nameLength = buffer.getInt();
      if (nameLength < 0) {
        throw new WitValueException(
            "Invalid field name length: " + nameLength, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      if (buffer.remaining() < nameLength) {
        throw new WitValueException(
            "Field name data truncated at field " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final byte[] nameBytes = new byte[nameLength];
      buffer.get(nameBytes);
      final String fieldName = new String(nameBytes, StandardCharsets.UTF_8);

      // Read field value
      if (buffer.remaining() < 8) { // Need discriminator + length
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
      fields.put(fieldName, fieldValue);
    }

    return WitRecord.of(fields);
  }

  /**
   * Deserializes a list value.
   *
   * <p>Format: [count: u32][for each: discriminator: i32, length: u32, data]
   *
   * @param data the serialized bytes
   * @return the list value
   * @throws WitValueException if data is invalid
   */
  private static WitList deserializeList(final byte[] data) throws WitValueException {
    if (data.length < 4) {
      throw new WitValueException(
          "List data too short for element count", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final int elementCount = buffer.getInt();

    if (elementCount < 0) {
      throw new WitValueException(
          "Invalid element count: " + elementCount, WitValueException.ErrorCode.INVALID_FORMAT);
    }

    if (elementCount == 0) {
      throw new WitValueException(
          "Cannot infer list element type from empty list",
          WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final java.util.List<WitValue> elements = new java.util.ArrayList<>(elementCount);

    for (int i = 0; i < elementCount; i++) {
      if (buffer.remaining() < 8) { // Need discriminator + length
        throw new WitValueException(
            "List data truncated at element " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final int discriminator = buffer.getInt();
      final int length = buffer.getInt();

      if (length < 0) {
        throw new WitValueException(
            "Invalid element data length: " + length, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      if (buffer.remaining() < length) {
        throw new WitValueException(
            "Element data truncated at element " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final byte[] elementData = new byte[length];
      buffer.get(elementData);

      final WitValue element = deserialize(discriminator, elementData);
      elements.add(element);
    }

    return WitList.of(elements);
  }

  /**
   * Deserializes a variant value.
   *
   * <p>Format: [name_length: u32][name: UTF-8][has_payload: u8][if yes: discriminator, length,
   * data]
   *
   * <p>Note: This method cannot fully deserialize a variant without its WitType definition. It
   * returns a minimal variant that may need type information for proper validation.
   *
   * @param data the serialized bytes
   * @return the variant value
   * @throws WitValueException if data is invalid
   */
  private static WitVariant deserializeVariant(final byte[] data) throws WitValueException {
    if (data.length < 5) { // name_length + has_payload
      throw new WitValueException(
          "Variant data too short", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

    // Read case name
    final int nameLength = buffer.getInt();
    if (nameLength < 0) {
      throw new WitValueException(
          "Invalid case name length: " + nameLength, WitValueException.ErrorCode.INVALID_FORMAT);
    }

    if (buffer.remaining() < nameLength + 1) {
      throw new WitValueException(
          "Variant case name truncated", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final byte[] nameBytes = new byte[nameLength];
    buffer.get(nameBytes);
    final String caseName = new String(nameBytes, StandardCharsets.UTF_8);

    // Read payload flag
    final byte hasPayload = buffer.get();

    if (hasPayload == 0) {
      // Create a placeholder variant type - full implementation would need actual WitType
      return WitVariant.of(ai.tegmentum.wasmtime4j.WitType.createString(), caseName);
    } else {
      // Read payload
      if (buffer.remaining() < 8) {
        throw new WitValueException(
            "Variant payload truncated", WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final int discriminator = buffer.getInt();
      final int length = buffer.getInt();

      if (length < 0) {
        throw new WitValueException(
            "Invalid payload length: " + length, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      if (buffer.remaining() < length) {
        throw new WitValueException(
            "Variant payload data truncated", WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final byte[] payloadData = new byte[length];
      buffer.get(payloadData);

      final WitValue payload = deserialize(discriminator, payloadData);

      // Create a placeholder variant type - full implementation would need actual WitType
      return WitVariant.of(ai.tegmentum.wasmtime4j.WitType.createString(), caseName, payload);
    }
  }

  /**
   * Deserializes an enum value.
   *
   * <p>Format: [name_length: u32][name: UTF-8]
   *
   * <p>Note: This method cannot fully deserialize an enum without its WitType definition. It
   * returns a minimal enum that may need type information for proper validation.
   *
   * @param data the serialized bytes
   * @return the enum value
   * @throws WitValueException if data is invalid
   */
  private static WitEnum deserializeEnum(final byte[] data) throws WitValueException {
    if (data.length < 4) {
      throw new WitValueException(
          "Enum data too short", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

    final int nameLength = buffer.getInt();
    if (nameLength < 0) {
      throw new WitValueException(
          "Invalid discriminant name length: " + nameLength,
          WitValueException.ErrorCode.INVALID_FORMAT);
    }

    if (buffer.remaining() < nameLength) {
      throw new WitValueException(
          "Enum discriminant name truncated", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final byte[] nameBytes = new byte[nameLength];
    buffer.get(nameBytes);
    final String discriminant = new String(nameBytes, StandardCharsets.UTF_8);

    // Create a placeholder enum type - full implementation would need actual WitType
    return WitEnum.of(ai.tegmentum.wasmtime4j.WitType.createString(), discriminant);
  }

  /**
   * Deserializes an option value.
   *
   * <p>Format: [is_some: u8][if yes: discriminator, length, data]
   *
   * <p>Note: This method cannot fully deserialize an option without its WitType definition. It
   * returns a minimal option that may need type information for proper validation.
   *
   * @param data the serialized bytes
   * @return the option value
   * @throws WitValueException if data is invalid
   */
  private static WitOption deserializeOption(final byte[] data) throws WitValueException {
    if (data.length < 1) {
      throw new WitValueException(
          "Option data too short", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

    final byte isSome = buffer.get();

    if (isSome == 0) {
      // Create a placeholder option type - full implementation would need actual WitType
      return WitOption.none(ai.tegmentum.wasmtime4j.WitType.option(null));
    } else {
      if (buffer.remaining() < 8) {
        throw new WitValueException(
            "Option value truncated", WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final int discriminator = buffer.getInt();
      final int length = buffer.getInt();

      if (length < 0) {
        throw new WitValueException(
            "Invalid value length: " + length, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      if (buffer.remaining() < length) {
        throw new WitValueException(
            "Option value data truncated", WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final byte[] valueData = new byte[length];
      buffer.get(valueData);

      final WitValue value = deserialize(discriminator, valueData);

      // Create a placeholder option type - full implementation would need actual WitType
      return WitOption.some(ai.tegmentum.wasmtime4j.WitType.option(null), value);
    }
  }

  /**
   * Deserializes a result value.
   *
   * <p>Format: [is_ok: u8][has_value: u8][if yes: discriminator, length, data]
   *
   * <p>Note: This method cannot fully deserialize a result without its WitType definition. It
   * returns a minimal result that may need type information for proper validation.
   *
   * @param data the serialized bytes
   * @return the result value
   * @throws WitValueException if data is invalid
   */
  private static WitResult deserializeResult(final byte[] data) throws WitValueException {
    if (data.length < 2) {
      throw new WitValueException(
          "Result data too short", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);

    final byte isOk = buffer.get();
    final byte hasValue = buffer.get();

    // Create a placeholder result type - full implementation would need actual WitType
    final ai.tegmentum.wasmtime4j.WitType resultType =
        ai.tegmentum.wasmtime4j.WitType.result(null, null);

    if (hasValue == 0) {
      return isOk != 0 ? WitResult.ok(resultType) : WitResult.err(resultType);
    } else {
      if (buffer.remaining() < 8) {
        throw new WitValueException(
            "Result value truncated", WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final int discriminator = buffer.getInt();
      final int length = buffer.getInt();

      if (length < 0) {
        throw new WitValueException(
            "Invalid value length: " + length, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      if (buffer.remaining() < length) {
        throw new WitValueException(
            "Result value data truncated", WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final byte[] valueData = new byte[length];
      buffer.get(valueData);

      final WitValue value = deserialize(discriminator, valueData);

      return isOk != 0 ? WitResult.ok(resultType, value) : WitResult.err(resultType, value);
    }
  }

  /**
   * Deserializes a flags value.
   *
   * <p>Format: [count: u32][for each: name_length: u32, name: UTF-8]
   *
   * <p>Note: This method cannot fully deserialize flags without its WitType definition. It returns
   * a minimal flags value that may need type information for proper validation.
   *
   * @param data the serialized bytes
   * @return the flags value
   * @throws WitValueException if data is invalid
   */
  private static WitFlags deserializeFlags(final byte[] data) throws WitValueException {
    if (data.length < 4) {
      throw new WitValueException(
          "Flags data too short for flag count", WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final ByteBuffer buffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN);
    final int flagCount = buffer.getInt();

    if (flagCount < 0) {
      throw new WitValueException(
          "Invalid flag count: " + flagCount, WitValueException.ErrorCode.INVALID_FORMAT);
    }

    final java.util.Set<String> flagNames = new java.util.HashSet<>(flagCount);

    for (int i = 0; i < flagCount; i++) {
      if (buffer.remaining() < 4) {
        throw new WitValueException(
            "Flags data truncated at flag " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final int nameLength = buffer.getInt();
      if (nameLength < 0) {
        throw new WitValueException(
            "Invalid flag name length: " + nameLength, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      if (buffer.remaining() < nameLength) {
        throw new WitValueException(
            "Flag name data truncated at flag " + i, WitValueException.ErrorCode.INVALID_FORMAT);
      }

      final byte[] nameBytes = new byte[nameLength];
      buffer.get(nameBytes);
      final String flagName = new String(nameBytes, StandardCharsets.UTF_8);

      flagNames.add(flagName);
    }

    // Create a placeholder flags type - full implementation would need actual WitType
    return WitFlags.of(
        ai.tegmentum.wasmtime4j.WitType.flags("placeholder", java.util.Arrays.asList()), flagNames);
  }
}
