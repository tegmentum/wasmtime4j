package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * WebAssembly tail call optimization instructions.
 *
 * <p>The tail call proposal adds proper tail call elimination to WebAssembly, enabling:
 *
 * <ul>
 *   <li>Stack space optimization for recursive functions
 *   <li>Cross-function tail call optimization
 *   <li>Functional programming patterns without stack overflow
 *   <li>Efficient implementation of state machines and continuations
 * </ul>
 *
 * <p>Tail calls replace the current function's stack frame instead of creating a new one, enabling
 * unlimited recursion depth for tail-recursive functions.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Tail call to a function (replaces current stack frame)
 * instance.callTailCall("recursive_function", WasmValue.i32(n - 1));
 *
 * // Tail call indirect through function table
 * instance.callTailCallIndirect(functionIndex, WasmValue.i32(arg));
 * }</pre>
 *
 * @since 1.1.0
 */
public enum TailCallInstructions {

  /**
   * Tail call to a function by index. Replaces the current stack frame with a call to the target
   * function.
   */
  TAIL_CALL(0x12, "tail_call", true, false),

  /**
   * Tail call indirect through a function table. Replaces the current stack frame with an indirect
   * call.
   */
  TAIL_CALL_INDIRECT(0x13, "tail_call_indirect", true, true),

  /**
   * Return call - performs a tail call and immediately returns. Equivalent to tail_call followed by
   * return.
   */
  RETURN_CALL(0x14, "return_call", false, false),

  /**
   * Return call indirect - performs an indirect tail call and returns. Equivalent to
   * tail_call_indirect followed by return.
   */
  RETURN_CALL_INDIRECT(0x15, "return_call_indirect", false, true);

  private final int opcode;
  private final String mnemonic;
  private final boolean replacesFrame;
  private final boolean isIndirect;

  TailCallInstructions(
      final int opcode,
      final String mnemonic,
      final boolean replacesFrame,
      final boolean isIndirect) {
    this.opcode = opcode;
    this.mnemonic = mnemonic;
    this.replacesFrame = replacesFrame;
    this.isIndirect = isIndirect;
  }

  /**
   * Gets the instruction opcode.
   *
   * @return the opcode value
   */
  public int getOpcode() {
    return opcode;
  }

  /**
   * Gets the instruction mnemonic.
   *
   * @return the mnemonic string
   */
  public String getMnemonic() {
    return mnemonic;
  }

  /**
   * Checks if this instruction replaces the current stack frame.
   *
   * @return true if the current frame is replaced, false if a new frame is created
   */
  public boolean replacesFrame() {
    return replacesFrame;
  }

  /**
   * Checks if this is an indirect call instruction.
   *
   * @return true if the call target is determined at runtime
   */
  public boolean isIndirect() {
    return isIndirect;
  }

  /**
   * Executes a tail call instruction.
   *
   * <p>This method provides a generic execution interface for tail call instructions with proper
   * stack frame management.
   *
   * @param context the execution context
   * @param target the call target (function index or table index)
   * @param parameters the function parameters
   * @return the function results (never returns for proper tail calls)
   * @throws WasmException if the tail call fails
   */
  public WasmValue[] execute(
      final TailCallContext context, final Object target, final WasmValue... parameters)
      throws WasmException {

    validateTailCallContext(context);

    switch (this) {
      case TAIL_CALL:
        return executeTailCall(context, (Integer) target, parameters);

      case TAIL_CALL_INDIRECT:
        return executeTailCallIndirect(context, (TailCallIndirectTarget) target, parameters);

      case RETURN_CALL:
        final WasmValue[] results = executeTailCall(context, (Integer) target, parameters);
        context.performReturn(results);
        return results; // Never reached in proper tail call

      case RETURN_CALL_INDIRECT:
        final WasmValue[] indirectResults =
            executeTailCallIndirect(context, (TailCallIndirectTarget) target, parameters);
        context.performReturn(indirectResults);
        return indirectResults; // Never reached in proper tail call

      default:
        throw new UnsupportedOperationException("Unknown tail call instruction: " + mnemonic);
    }
  }

  private WasmValue[] executeTailCall(
      final TailCallContext context, final int functionIndex, final WasmValue... parameters)
      throws WasmException {

    final FunctionReference function = context.resolveFunction(functionIndex);
    if (function == null) {
      throw new WasmException("Function not found at index: " + functionIndex);
    }

    // Validate parameter compatibility
    validateTailCallParameters(function.getFunctionType(), parameters);

    // Prepare for tail call optimization
    if (replacesFrame) {
      context.prepareFrameReplacement();
    }

    // Execute the tail call
    return function.call(parameters);
  }

