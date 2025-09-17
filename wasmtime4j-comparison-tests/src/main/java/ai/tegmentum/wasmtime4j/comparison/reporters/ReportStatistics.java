package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Map;
import java.util.Objects;

/**
 * Statistical information about the comparison report.
 *
 * @since 1.0.0
 */
public final class ReportStatistics {
  private final int totalDataPoints;
  private final long reportSizeBytes;
  private final Map<String, Integer> categoryBreakdown;
  private final double dataQualityScore;

  /**
   * Constructs a new ReportStatistics with the specified statistical data.
   *
   * @param totalDataPoints the total number of data points in the report
   * @param reportSizeBytes the size of the report in bytes
   * @param categoryBreakdown breakdown of data by category
   * @param dataQualityScore the data quality score
   */
  public ReportStatistics(
      final int totalDataPoints,
      final long reportSizeBytes,
      final Map<String, Integer> categoryBreakdown,
      final double dataQualityScore) {
    this.totalDataPoints = totalDataPoints;
    this.reportSizeBytes = reportSizeBytes;
    this.categoryBreakdown = Map.copyOf(categoryBreakdown);
    this.dataQualityScore = dataQualityScore;
  }

  public int getTotalDataPoints() {
    return totalDataPoints;
  }

  public long getReportSizeBytes() {
    return reportSizeBytes;
  }

  public Map<String, Integer> getCategoryBreakdown() {
    return categoryBreakdown;
  }

  public double getDataQualityScore() {
    return dataQualityScore;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ReportStatistics that = (ReportStatistics) obj;
    return totalDataPoints == that.totalDataPoints
        && reportSizeBytes == that.reportSizeBytes
        && Double.compare(that.dataQualityScore, dataQualityScore) == 0
        && Objects.equals(categoryBreakdown, that.categoryBreakdown);
  }

  @Override
  public int hashCode() {
    return Objects.hash(totalDataPoints, reportSizeBytes, categoryBreakdown, dataQualityScore);
  }

  @Override
  public String toString() {
    return "ReportStatistics{"
        + "dataPoints="
        + totalDataPoints
        + ", sizeBytes="
        + reportSizeBytes
        + ", qualityScore="
        + String.format("%.2f", dataQualityScore)
        + '}';
  }
}
