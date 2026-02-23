package ai.tegmentum.wasmtime4j.exception;

/**
 * Base class for all WebAssembly trap exceptions.
 *
 * <p>This exception is thrown when WebAssembly execution encounters a trap condition, such as
 * out-of-bounds memory access, stack overflow, or arithmetic errors. Traps represent runtime
 * failures that terminate WebAssembly execution and cannot be caught by the WebAssembly code
 * itself.
 *
 * <p>Trap exceptions provide detailed information about the trap condition:
 *
 * <ul>
 *   <li>Trap type and cause
 *   <li>WebAssembly backtrace (if available)
 *   <li>Function and instruction context
 *   <li>Recovery suggestions
 * </ul>
 *
 * <p>This class serves as the base for all specific trap types and provides consistent error
 * handling across different runtime implementations.
 *
 * @since 1.0.0
 */
public class TrapException extends WasmException {

  private static final long serialVersionUID = 1L;

  /** The specific trap type that occurred. */
  private final TrapType trapType;

  /** WebAssembly backtrace information (if available). */
  private final String wasmBacktrace;

  /** Function name where the trap occurred (if available). */
  private final String functionName;

  /** Instruction offset where the trap occurred (if available). */
  private final Integer instructionOffset;

  /** Recovery suggestion for this trap type. */
  private final String recoverySuggestion;

  /** Enumeration of WebAssembly trap types matching Wasmtime 41.0.3 Trap codes. */
  public enum TrapType {
    /** The current stack space was exhausted. */
    STACK_OVERFLOW("Stack overflow - recursion depth exceeded limit"),
    /** An out-of-bounds memory access occurred. */
    MEMORY_OUT_OF_BOUNDS("Memory access out of bounds"),
    /** A wasm atomic operation with misaligned memory address. */
    HEAP_MISALIGNED("Atomic operation with misaligned memory address"),
    /** An out-of-bounds access to a table occurred. */
    TABLE_OUT_OF_BOUNDS("Table access out of bounds"),
    /** Indirect call to a null table entry. */
    INDIRECT_CALL_TO_NULL("Indirect call to null table entry"),
    /** Signature mismatch on indirect call. */
    BAD_SIGNATURE("Function signature mismatch on indirect call"),
    /** An integer arithmetic operation caused an overflow. */
    INTEGER_OVERFLOW("Integer arithmetic overflow"),
    /** An integer division by zero occurred. */
    INTEGER_DIVISION_BY_ZERO("Integer division by zero"),
    /** Failed float-to-int conversion. */
    BAD_CONVERSION_TO_INTEGER("Invalid float-to-integer conversion"),
    /** Code that was supposed to be unreachable was reached. */
    UNREACHABLE_CODE_REACHED("Unreachable code was executed"),
    /** Execution has potentially run too long and was interrupted. */
    INTERRUPT("Execution interrupted (timeout or cancellation)"),
    /** Component model canonical lift/lower adapter that always traps. */
    ALWAYS_TRAP_ADAPTER("Component model always-trap adapter called"),
    /** Wasm code ran out of configured fuel. */
    OUT_OF_FUEL("Execution ran out of fuel"),
    /** Atomic wait operation on non-shared memory. */
    ATOMIC_WAIT_NON_SHARED_MEMORY("Atomic wait on non-shared memory"),
    /** Call to a null reference (GC proposal). */
    NULL_REFERENCE("Call to null reference"),
    /** Attempt to access beyond the bounds of an array (GC proposal). */
    ARRAY_OUT_OF_BOUNDS("Array access out of bounds"),
    /** Attempted allocation that was too large to succeed (GC proposal). */
    ALLOCATION_TOO_LARGE("Allocation too large"),
    /** Attempted cast to a type that the reference is not an instance of (GC proposal). */
    CAST_FAILURE("Reference cast failure"),
    /** Component tried to call another component violating reentrance rules. */
    CANNOT_ENTER_COMPONENT("Cannot enter component due to reentrance rules"),
    /** Async-lifted export failed to produce a result. */
    NO_ASYNC_RESULT("Async export did not produce a result"),
    /** Suspension to a tag for which there is no active handler. */
    UNHANDLED_TAG("Unhandled tag during suspension"),
    /** Attempt to resume a continuation that was already consumed. */
    CONTINUATION_ALREADY_CONSUMED("Continuation already consumed"),
    /** A Pulley opcode was disabled at compile time but executed at runtime. */
    DISABLED_OPCODE("Disabled opcode executed"),
    /** Async event loop deadlocked with no further progress possible. */
    ASYNC_DEADLOCK("Async event loop deadlocked"),
    /** Component instance tried to call import when not allowed (e.g. from post-return). */
    CANNOT_LEAVE_COMPONENT("Cannot leave component from current context"),
    /** Synchronous task attempted a potentially blocking call. */
    CANNOT_BLOCK_SYNC_TASK("Synchronous task cannot make blocking call"),
    /** Component tried to lift a char with an invalid bit pattern. */
    INVALID_CHAR("Invalid character bit pattern"),
    /** Component string access past the end of memory. */
    STRING_OUT_OF_BOUNDS("String access out of bounds"),
    /** Component list access past the end of memory. */
    LIST_OUT_OF_BOUNDS("List access out of bounds"),
    /** Component used an invalid discriminant when lowering a variant. */
    INVALID_DISCRIMINANT("Invalid discriminant for variant"),
    /** Component passed an unaligned pointer when lifting or lowering. */
    UNALIGNED_POINTER("Unaligned pointer in component operation"),
    /** Unknown or unspecified trap condition. */
    UNKNOWN("Unknown trap condition");

