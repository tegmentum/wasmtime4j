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

package ai.tegmentum.wasmtime4j.panama.wasi.http;

import ai.tegmentum.wasmtime4j.panama.NativeFunctionBindings;
import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpStats;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.Objects;

/**
 * Panama implementation of {@link WasiHttpStats}.
 *
 * <p>This class provides live statistics from the native WASI HTTP context by querying the native
 * layer for each statistic value.
 */
public final class PanamaWasiHttpStats implements WasiHttpStats {

  private final MemorySegment contextPtr;
  private final NativeFunctionBindings bindings;

  /**
   * Creates a new PanamaWasiHttpStats.
   *
   * @param contextPtr pointer to the native WASI HTTP context
   * @param bindings the native function bindings
   * @throws NullPointerException if any argument is null
   */
  public PanamaWasiHttpStats(
      final MemorySegment contextPtr, final NativeFunctionBindings bindings) {
    this.contextPtr = Objects.requireNonNull(contextPtr, "contextPtr cannot be null");
    this.bindings = Objects.requireNonNull(bindings, "bindings cannot be null");
  }

  @Override
  public long getTotalRequests() {
    return bindings.wasiHttpContextStatsTotalRequests(contextPtr);
  }

  @Override
  public long getSuccessfulRequests() {
    return bindings.wasiHttpContextStatsSuccessfulRequests(contextPtr);
  }

  @Override
  public long getFailedRequests() {
    return bindings.wasiHttpContextStatsFailedRequests(contextPtr);
  }

  @Override
  public int getActiveRequests() {
    return bindings.wasiHttpContextStatsActiveRequests(contextPtr);
  }

  @Override
  public long getTotalBytesSent() {
    return bindings.wasiHttpContextStatsBytesSent(contextPtr);
  }

  @Override
  public long getTotalBytesReceived() {
    return bindings.wasiHttpContextStatsBytesReceived(contextPtr);
  }

  @Override
  public Duration getAverageRequestDuration() {
    final long avgMs = bindings.wasiHttpContextStatsAvgDurationMs(contextPtr);
    return Duration.ofMillis(avgMs);
  }

  @Override
  public Duration getMinRequestDuration() {
    final long minMs = bindings.wasiHttpContextStatsMinDurationMs(contextPtr);
    if (minMs == Long.MAX_VALUE) {
      return Duration.ZERO;
    }
    return Duration.ofMillis(minMs);
  }

  @Override
  public Duration getMaxRequestDuration() {
    final long maxMs = bindings.wasiHttpContextStatsMaxDurationMs(contextPtr);
    return Duration.ofMillis(maxMs);
  }

  @Override
  public long getConnectionTimeouts() {
    return bindings.wasiHttpContextStatsConnectionTimeouts(contextPtr);
  }

  @Override
  public long getReadTimeouts() {
    return bindings.wasiHttpContextStatsReadTimeouts(contextPtr);
  }

  @Override
  public long getBlockedRequests() {
    return bindings.wasiHttpContextStatsBlockedRequests(contextPtr);
  }

  @Override
  public long getBodySizeLimitViolations() {
    return bindings.wasiHttpContextStatsBodySizeViolations(contextPtr);
  }

  @Override
  public int getActiveConnections() {
    return bindings.wasiHttpContextStatsActiveConnections(contextPtr);
  }

  @Override
  public int getIdleConnections() {
    return bindings.wasiHttpContextStatsIdleConnections(contextPtr);
  }

  @Override
  public String toString() {
    return "PanamaWasiHttpStats{"
        + "totalRequests="
        + getTotalRequests()
        + ", successfulRequests="
        + getSuccessfulRequests()
        + ", failedRequests="
        + getFailedRequests()
        + ", activeRequests="
        + getActiveRequests()
        + ", bytesSent="
        + getTotalBytesSent()
        + ", bytesReceived="
        + getTotalBytesReceived()
        + '}';
  }
}
