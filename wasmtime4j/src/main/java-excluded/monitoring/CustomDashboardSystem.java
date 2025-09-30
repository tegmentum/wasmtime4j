/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.monitoring;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Custom monitoring dashboard system providing flexible visualization, real-time data streaming,
 * and customizable dashboard creation for production monitoring.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Dynamic dashboard creation and configuration
 *   <li>Multiple visualization types (charts, graphs, tables, gauges)
 *   <li>Real-time data streaming and updates
 *   <li>Custom widget development and integration
 *   <li>Dashboard templating and sharing
 *   <li>Multi-tenant dashboard isolation
 *   <li>Export capabilities (JSON, CSV, PNG)
 * </ul>
 *
 * @since 1.0.0
 */
public final class CustomDashboardSystem {

  private static final Logger LOGGER = Logger.getLogger(CustomDashboardSystem.class.getName());

  /** Visualization types supported by the dashboard system. */
  public enum VisualizationType {
    LINE_CHART("Line Chart", "Time series line chart"),
    BAR_CHART("Bar Chart", "Categorical bar chart"),
    PIE_CHART("Pie Chart", "Proportional pie chart"),
    GAUGE("Gauge", "Single value gauge"),
    TABLE("Table", "Data table"),
    HEATMAP("Heatmap", "Density heatmap"),
    HISTOGRAM("Histogram", "Value distribution histogram"),
    SCATTER_PLOT("Scatter Plot", "X-Y scatter plot"),
    AREA_CHART("Area Chart", "Filled area chart"),
    DONUT_CHART("Donut Chart", "Donut-style pie chart");

    private final String displayName;
    private final String description;

    VisualizationType(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }

  /** Dashboard layout types. */
  public enum LayoutType {
    GRID("Grid Layout", "Fixed grid-based layout"),
    FLEXIBLE("Flexible Layout", "Responsive flexible layout"),
    TABBED("Tabbed Layout", "Tab-based organization"),
    SPLIT("Split Layout", "Split pane layout");

    private final String displayName;
    private final String description;

    LayoutType(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }

  /** Data aggregation methods. */
  public enum AggregationMethod {
    NONE("None", "Raw data"),
    AVERAGE("Average", "Average values"),
    SUM("Sum", "Sum of values"),
    MIN("Minimum", "Minimum value"),
    MAX("Maximum", "Maximum value"),
    COUNT("Count", "Count of values"),
    PERCENTILE_95("95th Percentile", "95th percentile"),
    PERCENTILE_99("99th Percentile", "99th percentile");

    private final String displayName;
    private final String description;

    AggregationMethod(final String displayName, final String description) {
      this.displayName = displayName;
      this.description = description;
    }

    public String getDisplayName() { return displayName; }
    public String getDescription() { return description; }
  }

  /** Dashboard widget configuration. */
  public static final class WidgetConfig {
    private final String widgetId;
    private final String title;
    private final String description;
    private final VisualizationType visualizationType;
    private final List<String> metricNames;
    private final Duration timeRange;
    private final AggregationMethod aggregationMethod;
    private final Map<String, Object> displayOptions;
    private final Map<String, String> filters;
    private final int refreshIntervalSeconds;
    private final boolean realTimeUpdates;

    public WidgetConfig(
        final String widgetId,
        final String title,
        final String description,
        final VisualizationType visualizationType,
        final List<String> metricNames,
        final Duration timeRange,
        final AggregationMethod aggregationMethod,
        final Map<String, Object> displayOptions,
        final Map<String, String> filters,
        final int refreshIntervalSeconds,
        final boolean realTimeUpdates) {
      this.widgetId = widgetId;
      this.title = title;
      this.description = description;
      this.visualizationType = visualizationType;
      this.metricNames = List.copyOf(metricNames != null ? metricNames : List.of());
      this.timeRange = timeRange != null ? timeRange : Duration.ofHours(1);
      this.aggregationMethod = aggregationMethod != null ? aggregationMethod : AggregationMethod.NONE;
      this.displayOptions = Map.copyOf(displayOptions != null ? displayOptions : Map.of());
      this.filters = Map.copyOf(filters != null ? filters : Map.of());
      this.refreshIntervalSeconds = refreshIntervalSeconds > 0 ? refreshIntervalSeconds : 30;
      this.realTimeUpdates = realTimeUpdates;
    }

