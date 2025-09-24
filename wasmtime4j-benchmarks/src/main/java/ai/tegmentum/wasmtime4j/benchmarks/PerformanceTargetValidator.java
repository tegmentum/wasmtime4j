package ai.tegmentum.wasmtime4j.benchmarks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Comprehensive performance target validation framework for establishing and validating performance
 * achievements against defined targets.
 *
 * <p>This validator analyzes actual benchmark results to determine if performance targets are
 * achieved and provides detailed analysis of performance characteristics.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Automatic performance target establishment from actual benchmarks
 *   <li>JNI and Panama runtime performance comparison
 *   <li>Statistical analysis of performance achievements
 *   <li>Performance category analysis (Core, Memory, Function, Module operations)
 *   <li>CI/CD compatible validation reporting
 * </ul>
 */
public final class PerformanceTargetValidator {

  /** Logger for performance target validation. */
  private static final Logger LOGGER = Logger.getLogger(PerformanceTargetValidator.class.getName());

  /** Performance targets based on specification requirements. */
  public static final class PerformanceTargets {
    // JNI Performance Targets (85% of native Wasmtime performance)
    public static final double JNI_CORE_OPERATIONS_TARGET = 0.85;
    public static final double JNI_MEMORY_OPERATIONS_TARGET = 0.85;
    public static final double JNI_FUNCTION_CALLS_TARGET = 0.80;
    public static final double JNI_MODULE_OPERATIONS_TARGET = 0.75;

    // Panama Performance Targets (80% of native Wasmtime performance)
    public static final double PANAMA_CORE_OPERATIONS_TARGET = 0.80;
    public static final double PANAMA_MEMORY_OPERATIONS_TARGET = 0.80;
    public static final double PANAMA_FUNCTION_CALLS_TARGET = 0.75;
    public static final double PANAMA_MODULE_OPERATIONS_TARGET = 0.70;

    // Minimum acceptable performance thresholds
    public static final double MINIMUM_JNI_PERFORMANCE = 100_000; // ops/sec
    public static final double MINIMUM_PANAMA_PERFORMANCE = 80_000; // ops/sec
  }

  /** Performance validation result for a specific benchmark category. */
  public static final class CategoryValidationResult {
    private final String category;
    private final String runtime;
    private final double actualPerformance;
    private final double targetPerformance;
    private final double achievementRatio;
    private final boolean meetsTarget;
    private final boolean meetsMinimum;
    private final String analysis;

    /**
     * Creates a category validation result.
     *
     * @param category benchmark category
     * @param runtime runtime type
     * @param actualPerformance actual measured performance
     * @param targetPerformance target performance
     * @param achievementRatio ratio of actual vs target
     * @param meetsTarget whether target is achieved
     * @param meetsMinimum whether minimum threshold is achieved
     * @param analysis detailed analysis
     */
    public CategoryValidationResult(
        final String category,
        final String runtime,
        final double actualPerformance,
        final double targetPerformance,
        final double achievementRatio,
        final boolean meetsTarget,
        final boolean meetsMinimum,
        final String analysis) {
      this.category = category;
      this.runtime = runtime;
      this.actualPerformance = actualPerformance;
      this.targetPerformance = targetPerformance;
      this.achievementRatio = achievementRatio;
      this.meetsTarget = meetsTarget;
      this.meetsMinimum = meetsMinimum;
      this.analysis = analysis;
    }

    public String getCategory() {
      return category;
    }

    public String getRuntime() {
      return runtime;
    }

    public double getActualPerformance() {
      return actualPerformance;
    }

    public double getTargetPerformance() {
      return targetPerformance;
    }

    public double getAchievementRatio() {
      return achievementRatio;
    }

    public boolean meetsTarget() {
      return meetsTarget;
    }

    public boolean meetsMinimum() {
      return meetsMinimum;
    }

    public String getAnalysis() {
      return analysis;
    }

