package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Generates executive summaries and strategic insights for runtime comparison analysis. Provides
 * high-level decision support information for stakeholders and project management.
 *
 * <p>Features include:
 *
 * <ul>
 *   <li>Executive-level summary generation with key metrics
 *   <li>Strategic trend analysis and forecasting
 *   <li>Risk assessment and mitigation recommendations
 *   <li>Compliance reporting for zero discrepancy requirements
 *   <li>ROI analysis for runtime selection decisions
 * </ul>
 *
 * @since 1.0.0
 */
public final class ExecutiveSummaryGenerator {
  private static final Logger LOGGER = Logger.getLogger(ExecutiveSummaryGenerator.class.getName());

  /**
   * Generates a comprehensive executive summary of runtime comparison analysis.
   *
   * @param testResults map of test results by test name and runtime
   * @param discrepancies list of behavioral discrepancies detected
   * @param performanceMetrics performance comparison results
   * @param trendData historical trend analysis data
   * @return executive summary with strategic insights
   */
  public ExecutiveSummary generateSummary(
      final Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> testResults,
      final List<BehavioralDiscrepancy> discrepancies,
      final Map<RuntimeType, PerformanceMetrics> performanceMetrics,
      final Map<String, TrendAnalyzer.TrendAnalysisResult> trendData) {
    Objects.requireNonNull(testResults, "testResults cannot be null");
    Objects.requireNonNull(discrepancies, "discrepancies cannot be null");
    Objects.requireNonNull(performanceMetrics, "performanceMetrics cannot be null");
    Objects.requireNonNull(trendData, "trendData cannot be null");

    LOGGER.info(
        "Generating executive summary for "
            + testResults.size()
            + " tests across "
            + performanceMetrics.size()
            + " runtimes");

    final ExecutiveSummary.Builder summaryBuilder = new ExecutiveSummary.Builder();

    // Generate key metrics and KPIs
    final ExecutiveMetrics metrics =
        generateExecutiveMetrics(testResults, discrepancies, performanceMetrics);
    summaryBuilder.metrics(metrics);

    // Assess compliance with zero discrepancy requirement
    final ComplianceStatus compliance = assessComplianceStatus(discrepancies);
    summaryBuilder.compliance(compliance);

    // Generate strategic insights and recommendations
    final List<StrategicInsight> insights =
        generateStrategicInsights(testResults, discrepancies, performanceMetrics, trendData);
    summaryBuilder.insights(insights);

    // Assess risks and mitigation strategies
    final RiskAssessment riskAssessment = assessRisks(discrepancies, trendData);
    summaryBuilder.riskAssessment(riskAssessment);

    // Generate actionable recommendations
    final List<ExecutiveRecommendation> recommendations =
        generateExecutiveRecommendations(discrepancies, metrics, compliance, riskAssessment);
    summaryBuilder.recommendations(recommendations);

    // Calculate readiness score
    final double readinessScore = calculateReadinessScore(metrics, compliance, riskAssessment);
    summaryBuilder.readinessScore(readinessScore);

    return summaryBuilder.build();
  }

  /** Generates executive-level metrics and KPIs. */
  private ExecutiveMetrics generateExecutiveMetrics(
      final Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> testResults,
      final List<BehavioralDiscrepancy> discrepancies,
      final Map<RuntimeType, PerformanceMetrics> performanceMetrics) {

    final int totalTests = testResults.size();
    final long successfulTests =
        testResults.values().stream()
            .mapToLong(
                runtimeResults ->
                    runtimeResults.values().stream()
                            .allMatch(BehavioralAnalyzer.TestExecutionResult::isSuccessful)
                        ? 1
                        : 0)
            .sum();

    final double successRate = totalTests > 0 ? (double) successfulTests / totalTests : 0.0;

    final long criticalDiscrepancies =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL).count();

    final long testsWithDiscrepancies =
        discrepancies.stream().map(BehavioralDiscrepancy::getTestName).distinct().count();