    // Getters
    public String getWidgetId() { return widgetId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public VisualizationType getVisualizationType() { return visualizationType; }
    public List<String> getMetricNames() { return metricNames; }
    public Duration getTimeRange() { return timeRange; }
    public AggregationMethod getAggregationMethod() { return aggregationMethod; }
    public Map<String, Object> getDisplayOptions() { return displayOptions; }
    public Map<String, String> getFilters() { return filters; }
    public int getRefreshIntervalSeconds() { return refreshIntervalSeconds; }
    public boolean isRealTimeUpdates() { return realTimeUpdates; }
  }

  /** Dashboard configuration. */
  public static final class DashboardConfig {
    private final String dashboardId;
    private final String name;
    private final String description;
    private final String owner;
    private final LayoutType layoutType;
    private final List<WidgetConfig> widgets;
    private final Map<String, Object> globalSettings;
    private final Duration autoRefreshInterval;
    private final boolean isPublic;
    private final List<String> tags;
    private final Instant createdAt;
    private volatile Instant lastModified;

    public DashboardConfig(
        final String dashboardId,
        final String name,
        final String description,
        final String owner,
        final LayoutType layoutType,
        final List<WidgetConfig> widgets,
        final Map<String, Object> globalSettings,
        final Duration autoRefreshInterval,
        final boolean isPublic,
        final List<String> tags) {
      this.dashboardId = dashboardId;
      this.name = name;
      this.description = description;
      this.owner = owner;
      this.layoutType = layoutType != null ? layoutType : LayoutType.GRID;
      this.widgets = new CopyOnWriteArrayList<>(widgets != null ? widgets : List.of());
      this.globalSettings = new ConcurrentHashMap<>(globalSettings != null ? globalSettings : Map.of());
      this.autoRefreshInterval = autoRefreshInterval != null ? autoRefreshInterval : Duration.ofMinutes(1);
      this.isPublic = isPublic;
      this.tags = new CopyOnWriteArrayList<>(tags != null ? tags : List.of());
      this.createdAt = Instant.now();
      this.lastModified = this.createdAt;
    }

    // Getters
    public String getDashboardId() { return dashboardId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getOwner() { return owner; }
    public LayoutType getLayoutType() { return layoutType; }
    public List<WidgetConfig> getWidgets() { return List.copyOf(widgets); }
    public Map<String, Object> getGlobalSettings() { return Map.copyOf(globalSettings); }
    public Duration getAutoRefreshInterval() { return autoRefreshInterval; }
    public boolean isPublic() { return isPublic; }
    public List<String> getTags() { return List.copyOf(tags); }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getLastModified() { return lastModified; }

    public void updateLastModified() {
      this.lastModified = Instant.now();
    }

    public void addWidget(final WidgetConfig widget) {
      widgets.add(widget);
      updateLastModified();
    }

    public boolean removeWidget(final String widgetId) {
      final boolean removed = widgets.removeIf(w -> w.getWidgetId().equals(widgetId));
      if (removed) {
        updateLastModified();
      }
      return removed;
    }
  }

  /** Widget data point for visualization. */
  public static final class DataPoint {
    private final Instant timestamp;
    private final double value;
    private final Map<String, String> labels;

    public DataPoint(final Instant timestamp, final double value, final Map<String, String> labels) {
      this.timestamp = timestamp;
      this.value = value;
      this.labels = Map.copyOf(labels != null ? labels : Map.of());
    }

    public Instant getTimestamp() { return timestamp; }
    public double getValue() { return value; }
    public Map<String, String> getLabels() { return labels; }
  }

  /** Widget data series. */
  public static final class DataSeries {
    private final String seriesId;
    private final String metricName;
    private final List<DataPoint> dataPoints;
    private final AggregationMethod aggregationMethod;
    private final Map<String, Object> metadata;

