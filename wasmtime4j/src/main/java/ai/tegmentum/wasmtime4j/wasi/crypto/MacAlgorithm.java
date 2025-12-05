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
 * Message authentication code (MAC) algorithms supported by WASI-crypto.
 *
 * @since 1.0.0
 */
public enum MacAlgorithm {

  /** HMAC with SHA-256. */
  HMAC_SHA256("HMAC-SHA256", 256),

  /** HMAC with SHA-384. */
  HMAC_SHA384("HMAC-SHA384", 384),

  /** HMAC with SHA-512. */
  HMAC_SHA512("HMAC-SHA512", 512),

  /** HMAC with SHA3-256. */
  HMAC_SHA3_256("HMAC-SHA3-256", 256),

  /** Poly1305 one-time authenticator. */
  POLY1305("Poly1305", 128),

  /** BLAKE2b-MAC. */
  BLAKE2B_MAC("BLAKE2b-MAC", 512),

  /** BLAKE2s-MAC. */
  BLAKE2S_MAC("BLAKE2s-MAC", 256),

  /** CMAC with AES. */
  AES_CMAC("AES-CMAC", 128),

  /** KMAC128 (NIST SP 800-185). */
  KMAC128("KMAC128", 256),

  /** KMAC256 (NIST SP 800-185). */
  KMAC256("KMAC256", 512);

  private final String algorithmName;
  private final int outputSize;

  MacAlgorithm(final String algorithmName, final int outputSize) {
    this.algorithmName = algorithmName;
    this.outputSize = outputSize;
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
   * Gets the output size in bits.
   *
   * @return the output size in bits
   */
  public int getOutputSize() {
    return outputSize;
  }
}
