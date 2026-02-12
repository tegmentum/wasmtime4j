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
import java.util.Map;

/**
 * Panama implementation of GC profiler.
 *
 * @since 1.0.0
 */
final class PanamaGcProfiler implements GcProfiler {

  private long startTime;
  private volatile boolean active = false;

  @Override
  public void start() {
    startTime = System.currentTimeMillis();
    active = true;
  }

  @Override
  public GcProfilingResults stop() {
    active = false;
    final long duration = System.currentTimeMillis() - startTime;
    return new DefaultGcProfilingResults(duration);
  }

  @Override
  public boolean isActive() {
    return active;
  }

  @Override
  public Duration getProfilingDuration() {
    if (!active) {
      return Duration.ZERO;
    }
    return Duration.ofMillis(System.currentTimeMillis() - startTime);
  }

  @Override
  public void recordEvent(
      final String eventName, final Duration duration, final Map<String, Object> metadata) {
    // No-op - stub implementation
  }
}