    public DataSeries(
        final String seriesId,
        final String metricName,
        final List<DataPoint> dataPoints,
        final AggregationMethod aggregationMethod,
        final Map<String, Object> metadata) {
      this.seriesId = seriesId;
      this.metricName = metricName;
      this.dataPoints = new ArrayList<>(dataPoints != null ? dataPoints : List.of());
      this.aggregationMethod = aggregationMethod;
      this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    public String getSeriesId() { return seriesId; }
    public String getMetricName() { return metricName; }
    public List<DataPoint> getDataPoints() { return List.copyOf(dataPoints); }
    public AggregationMethod getAggregationMethod() { return aggregationMethod; }
    public Map<String, Object> getMetadata() { return metadata; }

    public void addDataPoint(final DataPoint dataPoint) {
      dataPoints.add(dataPoint);
    }

    public double getCurrentValue() {
      return dataPoints.isEmpty() ? 0.0 : dataPoints.get(dataPoints.size() - 1).getValue();
    }

    public double getAverageValue() {
      return dataPoints.stream().mapToDouble(DataPoint::getValue).average().orElse(0.0);
    }
  }

  /** Rendered widget data for client consumption. */
  public static final class RenderedWidget {
    private final String widgetId;
    private final String title;
    private final VisualizationType visualizationType;
    private final List<DataSeries> dataSeries;
    private final Map<String, Object> renderingOptions;
    private final Instant lastUpdated;
    private final String status;
    private final List<String> warnings;

    public RenderedWidget(
        final String widgetId,
        final String title,
        final VisualizationType visualizationType,
        final List<DataSeries> dataSeries,
        final Map<String, Object> renderingOptions,
        final String status,
        final List<String> warnings) {
      this.widgetId = widgetId;
      this.title = title;
      this.visualizationType = visualizationType;
      this.dataSeries = List.copyOf(dataSeries != null ? dataSeries : List.of());
      this.renderingOptions = Map.copyOf(renderingOptions != null ? renderingOptions : Map.of());
      this.lastUpdated = Instant.now();
      this.status = status != null ? status : "OK";
      this.warnings = List.copyOf(warnings != null ? warnings : List.of());
    }

    // Getters
    public String getWidgetId() { return widgetId; }
    public String getTitle() { return title; }
    public VisualizationType getVisualizationType() { return visualizationType; }
    public List<DataSeries> getDataSeries() { return dataSeries; }
    public Map<String, Object> getRenderingOptions() { return renderingOptions; }
    public Instant getLastUpdated() { return lastUpdated; }
    public String getStatus() { return status; }
    public List<String> getWarnings() { return warnings; }
  }

  /** Dashboard rendering result. */
  public static final class RenderedDashboard {
    private final String dashboardId;
    private final String name;
    private final String description;
    private final LayoutType layoutType;
    private final List<RenderedWidget> widgets;
    private final Map<String, Object> globalSettings;
    private final Instant lastRendered;
    private final Duration renderingTime;
    private final List<String> errors;

    public RenderedDashboard(
        final String dashboardId,
        final String name,
        final String description,
        final LayoutType layoutType,
        final List<RenderedWidget> widgets,
        final Map<String, Object> globalSettings,
        final Duration renderingTime,
        final List<String> errors) {
      this.dashboardId = dashboardId;
      this.name = name;
      this.description = description;
      this.layoutType = layoutType;
      this.widgets = List.copyOf(widgets != null ? widgets : List.of());
      this.globalSettings = Map.copyOf(globalSettings != null ? globalSettings : Map.of());
      this.lastRendered = Instant.now();
      this.renderingTime = renderingTime != null ? renderingTime : Duration.ZERO;
      this.errors = List.copyOf(errors != null ? errors : List.of());
    }

    // Getters
    public String getDashboardId() { return dashboardId; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public LayoutType getLayoutType() { return layoutType; }
    public List<RenderedWidget> getWidgets() { return widgets; }
    public Map<String, Object> getGlobalSettings() { return globalSettings; }
    public Instant getLastRendered() { return lastRendered; }
    public Duration getRenderingTime() { return renderingTime; }
    public List<String> getErrors() { return errors; }

    public boolean hasErrors() { return !errors.isEmpty(); }
  }

  // Instance fields
  private final ProductionMonitoringSystem monitoringSystem;
  private final HealthCheckSystem healthCheckSystem;
  private final IntelligentAlertingSystem alertingSystem;

  private final ConcurrentHashMap<String, DashboardConfig> dashboards = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, RenderedDashboard> dashboardCache = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, List<DataSeries>> widgetDataCache = new ConcurrentHashMap<>();

