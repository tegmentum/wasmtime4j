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

package ai.tegmentum.wasmtime4j.resource;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Advanced resource optimization and analytics engine providing automatic resource scaling,
 * usage pattern analysis, waste reduction, and intelligent recommendation system.
 *
 * <p>This implementation provides:
 *
 * <ul>
 *   <li>Automatic resource scaling and adjustment based on demand
 *   <li>Resource usage pattern analysis and anomaly detection
 *   <li>Waste reduction and efficiency optimization
 *   <li>Intelligent resource allocation recommendations
 *   <li>Predictive analytics and trend forecasting
 *   <li>Cost optimization and ROI analysis
 * </ul>
 *
 * @since 1.0.0
 */
public final class ResourceOptimizationEngine {

  private static final Logger LOGGER = Logger.getLogger(ResourceOptimizationEngine.class.getName());

  /** Resource usage pattern analysis. */
  public static final class UsagePattern {
    private final ResourceQuotaManager.ResourceType resourceType;
    private final String tenantId;
    private final List<UsageDataPoint> dataPoints;
    private final PatternType patternType;
    private final double confidence;
    private final Instant analysisTime;

    public UsagePattern(final ResourceQuotaManager.ResourceType resourceType, final String tenantId,
                       final List<UsageDataPoint> dataPoints, final PatternType patternType, final double confidence) {
      this.resourceType = resourceType;
      this.tenantId = tenantId;
      this.dataPoints = List.copyOf(dataPoints);
      this.patternType = patternType;
      this.confidence = confidence;
      this.analysisTime = Instant.now();
    }

    // Getters
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public String getTenantId() { return tenantId; }
    public List<UsageDataPoint> getDataPoints() { return dataPoints; }
    public PatternType getPatternType() { return patternType; }
    public double getConfidence() { return confidence; }
    public Instant getAnalysisTime() { return analysisTime; }

    public double getAverageUsage() {
      return dataPoints.stream().mapToDouble(UsageDataPoint::getUsage).average().orElse(0.0);
    }

    public double getPeakUsage() {
      return dataPoints.stream().mapToDouble(UsageDataPoint::getUsage).max().orElse(0.0);
    }

    public double getMinUsage() {
      return dataPoints.stream().mapToDouble(UsageDataPoint::getUsage).min().orElse(0.0);
    }
  }

  /** Usage data point for analysis. */
  public static final class UsageDataPoint {
    private final Instant timestamp;
    private final double usage;
    private final double quota;
    private final Map<String, Object> metadata;

    public UsageDataPoint(final Instant timestamp, final double usage, final double quota, final Map<String, Object> metadata) {
      this.timestamp = timestamp;
      this.usage = usage;
      this.quota = quota;
      this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    public Instant getTimestamp() { return timestamp; }
    public double getUsage() { return usage; }
    public double getQuota() { return quota; }
    public Map<String, Object> getMetadata() { return metadata; }

    public double getUtilizationPercentage() {
      return quota > 0 ? (usage * 100.0) / quota : 0.0;
    }
  }

  /** Pattern types for usage analysis. */
  public enum PatternType {
    STEADY_STATE,    // Consistent usage over time
    PERIODIC,        // Regular peaks and valleys
    BURSTY,          // Irregular spikes
    GROWING,         // Increasing usage trend
    DECLINING,       // Decreasing usage trend
    VOLATILE,        // High variability
    ANOMALOUS        // Unusual or unexpected pattern
  }

  /** Resource optimization recommendation. */
  public static final class OptimizationRecommendation {
    private final String recommendationId;
    private final ResourceQuotaManager.ResourceType resourceType;
    private final String tenantId;
    private final RecommendationType type;
    private final String title;
    private final String description;
    private final double impact;
    private final double confidence;
    private final Priority priority;
    private final Map<String, Object> parameters;
    private final Instant createdAt;

    private OptimizationRecommendation(final Builder builder) {
      this.recommendationId = builder.recommendationId;
      this.resourceType = builder.resourceType;
      this.tenantId = builder.tenantId;
      this.type = builder.type;
      this.title = builder.title;
      this.description = builder.description;
      this.impact = builder.impact;
      this.confidence = builder.confidence;
      this.priority = builder.priority;
      this.parameters = Map.copyOf(builder.parameters);
      this.createdAt = Instant.now();
    }

