package ai.tegmentum.wasmtime4j.comparison.analyzers;

import java.nio.file.Path;

/**
 * Result of generating enhanced Wasmtime coverage dashboard.
 *
 * @since 1.0.0
 */
public final class WasmtimeDashboardResult {
  private final Path htmlDashboardPath;
  private final Path jsonReportPath;
  private final WasmtimeCoverageSummary summary;

  public WasmtimeDashboardResult(
      final Path htmlDashboardPath,
      final Path jsonReportPath,
      final WasmtimeCoverageSummary summary) {
    this.htmlDashboardPath = htmlDashboardPath;
    this.jsonReportPath = jsonReportPath;
    this.summary = summary;
  }

  public Path getHtmlDashboardPath() {
    return htmlDashboardPath;
  }

  public Path getJsonReportPath() {
    return jsonReportPath;
  }

  public WasmtimeCoverageSummary getSummary() {
    return summary;
  }

  public boolean isSuccessful() {
    return htmlDashboardPath != null && jsonReportPath != null;
  }
}
