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
 * Request for WebAssembly optimization operations.
 *
 * <p>Defines parameters for optimizing existing WebAssembly modules,
 * including optimization level, target constraints, and performance goals.
 *
 * @since 1.0.0
 */
public final class OptimizationRequest {

  private final Path inputFile;
  private final Path outputFile;
  private final int optimizationLevel;
  private final List<String> optimizationPasses;
  private final Map<String, String> optimizationFlags;
  private final Optional<Duration> timeout;
  private final OptimizationGoal goal;
  private final Map<String, Object> constraints;

  private OptimizationRequest(final Builder builder) {
    this.inputFile = Objects.requireNonNull(builder.inputFile);
    this.outputFile = Objects.requireNonNull(builder.outputFile);
    this.optimizationLevel = builder.optimizationLevel;
    this.optimizationPasses = Collections.unmodifiableList(builder.optimizationPasses);
    this.optimizationFlags = Collections.unmodifiableMap(new HashMap<>(builder.optimizationFlags));
    this.timeout = Optional.ofNullable(builder.timeout);
    this.goal = Objects.requireNonNull(builder.goal);
    this.constraints = Collections.unmodifiableMap(new HashMap<>(builder.constraints));
  }

  /**
   * Creates a new optimization request builder.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the input WebAssembly file to optimize.
   *
   * @return input file path
   */
  public Path getInputFile() {
    return inputFile;
  }

  /**
   * Gets the output file for the optimized WebAssembly.
   *
   * @return output file path
   */
  public Path getOutputFile() {
    return outputFile;
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
   * Gets the specific optimization passes to run.
   *
   * @return list of optimization pass names
   */
  public List<String> getOptimizationPasses() {
    return optimizationPasses;
  }

  /**
   * Gets the optimization flags.
   *
   * @return map of flag names to values
   */
  public Map<String, String> getOptimizationFlags() {
    return optimizationFlags;
  }

  /**
   * Gets the optimization timeout.
   *
   * @return timeout duration, or empty if no timeout
   */
  public Optional<Duration> getTimeout() {
    return timeout;
  }

  /**
   * Gets the optimization goal.
   *
   * @return optimization goal
   */
  public OptimizationGoal getGoal() {
    return goal;
  }

  /**
   * Gets the optimization constraints.
   *
   * @return map of constraint names to values
   */
  public Map<String, Object> getConstraints() {
    return constraints;
  }

  /**
   * Builder for optimization requests.
   */
  public static final class Builder {
    private Path inputFile;
    private Path outputFile;
    private int optimizationLevel = 2;
    private List<String> optimizationPasses = Collections.emptyList();
    private final Map<String, String> optimizationFlags = new HashMap<>();
    private Duration timeout;
    private OptimizationGoal goal = OptimizationGoal.BALANCED;
    private final Map<String, Object> constraints = new HashMap<>();

    public Builder inputFile(final Path inputFile) {
      this.inputFile = Objects.requireNonNull(inputFile);
      return this;
    }

    public Builder outputFile(final Path outputFile) {
      this.outputFile = Objects.requireNonNull(outputFile);
      return this;
    }

    public Builder optimizationLevel(final int level) {
      if (level < 0 || level > 3) {
        throw new IllegalArgumentException("Optimization level must be between 0 and 3");
      }
      this.optimizationLevel = level;
      return this;
    }

    public Builder optimizationPasses(final List<String> passes) {
      this.optimizationPasses = Objects.requireNonNull(passes);
      return this;
    }

    public Builder optimizationFlag(final String name, final String value) {
      this.optimizationFlags.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      return this;
    }

    public Builder optimizationFlags(final Map<String, String> flags) {
      this.optimizationFlags.clear();
      this.optimizationFlags.putAll(Objects.requireNonNull(flags));
      return this;
    }

    public Builder timeout(final Duration timeout) {
      this.timeout = timeout;
      return this;
    }

    public Builder goal(final OptimizationGoal goal) {
      this.goal = Objects.requireNonNull(goal);
      return this;
    }

    public Builder constraint(final String name, final Object value) {
      this.constraints.put(Objects.requireNonNull(name), Objects.requireNonNull(value));
      return this;
    }

    public Builder constraints(final Map<String, Object> constraints) {
      this.constraints.clear();
      this.constraints.putAll(Objects.requireNonNull(constraints));
      return this;
    }

    public OptimizationRequest build() {
      if (inputFile == null) {
        throw new IllegalStateException("Input file must be specified");
      }
      if (outputFile == null) {
        throw new IllegalStateException("Output file must be specified");
      }
      return new OptimizationRequest(this);
    }
  }

  /**
   * Optimization goals for WebAssembly modules.
   */
  public enum OptimizationGoal {
    /** Optimize for execution speed */
    SPEED,

    /** Optimize for binary size */
    SIZE,

    /** Optimize for compilation time */
    COMPILE_TIME,

    /** Balanced optimization */
    BALANCED,

    /** Optimize for memory usage */
    MEMORY
  }

  @Override
  public String toString() {
    return String.format("OptimizationRequest{input=%s, output=%s, level=O%d, goal=%s}",
        inputFile, outputFile, optimizationLevel, goal);
  }
}