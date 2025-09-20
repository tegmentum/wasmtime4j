package ai.tegmentum.wasmtime4j.jni.performance;

import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.performance.PerformanceEvent;
import ai.tegmentum.wasmtime4j.performance.PerformanceEventSeverity;
import ai.tegmentum.wasmtime4j.performance.PerformanceEventType;
import java.time.Instant;
import java.util.Map;
import java.util.logging.Logger;

/**
 * JNI implementation of PerformanceEvent interface.
 *
 * <p>Represents a specific performance event that occurred during WebAssembly execution.
 *
 * @since 1.0.0
 */
public final class JniPerformanceEvent extends JniResource implements PerformanceEvent {

  private static final Logger LOGGER = Logger.getLogger(JniPerformanceEvent.class.getName());

  /** Native handle for the performance event. */
  private final long nativeHandle;

  /**
   * Creates a new JNI performance event instance.
   *
   * @param nativeHandle the native performance event handle
   * @throws IllegalArgumentException if the native handle is invalid
   */
  public JniPerformanceEvent(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new IllegalArgumentException("Native handle cannot be zero");
    }

    this.nativeHandle = nativeHandle;
    LOGGER.fine("Created JNI performance event with handle: " + this.nativeHandle);
  }

  @Override
  public PerformanceEventType getType() {
    validateNotClosed();
    final int typeOrdinal = nativeGetType(nativeHandle);
    return PerformanceEventType.values()[typeOrdinal];
  }

  @Override
  public PerformanceEventSeverity getSeverity() {
    validateNotClosed();
    final int severityOrdinal = nativeGetSeverity(nativeHandle);
    return PerformanceEventSeverity.values()[severityOrdinal];
  }

  @Override
  public Instant getTimestamp() {
    validateNotClosed();
    final long timestampMicros = nativeGetTimestamp(nativeHandle);
    return Instant.ofEpochSecond(timestampMicros / 1_000_000, (timestampMicros % 1_000_000) * 1000);
  }

  @Override
  public String getDescription() {
    validateNotClosed();
    final String description = nativeGetDescription(nativeHandle);
    if (description == null) {
      throw new RuntimeException("Failed to get performance event description");
    }
    return description;
  }

  @Override
  public String getSource() {
    validateNotClosed();
    final String source = nativeGetSource(nativeHandle);
    return source != null ? source : "Unknown";
  }

  @Override
  public double getValue() {
    validateNotClosed();
    return nativeGetValue(nativeHandle);
  }

  @Override
  public String getUnit() {
    validateNotClosed();
    final String unit = nativeGetUnit(nativeHandle);
    return unit != null ? unit : "";
  }

  @Override
  public Map<String, Object> getMetadata() {
    validateNotClosed();
    final long metadataHandle = nativeGetMetadata(nativeHandle);
    if (metadataHandle == 0) {
      return Map.of(); // Return empty map if no metadata
    }
    return JniMetadataMap.fromNativeHandle(metadataHandle);
  }

  @Override
  public String getContextualInfo() {
    validateNotClosed();
    final String info = nativeGetContextualInfo(nativeHandle);
    return info != null ? info : "";
  }

  @Override
  public boolean isThresholdViolation() {
    validateNotClosed();
    return nativeIsThresholdViolation(nativeHandle);
  }

  @Override
  public double getThresholdValue() {
    validateNotClosed();
    return nativeGetThresholdValue(nativeHandle);
  }

  @Override
  public String getRecommendation() {
    validateNotClosed();
    final String recommendation = nativeGetRecommendation(nativeHandle);
    return recommendation != null ? recommendation : "";
  }

  @Override
  public long getEventId() {
    validateNotClosed();
    return nativeGetEventId(nativeHandle);
  }

  @Override
  public String getCategory() {
    validateNotClosed();
    final String category = nativeGetCategory(nativeHandle);
    return category != null ? category : "General";
  }

  @Override
  public boolean hasRelatedEvents() {
    validateNotClosed();
    return nativeHasRelatedEvents(nativeHandle);
  }

  @Override
  public String getFormattedMessage() {
    validateNotClosed();
    final String message = nativeGetFormattedMessage(nativeHandle);
    if (message == null) {
      throw new RuntimeException("Failed to get formatted performance event message");
    }
    return message;
  }

  @Override
  protected void disposeInternal() {
    if (nativeHandle != 0) {
      nativeDispose(nativeHandle);
      LOGGER.fine("Disposed JNI performance event");
    }
  }

  // Native method declarations

  private static native int nativeGetType(final long handle);

  private static native int nativeGetSeverity(final long handle);

  private static native long nativeGetTimestamp(final long handle);

  private static native String nativeGetDescription(final long handle);

  private static native String nativeGetSource(final long handle);

  private static native double nativeGetValue(final long handle);

  private static native String nativeGetUnit(final long handle);

  private static native long nativeGetMetadata(final long handle);

  private static native String nativeGetContextualInfo(final long handle);

  private static native boolean nativeIsThresholdViolation(final long handle);

  private static native double nativeGetThresholdValue(final long handle);

  private static native String nativeGetRecommendation(final long handle);

  private static native long nativeGetEventId(final long handle);

  private static native String nativeGetCategory(final long handle);

  private static native boolean nativeHasRelatedEvents(final long handle);

  private static native String nativeGetFormattedMessage(final long handle);

  private static native void nativeDispose(final long handle);
}
