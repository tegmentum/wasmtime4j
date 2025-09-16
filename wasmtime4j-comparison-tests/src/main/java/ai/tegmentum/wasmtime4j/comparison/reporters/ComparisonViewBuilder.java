package ai.tegmentum.wasmtime4j.comparison.reporters;

import ai.tegmentum.wasmtime4j.RuntimeType;
import ai.tegmentum.wasmtime4j.comparison.analyzers.BehavioralDiscrepancy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

/**
 * Builder for creating interactive side-by-side comparison views with diff highlighting,
 * runtime-specific output analysis, and behavioral discrepancy visualization. Supports both
 * text-based and structured data comparisons with detailed change tracking.
 *
 * @since 1.0.0
 */
public final class ComparisonViewBuilder {

  /**
   * Creates a side-by-side comparison view for a specific test result.
   *
   * @param testResult the test comparison result to visualize
   * @return interactive comparison view data
   */
  public Map<String, Object> createSideBySideComparison(final TestComparisonResult testResult) {
    Objects.requireNonNull(testResult, "testResult cannot be null");

    final Map<String, Object> comparisonView = new HashMap<>();

    // Basic test information
    comparisonView.put("testName", testResult.getTestName());
    comparisonView.put("overallStatus", testResult.getOverallStatus().toString());
    comparisonView.put("hasCriticalIssues", testResult.hasCriticalIssues());

    // Runtime comparison panels
    final List<Map<String, Object>> runtimePanels = createRuntimePanels(testResult);
    comparisonView.put("runtimePanels", runtimePanels);

    // Diff analysis
    final Map<String, Object> diffAnalysis = createDiffAnalysis(testResult);
    comparisonView.put("diffAnalysis", diffAnalysis);

    // Behavioral discrepancies
    final List<Map<String, Object>> discrepancies =
        createDiscrepancyViews(testResult.getDiscrepancies());
    comparisonView.put("discrepancies", discrepancies);

    // Performance comparison
    final Map<String, Object> performanceComparison = createPerformanceComparison(testResult);
    comparisonView.put("performanceComparison", performanceComparison);

    return comparisonView;
  }

  /**
   * Creates runtime panels for side-by-side comparison.
   *
   * @param testResult the test comparison result
   * @return list of runtime panel configurations
   */
  private List<Map<String, Object>> createRuntimePanels(final TestComparisonResult testResult) {
    final List<Map<String, Object>> panels = new ArrayList<>();

    for (final Map.Entry<RuntimeType, TestExecutionResult> entry :
        testResult.getRuntimeResults().entrySet()) {
      final RuntimeType runtime = entry.getKey();
      final TestExecutionResult result = entry.getValue();

      final Map<String, Object> panel = new HashMap<>();
      panel.put("runtime", runtime.toString());
      panel.put("successful", result.isSuccessful());
      panel.put("executionTime", result.getExecutionTime().toMillis());
      panel.put("output", result.getOutput());
      panel.put("errorMessage", result.getErrorMessage());
      panel.put("hasError", !result.getErrorMessage().isEmpty());

      // Formatted output for display
      panel.put("formattedOutput", formatOutputForDisplay(result.getOutput()));
      panel.put("outputLines", splitIntoLines(result.getOutput()));

      // Metrics visualization
      panel.put("metrics", result.getMetrics());
      panel.put("metricsFormatted", formatMetricsForDisplay(result.getMetrics()));

      // Status indicators
      panel.put("statusClass", getStatusClass(result));
      panel.put("statusIcon", getStatusIcon(result));

      panels.add(panel);
    }

    return panels;
  }

