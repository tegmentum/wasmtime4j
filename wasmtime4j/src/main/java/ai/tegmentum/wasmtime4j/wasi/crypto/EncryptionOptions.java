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

import java.util.Optional;

/**
 * Options for symmetric encryption operations.
 *
 * @since 1.0.0
 */
public final class EncryptionOptions {

  private final EncryptionMode mode;
  private final byte[] iv;
  private final byte[] additionalData;
  private final boolean useHardwareAcceleration;

  private EncryptionOptions(final Builder builder) {
    this.mode = builder.mode;
    this.iv = builder.iv != null ? builder.iv.clone() : null;
    this.additionalData = builder.additionalData != null ? builder.additionalData.clone() : null;
    this.useHardwareAcceleration = builder.useHardwareAcceleration;
  }

  /**
   * Creates a new builder for encryption options.
   *
   * @param mode the encryption mode
   * @return a new builder
   */
  public static Builder builder(final EncryptionMode mode) {
    return new Builder(mode);
  }

  /**
   * Gets the encryption mode.
   *
   * @return the encryption mode
   */
  public EncryptionMode getMode() {
    return mode;
  }

  /**
   * Gets the initialization vector.
   *
   * @return the IV, or empty if not set
   */
  public Optional<byte[]> getIv() {
    return Optional.ofNullable(iv != null ? iv.clone() : null);
  }

  /**
   * Gets the additional authenticated data (for AEAD modes).
   *
   * @return the AAD, or empty if not set
   */
  public Optional<byte[]> getAdditionalData() {
    return Optional.ofNullable(additionalData != null ? additionalData.clone() : null);
  }

  /**
   * Checks if hardware acceleration should be used.
   *
   * @return true if hardware acceleration is preferred
   */
  public boolean useHardwareAcceleration() {
    return useHardwareAcceleration;
  }

  /** Builder for encryption options. */
  public static final class Builder {
    private final EncryptionMode mode;
    private byte[] iv;
    private byte[] additionalData;
    private boolean useHardwareAcceleration = true;

    private Builder(final EncryptionMode mode) {
      this.mode = mode;
    }

    /**
     * Sets the initialization vector.
     *
     * @param iv the IV bytes
     * @return this builder
     */
    public Builder iv(final byte[] iv) {
      this.iv = iv != null ? iv.clone() : null;
      return this;
    }

    /**
     * Sets the additional authenticated data for AEAD modes.
     *
     * @param additionalData the AAD bytes
     * @return this builder
     */
    public Builder additionalData(final byte[] additionalData) {
      this.additionalData = additionalData != null ? additionalData.clone() : null;
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
     * Builds the encryption options.
     *
     * @return the encryption options
     */
    public EncryptionOptions build() {
      return new EncryptionOptions(this);
    }
  }
}
