package ai.tegmentum.wasmtime4j.benchmarks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.DoubleSummaryStatistics;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Comprehensive performance reporting and visualization dashboard framework for Wasmtime4j.
 *
 * <p>This framework provides rich performance data visualization, trend analysis, and
 * comprehensive reporting capabilities. It generates data exports for visualization
 * tools, creates HTML dashboards, and provides comprehensive performance insights.
 *
 * <p>Key features:
 * - Comprehensive HTML dashboard generation
 * - Performance trend visualization data
 * - Cross-platform comparison charts
 * - Runtime performance comparison
 * - Historical performance tracking
 * - Statistical analysis visualization
 * - Regression detection reporting
 * - Export formats for external tools (JSON, CSV)
 * - Interactive performance metrics
 * - Automated report generation
 */
public final class PerformanceReportingDashboard {

    private static final Logger LOGGER = Logger.getLogger(PerformanceReportingDashboard.class.getName());
    private static final ObjectMapper JSON_MAPPER = createJsonMapper();

    /**
     * Dashboard configuration.
     */
    public static final class DashboardConfig {
        private final String title;
        private final String subtitle;
        private final int maxDataPoints;
        private final boolean includeRegressionAnalysis;
        private final boolean includeCrossPlatformComparison;
        private final boolean includeStatisticalAnalysis;
        private final boolean generateInteractiveCharts;
        private final List<String> includedMetrics;

        private DashboardConfig(final Builder builder) {
            this.title = builder.title;
            this.subtitle = builder.subtitle;
            this.maxDataPoints = builder.maxDataPoints;
            this.includeRegressionAnalysis = builder.includeRegressionAnalysis;
            this.includeCrossPlatformComparison = builder.includeCrossPlatformComparison;
            this.includeStatisticalAnalysis = builder.includeStatisticalAnalysis;
            this.generateInteractiveCharts = builder.generateInteractiveCharts;
            this.includedMetrics = new ArrayList<>(builder.includedMetrics);
        }

        // Getters
        public String getTitle() { return title; }
        public String getSubtitle() { return subtitle; }
        public int getMaxDataPoints() { return maxDataPoints; }
        public boolean isIncludeRegressionAnalysis() { return includeRegressionAnalysis; }
        public boolean isIncludeCrossPlatformComparison() { return includeCrossPlatformComparison; }
        public boolean isIncludeStatisticalAnalysis() { return includeStatisticalAnalysis; }
        public boolean isGenerateInteractiveCharts() { return generateInteractiveCharts; }
        public List<String> getIncludedMetrics() { return new ArrayList<>(includedMetrics); }

        public static Builder builder() {
            return new Builder();
        }

        public static final class Builder {
            private String title = "Wasmtime4j Performance Dashboard";
            private String subtitle = "Comprehensive Performance Analysis and Monitoring";
            private int maxDataPoints = 100;
            private boolean includeRegressionAnalysis = true;
            private boolean includeCrossPlatformComparison = true;
            private boolean includeStatisticalAnalysis = true;
            private boolean generateInteractiveCharts = true;
            private List<String> includedMetrics = new ArrayList<>();

            public Builder title(final String title) {
                this.title = title;
                return this;
            }

            public Builder subtitle(final String subtitle) {
                this.subtitle = subtitle;
                return this;
            }

            public Builder maxDataPoints(final int maxDataPoints) {
                if (maxDataPoints <= 0) {
                    throw new IllegalArgumentException("Max data points must be positive");
                }
                this.maxDataPoints = maxDataPoints;
                return this;
            }

            public Builder includeRegressionAnalysis(final boolean include) {
                this.includeRegressionAnalysis = include;
                return this;
            }

            public Builder includeCrossPlatformComparison(final boolean include) {
                this.includeCrossPlatformComparison = include;
                return this;
            }

            public Builder includeStatisticalAnalysis(final boolean include) {
                this.includeStatisticalAnalysis = include;
                return this;
            }

