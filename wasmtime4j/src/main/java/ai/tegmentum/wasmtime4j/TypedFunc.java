package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * A type-safe WebAssembly function with statically known parameter and return types.
 *
 * <p>TypedFunc provides zero-cost abstraction for calling WebAssembly functions with known
 * signatures, eliminating runtime type checking overhead on each invocation. This makes it ideal
 * for high-frequency function calls where performance is critical.
 *
 * <p>Unlike {@link WasmFunction} which uses dynamic {@link WasmValue} arrays, TypedFunc provides
 * direct primitive type access for maximum performance.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create typed function from regular function
 * WasmFunction addFunc = instance.getFunction("add").get();
 * TypedFunc typedAdd = TypedFunc.create(addFunc, "ii->i");
 *
 * // Call with zero-cost type safety
 * int result = typedAdd.callI32I32ToI32(10, 20); // Returns 30
 * }</pre>
 *
 * <p>Supported signatures:
 *
 * <ul>
 *   <li>() -> void - {@link #callVoidToVoid()}
 *   <li>(i32) -> void - {@link #callI32ToVoid(int)}
 *   <li>(i32) -> i32 - {@link #callI32ToI32(int)}
 *   <li>(i32, i32) -> void - {@link #callI32I32ToVoid(int, int)}
 *   <li>(i32, i32) -> i32 - {@link #callI32I32ToI32(int, int)}
 *   <li>(i32, i32, i32) -> i32 - {@link #callI32I32I32ToI32(int, int, int)}
 *   <li>(i64) -> void - {@link #callI64ToVoid(long)}
 *   <li>(i64) -> i64 - {@link #callI64ToI64(long)}
 *   <li>(i64, i64) -> void - {@link #callI64I64ToVoid(long, long)}
 *   <li>(i64, i64) -> i64 - {@link #callI64I64ToI64(long, long)}
 *   <li>(i64, i64, i64) -> i64 - {@link #callI64I64I64ToI64(long, long, long)}
 *   <li>(f32) -> f32 - {@link #callF32ToF32(float)}
 *   <li>(f32, f32) -> f32 - {@link #callF32F32ToF32(float, float)}
 *   <li>(f64) -> f64 - {@link #callF64ToF64(double)}
 *   <li>(f64, f64) -> f64 - {@link #callF64F64ToF64(double, double)}
 * </ul>
 *
 * <p>Signature format: "params->results" where:
 *
 * <ul>
 *   <li>"i" = i32 (32-bit integer)
 *   <li>"I" = i64 (64-bit integer)
 *   <li>"f" = f32 (32-bit float)
 *   <li>"F" = f64 (64-bit float)
 *   <li>"v" = void (no return value)
 * </ul>
 *
 * <p>Performance benefits compared to {@link WasmFunction}:
 *
 * <ul>
 *   <li>No runtime type checking on each call
 *   <li>No WasmValue boxing/unboxing overhead
 *   <li>Direct primitive parameter passing
 *   <li>Optimized for JIT compiler inlining
 * </ul>
 *
 * @since 1.0.0
 */
public interface TypedFunc extends AutoCloseable {

  /**
   * Calls a typed function with no parameters and no return value: () -> ().
   *
   * @throws WasmException if function execution fails
   */
  void callVoidToVoid() throws WasmException;

  /**
   * Calls a typed function with i32 parameter and void result: (i32) -> ().
   *
   * @param param the i32 parameter
   * @throws WasmException if function execution fails
   */
  void callI32ToVoid(int param) throws WasmException;

  /**
   * Calls a typed function with (i32, i32) parameters and void result: (i32, i32) -> ().
   *
   * @param param1 the first i32 parameter
   * @param param2 the second i32 parameter
   * @throws WasmException if function execution fails
   */
  void callI32I32ToVoid(int param1, int param2) throws WasmException;

  /**
   * Calls a typed function with i64 parameter and void result: (i64) -> ().
   *
   * @param param the i64 parameter
   * @throws WasmException if function execution fails
   */
  void callI64ToVoid(long param) throws WasmException;

  /**
   * Calls a typed function with (i64, i64) parameters and void result: (i64, i64) -> ().
   *
   * @param param1 the first i64 parameter
   * @param param2 the second i64 parameter
   * @throws WasmException if function execution fails
   */
  void callI64I64ToVoid(long param1, long param2) throws WasmException;

  /**
   * Calls a typed function with i32 parameter and i32 result: (i32) -> i32.
   *
   * @param param the i32 parameter
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  int callI32ToI32(int param) throws WasmException;

  /**
   * Calls a typed function with (i32, i32) parameters and i32 result: (i32, i32) -> i32.
   *
   * @param param1 the first i32 parameter
   * @param param2 the second i32 parameter
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  int callI32I32ToI32(int param1, int param2) throws WasmException;

  /**
   * Calls a typed function with i64 parameter and i64 result: (i64) -> i64.
   *
   * @param param the i64 parameter
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  long callI64ToI64(long param) throws WasmException;

  /**
   * Calls a typed function with (i64, i64) parameters and i64 result: (i64, i64) -> i64.
   *
   * @param param1 the first i64 parameter
   * @param param2 the second i64 parameter
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  long callI64I64ToI64(long param1, long param2) throws WasmException;

  /**
   * Calls a typed function with f32 parameter and f32 result: (f32) -> f32.
   *
   * @param param the f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  float callF32ToF32(float param) throws WasmException;

  /**
   * Calls a typed function with f64 parameter and f64 result: (f64) -> f64.
   *
   * @param param the f64 parameter
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  double callF64ToF64(double param) throws WasmException;

  /**
   * Calls a typed function with (f32, f32) parameters and f32 result: (f32, f32) -> f32.
   *
   * @param param1 the first f32 parameter
   * @param param2 the second f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  float callF32F32ToF32(float param1, float param2) throws WasmException;

  /**
   * Calls a typed function with (f64, f64) parameters and f64 result: (f64, f64) -> f64.
   *
   * @param param1 the first f64 parameter
   * @param param2 the second f64 parameter
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  double callF64F64ToF64(double param1, double param2) throws WasmException;

  /**
   * Calls a typed function with (i32, i32, i32) parameters and i32 result: (i32, i32, i32) ->
   * i32.
   *
   * @param param1 the first i32 parameter
   * @param param2 the second i32 parameter
   * @param param3 the third i32 parameter
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  int callI32I32I32ToI32(int param1, int param2, int param3) throws WasmException;

  /**
   * Calls a typed function with (i64, i64, i64) parameters and i64 result: (i64, i64, i64) ->
   * i64.
   *
   * @param param1 the first i64 parameter
   * @param param2 the second i64 parameter
   * @param param3 the third i64 parameter
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  long callI64I64I64ToI64(long param1, long param2, long param3) throws WasmException;

  /**
   * Gets the signature string for this typed function.
   *
   * <p>The signature describes the parameter and return types in a compact format. For example:
   *
   * <ul>
   *   <li>"v->v" for () -> void
   *   <li>"i->i" for (i32) -> i32
   *   <li>"ii->i" for (i32, i32) -> i32
   *   <li>"I->I" for (i64) -> i64
   *   <li>"f->f" for (f32) -> f32
   * </ul>
   *
   * @return the signature string
   */
  String getSignature();

  /**
   * Gets the underlying WebAssembly function.
   *
   * <p>This allows access to the original function for operations not supported by the typed
   * interface.
   *
   * @return the underlying function
   */
  WasmFunction getFunction();

  /**
   * Closes this typed function and releases associated resources.
   *
   * <p>After calling close(), this TypedFunc instance should not be used. Implementations should
   * ensure this method is idempotent.
   */
  @Override
  void close();

  /**
   * Creates a TypedFunc from a WasmFunction with the specified signature.
   *
   * <p>This is a factory method that creates the appropriate implementation based on the current
   * runtime (JNI or Panama). The actual implementation is delegated to the WasmFunction itself.
   *
   * @param function the function to wrap with type information
   * @param signature the signature string (e.g., "ii->i" for (i32, i32) -> i32)
   * @return a new TypedFunc instance
   * @throws IllegalArgumentException if function or signature is invalid
   * @throws UnsupportedOperationException if the function implementation doesn't support typed
   *     functions
   */
  static TypedFunc create(final WasmFunction function, final String signature) {
    if (function == null) {
      throw new IllegalArgumentException("Function cannot be null");
    }
    if (signature == null || signature.isEmpty()) {
      throw new IllegalArgumentException("Signature cannot be null or empty");
    }

    // Check if the function supports creating typed wrappers
    if (function instanceof TypedFunctionSupport) {
      return ((TypedFunctionSupport) function).asTyped(signature);
    } else {
      throw new UnsupportedOperationException(
          "Function implementation does not support typed functions: "
              + function.getClass().getName());
    }
  }

  /**
   * Internal interface for WasmFunction implementations that support creating TypedFunc wrappers.
   *
   * <p>This is an internal SPI interface - end users should use {@link #create(WasmFunction,
   * String)} instead.
   */
  interface TypedFunctionSupport {
    /**
     * Creates a typed function wrapper with the specified signature.
     *
     * @param signature the signature string
     * @return a new TypedFunc instance
     */
    TypedFunc asTyped(String signature);
  }
}
