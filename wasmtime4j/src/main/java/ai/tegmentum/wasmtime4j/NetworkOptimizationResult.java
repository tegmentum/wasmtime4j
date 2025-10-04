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
 * Result of network optimization operations.
 *
 * <p>This class contains metrics and outcomes from network optimization attempts.
 *
 * @since 1.0.0
 */
public final class NetworkOptimizationResult {

  private final boolean successful;
  private final long bytesReduced;
  private final Duration timeSaved;
  private final String optimizationStrategy;
  private final String errorMessage;

  /**
   * Creates a successful network optimization result.
   *
   * @param bytesReduced the number of bytes saved
   * @param timeSaved the time saved
   * @param optimizationStrategy the strategy used
   */
  public NetworkOptimizationResult(
      final long bytesReduced, final Duration timeSaved, final String optimizationStrategy) {
    this(true, bytesReduced, timeSaved, optimizationStrategy, null);
  }

  /**
   * Creates a network optimization result.
   *
   * @param successful whether optimization succeeded
   * @param bytesReduced the number of bytes saved
   * @param timeSaved the time saved
   * @param optimizationStrategy the strategy used
   * @param errorMessage error message if failed
   */
  public NetworkOptimizationResult(
      final boolean successful,
      final long bytesReduced,
      final Duration timeSaved,
      final String optimizationStrategy,
      final String errorMessage) {
    this.successful = successful;
    this.bytesReduced = bytesReduced;
    this.timeSaved = Objects.requireNonNull(timeSaved, "timeSaved cannot be null");
    this.optimizationStrategy =
        Objects.requireNonNull(optimizationStrategy, "optimizationStrategy cannot be null");
    this.errorMessage = errorMessage;
  }

  /**
   * Checks if optimization was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the number of bytes reduced.
   *
   * @return bytes reduced
   */
  public long getBytesReduced() {
    return bytesReduced;
  }

  /**
   * Gets the time saved.
   *
   * @return time saved
   */
  public Duration getTimeSaved() {
    return timeSaved;
  }

  /**
   * Gets the optimization strategy used.
   *
   * @return optimization strategy
   */
  public String getOptimizationStrategy() {
    return optimizationStrategy;
  }

  /**
   * Gets the error message if optimization failed.
   *
   * @return error message, or null if successful
   */
  public String getErrorMessage() {
    return errorMessage;
  }

  /**
   * Creates a failed optimization result.
   *
   * @param errorMessage the error message
   * @return failed result
   */
  public static NetworkOptimizationResult failure(final String errorMessage) {
    return new NetworkOptimizationResult(
        false, 0, Duration.ZERO, "none", Objects.requireNonNull(errorMessage));
  }
}
