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

package ai.tegmentum.wasmtime4j;

import java.time.Duration;
import java.util.Objects;

/**
 * Configuration for component pipeline processing.
 *
 * <p>This class defines settings for data flow through component pipelines, including buffer sizes,
 * timeouts, and processing strategies.
 *
 * @since 1.0.0
 */
public final class ComponentPipelineConfig {

  private final int maxStages;
  private final int bufferSize;
  private final Duration stageTimeout;
  private final boolean enableParallelProcessing;
  private final boolean enableBackpressure;
  private final int maxConcurrency;

  /**
   * Creates a new component pipeline configuration with default values.
   *
   * <p>Default values:
   *
   * <ul>
   *   <li>maxStages: 10
   *   <li>bufferSize: 100
   *   <li>stageTimeout: 60 seconds
   *   <li>enableParallelProcessing: true
   *   <li>enableBackpressure: true
   *   <li>maxConcurrency: 4
   * </ul>
   */
  public ComponentPipelineConfig() {
    this(10, 100, Duration.ofSeconds(60), true, true, 4);
  }

  /**
   * Creates a new component pipeline configuration with specified values.
   *
   * @param maxStages maximum number of pipeline stages
   * @param bufferSize buffer size for each stage
   * @param stageTimeout timeout for stage processing
   * @param enableParallelProcessing whether to enable parallel processing
   * @param enableBackpressure whether to enable backpressure control
   * @param maxConcurrency maximum concurrent operations
   */
  public ComponentPipelineConfig(
      final int maxStages,
      final int bufferSize,
      final Duration stageTimeout,
      final boolean enableParallelProcessing,
      final boolean enableBackpressure,
      final int maxConcurrency) {
    if (maxStages <= 0) {
      throw new IllegalArgumentException("maxStages must be positive");
    }
    if (bufferSize <= 0) {
      throw new IllegalArgumentException("bufferSize must be positive");
    }
    if (maxConcurrency <= 0) {
      throw new IllegalArgumentException("maxConcurrency must be positive");
    }
    this.maxStages = maxStages;
    this.bufferSize = bufferSize;
    this.stageTimeout = Objects.requireNonNull(stageTimeout, "stageTimeout cannot be null");
    this.enableParallelProcessing = enableParallelProcessing;
    this.enableBackpressure = enableBackpressure;
    this.maxConcurrency = maxConcurrency;
  }

  /**
   * Gets the maximum number of pipeline stages.
   *
   * @return maximum stages
   */
  public int getMaxStages() {
    return maxStages;
  }

  /**
   * Gets the buffer size for each stage.
   *
   * @return buffer size
   */
  public int getBufferSize() {
    return bufferSize;
  }

  /**
   * Gets the stage processing timeout.
   *
   * @return stage timeout
   */
  public Duration getStageTimeout() {
    return stageTimeout;
  }

  /**
   * Checks if parallel processing is enabled.
   *
   * @return true if parallel processing is enabled
   */
  public boolean isParallelProcessingEnabled() {
    return enableParallelProcessing;
  }

  /**
   * Checks if backpressure control is enabled.
   *
   * @return true if backpressure is enabled
   */
  public boolean isBackpressureEnabled() {
    return enableBackpressure;
  }

  /**
   * Gets the maximum concurrent operations.
   *
   * @return maximum concurrency
   */
  public int getMaxConcurrency() {
    return maxConcurrency;
  }

  /**
   * Creates a new builder for component pipeline configuration.
   *
   * @return new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for component pipeline configuration. */
  public static final class Builder {
    private int maxStages = 10;
    private int bufferSize = 100;
    private Duration stageTimeout = Duration.ofSeconds(60);
    private boolean enableParallelProcessing = true;
    private boolean enableBackpressure = true;
    private int maxConcurrency = 4;

    /**
     * Sets the maximum number of pipeline stages.
     *
     * @param maxStages maximum stages
     * @return this builder
     */
    public Builder maxStages(final int maxStages) {
      this.maxStages = maxStages;
      return this;
    }

    /**
     * Sets the buffer size for each stage.
     *
     * @param bufferSize buffer size
     * @return this builder
     */
    public Builder bufferSize(final int bufferSize) {
      this.bufferSize = bufferSize;
      return this;
    }

    /**
     * Sets the stage processing timeout.
     *
     * @param stageTimeout stage timeout
     * @return this builder
     */
    public Builder stageTimeout(final Duration stageTimeout) {
      this.stageTimeout = stageTimeout;
      return this;
    }

    /**
     * Sets whether to enable parallel processing.
     *
     * @param enableParallelProcessing true to enable parallel processing
     * @return this builder
     */
    public Builder enableParallelProcessing(final boolean enableParallelProcessing) {
      this.enableParallelProcessing = enableParallelProcessing;
      return this;
    }

    /**
     * Sets whether to enable backpressure control.
     *
     * @param enableBackpressure true to enable backpressure
     * @return this builder
     */
    public Builder enableBackpressure(final boolean enableBackpressure) {
      this.enableBackpressure = enableBackpressure;
      return this;
    }

    /**
     * Sets the maximum concurrent operations.
     *
     * @param maxConcurrency maximum concurrency
     * @return this builder
     */
    public Builder maxConcurrency(final int maxConcurrency) {
      this.maxConcurrency = maxConcurrency;
      return this;
    }

    /**
     * Builds the component pipeline configuration.
     *
     * @return new configuration instance
     */
    public ComponentPipelineConfig build() {
      return new ComponentPipelineConfig(
          maxStages,
          bufferSize,
          stageTimeout,
          enableParallelProcessing,
          enableBackpressure,
          maxConcurrency);
    }
  }
}
