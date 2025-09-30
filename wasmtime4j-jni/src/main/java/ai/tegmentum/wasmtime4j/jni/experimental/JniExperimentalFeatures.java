package ai.tegmentum.wasmtime4j.jni.experimental;

import ai.tegmentum.wasmtime4j.experimental.ExperimentalFeature;
import ai.tegmentum.wasmtime4j.experimental.ExperimentalFeatureConfig;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation for experimental WebAssembly features and cutting-edge capabilities.
 *
 * <p><b>WARNING:</b> This class provides access to highly experimental features that are unstable
 * and subject to change. Use only for testing, development, and research.
 *
 * @since 1.0.0
 */
public final class JniExperimentalFeatures extends JniResource {

  private static final Logger LOGGER = Logger.getLogger(JniExperimentalFeatures.class.getName());

  private final Map<ExperimentalFeature, Boolean> enabledFeatures = new ConcurrentHashMap<>();
  private volatile boolean initialized = false;

  static {
    try {
      System.loadLibrary("wasmtime4j");
    } catch (final UnsatisfiedLinkError e) {
      LOGGER.log(Level.SEVERE, "Failed to load native library for experimental features", e);
      throw new RuntimeException("Failed to load wasmtime4j native library", e);
    }
  }

  /**
   * Creates a new JNI experimental features instance.
   *
   * @param config the experimental features configuration
   * @throws IllegalArgumentException if config is null
   * @throws RuntimeException if native initialization fails
   */
  public JniExperimentalFeatures(final ExperimentalFeatureConfig config) {
    JniValidation.requireNonNull(config, "ExperimentalFeatureConfig cannot be null");

    try {
      final long nativePtr = nativeCreateExperimentalFeatures();
      if (nativePtr == 0) {
        throw new RuntimeException("Failed to create native experimental features instance");
      }

      setNativeHandle(nativePtr);
      applyConfiguration(config);
      this.initialized = true;

      LOGGER.info("JNI experimental features initialized successfully");
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to initialize JNI experimental features", e);
      cleanup();
      throw new RuntimeException("Failed to initialize experimental features", e);
    }
  }

