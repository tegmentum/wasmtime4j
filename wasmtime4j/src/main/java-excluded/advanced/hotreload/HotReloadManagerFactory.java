package ai.tegmentum.wasmtime4j.hotreload;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.exception.WasmRuntimeException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.util.logging.Logger;
import java.util.logging.Level;

/**
 * Factory for creating HotReloadManager instances with automatic runtime selection.
 *
 * <p>This factory automatically chooses the best available implementation based on
 * the Java version and system properties:
 * <ul>
 *   <li>Java 23+: Uses Panama FFI implementation (preferred for performance)</li>
 *   <li>Java 8-22: Uses JNI implementation</li>
 *   <li>Manual override: Use system property "wasmtime4j.hotreload.runtime"</li>
 * </ul>
 *
 * <p>System property values:
 * <ul>
 *   <li>"jni" - Force JNI implementation</li>
 *   <li>"panama" - Force Panama implementation (requires Java 23+)</li>
 *   <li>"auto" - Automatic selection (default)</li>
 * </ul>
 *
 * <p>Example usage:
 * <pre>{@code
 * // Automatic selection
 * Engine engine = Engine.newEngine();
 * HotReloadManager manager = HotReloadManagerFactory.createManager(engine, config);
 *
 * // Force JNI (for testing or compatibility)
 * System.setProperty("wasmtime4j.hotreload.runtime", "jni");
 * HotReloadManager jniManager = HotReloadManagerFactory.createManager(engine, config);
 * }</pre>
 */
final class HotReloadManagerFactory {

    private static final Logger logger = Logger.getLogger(HotReloadManagerFactory.class.getName());

    private static final String RUNTIME_PROPERTY = "wasmtime4j.hotreload.runtime";
    private static final String RUNTIME_JNI = "jni";
    private static final String RUNTIME_PANAMA = "panama";
    private static final String RUNTIME_AUTO = "auto";

    // Java version detection
    private static final int JAVA_VERSION = getJavaVersion();
    private static final boolean PANAMA_AVAILABLE = JAVA_VERSION >= 23;

    private HotReloadManagerFactory() {
        // Utility class
    }

    /**
     * Creates a new hot reload manager with automatic runtime selection.
     *
     * @param engine The WebAssembly engine to use
     * @param config The hot reload configuration
     * @return A new hot reload manager instance
     * @throws IllegalArgumentException if engine or config is null
     * @throws WasmRuntimeException if no suitable implementation is available
     */
    static HotReloadManager createManager(final Engine engine, final HotReloadManager.HotReloadConfig config) {
        if (engine == null) {
            throw new IllegalArgumentException("Engine cannot be null");
        }
        if (config == null) {
            throw new IllegalArgumentException("Config cannot be null");
        }

        final String runtimePreference = System.getProperty(RUNTIME_PROPERTY, RUNTIME_AUTO);
        final RuntimeType selectedRuntime = determineRuntime(runtimePreference);

        logger.info(String.format("Creating hot reload manager with runtime: %s (Java %d)",
                                selectedRuntime, JAVA_VERSION));

        try {
            switch (selectedRuntime) {
                case JNI:
                    return createJniManager(engine, config);
                case PANAMA:
                    return createPanamaManager(engine, config);
                default:
                    throw new WasmRuntimeException("Unexpected runtime type: " + selectedRuntime);
            }
        } catch (final Exception e) {
            throw new WasmRuntimeException("Failed to create hot reload manager with runtime: " + selectedRuntime, e);
        }
    }

    /**
     * Determines which runtime to use based on preferences and availability.
     */
    private static RuntimeType determineRuntime(final String preference) {
        switch (preference.toLowerCase()) {
            case RUNTIME_JNI:
                logger.fine("JNI runtime explicitly requested");
                return RuntimeType.JNI;

            case RUNTIME_PANAMA:
                if (!PANAMA_AVAILABLE) {
                    logger.warning(String.format("Panama runtime requested but not available (Java %d < 23), falling back to JNI", JAVA_VERSION));
                    return RuntimeType.JNI;
                }
                logger.fine("Panama runtime explicitly requested");
                return RuntimeType.PANAMA;

            case RUNTIME_AUTO:
            default:
                if (PANAMA_AVAILABLE) {
                    logger.fine("Auto-selecting Panama runtime for optimal performance");
                    return RuntimeType.PANAMA;
                } else {
                    logger.fine("Auto-selecting JNI runtime for compatibility");
                    return RuntimeType.JNI;
                }
        }
    }