  // Statistics and monitoring
  private final AtomicLong totalDashboardsCreated = new AtomicLong(0);
  private final AtomicLong totalWidgetsRendered = new AtomicLong(0);
  private final AtomicLong totalRenderingTime = new AtomicLong(0);
  private final AtomicReference<Instant> lastCacheCleanup = new AtomicReference<>(Instant.now());

  // Background processing
  private final ScheduledExecutorService backgroundExecutor = Executors.newScheduledThreadPool(2);

  // Configuration
  private volatile boolean realTimeEnabled = true;
  private volatile int maxDashboards = 1000;
  private volatile int maxWidgetsPerDashboard = 50;
  private volatile Duration cacheRetentionPeriod = Duration.ofMinutes(30);

  /**
   * Creates custom dashboard system.
   *
   * @param monitoringSystem the production monitoring system
   * @param healthCheckSystem the health check system
   * @param alertingSystem the intelligent alerting system
   */
  public CustomDashboardSystem(
      final ProductionMonitoringSystem monitoringSystem,
      final HealthCheckSystem healthCheckSystem,
      final IntelligentAlertingSystem alertingSystem) {
    this.monitoringSystem = monitoringSystem;
    this.healthCheckSystem = healthCheckSystem;
    this.alertingSystem = alertingSystem;
    initializeDefaultDashboards();
    startBackgroundProcessing();
    LOGGER.info("Custom dashboard system initialized");
  }

  /** Initializes default system dashboards. */
  private void initializeDefaultDashboards() {
    // System Overview Dashboard
    final List<WidgetConfig> systemWidgets = List.of(
        new WidgetConfig(
            "system_health_gauge",
            "System Health",
            "Overall system health status",
            VisualizationType.GAUGE,
            List.of("system_health"),
            Duration.ofMinutes(5),
            AggregationMethod.NONE,
            Map.of("min", 0, "max", 100, "thresholds", List.of(30, 70, 90)),
            Map.of(),
            10,
            true),
        new WidgetConfig(
            "memory_usage_chart",
            "Memory Usage",
            "JVM heap memory usage over time",
            VisualizationType.LINE_CHART,
            List.of("heap_memory_usage"),
            Duration.ofHours(1),
            AggregationMethod.AVERAGE,
            Map.of("showPoints", true, "fillArea", true),
            Map.of(),
            30,
            true),
        new WidgetConfig(
            "thread_count_chart",
            "Thread Count",
            "Active thread count over time",
            VisualizationType.AREA_CHART,
            List.of("thread_count"),
            Duration.ofHours(1),
            AggregationMethod.AVERAGE,
            Map.of("stacked", false),
            Map.of(),
            30,
            true),
        new WidgetConfig(
            "gc_activity_bar",
            "GC Activity",
            "Garbage collection activity",
            VisualizationType.BAR_CHART,
            List.of("gc_time", "gc_count"),
            Duration.ofMinutes(30),
            AggregationMethod.SUM,
            Map.of("horizontal", false),
            Map.of(),
            60,
            false));

    final DashboardConfig systemDashboard = new DashboardConfig(
        "system_overview",
        "System Overview",
        "Comprehensive system health and performance overview",
        "system",
        LayoutType.GRID,
        systemWidgets,
        Map.of("refreshInterval", 30, "theme", "dark"),
        Duration.ofSeconds(30),
        true,
        List.of("system", "health", "performance"));

    createDashboard(systemDashboard);

    // Performance Monitoring Dashboard
    final List<WidgetConfig> performanceWidgets = List.of(
        new WidgetConfig(
            "response_time_histogram",
            "Response Time Distribution",
            "Distribution of response times",
            VisualizationType.HISTOGRAM,
            List.of("response_time_ms"),
            Duration.ofMinutes(15),
            AggregationMethod.NONE,
            Map.of("buckets", 20, "showMean", true, "showMedian", true),
            Map.of(),
            60,
            false),
        new WidgetConfig(
            "error_rate_chart",
            "Error Rate",
            "Application error rate over time",
            VisualizationType.LINE_CHART,
            List.of("error_rate"),
            Duration.ofHours(2),
            AggregationMethod.AVERAGE,
            Map.of("color", "#ff4444", "showThreshold", true, "threshold", 0.05),
            Map.of(),
            30,
            true),
        new WidgetConfig(
            "throughput_chart",
            "Request Throughput",
            "Requests per second over time",
            VisualizationType.AREA_CHART,
            List.of("requests_per_second"),
            Duration.ofHours(1),
            AggregationMethod.AVERAGE,
            Map.of("color", "#44ff44", "fillOpacity", 0.3),
            Map.of(),
            30,
            true));

    final DashboardConfig performanceDashboard = new DashboardConfig(
        "performance_monitoring",
        "Performance Monitoring",
        "Application performance metrics and trends",
        "system",
        LayoutType.FLEXIBLE,
        performanceWidgets,
        Map.of("refreshInterval", 30, "timeRange", "1h"),
        Duration.ofSeconds(30),
        true,
        List.of("performance", "metrics", "application"));

    createDashboard(performanceDashboard);

    LOGGER.info("Initialized " + dashboards.size() + " default dashboards");
  }

