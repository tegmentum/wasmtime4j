package ai.tegmentum.wasmtime4j.debug;

import ai.tegmentum.wasmtime4j.Instance;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Optional;

/**
 * A handle to a paused WebAssembly stack frame for debugging.
 *
 * <p>FrameHandle provides access to the state of a WebAssembly frame during a debug event,
 * including locals, stack values, and function identification. Frame handles are obtained from the
 * store's {@code debugExitFrames()} method.
 *
 * <p>Frame handles are only valid while the debug handler is executing. After the handler returns,
 * frame handles become invalid.
 *
 * @since 1.1.0
 */
public final class FrameHandle {

  private final long nativePtr;
  private final int functionIndex;
  private final int pc;
  private final int numLocals;
  private final int numStack;
  private final Instance instance;
  private final Module module;
  private volatile boolean valid;

  /**
   * Creates a new FrameHandle.
   *
   * @param nativePtr the native pointer to the frame handle
   * @param functionIndex the function index in the module
   * @param pc the program counter (instruction offset)
   * @param numLocals the number of local variables
   * @param numStack the number of stack values
   * @param instance the instance this frame belongs to
   * @param module the module this frame belongs to
   */
  public FrameHandle(
      final long nativePtr,
      final int functionIndex,
      final int pc,
      final int numLocals,
      final int numStack,
      final Instance instance,
      final Module module) {
    this.nativePtr = nativePtr;
    this.functionIndex = functionIndex;
    this.pc = pc;
    this.numLocals = numLocals;
    this.numStack = numStack;
    this.instance = instance;
    this.module = module;
    this.valid = true;
  }

  /**
   * Checks if this frame handle is still valid.
   *
   * <p>Frame handles become invalid after the debug handler returns.
   *
   * @return true if this handle is still valid
   */
  public boolean isValid() {
    return valid;
  }

  /**
   * Invalidates this frame handle.
   *
   * <p>Called internally when the debug handler returns.
   */
  public void invalidate() {
    this.valid = false;
  }

  /**
   * Gets the native pointer to this frame handle.
   *
   * @return the native pointer
   */
  public long getNativePtr() {
    return nativePtr;
  }

  /**
   * Gets the WebAssembly function index of this frame.
   *
   * @return the function index
   */
  public int getFunctionIndex() {
    return functionIndex;
  }

  /**
   * Gets the program counter (instruction offset) of this frame.
   *
   * @return the program counter
   */
  public int getPc() {
    return pc;
  }

  /**
   * Gets the number of local variables in this frame.
   *
   * @return the number of locals
   */
  public int getNumLocals() {
    return numLocals;
  }

  /**
   * Gets the number of values on the operand stack in this frame.
   *
   * @return the number of stack values
   */
  public int getNumStack() {
    return numStack;
  }

  /**
   * Gets the instance this frame belongs to.
   *
   * @return the instance, or empty if not available
   */
  public Optional<Instance> getInstance() {
    return Optional.ofNullable(instance);
  }

  /**
   * Gets the module this frame belongs to.
   *
   * @return the module, or empty if not available
   */
  public Optional<Module> getModule() {
    return Optional.ofNullable(module);
  }

  /**
   * Gets the function index and program counter as a pair.
   *
   * @return an array of [functionIndex, pc]
   */
  public int[] getFunctionIndexAndPc() {
    return new int[] {functionIndex, pc};
  }

  /**
   * Gets the parent frame (the caller of this frame).
   *
   * <p>This is only available during a debug handler callback with native support.
   *
   * @return the parent frame handle, or empty if this is the outermost frame
   * @throws IllegalStateException if this handle is no longer valid
   */
  public Optional<FrameHandle> getParent() {
    if (!valid) {
      throw new IllegalStateException("Frame handle is no longer valid");
    }
    // Parent traversal requires native callback - return empty in base implementation
    return Optional.empty();
  }

  /**
   * Gets a local variable value by index.
   *
   * <p>This is only available during a debug handler callback with native support.
   *
   * @param index the local variable index
   * @return the local variable value, or empty if not available
   * @throws IllegalStateException if this handle is no longer valid
   * @throws IndexOutOfBoundsException if index is out of range
   */
  public Optional<WasmValue> getLocal(final int index) {
    if (!valid) {
      throw new IllegalStateException("Frame handle is no longer valid");
    }
    if (index < 0 || index >= numLocals) {
      throw new IndexOutOfBoundsException(
          "Local index " + index + " out of range [0, " + numLocals + ")");
    }
    // Local access requires native callback - return empty in base implementation
    return Optional.empty();
  }

  /**
   * Gets a stack value by index.
   *
   * <p>This is only available during a debug handler callback with native support.
   *
   * @param index the stack index (0 = top of stack)
   * @return the stack value, or empty if not available
   * @throws IllegalStateException if this handle is no longer valid
   * @throws IndexOutOfBoundsException if index is out of range
   */
  public Optional<WasmValue> getStack(final int index) {
    if (!valid) {
      throw new IllegalStateException("Frame handle is no longer valid");
    }
    if (index < 0 || index >= numStack) {
      throw new IndexOutOfBoundsException(
          "Stack index " + index + " out of range [0, " + numStack + ")");
    }
    // Stack access requires native callback - return empty in base implementation
    return Optional.empty();
  }

  @Override
  public String toString() {
    return "FrameHandle{funcIndex="
        + functionIndex
        + ", pc="
        + pc
        + ", locals="
        + numLocals
        + ", stack="
        + numStack
        + ", valid="
        + valid
        + "}";
  }
}
