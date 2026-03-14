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
package ai.tegmentum.wasmtime4j.benchmarks;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.Blackhole;

/**
 * Benchmarks comparing ReentrantReadWriteLock vs AtomicInteger-based operation guards.
 *
 * <p>Every JNI function call goes through beginOperation()/endOperation() which currently
 * acquires/releases a ReentrantReadWriteLock read lock. This benchmark measures the overhead of the
 * current approach vs a lighter-weight AtomicInteger reference counting approach.
 */
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.MICROSECONDS)
@State(Scope.Thread)
@Warmup(iterations = 3, time = 2, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@Fork(2)
public class OperationGuardBenchmark {

  // Current approach: ReentrantReadWriteLock
  private ReentrantReadWriteLock rwLock;
  private AtomicBoolean closedRwl;

  // New approach: AtomicInteger reference counting
  private AtomicInteger operationCount;
  private volatile boolean closedAtomic;

  // Simulated work payload
  private long nativeHandle;

  @Setup
  public void setup() {
    rwLock = new ReentrantReadWriteLock();
    closedRwl = new AtomicBoolean(false);
    operationCount = new AtomicInteger(0);
    closedAtomic = false;
    nativeHandle = 0x1234567890L;
  }

  @Benchmark
  public long currentRwlGuard(Blackhole bh) {
    // Simulate current beginOperation/endOperation with RWL
    rwLock.readLock().lock();
    try {
      if (closedRwl.get()) {
        throw new IllegalStateException("closed");
      }
      // Simulated work: just return the handle
      return nativeHandle;
    } finally {
      rwLock.readLock().unlock();
    }
  }

  @Benchmark
  public long newAtomicGuard(Blackhole bh) {
    // Simulate new approach: increment first, then check
    operationCount.incrementAndGet();
    try {
      if (closedAtomic) {
        throw new IllegalStateException("closed");
      }
      // Simulated work: just return the handle
      return nativeHandle;
    } finally {
      operationCount.decrementAndGet();
    }
  }

  @Benchmark
  public long baselineNoGuard(Blackhole bh) {
    // Baseline: no guard at all
    return nativeHandle;
  }
}
