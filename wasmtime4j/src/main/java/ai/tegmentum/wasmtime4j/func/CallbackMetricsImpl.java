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
package ai.tegmentum.wasmtime4j.func;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Implementation of {@link CallbackRegistry.CallbackMetrics}.
 *
 * <p>Thread-safe metrics tracker using atomic counters.
 *
 * @since 1.0.0
 */
public class CallbackMetricsImpl implements CallbackRegistry.CallbackMetrics {

  private final AtomicLong totalInvocations = new AtomicLong(0);
  private final AtomicLong totalExecutionTimeNanos = new AtomicLong(0);
  private final AtomicLong failureCount = new AtomicLong(0);
  private final AtomicLong timeoutCount = new AtomicLong(0);

  /**
   * Records a successful invocation.
   *
   * @param executionTimeNanos the execution time in nanoseconds
   */
  public void recordInvocation(final long executionTimeNanos) {
    totalInvocations.incrementAndGet();
    totalExecutionTimeNanos.addAndGet(executionTimeNanos);
  }

  /**
   * Records a failed invocation.
   *
   * @param executionTimeNanos the execution time in nanoseconds
   */
  public void recordFailure(final long executionTimeNanos) {
    totalInvocations.incrementAndGet();
    totalExecutionTimeNanos.addAndGet(executionTimeNanos);
    failureCount.incrementAndGet();
  }

  /** Records a timeout. */
  public void recordTimeout() {
    timeoutCount.incrementAndGet();
  }

  @Override
  public long getTotalInvocations() {
    return totalInvocations.get();
  }

  @Override
  public double getAverageExecutionTimeNanos() {
    final long invocations = totalInvocations.get();
    return invocations > 0 ? (double) totalExecutionTimeNanos.get() / invocations : 0.0;
  }

  @Override
  public long getTotalExecutionTimeNanos() {
    return totalExecutionTimeNanos.get();
  }

  @Override
  public long getFailureCount() {
    return failureCount.get();
  }

  @Override
  public long getTimeoutCount() {
    return timeoutCount.get();
  }
}
