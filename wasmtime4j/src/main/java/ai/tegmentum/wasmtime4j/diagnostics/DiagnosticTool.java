package ai.tegmentum.wasmtime4j.diagnostics;

/**
 * Diagnostic tool interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DiagnosticTool {

  /**
   * Runs diagnostic analysis.
   *
   * @return diagnostic report
   */
  DiagnosticReport runDiagnostics();

  /**
   * Gets the tool name.
   *
   * @return tool name
   */
  String getToolName();

  /**
   * Checks if the tool is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Configures the diagnostic tool.
   *
   * @param config configuration parameters
   */
  void configure(String config);
}
