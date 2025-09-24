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

package ai.tegmentum.wasmtime4j.serialization.security;

import java.time.Instant;
import java.util.Arrays;
import java.util.Objects;

/**
 * Container for encrypted WebAssembly module data.
 *
 * <p>This class encapsulates encrypted data along with the necessary metadata for decryption
 * including the initialization vector, encryption algorithm, and optional authentication tag.
 *
 * @since 1.0.0
 */
public final class EncryptedData {

  private final byte[] ciphertext;
  private final byte[] iv;
  private final String algorithm;
  private final byte[] authTag;
  private final Instant encryptionTimestamp;
  private final String keyId;

  /**
   * Creates encrypted data with the specified parameters.
   *
   * @param ciphertext the encrypted data
   * @param iv the initialization vector used for encryption
   * @param algorithm the encryption algorithm used
   * @throws IllegalArgumentException if required parameters are null
   */
  public EncryptedData(final byte[] ciphertext, final byte[] iv, final String algorithm) {
    this(ciphertext, iv, algorithm, null, null);
  }

  /**
   * Creates encrypted data with authentication tag and key ID.
   *
   * @param ciphertext the encrypted data
   * @param iv the initialization vector used for encryption
   * @param algorithm the encryption algorithm used
   * @param authTag the authentication tag (for authenticated encryption)
   * @param keyId the key identifier used for encryption
   * @throws IllegalArgumentException if required parameters are null
   */
  public EncryptedData(final byte[] ciphertext, final byte[] iv, final String algorithm,
                      final byte[] authTag, final String keyId) {
    this.ciphertext = Objects.requireNonNull(ciphertext, "Ciphertext cannot be null").clone();
    this.iv = Objects.requireNonNull(iv, "IV cannot be null").clone();
    this.algorithm = Objects.requireNonNull(algorithm, "Algorithm cannot be null");
    this.authTag = authTag != null ? authTag.clone() : null;
    this.keyId = keyId;
    this.encryptionTimestamp = Instant.now();
  }

  /**
   * Gets the encrypted data (ciphertext).
   *
   * @return a defensive copy of the ciphertext
   */
  public byte[] getCiphertext() {
    return ciphertext.clone();
  }

  /**
   * Gets the initialization vector used for encryption.
   *
   * @return a defensive copy of the IV
   */
  public byte[] getIv() {
    return iv.clone();
  }

  /**
   * Gets the encryption algorithm used.
   *
   * @return the algorithm name
   */
  public String getAlgorithm() {
    return algorithm;
  }

  /**
   * Gets the authentication tag if available.
   *
   * @return a defensive copy of the authentication tag, or null if not applicable
   */
  public byte[] getAuthTag() {
    return authTag != null ? authTag.clone() : null;
  }

  /**
   * Gets the key identifier used for encryption.
   *
   * @return the key ID, or null if not specified
   */
  public String getKeyId() {
    return keyId;
  }

  /**
   * Gets the timestamp when encryption was performed.
   *
   * @return the encryption timestamp
   */
  public Instant getEncryptionTimestamp() {
    return encryptionTimestamp;
  }

  /**
   * Gets the size of the encrypted data in bytes.
   *
   * @return the size of the ciphertext
   */
  public int getSize() {
    return ciphertext.length;
  }

  /**
   * Checks if this encrypted data uses authenticated encryption.
   *
   * @return true if authentication tag is present
   */
  public boolean isAuthenticatedEncryption() {
    return authTag != null || algorithm.contains("GCM");
  }

  /**
   * Creates a serialized representation of this encrypted data.
   *
   * <p>The serialized format contains all necessary information for decryption
   * in a portable binary format.
   *
   * @return the serialized encrypted data
   */
  public byte[] serialize() {
    // Calculate total size needed
    final byte[] algorithmBytes = algorithm.getBytes(java.nio.charset.StandardCharsets.UTF_8);
    final byte[] keyIdBytes = keyId != null ?
        keyId.getBytes(java.nio.charset.StandardCharsets.UTF_8) : new byte[0];
    final byte[] timestampBytes = encryptionTimestamp.toString()
        .getBytes(java.nio.charset.StandardCharsets.UTF_8);

    final int totalSize = 4 + // version
                         4 + algorithmBytes.length + algorithmBytes.length + // algorithm
                         4 + iv.length + iv.length + // IV
                         4 + ciphertext.length + ciphertext.length + // ciphertext
                         4 + (authTag != null ? authTag.length : 0) +
                         (authTag != null ? authTag.length : 0) + // auth tag
                         4 + keyIdBytes.length + keyIdBytes.length + // key ID
                         4 + timestampBytes.length + timestampBytes.length; // timestamp

    final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.allocate(totalSize);

    // Write version
    buffer.putInt(1);

    // Write algorithm
    buffer.putInt(algorithmBytes.length);
    buffer.put(algorithmBytes);

    // Write IV
    buffer.putInt(iv.length);
    buffer.put(iv);

    // Write ciphertext
    buffer.putInt(ciphertext.length);
    buffer.put(ciphertext);

    // Write authentication tag
    if (authTag != null) {
      buffer.putInt(authTag.length);
      buffer.put(authTag);
    } else {
      buffer.putInt(0);
    }

    // Write key ID
    buffer.putInt(keyIdBytes.length);
    buffer.put(keyIdBytes);

    // Write timestamp
    buffer.putInt(timestampBytes.length);
    buffer.put(timestampBytes);

    return buffer.array();
  }