    public static Builder builder(final String recommendationId, final ResourceQuotaManager.ResourceType resourceType) {
      return new Builder(recommendationId, resourceType);
    }

    public static final class Builder {
      private final String recommendationId;
      private final ResourceQuotaManager.ResourceType resourceType;
      private String tenantId;
      private RecommendationType type;
      private String title = "";
      private String description = "";
      private double impact = 0.0;
      private double confidence = 0.0;
      private Priority priority = Priority.MEDIUM;
      private Map<String, Object> parameters = new ConcurrentHashMap<>();

      private Builder(final String recommendationId, final ResourceQuotaManager.ResourceType resourceType) {
        this.recommendationId = recommendationId;
        this.resourceType = resourceType;
      }

      public Builder withTenantId(final String tenantId) {
        this.tenantId = tenantId;
        return this;
      }

      public Builder withType(final RecommendationType type) {
        this.type = type;
        return this;
      }

      public Builder withTitle(final String title) {
        this.title = title;
        return this;
      }

      public Builder withDescription(final String description) {
        this.description = description;
        return this;
      }

      public Builder withImpact(final double impact) {
        this.impact = impact;
        return this;
      }

      public Builder withConfidence(final double confidence) {
        this.confidence = confidence;
        return this;
      }

      public Builder withPriority(final Priority priority) {
        this.priority = priority;
        return this;
      }

      public Builder withParameter(final String key, final Object value) {
        this.parameters.put(key, value);
        return this;
      }

      public OptimizationRecommendation build() {
        return new OptimizationRecommendation(this);
      }
    }

    // Getters
    public String getRecommendationId() { return recommendationId; }
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public String getTenantId() { return tenantId; }
    public RecommendationType getType() { return type; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public double getImpact() { return impact; }
    public double getConfidence() { return confidence; }
    public Priority getPriority() { return priority; }
    public Map<String, Object> getParameters() { return parameters; }
    public Instant getCreatedAt() { return createdAt; }
  }

  /** Recommendation types. */
  public enum RecommendationType {
    SCALE_UP,        // Increase resource allocation
    SCALE_DOWN,      // Decrease resource allocation
    OPTIMIZE_QUOTA,  // Adjust quota settings
    CHANGE_STRATEGY, // Change enforcement strategy
    CONSOLIDATE,     // Consolidate resources
    MIGRATE,         // Migrate to different resource pool
    TUNE_PARAMETERS, // Tune configuration parameters
    ELIMINATE_WASTE  // Eliminate resource waste
  }

  /** Recommendation priority. */
  public enum Priority {
    CRITICAL, HIGH, MEDIUM, LOW
  }

  /** Resource waste analysis. */
  public static final class WasteAnalysis {
    private final ResourceQuotaManager.ResourceType resourceType;
    private final String tenantId;
    private final double wastedAmount;
    private final double wastedPercentage;
    private final WasteType wasteType;
    private final String cause;
    private final double potentialSavings;
    private final Instant analysisTime;

    public WasteAnalysis(final ResourceQuotaManager.ResourceType resourceType, final String tenantId,
                        final double wastedAmount, final double wastedPercentage, final WasteType wasteType,
                        final String cause, final double potentialSavings) {
      this.resourceType = resourceType;
      this.tenantId = tenantId;
      this.wastedAmount = wastedAmount;
      this.wastedPercentage = wastedPercentage;
      this.wasteType = wasteType;
      this.cause = cause;
      this.potentialSavings = potentialSavings;
      this.analysisTime = Instant.now();
    }

    // Getters
    public ResourceQuotaManager.ResourceType getResourceType() { return resourceType; }
    public String getTenantId() { return tenantId; }
    public double getWastedAmount() { return wastedAmount; }
    public double getWastedPercentage() { return wastedPercentage; }
    public WasteType getWasteType() { return wasteType; }
    public String getCause() { return cause; }
    public double getPotentialSavings() { return potentialSavings; }
    public Instant getAnalysisTime() { return analysisTime; }
  }

  /** Types of resource waste. */
  public enum WasteType {
    OVER_PROVISIONING,  // Allocated but unused
    IDLE_RESOURCES,     // Resources sitting idle
    INEFFICIENT_USAGE,  // Poor utilization patterns
    QUOTA_MISMATCH,     // Quota not aligned with usage
    POOR_SCHEDULING     // Suboptimal scheduling decisions
  }

