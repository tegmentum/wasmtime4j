package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Request for WebAssembly profiling operations.
 *
 * <p>Defines parameters for profiling WebAssembly execution to gather
 * performance metrics and optimization insights.
 *
 * @since 1.0.0
 */
public final class ProfilingRequest {

  private final Path wasmModule;
  private final String functionName;
  private final List<Object> functionArgs;
  private final ProfilingMode profilingMode;
  private final Duration profilingDuration;
  private final Optional<Path> outputFile;
  private final Map<String, Object> profilingOptions;
  private final boolean enableMemoryProfiling;
  private final boolean enableInstructionCounting;
  private final boolean enableCallGraphAnalysis;
  private final int samplingIntervalMs;

  private ProfilingRequest(final Builder builder) {
    this.wasmModule = Objects.requireNonNull(builder.wasmModule);
    this.functionName = builder.functionName;
    this.functionArgs = List.copyOf(builder.functionArgs);
    this.profilingMode = Objects.requireNonNull(builder.profilingMode);
    this.profilingDuration = Objects.requireNonNull(builder.profilingDuration);
    this.outputFile = Optional.ofNullable(builder.outputFile);
    this.profilingOptions = Map.copyOf(builder.profilingOptions);
    this.enableMemoryProfiling = builder.enableMemoryProfiling;
    this.enableInstructionCounting = builder.enableInstructionCounting;
    this.enableCallGraphAnalysis = builder.enableCallGraphAnalysis;
    this.samplingIntervalMs = builder.samplingIntervalMs;
  }

  /**
   * Creates a new profiling request builder.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the WebAssembly module to profile.
   *
   * @return module path
   */
  public Path getWasmModule() {
    return wasmModule;
  }

  /**
   * Gets the function name to profile.
   *
   * @return function name, or null for module-level profiling
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the function arguments.
   *
   * @return list of function arguments
   */
  public List<Object> getFunctionArgs() {
    return functionArgs;
  }

  /**
   * Gets the profiling mode.
   *
   * @return profiling mode
   */
  public ProfilingMode getProfilingMode() {
    return profilingMode;
  }

  /**
   * Gets the profiling duration.
   *
   * @return profiling duration
   */
  public Duration getProfilingDuration() {
    return profilingDuration;
  }

  /**
   * Gets the output file for profiling results.
   *
   * @return output file, or empty for in-memory results
   */
  public Optional<Path> getOutputFile() {
    return outputFile;
  }

  /**
   * Gets the profiling options.
   *
   * @return map of profiling options
   */
  public Map<String, Object> getProfilingOptions() {
    return profilingOptions;
  }

  /**
   * Checks if memory profiling is enabled.
   *
   * @return true if memory profiling is enabled
   */
  public boolean isMemoryProfilingEnabled() {
    return enableMemoryProfiling;
  }

  /**
   * Checks if instruction counting is enabled.
   *
   * @return true if instruction counting is enabled
   */
  public boolean isInstructionCountingEnabled() {
    return enableInstructionCounting;
  }

  /**
   * Checks if call graph analysis is enabled.
   *
   * @return true if call graph analysis is enabled
   */
  public boolean isCallGraphAnalysisEnabled() {
    return enableCallGraphAnalysis;
  }

  /**
   * Gets the sampling interval in milliseconds.
   *
   * @return sampling interval
   */
  public int getSamplingIntervalMs() {
    return samplingIntervalMs;
  }

  /**
   * Builder for profiling requests.
   */
  public static final class Builder {
    private Path wasmModule;
    private String functionName;
    private List<Object> functionArgs = List.of();
    private ProfilingMode profilingMode = ProfilingMode.SAMPLING;
    private Duration profilingDuration = Duration.ofSeconds(10);
    private Path outputFile;
    private Map<String, Object> profilingOptions = Map.of();
    private boolean enableMemoryProfiling = false;
    private boolean enableInstructionCounting = false;
    private boolean enableCallGraphAnalysis = false;
    private int samplingIntervalMs = 1;

    public Builder wasmModule(final Path wasmModule) {
      this.wasmModule = Objects.requireNonNull(wasmModule);
      return this;
    }

    public Builder functionName(final String functionName) {
      this.functionName = functionName;
      return this;
    }

    public Builder functionArgs(final List<Object> functionArgs) {
      this.functionArgs = Objects.requireNonNull(functionArgs);
      return this;
    }

    public Builder profilingMode(final ProfilingMode profilingMode) {
      this.profilingMode = Objects.requireNonNull(profilingMode);
      return this;
    }

    public Builder profilingDuration(final Duration profilingDuration) {
      this.profilingDuration = Objects.requireNonNull(profilingDuration);
      return this;
    }

    public Builder outputFile(final Path outputFile) {
      this.outputFile = outputFile;
      return this;
    }

    public Builder profilingOptions(final Map<String, Object> profilingOptions) {
      this.profilingOptions = Map.copyOf(Objects.requireNonNull(profilingOptions));
      return this;
    }

    public Builder enableMemoryProfiling(final boolean enableMemoryProfiling) {
      this.enableMemoryProfiling = enableMemoryProfiling;
      return this;
    }

    public Builder enableInstructionCounting(final boolean enableInstructionCounting) {
      this.enableInstructionCounting = enableInstructionCounting;
      return this;
    }

    public Builder enableCallGraphAnalysis(final boolean enableCallGraphAnalysis) {
      this.enableCallGraphAnalysis = enableCallGraphAnalysis;
      return this;
    }

    public Builder samplingIntervalMs(final int samplingIntervalMs) {
      if (samplingIntervalMs <= 0) {
        throw new IllegalArgumentException("Sampling interval must be positive");
      }
      this.samplingIntervalMs = samplingIntervalMs;
      return this;
    }

    public ProfilingRequest build() {
      if (wasmModule == null) {
        throw new IllegalStateException("WASM module must be specified");
      }
      return new ProfilingRequest(this);
    }
  }


  @Override
  public String toString() {
    return String.format("ProfilingRequest{module=%s, function=%s, mode=%s, duration=%s}",
        wasmModule, functionName, profilingMode, profilingDuration);
  }
}