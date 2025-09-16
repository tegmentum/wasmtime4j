package ai.tegmentum.wasmtime4j.comparison.reporters;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;

/**
 * Interactive dashboard generator that creates a web-based interface for exploring comparison
 * results. Uses an embedded Jetty server to serve the dashboard with REST API endpoints for dynamic
 * data loading, filtering, and real-time updates.
 *
 * @since 1.0.0
 */
public final class DashboardGenerator {
  private static final Logger LOGGER = Logger.getLogger(DashboardGenerator.class.getName());
  private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

  private final DashboardConfiguration configuration;
  private final HtmlReporter htmlReporter;
  private final Map<String, ComparisonReport> reportCache;
  private Server jettyServer;
  private boolean isRunning;

  public DashboardGenerator(final DashboardConfiguration configuration) {
    this.configuration = Objects.requireNonNull(configuration, "configuration cannot be null");
    this.htmlReporter = new HtmlReporter(createHtmlReporterConfiguration());
    this.reportCache = new ConcurrentHashMap<>();
    this.isRunning = false;
  }

  /**
   * Starts the dashboard server and serves the comparison report.
   *
   * @param report the comparison report to serve
   * @return URI where the dashboard can be accessed
   * @throws IOException if the server cannot be started
   */
  public URI startDashboard(final ComparisonReport report) throws IOException {
    Objects.requireNonNull(report, "report cannot be null");

    // Cache the report for serving
    reportCache.put(report.getReportId(), report);

    if (!isRunning) {
      startServer();
    }

    final String dashboardUrl =
        String.format(
            "http://localhost:%d/dashboard/%s", configuration.getPort(), report.getReportId());
    LOGGER.info("Dashboard started: " + dashboardUrl);

    return URI.create(dashboardUrl);
  }

  /**
   * Starts the dashboard server asynchronously.
   *
   * @param report the comparison report to serve
   * @return CompletableFuture that completes with the dashboard URI
   */
  public CompletableFuture<URI> startDashboardAsync(final ComparisonReport report) {
    return CompletableFuture.supplyAsync(
        () -> {
          try {
            return startDashboard(report);
          } catch (final IOException e) {
            throw new RuntimeException("Failed to start dashboard", e);
          }
        });
  }

  /**
   * Stops the dashboard server.
   *
   * @throws IOException if the server cannot be stopped cleanly
   */
  public void stopDashboard() throws IOException {
    if (jettyServer != null && isRunning) {
      try {
        jettyServer.stop();
        jettyServer.join();
        isRunning = false;
        LOGGER.info("Dashboard server stopped");
      } catch (final Exception e) {
        throw new IOException("Failed to stop dashboard server", e);
      }
    }
  }

  /**
   * Starts the embedded Jetty server with configured servlets.
   *
   * @throws IOException if the server cannot be started
   */
  private void startServer() throws IOException {
    try {
      jettyServer = new Server(configuration.getPort());

      final ServletContextHandler context =
          new ServletContextHandler(ServletContextHandler.SESSIONS);
      context.setContextPath("/");

      // Dashboard servlet for serving HTML reports
      context.addServlet(new ServletHolder(new DashboardServlet()), "/dashboard/*");

      // REST API servlets
      context.addServlet(new ServletHolder(new ApiServlet()), "/api/*");
      context.addServlet(new ServletHolder(new DataServlet()), "/data/*");
      context.addServlet(new ServletHolder(new FilterServlet()), "/filter/*");

      // Static resource servlet
      context.addServlet(new ServletHolder(new StaticResourceServlet()), "/resources/*");

      jettyServer.setHandler(context);
      jettyServer.start();

      isRunning = true;
      LOGGER.info("Dashboard server started on port " + configuration.getPort());

    } catch (final Exception e) {
      throw new IOException("Failed to start dashboard server", e);
    }
  }

  /**
   * Creates HTML reporter configuration from dashboard configuration.
   *
   * @return HTML reporter configuration
   */
  private HtmlReporterConfiguration createHtmlReporterConfiguration() {
    return HtmlReporterConfiguration.builder()
        .reportTitle(configuration.getTitle())
        .theme(configuration.getTheme())
        .enableInteractiveFeatures(true)
        .enablePerformanceCharts(true)
        .enableCoverageAnalysis(true)
        .includeStaticResources(false) // Served by StaticResourceServlet
        .verbosityLevel(VerbosityLevel.NORMAL)
        .build();
  }

  /**
   * Gets the current dashboard configuration.
   *
   * @return dashboard configuration
   */
  public DashboardConfiguration getConfiguration() {
    return configuration;
  }

  /**
   * Checks if the dashboard server is currently running.
   *
   * @return true if the server is running
   */
  public boolean isRunning() {
    return isRunning;
  }

