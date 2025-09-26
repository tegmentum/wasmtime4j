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

package ai.tegmentum.wasmtime4j;

import java.util.Map;
import java.util.Set;

/**
 * Configuration for interface adaptation between WIT interface versions.
 *
 * <p>This configuration controls how interface adapters handle version differences,
 * type transformations, and function bridging between different interface versions.
 *
 * @since 1.0.0
 */
public final class AdaptationConfig {

  private final boolean strictMode;
  private final boolean allowDataLoss;
  private final boolean autoGenerateAdapters;
  private final Map<String, TypeMapping> typeMappings;
  private final Map<String, FunctionMapping> functionMappings;
  private final Set<String> ignoredFunctions;
  private final Set<String> requiredFunctions;
  private final boolean enablePerformanceOptimizations;
  private final boolean enableStatisticsCollection;
  private final int maxAdaptationDepth;
  private final long timeoutMillis;

  private AdaptationConfig(Builder builder) {
    this.strictMode = builder.strictMode;
    this.allowDataLoss = builder.allowDataLoss;
    this.autoGenerateAdapters = builder.autoGenerateAdapters;
    this.typeMappings = Map.copyOf(builder.typeMappings);
    this.functionMappings = Map.copyOf(builder.functionMappings);
    this.ignoredFunctions = Set.copyOf(builder.ignoredFunctions);
    this.requiredFunctions = Set.copyOf(builder.requiredFunctions);
    this.enablePerformanceOptimizations = builder.enablePerformanceOptimizations;
    this.enableStatisticsCollection = builder.enableStatisticsCollection;
    this.maxAdaptationDepth = builder.maxAdaptationDepth;
    this.timeoutMillis = builder.timeoutMillis;
  }

  /**
   * Gets whether strict mode is enabled.
   *
   * @return true if strict mode is enabled
   */
  public boolean isStrictMode() {
    return strictMode;
  }

  /**
   * Gets whether data loss is allowed during adaptation.
   *
   * @return true if data loss is allowed
   */
  public boolean isAllowDataLoss() {
    return allowDataLoss;
  }

  /**
   * Gets whether auto-generation of adapters is enabled.
   *
   * @return true if auto-generation is enabled
   */
  public boolean isAutoGenerateAdapters() {
    return autoGenerateAdapters;
  }

  /**
   * Gets the type mappings for adaptation.
   *
   * @return type mappings map
   */
  public Map<String, TypeMapping> getTypeMappings() {
    return typeMappings;
  }

  /**
   * Gets the function mappings for adaptation.
   *
   * @return function mappings map
   */
  public Map<String, FunctionMapping> getFunctionMappings() {
    return functionMappings;
  }

  /**
   * Gets the set of functions to ignore during adaptation.
   *
   * @return ignored functions set
   */
  public Set<String> getIgnoredFunctions() {
    return ignoredFunctions;
  }

  /**
   * Gets the set of required functions that must be present.
   *
   * @return required functions set
   */
  public Set<String> getRequiredFunctions() {
    return requiredFunctions;
  }

  /**
   * Gets whether performance optimizations are enabled.
   *
   * @return true if performance optimizations are enabled
   */
  public boolean isEnablePerformanceOptimizations() {
    return enablePerformanceOptimizations;
  }

  /**
   * Gets whether statistics collection is enabled.
   *
   * @return true if statistics collection is enabled
   */
  public boolean isEnableStatisticsCollection() {
    return enableStatisticsCollection;
  }

  /**
   * Gets the maximum adaptation depth allowed.
   *
   * @return maximum adaptation depth
   */
  public int getMaxAdaptationDepth() {
    return maxAdaptationDepth;
  }

  /**
   * Gets the adaptation timeout in milliseconds.
   *
   * @return timeout in milliseconds
   */
  public long getTimeoutMillis() {
    return timeoutMillis;
  }

  /**
   * Creates a new builder for AdaptationConfig.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default adaptation configuration.
   *
   * @return default configuration
   */
  public static AdaptationConfig defaults() {
    return new Builder().build();
  }

  /**
   * Builder for AdaptationConfig.
   */
  public static final class Builder {
    private boolean strictMode = false;
    private boolean allowDataLoss = false;
    private boolean autoGenerateAdapters = true;
    private Map<String, TypeMapping> typeMappings = Map.of();
    private Map<String, FunctionMapping> functionMappings = Map.of();
    private Set<String> ignoredFunctions = Set.of();
    private Set<String> requiredFunctions = Set.of();
    private boolean enablePerformanceOptimizations = true;
    private boolean enableStatisticsCollection = false;
    private int maxAdaptationDepth = 10;
    private long timeoutMillis = 30000L; // 30 seconds

    /**
     * Sets strict mode.
     *
     * @param strictMode true to enable strict mode
     * @return this builder
     */
    public Builder strictMode(boolean strictMode) {
      this.strictMode = strictMode;
      return this;
    }

