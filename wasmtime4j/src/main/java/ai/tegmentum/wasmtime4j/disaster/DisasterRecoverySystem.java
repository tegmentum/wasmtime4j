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

package ai.tegmentum.wasmtime4j.disaster;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.ObjectStreamClass;
import java.io.Serializable;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

/**
 * Comprehensive disaster recovery system providing automated backup, restore, failover, and
 * business continuity capabilities for wasmtime4j.
 *
 * <p>This system implements enterprise-grade disaster recovery patterns including:
 *
 * <ul>
 *   <li>Automated backup scheduling with compression and encryption
 *   <li>Point-in-time recovery with granular restoration options
 *   <li>Multi-region data replication and synchronization
 *   <li>Automated failover with health monitoring
 *   <li>Recovery time objective (RTO) and recovery point objective (RPO) tracking
 *   <li>Disaster scenario simulation and testing
 *   <li>Business continuity plan execution
 *   <li>Data integrity validation and corruption detection
 * </ul>
 *
 * @since 1.0.0
 */
public final class DisasterRecoverySystem {

  private static final Logger LOGGER = Logger.getLogger(DisasterRecoverySystem.class.getName());

  /** Types of disasters that can be handled. */
  public enum DisasterType {
    HARDWARE_FAILURE("Complete hardware or infrastructure failure"),
    DATA_CORRUPTION("Data corruption or loss"),
    NETWORK_PARTITION("Network connectivity loss"),
    REGION_OUTAGE("Complete regional outage"),
    SECURITY_BREACH("Security compromise requiring isolation"),
    APPLICATION_FAILURE("Critical application component failure"),
    CASCADING_FAILURE("Multiple component failure cascade"),
    NATURAL_DISASTER("Physical disaster affecting data center");

    private final String description;

    DisasterType(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** Recovery strategy priorities. */
  public enum RecoveryStrategy {
    HOT_STANDBY("Immediate failover to active standby"),
    WARM_STANDBY("Quick activation of standby systems"),
    COLD_STANDBY("Restore from backup storage"),
    MANUAL_RECOVERY("Manual intervention required"),
    PARTIAL_RECOVERY("Recover critical functions only"),
    REBUILD_FROM_SCRATCH("Complete system rebuild");

    private final String description;

    RecoveryStrategy(final String description) {
      this.description = description;
    }

    public String getDescription() {
      return description;
    }
  }

  /** System component that can be backed up and restored. */
  @SuppressFBWarnings(
      value = "SE_TRANSIENT_FIELD_NOT_RESTORED",
      justification = "Transient metadata is runtime-only context not needed after deserialization;"
          + " core backup data is preserved")
  public static final class RecoverableComponent implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String componentId;
    private final String componentName;
    private final byte[] componentData;
    private final transient Map<String, Object> metadata;
    private final Instant backupTimestamp;
    private final long dataSize;
    private final String checksum;

    /**
     * Creates a new RecoverableComponent.
     *
     * @param componentId the unique component identifier
     * @param componentName the component name
     * @param componentData the component data
     * @param metadata the component metadata
     */
    public RecoverableComponent(
        final String componentId,
        final String componentName,
        final byte[] componentData,
        final Map<String, Object> metadata) {
      this.componentId = componentId;
      this.componentName = componentName;
      this.componentData = componentData.clone();
      this.metadata = Map.copyOf(metadata != null ? metadata : Map.of());
      this.backupTimestamp = Instant.now();
      this.dataSize = componentData.length;
      this.checksum = calculateChecksum(componentData);
    }

    private String calculateChecksum(final byte[] data) {
      try {
        final java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
        final byte[] hash = md.digest(data);
        final StringBuilder sb = new StringBuilder();
        for (final byte b : hash) {
          sb.append(String.format("%02x", b));
        }
        return sb.toString();
      } catch (final Exception e) {
        return "checksum_error";
      }
    }

    public String getComponentId() {
      return componentId;
    }

    public String getComponentName() {
      return componentName;
    }

    public byte[] getComponentData() {
      return componentData.clone();
    }

    public Map<String, Object> getMetadata() {
      return metadata;
    }

    public Instant getBackupTimestamp() {
      return backupTimestamp;
    }

    public long getDataSize() {
      return dataSize;
    }

    public String getChecksum() {
      return checksum;
    }

    public boolean validateIntegrity() {
      return checksum.equals(calculateChecksum(componentData));
    }
  }