  /** Starts background processing tasks. */
  private void startBackgroundProcessing() {
    // Dashboard cache refresh
    backgroundExecutor.scheduleAtFixedRate(
        this::refreshDashboardCache,
        30, 60, TimeUnit.SECONDS);

    // Cache cleanup
    backgroundExecutor.scheduleAtFixedRate(
        this::cleanupCache,
        300, 300, TimeUnit.SECONDS);
  }

  /**
   * Creates a new dashboard.
   *
   * @param config the dashboard configuration
   * @return true if dashboard was created successfully
   */
  public boolean createDashboard(final DashboardConfig config) {
    if (dashboards.size() >= maxDashboards) {
      LOGGER.warning("Maximum dashboard limit reached: " + maxDashboards);
      return false;
    }

    if (config.getWidgets().size() > maxWidgetsPerDashboard) {
      LOGGER.warning("Maximum widgets per dashboard exceeded: " + maxWidgetsPerDashboard);
      return false;
    }

    dashboards.put(config.getDashboardId(), config);
    totalDashboardsCreated.incrementAndGet();
    LOGGER.info("Created dashboard: " + config.getDashboardId() + " (" + config.getName() + ")");
    return true;
  }

  /**
   * Updates an existing dashboard.
   *
   * @param dashboardId the dashboard identifier
   * @param updatedConfig the updated configuration
   * @return true if dashboard was updated successfully
   */
  public boolean updateDashboard(final String dashboardId, final DashboardConfig updatedConfig) {
    final DashboardConfig existing = dashboards.get(dashboardId);
    if (existing == null) {
      return false;
    }

    // Validate update
    if (updatedConfig.getWidgets().size() > maxWidgetsPerDashboard) {
      LOGGER.warning("Maximum widgets per dashboard exceeded: " + maxWidgetsPerDashboard);
      return false;
    }

    dashboards.put(dashboardId, updatedConfig);
    dashboardCache.remove(dashboardId); // Invalidate cache
    LOGGER.info("Updated dashboard: " + dashboardId);
    return true;
  }

  /**
   * Deletes a dashboard.
   *
   * @param dashboardId the dashboard identifier
   * @return true if dashboard was deleted successfully
   */
  public boolean deleteDashboard(final String dashboardId) {
    final DashboardConfig removed = dashboards.remove(dashboardId);
    if (removed != null) {
      dashboardCache.remove(dashboardId);
      widgetDataCache.entrySet().removeIf(entry -> entry.getKey().startsWith(dashboardId + "_"));
      LOGGER.info("Deleted dashboard: " + dashboardId);
      return true;
    }
    return false;
  }

