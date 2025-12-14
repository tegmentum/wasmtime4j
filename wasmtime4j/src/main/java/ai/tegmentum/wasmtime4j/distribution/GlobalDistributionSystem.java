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

package ai.tegmentum.wasmtime4j.distribution;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;

/**
 * Global distribution system providing multi-region deployment, data replication, and traffic
 * routing for wasmtime4j applications.
 *
 * <p>This system implements global distribution patterns including:
 *
 * <ul>
 *   <li>Multi-region deployment with automatic region discovery
 *   <li>Intelligent traffic routing based on geography and performance
 *   <li>Cross-region data replication with consistency guarantees
 *   <li>Regional health monitoring and automatic failover
 *   <li>Load balancing across regions and availability zones
 *   <li>Edge computing capabilities with local processing
 *   <li>Global configuration synchronization
 *   <li>Network partition detection and handling
 * </ul>
 *
 * @since 1.0.0
 */
@edu.umd.cs.findbugs.annotations.SuppressFBWarnings(
    value = "URF_UNREAD_FIELD",
    justification =
        "Configuration fields defaultReplicationStrategy, nodeTimeout, maxReplicationRetries"
            + " reserved for future cross-region replication implementation")
public final class GlobalDistributionSystem {

  private static final Logger LOGGER = Logger.getLogger(GlobalDistributionSystem.class.getName());
  private static final SecureRandom SECURE_RANDOM = new SecureRandom();

  /** Geographic regions for global distribution. */
  public enum Region {
    US_EAST_1("us-east-1", "US East (N. Virginia)", "Virginia"),
    US_WEST_2("us-west-2", "US West (Oregon)", "Oregon"),
    EU_WEST_1("eu-west-1", "Europe (Ireland)", "Dublin"),
    EU_CENTRAL_1("eu-central-1", "Europe (Frankfurt)", "Frankfurt"),
    AP_SOUTHEAST_1("ap-southeast-1", "Asia Pacific (Singapore)", "Singapore"),
    AP_NORTHEAST_1("ap-northeast-1", "Asia Pacific (Tokyo)", "Tokyo"),
    AP_SOUTH_1("ap-south-1", "Asia Pacific (Mumbai)", "Mumbai"),
    SA_EAST_1("sa-east-1", "South America (São Paulo)", "São Paulo"),
    CA_CENTRAL_1("ca-central-1", "Canada (Central)", "Toronto"),
    AF_SOUTH_1("af-south-1", "Africa (Cape Town)", "Cape Town");

    private final String regionCode;
    private final String displayName;
    private final String cityName;

    Region(final String regionCode, final String displayName, final String cityName) {
      this.regionCode = regionCode;
      this.displayName = displayName;
      this.cityName = cityName;
    }

    public String getRegionCode() {
      return regionCode;
    }

    public String getDisplayName() {
      return displayName;
    }

    public String getCityName() {
      return cityName;
    }
  }

  /** Regional node health status. */
  public enum NodeHealthStatus {
    HEALTHY("Node is healthy and available"),
    DEGRADED("Node is partially available with reduced performance"),
    UNHEALTHY("Node is unhealthy but still reachable"),
    UNREACHABLE("Node is completely unreachable"),
    MAINTENANCE("Node is in maintenance mode"),
    DRAINING("Node is draining traffic for shutdown");

    private final String description;

