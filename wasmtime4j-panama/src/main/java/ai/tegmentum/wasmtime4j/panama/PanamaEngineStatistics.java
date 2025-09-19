package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.EngineStatistics;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.MemorySegment;
import java.util.Objects;

/**
 * Panama FFI implementation of EngineStatistics providing runtime metrics for WebAssembly engines.
 *
 * <p>This class provides access to various performance and usage statistics from the native
 * Wasmtime engine through Panama FFI calls. Statistics include compilation counts, cache
 * performance, and memory usage metrics.
 *
 * @since 1.0.0
 */
public final class PanamaEngineStatistics implements EngineStatistics {

  private final MemorySegment enginePtr;
  private final ArenaResourceManager resourceManager;
  private final NativeFunctionBindings nativeFunctions;

  /**
   * Creates statistics accessor for the given engine pointer.
   *
   * @param enginePtr the native engine pointer
   * @param resourceManager the resource manager for memory operations
   * @throws IllegalArgumentException if enginePtr is null or invalid
   */
  PanamaEngineStatistics(
      final MemorySegment enginePtr, final ArenaResourceManager resourceManager) {
    this.enginePtr = Objects.requireNonNull(enginePtr, "Engine pointer cannot be null");
    this.resourceManager =
        Objects.requireNonNull(resourceManager, "Resource manager cannot be null");
    this.nativeFunctions = NativeFunctionBindings.getInstance();

    PanamaErrorHandler.requireValidPointer(enginePtr, "enginePtr");

    if (!nativeFunctions.isInitialized()) {
      throw new IllegalArgumentException("Native function bindings not initialized");
    }
  }

  @Override
  public long getCompiledModuleCount() {
    try {
      return nativeFunctions.engineGetCompiledModuleCount(enginePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to get compiled module count", e);
    }
  }

  @Override
  public long getCacheHits() {
    try {
      return nativeFunctions.engineGetCacheHits(enginePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to get cache hits", e);
    }
  }

  @Override
  public long getCacheMisses() {
    try {
      return nativeFunctions.engineGetCacheMisses(enginePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to get cache misses", e);
    }
  }

  @Override
  public long getMemoryUsage() {
    try {
      return nativeFunctions.engineGetMemoryUsage(enginePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to get memory usage", e);
    }
  }

  @Override
  public long getPeakMemoryUsage() {
    try {
      return nativeFunctions.engineGetPeakMemoryUsage(enginePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to get peak memory usage", e);
    }
  }

  @Override
  public long getTotalCompilationTimeMs() {
    try {
      return nativeFunctions.engineGetTotalCompilationTimeMs(enginePtr);
    } catch (final Exception e) {
      throw new WasmException("Failed to get total compilation time", e);
    }
  }
}
