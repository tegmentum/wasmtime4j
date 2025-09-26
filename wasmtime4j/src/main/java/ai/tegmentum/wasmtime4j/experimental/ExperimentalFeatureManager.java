package ai.tegmentum.wasmtime4j.experimental;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Central manager for experimental feature flagging and configuration.
 *
 * <p>This class provides a centralized system for managing experimental features,
 * including feature flags, runtime configuration, safety checks, and rollback capabilities.
 *
 * <p><b>WARNING:</b> Experimental features are unstable and subject to change.
 * This manager provides safety mechanisms but cannot guarantee compatibility.
 *
 * @since 1.0.0
 */
public final class ExperimentalFeatureManager {

    private static final Logger LOGGER = Logger.getLogger(ExperimentalFeatureManager.class.getName());

    // Singleton instance
    private static volatile ExperimentalFeatureManager instance;
    private static final Object LOCK = new Object();

    // Thread-safe storage for feature configurations
    private final Map<String, FeatureConfiguration> configurations = new ConcurrentHashMap<>();
    private final Map<ExperimentalFeature, FeatureStatus> featureStatuses = new ConcurrentHashMap<>();
    private final Map<String, Object> globalProperties = new ConcurrentHashMap<>();

    // Configuration locks for safe concurrent access
    private final ReadWriteLock configLock = new ReentrantReadWriteLock();

    // Feature rollback tracking
    private final Map<ExperimentalFeature, FeatureSnapshot> featureSnapshots = new ConcurrentHashMap<>();

    // Safety mechanisms
    private volatile boolean safetyModeEnabled = true;
    private volatile int maxConcurrentExperimentalFeatures = 10;
    private final Set<ExperimentalFeature.FeatureCategory> allowedCategories = EnumSet.allOf(ExperimentalFeature.FeatureCategory.class);

    /**
     * Gets the singleton instance of the experimental feature manager.
     *
     * @return the feature manager instance
     */
    public static ExperimentalFeatureManager getInstance() {
        if (instance == null) {
            synchronized (LOCK) {
                if (instance == null) {
                    instance = new ExperimentalFeatureManager();
                }
            }
        }
        return instance;
    }

    private ExperimentalFeatureManager() {
        initializeDefaultConfigurations();
        LOGGER.info("Experimental Feature Manager initialized");
    }

