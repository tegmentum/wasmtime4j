package ai.tegmentum.wasmtime4j.performance;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration settings used by the WebAssembly compiler.
 *
 * <p>This class captures the compiler configuration that was used during module compilation,
 * providing insights into optimization levels, target features, and other compilation settings.
 *
 * @since 1.0.0
 */
public final class CompilerConfig {
  private final String compilerName;
  private final String optimizationLevel;
  private final boolean debugInfo;
  private final boolean profilingInfo;
  private final Map<String, Object> features;
  private final Map<String, Object> options;

  /**
   * Creates a compiler configuration record.
   *
   * @param compilerName the name of the compiler used
   * @param optimizationLevel the optimization level (e.g., "none", "speed", "size")
   * @param debugInfo whether debug information was included
   * @param profilingInfo whether profiling information was included
   * @param features map of enabled features
   * @param options map of compiler options
   */
  public CompilerConfig(
      final String compilerName,
      final String optimizationLevel,
      final boolean debugInfo,
      final boolean profilingInfo,
      final Map<String, Object> features,
      final Map<String, Object> options) {
    this.compilerName = Objects.requireNonNull(compilerName, "compilerName cannot be null");
    this.optimizationLevel =
        Objects.requireNonNull(optimizationLevel, "optimizationLevel cannot be null");
    this.debugInfo = debugInfo;
    this.profilingInfo = profilingInfo;
    this.features = Map.copyOf(Objects.requireNonNull(features, "features cannot be null"));
    this.options = Map.copyOf(Objects.requireNonNull(options, "options cannot be null"));
  }

  /**
   * Gets the name of the compiler used.
   *
   * @return compiler name
   */
  public String getCompilerName() {
    return compilerName;
  }

  /**
   * Gets the optimization level used.
   *
   * @return optimization level
   */
  public String getOptimizationLevel() {
    return optimizationLevel;
  }

  /**
   * Checks if debug information was included.
   *
   * @return true if debug info was included
   */
  public boolean hasDebugInfo() {
    return debugInfo;
  }

  /**
   * Checks if profiling information was included.
   *
   * @return true if profiling info was included
   */
  public boolean hasProfilingInfo() {
    return profilingInfo;
  }

  /**
   * Gets the enabled features.
   *
   * @return map of enabled features
   */
  public Map<String, Object> getFeatures() {
    return features;
  }

  /**
   * Gets the compiler options.
   *
   * @return map of compiler options
   */
  public Map<String, Object> getOptions() {
    return options;
  }

  /**
   * Checks if a specific feature is enabled.
   *
   * @param featureName the feature name
   * @return true if the feature is enabled
   */
  public boolean isFeatureEnabled(final String featureName) {
    final Object value = features.get(featureName);
    if (value instanceof Boolean) {
      return (Boolean) value;
    }
    return features.containsKey(featureName);
  }

  /**
   * Gets a specific compiler option value.
   *
   * @param optionName the option name
   * @return option value, or null if not set
   */
  public Object getOption(final String optionName) {
    return options.get(optionName);
  }

  /**
   * Checks if optimizations are enabled.
   *
   * @return true if optimization level is not "none"
   */
  public boolean hasOptimizations() {
    return !"none".equals(optimizationLevel.toLowerCase(Locale.ROOT));
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final CompilerConfig that = (CompilerConfig) obj;
    return debugInfo == that.debugInfo
        && profilingInfo == that.profilingInfo
        && Objects.equals(compilerName, that.compilerName)
        && Objects.equals(optimizationLevel, that.optimizationLevel)
        && Objects.equals(features, that.features)
        && Objects.equals(options, that.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        compilerName, optimizationLevel, debugInfo, profilingInfo, features, options);
  }

  @Override
  public String toString() {
    return String.format(
        "CompilerConfig{compilerName='%s', optimizationLevel='%s', debugInfo=%s, "
            + "profilingInfo=%s, features=%s, options=%s}",
        compilerName, optimizationLevel, debugInfo, profilingInfo, features, options);
  }
}
