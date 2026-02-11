package ai.tegmentum.wasmtime4j.config.profiles;

import ai.tegmentum.wasmtime4j.config.EngineConfig;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;
import ai.tegmentum.wasmtime4j.WasmFeature;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Template-based optimization configuration for common use case patterns.
 *
 * <p>This class provides configurable optimization templates that can be customized and combined to
 * create tailored engine configurations for specific application needs.
 *
 * @since 1.0.0
 */
public final class OptimizationTemplate {

  private final String name;
  private final String description;
  private final TemplateType type;
  private final Map<String, Object> parameters;
  private final Set<WasmFeature> requiredFeatures;
  private final Set<WasmFeature> optionalFeatures;

  private OptimizationTemplate(final Builder builder) {
    this.name = Objects.requireNonNull(builder.name, "Template name cannot be null");
    this.description =
        Objects.requireNonNull(builder.description, "Template description cannot be null");
    this.type = Objects.requireNonNull(builder.type, "Template type cannot be null");
    this.parameters = Map.copyOf(builder.parameters);
    this.requiredFeatures = Set.copyOf(builder.requiredFeatures);
    this.optionalFeatures = Set.copyOf(builder.optionalFeatures);
  }

  /** Template types for different optimization strategies. */
  public enum TemplateType {
    /** CPU-bound workload optimization. Also suitable for batch processing and ML inference. */
    CPU_INTENSIVE,
    /** I/O-bound workload optimization. Also suitable for web applications and microservices. */
    IO_INTENSIVE,
    /** Memory-constrained optimization. */
    MEMORY_CONSTRAINED,
    /** Real-time performance optimization. Also suitable for game engines and latency-sensitive apps. */
    REAL_TIME
  }

  /**
   * Apply this optimization template to an engine configuration.
   *
   * @param config the engine configuration to modify
   * @return a new modified engine configuration (original is unchanged)
   * @throws IllegalArgumentException if config is null
   */
  public EngineConfig applyTo(final EngineConfig config) {
    Objects.requireNonNull(config, "Engine configuration cannot be null");

    // Create a copy to preserve immutability
    EngineConfig modifiedConfig = config.copy();

    // Apply type-specific optimizations
    switch (type) {
      case CPU_INTENSIVE:
        modifiedConfig = applyCpuIntensiveOptimizations(modifiedConfig);
        break;
      case IO_INTENSIVE:
        modifiedConfig = applyIoIntensiveOptimizations(modifiedConfig);
        break;
      case MEMORY_CONSTRAINED:
        modifiedConfig = applyMemoryConstrainedOptimizations(modifiedConfig);
        break;
      case REAL_TIME:
        modifiedConfig = applyRealTimeOptimizations(modifiedConfig);
        break;
      default:
        // No optimization changes for unknown types
        break;
    }

    // Apply custom parameters
    modifiedConfig = applyCustomParameters(modifiedConfig);

    // Apply required features
    if (!requiredFeatures.isEmpty()) {
      final Set<WasmFeature> features = new HashSet<>(modifiedConfig.getWasmFeatures());
      features.addAll(requiredFeatures);
      modifiedConfig = modifiedConfig.setWasmFeatures(features);
    }

    return modifiedConfig;
  }

  private EngineConfig applyCpuIntensiveOptimizations(final EngineConfig config) {
    final Map<String, String> settings = new HashMap<>();
    settings.put("opt_level", "speed");
    settings.put("enable_verifier", "false");
    settings.put("enable_nan_canonicalization", "false");
    settings.put("enable_jump_threading", "true");
    settings.put("enable_alias_analysis", "true");
    settings.put("enable_llvm_abi_extensions", "true");
    settings.put("regalloc", "backtracking");
    settings.put("enable_probestack", "false");
    settings.put("enable_safepoints", "false");

    return config
        .optimizationLevel(OptimizationLevel.SPEED)
        .parallelCompilation(true)
        .craneliftDebugVerifier(false)
        .setGenerateDebugInfo(false)
        .setCraneliftSettings(settings);
  }