    /**
     * Registers a feature configuration with the manager.
     *
     * @param configurationId unique identifier for the configuration
     * @param config the experimental feature configuration
     * @throws IllegalArgumentException if parameters are invalid
     * @throws IllegalStateException if safety checks fail
     */
    public void registerConfiguration(final String configurationId, final ExperimentalFeatureConfig config) {
        Objects.requireNonNull(configurationId, "Configuration ID cannot be null");
        Objects.requireNonNull(config, "Configuration cannot be null");

        configLock.writeLock().lock();
        try {
            // Safety checks
            if (safetyModeEnabled) {
                performSafetyChecks(config);
            }

            // Create feature configuration record
            final FeatureConfiguration featureConfig = new FeatureConfiguration(
                configurationId,
                config,
                System.currentTimeMillis(),
                getCurrentUser(),
                FeatureConfiguration.Status.REGISTERED
            );

            configurations.put(configurationId, featureConfig);

            // Update feature statuses
            updateFeatureStatuses(config);

            // Create snapshots for rollback capability
            createFeatureSnapshots(config);

            LOGGER.info("Registered experimental feature configuration: {} with {} features",
                       configurationId, config.getEnabledFeatures().size());

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to register configuration: " + configurationId, e);
            throw new RuntimeException("Failed to register configuration", e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Retrieves a registered feature configuration.
     *
     * @param configurationId the configuration identifier
     * @return the feature configuration, or null if not found
     */
    public FeatureConfiguration getConfiguration(final String configurationId) {
        Objects.requireNonNull(configurationId, "Configuration ID cannot be null");

        configLock.readLock().lock();
        try {
            return configurations.get(configurationId);
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Enables a specific experimental feature globally.
     *
     * @param feature the feature to enable
     * @param reason reason for enabling the feature
     * @throws IllegalArgumentException if feature is null
     * @throws IllegalStateException if feature cannot be safely enabled
     */
    public void enableFeature(final ExperimentalFeature feature, final String reason) {
        Objects.requireNonNull(feature, "Feature cannot be null");

        configLock.writeLock().lock();
        try {
            // Safety checks
            if (safetyModeEnabled) {
                validateFeatureEnablement(feature);
            }

            // Create feature snapshot before enabling
            createFeatureSnapshot(feature);

            // Enable the feature
            final FeatureStatus status = new FeatureStatus(
                feature,
                true,
                System.currentTimeMillis(),
                getCurrentUser(),
                reason != null ? reason : "Manual enablement"
            );

            featureStatuses.put(feature, status);

            LOGGER.info("Enabled experimental feature: {} - {}", feature, reason);

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to enable feature: " + feature, e);
            throw new RuntimeException("Failed to enable feature", e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Disables a specific experimental feature globally.
     *
     * @param feature the feature to disable
     * @param reason reason for disabling the feature
     */
    public void disableFeature(final ExperimentalFeature feature, final String reason) {
        Objects.requireNonNull(feature, "Feature cannot be null");

        configLock.writeLock().lock();
        try {
            final FeatureStatus status = new FeatureStatus(
                feature,
                false,
                System.currentTimeMillis(),
                getCurrentUser(),
                reason != null ? reason : "Manual disablement"
            );

            featureStatuses.put(feature, status);

            LOGGER.info("Disabled experimental feature: {} - {}", feature, reason);

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to disable feature: " + feature, e);
            throw new RuntimeException("Failed to disable feature", e);
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Checks if a specific experimental feature is globally enabled.
     *
     * @param feature the feature to check
     * @return true if the feature is enabled
     */
    public boolean isFeatureEnabled(final ExperimentalFeature feature) {
        Objects.requireNonNull(feature, "Feature cannot be null");

        configLock.readLock().lock();
        try {
            final FeatureStatus status = featureStatuses.get(feature);
            return status != null && status.isEnabled();
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Gets the status of a specific experimental feature.
     *
     * @param feature the feature to check
     * @return the feature status, or null if not tracked
     */
    public FeatureStatus getFeatureStatus(final ExperimentalFeature feature) {
        Objects.requireNonNull(feature, "Feature cannot be null");

        configLock.readLock().lock();
        try {
            return featureStatuses.get(feature);
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Rolls back a feature to its previous state.
     *
     * @param feature the feature to roll back
     * @return true if rollback was successful
     */
    public boolean rollbackFeature(final ExperimentalFeature feature) {
        Objects.requireNonNull(feature, "Feature cannot be null");

        configLock.writeLock().lock();
        try {
            final FeatureSnapshot snapshot = featureSnapshots.get(feature);
            if (snapshot == null) {
                LOGGER.warning("No snapshot available for feature rollback: {}", feature);
                return false;
            }

            // Restore previous state
            if (snapshot.wasEnabled()) {
                enableFeature(feature, "Rollback to previous state");
            } else {
                disableFeature(feature, "Rollback to previous state");
            }

            LOGGER.info("Rolled back feature to previous state: {}", feature);
            return true;

        } catch (final Exception e) {
            LOGGER.log(Level.SEVERE, "Failed to rollback feature: " + feature, e);
            return false;
        } finally {
            configLock.writeLock().unlock();
        }
    }

    /**
     * Gets all registered configurations.
     *
     * @return map of configuration IDs to configurations
     */
    public Map<String, FeatureConfiguration> getAllConfigurations() {
        configLock.readLock().lock();
        try {
            return new HashMap<>(configurations);
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Gets all feature statuses.
     *
     * @return map of features to their statuses
     */
    public Map<ExperimentalFeature, FeatureStatus> getAllFeatureStatuses() {
        configLock.readLock().lock();
        try {
            return new HashMap<>(featureStatuses);
        } finally {
            configLock.readLock().unlock();
        }
    }

    /**
     * Enables or disables safety mode for experimental features.
     *
     * @param enabled true to enable safety mode
     */
    public void setSafetyModeEnabled(final boolean enabled) {
        this.safetyModeEnabled = enabled;
        LOGGER.info("Safety mode {}", enabled ? "enabled" : "disabled");
    }

    /**
     * Sets the maximum number of concurrent experimental features allowed.
     *
     * @param maxFeatures the maximum number of features
     */
    public void setMaxConcurrentExperimentalFeatures(final int maxFeatures) {
        if (maxFeatures < 0) {
            throw new IllegalArgumentException("Max concurrent features cannot be negative");
        }
        this.maxConcurrentExperimentalFeatures = maxFeatures;
        LOGGER.info("Set max concurrent experimental features to: {}", maxFeatures);
    }

    /**
     * Sets the allowed feature categories.
     *
     * @param categories the allowed categories
     */
    public void setAllowedCategories(final Set<ExperimentalFeature.FeatureCategory> categories) {
        Objects.requireNonNull(categories, "Categories cannot be null");
        this.allowedCategories.clear();
        this.allowedCategories.addAll(categories);
        LOGGER.info("Set allowed feature categories: {}", categories);
    }

    /**
     * Gets summary statistics about experimental feature usage.
     *
     * @return usage statistics
     */
    public FeatureUsageStatistics getUsageStatistics() {
        configLock.readLock().lock();
        try {
            final int totalConfigurations = configurations.size();
            final long enabledFeatures = featureStatuses.values().stream()
                .mapToLong(status -> status.isEnabled() ? 1 : 0)
                .sum();
            final long disabledFeatures = featureStatuses.size() - enabledFeatures;

            final Map<ExperimentalFeature.FeatureCategory, Long> featuresByCategory = new HashMap<>();
            for (final FeatureStatus status : featureStatuses.values()) {
                if (status.isEnabled()) {
                    featuresByCategory.merge(status.getFeature().getCategory(), 1L, Long::sum);
                }
            }

            return new FeatureUsageStatistics(
                totalConfigurations,
                (int) enabledFeatures,
                (int) disabledFeatures,
                featuresByCategory,
                safetyModeEnabled,
                maxConcurrentExperimentalFeatures
            );

        } finally {
            configLock.readLock().unlock();
        }
    }

    // Private helper methods

    private void initializeDefaultConfigurations() {
        // Set up default global properties
        globalProperties.put("safety.mode.enabled", safetyModeEnabled);
        globalProperties.put("safety.max.concurrent.features", maxConcurrentExperimentalFeatures);
        globalProperties.put("manager.version", "1.0.0");
        globalProperties.put("manager.initialized.time", System.currentTimeMillis());
    }

    private void performSafetyChecks(final ExperimentalFeatureConfig config) {
        // Check maximum concurrent features
        final long enabledCount = config.getEnabledFeatures().values().stream()
            .mapToLong(enabled -> enabled ? 1 : 0)
            .sum();

        if (enabledCount > maxConcurrentExperimentalFeatures) {
            throw new IllegalStateException(
                String.format("Too many experimental features enabled: %d (max: %d)",
                             enabledCount, maxConcurrentExperimentalFeatures)
            );
        }

        // Check allowed categories
        for (final ExperimentalFeature feature : config.getEnabledFeatures().keySet()) {
            if (config.isFeatureEnabled(feature) && !allowedCategories.contains(feature.getCategory())) {
                throw new IllegalStateException(
                    String.format("Feature category not allowed: %s (%s)",
                                 feature, feature.getCategory())
                );
            }
        }
    }

    private void validateFeatureEnablement(final ExperimentalFeature feature) {
        // Check if category is allowed
        if (!allowedCategories.contains(feature.getCategory())) {
            throw new IllegalStateException(
                String.format("Feature category not allowed: %s", feature.getCategory())
            );
        }

        // Check concurrent feature limit
        final long currentlyEnabled = featureStatuses.values().stream()
            .mapToLong(status -> status.isEnabled() ? 1 : 0)
            .sum();

        if (currentlyEnabled >= maxConcurrentExperimentalFeatures) {
            throw new IllegalStateException(
                String.format("Cannot enable feature: too many concurrent features (%d/%d)",
                             currentlyEnabled, maxConcurrentExperimentalFeatures)
            );
        }
    }

    private void updateFeatureStatuses(final ExperimentalFeatureConfig config) {
        final String user = getCurrentUser();
        final long timestamp = System.currentTimeMillis();

        for (final Map.Entry<ExperimentalFeature, Boolean> entry : config.getEnabledFeatures().entrySet()) {
            final ExperimentalFeature feature = entry.getKey();
            final boolean enabled = entry.getValue();

            final FeatureStatus status = new FeatureStatus(
                feature,
                enabled,
                timestamp,
                user,
                "Configuration update"
            );

            featureStatuses.put(feature, status);
        }
    }

    private void createFeatureSnapshots(final ExperimentalFeatureConfig config) {
        for (final ExperimentalFeature feature : config.getEnabledFeatures().keySet()) {
            createFeatureSnapshot(feature);
        }
    }

    private void createFeatureSnapshot(final ExperimentalFeature feature) {
        final FeatureStatus currentStatus = featureStatuses.get(feature);
        final boolean wasEnabled = currentStatus != null && currentStatus.isEnabled();

        final FeatureSnapshot snapshot = new FeatureSnapshot(
            feature,
            wasEnabled,
            System.currentTimeMillis(),
            getCurrentUser()
        );

        featureSnapshots.put(feature, snapshot);
    }

    private String getCurrentUser() {
        return System.getProperty("user.name", "unknown");
    }

    /**
     * Feature configuration record.
     */
    public static final class FeatureConfiguration {
        private final String configurationId;
        private final ExperimentalFeatureConfig config;
        private final long registrationTime;
        private final String registeredBy;
        private final Status status;

        public enum Status {
            REGISTERED,
            ACTIVE,
            DEPRECATED,
            ROLLED_BACK
        }

        public FeatureConfiguration(final String configurationId, final ExperimentalFeatureConfig config,
                                   final long registrationTime, final String registeredBy, final Status status) {
            this.configurationId = configurationId;
            this.config = config;
            this.registrationTime = registrationTime;
            this.registeredBy = registeredBy;
            this.status = status;
        }

        public String getConfigurationId() { return configurationId; }
        public ExperimentalFeatureConfig getConfig() { return config; }
        public long getRegistrationTime() { return registrationTime; }
        public String getRegisteredBy() { return registeredBy; }
        public Status getStatus() { return status; }
    }

    /**
     * Feature status record.
     */
    public static final class FeatureStatus {
        private final ExperimentalFeature feature;
        private final boolean enabled;
        private final long statusTime;
        private final String statusBy;
        private final String reason;

        public FeatureStatus(final ExperimentalFeature feature, final boolean enabled,
                           final long statusTime, final String statusBy, final String reason) {
            this.feature = feature;
            this.enabled = enabled;
            this.statusTime = statusTime;
            this.statusBy = statusBy;
            this.reason = reason;
        }

        public ExperimentalFeature getFeature() { return feature; }
        public boolean isEnabled() { return enabled; }
        public long getStatusTime() { return statusTime; }
        public String getStatusBy() { return statusBy; }
        public String getReason() { return reason; }
    }

    /**
     * Feature snapshot for rollback capability.
     */
    private static final class FeatureSnapshot {
        private final ExperimentalFeature feature;
        private final boolean wasEnabled;
        private final long snapshotTime;
        private final String snapshotBy;

        public FeatureSnapshot(final ExperimentalFeature feature, final boolean wasEnabled,
                             final long snapshotTime, final String snapshotBy) {
            this.feature = feature;
            this.wasEnabled = wasEnabled;
            this.snapshotTime = snapshotTime;
            this.snapshotBy = snapshotBy;
        }

        public ExperimentalFeature getFeature() { return feature; }
        public boolean wasEnabled() { return wasEnabled; }
        public long getSnapshotTime() { return snapshotTime; }
        public String getSnapshotBy() { return snapshotBy; }
    }

    /**
     * Feature usage statistics.
     */
    public static final class FeatureUsageStatistics {
        private final int totalConfigurations;
        private final int enabledFeatures;
        private final int disabledFeatures;
        private final Map<ExperimentalFeature.FeatureCategory, Long> featuresByCategory;
        private final boolean safetyModeEnabled;
        private final int maxConcurrentFeatures;

        public FeatureUsageStatistics(final int totalConfigurations, final int enabledFeatures,
                                     final int disabledFeatures, final Map<ExperimentalFeature.FeatureCategory, Long> featuresByCategory,
                                     final boolean safetyModeEnabled, final int maxConcurrentFeatures) {
            this.totalConfigurations = totalConfigurations;
            this.enabledFeatures = enabledFeatures;
            this.disabledFeatures = disabledFeatures;
            this.featuresByCategory = new HashMap<>(featuresByCategory);
            this.safetyModeEnabled = safetyModeEnabled;
            this.maxConcurrentFeatures = maxConcurrentFeatures;
        }

        public int getTotalConfigurations() { return totalConfigurations; }
        public int getEnabledFeatures() { return enabledFeatures; }
        public int getDisabledFeatures() { return disabledFeatures; }
        public Map<ExperimentalFeature.FeatureCategory, Long> getFeaturesByCategory() { return new HashMap<>(featuresByCategory); }
        public boolean isSafetyModeEnabled() { return safetyModeEnabled; }
        public int getMaxConcurrentFeatures() { return maxConcurrentFeatures; }

        @Override
        public String toString() {
            return String.format("FeatureUsageStatistics{configurations=%d, enabled=%d, disabled=%d, safetyMode=%s}",
                               totalConfigurations, enabledFeatures, disabledFeatures, safetyModeEnabled);
        }
    }
}