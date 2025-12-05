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

package ai.tegmentum.wasmtime4j.wasi.keyvalue;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Represents a key-value entry with metadata.
 *
 * @since 1.0.0
 */
public final class KeyValueEntry {

  private final String key;
  private final byte[] value;
  private final long version;
  private final Instant createdAt;
  private final Instant modifiedAt;
  private final Instant expiresAt;

  private KeyValueEntry(final Builder builder) {
    this.key = builder.key;
    this.value = builder.value != null ? builder.value.clone() : null;
    this.version = builder.version;
    this.createdAt = builder.createdAt;
    this.modifiedAt = builder.modifiedAt;
    this.expiresAt = builder.expiresAt;
  }

  /**
   * Creates a new builder for key-value entries.
   *
   * @param key the key
   * @param value the value
   * @return a new builder
   */
  public static Builder builder(final String key, final byte[] value) {
    return new Builder(key, value);
  }

  /**
   * Gets the key.
   *
   * @return the key
   */
  public String getKey() {
    return key;
  }

  /**
   * Gets the value.
   *
   * @return the value bytes
   */
  public byte[] getValue() {
    return value != null ? value.clone() : null;
  }

  /**
   * Gets the version number.
   *
   * @return the version
   */
  public long getVersion() {
    return version;
  }

  /**
   * Gets the creation timestamp.
   *
   * @return the creation time, or empty if not available
   */
  public Optional<Instant> getCreatedAt() {
    return Optional.ofNullable(createdAt);
  }

  /**
   * Gets the last modification timestamp.
   *
   * @return the modification time, or empty if not available
   */
  public Optional<Instant> getModifiedAt() {
    return Optional.ofNullable(modifiedAt);
  }

  /**
   * Gets the expiration timestamp.
   *
   * @return the expiration time, or empty if entry does not expire
   */
  public Optional<Instant> getExpiresAt() {
    return Optional.ofNullable(expiresAt);
  }

  /**
   * Checks if this entry has expired.
   *
   * @return true if expired
   */
  public boolean isExpired() {
    return expiresAt != null && Instant.now().isAfter(expiresAt);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof KeyValueEntry)) {
      return false;
    }
    final KeyValueEntry other = (KeyValueEntry) obj;
    return Objects.equals(key, other.key) && version == other.version;
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, version);
  }

  @Override
  public String toString() {
    return String.format("KeyValueEntry{key='%s', version=%d}", key, version);
  }

  /** Builder for key-value entries. */
  public static final class Builder {
    private final String key;
    private final byte[] value;
    private long version = 1;
    private Instant createdAt;
    private Instant modifiedAt;
    private Instant expiresAt;

    private Builder(final String key, final byte[] value) {
      this.key = key;
      this.value = value != null ? value.clone() : null;
    }

    /**
     * Sets the version.
     *
     * @param version the version number
     * @return this builder
     */
    public Builder version(final long version) {
      this.version = version;
      return this;
    }

    /**
     * Sets the creation timestamp.
     *
     * @param createdAt the creation time
     * @return this builder
     */
    public Builder createdAt(final Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    /**
     * Sets the modification timestamp.
     *
     * @param modifiedAt the modification time
     * @return this builder
     */
    public Builder modifiedAt(final Instant modifiedAt) {
      this.modifiedAt = modifiedAt;
      return this;
    }

    /**
     * Sets the expiration timestamp.
     *
     * @param expiresAt the expiration time
     * @return this builder
     */
    public Builder expiresAt(final Instant expiresAt) {
      this.expiresAt = expiresAt;
      return this;
    }

    /**
     * Builds the key-value entry.
     *
     * @return the key-value entry
     */
    public KeyValueEntry build() {
      return new KeyValueEntry(this);
    }
  }
}