    /**
     * Creates a JNI-based hot reload manager.
     */
    private static HotReloadManager createJniManager(final Engine engine, final HotReloadManager.HotReloadConfig config) {
        try {
            // Use reflection to avoid compile-time dependency on JNI implementation
            final Class<?> jniEngineClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniEngine");
            final Class<?> jniManagerClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniHotReloadManager");
            final Class<?> jniConfigClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniHotReloadManager$HotReloadConfig");

            // Get the underlying JNI engine
            Object jniEngine = WasmRuntimeFactory.getUnderlyingEngine(engine);
            if (!jniEngineClass.isInstance(jniEngine)) {
                throw new WasmRuntimeException("Engine is not a JNI engine: " + jniEngine.getClass());
            }

            // Convert the configuration
            final Object jniConfig = convertConfigToJni(config, jniConfigClass);

            // Create the JNI hot reload manager
            final Object jniManager = jniManagerClass.getConstructor(jniEngineClass, jniConfigClass)
                    .newInstance(jniEngine, jniConfig);

            return new HotReloadManagerWrapper(jniManager, RuntimeType.JNI);

        } catch (final Exception e) {
            throw new WasmRuntimeException("Failed to create JNI hot reload manager", e);
        }
    }

    /**
     * Creates a Panama-based hot reload manager.
     */
    private static HotReloadManager createPanamaManager(final Engine engine, final HotReloadManager.HotReloadConfig config) {
        try {
            // Use reflection to avoid compile-time dependency on Panama implementation
            final Class<?> panamaEngineClass = Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaEngine");
            final Class<?> panamaManagerClass = Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaHotReloadManager");
            final Class<?> panamaConfigClass = Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaHotReloadManager$HotReloadConfig");

            // Get the underlying Panama engine
            Object panamaEngine = WasmRuntimeFactory.getUnderlyingEngine(engine);
            if (!panamaEngineClass.isInstance(panamaEngine)) {
                throw new WasmRuntimeException("Engine is not a Panama engine: " + panamaEngine.getClass());
            }

            // Convert the configuration
            final Object panamaConfig = convertConfigToPanama(config, panamaConfigClass);

            // Create the Panama hot reload manager
            final Object panamaManager = panamaManagerClass.getConstructor(panamaEngineClass, panamaConfigClass)
                    .newInstance(panamaEngine, panamaConfig);

            return new HotReloadManagerWrapper(panamaManager, RuntimeType.PANAMA);

        } catch (final Exception e) {
            throw new WasmRuntimeException("Failed to create Panama hot reload manager", e);
        }
    }

    /**
     * Converts the public configuration to JNI-specific configuration.
     */
    private static Object convertConfigToJni(final HotReloadManager.HotReloadConfig config, final Class<?> jniConfigClass) {
        try {
            final Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.jni.JniHotReloadManager$HotReloadConfig$Builder");

            Object builder = jniConfigClass.getMethod("builder").invoke(null);
            builder = builderClass.getMethod("validationEnabled", boolean.class).invoke(builder, config.isValidationEnabled());
            builder = builderClass.getMethod("statePreservationEnabled", boolean.class).invoke(builder, config.isStatePreservationEnabled());
            builder = builderClass.getMethod("debounceDelayMs", long.class).invoke(builder, config.getDebounceDelayMs());
            builder = builderClass.getMethod("precompilationEnabled", boolean.class).invoke(builder, config.isPrecompilationEnabled());
            builder = builderClass.getMethod("maxReloadAttempts", int.class).invoke(builder, config.getMaxReloadAttempts());
            builder = builderClass.getMethod("healthCheckIntervalSecs", long.class).invoke(builder, config.getHealthCheckIntervalSecs());
            builder = builderClass.getMethod("loaderThreadCount", int.class).invoke(builder, config.getLoaderThreadCount());
            builder = builderClass.getMethod("cacheSize", int.class).invoke(builder, config.getCacheSize());

            return builderClass.getMethod("build").invoke(builder);

        } catch (final Exception e) {
            throw new WasmRuntimeException("Failed to convert configuration for JNI", e);
        }
    }