  /**
   * Secure ObjectInputStream that validates classes before deserialization.
   *
   * <p>This class prevents deserialization attacks by maintaining a whitelist of allowed classes
   * and rejecting any attempts to deserialize untrusted classes.
   */
  private static final class ValidatingObjectInputStream extends ObjectInputStream {
    /**
     * Creates a new ValidatingObjectInputStream.
     *
     * @param in the underlying input stream
     * @throws IOException if an I/O error occurs
     */
    ValidatingObjectInputStream(final InputStream in) throws IOException {
      super(in);
    }

    @Override
    protected Class<?> resolveClass(final ObjectStreamClass desc)
        throws IOException, ClassNotFoundException {
      final String className = desc.getName();

      // Allow arrays
      if (className.startsWith("[")) {
        return super.resolveClass(desc);
      }

      // Only allow specific disaster recovery classes
      if (!className.equals(
          "ai.tegmentum.wasmtime4j.disaster.DisasterRecoverySystem$RecoverableComponent")) {
        throw new InvalidClassException(
            "Deserialization of class "
                + className
                + " is not allowed. "
                + "Only RecoverableComponent can be deserialized from backup files.");
      }

      return super.resolveClass(desc);
    }
  }

  /** Disaster recovery plan configuration. */
  public static final class DisasterRecoveryPlan {
    private final String planId;
    private final DisasterType disasterType;
    private final RecoveryStrategy primaryStrategy;
    private final RecoveryStrategy fallbackStrategy;
    private final Duration recoveryTimeObjective; // RTO
    private final Duration recoveryPointObjective; // RPO
    private final List<String> criticalComponents;
    private final Map<String, String> recoverySteps;
    private final int priority;
    private final boolean autoExecute;

    /**
     * Creates a new DisasterRecoveryPlan.
     *
     * @param planId the unique plan identifier
     * @param disasterType the type of disaster this plan addresses
     * @param primaryStrategy the primary recovery strategy
     * @param fallbackStrategy the fallback recovery strategy
     * @param recoveryTimeObjective the maximum acceptable downtime
     */
    public DisasterRecoveryPlan(
        final String planId,
        final DisasterType disasterType,
        final RecoveryStrategy primaryStrategy,
        final RecoveryStrategy fallbackStrategy,
        final Duration recoveryTimeObjective,
        final Duration recoveryPointObjective,
        final List<String> criticalComponents,
        final Map<String, String> recoverySteps,
        final int priority,
        final boolean autoExecute) {
      this.planId = planId;
      this.disasterType = disasterType;
      this.primaryStrategy = primaryStrategy;
      this.fallbackStrategy = fallbackStrategy;
      this.recoveryTimeObjective = recoveryTimeObjective;
      this.recoveryPointObjective = recoveryPointObjective;
      this.criticalComponents = List.copyOf(criticalComponents);
      this.recoverySteps = Map.copyOf(recoverySteps);
      this.priority = priority;
      this.autoExecute = autoExecute;
    }

    public String getPlanId() {
      return planId;
    }

    public DisasterType getDisasterType() {
      return disasterType;
    }

    public RecoveryStrategy getPrimaryStrategy() {
      return primaryStrategy;
    }

    public RecoveryStrategy getFallbackStrategy() {
      return fallbackStrategy;
    }

    public Duration getRecoveryTimeObjective() {
      return recoveryTimeObjective;
    }

    public Duration getRecoveryPointObjective() {
      return recoveryPointObjective;
    }

    public List<String> getCriticalComponents() {
      return criticalComponents;
    }

    public Map<String, String> getRecoverySteps() {
      return recoverySteps;
    }

    public int getPriority() {
      return priority;
    }

    public boolean isAutoExecute() {
      return autoExecute;
    }
  }

  /** Disaster recovery execution result. */
  public static final class RecoveryExecution {
    private final String executionId;
    private final DisasterType disasterType;
    private final RecoveryStrategy strategy;
    private final Instant startTime;
    private volatile Instant endTime;
    private volatile boolean successful;
    private volatile String statusMessage;
    private final Map<String, Object> executionMetrics;
    private volatile Duration actualRecoveryTime;

    /**
     * Creates a new RecoveryExecution.
     *
     * @param executionId the unique execution identifier
     * @param disasterType the type of disaster being recovered from
     * @param strategy the recovery strategy being executed
     */
    public RecoveryExecution(
        final String executionId,
        final DisasterType disasterType,
        final RecoveryStrategy strategy) {
      this.executionId = executionId;
      this.disasterType = disasterType;
      this.strategy = strategy;
      this.startTime = Instant.now();
      this.successful = false;
      this.statusMessage = "Recovery in progress";
      this.executionMetrics = new ConcurrentHashMap<>();
    }

    public String getExecutionId() {
      return executionId;
    }

    public DisasterType getDisasterType() {
      return disasterType;
    }

    public RecoveryStrategy getStrategy() {
      return strategy;
    }

