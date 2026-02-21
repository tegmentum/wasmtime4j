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

package ai.tegmentum.wasmtime4j.wasi.random;

/**
 * Callback interface for providing random bytes to WASI components.
 *
 * <p>This interface is used to override the default random number generation in WASI.
 * Implementations must fill the provided byte array with random data.
 *
 * <p>For secure random, the implementation must provide cryptographically secure
 * random bytes. For insecure random, any source of randomness is acceptable.
 *
 * <p>Example usage with a deterministic source for testing:
 *
 * <pre>{@code
 * WasiRandomSource deterministicRng = (dest) -> {
 *     java.util.Random rng = new java.util.Random(42);
 *     rng.nextBytes(dest);
 * };
 *
 * WasiPreview2Config config = WasiPreview2Config.builder()
 *     .insecureRandom(deterministicRng)
 *     .build();
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface WasiRandomSource {

  /**
   * Fills the provided byte array with random data.
   *
   * @param dest the byte array to fill with random bytes
   */
  void fillBytes(byte[] dest);
}
