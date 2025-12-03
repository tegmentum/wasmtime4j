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

package ai.tegmentum.wasmtime4j.jni.wasi.http;

import ai.tegmentum.wasmtime4j.wasi.http.WasiHttpStats;
import java.time.Duration;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * JNI implementation of {@link WasiHttpStats}.
 *
 * <p>This class provides statistics tracking for WASI HTTP operations using atomic counters for
 * thread safety.
 *
 * @since 1.0.0
 */
public final class JniWasiHttpStats implements WasiHttpStats {

  private final long contextHandle;
  private final AtomicLong totalRequests = new AtomicLong(0);
  private final AtomicLong successfulRequests = new AtomicLong(0);
  private final AtomicLong failedRequests = new AtomicLong(0);
  private final AtomicInteger activeRequests = new AtomicInteger(0);
  private final AtomicLong totalBytesSent = new AtomicLong(0);
  private final AtomicLong totalBytesReceived = new AtomicLong(0);
  private final AtomicLong totalDurationMs = new AtomicLong(0);
  private final AtomicLong minDurationMs = new AtomicLong(Long.MAX_VALUE);
  private final AtomicLong maxDurationMs = new AtomicLong(0);
  private final AtomicLong connectionTimeouts = new AtomicLong(0);
  private final AtomicLong readTimeouts = new AtomicLong(0);
  private final AtomicLong blockedRequests = new AtomicLong(0);
  private final AtomicLong bodySizeViolations = new AtomicLong(0);
  private final AtomicInteger activeConnections = new AtomicInteger(0);
  private final AtomicInteger idleConnections = new AtomicInteger(0);

  /**
   * Creates a new JniWasiHttpStats.
   *
   * @param contextHandle native handle to the WASI HTTP context
   */
  JniWasiHttpStats(final long contextHandle) {
    this.contextHandle = contextHandle;
  }

  @Override
  public long getTotalRequests() {
    return totalRequests.get();
  }

  @Override
  public long getSuccessfulRequests() {
    return successfulRequests.get();
  }

  @Override
  public long getFailedRequests() {
    return failedRequests.get();
  }

  @Override
  public int getActiveRequests() {
    return activeRequests.get();
  }

  @Override
  public long getTotalBytesSent() {
    return totalBytesSent.get();
  }

  @Override
  public long getTotalBytesReceived() {
    return totalBytesReceived.get();
  }

  @Override
  public Duration getAverageRequestDuration() {
    final long total = totalRequests.get();
    if (total == 0) {
      return Duration.ZERO;
    }
    return Duration.ofMillis(totalDurationMs.get() / total);
  }

  @Override
  public Duration getMinRequestDuration() {
    final long min = minDurationMs.get();
    if (min == Long.MAX_VALUE) {
      return Duration.ZERO;
    }
    return Duration.ofMillis(min);
  }

  @Override
  public Duration getMaxRequestDuration() {
    return Duration.ofMillis(maxDurationMs.get());
  }

  @Override
  public long getConnectionTimeouts() {
    return connectionTimeouts.get();
  }

  @Override
  public long getReadTimeouts() {
    return readTimeouts.get();
  }

  @Override
  public long getBlockedRequests() {
    return blockedRequests.get();
  }

  @Override
  public long getBodySizeLimitViolations() {
    return bodySizeViolations.get();
  }

  @Override
  public int getActiveConnections() {
    return activeConnections.get();
  }

  @Override
  public int getIdleConnections() {
    return idleConnections.get();
  }

  /**
   * Records the start of a new request.
   *
   * @return the request start time in milliseconds
   */
  public long recordRequestStart() {
    totalRequests.incrementAndGet();
    activeRequests.incrementAndGet();
    return System.currentTimeMillis();
  }

  /**
   * Records the successful completion of a request.
   *
   * @param startTimeMs the request start time in milliseconds
   * @param bytesSent bytes sent in the request
   * @param bytesReceived bytes received in the response
   */
  public void recordRequestSuccess(
      final long startTimeMs, final long bytesSent, final long bytesReceived) {
    final long duration = System.currentTimeMillis() - startTimeMs;
    successfulRequests.incrementAndGet();
    activeRequests.decrementAndGet();
    totalBytesSent.addAndGet(bytesSent);
    totalBytesReceived.addAndGet(bytesReceived);
    totalDurationMs.addAndGet(duration);
    updateMinDuration(duration);
    updateMaxDuration(duration);
  }

  /**
   * Records the failure of a request.
   *
   * @param startTimeMs the request start time in milliseconds
   */
  public void recordRequestFailure(final long startTimeMs) {
    final long duration = System.currentTimeMillis() - startTimeMs;
    failedRequests.incrementAndGet();
    activeRequests.decrementAndGet();
    totalDurationMs.addAndGet(duration);
    updateMinDuration(duration);
    updateMaxDuration(duration);
  }

  /** Records a connection timeout. */
  public void recordConnectionTimeout() {
    connectionTimeouts.incrementAndGet();
  }

  /** Records a read timeout. */
  public void recordReadTimeout() {
    readTimeouts.incrementAndGet();
  }

  /** Records a blocked request (due to host restrictions). */
  public void recordBlockedRequest() {
    blockedRequests.incrementAndGet();
  }

  /** Records a body size limit violation. */
  public void recordBodySizeViolation() {
    bodySizeViolations.incrementAndGet();
  }

  /**
   * Updates the active connection count.
   *
   * @param active the number of active connections
   */
  public void setActiveConnections(final int active) {
    activeConnections.set(active);
  }

  /**
   * Updates the idle connection count.
   *
   * @param idle the number of idle connections
   */
  public void setIdleConnections(final int idle) {
    idleConnections.set(idle);
  }

  /** Resets all statistics counters. */
  void reset() {
    totalRequests.set(0);
    successfulRequests.set(0);
    failedRequests.set(0);
    activeRequests.set(0);
    totalBytesSent.set(0);
    totalBytesReceived.set(0);
    totalDurationMs.set(0);
    minDurationMs.set(Long.MAX_VALUE);
    maxDurationMs.set(0);
    connectionTimeouts.set(0);
    readTimeouts.set(0);
    blockedRequests.set(0);
    bodySizeViolations.set(0);
    activeConnections.set(0);
    idleConnections.set(0);
  }

  private void updateMinDuration(final long duration) {
    long current;
    do {
      current = minDurationMs.get();
      if (duration >= current) {
        return;
      }
    } while (!minDurationMs.compareAndSet(current, duration));
  }

  private void updateMaxDuration(final long duration) {
    long current;
    do {
      current = maxDurationMs.get();
      if (duration <= current) {
        return;
      }
    } while (!maxDurationMs.compareAndSet(current, duration));
  }

  /**
   * Returns the native context handle.
   *
   * @return the context handle
   */
  long getContextHandle() {
    return contextHandle;
  }

  @Override
  public String toString() {
    return "JniWasiHttpStats{"
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
