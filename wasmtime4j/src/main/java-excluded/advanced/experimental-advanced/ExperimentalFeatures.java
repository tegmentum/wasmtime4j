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
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;

/**
 * Manager for experimental WebAssembly features.
 *
 * <p>This class provides centralized control over experimental features that are not yet part of
 * the stable WebAssembly specification. Features must be explicitly enabled before they can be
 * used.
 *
 * <p><strong>WARNING:</strong> Experimental features are subject to change and may be removed or
 * modified in future releases without notice.
 *
 * @since 1.0.0
 */
public final class ExperimentalFeatures {

  private static final Logger LOGGER = Logger.getLogger(ExperimentalFeatures.class.getName());

  /** Enumeration of experimental WebAssembly features. */
  public enum Feature {
    /** WebAssembly exception handling proposal. */
    EXCEPTION_HANDLING("exception-handling", "WebAssembly Exception Handling Proposal"),

    /** WebAssembly component model proposal. */
    COMPONENT_MODEL("component-model", "WebAssembly Component Model Proposal"),

    /** WebAssembly garbage collection proposal. */
    GARBAGE_COLLECTION("gc", "WebAssembly Garbage Collection Proposal"),

    /** WebAssembly SIMD proposal. */
    SIMD("simd", "WebAssembly SIMD Proposal"),

    /** WebAssembly threads proposal. */
    THREADS("threads", "WebAssembly Threads Proposal"),

    /** WebAssembly multi-value proposal. */
    MULTI_VALUE("multi-value", "WebAssembly Multi-Value Proposal"),

    /** WebAssembly reference types proposal. */
    REFERENCE_TYPES("reference-types", "WebAssembly Reference Types Proposal"),

    /** WebAssembly bulk memory operations proposal. */
    BULK_MEMORY("bulk-memory", "WebAssembly Bulk Memory Operations Proposal"),

    /** WebAssembly tail call proposal. */
    TAIL_CALL("tail-call", "WebAssembly Tail Call Proposal"),

    /** WebAssembly memory64 proposal. */
    MEMORY64("memory64", "WebAssembly Memory64 Proposal");

    private final String name;
    private final String description;

    Feature(final String name, final String description) {
      this.name = name;
      this.description = description;
    }

    /**
     * Gets the feature name.
     *
     * @return the feature name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the feature description.
     *
     * @return the feature description
     */
    public String getDescription() {
      return description;
    }

    @Override
    public String toString() {
      return name + " (" + description + ")";
    }
  }

  private static final Set<Feature> enabledFeatures = ConcurrentHashMap.newKeySet();
  private static final Set<Feature> supportedFeatures = EnumSet.allOf(Feature.class);

  // Prevent instantiation
  private ExperimentalFeatures() {}

  /**
   * Enables an experimental feature.
   *
   * @param feature the feature to enable
   * @throws IllegalArgumentException if feature is null
   * @throws UnsupportedOperationException if feature is not supported
   */
  public static void enableFeature(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    if (!supportedFeatures.contains(feature)) {
      throw new UnsupportedOperationException("Feature " + feature.getName() + " is not supported");
    }

    if (enabledFeatures.add(feature)) {
      LOGGER.info("Enabled experimental feature: " + feature);
    }
  }

  /**
   * Disables an experimental feature.
   *
   * @param feature the feature to disable
   * @throws IllegalArgumentException if feature is null
   */
  public static void disableFeature(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    if (enabledFeatures.remove(feature)) {
      LOGGER.info("Disabled experimental feature: " + feature);
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

    return enabledFeatures.contains(feature);
  }

  /**
   * Checks if an experimental feature is supported.
   *
   * @param feature the feature to check
   * @return true if supported, false otherwise
   * @throws IllegalArgumentException if feature is null
   */
  public static boolean isFeatureSupported(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    return supportedFeatures.contains(feature);
  }

  /**
   * Gets all enabled experimental features.
   *
   * @return immutable set of enabled features
   */
  public static Set<Feature> getEnabledFeatures() {
    return Set.copyOf(enabledFeatures);
  }

  /**
   * Gets all supported experimental features.
   *
   * @return immutable set of supported features
   */
  public static Set<Feature> getSupportedFeatures() {
    return Set.copyOf(supportedFeatures);
  }

  /**
   * Validates that a feature is enabled and supported.
   *
   * @param feature the feature to validate
   * @throws IllegalArgumentException if feature is null
   * @throws UnsupportedOperationException if feature is not supported or not enabled
   */
  public static void validateFeatureSupport(final Feature feature) {
    if (feature == null) {
      throw new IllegalArgumentException("Feature cannot be null");
    }

    if (!supportedFeatures.contains(feature)) {
      throw new UnsupportedOperationException(
          "Experimental feature " + feature.getName() + " is not supported");
    }

    if (!enabledFeatures.contains(feature)) {
      throw new UnsupportedOperationException(
          "Experimental feature "
              + feature.getName()
              + " is not enabled. "
              + "Enable it with ExperimentalFeatures.enableFeature("
              + feature.name()
              + ")");
    }
  }

  /**
   * Enables multiple experimental features at once.
   *
   * @param features the features to enable
   * @throws IllegalArgumentException if features is null or contains null elements
   * @throws UnsupportedOperationException if any feature is not supported
   */
  public static void enableFeatures(final Feature... features) {
    if (features == null) {
      throw new IllegalArgumentException("Features cannot be null");
    }

    for (final Feature feature : features) {
      enableFeature(feature);
    }
  }

  /**
   * Disables multiple experimental features at once.
   *
   * @param features the features to disable
   * @throws IllegalArgumentException if features is null or contains null elements
   */
  public static void disableFeatures(final Feature... features) {
    if (features == null) {
      throw new IllegalArgumentException("Features cannot be null");
    }

    for (final Feature feature : features) {
      disableFeature(feature);
    }
  }

  /** Disables all experimental features. */
  public static void reset() {
    enabledFeatures.clear();
    LOGGER.info("Reset all experimental features");
  }

  /** Enables all supported experimental features. */
  public static void enableAllFeatures() {
    enabledFeatures.addAll(supportedFeatures);
    LOGGER.info("Enabled all experimental features");
  }

  /**
   * Gets feature information as a formatted string.
   *
   * @return formatted feature information
   */
  public static String getFeatureInfo() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Experimental Features:\n");

    for (final Feature feature : Feature.values()) {
      sb.append("  ")
          .append(feature.getName())
          .append(" - ")
          .append(feature.getDescription())
          .append(" [")
          .append(isFeatureEnabled(feature) ? "ENABLED" : "DISABLED")
          .append("]\n");
    }

    return sb.toString();
  }

  /**
   * Checks if experimental features are globally enabled.
   *
   * @return true if any experimental feature is enabled
   */
  public static boolean hasEnabledFeatures() {
    return !enabledFeatures.isEmpty();
  }

  /**
   * Gets the count of enabled features.
   *
   * @return the number of enabled features
   */
  public static int getEnabledFeatureCount() {
    return enabledFeatures.size();
  }

  /**
   * Gets the count of supported features.
   *
   * @return the number of supported features
   */
  public static int getSupportedFeatureCount() {
    return supportedFeatures.size();
  }
}
