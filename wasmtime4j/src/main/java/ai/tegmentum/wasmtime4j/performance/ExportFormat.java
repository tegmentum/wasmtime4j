package ai.tegmentum.wasmtime4j.performance;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Locale;

/**
 * Supported export formats for performance profiling data.
 *
 * <p>Each format provides different advantages for data analysis and visualization:
 *
 * <ul>
 *   <li>{@link #JSON} - Human-readable structured data for web dashboards
 *   <li>{@link #CSV} - Tabular data for spreadsheet analysis
 *   <li>{@link #BINARY} - Compact binary format for efficient storage
 *   <li>{@link #JFR} - Java Flight Recorder format for JDK tools
 *   <li>{@link #FLAME_GRAPH} - Flame graph format for visualization tools
 *   <li>{@link #JMH_JSON} - JMH benchmark result format
 * </ul>
 *
 * @since 1.0.0
 */
public enum ExportFormat {
  /**
   * JSON format for structured data export.
   *
   * <p>Produces human-readable JSON output suitable for:
   *
   * <ul>
   *   <li>Web dashboard consumption
   *   <li>REST API integration
   *   <li>Configuration management
   *   <li>Custom analysis tools
   * </ul>
   *
   * <p>Output includes nested structures for metrics, timelines, and metadata.
   */
  JSON("json", "application/json", "JSON structured data", true, false),

  /**
   * CSV format for tabular data export.
   *
   * <p>Produces comma-separated values suitable for:
   *
   * <ul>
   *   <li>Spreadsheet analysis (Excel, LibreOffice)
   *   <li>Database import operations
   *   <li>Statistical analysis tools (R, Python pandas)
   *   <li>Quick data visualization
   * </ul>
   *
   * <p>Flattens nested data structures into tabular format.
   */
  CSV("csv", "text/csv", "Comma-separated values", true, false),

  /**
   * Compact binary format for efficient storage and transfer.
   *
   * <p>Produces compressed binary output suitable for:
   *
   * <ul>
   *   <li>Long-term archival storage
   *   <li>Network transfer optimization
   *   <li>High-volume data collection
   *   <li>Custom binary analysis tools
   * </ul>
   *
   * <p>Uses efficient serialization with optional compression.
   */
  BINARY("bin", "application/octet-stream", "Compact binary format", false, true),

  /**
   * Java Flight Recorder format for JDK tool integration.
   *
   * <p>Produces JFR-compatible output suitable for:
   *
   * <ul>
   *   <li>JDK Mission Control analysis
   *   <li>JProfiler import
   *   <li>Standard JVM profiling workflows
   *   <li>Performance regression testing
   * </ul>
   *
   * <p>Integrates with existing JVM profiling ecosystem.
   */
  JFR("jfr", "application/octet-stream", "Java Flight Recorder format", false, true),

  /**
   * Flame graph format for performance visualization.
   *
   * <p>Produces flame graph data suitable for:
   *
   * <ul>
   *   <li>FlameGraph tool visualization
   *   <li>Call stack analysis
   *   <li>Performance hotspot identification
   *   <li>CPU profiling visualization
   * </ul>
   *
   * <p>Optimized for flame graph rendering tools.
   */
  FLAME_GRAPH("svg", "image/svg+xml", "Flame graph visualization", true, false),

  /**
   * JMH JSON format for benchmark result integration.
   *
   * <p>Produces JMH-compatible JSON output suitable for:
   *
   * <ul>
   *   <li>JMH benchmark result processing
   *   <li>Performance comparison tools
   *   <li>Continuous integration reporting
   *   <li>Benchmark trend analysis
   * </ul>
   *
   * <p>Compatible with JMH result processing tools.
   */
  JMH_JSON("json", "application/json", "JMH benchmark result format", true, false);

  private final String fileExtension;
  private final String mimeType;
  private final String description;
  private final boolean humanReadable;
  private final boolean compressed;

  ExportFormat(
      final String fileExtension,
      final String mimeType,
      final String description,
      final boolean humanReadable,
      final boolean compressed) {
    this.fileExtension = fileExtension;
    this.mimeType = mimeType;
    this.description = description;
    this.humanReadable = humanReadable;
    this.compressed = compressed;
  }

