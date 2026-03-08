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
package ai.tegmentum.wasmtime4j.panama.wasi.http;

import ai.tegmentum.wasmtime4j.panama.NativeHttpBindings;
import ai.tegmentum.wasmtime4j.wasi.http.AbstractWasiHttpStats;
import java.lang.foreign.MemorySegment;

/**
 * Panama implementation of WASI HTTP statistics.
 *
 * <p>This is a point-in-time snapshot that eagerly captures all values from the native layer at
 * construction time.
 */
final class PanamaWasiHttpStats extends AbstractWasiHttpStats {

  PanamaWasiHttpStats(final NativeHttpBindings bindings, final MemorySegment contextPtr) {
    super(
        bindings.wasiHttpContextStatsTotalRequests(contextPtr),
        bindings.wasiHttpContextStatsSuccessfulRequests(contextPtr),
        bindings.wasiHttpContextStatsFailedRequests(contextPtr),
        bindings.wasiHttpContextStatsActiveRequests(contextPtr),
        bindings.wasiHttpContextStatsBytesSent(contextPtr),
        bindings.wasiHttpContextStatsBytesReceived(contextPtr),
        bindings.wasiHttpContextStatsConnectionTimeouts(contextPtr),
        bindings.wasiHttpContextStatsReadTimeouts(contextPtr),
        bindings.wasiHttpContextStatsBlockedRequests(contextPtr),
        bindings.wasiHttpContextStatsBodySizeViolations(contextPtr),
        bindings.wasiHttpContextStatsActiveConnections(contextPtr),
        bindings.wasiHttpContextStatsIdleConnections(contextPtr),
        bindings.wasiHttpContextStatsAvgDurationMs(contextPtr),
        bindings.wasiHttpContextStatsMinDurationMs(contextPtr),
        bindings.wasiHttpContextStatsMaxDurationMs(contextPtr));
  }
}
