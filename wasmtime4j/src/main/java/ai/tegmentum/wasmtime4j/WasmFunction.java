package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a WebAssembly function.
 *
 * <p>This interface provides access to both exported functions from WebAssembly modules and host
 * functions that can be imported by WebAssembly modules.
 *
 * @since 1.0.0
 */
public interface WasmFunction {

  /**
   * Calls this function with the given parameters.
   *
   * @param params the parameters to pass to the function
   * @return the results returned by the function
   * @throws WasmException if function execution fails
   */
  WasmValue[] call(final WasmValue... params) throws WasmException;

  /**
   * Gets the function type signature.
   *
   * @return the function type
   */
  FunctionType getFunctionType();

  /**
   * Gets the name of this function, if available.
   *
   * @return the function name, or null if not available
   */
  String getName();
}