    /**
     * Converts the public configuration to Panama-specific configuration.
     */
    private static Object convertConfigToPanama(final HotReloadManager.HotReloadConfig config, final Class<?> panamaConfigClass) {
        try {
            final Class<?> builderClass = Class.forName("ai.tegmentum.wasmtime4j.panama.PanamaHotReloadManager$HotReloadConfig$Builder");

            Object builder = panamaConfigClass.getMethod("builder").invoke(null);
            builder = builderClass.getMethod("validationEnabled", boolean.class).invoke(builder, config.isValidationEnabled());
            builder = builderClass.getMethod("statePreservationEnabled", boolean.class).invoke(builder, config.isStatePreservationEnabled());
            builder = builderClass.getMethod("debounceDelayMs", long.class).invoke(builder, config.getDebounceDelayMs());
            builder = builderClass.getMethod("precompilationEnabled", boolean.class).invoke(builder, config.isPrecompilationEnabled());
            builder = builderClass.getMethod("maxReloadAttempts", int.class).invoke(builder, config.getMaxReloadAttempts());
            builder = builderClass.getMethod("healthCheckIntervalSecs", long.class).invoke(builder, config.getHealthCheckIntervalSecs());
            builder = builderClass.getMethod("loaderThreadCount", int.class).invoke(builder, config.getLoaderThreadCount());
            builder = builderClass.getMethod("cacheSize", int.class).invoke(builder, config.getCacheSize());

            return builderClass.getMethod("build").invoke(builder);

        } catch (final Exception e) {
            throw new WasmRuntimeException("Failed to convert configuration for Panama", e);
        }
    }

    /**
     * Gets the current Java version.
     */
    private static int getJavaVersion() {
        final String version = System.getProperty("java.version");
        if (version.startsWith("1.")) {
            return Integer.parseInt(version.substring(2, 3));
        } else {
            final int dotIndex = version.indexOf('.');
            if (dotIndex != -1) {
                return Integer.parseInt(version.substring(0, dotIndex));
            } else {
                return Integer.parseInt(version);
            }
        }
    }

    /**
     * Runtime type enumeration.
     */
    private enum RuntimeType {
        JNI,
        PANAMA
    }

    /**
     * Wrapper that delegates to the actual implementation while providing a unified interface.
     */
    private static final class HotReloadManagerWrapper implements HotReloadManager {

        private final Object delegate;
        private final RuntimeType runtimeType;

        private HotReloadManagerWrapper(final Object delegate, final RuntimeType runtimeType) {
            this.delegate = delegate;
            this.runtimeType = runtimeType;
        }

        @Override
        public String startHotSwap(final String componentName, final String targetVersion, final SwapStrategy strategy) {
            try {
                // Convert strategy to implementation-specific format
                final Object implStrategy = convertSwapStrategy(strategy);

                return (String) delegate.getClass()
                        .getMethod("startHotSwap", String.class, String.class, getSwapStrategyClass())
                        .invoke(delegate, componentName, targetVersion, implStrategy);

            } catch (final Exception e) {
                throw new WasmRuntimeException("Failed to start hot swap", e);
            }
        }

        @Override
        public HotSwapStatus getSwapStatus(final String operationId) {
            try {
                final Object status = delegate.getClass()
                        .getMethod("getSwapStatus", String.class)
                        .invoke(delegate, operationId);

                return status != null ? convertHotSwapStatus(status) : null;

            } catch (final Exception e) {
                throw new WasmRuntimeException("Failed to get swap status", e);
            }
        }

        @Override
        public boolean cancelHotSwap(final String operationId) {
            try {
                return (Boolean) delegate.getClass()
                        .getMethod("cancelHotSwap", String.class)
                        .invoke(delegate, operationId);

            } catch (final Exception e) {
                throw new WasmRuntimeException("Failed to cancel hot swap", e);
            }
        }