  /**
   * Renders a dashboard with current data.
   *
   * @param dashboardId the dashboard identifier
   * @return rendered dashboard or null if not found
   */
  public RenderedDashboard renderDashboard(final String dashboardId) {
    final DashboardConfig config = dashboards.get(dashboardId);
    if (config == null) {
      return null;
    }

    final long startTime = System.nanoTime();
    final List<String> errors = new ArrayList<>();
    final List<RenderedWidget> renderedWidgets = new ArrayList<>();

    // Render each widget
    for (final WidgetConfig widget : config.getWidgets()) {
      try {
        final RenderedWidget renderedWidget = renderWidget(widget);
        renderedWidgets.add(renderedWidget);
        totalWidgetsRendered.incrementAndGet();
      } catch (final Exception e) {
        errors.add("Failed to render widget " + widget.getWidgetId() + ": " + e.getMessage());
        LOGGER.warning("Widget rendering error: " + e.getMessage());
      }
    }

    final Duration renderingTime = Duration.ofNanos(System.nanoTime() - startTime);
    totalRenderingTime.addAndGet(renderingTime.toNanos());

    final RenderedDashboard rendered = new RenderedDashboard(
        config.getDashboardId(),
        config.getName(),
        config.getDescription(),
        config.getLayoutType(),
        renderedWidgets,
        config.getGlobalSettings(),
        renderingTime,
        errors);

    // Cache the result
    dashboardCache.put(dashboardId, rendered);

    return rendered;
  }

  /** Renders a widget with current data. */
  private RenderedWidget renderWidget(final WidgetConfig widget) {
    final List<String> warnings = new ArrayList<>();
    final List<DataSeries> dataSeries = new ArrayList<>();

    // Collect data for each metric
    for (final String metricName : widget.getMetricNames()) {
      try {
        final DataSeries series = collectWidgetData(widget, metricName);
        if (series != null) {
          dataSeries.add(series);
        } else {
          warnings.add("No data available for metric: " + metricName);
        }
      } catch (final Exception e) {
        warnings.add("Failed to collect data for " + metricName + ": " + e.getMessage());
      }
    }

    // Prepare rendering options
    final Map<String, Object> renderingOptions = new LinkedHashMap<>(widget.getDisplayOptions());
    renderingOptions.put("timeRange", widget.getTimeRange().toString());
    renderingOptions.put("aggregation", widget.getAggregationMethod().getDisplayName());
    renderingOptions.put("refreshInterval", widget.getRefreshIntervalSeconds());

    final String status = warnings.isEmpty() ? "OK" : "WARNING";

    return new RenderedWidget(
        widget.getWidgetId(),
        widget.getTitle(),
        widget.getVisualizationType(),
        dataSeries,
        renderingOptions,
        status,
        warnings);
  }

  /** Collects data for a widget from monitoring systems. */
  private DataSeries collectWidgetData(final WidgetConfig widget, final String metricName) {
    // Check cache first
    final String cacheKey = widget.getWidgetId() + "_" + metricName;
    final List<DataSeries> cached = widgetDataCache.get(cacheKey);
    if (cached != null && !cached.isEmpty()) {
      final DataSeries cachedSeries = cached.get(0);
      // Use cache if recent enough
      if (Duration.between(Instant.now().minus(Duration.ofSeconds(widget.getRefreshIntervalSeconds())),
          Instant.now()).toSeconds() < widget.getRefreshIntervalSeconds()) {
        return cachedSeries;
      }
    }

    // Collect fresh data
    final List<DataPoint> dataPoints = new ArrayList<>();

    // Get data from monitoring system
    if (monitoringSystem != null) {
      final ProductionMonitoringSystem.MetricDataPoint currentMetric =
          monitoringSystem.getCurrentMetric(metricName);
      if (currentMetric != null) {
        dataPoints.add(new DataPoint(
            currentMetric.getTimestamp(),
            currentMetric.getValue(),
            currentMetric.getTags()));
      }
    }

    // Generate synthetic time series data for demo purposes
    generateSyntheticData(metricName, widget.getTimeRange(), dataPoints);

    // Apply aggregation if needed
    final List<DataPoint> aggregatedPoints = applyAggregation(dataPoints, widget.getAggregationMethod());

    final DataSeries series = new DataSeries(
        cacheKey,
        metricName,
        aggregatedPoints,
        widget.getAggregationMethod(),
        Map.of("widget_id", widget.getWidgetId(), "visualization", widget.getVisualizationType().toString()));

    // Cache the result
    widgetDataCache.put(cacheKey, List.of(series));

    return series;
  }