  /**
   * Creates detailed diff analysis between runtime outputs.
   *
   * @param testResult the test comparison result
   * @return diff analysis data
   */
  private Map<String, Object> createDiffAnalysis(final TestComparisonResult testResult) {
    final Map<String, Object> diffAnalysis = new HashMap<>();

    final List<TestExecutionResult> results =
        new ArrayList<>(testResult.getRuntimeResults().values());
    if (results.size() < 2) {
      diffAnalysis.put("hasDifferences", false);
      diffAnalysis.put("message", "Need at least 2 runtimes to compare");
      return diffAnalysis;
    }

    // Compare outputs between runtimes
    final List<Map<String, Object>> comparisons = new ArrayList<>();
    for (int i = 0; i < results.size(); i++) {
      for (int j = i + 1; j < results.size(); j++) {
        final TestExecutionResult result1 = results.get(i);
        final TestExecutionResult result2 = results.get(j);

        final Map<String, Object> comparison = createPairwiseComparison(result1, result2);
        comparisons.add(comparison);
      }
    }

    diffAnalysis.put("comparisons", comparisons);
    diffAnalysis.put(
        "hasDifferences",
        comparisons.stream().anyMatch(comp -> (Boolean) comp.get("hasDifferences")));

    // Overall diff summary
    final Map<String, Object> summary = createDiffSummary(comparisons);
    diffAnalysis.put("summary", summary);

    return diffAnalysis;
  }

  /**
   * Creates a pairwise comparison between two runtime results.
   *
   * @param result1 the first runtime result
   * @param result2 the second runtime result
   * @return pairwise comparison data
   */
  private Map<String, Object> createPairwiseComparison(
      final TestExecutionResult result1, final TestExecutionResult result2) {

    final Map<String, Object> comparison = new HashMap<>();

    comparison.put("runtime1", result1.getRuntime().toString());
    comparison.put("runtime2", result2.getRuntime().toString());

    // Output comparison
    final DiffResult outputDiff = computeTextDiff(result1.getOutput(), result2.getOutput());
    comparison.put("outputDiff", outputDiff.toMap());

    // Error message comparison
    final DiffResult errorDiff =
        computeTextDiff(result1.getErrorMessage(), result2.getErrorMessage());
    comparison.put("errorDiff", errorDiff.toMap());

    // Execution time comparison
    final long timeDiff =
        result2.getExecutionTime().toMillis() - result1.getExecutionTime().toMillis();
    comparison.put("executionTimeDiff", timeDiff);
    comparison.put(
        "executionTimeRatio",
        result1.getExecutionTime().toMillis() > 0
            ? (double) result2.getExecutionTime().toMillis() / result1.getExecutionTime().toMillis()
            : 0.0);

    // Success status comparison
    comparison.put("successDiff", result1.isSuccessful() != result2.isSuccessful());

    // Overall differences
    comparison.put(
        "hasDifferences",
        outputDiff.hasDifferences()
            || errorDiff.hasDifferences()
            || timeDiff != 0
            || result1.isSuccessful() != result2.isSuccessful());

    return comparison;
  }

  /**
   * Computes a text diff between two strings with line-by-line analysis.
   *
   * @param text1 the first text
   * @param text2 the second text
   * @return diff result with highlighted changes
   */
  private DiffResult computeTextDiff(final String text1, final String text2) {
    final String[] lines1 = text1.split("\\r?\\n");
    final String[] lines2 = text2.split("\\r?\\n");

    final List<DiffLine> diffLines = new ArrayList<>();
    final int maxLines = Math.max(lines1.length, lines2.length);

    for (int i = 0; i < maxLines; i++) {
      final String line1 = i < lines1.length ? lines1[i] : null;
      final String line2 = i < lines2.length ? lines2[i] : null;

      if (line1 == null) {
        diffLines.add(new DiffLine(i, null, line2, DiffType.ADDED));
      } else if (line2 == null) {
        diffLines.add(new DiffLine(i, line1, null, DiffType.REMOVED));
      } else if (line1.equals(line2)) {
        diffLines.add(new DiffLine(i, line1, line2, DiffType.UNCHANGED));
      } else {
        diffLines.add(new DiffLine(i, line1, line2, DiffType.MODIFIED));
      }
    }

    final boolean hasDifferences =
        diffLines.stream().anyMatch(line -> line.getType() != DiffType.UNCHANGED);

    return new DiffResult(diffLines, hasDifferences);
  }