    /**
     * Sets whether data loss is allowed.
     *
     * @param allowDataLoss true to allow data loss
     * @return this builder
     */
    public Builder allowDataLoss(boolean allowDataLoss) {
      this.allowDataLoss = allowDataLoss;
      return this;
    }

    /**
     * Sets whether to auto-generate adapters.
     *
     * @param autoGenerateAdapters true to auto-generate adapters
     * @return this builder
     */
    public Builder autoGenerateAdapters(boolean autoGenerateAdapters) {
      this.autoGenerateAdapters = autoGenerateAdapters;
      return this;
    }

    /**
     * Sets the type mappings.
     *
     * @param typeMappings the type mappings
     * @return this builder
     */
    public Builder typeMappings(Map<String, TypeMapping> typeMappings) {
      this.typeMappings = typeMappings != null ? Map.copyOf(typeMappings) : Map.of();
      return this;
    }

    /**
     * Sets the function mappings.
     *
     * @param functionMappings the function mappings
     * @return this builder
     */
    public Builder functionMappings(Map<String, FunctionMapping> functionMappings) {
      this.functionMappings = functionMappings != null ? Map.copyOf(functionMappings) : Map.of();
      return this;
    }

    /**
     * Sets the ignored functions.
     *
     * @param ignoredFunctions the functions to ignore
     * @return this builder
     */
    public Builder ignoredFunctions(Set<String> ignoredFunctions) {
      this.ignoredFunctions = ignoredFunctions != null ? Set.copyOf(ignoredFunctions) : Set.of();
      return this;
    }

    /**
     * Sets the required functions.
     *
     * @param requiredFunctions the functions that must be present
     * @return this builder
     */
    public Builder requiredFunctions(Set<String> requiredFunctions) {
      this.requiredFunctions = requiredFunctions != null ? Set.copyOf(requiredFunctions) : Set.of();
      return this;
    }

    /**
     * Sets whether to enable performance optimizations.
     *
     * @param enablePerformanceOptimizations true to enable performance optimizations
     * @return this builder
     */
    public Builder enablePerformanceOptimizations(boolean enablePerformanceOptimizations) {
      this.enablePerformanceOptimizations = enablePerformanceOptimizations;
      return this;
    }

    /**
     * Sets whether to enable statistics collection.
     *
     * @param enableStatisticsCollection true to enable statistics collection
     * @return this builder
     */
    public Builder enableStatisticsCollection(boolean enableStatisticsCollection) {
      this.enableStatisticsCollection = enableStatisticsCollection;
      return this;
    }

    /**
     * Sets the maximum adaptation depth.
     *
     * @param maxAdaptationDepth the maximum depth
     * @return this builder
     */
    public Builder maxAdaptationDepth(int maxAdaptationDepth) {
      if (maxAdaptationDepth <= 0) {
        throw new IllegalArgumentException("Max adaptation depth must be positive");
      }
      this.maxAdaptationDepth = maxAdaptationDepth;
      return this;
    }

    /**
     * Sets the adaptation timeout.
     *
     * @param timeoutMillis the timeout in milliseconds
     * @return this builder
     */
    public Builder timeout(long timeoutMillis) {
      if (timeoutMillis <= 0) {
        throw new IllegalArgumentException("Timeout must be positive");
      }
      this.timeoutMillis = timeoutMillis;
      return this;
    }

    /**
     * Builds the AdaptationConfig.
     *
     * @return the configured AdaptationConfig
     */
    public AdaptationConfig build() {
      return new AdaptationConfig(this);
    }
  }

  /**
   * Type mapping configuration for adaptation.
   */
  public static final class TypeMapping {
    private final String sourceType;
    private final String targetType;
    private final String conversionFunction;
    private final boolean lossyConversion;

    public TypeMapping(String sourceType, String targetType, String conversionFunction, boolean lossyConversion) {
      this.sourceType = sourceType;
      this.targetType = targetType;
      this.conversionFunction = conversionFunction;
      this.lossyConversion = lossyConversion;
    }

    public String getSourceType() {
      return sourceType;
    }

    public String getTargetType() {
      return targetType;
    }

    public String getConversionFunction() {
      return conversionFunction;
    }

    public boolean isLossyConversion() {
      return lossyConversion;
    }
  }

  /**
   * Function mapping configuration for adaptation.
   */
  public static final class FunctionMapping {
    private final String sourceFunction;
    private final String targetFunction;
    private final Map<String, String> parameterMappings;
    private final String returnValueMapping;

    public FunctionMapping(String sourceFunction, String targetFunction,
                           Map<String, String> parameterMappings, String returnValueMapping) {
      this.sourceFunction = sourceFunction;
      this.targetFunction = targetFunction;
      this.parameterMappings = parameterMappings != null ? Map.copyOf(parameterMappings) : Map.of();
      this.returnValueMapping = returnValueMapping;
    }

    public String getSourceFunction() {
      return sourceFunction;
    }

    public String getTargetFunction() {
      return targetFunction;
    }

    public Map<String, String> getParameterMappings() {
      return parameterMappings;
    }

    public String getReturnValueMapping() {
      return returnValueMapping;
    }
  }
}