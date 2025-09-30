package ai.tegmentum.wasmtime4j.debug;

/**
 * Debug capabilities interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DebugCapabilities {

  /**
   * Checks if breakpoints are supported.
   *
   * @return true if breakpoints supported
   */
  boolean supportsBreakpoints();

  /**
   * Checks if step debugging is supported.
   *
   * @return true if step debugging supported
   */
  boolean supportsStepDebugging();

  /**
   * Checks if variable inspection is supported.
   *
   * @return true if variable inspection supported
   */
  boolean supportsVariableInspection();

  /**
   * Checks if memory inspection is supported.
   *
   * @return true if memory inspection supported
   */
  boolean supportsMemoryInspection();

  /**
   * Checks if profiling is supported.
   *
   * @return true if profiling supported
   */
  boolean supportsProfiling();

  /**
   * Checks if source map debugging is supported.
   *
   * @return true if source map debugging supported
   */
  boolean supportsSourceMap();

  /**
   * Checks if DWARF debugging is supported.
   *
   * @return true if DWARF debugging supported
   */
  boolean supportsDwarf();

  /**
   * Gets the maximum number of breakpoints.
   *
   * @return maximum breakpoints
   */
  int getMaxBreakpoints();

  /**
   * Gets supported debug formats.
   *
   * @return list of supported formats
   */
  java.util.List<String> getSupportedFormats();

  /**
   * Gets the debugger version.
   *
   * @return version string
   */
  String getDebuggerVersion();
}
