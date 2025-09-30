package ai.tegmentum.wasmtime4j.diagnostics;

/**
 * Diagnostics collector interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DiagnosticsCollector {

  /** Starts diagnostics collection. */
  void startCollection();

  /** Stops diagnostics collection. */
  void stopCollection();

  /**
   * Gets collected diagnostics data.
   *
   * @return diagnostics data as string
   */
  String getDiagnosticsData();

  /** Clears collected diagnostics. */
  void clearDiagnostics();

  /**
   * Checks if collection is active.
   *
   * @return true if collection is active
   */
  boolean isCollecting();
}
