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
 * Asymmetric encryption algorithms supported by WASI-crypto.
 *
 * @since 1.0.0
 */
public enum AsymmetricAlgorithm {

  /** RSA with 2048-bit key. */
  RSA_2048("RSA-2048", 2048),

  /** RSA with 3072-bit key. */
  RSA_3072("RSA-3072", 3072),

  /** RSA with 4096-bit key. */
  RSA_4096("RSA-4096", 4096),

  /** X25519 key exchange. */
  X25519("X25519", 256),

  /** X448 key exchange. */
  X448("X448", 448),

  /** ECDH on P-256 curve. */
  ECDH_P256("ECDH-P256", 256),

  /** ECDH on P-384 curve. */
  ECDH_P384("ECDH-P384", 384),

  /** ECDH on P-521 curve. */
  ECDH_P521("ECDH-P521", 521),

  /** ECDH on secp256k1 curve. */
  ECDH_SECP256K1("ECDH-secp256k1", 256);

  private final String algorithmName;
  private final int keySize;

  AsymmetricAlgorithm(final String algorithmName, final int keySize) {
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
