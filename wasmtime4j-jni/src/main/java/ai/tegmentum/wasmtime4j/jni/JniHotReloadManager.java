package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.Objects;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * JNI implementation of hot reload manager for dynamic WebAssembly module updates.
 *
 * <p>This class provides comprehensive hot-reload capabilities including:
 * <ul>
 *   <li>Live module replacement without service interruption</li>
 *   <li>State migration and preservation during updates</li>
 *   <li>Version compatibility checking and rollback mechanisms</li>
 *   <li>Background component loading and validation</li>
 *   <li>Multiple deployment strategies (canary, blue-green, rolling)</li>
 *   <li>Health monitoring and automatic rollback</li>
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
 * try (JniHotReloadManager manager = new JniHotReloadManager(engine, config)) {
 *     // Start a canary deployment
 *     String operationId = manager.startHotSwap("my-component", "2.0.0",
 *         SwapStrategy.canary(10.0f, 25.0f, 0.99f));
 *
 *     // Monitor the operation
 *     HotSwapStatus status = manager.getSwapStatus(operationId);
 *     System.out.println("Progress: " + status.getProgress());
 * }
 * }</pre>
 *
 * <p><strong>Thread Safety:</strong> This class is thread-safe and can be used concurrently
 * from multiple threads.
 *
 * <p><strong>Resource Management:</strong> This class implements {@link AutoCloseable}
 * and should be used with try-with-resources to ensure proper cleanup.
 */
public final class JniHotReloadManager implements AutoCloseable {

    private static final Logger logger = Logger.getLogger(JniHotReloadManager.class.getName());

    private final long nativeHandle;
    private volatile boolean closed = false;

