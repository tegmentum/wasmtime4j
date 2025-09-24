package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a WebAssembly function.
 *
 * <p>This interface provides access to both exported functions from WebAssembly modules and host
 * functions that can be imported by WebAssembly modules. Supports the WebAssembly multi-value
 * proposal for functions that return multiple values.
 *
 * @since 1.0.0
 */
public interface WasmFunction {

  /**
   * Calls this function with the given parameters.
   *
   * @param params the parameters to pass to the function
   * @return the results returned by the function (may be multiple values)
   * @throws WasmException if function execution fails
   */
  WasmValue[] call(final WasmValue... params) throws WasmException;

  /**
   * Calls this function with no parameters.
   *
   * @return the results returned by the function (may be multiple values)
   * @throws WasmException if function execution fails
   */
  default WasmValue[] call() throws WasmException {
    return call(new WasmValue[0]);
  }

  /**
   * Calls this function and returns the first result, or null if no results. Convenient method for
   * functions that return a single value.
   *
   * @param params the parameters to pass to the function
   * @return the first result, or null if no results
   * @throws WasmException if function execution fails
   */
  default WasmValue callSingle(final WasmValue... params) throws WasmException {
    final WasmValue[] results = call(params);
    return WasmValue.getFirstValue(results);
  }

  /**
   * Calls this function and validates that it returns the expected number of values.
   *
   * @param expectedResultCount the expected number of return values
   * @param params the parameters to pass to the function
   * @return the results returned by the function
   * @throws WasmException if function execution fails or result count is wrong
   */
  default WasmValue[] callWithResultCount(final int expectedResultCount, final WasmValue... params)
      throws WasmException {
    final WasmValue[] results = call(params);
    if (results.length != expectedResultCount) {
      throw new WasmException(
          String.format("Expected %d return values, got %d", expectedResultCount, results.length));
    }
    return results;
  }

  /**
   * Checks if this function supports multi-value returns.
   *
   * @return true if the function can return multiple values
   */
  default boolean supportsMultiValue() {
    final FunctionType type = getFunctionType();
    return type != null && type.hasMultipleReturns();
  }

  /**
   * Gets the expected number of return values for this function.
   *
   * @return the number of return values, or 0 if unknown
   */
  default int getReturnValueCount() {
    final FunctionType type = getFunctionType();
    return type != null ? type.getReturnCount() : 0;
  }

  /**
   * Gets the expected number of parameters for this function.
   *
   * @return the number of parameters, or 0 if unknown
   */
  default int getParameterCount() {
    final FunctionType type = getFunctionType();
    return type != null ? type.getParamCount() : 0;
  }

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