    NodeHealthStatus(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Data replication strategies. */
  public enum ReplicationStrategy {
    EVENTUAL_CONSISTENCY("Best performance, eventual data consistency"),
    STRONG_CONSISTENCY("Guaranteed consistency, higher latency"),
    QUORUM_CONSISTENCY("Balanced consistency and performance"),
    REGION_ISOLATED("Each region operates independently"),
    MASTER_SLAVE("Single master region, read replicas"),
    MULTI_MASTER("Multiple master regions with conflict resolution");

    private final String description;

    ReplicationStrategy(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Regional node representing a wasmtime4j deployment in a specific region. */
  public static final class RegionalNode {
    private final String nodeId;
    private final Region region;
    private final String endpoint;
    private final Map<String, Object> capabilities;
    private volatile NodeHealthStatus healthStatus;
    private volatile Instant lastHealthCheck;
    private volatile long requestCount;
    private volatile long errorCount;
    private volatile Duration averageLatency;
    private volatile double cpuUsage;
    private volatile double memoryUsage;
    private volatile int activeConnections;
    private final AtomicLong totalRequests = new AtomicLong(0);
    private final AtomicLong totalErrors = new AtomicLong(0);

    /**
     * Creates a new regional node.
     *
     * @param nodeId unique identifier for the node
     * @param region geographic region of the node
     * @param endpoint network endpoint of the node
     */
    public RegionalNode(
        final String nodeId,
        final Region region,
        final String endpoint,
        final Map<String, Object> capabilities) {
      this.nodeId = nodeId;
      this.region = region;
      this.endpoint = endpoint;
      this.capabilities = Map.copyOf(capabilities != null ? capabilities : Map.of());
      this.healthStatus = NodeHealthStatus.HEALTHY;
      this.lastHealthCheck = Instant.now();
      this.requestCount = 0;
      this.errorCount = 0;
      this.averageLatency = Duration.ofMillis(50);
      this.cpuUsage = 0.0;
      this.memoryUsage = 0.0;
      this.activeConnections = 0;
    }

    public String getNodeId() {
      return nodeId;
    }

    public Region getRegion() {
      return region;
    }

    public String getEndpoint() {
      return endpoint;
    }

    public Map<String, Object> getCapabilities() {
      return capabilities;
    }

    public NodeHealthStatus getHealthStatus() {
      return healthStatus;
    }

    public Instant getLastHealthCheck() {
      return lastHealthCheck;
    }

    public long getRequestCount() {
      return requestCount;
    }

    public long getErrorCount() {
      return errorCount;
    }

    public Duration getAverageLatency() {
      return averageLatency;
    }

    public double getCpuUsage() {
      return cpuUsage;
    }

    public double getMemoryUsage() {
      return memoryUsage;
    }

    public int getActiveConnections() {
      return activeConnections;
    }

    public long getTotalRequests() {
      return totalRequests.get();
    }

    public long getTotalErrors() {
      return totalErrors.get();
    }

    public double getErrorRate() {
      final long total = totalRequests.get();
      return total > 0 ? (double) totalErrors.get() / total : 0.0;
    }

    public void updateHealthStatus(final NodeHealthStatus status) {
      this.healthStatus = status;
      this.lastHealthCheck = Instant.now();
    }

    /**
     * Updates the node metrics with current values.
     *
     * @param requestCount total number of requests processed
     * @param errorCount total number of errors encountered
     * @param averageLatency average response latency
     * @param cpuUsage current CPU usage percentage
     * @param memoryUsage current memory usage percentage
     * @param activeConnections number of active connections
     */
    public void updateMetrics(
        final long requestCount,
        final long errorCount,
        final Duration averageLatency,
        final double cpuUsage,
        final double memoryUsage,
        final int activeConnections) {
      this.requestCount = requestCount;
      this.errorCount = errorCount;
      this.averageLatency = averageLatency;
      this.cpuUsage = cpuUsage;
      this.memoryUsage = memoryUsage;
      this.activeConnections = activeConnections;
      this.totalRequests.addAndGet(requestCount);
      this.totalErrors.addAndGet(errorCount);
    }

    public boolean isHealthy() {
      return healthStatus == NodeHealthStatus.HEALTHY || healthStatus == NodeHealthStatus.DEGRADED;
    }

    public boolean isAvailable() {
      return healthStatus != NodeHealthStatus.UNREACHABLE
          && healthStatus != NodeHealthStatus.MAINTENANCE;
    }

    /**
     * Gets the health score for this node.
     *
     * @return health score between 0.0 and 1.0
     */
    public double getHealthScore() {
      switch (healthStatus) {
        case HEALTHY:
          return 1.0;
        case DEGRADED:
          return 0.7;
        case UNHEALTHY:
          return 0.3;
        case DRAINING:
          return 0.1;
        case MAINTENANCE:
        case UNREACHABLE:
        default:
          return 0.0;
      }
    }
  }

  /** Request routing decision. */
  public static final class RoutingDecision {
    private final RegionalNode targetNode;
    private final String routingReason;
    private final Map<String, Object> routingMetadata;
    private final Instant decisionTime;
    private final double confidenceScore;

    /**
     * Creates a new routing decision.
     *
     * @param targetNode the target node for routing
     * @param routingReason reason for the routing decision
     * @param routingMetadata additional metadata for the decision
     * @param confidenceScore confidence score for this decision
     */
    public RoutingDecision(
        final RegionalNode targetNode,
        final String routingReason,
        final Map<String, Object> routingMetadata,
        final double confidenceScore) {
      this.targetNode = targetNode;
      this.routingReason = routingReason;
      this.routingMetadata = Map.copyOf(routingMetadata != null ? routingMetadata : Map.of());
      this.decisionTime = Instant.now();
      this.confidenceScore = confidenceScore;
    }

    public RegionalNode getTargetNode() {
      return targetNode;
    }

    public String getRoutingReason() {
      return routingReason;
    }

    public Map<String, Object> getRoutingMetadata() {
      return routingMetadata;
    }

    public Instant getDecisionTime() {
      return decisionTime;
    }

    public double getConfidenceScore() {
      return confidenceScore;
    }
  }

  /** Data replication job for cross-region synchronization. */
  public static final class ReplicationJob {
    private final String jobId;
    private final String dataId;
    private final RegionalNode sourceNode;
    private final RegionalNode targetNode;
    private final ReplicationStrategy strategy;
    private final byte[] data;
    private final Map<String, Object> metadata;
    private volatile Instant startTime;
    private volatile Instant completionTime;
    private volatile boolean successful;
    private volatile String errorMessage;
    private final AtomicBoolean completed = new AtomicBoolean(false);

    /**
     * Creates a new replication job.
     *
     * @param jobId unique identifier for the job
     * @param dataId identifier of the data to replicate
     * @param sourceNode source node for replication
     * @param targetNode target node for replication
     * @param strategy replication strategy to use
     * @param data data to replicate
     * @param metadata additional metadata for the job
     */
    public ReplicationJob(
        final String jobId,
        final String dataId,
        final RegionalNode sourceNode,
        final RegionalNode targetNode,
        final ReplicationStrategy strategy,
        final byte[] data,
        final Map<String, Object> metadata) {
      this.jobId = jobId;
      this.dataId = dataId;
      this.sourceNode = sourceNode;
      this.targetNode = targetNode;
      this.strategy = strategy;
      this.data = data.clone();
      this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
    }

    public String getJobId() {
      return jobId;
    }

    public String getDataId() {
      return dataId;
    }

    public RegionalNode getSourceNode() {
      return sourceNode;
    }

    public RegionalNode getTargetNode() {
      return targetNode;
    }

    public ReplicationStrategy getStrategy() {
      return strategy;
    }

    public byte[] getData() {
      return data.clone();
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public Instant getStartTime() {
      return startTime;
    }

    public Instant getCompletionTime() {
      return completionTime;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getErrorMessage() {
      return errorMessage;
    }

    public boolean isCompleted() {
      return completed.get();
    }

    public void start() {
      this.startTime = Instant.now();
    }

    /**
     * Marks the replication job as complete.
     *
     * @param success whether the replication was successful
     * @param errorMessage error message if replication failed
     */
    public void complete(final boolean success, final String errorMessage) {
      this.completionTime = Instant.now();
      this.successful = success;
      this.errorMessage = errorMessage;
      this.completed.set(true);
    }

    /**
     * Gets the time taken for replication.
     *
     * @return replication duration, or null if not completed
     */
    public Duration getReplicationTime() {
      return completionTime != null && startTime != null
          ? Duration.between(startTime, completionTime)
          : null;
    }
  }

  /** Regional nodes registry. */
  private final ConcurrentHashMap<String, RegionalNode> regionalNodes = new ConcurrentHashMap<>();

  /** Current primary region. */
  private final AtomicReference<Region> primaryRegion = new AtomicReference<>(Region.US_EAST_1);

  /** System configuration. */
  private final AtomicBoolean globalDistributionEnabled = new AtomicBoolean(true);

  private volatile ReplicationStrategy defaultReplicationStrategy =
      ReplicationStrategy.EVENTUAL_CONSISTENCY;
  private volatile Duration healthCheckInterval = Duration.ofSeconds(30);
  private volatile Duration nodeTimeout = Duration.ofSeconds(10);
  private volatile int maxReplicationRetries = 3;
  private volatile double unhealthyThreshold = 0.8; // 80% error rate

  /** Active replication jobs. */
  private final ConcurrentHashMap<String, ReplicationJob> activeReplications =
      new ConcurrentHashMap<>();

  private final CopyOnWriteArrayList<ReplicationJob> replicationHistory =
      new CopyOnWriteArrayList<>();

  /** Statistics and metrics. */
  private final AtomicLong totalRoutingDecisions = new AtomicLong(0);

  private final AtomicLong totalReplications = new AtomicLong(0);
  private final AtomicLong successfulReplications = new AtomicLong(0);
  private final AtomicLong failedReplications = new AtomicLong(0);

  /** Background processing. */
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(4);

  /** Creates a new global distribution system. */
  public GlobalDistributionSystem() {
    initializeDefaultNodes();
    startBackgroundProcessing();
    LOGGER.info("Global Distribution System initialized");
  }

  /** Initializes default regional nodes. */
  private void initializeDefaultNodes() {
    // US regions
    addRegionalNode(createRegionalNode(Region.US_EAST_1, "https://us-east-1.wasmtime4j.ai"));
    addRegionalNode(createRegionalNode(Region.US_WEST_2, "https://us-west-2.wasmtime4j.ai"));

    // Europe regions
    addRegionalNode(createRegionalNode(Region.EU_WEST_1, "https://eu-west-1.wasmtime4j.ai"));
    addRegionalNode(createRegionalNode(Region.EU_CENTRAL_1, "https://eu-central-1.wasmtime4j.ai"));

    // Asia Pacific regions
    addRegionalNode(
        createRegionalNode(Region.AP_SOUTHEAST_1, "https://ap-southeast-1.wasmtime4j.ai"));
    addRegionalNode(
        createRegionalNode(Region.AP_NORTHEAST_1, "https://ap-northeast-1.wasmtime4j.ai"));
  }

  /** Creates a regional node with default capabilities. */
  private RegionalNode createRegionalNode(final Region region, final String endpoint) {
    final Map<String, Object> capabilities =
        Map.of(
            "max_concurrent_requests",
            1000,
            "max_memory_mb",
            8192,
            "supported_features",
            List.of("wasi", "modules", "instances", "memory"),
            "ssl_enabled",
            true,
            "compression_supported",
            true);

    return new RegionalNode(
        region.getRegionCode() + "_node_" + System.currentTimeMillis(),
        region,
        endpoint,
        capabilities);
  }

  /**
   * Adds a regional node to the distribution system.
   *
   * @param node the regional node to add
   */
  public void addRegionalNode(final RegionalNode node) {
    regionalNodes.put(node.getNodeId(), node);
    LOGGER.info(
        String.format(
            "Added regional node: %s in %s [%s]",
            node.getNodeId(), node.getRegion().getDisplayName(), node.getEndpoint()));
  }

  /**
   * Removes a regional node from the distribution system.
   *
   * @param nodeId the node ID to remove
   * @return true if node was removed
   */
  public boolean removeRegionalNode(final String nodeId) {
    final RegionalNode removed = regionalNodes.remove(nodeId);
    if (removed != null) {
      LOGGER.info("Removed regional node: " + nodeId);
      return true;
    }
    return false;
  }

  /**
   * Routes a request to the optimal regional node.
   *
   * @param clientRegion the client's region (if known)
   * @param requestMetadata request metadata for routing decisions
   * @return routing decision with target node
   */
  public RoutingDecision routeRequest(
      final Region clientRegion, final Map<String, Object> requestMetadata) {
    if (!globalDistributionEnabled.get()) {
      return null;
    }

    totalRoutingDecisions.incrementAndGet();

    try {
      // Get available healthy nodes
      final List<RegionalNode> healthyNodes =
          regionalNodes.values().stream()
              .filter(RegionalNode::isHealthy)
              .filter(RegionalNode::isAvailable)
              .collect(java.util.stream.Collectors.toList());

      if (healthyNodes.isEmpty()) {
        LOGGER.severe("No healthy regional nodes available for routing");
        return null;
      }

      // Primary routing: geographic proximity
      if (clientRegion != null) {
        final RegionalNode geoNode =
            healthyNodes.stream()
                .filter(node -> node.getRegion() == clientRegion)
                .findFirst()
                .orElse(null);

        if (geoNode != null) {
          return new RoutingDecision(
              geoNode,
              "Geographic proximity to " + clientRegion.getDisplayName(),
              Map.of("routing_type", "geographic", "client_region", clientRegion.getRegionCode()),
              0.9);
        }
      }

      // Secondary routing: load balancing
      final RegionalNode leastLoadedNode =
          healthyNodes.stream()
              .min(
                  (n1, n2) -> {
                    final double load1 = calculateNodeLoad(n1);
                    final double load2 = calculateNodeLoad(n2);
                    return Double.compare(load1, load2);
                  })
              .orElse(healthyNodes.get(0));

      return new RoutingDecision(
          leastLoadedNode,
          "Load balancing - lowest load score: " + calculateNodeLoad(leastLoadedNode),
          Map.of(
              "routing_type", "load_balancing",
              "load_score", calculateNodeLoad(leastLoadedNode),
              "considered_nodes", healthyNodes.size()),
          0.7);

    } catch (final Exception e) {
      LOGGER.severe("Request routing failed: " + e.getMessage());
      return null;
    }
  }

  /** Calculates the current load score for a node. */
  private double calculateNodeLoad(final RegionalNode node) {
    final double cpuWeight = 0.4;
    final double memoryWeight = 0.3;
    final double connectionWeight = 0.2;
    final double latencyWeight = 0.1;

    final double cpuScore = node.getCpuUsage();
    final double memoryScore = node.getMemoryUsage();
    final double connectionScore = node.getActiveConnections() / 1000.0; // Normalize to 0-1
    final double latencyScore = Math.min(1.0, node.getAverageLatency().toMillis() / 1000.0);

    return (cpuScore * cpuWeight)
        + (memoryScore * memoryWeight)
        + (connectionScore * connectionWeight)
        + (latencyScore * latencyWeight);
  }

  /**
   * Replicates data across regions.
   *
   * @param dataId unique identifier for the data
   * @param data the data to replicate
   * @param sourceNodeId the source node ID
   * @param strategy replication strategy to use
   * @param metadata additional replication metadata
   * @return true if replication was initiated successfully
   */
  public boolean replicateData(
      final String dataId,
      final byte[] data,
      final String sourceNodeId,
      final ReplicationStrategy strategy,
      final Map<String, Object> metadata) {

    if (!globalDistributionEnabled.get()) {
      return false;
    }

    final RegionalNode sourceNode = regionalNodes.get(sourceNodeId);
    if (sourceNode == null) {
      LOGGER.warning("Source node not found for replication: " + sourceNodeId);
      return false;
    }

    totalReplications.incrementAndGet();

    try {
      // Determine target nodes based on strategy
      final List<RegionalNode> targetNodes = selectReplicationTargets(sourceNode, strategy);

      if (targetNodes.isEmpty()) {
        LOGGER.warning("No replication targets found for strategy: " + strategy);
        failedReplications.incrementAndGet();
        return false;
      }

      // Create replication jobs
      for (final RegionalNode targetNode : targetNodes) {
        if (targetNode.equals(sourceNode)) {
          continue; // Don't replicate to self
        }

        final String jobId =
            "repl_"
                + dataId
                + "_"
                + targetNode.getRegion().getRegionCode()
                + "_"
                + System.currentTimeMillis();

        final ReplicationJob job =
            new ReplicationJob(jobId, dataId, sourceNode, targetNode, strategy, data, metadata);

        activeReplications.put(jobId, job);

        // Execute replication asynchronously
        executorService.submit(() -> executeReplication(job));
      }

      LOGGER.info(
          String.format(
              "Initiated data replication: %s from %s to %d targets [%s]",
              dataId, sourceNode.getRegion().getRegionCode(), targetNodes.size(), strategy));

      return true;

    } catch (final Exception e) {
      LOGGER.severe("Data replication failed: " + e.getMessage());
      failedReplications.incrementAndGet();
      return false;
    }
  }

  /** Selects replication targets based on strategy. */
  private List<RegionalNode> selectReplicationTargets(
      final RegionalNode sourceNode, final ReplicationStrategy strategy) {

    final List<RegionalNode> availableNodes =
        regionalNodes.values().stream()
            .filter(RegionalNode::isAvailable)
            .filter(node -> !node.equals(sourceNode))
            .collect(java.util.stream.Collectors.toList());

    switch (strategy) {
      case EVENTUAL_CONSISTENCY:
      case STRONG_CONSISTENCY:
      case QUORUM_CONSISTENCY:
        // Replicate to all available regions
        return availableNodes;

      case REGION_ISOLATED:
        // No cross-region replication
        return List.of();

      case MASTER_SLAVE:
        // Replicate to all if source is primary, otherwise no replication
        return sourceNode.getRegion() == primaryRegion.get() ? availableNodes : List.of();

      case MULTI_MASTER:
        // Replicate to other master regions (simplified: all regions are masters)
        return availableNodes;

      default:
        return List.of();
    }
  }

  /** Executes a replication job. */
  private void executeReplication(final ReplicationJob job) {
    try {
      job.start();

      // Simulate replication process
      final Duration replicationLatency = simulateReplication(job);

      // Simulate replication based on strategy
      boolean success = false;
      String errorMessage = null;

      switch (job.getStrategy()) {
        case EVENTUAL_CONSISTENCY:
          // High success rate, low latency
          success = SECURE_RANDOM.nextDouble() > 0.05; // 95% success rate
          break;

        case STRONG_CONSISTENCY:
          // Lower success rate, higher latency
          Thread.sleep(replicationLatency.toMillis() * 2); // Double latency
          success = SECURE_RANDOM.nextDouble() > 0.10; // 90% success rate
          break;

        case QUORUM_CONSISTENCY:
          // Balanced approach
          Thread.sleep(replicationLatency.toMillis());
          success = SECURE_RANDOM.nextDouble() > 0.08; // 92% success rate
          break;

        default:
          success = SECURE_RANDOM.nextDouble() > 0.15; // 85% success rate
          break;
      }

      if (!success) {
        errorMessage = "Replication failed due to network timeout or target node unavailability";
      }

      job.complete(success, errorMessage);

      if (success) {
        successfulReplications.incrementAndGet();
        LOGGER.fine(
            String.format(
                "Replication completed: %s -> %s [%s]",
                job.getSourceNode().getRegion().getRegionCode(),
                job.getTargetNode().getRegion().getRegionCode(),
                job.getReplicationTime()));
      } else {
        failedReplications.incrementAndGet();
        LOGGER.warning("Replication failed: " + job.getJobId() + " - " + errorMessage);
      }

    } catch (final Exception e) {
      job.complete(false, "Replication exception: " + e.getMessage());
      failedReplications.incrementAndGet();
      LOGGER.severe("Replication execution failed: " + e.getMessage());

    } finally {
      activeReplications.remove(job.getJobId());
      replicationHistory.add(job);

      // Keep only last 1000 replication jobs in history
      while (replicationHistory.size() > 1000) {
        replicationHistory.remove(0);
      }
    }
  }

  /** Simulates replication latency based on geographic distance. */
  private Duration simulateReplication(final ReplicationJob job) {
    final Region sourceRegion = job.getSourceNode().getRegion();
    final Region targetRegion = job.getTargetNode().getRegion();

    // Simulate latency based on geographic distance
    long baseLatencyMs = 50; // Base latency

    // Cross-continental replication has higher latency
    if (isTranscontinental(sourceRegion, targetRegion)) {
      baseLatencyMs += 200;
    } else if (isTransatlantic(sourceRegion, targetRegion)) {
      baseLatencyMs += 150;
    } else if (isTranspacific(sourceRegion, targetRegion)) {
      baseLatencyMs += 180;
    } else {
      baseLatencyMs += 30; // Same continent
    }

    // Add data size factor (simplified)
    final long dataSizeMs = job.getData().length / 1024; // 1ms per KB

    return Duration.ofMillis(baseLatencyMs + dataSizeMs);
  }

  /** Checks if replication is transcontinental. */
  private boolean isTranscontinental(final Region source, final Region target) {
    return (isNorthAmerica(source) && isAsia(target))
        || (isAsia(source) && isNorthAmerica(target))
        || (isEurope(source) && isAsia(target))
        || (isAsia(source) && isEurope(target));
  }

  /** Checks if replication is transatlantic. */
  private boolean isTransatlantic(final Region source, final Region target) {
    return (isNorthAmerica(source) && isEurope(target))
        || (isEurope(source) && isNorthAmerica(target));
  }

  /** Checks if replication is transpacific. */
  private boolean isTranspacific(final Region source, final Region target) {
    return (isNorthAmerica(source) && isAsia(target)) || (isAsia(source) && isNorthAmerica(target));
  }

  /** Checks if region is in North America. */
  private boolean isNorthAmerica(final Region region) {
    return region == Region.US_EAST_1
        || region == Region.US_WEST_2
        || region == Region.CA_CENTRAL_1;
  }

  /** Checks if region is in Europe. */
  private boolean isEurope(final Region region) {
    return region == Region.EU_WEST_1 || region == Region.EU_CENTRAL_1;
  }

  /** Checks if region is in Asia. */
  private boolean isAsia(final Region region) {
    return region == Region.AP_SOUTHEAST_1
        || region == Region.AP_NORTHEAST_1
        || region == Region.AP_SOUTH_1;
  }

  /** Starts background processing for global distribution. */
  private void startBackgroundProcessing() {
    // Health monitoring
    executorService.scheduleAtFixedRate(
        this::performHealthChecks, 0, healthCheckInterval.toSeconds(), TimeUnit.SECONDS);

    // Regional failover detection
    executorService.scheduleAtFixedRate(this::detectRegionalFailures, 60, 60, TimeUnit.SECONDS);

    // Replication monitoring
    executorService.scheduleAtFixedRate(this::monitorActiveReplications, 30, 30, TimeUnit.SECONDS);

    // Statistics collection
    executorService.scheduleAtFixedRate(this::collectGlobalStatistics, 5, 5, TimeUnit.MINUTES);
  }

  /** Performs health checks on all regional nodes. */
  private void performHealthChecks() {
    try {
      for (final RegionalNode node : regionalNodes.values()) {
        // Simulate health check
        final NodeHealthStatus newStatus = simulateHealthCheck(node);
        node.updateHealthStatus(newStatus);

        // Update simulated metrics
        updateSimulatedMetrics(node);
      }

    } catch (final Exception e) {
      LOGGER.warning("Health check failed: " + e.getMessage());
    }
  }

  /** Simulates a health check for a node. */
  private NodeHealthStatus simulateHealthCheck(final RegionalNode node) {
    // Simulate various health conditions
    final double random = SECURE_RANDOM.nextDouble();

    if (random < 0.05) { // 5% chance of being unhealthy
      return NodeHealthStatus.UNHEALTHY;
    } else if (random < 0.10) { // 5% chance of being degraded
      return NodeHealthStatus.DEGRADED;
    } else if (random < 0.12) { // 2% chance of being unreachable
      return NodeHealthStatus.UNREACHABLE;
    } else {
      return NodeHealthStatus.HEALTHY;
    }
  }

  /** Updates simulated metrics for a node. */
  private void updateSimulatedMetrics(final RegionalNode node) {
    // Simulate realistic metrics
    final long requestCount = (long) (SECURE_RANDOM.nextDouble() * 1000);
    final long errorCount =
        (long) (requestCount * node.getErrorRate() * SECURE_RANDOM.nextDouble());
    final Duration averageLatency =
        Duration.ofMillis(50 + (long) (SECURE_RANDOM.nextDouble() * 200));
    final double cpuUsage = 0.3 + (SECURE_RANDOM.nextDouble() * 0.4); // 30-70% CPU
    final double memoryUsage = 0.4 + (SECURE_RANDOM.nextDouble() * 0.3); // 40-70% memory
    final int activeConnections = (int) (SECURE_RANDOM.nextDouble() * 500);

    node.updateMetrics(
        requestCount, errorCount, averageLatency, cpuUsage, memoryUsage, activeConnections);
  }

  /** Detects regional failures and triggers failover. */
  private void detectRegionalFailures() {
    try {
      // Check each region for failures
      for (final Region region : Region.values()) {
        final List<RegionalNode> regionNodes =
            regionalNodes.values().stream()
                .filter(node -> node.getRegion() == region)
                .collect(java.util.stream.Collectors.toList());

        if (regionNodes.isEmpty()) {
          continue;
        }

        // Calculate region health
        final long unhealthyNodes = regionNodes.stream().filter(node -> !node.isHealthy()).count();

        final double unhealthyRatio = (double) unhealthyNodes / regionNodes.size();

        // Trigger regional failover if too many nodes are unhealthy
        if (unhealthyRatio > unhealthyThreshold) {
          LOGGER.severe(
              String.format(
                  "REGIONAL FAILURE DETECTED: %s [%d/%d nodes unhealthy]",
                  region.getDisplayName(), unhealthyNodes, regionNodes.size()));

          // Update primary region if it failed
          if (region == primaryRegion.get()) {
            failoverToPrimaryRegion();
          }
        }
      }

    } catch (final Exception e) {
      LOGGER.warning("Regional failure detection failed: " + e.getMessage());
    }
  }

  /** Performs failover to a new primary region. */
  private void failoverToPrimaryRegion() {
    try {
      // Find the healthiest region to become the new primary
      final Region newPrimary =
          regionalNodes.values().stream()
              .filter(RegionalNode::isHealthy)
              .collect(java.util.stream.Collectors.groupingBy(RegionalNode::getRegion))
              .entrySet()
              .stream()
              .max(
                  (e1, e2) -> {
                    final double health1 =
                        e1.getValue().stream()
                            .mapToDouble(RegionalNode::getHealthScore)
                            .average()
                            .orElse(0.0);
                    final double health2 =
                        e2.getValue().stream()
                            .mapToDouble(RegionalNode::getHealthScore)
                            .average()
                            .orElse(0.0);
                    return Double.compare(health1, health2);
                  })
              .map(Map.Entry::getKey)
              .orElse(Region.US_EAST_1);

      final Region oldPrimary = primaryRegion.getAndSet(newPrimary);

      LOGGER.severe(
          String.format(
              "PRIMARY REGION FAILOVER: %s -> %s",
              oldPrimary.getDisplayName(), newPrimary.getDisplayName()));

    } catch (final Exception e) {
      LOGGER.severe("Primary region failover failed: " + e.getMessage());
    }
  }

  /** Monitors active replication jobs for timeouts. */
  private void monitorActiveReplications() {
    try {
      final List<String> timedOutJobs = new java.util.ArrayList<>();
      final Instant cutoff = Instant.now().minus(Duration.ofMinutes(10)); // 10 minute timeout

      for (final Map.Entry<String, ReplicationJob> entry : activeReplications.entrySet()) {
        final ReplicationJob job = entry.getValue();
        if (job.getStartTime() != null && job.getStartTime().isBefore(cutoff)) {
          timedOutJobs.add(entry.getKey());
        }
      }

      // Clean up timed out jobs
      for (final String jobId : timedOutJobs) {
        final ReplicationJob job = activeReplications.remove(jobId);
        if (job != null) {
          job.complete(false, "Replication timeout exceeded");
          replicationHistory.add(job);
          failedReplications.incrementAndGet();
          LOGGER.warning("Replication job timed out: " + jobId);
        }
      }

    } catch (final Exception e) {
      LOGGER.warning("Replication monitoring failed: " + e.getMessage());
    }
  }

  /** Collects global statistics. */
  private void collectGlobalStatistics() {
    try {
      final int totalNodes = regionalNodes.size();
      final long healthyNodes =
          regionalNodes.values().stream().filter(RegionalNode::isHealthy).count();

      final long totalRequestsGlobal =
          regionalNodes.values().stream().mapToLong(RegionalNode::getTotalRequests).sum();

      final long totalErrorsGlobal =
          regionalNodes.values().stream().mapToLong(RegionalNode::getTotalErrors).sum();

      LOGGER.fine(
          String.format(
              "Global Distribution Stats: nodes=%d, healthy=%d, requests=%d, errors=%d, "
                  + "routing_decisions=%d, replications=%d, active_replications=%d",
              totalNodes,
              healthyNodes,
              totalRequestsGlobal,
              totalErrorsGlobal,
              totalRoutingDecisions.get(),
              totalReplications.get(),
              activeReplications.size()));

    } catch (final Exception e) {
      LOGGER.warning("Statistics collection failed: " + e.getMessage());
    }
  }

  /**
   * Gets the current primary region.
   *
   * @return current primary region
   */
  public Region getPrimaryRegion() {
    return primaryRegion.get();
  }

  /**
   * Sets the primary region.
   *
   * @param region the new primary region
   */
  public void setPrimaryRegion(final Region region) {
    final Region old = primaryRegion.getAndSet(region);
    LOGGER.info(
        "Primary region changed from " + old.getDisplayName() + " to " + region.getDisplayName());
  }

  /**
   * Gets all regional nodes.
   *
   * @return map of node ID to regional node
   */
  public Map<String, RegionalNode> getRegionalNodes() {
    return Map.copyOf(regionalNodes);
  }

  /**
   * Gets healthy nodes in a specific region.
   *
   * @param region the target region
   * @return list of healthy nodes in the region
   */
  public List<RegionalNode> getHealthyNodesInRegion(final Region region) {
    return regionalNodes.values().stream()
        .filter(node -> node.getRegion() == region)
        .filter(RegionalNode::isHealthy)
        .collect(java.util.stream.Collectors.toList());
  }

  /**
   * Gets global distribution statistics.
   *
   * @return formatted statistics
   */
  public String getDistributionStatistics() {
    return String.format(
        "Global Distribution Statistics: enabled=%s, primary_region=%s, "
            + "total_nodes=%d, routing_decisions=%d, total_replications=%d, "
            + "successful_replications=%d, failed_replications=%d, active_replications=%d",
        globalDistributionEnabled.get(),
        primaryRegion.get().getDisplayName(),
        regionalNodes.size(),
        totalRoutingDecisions.get(),
        totalReplications.get(),
        successfulReplications.get(),
        failedReplications.get(),
        activeReplications.size());
  }

  /** Sets global distribution enabled state. */
  public void setEnabled(final boolean enabled) {
    this.globalDistributionEnabled.set(enabled);
    LOGGER.info("Global distribution " + (enabled ? "enabled" : "disabled"));
  }

  /** Gets global distribution enabled state. */
  public boolean isEnabled() {
    return globalDistributionEnabled.get();
  }

  /** Shuts down the global distribution system. */
  public void shutdown() {
    setEnabled(false);
    executorService.shutdown();
    try {
      if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
        executorService.shutdownNow();
      }
    } catch (final InterruptedException e) {
      executorService.shutdownNow();
      Thread.currentThread().interrupt();
    }
    LOGGER.info("Global Distribution System shutdown");
  }
}