  private EngineConfig applyIoIntensiveOptimizations(final EngineConfig config) {
    final Map<String, String> settings = new HashMap<>();
    settings.put("opt_level", "speed");
    settings.put("enable_verifier", "false");
    settings.put("enable_nan_canonicalization", "true");
    settings.put("enable_jump_threading", "true");
    settings.put("enable_alias_analysis", "true");
    settings.put("regalloc", "linear_scan");

    return config
        .optimizationLevel(OptimizationLevel.SPEED)
        .parallelCompilation(true)
        .setFuelConsumption(true) // Enable cooperative scheduling
        .setEpochInterruption(true)
        .setCraneliftSettings(settings);
  }

  private EngineConfig applyMemoryConstrainedOptimizations(final EngineConfig config) {
    final Map<String, String> settings = new HashMap<>();
    settings.put("opt_level", "size");
    settings.put("enable_verifier", "false");
    settings.put("enable_nan_canonicalization", "false");
    settings.put("enable_jump_threading", "false");
    settings.put("enable_alias_analysis", "false");
    settings.put("regalloc", "linear_scan");
    settings.put("enable_probestack", "true");

    return config
        .optimizationLevel(OptimizationLevel.SIZE)
        .parallelCompilation(false)
        .setMaxWasmStack(256 * 1024) // 256KB stack limit
        .setAsyncStackSize(128 * 1024) // 128KB async stack
        .setCraneliftSettings(settings);
  }

  private EngineConfig applyRealTimeOptimizations(final EngineConfig config) {
    final Map<String, String> settings = new HashMap<>();
    settings.put("opt_level", "speed");
    settings.put("enable_verifier", "false");
    settings.put("enable_nan_canonicalization", "false");
    settings.put("enable_jump_threading", "true");
    settings.put("enable_alias_analysis", "true");
    settings.put("regalloc", "backtracking");
    settings.put("enable_probestack", "false");
    settings.put("enable_safepoints", "false");

    return config
        .optimizationLevel(OptimizationLevel.SPEED)
        .parallelCompilation(true)
        .setMaxWasmStack(2 * 1024 * 1024) // 2MB stack for reduced allocations
        .setFuelConsumption(false) // Disable for predictable timing
        .setEpochInterruption(false)
        .setCraneliftSettings(settings);
  }

  private EngineConfig applyCustomParameters(final EngineConfig config) {
    EngineConfig modifiedConfig = config;

    // Apply optimization level if specified
    if (parameters.containsKey("optimization_level")) {
      final OptimizationLevel level = (OptimizationLevel) parameters.get("optimization_level");
      modifiedConfig = modifiedConfig.optimizationLevel(level);
    }

    // Apply parallel compilation setting
    if (parameters.containsKey("parallel_compilation")) {
      final Boolean parallel = (Boolean) parameters.get("parallel_compilation");
      modifiedConfig = modifiedConfig.parallelCompilation(parallel);
    }

    // Apply debug info setting
    if (parameters.containsKey("debug_info")) {
      final Boolean debugInfo = (Boolean) parameters.get("debug_info");
      modifiedConfig = modifiedConfig.setGenerateDebugInfo(debugInfo);
    }

    // Apply fuel consumption setting
    if (parameters.containsKey("fuel_consumption")) {
      final Boolean fuel = (Boolean) parameters.get("fuel_consumption");
      modifiedConfig = modifiedConfig.setFuelConsumption(fuel);
    }

    // Apply stack size setting
    if (parameters.containsKey("max_stack_size")) {
      final Long stackSize = (Long) parameters.get("max_stack_size");
      modifiedConfig = modifiedConfig.setMaxWasmStack(stackSize);
    }

    // Apply custom Cranelift settings
    if (parameters.containsKey("cranelift_settings")) {
      @SuppressWarnings("unchecked")
      final Map<String, String> craneliftSettings =
          (Map<String, String>) parameters.get("cranelift_settings");
      modifiedConfig = modifiedConfig.setCraneliftSettings(craneliftSettings);
    }

    return modifiedConfig;
  }

