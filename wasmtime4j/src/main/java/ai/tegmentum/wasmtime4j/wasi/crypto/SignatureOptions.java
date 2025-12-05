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
 * Options for digital signature operations.
 *
 * @since 1.0.0
 */
public final class SignatureOptions {

  private final boolean prehashed;
  private final HashAlgorithm prehashAlgorithm;
  private final boolean useHardwareAcceleration;
  private final boolean deterministicSignatures;

  private SignatureOptions(final Builder builder) {
    this.prehashed = builder.prehashed;
    this.prehashAlgorithm = builder.prehashAlgorithm;
    this.useHardwareAcceleration = builder.useHardwareAcceleration;
    this.deterministicSignatures = builder.deterministicSignatures;
  }

  /**
   * Creates a new builder for signature options.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates default signature options.
   *
   * @return default options
   */
  public static SignatureOptions defaults() {
    return new Builder().build();
  }

  /**
   * Checks if the message is already prehashed.
   *
   * @return true if prehashed
   */
  public boolean isPrehashed() {
    return prehashed;
  }

  /**
   * Gets the prehash algorithm if message is prehashed.
   *
   * @return the prehash algorithm, or null
   */
  public HashAlgorithm getPrehashAlgorithm() {
    return prehashAlgorithm;
  }

  /**
   * Checks if hardware acceleration should be used.
   *
   * @return true if hardware acceleration is preferred
   */
  public boolean useHardwareAcceleration() {
    return useHardwareAcceleration;
  }

  /**
   * Checks if deterministic signatures should be used (RFC 6979).
   *
   * @return true if deterministic signatures are preferred
   */
  public boolean useDeterministicSignatures() {
    return deterministicSignatures;
  }

  /** Builder for signature options. */
  public static final class Builder {
    private boolean prehashed;
    private HashAlgorithm prehashAlgorithm;
    private boolean useHardwareAcceleration = true;
    private boolean deterministicSignatures = true;

    private Builder() {}

    /**
     * Marks the message as prehashed.
     *
     * @param algorithm the hash algorithm used
     * @return this builder
     */
    public Builder prehashed(final HashAlgorithm algorithm) {
      this.prehashed = true;
      this.prehashAlgorithm = algorithm;
      return this;
    }

    /**
     * Sets whether to use hardware acceleration.
     *
     * @param useHardwareAcceleration true to prefer hardware acceleration
     * @return this builder
     */
    public Builder useHardwareAcceleration(final boolean useHardwareAcceleration) {
      this.useHardwareAcceleration = useHardwareAcceleration;
      return this;
    }

    /**
     * Sets whether to use deterministic signatures (RFC 6979).
     *
     * @param deterministicSignatures true to use deterministic signatures
     * @return this builder
     */
    public Builder deterministicSignatures(final boolean deterministicSignatures) {
      this.deterministicSignatures = deterministicSignatures;
      return this;
    }

    /**
     * Builds the signature options.
     *
     * @return the signature options
     */
    public SignatureOptions build() {
      return new SignatureOptions(this);
    }
  }
}
