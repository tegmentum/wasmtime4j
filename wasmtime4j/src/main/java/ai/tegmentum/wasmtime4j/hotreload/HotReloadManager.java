package ai.tegmentum.wasmtime4j.hotreload;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.util.concurrent.CompletableFuture;

/**
 * Public API for hot-reloading WebAssembly modules without service interruption.
 *
 * <p>The HotReloadManager provides comprehensive hot-reload capabilities for WebAssembly
 * components, enabling live updates, state migration, and multiple deployment strategies.
 *
 * <p>Key features include:
 * <ul>
 *   <li>Live module replacement without service interruption</li>
 *   <li>State migration and preservation during updates</li>
 *   <li>Version compatibility checking and rollback mechanisms</li>
 *   <li>Background component loading and validation</li>
 *   <li>Multiple deployment strategies (canary, blue-green, rolling)</li>
 *   <li>Health monitoring and automatic rollback</li>
 *   <li>Performance metrics and monitoring</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * Engine engine = Engine.newEngine();
 * HotReloadConfig config = HotReloadConfig.builder()
 *     .validationEnabled(true)
 *     .statePreservationEnabled(true)
 *     .build();
 *
 * try (HotReloadManager manager = HotReloadManager.create(engine, config)) {
 *     // Start a canary deployment
 *     String operationId = manager.startHotSwap("my-component", "2.0.0",
 *         SwapStrategy.canary(10.0f, 25.0f, 0.99f));
 *
 *     // Monitor the operation
 *     HotSwapStatus status = manager.getSwapStatus(operationId);
 *     System.out.println("Progress: " + status.getProgress());
 *
 *     // Get performance metrics
 *     HotReloadMetrics metrics = manager.getMetrics();
 *     System.out.println("Success rate: " + metrics.getSuccessRate());
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> Implementations are thread-safe and can be used
 * concurrently from multiple threads.
 *
 * <p><strong>Resource Management:</strong> This interface extends {@link AutoCloseable}
 * and should be used with try-with-resources to ensure proper cleanup.
 */
public interface HotReloadManager extends AutoCloseable {

