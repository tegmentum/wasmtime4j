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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.gc.GcProfiler;
import java.time.Duration;
import java.util.Collections;
import java.util.List;

/**
 * Default implementation of GC profiling results.
 *
 * @since 1.0.0
 */
final class DefaultGcProfilingResults implements GcProfiler.GcProfilingResults {

  private final long durationMs;

  DefaultGcProfilingResults(final long durationMs) {
    this.durationMs = durationMs;
  }

  @Override
  public Duration getTotalDuration() {
    return Duration.ofMillis(durationMs);
  }

  @Override
  public long getSampleCount() {
    return 0;
  }

  @Override
  public GcProfiler.AllocationStatistics getAllocationStatistics() {
    return null;
  }

  @Override
  public GcProfiler.FieldAccessStatistics getFieldAccessStatistics() {
    return null;
  }

  @Override
  public GcProfiler.ArrayAccessStatistics getArrayAccessStatistics() {
    return null;
  }

  @Override
  public GcProfiler.ReferenceOperationStatistics getReferenceOperationStatistics() {
    return null;
  }

  @Override
  public GcProfiler.GcPerformanceStatistics getGcPerformanceStatistics() {
    return null;
  }

  @Override
  public GcProfiler.TypeOperationStatistics getTypeOperationStatistics() {
    return null;
  }

  @Override
  public List<GcProfiler.PerformanceHotspot> getHotspots() {
    return Collections.emptyList();
  }

  @Override
  public GcProfiler.PerformanceComparison getBaselineComparison() {
    return null;
  }

  @Override
  public GcProfiler.ProfilingTimeline getTimeline() {
    return null;
  }
}
