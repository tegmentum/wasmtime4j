/*
 * Copyright 2025 Tegmentum AI
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
package ai.tegmentum.wasmtime4j.wasi.random;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * WASI random data API for secure random number generation.
 *
 * <p>Provides access to cryptographically-secure random data designed for portability across
 * Unix-family platforms and Windows.
 *
 * <p>All random data returned by this interface must be:
 *
 * <ul>
 *   <li>Cryptographically-secure (equivalent to properly seeded CSPRNG)
 *   <li>Unpredictable and fresh
 *   <li>Non-blocking under all circumstances
 * </ul>
 *
 * <p>WASI Preview 2 specification: wasi:random/random@0.2.8
 */
public interface WasiRandom {

  /**
   * Gets cryptographically-secure random bytes.
   *
   * <p>Returns a byte array of the specified length filled with cryptographically-secure random
   * data. This method cannot block and must always return unpredictable, fresh data.
   *
   * <p>The returned data is suitable for cryptographic operations, session keys, nonces, and other
   * security-sensitive applications.
   *
   * @param len number of random bytes to generate
   * @return byte array containing random data
   * @throws WasmException if generating random data fails
   * @throws IllegalArgumentException if len is negative
   */
  byte[] getRandomBytes(long len);

  /**
   * Gets a cryptographically-secure random unsigned 64-bit integer.
   *
   * <p>Returns a random long value with the same data quality guarantees as {@link
   * #getRandomBytes(long)}. This is a convenience method for obtaining scalar random values.
   *
   * <p>The returned value is suitable for random identifiers, seeds, and other applications
   * requiring unpredictable 64-bit values.
   *
   * @return random unsigned 64-bit integer
   * @throws WasmException if generating random data fails
   */
  long getRandomU64();
}