  private WasmValue[] executeTailCallIndirect(
      final TailCallContext context,
      final TailCallIndirectTarget target,
      final WasmValue... parameters)
      throws WasmException {

    final FunctionReference function =
        context.resolveIndirectFunction(
            target.getTableIndex(), target.getFunctionIndex(), target.getExpectedType());

    if (function == null) {
      throw new WasmException("Indirect function not found");
    }

    // Validate parameter compatibility
    validateTailCallParameters(function.getFunctionType(), parameters);

    // Prepare for tail call optimization
    if (replacesFrame) {
      context.prepareFrameReplacement();
    }

    // Execute the indirect tail call
    return function.call(parameters);
  }

  private void validateTailCallContext(final TailCallContext context) throws WasmException {
    if (context == null) {
      throw new WasmException("Tail call context is required");
    }

    if (!context.supportsTailCalls()) {
      throw new WasmException("Tail calls not supported in this context");
    }
  }

  private void validateTailCallParameters(
      final FunctionType targetType, final WasmValue... parameters) throws WasmException {

    final WasmValueType[] expectedTypes = targetType.getParamTypes();
    if (parameters.length != expectedTypes.length) {
      throw new WasmException(
          String.format(
              "Parameter count mismatch: expected %d, got %d",
              expectedTypes.length, parameters.length));
    }

    for (int i = 0; i < parameters.length; i++) {
      if (!parameters[i].getType().equals(expectedTypes[i])) {
        throw new WasmException(
            String.format(
                "Parameter %d type mismatch: expected %s, got %s",
                i, expectedTypes[i], parameters[i].getType()));
      }
    }
  }

  /**
   * Checks if a function call can be optimized as a tail call.
   *
   * @param currentFunction the current function context
   * @param targetFunction the target function to call
   * @param callSite the call site information
   * @return true if tail call optimization is possible
   */
  public static boolean canOptimizeAsTailCall(
      final FunctionContext currentFunction,
      final FunctionContext targetFunction,
      final CallSite callSite) {

    // Basic tail call eligibility checks
    if (!callSite.isInTailPosition()) {
      return false; // Not in tail position
    }

    if (!currentFunction.supportsTailCalls() || !targetFunction.supportsTailCalls()) {
      return false; // Tail calls not supported
    }

    // Check return type compatibility
    final WasmValueType[] currentReturnTypes = currentFunction.getReturnTypes();
    final WasmValueType[] targetReturnTypes = targetFunction.getReturnTypes();

    if (currentReturnTypes.length != targetReturnTypes.length) {
      return false; // Return type count mismatch
    }

    for (int i = 0; i < currentReturnTypes.length; i++) {
      if (!FunctionTypeValidator.isSubtypeOf(targetReturnTypes[i], currentReturnTypes[i])) {
        return false; // Return type mismatch
      }
    }

    return true;
  }

  /**
   * Estimates the stack space savings from tail call optimization.
   *
   * @param currentFunction the current function
   * @param targetFunction the target function
   * @return estimated stack space savings in bytes
   */
  public static int estimateStackSavings(
      final FunctionContext currentFunction, final FunctionContext targetFunction) {

    final int currentFrameSize = currentFunction.getStackFrameSize();
    final int targetFrameSize = targetFunction.getStackFrameSize();

    // In proper tail call, we reuse the current frame
    return Math.max(0, currentFrameSize - targetFrameSize);
  }

  /** Target information for indirect tail calls. */
  public static class TailCallIndirectTarget {
    private final int tableIndex;
    private final int functionIndex;
    private final FunctionType expectedType;

    /**
     * Creates a new tail call indirect target.
     *
     * @param tableIndex the table index
     * @param functionIndex the function index
     * @param expectedType the expected function type
     */
    public TailCallIndirectTarget(
        final int tableIndex, final int functionIndex, final FunctionType expectedType) {
      this.tableIndex = tableIndex;
      this.functionIndex = functionIndex;
      this.expectedType = expectedType;
    }

    public int getTableIndex() {
      return tableIndex;
    }

    public int getFunctionIndex() {
      return functionIndex;
    }

    public FunctionType getExpectedType() {
      return expectedType;
    }
  }

  /**
   * Finds a tail call instruction by opcode.
   *
   * @param opcode the instruction opcode
   * @return the matching instruction, or null if not found
   */
  public static TailCallInstructions fromOpcode(final int opcode) {
    for (final TailCallInstructions instruction : values()) {
      if (instruction.opcode == opcode) {
        return instruction;
      }
    }
    return null;
  }

  /**
   * Finds a tail call instruction by mnemonic.
   *
   * @param mnemonic the instruction mnemonic
   * @return the matching instruction, or null if not found
   */
  public static TailCallInstructions fromMnemonic(final String mnemonic) {
    for (final TailCallInstructions instruction : values()) {
      if (instruction.mnemonic.equals(mnemonic)) {
        return instruction;
      }
    }
    return null;
  }
}