            public Builder generateInteractiveCharts(final boolean generate) {
                this.generateInteractiveCharts = generate;
                return this;
            }

            public Builder includeMetric(final String metric) {
                if (metric != null && !metric.trim().isEmpty()) {
                    this.includedMetrics.add(metric);
                }
                return this;
            }

            public DashboardConfig build() {
                return new DashboardConfig(this);
            }
        }
    }

    /**
     * Performance dashboard data model.
     */
    public static final class DashboardData {
        private final String title;
        private final LocalDateTime generatedAt;
        private final List<BenchmarkSummary> benchmarkSummaries;
        private final Map<String, List<PerformanceDataPoint>> historicalData;
        private final List<RegressionAnalysisEntry> regressionAnalysis;
        private final Map<String, PlatformComparisonData> platformComparisons;
        private final StatisticalSummary statisticalSummary;

        public DashboardData(final String title, final List<BenchmarkSummary> benchmarkSummaries,
                           final Map<String, List<PerformanceDataPoint>> historicalData,
                           final List<RegressionAnalysisEntry> regressionAnalysis,
                           final Map<String, PlatformComparisonData> platformComparisons,
                           final StatisticalSummary statisticalSummary) {
            this.title = title;
            this.generatedAt = LocalDateTime.now();
            this.benchmarkSummaries = new ArrayList<>(benchmarkSummaries);
            this.historicalData = new HashMap<>(historicalData);
            this.regressionAnalysis = new ArrayList<>(regressionAnalysis);
            this.platformComparisons = new HashMap<>(platformComparisons);
            this.statisticalSummary = statisticalSummary;
        }

        // Getters
        public String getTitle() { return title; }
        public LocalDateTime getGeneratedAt() { return generatedAt; }
        public List<BenchmarkSummary> getBenchmarkSummaries() { return new ArrayList<>(benchmarkSummaries); }
        public Map<String, List<PerformanceDataPoint>> getHistoricalData() { return new HashMap<>(historicalData); }
        public List<RegressionAnalysisEntry> getRegressionAnalysis() { return new ArrayList<>(regressionAnalysis); }
        public Map<String, PlatformComparisonData> getPlatformComparisons() { return new HashMap<>(platformComparisons); }
        public StatisticalSummary getStatisticalSummary() { return statisticalSummary; }
    }

    /**
     * Benchmark summary for dashboard display.
     */
    public static final class BenchmarkSummary {
        private final String name;
        private final String runtimeType;
        private final double currentScore;
        private final double averageScore;
        private final double bestScore;
        private final double worstScore;
        private final String trend;
        private final LocalDateTime lastUpdated;

        public BenchmarkSummary(final String name, final String runtimeType, final double currentScore,
                              final double averageScore, final double bestScore, final double worstScore,
                              final String trend, final LocalDateTime lastUpdated) {
            this.name = name;
            this.runtimeType = runtimeType;
            this.currentScore = currentScore;
            this.averageScore = averageScore;
            this.bestScore = bestScore;
            this.worstScore = worstScore;
            this.trend = trend;
            this.lastUpdated = lastUpdated;
        }

        // Getters
        public String getName() { return name; }
        public String getRuntimeType() { return runtimeType; }
        public double getCurrentScore() { return currentScore; }
        public double getAverageScore() { return averageScore; }
        public double getBestScore() { return bestScore; }
        public double getWorstScore() { return worstScore; }
        public String getTrend() { return trend; }
        public LocalDateTime getLastUpdated() { return lastUpdated; }
    }

    /**
     * Performance data point for visualization.
     */
    public static final class PerformanceDataPoint {
        private final LocalDateTime timestamp;
        private final double score;
        private final String label;
        private final Map<String, Object> metadata;

        public PerformanceDataPoint(final LocalDateTime timestamp, final double score, final String label,
                                  final Map<String, Object> metadata) {
            this.timestamp = timestamp;
            this.score = score;
            this.label = label;
            this.metadata = metadata != null ? new HashMap<>(metadata) : new HashMap<>();
        }

