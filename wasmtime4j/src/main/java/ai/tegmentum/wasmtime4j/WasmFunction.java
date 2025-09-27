package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a WebAssembly function that can be called from Java code.
 *
 * <p>This interface provides access to both exported functions from WebAssembly modules and host
 * functions that can be imported by WebAssembly modules. Functions maintain their WebAssembly
 * semantics including parameter and return value types.
 *
 * <p>WebAssembly functions are strongly typed with a specific signature defined by their
 * {@link FunctionType}. All parameters and return values must match the expected types at
 * runtime.
 *
 * <p>Example usage for calling an exported function:
 *
 * <pre>{@code
 * // Get an exported function from an instance
 * Optional<WasmFunction> addFunction = instance.getFunction("add");
 * if (addFunction.isPresent()) {
 *     // Call the function with two i32 parameters
 *     WasmValue[] results = addFunction.get().call(
 *         WasmValue.i32(10),
 *         WasmValue.i32(20)
 *     );
 *     int sum = results[0].i32(); // Result: 30
 * }
 * }</pre>
 *
 * <p>Functions are thread-safe when called from the same store context, but should not be
 * called concurrently from different stores or engines.
 *
 * @since 1.0.0
 */
public interface WasmFunction {

  /**
   * Calls this function with the given parameters.
   *
   * <p>This method executes the WebAssembly function with the provided parameters. The number
   * and types of parameters must match the function's signature as defined by its
   * {@link FunctionType}.
   *
   * <p>The function execution follows WebAssembly semantics including proper stack management,
   * memory isolation, and error handling. Any WebAssembly traps will be converted to
   * {@link WasmException}.
   *
   * @param params the parameters to pass to the function; must match the function signature
   * @return an array of result values; empty array if the function returns no values
   * @throws WasmException if function execution fails, including WebAssembly traps,
   *         type mismatches, or runtime errors
   * @throws IllegalArgumentException if the number or types of parameters don't match
   *         the function signature
   */
  WasmValue[] call(final WasmValue... params) throws WasmException;

  /**
   * Gets the function type signature.
   *
   * <p>The function type describes the parameter types and return types of this function.
   * This information can be used to validate calls and understand the function's interface.
   *
   * @return the function type containing parameter and return type information
   */
  FunctionType getFunctionType();

  /**
   * Gets the name of this function, if available.
   *
   * <p>The function name is typically available for exported functions and may include
   * debugging information. Host functions may also have names depending on how they
   * were defined.
   *
   * @return the function name if available, or null if the function has no name or
   *         name information is not accessible
   */
  String getName();
}
