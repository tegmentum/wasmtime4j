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

package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.profiler.Profiler;
import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

/**
 * JNI implementation of the Profiler interface.
 *
 * <p>This implementation uses JNI to call native profiler functions.
 *
 * @since 1.0.0
 */
public final class JniProfiler implements Profiler {

  private static final Logger LOGGER = Logger.getLogger(JniProfiler.class.getName());

  private final long profilerPtr;
  private final AtomicBoolean closed = new AtomicBoolean(false);

  // Native methods
  private static native long nativeProfilerCreate();

  private static native boolean nativeProfilerStart(long profilerPtr);

  private static native boolean nativeProfilerStop(long profilerPtr);

  private static native boolean nativeProfilerIsProfiling(long profilerPtr);

  private static native boolean nativeProfilerRecordFunction(
      long profilerPtr, String functionName, long executionTimeNanos, long memoryDelta);

  private static native boolean nativeProfilerRecordCompilation(
      long profilerPtr,
      long compilationTimeNanos,
      long bytecodeSize,
      boolean cached,
      boolean optimized);

  private static native long nativeProfilerGetModulesCompiled(long profilerPtr);

  private static native long nativeProfilerGetTotalCompilationTimeNanos(long profilerPtr);

  private static native long nativeProfilerGetAverageCompilationTimeNanos(long profilerPtr);

  private static native long nativeProfilerGetBytesCompiled(long profilerPtr);

  private static native long nativeProfilerGetCacheHits(long profilerPtr);

  private static native long nativeProfilerGetCacheMisses(long profilerPtr);

  private static native long nativeProfilerGetOptimizedModules(long profilerPtr);

  private static native long nativeProfilerGetCurrentMemoryBytes(long profilerPtr);

  private static native long nativeProfilerGetPeakMemoryBytes(long profilerPtr);

  private static native long nativeProfilerGetUptimeNanos(long profilerPtr);

  private static native double nativeProfilerGetFunctionCallsPerSecond(long profilerPtr);

  private static native long nativeProfilerGetTotalFunctionCalls(long profilerPtr);

  private static native long nativeProfilerGetTotalExecutionTimeNanos(long profilerPtr);

  private static native boolean nativeProfilerReset(long profilerPtr);

  private static native void nativeProfilerDestroy(long profilerPtr);

  static {
    NativeLibraryLoader.loadLibrary();
  }

  /**
   * Creates a new JniProfiler.
   *
   * @throws WasmException if the profiler cannot be created
   */
  public JniProfiler() throws WasmException {
    this.profilerPtr = nativeProfilerCreate();

    if (this.profilerPtr == 0) {
      throw new WasmException("Failed to create native profiler");
    }

    LOGGER.fine("Created JniProfiler");
  }

  @Override
  public void startProfiling() throws WasmException {
    ensureOpen();
    if (!nativeProfilerStart(profilerPtr)) {
      throw new WasmException("Failed to start profiling");
    }
    LOGGER.fine("Started profiling");
  }

  @Override
  public void stopProfiling() throws WasmException {
    ensureOpen();
    if (!nativeProfilerStop(profilerPtr)) {
      throw new WasmException("Failed to stop profiling");
    }
    LOGGER.fine("Stopped profiling");
  }

  @Override
  public boolean isProfiling() {
    if (closed.get()) {
      return false;
    }
    return nativeProfilerIsProfiling(profilerPtr);
  }

  @Override
  public void recordFunctionExecution(
      final String functionName, final Duration executionTime, final long memoryDelta)
      throws WasmException {
    ensureOpen();
    Objects.requireNonNull(functionName, "functionName cannot be null");
    Objects.requireNonNull(executionTime, "executionTime cannot be null");

    if (!nativeProfilerRecordFunction(
        profilerPtr, functionName, executionTime.toNanos(), memoryDelta)) {
      throw new WasmException("Failed to record function execution");
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

    if (!nativeProfilerRecordCompilation(
        profilerPtr, compilationTime.toNanos(), bytecodeSize, cached, optimized)) {
      throw new WasmException("Failed to record compilation");
    }
  }

  @Override
  public long getModulesCompiled() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetModulesCompiled(profilerPtr);
  }

  @Override
  public Duration getTotalCompilationTime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(nativeProfilerGetTotalCompilationTimeNanos(profilerPtr));
  }

  @Override
  public Duration getAverageCompilationTime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(nativeProfilerGetAverageCompilationTimeNanos(profilerPtr));
  }

  @Override
  public long getBytesCompiled() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetBytesCompiled(profilerPtr);
  }

  @Override
  public long getCacheHits() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetCacheHits(profilerPtr);
  }

  @Override
  public long getCacheMisses() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetCacheMisses(profilerPtr);
  }

  @Override
  public long getOptimizedModules() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetOptimizedModules(profilerPtr);
  }

  @Override
  public long getCurrentMemoryBytes() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetCurrentMemoryBytes(profilerPtr);
  }

  @Override
  public long getPeakMemoryBytes() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetPeakMemoryBytes(profilerPtr);
  }

  @Override
  public Duration getUptime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(nativeProfilerGetUptimeNanos(profilerPtr));
  }

  @Override
  public double getFunctionCallsPerSecond() {
    if (closed.get()) {
      return 0.0;
    }
    return nativeProfilerGetFunctionCallsPerSecond(profilerPtr);
  }

  @Override
  public long getTotalFunctionCalls() {
    if (closed.get()) {
      return 0;
    }
    return nativeProfilerGetTotalFunctionCalls(profilerPtr);
  }

  @Override
  public Duration getTotalExecutionTime() {
    if (closed.get()) {
      return Duration.ZERO;
    }
    return Duration.ofNanos(nativeProfilerGetTotalExecutionTimeNanos(profilerPtr));
  }

  @Override
  public void reset() throws WasmException {
    ensureOpen();
    if (!nativeProfilerReset(profilerPtr)) {
      throw new WasmException("Failed to reset profiler");
    }
    LOGGER.fine("Reset profiler");
  }

  @Override
  public void close() throws WasmException {
    if (closed.compareAndSet(false, true)) {
      nativeProfilerDestroy(profilerPtr);
      LOGGER.fine("Closed JniProfiler");
    }
  }

  private void ensureOpen() throws WasmException {
    if (closed.get()) {
      throw new WasmException("Profiler has been closed");
    }
  }

  @Override
  public String toString() {
    return "JniProfiler{closed=" + closed.get() + "}";
  }
}