  /**
   * Gets the recommended file extension for this format.
   *
   * @return file extension without leading dot
   */
  public String getFileExtension() {
    return fileExtension;
  }

  /**
   * Gets the MIME type for this format.
   *
   * @return MIME type string
   */
  public String getMimeType() {
    return mimeType;
  }

  /**
   * Gets a human-readable description of this format.
   *
   * @return format description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this format produces human-readable output.
   *
   * @return true if the format is human-readable
   */
  public boolean isHumanReadable() {
    return humanReadable;
  }

  /**
   * Checks if this format uses compression or compact encoding.
   *
   * @return true if the format is compressed or compact
   */
  public boolean isCompressed() {
    return compressed;
  }

  /**
   * Checks if this format is suitable for web APIs.
   *
   * @return true if suitable for web API responses
   */
  public boolean isWebCompatible() {
    return this == JSON || this == CSV;
  }

  /**
   * Checks if this format is suitable for visualization tools.
   *
   * @return true if suitable for visualization
   */
  public boolean isVisualizationFormat() {
    return this == FLAME_GRAPH || this == JFR;
  }

  /**
   * Checks if this format is suitable for data analysis tools.
   *
   * @return true if suitable for analysis tools
   */
  public boolean isAnalysisFormat() {
    return this == CSV || this == JSON || this == JMH_JSON;
  }

  /**
   * Checks if this format is suitable for long-term storage.
   *
   * @return true if suitable for archival storage
   */
  public boolean isArchivalFormat() {
    return this == BINARY || this == JFR;
  }

  /**
   * Gets the recommended filename for exported data.
   *
   * @param baseName base name for the file
   * @return complete filename with extension
   */
  public String getFilename(final String baseName) {
    return baseName + "." + fileExtension;
  }

  /**
   * Estimates the relative size of output for this format.
   *
   * <p>Returns a rough multiplier compared to raw data size:
   *
   * <ul>
   *   <li>Binary formats: 0.3-0.8 (compressed)
   *   <li>JSON: 1.5-3.0 (structured text)
   *   <li>CSV: 1.0-1.5 (tabular text)
   * </ul>
   *
   * @return estimated size multiplier
   */
  public double getEstimatedSizeMultiplier() {
    switch (this) {
      case BINARY:
        return 0.3;
      case JFR:
        return 0.8;
      case CSV:
        return 1.2;
      case JSON:
      case JMH_JSON:
        return 2.0;
      case FLAME_GRAPH:
        return 1.5;
      default:
        return 1.0;
    }
  }

  /**
   * Gets the format that best matches the specified requirements.
   *
   * @param humanReadable whether human readability is required
   * @param compressed whether compression is preferred
   * @param webCompatible whether web compatibility is required
   * @return the best matching format
   */
  public static ExportFormat getBestMatch(
      final boolean humanReadable, final boolean compressed, final boolean webCompatible) {
    if (webCompatible) {
      return humanReadable ? JSON : CSV;
    }
    if (compressed) {
      return BINARY;
    }
    if (humanReadable) {
      return JSON;
    }
    return BINARY;
  }

  /**
   * Parses an export format from a string representation.
   *
   * @param formatString string representation (case-insensitive)
   * @return the corresponding export format
   * @throws IllegalArgumentException if the format is not recognized
   */
  @SuppressFBWarnings(
      value = "IMPROPER_UNICODE",
      justification = "Format strings are ASCII-only technical identifiers")
  public static ExportFormat fromString(final String formatString) {
    if (formatString == null || formatString.trim().isEmpty()) {
      throw new IllegalArgumentException("Format string cannot be null or empty");
    }

    final String normalized = formatString.trim().toUpperCase(Locale.ROOT).replace("-", "_");

    try {
      return valueOf(normalized);
    } catch (final IllegalArgumentException e) {
      // Try alternative names
      switch (normalized) {
        case "FLAMEGRAPH":
        case "FLAME":
          return FLAME_GRAPH;
        case "JMH":
          return JMH_JSON;
        default:
          throw new IllegalArgumentException("Unknown export format: " + formatString);
      }
    }
  }
}
