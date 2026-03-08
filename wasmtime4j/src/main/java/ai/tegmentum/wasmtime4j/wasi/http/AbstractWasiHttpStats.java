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
package ai.tegmentum.wasmtime4j.wasi.http;

/**
 * Abstract base class for {@link WasiHttpStats} implementations.
 *
 * <p>This class holds all 15 statistics fields and provides the complete getter implementations and
 * {@code toString()}. Subclasses only need to provide a constructor that fetches values from their
 * respective native layer.
 *
 * @since 1.0.0
 */
public abstract class AbstractWasiHttpStats implements WasiHttpStats {

  private final long totalRequests;
  private final long successfulRequests;
  private final long failedRequests;
  private final int activeRequests;
  private final long bytesSent;
  private final long bytesReceived;
  private final long connectionTimeouts;
  private final long readTimeouts;
  private final long blockedRequests;
  private final long bodySizeViolations;
  private final int activeConnections;
  private final int idleConnections;
  private final long avgDurationMs;
  private final long minDurationMs;
  private final long maxDurationMs;

  /**
   * Creates a new AbstractWasiHttpStats with the given values.
   *
   * @param totalRequests total number of HTTP requests
   * @param successfulRequests number of successful requests
   * @param failedRequests number of failed requests
   * @param activeRequests number of currently active requests
   * @param bytesSent total bytes sent
   * @param bytesReceived total bytes received
   * @param connectionTimeouts number of connection timeouts
   * @param readTimeouts number of read timeouts
   * @param blockedRequests number of blocked requests
   * @param bodySizeViolations number of body size violations
   * @param activeConnections number of active connections
   * @param idleConnections number of idle connections
   * @param avgDurationMs average request duration in milliseconds
   * @param minDurationMs minimum request duration in milliseconds
   * @param maxDurationMs maximum request duration in milliseconds
   */
  @SuppressWarnings("ParameterNumber")
  protected AbstractWasiHttpStats(
      final long totalRequests,
      final long successfulRequests,
      final long failedRequests,
      final int activeRequests,
      final long bytesSent,
      final long bytesReceived,
      final long connectionTimeouts,
      final long readTimeouts,
      final long blockedRequests,
      final long bodySizeViolations,
      final int activeConnections,
      final int idleConnections,
      final long avgDurationMs,
      final long minDurationMs,
      final long maxDurationMs) {
    this.totalRequests = totalRequests;
    this.successfulRequests = successfulRequests;
    this.failedRequests = failedRequests;
    this.activeRequests = activeRequests;
    this.bytesSent = bytesSent;
    this.bytesReceived = bytesReceived;
    this.connectionTimeouts = connectionTimeouts;
    this.readTimeouts = readTimeouts;
    this.blockedRequests = blockedRequests;
    this.bodySizeViolations = bodySizeViolations;
    this.activeConnections = activeConnections;
    this.idleConnections = idleConnections;
    this.avgDurationMs = avgDurationMs;
    this.minDurationMs = minDurationMs;
    this.maxDurationMs = maxDurationMs;
  }

  @Override
  public long totalRequests() {
    return totalRequests;
  }

  @Override
  public long successfulRequests() {
    return successfulRequests;
  }

  @Override
  public long failedRequests() {
    return failedRequests;
  }

  @Override
  public int activeRequests() {
    return activeRequests;
  }

  @Override
  public long bytesSent() {
    return bytesSent;
  }

  @Override
  public long bytesReceived() {
    return bytesReceived;
  }

  @Override
  public long connectionTimeouts() {
    return connectionTimeouts;
  }

  @Override
  public long readTimeouts() {
    return readTimeouts;
  }

  @Override
  public long blockedRequests() {
    return blockedRequests;
  }

  @Override
  public long bodySizeViolations() {
    return bodySizeViolations;
  }

  @Override
  public int activeConnections() {
    return activeConnections;
  }

  @Override
  public int idleConnections() {
    return idleConnections;
  }

  @Override
  public long avgDurationMs() {
    return avgDurationMs;
  }

  @Override
  public long minDurationMs() {
    return minDurationMs;
  }

  @Override
  public long maxDurationMs() {
    return maxDurationMs;
  }

  @Override
  public String toString() {
    return "WasiHttpStats{"
        + "totalRequests="
        + totalRequests
        + ", successfulRequests="
        + successfulRequests
        + ", failedRequests="
        + failedRequests
        + ", activeRequests="
        + activeRequests
        + ", bytesSent="
        + bytesSent
        + ", bytesReceived="
        + bytesReceived
        + ", connectionTimeouts="
        + connectionTimeouts
        + ", readTimeouts="
        + readTimeouts
        + ", blockedRequests="
        + blockedRequests
        + ", bodySizeViolations="
        + bodySizeViolations
        + ", activeConnections="
        + activeConnections
        + ", idleConnections="
        + idleConnections
        + ", avgDurationMs="
        + avgDurationMs
        + ", minDurationMs="
        + minDurationMs
        + ", maxDurationMs="
        + maxDurationMs
        + '}';
  }
}
