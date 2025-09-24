package ai.tegmentum.wasmtime4j.exception;

/**
 * Exception thrown when WebAssembly module compilation fails.
 *
 * <p>This exception is thrown when a validated WebAssembly module cannot be compiled to native code
 * due to compiler limitations, resource constraints, or optimization failures. Compilation errors
 * occur after successful validation but before the module can be instantiated.
 *
 * <p>Module compilation exceptions provide detailed information about compilation failures:
 *
 * <ul>
 *   <li>Compilation error type and phase
 *   <li>Function and optimization context
 *   <li>Resource usage information
 *   <li>Recovery and optimization suggestions
 * </ul>
 *
 * @since 1.0.0
 */
public class ModuleCompilationException extends CompilationException {

  private static final long serialVersionUID = 1L;

  /** The specific compilation error type. */
  private final CompilationErrorType errorType;

  /** Compilation phase where the error occurred. */
  private final CompilationPhase phase;

  /** Function name being compiled (if available). */
  private final String functionName;

  /** Function index being compiled (if available). */
  private final Integer functionIndex;

  /** Recovery suggestion for this compilation error. */
  private final String recoverySuggestion;

  /** Enumeration of WebAssembly compilation error types. */
  public enum CompilationErrorType {
    /** Compiler ran out of memory during compilation. */
    OUT_OF_MEMORY("Compiler ran out of memory"),
    /** Compilation exceeded time limits. */
    TIMEOUT("Compilation timeout exceeded"),
    /** Function is too complex to compile. */
    FUNCTION_TOO_COMPLEX("Function too complex for compilation"),
    /** Unsupported instruction or feature. */
    UNSUPPORTED_INSTRUCTION("Unsupported instruction or feature"),
    /** Code generation failed. */
    CODE_GENERATION_FAILED("Native code generation failed"),
    /** Optimization pass failed. */
    OPTIMIZATION_FAILED("Code optimization failed"),
    /** Register allocation failed. */
    REGISTER_ALLOCATION_FAILED("Register allocation failed"),
    /** Control flow graph construction failed. */
    CFG_CONSTRUCTION_FAILED("Control flow graph construction failed"),
    /** Target architecture not supported. */
    UNSUPPORTED_TARGET("Target architecture not supported"),
    /** Compiler internal error. */
    COMPILER_INTERNAL_ERROR("Compiler internal error"),
    /** Resource limit exceeded during compilation. */
    RESOURCE_LIMIT_EXCEEDED("Compilation resource limit exceeded"),
    /** Feature flag configuration error. */
    FEATURE_CONFIGURATION_ERROR("Feature configuration error"),
    /** Unknown compilation error. */
    UNKNOWN("Unknown compilation error");

    private final String description;

    CompilationErrorType(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this compilation error type.
     *
     * @return the error type description
     */
    public String getDescription() {
      return description;
    }
  }

  /** Enumeration of compilation phases. */
  public enum CompilationPhase {
    /** Initial parsing and setup phase. */
    INITIALIZATION("Initialization"),
    /** Control flow graph construction. */
    CFG_CONSTRUCTION("Control Flow Graph Construction"),
    /** Type checking and validation. */
    TYPE_CHECKING("Type Checking"),
    /** Optimization passes. */
    OPTIMIZATION("Optimization"),
    /** Register allocation. */
    REGISTER_ALLOCATION("Register Allocation"),
    /** Machine code generation. */
    CODE_GENERATION("Code Generation"),
    /** Final linking and setup. */
    FINALIZATION("Finalization"),
    /** Unknown compilation phase. */
    UNKNOWN("Unknown Phase");

    private final String description;