  /**
   * Creates a summary of all diff comparisons.
   *
   * @param comparisons the list of pairwise comparisons
   * @return diff summary data
   */
  private Map<String, Object> createDiffSummary(final List<Map<String, Object>> comparisons) {
    final Map<String, Object> summary = new HashMap<>();

    final long totalComparisons = comparisons.size();
    final long comparisonsWithDifferences =
        comparisons.stream().mapToLong(comp -> (Boolean) comp.get("hasDifferences") ? 1 : 0).sum();

    summary.put("totalComparisons", totalComparisons);
    summary.put("comparisonsWithDifferences", comparisonsWithDifferences);
    summary.put(
        "differencePercentage",
        totalComparisons > 0
            ? (double) comparisonsWithDifferences / totalComparisons * 100.0
            : 0.0);

    // Categorize differences
    final Map<String, Integer> differenceTypes = new HashMap<>();
    differenceTypes.put("outputDifferences", 0);
    differenceTypes.put("errorDifferences", 0);
    differenceTypes.put("timingDifferences", 0);
    differenceTypes.put("successDifferences", 0);

    for (final Map<String, Object> comparison : comparisons) {
      final Map<String, Object> outputDiff = (Map<String, Object>) comparison.get("outputDiff");
      if ((Boolean) outputDiff.get("hasDifferences")) {
        differenceTypes.put("outputDifferences", differenceTypes.get("outputDifferences") + 1);
      }

      final Map<String, Object> errorDiff = (Map<String, Object>) comparison.get("errorDiff");
      if ((Boolean) errorDiff.get("hasDifferences")) {
        differenceTypes.put("errorDifferences", differenceTypes.get("errorDifferences") + 1);
      }

      if ((Long) comparison.get("executionTimeDiff") != 0) {
        differenceTypes.put("timingDifferences", differenceTypes.get("timingDifferences") + 1);
      }

      if ((Boolean) comparison.get("successDiff")) {
        differenceTypes.put("successDifferences", differenceTypes.get("successDifferences") + 1);
      }
    }

    summary.put("differenceTypes", differenceTypes);

    return summary;
  }

  /**
   * Creates discrepancy views for behavioral analysis.
   *
   * @param discrepancies the list of behavioral discrepancies
   * @return list of discrepancy view configurations
   */
  private List<Map<String, Object>> createDiscrepancyViews(
      final List<BehavioralDiscrepancy> discrepancies) {
    return discrepancies.stream().map(this::createDiscrepancyView).toList();
  }

  /**
   * Creates a view for a single behavioral discrepancy.
   *
   * @param discrepancy the behavioral discrepancy
   * @return discrepancy view data
   */
  private Map<String, Object> createDiscrepancyView(final BehavioralDiscrepancy discrepancy) {
    final Map<String, Object> view = new HashMap<>();

    view.put("type", discrepancy.getType().toString());
    view.put("severity", discrepancy.getSeverity().toString());
    view.put("description", discrepancy.getDescription());
    view.put("details", discrepancy.getDetails());
    view.put("recommendation", discrepancy.getRecommendation());
    view.put("detectedAt", discrepancy.getDetectedAt().toString());
    view.put("isCritical", discrepancy.isCritical());

    // Visual styling
    view.put("severityClass", getSeverityClass(discrepancy.getSeverity().toString()));
    view.put("severityIcon", getSeverityIcon(discrepancy.getSeverity().toString()));

    return view;
  }

  /**
   * Creates performance comparison visualization.
   *
   * @param testResult the test comparison result
   * @return performance comparison data
   */
  private Map<String, Object> createPerformanceComparison(final TestComparisonResult testResult) {
    final Map<String, Object> comparison = new HashMap<>();

    // Extract execution times
    final Map<String, Long> executionTimes = new HashMap<>();
    testResult
        .getRuntimeResults()
        .forEach(
            (runtime, result) ->
                executionTimes.put(runtime.toString(), result.getExecutionTime().toMillis()));

    comparison.put("executionTimes", executionTimes);

    // Calculate performance metrics
    if (!executionTimes.isEmpty()) {
      final long minTime =
          executionTimes.values().stream().mapToLong(Long::longValue).min().orElse(0);
      final long maxTime =
          executionTimes.values().stream().mapToLong(Long::longValue).max().orElse(0);
      final double avgTime =
          executionTimes.values().stream().mapToLong(Long::longValue).average().orElse(0.0);

      comparison.put("minExecutionTime", minTime);
      comparison.put("maxExecutionTime", maxTime);
      comparison.put("averageExecutionTime", avgTime);
      comparison.put("performanceVariance", maxTime > 0 ? (double) maxTime / minTime : 1.0);

      // Performance ratios relative to fastest
      final Map<String, Double> performanceRatios = new HashMap<>();
      executionTimes.forEach(
          (runtime, time) ->
              performanceRatios.put(runtime, minTime > 0 ? (double) time / minTime : 1.0));
      comparison.put("performanceRatios", performanceRatios);
    }

    return comparison;
  }

