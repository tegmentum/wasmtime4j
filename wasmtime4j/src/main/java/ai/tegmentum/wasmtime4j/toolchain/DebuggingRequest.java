package ai.tegmentum.wasmtime4j.toolchain;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Request for WebAssembly debugging operations.
 *
 * <p>Defines parameters for starting a debugging session with a WebAssembly
 * module, including breakpoints, watch expressions, and debugging options.
 *
 * @since 1.0.0
 */
public final class DebuggingRequest {

  private final Path wasmModule;
  private final String functionName;
  private final List<Object> functionArgs;
  private final List<Breakpoint> breakpoints;
  private final List<String> watchExpressions;
  private final DebuggingMode debuggingMode;
  private final Optional<Duration> sessionTimeout;
  private final Map<String, Object> debuggingOptions;
  private final boolean enableStepThrough;
  private final boolean enableVariableInspection;
  private final boolean enableCallStackTracking;
  private final boolean enableMemoryInspection;

  private DebuggingRequest(final Builder builder) {
    this.wasmModule = Objects.requireNonNull(builder.wasmModule);
    this.functionName = builder.functionName;
    this.functionArgs = List.copyOf(builder.functionArgs);
    this.breakpoints = List.copyOf(builder.breakpoints);
    this.watchExpressions = List.copyOf(builder.watchExpressions);
    this.debuggingMode = Objects.requireNonNull(builder.debuggingMode);
    this.sessionTimeout = Optional.ofNullable(builder.sessionTimeout);
    this.debuggingOptions = Map.copyOf(builder.debuggingOptions);
    this.enableStepThrough = builder.enableStepThrough;
    this.enableVariableInspection = builder.enableVariableInspection;
    this.enableCallStackTracking = builder.enableCallStackTracking;
    this.enableMemoryInspection = builder.enableMemoryInspection;
  }

  /**
   * Creates a new debugging request builder.
   *
   * @return new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the WebAssembly module to debug.
   *
   * @return module path
   */
  public Path getWasmModule() {
    return wasmModule;
  }

  /**
   * Gets the function name to debug.
   *
   * @return function name, or null for module-level debugging
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
   * Gets the breakpoints to set.
   *
   * @return list of breakpoints
   */
  public List<Breakpoint> getBreakpoints() {
    return breakpoints;
  }

  /**
   * Gets the watch expressions.
   *
   * @return list of watch expressions
   */
  public List<String> getWatchExpressions() {
    return watchExpressions;
  }

  /**
   * Gets the debugging mode.
   *
   * @return debugging mode
   */
  public DebuggingMode getDebuggingMode() {
    return debuggingMode;
  }

  /**
   * Gets the session timeout.
   *
   * @return session timeout, or empty for no timeout
   */
  public Optional<Duration> getSessionTimeout() {
    return sessionTimeout;
  }

  /**
   * Gets the debugging options.
   *
   * @return map of debugging options
   */
  public Map<String, Object> getDebuggingOptions() {
    return debuggingOptions;
  }

  /**
   * Checks if step-through debugging is enabled.
   *
   * @return true if step-through is enabled
   */
  public boolean isStepThroughEnabled() {
    return enableStepThrough;
  }

  /**
   * Checks if variable inspection is enabled.
   *
   * @return true if variable inspection is enabled
   */
  public boolean isVariableInspectionEnabled() {
    return enableVariableInspection;
  }

  /**
   * Checks if call stack tracking is enabled.
   *
   * @return true if call stack tracking is enabled
   */
  public boolean isCallStackTrackingEnabled() {
    return enableCallStackTracking;
  }

  /**
   * Checks if memory inspection is enabled.
   *
   * @return true if memory inspection is enabled
   */
  public boolean isMemoryInspectionEnabled() {
    return enableMemoryInspection;
  }

  /**
   * Builder for debugging requests.
   */
  public static final class Builder {
    private Path wasmModule;
    private String functionName;
    private List<Object> functionArgs = List.of();
    private List<Breakpoint> breakpoints = List.of();
    private List<String> watchExpressions = List.of();
    private DebuggingMode debuggingMode = DebuggingMode.INTERACTIVE;
    private Duration sessionTimeout;
    private Map<String, Object> debuggingOptions = Map.of();
    private boolean enableStepThrough = true;
    private boolean enableVariableInspection = true;
    private boolean enableCallStackTracking = true;
    private boolean enableMemoryInspection = false;

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

