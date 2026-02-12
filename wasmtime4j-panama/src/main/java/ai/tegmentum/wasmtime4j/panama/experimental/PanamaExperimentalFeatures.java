package ai.tegmentum.wasmtime4j.panama.experimental;

import ai.tegmentum.wasmtime4j.experimental.ExperimentalFeature;
import ai.tegmentum.wasmtime4j.experimental.ExperimentalFeatureConfig;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama Foreign Function API implementation for experimental WebAssembly features.
 *
 * <p><b>WARNING:</b> This class provides access to highly experimental features that are unstable
 * and subject to change. Use only for testing, development, and research.
 *
 * <p>This implementation uses Panama FFI (Java 23+) for native interop.
 *
 * @since 1.0.0
 */
public final class PanamaExperimentalFeatures implements AutoCloseable {

  private static final Logger LOGGER = Logger.getLogger(PanamaExperimentalFeatures.class.getName());

  // Native library and linker
  private static final SymbolLookup NATIVE_LOOKUP;
  private static final Linker NATIVE_LINKER = Linker.nativeLinker();

  // Memory arena for native resources
  private final Arena arena = Arena.ofConfined();

  // Native function handles
  private static final MethodHandle CREATE_EXPERIMENTAL_FEATURES;
  private static final MethodHandle ENABLE_EXPERIMENTAL_FEATURE;
  private static final MethodHandle DISABLE_EXPERIMENTAL_FEATURE;
  private static final MethodHandle CONFIGURE_STACK_SWITCHING;
  private static final MethodHandle CONFIGURE_CALL_CC;
  private static final MethodHandle CONFIGURE_ADVANCED_SECURITY;
  private static final MethodHandle CONFIGURE_ADVANCED_PROFILING;
  private static final MethodHandle START_PROFILING;
  private static final MethodHandle STOP_PROFILING;
  private static final MethodHandle GET_PROFILING_RESULTS;
  private static final MethodHandle DESTROY_EXPERIMENTAL_FEATURES;