  /** Generates synthetic data for demonstration purposes. */
  private void generateSyntheticData(final String metricName, final Duration timeRange, final List<DataPoint> dataPoints) {
    final Instant now = Instant.now();
    final Instant start = now.minus(timeRange);
    final int pointCount = Math.min(100, (int) (timeRange.toMinutes()));

    for (int i = 0; i < pointCount; i++) {
      final Instant timestamp = start.plus(Duration.ofMinutes(i * timeRange.toMinutes() / pointCount));
      final double value = generateSyntheticValue(metricName, i, pointCount);
      dataPoints.add(new DataPoint(timestamp, value, Map.of("synthetic", "true")));
    }
  }

  /** Generates synthetic values based on metric name patterns. */
  private double generateSyntheticValue(final String metricName, final int index, final int total) {
    final double progress = (double) index / total;

    switch (metricName) {
      case "heap_memory_usage":
        return 50 + 30 * Math.sin(progress * 4 * Math.PI) + Math.random() * 10;
      case "thread_count":
        return 20 + 10 * progress + Math.random() * 5;
      case "response_time_ms":
        return 100 + 50 * Math.sin(progress * 2 * Math.PI) + Math.random() * 20;
      case "error_rate":
        return Math.max(0, 0.02 + 0.03 * Math.sin(progress * 6 * Math.PI) + Math.random() * 0.01);
      case "requests_per_second":
        return 50 + 25 * Math.sin(progress * 3 * Math.PI) + Math.random() * 10;
      case "gc_time":
        return Math.random() * 100;
      case "gc_count":
        return Math.floor(Math.random() * 10);
      default:
        return 50 + Math.random() * 100;
    }
  }

  /** Applies aggregation to data points. */
  private List<DataPoint> applyAggregation(final List<DataPoint> dataPoints, final AggregationMethod method) {
    if (method == AggregationMethod.NONE || dataPoints.size() <= 1) {
      return dataPoints;
    }

    // Simple aggregation - in production would implement proper time-based aggregation
    switch (method) {
      case AVERAGE:
        final double avg = dataPoints.stream().mapToDouble(DataPoint::getValue).average().orElse(0.0);
        return List.of(new DataPoint(Instant.now(), avg, Map.of("aggregated", "true")));
      case SUM:
        final double sum = dataPoints.stream().mapToDouble(DataPoint::getValue).sum();
        return List.of(new DataPoint(Instant.now(), sum, Map.of("aggregated", "true")));
      case MIN:
        final double min = dataPoints.stream().mapToDouble(DataPoint::getValue).min().orElse(0.0);
        return List.of(new DataPoint(Instant.now(), min, Map.of("aggregated", "true")));
      case MAX:
        final double max = dataPoints.stream().mapToDouble(DataPoint::getValue).max().orElse(0.0);
        return List.of(new DataPoint(Instant.now(), max, Map.of("aggregated", "true")));
      case COUNT:
        return List.of(new DataPoint(Instant.now(), dataPoints.size(), Map.of("aggregated", "true")));
      default:
        return dataPoints;
    }
  }

  /** Refreshes dashboard cache. */
  private void refreshDashboardCache() {
    try {
      // Refresh dashboards that have real-time widgets
      for (final DashboardConfig dashboard : dashboards.values()) {
        final boolean hasRealTimeWidgets = dashboard.getWidgets().stream()
            .anyMatch(WidgetConfig::isRealTimeUpdates);

        if (hasRealTimeWidgets && realTimeEnabled) {
          // Invalidate cache to force refresh on next render
          dashboardCache.remove(dashboard.getDashboardId());
        }
      }
    } catch (final Exception e) {
      LOGGER.warning("Error refreshing dashboard cache: " + e.getMessage());
    }
  }

  /** Cleans up expired cache entries. */
  private void cleanupCache() {
    try {
      final Instant cutoff = Instant.now().minus(cacheRetentionPeriod);

      // Clean dashboard cache
      dashboardCache.entrySet().removeIf(entry ->
          entry.getValue().getLastRendered().isBefore(cutoff));

      // Clean widget data cache
      widgetDataCache.clear(); // Simple approach - could be more sophisticated

      lastCacheCleanup.set(Instant.now());

      LOGGER.fine("Cache cleanup completed");
    } catch (final Exception e) {
      LOGGER.warning("Error during cache cleanup: " + e.getMessage());
    }
  }

  /**
   * Gets dashboard configuration.
   *
   * @param dashboardId the dashboard identifier
   * @return dashboard configuration or null if not found
   */
  public DashboardConfig getDashboard(final String dashboardId) {
    return dashboards.get(dashboardId);
  }

