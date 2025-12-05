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
 * Error codes for WASI-crypto operations.
 *
 * @since 1.0.0
 */
public enum CryptoErrorCode {

  /** Unknown or unspecified error. */
  UNKNOWN,

  /** The requested algorithm is not supported. */
  UNSUPPORTED_ALGORITHM,

  /** The requested operation is not supported. */
  UNSUPPORTED_OPERATION,

  /** Invalid key provided. */
  INVALID_KEY,

  /** Key not found. */
  KEY_NOT_FOUND,

  /** Invalid signature. */
  INVALID_SIGNATURE,

  /** Signature verification failed. */
  VERIFICATION_FAILED,

  /** Invalid input data. */
  INVALID_INPUT,

  /** Invalid output buffer size. */
  INVALID_OUTPUT_SIZE,

  /** Invalid nonce or IV. */
  INVALID_NONCE,

  /** Authentication tag verification failed. */
  AUTHENTICATION_FAILED,

  /** Encryption failed. */
  ENCRYPTION_FAILED,

  /** Decryption failed. */
  DECRYPTION_FAILED,

  /** Key generation failed. */
  KEY_GENERATION_FAILED,

  /** Random number generation failed. */
  RNG_FAILED,

  /** Hardware acceleration unavailable. */
  HARDWARE_UNAVAILABLE,

  /** Operation not permitted. */
  NOT_PERMITTED,

  /** Resource exhausted. */
  RESOURCE_EXHAUSTED,

  /** Operation timed out. */
  TIMEOUT,

  /** Internal error. */
  INTERNAL_ERROR
}
