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
 * Cryptographic hash algorithms supported by WASI-crypto.
 *
 * @since 1.0.0
 */
public enum HashAlgorithm {

  /** SHA-256 (256-bit output). */
  SHA_256("SHA-256", 256),

  /** SHA-384 (384-bit output). */
  SHA_384("SHA-384", 384),

  /** SHA-512 (512-bit output). */
  SHA_512("SHA-512", 512),

  /** SHA-512/256 (256-bit output with SHA-512 internals). */
  SHA_512_256("SHA-512/256", 256),

  /** SHA3-256 (256-bit output). */
  SHA3_256("SHA3-256", 256),

  /** SHA3-384 (384-bit output). */
  SHA3_384("SHA3-384", 384),

  /** SHA3-512 (512-bit output). */
  SHA3_512("SHA3-512", 512),

  /** BLAKE2b (up to 512-bit output). */
  BLAKE2B("BLAKE2b", 512),

  /** BLAKE2s (up to 256-bit output). */
  BLAKE2S("BLAKE2s", 256),

  /** BLAKE3 (default 256-bit output, variable). */
  BLAKE3("BLAKE3", 256);

  private final String algorithmName;
  private final int outputSize;

  HashAlgorithm(final String algorithmName, final int outputSize) {
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