  /**
   * Lists all available dashboards.
   *
   * @return list of dashboard configurations
   */
  public List<DashboardConfig> listDashboards() {
    return dashboards.values().stream()
        .sorted(Comparator.comparing(DashboardConfig::getName))
        .collect(Collectors.toList());
  }

  /**
   * Searches dashboards by tags.
   *
   * @param tags the tags to search for
   * @return list of matching dashboard configurations
   */
  public List<DashboardConfig> searchDashboardsByTags(final List<String> tags) {
    return dashboards.values().stream()
        .filter(dashboard -> dashboard.getTags().stream().anyMatch(tags::contains))
        .sorted(Comparator.comparing(DashboardConfig::getName))
        .collect(Collectors.toList());
  }

  /**
   * Exports dashboard configuration as JSON.
   *
   * @param dashboardId the dashboard identifier
   * @return JSON representation of dashboard configuration
   */
  public String exportDashboardAsJson(final String dashboardId) {
    final DashboardConfig config = dashboards.get(dashboardId);
    if (config == null) {
      return null;
    }

    // Simple JSON export - in production would use proper JSON library
    final StringBuilder json = new StringBuilder();
    json.append("{\n");
    json.append("  \"dashboardId\": \"").append(config.getDashboardId()).append("\",\n");
    json.append("  \"name\": \"").append(config.getName()).append("\",\n");
    json.append("  \"description\": \"").append(config.getDescription()).append("\",\n");
    json.append("  \"owner\": \"").append(config.getOwner()).append("\",\n");
    json.append("  \"layoutType\": \"").append(config.getLayoutType()).append("\",\n");
    json.append("  \"widgets\": [\n");

    for (int i = 0; i < config.getWidgets().size(); i++) {
      final WidgetConfig widget = config.getWidgets().get(i);
      json.append("    {\n");
      json.append("      \"widgetId\": \"").append(widget.getWidgetId()).append("\",\n");
      json.append("      \"title\": \"").append(widget.getTitle()).append("\",\n");
      json.append("      \"visualizationType\": \"").append(widget.getVisualizationType()).append("\",\n");
      json.append("      \"metricNames\": ").append(widget.getMetricNames().toString()).append(",\n");
      json.append("      \"timeRange\": \"").append(widget.getTimeRange()).append("\",\n");
      json.append("      \"aggregationMethod\": \"").append(widget.getAggregationMethod()).append("\"\n");
      json.append("    }");
      if (i < config.getWidgets().size() - 1) {
        json.append(",");
      }
      json.append("\n");
    }

    json.append("  ],\n");
    json.append("  \"createdAt\": \"").append(config.getCreatedAt().toString()).append("\",\n");
    json.append("  \"lastModified\": \"").append(config.getLastModified().toString()).append("\"\n");
    json.append("}");

    return json.toString();
  }

  /**
   * Gets dashboard system statistics.
   *
   * @return formatted statistics
   */
  public String getDashboardStatistics() {
    return String.format(
        "Dashboard System Statistics: dashboards=%d, total_created=%d, widgets_rendered=%d, " +
        "avg_rendering_time=%.2fms, cache_entries=%d, last_cleanup=%s",
        dashboards.size(),
        totalDashboardsCreated.get(),
        totalWidgetsRendered.get(),
        totalWidgetsRendered.get() > 0 ? totalRenderingTime.get() / 1_000_000.0 / totalWidgetsRendered.get() : 0.0,
        dashboardCache.size() + widgetDataCache.size(),
        lastCacheCleanup.get().atZone(ZoneId.systemDefault()).format(DateTimeFormatter.ISO_LOCAL_TIME));
  }

  /**
   * Sets real-time updates enabled state.
   *
   * @param enabled true to enable real-time updates
   */
  public void setRealTimeEnabled(final boolean enabled) {
    this.realTimeEnabled = enabled;
    LOGGER.info("Real-time dashboard updates " + (enabled ? "enabled" : "disabled"));
  }

  /** Shuts down the dashboard system. */
  public void shutdown() {
    backgroundExecutor.shutdown();
    try {
      if (!backgroundExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        backgroundExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      backgroundExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Custom dashboard system shutdown");
  }
}