    /**
     * Creates a new hot reload manager with the specified engine and configuration.
     *
     * @param engine The WebAssembly engine to use for module compilation
     * @param config The hot reload configuration
     * @throws IllegalArgumentException if engine or config is null
     * @throws WasmRuntimeException if the native manager cannot be created
     */
    public JniHotReloadManager(final JniEngine engine, final HotReloadConfig config) {
        if (engine == null) {
            throw new IllegalArgumentException("Engine cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        logger.fine("Creating hot reload manager");

        this.nativeHandle = nativeCreateHotReloadManager(
            engine.getNativeHandle(),
            config.isValidationEnabled(),
            config.isStatePreservationEnabled(),
            config.getDebounceDelayMs(),
            config.isPrecompilationEnabled(),
            config.getMaxReloadAttempts(),
            config.getHealthCheckIntervalSecs(),
            config.getLoaderThreadCount(),
            config.getCacheSize()
        );

        if (this.nativeHandle == 0) {
            throw new WasmRuntimeException("Failed to create native hot reload manager");
        }

        logger.info("Hot reload manager created successfully");
    }

    /**
     * Starts a hot swap operation to replace the current component version.
     *
     * @param componentName The name of the component to swap
     * @param targetVersion The target version to swap to
     * @param strategy The swap strategy to use (null for default)
     * @return A unique operation ID for tracking the swap
     * @throws IllegalArgumentException if componentName or targetVersion is null/empty
     * @throws WasmRuntimeException if the swap cannot be started
     * @throws IllegalStateException if this manager has been closed
     */
    public String startHotSwap(final String componentName, final String targetVersion,
                              final SwapStrategy strategy) {
        JniValidation.checkNotClosed(closed, "Hot reload manager");

        if (componentName == null || componentName.trim().isEmpty()) {
            throw new IllegalArgumentException("Component name cannot be null or empty");
        }
        if (targetVersion == null || targetVersion.trim().isEmpty()) {
            throw new IllegalArgumentException("Target version cannot be null or empty");
        }

        logger.info(String.format("Starting hot swap: %s -> %s", componentName, targetVersion));

        final SwapStrategy actualStrategy = strategy != null ? strategy : SwapStrategy.getDefault();

        final String operationId = nativeStartHotSwap(
            nativeHandle,
            componentName,
            targetVersion,
            actualStrategy.getType(),
            actualStrategy.getParam1(),
            actualStrategy.getParam2(),
            actualStrategy.getParam3()
        );

        if (operationId == null) {
            throw new WasmRuntimeException("Failed to start hot swap operation");
        }

        logger.info(String.format("Hot swap operation started: %s", operationId));
        return operationId;
    }

    /**
     * Gets the current status of a hot swap operation.
     *
     * @param operationId The operation ID returned by startHotSwap
     * @return The current status of the operation, or null if not found
     * @throws IllegalArgumentException if operationId is null/empty
     * @throws IllegalStateException if this manager has been closed
     */
    public HotSwapStatus getSwapStatus(final String operationId) {
        JniValidation.checkNotClosed(closed, "Hot reload manager");

        if (operationId == null || operationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation ID cannot be null or empty");
        }

        return nativeGetSwapStatus(nativeHandle, operationId);
    }

    /**
     * Cancels a hot swap operation if it's still in progress.
     *
     * @param operationId The operation ID to cancel
     * @return true if the operation was cancelled, false if it couldn't be cancelled
     * @throws IllegalArgumentException if operationId is null/empty
     * @throws IllegalStateException if this manager has been closed
     */
    public boolean cancelHotSwap(final String operationId) {
        JniValidation.checkNotClosed(closed, "Hot reload manager");

        if (operationId == null || operationId.trim().isEmpty()) {
            throw new IllegalArgumentException("Operation ID cannot be null or empty");
        }

        logger.info(String.format("Cancelling hot swap operation: %s", operationId));

        final boolean result = nativeCancelHotSwap(nativeHandle, operationId);

        if (result) {
            logger.info(String.format("Hot swap operation cancelled: %s", operationId));
        } else {
            logger.warning(String.format("Failed to cancel hot swap operation: %s", operationId));
        }

        return result;
    }

    /**
     * Loads a component asynchronously in the background for faster hot swaps.
     *
     * @param request The load request containing component details
     * @return A CompletableFuture that completes with the request ID
     * @throws IllegalArgumentException if request is null or invalid
     * @throws IllegalStateException if this manager has been closed
     */
    public CompletableFuture<String> loadComponentAsync(final LoadRequest request) {
        JniValidation.checkNotClosed(closed, "Hot reload manager");

        if (request == null) {
            throw new IllegalArgumentException("Load request cannot be null");
        }
        request.validate();

        return CompletableFuture.supplyAsync(() -> {
            try {
                logger.fine(String.format("Loading component asynchronously: %s", request.getComponentName()));

                final String requestId = nativeLoadComponentAsync(
                    nativeHandle,
                    request.getComponentName(),
                    request.getComponentPath(),
                    request.getVersion(),
                    request.getPriority().ordinal(),
                    request.getValidationConfig().isValidateInterfaces(),
                    request.getValidationConfig().isValidateDependencies(),
                    request.getValidationConfig().isValidateSecurity(),
                    request.getValidationConfig().isValidatePerformance(),
                    request.getValidationConfig().getTimeoutSecs()
                );

                if (requestId == null) {
                    throw new WasmRuntimeException("Failed to submit component load request");
                }

                logger.fine(String.format("Component load request submitted: %s", requestId));
                return requestId;
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Failed to load component asynchronously", e);
                throw new CompletionException(e);
            }
        });
    }

    /**
     * Gets the current hot reload metrics and performance statistics.
     *
     * @return The current metrics
     * @throws IllegalStateException if this manager has been closed
     */
    public HotReloadMetrics getMetrics() {
        JniValidation.checkNotClosed(closed, "Hot reload manager");

        final HotReloadMetrics metrics = nativeGetMetrics(nativeHandle);
        if (metrics == null) {
            throw new WasmRuntimeException("Failed to retrieve hot reload metrics");
        }

        return metrics;
    }

    /**
     * Gets the native handle for this hot reload manager.
     *
     * @return The native handle
     * @throws IllegalStateException if this manager has been closed
     */
    public long getNativeHandle() {
        JniValidation.checkNotClosed(closed, "Hot reload manager");
        return nativeHandle;
    }

    /**
     * Checks if this hot reload manager has been closed.
     *
     * @return true if closed, false otherwise
     */
    public boolean isClosed() {
        return closed;
    }

    @Override
    public void close() {
        if (!closed) {
            closed = true;

            logger.fine("Closing hot reload manager");

            try {
                nativeDestroyHotReloadManager(nativeHandle);
                logger.info("Hot reload manager closed successfully");
            } catch (final Exception e) {
                logger.log(Level.WARNING, "Error closing hot reload manager", e);
            }
        }
    }

    @Override
    public String toString() {
        return String.format("JniHotReloadManager{handle=%d, closed=%s}", nativeHandle, closed);
    }

    @Override
    public boolean equals(final Object obj) {
        if (this == obj) return true;
        if (!(obj instanceof JniHotReloadManager)) return false;
        final JniHotReloadManager other = (JniHotReloadManager) obj;
        return nativeHandle == other.nativeHandle;
    }

    @Override
    public int hashCode() {
        return Objects.hash(nativeHandle);
    }

    // Native methods

    private static native long nativeCreateHotReloadManager(
        long engineHandle,
        boolean validationEnabled,
        boolean statePreservationEnabled,
        long debounceDelayMs,
        boolean precompilationEnabled,
        int maxReloadAttempts,
        long healthCheckIntervalSecs,
        int loaderThreadCount,
        int cacheSize
    );

    private static native void nativeDestroyHotReloadManager(long handle);

    private static native String nativeStartHotSwap(
        long handle,
        String componentName,
        String versionString,
        int swapStrategyType,
        long strategyParam1,
        long strategyParam2,
        double strategyParam3
    );

    private static native HotSwapStatus nativeGetSwapStatus(long handle, String operationId);

    private static native boolean nativeCancelHotSwap(long handle, String operationId);

    private static native String nativeLoadComponentAsync(
        long handle,
        String componentName,
        String componentPath,
        String versionString,
        int priority,
        boolean validateInterfaces,
        boolean validateDependencies,
        boolean validateSecurity,
        boolean validatePerformance,
        long timeoutSecs
    );

    private static native HotReloadMetrics nativeGetMetrics(long handle);

    // Static classes for configuration and data transfer

    /**
     * Configuration for hot reload behavior.
     */
    public static final class HotReloadConfig {
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
         * @return A default configuration
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

            public Builder validationEnabled(final boolean enabled) {
                this.validationEnabled = enabled;
                return this;
            }

            public Builder statePreservationEnabled(final boolean enabled) {
                this.statePreservationEnabled = enabled;
                return this;
            }

            public Builder debounceDelayMs(final long delayMs) {
                if (delayMs < 0) {
                    throw new IllegalArgumentException("Debounce delay must be non-negative");
                }
                this.debounceDelayMs = delayMs;
                return this;
            }

            public Builder precompilationEnabled(final boolean enabled) {
                this.precompilationEnabled = enabled;
                return this;
            }

            public Builder maxReloadAttempts(final int attempts) {
                if (attempts < 1) {
                    throw new IllegalArgumentException("Max reload attempts must be positive");
                }
                this.maxReloadAttempts = attempts;
                return this;
            }

            public Builder healthCheckIntervalSecs(final long intervalSecs) {
                if (intervalSecs < 1) {
                    throw new IllegalArgumentException("Health check interval must be positive");
                }
                this.healthCheckIntervalSecs = intervalSecs;
                return this;
            }

            public Builder loaderThreadCount(final int threadCount) {
                if (threadCount < 1) {
                    throw new IllegalArgumentException("Loader thread count must be positive");
                }
                this.loaderThreadCount = threadCount;
                return this;
            }

            public Builder cacheSize(final int size) {
                if (size < 1) {
                    throw new IllegalArgumentException("Cache size must be positive");
                }
                this.cacheSize = size;
                return this;
            }

            public HotReloadConfig build() {
                return new HotReloadConfig(this);
            }
        }
    }

    /**
     * Hot swap deployment strategies.
     */
    public static abstract class SwapStrategy {
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
         * Creates an immediate swap strategy.
         */
        public static SwapStrategy immediate() {
            return new SwapStrategy(0, 0, 0, 0.0) {};
        }

        /**
         * Creates a canary deployment strategy.
         */
        public static SwapStrategy canary(final float initialPercentage, final float incrementPercentage,
                                        final float successThreshold) {
            return new SwapStrategy(1, (long)(initialPercentage * 100), (long)(incrementPercentage * 100), successThreshold) {};
        }

        /**
         * Creates a blue-green deployment strategy.
         */
        public static SwapStrategy blueGreen() {
            return new SwapStrategy(2, 0, 0, 0.0) {};
        }

        /**
         * Creates a rolling update strategy.
         */
        public static SwapStrategy rollingUpdate(final int batchSize, final long batchIntervalSecs) {
            return new SwapStrategy(3, batchSize, batchIntervalSecs, 0.0) {};
        }

        /**
         * Creates an A/B test strategy.
         */
        public static SwapStrategy abTest(final float testPercentage, final long testDurationSecs) {
            return new SwapStrategy(4, (long)(testPercentage * 100), testDurationSecs, 0.0) {};
        }

        /**
         * Gets the default swap strategy.
         */
        public static SwapStrategy getDefault() {
            return canary(10.0f, 25.0f, 0.99f);
        }
    }

    /**
     * Status of a hot swap operation.
     */
    public static final class HotSwapStatus {
        private final String operationId;
        private final String componentName;
        private final int status;
        private final float progress;

        // Constructor called from native code
        public HotSwapStatus(final String operationId, final String componentName,
                           final int status, final float progress) {
            this.operationId = operationId;
            this.componentName = componentName;
            this.status = status;
            this.progress = progress;
        }

        public String getOperationId() { return operationId; }
        public String getComponentName() { return componentName; }
        public SwapStatus getStatus() { return SwapStatus.fromOrdinal(status); }
        public float getProgress() { return progress; }

        @Override
        public String toString() {
            return String.format("HotSwapStatus{id=%s, component=%s, status=%s, progress=%.1f%%}",
                               operationId, componentName, getStatus(), progress * 100);
        }
    }

    /**
     * Hot swap operation status enumeration.
     */
    public enum SwapStatus {
        PENDING,
        PRE_LOADING,
        VALIDATING,
        STARTING,
        TRAFFIC_SHIFTING,
        MONITORING,
        COMPLETED,
        FAILED,
        ROLLING_BACK,
        ROLLBACK_COMPLETED;

        public static SwapStatus fromOrdinal(final int ordinal) {
            final SwapStatus[] values = values();
            return ordinal >= 0 && ordinal < values.length ? values[ordinal] : PENDING;
        }
    }

    /**
     * Component load request for background loading.
     */
    public static final class LoadRequest {
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

        // Getters
        public String getComponentName() { return componentName; }
        public String getComponentPath() { return componentPath; }
        public String getVersion() { return version; }
        public LoadPriority getPriority() { return priority; }
        public ValidationConfig getValidationConfig() { return validationConfig; }
    }

    /**
     * Load priority enumeration.
     */
    public enum LoadPriority {
        LOW, NORMAL, HIGH, CRITICAL
    }

    /**
     * Validation configuration for component loading.
     */
    public static final class ValidationConfig {
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

        public static ValidationConfig getDefault() {
            return new ValidationConfig(true, true, true, false, 30);
        }

        // Getters
        public boolean isValidateInterfaces() { return validateInterfaces; }
        public boolean isValidateDependencies() { return validateDependencies; }
        public boolean isValidateSecurity() { return validateSecurity; }
        public boolean isValidatePerformance() { return validatePerformance; }
        public long getTimeoutSecs() { return timeoutSecs; }
    }

    /**
     * Hot reload performance metrics.
     */
    public static final class HotReloadMetrics {
        private final long totalSwaps;
        private final long successfulSwaps;
        private final long failedSwaps;
        private final long rollbacks;
        private final long avgSwapTimeMs;
        private final int currentActiveSwaps;
        private final long componentsLoaded;
        private final float cacheEfficiency;

        // Constructor called from native code
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

        // Getters
        public long getTotalSwaps() { return totalSwaps; }
        public long getSuccessfulSwaps() { return successfulSwaps; }
        public long getFailedSwaps() { return failedSwaps; }
        public long getRollbacks() { return rollbacks; }
        public long getAvgSwapTimeMs() { return avgSwapTimeMs; }
        public int getCurrentActiveSwaps() { return currentActiveSwaps; }
        public long getComponentsLoaded() { return componentsLoaded; }
        public float getCacheEfficiency() { return cacheEfficiency; }

        public double getSuccessRate() {
            return totalSwaps > 0 ? (double) successfulSwaps / totalSwaps : 1.0;
        }

        @Override
        public String toString() {
            return String.format("HotReloadMetrics{totalSwaps=%d, successRate=%.1f%%, avgSwapTime=%dms, " +
                               "activeSwaps=%d, componentsLoaded=%d, cacheEfficiency=%.1f%%}",
                               totalSwaps, getSuccessRate() * 100, avgSwapTimeMs, currentActiveSwaps,
                               componentsLoaded, cacheEfficiency * 100);
        }
    }
}