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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.wit.WitValue;
import ai.tegmentum.wasmtime4j.wit.WitValueDeserializer;
import ai.tegmentum.wasmtime4j.wit.WitValueMarshaller.MarshalledValue;
import ai.tegmentum.wasmtime4j.wit.WitValueSerializer;

/**
 * JNI implementation of WIT value marshaller.
 *
 * <p>This class provides bidirectional marshalling between Java WIT values and the binary format
 * used by native Wasmtime component runtime. It uses JNI to call native Rust functions for actual
 * marshalling work to ensure consistency with Wasmtime's expectations.
 *
 * <p>The marshalling process:
 *
 * <ol>
 *   <li>Java WIT values are serialized using {@link WitValueSerializer}
 *   <li>Serialized bytes are passed to native marshalling functions via JNI
 *   <li>Native code validates and potentially transforms the data
 *   <li>Result is returned as marshalled bytes ready for component calls
 * </ol>
 *
 * @since 1.0.0
 */
public final class JniWitValueMarshaller {

  static {
    NativeLibraryLoader.loadLibrary();
  }

  /** Private constructor to prevent instantiation. */
  private JniWitValueMarshaller() {}

  /**
   * Marshals a WIT value to binary format for native use.
   *
   * @param value the WIT value to marshal
   * @return marshalled value ready for native calls
   * @throws ValidationException if marshalling fails
   */
  public static MarshalledValue marshal(final WitValue value) throws ValidationException {
    if (value == null) {
      throw new ValidationException("Cannot marshal null value");
    }

    // Get type discriminator and serialize value
    final int typeDiscriminator = WitValueSerializer.getTypeDiscriminator(value);
    final byte[] data = WitValueSerializer.serialize(value);

    // Call native marshalling function
    final byte[] marshalledData = witValueSerializeNative(typeDiscriminator, data);

    if (marshalledData == null) {
      throw new ValidationException(
          "Native marshalling failed for type discriminator " + typeDiscriminator);
    }

    return new MarshalledValue(typeDiscriminator, marshalledData);
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

    // Validate discriminator
    if (!validateDiscriminator(typeDiscriminator)) {
      throw new ValidationException("Invalid type discriminator: " + typeDiscriminator);
    }

    // Call native unmarshalling function
    final byte[] unmarshalledData = witValueDeserializeNative(typeDiscriminator, data);

    if (unmarshalledData == null) {
      throw new ValidationException(
          "Native unmarshalling failed for type discriminator " + typeDiscriminator);
    }

    // Deserialize using the public API
    return WitValueDeserializer.deserialize(typeDiscriminator, unmarshalledData);
  }

  /**
   * Validates a type discriminator.
   *
   * @param typeDiscriminator the type discriminator to validate
   * @return true if valid, false otherwise
   */
  public static boolean validateDiscriminator(final int typeDiscriminator) {
    return witValueValidateDiscriminatorNative(typeDiscriminator);
  }

  // Native methods

  /**
   * Serializes a WIT value to binary format via native code.
   *
   * @param typeDiscriminator the type discriminator (1-6)
   * @param data the serialized value data
   * @return the marshalled byte array, or null if marshalling fails
   */
  private static native byte[] witValueSerializeNative(int typeDiscriminator, byte[] data);

  /**
   * Deserializes a WIT value from binary format via native code.
   *
   * @param typeDiscriminator the type discriminator (1-6)
   * @param data the serialized data
   * @return the unmarshalled byte array, or null if unmarshalling fails
   */
  private static native byte[] witValueDeserializeNative(int typeDiscriminator, byte[] data);

  /**
   * Validates a type discriminator via native code.
   *
   * @param typeDiscriminator the type discriminator to validate
   * @return true if valid (1-6), false otherwise
   */
  private static native boolean witValueValidateDiscriminatorNative(int typeDiscriminator);
}