  /**
   * Get the template name.
   *
   * @return template name
   */
  public String getName() {
    return name;
  }

  /**
   * Get the template description.
   *
   * @return template description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Get the template type.
   *
   * @return template type
   */
  public TemplateType getType() {
    return type;
  }

  /**
   * Get the template parameters.
   *
   * @return immutable map of parameters
   */
  public Map<String, Object> getParameters() {
    return parameters;
  }

  /**
   * Get the required WebAssembly features.
   *
   * @return immutable set of required features
   */
  public Set<WasmFeature> getRequiredFeatures() {
    return requiredFeatures;
  }

  /**
   * Get the optional WebAssembly features.
   *
   * @return immutable set of optional features
   */
  public Set<WasmFeature> getOptionalFeatures() {
    return optionalFeatures;
  }

  /**
   * Create a new engine configuration with this template applied.
   *
   * @return new engine configuration
   */
  public EngineConfig createConfig() {
    return applyTo(new EngineConfig());
  }

  /**
   * Create a builder for constructing optimization templates.
   *
   * @param name template name
   * @param type template type
   * @return new builder instance
   */
  public static Builder builder(final String name, final TemplateType type) {
    return new Builder(name, type);
  }

  /**
   * Get predefined templates for common use cases.
   *
   * @return array of predefined templates
   */
  public static OptimizationTemplate[] getPredefinedTemplates() {
    return new OptimizationTemplate[] {
      createCpuIntensiveTemplate(),
      createIoIntensiveTemplate(),
      createMemoryConstrainedTemplate(),
      createRealTimeTemplate()
    };
  }

  /**
   * Find a predefined template by name.
   *
   * @param name template name to search for
   * @return template if found, null otherwise
   */
  public static OptimizationTemplate findTemplate(final String name) {
    for (final OptimizationTemplate template : getPredefinedTemplates()) {
      if (template.getName().equals(name)) {
        return template;
      }
    }
    return null;
  }

  private static OptimizationTemplate createCpuIntensiveTemplate() {
    return builder("CPU Intensive", TemplateType.CPU_INTENSIVE)
        .description(
            "Optimized for CPU-bound computations including batch processing, ML inference, and scientific computing")
        .parameter("optimization_level", OptimizationLevel.SPEED)
        .parameter("parallel_compilation", true)
        .parameter("debug_info", false)
        .parameter("fuel_consumption", false)
        .parameter("max_stack_size", 4L * 1024 * 1024) // 4MB
        .optionalFeature(WasmFeature.SIMD)
        .optionalFeature(WasmFeature.THREADS)
        .build();
  }

  private static OptimizationTemplate createIoIntensiveTemplate() {
    return builder("I/O Intensive", TemplateType.IO_INTENSIVE)
        .description(
            "Optimized for I/O-bound applications including web apps and microservices with cooperative scheduling")
        .parameter("optimization_level", OptimizationLevel.SPEED)
        .parameter("parallel_compilation", true)
        .parameter("fuel_consumption", true)
        .parameter("max_stack_size", 1L * 1024 * 1024) // 1MB
        .requiredFeature(WasmFeature.REFERENCE_TYPES)
        .requiredFeature(WasmFeature.BULK_MEMORY)
        .build();
  }

  private static OptimizationTemplate createMemoryConstrainedTemplate() {
    return builder("Memory Constrained", TemplateType.MEMORY_CONSTRAINED)
        .description("Optimized for environments with limited memory resources")
        .parameter("optimization_level", OptimizationLevel.SIZE)
        .parameter("parallel_compilation", false)
        .parameter("debug_info", false)
        .parameter("max_stack_size", 256L * 1024) // 256KB
        .build();
  }

