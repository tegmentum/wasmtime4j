package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Configuration for WebAssembly toolchain settings.
 *
 * <p>Defines paths, options, and settings for the WebAssembly compilation
 * toolchain including Rust, Clang, and other build tools.
 *
 * @since 1.0.0
 */
public final class ToolchainConfiguration {

  private final Path toolchainRoot;
  private final Map<String, Path> toolPaths;
  private final Map<String, String> environmentVariables;
  private final Map<String, String> buildFlags;
  private final Optional<String> targetTriple;
  private final boolean crossCompilationEnabled;
  private final boolean optimizationEnabled;
  private final ToolchainType toolchainType;

  private ToolchainConfiguration(final Builder builder) {
    this.toolchainRoot = builder.toolchainRoot;
    this.toolPaths = Collections.unmodifiableMap(new HashMap<>(builder.toolPaths));
    this.environmentVariables = Collections.unmodifiableMap(new HashMap<>(builder.environmentVariables));
    this.buildFlags = Collections.unmodifiableMap(new HashMap<>(builder.buildFlags));
    this.targetTriple = Optional.ofNullable(builder.targetTriple);
    this.crossCompilationEnabled = builder.crossCompilationEnabled;
    this.optimizationEnabled = builder.optimizationEnabled;
    this.toolchainType = builder.toolchainType;
  }

  /**
   * Creates a new toolchain configuration builder.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a default toolchain configuration.
   *
   * @return default configuration
   */
  public static ToolchainConfiguration defaultConfiguration() {
    return builder().build();
  }

  /**
   * Gets the toolchain root directory.
   *
   * @return toolchain root path
   */
  public Optional<Path> getToolchainRoot() {
    return Optional.ofNullable(toolchainRoot);
  }

  /**
   * Gets the path to a specific tool.
   *
   * @param toolName the tool name
   * @return tool path, or empty if not configured
   */
  public Optional<Path> getToolPath(final String toolName) {
    return Optional.ofNullable(toolPaths.get(toolName));
  }

  /**
   * Gets all configured tool paths.
   *
   * @return map of tool names to paths
   */
  public Map<String, Path> getToolPaths() {
    return toolPaths;
  }

  /**
   * Gets environment variables for the toolchain.
   *
   * @return map of environment variable names to values
   */
  public Map<String, String> getEnvironmentVariables() {
    return environmentVariables;
  }

  /**
   * Gets build flags for the toolchain.
   *
   * @return map of flag names to values
   */
  public Map<String, String> getBuildFlags() {
    return buildFlags;
  }

  /**
   * Gets the target triple for cross-compilation.
   *
   * @return target triple, or empty if not specified
   */
  public Optional<String> getTargetTriple() {
    return targetTriple;
  }

  /**
   * Checks if cross-compilation is enabled.
   *
   * @return true if cross-compilation is enabled
   */
  public boolean isCrossCompilationEnabled() {
    return crossCompilationEnabled;
  }

  /**
   * Checks if optimization is enabled.
   *
   * @return true if optimization is enabled
   */
  public boolean isOptimizationEnabled() {
    return optimizationEnabled;
  }

  /**
   * Gets the toolchain type.
   *
   * @return toolchain type
   */
  public ToolchainType getToolchainType() {
    return toolchainType;
  }

  /**
   * Creates a new configuration with modified settings.
   *
   * @return new builder based on this configuration
   */
  public Builder toBuilder() {
    return new Builder()
        .toolchainRoot(toolchainRoot)
        .toolPaths(toolPaths)
        .environmentVariables(environmentVariables)
        .buildFlags(buildFlags)
        .targetTriple(targetTriple.orElse(null))
        .crossCompilationEnabled(crossCompilationEnabled)
        .optimizationEnabled(optimizationEnabled)
        .toolchainType(toolchainType);
  }

  /**
   * Builder for toolchain configuration.
   */
  public static final class Builder {
    private Path toolchainRoot;
    private final Map<String, Path> toolPaths = new HashMap<>();
    private final Map<String, String> environmentVariables = new HashMap<>();
    private final Map<String, String> buildFlags = new HashMap<>();
    private String targetTriple;
    private boolean crossCompilationEnabled = false;
    private boolean optimizationEnabled = true;
    private ToolchainType toolchainType = ToolchainType.RUST;

    public Builder toolchainRoot(final Path toolchainRoot) {
      this.toolchainRoot = toolchainRoot;
      return this;
    }

    public Builder toolPath(final String toolName, final Path path) {
      this.toolPaths.put(Objects.requireNonNull(toolName), Objects.requireNonNull(path));
      return this;
    }

    public Builder toolPaths(final Map<String, Path> toolPaths) {
      this.toolPaths.clear();
      this.toolPaths.putAll(Objects.requireNonNull(toolPaths));
      return this;
    }

    public Builder environmentVariable(final String name, final String value) {
      this.environmentVariables.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      return this;
    }

    public Builder environmentVariables(final Map<String, String> environmentVariables) {
      this.environmentVariables.clear();
      this.environmentVariables.putAll(Objects.requireNonNull(environmentVariables));
      return this;
    }

    public Builder buildFlag(final String name, final String value) {
      this.buildFlags.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      return this;
    }

    public Builder buildFlags(final Map<String, String> buildFlags) {
      this.buildFlags.clear();
      this.buildFlags.putAll(Objects.requireNonNull(buildFlags));
      return this;
    }

    public Builder targetTriple(final String targetTriple) {
      this.targetTriple = targetTriple;
      return this;
    }

    public Builder crossCompilationEnabled(final boolean enabled) {
      this.crossCompilationEnabled = enabled;
      return this;
    }

    public Builder optimizationEnabled(final boolean enabled) {
      this.optimizationEnabled = enabled;
      return this;
    }

    public Builder toolchainType(final ToolchainType toolchainType) {
      this.toolchainType = Objects.requireNonNull(toolchainType);
      return this;
    }

    public ToolchainConfiguration build() {
      return new ToolchainConfiguration(this);
    }
  }
}