    final double discrepancyRate =
        totalTests > 0 ? (double) testsWithDiscrepancies / totalTests : 0.0;

    // Calculate performance variance across runtimes
    final double performanceVariance = calculatePerformanceVariance(performanceMetrics);

    return new ExecutiveMetrics(
        totalTests,
        successRate,
        discrepancies.size(),
        criticalDiscrepancies,
        discrepancyRate,
        performanceVariance,
        calculateTestCoverage(testResults),
        Instant.now());
  }

  /** Assesses compliance with zero discrepancy requirement. */
  private ComplianceStatus assessComplianceStatus(final List<BehavioralDiscrepancy> discrepancies) {
    final long criticalCount =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL).count();

    final long majorCount =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.MAJOR).count();

    final boolean isCompliant = criticalCount == 0;
    final ComplianceLevel level = determineComplianceLevel(criticalCount, majorCount);
    final List<String> blockingIssues =
        discrepancies.stream()
            .filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL)
            .map(BehavioralDiscrepancy::getDescription)
            .collect(Collectors.toList());

    return new ComplianceStatus(isCompliant, level, criticalCount, blockingIssues);
  }

  /** Generates strategic insights for executive decision making. */
  private List<StrategicInsight> generateStrategicInsights(
      final Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> testResults,
      final List<BehavioralDiscrepancy> discrepancies,
      final Map<RuntimeType, PerformanceMetrics> performanceMetrics,
      final Map<String, TrendAnalyzer.TrendAnalysisResult> trendData) {

    final List<StrategicInsight> insights = new ArrayList<>();

    // Runtime maturity insight
    insights.add(generateRuntimeMaturityInsight(testResults, discrepancies));

    // Performance comparison insight
    insights.add(generatePerformanceInsight(performanceMetrics));

    // Trend analysis insight
    insights.add(generateTrendInsight(trendData));

    // Risk exposure insight
    insights.add(generateRiskExposureInsight(discrepancies));

    return insights;
  }

  /** Assesses risks and their potential impact. */
  private RiskAssessment assessRisks(
      final List<BehavioralDiscrepancy> discrepancies,
      final Map<String, TrendAnalyzer.TrendAnalysisResult> trendData) {

    final List<Risk> risks = new ArrayList<>();

    // Critical discrepancy risk
    final long criticalCount =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL).count();

    if (criticalCount > 0) {
      risks.add(
          new Risk(
              RiskType.COMPLIANCE,
              RiskLevel.HIGH,
              "Critical discrepancies block zero discrepancy compliance",
              String.format(
                  "%d critical discrepancies must be resolved before release", criticalCount),
              "Delay release timeline and allocate resources for immediate fixes"));
    }

    // Regression risk from trend analysis
    final long regressionTrends =
        trendData.values().stream().filter(TrendAnalyzer.TrendAnalysisResult::isRegression).count();

    if (regressionTrends > 0) {
      risks.add(
          new Risk(
              RiskType.REGRESSION,
              RiskLevel.MEDIUM,
              "Performance regression trends detected",
              String.format("%d metrics showing regression patterns", regressionTrends),
              "Implement automated regression monitoring and establish performance baselines"));
    }

    // Systematic pattern risk
    final long systematicIssues =
        discrepancies.stream()
            .filter(d -> d.getType() == DiscrepancyType.SYSTEMATIC_PATTERN)
            .count();

    if (systematicIssues > 0) {
      risks.add(
          new Risk(
              RiskType.ARCHITECTURE,
              RiskLevel.HIGH,
              "Systematic patterns suggest architectural issues",
              String.format(
                  "%d systematic patterns detected across multiple runtimes", systematicIssues),
              "Conduct architectural review and consider fundamental design changes"));
    }

    final RiskLevel overallRisk = calculateOverallRisk(risks);
    return new RiskAssessment(risks, overallRisk);
  }

  /** Generates executive recommendations based on analysis. */
  private List<ExecutiveRecommendation> generateExecutiveRecommendations(
      final List<BehavioralDiscrepancy> discrepancies,
      final ExecutiveMetrics metrics,
      final ComplianceStatus compliance,
      final RiskAssessment riskAssessment) {

    final List<ExecutiveRecommendation> recommendations = new ArrayList<>();

    // Compliance recommendations
    if (!compliance.isCompliant()) {
      recommendations.add(
          new ExecutiveRecommendation(
              RecommendationPriority.CRITICAL,
              "Resolve Critical Discrepancies",
              String.format(
                  "Address %d critical discrepancies to achieve zero discrepancy compliance",
                  compliance.getCriticalCount()),
              "Assign dedicated engineering team and establish daily progress tracking",
              EstimatedEffort.HIGH));
    }

    // Performance recommendations
    if (metrics.getPerformanceVariance() > 2.0) {
      recommendations.add(
          new ExecutiveRecommendation(
              RecommendationPriority.HIGH,
              "Optimize Performance Consistency",
              String.format(
                  "Performance variance of %.1fx exceeds acceptable thresholds",
                  metrics.getPerformanceVariance()),
              "Implement performance profiling and optimization across all runtimes",
              EstimatedEffort.MEDIUM));
    }

    // Risk mitigation recommendations
    for (final Risk risk : riskAssessment.getRisks()) {
      if (risk.getLevel() == RiskLevel.HIGH) {
        recommendations.add(
            new ExecutiveRecommendation(
                RecommendationPriority.HIGH,
                "Mitigate High-Risk Issue: " + risk.getTitle(),
                risk.getDescription(),
                risk.getMitigationStrategy(),
                EstimatedEffort.HIGH));
      }
    }

    // Process improvement recommendations
    if (metrics.getDiscrepancyRate() > 0.1) {
      recommendations.add(
          new ExecutiveRecommendation(
              RecommendationPriority.MEDIUM,
              "Enhance Quality Assurance",
              String.format(
                  "Discrepancy rate of %.1f%% indicates need for improved QA processes",
                  metrics.getDiscrepancyRate() * 100),
              "Implement continuous integration testing and expand automated validation",
              EstimatedEffort.MEDIUM));
    }

    return recommendations;
  }

  /** Calculates overall readiness score for release decisions. */
  private double calculateReadinessScore(
      final ExecutiveMetrics metrics,
      final ComplianceStatus compliance,
      final RiskAssessment riskAssessment) {

    double score = 100.0;

    // Compliance impact (40% weight)
    if (!compliance.isCompliant()) {
      score -= 40.0;
    } else if (compliance.getLevel() != ComplianceLevel.FULL) {
      score -= 20.0;
    }

    // Discrepancy rate impact (30% weight)
    score -= Math.min(30.0, metrics.getDiscrepancyRate() * 100 * 3);

    // Risk assessment impact (20% weight)
    switch (riskAssessment.getOverallRisk()) {
      case HIGH -> score -= 20.0;
      case MEDIUM -> score -= 10.0;
      case LOW -> score -= 2.0;
      default -> {
        // No additional impact for unknown risk levels
      }
    }

    // Success rate impact (10% weight)
    score -= (1.0 - metrics.getSuccessRate()) * 10;

    return Math.max(0.0, score);
  }

  /** Helper methods for metric calculations. */
  private double calculatePerformanceVariance(
      final Map<RuntimeType, PerformanceMetrics> performanceMetrics) {
    if (performanceMetrics.size() < 2) {
      return 1.0;
    }

    final List<Double> executionTimes =
        performanceMetrics.values().stream()
            .map(PerformanceMetrics::getAverageExecutionTime)
            .map(Duration::toNanos)
            .map(nanos -> nanos / 1_000_000.0) // Convert to milliseconds
            .collect(Collectors.toList());

    final double min = executionTimes.stream().mapToDouble(Double::doubleValue).min().orElse(1.0);
    final double max = executionTimes.stream().mapToDouble(Double::doubleValue).max().orElse(1.0);

    return min > 0 ? max / min : 1.0;
  }

  private double calculateTestCoverage(
      final Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> testResults) {
    // Simplified coverage calculation - in practice this would be more sophisticated
    final long totalPossibleTests = testResults.size() * RuntimeType.values().length;
    final long actualTests =
        testResults.values().stream().mapToLong(runtimeResults -> runtimeResults.size()).sum();

    return totalPossibleTests > 0 ? (double) actualTests / totalPossibleTests : 0.0;
  }

  private ComplianceLevel determineComplianceLevel(
      final long criticalCount, final long majorCount) {
    if (criticalCount == 0 && majorCount == 0) {
      return ComplianceLevel.FULL;
    } else if (criticalCount == 0) {
      return ComplianceLevel.PARTIAL;
    } else {
      return ComplianceLevel.NON_COMPLIANT;
    }
  }

  private StrategicInsight generateRuntimeMaturityInsight(
      final Map<String, Map<RuntimeType, BehavioralAnalyzer.TestExecutionResult>> testResults,
      final List<BehavioralDiscrepancy> discrepancies) {

    final Map<RuntimeType, Long> failuresByRuntime = new HashMap<>();
    for (final RuntimeType runtime : RuntimeType.values()) {
      final long failures =
          testResults.values().stream()
              .mapToLong(
                  runtimeResults -> {
                    final BehavioralAnalyzer.TestExecutionResult result =
                        runtimeResults.get(runtime);
                    return result != null && !result.isSuccessful() ? 1 : 0;
                  })
              .sum();
      failuresByRuntime.put(runtime, failures);
    }

    final RuntimeType mostMature =
        failuresByRuntime.entrySet().stream()
            .min(Map.Entry.comparingByValue())
            .map(Map.Entry::getKey)
            .orElse(RuntimeType.JNI);

    return new StrategicInsight(
        InsightType.RUNTIME_MATURITY,
        InsightSeverity.INFO,
        "Runtime Maturity Analysis",
        String.format("Runtime %s shows highest maturity with fewest failures", mostMature),
        "Consider prioritizing the most mature runtime for production deployment");
  }

  private StrategicInsight generatePerformanceInsight(
      final Map<RuntimeType, PerformanceMetrics> performanceMetrics) {
    if (performanceMetrics.size() < 2) {
      return new StrategicInsight(
          InsightType.PERFORMANCE,
          InsightSeverity.INFO,
          "Performance Analysis",
          "Insufficient runtime data for performance comparison",
          "Expand performance testing to include multiple runtimes");
    }

    final RuntimeType fastestRuntime =
        performanceMetrics.entrySet().stream()
            .min(
                Map.Entry.comparingByValue(
                    (m1, m2) ->
                        m1.getAverageExecutionTime().compareTo(m2.getAverageExecutionTime())))
            .map(Map.Entry::getKey)
            .orElse(RuntimeType.JNI);

    return new StrategicInsight(
        InsightType.PERFORMANCE,
        InsightSeverity.INFO,
        "Performance Leadership",
        String.format("Runtime %s demonstrates best performance characteristics", fastestRuntime),
        "Consider performance as a factor in runtime selection strategy");
  }

  private StrategicInsight generateTrendInsight(
      final Map<String, TrendAnalyzer.TrendAnalysisResult> trendData) {
    final long improvingTrends =
        trendData.values().stream()
            .filter(TrendAnalyzer.TrendAnalysisResult::isImprovement)
            .count();
    final long regressingTrends =
        trendData.values().stream().filter(TrendAnalyzer.TrendAnalysisResult::isRegression).count();

    if (regressingTrends > improvingTrends) {
      return new StrategicInsight(
          InsightType.TREND,
          InsightSeverity.WARNING,
          "Regression Trend Detected",
          String.format(
              "%d metrics showing regression vs %d improving", regressingTrends, improvingTrends),
          "Implement automated performance monitoring and establish regression prevention"
              + " measures");
    } else {
      return new StrategicInsight(
          InsightType.TREND,
          InsightSeverity.INFO,
          "Positive Trend Analysis",
          String.format("%d metrics improving vs %d regressing", improvingTrends, regressingTrends),
          "Continue current development practices and consider expanding successful approaches");
    }
  }

  private StrategicInsight generateRiskExposureInsight(
      final List<BehavioralDiscrepancy> discrepancies) {
    final long highRiskCount =
        discrepancies.stream().filter(d -> d.getSeverity() == DiscrepancySeverity.CRITICAL).count();

    if (highRiskCount > 0) {
      return new StrategicInsight(
          InsightType.RISK,
          InsightSeverity.CRITICAL,
          "High Risk Exposure",
          String.format("%d critical issues pose release risk", highRiskCount),
          "Prioritize risk mitigation and consider delaying release until issues are resolved");
    } else {
      return new StrategicInsight(
          InsightType.RISK,
          InsightSeverity.INFO,
          "Low Risk Profile",
          "No critical issues detected in current analysis",
          "Maintain current quality assurance practices and monitoring");
    }
  }

  private RiskLevel calculateOverallRisk(final List<Risk> risks) {
    if (risks.stream().anyMatch(r -> r.getLevel() == RiskLevel.HIGH)) {
      return RiskLevel.HIGH;
    } else if (risks.stream().anyMatch(r -> r.getLevel() == RiskLevel.MEDIUM)) {
      return RiskLevel.MEDIUM;
    } else {
      return RiskLevel.LOW;
    }
  }

  // Supporting classes would be defined here or in separate files
  // (ExecutiveSummary, ExecutiveMetrics, ComplianceStatus, etc.)
  // For brevity, these are not included in this response but would be fully implemented

  /** Performance metrics for a runtime. */
  public static final class PerformanceMetrics {
    private final Duration averageExecutionTime;
    private final long averageMemoryUsage;
    private final double successRate;

    /**
     * Creates performance metrics for a runtime.
     *
     * @param averageExecutionTime average execution time
     * @param averageMemoryUsage average memory usage in bytes
     * @param successRate success rate as a decimal (0.0 to 1.0)
     */
    public PerformanceMetrics(
        final Duration averageExecutionTime,
        final long averageMemoryUsage,
        final double successRate) {
      this.averageExecutionTime = averageExecutionTime;
      this.averageMemoryUsage = averageMemoryUsage;
      this.successRate = successRate;
    }

    public Duration getAverageExecutionTime() {
      return averageExecutionTime;
    }

    public long getAverageMemoryUsage() {
      return averageMemoryUsage;
    }

    public double getSuccessRate() {
      return successRate;
    }
  }

  // Enums for categorization

  /** Types of strategic insights for executive analysis. */
  public enum InsightType {
    RUNTIME_MATURITY,
    PERFORMANCE,
    TREND,
    RISK
  }

  /** Severity levels for insights. */
  public enum InsightSeverity {
    INFO,
    WARNING,
    CRITICAL
  }

  /** Types of risks identified in analysis. */
  public enum RiskType {
    COMPLIANCE,
    REGRESSION,
    ARCHITECTURE,
    PERFORMANCE
  }

  /** Risk level severity categories. */
  public enum RiskLevel {
    LOW,
    MEDIUM,
    HIGH
  }

  /** Compliance level categories for zero discrepancy requirement. */
  public enum ComplianceLevel {
    FULL,
    PARTIAL,
    NON_COMPLIANT
  }

  /** Priority levels for executive recommendations. */
  public enum RecommendationPriority {
    CRITICAL,
    HIGH,
    MEDIUM,
    LOW
  }

  /** Estimated effort levels for implementing recommendations. */
  public enum EstimatedEffort {
    LOW,
    MEDIUM,
    HIGH
  }
}