  /**
   * Formats output text for display with syntax highlighting hints.
   *
   * @param output the raw output text
   * @return formatted output with display metadata
   */
  private Map<String, Object> formatOutputForDisplay(final String output) {
    final Map<String, Object> formatted = new HashMap<>();

    formatted.put("raw", output);
    formatted.put("length", output.length());
    formatted.put("lineCount", output.split("\\r?\\n").length);

    // Detect output type for syntax highlighting
    final String outputType = detectOutputType(output);
    formatted.put("type", outputType);
    formatted.put("syntaxHighlighting", outputType);

    // Extract structured data if possible
    if ("json".equals(outputType)) {
      formatted.put("structured", parseJsonSafely(output));
    } else if ("xml".equals(outputType)) {
      formatted.put("structured", parseXmlSafely(output));
    }

    return formatted;
  }

  /**
   * Detects the type of output for appropriate formatting.
   *
   * @param output the output text to analyze
   * @return detected output type
   */
  private String detectOutputType(final String output) {
    final String trimmed = output.trim();

    if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
      return "json";
    } else if (trimmed.startsWith("[") && trimmed.endsWith("]")) {
      return "json";
    } else if (trimmed.startsWith("<") && trimmed.endsWith(">")) {
      return "xml";
    } else if (Pattern.compile("\\d{4}-\\d{2}-\\d{2}").matcher(trimmed).find()) {
      return "log";
    } else {
      return "text";
    }
  }

  /**
   * Safely parses JSON output, returning null if parsing fails.
   *
   * @param output the output to parse as JSON
   * @return parsed JSON object or null
   */
  private Object parseJsonSafely(final String output) {
    try {
      // This would use a JSON parser like Jackson in a real implementation
      return Map.of("parsed", true, "note", "JSON parsing would be implemented here");
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * Safely parses XML output, returning null if parsing fails.
   *
   * @param output the output to parse as XML
   * @return parsed XML object or null
   */
  private Object parseXmlSafely(final String output) {
    try {
      // This would use an XML parser in a real implementation
      return Map.of("parsed", true, "note", "XML parsing would be implemented here");
    } catch (final Exception e) {
      return null;
    }
  }

  /**
   * Formats metrics for display.
   *
   * @param metrics the metrics map
   * @return formatted metrics for display
   */
  private List<Map<String, Object>> formatMetricsForDisplay(final Map<String, Object> metrics) {
    return metrics.entrySet().stream()
        .map(
            entry -> {
              final Map<String, Object> metric = new HashMap<>();
              metric.put("name", entry.getKey());
              metric.put("value", entry.getValue());
              metric.put("type", entry.getValue().getClass().getSimpleName());
              return metric;
            })
        .toList();
  }

  /**
   * Splits text into lines for line-by-line comparison.
   *
   * @param text the text to split
   * @return list of lines
   */
  private List<String> splitIntoLines(final String text) {
    return List.of(text.split("\\r?\\n"));
  }

  /**
   * Gets the CSS class for a test execution result status.
   *
   * @param result the test execution result
   * @return CSS class name
   */
  private String getStatusClass(final TestExecutionResult result) {
    if (result.isSuccessful()) {
      return "status-success";
    } else if (!result.getErrorMessage().isEmpty()) {
      return "status-error";
    } else {
      return "status-warning";
    }
  }

  /**
   * Gets the icon for a test execution result status.
   *
   * @param result the test execution result
   * @return icon identifier
   */
  private String getStatusIcon(final TestExecutionResult result) {
    if (result.isSuccessful()) {
      return "check-circle";
    } else if (!result.getErrorMessage().isEmpty()) {
      return "x-circle";
    } else {
      return "alert-triangle";
    }
  }

  /**
   * Gets the CSS class for a severity level.
   *
   * @param severity the severity string
   * @return CSS class name
   */
  private String getSeverityClass(final String severity) {
    return switch (severity.toLowerCase()) {
      case "critical" -> "severity-critical";
      case "high" -> "severity-high";
      case "medium" -> "severity-medium";
      case "low" -> "severity-low";
      default -> "severity-unknown";
    };
  }

  /**
   * Gets the icon for a severity level.
   *
   * @param severity the severity string
   * @return icon identifier
   */
  private String getSeverityIcon(final String severity) {
    return switch (severity.toLowerCase()) {
      case "critical" -> "alert-octagon";
      case "high" -> "alert-triangle";
      case "medium" -> "alert-circle";
      case "low" -> "info";
      default -> "help-circle";
    };
  }
}

/** Represents a single line in a diff comparison. */
final class DiffLine {
  private final int lineNumber;
  private final String leftText;
  private final String rightText;
  private final DiffType type;

  public DiffLine(
      final int lineNumber, final String leftText, final String rightText, final DiffType type) {
    this.lineNumber = lineNumber;
    this.leftText = leftText;
    this.rightText = rightText;
    this.type = Objects.requireNonNull(type, "type cannot be null");
  }

  public int getLineNumber() {
    return lineNumber;
  }

  public String getLeftText() {
    return leftText;
  }

  public String getRightText() {
    return rightText;
  }

  public DiffType getType() {
    return type;
  }

  public Map<String, Object> toMap() {
    final Map<String, Object> map = new HashMap<>();
    map.put("lineNumber", lineNumber);
    map.put("leftText", leftText);
    map.put("rightText", rightText);
    map.put("type", type.toString());
    map.put("cssClass", type.getCssClass());
    return map;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final DiffLine diffLine = (DiffLine) obj;
    return lineNumber == diffLine.lineNumber
        && Objects.equals(leftText, diffLine.leftText)
        && Objects.equals(rightText, diffLine.rightText)
        && type == diffLine.type;
  }

  @Override
  public int hashCode() {
    return Objects.hash(lineNumber, leftText, rightText, type);
  }

  @Override
  public String toString() {
    return "DiffLine{" + "lineNumber=" + lineNumber + ", type=" + type + '}';
  }
}

/** Types of differences in a diff comparison. */
enum DiffType {
  /** Line exists in both sides and is identical. */
  UNCHANGED("diff-unchanged"),

  /** Line was added in the right side. */
  ADDED("diff-added"),

  /** Line was removed from the left side. */
  REMOVED("diff-removed"),

  /** Line exists in both sides but with different content. */
  MODIFIED("diff-modified");

  private final String cssClass;

  DiffType(final String cssClass) {
    this.cssClass = cssClass;
  }

  public String getCssClass() {
    return cssClass;
  }
}

/** Result of a diff comparison operation. */
final class DiffResult {
  private final List<DiffLine> diffLines;
  private final boolean hasDifferences;

  public DiffResult(final List<DiffLine> diffLines, final boolean hasDifferences) {
    this.diffLines = List.copyOf(diffLines);
    this.hasDifferences = hasDifferences;
  }

  public List<DiffLine> getDiffLines() {
    return diffLines;
  }

  public boolean hasDifferences() {
    return hasDifferences;
  }

  public Map<String, Object> toMap() {
    final Map<String, Object> map = new HashMap<>();
    map.put("lines", diffLines.stream().map(DiffLine::toMap).toList());
    map.put("hasDifferences", hasDifferences);
    map.put(
        "addedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.ADDED ? 1 : 0).sum());
    map.put(
        "removedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.REMOVED ? 1 : 0).sum());
    map.put(
        "modifiedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.MODIFIED ? 1 : 0).sum());
    map.put(
        "unchangedLines",
        diffLines.stream().mapToLong(line -> line.getType() == DiffType.UNCHANGED ? 1 : 0).sum());
    return map;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final DiffResult that = (DiffResult) obj;
    return hasDifferences == that.hasDifferences && Objects.equals(diffLines, that.diffLines);
  }

  @Override
  public int hashCode() {
    return Objects.hash(diffLines, hasDifferences);
  }

  @Override
  public String toString() {
    return "DiffResult{" + "lines=" + diffLines.size() + ", hasDifferences=" + hasDifferences + '}';
  }
}
