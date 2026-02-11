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

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Statistics and metrics for a ComponentRegistry.
 *
 * <p>This class provides comprehensive statistics about a component registry's state, including
 * component counts, memory usage, performance metrics, and operational statistics.
 *
 * @since 1.0.0
 */
public final class ComponentRegistryStatistics {

  private final int totalComponents;
  private final int activeComponents;
  private final int inactiveComponents;
  private final long totalMemoryUsage;
  private final long averageComponentSize;
  private final Set<String> availableInterfaces;
  private final Map<String, Integer> componentsByAuthor;
  private final Map<String, Integer> componentsByTag;
  private final Instant lastRegistration;
  private final Instant lastUnregistration;
  private final long totalRegistrations;
  private final long totalUnregistrations;
  private final double averageQueryTime;
  private final long totalQueries;

  private ComponentRegistryStatistics(Builder builder) {
    this.totalComponents = builder.totalComponents;
    this.activeComponents = builder.activeComponents;
    this.inactiveComponents = builder.inactiveComponents;
    this.totalMemoryUsage = builder.totalMemoryUsage;
    this.averageComponentSize = builder.averageComponentSize;
    this.availableInterfaces = Set.copyOf(builder.availableInterfaces);
    this.componentsByAuthor = Map.copyOf(builder.componentsByAuthor);
    this.componentsByTag = Map.copyOf(builder.componentsByTag);
    this.lastRegistration = builder.lastRegistration;
    this.lastUnregistration = builder.lastUnregistration;
    this.totalRegistrations = builder.totalRegistrations;
    this.totalUnregistrations = builder.totalUnregistrations;
    this.averageQueryTime = builder.averageQueryTime;
    this.totalQueries = builder.totalQueries;
  }

  /**
   * Gets the total number of registered components.
   *
   * @return the total component count
   */
  public int getTotalComponents() {
    return totalComponents;
  }

  /**
   * Gets the number of active components.
   *
   * @return the active component count
   */
  public int getActiveComponents() {
    return activeComponents;
  }

  /**
   * Gets the number of inactive components.
   *
   * @return the inactive component count
   */
  public int getInactiveComponents() {
    return inactiveComponents;
  }

  /**
   * Gets the total memory usage of all registered components.
   *
   * @return the total memory usage in bytes
   */
  public long getTotalMemoryUsage() {
    return totalMemoryUsage;
  }

  /**
   * Gets the average component size.
   *
   * @return the average component size in bytes
   */
  public long getAverageComponentSize() {
    return averageComponentSize;
  }

  /**
   * Gets all available interfaces across registered components.
   *
   * @return the set of available interface names
   */
  public Set<String> getAvailableInterfaces() {
    return availableInterfaces;
  }

  /**
   * Gets component counts grouped by author.
   *
   * @return map of author name to component count
   */
  public Map<String, Integer> getComponentsByAuthor() {
    return componentsByAuthor;
  }

  /**
   * Gets component counts grouped by tag.
   *
   * @return map of tag name to component count
   */
  public Map<String, Integer> getComponentsByTag() {
    return componentsByTag;
  }

  /**
   * Gets the timestamp of the last component registration.
   *
   * @return the last registration timestamp, or null if no registrations
   */
  public Instant getLastRegistration() {
    return lastRegistration;
  }

  /**
   * Gets the timestamp of the last component unregistration.
   *
   * @return the last unregistration timestamp, or null if no unregistrations
   */
  public Instant getLastUnregistration() {
    return lastUnregistration;
  }

  /**
   * Gets the total number of component registrations performed.
   *
   * @return the total registration count
   */
  public long getTotalRegistrations() {
    return totalRegistrations;
  }

  /**
   * Gets the total number of component unregistrations performed.
   *
   * @return the total unregistration count
   */
  public long getTotalUnregistrations() {
    return totalUnregistrations;
  }

  /**
   * Gets the average query execution time.
   *
   * @return the average query time in milliseconds
   */
  public double getAverageQueryTime() {
    return averageQueryTime;
  }

  /**
   * Gets the total number of queries performed.
   *
   * @return the total query count
   */
  public long getTotalQueries() {
    return totalQueries;
  }

