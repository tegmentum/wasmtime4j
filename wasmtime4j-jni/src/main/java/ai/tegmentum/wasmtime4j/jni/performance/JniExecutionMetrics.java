package ai.tegmentum.wasmtime4j.jni.performance;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.performance.ExecutionMetrics;
import java.time.Duration;
import java.time.Instant;

/**
 * JNI implementation of ExecutionMetrics interface.
 *
 * <p>This class provides real-time execution metrics for WebAssembly operations by interfacing with
 * the native performance monitoring system through JNI.
 *
 * @since 1.0.0
 */
public final class JniExecutionMetrics extends JniResource implements ExecutionMetrics {

  /** Native handle for the execution metrics. */
  private final long nativeHandle;

  /**
   * Creates a new JNI execution metrics instance.
   *
   * @param nativeHandle the native execution metrics handle
   * @throws IllegalArgumentException if the native handle is invalid
   */
  public JniExecutionMetrics(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be zero");
    }
    this.nativeHandle = nativeHandle;
  }

  @Override
  public Instant getTimestamp() {
    validateNotClosed();
    final long timestampMicros = nativeGetTimestamp(nativeHandle);
    return Instant.ofEpochSecond(timestampMicros / 1_000_000, (timestampMicros % 1_000_000) * 1000);
  }

  @Override
  public long getInstructionsExecuted() {
    validateNotClosed();
    return nativeGetInstructionsExecuted(nativeHandle);
  }

  @Override
  public Duration getExecutionTime() {
    validateNotClosed();
    final long executionTimeMicros = nativeGetExecutionTime(nativeHandle);
    return Duration.ofNanos(executionTimeMicros * 1000);
  }

  @Override
  public long getMemoryAllocations() {
    validateNotClosed();
    return nativeGetMemoryAllocations(nativeHandle);
  }

  @Override
  public long getTotalAllocatedBytes() {
    validateNotClosed();
    return nativeGetTotalAllocatedBytes(nativeHandle);
  }

  @Override
  public int getFunctionCalls() {
    validateNotClosed();
    return nativeGetFunctionCalls(nativeHandle);
  }

  @Override
  public double getCpuUsage() {
    validateNotClosed();
    return nativeGetCpuUsage(nativeHandle);
  }

  @Override
  public long getCurrentMemoryUsage() {
    validateNotClosed();
    return nativeGetCurrentMemoryUsage(nativeHandle);
  }

  @Override
  public long getPeakMemoryUsage() {
    validateNotClosed();
    return nativeGetPeakMemoryUsage(nativeHandle);
  }

  @Override
  public int getGarbageCollectionCount() {
    validateNotClosed();
    return nativeGetGarbageCollectionCount(nativeHandle);
  }

  @Override
  public Duration getGarbageCollectionTime() {
    validateNotClosed();
    final long gcTimeMicros = nativeGetGarbageCollectionTime(nativeHandle);
    return Duration.ofNanos(gcTimeMicros * 1000);
  }

  @Override
  public int getHostFunctionCalls() {
    validateNotClosed();
    return nativeGetHostFunctionCalls(nativeHandle);
  }

  @Override
  public Duration getHostFunctionTime() {
    validateNotClosed();
    final long hostFunctionTimeMicros = nativeGetHostFunctionTime(nativeHandle);
    return Duration.ofNanos(hostFunctionTimeMicros * 1000);
  }

  @Override
  public double getInstructionsPerSecond() {
    validateNotClosed();
    return nativeGetInstructionsPerSecond(nativeHandle);
  }

  @Override
  public double getFunctionCallsPerSecond() {
    validateNotClosed();
    return nativeGetFunctionCallsPerSecond(nativeHandle);
  }

  @Override
  public double getBytesAllocatedPerSecond() {
    validateNotClosed();
    return nativeGetBytesAllocatedPerSecond(nativeHandle);
  }

  @Override
  public int getJitCompilationCount() {
    validateNotClosed();
    return nativeGetJitCompilationCount(nativeHandle);
  }

  @Override
  public Duration getJitCompilationTime() {
    validateNotClosed();
    final long jitCompilationTimeMicros = nativeGetJitCompilationTime(nativeHandle);
    return Duration.ofNanos(jitCompilationTimeMicros * 1000);
  }

  @Override
  public String getSummary() {
    validateNotClosed();
    final String summary = nativeGetSummary(nativeHandle);
    if (summary == null) {
      throw new RuntimeException("Failed to get execution metrics summary");
    }
    return summary;
  }

  @Override
  protected void disposeInternal() {
    if (nativeHandle != 0) {
      nativeDispose(nativeHandle);
    }
  }

  // Native method declarations

  private static native long nativeGetTimestamp(final long handle);

  private static native long nativeGetInstructionsExecuted(final long handle);

  private static native long nativeGetExecutionTime(final long handle);

  private static native long nativeGetMemoryAllocations(final long handle);

  private static native long nativeGetTotalAllocatedBytes(final long handle);

  private static native int nativeGetFunctionCalls(final long handle);

  private static native double nativeGetCpuUsage(final long handle);

  private static native long nativeGetCurrentMemoryUsage(final long handle);

  private static native long nativeGetPeakMemoryUsage(final long handle);

  private static native int nativeGetGarbageCollectionCount(final long handle);

  private static native long nativeGetGarbageCollectionTime(final long handle);

  private static native int nativeGetHostFunctionCalls(final long handle);

  private static native long nativeGetHostFunctionTime(final long handle);

  private static native double nativeGetInstructionsPerSecond(final long handle);

  private static native double nativeGetFunctionCallsPerSecond(final long handle);

  private static native double nativeGetBytesAllocatedPerSecond(final long handle);

  private static native int nativeGetJitCompilationCount(final long handle);

  private static native long nativeGetJitCompilationTime(final long handle);

  private static native String nativeGetSummary(final long handle);

  private static native void nativeDispose(final long handle);
}
