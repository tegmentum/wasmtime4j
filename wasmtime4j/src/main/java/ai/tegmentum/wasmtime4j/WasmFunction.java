package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.concurrent.CompletableFuture;

/**
 * Represents a WebAssembly function that can be called from Java code.
 *
 * <p>This interface provides access to both exported functions from WebAssembly modules and host
 * functions that can be imported by WebAssembly modules. Functions maintain their WebAssembly
 * semantics including parameter and return value types.
 *
 * <p>WebAssembly functions are strongly typed with a specific signature defined by their {@link
 * FunctionType}. All parameters and return values must match the expected types at runtime.
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
 * <p>Functions are thread-safe when called from the same store context, but should not be called
 * concurrently from different stores or engines.
 *
 * @since 1.0.0
 */
public interface WasmFunction {

  /**
   * Calls this function with the given parameters.
   *
   * <p>This method executes the WebAssembly function with the provided parameters. The number and
   * types of parameters must match the function's signature as defined by its {@link FunctionType}.
   *
   * <p>The function execution follows WebAssembly semantics including proper stack management,
   * memory isolation, and error handling. Any WebAssembly traps will be converted to {@link
   * WasmException}.
   *
   * @param params the parameters to pass to the function; must match the function signature
   * @return an array of result values; empty array if the function returns no values
   * @throws WasmException if function execution fails, including WebAssembly traps, type
   *     mismatches, or runtime errors
   * @throws IllegalArgumentException if the number or types of parameters don't match the function
   *     signature
   */
  WasmValue[] call(final WasmValue... params) throws WasmException;

  /**
   * Gets the function type signature.
   *
   * <p>The function type describes the parameter types and return types of this function. This
   * information can be used to validate calls and understand the function's interface.
   *
   * @return the function type containing parameter and return type information
   */
  FunctionType getFunctionType();

  /**
   * Gets the name of this function, if available.
   *
   * <p>The function name is typically available for exported functions and may include debugging
   * information. Host functions may also have names depending on how they were defined.
   *
   * @return the function name if available, or null if the function has no name or name information
   *     is not accessible
   */
  String getName();

  /**
   * Calls this function asynchronously with the given parameters.
   *
   * <p>This method is designed to work with async-enabled stores and will properly handle
   * WebAssembly async calls that may yield during execution. The returned CompletableFuture
   * completes when the function execution finishes.
   *
   * <p>For non-async stores, this method typically delegates to a background thread execution of
   * the synchronous {@link #call(WasmValue...)} method.
   *
   * @param params the parameters to pass to the function; must match the function signature
   * @return a CompletableFuture that completes with an array of result values
   * @throws IllegalArgumentException if the number or types of parameters don't match the function
   *     signature
   */
  CompletableFuture<WasmValue[]> callAsync(final WasmValue... params);

  /**
   * Calls this function asynchronously and returns a single result.
   *
   * <p>This is a convenience method for functions that return exactly one value. If the function
   * returns multiple values or no values, the CompletableFuture will complete exceptionally.
   *
   * @param params the parameters to pass to the function; must match the function signature
   * @return a CompletableFuture that completes with the single result value
   * @throws IllegalArgumentException if the number or types of parameters don't match the function
   *     signature
   */
  default CompletableFuture<WasmValue> callSingleAsync(final WasmValue... params) {
    return callAsync(params)
        .thenApply(
            results -> {
              if (results == null || results.length == 0) {
                throw new IllegalStateException("Function returned no values");
              }
              if (results.length > 1) {
                throw new IllegalStateException(
                    "Function returned " + results.length + " values, expected 1");
              }
              return results[0];
            });
  }

  /**
   * Returns a typed function wrapper for this function with the specified signature.
   *
   * <p>TypedFunc provides type-safe function calls with compile-time signature verification,
   * eliminating the overhead of WasmValue boxing/unboxing for high-frequency calls.
   *
   * <p>Example usage:
   *
   * <pre>{@code
   * WasmFunction addFunc = instance.getFunction("add").get();
   * TypedFunc typedAdd = addFunc.typed("ii->i");
   * int result = typedAdd.callI32I32ToI32(10, 20);
   * }</pre>
   *
   * @param signature the signature string (e.g., "ii->i" for (i32, i32) -> i32)
   * @return a TypedFunc wrapper for this function
   * @throws IllegalArgumentException if signature is null, empty, or doesn't match function type
   * @since 1.1.0
   */
  default TypedFunc typed(final String signature) {
    return TypedFunc.create(this, signature);
  }

  /**
   * Checks if this function's signature matches the expected types.
   *
   * <p>This method validates that the function's parameter and result types match the provided
   * types without creating a TypedFunc wrapper.
   *
   * @param paramTypes the expected parameter types
   * @param resultTypes the expected result types
   * @return true if the signature matches
   * @since 1.1.0
   */
  default boolean matchesType(final WasmValueType[] paramTypes, final WasmValueType[] resultTypes) {
    FunctionType funcType = getFunctionType();
    if (funcType == null) {
      return false;
    }

    WasmValueType[] actualParams = funcType.getParamTypes();
    WasmValueType[] actualResults = funcType.getReturnTypes();

    if (actualParams.length != paramTypes.length || actualResults.length != resultTypes.length) {
      return false;
    }

    for (int i = 0; i < paramTypes.length; i++) {
      if (actualParams[i] != paramTypes[i]) {
        return false;
      }
    }

    for (int i = 0; i < resultTypes.length; i++) {
      if (actualResults[i] != resultTypes[i]) {
        return false;
      }
    }

    return true;
  }

  // ===== Low-level/Unsafe Methods =====

  /**
   * Calls this function without type checking using raw values.
   *
   * <p>This is a low-level method that bypasses type validation for maximum performance. The caller
   * is responsible for ensuring that the ValRaw values have the correct types matching the function
   * signature.
   *
   * <p><b>Warning:</b> Incorrect types may cause undefined behavior, memory corruption, or JVM
   * crashes. Only use this method when:
   *
   * <ul>
   *   <li>You have already validated the types externally
   *   <li>You need the absolute maximum performance
   *   <li>You understand the risks of bypassing type safety
   * </ul>
   *
   * @param params the raw parameter values
   * @return an array of raw result values
   * @throws WasmException if function execution fails (not type errors)
   * @since 1.1.0
   */
  default ValRaw[] callUnchecked(final ValRaw... params) throws WasmException {
    // Default implementation converts to WasmValue and uses type-checked call
    WasmValueType[] paramTypes = getFunctionType().getParamTypes();
    WasmValue[] wasmParams = new WasmValue[params.length];
    for (int i = 0; i < params.length; i++) {
      wasmParams[i] = params[i].toWasmValue(paramTypes[i]);
    }
    WasmValue[] results = call(wasmParams);
    ValRaw[] rawResults = new ValRaw[results.length];
    for (int i = 0; i < results.length; i++) {
      rawResults[i] = ValRaw.fromWasmValue(results[i]);
    }
    return rawResults;
  }

  /**
   * Gets the raw function reference for use with unchecked calls.
   *
   * <p>This method returns a handle suitable for direct native calls without going through the Java
   * type system. The returned value is implementation-specific.
   *
   * @return the native function handle
   * @since 1.1.0
   */
  default long getNativeHandle() {
    return 0L;
  }
}
