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
 * Snapshot of WASI HTTP statistics for a {@link WasiHttpContext}.
 *
 * <p>All values are point-in-time snapshots captured when {@link WasiHttpContext#getStats()} is
 * called. Counter values (total, successful, failed, etc.) are cumulative since context creation or
 * the last call to {@link WasiHttpContext#resetStats()}. Gauge values (active requests, active
 * connections, idle connections) reflect current state and are not affected by reset.
 *
 * @since 1.0.0
 */
public interface WasiHttpStats {

  /**
   * Returns the total number of HTTP requests made through this context.
   *
   * @return total request count
   */
  long totalRequests();

  /**
   * Returns the number of HTTP requests that completed successfully.
   *
   * @return successful request count
   */
  long successfulRequests();

  /**
   * Returns the number of HTTP requests that failed.
   *
   * @return failed request count
   */
  long failedRequests();

  /**
   * Returns the number of HTTP requests currently in flight.
   *
   * @return active request count
   */
  int activeRequests();

  /**
   * Returns the total number of bytes sent across all requests.
   *
   * @return total bytes sent
   */
  long bytesSent();

  /**
   * Returns the total number of bytes received across all responses.
   *
   * @return total bytes received
   */
  long bytesReceived();

  /**
   * Returns the number of connection timeouts encountered.
   *
   * @return connection timeout count
   */
  long connectionTimeouts();

  /**
   * Returns the number of read timeouts encountered.
   *
   * @return read timeout count
   */
  long readTimeouts();

  /**
   * Returns the number of requests blocked by security policy.
   *
   * @return blocked request count
   */
  long blockedRequests();

  /**
   * Returns the number of requests or responses that violated body size limits.
   *
   * @return body size violation count
   */
  long bodySizeViolations();

  /**
   * Returns the number of currently active connections.
   *
   * @return active connection count
   */
  int activeConnections();

  /**
   * Returns the number of currently idle connections in the pool.
   *
   * @return idle connection count
   */
  int idleConnections();

  /**
   * Returns the average request duration in milliseconds.
   *
   * <p>Returns 0 if no requests have been completed.
   *
   * @return average duration in milliseconds
   */
  long avgDurationMs();

  /**
   * Returns the minimum request duration in milliseconds.
   *
   * <p>Returns 0 if no requests have been completed.
   *
   * @return minimum duration in milliseconds
   */
  long minDurationMs();

  /**
   * Returns the maximum request duration in milliseconds.
   *
   * <p>Returns 0 if no requests have been completed.
   *
   * @return maximum duration in milliseconds
   */
  long maxDurationMs();
}