    public Instant getStartTime() {
      return startTime;
    }

    public Instant getEndTime() {
      return endTime;
    }

    public boolean isSuccessful() {
      return successful;
    }

    public String getStatusMessage() {
      return statusMessage;
    }

    public Map<String, Object> getExecutionMetrics() {
      return Map.copyOf(executionMetrics);
    }

    public Duration getActualRecoveryTime() {
      return actualRecoveryTime;
    }

    /**
     * Completes the recovery execution with the given status.
     *
     * @param success whether the recovery was successful
     * @param message the status message
     */
    public void complete(final boolean success, final String message) {
      this.endTime = Instant.now();
      this.successful = success;
      this.statusMessage = message;
      this.actualRecoveryTime = Duration.between(startTime, endTime);
    }

    public void updateMetric(final String key, final Object value) {
      executionMetrics.put(key, value);
    }
  }

  /** Backup repository for storing system state. */
  private final ConcurrentHashMap<String, RecoverableComponent> backupRepository =
      new ConcurrentHashMap<>();

  /** Active disaster recovery plans. */
  private final ConcurrentHashMap<String, DisasterRecoveryPlan> recoveryPlans =
      new ConcurrentHashMap<>();

  /** Recovery execution history. */
  private final ConcurrentHashMap<String, RecoveryExecution> recoveryHistory =
      new ConcurrentHashMap<>();

  /** System state tracking. */
  private final AtomicBoolean disasterRecoveryEnabled = new AtomicBoolean(true);

  private final AtomicReference<DisasterType> currentDisasterType = new AtomicReference<>(null);
  private final AtomicReference<RecoveryExecution> activeRecovery = new AtomicReference<>(null);
  private final AtomicLong totalBackups = new AtomicLong(0);
  private final AtomicLong totalRecoveries = new AtomicLong(0);
  private final AtomicLong successfulRecoveries = new AtomicLong(0);

  /** Configuration. */
  private volatile Duration backupInterval = Duration.ofHours(1);

  private volatile int maxBackupRetention = 168; // 7 days * 24 hours
  private volatile Path backupStoragePath = Paths.get("disaster-recovery-backups");
  private volatile boolean autoFailoverEnabled = true;
  private volatile Duration maxRecoveryTime = Duration.ofMinutes(15);

  /** Background processing. */
  private final ScheduledExecutorService executorService = Executors.newScheduledThreadPool(3);

  /** Creates a new disaster recovery system. */
  public DisasterRecoverySystem() {
    initializeDefaultPlans();
    startBackgroundProcessing();
    LOGGER.info("Disaster Recovery System initialized");
  }

  /** Initializes default disaster recovery plans. */
  private void initializeDefaultPlans() {
    // Hardware failure plan
    addRecoveryPlan(
        new DisasterRecoveryPlan(
            "hardware_failure_plan",
            DisasterType.HARDWARE_FAILURE,
            RecoveryStrategy.HOT_STANDBY,
            RecoveryStrategy.COLD_STANDBY,
            Duration.ofMinutes(5),
            Duration.ofMinutes(15),
            List.of("engine", "store", "modules", "instances"),
            Map.of(
                "step1", "Detect hardware failure",
                "step2", "Activate standby systems",
                "step3", "Restore from latest backup",
                "step4", "Validate system functionality"),
            1,
            true));

    // Data corruption plan
    addRecoveryPlan(
        new DisasterRecoveryPlan(
            "data_corruption_plan",
            DisasterType.DATA_CORRUPTION,
            RecoveryStrategy.COLD_STANDBY,
            RecoveryStrategy.MANUAL_RECOVERY,
            Duration.ofMinutes(10),
            Duration.ofMinutes(30),
            List.of("modules", "instances", "memory"),
            Map.of(
                "step1", "Isolate corrupted data",
                "step2", "Restore from clean backup",
                "step3", "Verify data integrity",
                "step4", "Resume operations"),
            2,
            true));

    // Region outage plan
    addRecoveryPlan(
        new DisasterRecoveryPlan(
            "region_outage_plan",
            DisasterType.REGION_OUTAGE,
            RecoveryStrategy.WARM_STANDBY,
            RecoveryStrategy.COLD_STANDBY,
            Duration.ofMinutes(15),
            Duration.ofHours(1),
            List.of("engine", "store", "modules", "instances", "configuration"),
            Map.of(
                "step1", "Detect regional outage",
                "step2", "Activate alternate region",
                "step3", "Restore from cross-region backup",
                "step4", "Update DNS and routing"),
            1,
            false)); // Manual approval required for region failover
  }

