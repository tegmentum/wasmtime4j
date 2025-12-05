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

package ai.tegmentum.wasmtime4j.wasi.crypto;

import java.util.Optional;

/**
 * Represents a cryptographic key in WASI-crypto.
 *
 * <p>Keys are opaque handles that can be used for cryptographic operations. The actual key material
 * is managed by the crypto implementation and is not directly accessible.
 *
 * @since 1.0.0
 */
public interface CryptoKey extends AutoCloseable {

  /**
   * Gets the unique identifier for this key.
   *
   * @return the key identifier
   */
  String getId();

  /**
   * Gets the type of this key.
   *
   * @return the key type
   */
  CryptoKeyType getKeyType();

  /**
   * Gets the algorithm this key is intended for.
   *
   * @return the algorithm name
   */
  String getAlgorithm();

  /**
   * Gets the key size in bits.
   *
   * @return the key size in bits
   */
  int getKeySizeBits();

  /**
   * Checks if this key can be exported.
   *
   * @return true if the key is exportable
   */
  boolean isExportable();

  /**
   * Exports the public key bytes if this is a key pair.
   *
   * @return the public key bytes, or empty if not a key pair
   * @throws CryptoException if export fails
   */
  Optional<byte[]> exportPublicKey() throws CryptoException;

  /**
   * Extracts the public key from this key pair.
   *
   * @return the public key
   * @throws CryptoException if this is not a key pair
   */
  CryptoKey extractPublicKey() throws CryptoException;

  /**
   * Checks if this key is still valid and usable.
   *
   * @return true if the key is valid
   */
  boolean isValid();

  @Override
  void close();
}
