package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a host function that can be called from WebAssembly.
 *
 * <p>Host functions allow Java code to provide implementations that can be imported and called by
 * WebAssembly modules. They enable bidirectional communication between Java and WebAssembly
 * runtimes.
 *
 * <p>Host functions must have a well-defined function type signature that matches the WebAssembly
 * import declaration. The runtime validates parameter and return types during module instantiation.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create a simple host function that adds two numbers
 * HostFunction addFunction = new HostFunction() {
 *     @Override
 *     public WasmValue[] execute(WasmValue[] params) throws WasmException {
 *         int a = params[0].asI32();
 *         int b = params[1].asI32();
 *         return new WasmValue[] { WasmValue.i32(a + b) };
 *     }
 * };
 *
 * // Register with import map
 * ImportMap imports = ImportMap.empty();
 * Store store = engine.createStore();
 * WasmFunction func = store.createHostFunction("add", functionType, addFunction);
 * imports.addFunction("env", "add", func);
 * }</pre>
 *
 * @since 1.0.0
 */
@FunctionalInterface
public interface HostFunction {

  /**
   * Executes the host function with the given parameters.
   *
   * <p>This method is called when WebAssembly code invokes the host function. Parameters are
   * automatically converted from WebAssembly values to WasmValue objects. The implementation should
   * validate parameter types and counts as needed.
   *
   * <p>Exceptions thrown by this method are caught by the runtime and converted to WebAssembly
   * traps, causing the calling WebAssembly code to abort execution.
   *
   * @param params the parameters passed from WebAssembly, never null
   * @return the results to return to WebAssembly, must match the function signature
   * @throws WasmException if function execution fails
   */
  WasmValue[] execute(final WasmValue[] params) throws WasmException;
}