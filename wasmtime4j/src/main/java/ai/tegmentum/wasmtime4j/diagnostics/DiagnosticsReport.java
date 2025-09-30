package ai.tegmentum.wasmtime4j.diagnostics;

/**
 * Diagnostics report interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DiagnosticsReport {

  /**
   * Gets the report ID.
   *
   * @return report ID
   */
  String getReportId();

  /**
   * Gets the report timestamp.
   *
   * @return timestamp in milliseconds
   */
  long getTimestamp();

  /**
   * Gets the diagnostics level.
   *
   * @return diagnostics level
   */
  DiagnosticsLevel getLevel();

  /**
   * Gets the report summary.
   *
   * @return summary text
   */
  String getSummary();

  /**
   * Gets the detailed report.
   *
   * @return detailed report text
   */
  String getDetails();

  /**
   * Gets the report status.
   *
   * @return report status
   */
  ReportStatus getStatus();

  /** Report status enumeration. */
  enum ReportStatus {
    /** Report is being generated. */
    GENERATING,
    /** Report is ready. */
    READY,
    /** Report generation failed. */
    FAILED
  }
}
