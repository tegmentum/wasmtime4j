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
 * Block cipher encryption modes for symmetric encryption.
 *
 * @since 1.0.0
 */
public enum EncryptionMode {

  /** Electronic Codebook mode (not recommended for most use cases). */
  ECB("ECB", false, false),

  /** Cipher Block Chaining mode. */
  CBC("CBC", true, false),

  /** Counter mode. */
  CTR("CTR", true, false),

  /** Galois/Counter Mode - provides authenticated encryption. */
  GCM("GCM", true, true),

  /** Counter with CBC-MAC - provides authenticated encryption. */
  CCM("CCM", true, true),

  /** Offset Codebook Mode - provides authenticated encryption. */
  OCB("OCB", true, true),

  /** Synthetic Initialization Vector mode - deterministic AEAD. */
  SIV("SIV", true, true),

  /** AES-GCM-SIV - nonce-misuse resistant AEAD. */
  GCM_SIV("GCM-SIV", true, true);

  private final String modeName;
  private final boolean requiresIv;
  private final boolean providesAuthentication;

  EncryptionMode(
      final String modeName, final boolean requiresIv, final boolean providesAuthentication) {
    this.modeName = modeName;
    this.requiresIv = requiresIv;
    this.providesAuthentication = providesAuthentication;
  }

  /**
   * Gets the mode name.
   *
   * @return the mode name
   */
  public String getModeName() {
    return modeName;
  }

  /**
   * Checks if this mode requires an initialization vector.
   *
   * @return true if IV is required
   */
  public boolean requiresIv() {
    return requiresIv;
  }

  /**
   * Checks if this mode provides authenticated encryption.
   *
   * @return true if the mode provides authentication
   */
  public boolean providesAuthentication() {
    return providesAuthentication;
  }
}