  /**
   * Applies the experimental features configuration to the native implementation.
   *
   * @param config the configuration to apply
   */
  private void applyConfiguration(final ExperimentalFeatureConfig config) {
    JniValidation.validateResourceNotClosed(this);

    try {
      // Apply basic experimental features
      for (final Map.Entry<ExperimentalFeature, Boolean> entry :
          config.getEnabledFeatures().entrySet()) {
        final ExperimentalFeature feature = entry.getKey();
        final boolean enabled = entry.getValue();

        if (enabled) {
          enableExperimentalFeature(feature);
        }
      }

      // Configure stack switching if enabled
      if (config.isFeatureEnabled(ExperimentalFeature.STACK_SWITCHING)) {
        configureStackSwitching(
            config.getStackSwitchingStackSize(),
            config.getStackSwitchingMaxConcurrentStacks(),
            config.getStackSwitchingStrategy().ordinal());
      }

      // Configure call/cc if enabled
      if (config.isFeatureEnabled(ExperimentalFeature.CALL_CC)) {
        configureCallCc(
            config.getCallCcMaxContinuations(),
            config.getCallCcStorageStrategy().ordinal(),
            config.isCallCcCompressionEnabled() ? 1 : 0);
      }

      // Configure security features
      configureAdvancedSecurity(
          config.getSecurityLevel().ordinal(),
          config.isAdvancedSandboxingEnabled() ? 1 : 0,
          config.isResourceLimitingEnabled() ? 1 : 0,
          1024 // Default 1GB memory limit
          );

      // Configure profiling features
      if (config.isAdvancedProfilingEnabled()) {
        configureAdvancedProfiling(
            1, // Enable performance counters
            config.isExecutionTracingEnabled() ? 1 : 0,
            config.getProfilingGranularity().ordinal(),
            1000 // 1ms sampling interval
            );
      }

      LOGGER.info(
          "Applied experimental features configuration with {} enabled features",
          config.getEnabledFeatures().size());
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
    JniValidation.requireNonNull(feature, "Experimental feature cannot be null");
    JniValidation.validateResourceNotClosed(this);

    try {
      nativeEnableExperimentalFeature(getNativeHandle(), feature.getKey());
      enabledFeatures.put(feature, true);
      LOGGER.info("Enabled experimental feature: {}", feature);
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to enable experimental feature: " + feature, e);
      throw new RuntimeException("Failed to enable experimental feature: " + feature, e);
    }
  }

  /**
   * Disables a specific experimental feature.
   *
   * @param feature the feature to disable
   */
  public void disableExperimentalFeature(final ExperimentalFeature feature) {
    JniValidation.requireNonNull(feature, "Experimental feature cannot be null");
    JniValidation.validateResourceNotClosed(this);

    try {
      nativeDisableExperimentalFeature(getNativeHandle(), feature.getKey());
      enabledFeatures.remove(feature);
      LOGGER.info("Disabled experimental feature: {}", feature);
    } catch (final Exception e) {
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
    JniValidation.requireNonNull(feature, "Experimental feature cannot be null");
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
    JniValidation.validateResourceNotClosed(this);

    if (stackSize < 4096) {
      throw new IllegalArgumentException("Stack size must be at least 4KB");
    }
    if (maxStacks <= 0) {
      throw new IllegalArgumentException("Maximum stacks must be positive");
    }

    try {
      nativeConfigureStackSwitching(getNativeHandle(), stackSize, maxStacks, strategy);
      LOGGER.info(
          "Configured stack switching: stackSize={}, maxStacks={}, strategy={}",
          stackSize,
          maxStacks,
          strategy);
    } catch (final Exception e) {
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
    JniValidation.validateResourceNotClosed(this);

    if (maxContinuations <= 0) {
      throw new IllegalArgumentException("Maximum continuations must be positive");
    }

    try {
      nativeConfigureCallCc(
          getNativeHandle(), maxContinuations, storageStrategy, compressionEnabled);
      LOGGER.info(
          "Configured call/cc: maxContinuations={}, storageStrategy={}, compression={}",
          maxContinuations,
          storageStrategy,
          compressionEnabled != 0);
    } catch (final Exception e) {
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
    JniValidation.validateResourceNotClosed(this);

    try {
      nativeConfigureAdvancedSecurity(
          getNativeHandle(), securityLevel, enableSandboxing, enableResourceLimits, maxMemoryMb);
      LOGGER.info(
          "Configured advanced security: level={}, sandboxing={}, resourceLimits={}, memory={}MB",
          securityLevel,
          enableSandboxing != 0,
          enableResourceLimits != 0,
          maxMemoryMb);
    } catch (final Exception e) {
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
    JniValidation.validateResourceNotClosed(this);

    try {
      nativeConfigureAdvancedProfiling(
          getNativeHandle(), enablePerfCounters, enableTracing, granularity, samplingInterval);
      LOGGER.info(
          "Configured advanced profiling: perfCounters={}, tracing={}, granularity={},"
              + " interval={}μs",
          enablePerfCounters != 0,
          enableTracing != 0,
          granularity,
          samplingInterval);
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to configure advanced profiling", e);
      throw new RuntimeException("Failed to configure advanced profiling", e);
    }
  }

  /** Starts an advanced profiling session. */
  public void startProfiling() {
    JniValidation.validateResourceNotClosed(this);

    try {
      nativeStartProfiling(getNativeHandle());
      LOGGER.info("Started advanced profiling session");
    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Failed to start profiling", e);
      throw new RuntimeException("Failed to start profiling", e);
    }
  }

  /** Stops the current profiling session. */
  public void stopProfiling() {
    JniValidation.validateResourceNotClosed(this);

    try {
      nativeStopProfiling(getNativeHandle());
      LOGGER.info("Stopped advanced profiling session");
    } catch (final Exception e) {
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
    JniValidation.validateResourceNotClosed(this);

    try {
      final String results = nativeGetProfilingResults(getNativeHandle());
      LOGGER.fine("Retrieved profiling results");
      return results;
    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to get profiling results", e);
      return "Failed to retrieve profiling results: " + e.getMessage();
    }
  }

  /**
   * Checks if the experimental features are properly initialized.
   *
   * @return true if initialized successfully
   */
  public boolean isInitialized() {
    return initialized && !isClosed();
  }

  @Override
  protected void cleanup() {
    if (getNativeHandle() != 0) {
      try {
        nativeDestroy(getNativeHandle());
      } catch (final Exception e) {
        LOGGER.log(Level.WARNING, "Failed to destroy native experimental features", e);
      } finally {
        clearNativeHandle();
        enabledFeatures.clear();
        initialized = false;
      }
    }
  }

  // Native method declarations

  private static native long nativeCreateExperimentalFeatures();

  private static native void nativeEnableExperimentalFeature(long handle, String featureKey);

  private static native void nativeDisableExperimentalFeature(long handle, String featureKey);

  private static native void nativeConfigureStackSwitching(
      long handle, long stackSize, int maxStacks, int strategy);

  private static native void nativeConfigureCallCc(
      long handle, int maxContinuations, int storageStrategy, int compressionEnabled);

  private static native void nativeConfigureAdvancedSecurity(
      long handle,
      int securityLevel,
      int enableSandboxing,
      int enableResourceLimits,
      int maxMemoryMb);

  private static native void nativeConfigureAdvancedProfiling(
      long handle,
      int enablePerfCounters,
      int enableTracing,
      int granularity,
      long samplingInterval);

  private static native void nativeStartProfiling(long handle);

  private static native void nativeStopProfiling(long handle);

  private static native String nativeGetProfilingResults(long handle);

  private static native void nativeDestroy(long handle);
}
