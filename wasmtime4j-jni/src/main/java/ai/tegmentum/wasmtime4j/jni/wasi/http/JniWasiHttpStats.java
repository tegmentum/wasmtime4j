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

/**
 * JNI implementation of {@link WasiHttpStats}.
 *
 * <p>This is a point-in-time snapshot that eagerly captures all values from the native layer at
 * construction time.
 */
final class JniWasiHttpStats implements WasiHttpStats {

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

  JniWasiHttpStats(final long contextHandle) {
    this.totalRequests = JniWasiHttpContext.nativeStatsTotalRequests(contextHandle);
    this.successfulRequests = JniWasiHttpContext.nativeStatsSuccessfulRequests(contextHandle);
    this.failedRequests = JniWasiHttpContext.nativeStatsFailedRequests(contextHandle);
    this.activeRequests = JniWasiHttpContext.nativeStatsActiveRequests(contextHandle);
    this.bytesSent = JniWasiHttpContext.nativeStatsBytesSent(contextHandle);
    this.bytesReceived = JniWasiHttpContext.nativeStatsBytesReceived(contextHandle);
    this.connectionTimeouts = JniWasiHttpContext.nativeStatsConnectionTimeouts(contextHandle);
    this.readTimeouts = JniWasiHttpContext.nativeStatsReadTimeouts(contextHandle);
    this.blockedRequests = JniWasiHttpContext.nativeStatsBlockedRequests(contextHandle);
    this.bodySizeViolations = JniWasiHttpContext.nativeStatsBodySizeViolations(contextHandle);
    this.activeConnections = JniWasiHttpContext.nativeStatsActiveConnections(contextHandle);
    this.idleConnections = JniWasiHttpContext.nativeStatsIdleConnections(contextHandle);
    this.avgDurationMs = JniWasiHttpContext.nativeStatsAvgDurationMs(contextHandle);
    this.minDurationMs = JniWasiHttpContext.nativeStatsMinDurationMs(contextHandle);
    this.maxDurationMs = JniWasiHttpContext.nativeStatsMaxDurationMs(contextHandle);
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