        @Override
        public java.util.concurrent.CompletableFuture<String> loadComponentAsync(final LoadRequest request) {
            try {
                // Convert load request to implementation-specific format
                final Object implRequest = convertLoadRequest(request);

                @SuppressWarnings("unchecked")
                final java.util.concurrent.CompletableFuture<String> result =
                        (java.util.concurrent.CompletableFuture<String>) delegate.getClass()
                                .getMethod("loadComponentAsync", getLoadRequestClass())
                                .invoke(delegate, implRequest);

                return result;

            } catch (final Exception e) {
                throw new WasmRuntimeException("Failed to load component async", e);
            }
        }

        @Override
        public HotReloadMetrics getMetrics() {
            try {
                final Object metrics = delegate.getClass()
                        .getMethod("getMetrics")
                        .invoke(delegate);

                return convertHotReloadMetrics(metrics);

            } catch (final Exception e) {
                throw new WasmRuntimeException("Failed to get metrics", e);
            }
        }

        @Override
        public boolean isClosed() {
            try {
                return (Boolean) delegate.getClass()
                        .getMethod("isClosed")
                        .invoke(delegate);

            } catch (final Exception e) {
                logger.log(Level.WARNING, "Failed to check closed status", e);
                return true; // Assume closed on error
            }
        }

        @Override
        public void close() {
            try {
                delegate.getClass()
                        .getMethod("close")
                        .invoke(delegate);

            } catch (final Exception e) {
                logger.log(Level.WARNING, "Error closing hot reload manager", e);
            }
        }

        @Override
        public String toString() {
            return String.format("HotReloadManagerWrapper{runtime=%s, delegate=%s}", runtimeType, delegate);
        }

        // Helper methods for converting between API types and implementation types

        private Object convertSwapStrategy(final SwapStrategy strategy) throws Exception {
            if (strategy == null) {
                return null;
            }

            final Class<?> strategyClass = getSwapStrategyClass();

            switch (strategy.getType()) {
                case 0: // Immediate
                    return strategyClass.getMethod("immediate").invoke(null);
                case 1: // Canary
                    return strategyClass.getMethod("canary", float.class, float.class, float.class)
                            .invoke(null, strategy.getParam1() / 100.0f, strategy.getParam2() / 100.0f, (float) strategy.getParam3());
                case 2: // Blue-green
                    return strategyClass.getMethod("blueGreen").invoke(null);
                case 3: // Rolling update
                    return strategyClass.getMethod("rollingUpdate", int.class, long.class)
                            .invoke(null, (int) strategy.getParam1(), strategy.getParam2());
                case 4: // A/B test
                    return strategyClass.getMethod("abTest", float.class, long.class)
                            .invoke(null, strategy.getParam1() / 100.0f, strategy.getParam2());
                default:
                    return strategyClass.getMethod("getDefault").invoke(null);
            }
        }

        private Object convertLoadRequest(final LoadRequest request) throws Exception {
            final Class<?> requestClass = getLoadRequestClass();
            final Class<?> priorityClass = getLoadPriorityClass();
            final Class<?> validationClass = getValidationConfigClass();

            // Convert priority
            final Object priority = priorityClass.getMethod("valueOf", String.class)
                    .invoke(null, request.getPriority().name());

            // Convert validation config
            final Object validation = validationClass.getConstructor(
                    boolean.class, boolean.class, boolean.class, boolean.class, long.class)
                    .newInstance(
                            request.getValidationConfig().isValidateInterfaces(),
                            request.getValidationConfig().isValidateDependencies(),
                            request.getValidationConfig().isValidateSecurity(),
                            request.getValidationConfig().isValidatePerformance(),
                            request.getValidationConfig().getTimeoutSecs()
                    );

            return requestClass.getConstructor(String.class, String.class, String.class, priorityClass, validationClass)
                    .newInstance(request.getComponentName(), request.getComponentPath(),
                               request.getVersion(), priority, validation);
        }