  /** Servlet for serving the main dashboard HTML. */
  private final class DashboardServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

      final String pathInfo = request.getPathInfo();
      if (pathInfo == null || pathInfo.length() <= 1) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Report ID required");
        return;
      }

      final String reportId = pathInfo.substring(1); // Remove leading slash
      final ComparisonReport report = reportCache.get(reportId);

      if (report == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Report not found: " + reportId);
        return;
      }

      response.setContentType("text/html; charset=UTF-8");
      response.setStatus(HttpServletResponse.SC_OK);

      try {
        htmlReporter.generateReport(report, response.getOutputStream());
      } catch (final IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to generate dashboard HTML", e);
        response.sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to generate dashboard");
      }
    }
  }

  /** REST API servlet for dashboard data operations. */
  private final class ApiServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

      final String pathInfo = request.getPathInfo();
      if (pathInfo == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "API endpoint required");
        return;
      }

      response.setContentType("application/json; charset=UTF-8");
      response.setStatus(HttpServletResponse.SC_OK);

      try {
        final String jsonResponse = handleApiRequest(pathInfo, request);
        response.getWriter().write(jsonResponse);
      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "API request failed: " + pathInfo, e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "API request failed");
      }
    }

    private String handleApiRequest(final String endpoint, final HttpServletRequest request)
        throws IOException {
      final String reportId = request.getParameter("reportId");
      if (reportId == null) {
        throw new IllegalArgumentException("reportId parameter required");
      }

      final ComparisonReport report = reportCache.get(reportId);
      if (report == null) {
        throw new IllegalArgumentException("Report not found: " + reportId);
      }

      return switch (endpoint) {
        case "/summary" -> OBJECT_MAPPER.writeValueAsString(createSummaryData(report));
        case "/performance" -> OBJECT_MAPPER.writeValueAsString(createPerformanceData(report));
        case "/coverage" -> OBJECT_MAPPER.writeValueAsString(createCoverageData(report));
        case "/discrepancies" -> OBJECT_MAPPER.writeValueAsString(createDiscrepanciesData(report));
        case "/recommendations" -> OBJECT_MAPPER.writeValueAsString(
            createRecommendationsData(report));
        default -> throw new IllegalArgumentException("Unknown API endpoint: " + endpoint);
      };
    }

    private Map<String, Object> createSummaryData(final ComparisonReport report) {
      final Map<String, Object> summary = new HashMap<>();
      summary.put("reportId", report.getReportId());
      summary.put("generatedAt", report.getGeneratedAt().toString());
      summary.put("totalTests", report.getTestResults().size());
      summary.put("criticalIssues", report.getCriticalTestResults().size());
      summary.put("executiveSummary", report.getExecutiveSummary());
      return summary;
    }

    private Map<String, Object> createPerformanceData(final ComparisonReport report) {
      final VisualizationBuilder visualizationBuilder = new VisualizationBuilder();
      return visualizationBuilder.createPerformanceChartData(report);
    }

    private Map<String, Object> createCoverageData(final ComparisonReport report) {
      final VisualizationBuilder visualizationBuilder = new VisualizationBuilder();
      return visualizationBuilder.createCoverageChartData(report);
    }

    private Map<String, Object> createDiscrepanciesData(final ComparisonReport report) {
      final VisualizationBuilder visualizationBuilder = new VisualizationBuilder();
      return visualizationBuilder.createDiscrepancyVisualization(report);
    }

    private Map<String, Object> createRecommendationsData(final ComparisonReport report) {
      final Map<String, Object> data = new HashMap<>();
      data.put("recommendations", report.getRecommendations());
      data.put("totalRecommendations", report.getRecommendations().size());
      data.put(
          "highPriorityCount",
          report.getRecommendations().stream()
              .mapToLong(r -> r.getHighPriorityRecommendations().size())
              .sum());
      return data;
    }
  }

  /** Servlet for serving filtered and paginated data. */
  private final class DataServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

      final String reportId = request.getParameter("reportId");
      if (reportId == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "reportId parameter required");
        return;
      }

      final ComparisonReport report = reportCache.get(reportId);
      if (report == null) {
        response.sendError(HttpServletResponse.SC_NOT_FOUND, "Report not found: " + reportId);
        return;
      }

      // Parse pagination parameters
      final int page = parseIntParameter(request, "page", 0);
      final int pageSize = parseIntParameter(request, "pageSize", 50);
      final String filter = request.getParameter("filter");
      final String sortBy = request.getParameter("sortBy");

      response.setContentType("application/json; charset=UTF-8");
      response.setStatus(HttpServletResponse.SC_OK);

      try {
        final Map<String, Object> data =
            createPaginatedData(report, page, pageSize, filter, sortBy);
        final String jsonResponse = OBJECT_MAPPER.writeValueAsString(data);
        response.getWriter().write(jsonResponse);
      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Failed to create paginated data", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to create data");
      }
    }

    private int parseIntParameter(
        final HttpServletRequest request, final String paramName, final int defaultValue) {
      final String paramValue = request.getParameter(paramName);
      if (paramValue == null) {
        return defaultValue;
      }
      try {
        return Integer.parseInt(paramValue);
      } catch (final NumberFormatException e) {
        return defaultValue;
      }
    }

    private Map<String, Object> createPaginatedData(
        final ComparisonReport report,
        final int page,
        final int pageSize,
        final String filter,
        final String sortBy) {

      List<TestComparisonResult> testResults = report.getTestResults();

      // Apply filtering
      if (filter != null && !filter.trim().isEmpty()) {
        testResults =
            testResults.stream()
                .filter(
                    test ->
                        test.getTestName().toLowerCase().contains(filter.toLowerCase())
                            || test.getOverallStatus()
                                .toString()
                                .toLowerCase()
                                .contains(filter.toLowerCase()))
                .toList();
      }

      // Apply sorting
      if (sortBy != null) {
        testResults =
            testResults.stream()
                .sorted(
                    (a, b) ->
                        switch (sortBy) {
                          case "name" -> a.getTestName().compareTo(b.getTestName());
                          case "status" -> a.getOverallStatus().compareTo(b.getOverallStatus());
                          case "critical" -> Boolean.compare(
                              b.hasCriticalIssues(), a.hasCriticalIssues());
                          default -> 0;
                        })
                .toList();
      }

      // Apply pagination
      final int startIndex = page * pageSize;
      final int endIndex = Math.min(startIndex + pageSize, testResults.size());
      final List<TestComparisonResult> paginatedResults = testResults.subList(startIndex, endIndex);

      final Map<String, Object> data = new HashMap<>();
      data.put("results", paginatedResults);
      data.put("totalCount", testResults.size());
      data.put("page", page);
      data.put("pageSize", pageSize);
      data.put("totalPages", (testResults.size() + pageSize - 1) / pageSize);

      return data;
    }
  }

  /** Servlet for handling filter operations. */
  private final class FilterServlet extends HttpServlet {
    @Override
    protected void doPost(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

      response.setContentType("application/json; charset=UTF-8");

      try {
        // Parse filter criteria from request body
        final Map<String, Object> filterCriteria =
            OBJECT_MAPPER.readValue(request.getInputStream(), Map.class);

        final String reportId = (String) filterCriteria.get("reportId");
        if (reportId == null) {
          response.sendError(HttpServletResponse.SC_BAD_REQUEST, "reportId required");
          return;
        }

        final ComparisonReport report = reportCache.get(reportId);
        if (report == null) {
          response.sendError(HttpServletResponse.SC_NOT_FOUND, "Report not found: " + reportId);
          return;
        }

        final Map<String, Object> filteredData = applyFilters(report, filterCriteria);
        final String jsonResponse = OBJECT_MAPPER.writeValueAsString(filteredData);
        response.getWriter().write(jsonResponse);

      } catch (final Exception e) {
        LOGGER.log(Level.SEVERE, "Filter operation failed", e);
        response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Filter operation failed");
      }
    }

    private Map<String, Object> applyFilters(
        final ComparisonReport report, final Map<String, Object> criteria) {
      final Map<String, Object> result = new HashMap<>();

      // Apply various filter criteria
      List<TestComparisonResult> filteredTests = report.getTestResults();

      // Filter by status
      if (criteria.containsKey("status")) {
        final String status = (String) criteria.get("status");
        if (!"ALL".equals(status)) {
          filteredTests =
              filteredTests.stream()
                  .filter(test -> test.getOverallStatus().toString().equals(status))
                  .toList();
        }
      }

      // Filter by runtime
      if (criteria.containsKey("runtime")) {
        final String runtime = (String) criteria.get("runtime");
        if (!"ALL".equals(runtime)) {
          filteredTests =
              filteredTests.stream()
                  .filter(
                      test ->
                          test.getRuntimeResults().keySet().stream()
                              .anyMatch(rt -> rt.toString().equals(runtime)))
                  .toList();
        }
      }

      // Filter by critical issues
      if (criteria.containsKey("onlyCritical")
          && Boolean.TRUE.equals(criteria.get("onlyCritical"))) {
        filteredTests =
            filteredTests.stream().filter(TestComparisonResult::hasCriticalIssues).toList();
      }

      result.put("filteredTests", filteredTests);
      result.put("totalFiltered", filteredTests.size());
      result.put("appliedFilters", criteria);

      return result;
    }
  }

  /** Servlet for serving static resources (CSS, JS, images). */
  private final class StaticResourceServlet extends HttpServlet {
    @Override
    protected void doGet(final HttpServletRequest request, final HttpServletResponse response)
        throws ServletException, IOException {

      final String pathInfo = request.getPathInfo();
      if (pathInfo == null) {
        response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Resource path required");
        return;
      }

      final String resourcePath = "static" + pathInfo;

      try (final var inputStream = getClass().getClassLoader().getResourceAsStream(resourcePath)) {
        if (inputStream == null) {
          response.sendError(
              HttpServletResponse.SC_NOT_FOUND, "Resource not found: " + resourcePath);
          return;
        }

        // Set content type based on file extension
        final String contentType = getContentType(pathInfo);
        response.setContentType(contentType);
        response.setStatus(HttpServletResponse.SC_OK);

        // Copy resource to response
        inputStream.transferTo(response.getOutputStream());

      } catch (final IOException e) {
        LOGGER.log(Level.SEVERE, "Failed to serve static resource: " + resourcePath, e);
        response.sendError(
            HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Failed to serve resource");
      }
    }

    private String getContentType(final String path) {
      if (path.endsWith(".css")) {
        return "text/css";
      } else if (path.endsWith(".js")) {
        return "application/javascript";
      } else if (path.endsWith(".png")) {
        return "image/png";
      } else if (path.endsWith(".jpg") || path.endsWith(".jpeg")) {
        return "image/jpeg";
      } else if (path.endsWith(".svg")) {
        return "image/svg+xml";
      } else {
        return "application/octet-stream";
      }
    }
  }
}

