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
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

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
   * @throws ValidationException if marshalling fails
   */
  public static MarshalledValue marshal(final WitValue value) throws ValidationException {
    if (value == null) {
      throw new ValidationException("Cannot marshal null value");
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
   * @throws ValidationException if unmarshalling fails
   */
  public static WitValue unmarshal(final int typeDiscriminator, final byte[] data)
      throws ValidationException {
    if (data == null) {
      throw new ValidationException("Cannot unmarshal null data");
    }

    return WitValueDeserializer.deserialize(typeDiscriminator, data);
  }

  /**
   * Marshals multiple WIT values.
   *
   * @param values the list of WIT values to marshal
   * @return list of marshalled values
   * @throws ValidationException if marshalling any value fails
   */
  public static List<MarshalledValue> marshalAll(final List<WitValue> values)
      throws ValidationException {
    if (values == null) {
      throw new ValidationException("Cannot marshal null value list");
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
   * @throws ValidationException if unmarshalling any value fails
   */
  public static List<WitValue> unmarshalAll(final List<MarshalledValue> marshalledValues)
      throws ValidationException {
    if (marshalledValues == null) {
      throw new ValidationException("Cannot unmarshal null marshalled value list");
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
   * @throws ValidationException if the conversion fails
   */
  @SuppressFBWarnings(
      value = "IMPROPER_UNICODE",
      justification = "WIT types are ASCII-only specification identifiers")
  public static WitValue fromJava(final Object javaValue, final String witType)
      throws ValidationException {
    if (javaValue == null) {
      throw new ValidationException("Cannot create WIT value from null");
    }

    switch (witType.toLowerCase(Locale.ROOT)) {
      case "bool":
        if (!(javaValue instanceof Boolean)) {
          throw new ValidationException(
              "Expected Boolean for bool type, got " + javaValue.getClass());
        }
        return WitBool.of((Boolean) javaValue);

      case "s32":
        if (!(javaValue instanceof Integer)) {
          throw new ValidationException(
              "Expected Integer for s32 type, got " + javaValue.getClass());
        }
        return WitS32.of((Integer) javaValue);

      case "s64":
        if (!(javaValue instanceof Long)) {
          throw new ValidationException("Expected Long for s64 type, got " + javaValue.getClass());
        }
        return WitS64.of((Long) javaValue);

      case "float64":
        if (!(javaValue instanceof Double)) {
          throw new ValidationException(
              "Expected Double for float64 type, got " + javaValue.getClass());
        }
        return WitFloat64.of((Double) javaValue);

      case "char":
        if (javaValue instanceof Integer) {
          return WitChar.of((Integer) javaValue);
        } else if (javaValue instanceof Character) {
          return WitChar.of((int) (Character) javaValue);
        } else {
          throw new ValidationException(
              "Expected Integer or Character for char type, got " + javaValue.getClass());
        }

      case "string":
        if (!(javaValue instanceof String)) {
          throw new ValidationException(
              "Expected String for string type, got " + javaValue.getClass());
        }
        return WitString.of((String) javaValue);

      case "s8":
        if (!(javaValue instanceof Number)) {
          throw new ValidationException("Expected Number for s8 type, got " + javaValue.getClass());
        }
        return WitS8.of(((Number) javaValue).byteValue());

      case "s16":
        if (!(javaValue instanceof Number)) {
          throw new ValidationException(
              "Expected Number for s16 type, got " + javaValue.getClass());
        }
        return WitS16.of(((Number) javaValue).shortValue());

      case "u8":
        if (!(javaValue instanceof Number)) {
          throw new ValidationException("Expected Number for u8 type, got " + javaValue.getClass());
        }
        return WitU8.ofUnsigned(((Number) javaValue).intValue());

      case "u16":
        if (!(javaValue instanceof Number)) {
          throw new ValidationException(
              "Expected Number for u16 type, got " + javaValue.getClass());
        }
        return WitU16.ofUnsigned(((Number) javaValue).intValue());

      case "u32":
        if (!(javaValue instanceof Number)) {
          throw new ValidationException(
              "Expected Number for u32 type, got " + javaValue.getClass());
        }
        return WitU32.ofUnsigned(((Number) javaValue).longValue());

      case "u64":
        if (!(javaValue instanceof Number)) {
          throw new ValidationException(
              "Expected Number for u64 type, got " + javaValue.getClass());
        }
        return WitU64.of(((Number) javaValue).longValue());

      case "float32":
        if (!(javaValue instanceof Number)) {
          throw new ValidationException(
              "Expected Number for float32 type, got " + javaValue.getClass());
        }
        return WitFloat32.of(((Number) javaValue).floatValue());

      default:
        throw new ValidationException("Unsupported WIT type: " + witType);
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