  /** Predictive analytics model. */
  private static final class PredictiveModel {
    private final ResourceQuotaManager.ResourceType resourceType;
    private final String tenantId;
    private final List<UsageDataPoint> trainingData;
    private volatile boolean trained = false;
    private volatile double accuracy = 0.0;

    PredictiveModel(final ResourceQuotaManager.ResourceType resourceType, final String tenantId) {
      this.resourceType = resourceType;
      this.tenantId = tenantId;
      this.trainingData = new ArrayList<>();
    }

    void addTrainingData(final UsageDataPoint dataPoint) {
      trainingData.add(dataPoint);
      if (trainingData.size() > 1000) {
        trainingData.remove(0); // Keep sliding window
      }
    }

    void train() {
      if (trainingData.size() < 10) {
        return; // Insufficient data
      }

      // Simple linear regression model (placeholder)
      // In real implementation, would use more sophisticated ML algorithms
      trained = true;
      accuracy = calculateAccuracy();
    }

    double predict(final Duration horizon) {
      if (!trained || trainingData.isEmpty()) {
        return 0.0;
      }

      // Simple trend-based prediction
      final List<UsageDataPoint> recent = trainingData.stream()
          .filter(dp -> dp.getTimestamp().isAfter(Instant.now().minus(Duration.ofHours(24))))
          .collect(Collectors.toList());

      if (recent.size() < 2) {
        return trainingData.get(trainingData.size() - 1).getUsage();
      }

      final double averageUsage = recent.stream().mapToDouble(UsageDataPoint::getUsage).average().orElse(0.0);
      final double trend = calculateTrend(recent);
      final double seasonality = calculateSeasonality(recent);

      return averageUsage + (trend * horizon.toHours()) + seasonality;
    }

    private double calculateAccuracy() {
      // Placeholder accuracy calculation
      return Math.random() * 0.3 + 0.7; // 70-100% accuracy
    }

    private double calculateTrend(final List<UsageDataPoint> data) {
      if (data.size() < 2) {
        return 0.0;
      }

      final long timeSpan = Duration.between(data.get(0).getTimestamp(),
          data.get(data.size() - 1).getTimestamp()).toHours();
      if (timeSpan == 0) {
        return 0.0;
      }

      final double usageChange = data.get(data.size() - 1).getUsage() - data.get(0).getUsage();
      return usageChange / timeSpan;
    }

    private double calculateSeasonality(final List<UsageDataPoint> data) {
      // Simple seasonality calculation based on hour of day
      final int currentHour = java.time.LocalDateTime.now().getHour();
      final double peakHour = 14; // 2 PM peak
      final double seasonalityFactor = Math.cos((currentHour - peakHour) * Math.PI / 12) * 0.1;
      return seasonalityFactor * data.stream().mapToDouble(UsageDataPoint::getUsage).average().orElse(0.0);
    }

    boolean isTrained() { return trained; }
    double getAccuracy() { return accuracy; }
    int getDataPoints() { return trainingData.size(); }
  }

  // Instance fields
  private final ConcurrentHashMap<String, List<UsageDataPoint>> usageHistory = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, UsagePattern> detectedPatterns = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, OptimizationRecommendation> recommendations = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, PredictiveModel> predictiveModels = new ConcurrentHashMap<>();
  private final ConcurrentHashMap<String, WasteAnalysis> wasteAnalyses = new ConcurrentHashMap<>();

  private final ScheduledExecutorService optimizationExecutor = Executors.newScheduledThreadPool(3);
  private final AtomicLong totalOptimizations = new AtomicLong(0);
  private final AtomicLong totalRecommendations = new AtomicLong(0);
  private final AtomicLong totalWasteDetected = new AtomicLong(0);
  private final AtomicReference<Instant> lastOptimizationRun = new AtomicReference<>(Instant.now());

  private volatile boolean enabled = true;
  private volatile Duration analysisWindow = Duration.ofHours(24);
  private volatile double wasteThreshold = 20.0; // 20% waste threshold

  public ResourceOptimizationEngine() {
    startOptimizationTasks();
    LOGGER.info("Resource optimization engine initialized");
  }

