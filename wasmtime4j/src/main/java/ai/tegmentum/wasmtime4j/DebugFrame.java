package ai.tegmentum.wasmtime4j;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Represents a debug stack frame from WebAssembly execution.
 *
 * <p>DebugFrame provides detailed information about a single stack frame during WebAssembly
 * execution, including function information, local variables, and operand stack state.
 * This is useful for debugging and profiling WebAssembly code.
 *
 * <p>Frames are obtained from {@link Store#debugFrames()} when guest debugging is enabled.
 *
 * @since 1.0.0
 */
public final class DebugFrame {

  private final int functionIndex;
  private final String functionName;
  private final String moduleName;
  private final long instructionOffset;
  private final List<WasmValue> locals;
  private final List<WasmValue> operandStack;
  private final Map<String, Object> attributes;

  /**
   * Creates a new debug frame.
   *
   * @param functionIndex the index of the function in the module
   * @param functionName the name of the function (may be null)
   * @param moduleName the name of the module (may be null)
   * @param instructionOffset the byte offset of the current instruction
   * @param locals the local variable values
   * @param operandStack the operand stack values
   * @param attributes additional frame attributes
   */
  public DebugFrame(
      final int functionIndex,
      final String functionName,
      final String moduleName,
      final long instructionOffset,
      final List<WasmValue> locals,
      final List<WasmValue> operandStack,
      final Map<String, Object> attributes) {
    this.functionIndex = functionIndex;
    this.functionName = functionName;
    this.moduleName = moduleName;
    this.instructionOffset = instructionOffset;
    this.locals = locals != null
        ? Collections.unmodifiableList(locals) : Collections.emptyList();
    this.operandStack = operandStack != null
        ? Collections.unmodifiableList(operandStack) : Collections.emptyList();
    this.attributes = attributes != null
        ? Collections.unmodifiableMap(attributes) : Collections.emptyMap();
  }

  /**
   * Creates a debug frame with minimal information.
   *
   * @param functionIndex the index of the function in the module
   * @param instructionOffset the byte offset of the current instruction
   */
  public DebugFrame(final int functionIndex, final long instructionOffset) {
    this(functionIndex, null, null, instructionOffset, null, null, null);
  }

  /**
   * Gets the function index within the module.
   *
   * @return the function index
   */
  public int getFunctionIndex() {
    return functionIndex;
  }

  /**
   * Gets the function name if available.
   *
   * <p>The function name is derived from the WebAssembly name section if present,
   * or may be null if the module was compiled without debug information.
   *
   * @return the function name, or null if not available
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the module name if available.
   *
   * @return the module name, or null if not available
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Gets the byte offset of the current instruction within the function.
   *
   * @return the instruction offset
   */
  public long getInstructionOffset() {
    return instructionOffset;
  }

  /**
   * Gets the current values of local variables.
   *
   * <p>The list contains values for all local variables in the function,
   * including function parameters. The order matches the WebAssembly local
   * variable indices.
   *
   * @return an unmodifiable list of local variable values
   */
  public List<WasmValue> getLocals() {
    return locals;
  }

  /**
   * Gets a specific local variable value.
   *
   * @param index the local variable index
   * @return the local variable value
   * @throws IndexOutOfBoundsException if the index is out of range
   */
  public WasmValue getLocal(final int index) {
    return locals.get(index);
  }

  /**
   * Gets the number of local variables.
   *
   * @return the number of locals
   */
  public int getLocalCount() {
    return locals.size();
  }

  /**
   * Gets the current operand stack values.
   *
   * <p>The list contains values currently on the operand stack, with the
   * top of stack at the end of the list.
   *
   * @return an unmodifiable list of operand stack values
   */
  public List<WasmValue> getOperandStack() {
    return operandStack;
  }

  /**
   * Gets the current operand stack depth.
   *
   * @return the number of values on the operand stack
   */
  public int getStackDepth() {
    return operandStack.size();
  }

  /**
   * Gets additional frame attributes.
   *
   * <p>Attributes may include debug metadata, source locations, and other
   * implementation-specific information.
   *
   * @return an unmodifiable map of frame attributes
   */
  public Map<String, Object> getAttributes() {
    return attributes;
  }

  /**
   * Gets a specific frame attribute.
   *
   * @param key the attribute key
   * @return the attribute value, or null if not present
   */
  public Object getAttribute(final String key) {
    return attributes.get(key);
  }

  /**
   * Checks if the frame has a specific attribute.
   *
   * @param key the attribute key
   * @return true if the attribute exists
   */
  public boolean hasAttribute(final String key) {
    return attributes.containsKey(key);
  }

  /**
   * Returns a formatted string representation of the frame.
   *
   * <p>The format is suitable for stack trace display:
   * {@code moduleName!functionName+offset} or {@code func[index]+offset}
   *
   * @return formatted frame string
   */
  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();

    if (moduleName != null) {
      sb.append(moduleName).append("!");
    }

    if (functionName != null) {
      sb.append(functionName);
    } else {
      sb.append("func[").append(functionIndex).append("]");
    }

    sb.append("+0x").append(Long.toHexString(instructionOffset));

    return sb.toString();
  }

  /**
   * Builder for creating DebugFrame instances.
   *
   * @since 1.0.0
   */
  public static final class Builder {
    private int functionIndex;
    private String functionName;
    private String moduleName;
    private long instructionOffset;
    private List<WasmValue> locals;
    private List<WasmValue> operandStack;
    private Map<String, Object> attributes;

    /**
     * Creates a new builder.
     */
    public Builder() {
      // Default constructor
    }

    /**
     * Sets the function index.
     *
     * @param functionIndex the function index
     * @return this builder
     */
    public Builder functionIndex(final int functionIndex) {
      this.functionIndex = functionIndex;
      return this;
    }

    /**
     * Sets the function name.
     *
     * @param functionName the function name
     * @return this builder
     */
    public Builder functionName(final String functionName) {
      this.functionName = functionName;
      return this;
    }

    /**
     * Sets the module name.
     *
     * @param moduleName the module name
     * @return this builder
     */
    public Builder moduleName(final String moduleName) {
      this.moduleName = moduleName;
      return this;
    }

    /**
     * Sets the instruction offset.
     *
     * @param instructionOffset the instruction offset
     * @return this builder
     */
    public Builder instructionOffset(final long instructionOffset) {
      this.instructionOffset = instructionOffset;
      return this;
    }

    /**
     * Sets the local variables.
     *
     * @param locals the local variables
     * @return this builder
     */
    public Builder locals(final List<WasmValue> locals) {
      this.locals = locals;
      return this;
    }

    /**
     * Sets the operand stack.
     *
     * @param operandStack the operand stack
     * @return this builder
     */
    public Builder operandStack(final List<WasmValue> operandStack) {
      this.operandStack = operandStack;
      return this;
    }

    /**
     * Sets the attributes.
     *
     * @param attributes the attributes
     * @return this builder
     */
    public Builder attributes(final Map<String, Object> attributes) {
      this.attributes = attributes;
      return this;
    }

    /**
     * Builds the debug frame.
     *
     * @return the debug frame
     */
    public DebugFrame build() {
      return new DebugFrame(
          functionIndex, functionName, moduleName, instructionOffset,
          locals, operandStack, attributes);
    }
  }

  /**
   * Creates a new builder for DebugFrame.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }
}
