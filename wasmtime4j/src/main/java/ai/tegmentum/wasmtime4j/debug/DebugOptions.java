package ai.tegmentum.wasmtime4j.debug;

/**
 * Debug options interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DebugOptions {

  /**
   * Checks if debug mode is enabled.
   *
   * @return true if debug mode enabled
   */
  boolean isDebugEnabled();

  /**
   * Checks if profiling is enabled.
   *
   * @return true if profiling enabled
   */
  boolean isProfilingEnabled();

  /**
   * Checks if tracing is enabled.
   *
   * @return true if tracing enabled
   */
  boolean isTracingEnabled();

  /**
   * Gets the debug log level.
   *
   * @return debug log level
   */
  String getLogLevel();

  /**
   * Gets the output directory for debug files.
   *
   * @return output directory path
   */
  String getOutputDirectory();

  /**
   * Checks if source map support is enabled.
   *
   * @return true if source map enabled
   */
  boolean isSourceMapEnabled();

  /**
   * Checks if DWARF support is enabled.
   *
   * @return true if DWARF enabled
   */
  boolean isDwarfEnabled();

  /**
   * Gets the maximum trace buffer size.
   *
   * @return buffer size
   */
  int getMaxTraceBufferSize();

  /**
   * Gets the debug timeout in milliseconds.
   *
   * @return timeout value
   */
  long getDebugTimeout();

  /**
   * Checks if memory dumps are enabled.
   *
   * @return true if memory dumps enabled
   */
  boolean isMemoryDumpEnabled();
}
