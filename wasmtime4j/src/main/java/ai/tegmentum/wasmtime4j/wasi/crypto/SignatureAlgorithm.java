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
 * Digital signature algorithms supported by WASI-crypto.
 *
 * @since 1.0.0
 */
public enum SignatureAlgorithm {

  /** RSA-PKCS1-v1.5 with SHA-256. */
  RSA_PKCS1_SHA256("RSA-PKCS1-SHA256", 2048),

  /** RSA-PKCS1-v1.5 with SHA-384. */
  RSA_PKCS1_SHA384("RSA-PKCS1-SHA384", 3072),

  /** RSA-PKCS1-v1.5 with SHA-512. */
  RSA_PKCS1_SHA512("RSA-PKCS1-SHA512", 4096),

  /** RSA-PSS with SHA-256. */
  RSA_PSS_SHA256("RSA-PSS-SHA256", 2048),

  /** RSA-PSS with SHA-384. */
  RSA_PSS_SHA384("RSA-PSS-SHA384", 3072),

  /** RSA-PSS with SHA-512. */
  RSA_PSS_SHA512("RSA-PSS-SHA512", 4096),

  /** ECDSA on P-256 curve with SHA-256. */
  ECDSA_P256_SHA256("ECDSA-P256-SHA256", 256),

  /** ECDSA on P-384 curve with SHA-384. */
  ECDSA_P384_SHA384("ECDSA-P384-SHA384", 384),

  /** ECDSA on P-521 curve with SHA-512. */
  ECDSA_P521_SHA512("ECDSA-P521-SHA512", 521),

  /** ECDSA on secp256k1 curve with SHA-256 (Bitcoin/Ethereum). */
  ECDSA_SECP256K1_SHA256("ECDSA-secp256k1-SHA256", 256),

  /** Ed25519 signature scheme. */
  ED25519("Ed25519", 256),

  /** Ed448 signature scheme. */
  ED448("Ed448", 448);

  private final String algorithmName;
  private final int keySize;

  SignatureAlgorithm(final String algorithmName, final int keySize) {
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
