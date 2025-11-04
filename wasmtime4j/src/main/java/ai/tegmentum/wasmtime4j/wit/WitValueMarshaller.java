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
import java.util.ArrayList;
import java.util.List;

/**
 * High-level marshaller for WIT values.
 *
 * <p>This class provides convenient methods for marshalling WIT values to/from the binary format
 * used for native communication. It serves as the primary interface for converting between
 * type-safe {@link WitValue} instances and the serialized format expected by the native layer.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Marshal a single value
 * WitS32 value = WitS32.of(42);
 * MarshalledValue marshalled = WitValueMarshaller.marshal(value);
 *
 * // Unmarshal back
 * WitValue restored = WitValueMarshaller.unmarshal(
 *     marshalled.getTypeDiscriminator(),
 *     marshalled.getData()
 * );
 *
 * // Marshal multiple values
 * List<WitValue> values = List.of(
 *     WitBool.of(true),
 *     WitS32.of(100),
 *     WitString.of("hello")
 * );
 * List<MarshalledValue> marshalled = WitValueMarshaller.marshalAll(values);
 * }</pre>
 *
 * @since 1.0.0
 */
public final class WitValueMarshaller {

  /** Private constructor to prevent instantiation. */
  private WitValueMarshaller() {}

  /**
   * Marshals a WIT value to binary format.
   *
   * @param value the WIT value to marshal
   * @return marshalled value containing type discriminator and binary data
   * @throws WitValueException if marshalling fails
   */
  public static MarshalledValue marshal(final WitValue value) throws WitValueException {
    if (value == null) {
      throw new WitValueException(
          "Cannot marshal null value", WitValueException.ErrorCode.NULL_VALUE);
    }

    final int typeDiscriminator = WitValueSerializer.getTypeDiscriminator(value);
    final byte[] data = WitValueSerializer.serialize(value);

    return new MarshalledValue(typeDiscriminator, data);
  }

  /**
   * Unmarshals a WIT value from binary format.
   *
   * @param typeDiscriminator the type discriminator (1-6)
   * @param data the serialized byte array
   * @return the unmarshalled WIT value
   * @throws WitValueException if unmarshalling fails
   */
  public static WitValue unmarshal(final int typeDiscriminator, final byte[] data)
      throws WitValueException {
    if (data == null) {
      throw new WitValueException(
          "Cannot unmarshal null data", WitValueException.ErrorCode.NULL_VALUE);
    }

    return WitValueDeserializer.deserialize(typeDiscriminator, data);
  }

  /**
   * Marshals multiple WIT values.
   *
   * @param values the list of WIT values to marshal
   * @return list of marshalled values
   * @throws WitValueException if marshalling any value fails
   */
  public static List<MarshalledValue> marshalAll(final List<WitValue> values)
      throws WitValueException {
    if (values == null) {
      throw new WitValueException(
          "Cannot marshal null value list", WitValueException.ErrorCode.NULL_VALUE);
    }

    final List<MarshalledValue> result = new ArrayList<>(values.size());
    for (final WitValue value : values) {
      result.add(marshal(value));
    }
    return result;
  }

  /**
   * Unmarshals multiple WIT values.
   *
   * @param marshalledValues the list of marshalled values
   * @return list of unmarshalled WIT values
   * @throws WitValueException if unmarshalling any value fails
   */
  public static List<WitValue> unmarshalAll(final List<MarshalledValue> marshalledValues)
      throws WitValueException {
    if (marshalledValues == null) {
      throw new WitValueException(
          "Cannot unmarshal null marshalled value list", WitValueException.ErrorCode.NULL_VALUE);
    }

    final List<WitValue> result = new ArrayList<>(marshalledValues.size());
    for (final MarshalledValue marshalled : marshalledValues) {
      result.add(unmarshal(marshalled.typeDiscriminator, marshalled.data));
    }
    return result;
  }

  /**
   * Converts a WitValue to its Java representation.
   *
   * <p>This is a convenience method that delegates to {@link WitValue#toJava()}.
   *
   * @param value the WIT value
   * @return the Java representation
   */
  public static Object toJava(final WitValue value) {
    if (value == null) {
      return null;
    }
    return value.toJava();
  }