    private final String description;

    TrapType(final String description) {
      this.description = description;
    }

    /**
     * Gets a human-readable description of this trap type.
     *
     * @return the trap type description
     */
    public String getDescription() {
      return description;
    }
  }

  /**
   * Creates a new trap exception with the specified trap type and message.
   *
   * @param trapType the specific trap type
   * @param message the error message
   */
  public TrapException(final TrapType trapType, final String message) {
    this(trapType, message, null, null, null, null);
  }

  /**
   * Creates a new trap exception with the specified trap type, message, and cause.
   *
   * @param trapType the specific trap type
   * @param message the error message
   * @param cause the underlying cause
   */
  public TrapException(final TrapType trapType, final String message, final Throwable cause) {
    this(trapType, message, null, null, null, cause);
  }

  /**
   * Creates a new trap exception with detailed trap information.
   *
   * @param trapType the specific trap type
   * @param message the error message
   * @param wasmBacktrace WebAssembly backtrace (may be null)
   * @param functionName function where trap occurred (may be null)
   * @param instructionOffset instruction offset where trap occurred (may be null)
   * @param cause the underlying cause (may be null)
   */
  public TrapException(
      final TrapType trapType,
      final String message,
      final String wasmBacktrace,
      final String functionName,
      final Integer instructionOffset,
      final Throwable cause) {
    super(formatMessage(trapType, message, functionName, instructionOffset), cause);
    this.trapType = trapType != null ? trapType : TrapType.UNKNOWN;
    this.wasmBacktrace = wasmBacktrace;
    this.functionName = functionName;
    this.instructionOffset = instructionOffset;
    this.recoverySuggestion = generateRecoverySuggestion(this.trapType);
  }

  /**
   * Gets the specific trap type that occurred.
   *
   * @return the trap type
   */
  public TrapType getTrapType() {
    return trapType;
  }

  /**
   * Gets the WebAssembly backtrace information.
   *
   * @return the backtrace string, or null if not available
   */
  public String getWasmBacktrace() {
    return wasmBacktrace;
  }

  /**
   * Gets the function name where the trap occurred.
   *
   * @return the function name, or null if not available
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the instruction offset where the trap occurred.
   *
   * @return the instruction offset, or null if not available
   */
  public Integer getInstructionOffset() {
    return instructionOffset;
  }

  /**
   * Gets a recovery suggestion for this trap type.
   *
   * @return the recovery suggestion
   */
  public String getRecoverySuggestion() {
    return recoverySuggestion;
  }

  /**
   * Checks if this trap represents a memory-related error.
   *
   * @return true if this is a memory error, false otherwise
   */
  public boolean isMemoryError() {
    return trapType == TrapType.MEMORY_OUT_OF_BOUNDS
        || trapType == TrapType.HEAP_MISALIGNED
        || trapType == TrapType.STACK_OVERFLOW
        || trapType == TrapType.ATOMIC_WAIT_NON_SHARED_MEMORY;
  }

  /**
   * Checks if this trap represents an arithmetic error.
   *
   * @return true if this is an arithmetic error, false otherwise
   */
  public boolean isArithmeticError() {
    return trapType == TrapType.INTEGER_OVERFLOW
        || trapType == TrapType.INTEGER_DIVISION_BY_ZERO
        || trapType == TrapType.BAD_CONVERSION_TO_INTEGER;
  }

  /**
   * Checks if this trap represents a control flow error.
   *
   * @return true if this is a control flow error, false otherwise
   */
  public boolean isControlFlowError() {
    return trapType == TrapType.INDIRECT_CALL_TO_NULL
        || trapType == TrapType.BAD_SIGNATURE
        || trapType == TrapType.UNREACHABLE_CODE_REACHED
        || trapType == TrapType.NULL_REFERENCE
        || trapType == TrapType.CAST_FAILURE
        || trapType == TrapType.ALWAYS_TRAP_ADAPTER;
  }

  /**
   * Checks if this trap represents a resource exhaustion error.
   *
   * @return true if this is a resource exhaustion error, false otherwise
   */
  public boolean isResourceExhaustionError() {
    return trapType == TrapType.STACK_OVERFLOW
        || trapType == TrapType.OUT_OF_FUEL
        || trapType == TrapType.INTERRUPT
        || trapType == TrapType.ALLOCATION_TOO_LARGE;
  }