        // Getters
        public LocalDateTime getTimestamp() { return timestamp; }
        public double getScore() { return score; }
        public String getLabel() { return label; }
        public Map<String, Object> getMetadata() { return new HashMap<>(metadata); }
    }

    /**
     * Regression analysis entry for dashboard.
     */
    public static final class RegressionAnalysisEntry {
        private final String benchmarkName;
        private final String runtimeType;
        private final double regressionPercentage;
        private final String severity;
        private final String description;
        private final LocalDateTime detectedAt;

        public RegressionAnalysisEntry(final String benchmarkName, final String runtimeType,
                                     final double regressionPercentage, final String severity,
                                     final String description, final LocalDateTime detectedAt) {
            this.benchmarkName = benchmarkName;
            this.runtimeType = runtimeType;
            this.regressionPercentage = regressionPercentage;
            this.severity = severity;
            this.description = description;
            this.detectedAt = detectedAt;
        }

        // Getters
        public String getBenchmarkName() { return benchmarkName; }
        public String getRuntimeType() { return runtimeType; }
        public double getRegressionPercentage() { return regressionPercentage; }
        public String getSeverity() { return severity; }
        public String getDescription() { return description; }
        public LocalDateTime getDetectedAt() { return detectedAt; }
    }

    /**
     * Platform comparison data.
     */
    public static final class PlatformComparisonData {
        private final String platformName;
        private final Map<String, Double> benchmarkScores;
        private final double averageScore;
        private final String relativePerformance;

        public PlatformComparisonData(final String platformName, final Map<String, Double> benchmarkScores) {
            this.platformName = platformName;
            this.benchmarkScores = new HashMap<>(benchmarkScores);
            this.averageScore = benchmarkScores.values().stream().mapToDouble(Double::doubleValue).average().orElse(0.0);
            this.relativePerformance = "N/A"; // Calculated relative to other platforms
        }

        // Getters
        public String getPlatformName() { return platformName; }
        public Map<String, Double> getBenchmarkScores() { return new HashMap<>(benchmarkScores); }
        public double getAverageScore() { return averageScore; }
        public String getRelativePerformance() { return relativePerformance; }
    }

    /**
     * Statistical summary for dashboard.
     */
    public static final class StatisticalSummary {
        private final int totalBenchmarks;
        private final int totalDataPoints;
        private final double overallAverageScore;
        private final double performanceVariability;
        private final String mostStableBenchmark;
        private final String mostVariableBenchmark;
        private final LocalDateTime analysisDate;

        public StatisticalSummary(final int totalBenchmarks, final int totalDataPoints,
                                final double overallAverageScore, final double performanceVariability,
                                final String mostStableBenchmark, final String mostVariableBenchmark) {
            this.totalBenchmarks = totalBenchmarks;
            this.totalDataPoints = totalDataPoints;
            this.overallAverageScore = overallAverageScore;
            this.performanceVariability = performanceVariability;
            this.mostStableBenchmark = mostStableBenchmark;
            this.mostVariableBenchmark = mostVariableBenchmark;
            this.analysisDate = LocalDateTime.now();
        }

        // Getters
        public int getTotalBenchmarks() { return totalBenchmarks; }
        public int getTotalDataPoints() { return totalDataPoints; }
        public double getOverallAverageScore() { return overallAverageScore; }
        public double getPerformanceVariability() { return performanceVariability; }
        public String getMostStableBenchmark() { return mostStableBenchmark; }
        public String getMostVariableBenchmark() { return mostVariableBenchmark; }
        public LocalDateTime getAnalysisDate() { return analysisDate; }
    }

    /**
     * Dashboard generator for creating comprehensive reports.
     */
    public static final class DashboardGenerator {
        private final DashboardConfig config;

        public DashboardGenerator(final DashboardConfig config) {
            this.config = config;
        }