/** Configuration for the dashboard generator. */
final class DashboardConfiguration {
  private final int port;
  private final String title;
  private final String theme;
  private final boolean enableRealTimeUpdates;
  private final int maxCachedReports;

  private DashboardConfiguration(final Builder builder) {
    this.port = builder.port;
    this.title = Objects.requireNonNull(builder.title, "title cannot be null");
    this.theme = Objects.requireNonNull(builder.theme, "theme cannot be null");
    this.enableRealTimeUpdates = builder.enableRealTimeUpdates;
    this.maxCachedReports = builder.maxCachedReports;
  }

  public int getPort() {
    return port;
  }

  public String getTitle() {
    return title;
  }

  public String getTheme() {
    return theme;
  }

  public boolean isRealTimeUpdatesEnabled() {
    return enableRealTimeUpdates;
  }

  public int getMaxCachedReports() {
    return maxCachedReports;
  }

  /** Creates a new builder with default configuration. */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for DashboardConfiguration. */
  public static final class Builder {
    private int port = 8080;
    private String title = "Wasmtime4j Comparison Dashboard";
    private String theme = "default";
    private boolean enableRealTimeUpdates = false;
    private int maxCachedReports = 10;

    public Builder port(final int port) {
      if (port <= 0 || port > 65535) {
        throw new IllegalArgumentException("Port must be between 1 and 65535");
      }
      this.port = port;
      return this;
    }

