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

package ai.tegmentum.wasmtime4j.component;

import java.util.Optional;
import java.util.Set;

/**
 * Search criteria for finding components in a ComponentRegistry.
 *
 * <p>This class provides comprehensive search criteria for component discovery, including name
 * patterns, version ranges, interface requirements, and metadata filtering.
 *
 * @since 1.0.0
 */
public final class ComponentSearchCriteria {

  private final Optional<String> namePattern;
  private final Optional<ComponentVersion> minVersion;
  private final Optional<ComponentVersion> maxVersion;
  private final Set<String> requiredInterfaces;
  private final Set<String> excludedInterfaces;
  private final Optional<String> author;
  private final Optional<String> description;
  private final Set<String> tags;
  private final Optional<Long> maxSize;
  private final Optional<Long> minSize;

  private ComponentSearchCriteria(Builder builder) {
    this.namePattern = builder.namePattern;
    this.minVersion = builder.minVersion;
    this.maxVersion = builder.maxVersion;
    this.requiredInterfaces = Set.copyOf(builder.requiredInterfaces);
    this.excludedInterfaces = Set.copyOf(builder.excludedInterfaces);
    this.author = builder.author;
    this.description = builder.description;
    this.tags = Set.copyOf(builder.tags);
    this.maxSize = builder.maxSize;
    this.minSize = builder.minSize;
  }

  /**
   * Gets the name pattern for component matching.
   *
   * @return the name pattern if specified
   */
  public Optional<String> getNamePattern() {
    return namePattern;
  }

  /**
   * Gets the minimum version requirement.
   *
   * @return the minimum version if specified
   */
  public Optional<ComponentVersion> getMinVersion() {
    return minVersion;
  }

  /**
   * Gets the maximum version requirement.
   *
   * @return the maximum version if specified
   */
  public Optional<ComponentVersion> getMaxVersion() {
    return maxVersion;
  }

  /**
   * Gets the required interfaces that components must implement.
   *
   * @return the set of required interface names
   */
  public Set<String> getRequiredInterfaces() {
    return requiredInterfaces;
  }

  /**
   * Gets the interfaces that components must NOT implement.
   *
   * @return the set of excluded interface names
   */
  public Set<String> getExcludedInterfaces() {
    return excludedInterfaces;
  }

  /**
   * Gets the author filter.
   *
   * @return the author filter if specified
   */
  public Optional<String> getAuthor() {
    return author;
  }

  /**
   * Gets the description filter.
   *
   * @return the description filter if specified
   */
  public Optional<String> getDescription() {
    return description;
  }

  /**
   * Gets the required tags for component matching.
   *
   * @return the set of required tags
   */
  public Set<String> getTags() {
    return tags;
  }

  /**
   * Gets the maximum size requirement.
   *
   * @return the maximum size in bytes if specified
   */
  public Optional<Long> getMaxSize() {
    return maxSize;
  }

  /**
   * Gets the minimum size requirement.
   *
   * @return the minimum size in bytes if specified
   */
  public Optional<Long> getMinSize() {
    return minSize;
  }

  /**
   * Creates a new builder for ComponentSearchCriteria.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ComponentSearchCriteria. */
  public static final class Builder {
    private Optional<String> namePattern = Optional.empty();
    private Optional<ComponentVersion> minVersion = Optional.empty();
    private Optional<ComponentVersion> maxVersion = Optional.empty();
    private Set<String> requiredInterfaces = Set.of();
    private Set<String> excludedInterfaces = Set.of();
    private Optional<String> author = Optional.empty();
    private Optional<String> description = Optional.empty();
    private Set<String> tags = Set.of();
    private Optional<Long> maxSize = Optional.empty();
    private Optional<Long> minSize = Optional.empty();

    private Builder() {}

    /**
     * Sets the name pattern for component matching.
     *
     * @param namePattern the name pattern (supports wildcards)
     * @return this builder
     */
    public Builder namePattern(String namePattern) {
      this.namePattern = Optional.ofNullable(namePattern);
      return this;
    }

    /**
     * Sets the minimum version requirement.
     *
     * @param minVersion the minimum version
     * @return this builder
     */
    public Builder minVersion(ComponentVersion minVersion) {
      this.minVersion = Optional.ofNullable(minVersion);
      return this;
    }

    /**
     * Sets the maximum version requirement.
     *
     * @param maxVersion the maximum version
     * @return this builder
     */
    public Builder maxVersion(ComponentVersion maxVersion) {
      this.maxVersion = Optional.ofNullable(maxVersion);
      return this;
    }

    /**
     * Sets the required interfaces that components must implement.
     *
     * @param requiredInterfaces the set of required interface names
     * @return this builder
     */
    public Builder requiredInterfaces(Set<String> requiredInterfaces) {
      this.requiredInterfaces = requiredInterfaces != null ? requiredInterfaces : Set.of();
      return this;
    }

    /**
     * Sets the interfaces that components must NOT implement.
     *
     * @param excludedInterfaces the set of excluded interface names
     * @return this builder
     */
    public Builder excludedInterfaces(Set<String> excludedInterfaces) {
      this.excludedInterfaces = excludedInterfaces != null ? excludedInterfaces : Set.of();
      return this;
    }

    /**
     * Sets the author filter.
     *
     * @param author the author to match
     * @return this builder
     */
    public Builder author(String author) {
      this.author = Optional.ofNullable(author);
      return this;
    }

    /**
     * Sets the description filter.
     *
     * @param description the description pattern to match
     * @return this builder
     */
    public Builder description(String description) {
      this.description = Optional.ofNullable(description);
      return this;
    }

    /**
     * Sets the required tags for component matching.
     *
     * @param tags the set of required tags
     * @return this builder
     */
    public Builder tags(Set<String> tags) {
      this.tags = tags != null ? tags : Set.of();
      return this;
    }

    /**
     * Sets the maximum size requirement.
     *
     * @param maxSize the maximum size in bytes
     * @return this builder
     */
    public Builder maxSize(long maxSize) {
      this.maxSize = Optional.of(maxSize);
      return this;
    }

    /**
     * Sets the minimum size requirement.
     *
     * @param minSize the minimum size in bytes
     * @return this builder
     */
    public Builder minSize(long minSize) {
      this.minSize = Optional.of(minSize);
      return this;
    }

    /**
     * Builds the ComponentSearchCriteria.
     *
     * @return the configured ComponentSearchCriteria
     */
    public ComponentSearchCriteria build() {
      return new ComponentSearchCriteria(this);
    }
  }
}