    public Builder breakpoints(final List<Breakpoint> breakpoints) {
      this.breakpoints = Objects.requireNonNull(breakpoints);
      return this;
    }

    public Builder addBreakpoint(final Breakpoint breakpoint) {
      this.breakpoints = List.copyOf(
          java.util.stream.Stream.concat(
              breakpoints.stream(),
              java.util.stream.Stream.of(Objects.requireNonNull(breakpoint))
          ).toList()
      );
      return this;
    }

    public Builder watchExpressions(final List<String> watchExpressions) {
      this.watchExpressions = Objects.requireNonNull(watchExpressions);
      return this;
    }

    public Builder addWatchExpression(final String expression) {
      this.watchExpressions = List.copyOf(
          java.util.stream.Stream.concat(
              watchExpressions.stream(),
              java.util.stream.Stream.of(Objects.requireNonNull(expression))
          ).toList()
      );
      return this;
    }

    public Builder debuggingMode(final DebuggingMode debuggingMode) {
      this.debuggingMode = Objects.requireNonNull(debuggingMode);
      return this;
    }

    public Builder sessionTimeout(final Duration sessionTimeout) {
      this.sessionTimeout = sessionTimeout;
      return this;
    }

    public Builder debuggingOptions(final Map<String, Object> debuggingOptions) {
      this.debuggingOptions = Map.copyOf(Objects.requireNonNull(debuggingOptions));
      return this;
    }

    public Builder enableStepThrough(final boolean enableStepThrough) {
      this.enableStepThrough = enableStepThrough;
      return this;
    }

    public Builder enableVariableInspection(final boolean enableVariableInspection) {
      this.enableVariableInspection = enableVariableInspection;
      return this;
    }

    public Builder enableCallStackTracking(final boolean enableCallStackTracking) {
      this.enableCallStackTracking = enableCallStackTracking;
      return this;
    }

    public Builder enableMemoryInspection(final boolean enableMemoryInspection) {
      this.enableMemoryInspection = enableMemoryInspection;
      return this;
    }

    public DebuggingRequest build() {
      if (wasmModule == null) {
        throw new IllegalStateException("WASM module must be specified");
      }
      return new DebuggingRequest(this);
    }
  }

  /**
   * Debugging modes for WebAssembly execution.
   */
  public enum DebuggingMode {
    /** Interactive debugging with user control */
    INTERACTIVE,

    /** Automated debugging with script control */
    AUTOMATED,

    /** Trace-only mode without stopping execution */
    TRACE_ONLY,

    /** Post-mortem debugging of crashed execution */
    POST_MORTEM
  }

  /**
   * Breakpoint definition for debugging.
   */
  public static final class Breakpoint {
    private final String location;
    private final BreakpointType type;
    private final Optional<String> condition;
    private final boolean enabled;

    public Breakpoint(final String location,
                      final BreakpointType type,
                      final String condition,
                      final boolean enabled) {
      this.location = Objects.requireNonNull(location);
      this.type = Objects.requireNonNull(type);
      this.condition = Optional.ofNullable(condition);
      this.enabled = enabled;
    }

    public static Breakpoint atFunction(final String functionName) {
      return new Breakpoint(functionName, BreakpointType.FUNCTION, null, true);
    }

    public static Breakpoint atLine(final String location) {
      return new Breakpoint(location, BreakpointType.LINE, null, true);
    }

    public static Breakpoint conditional(final String location, final String condition) {
      return new Breakpoint(location, BreakpointType.CONDITIONAL, condition, true);
    }

    public String getLocation() { return location; }
    public BreakpointType getType() { return type; }
    public Optional<String> getCondition() { return condition; }
    public boolean isEnabled() { return enabled; }

    public enum BreakpointType {
      FUNCTION, LINE, CONDITIONAL, MEMORY_ACCESS, EXCEPTION
    }

    @Override
    public String toString() {
      return String.format("Breakpoint{location='%s', type=%s, enabled=%s}",
          location, type, enabled);
    }
  }

  @Override
  public String toString() {
    return String.format("DebuggingRequest{module=%s, function=%s, mode=%s, breakpoints=%d}",
        wasmModule, functionName, debuggingMode, breakpoints.size());
  }
}