    /**
     * Creates a new hot reload manager using the best available implementation
     * for the current Java version.
     *
     * <p>On Java 23+, this will use the Panama FFI implementation for better
     * performance. On Java 8-22, it will use the JNI implementation.
     *
     * @param engine The WebAssembly engine to use
     * @param config The hot reload configuration
     * @return A new hot reload manager instance
     * @throws IllegalArgumentException if engine or config is null
     * @throws WasmRuntimeException if the manager cannot be created
     */
    static HotReloadManager create(final Engine engine, final HotReloadConfig config) {
        if (engine == null) {
            throw new IllegalArgumentException("Engine cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        return HotReloadManagerFactory.createManager(engine, config);
    }

    /**
     * Starts a hot swap operation to replace the current component version.
     *
     * <p>This operation is asynchronous and returns immediately with an operation ID
     * that can be used to track progress and cancel the operation if needed.
     *
     * @param componentName The name of the component to swap
     * @param targetVersion The target version to swap to
     * @param strategy The swap strategy to use (null for default canary)
     * @return A unique operation ID for tracking the swap
     * @throws IllegalArgumentException if componentName or targetVersion is null/empty
     * @throws WasmRuntimeException if the swap cannot be started
     * @throws IllegalStateException if this manager has been closed
     */
    String startHotSwap(String componentName, String targetVersion, SwapStrategy strategy);

    /**
     * Gets the current status of a hot swap operation.
     *
     * @param operationId The operation ID returned by startHotSwap
     * @return The current status of the operation, or null if not found
     * @throws IllegalArgumentException if operationId is null/empty
     * @throws IllegalStateException if this manager has been closed
     */
    HotSwapStatus getSwapStatus(String operationId);

    /**
     * Cancels a hot swap operation if it's still in progress.
     *
     * <p>Operations that are already completed or in critical phases may not
     * be cancellable. This method will attempt to cancel gracefully, potentially
     * triggering a rollback if the operation is already in progress.
     *
     * @param operationId The operation ID to cancel
     * @return true if the operation was cancelled, false if it couldn't be cancelled
     * @throws IllegalArgumentException if operationId is null/empty
     * @throws IllegalStateException if this manager has been closed
     */
    boolean cancelHotSwap(String operationId);

    /**
     * Loads a component asynchronously in the background for faster hot swaps.
     *
     * <p>Pre-loading components allows for faster swap operations since the
     * component is already compiled and validated when the swap is triggered.
     *
     * @param request The load request containing component details
     * @return A CompletableFuture that completes with the request ID
     * @throws IllegalArgumentException if request is null or invalid
     * @throws IllegalStateException if this manager has been closed
     */
    CompletableFuture<String> loadComponentAsync(LoadRequest request);

    /**
     * Gets the current hot reload metrics and performance statistics.
     *
     * @return The current metrics
     * @throws IllegalStateException if this manager has been closed
     */
    HotReloadMetrics getMetrics();

    /**
     * Checks if this hot reload manager has been closed.
     *
     * @return true if closed, false otherwise
     */
    boolean isClosed();

    /**
     * Configuration for hot reload behavior.
     */
    final class HotReloadConfig {
        private final boolean validationEnabled;
        private final boolean statePreservationEnabled;
        private final long debounceDelayMs;
        private final boolean precompilationEnabled;
        private final int maxReloadAttempts;
        private final long healthCheckIntervalSecs;
        private final int loaderThreadCount;
        private final int cacheSize;

        private HotReloadConfig(final Builder builder) {
            this.validationEnabled = builder.validationEnabled;
            this.statePreservationEnabled = builder.statePreservationEnabled;
            this.debounceDelayMs = builder.debounceDelayMs;
            this.precompilationEnabled = builder.precompilationEnabled;
            this.maxReloadAttempts = builder.maxReloadAttempts;
            this.healthCheckIntervalSecs = builder.healthCheckIntervalSecs;
            this.loaderThreadCount = builder.loaderThreadCount;
            this.cacheSize = builder.cacheSize;
        }

        /**
         * Creates a new builder for hot reload configuration.
         *
         * @return A new builder instance
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * Creates a default hot reload configuration.
         *
         * @return A default configuration with recommended settings
         */
        public static HotReloadConfig getDefault() {
            return builder().build();
        }

        // Getters
        public boolean isValidationEnabled() { return validationEnabled; }
        public boolean isStatePreservationEnabled() { return statePreservationEnabled; }
        public long getDebounceDelayMs() { return debounceDelayMs; }
        public boolean isPrecompilationEnabled() { return precompilationEnabled; }
        public int getMaxReloadAttempts() { return maxReloadAttempts; }
        public long getHealthCheckIntervalSecs() { return healthCheckIntervalSecs; }
        public int getLoaderThreadCount() { return loaderThreadCount; }
        public int getCacheSize() { return cacheSize; }

        @Override
        public String toString() {
            return String.format("HotReloadConfig{validation=%s, statePreservation=%s, " +
                               "debounceMs=%d, precompilation=%s, maxAttempts=%d, " +
                               "healthCheckSecs=%d, loaderThreads=%d, cacheSize=%d}",
                               validationEnabled, statePreservationEnabled, debounceDelayMs,
                               precompilationEnabled, maxReloadAttempts, healthCheckIntervalSecs,
                               loaderThreadCount, cacheSize);
        }

        /**
         * Builder for hot reload configuration.
         */
        public static final class Builder {
            private boolean validationEnabled = true;
            private boolean statePreservationEnabled = true;
            private long debounceDelayMs = 100;
            private boolean precompilationEnabled = true;
            private int maxReloadAttempts = 3;
            private long healthCheckIntervalSecs = 30;
            private int loaderThreadCount = 4;
            private int cacheSize = 100;

            /**
             * Enables or disables component validation during reload.
             *
             * @param enabled true to enable validation (default)
             * @return this builder
             */
            public Builder validationEnabled(final boolean enabled) {
                this.validationEnabled = enabled;
                return this;
            }

            /**
             * Enables or disables state preservation across reloads.
             *
             * @param enabled true to enable state preservation (default)
             * @return this builder
             */
            public Builder statePreservationEnabled(final boolean enabled) {
                this.statePreservationEnabled = enabled;
                return this;
            }

            /**
             * Sets the debounce delay for file system changes.
             *
             * @param delayMs debounce delay in milliseconds (default: 100)
             * @return this builder
             * @throws IllegalArgumentException if delayMs is negative
             */
            public Builder debounceDelayMs(final long delayMs) {
                if (delayMs < 0) {
                    throw new IllegalArgumentException("Debounce delay must be non-negative");
                }
                this.debounceDelayMs = delayMs;
                return this;
            }

            /**
             * Enables or disables component precompilation.
             *
             * @param enabled true to enable precompilation (default)
             * @return this builder
             */
            public Builder precompilationEnabled(final boolean enabled) {
                this.precompilationEnabled = enabled;
                return this;
            }

            /**
             * Sets the maximum number of reload attempts.
             *
             * @param attempts maximum attempts (default: 3)
             * @return this builder
             * @throws IllegalArgumentException if attempts is less than 1
             */
            public Builder maxReloadAttempts(final int attempts) {
                if (attempts < 1) {
                    throw new IllegalArgumentException("Max reload attempts must be positive");
                }
                this.maxReloadAttempts = attempts;
                return this;
            }

            /**
             * Sets the health check interval.
             *
             * @param intervalSecs interval in seconds (default: 30)
             * @return this builder
             * @throws IllegalArgumentException if intervalSecs is less than 1
             */
            public Builder healthCheckIntervalSecs(final long intervalSecs) {
                if (intervalSecs < 1) {
                    throw new IllegalArgumentException("Health check interval must be positive");
                }
                this.healthCheckIntervalSecs = intervalSecs;
                return this;
            }

            /**
             * Sets the number of background loader threads.
             *
             * @param threadCount number of threads (default: 4)
             * @return this builder
             * @throws IllegalArgumentException if threadCount is less than 1
             */
            public Builder loaderThreadCount(final int threadCount) {
                if (threadCount < 1) {
                    throw new IllegalArgumentException("Loader thread count must be positive");
                }
                this.loaderThreadCount = threadCount;
                return this;
            }

            /**
             * Sets the component cache size.
             *
             * @param size cache size (default: 100)
             * @return this builder
             * @throws IllegalArgumentException if size is less than 1
             */
            public Builder cacheSize(final int size) {
                if (size < 1) {
                    throw new IllegalArgumentException("Cache size must be positive");
                }
                this.cacheSize = size;
                return this;
            }

            /**
             * Builds the hot reload configuration.
             *
             * @return A new configuration instance
             */
            public HotReloadConfig build() {
                return new HotReloadConfig(this);
            }
        }
    }

    /**
     * Hot swap deployment strategies.
     */
    abstract class SwapStrategy {
        protected final int type;
        protected final long param1;
        protected final long param2;
        protected final double param3;

        protected SwapStrategy(final int type, final long param1, final long param2, final double param3) {
            this.type = type;
            this.param1 = param1;
            this.param2 = param2;
            this.param3 = param3;
        }

        public int getType() { return type; }
        public long getParam1() { return param1; }
        public long getParam2() { return param2; }
        public double getParam3() { return param3; }

        /**
         * Creates an immediate swap strategy that replaces the component instantly.
         *
         * <p>This strategy has the fastest deployment time but provides no
         * gradual rollout or safety checks beyond basic validation.
         *
         * @return An immediate swap strategy
         */
        public static SwapStrategy immediate() {
            return new SwapStrategy(0, 0, 0, 0.0) {
                @Override
                public String toString() { return "Immediate"; }
            };
        }

        /**
         * Creates a canary deployment strategy that gradually shifts traffic.
         *
         * <p>This strategy starts by routing a small percentage of traffic to
         * the new version and gradually increases it based on health metrics.
         *
         * @param initialPercentage Initial traffic percentage for the new version (0.0-100.0)
         * @param incrementPercentage Traffic increment per step (0.0-100.0)
         * @param successThreshold Success rate threshold to continue (0.0-1.0)
         * @return A canary deployment strategy
         * @throws IllegalArgumentException if parameters are out of valid ranges
         */
        public static SwapStrategy canary(final float initialPercentage, final float incrementPercentage,
                                        final float successThreshold) {
            if (initialPercentage < 0 || initialPercentage > 100) {
                throw new IllegalArgumentException("Initial percentage must be between 0 and 100");
            }
            if (incrementPercentage < 0 || incrementPercentage > 100) {
                throw new IllegalArgumentException("Increment percentage must be between 0 and 100");
            }
            if (successThreshold < 0 || successThreshold > 1) {
                throw new IllegalArgumentException("Success threshold must be between 0 and 1");
            }

            return new SwapStrategy(1, (long)(initialPercentage * 100), (long)(incrementPercentage * 100), successThreshold) {
                @Override
                public String toString() {
                    return String.format("Canary{initial=%.1f%%, increment=%.1f%%, threshold=%.1f%%}",
                                       initialPercentage, incrementPercentage, successThreshold * 100);
                }
            };
        }

        /**
         * Creates a blue-green deployment strategy with instant switchover.
         *
         * <p>This strategy prepares the new version completely in parallel,
         * then switches all traffic at once after validation.
         *
         * @return A blue-green deployment strategy
         */
        public static SwapStrategy blueGreen() {
            return new SwapStrategy(2, 0, 0, 0.0) {
                @Override
                public String toString() { return "BlueGreen"; }
            };
        }

        /**
         * Creates a rolling update strategy that updates in batches.
         *
         * <p>This strategy updates components in batches with delays between
         * batches to allow for monitoring and validation.
         *
         * @param batchSize Number of instances to update per batch
         * @param batchIntervalSecs Delay between batches in seconds
         * @return A rolling update strategy
         * @throws IllegalArgumentException if parameters are invalid
         */
        public static SwapStrategy rollingUpdate(final int batchSize, final long batchIntervalSecs) {
            if (batchSize < 1) {
                throw new IllegalArgumentException("Batch size must be positive");
            }
            if (batchIntervalSecs < 0) {
                throw new IllegalArgumentException("Batch interval must be non-negative");
            }

            return new SwapStrategy(3, batchSize, batchIntervalSecs, 0.0) {
                @Override
                public String toString() {
                    return String.format("RollingUpdate{batchSize=%d, intervalSecs=%d}",
                                       batchSize, batchIntervalSecs);
                }
            };
        }

        /**
         * Creates an A/B test strategy for controlled testing.
         *
         * <p>This strategy routes a fixed percentage of traffic to the new
         * version for a specified duration to gather metrics.
         *
         * @param testPercentage Percentage of traffic for testing (0.0-100.0)
         * @param testDurationSecs Duration of the test in seconds
         * @return An A/B test strategy
         * @throws IllegalArgumentException if parameters are invalid
         */
        public static SwapStrategy abTest(final float testPercentage, final long testDurationSecs) {
            if (testPercentage < 0 || testPercentage > 100) {
                throw new IllegalArgumentException("Test percentage must be between 0 and 100");
            }
            if (testDurationSecs < 1) {
                throw new IllegalArgumentException("Test duration must be positive");
            }

            return new SwapStrategy(4, (long)(testPercentage * 100), testDurationSecs, 0.0) {
                @Override
                public String toString() {
                    return String.format("ABTest{testPercentage=%.1f%%, durationSecs=%d}",
                                       testPercentage, testDurationSecs);
                }
            };
        }

        /**
         * Gets the default swap strategy (canary deployment).
         *
         * @return A default canary strategy with 10% initial, 25% increment, and 99% threshold
         */
        public static SwapStrategy getDefault() {
            return canary(10.0f, 25.0f, 0.99f);
        }
    }

    /**
     * Status of a hot swap operation.
     */
    final class HotSwapStatus {
        private final String operationId;
        private final String componentName;
        private final String fromVersion;
        private final String toVersion;
        private final SwapStatus status;
        private final float progress;

        public HotSwapStatus(final String operationId, final String componentName,
                           final String fromVersion, final String toVersion,
                           final SwapStatus status, final float progress) {
            this.operationId = operationId;
            this.componentName = componentName;
            this.fromVersion = fromVersion;
            this.toVersion = toVersion;
            this.status = status;
            this.progress = progress;
        }

        public String getOperationId() { return operationId; }
        public String getComponentName() { return componentName; }
        public String getFromVersion() { return fromVersion; }
        public String getToVersion() { return toVersion; }
        public SwapStatus getStatus() { return status; }
        public float getProgress() { return progress; }

        @Override
        public String toString() {
            return String.format("HotSwapStatus{id=%s, component=%s, %s->%s, status=%s, progress=%.1f%%}",
                               operationId, componentName, fromVersion, toVersion, status, progress * 100);
        }
    }

    /**
     * Hot swap operation status enumeration.
     */
    enum SwapStatus {
        /** Operation has been queued but not yet started */
        PENDING,
        /** Pre-loading the new component version */
        PRE_LOADING,
        /** Validating the new component */
        VALIDATING,
        /** Starting the swap process */
        STARTING,
        /** Shifting traffic to the new version */
        TRAFFIC_SHIFTING,
        /** Monitoring the new version for health */
        MONITORING,
        /** Swap completed successfully */
        COMPLETED,
        /** Swap failed */
        FAILED,
        /** Rolling back to previous version */
        ROLLING_BACK,
        /** Rollback completed */
        ROLLBACK_COMPLETED;

        public static SwapStatus fromOrdinal(final int ordinal) {
            final SwapStatus[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PENDING;
        }

        public boolean isTerminal() {
            return this == COMPLETED || this == FAILED || this == ROLLBACK_COMPLETED;
        }

        public boolean isInProgress() {
            return !isTerminal() && this != PENDING;
        }
    }

    /**
     * Component load request for background loading.
     */
    final class LoadRequest {
        private final String componentName;
        private final String componentPath;
        private final String version;
        private final LoadPriority priority;
        private final ValidationConfig validationConfig;

        public LoadRequest(final String componentName, final String componentPath, final String version,
                         final LoadPriority priority, final ValidationConfig validationConfig) {
            this.componentName = componentName;
            this.componentPath = componentPath;
            this.version = version;
            this.priority = priority != null ? priority : LoadPriority.NORMAL;
            this.validationConfig = validationConfig != null ? validationConfig : ValidationConfig.getDefault();
        }

        public void validate() {
            if (componentName == null || componentName.trim().isEmpty()) {
                throw new IllegalArgumentException("Component name cannot be null or empty");
            }
            if (componentPath == null || componentPath.trim().isEmpty()) {
                throw new IllegalArgumentException("Component path cannot be null or empty");
            }
            if (version == null || version.trim().isEmpty()) {
                throw new IllegalArgumentException("Version cannot be null or empty");
            }
        }

        public String getComponentName() { return componentName; }
        public String getComponentPath() { return componentPath; }
        public String getVersion() { return version; }
        public LoadPriority getPriority() { return priority; }
        public ValidationConfig getValidationConfig() { return validationConfig; }

        @Override
        public String toString() {
            return String.format("LoadRequest{name=%s, version=%s, priority=%s}",
                               componentName, version, priority);
        }
    }

    /**
     * Load priority enumeration.
     */
    enum LoadPriority {
        /** Low priority, processed when resources are available */
        LOW,
        /** Normal priority (default) */
        NORMAL,
        /** High priority, processed before normal priority items */
        HIGH,
        /** Critical priority, processed immediately */
        CRITICAL
    }

    /**
     * Validation configuration for component loading.
     */
    final class ValidationConfig {
        private final boolean validateInterfaces;
        private final boolean validateDependencies;
        private final boolean validateSecurity;
        private final boolean validatePerformance;
        private final long timeoutSecs;

        public ValidationConfig(final boolean validateInterfaces, final boolean validateDependencies,
                              final boolean validateSecurity, final boolean validatePerformance,
                              final long timeoutSecs) {
            this.validateInterfaces = validateInterfaces;
            this.validateDependencies = validateDependencies;
            this.validateSecurity = validateSecurity;
            this.validatePerformance = validatePerformance;
            this.timeoutSecs = timeoutSecs;
        }

        /**
         * Creates a default validation configuration with recommended settings.
         *
         * @return A default configuration
         */
        public static ValidationConfig getDefault() {
            return new ValidationConfig(true, true, true, false, 30);
        }

        public boolean isValidateInterfaces() { return validateInterfaces; }
        public boolean isValidateDependencies() { return validateDependencies; }
        public boolean isValidateSecurity() { return validateSecurity; }
        public boolean isValidatePerformance() { return validatePerformance; }
        public long getTimeoutSecs() { return timeoutSecs; }

        @Override
        public String toString() {
            return String.format("ValidationConfig{interfaces=%s, dependencies=%s, security=%s, performance=%s, timeoutSecs=%d}",
                               validateInterfaces, validateDependencies, validateSecurity, validatePerformance, timeoutSecs);
        }
    }

    /**
     * Hot reload performance metrics.
     */
    final class HotReloadMetrics {
        private final long totalSwaps;
        private final long successfulSwaps;
        private final long failedSwaps;
        private final long rollbacks;
        private final long avgSwapTimeMs;
        private final int currentActiveSwaps;
        private final long componentsLoaded;
        private final float cacheEfficiency;

        public HotReloadMetrics(final long totalSwaps, final long successfulSwaps, final long failedSwaps,
                              final long rollbacks, final long avgSwapTimeMs, final int currentActiveSwaps,
                              final long componentsLoaded, final float cacheEfficiency) {
            this.totalSwaps = totalSwaps;
            this.successfulSwaps = successfulSwaps;
            this.failedSwaps = failedSwaps;
            this.rollbacks = rollbacks;
            this.avgSwapTimeMs = avgSwapTimeMs;
            this.currentActiveSwaps = currentActiveSwaps;
            this.componentsLoaded = componentsLoaded;
            this.cacheEfficiency = cacheEfficiency;
        }

        public long getTotalSwaps() { return totalSwaps; }
        public long getSuccessfulSwaps() { return successfulSwaps; }
        public long getFailedSwaps() { return failedSwaps; }
        public long getRollbacks() { return rollbacks; }
        public long getAvgSwapTimeMs() { return avgSwapTimeMs; }
        public int getCurrentActiveSwaps() { return currentActiveSwaps; }
        public long getComponentsLoaded() { return componentsLoaded; }
        public float getCacheEfficiency() { return cacheEfficiency; }

        /**
         * Calculates the success rate as a percentage.
         *
         * @return Success rate between 0.0 and 1.0
         */
        public double getSuccessRate() {
            return totalSwaps > 0 ? (double) successfulSwaps / totalSwaps : 1.0;
        }

        /**
         * Calculates the rollback rate as a percentage.
         *
         * @return Rollback rate between 0.0 and 1.0
         */
        public double getRollbackRate() {
            return totalSwaps > 0 ? (double) rollbacks / totalSwaps : 0.0;
        }

        @Override
        public String toString() {
            return String.format("HotReloadMetrics{totalSwaps=%d, successRate=%.1f%%, avgSwapTime=%dms, " +
                               "activeSwaps=%d, componentsLoaded=%d, cacheEfficiency=%.1f%%, rollbackRate=%.1f%%}",
                               totalSwaps, getSuccessRate() * 100, avgSwapTimeMs, currentActiveSwaps,
                               componentsLoaded, cacheEfficiency * 100, getRollbackRate() * 100);
        }
    }
}