package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Function.
 *
 * @since 1.0.0
 */
public final class PanamaFunction implements WasmFunction {
  private static final Logger LOGGER = Logger.getLogger(PanamaFunction.class.getName());

  private final Arena arena;
  private final MemorySegment nativeFunction;
  private final FunctionType functionType;
  private final String name;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama function.
   *
   * @param functionType the function type signature
   * @param name the function name (may be null)
   */
  public PanamaFunction(final FunctionType functionType, final String name) {
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    this.functionType = functionType;
    this.name = name;
    this.arena = Arena.ofShared();

    // TODO: Create native function via Panama FFI
    this.nativeFunction = MemorySegment.NULL;

    LOGGER.fine("Created Panama function");
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    if (params == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }
    ensureNotClosed();
    // TODO: Implement function call
    throw new UnsupportedOperationException("Function call not yet implemented");
  }

  @Override
  public FunctionType getFunctionType() {
    return functionType;
  }

  @Override
  public String getName() {
    return name;
  }

  /**
   * Closes the function and releases resources.
   */
  public void close() {
    if (closed) {
      return;
    }

    try {
      // TODO: Destroy native function
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama function");
    } catch (final Exception e) {
      LOGGER.warning("Error closing function: " + e.getMessage());
    }
  }

  /**
   * Gets the native function pointer.
   *
   * @return native function segment
   */
  public MemorySegment getNativeFunction() {
    return nativeFunction;
  }

  /**
   * Ensures the function is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Function has been closed");
    }
  }
}