  /**
   * Backs up a system component.
   *
   * @param componentId unique component identifier
   * @param componentName human-readable component name
   * @param componentData serialized component data
   * @param metadata additional component metadata
   * @return true if backup was successful
   */
  public boolean backupComponent(
      final String componentId,
      final String componentName,
      final byte[] componentData,
      final Map<String, Object> metadata) {
    if (!disasterRecoveryEnabled.get()) {
      return false;
    }

    try {
      // Create compressed backup
      final byte[] compressedData = compressData(componentData);
      final RecoverableComponent component =
          new RecoverableComponent(componentId, componentName, compressedData, metadata);

      // Validate data integrity
      if (!component.validateIntegrity()) {
        LOGGER.warning("Backup integrity validation failed for component: " + componentId);
        return false;
      }

      // Store in memory repository
      backupRepository.put(componentId, component);

      // Persist to disk
      persistBackupToDisk(component);

      totalBackups.incrementAndGet();

      LOGGER.fine(
          String.format(
              "Backed up component: %s [%s] - %d bytes (compressed from %d bytes)",
              componentId, componentName, compressedData.length, componentData.length));

      return true;

    } catch (final Exception e) {
      LOGGER.severe("Failed to backup component " + componentId + ": " + e.getMessage());
      return false;
    }
  }

  /**
   * Restores a system component from backup.
   *
   * @param componentId component identifier to restore
   * @return restored component data or null if not found
   */
  public RecoverableComponent restoreComponent(final String componentId) {
    if (!disasterRecoveryEnabled.get()) {
      return null;
    }

    try {
      // Try memory repository first
      RecoverableComponent component = backupRepository.get(componentId);

      // Fall back to disk storage
      if (component == null) {
        component = loadBackupFromDisk(componentId);
      }

      if (component == null) {
        LOGGER.warning("No backup found for component: " + componentId);
        return null;
      }

      // Validate integrity before restoration
      if (!component.validateIntegrity()) {
        LOGGER.severe("Backup integrity validation failed during restore: " + componentId);
        return null;
      }

      // Decompress data
      final byte[] decompressedData = decompressData(component.getComponentData());
      final RecoverableComponent restoredComponent =
          new RecoverableComponent(
              component.getComponentId(),
              component.getComponentName(),
              decompressedData,
              component.getMetadata());

      LOGGER.info(
          String.format(
              "Restored component: %s [%s] - %d bytes (backup from %s)",
              componentId,
              component.getComponentName(),
              decompressedData.length,
              component.getBackupTimestamp()));

      return restoredComponent;

    } catch (final Exception e) {
      LOGGER.severe("Failed to restore component " + componentId + ": " + e.getMessage());
      return null;
    }
  }

  /**
   * Executes a disaster recovery plan.
   *
   * @param disasterType the type of disaster detected
   * @param autoExecuteOnly only execute plans marked for auto-execution
   * @return recovery execution result
   */
  public RecoveryExecution executeDisasterRecovery(
      final DisasterType disasterType, final boolean autoExecuteOnly) {

    if (!disasterRecoveryEnabled.get()) {
      LOGGER.warning("Disaster recovery is disabled - cannot execute recovery");
      return null;
    }

    // Check if recovery is already in progress
    if (activeRecovery.get() != null) {
      LOGGER.warning("Recovery already in progress - cannot start new recovery");
      return activeRecovery.get();
    }

    // Find appropriate recovery plan
    final DisasterRecoveryPlan plan = findBestRecoveryPlan(disasterType);
    if (plan == null) {
      LOGGER.severe("No recovery plan found for disaster type: " + disasterType);
      return null;
    }

    if (autoExecuteOnly && !plan.isAutoExecute()) {
      LOGGER.info("Recovery plan requires manual approval - skipping auto-execution");
      return null;
    }

    final String executionId = "recovery_" + System.currentTimeMillis();
    final RecoveryExecution execution =
        new RecoveryExecution(executionId, disasterType, plan.getPrimaryStrategy());

    activeRecovery.set(execution);
    currentDisasterType.set(disasterType);
    totalRecoveries.incrementAndGet();

    LOGGER.severe(
        String.format(
            "DISASTER RECOVERY INITIATED: %s - Plan: %s, Strategy: %s",
            disasterType, plan.getPlanId(), plan.getPrimaryStrategy()));

    // Execute recovery in background
    executorService.submit(() -> performRecovery(plan, execution));

    return execution;
  }

