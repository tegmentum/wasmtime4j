/*
 * Copyright 2025 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.exception.ValidationException;
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
 *   <li>record → 4 bytes field count + (name length + name UTF-8 + discriminator + length + data)
 *       for each field
 * </ul>
 *
 * @since 1.0.0
 */
public final class WitValueSerializer {

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
  private WitValueSerializer() {}

  /**
   * Serializes a WIT value to binary format.
   *
   * @param value the WIT value to serialize
   * @return byte array containing serialized value
   * @throws ValidationException if value cannot be serialized
   */
  public static byte[] serialize(final WitValue value) throws ValidationException {
    if (value == null) {
      throw new ValidationException("Cannot serialize null value");
    }

    // Handle composite types first
    if (value instanceof WitRecord) {
      return serializeRecord((WitRecord) value);
    } else if (value instanceof WitList) {
      return serializeList((WitList) value);
    } else if (value instanceof WitVariant) {
      return serializeVariant((WitVariant) value);
    } else if (value instanceof WitEnum) {
      return serializeEnum((WitEnum) value);
    } else if (value instanceof WitOption) {
      return serializeOption((WitOption) value);
    } else if (value instanceof WitResult) {
      return serializeResult((WitResult) value);
    } else if (value instanceof WitTuple) {
      return serializeTuple((WitTuple) value);
    } else if (value instanceof WitFlags) {
      return serializeFlags((WitFlags) value);
    } else if (value instanceof WitOwn) {
      return serializeOwn((WitOwn) value);
    } else if (value instanceof WitBorrow) {
      return serializeBorrow((WitBorrow) value);
    }

    // Handle primitive types
    if (!(value instanceof WitPrimitiveValue)) {
      throw new ValidationException(
          "Unsupported value type for serialization: " + value.getClass().getName());
    }

    final WitPrimitiveValue primitive = (WitPrimitiveValue) value;

    if (primitive instanceof WitBool) {
      return serializeBool((WitBool) primitive);
    } else if (primitive instanceof WitS8) {
      return serializeS8((WitS8) primitive);
    } else if (primitive instanceof WitS16) {
      return serializeS16((WitS16) primitive);
    } else if (primitive instanceof WitS32) {
      return serializeS32((WitS32) primitive);
    } else if (primitive instanceof WitS64) {
      return serializeS64((WitS64) primitive);
    } else if (primitive instanceof WitU8) {
      return serializeU8((WitU8) primitive);
    } else if (primitive instanceof WitU16) {
      return serializeU16((WitU16) primitive);
    } else if (primitive instanceof WitU32) {
      return serializeU32((WitU32) primitive);
    } else if (primitive instanceof WitU64) {
      return serializeU64((WitU64) primitive);
    } else if (primitive instanceof WitFloat32) {
      return serializeFloat32((WitFloat32) primitive);
    } else if (primitive instanceof WitFloat64) {
      return serializeFloat64((WitFloat64) primitive);
    } else if (primitive instanceof WitChar) {
      return serializeChar((WitChar) primitive);
    } else if (primitive instanceof WitString) {
      return serializeString((WitString) primitive);
    } else {
      throw new ValidationException(
          "Unsupported primitive type: " + primitive.getClass().getName());
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
   * Serializes a signed 8-bit integer value.
   *
   * @param value the byte value
   * @return serialized bytes
   */
  private static byte[] serializeS8(final WitS8 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(S8_SIZE);
    buffer.put(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes a signed 16-bit integer value.
   *
   * @param value the short value
   * @return serialized bytes
   */
  private static byte[] serializeS16(final WitS16 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(S16_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort(value.getValue());
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
   * Serializes an unsigned 8-bit integer value.
   *
   * @param value the u8 value
   * @return serialized bytes
   */
  private static byte[] serializeU8(final WitU8 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(U8_SIZE);
    buffer.put(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes an unsigned 16-bit integer value.
   *
   * @param value the u16 value
   * @return serialized bytes
   */
  private static byte[] serializeU16(final WitU16 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(U16_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putShort(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes an unsigned 32-bit integer value.
   *
   * @param value the u32 value
   * @return serialized bytes
   */
  private static byte[] serializeU32(final WitU32 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(U32_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes an unsigned 64-bit integer value.
   *
   * @param value the u64 value
   * @return serialized bytes
   */
  private static byte[] serializeU64(final WitU64 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(U64_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putLong(value.getValue());
    return buffer.array();
  }

  /**
   * Serializes a 32-bit floating-point value.
   *
   * @param value the float value
   * @return serialized bytes
   */
  private static byte[] serializeFloat32(final WitFloat32 value) {
    final ByteBuffer buffer = ByteBuffer.allocate(FLOAT32_SIZE).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putFloat(value.getValue());
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
   * @throws ValidationException if serialization fails
   */
  private static byte[] serializeRecord(final WitRecord record) throws ValidationException {
    final var fields = record.getFields();

    // Calculate total size: 4 bytes for count + all field data
    int totalSize = 4; // field count
    final java.util.List<byte[]> fieldData = new java.util.ArrayList<>(fields.size());
    final java.util.List<byte[]> fieldNames = new java.util.ArrayList<>(fields.size());

    for (final java.util.Map.Entry<String, WitValue> entry : fields.entrySet()) {
      final String fieldName = entry.getKey();
      final WitValue fieldValue = entry.getValue();
      final byte[] data = serialize(fieldValue);
      final byte[] nameBytes = fieldName.getBytes(StandardCharsets.UTF_8);

      totalSize += 4; // name length
      totalSize += nameBytes.length; // name UTF-8 bytes
      totalSize += 4; // discriminator
      totalSize += 4; // length
      totalSize += data.length; // field data

      fieldNames.add(nameBytes);
      fieldData.add(data);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write field count
    buffer.putInt(fields.size());

    // Write each field
    int fieldIndex = 0;
    for (final java.util.Map.Entry<String, WitValue> entry : fields.entrySet()) {
      final WitValue fieldValue = entry.getValue();
      final byte[] nameBytes = fieldNames.get(fieldIndex);
      final byte[] data = fieldData.get(fieldIndex);
      fieldIndex++;

      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
      buffer.putInt(getTypeDiscriminator(fieldValue));
      buffer.putInt(data.length);
      buffer.put(data);
    }

    return buffer.array();
  }

  /**
   * Serializes a list value.
   *
   * <p>Format: [count: u32][for each: discriminator: i32, length: u32, data]
   *
   * @param list the list value
   * @return serialized bytes
   * @throws ValidationException if serialization fails
   */
  private static byte[] serializeList(final WitList list) throws ValidationException {
    final java.util.List<WitValue> elements = list.getElements();

    // Calculate total size
    int totalSize = 4; // element count
    final java.util.List<byte[]> elementData = new java.util.ArrayList<>(elements.size());

    for (final WitValue element : elements) {
      final byte[] data = serialize(element);
      totalSize += 4; // discriminator
      totalSize += 4; // length
      totalSize += data.length; // element data
      elementData.add(data);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write element count
    buffer.putInt(elements.size());

    // Write each element
    for (int i = 0; i < elements.size(); i++) {
      final WitValue element = elements.get(i);
      final byte[] data = elementData.get(i);

      buffer.putInt(getTypeDiscriminator(element));
      buffer.putInt(data.length);
      buffer.put(data);
    }

    return buffer.array();
  }

  /**
   * Serializes a variant value.
   *
   * <p>Format: [name_length: u32][name: UTF-8][has_payload: u8][if yes: discriminator, length,
   * data]
   *
   * @param variant the variant value
   * @return serialized bytes
   * @throws ValidationException if serialization fails
   */
  private static byte[] serializeVariant(final WitVariant variant) throws ValidationException {
    final byte[] nameBytes = variant.getCaseName().getBytes(StandardCharsets.UTF_8);
    final java.util.Optional<WitValue> payload = variant.getPayload();

    int totalSize = 4 + nameBytes.length + 1; // name length + name + has_payload flag

    byte[] payloadData = null;
    int payloadDiscriminator = 0;

    if (payload.isPresent()) {
      payloadData = serialize(payload.get());
      payloadDiscriminator = getTypeDiscriminator(payload.get());
      totalSize += 4 + 4 + payloadData.length; // discriminator + length + data
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write case name
    buffer.putInt(nameBytes.length);
    buffer.put(nameBytes);

    // Write payload flag and data
    if (payload.isPresent()) {
      buffer.put((byte) 1);
      buffer.putInt(payloadDiscriminator);
      buffer.putInt(payloadData.length);
      buffer.put(payloadData);
    } else {
      buffer.put((byte) 0);
    }

    return buffer.array();
  }

  /**
   * Serializes an enum value.
   *
   * <p>Format: [name_length: u32][name: UTF-8]
   *
   * @param enumValue the enum value
   * @return serialized bytes
   */
  private static byte[] serializeEnum(final WitEnum enumValue) {
    final byte[] nameBytes = enumValue.getDiscriminant().getBytes(StandardCharsets.UTF_8);

    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + nameBytes.length).order(ByteOrder.LITTLE_ENDIAN);

    buffer.putInt(nameBytes.length);
    buffer.put(nameBytes);

    return buffer.array();
  }

  /**
   * Serializes an option value.
   *
   * <p>Format: [is_some: u8][if yes: discriminator, length, data]
   *
   * @param option the option value
   * @return serialized bytes
   * @throws ValidationException if serialization fails
   */
  private static byte[] serializeOption(final WitOption option) throws ValidationException {
    final java.util.Optional<WitValue> value = option.getValue();

    int totalSize = 1; // is_some flag

    byte[] valueData = null;
    int valueDiscriminator = 0;

    if (value.isPresent()) {
      valueData = serialize(value.get());
      valueDiscriminator = getTypeDiscriminator(value.get());
      totalSize += 4 + 4 + valueData.length; // discriminator + length + data
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write is_some flag and data
    if (value.isPresent()) {
      buffer.put((byte) 1);
      buffer.putInt(valueDiscriminator);
      buffer.putInt(valueData.length);
      buffer.put(valueData);
    } else {
      buffer.put((byte) 0);
    }

    return buffer.array();
  }

  /**
   * Serializes a result value.
   *
   * <p>Format: [is_ok: u8][has_value: u8][if yes: discriminator, length, data]
   *
   * @param result the result value
   * @return serialized bytes
   * @throws ValidationException if serialization fails
   */
  private static byte[] serializeResult(final WitResult result) throws ValidationException {
    final boolean isOk = result.isOk();
    final java.util.Optional<WitValue> value = result.getValue();

    int totalSize = 2; // is_ok flag + has_value flag

    byte[] valueData = null;
    int valueDiscriminator = 0;

    if (value.isPresent()) {
      valueData = serialize(value.get());
      valueDiscriminator = getTypeDiscriminator(value.get());
      totalSize += 4 + 4 + valueData.length; // discriminator + length + data
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write is_ok flag
    buffer.put((byte) (isOk ? 1 : 0));

    // Write has_value flag and data
    if (value.isPresent()) {
      buffer.put((byte) 1);
      buffer.putInt(valueDiscriminator);
      buffer.putInt(valueData.length);
      buffer.put(valueData);
    } else {
      buffer.put((byte) 0);
    }

    return buffer.array();
  }

  /**
   * Serializes a flags value.
   *
   * <p>Format: [count: u32][for each: name_length: u32, name: UTF-8]
   *
   * @param flags the flags value
   * @return serialized bytes
   */
  private static byte[] serializeFlags(final WitFlags flags) {
    final java.util.Set<String> setFlags = flags.getSetFlags();

    // Calculate total size
    int totalSize = 4; // flag count
    final java.util.List<byte[]> flagNames = new java.util.ArrayList<>(setFlags.size());

    for (final String flagName : setFlags) {
      final byte[] nameBytes = flagName.getBytes(StandardCharsets.UTF_8);
      totalSize += 4 + nameBytes.length; // name length + name
      flagNames.add(nameBytes);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write flag count
    buffer.putInt(setFlags.size());

    // Write each flag name
    for (final byte[] nameBytes : flagNames) {
      buffer.putInt(nameBytes.length);
      buffer.put(nameBytes);
    }

    return buffer.array();
  }

  /**
   * Serializes a tuple value.
   *
   * <p>Format: [count: u32][for each element: discriminator: i32, length: u32, data]
   *
   * @param tuple the tuple value
   * @return serialized bytes
   * @throws ValidationException if serialization fails
   */
  private static byte[] serializeTuple(final WitTuple tuple) throws ValidationException {
    final java.util.List<WitValue> elements = tuple.getElements();

    // Calculate total size
    int totalSize = 4; // element count
    final java.util.List<byte[]> elementData = new java.util.ArrayList<>(elements.size());

    for (final WitValue element : elements) {
      final byte[] data = serialize(element);
      totalSize += 4; // discriminator
      totalSize += 4; // length
      totalSize += data.length; // element data
      elementData.add(data);
    }

    final ByteBuffer buffer = ByteBuffer.allocate(totalSize).order(ByteOrder.LITTLE_ENDIAN);

    // Write element count
    buffer.putInt(elements.size());

    // Write each element
    for (int i = 0; i < elements.size(); i++) {
      final WitValue element = elements.get(i);
      final byte[] data = elementData.get(i);

      buffer.putInt(getTypeDiscriminator(element));
      buffer.putInt(data.length);
      buffer.put(data);
    }

    return buffer.array();
  }

  /**
   * Serializes an owned resource handle value.
   *
   * <p>Format: [type_name_length: i32][type_name: UTF-8][handle: i64] (little-endian)
   *
   * @param own the owned resource handle value
   * @return serialized bytes
   */
  private static byte[] serializeOwn(final WitOwn own) {
    final byte[] typeNameBytes = own.getResourceType().getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    final long nativeHandle = own.getHandle().getNativeHandle();
    buffer.putLong(nativeHandle >= 0 ? nativeHandle : Integer.toUnsignedLong(own.getIndex()));
    return buffer.array();
  }

  /**
   * Serializes a borrowed resource handle value.
   *
   * <p>Format: [type_name_length: i32][type_name: UTF-8][handle: i64] (little-endian)
   *
   * @param borrow the borrowed resource handle value
   * @return serialized bytes
   */
  private static byte[] serializeBorrow(final WitBorrow borrow) {
    final byte[] typeNameBytes = borrow.getResourceType().getBytes(StandardCharsets.UTF_8);
    final ByteBuffer buffer =
        ByteBuffer.allocate(4 + typeNameBytes.length + 8).order(ByteOrder.LITTLE_ENDIAN);
    buffer.putInt(typeNameBytes.length);
    buffer.put(typeNameBytes);
    final long nativeHandle = borrow.getHandle().getNativeHandle();
    buffer.putLong(nativeHandle >= 0 ? nativeHandle : Integer.toUnsignedLong(borrow.getIndex()));
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
   *   <li>8 = tuple
   *   <li>9 = u32
   *   <li>10 = u64
   *   <li>11 = list
   *   <li>12 = variant
   *   <li>13 = enum
   *   <li>14 = option
   *   <li>15 = result
   *   <li>16 = flags
   *   <li>17 = s8
   *   <li>18 = s16
   *   <li>19 = u8
   *   <li>20 = u16
   *   <li>21 = float32
   *   <li>22 = own (owned resource handle)
   *   <li>23 = borrow (borrowed resource handle)
   * </ul>
   *
   * @param value the WIT value
   * @return type discriminator
   * @throws ValidationException if value type is not supported
   */
  public static int getTypeDiscriminator(final WitValue value) throws ValidationException {
    if (value == null) {
      throw new ValidationException("Cannot get type discriminator for null value");
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
    } else if (value instanceof WitTuple) {
      return 8;
    } else if (value instanceof WitU32) {
      return 9;
    } else if (value instanceof WitU64) {
      return 10;
    } else if (value instanceof WitList) {
      return 11;
    } else if (value instanceof WitVariant) {
      return 12;
    } else if (value instanceof WitEnum) {
      return 13;
    } else if (value instanceof WitOption) {
      return 14;
    } else if (value instanceof WitResult) {
      return 15;
    } else if (value instanceof WitFlags) {
      return 16;
    } else if (value instanceof WitS8) {
      return 17;
    } else if (value instanceof WitS16) {
      return 18;
    } else if (value instanceof WitU8) {
      return 19;
    } else if (value instanceof WitU16) {
      return 20;
    } else if (value instanceof WitFloat32) {
      return 21;
    } else if (value instanceof WitOwn) {
      return 22;
    } else if (value instanceof WitBorrow) {
      return 23;
    } else {
      throw new ValidationException("Unsupported value type: " + value.getClass().getName());
    }
  }
}
