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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.profiler.Profiler;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * Panama implementation of the Profiler interface.
 *
 * <p>This implementation uses Panama Foreign Function API to call native profiler functions.
 *
 * @since 1.0.0
 */
public final class PanamaProfiler implements Profiler {

  private static final Logger LOGGER = Logger.getLogger(PanamaProfiler.class.getName());

  private final NativeExecutionBindings bindings;
  private final Arena arena;
  private final MemorySegment profilerPtr;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  /**
   * Creates a new PanamaProfiler.
   *
   * @throws WasmException if the profiler cannot be created
   */
  public PanamaProfiler() throws WasmException {
    this.bindings = NativeExecutionBindings.getInstance();
    this.arena = Arena.ofShared();
    this.profilerPtr = bindings.profilerCreate();

    if (profilerPtr == null || profilerPtr.equals(MemorySegment.NULL)) {
      arena.close();
      throw new WasmException("Failed to create native profiler");
    }

    LOGGER.fine("Created PanamaProfiler");
  }

  @Override
  public void startProfiling() throws WasmException {
    ensureOpen();
    if (!bindings.profilerStart(profilerPtr)) {
      throw new WasmException("Failed to start profiling");
    }
    LOGGER.fine("Started profiling");
  }

  @Override
  public void stopProfiling() throws WasmException {
    ensureOpen();
    if (!bindings.profilerStop(profilerPtr)) {
      throw new WasmException("Failed to stop profiling");
    }
    LOGGER.fine("Stopped profiling");
  }

  @Override
  public boolean isProfiling() {
    if (closed.get()) {
      return false;
    }
    return bindings.profilerIsProfiling(profilerPtr);
  }

  @Override
  public void recordFunctionExecution(
      final String functionName, final Duration executionTime, final long memoryDelta)
      throws WasmException {
    ensureOpen();
    Objects.requireNonNull(functionName, "functionName cannot be null");
    Objects.requireNonNull(executionTime, "executionTime cannot be null");

    try (Arena localArena = Arena.ofConfined()) {
      final MemorySegment functionNamePtr =
          localArena.allocateFrom(functionName, java.nio.charset.StandardCharsets.UTF_8);
      if (!bindings.profilerRecordFunction(
          profilerPtr, functionNamePtr, executionTime.toNanos(), memoryDelta)) {
        throw new WasmException("Failed to record function execution");
      }
    }
  }

  @Override
  public void recordCompilation(
      final Duration compilationTime,
      final long bytecodeSize,
      final boolean cached,
      final boolean optimized)
      throws WasmException {
    ensureOpen();
    Objects.requireNonNull(compilationTime, "compilationTime cannot be null");

    if (!bindings.profilerRecordCompilation(
        profilerPtr, compilationTime.toNanos(), bytecodeSize, cached, optimized)) {
      throw new WasmException("Failed to record compilation");
    }
  }

  @Override
  public long getModulesCompiled() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetModulesCompiled(profilerPtr);
  }

  @Override
  public Duration getTotalCompilationTime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(bindings.profilerGetTotalCompilationTimeNanos(profilerPtr));
  }

  @Override
  public Duration getAverageCompilationTime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(bindings.profilerGetAverageCompilationTimeNanos(profilerPtr));
  }

  @Override
  public long getBytesCompiled() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetBytesCompiled(profilerPtr);
  }

  @Override
  public long getCacheHits() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetCacheHits(profilerPtr);
  }

  @Override
  public long getCacheMisses() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetCacheMisses(profilerPtr);
  }

  @Override
  public long getOptimizedModules() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetOptimizedModules(profilerPtr);
  }

  @Override
  public long getCurrentMemoryBytes() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetCurrentMemoryBytes(profilerPtr);
  }

  @Override
  public long getPeakMemoryBytes() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetPeakMemoryBytes(profilerPtr);
  }

  @Override
  public Duration getUptime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(bindings.profilerGetUptimeNanos(profilerPtr));
  }

  @Override
  public double getFunctionCallsPerSecond() {
    if (closed.get()) {
      return 0.0;
    }
    return bindings.profilerGetFunctionCallsPerSecond(profilerPtr);
  }

  @Override
  public long getTotalFunctionCalls() {
    if (closed.get()) {
      return 0;
    }
    return bindings.profilerGetTotalFunctionCalls(profilerPtr);
  }

  @Override
  public Duration getTotalExecutionTime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(bindings.profilerGetTotalExecutionTimeNanos(profilerPtr));
  }

  @Override
  public void reset() throws WasmException {
    ensureOpen();
    if (!bindings.profilerReset(profilerPtr)) {
      throw new WasmException("Failed to reset profiler");
    }
    LOGGER.fine("Reset profiler");
  }

  @Override
  public void close() throws WasmException {
    if (closed.compareAndSet(false, true)) {
      bindings.profilerDestroy(profilerPtr);
      arena.close();
      LOGGER.fine("Closed PanamaProfiler");
    }
  }

  private void ensureOpen() throws WasmException {
    if (closed.get()) {
      throw new WasmException("Profiler has been closed");
    }
  }

  @Override
  public String toString() {
    return "PanamaProfiler{closed=" + closed.get() + "}";
  }
}