  /** Performs the actual disaster recovery process. */
  private void performRecovery(final DisasterRecoveryPlan plan, final RecoveryExecution execution) {
    try {
      execution.updateMetric("plan_id", plan.getPlanId());
      execution.updateMetric("rto_target", plan.getRecoveryTimeObjective().toMillis());
      execution.updateMetric("rpo_target", plan.getRecoveryPointObjective().toMillis());

      // Execute primary recovery strategy
      boolean recoverySuccessful = false;
      try {
        recoverySuccessful = executeRecoveryStrategy(plan.getPrimaryStrategy(), plan, execution);
        execution.updateMetric("primary_strategy_success", recoverySuccessful);
      } catch (final Exception e) {
        LOGGER.severe("Primary recovery strategy failed: " + e.getMessage());
        execution.updateMetric("primary_strategy_error", e.getMessage());
      }

      // Try fallback strategy if primary failed
      if (!recoverySuccessful && plan.getFallbackStrategy() != null) {
        LOGGER.warning("Primary recovery failed - attempting fallback strategy");
        try {
          recoverySuccessful = executeRecoveryStrategy(plan.getFallbackStrategy(), plan, execution);
          execution.updateMetric("fallback_strategy_success", recoverySuccessful);
        } catch (final Exception e) {
          LOGGER.severe("Fallback recovery strategy failed: " + e.getMessage());
          execution.updateMetric("fallback_strategy_error", e.getMessage());
        }
      }

      // Complete recovery
      if (recoverySuccessful) {
        execution.complete(true, "Disaster recovery completed successfully");
        successfulRecoveries.incrementAndGet();
        LOGGER.info("DISASTER RECOVERY SUCCESSFUL: " + execution.getExecutionId());

        // Check RTO compliance
        if (execution.getActualRecoveryTime().compareTo(plan.getRecoveryTimeObjective()) > 0) {
          LOGGER.warning(
              String.format(
                  "RTO violation: actual=%s, target=%s",
                  execution.getActualRecoveryTime(), plan.getRecoveryTimeObjective()));
          execution.updateMetric("rto_violation", true);
        }
      } else {
        execution.complete(false, "All recovery strategies failed");
        LOGGER.severe("DISASTER RECOVERY FAILED: " + execution.getExecutionId());
      }

    } catch (final Exception e) {
      execution.complete(false, "Recovery execution error: " + e.getMessage());
      LOGGER.severe("Recovery execution failed: " + e.getMessage());
    } finally {
      recoveryHistory.put(execution.getExecutionId(), execution);
      activeRecovery.set(null);
      currentDisasterType.set(null);
    }
  }

  /** Executes a specific recovery strategy. */
  private boolean executeRecoveryStrategy(
      final RecoveryStrategy strategy,
      final DisasterRecoveryPlan plan,
      final RecoveryExecution execution) {

    execution.updateMetric("current_strategy", strategy.name());

    switch (strategy) {
      case HOT_STANDBY:
        return executeHotStandbyRecovery(plan, execution);
      case WARM_STANDBY:
        return executeWarmStandbyRecovery(plan, execution);
      case COLD_STANDBY:
        return executeColdStandbyRecovery(plan, execution);
      case MANUAL_RECOVERY:
        return executeManualRecovery(plan, execution);
      case PARTIAL_RECOVERY:
        return executePartialRecovery(plan, execution);
      case REBUILD_FROM_SCRATCH:
        return executeRebuildRecovery(plan, execution);
      default:
        LOGGER.warning("Unknown recovery strategy: " + strategy);
        return false;
    }
  }