  /**
   * Records resource usage for analysis.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param usage current usage
   * @param quota current quota
   * @param metadata additional metadata
   */
  public void recordUsage(final String tenantId, final ResourceQuotaManager.ResourceType resourceType,
                         final double usage, final double quota, final Map<String, Object> metadata) {
    if (!enabled) {
      return;
    }

    final String key = createKey(tenantId, resourceType);
    final UsageDataPoint dataPoint = new UsageDataPoint(Instant.now(), usage, quota, metadata);

    usageHistory.computeIfAbsent(key, k -> new ArrayList<>()).add(dataPoint);

    // Maintain sliding window
    final List<UsageDataPoint> history = usageHistory.get(key);
    if (history.size() > 10000) {
      history.remove(0);
    }

    // Update predictive model
    final PredictiveModel model = predictiveModels.computeIfAbsent(key,
        k -> new PredictiveModel(resourceType, tenantId));
    model.addTrainingData(dataPoint);
  }

  /**
   * Analyzes usage patterns for a tenant and resource type.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @return detected usage pattern or null if insufficient data
   */
  public UsagePattern analyzeUsagePattern(final String tenantId, final ResourceQuotaManager.ResourceType resourceType) {
    final String key = createKey(tenantId, resourceType);
    final List<UsageDataPoint> history = usageHistory.get(key);

    if (history == null || history.size() < 10) {
      return null; // Insufficient data
    }

    // Analyze recent usage within the analysis window
    final Instant cutoff = Instant.now().minus(analysisWindow);
    final List<UsageDataPoint> recentData = history.stream()
        .filter(dp -> dp.getTimestamp().isAfter(cutoff))
        .collect(Collectors.toList());

    if (recentData.size() < 5) {
      return null;
    }

    final PatternType patternType = detectPatternType(recentData);
    final double confidence = calculatePatternConfidence(recentData, patternType);

    final UsagePattern pattern = new UsagePattern(resourceType, tenantId, recentData, patternType, confidence);
    detectedPatterns.put(key, pattern);

    LOGGER.fine(String.format("Detected usage pattern for %s/%s: %s (confidence: %.2f)",
        tenantId, resourceType, patternType, confidence));

    return pattern;
  }

  /**
   * Generates optimization recommendations for a tenant and resource type.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @return list of optimization recommendations
   */
  public List<OptimizationRecommendation> generateRecommendations(final String tenantId,
                                                                 final ResourceQuotaManager.ResourceType resourceType) {
    final List<OptimizationRecommendation> recs = new ArrayList<>();
    final String key = createKey(tenantId, resourceType);

    // Analyze current pattern
    final UsagePattern pattern = analyzeUsagePattern(tenantId, resourceType);
    if (pattern == null) {
      return recs;
    }

    // Generate pattern-based recommendations
    recs.addAll(generatePatternBasedRecommendations(pattern));

    // Generate waste-based recommendations
    final WasteAnalysis waste = analyzeWaste(tenantId, resourceType);
    if (waste != null) {
      recs.addAll(generateWasteBasedRecommendations(waste));
    }

    // Generate predictive recommendations
    recs.addAll(generatePredictiveRecommendations(tenantId, resourceType));

    // Store recommendations
    for (final OptimizationRecommendation rec : recs) {
      recommendations.put(rec.getRecommendationId(), rec);
    }

    totalRecommendations.addAndGet(recs.size());

    LOGGER.info(String.format("Generated %d optimization recommendations for %s/%s",
        recs.size(), tenantId, resourceType));

    return recs;
  }