    public Builder title(final String title) {
      this.title = Objects.requireNonNull(title, "title cannot be null");
      return this;
    }

    public Builder theme(final String theme) {
      this.theme = Objects.requireNonNull(theme, "theme cannot be null");
      return this;
    }

    public Builder enableRealTimeUpdates(final boolean enableRealTimeUpdates) {
      this.enableRealTimeUpdates = enableRealTimeUpdates;
      return this;
    }

    public Builder maxCachedReports(final int maxCachedReports) {
      if (maxCachedReports <= 0) {
        throw new IllegalArgumentException("maxCachedReports must be positive");
      }
      this.maxCachedReports = maxCachedReports;
      return this;
    }

    public DashboardConfiguration build() {
      return new DashboardConfiguration(this);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final DashboardConfiguration that = (DashboardConfiguration) obj;
    return port == that.port
        && enableRealTimeUpdates == that.enableRealTimeUpdates
        && maxCachedReports == that.maxCachedReports
        && Objects.equals(title, that.title)
        && Objects.equals(theme, that.theme);
  }

  @Override
  public int hashCode() {
    return Objects.hash(port, title, theme, enableRealTimeUpdates, maxCachedReports);
  }

  @Override
  public String toString() {
    return "DashboardConfiguration{"
        + "port="
        + port
        + ", title='"
        + title
        + '\''
        + ", theme='"
        + theme
        + '\''
        + ", realTimeUpdates="
        + enableRealTimeUpdates
        + '}';
  }
}