  private static OptimizationTemplate createRealTimeTemplate() {
    return builder("Real-time", TemplateType.REAL_TIME)
        .description(
            "Optimized for consistent, low-latency real-time performance including game engines and trading systems")
        .parameter("optimization_level", OptimizationLevel.SPEED)
        .parameter("parallel_compilation", true)
        .parameter("fuel_consumption", false)
        .parameter("max_stack_size", 2L * 1024 * 1024) // 2MB
        .optionalFeature(WasmFeature.SIMD)
        .optionalFeature(WasmFeature.THREADS)
        .build();
  }

  /** Builder for creating optimization templates. */
  public static final class Builder {
    private final String name;
    private final TemplateType type;
    private String description = "";
    private final Map<String, Object> parameters = new HashMap<>();
    private final Set<WasmFeature> requiredFeatures = EnumSet.noneOf(WasmFeature.class);
    private final Set<WasmFeature> optionalFeatures = EnumSet.noneOf(WasmFeature.class);

    private Builder(final String name, final TemplateType type) {
      this.name = Objects.requireNonNull(name, "Template name cannot be null");
      this.type = Objects.requireNonNull(type, "Template type cannot be null");
    }

    /**
     * Set the template description.
     *
     * @param description template description
     * @return this builder
     */
    public Builder description(final String description) {
      this.description = Objects.requireNonNull(description, "Description cannot be null");
      return this;
    }

    /**
     * Add a parameter to the template.
     *
     * @param key parameter key
     * @param value parameter value
     * @return this builder
     */
    public Builder parameter(final String key, final Object value) {
      this.parameters.put(
          Objects.requireNonNull(key, "Parameter key cannot be null"),
          Objects.requireNonNull(value, "Parameter value cannot be null"));
      return this;
    }

    /**
     * Add multiple parameters to the template.
     *
     * @param parameters map of parameters to add
     * @return this builder
     */
    public Builder parameters(final Map<String, Object> parameters) {
      Objects.requireNonNull(parameters, "Parameters map cannot be null");
      this.parameters.putAll(parameters);
      return this;
    }

    /**
     * Add a required WebAssembly feature.
     *
     * @param feature required feature
     * @return this builder
     */
    public Builder requiredFeature(final WasmFeature feature) {
      this.requiredFeatures.add(Objects.requireNonNull(feature, "Required feature cannot be null"));
      return this;
    }

    /**
     * Add multiple required WebAssembly features.
     *
     * @param features required features
     * @return this builder
     */
    public Builder requiredFeatures(final Set<WasmFeature> features) {
      Objects.requireNonNull(features, "Required features set cannot be null");
      this.requiredFeatures.addAll(features);
      return this;
    }

    /**
     * Add an optional WebAssembly feature.
     *
     * @param feature optional feature
     * @return this builder
     */
    public Builder optionalFeature(final WasmFeature feature) {
      this.optionalFeatures.add(Objects.requireNonNull(feature, "Optional feature cannot be null"));
      return this;
    }

    /**
     * Add multiple optional WebAssembly features.
     *
     * @param features optional features
     * @return this builder
     */
    public Builder optionalFeatures(final Set<WasmFeature> features) {
      Objects.requireNonNull(features, "Optional features set cannot be null");
      this.optionalFeatures.addAll(features);
      return this;
    }

    /**
     * Build the optimization template.
     *
     * @return new optimization template
     */
    public OptimizationTemplate build() {
      return new OptimizationTemplate(this);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "OptimizationTemplate{name='%s', type=%s, description='%s'}", name, type, description);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final OptimizationTemplate that = (OptimizationTemplate) obj;
    return Objects.equals(name, that.name)
        && type == that.type
        && Objects.equals(description, that.description)
        && Objects.equals(parameters, that.parameters)
        && Objects.equals(requiredFeatures, that.requiredFeatures)
        && Objects.equals(optionalFeatures, that.optionalFeatures);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, type, description, parameters, requiredFeatures, optionalFeatures);
  }
}