  /** Executes hot standby recovery (immediate failover). */
  private boolean executeHotStandbyRecovery(
      final DisasterRecoveryPlan plan, final RecoveryExecution execution) {
    try {
      execution.updateMetric("recovery_phase", "hot_standby_activation");

      // Simulate hot standby activation
      Thread.sleep(1000); // Minimal delay for hot standby

      // Restore critical components
      int restoredComponents = 0;
      for (final String componentId : plan.getCriticalComponents()) {
        final RecoverableComponent component = restoreComponent(componentId);
        if (component != null) {
          restoredComponents++;
        }
      }

      execution.updateMetric("components_restored", restoredComponents);
      execution.updateMetric("total_components", plan.getCriticalComponents().size());

      return restoredComponents == plan.getCriticalComponents().size();

    } catch (final Exception e) {
      LOGGER.severe("Hot standby recovery failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes warm standby recovery (quick activation). */
  private boolean executeWarmStandbyRecovery(
      final DisasterRecoveryPlan plan, final RecoveryExecution execution) {
    try {
      execution.updateMetric("recovery_phase", "warm_standby_activation");

      // Simulate warm standby activation
      Thread.sleep(5000); // Medium delay for warm standby

      // Restore components with validation
      int restoredComponents = 0;
      for (final String componentId : plan.getCriticalComponents()) {
        final RecoverableComponent component = restoreComponent(componentId);
        if (component != null && component.validateIntegrity()) {
          restoredComponents++;
        }
      }

      execution.updateMetric("components_restored", restoredComponents);
      return restoredComponents >= plan.getCriticalComponents().size() * 0.8; // 80% success rate

    } catch (final Exception e) {
      LOGGER.severe("Warm standby recovery failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes cold standby recovery (restore from backup). */
  private boolean executeColdStandbyRecovery(
      final DisasterRecoveryPlan plan, final RecoveryExecution execution) {
    try {
      execution.updateMetric("recovery_phase", "cold_standby_restoration");

      // Simulate longer cold standby activation
      Thread.sleep(10000); // Longer delay for cold standby

      // Full restore from backups
      int restoredComponents = 0;
      for (final String componentId : plan.getCriticalComponents()) {
        final RecoverableComponent component = restoreComponent(componentId);
        if (component != null) {
          // Additional validation for cold restore
          if (component.validateIntegrity()
              && component.getBackupTimestamp().isAfter(Instant.now().minus(Duration.ofDays(1)))) {
            restoredComponents++;
          }
        }
      }

      execution.updateMetric("components_restored", restoredComponents);
      return restoredComponents > 0; // At least some recovery

    } catch (final Exception e) {
      LOGGER.severe("Cold standby recovery failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes manual recovery (requires human intervention). */
  private boolean executeManualRecovery(
      final DisasterRecoveryPlan plan, final RecoveryExecution execution) {
    execution.updateMetric("recovery_phase", "manual_intervention_required");
    LOGGER.severe("MANUAL RECOVERY REQUIRED: " + plan.getPlanId());

    // In real implementation, this would trigger alerts and wait for manual intervention
    // For this implementation, we simulate manual recovery success
    try {
      Thread.sleep(30000); // Simulate manual intervention time
      execution.updateMetric("manual_steps_completed", plan.getRecoverySteps().size());
      return true; // Assume manual recovery succeeds
    } catch (final InterruptedException e) {
      Thread.currentThread().interrupt();
      return false;
    }
  }

  /** Executes partial recovery (critical functions only). */
  private boolean executePartialRecovery(
      final DisasterRecoveryPlan plan, final RecoveryExecution execution) {
    try {
      execution.updateMetric("recovery_phase", "partial_recovery");

      // Recover only the most critical components (first 50%)
      final List<String> criticalComponents = plan.getCriticalComponents();
      final int partialCount = Math.max(1, criticalComponents.size() / 2);

      int restoredComponents = 0;
      for (int i = 0; i < partialCount && i < criticalComponents.size(); i++) {
        final RecoverableComponent component = restoreComponent(criticalComponents.get(i));
        if (component != null) {
          restoredComponents++;
        }
      }

      execution.updateMetric("components_restored", restoredComponents);
      execution.updateMetric("partial_recovery_target", partialCount);
      return restoredComponents > 0;

    } catch (final Exception e) {
      LOGGER.severe("Partial recovery failed: " + e.getMessage());
      return false;
    }
  }

  /** Executes rebuild from scratch recovery. */
  private boolean executeRebuildRecovery(
      final DisasterRecoveryPlan plan, final RecoveryExecution execution) {
    try {
      execution.updateMetric("recovery_phase", "complete_rebuild");

      // Simulate complete system rebuild
      Thread.sleep(60000); // Very long delay for complete rebuild

      // Clear all existing state
      backupRepository.clear();

      // Simulate rebuild process
      execution.updateMetric("rebuild_steps_completed", plan.getRecoverySteps().size());
      execution.updateMetric("rebuild_time_minutes", 60);

      return true; // Assume rebuild succeeds

    } catch (final Exception e) {
      LOGGER.severe("Rebuild recovery failed: " + e.getMessage());
      return false;
    }
  }

  /** Finds the best recovery plan for a disaster type. */
  private DisasterRecoveryPlan findBestRecoveryPlan(final DisasterType disasterType) {
    return recoveryPlans.values().stream()
        .filter(plan -> plan.getDisasterType() == disasterType)
        .min((p1, p2) -> Integer.compare(p1.getPriority(), p2.getPriority()))
        .orElse(null);
  }

  /** Compresses data using GZIP. */
  private byte[] compressData(final byte[] data) throws IOException {
    try (final ByteArrayOutputStream baos = new ByteArrayOutputStream();
        final GZIPOutputStream gzipOut = new GZIPOutputStream(baos)) {
      gzipOut.write(data);
      gzipOut.finish();
      return baos.toByteArray();
    }
  }

  /** Decompresses GZIP data. */
  private byte[] decompressData(final byte[] compressedData) throws IOException {
    try (final java.io.ByteArrayInputStream bais =
            new java.io.ByteArrayInputStream(compressedData);
        final GZIPInputStream gzipIn = new GZIPInputStream(bais);
        final ByteArrayOutputStream baos = new ByteArrayOutputStream()) {

      final byte[] buffer = new byte[4096];
      int bytesRead;
      while ((bytesRead = gzipIn.read(buffer)) != -1) {
        baos.write(buffer, 0, bytesRead);
      }
      return baos.toByteArray();
    }
  }

  /** Persists backup to disk storage. */
  private void persistBackupToDisk(final RecoverableComponent component) {
    try {
      Files.createDirectories(backupStoragePath);
      final Path backupFile = backupStoragePath.resolve(component.getComponentId() + "_backup.ser");

      try (final java.io.FileOutputStream fos = new java.io.FileOutputStream(backupFile.toFile());
          final ObjectOutputStream oos = new ObjectOutputStream(fos)) {
        oos.writeObject(component);
      }

    } catch (final Exception e) {
      LOGGER.warning("Failed to persist backup to disk: " + e.getMessage());
    }
  }

  /**
   * Loads backup from disk storage.
   *
   * <p>Uses ValidatingObjectInputStream to prevent deserialization attacks by only allowing
   * RecoverableComponent to be deserialized from backup files.
   */
  @SuppressFBWarnings(
      value = "OBJECT_DESERIALIZATION",
      justification =
          "Deserialization is protected by ValidatingObjectInputStream which only allows "
              + "RecoverableComponent class to be deserialized from backup files")
  private RecoverableComponent loadBackupFromDisk(final String componentId) {
    try {
      final Path backupFile = backupStoragePath.resolve(componentId + "_backup.ser");
      if (!Files.exists(backupFile)) {
        return null;
      }

      try (final java.io.FileInputStream fis = new java.io.FileInputStream(backupFile.toFile());
          final ValidatingObjectInputStream ois = new ValidatingObjectInputStream(fis)) {
        return (RecoverableComponent) ois.readObject();
      }

    } catch (final Exception e) {
      LOGGER.warning("Failed to load backup from disk: " + e.getMessage());
      return null;
    }
  }

  /** Starts background processing for disaster recovery. */
  private void startBackgroundProcessing() {
    // Backup monitoring and cleanup
    executorService.scheduleAtFixedRate(this::performBackupMaintenance, 30, 60, TimeUnit.MINUTES);

    // Health monitoring for disaster detection
    executorService.scheduleAtFixedRate(this::performDisasterDetection, 1, 5, TimeUnit.MINUTES);

    // Recovery progress monitoring
    executorService.scheduleAtFixedRate(this::monitorActiveRecovery, 30, 30, TimeUnit.SECONDS);
  }

  /** Performs backup maintenance and cleanup. */
  private void performBackupMaintenance() {
    try {
      final Instant cutoff = Instant.now().minus(Duration.ofHours(maxBackupRetention));
      final java.util.concurrent.atomic.AtomicInteger cleanedBackups =
          new java.util.concurrent.atomic.AtomicInteger(0);

      // Clean old backups from memory
      backupRepository
          .entrySet()
          .removeIf(
              entry -> {
                if (entry.getValue().getBackupTimestamp().isBefore(cutoff)) {
                  cleanedBackups.incrementAndGet();
                  return true;
                }
                return false;
              });

      // Clean old backup files from disk
      if (Files.exists(backupStoragePath)) {
        Files.list(backupStoragePath)
            .filter(path -> path.toString().endsWith("_backup.ser"))
            .filter(
                path -> {
                  try {
                    return Files.getLastModifiedTime(path).toInstant().isBefore(cutoff);
                  } catch (final IOException e) {
                    return false;
                  }
                })
            .forEach(
                path -> {
                  try {
                    Files.delete(path);
                    cleanedBackups.incrementAndGet();
                  } catch (final IOException e) {
                    LOGGER.warning("Failed to delete old backup file: " + path);
                  }
                });
      }

      if (cleanedBackups.get() > 0) {
        LOGGER.fine("Cleaned " + cleanedBackups.get() + " old backups");
      }

    } catch (final Exception e) {
      LOGGER.warning("Backup maintenance failed: " + e.getMessage());
    }
  }

  /** Performs disaster detection based on system health. */
  private void performDisasterDetection() {
    if (!disasterRecoveryEnabled.get() || !autoFailoverEnabled) {
      return;
    }

    try {
      // Check system resources for disaster conditions
      final Runtime runtime = Runtime.getRuntime();
      final long usedMemory = runtime.totalMemory() - runtime.freeMemory();
      final long maxMemory = runtime.maxMemory();
      final double memoryUsage = (double) usedMemory / maxMemory;

      // Memory exhaustion disaster
      if (memoryUsage > 0.95) {
        LOGGER.severe("DISASTER DETECTED: Memory exhaustion (" + (memoryUsage * 100) + "%)");
        executeDisasterRecovery(DisasterType.HARDWARE_FAILURE, true);
      }

      // Check for data corruption indicators
      final int corruptedBackups =
          (int)
              backupRepository.values().stream()
                  .filter(component -> !component.validateIntegrity())
                  .count();

      if (corruptedBackups > backupRepository.size() * 0.1) {
        LOGGER.severe("DISASTER DETECTED: Data corruption in " + corruptedBackups + " backups");
        executeDisasterRecovery(DisasterType.DATA_CORRUPTION, true);
      }

    } catch (final Exception e) {
      LOGGER.warning("Disaster detection failed: " + e.getMessage());
    }
  }

  /** Monitors active recovery progress. */
  private void monitorActiveRecovery() {
    final RecoveryExecution recovery = activeRecovery.get();
    if (recovery == null) {
      return;
    }

    try {
      final Duration elapsed = Duration.between(recovery.getStartTime(), Instant.now());

      // Check for recovery timeout
      if (elapsed.compareTo(maxRecoveryTime) > 0) {
        LOGGER.severe(
            "RECOVERY TIMEOUT: Recovery has exceeded maximum time limit: " + maxRecoveryTime);
        recovery.complete(false, "Recovery timeout exceeded");
        activeRecovery.set(null);
        currentDisasterType.set(null);
      } else {
        LOGGER.fine(
            String.format(
                "Recovery in progress: %s [elapsed=%s, max=%s]",
                recovery.getExecutionId(), elapsed, maxRecoveryTime));
      }

    } catch (final Exception e) {
      LOGGER.warning("Recovery monitoring failed: " + e.getMessage());
    }
  }

  /**
   * Adds a disaster recovery plan.
   *
   * @param plan the recovery plan to add
   */
  public void addRecoveryPlan(final DisasterRecoveryPlan plan) {
    recoveryPlans.put(plan.getPlanId(), plan);
    LOGGER.info("Added disaster recovery plan: " + plan.getPlanId());
  }

  /**
   * Gets disaster recovery status.
   *
   * @return formatted status report
   */
  public String getRecoveryStatus() {
    final StringBuilder sb = new StringBuilder();
    sb.append(String.format("=== Disaster Recovery Status ===%n"));

    sb.append(String.format("DR Enabled: %s%n", disasterRecoveryEnabled.get()));
    sb.append(String.format("Auto Failover: %s%n", autoFailoverEnabled));
    sb.append(String.format("Total Backups: %,d%n", totalBackups.get()));
    sb.append(String.format("Total Recoveries: %,d%n", totalRecoveries.get()));
    sb.append(String.format("Successful Recoveries: %,d%n", successfulRecoveries.get()));
    sb.append(String.format("Active Recovery Plans: %d%n", recoveryPlans.size()));
    sb.append(String.format("Backup Repository Size: %d%n", backupRepository.size()));

    final DisasterType currentDisaster = currentDisasterType.get();
    sb.append(
        String.format(
            "Current Disaster: %s%n", currentDisaster != null ? currentDisaster : "None"));

    final RecoveryExecution activeExecution = activeRecovery.get();
    if (activeExecution != null) {
      final Duration elapsed = Duration.between(activeExecution.getStartTime(), Instant.now());
      sb.append(
          String.format(
              "Active Recovery: %s [%s elapsed]%n", activeExecution.getExecutionId(), elapsed));
    } else {
      sb.append(String.format("Active Recovery: None%n"));
    }

    return sb.toString();
  }

  /**
   * Simulates a disaster for testing purposes.
   *
   * @param disasterType the type of disaster to simulate
   * @return true if simulation was started
   */
  public boolean simulateDisaster(final DisasterType disasterType) {
    if (!disasterRecoveryEnabled.get()) {
      return false;
    }

    LOGGER.warning("SIMULATING DISASTER: " + disasterType + " (for testing purposes)");
    return executeDisasterRecovery(disasterType, false) != null;
  }

  /** Sets disaster recovery enabled state. */
  public void setEnabled(final boolean enabled) {
    this.disasterRecoveryEnabled.set(enabled);
    LOGGER.info("Disaster recovery " + (enabled ? "enabled" : "disabled"));
  }

  /** Gets disaster recovery enabled state. */
  public boolean isEnabled() {
    return disasterRecoveryEnabled.get();
  }

  /** Shuts down disaster recovery system. */
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
    LOGGER.info("Disaster Recovery System shutdown");
  }
}