        /**
         * Generates comprehensive performance dashboard.
         */
        public void generateDashboard(final DashboardData data, final Path outputDir) throws IOException {
            Files.createDirectories(outputDir);

            // Generate HTML dashboard
            generateHtmlDashboard(data, outputDir);

            // Generate JSON data export
            generateJsonExport(data, outputDir);

            // Generate CSV exports
            generateCsvExports(data, outputDir);

            // Generate visualization data
            generateVisualizationData(data, outputDir);

            LOGGER.info("Performance dashboard generated at: " + outputDir);
        }

        private void generateHtmlDashboard(final DashboardData data, final Path outputDir) throws IOException {
            final StringBuilder html = new StringBuilder();

            html.append("<!DOCTYPE html>\n");
            html.append("<html lang=\"en\">\n");
            html.append("<head>\n");
            html.append("    <meta charset=\"UTF-8\">\n");
            html.append("    <meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n");
            html.append("    <title>").append(data.getTitle()).append("</title>\n");
            html.append("    <style>\n");
            html.append(getDashboardCss());
            html.append("    </style>\n");
            if (config.isGenerateInteractiveCharts()) {
                html.append("    <script src=\"https://cdn.jsdelivr.net/npm/chart.js\"></script>\n");
            }
            html.append("</head>\n");
            html.append("<body>\n");

            // Header
            html.append("    <header class=\"dashboard-header\">\n");
            html.append("        <h1>").append(data.getTitle()).append("</h1>\n");
            html.append("        <p class=\"subtitle\">").append(config.getSubtitle()).append("</p>\n");
            html.append("        <p class=\"generated-at\">Generated: ")
                    .append(data.getGeneratedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME))
                    .append("</p>\n");
            html.append("    </header>\n");

            // Summary cards
            html.append("    <section class=\"summary-section\">\n");
            html.append("        <h2>Performance Summary</h2>\n");
            html.append("        <div class=\"summary-cards\">\n");
            generateSummaryCards(html, data);
            html.append("        </div>\n");
            html.append("    </section>\n");

            // Benchmark results
            html.append("    <section class=\"benchmarks-section\">\n");
            html.append("        <h2>Benchmark Results</h2>\n");
            generateBenchmarkTable(html, data);
            html.append("    </section>\n");

            // Charts section
            if (config.isGenerateInteractiveCharts()) {
                html.append("    <section class=\"charts-section\">\n");
                html.append("        <h2>Performance Trends</h2>\n");
                generateChartsSection(html, data);
                html.append("    </section>\n");
            }

            // Regression analysis
            if (config.isIncludeRegressionAnalysis() && !data.getRegressionAnalysis().isEmpty()) {
                html.append("    <section class=\"regression-section\">\n");
                html.append("        <h2>Regression Analysis</h2>\n");
                generateRegressionTable(html, data);
                html.append("    </section>\n");
            }

            html.append("    <footer class=\"dashboard-footer\">\n");
            html.append("        <p>Wasmtime4j Performance Dashboard - Automated Performance Monitoring</p>\n");
            html.append("    </footer>\n");

            if (config.isGenerateInteractiveCharts()) {
                html.append("    <script>\n");
                html.append(getChartJavaScript(data));
                html.append("    </script>\n");
            }

            html.append("</body>\n");
            html.append("</html>\n");

