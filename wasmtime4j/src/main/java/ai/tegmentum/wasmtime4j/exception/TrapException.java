/*
 * Copyright 2025 Tegmentum AI
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
package ai.tegmentum.wasmtime4j.exception;

import ai.tegmentum.wasmtime4j.debug.WasmBacktrace;

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

  /** Structured backtrace parsed from the error message (if available). */
  private final transient WasmBacktrace structuredBacktrace;

  /** Coredump ID in the native registry, or -1 if no coredump is available. */
  private final long coredumpId;

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
    /**
     * Component model canonical lift/lower adapter that always traps.
     *
     * @deprecated Reserved ordinal slot for forward compatibility. This trap variant does not exist
     *     in wasmtime 42.0.1 and is not produced by the primary trap code path. Retained to
     *     preserve ordinal stability of subsequent enum constants.
     */
    @Deprecated
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
    /** Debug assertion: string encoding not finished. */
    DEBUG_ASSERT_STRING_ENCODING_FINISHED("Debug assertion failed: string encoding not finished"),
    /** Debug assertion: code units are not equal. */
    DEBUG_ASSERT_EQUAL_CODE_UNITS("Debug assertion failed: code units are not equal"),
    /**
     * Debug assertion: may_enter flag was not unset.
     *
     * @deprecated Reserved ordinal slot for forward compatibility. This trap variant does not exist
     *     in wasmtime 42.0.1 and is not produced by the primary trap code path. Retained to
     *     preserve ordinal stability of subsequent enum constants.
     */
    @Deprecated
    DEBUG_ASSERT_MAY_ENTER_UNSET("Debug assertion failed: may_enter flag was not unset"),
    /** Debug assertion: pointer is not properly aligned. */
    DEBUG_ASSERT_POINTER_ALIGNED("Debug assertion failed: pointer is not aligned"),
    /** Debug assertion: upper bits are not unset. */
    DEBUG_ASSERT_UPPER_BITS_UNSET("Debug assertion failed: upper bits are not unset"),
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

  /** Prefix used by the native layer to embed coredump IDs in error messages. */
  private static final String COREDUMP_PREFIX = "[coredump:";

  /** Prefix used by the native layer to embed numeric trap codes in error messages. */
  private static final String TRAP_CODE_PREFIX = "[trap_code:";

  /**
   * Creates a new trap exception with the specified trap type and message.
   *
   * @param trapType the specific trap type
   * @param message the error message
   */
  public TrapException(final TrapType trapType, final String message) {
    this(trapType, message, null, null, null, null, -1L);
  }

  /**
   * Creates a new trap exception with the specified trap type, message, and cause.
   *
   * @param trapType the specific trap type
   * @param message the error message
   * @param cause the underlying cause
   */
  public TrapException(final TrapType trapType, final String message, final Throwable cause) {
    this(trapType, message, null, null, null, cause, -1L);
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
    this(trapType, message, wasmBacktrace, functionName, instructionOffset, cause, -1L);
  }

  /**
   * Creates a new trap exception with detailed trap information and coredump reference.
   *
   * @param trapType the specific trap type
   * @param message the error message
   * @param wasmBacktrace WebAssembly backtrace (may be null)
   * @param functionName function where trap occurred (may be null)
   * @param instructionOffset instruction offset where trap occurred (may be null)
   * @param cause the underlying cause (may be null)
   * @param coredumpId the coredump ID in the native registry, or -1 if not available
   */
  public TrapException(
      final TrapType trapType,
      final String message,
      final String wasmBacktrace,
      final String functionName,
      final Integer instructionOffset,
      final Throwable cause,
      final long coredumpId) {
    super(formatMessage(trapType, message, functionName, instructionOffset), cause);
    this.trapType = trapType != null ? trapType : TrapType.UNKNOWN;
    this.wasmBacktrace = wasmBacktrace;
    this.functionName = functionName;
    this.instructionOffset = instructionOffset;
    this.recoverySuggestion = generateRecoverySuggestion(this.trapType);
    this.structuredBacktrace =
        wasmBacktrace != null
            ? WasmBacktrace.fromErrorMessage(wasmBacktrace)
            : WasmBacktrace.fromErrorMessage(message);
    this.coredumpId = coredumpId;
  }

  /**
   * Creates a TrapException from a native error message, automatically parsing any embedded
   * coredump ID prefix.
   *
   * <p>The native layer embeds coredump IDs in error messages using the format {@code
   * [coredump:ID]message}. This factory method parses that prefix and creates a TrapException with
   * the coredump ID set appropriately.
   *
   * @param trapType the specific trap type
   * @param nativeMessage the raw error message from the native layer (may contain coredump prefix)
   * @return a new TrapException with coredump ID parsed from the message
   */
  public static TrapException fromNativeMessage(
      final TrapType trapType, final String nativeMessage) {
    if (nativeMessage == null) {
      return new TrapException(trapType, "Unknown trap");
    }
    long parsedCoredumpId = -1L;
    TrapType resolvedType = trapType;
    String cleanMessage = nativeMessage;

    // Strip [coredump:ID] prefix if present
    if (cleanMessage.startsWith(COREDUMP_PREFIX)) {
      final int closeBracket = cleanMessage.indexOf(']', COREDUMP_PREFIX.length());
      if (closeBracket > 0) {
        try {
          parsedCoredumpId =
              Long.parseLong(cleanMessage.substring(COREDUMP_PREFIX.length(), closeBracket));
          cleanMessage = cleanMessage.substring(closeBracket + 1);
        } catch (NumberFormatException ignored) {
          // Not a valid coredump prefix, use message as-is
        }
      }
    }

    // Strip [trap_code:N] prefix if present and resolve TrapType from ordinal
    if (cleanMessage.startsWith(TRAP_CODE_PREFIX)) {
      final int closeBracket = cleanMessage.indexOf(']', TRAP_CODE_PREFIX.length());
      if (closeBracket > 0) {
        try {
          final int code =
              Integer.parseInt(cleanMessage.substring(TRAP_CODE_PREFIX.length(), closeBracket));
          final TrapType[] values = TrapType.values();
          if (code >= 0 && code < values.length) {
            resolvedType = values[code];
          }
          cleanMessage = cleanMessage.substring(closeBracket + 1);
        } catch (NumberFormatException ignored) {
          // Not a valid trap code prefix, use message as-is
        }
      }
    }

    return new TrapException(resolvedType, cleanMessage, null, null, null, null, parsedCoredumpId);
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
   * Gets the structured backtrace parsed from the error message.
   *
   * <p>This provides access to individual {@link ai.tegmentum.wasmtime4j.debug.FrameInfo} frames
   * parsed from the wasmtime backtrace text format, enabling programmatic inspection of the call
   * stack.
   *
   * @return the structured backtrace (may be empty if no backtrace was present)
   */
  public WasmBacktrace getStructuredBacktrace() {
    return structuredBacktrace;
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
   * Gets the coredump ID in the native registry.
   *
   * <p>This ID can be used to retrieve detailed coredump information (stack frames, memory
   * snapshots, etc.) from the native coredump registry. The coredump must be explicitly freed when
   * no longer needed.
   *
   * @return the coredump ID, or -1 if no coredump is available
   */
  public long getCoredumpId() {
    return coredumpId;
  }

  /**
   * Checks whether a coredump is available for this trap.
   *
   * <p>A coredump is available when the engine was configured with {@code coredumpOnTrap(true)} and
   * a trap occurred during WebAssembly execution.
   *
   * @return true if a coredump is available
   */
  public boolean hasCoreDump() {
    return coredumpId >= 0;
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
   * Checks if this trap represents a debug assertion failure.
   *
   * <p>Debug assertion traps are internal consistency checks in the component model implementation.
   * They typically indicate a bug in the component model runtime rather than user error.
   *
   * @return true if this is a debug assertion error, false otherwise
   */
  public boolean isDebugAssertError() {
    return trapType == TrapType.DEBUG_ASSERT_STRING_ENCODING_FINISHED
        || trapType == TrapType.DEBUG_ASSERT_EQUAL_CODE_UNITS
        || trapType == TrapType.DEBUG_ASSERT_MAY_ENTER_UNSET
        || trapType == TrapType.DEBUG_ASSERT_POINTER_ALIGNED
        || trapType == TrapType.DEBUG_ASSERT_UPPER_BITS_UNSET;
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
      case DEBUG_ASSERT_STRING_ENCODING_FINISHED:
        return "Debug assertion in component model string encoding; report as a bug";
      case DEBUG_ASSERT_EQUAL_CODE_UNITS:
        return "Debug assertion in component model string transcoding; report as a bug";
      case DEBUG_ASSERT_MAY_ENTER_UNSET:
        return "Debug assertion in component model reentrance guard; report as a bug";
      case DEBUG_ASSERT_POINTER_ALIGNED:
        return "Debug assertion for pointer alignment in component model; report as a bug";
      case DEBUG_ASSERT_UPPER_BITS_UNSET:
        return "Debug assertion for value bit-width in component model; report as a bug";
      case UNKNOWN:
      default:
        return "Review WebAssembly code for potential runtime issues";
    }
  }
}
