package ai.tegmentum.wasmtime4j.jni.performance;

import ai.tegmentum.wasmtime4j.performance.CallStack;
import ai.tegmentum.wasmtime4j.performance.CompilationProfile;
import ai.tegmentum.wasmtime4j.performance.FunctionProfile;
import ai.tegmentum.wasmtime4j.performance.HostFunctionProfile;
import ai.tegmentum.wasmtime4j.performance.MemoryProfile;
import ai.tegmentum.wasmtime4j.performance.ProfileReport;
import ai.tegmentum.wasmtime4j.performance.ProfilingDataFormat;
import ai.tegmentum.wasmtime4j.performance.ProfilingMarker;
import ai.tegmentum.wasmtime4j.performance.ProfilingOptions;
import ai.tegmentum.wasmtime4j.performance.ProfilingStatistics;
import ai.tegmentum.wasmtime4j.performance.WasmProfiler;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNI implementation of the WasmProfiler interface.
 *
 * <p>This implementation provides comprehensive WebAssembly execution profiling
 * capabilities using JNI calls to the native profiling system.
 *
 * @since 1.0.0
 */
public final class JniWasmProfiler extends JniResource implements WasmProfiler {

  private static final Logger LOGGER = Logger.getLogger(JniWasmProfiler.class.getName());

  /** Native handle for the profiler. */
  private final long nativeHandle;

  /** Current profiling options. */
  private volatile ProfilingOptions currentOptions;

  /** Maximum data retention setting. */
  private volatile int maxDataRetention = 100000;

  /**
   * Creates a new JNI WebAssembly profiler.
   *
   * @param engineHandle the engine handle to profile
   * @throws IllegalArgumentException if the engine handle is invalid
   */
  public JniWasmProfiler(final long engineHandle) {
    if (engineHandle == 0) {
      throw new IllegalArgumentException("Engine handle cannot be zero");
    }

    this.nativeHandle = nativeCreateProfiler(engineHandle);
    if (this.nativeHandle == 0) {
      throw new RuntimeException("Failed to create native profiler");
    }

    LOGGER.info("Created JNI WebAssembly profiler with handle: " + this.nativeHandle);
  }

  @Override
  public void startProfiling(final ProfilingOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("Profiling options cannot be null");
    }

    validateNotClosed();

    if (isProfiling()) {
      throw new IllegalStateException("Profiling is already active");
    }

    final int result = nativeStartProfiling(nativeHandle,
        options.isEnableFunctionProfiling(),
        options.isEnableMemoryProfiling(),
        options.isEnableCallStackSampling(),
        options.isEnableCompilationProfiling(),
        options.isEnableHostFunctionProfiling(),
        options.isEnableInstructionProfiling(),
        options.getSamplingInterval().toNanos() / 1000,
        options.getMaxSamples(),
        options.getMinExecutionTimeThreshold().toNanos() / 1000,
        options.getMinAllocationSizeThreshold(),
        options.isAggregateData(),
        options.isIncludeSourceLocations(),
        options.getBufferSize(),
        options.isThreadSafe());

    if (result != 0) {
      throw new RuntimeException("Failed to start profiling: error code " + result);
    }

