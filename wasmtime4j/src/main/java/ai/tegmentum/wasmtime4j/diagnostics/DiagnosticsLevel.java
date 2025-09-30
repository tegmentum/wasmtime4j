package ai.tegmentum.wasmtime4j.diagnostics;

/**
 * Diagnostics level enumeration for WebAssembly components.
 *
 * @since 1.0.0
 */
public enum DiagnosticsLevel {
  /** Trace level - most verbose. */
  TRACE,
  /** Debug level. */
  DEBUG,
  /** Info level. */
  INFO,
  /** Warning level. */
  WARN,
  /** Error level. */
  ERROR,
  /** Fatal level - least verbose. */
  FATAL
}