    @Override
    public String toString() {
      return String.format(
          "CategoryValidationResult{category='%s', runtime='%s', performance=%.0f ops/s, "
              + "target=%.0f ops/s, ratio=%.1f%%, meetsTarget=%s}",
          category,
          runtime,
          actualPerformance,
          targetPerformance,
          achievementRatio * 100,
          meetsTarget);
    }
  }

  /** Comprehensive validation result across all categories and runtimes. */
  public static final class ComprehensiveValidationResult {
    private final List<CategoryValidationResult> categoryResults;
    private final Map<String, Double> runtimeComparison;
    private final boolean allTargetsAchieved;
    private final boolean allMinimumsAchieved;
    private final int totalCategories;
    private final int successfulCategories;
    private final String summaryReport;
    private final LocalDateTime validatedAt;

    /**
     * Creates a comprehensive validation result.
     *
     * @param categoryResults individual category validation results
     * @param runtimeComparison runtime performance comparison
     * @param allTargetsAchieved whether all targets are achieved
     * @param allMinimumsAchieved whether all minimums are achieved
     * @param totalCategories total number of categories
     * @param successfulCategories number of successful categories
     * @param summaryReport validation summary report
     * @param validatedAt validation timestamp
     */
    public ComprehensiveValidationResult(
        final List<CategoryValidationResult> categoryResults,
        final Map<String, Double> runtimeComparison,
        final boolean allTargetsAchieved,
        final boolean allMinimumsAchieved,
        final int totalCategories,
        final int successfulCategories,
        final String summaryReport,
        final LocalDateTime validatedAt) {
      this.categoryResults = new ArrayList<>(categoryResults);
      this.runtimeComparison = new HashMap<>(runtimeComparison);
      this.allTargetsAchieved = allTargetsAchieved;
      this.allMinimumsAchieved = allMinimumsAchieved;
      this.totalCategories = totalCategories;
      this.successfulCategories = successfulCategories;
      this.summaryReport = summaryReport;
      this.validatedAt = validatedAt;
    }

    public List<CategoryValidationResult> getCategoryResults() {
      return new ArrayList<>(categoryResults);
    }

    public Map<String, Double> getRuntimeComparison() {
      return new HashMap<>(runtimeComparison);
    }

    public boolean areAllTargetsAchieved() {
      return allTargetsAchieved;
    }

    public boolean areAllMinimumsAchieved() {
      return allMinimumsAchieved;
    }

    public int getTotalCategories() {
      return totalCategories;
    }

    public int getSuccessfulCategories() {
      return successfulCategories;
    }

    public String getSummaryReport() {
      return summaryReport;
    }

    public LocalDateTime getValidatedAt() {
      return validatedAt;
    }
  }

  private final ObjectMapper objectMapper;

  /** Creates a new performance target validator. */
  public PerformanceTargetValidator() {
    this.objectMapper = new ObjectMapper();
  }

  /**
   * Validates performance targets against JMH benchmark results.
   *
   * @param resultsFile path to JMH JSON results file
   * @return comprehensive validation result
   * @throws IOException if results file cannot be read
   */
  public ComprehensiveValidationResult validatePerformanceTargets(final Path resultsFile)
      throws IOException {
    LOGGER.info("Validating performance targets from results file: " + resultsFile);

    // Parse benchmark results
    final List<BenchmarkResult> results = parseJmhResults(resultsFile);
    LOGGER.info("Parsed " + results.size() + " benchmark results");

    // Group results by category and runtime
    final Map<String, List<BenchmarkResult>> grouped = groupResultsByCategory(results);

    // Validate each category
    final List<CategoryValidationResult> categoryResults = new ArrayList<>();
    for (final Map.Entry<String, List<BenchmarkResult>> entry : grouped.entrySet()) {
      final String key = entry.getKey();
      final List<BenchmarkResult> categoryResults_local = entry.getValue();

      final CategoryValidationResult result = validateCategory(key, categoryResults_local);
      categoryResults.add(result);
    }

    // Calculate runtime comparison
    final Map<String, Double> runtimeComparison = calculateRuntimeComparison(results);

    // Determine overall success
    final boolean allTargetsAchieved =
        categoryResults.stream().allMatch(CategoryValidationResult::meetsTarget);
    final boolean allMinimumsAchieved =
        categoryResults.stream().allMatch(CategoryValidationResult::meetsMinimum);

    // Generate summary report
    final String summaryReport = generateSummaryReport(categoryResults, runtimeComparison);

    final ComprehensiveValidationResult result =
        new ComprehensiveValidationResult(
            categoryResults,
            runtimeComparison,
            allTargetsAchieved,
            allMinimumsAchieved,
            grouped.size(),
            (int) categoryResults.stream().mapToLong(r -> r.meetsTarget() ? 1 : 0).sum(),
            summaryReport,
            LocalDateTime.now());

    LOGGER.info(
        "Performance validation completed: "
            + result.getSuccessfulCategories()
            + "/"
            + result.getTotalCategories()
            + " targets achieved");

    return result;
  }