  /**
   * Creates a new builder for ComponentRegistryStatistics.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for ComponentRegistryStatistics. */
  public static final class Builder {
    private int totalComponents = 0;
    private int activeComponents = 0;
    private int inactiveComponents = 0;
    private long totalMemoryUsage = 0;
    private long averageComponentSize = 0;
    private Set<String> availableInterfaces = Set.of();
    private Map<String, Integer> componentsByAuthor = Map.of();
    private Map<String, Integer> componentsByTag = Map.of();
    private Instant lastRegistration = null;
    private Instant lastUnregistration = null;
    private long totalRegistrations = 0;
    private long totalUnregistrations = 0;
    private double averageQueryTime = 0.0;
    private long totalQueries = 0;

    private Builder() {}

    /**
     * Sets the total number of registered components.
     *
     * @param totalComponents the total component count
     * @return this builder
     */
    public Builder totalComponents(int totalComponents) {
      this.totalComponents = totalComponents;
      return this;
    }

    /**
     * Sets the number of active components.
     *
     * @param activeComponents the active component count
     * @return this builder
     */
    public Builder activeComponents(int activeComponents) {
      this.activeComponents = activeComponents;
      return this;
    }

    /**
     * Sets the number of inactive components.
     *
     * @param inactiveComponents the inactive component count
     * @return this builder
     */
    public Builder inactiveComponents(int inactiveComponents) {
      this.inactiveComponents = inactiveComponents;
      return this;
    }

    /**
     * Sets the total memory usage of all registered components.
     *
     * @param totalMemoryUsage the total memory usage in bytes
     * @return this builder
     */
    public Builder totalMemoryUsage(long totalMemoryUsage) {
      this.totalMemoryUsage = totalMemoryUsage;
      return this;
    }

    /**
     * Sets the average component size.
     *
     * @param averageComponentSize the average component size in bytes
     * @return this builder
     */
    public Builder averageComponentSize(long averageComponentSize) {
      this.averageComponentSize = averageComponentSize;
      return this;
    }

    /**
     * Sets all available interfaces across registered components.
     *
     * @param availableInterfaces the set of available interface names
     * @return this builder
     */
    public Builder availableInterfaces(Set<String> availableInterfaces) {
      this.availableInterfaces = availableInterfaces != null ? availableInterfaces : Set.of();
      return this;
    }

    /**
     * Sets component counts grouped by author.
     *
     * @param componentsByAuthor map of author name to component count
     * @return this builder
     */
    public Builder componentsByAuthor(Map<String, Integer> componentsByAuthor) {
      this.componentsByAuthor = componentsByAuthor != null ? componentsByAuthor : Map.of();
      return this;
    }

    /**
     * Sets component counts grouped by tag.
     *
     * @param componentsByTag map of tag name to component count
     * @return this builder
     */
    public Builder componentsByTag(Map<String, Integer> componentsByTag) {
      this.componentsByTag = componentsByTag != null ? componentsByTag : Map.of();
      return this;
    }

    /**
     * Sets the timestamp of the last component registration.
     *
     * @param lastRegistration the last registration timestamp
     * @return this builder
     */
    public Builder lastRegistration(Instant lastRegistration) {
      this.lastRegistration = lastRegistration;
      return this;
    }

    /**
     * Sets the timestamp of the last component unregistration.
     *
     * @param lastUnregistration the last unregistration timestamp
     * @return this builder
     */
    public Builder lastUnregistration(Instant lastUnregistration) {
      this.lastUnregistration = lastUnregistration;
      return this;
    }

    /**
     * Sets the total number of component registrations performed.
     *
     * @param totalRegistrations the total registration count
     * @return this builder
     */
    public Builder totalRegistrations(long totalRegistrations) {
      this.totalRegistrations = totalRegistrations;
      return this;
    }

    /**
     * Sets the total number of component unregistrations performed.
     *
     * @param totalUnregistrations the total unregistration count
     * @return this builder
     */
    public Builder totalUnregistrations(long totalUnregistrations) {
      this.totalUnregistrations = totalUnregistrations;
      return this;
    }

    /**
     * Sets the average query execution time.
     *
     * @param averageQueryTime the average query time in milliseconds
     * @return this builder
     */
    public Builder averageQueryTime(double averageQueryTime) {
      this.averageQueryTime = averageQueryTime;
      return this;
    }

    /**
     * Sets the total number of queries performed.
     *
     * @param totalQueries the total query count
     * @return this builder
     */
    public Builder totalQueries(long totalQueries) {
      this.totalQueries = totalQueries;
      return this;
    }

    /**
     * Builds the ComponentRegistryStatistics.
     *
     * @return the configured ComponentRegistryStatistics
     */
    public ComponentRegistryStatistics build() {
      return new ComponentRegistryStatistics(this);
    }
  }
}