  /**
   * Checks if this trap represents a bounds checking error.
   *
   * @return true if this is a bounds checking error, false otherwise
   */
  public boolean isBoundsError() {
    return trapType == TrapType.MEMORY_OUT_OF_BOUNDS
        || trapType == TrapType.TABLE_OUT_OF_BOUNDS
        || trapType == TrapType.ARRAY_OUT_OF_BOUNDS
        || trapType == TrapType.STRING_OUT_OF_BOUNDS
        || trapType == TrapType.LIST_OUT_OF_BOUNDS;
  }

  /**
   * Formats the exception message with trap details.
   *
   * @param trapType the trap type
   * @param message the base message
   * @param functionName the function name
   * @param instructionOffset the instruction offset
   * @return the formatted message
   */
  private static String formatMessage(
      final TrapType trapType,
      final String message,
      final String functionName,
      final Integer instructionOffset) {
    if (message == null || message.isEmpty()) {
      throw new IllegalArgumentException("Message cannot be null or empty");
    }

    final StringBuilder sb = new StringBuilder();

    if (trapType != null) {
      sb.append("[").append(trapType.name()).append("] ");
    }

    sb.append(message);

    if (functionName != null && !functionName.isEmpty()) {
      sb.append(" (function: ").append(functionName).append(")");
    }

    if (instructionOffset != null) {
      sb.append(" (offset: ").append(instructionOffset).append(")");
    }

    return sb.toString();
  }

  /**
   * Generates a recovery suggestion based on the trap type.
   *
   * @param trapType the trap type
   * @return a recovery suggestion
   */
  private static String generateRecoverySuggestion(final TrapType trapType) {
    switch (trapType) {
      case STACK_OVERFLOW:
        return "Reduce recursion depth or increase stack size limits";
      case MEMORY_OUT_OF_BOUNDS:
        return "Check array indices and memory access patterns in WebAssembly code";
      case HEAP_MISALIGNED:
        return "Ensure atomic operations use naturally-aligned memory addresses";
      case TABLE_OUT_OF_BOUNDS:
        return "Verify table indices are within valid range";
      case INDIRECT_CALL_TO_NULL:
        return "Initialize function table entries before calling";
      case BAD_SIGNATURE:
        return "Verify function signatures match between caller and callee";
      case INTEGER_OVERFLOW:
        return "Use larger integer types or add overflow checking";
      case INTEGER_DIVISION_BY_ZERO:
        return "Add zero divisor checks before division operations";
      case BAD_CONVERSION_TO_INTEGER:
        return "Validate floating-point values before integer conversion";
      case UNREACHABLE_CODE_REACHED:
        return "Review code paths to ensure unreachable instructions are not executed";
      case INTERRUPT:
        return "Increase execution timeout or optimize WebAssembly code";
      case ALWAYS_TRAP_ADAPTER:
        return "Avoid calling component model adapters that are not intended to be called directly";
      case OUT_OF_FUEL:
        return "Increase fuel limit or optimize WebAssembly code for better performance";
      case ATOMIC_WAIT_NON_SHARED_MEMORY:
        return "Use shared memory for atomic wait operations (enable wasmThreads)";
      case NULL_REFERENCE:
        return "Check for null references before dereferencing";
      case ARRAY_OUT_OF_BOUNDS:
        return "Validate array indices before access";
      case ALLOCATION_TOO_LARGE:
        return "Reduce allocation size or increase memory limits";
      case CAST_FAILURE:
        return "Verify reference types before casting";
      case CANNOT_ENTER_COMPONENT:
        return "Avoid reentrant calls between components";
      case NO_ASYNC_RESULT:
        return "Ensure async exports call task.return before completing";
      case UNHANDLED_TAG:
        return "Add a handler for the tag being suspended to";
      case CONTINUATION_ALREADY_CONSUMED:
        return "Do not resume a continuation more than once";
      case DISABLED_OPCODE:
        return "Enable the required feature at compile time";
      case ASYNC_DEADLOCK:
        return "Ensure async tasks can make progress and avoid circular dependencies";
      case CANNOT_LEAVE_COMPONENT:
        return "Do not call imports from post-return functions";
      case CANNOT_BLOCK_SYNC_TASK:
        return "Use async operations or ensure the task completes synchronously";
      case INVALID_CHAR:
        return "Ensure character values are valid Unicode code points";
      case STRING_OUT_OF_BOUNDS:
        return "Verify string pointers and lengths are within memory bounds";
      case LIST_OUT_OF_BOUNDS:
        return "Verify list pointers and lengths are within memory bounds";
      case INVALID_DISCRIMINANT:
        return "Ensure variant discriminants are within valid range";
      case UNALIGNED_POINTER:
        return "Ensure pointers are properly aligned for their type";
      case UNKNOWN:
      default:
        return "Review WebAssembly code for potential runtime issues";
    }
  }
}