  // Function signatures
  private static final FunctionDescriptor CREATE_FEATURES_DESC =
      FunctionDescriptor.of(ValueLayout.ADDRESS);
  private static final FunctionDescriptor ENABLE_FEATURE_DESC =
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS);
  private static final FunctionDescriptor DISABLE_FEATURE_DESC =
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.ADDRESS);
  private static final FunctionDescriptor CONFIGURE_STACK_SWITCHING_DESC =
      FunctionDescriptor.ofVoid(
          ValueLayout.ADDRESS, ValueLayout.JAVA_LONG, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
  private static final FunctionDescriptor CONFIGURE_CALL_CC_DESC =
      FunctionDescriptor.ofVoid(
          ValueLayout.ADDRESS, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, ValueLayout.JAVA_INT);
  private static final FunctionDescriptor CONFIGURE_SECURITY_DESC =
      FunctionDescriptor.ofVoid(
          ValueLayout.ADDRESS,
          ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT);
  private static final FunctionDescriptor CONFIGURE_PROFILING_DESC =
      FunctionDescriptor.ofVoid(
          ValueLayout.ADDRESS,
          ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT,
          ValueLayout.JAVA_INT,
          ValueLayout.JAVA_LONG);
  private static final FunctionDescriptor START_PROFILING_DESC =
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
  private static final FunctionDescriptor STOP_PROFILING_DESC =
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);
  private static final FunctionDescriptor GET_RESULTS_DESC =
      FunctionDescriptor.of(ValueLayout.ADDRESS, ValueLayout.ADDRESS);
  private static final FunctionDescriptor DESTROY_DESC =
      FunctionDescriptor.ofVoid(ValueLayout.ADDRESS);

  static {
    try {
      // Load native library
      System.loadLibrary("wasmtime4j");
      NATIVE_LOOKUP = SymbolLookup.loaderLookup();

      // Initialize function handles
      CREATE_EXPERIMENTAL_FEATURES =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_create_experimental_features").orElseThrow(),
              CREATE_FEATURES_DESC);

      ENABLE_EXPERIMENTAL_FEATURE =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_enable_experimental_feature").orElseThrow(),
              ENABLE_FEATURE_DESC);

      DISABLE_EXPERIMENTAL_FEATURE =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_disable_experimental_feature").orElseThrow(),
              DISABLE_FEATURE_DESC);

      CONFIGURE_STACK_SWITCHING =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_configure_stack_switching").orElseThrow(),
              CONFIGURE_STACK_SWITCHING_DESC);

      CONFIGURE_CALL_CC =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_configure_call_cc").orElseThrow(),
              CONFIGURE_CALL_CC_DESC);

      CONFIGURE_ADVANCED_SECURITY =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_configure_advanced_security").orElseThrow(),
              CONFIGURE_SECURITY_DESC);

      CONFIGURE_ADVANCED_PROFILING =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_configure_advanced_profiling").orElseThrow(),
              CONFIGURE_PROFILING_DESC);

      START_PROFILING =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_start_profiling").orElseThrow(), START_PROFILING_DESC);

      STOP_PROFILING =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_stop_profiling").orElseThrow(), STOP_PROFILING_DESC);

      GET_PROFILING_RESULTS =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_get_profiling_results").orElseThrow(),
              GET_RESULTS_DESC);

      DESTROY_EXPERIMENTAL_FEATURES =
          NATIVE_LINKER.downcallHandle(
              NATIVE_LOOKUP.find("wasmtime4j_destroy_experimental_features").orElseThrow(),
              DESTROY_DESC);

    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize Panama experimental features", e);
      throw new RuntimeException("Failed to initialize Panama experimental features", e);
    }
  }

  private final MemorySegment nativeHandle;
  private final Map<ExperimentalFeature, Boolean> enabledFeatures = new ConcurrentHashMap<>();
  private volatile boolean closed = false;

  /**
   * Creates a new Panama experimental features instance.
   *
   * @param config the experimental features configuration
   * @throws IllegalArgumentException if config is null
   * @throws RuntimeException if native initialization fails
   */
  public PanamaExperimentalFeatures(final ExperimentalFeatureConfig config) {
    if (config == null) {
      throw new IllegalArgumentException("ExperimentalFeatureConfig cannot be null");
    }

    try {
      // Create native experimental features instance
      nativeHandle = (MemorySegment) CREATE_EXPERIMENTAL_FEATURES.invoke();
      if (nativeHandle == null || nativeHandle.address() == 0) {
        throw new RuntimeException("Failed to create native experimental features instance");
      }

      applyConfiguration(config);

      LOGGER.info("Panama experimental features initialized successfully");
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize Panama experimental features", e);
      throw new RuntimeException("Failed to initialize experimental features", e);
    }
  }

  /**
   * Applies the experimental features configuration to the native implementation.
   *
   * @param config the configuration to apply
   */
  private void applyConfiguration(final ExperimentalFeatureConfig config) {
    ensureNotClosed();

    try {
      // Apply basic experimental features
      for (final String featureName : config.getEnabledFeatures()) {
        try {
          final MemorySegment featureKeySegment = arena.allocateFrom(featureName);
          ENABLE_EXPERIMENTAL_FEATURE.invoke(nativeHandle, featureKeySegment);
          LOGGER.info("Enabled experimental feature: " + featureName);
        } catch (final Throwable e) {
          LOGGER.log(Level.WARNING, "Failed to enable experimental feature: " + featureName, e);
        }
      }

      // TODO: Configure advanced features when config interface is extended
      // For now, basic feature enablement only

      LOGGER.info(
          "Applied experimental features configuration with "
              + config.getEnabledFeatures().size()
              + " enabled features");
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to apply experimental features configuration", e);
      throw new RuntimeException("Failed to configure experimental features", e);
    }
  }

  /**
   * Enables a specific experimental feature.
   *
   * @param feature the feature to enable
   */
  public void enableExperimentalFeature(final ExperimentalFeature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Experimental feature cannot be null");
    }
    ensureNotClosed();

    try {
      final MemorySegment featureKeySegment = arena.allocateFrom(feature.getFeatureName());
      ENABLE_EXPERIMENTAL_FEATURE.invoke(nativeHandle, featureKeySegment);
      enabledFeatures.put(feature, true);
      LOGGER.info("Enabled experimental feature: " + feature.getFeatureName());
    } catch (final Throwable e) {
      LOGGER.log(
          Level.WARNING, "Failed to enable experimental feature: " + feature.getFeatureName(), e);
      throw new RuntimeException(
          "Failed to enable experimental feature: " + feature.getFeatureName(), e);
    }
  }

  /**
   * Disables a specific experimental feature.
   *
   * @param feature the feature to disable
   */
  public void disableExperimentalFeature(final ExperimentalFeature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Experimental feature cannot be null");
    }
    ensureNotClosed();

    try {
      final MemorySegment featureKeySegment = arena.allocateFrom(feature.getFeatureName());
      DISABLE_EXPERIMENTAL_FEATURE.invoke(nativeHandle, featureKeySegment);
      enabledFeatures.remove(feature);
      LOGGER.info("Disabled experimental feature: " + feature.getFeatureName());
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to disable experimental feature: " + feature, e);
      throw new RuntimeException("Failed to disable experimental feature: " + feature, e);
    }
  }

  /**
   * Checks if a specific experimental feature is enabled.
   *
   * @param feature the feature to check
   * @return true if the feature is enabled
   */
  public boolean isFeatureEnabled(final ExperimentalFeature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Experimental feature cannot be null");
    }
    return enabledFeatures.getOrDefault(feature, false);
  }

  /**
   * Configures stack switching parameters.
   *
   * @param stackSize stack size in bytes
   * @param maxStacks maximum concurrent stacks
   * @param strategy switching strategy (ordinal)
   */
  public void configureStackSwitching(
      final long stackSize, final int maxStacks, final int strategy) {
    ensureNotClosed();

    if (stackSize < 4096) {
      throw new IllegalArgumentException("Stack size must be at least 4KB");
    }
    if (maxStacks <= 0) {
      throw new IllegalArgumentException("Maximum stacks must be positive");
    }

    try {
      CONFIGURE_STACK_SWITCHING.invoke(nativeHandle, stackSize, maxStacks, strategy);
      LOGGER.info(
          "Configured stack switching: stackSize="
              + stackSize
              + ", maxStacks="
              + maxStacks
              + ", strategy="
              + strategy);
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to configure stack switching", e);
      throw new RuntimeException("Failed to configure stack switching", e);
    }
  }

  /**
   * Configures call/cc parameters.
   *
   * @param maxContinuations maximum number of continuations
   * @param storageStrategy storage strategy (ordinal)
   * @param compressionEnabled whether compression is enabled (1) or not (0)
   */
  public void configureCallCc(
      final int maxContinuations, final int storageStrategy, final int compressionEnabled) {
    ensureNotClosed();

    if (maxContinuations <= 0) {
      throw new IllegalArgumentException("Maximum continuations must be positive");
    }

    try {
      CONFIGURE_CALL_CC.invoke(nativeHandle, maxContinuations, storageStrategy, compressionEnabled);
      LOGGER.info(
          "Configured call/cc: maxContinuations="
              + maxContinuations
              + ", storageStrategy="
              + storageStrategy
              + ", compression="
              + (compressionEnabled != 0));
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to configure call/cc", e);
      throw new RuntimeException("Failed to configure call/cc", e);
    }
  }

  /**
   * Configures advanced security features.
   *
   * @param securityLevel security level (ordinal)
   * @param enableSandboxing enable sandboxing (1) or not (0)
   * @param enableResourceLimits enable resource limits (1) or not (0)
   * @param maxMemoryMb maximum memory in MB
   */
  public void configureAdvancedSecurity(
      final int securityLevel,
      final int enableSandboxing,
      final int enableResourceLimits,
      final int maxMemoryMb) {
    ensureNotClosed();

    try {
      CONFIGURE_ADVANCED_SECURITY.invoke(
          nativeHandle, securityLevel, enableSandboxing, enableResourceLimits, maxMemoryMb);
      LOGGER.info(
          "Configured advanced security: level="
              + securityLevel
              + ", sandboxing="
              + (enableSandboxing != 0)
              + ", resourceLimits="
              + (enableResourceLimits != 0)
              + ", memory="
              + maxMemoryMb
              + "MB");
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to configure advanced security", e);
      throw new RuntimeException("Failed to configure advanced security", e);
    }
  }

  /**
   * Configures advanced profiling features.
   *
   * @param enablePerfCounters enable performance counters (1) or not (0)
   * @param enableTracing enable execution tracing (1) or not (0)
   * @param granularity profiling granularity (ordinal)
   * @param samplingInterval sampling interval in microseconds
   */
  public void configureAdvancedProfiling(
      final int enablePerfCounters,
      final int enableTracing,
      final int granularity,
      final long samplingInterval) {
    ensureNotClosed();

    try {
      CONFIGURE_ADVANCED_PROFILING.invoke(
          nativeHandle, enablePerfCounters, enableTracing, granularity, samplingInterval);
      LOGGER.info(
          "Configured advanced profiling: perfCounters="
              + (enablePerfCounters != 0)
              + ", tracing="
              + (enableTracing != 0)
              + ", granularity="
              + granularity
              + ", interval="
              + samplingInterval
              + "μs");
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to configure advanced profiling", e);
      throw new RuntimeException("Failed to configure advanced profiling", e);
    }
  }

  /** Starts an advanced profiling session. */
  public void startProfiling() {
    ensureNotClosed();

    try {
      START_PROFILING.invoke(nativeHandle);
      LOGGER.info("Started advanced profiling session");
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to start profiling", e);
      throw new RuntimeException("Failed to start profiling", e);
    }
  }

  /** Stops the current profiling session. */
  public void stopProfiling() {
    ensureNotClosed();

    try {
      STOP_PROFILING.invoke(nativeHandle);
      LOGGER.info("Stopped advanced profiling session");
    } catch (final Throwable e) {
      LOGGER.log(Level.SEVERE, "Failed to stop profiling", e);
      throw new RuntimeException("Failed to stop profiling", e);
    }
  }

  /**
   * Gets the current profiling results.
   *
   * @return profiling results as a formatted string
   */
  public String getProfilingResults() {
    ensureNotClosed();

    try {
      final MemorySegment resultSegment =
          (MemorySegment) GET_PROFILING_RESULTS.invoke(nativeHandle);
      if (resultSegment == null || resultSegment.address() == 0) {
        return "No profiling results available";
      }

      final String results = resultSegment.getString(0, java.nio.charset.StandardCharsets.UTF_8);
      LOGGER.fine("Retrieved profiling results");
      return results;
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to get profiling results", e);
      return "Failed to retrieve profiling results: " + e.getMessage();
    }
  }

  /**
   * Checks if the experimental features are properly initialized and not closed.
   *
   * @return true if initialized and not closed
   */
  public boolean isInitialized() {
    return nativeHandle != null && nativeHandle.address() != 0 && !closed;
  }

  /**
   * Ensures that this instance is not closed.
   *
   * @throws IllegalStateException if the instance is closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Experimental features instance has been closed");
    }
  }

  @Override
  public void close() {
    if (closed) {
      return;
    }

    try {
      if (nativeHandle != null && nativeHandle.address() != 0) {
        DESTROY_EXPERIMENTAL_FEATURES.invoke(nativeHandle);
      }

      arena.close();
      enabledFeatures.clear();
      closed = true;

      LOGGER.info("Panama experimental features closed successfully");
    } catch (final Throwable e) {
      LOGGER.log(Level.WARNING, "Failed to close Panama experimental features cleanly", e);
    }
  }
}