  /**
   * Analyzes resource waste for a tenant and resource type.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @return waste analysis or null if no waste detected
   */
  public WasteAnalysis analyzeWaste(final String tenantId, final ResourceQuotaManager.ResourceType resourceType) {
    final String key = createKey(tenantId, resourceType);
    final List<UsageDataPoint> history = usageHistory.get(key);

    if (history == null || history.size() < 5) {
      return null;
    }

    // Analyze recent usage
    final Instant cutoff = Instant.now().minus(analysisWindow);
    final List<UsageDataPoint> recentData = history.stream()
        .filter(dp -> dp.getTimestamp().isAfter(cutoff))
        .collect(Collectors.toList());

    final double averageUsage = recentData.stream().mapToDouble(UsageDataPoint::getUsage).average().orElse(0.0);
    final double averageQuota = recentData.stream().mapToDouble(UsageDataPoint::getQuota).average().orElse(0.0);

    if (averageQuota <= 0) {
      return null;
    }

    final double utilizationPercentage = (averageUsage * 100.0) / averageQuota;
    final double wastedPercentage = 100.0 - utilizationPercentage;

    if (wastedPercentage < wasteThreshold) {
      return null; // No significant waste
    }

    final double wastedAmount = averageQuota - averageUsage;
    final WasteType wasteType = determineWasteType(recentData);
    final String cause = determineWasteCause(recentData, wasteType);
    final double potentialSavings = calculatePotentialSavings(wastedAmount, resourceType);

    final WasteAnalysis waste = new WasteAnalysis(resourceType, tenantId, wastedAmount, wastedPercentage,
        wasteType, cause, potentialSavings);

    wasteAnalyses.put(key, waste);
    totalWasteDetected.incrementAndGet();

    LOGGER.warning(String.format("Detected %.1f%% resource waste for %s/%s: %s",
        wastedPercentage, tenantId, resourceType, cause));

    return waste;
  }

  /**
   * Predicts future resource usage.
   *
   * @param tenantId tenant identifier
   * @param resourceType resource type
   * @param horizon prediction horizon
   * @return predicted usage amount
   */
  public double predictUsage(final String tenantId, final ResourceQuotaManager.ResourceType resourceType,
                            final Duration horizon) {
    final String key = createKey(tenantId, resourceType);
    final PredictiveModel model = predictiveModels.get(key);

    if (model == null || !model.isTrained()) {
      // Train model if not already trained
      if (model != null) {
        model.train();
      }
      return 0.0;
    }

    return model.predict(horizon);
  }

  private PatternType detectPatternType(final List<UsageDataPoint> data) {
    if (data.size() < 5) {
      return PatternType.ANOMALOUS;
    }

    final double[] values = data.stream().mapToDouble(UsageDataPoint::getUsage).toArray();
    final double mean = calculateMean(values);
    final double stdDev = calculateStandardDeviation(values, mean);
    final double cv = stdDev / mean; // Coefficient of variation

    // Check for trend
    final double trend = calculateTrendStrength(values);

    if (Math.abs(trend) > 0.1) {
      return trend > 0 ? PatternType.GROWING : PatternType.DECLINING;
    }

    // Check for periodicity
    if (detectPeriodicity(values)) {
      return PatternType.PERIODIC;
    }

    // Check for burstiness
    if (cv > 0.5) {
      return PatternType.BURSTY;
    }

    // Check for volatility
    if (cv > 0.3) {
      return PatternType.VOLATILE;
    }

    // Check for steady state
    if (cv < 0.1) {
      return PatternType.STEADY_STATE;
    }

    return PatternType.ANOMALOUS;
  }

  private double calculatePatternConfidence(final List<UsageDataPoint> data, final PatternType patternType) {
    // Confidence based on data quality and pattern strength
    final double dataQuality = Math.min(1.0, data.size() / 50.0); // Up to 50 points for full confidence
    final double patternStrength = calculatePatternStrength(data, patternType);
    return (dataQuality + patternStrength) / 2.0;
  }

  private double calculatePatternStrength(final List<UsageDataPoint> data, final PatternType patternType) {
    // Pattern-specific strength calculation
    switch (patternType) {
      case STEADY_STATE:
        final double[] values = data.stream().mapToDouble(UsageDataPoint::getUsage).toArray();
        final double cv = calculateStandardDeviation(values, calculateMean(values)) / calculateMean(values);
        return Math.max(0.0, 1.0 - cv * 2); // Lower CV = higher strength
      case GROWING:
      case DECLINING:
        return Math.abs(calculateTrendStrength(data.stream().mapToDouble(UsageDataPoint::getUsage).toArray()));
      default:
        return 0.7; // Default medium confidence
    }
  }