            Files.write(outputDir.resolve("dashboard.html"), html.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        private void generateSummaryCards(final StringBuilder html, final DashboardData data) {
            final StatisticalSummary stats = data.getStatisticalSummary();

            html.append("            <div class=\"summary-card\">\n");
            html.append("                <h3>Total Benchmarks</h3>\n");
            html.append("                <div class=\"metric-value\">").append(stats.getTotalBenchmarks()).append("</div>\n");
            html.append("            </div>\n");

            html.append("            <div class=\"summary-card\">\n");
            html.append("                <h3>Data Points</h3>\n");
            html.append("                <div class=\"metric-value\">").append(stats.getTotalDataPoints()).append("</div>\n");
            html.append("            </div>\n");

            html.append("            <div class=\"summary-card\">\n");
            html.append("                <h3>Average Score</h3>\n");
            html.append("                <div class=\"metric-value\">").append(String.format("%.2f", stats.getOverallAverageScore())).append("</div>\n");
            html.append("            </div>\n");

            html.append("            <div class=\"summary-card\">\n");
            html.append("                <h3>Performance Variability</h3>\n");
            html.append("                <div class=\"metric-value\">").append(String.format("%.1f%%", stats.getPerformanceVariability() * 100)).append("</div>\n");
            html.append("            </div>\n");
        }

        private void generateBenchmarkTable(final StringBuilder html, final DashboardData data) {
            html.append("        <div class=\"table-container\">\n");
            html.append("            <table class=\"benchmark-table\">\n");
            html.append("                <thead>\n");
            html.append("                    <tr>\n");
            html.append("                        <th>Benchmark</th>\n");
            html.append("                        <th>Runtime</th>\n");
            html.append("                        <th>Current Score</th>\n");
            html.append("                        <th>Average</th>\n");
            html.append("                        <th>Best</th>\n");
            html.append("                        <th>Worst</th>\n");
            html.append("                        <th>Trend</th>\n");
            html.append("                        <th>Last Updated</th>\n");
            html.append("                    </tr>\n");
            html.append("                </thead>\n");
            html.append("                <tbody>\n");

            for (final BenchmarkSummary summary : data.getBenchmarkSummaries()) {
                html.append("                    <tr>\n");
                html.append("                        <td>").append(summary.getName()).append("</td>\n");
                html.append("                        <td><span class=\"runtime-badge\">").append(summary.getRuntimeType()).append("</span></td>\n");
                html.append("                        <td>").append(String.format("%.2f", summary.getCurrentScore())).append("</td>\n");
                html.append("                        <td>").append(String.format("%.2f", summary.getAverageScore())).append("</td>\n");
                html.append("                        <td>").append(String.format("%.2f", summary.getBestScore())).append("</td>\n");
                html.append("                        <td>").append(String.format("%.2f", summary.getWorstScore())).append("</td>\n");
                html.append("                        <td><span class=\"trend-indicator\">").append(summary.getTrend()).append("</span></td>\n");
                html.append("                        <td>").append(summary.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</td>\n");
                html.append("                    </tr>\n");
            }

            html.append("                </tbody>\n");
            html.append("            </table>\n");
            html.append("        </div>\n");
        }

        private void generateChartsSection(final StringBuilder html, final DashboardData data) {
            html.append("        <div class=\"charts-grid\">\n");

            int chartId = 0;
            for (final Map.Entry<String, List<PerformanceDataPoint>> entry : data.getHistoricalData().entrySet()) {
                html.append("            <div class=\"chart-container\">\n");
                html.append("                <h3>").append(entry.getKey()).append("</h3>\n");
                html.append("                <canvas id=\"chart").append(chartId).append("\"></canvas>\n");
                html.append("            </div>\n");
                chartId++;
            }

            html.append("        </div>\n");
        }

        private void generateRegressionTable(final StringBuilder html, final DashboardData data) {
            html.append("        <div class=\"table-container\">\n");
            html.append("            <table class=\"regression-table\">\n");
            html.append("                <thead>\n");
            html.append("                    <tr>\n");
            html.append("                        <th>Benchmark</th>\n");
            html.append("                        <th>Runtime</th>\n");
            html.append("                        <th>Regression %</th>\n");
            html.append("                        <th>Severity</th>\n");
            html.append("                        <th>Description</th>\n");
            html.append("                        <th>Detected</th>\n");
            html.append("                    </tr>\n");
            html.append("                </thead>\n");
            html.append("                <tbody>\n");

            for (final RegressionAnalysisEntry regression : data.getRegressionAnalysis()) {
                html.append("                    <tr>\n");
                html.append("                        <td>").append(regression.getBenchmarkName()).append("</td>\n");
                html.append("                        <td>").append(regression.getRuntimeType()).append("</td>\n");
                html.append("                        <td>").append(String.format("%.1f%%", regression.getRegressionPercentage() * 100)).append("</td>\n");
                html.append("                        <td><span class=\"severity-").append(regression.getSeverity().toLowerCase()).append("\">")
                        .append(regression.getSeverity()).append("</span></td>\n");
                html.append("                        <td>").append(regression.getDescription()).append("</td>\n");
                html.append("                        <td>").append(regression.getDetectedAt().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("</td>\n");
                html.append("                    </tr>\n");
            }

            html.append("                </tbody>\n");
            html.append("            </table>\n");
            html.append("        </div>\n");
        }

        private String getDashboardCss() {
            return "        body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; margin: 0; padding: 0; background-color: #f5f7fa; }\n" +
                   "        .dashboard-header { background: linear-gradient(135deg, #667eea 0%, #764ba2 100%); color: white; padding: 2rem; text-align: center; }\n" +
                   "        .dashboard-header h1 { margin: 0; font-size: 2.5rem; font-weight: 700; }\n" +
                   "        .subtitle { font-size: 1.2rem; opacity: 0.9; margin: 0.5rem 0; }\n" +
                   "        .generated-at { font-size: 0.9rem; opacity: 0.8; }\n" +
                   "        .summary-section, .benchmarks-section, .charts-section, .regression-section { padding: 2rem; }\n" +
                   "        .summary-cards { display: grid; grid-template-columns: repeat(auto-fit, minmax(250px, 1fr)); gap: 1.5rem; margin-top: 1rem; }\n" +
                   "        .summary-card { background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); text-align: center; }\n" +
                   "        .summary-card h3 { margin: 0 0 1rem; color: #4a5568; font-size: 0.9rem; text-transform: uppercase; }\n" +
                   "        .metric-value { font-size: 2rem; font-weight: 700; color: #2d3748; }\n" +
                   "        .table-container { background: white; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); overflow: hidden; }\n" +
                   "        table { width: 100%; border-collapse: collapse; }\n" +
                   "        th, td { padding: 1rem; text-align: left; border-bottom: 1px solid #e2e8f0; }\n" +
                   "        th { background-color: #f7fafc; font-weight: 600; color: #4a5568; }\n" +
                   "        .runtime-badge { background: #e2e8f0; padding: 0.25rem 0.75rem; border-radius: 4px; font-size: 0.875rem; }\n" +
                   "        .charts-grid { display: grid; grid-template-columns: repeat(auto-fit, minmax(400px, 1fr)); gap: 2rem; }\n" +
                   "        .chart-container { background: white; padding: 1.5rem; border-radius: 8px; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }\n" +
                   "        .dashboard-footer { background: #2d3748; color: white; text-align: center; padding: 1rem; }\n" +
                   "        .severity-high { background: #fed7d7; color: #c53030; padding: 0.25rem 0.5rem; border-radius: 4px; }\n" +
                   "        .severity-medium { background: #feebc8; color: #dd6b20; padding: 0.25rem 0.5rem; border-radius: 4px; }\n" +
                   "        .severity-low { background: #c6f6d5; color: #38a169; padding: 0.25rem 0.5rem; border-radius: 4px; }\n";
        }

        private String getChartJavaScript(final DashboardData data) {
            final StringBuilder js = new StringBuilder();
            js.append("        document.addEventListener('DOMContentLoaded', function() {\n");

            int chartId = 0;
            for (final Map.Entry<String, List<PerformanceDataPoint>> entry : data.getHistoricalData().entrySet()) {
                js.append("            new Chart(document.getElementById('chart").append(chartId).append("'), {\n");
                js.append("                type: 'line',\n");
                js.append("                data: {\n");
                js.append("                    labels: [");

                final List<PerformanceDataPoint> points = entry.getValue();
                for (int i = 0; i < points.size(); i++) {
                    if (i > 0) js.append(", ");
                    js.append("'").append(points.get(i).getTimestamp().format(DateTimeFormatter.ofPattern("MM/dd HH:mm"))).append("'");
                }

                js.append("],\n");
                js.append("                    datasets: [{\n");
                js.append("                        label: 'Performance Score',\n");
                js.append("                        data: [");

                for (int i = 0; i < points.size(); i++) {
                    if (i > 0) js.append(", ");
                    js.append(points.get(i).getScore());
                }

                js.append("],\n");
                js.append("                        borderColor: 'rgb(75, 192, 192)',\n");
                js.append("                        backgroundColor: 'rgba(75, 192, 192, 0.2)',\n");
                js.append("                        tension: 0.1\n");
                js.append("                    }]\n");
                js.append("                },\n");
                js.append("                options: { responsive: true, scales: { y: { beginAtZero: false } } }\n");
                js.append("            });\n");

                chartId++;
            }

            js.append("        });\n");
            return js.toString();
        }

        private void generateJsonExport(final DashboardData data, final Path outputDir) throws IOException {
            final ObjectNode root = JSON_MAPPER.createObjectNode();

            root.put("title", data.getTitle());
            root.put("generatedAt", data.getGeneratedAt().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

            // Export benchmark summaries
            final ArrayNode benchmarksArray = JSON_MAPPER.createArrayNode();
            for (final BenchmarkSummary summary : data.getBenchmarkSummaries()) {
                final ObjectNode benchmarkNode = JSON_MAPPER.createObjectNode();
                benchmarkNode.put("name", summary.getName());
                benchmarkNode.put("runtimeType", summary.getRuntimeType());
                benchmarkNode.put("currentScore", summary.getCurrentScore());
                benchmarkNode.put("averageScore", summary.getAverageScore());
                benchmarkNode.put("bestScore", summary.getBestScore());
                benchmarkNode.put("worstScore", summary.getWorstScore());
                benchmarkNode.put("trend", summary.getTrend());
                benchmarksArray.add(benchmarkNode);
            }
            root.set("benchmarks", benchmarksArray);

            // Export statistical summary
            final ObjectNode statsNode = JSON_MAPPER.createObjectNode();
            final StatisticalSummary stats = data.getStatisticalSummary();
            statsNode.put("totalBenchmarks", stats.getTotalBenchmarks());
            statsNode.put("totalDataPoints", stats.getTotalDataPoints());
            statsNode.put("overallAverageScore", stats.getOverallAverageScore());
            statsNode.put("performanceVariability", stats.getPerformanceVariability());
            root.set("statistics", statsNode);

            Files.write(outputDir.resolve("dashboard-data.json"), JSON_MAPPER.writeValueAsBytes(root),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        private void generateCsvExports(final DashboardData data, final Path outputDir) throws IOException {
            // Export benchmark summaries to CSV
            final StringBuilder csv = new StringBuilder();
            csv.append("Name,Runtime,Current Score,Average Score,Best Score,Worst Score,Trend,Last Updated\n");

            for (final BenchmarkSummary summary : data.getBenchmarkSummaries()) {
                csv.append(summary.getName()).append(",");
                csv.append(summary.getRuntimeType()).append(",");
                csv.append(summary.getCurrentScore()).append(",");
                csv.append(summary.getAverageScore()).append(",");
                csv.append(summary.getBestScore()).append(",");
                csv.append(summary.getWorstScore()).append(",");
                csv.append(summary.getTrend()).append(",");
                csv.append(summary.getLastUpdated().format(DateTimeFormatter.ISO_LOCAL_DATE)).append("\n");
            }

            Files.write(outputDir.resolve("benchmark-summary.csv"), csv.toString().getBytes(),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }

        private void generateVisualizationData(final DashboardData data, final Path outputDir) throws IOException {
            final ObjectNode vizData = JSON_MAPPER.createObjectNode();

            // Time series data for charts
            final ObjectNode timeSeriesData = JSON_MAPPER.createObjectNode();
            for (final Map.Entry<String, List<PerformanceDataPoint>> entry : data.getHistoricalData().entrySet()) {
                final ArrayNode seriesArray = JSON_MAPPER.createArrayNode();

                for (final PerformanceDataPoint point : entry.getValue()) {
                    final ObjectNode pointNode = JSON_MAPPER.createObjectNode();
                    pointNode.put("timestamp", point.getTimestamp().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    pointNode.put("score", point.getScore());
                    pointNode.put("label", point.getLabel());
                    seriesArray.add(pointNode);
                }

                timeSeriesData.set(entry.getKey(), seriesArray);
            }
            vizData.set("timeSeries", timeSeriesData);

            Files.write(outputDir.resolve("visualization-data.json"), JSON_MAPPER.writeValueAsBytes(vizData),
                    StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        }
    }

    /**
     * Creates a configured JSON mapper.
     */
    private static ObjectMapper createJsonMapper() {
        final ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
        return mapper;
    }

    /**
     * Main method for generating performance dashboard.
     */
    public static void main(final String[] args) throws Exception {
        LOGGER.info("Generating comprehensive performance dashboard");

        // Create sample data for demonstration
        final DashboardData sampleData = createSampleDashboardData();

        // Configure dashboard
        final DashboardConfig config = DashboardConfig.builder()
                .title("Wasmtime4j Performance Dashboard")
                .subtitle("Comprehensive Performance Analysis and Monitoring")
                .maxDataPoints(50)
                .includeRegressionAnalysis(true)
                .includeCrossPlatformComparison(true)
                .includeStatisticalAnalysis(true)
                .generateInteractiveCharts(true)
                .build();

        // Generate dashboard
        final DashboardGenerator generator = new DashboardGenerator(config);
        final Path outputDir = Paths.get("dashboard-output");
        generator.generateDashboard(sampleData, outputDir);

        LOGGER.info("Performance dashboard generated successfully");
        System.out.println("Dashboard available at: " + outputDir.resolve("dashboard.html").toAbsolutePath());
    }

    /**
     * Creates sample dashboard data for demonstration.
     */
    private static DashboardData createSampleDashboardData() {
        final List<BenchmarkSummary> benchmarks = new ArrayList<>();
        benchmarks.add(new BenchmarkSummary("RuntimeInitializationBenchmark", "JNI", 1250.5, 1200.0, 1350.0, 1100.0, "↗", LocalDateTime.now()));
        benchmarks.add(new BenchmarkSummary("RuntimeInitializationBenchmark", "PANAMA", 1180.2, 1150.0, 1250.0, 1050.0, "→", LocalDateTime.now()));
        benchmarks.add(new BenchmarkSummary("ModuleOperationBenchmark", "JNI", 890.3, 900.0, 950.0, 850.0, "↘", LocalDateTime.now()));

        final Map<String, List<PerformanceDataPoint>> historicalData = new HashMap<>();
        final List<PerformanceDataPoint> jniData = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            jniData.add(new PerformanceDataPoint(LocalDateTime.now().minusHours(i * 2), 1200 + (Math.random() * 100), "JNI", null));
        }
        historicalData.put("JNI Runtime Performance", jniData);

        final List<RegressionAnalysisEntry> regressions = new ArrayList<>();
        regressions.add(new RegressionAnalysisEntry("ModuleOperationBenchmark", "JNI", -0.05, "MEDIUM", "5% performance regression detected", LocalDateTime.now()));

        final Map<String, PlatformComparisonData> platformComparisons = new HashMap<>();
        final StatisticalSummary stats = new StatisticalSummary(3, 30, 1100.0, 0.15, "RuntimeInitializationBenchmark", "ModuleOperationBenchmark");

        return new DashboardData("Wasmtime4j Performance Dashboard", benchmarks, historicalData, regressions, platformComparisons, stats);
    }
}