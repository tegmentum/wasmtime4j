package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.EngineStatistics;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;

/**
 * JNI implementation of EngineStatistics providing runtime metrics for WebAssembly engines.
 *
 * <p>This class provides access to various performance and usage statistics from the native
 * Wasmtime engine through JNI calls. Statistics include compilation counts, cache performance, and
 * memory usage metrics.
 *
 * @since 1.0.0
 */
public final class JniEngineStatistics implements EngineStatistics {

  private final long engineHandle;

  /**
   * Creates statistics accessor for the given engine handle.
   *
   * @param engineHandle the native engine handle
   * @throws IllegalArgumentException if engineHandle is invalid
   */
  JniEngineStatistics(final long engineHandle) {
    JniValidation.requireValidHandle(engineHandle, "engineHandle");
    this.engineHandle = engineHandle;
  }

  @Override
  public long getCompiledModuleCount() {
    try {
      return nativeGetCompiledModuleCount(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to get compiled module count", e);
    }
  }

  @Override
  public long getCacheHits() {
    try {
      return nativeGetCacheHits(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to get cache hits", e);
    }
  }

  @Override
  public long getCacheMisses() {
    try {
      return nativeGetCacheMisses(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to get cache misses", e);
    }
  }

  @Override
  public long getMemoryUsage() {
    try {
      return nativeGetMemoryUsage(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to get memory usage", e);
    }
  }

  @Override
  public long getPeakMemoryUsage() {
    try {
      return nativeGetPeakMemoryUsage(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to get peak memory usage", e);
    }
  }

  @Override
  public long getTotalCompilationTimeMs() {
    try {
      return nativeGetTotalCompilationTimeMs(engineHandle);
    } catch (final Exception e) {
      throw new JniException("Failed to get total compilation time", e);
    }
  }

  // Native method declarations

  /**
   * Gets the number of compiled modules from the native engine.
   *
   * @param engineHandle the native engine handle
   * @return the number of compiled modules
   */
  private static native long nativeGetCompiledModuleCount(long engineHandle);

  /**
   * Gets the number of cache hits from the native engine.
   *
   * @param engineHandle the native engine handle
   * @return the number of cache hits
   */
  private static native long nativeGetCacheHits(long engineHandle);

  /**
   * Gets the number of cache misses from the native engine.
   *
   * @param engineHandle the native engine handle
   * @return the number of cache misses
   */
  private static native long nativeGetCacheMisses(long engineHandle);

  /**
   * Gets the current memory usage from the native engine.
   *
   * @param engineHandle the native engine handle
   * @return the current memory usage in bytes
   */
  private static native long nativeGetMemoryUsage(long engineHandle);

  /**
   * Gets the peak memory usage from the native engine.
   *
   * @param engineHandle the native engine handle
   * @return the peak memory usage in bytes
   */
  private static native long nativeGetPeakMemoryUsage(long engineHandle);

  /**
   * Gets the total compilation time from the native engine.
   *
   * @param engineHandle the native engine handle
   * @return the total compilation time in milliseconds
   */
  private static native long nativeGetTotalCompilationTimeMs(long engineHandle);
}
