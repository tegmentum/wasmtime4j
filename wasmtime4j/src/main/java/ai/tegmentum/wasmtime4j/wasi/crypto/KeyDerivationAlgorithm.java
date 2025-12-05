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
 * Key derivation function algorithms supported by WASI-crypto.
 *
 * @since 1.0.0
 */
public enum KeyDerivationAlgorithm {

  /** HKDF with SHA-256. */
  HKDF_SHA256("HKDF-SHA256"),

  /** HKDF with SHA-384. */
  HKDF_SHA384("HKDF-SHA384"),

  /** HKDF with SHA-512. */
  HKDF_SHA512("HKDF-SHA512"),

  /** PBKDF2 with HMAC-SHA256. */
  PBKDF2_HMAC_SHA256("PBKDF2-HMAC-SHA256"),

  /** PBKDF2 with HMAC-SHA384. */
  PBKDF2_HMAC_SHA384("PBKDF2-HMAC-SHA384"),

  /** PBKDF2 with HMAC-SHA512. */
  PBKDF2_HMAC_SHA512("PBKDF2-HMAC-SHA512"),

  /** Argon2id password hashing. */
  ARGON2ID("Argon2id"),

  /** Argon2i password hashing. */
  ARGON2I("Argon2i"),

  /** scrypt password hashing. */
  SCRYPT("scrypt"),

  /** bcrypt password hashing. */
  BCRYPT("bcrypt");

  private final String algorithmName;

  KeyDerivationAlgorithm(final String algorithmName) {
    this.algorithmName = algorithmName;
  }

  /**
   * Gets the algorithm name.
   *
   * @return the algorithm name
   */
  public String getAlgorithmName() {
    return algorithmName;
  }
}
