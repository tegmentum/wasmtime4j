/*
 * Copyright 2024 Tegmentum Technology, Inc.
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

package ai.tegmentum.wasmtime4j.experimental;

import java.util.EnumSet;
import java.util.Set;

/**
 * Experimental WebAssembly feature flags and configuration.
 *
 * <p><strong>WARNING:</strong> Experimental features are subject to change and may be removed in
 * future versions. They are provided for early access to cutting-edge WebAssembly proposals and
 * should not be used in production environments without thorough testing.
 *
 * <p>Features can be enabled via system properties or programmatically. System properties take
 * precedence over programmatic configuration.
 *
 * @since 1.0.0
 */
public final class ExperimentalFeatures {

  /** Available experimental WebAssembly features. */
  public enum Feature {
    /** Exception handling proposal - try/catch blocks and exception throwing. */
    EXCEPTION_HANDLING("wasmtime4j.experimental.exceptions", false),

    /** Advanced SIMD vector operations beyond basic v128 support. */
    ADVANCED_SIMD("wasmtime4j.experimental.simd", false),

    /** Multi-value proposal - functions returning multiple values. */
    MULTI_VALUE("wasmtime4j.experimental.multivalue", false),

    /** Extended reference types beyond funcref and externref. */
    REFERENCE_TYPES_EXTENDED("wasmtime4j.experimental.reftypes", false),

    /** Relaxed SIMD proposal for platform-specific optimizations. */
    RELAXED_SIMD("wasmtime4j.experimental.relaxed_simd", false);

    private final String systemProperty;
    private final boolean defaultEnabled;

    Feature(final String systemProperty, final boolean defaultEnabled) {
      this.systemProperty = systemProperty;
      this.defaultEnabled = defaultEnabled;
    }

    /**
     * Gets the system property name for this feature.
     *
     * @return the system property name
     */
    public String getSystemProperty() {
      return systemProperty;
    }

    /**
     * Gets the default enabled state for this feature.
     *
     * @return true if enabled by default, false otherwise
     */
    public boolean isDefaultEnabled() {
      return defaultEnabled;
    }

    /**
     * Checks if this feature is enabled via system property or default setting.
     *
     * @return true if enabled, false otherwise
     */
    public boolean isEnabled() {
      final String property = System.getProperty(systemProperty);
      if (property != null) {
        return Boolean.parseBoolean(property);
      }
      return ExperimentalFeatures.isFeatureEnabled(this);
    }
  }

  private static final Set<Feature> enabledFeatures = EnumSet.noneOf(Feature.class);
  private static volatile boolean initialized = false;

  static {
    initialize();
  }

  /** Initialize experimental features from system properties. */
  private static synchronized void initialize() {
    if (initialized) {
      return;
    }

    // Check system properties for each feature
    for (final Feature feature : Feature.values()) {
      final String property = System.getProperty(feature.getSystemProperty());
      if (property != null) {
        if (Boolean.parseBoolean(property)) {
          enabledFeatures.add(feature);
        }
      } else if (feature.isDefaultEnabled()) {
        enabledFeatures.add(feature);
      }
    }

    initialized = true;
  }

  /**
   * Enables an experimental feature programmatically.
   *
   * <p>Note: System properties take precedence over programmatic configuration.
   *
   * @param feature the feature to enable
   * @throws IllegalArgumentException if feature is null
   */
  public static synchronized void enableFeature(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    // Only enable if not overridden by system property
    if (System.getProperty(feature.getSystemProperty()) == null) {
      enabledFeatures.add(feature);
    }
  }

  /**
   * Disables an experimental feature programmatically.
   *
   * <p>Note: System properties take precedence over programmatic configuration.
   *
   * @param feature the feature to disable
   * @throws IllegalArgumentException if feature is null
   */
  public static synchronized void disableFeature(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    // Only disable if not overridden by system property
    if (System.getProperty(feature.getSystemProperty()) == null) {
      enabledFeatures.remove(feature);
    }
  }

  /**
   * Checks if an experimental feature is enabled.
   *
   * @param feature the feature to check
   * @return true if enabled, false otherwise
   * @throws IllegalArgumentException if feature is null
   */
  public static boolean isFeatureEnabled(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    // System properties override programmatic settings
    final String property = System.getProperty(feature.getSystemProperty());
    if (property != null) {
      return Boolean.parseBoolean(property);
    }

    return enabledFeatures.contains(feature);
  }

  /**
   * Gets all currently enabled experimental features.
   *
   * @return immutable set of enabled features
   */
  public static Set<Feature> getEnabledFeatures() {
    final Set<Feature> result = EnumSet.noneOf(Feature.class);

    for (final Feature feature : Feature.values()) {
      if (isFeatureEnabled(feature)) {
        result.add(feature);
      }
    }

    return result;
  }

  /**
   * Validates feature compatibility with the current runtime.
   *
   * @param feature the feature to validate
   * @throws UnsupportedOperationException if feature is not supported
   * @throws IllegalArgumentException if feature is null
   */
  public static void validateFeatureSupport(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    if (!isFeatureEnabled(feature)) {
      throw new UnsupportedOperationException(
          "Experimental feature "
              + feature
              + " is not enabled. "
              + "Enable via system property: -D"
              + feature.getSystemProperty()
              + "=true");
    }

    // Additional runtime-specific validation could be added here
    // For now, we assume all features are supported if enabled
  }

  /**
   * Checks if any experimental features are enabled.
   *
   * @return true if any experimental features are enabled, false otherwise
   */
  public static boolean hasEnabledFeatures() {
    return !getEnabledFeatures().isEmpty();
  }

  /**
   * Resets all experimental feature settings to defaults.
   *
   * <p>This method is primarily for testing purposes.
   */
  static synchronized void reset() {
    enabledFeatures.clear();
    initialized = false;
    initialize();
  }

  // Private constructor to prevent instantiation
  private ExperimentalFeatures() {
    throw new UnsupportedOperationException("Utility class cannot be instantiated");
  }
}
