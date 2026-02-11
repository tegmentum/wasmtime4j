package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.FunctionType;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a WebAssembly function reference.
 *
 * <p>Function references allow WebAssembly modules to hold references to both exported functions
 * from other modules and host functions provided by Java code. They enable dynamic function
 * dispatch and callback mechanisms in WebAssembly programs.
 *
 * <p>Function references can be:
 *
 * <ul>
 *   <li>Passed as parameters to WebAssembly functions
 *   <li>Returned from WebAssembly functions
 *   <li>Stored in WebAssembly tables
 *   <li>Called indirectly through call_indirect instructions
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a function reference from a host function
 * HostFunction callback = (params) -> {
 *     // Handle callback logic
 *     return new WasmValue[0];
 * };
 * FunctionReference funcRef = store.createFunctionReference(callback, functionType);
 *
 * // Pass function reference to WebAssembly
 * WasmValue refValue = WasmValue.funcref(funcRef);
 * instance.call("set_callback", refValue);
 * }</pre>
 *
 * @since 1.0.0
 */
public interface FunctionReference {

  /**
   * Gets the function type signature of this function reference.
   *
   * @return the function type
   */
  FunctionType getFunctionType();

  /**
   * Calls this function reference with the given parameters.
   *
   * <p>This allows direct invocation of the referenced function from Java code. The function may be
   * either a host function or a WebAssembly function.
   *
   * @param params the parameters to pass to the function
   * @return the results returned by the function
   * @throws WasmException if function execution fails
   */
  WasmValue[] call(final WasmValue... params) throws WasmException;

  /**
   * Gets the name of this function reference, if available.
   *
   * @return the function name, or null if not available
   */
  String getName();

  /**
   * Checks if this function reference is valid and can be called.
   *
   * @return true if the function reference is valid
   */
  boolean isValid();

  /**
   * Gets a unique identifier for this function reference.
   *
   * <p>This ID can be used for debugging, logging, or tracking purposes. The ID is unique within
   * the scope of the creating store.
   *
   * @return the function reference ID
   */
  long getId();
}