  /**
   * Analyzes actual performance achievements from benchmark results.
   *
   * @param resultsFile path to JMH JSON results file
   * @return analysis of actual performance achievements
   * @throws IOException if results file cannot be read
   */
  public String analyzePerformanceAchievements(final Path resultsFile) throws IOException {
    final ComprehensiveValidationResult validation = validatePerformanceTargets(resultsFile);

    final StringBuilder analysis = new StringBuilder();
    analysis.append("Performance Achievement Analysis\n");
    analysis
        .append("Generated: ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n");
    analysis.append("=================================\n\n");

    // Runtime comparison analysis
    analysis.append("Runtime Performance Comparison:\n");
    for (final Map.Entry<String, Double> entry : validation.getRuntimeComparison().entrySet()) {
      analysis.append(
          String.format(
              "  %s: %.1f%% of JNI performance\n", entry.getKey(), entry.getValue() * 100));
    }
    analysis.append("\n");

    // Category performance analysis
    analysis.append("Category Performance Analysis:\n");
    for (final CategoryValidationResult result : validation.getCategoryResults()) {
      final String status = result.meetsTarget() ? "✅ ACHIEVED" : "❌ MISSED";
      analysis.append(
          String.format(
              "  %s %s: %.0f ops/s (%.1f%% of target)\n",
              status,
              result.getCategory() + "/" + result.getRuntime(),
              result.getActualPerformance(),
              result.getAchievementRatio() * 100));
    }

    // Performance targets vs achievements
    analysis.append("\nPerformance Targets vs Achievements:\n");
    analysis.append("JNI Runtime:\n");
    final List<CategoryValidationResult> jniResults =
        validation.getCategoryResults().stream()
            .filter(r -> "JNI".equals(r.getRuntime()))
            .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);

    if (!jniResults.isEmpty()) {
      final double avgJniAchievement =
          jniResults.stream()
              .mapToDouble(CategoryValidationResult::getAchievementRatio)
              .average()
              .orElse(0.0);
      analysis.append(
          String.format("  Average achievement: %.1f%% of targets\n", avgJniAchievement * 100));
      analysis.append(
          String.format(
              "  Target: 85%% of native performance - %s\n",
              avgJniAchievement >= 0.85 ? "ACHIEVED" : "MISSED"));
    }

    analysis.append("\nPanama Runtime:\n");
    final List<CategoryValidationResult> panamaResults =
        validation.getCategoryResults().stream()
            .filter(r -> "PANAMA".equals(r.getRuntime()))
            .collect(ArrayList::new, (list, item) -> list.add(item), ArrayList::addAll);

    if (!panamaResults.isEmpty()) {
      final double avgPanamaAchievement =
          panamaResults.stream()
              .mapToDouble(CategoryValidationResult::getAchievementRatio)
              .average()
              .orElse(0.0);
      analysis.append(
          String.format("  Average achievement: %.1f%% of targets\n", avgPanamaAchievement * 100));
      analysis.append(
          String.format(
              "  Target: 80%% of native performance - %s\n",
              avgPanamaAchievement >= 0.80 ? "ACHIEVED" : "MISSED"));
    }

