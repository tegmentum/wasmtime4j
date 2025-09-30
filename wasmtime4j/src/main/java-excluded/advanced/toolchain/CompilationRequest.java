package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Request for WebAssembly compilation operations.
 *
 * <p>Encapsulates all parameters needed for compiling source code
 * to WebAssembly, including source files, compilation options,
 * optimization settings, and output configuration.
 *
 * @since 1.0.0
 */
public final class CompilationRequest {

  private final List<Path> sourceFiles;
  private final Path outputPath;
  private final String targetTriple;
  private final Map<String, String> compilationFlags;
  private final Map<String, String> environmentVariables;
  private final Optional<Duration> timeout;
  private final boolean optimizationEnabled;
  private final int optimizationLevel;
  private final boolean debugInfoEnabled;
  private final ToolchainType toolchainType;

  private CompilationRequest(final Builder builder) {
    this.sourceFiles = Collections.unmodifiableList(builder.sourceFiles);
    this.outputPath = Objects.requireNonNull(builder.outputPath);
    this.targetTriple = builder.targetTriple;
    this.compilationFlags = Collections.unmodifiableMap(new HashMap<>(builder.compilationFlags));
    this.environmentVariables = Collections.unmodifiableMap(new HashMap<>(builder.environmentVariables));
    this.timeout = Optional.ofNullable(builder.timeout);
    this.optimizationEnabled = builder.optimizationEnabled;
    this.optimizationLevel = builder.optimizationLevel;
    this.debugInfoEnabled = builder.debugInfoEnabled;
    this.toolchainType = Objects.requireNonNull(builder.toolchainType);
  }

  /**
   * Creates a new compilation request builder.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the source files to compile.
   *
   * @return list of source file paths
   */
  public List<Path> getSourceFiles() {
    return sourceFiles;
  }

  /**
   * Gets the output path for the compiled WebAssembly.
   *
   * @return output file path
   */
  public Path getOutputPath() {
    return outputPath;
  }

  /**
   * Gets the target triple for compilation.
   *
   * @return target triple
   */
  public String getTargetTriple() {
    return targetTriple;
  }

  /**
   * Gets the compilation flags.
   *
   * @return map of flag names to values
   */
  public Map<String, String> getCompilationFlags() {
    return compilationFlags;
  }

  /**
   * Gets the environment variables for compilation.
   *
   * @return map of environment variable names to values
   */
  public Map<String, String> getEnvironmentVariables() {
    return environmentVariables;
  }

  /**
   * Gets the compilation timeout.
   *
   * @return timeout duration, or empty if no timeout
   */
  public Optional<Duration> getTimeout() {
    return timeout;
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
   * Gets the optimization level.
   *
   * @return optimization level (0-3)
   */
  public int getOptimizationLevel() {
    return optimizationLevel;
  }

  /**
   * Checks if debug information should be included.
   *
   * @return true if debug info is enabled
   */
  public boolean isDebugInfoEnabled() {
    return debugInfoEnabled;
  }

  /**
   * Gets the toolchain type for compilation.
   *
   * @return toolchain type
   */
  public ToolchainType getToolchainType() {
    return toolchainType;
  }

  /**
   * Builder for compilation requests.
   */
  public static final class Builder {
    private List<Path> sourceFiles = Collections.emptyList();
    private Path outputPath;
    private String targetTriple = "wasm32-unknown-unknown";
    private final Map<String, String> compilationFlags = new HashMap<>();
    private final Map<String, String> environmentVariables = new HashMap<>();
    private Duration timeout;
    private boolean optimizationEnabled = true;
    private int optimizationLevel = 2;
    private boolean debugInfoEnabled = false;
    private ToolchainType toolchainType = ToolchainType.RUST;

    public Builder sourceFiles(final List<Path> sourceFiles) {
      this.sourceFiles = Objects.requireNonNull(sourceFiles);
      return this;
    }

    public Builder sourceFile(final Path sourceFile) {
      this.sourceFiles = List.of(Objects.requireNonNull(sourceFile));
      return this;
    }

    public Builder outputPath(final Path outputPath) {
      this.outputPath = Objects.requireNonNull(outputPath);
      return this;
    }

    public Builder targetTriple(final String targetTriple) {
      this.targetTriple = Objects.requireNonNull(targetTriple);
      return this;
    }

    public Builder compilationFlag(final String name, final String value) {
      this.compilationFlags.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      return this;
    }

    public Builder compilationFlags(final Map<String, String> flags) {
      this.compilationFlags.clear();
      this.compilationFlags.putAll(Objects.requireNonNull(flags));
      return this;
    }

    public Builder environmentVariable(final String name, final String value) {
      this.environmentVariables.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      return this;
    }

    public Builder environmentVariables(final Map<String, String> variables) {
      this.environmentVariables.clear();
      this.environmentVariables.putAll(Objects.requireNonNull(variables));
      return this;
    }

    public Builder timeout(final Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder optimizationEnabled(final boolean enabled) {
      this.optimizationEnabled = enabled;
      return this;
    }

    public Builder optimizationLevel(final int level) {
      if (level < 0 || level > 3) {
        throw new IllegalArgumentException("Optimization level must be between 0 and 3");
      }
      this.optimizationLevel = level;
      return this;
    }

    public Builder debugInfoEnabled(final boolean enabled) {
      this.debugInfoEnabled = enabled;
      return this;
    }

    public Builder toolchainType(final ToolchainType toolchainType) {
      this.toolchainType = Objects.requireNonNull(toolchainType);
      return this;
    }

    public CompilationRequest build() {
      if (outputPath == null) {
        throw new IllegalStateException("Output path must be specified");
      }
      if (sourceFiles.isEmpty()) {
        throw new IllegalStateException("At least one source file must be specified");
      }
      return new CompilationRequest(this);
    }
  }

  @Override
  public String toString() {
    return String.format("CompilationRequest{sourceFiles=%d, output=%s, toolchain=%s, optimization=%s}",
        sourceFiles.size(), outputPath, toolchainType, optimizationEnabled ? "O" + optimizationLevel : "none");
  }
}