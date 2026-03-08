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

import ai.tegmentum.wasmtime4j.wasi.http.AbstractWasiHttpStats;

/**
 * JNI implementation of WASI HTTP statistics.
 *
 * <p>This is a point-in-time snapshot that eagerly captures all values from the native layer at
 * construction time.
 */
final class JniWasiHttpStats extends AbstractWasiHttpStats {

  JniWasiHttpStats(final long contextHandle) {
    super(
        JniWasiHttpContext.nativeStatsTotalRequests(contextHandle),
        JniWasiHttpContext.nativeStatsSuccessfulRequests(contextHandle),
        JniWasiHttpContext.nativeStatsFailedRequests(contextHandle),
        JniWasiHttpContext.nativeStatsActiveRequests(contextHandle),
        JniWasiHttpContext.nativeStatsBytesSent(contextHandle),
        JniWasiHttpContext.nativeStatsBytesReceived(contextHandle),
        JniWasiHttpContext.nativeStatsConnectionTimeouts(contextHandle),
        JniWasiHttpContext.nativeStatsReadTimeouts(contextHandle),
        JniWasiHttpContext.nativeStatsBlockedRequests(contextHandle),
        JniWasiHttpContext.nativeStatsBodySizeViolations(contextHandle),
        JniWasiHttpContext.nativeStatsActiveConnections(contextHandle),
        JniWasiHttpContext.nativeStatsIdleConnections(contextHandle),
        JniWasiHttpContext.nativeStatsAvgDurationMs(contextHandle),
        JniWasiHttpContext.nativeStatsMinDurationMs(contextHandle),
        JniWasiHttpContext.nativeStatsMaxDurationMs(contextHandle));
  }
}