  /**
   * Creates a WIT value from a Java object and WIT type.
   *
   * <p>This method provides a convenient way to create typed WIT values from raw Java objects.
   *
   * @param javaValue the Java value
   * @param witType the target WIT type
   * @return the created WIT value
   * @throws WitValueException if the conversion fails
   */
  public static WitValue fromJava(final Object javaValue, final String witType)
      throws WitValueException {
    if (javaValue == null) {
      throw new WitValueException(
          "Cannot create WIT value from null", WitValueException.ErrorCode.NULL_VALUE);
    }

    switch (witType.toLowerCase()) {
      case "bool":
        if (!(javaValue instanceof Boolean)) {
          throw new WitValueException(
              "Expected Boolean for bool type, got " + javaValue.getClass(),
              WitValueException.ErrorCode.TYPE_MISMATCH);
        }
        return WitBool.of((Boolean) javaValue);

      case "s32":
        if (!(javaValue instanceof Integer)) {
          throw new WitValueException(
              "Expected Integer for s32 type, got " + javaValue.getClass(),
              WitValueException.ErrorCode.TYPE_MISMATCH);
        }
        return WitS32.of((Integer) javaValue);

      case "s64":
        if (!(javaValue instanceof Long)) {
          throw new WitValueException(
              "Expected Long for s64 type, got " + javaValue.getClass(),
              WitValueException.ErrorCode.TYPE_MISMATCH);
        }
        return WitS64.of((Long) javaValue);

      case "float64":
        if (!(javaValue instanceof Double)) {
          throw new WitValueException(
              "Expected Double for float64 type, got " + javaValue.getClass(),
              WitValueException.ErrorCode.TYPE_MISMATCH);
        }
        return WitFloat64.of((Double) javaValue);

      case "char":
        if (javaValue instanceof Integer) {
          return WitChar.of((Integer) javaValue);
        } else if (javaValue instanceof Character) {
          return WitChar.of((int) (Character) javaValue);
        } else {
          throw new WitValueException(
              "Expected Integer or Character for char type, got " + javaValue.getClass(),
              WitValueException.ErrorCode.TYPE_MISMATCH);
        }

      case "string":
        if (!(javaValue instanceof String)) {
          throw new WitValueException(
              "Expected String for string type, got " + javaValue.getClass(),
              WitValueException.ErrorCode.TYPE_MISMATCH);
        }
        return WitString.of((String) javaValue);

      default:
        throw new WitValueException(
            "Unsupported WIT type: " + witType, WitValueException.ErrorCode.UNSUPPORTED_OPERATION);
    }
  }

  /**
   * Container for marshalled value data.
   *
   * <p>This class holds the type discriminator and binary data for a marshalled WIT value.
   */
  public static final class MarshalledValue {
    private final int typeDiscriminator;
    private final byte[] data;

    /**
     * Creates a new marshalled value.
     *
     * @param typeDiscriminator the type discriminator (1-6)
     * @param data the serialized byte array (must not be null)
     */
    public MarshalledValue(final int typeDiscriminator, final byte[] data) {
      if (data == null) {
        throw new IllegalArgumentException("Data cannot be null");
      }
      this.typeDiscriminator = typeDiscriminator;
      this.data = data.clone(); // Defensive copy
    }

    /**
     * Gets the type discriminator.
     *
     * @return the type discriminator
     */
    public int getTypeDiscriminator() {
      return typeDiscriminator;
    }

    /**
     * Gets the serialized data.
     *
     * @return a copy of the serialized byte array
     */
    public byte[] getData() {
      return data.clone(); // Defensive copy
    }

    /**
     * Gets the size of the serialized data in bytes.
     *
     * @return the data size
     */
    public int getDataSize() {
      return data.length;
    }

    @Override
    public String toString() {
      return String.format(
          "MarshalledValue{discriminator=%d, size=%d}", typeDiscriminator, data.length);
    }
  }
}