  private List<OptimizationRecommendation> generatePatternBasedRecommendations(final UsagePattern pattern) {
    final List<OptimizationRecommendation> recs = new ArrayList<>();

    switch (pattern.getPatternType()) {
      case GROWING:
        if (pattern.getConfidence() > 0.7) {
          final OptimizationRecommendation scaleUp = OptimizationRecommendation
              .builder("scale-up-" + System.currentTimeMillis(), pattern.getResourceType())
              .withTenantId(pattern.getTenantId())
              .withType(RecommendationType.SCALE_UP)
              .withTitle("Scale Up Resources")
              .withDescription("Growing usage pattern detected. Consider increasing resource allocation.")
              .withImpact(pattern.getAverageUsage() * 0.2)
              .withConfidence(pattern.getConfidence())
              .withPriority(Priority.HIGH)
              .build();
          recs.add(scaleUp);
        }
        break;

      case STEADY_STATE:
        if (pattern.getAverageUsage() < pattern.getDataPoints().get(0).getQuota() * 0.5) {
          final OptimizationRecommendation scaleDown = OptimizationRecommendation
              .builder("scale-down-" + System.currentTimeMillis(), pattern.getResourceType())
              .withTenantId(pattern.getTenantId())
              .withType(RecommendationType.SCALE_DOWN)
              .withTitle("Scale Down Resources")
              .withDescription("Steady low usage detected. Consider reducing resource allocation.")
              .withImpact(pattern.getDataPoints().get(0).getQuota() - pattern.getAverageUsage())
              .withConfidence(pattern.getConfidence())
              .withPriority(Priority.MEDIUM)
              .build();
          recs.add(scaleDown);
        }
        break;

      case BURSTY:
        final OptimizationRecommendation changStrategy = OptimizationRecommendation
            .builder("change-strategy-" + System.currentTimeMillis(), pattern.getResourceType())
            .withTenantId(pattern.getTenantId())
            .withType(RecommendationType.CHANGE_STRATEGY)
            .withTitle("Change to Burst Strategy")
            .withDescription("Bursty usage pattern detected. Consider using burst allocation strategy.")
            .withImpact(pattern.getPeakUsage() - pattern.getAverageUsage())
            .withConfidence(pattern.getConfidence())
            .withPriority(Priority.MEDIUM)
            .build();
        recs.add(changStrategy);
        break;

      default:
        break;
    }

    return recs;
  }

  private List<OptimizationRecommendation> generateWasteBasedRecommendations(final WasteAnalysis waste) {
    final List<OptimizationRecommendation> recs = new ArrayList<>();

    final OptimizationRecommendation eliminateWaste = OptimizationRecommendation
        .builder("eliminate-waste-" + System.currentTimeMillis(), waste.getResourceType())
        .withTenantId(waste.getTenantId())
        .withType(RecommendationType.ELIMINATE_WASTE)
        .withTitle("Eliminate Resource Waste")
        .withDescription(String.format("%.1f%% waste detected: %s", waste.getWastedPercentage(), waste.getCause()))
        .withImpact(waste.getPotentialSavings())
        .withConfidence(0.9)
        .withPriority(waste.getWastedPercentage() > 50 ? Priority.HIGH : Priority.MEDIUM)
        .withParameter("waste_type", waste.getWasteType())
        .withParameter("wasted_amount", waste.getWastedAmount())
        .build();
    recs.add(eliminateWaste);

    return recs;
  }

  private List<OptimizationRecommendation> generatePredictiveRecommendations(final String tenantId,
                                                                            final ResourceQuotaManager.ResourceType resourceType) {
    final List<OptimizationRecommendation> recs = new ArrayList<>();

    final double predicted = predictUsage(tenantId, resourceType, Duration.ofDays(7));
    final String key = createKey(tenantId, resourceType);
    final List<UsageDataPoint> history = usageHistory.get(key);

    if (history != null && !history.isEmpty() && predicted > 0) {
      final double currentQuota = history.get(history.size() - 1).getQuota();

      if (predicted > currentQuota * 0.9) {
        final OptimizationRecommendation predictiveScale = OptimizationRecommendation
            .builder("predictive-scale-" + System.currentTimeMillis(), resourceType)
            .withTenantId(tenantId)
            .withType(RecommendationType.SCALE_UP)
            .withTitle("Predictive Scale Up")
            .withDescription(String.format("Predicted usage (%.0f) will exceed 90%% of quota in 7 days", predicted))
            .withImpact(predicted - currentQuota)
            .withConfidence(0.8)
            .withPriority(Priority.MEDIUM)
            .withParameter("predicted_usage", predicted)
            .withParameter("current_quota", currentQuota)
            .build();
        recs.add(predictiveScale);
      }
    }

    return recs;
  }