    this.currentOptions = options;
    LOGGER.info("Profiling started with options: " + options);
  }

  @Override
  public ProfileReport stopProfiling() {
    validateNotClosed();

    if (!isProfiling()) {
      throw new IllegalStateException("Profiling is not active");
    }

    final long reportHandle = nativeStopProfiling(nativeHandle);
    if (reportHandle == 0) {
      throw new RuntimeException("Failed to stop profiling and generate report");
    }

    this.currentOptions = null;
    LOGGER.info("Profiling stopped and report generated");

    return new JniProfileReport(reportHandle);
  }

  @Override
  public boolean isProfiling() {
    validateNotClosed();
    return nativeIsProfiling(nativeHandle);
  }

  @Override
  public Map<String, FunctionProfile> getFunctionProfiles() {
    validateNotClosed();

    if (currentOptions == null) {
      throw new IllegalStateException("No profiling data available");
    }

    final long profilesHandle = nativeGetFunctionProfiles(nativeHandle);
    if (profilesHandle == 0) {
      throw new RuntimeException("Failed to get function profiles");
    }

    return JniFunctionProfileMap.fromNativeHandle(profilesHandle);
  }

  @Override
  public List<CallStack> getSampleCallStacks() {
    validateNotClosed();

    if (currentOptions == null) {
      throw new IllegalStateException("No profiling data available");
    }

    final long callStacksHandle = nativeGetSampleCallStacks(nativeHandle);
    if (callStacksHandle == 0) {
      throw new RuntimeException("Failed to get sample call stacks");
    }

    return JniCallStackList.fromNativeHandle(callStacksHandle);
  }

  @Override
  public MemoryProfile getMemoryProfile() {
    validateNotClosed();

    if (currentOptions == null) {
      throw new IllegalStateException("No profiling data available");
    }

    final long memoryProfileHandle = nativeGetMemoryProfile(nativeHandle);
    if (memoryProfileHandle == 0) {
      throw new RuntimeException("Failed to get memory profile");
    }

    return new JniMemoryProfile(memoryProfileHandle);
  }

  @Override
  public CompilationProfile getCompilationProfile() {
    validateNotClosed();

    if (currentOptions == null) {
      throw new IllegalStateException("No profiling data available");
    }

    final long compilationProfileHandle = nativeGetCompilationProfile(nativeHandle);
    if (compilationProfileHandle == 0) {
      throw new RuntimeException("Failed to get compilation profile");
    }

    return new JniCompilationProfile(compilationProfileHandle);
  }

  @Override
  public HostFunctionProfile getHostFunctionProfile() {
    validateNotClosed();

    if (currentOptions == null) {
      throw new IllegalStateException("No profiling data available");
    }

    final long hostFunctionProfileHandle = nativeGetHostFunctionProfile(nativeHandle);
    if (hostFunctionProfileHandle == 0) {
      throw new RuntimeException("Failed to get host function profile");
    }

    return new JniHostFunctionProfile(hostFunctionProfileHandle);
  }

  @Override
  public void addMarker(final String markerName, final Map<String, Object> metadata) {
    if (markerName == null || markerName.trim().isEmpty()) {
      throw new IllegalArgumentException("Marker name cannot be null or empty");
    }

    validateNotClosed();

    // Convert metadata to native format - for now, we'll serialize it as a string
    final String metadataString = metadata != null ? metadata.toString() : "";

    final int result = nativeAddMarker(nativeHandle, markerName, metadataString);
    if (result != 0) {
      throw new RuntimeException("Failed to add profiling marker: error code " + result);
    }

    LOGGER.fine("Added profiling marker: " + markerName);
  }

  @Override
  public void addMarker(final String markerName) {
    addMarker(markerName, null);
  }

  @Override
  public List<ProfilingMarker> getMarkers() {
    validateNotClosed();

    final long markersHandle = nativeGetMarkers(nativeHandle);
    if (markersHandle == 0) {
      throw new RuntimeException("Failed to get profiling markers");
    }

    return JniProfilingMarkerList.fromNativeHandle(markersHandle);
  }

  @Override
  public void reset() {
    validateNotClosed();

    final int result = nativeReset(nativeHandle);
    if (result != 0) {
      throw new RuntimeException("Failed to reset profiler: error code " + result);
    }

    LOGGER.info("Profiler reset");
  }

  @Override
  public ProfilingOptions getCurrentOptions() {
    return currentOptions;
  }

  @Override
  public ProfilingStatistics getCurrentStatistics() {
    validateNotClosed();

    if (!isProfiling()) {
      throw new IllegalStateException("Profiling is not active");
    }

    final long statisticsHandle = nativeGetCurrentStatistics(nativeHandle);
    if (statisticsHandle == 0) {
      throw new RuntimeException("Failed to get current profiling statistics");
    }

    return new JniProfilingStatistics(statisticsHandle);
  }

  @Override
  public void setMaxDataRetention(final int maxDataPoints) {
    if (maxDataPoints < 0) {
      throw new IllegalArgumentException("Max data points cannot be negative");
    }

    validateNotClosed();

    final int result = nativeSetMaxDataRetention(nativeHandle, maxDataPoints);
    if (result != 0) {
      throw new RuntimeException("Failed to set max data retention: error code " + result);
    }

    this.maxDataRetention = maxDataPoints;
    LOGGER.info("Max data retention set to: " + maxDataPoints);
  }

  @Override
  public int getMaxDataRetention() {
    return maxDataRetention;
  }

  @Override
  public String exportData(final ProfilingDataFormat format) {
    if (format == null) {
      throw new IllegalArgumentException("Export format cannot be null");
    }

    validateNotClosed();

    if (currentOptions == null) {
      throw new IllegalStateException("No profiling data available");
    }

    final String exportedData = nativeExportData(nativeHandle, format.name());
    if (exportedData == null) {
      throw new RuntimeException("Failed to export profiling data in format: " + format);
    }

    return exportedData;
  }

  @Override
  protected void disposeInternal() {
    if (nativeHandle != 0) {
      // Stop profiling if it's still active
      if (isProfiling()) {
        try {
          nativeStopProfiling(nativeHandle);
        } catch (final Exception e) {
          LOGGER.warning("Failed to stop profiling during disposal: " + e.getMessage());
        }
      }

      nativeDispose(nativeHandle);
      LOGGER.info("Disposed JNI WebAssembly profiler");
    }
  }

  // Native method declarations

  private static native long nativeCreateProfiler(final long engineHandle);

  private static native int nativeStartProfiling(
      final long handle,
      final boolean enableFunctionProfiling,
      final boolean enableMemoryProfiling,
      final boolean enableCallStackSampling,
      final boolean enableCompilationProfiling,
      final boolean enableHostFunctionProfiling,
      final boolean enableInstructionProfiling,
      final long samplingIntervalMicros,
      final int maxSamples,
      final long minExecutionTimeThresholdMicros,
      final long minAllocationSizeThreshold,
      final boolean aggregateData,
      final boolean includeSourceLocations,
      final int bufferSize,
      final boolean threadSafe);

  private static native long nativeStopProfiling(final long handle);
  private static native boolean nativeIsProfiling(final long handle);
  private static native long nativeGetFunctionProfiles(final long handle);
  private static native long nativeGetSampleCallStacks(final long handle);
  private static native long nativeGetMemoryProfile(final long handle);
  private static native long nativeGetCompilationProfile(final long handle);
  private static native long nativeGetHostFunctionProfile(final long handle);
  private static native int nativeAddMarker(final long handle, final String markerName, final String metadataString);
  private static native long nativeGetMarkers(final long handle);
  private static native int nativeReset(final long handle);
  private static native long nativeGetCurrentStatistics(final long handle);
  private static native int nativeSetMaxDataRetention(final long handle, final int maxDataPoints);
  private static native String nativeExportData(final long handle, final String format);
  private static native void nativeDispose(final long handle);
}