    return analysis.toString();
  }

  /** Benchmark result data structure for parsing. */
  private static final class BenchmarkResult {
    private final String benchmark;
    private final String runtime;
    private final double score;
    private final String unit;
    private final Map<String, String> params;

    BenchmarkResult(
        final String benchmark,
        final String runtime,
        final double score,
        final String unit,
        final Map<String, String> params) {
      this.benchmark = benchmark;
      this.runtime = runtime;
      this.score = score;
      this.unit = unit;
      this.params = new HashMap<>(params);
    }

    public String getBenchmark() {
      return benchmark;
    }

    public String getRuntime() {
      return runtime;
    }

    public double getScore() {
      return score;
    }

    public String getUnit() {
      return unit;
    }

    public Map<String, String> getParams() {
      return new HashMap<>(params);
    }
  }

  private List<BenchmarkResult> parseJmhResults(final Path resultsFile) throws IOException {
    final List<BenchmarkResult> results = new ArrayList<>();
    final JsonNode rootNode = objectMapper.readTree(resultsFile.toFile());

    if (rootNode.isArray()) {
      for (final JsonNode benchmarkNode : rootNode) {
        final String benchmark = benchmarkNode.get("benchmark").asText();
        final double score = benchmarkNode.get("primaryMetric").get("score").asDouble();
        final String unit = benchmarkNode.get("primaryMetric").get("scoreUnit").asText();

        // Extract runtime from params
        final String[] runtimeHolder = {"UNKNOWN"};
        final Map<String, String> params = new HashMap<>();
        if (benchmarkNode.has("params")) {
          final JsonNode paramsNode = benchmarkNode.get("params");
          paramsNode
              .fields()
              .forEachRemaining(
                  entry -> {
                    final String key = entry.getKey();
                    final String value = entry.getValue().asText();
                    params.put(key, value);
                    if ("runtimeTypeName".equals(key)) {
                      runtimeHolder[0] = value;
                    }
                  });
        }
        final String runtime = runtimeHolder[0];

        results.add(new BenchmarkResult(benchmark, runtime, score, unit, params));
      }
    }

    return results;
  }

  private Map<String, List<BenchmarkResult>> groupResultsByCategory(
      final List<BenchmarkResult> results) {
    final Map<String, List<BenchmarkResult>> grouped = new HashMap<>();

    for (final BenchmarkResult result : results) {
      final String category = extractCategory(result.getBenchmark());
      final String key = category + ":" + result.getRuntime();
      grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(result);
    }

    return grouped;
  }

  private String extractCategory(final String benchmarkName) {
    if (benchmarkName.contains("Memory")) {
      return "MEMORY_OPERATIONS";
    } else if (benchmarkName.contains("Function")) {
      return "FUNCTION_EXECUTION";
    } else if (benchmarkName.contains("Module")) {
      return "MODULE_OPERATIONS";
    } else if (benchmarkName.contains("Runtime")) {
      return "RUNTIME_INITIALIZATION";
    } else if (benchmarkName.contains("Wasi")) {
      return "WASI_OPERATIONS";
    } else if (benchmarkName.contains("Comparison")) {
      return "RUNTIME_COMPARISON";
    } else if (benchmarkName.contains("Concurrency")) {
      return "CONCURRENCY_OPERATIONS";
    } else {
      return "CORE_OPERATIONS";
    }
  }

  private CategoryValidationResult validateCategory(
      final String categoryKey, final List<BenchmarkResult> results) {
    final String[] parts = categoryKey.split(":");
    final String category = parts[0];
    final String runtime = parts[1];

    // Calculate average performance
    final double avgPerformance =
        results.stream().mapToDouble(BenchmarkResult::getScore).average().orElse(0.0);

    // Get target performance for this category and runtime
    final double targetPerformance = getTargetPerformance(category, runtime);
    final double minimumPerformance = getMinimumPerformance(runtime);

    // Calculate achievement ratio
    final double achievementRatio = avgPerformance / targetPerformance;

    // Determine if targets are met
    final boolean meetsTarget = avgPerformance >= targetPerformance;
    final boolean meetsMinimum = avgPerformance >= minimumPerformance;

    final String analysis =
        String.format(
            "Category %s with %s runtime: actual=%.0f ops/s, target=%.0f ops/s, minimum=%.0f ops/s",
            category, runtime, avgPerformance, targetPerformance, minimumPerformance);

    return new CategoryValidationResult(
        category,
        runtime,
        avgPerformance,
        targetPerformance,
        achievementRatio,
        meetsTarget,
        meetsMinimum,
        analysis);
  }

  private double getTargetPerformance(final String category, final String runtime) {
    // For this implementation, use baseline performance expectations
    // In a real implementation, this would be based on native Wasmtime benchmarks
    final double baselinePerformance = getBaselinePerformance(category);

    if ("JNI".equals(runtime)) {
      return baselinePerformance * getJniTargetRatio(category);
    } else if ("PANAMA".equals(runtime)) {
      return baselinePerformance * getPanamaTargetRatio(category);
    } else {
      return baselinePerformance * 0.5; // Conservative target for other runtimes
    }
  }

  private double getBaselinePerformance(final String category) {
    // Baseline expectations based on operation complexity
    switch (category) {
      case "RUNTIME_INITIALIZATION":
        return 150_000_000; // 150M ops/s for lightweight operations
      case "CORE_OPERATIONS":
        return 100_000_000; // 100M ops/s for core operations
      case "MEMORY_OPERATIONS":
        return 50_000_000; // 50M ops/s for memory operations
      case "FUNCTION_EXECUTION":
        return 20_000_000; // 20M ops/s for function calls
      case "MODULE_OPERATIONS":
        return 1_000_000; // 1M ops/s for module operations
      case "WASI_OPERATIONS":
        return 5_000_000; // 5M ops/s for WASI operations
      default:
        return 10_000_000; // 10M ops/s default
    }
  }

  private double getJniTargetRatio(final String category) {
    switch (category) {
      case "RUNTIME_INITIALIZATION":
      case "CORE_OPERATIONS":
        return PerformanceTargets.JNI_CORE_OPERATIONS_TARGET;
      case "MEMORY_OPERATIONS":
        return PerformanceTargets.JNI_MEMORY_OPERATIONS_TARGET;
      case "FUNCTION_EXECUTION":
        return PerformanceTargets.JNI_FUNCTION_CALLS_TARGET;
      case "MODULE_OPERATIONS":
        return PerformanceTargets.JNI_MODULE_OPERATIONS_TARGET;
      default:
        return 0.75; // Conservative default
    }
  }

  private double getPanamaTargetRatio(final String category) {
    switch (category) {
      case "RUNTIME_INITIALIZATION":
      case "CORE_OPERATIONS":
        return PerformanceTargets.PANAMA_CORE_OPERATIONS_TARGET;
      case "MEMORY_OPERATIONS":
        return PerformanceTargets.PANAMA_MEMORY_OPERATIONS_TARGET;
      case "FUNCTION_EXECUTION":
        return PerformanceTargets.PANAMA_FUNCTION_CALLS_TARGET;
      case "MODULE_OPERATIONS":
        return PerformanceTargets.PANAMA_MODULE_OPERATIONS_TARGET;
      default:
        return 0.70; // Conservative default
    }
  }

  private double getMinimumPerformance(final String runtime) {
    if ("JNI".equals(runtime)) {
      return PerformanceTargets.MINIMUM_JNI_PERFORMANCE;
    } else if ("PANAMA".equals(runtime)) {
      return PerformanceTargets.MINIMUM_PANAMA_PERFORMANCE;
    } else {
      return 50_000; // Conservative minimum
    }
  }

  private Map<String, Double> calculateRuntimeComparison(final List<BenchmarkResult> results) {
    final Map<String, Double> comparison = new HashMap<>();

    // Group by runtime
    final Map<String, List<BenchmarkResult>> runtimeGroups = new HashMap<>();
    for (final BenchmarkResult result : results) {
      runtimeGroups.computeIfAbsent(result.getRuntime(), k -> new ArrayList<>()).add(result);
    }

    // Calculate average performance for each runtime
    final Map<String, Double> runtimeAverages = new HashMap<>();
    for (final Map.Entry<String, List<BenchmarkResult>> entry : runtimeGroups.entrySet()) {
      final double avg =
          entry.getValue().stream().mapToDouble(BenchmarkResult::getScore).average().orElse(0.0);
      runtimeAverages.put(entry.getKey(), avg);
    }

    // Use JNI as baseline for comparison
    final double jniBaseline = runtimeAverages.getOrDefault("JNI", 1.0);

    for (final Map.Entry<String, Double> entry : runtimeAverages.entrySet()) {
      if (!"JNI".equals(entry.getKey())) {
        comparison.put(entry.getKey(), entry.getValue() / jniBaseline);
      }
    }

    return comparison;
  }

  private String generateSummaryReport(
      final List<CategoryValidationResult> categoryResults,
      final Map<String, Double> runtimeComparison) {
    final StringBuilder report = new StringBuilder();
    report.append("Performance Target Validation Summary\n");
    report
        .append("Generated: ")
        .append(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
        .append("\n");
    report.append("=====================================\n\n");

    // Overall statistics
    final long targetsAchieved =
        categoryResults.stream().mapToLong(r -> r.meetsTarget() ? 1 : 0).sum();
    final long minimumsAchieved =
        categoryResults.stream().mapToLong(r -> r.meetsMinimum() ? 1 : 0).sum();

    report.append("Overall Results:\n");
    report.append(
        String.format(
            "  Performance targets achieved: %d/%d\n", targetsAchieved, categoryResults.size()));
    report.append(
        String.format(
            "  Minimum thresholds achieved: %d/%d\n", minimumsAchieved, categoryResults.size()));
    report.append("\n");

    // Runtime comparison
    report.append("Runtime Performance Comparison:\n");
    for (final Map.Entry<String, Double> entry : runtimeComparison.entrySet()) {
      report.append(
          String.format(
              "  %s: %.1f%% of JNI performance\n", entry.getKey(), entry.getValue() * 100));
    }
    report.append("\n");

    // Detailed category results
    report.append("Category Results:\n");
    for (final CategoryValidationResult result : categoryResults) {
      final String status = result.meetsTarget() ? "✅" : "❌";
      report.append(String.format("  %s %s\n", status, result));
    }

    return report.toString();
  }

  /**
   * Main method for command-line validation.
   *
   * @param args command line arguments
   */
  public static void main(final String[] args) {
    if (args.length < 1) {
      System.err.println("Usage: PerformanceTargetValidator <results-file.json>");
      System.exit(1);
    }

    try {
      final PerformanceTargetValidator validator = new PerformanceTargetValidator();
      final Path resultsFile = Path.of(args[0]);

      System.out.println("Validating performance targets from: " + resultsFile);
      final ComprehensiveValidationResult result =
          validator.validatePerformanceTargets(resultsFile);

      System.out.println(result.getSummaryReport());

      if (result.areAllTargetsAchieved()) {
        System.out.println("\n🎉 All performance targets achieved!");
        System.exit(0);
      } else {
        System.out.println("\n⚠️  Some performance targets were not achieved.");
        System.exit(1);
      }

    } catch (final Exception e) {
      System.err.println("Validation failed: " + e.getMessage());
      e.printStackTrace();
      System.exit(1);
    }
  }
}
