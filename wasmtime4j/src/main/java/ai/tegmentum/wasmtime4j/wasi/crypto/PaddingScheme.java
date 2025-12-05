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
 * Padding schemes for asymmetric encryption.
 *
 * @since 1.0.0
 */
public enum PaddingScheme {

  /** No padding (raw encryption). */
  NONE("None"),

  /** PKCS#1 v1.5 padding. */
  PKCS1_V15("PKCS1-v1.5"),

  /** Optimal Asymmetric Encryption Padding (OAEP) with SHA-1. */
  OAEP_SHA1("OAEP-SHA1"),

  /** OAEP with SHA-256. */
  OAEP_SHA256("OAEP-SHA256"),

  /** OAEP with SHA-384. */
  OAEP_SHA384("OAEP-SHA384"),

  /** OAEP with SHA-512. */
  OAEP_SHA512("OAEP-SHA512"),

  /** Probabilistic Signature Scheme (PSS) for signatures. */
  PSS("PSS");

  private final String schemeName;

  PaddingScheme(final String schemeName) {
    this.schemeName = schemeName;
  }

  /**
   * Gets the padding scheme name.
   *
   * @return the scheme name
   */
  public String getSchemeName() {
    return schemeName;
  }
}
