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

package ai.tegmentum.wasmtime4j.panama.wit;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.panama.NativeComponentBindings;
import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue;
import ai.tegmentum.wasmtime4j.wit.WitValueSerializer;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;

/**
 * Panama implementation of WIT value marshaller.
 *
 * <p>This class provides bidirectional marshalling between Java WIT values and the binary format
 * used by native Wasmtime component runtime. It delegates to native Rust functions for actual
 * marshalling work to ensure consistency with Wasmtime's expectations.
 *
 * <p>The marshalling process:
 *
 * <ol>
 *   <li>Java WIT values are serialized using {@link WitValueSerializer}
 *   <li>Serialized bytes are passed to native marshalling functions
 *   <li>Native code validates and potentially transforms the data
 *   <li>Result is returned as marshalled bytes ready for component calls
 * </ol>
 *
 * @since 1.0.0
 */
public final class PanamaWitValueMarshaller {

  private static final NativeComponentBindings NATIVE_BINDINGS =
      NativeComponentBindings.getInstance();

  /** Private constructor to prevent instantiation. */
  private PanamaWitValueMarshaller() {}

  /**
   * Marshals a WIT value to binary format for native use.
   *
   * @param value the WIT value to marshal
   * @param arena the memory arena for allocation
   * @return marshalled value ready for native calls
   * @throws ValidationException if marshalling fails
   */
  public static MarshalledValue marshal(final WitValue value, final Arena arena)
      throws ValidationException {
    if (value == null) {
      throw new ValidationException("Cannot marshal null value");
    }
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }

    // Get type discriminator and serialize value
    final int typeDiscriminator = WitValueSerializer.getTypeDiscriminator(value);
    final byte[] data = WitValueSerializer.serialize(value);

    // Allocate native memory for input and output
    final MemorySegment valueSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, data);
    final MemorySegment outDataPtr = arena.allocate(ValueLayout.ADDRESS);
    final MemorySegment outLenPtr = arena.allocate(ValueLayout.JAVA_LONG);

    // Call native marshalling function
    final int errorCode =
        NATIVE_BINDINGS.witValueSerialize(
            typeDiscriminator, valueSegment, data.length, outDataPtr, outLenPtr);

    if (errorCode != 0) {
      throw new ValidationException(
          "Native marshalling failed for type discriminator " + typeDiscriminator);
    }

    // Read output
    final MemorySegment dataPtr = outDataPtr.get(ValueLayout.ADDRESS, 0);
    final long dataLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

    // Reinterpret the pointer with the correct size
    final MemorySegment dataPtrWithSize = dataPtr.reinterpret(dataLen);

    // Copy data from native memory
    final byte[] marshalledData = new byte[(int) dataLen];
    MemorySegment.copy(dataPtrWithSize, ValueLayout.JAVA_BYTE, 0, marshalledData, 0, (int) dataLen);

    // Free native buffer
    NATIVE_BINDINGS.witValueFreeBuffer(dataPtr, dataLen);

    return new MarshalledValue(typeDiscriminator, marshalledData);
  }

  /**
   * Unmarshals a WIT value from binary format.
   *
   * @param typeDiscriminator the type discriminator (1-8)
   * @param data the serialized byte array
   * @param arena the memory arena for allocation
   * @return the unmarshalled WIT value
   * @throws ValidationException if unmarshalling fails
   */
  public static WitValue unmarshal(
      final int typeDiscriminator, final byte[] data, final Arena arena)
      throws ValidationException {
    if (data == null) {
      throw new ValidationException("Cannot unmarshal null data");
    }
    if (arena == null) {
      throw new IllegalArgumentException("Arena cannot be null");
    }

    // Validate discriminator
    if (!NATIVE_BINDINGS.witValueValidateDiscriminator(typeDiscriminator)) {
      throw new ValidationException("Invalid type discriminator: " + typeDiscriminator);
    }

    // Allocate native memory
    final MemorySegment dataSegment = arena.allocateFrom(ValueLayout.JAVA_BYTE, data);
    final MemorySegment outValuePtr = arena.allocate(ValueLayout.ADDRESS);
    final MemorySegment outLenPtr = arena.allocate(ValueLayout.JAVA_LONG);

    // Call native unmarshalling function
    final int errorCode =
        NATIVE_BINDINGS.witValueDeserialize(
            typeDiscriminator, dataSegment, data.length, outValuePtr, outLenPtr);

    if (errorCode != 0) {
      throw new ValidationException(
          "Native unmarshalling failed for type discriminator " + typeDiscriminator);
    }

    // Read output
    final MemorySegment valuePtr = outValuePtr.get(ValueLayout.ADDRESS, 0);
    final long valueLen = outLenPtr.get(ValueLayout.JAVA_LONG, 0);

    // Reinterpret pointer with correct size
    final MemorySegment valuePtrWithSize = valuePtr.reinterpret(valueLen);

    // Copy data from native memory
    final byte[] unmarshalledData = new byte[(int) valueLen];
    MemorySegment.copy(
        valuePtrWithSize, ValueLayout.JAVA_BYTE, 0, unmarshalledData, 0, (int) valueLen);

    // Free native buffer
    NATIVE_BINDINGS.witValueFreeBuffer(valuePtr, valueLen);

    // Deserialize using the public API
    return ai.tegmentum.wasmtime4j.wit.WitValueDeserializer.deserialize(
        typeDiscriminator, unmarshalledData);
  }

  /**
   * Validates a type discriminator.
   *
   * @param typeDiscriminator the type discriminator to validate
   * @return true if valid, false otherwise
   */
  public static boolean validateDiscriminator(final int typeDiscriminator) {
    return NATIVE_BINDINGS.witValueValidateDiscriminator(typeDiscriminator);
  }
}
