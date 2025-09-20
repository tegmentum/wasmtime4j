package ai.tegmentum.wasmtime4j.jni.performance;

import ai.tegmentum.wasmtime4j.performance.PerformanceEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 * Utility class for creating Java List of PerformanceEvent from native handles.
 *
 * <p>This class provides static methods to convert native performance event arrays into Java List
 * objects for convenient use in the Java API.
 *
 * @since 1.0.0
 */
public final class JniPerformanceEventList {

  private static final Logger LOGGER = Logger.getLogger(JniPerformanceEventList.class.getName());

  /** Private constructor to prevent instantiation. */
  private JniPerformanceEventList() {
    // Utility class
  }

  /**
   * Creates a Java List of PerformanceEvent from a native handle.
   *
   * @param nativeHandle the native handle to the performance event array
   * @return Java List of PerformanceEvent objects
   * @throws RuntimeException if the native handle is invalid or conversion fails
   */
  public static List<PerformanceEvent> fromNativeHandle(final long nativeHandle) {
    if (nativeHandle == 0) {
      throw new RuntimeException("Invalid native handle for performance event list");
    }

    try {
      final int count = nativeGetEventCount(nativeHandle);
      final List<PerformanceEvent> events = new ArrayList<>(count);

      for (int i = 0; i < count; i++) {
        final long eventHandle = nativeGetEventAt(nativeHandle, i);
        if (eventHandle != 0) {
          events.add(new JniPerformanceEvent(eventHandle));
        } else {
          LOGGER.warning("Null event handle at index " + i);
        }
      }

      // Dispose the native array after extraction
      nativeDisposeEventList(nativeHandle);

      return events;
    } catch (final Exception e) {
      // Ensure native resources are cleaned up on error
      try {
        nativeDisposeEventList(nativeHandle);
      } catch (final Exception cleanupException) {
        LOGGER.warning("Failed to cleanup native event list: " + cleanupException.getMessage());
      }
      throw new RuntimeException("Failed to convert native performance event list", e);
    }
  }

  // Native method declarations

  /**
   * Gets the number of events in the native event list.
   *
   * @param handle the native event list handle
   * @return number of events
   */
  private static native int nativeGetEventCount(final long handle);

  /**
   * Gets the event handle at the specified index.
   *
   * @param handle the native event list handle
   * @param index the event index
   * @return native event handle
   */
  private static native long nativeGetEventAt(final long handle, final int index);

  /**
   * Disposes the native event list.
   *
   * @param handle the native event list handle
   */
  private static native void nativeDisposeEventList(final long handle);
}