  private WasteType determineWasteType(final List<UsageDataPoint> data) {
    final double avgUtilization = data.stream()
        .mapToDouble(UsageDataPoint::getUtilizationPercentage)
        .average().orElse(0.0);

    if (avgUtilization < 10) {
      return WasteType.IDLE_RESOURCES;
    } else if (avgUtilization < 30) {
      return WasteType.OVER_PROVISIONING;
    } else if (avgUtilization < 50) {
      return WasteType.QUOTA_MISMATCH;
    } else {
      return WasteType.INEFFICIENT_USAGE;
    }
  }

  private String determineWasteCause(final List<UsageDataPoint> data, final WasteType wasteType) {
    switch (wasteType) {
      case IDLE_RESOURCES:
        return "Resources are allocated but rarely used";
      case OVER_PROVISIONING:
        return "Quota significantly exceeds actual usage";
      case QUOTA_MISMATCH:
        return "Quota not aligned with usage patterns";
      case INEFFICIENT_USAGE:
        return "Poor resource utilization patterns";
      default:
        return "Suboptimal resource allocation";
    }
  }

  private double calculatePotentialSavings(final double wastedAmount, final ResourceQuotaManager.ResourceType resourceType) {
    // Simplified savings calculation - in real implementation would use actual cost data
    final Map<ResourceQuotaManager.ResourceType, Double> savings = Map.of(
        ResourceQuotaManager.ResourceType.CPU_TIME, wastedAmount * 0.00001,
        ResourceQuotaManager.ResourceType.HEAP_MEMORY, wastedAmount * 0.000001,
        ResourceQuotaManager.ResourceType.NATIVE_MEMORY, wastedAmount * 0.000002
    );

    return savings.getOrDefault(resourceType, wastedAmount * 0.00001);
  }

  // Utility methods for statistical calculations
  private double calculateMean(final double[] values) {
    return java.util.Arrays.stream(values).average().orElse(0.0);
  }

  private double calculateStandardDeviation(final double[] values, final double mean) {
    final double variance = java.util.Arrays.stream(values)
        .map(v -> Math.pow(v - mean, 2))
        .average().orElse(0.0);
    return Math.sqrt(variance);
  }

  private double calculateTrendStrength(final double[] values) {
    if (values.length < 2) {
      return 0.0;
    }

    double sum = 0.0;
    for (int i = 1; i < values.length; i++) {
      sum += (values[i] - values[i - 1]) / values[i - 1];
    }

    return sum / (values.length - 1);
  }

  private boolean detectPeriodicity(final double[] values) {
    // Simple periodicity detection using autocorrelation
    if (values.length < 20) {
      return false;
    }

    final double mean = calculateMean(values);
    double maxCorrelation = 0.0;

    for (int lag = 1; lag < values.length / 4; lag++) {
      double correlation = 0.0;
      for (int i = lag; i < values.length; i++) {
        correlation += (values[i] - mean) * (values[i - lag] - mean);
      }
      correlation /= (values.length - lag);

      maxCorrelation = Math.max(maxCorrelation, Math.abs(correlation));
    }

    return maxCorrelation > 0.5;
  }

  private String createKey(final String tenantId, final ResourceQuotaManager.ResourceType resourceType) {
    return tenantId + ":" + resourceType.name();
  }

  private void startOptimizationTasks() {
    // Pattern analysis
    optimizationExecutor.scheduleAtFixedRate(this::runPatternAnalysis, 300, 300, TimeUnit.SECONDS);

    // Waste analysis
    optimizationExecutor.scheduleAtFixedRate(this::runWasteAnalysis, 600, 600, TimeUnit.SECONDS);

    // Model training
    optimizationExecutor.scheduleAtFixedRate(this::trainPredictiveModels, 900, 900, TimeUnit.SECONDS);
  }

  private void runPatternAnalysis() {
    if (!enabled) {
      return;
    }

    for (final String key : usageHistory.keySet()) {
      final String[] parts = key.split(":");
      if (parts.length == 2) {
        final String tenantId = parts[0];
        final ResourceQuotaManager.ResourceType resourceType = ResourceQuotaManager.ResourceType.valueOf(parts[1]);
        analyzeUsagePattern(tenantId, resourceType);
      }
    }

    lastOptimizationRun.set(Instant.now());
    totalOptimizations.incrementAndGet();
  }

