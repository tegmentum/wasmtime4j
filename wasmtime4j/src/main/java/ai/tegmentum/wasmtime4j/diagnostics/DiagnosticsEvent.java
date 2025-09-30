package ai.tegmentum.wasmtime4j.diagnostics;

/**
 * Diagnostics event interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DiagnosticsEvent {

  /**
   * Gets the event type.
   *
   * @return event type
   */
  String getEventType();

  /**
   * Gets the event timestamp.
   *
   * @return timestamp in milliseconds
   */
  long getTimestamp();

  /**
   * Gets the event data.
   *
   * @return event data as string
   */
  String getEventData();

  /**
   * Gets the event source.
   *
   * @return event source
   */
  String getSource();
}