        private HotSwapStatus convertHotSwapStatus(final Object status) throws Exception {
            final String operationId = (String) status.getClass().getMethod("getOperationId").invoke(status);
            final String componentName = (String) status.getClass().getMethod("getComponentName").invoke(status);

            // Try to get version info (may not be available in all implementations)
            String fromVersion = null;
            String toVersion = null;
            try {
                fromVersion = (String) status.getClass().getMethod("getFromVersion").invoke(status);
                toVersion = (String) status.getClass().getMethod("getToVersion").invoke(status);
            } catch (final Exception e) {
                // These methods may not exist in all implementations
                fromVersion = "unknown";
                toVersion = "unknown";
            }

            final Object statusEnum = status.getClass().getMethod("getStatus").invoke(status);
            final SwapStatus swapStatus = SwapStatus.valueOf(statusEnum.toString());

            final Float progress = (Float) status.getClass().getMethod("getProgress").invoke(status);

            return new HotSwapStatus(operationId, componentName, fromVersion, toVersion, swapStatus, progress);
        }

        private HotReloadMetrics convertHotReloadMetrics(final Object metrics) throws Exception {
            final Long totalSwaps = (Long) metrics.getClass().getMethod("getTotalSwaps").invoke(metrics);
            final Long successfulSwaps = (Long) metrics.getClass().getMethod("getSuccessfulSwaps").invoke(metrics);
            final Long failedSwaps = (Long) metrics.getClass().getMethod("getFailedSwaps").invoke(metrics);
            final Long rollbacks = (Long) metrics.getClass().getMethod("getRollbacks").invoke(metrics);
            final Long avgSwapTimeMs = (Long) metrics.getClass().getMethod("getAvgSwapTimeMs").invoke(metrics);
            final Integer currentActiveSwaps = (Integer) metrics.getClass().getMethod("getCurrentActiveSwaps").invoke(metrics);
            final Long componentsLoaded = (Long) metrics.getClass().getMethod("getComponentsLoaded").invoke(metrics);
            final Float cacheEfficiency = (Float) metrics.getClass().getMethod("getCacheEfficiency").invoke(metrics);

            return new HotReloadMetrics(totalSwaps, successfulSwaps, failedSwaps, rollbacks,
                                      avgSwapTimeMs, currentActiveSwaps, componentsLoaded, cacheEfficiency);
        }

        private Class<?> getSwapStrategyClass() throws ClassNotFoundException {
            final String packageName = runtimeType == RuntimeType.JNI ? "ai.tegmentum.wasmtime4j.jni" : "ai.tegmentum.wasmtime4j.panama";
            final String className = runtimeType == RuntimeType.JNI ? "JniHotReloadManager" : "PanamaHotReloadManager";
            return Class.forName(packageName + "." + className + "$SwapStrategy");
        }

        private Class<?> getLoadRequestClass() throws ClassNotFoundException {
            final String packageName = runtimeType == RuntimeType.JNI ? "ai.tegmentum.wasmtime4j.jni" : "ai.tegmentum.wasmtime4j.panama";
            final String className = runtimeType == RuntimeType.JNI ? "JniHotReloadManager" : "PanamaHotReloadManager";
            return Class.forName(packageName + "." + className + "$LoadRequest");
        }

        private Class<?> getLoadPriorityClass() throws ClassNotFoundException {
            final String packageName = runtimeType == RuntimeType.JNI ? "ai.tegmentum.wasmtime4j.jni" : "ai.tegmentum.wasmtime4j.panama";
            final String className = runtimeType == RuntimeType.JNI ? "JniHotReloadManager" : "PanamaHotReloadManager";
            return Class.forName(packageName + "." + className + "$LoadPriority");
        }

        private Class<?> getValidationConfigClass() throws ClassNotFoundException {
            final String packageName = runtimeType == RuntimeType.JNI ? "ai.tegmentum.wasmtime4j.jni" : "ai.tegmentum.wasmtime4j.panama";
            final String className = runtimeType == RuntimeType.JNI ? "JniHotReloadManager" : "PanamaHotReloadManager";
            return Class.forName(packageName + "." + className + "$ValidationConfig");
        }
    }
}