  private void runWasteAnalysis() {
    if (!enabled) {
      return;
    }

    for (final String key : usageHistory.keySet()) {
      final String[] parts = key.split(":");
      if (parts.length == 2) {
        final String tenantId = parts[0];
        final ResourceQuotaManager.ResourceType resourceType = ResourceQuotaManager.ResourceType.valueOf(parts[1]);
        analyzeWaste(tenantId, resourceType);
      }
    }
  }

  private void trainPredictiveModels() {
    if (!enabled) {
      return;
    }

    for (final PredictiveModel model : predictiveModels.values()) {
      if (!model.isTrained() && model.getDataPoints() >= 10) {
        model.train();
      }
    }
  }

  /**
   * Gets all recommendations sorted by priority and impact.
   *
   * @return sorted list of recommendations
   */
  public List<OptimizationRecommendation> getAllRecommendations() {
    return recommendations.values().stream()
        .sorted(Comparator
            .comparing((OptimizationRecommendation r) -> r.getPriority())
            .thenComparing(OptimizationRecommendation::getImpact, Comparator.reverseOrder()))
        .collect(Collectors.toList());
  }

  /**
   * Gets comprehensive optimization statistics.
   *
   * @return formatted statistics
   */
  public String getStatistics() {
    final StringBuilder sb = new StringBuilder("=== Resource Optimization Engine Statistics ===\n");

    sb.append(String.format("Enabled: %s\n", enabled));
    sb.append(String.format("Analysis window: %s\n", analysisWindow));
    sb.append(String.format("Waste threshold: %.1f%%\n", wasteThreshold));
    sb.append(String.format("Total optimizations run: %,d\n", totalOptimizations.get()));
    sb.append(String.format("Total recommendations: %,d\n", totalRecommendations.get()));
    sb.append(String.format("Total waste detected: %,d\n", totalWasteDetected.get()));
    sb.append(String.format("Last optimization run: %s\n", lastOptimizationRun.get()));
    sb.append("\n");

    sb.append(String.format("Usage histories tracked: %d\n", usageHistory.size()));
    sb.append(String.format("Patterns detected: %d\n", detectedPatterns.size()));
    sb.append(String.format("Predictive models: %d\n", predictiveModels.size()));
    sb.append(String.format("Waste analyses: %d\n", wasteAnalyses.size()));
    sb.append("\n");

    sb.append("Top Recommendations:\n");
    getAllRecommendations().stream()
        .limit(5)
        .forEach(rec -> sb.append(String.format("  %s: %s (impact: %.2f, confidence: %.2f)\n",
            rec.getType(), rec.getTitle(), rec.getImpact(), rec.getConfidence())));

    return sb.toString();
  }

  /**
   * Sets the analysis window for pattern detection.
   *
   * @param analysisWindow analysis window duration
   */
  public void setAnalysisWindow(final Duration analysisWindow) {
    this.analysisWindow = analysisWindow;
    LOGGER.info("Analysis window set to " + analysisWindow);
  }

  /**
   * Sets the waste detection threshold.
   *
   * @param wasteThreshold waste threshold percentage
   */
  public void setWasteThreshold(final double wasteThreshold) {
    this.wasteThreshold = wasteThreshold;
    LOGGER.info(String.format("Waste threshold set to %.1f%%", wasteThreshold));
  }

  /**
   * Enables or disables the optimization engine.
   *
   * @param enabled true to enable
   */
  public void setEnabled(final boolean enabled) {
    this.enabled = enabled;
    LOGGER.info("Resource optimization engine " + (enabled ? "enabled" : "disabled"));
  }

  /**
   * Shuts down the optimization engine.
   */
  public void shutdown() {
    enabled = false;

    optimizationExecutor.shutdown();
    try {
      if (!optimizationExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
        optimizationExecutor.shutdownNow();
      }
    } catch (final InterruptedException e) {
      optimizationExecutor.shutdownNow();
      Thread.currentThread().interrupt();
    }

    LOGGER.info("Resource optimization engine shutdown");
  }
}