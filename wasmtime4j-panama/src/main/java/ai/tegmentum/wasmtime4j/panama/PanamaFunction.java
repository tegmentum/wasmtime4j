package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.FunctionType;
import ai.tegmentum.wasmtime4j.TypedFunc;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Function.
 *
 * @since 1.0.0
 */
public final class PanamaFunction implements WasmFunction, TypedFunc.TypedFunctionSupport {
  private static final Logger LOGGER = Logger.getLogger(PanamaFunction.class.getName());

  private final PanamaInstance instance;
  private final FunctionType functionType;
  private final String name;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama function that delegates to an instance.
   *
   * @param instance the instance containing this function
   * @param name the function name
   * @param functionType the function type signature
   */
  public PanamaFunction(
      final PanamaInstance instance, final String name, final FunctionType functionType) {
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    if (name == null) {
      throw new IllegalArgumentException("Name cannot be null");
    }
    if (functionType == null) {
      throw new IllegalArgumentException("Function type cannot be null");
    }
    this.instance = instance;
    this.functionType = functionType;
    this.name = name;

    LOGGER.fine("Created Panama function: " + name);
  }

  @Override
  public WasmValue[] call(final WasmValue... params) throws WasmException {
    if (params == null) {
      throw new IllegalArgumentException("Parameters cannot be null");
    }
    ensureNotClosed();
    // Delegate to the instance's callFunction method
    return instance.callFunction(name, params);
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
   * Creates a typed function wrapper with the specified signature.
   *
   * <p>This method provides zero-cost typed function calls by eliminating runtime type checking
   * overhead. The signature string encodes parameter and return types in a compact format.
   *
   * @param signature the signature string (e.g., "ii->i" for (i32, i32) -> i32)
   * @return a new TypedFunc instance wrapping this function
   * @throws IllegalArgumentException if signature is invalid
   */
  @Override
  public TypedFunc asTyped(final String signature) {
    return new PanamaTypedFunc(this, signature);
  }

  /** Closes the function and releases resources. */
  public void close() {
    if (closed) {
      return;
    }

    try {
      closed = true;
      LOGGER.fine("Closed Panama function: " + name);
    } catch (final Exception e) {
      LOGGER.warning("Error closing function: " + e.getMessage());
    }
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