    CompilationPhase(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this compilation phase.
     *
     * @return the phase description
     */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Creates a new module compilation exception with the specified error type and message.
   *
   * @param errorType the specific compilation error type
   * @param message the error message
   */
  public ModuleCompilationException(final CompilationErrorType errorType, final String message) {
    this(errorType, message, CompilationPhase.UNKNOWN, null, null, null);
  }

  /**
   * Creates a new module compilation exception with the specified error type, message, and cause.
   *
   * @param errorType the specific compilation error type
   * @param message the error message
   * @param cause the underlying cause
   */
  public ModuleCompilationException(
      final CompilationErrorType errorType, final String message, final Throwable cause) {
    this(errorType, message, CompilationPhase.UNKNOWN, null, null, cause);
  }

  /**
   * Creates a new module compilation exception with detailed error information.
   *
   * @param errorType the specific compilation error type
   * @param message the error message
   * @param phase compilation phase where error occurred
   * @param functionName function being compiled (may be null)
   * @param functionIndex function index being compiled (may be null)
   * @param cause the underlying cause (may be null)
   */
  public ModuleCompilationException(
      final CompilationErrorType errorType,
      final String message,
      final CompilationPhase phase,
      final String functionName,
      final Integer functionIndex,
      final Throwable cause) {
    super(formatMessage(errorType, message, phase, functionName, functionIndex), cause);
    this.errorType = errorType != null ? errorType : CompilationErrorType.UNKNOWN;
    this.phase = phase != null ? phase : CompilationPhase.UNKNOWN;
    this.functionName = functionName;
    this.functionIndex = functionIndex;
    this.recoverySuggestion = generateRecoverySuggestion(this.errorType);
  }

  /**
   * Gets the specific compilation error type.
   *
   * @return the compilation error type
   */
  public CompilationErrorType getErrorType() {
    return errorType;
  }

  /**
   * Gets the compilation phase where the error occurred.
   *
   * @return the compilation phase
   */
  public CompilationPhase getPhase() {
    return phase;
  }

  /**
   * Gets the function name being compiled.
   *
   * @return the function name, or null if not available
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the function index being compiled.
   *
   * @return the function index, or null if not available
   */
  public Integer getFunctionIndex() {
    return functionIndex;
  }

  /**
   * Gets a recovery suggestion for this compilation error.
   *
   * @return the recovery suggestion
   */
  public String getRecoverySuggestion() {
    return recoverySuggestion;
  }

  /**
   * Checks if this compilation error is related to resource constraints.
   *
   * @return true if this is a resource error, false otherwise
   */
  public boolean isResourceError() {
    return errorType == CompilationErrorType.OUT_OF_MEMORY
        || errorType == CompilationErrorType.TIMEOUT
        || errorType == CompilationErrorType.RESOURCE_LIMIT_EXCEEDED;
  }

  /**
   * Checks if this compilation error is related to code complexity.
   *
   * @return true if this is a complexity error, false otherwise
   */
  public boolean isComplexityError() {
    return errorType == CompilationErrorType.FUNCTION_TOO_COMPLEX
        || errorType == CompilationErrorType.CFG_CONSTRUCTION_FAILED
        || errorType == CompilationErrorType.REGISTER_ALLOCATION_FAILED;
  }

  /**
   * Checks if this compilation error is related to feature support.
   *
   * @return true if this is a feature support error, false otherwise
   */
  public boolean isFeatureError() {
    return errorType == CompilationErrorType.UNSUPPORTED_INSTRUCTION
        || errorType == CompilationErrorType.UNSUPPORTED_TARGET
        || errorType == CompilationErrorType.FEATURE_CONFIGURATION_ERROR;
  }

  /**
   * Checks if this compilation error is an internal compiler error.
   *
   * @return true if this is an internal error, false otherwise
   */
  public boolean isInternalError() {
    return errorType == CompilationErrorType.COMPILER_INTERNAL_ERROR
        || errorType == CompilationErrorType.CODE_GENERATION_FAILED
        || errorType == CompilationErrorType.OPTIMIZATION_FAILED;
  }

  /**
   * Formats the exception message with compilation error details.
   *
   * @param errorType the compilation error type
   * @param message the base message
   * @param phase the compilation phase
   * @param functionName the function name
   * @param functionIndex the function index
   * @return the formatted message
   */
  private static String formatMessage(
      final CompilationErrorType errorType,
      final String message,
      final CompilationPhase phase,
      final String functionName,
      final Integer functionIndex) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder();

    if (errorType != null) {
      sb.append("[").append(errorType.name()).append("] ");
    }

    sb.append(message);

    if (phase != null && phase != CompilationPhase.UNKNOWN) {
      sb.append(" (phase: ").append(phase.getDescription()).append(")");
    }

    if (functionName != null && !functionName.isEmpty()) {
      sb.append(" (function: ").append(functionName).append(")");
    } else if (functionIndex != null) {
      sb.append(" (function index: ").append(functionIndex).append(")");
    }

    return sb.toString();
  }

  /**
   * Generates a recovery suggestion based on the compilation error type.
   *
   * @param errorType the compilation error type
   * @return a recovery suggestion
   */
  private static String generateRecoverySuggestion(final CompilationErrorType errorType) {
    switch (errorType) {
      case OUT_OF_MEMORY:
        return "Increase heap size or split module into smaller parts";
      case TIMEOUT:
        return "Increase compilation timeout or simplify WebAssembly code";
      case FUNCTION_TOO_COMPLEX:
        return "Simplify function logic or split into smaller functions";
      case UNSUPPORTED_INSTRUCTION:
        return "Check WebAssembly features are enabled or use supported instructions";
      case CODE_GENERATION_FAILED:
        return "Try different optimization level or target configuration";
      case OPTIMIZATION_FAILED:
        return "Disable specific optimizations or reduce optimization level";
      case REGISTER_ALLOCATION_FAILED:
        return "Reduce function complexity or disable advanced optimizations";
      case CFG_CONSTRUCTION_FAILED:
        return "Simplify control flow or reduce nested block depth";
      case UNSUPPORTED_TARGET:
        return "Use supported target architecture or check build configuration";
      case COMPILER_INTERNAL_ERROR:
        return "Report issue to wasmtime4j maintainers with module details";
      case RESOURCE_LIMIT_EXCEEDED:
        return "Increase compilation resource limits or reduce module size";
      case FEATURE_CONFIGURATION_ERROR:
        return "Check engine configuration for proper feature enablement";
      case UNKNOWN:
      default:
        return "Review module complexity and engine configuration";
    }
  }
}
