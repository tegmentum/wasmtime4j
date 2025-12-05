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

/**
 * Symmetric encryption algorithms supported by WASI-crypto.
 *
 * @since 1.0.0
 */
public enum SymmetricAlgorithm {

  /** AES with 128-bit key. */
  AES_128("AES-128", 128),

  /** AES with 192-bit key. */
  AES_192("AES-192", 192),

  /** AES with 256-bit key. */
  AES_256("AES-256", 256),

  /** ChaCha20 stream cipher. */
  CHACHA20("ChaCha20", 256),

  /** XChaCha20 extended nonce stream cipher. */
  XCHACHA20("XChaCha20", 256),

  /** ChaCha20-Poly1305 AEAD. */
  CHACHA20_POLY1305("ChaCha20-Poly1305", 256),

  /** XChaCha20-Poly1305 AEAD with extended nonce. */
  XCHACHA20_POLY1305("XChaCha20-Poly1305", 256);

  private final String algorithmName;
  private final int keySize;

  SymmetricAlgorithm(final String algorithmName, final int keySize) {
    this.algorithmName = algorithmName;
    this.keySize = keySize;
  }

  /**
   * Gets the algorithm name.
   *
   * @return the algorithm name
   */
  public String getAlgorithmName() {
    return algorithmName;
  }

  /**
   * Gets the key size in bits.
   *
   * @return the key size in bits
   */
  public int getKeySize() {
    return keySize;
  }
}