  /**
   * Deserializes encrypted data from a serialized representation.
   *
   * @param serializedData the serialized encrypted data
   * @return the deserialized encrypted data
   * @throws IllegalArgumentException if serialized data is invalid
   */
  public static EncryptedData deserialize(final byte[] serializedData) {
    Objects.requireNonNull(serializedData, "Serialized data cannot be null");

    final java.nio.ByteBuffer buffer = java.nio.ByteBuffer.wrap(serializedData);

    try {
      // Read version
      final int version = buffer.getInt();
      if (version != 1) {
        throw new IllegalArgumentException("Unsupported serialization version: " + version);
      }

      // Read algorithm
      final int algorithmLength = buffer.getInt();
      final byte[] algorithmBytes = new byte[algorithmLength];
      buffer.get(algorithmBytes);
      final String algorithm = new String(algorithmBytes, java.nio.charset.StandardCharsets.UTF_8);

      // Read IV
      final int ivLength = buffer.getInt();
      final byte[] iv = new byte[ivLength];
      buffer.get(iv);

      // Read ciphertext
      final int ciphertextLength = buffer.getInt();
      final byte[] ciphertext = new byte[ciphertextLength];
      buffer.get(ciphertext);

      // Read authentication tag
      final int authTagLength = buffer.getInt();
      byte[] authTag = null;
      if (authTagLength > 0) {
        authTag = new byte[authTagLength];
        buffer.get(authTag);
      }

      // Read key ID
      final int keyIdLength = buffer.getInt();
      String keyId = null;
      if (keyIdLength > 0) {
        final byte[] keyIdBytes = new byte[keyIdLength];
        buffer.get(keyIdBytes);
        keyId = new String(keyIdBytes, java.nio.charset.StandardCharsets.UTF_8);
      }

      // Read timestamp (for completeness, though we create a new timestamp)
      final int timestampLength = buffer.getInt();
      if (timestampLength > 0) {
        final byte[] timestampBytes = new byte[timestampLength];
        buffer.get(timestampBytes);
        // We ignore the original timestamp and use current time
      }

      return new EncryptedData(ciphertext, iv, algorithm, authTag, keyId);

    } catch (Exception e) {
      throw new IllegalArgumentException("Failed to deserialize encrypted data", e);
    }
  }

  /**
   * Validates the integrity of this encrypted data.
   *
   * @return true if the data appears valid
   */
  public boolean validateIntegrity() {
    // Basic validation checks
    if (ciphertext.length == 0) {
      return false;
    }

    if (iv.length == 0) {
      return false;
    }

    // Algorithm-specific validation
    if (algorithm.contains("GCM")) {
      // GCM should have 12-byte IV
      return iv.length == 12;
    } else if (algorithm.contains("CBC")) {
      // CBC should have 16-byte IV for AES
      return iv.length == 16;
    }

    return true;
  }

  /**
   * Creates a copy of this encrypted data with a new key ID.
   *
   * @param newKeyId the new key identifier
   * @return a new EncryptedData instance with the updated key ID
   */
  public EncryptedData withKeyId(final String newKeyId) {
    return new EncryptedData(ciphertext, iv, algorithm, authTag, newKeyId);
  }

  /**
   * Securely clears the encrypted data from memory.
   *
   * <p>This method overwrites the internal arrays with zeros to prevent
   * sensitive data from remaining in memory longer than necessary.
   */
  public void clear() {
    Arrays.fill(ciphertext, (byte) 0);
    Arrays.fill(iv, (byte) 0);
    if (authTag != null) {
      Arrays.fill(authTag, (byte) 0);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "EncryptedData{algorithm='%s', size=%d bytes, iv=%d bytes, authenticated=%s, keyId='%s', timestamp=%s}",
        algorithm,
        ciphertext.length,
        iv.length,
        isAuthenticatedEncryption(),
        keyId != null ? keyId : "none",
        encryptionTimestamp);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) return true;
    if (obj == null || getClass() != obj.getClass()) return false;

    final EncryptedData other = (EncryptedData) obj;
    return Arrays.equals(ciphertext, other.ciphertext) &&
           Arrays.equals(iv, other.iv) &&
           Objects.equals(algorithm, other.algorithm) &&
           Arrays.equals(authTag, other.authTag) &&
           Objects.equals(keyId, other.keyId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        Arrays.hashCode(ciphertext),
        Arrays.hashCode(iv),
        algorithm,
        Arrays.hashCode(authTag),
        keyId);
  }

  @Override
  protected void finalize() throws Throwable {
    try {
      // Ensure sensitive data is cleared when object is garbage collected
      clear();
    } finally {
      super.finalize();
    }
  }
}