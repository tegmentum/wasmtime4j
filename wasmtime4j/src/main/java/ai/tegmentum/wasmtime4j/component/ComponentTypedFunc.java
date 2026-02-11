/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * A type-safe WebAssembly Component Model function with statically known parameter and return
 * types.
 *
 * <p>ComponentTypedFunc provides zero-cost abstraction for calling component functions with known
 * signatures, eliminating runtime type checking overhead on each invocation. This makes it ideal
 * for high-frequency function calls where performance is critical.
 *
 * <p>Unlike {@link ComponentFunc} which uses dynamic {@link ComponentVal} arrays,
 * ComponentTypedFunc provides direct primitive type access for maximum performance when working
 * with primitive types.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * // Create typed function from regular component function
 * ComponentFunc addFunc = instance.getFunc("add");
 * ComponentTypedFunc typedAdd = ComponentTypedFunc.create(addFunc, "s32,s32->s32");
 *
 * // Call with zero-cost type safety
 * int result = typedAdd.callS32S32ToS32(10, 20); // Returns 30
 * }</pre>
 *
 * <p>Supported signatures:
 *
 * <ul>
 *   <li>() -> void - {@link #callVoidToVoid()}
 *   <li>(s32) -> void - {@link #callS32ToVoid(int)}
 *   <li>(s32) -> s32 - {@link #callS32ToS32(int)}
 *   <li>(s32, s32) -> void - {@link #callS32S32ToVoid(int, int)}
 *   <li>(s32, s32) -> s32 - {@link #callS32S32ToS32(int, int)}
 *   <li>(s32, s32) -> s64 - {@link #callS32S32ToS64(int, int)}
 *   <li>(s32, s32, s32) -> s32 - {@link #callS32S32S32ToS32(int, int, int)}
 *   <li>(s64) -> void - {@link #callS64ToVoid(long)}
 *   <li>(s64) -> s32 - {@link #callS64ToS32(long)}
 *   <li>(s64) -> s64 - {@link #callS64ToS64(long)}
 *   <li>(s64, s64) -> void - {@link #callS64S64ToVoid(long, long)}
 *   <li>(s64, s64) -> s64 - {@link #callS64S64ToS64(long, long)}
 *   <li>(s64, s64, s64) -> s64 - {@link #callS64S64S64ToS64(long, long, long)}
 *   <li>(f32) -> f32 - {@link #callF32ToF32(float)}
 *   <li>(f32, f32) -> f32 - {@link #callF32F32ToF32(float, float)}
 *   <li>(f32, f32, f32) -> f32 - {@link #callF32F32F32ToF32(float, float, float)}
 *   <li>(f64) -> f64 - {@link #callF64ToF64(double)}
 *   <li>(f64, f64) -> f64 - {@link #callF64F64ToF64(double, double)}
 *   <li>(f64, f64, f64) -> f64 - {@link #callF64F64F64ToF64(double, double, double)}
 *   <li>(string) -> void - {@link #callStringToVoid(String)}
 *   <li>(string) -> string - {@link #callStringToString(String)}
 *   <li>(string, string) -> string - {@link #callStringStringToString(String, String)}
 *   <li>() -> string - {@link #callVoidToString()}
 *   <li>() -> bool - {@link #callVoidToBool()}
 *   <li>(bool) -> bool - {@link #callBoolToBool(boolean)}
 * </ul>
 *
 * <p>Signature format: "params->results" where:
 *
 * <ul>
 *   <li>"s8", "s16", "s32", "s64" = signed integers
 *   <li>"u8", "u16", "u32", "u64" = unsigned integers
 *   <li>"f32", "f64" = floating point
 *   <li>"bool" = boolean
 *   <li>"string" = string
 *   <li>"void" or empty = no value
 * </ul>
 *
 * <p>Performance benefits compared to {@link ComponentFunc}:
 *
 * <ul>
 *   <li>No runtime type checking on each call
 *   <li>No ComponentVal boxing/unboxing overhead
 *   <li>Direct primitive parameter passing
 *   <li>Optimized for JIT compiler inlining
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentTypedFunc extends AutoCloseable {

  // ========== Void return signatures ==========

  /**
   * Calls a typed function with no parameters and no return value: () -> ().
   *
   * @throws WasmException if function execution fails
   */
  void callVoidToVoid() throws WasmException;

  /**
   * Calls a typed function with s32 parameter and void result: (s32) -> ().
   *
   * @param param the s32 parameter
   * @throws WasmException if function execution fails
   */
  void callS32ToVoid(int param) throws WasmException;

  /**
   * Calls a typed function with (s32, s32) parameters and void result: (s32, s32) -> ().
   *
   * @param param1 the first s32 parameter
   * @param param2 the second s32 parameter
   * @throws WasmException if function execution fails
   */
  void callS32S32ToVoid(int param1, int param2) throws WasmException;

  /**
   * Calls a typed function with s64 parameter and void result: (s64) -> ().
   *
   * @param param the s64 parameter
   * @throws WasmException if function execution fails
   */
  void callS64ToVoid(long param) throws WasmException;

  /**
   * Calls a typed function with (s64, s64) parameters and void result: (s64, s64) -> ().
   *
   * @param param1 the first s64 parameter
   * @param param2 the second s64 parameter
   * @throws WasmException if function execution fails
   */
  void callS64S64ToVoid(long param1, long param2) throws WasmException;

  /**
   * Calls a typed function with string parameter and void result: (string) -> ().
   *
   * @param param the string parameter
   * @throws WasmException if function execution fails
   */
  void callStringToVoid(String param) throws WasmException;

  // ========== s32 return signatures ==========

  /**
   * Calls a typed function with s32 parameter and s32 result: (s32) -> s32.
   *
   * @param param the s32 parameter
   * @return the s32 result
   * @throws WasmException if function execution fails
   */
  int callS32ToS32(int param) throws WasmException;

  /**
   * Calls a typed function with (s32, s32) parameters and s32 result: (s32, s32) -> s32.
   *
   * @param param1 the first s32 parameter
   * @param param2 the second s32 parameter
   * @return the s32 result
   * @throws WasmException if function execution fails
   */
  int callS32S32ToS32(int param1, int param2) throws WasmException;

  /**
   * Calls a typed function with (s32, s32, s32) parameters and s32 result: (s32, s32, s32) -> s32.
   *
   * @param param1 the first s32 parameter
   * @param param2 the second s32 parameter
   * @param param3 the third s32 parameter
   * @return the s32 result
   * @throws WasmException if function execution fails
   */
  int callS32S32S32ToS32(int param1, int param2, int param3) throws WasmException;

  /**
   * Calls a typed function with s64 parameter and s32 result: (s64) -> s32.
   *
   * @param param the s64 parameter
   * @return the s32 result
   * @throws WasmException if function execution fails
   */
  int callS64ToS32(long param) throws WasmException;

  // ========== s64 return signatures ==========

  /**
   * Calls a typed function with s64 parameter and s64 result: (s64) -> s64.
   *
   * @param param the s64 parameter
   * @return the s64 result
   * @throws WasmException if function execution fails
   */
  long callS64ToS64(long param) throws WasmException;

  /**
   * Calls a typed function with (s64, s64) parameters and s64 result: (s64, s64) -> s64.
   *
   * @param param1 the first s64 parameter
   * @param param2 the second s64 parameter
   * @return the s64 result
   * @throws WasmException if function execution fails
   */
  long callS64S64ToS64(long param1, long param2) throws WasmException;

  /**
   * Calls a typed function with (s64, s64, s64) parameters and s64 result: (s64, s64, s64) -> s64.
   *
   * @param param1 the first s64 parameter
   * @param param2 the second s64 parameter
   * @param param3 the third s64 parameter
   * @return the s64 result
   * @throws WasmException if function execution fails
   */
  long callS64S64S64ToS64(long param1, long param2, long param3) throws WasmException;

  /**
   * Calls a typed function with (s32, s32) parameters and s64 result: (s32, s32) -> s64.
   *
   * @param param1 the first s32 parameter
   * @param param2 the second s32 parameter
   * @return the s64 result
   * @throws WasmException if function execution fails
   */
  long callS32S32ToS64(int param1, int param2) throws WasmException;

  // ========== f32 return signatures ==========

  /**
   * Calls a typed function with f32 parameter and f32 result: (f32) -> f32.
   *
   * @param param the f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  float callF32ToF32(float param) throws WasmException;

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
   * Calls a typed function with (f32, f32, f32) parameters and f32 result: (f32, f32, f32) -> f32.
   *
   * @param param1 the first f32 parameter
   * @param param2 the second f32 parameter
   * @param param3 the third f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  float callF32F32F32ToF32(float param1, float param2, float param3) throws WasmException;

  // ========== f64 return signatures ==========

  /**
   * Calls a typed function with f64 parameter and f64 result: (f64) -> f64.
   *
   * @param param the f64 parameter
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  double callF64ToF64(double param) throws WasmException;

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
   * Calls a typed function with (f64, f64, f64) parameters and f64 result: (f64, f64, f64) -> f64.
   *
   * @param param1 the first f64 parameter
   * @param param2 the second f64 parameter
   * @param param3 the third f64 parameter
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  double callF64F64F64ToF64(double param1, double param2, double param3) throws WasmException;

  // ========== string return signatures ==========

  /**
   * Calls a typed function with no parameters and string result: () -> string.
   *
   * @return the string result
   * @throws WasmException if function execution fails
   */
  String callVoidToString() throws WasmException;

  /**
   * Calls a typed function with string parameter and string result: (string) -> string.
   *
   * @param param the string parameter
   * @return the string result
   * @throws WasmException if function execution fails
   */
  String callStringToString(String param) throws WasmException;

  /**
   * Calls a typed function with (string, string) parameters and string result: (string, string) ->
   * string.
   *
   * @param param1 the first string parameter
   * @param param2 the second string parameter
   * @return the string result
   * @throws WasmException if function execution fails
   */
  String callStringStringToString(String param1, String param2) throws WasmException;

  // ========== bool signatures ==========

  /**
   * Calls a typed function with no parameters and bool result: () -> bool.
   *
   * @return the bool result
   * @throws WasmException if function execution fails
   */
  boolean callVoidToBool() throws WasmException;

  /**
   * Calls a typed function with bool parameter and bool result: (bool) -> bool.
   *
   * @param param the bool parameter
   * @return the bool result
   * @throws WasmException if function execution fails
   */
  boolean callBoolToBool(boolean param) throws WasmException;

  // ========== Metadata and lifecycle ==========

  /**
   * Gets the signature string for this typed function.
   *
   * <p>The signature describes the parameter and return types in a compact format. For example:
   *
   * <ul>
   *   <li>"void->void" for () -> void
   *   <li>"s32->s32" for (s32) -> s32
   *   <li>"s32,s32->s32" for (s32, s32) -> s32
   *   <li>"string->string" for (string) -> string
   * </ul>
   *
   * @return the signature string
   */
  String getSignature();

  /**
   * Gets the underlying component function.
   *
   * <p>This allows access to the original function for operations not supported by the typed
   * interface.
   *
   * @return the underlying function
   */
  ComponentFunc getFunction();

  /**
   * Closes this typed function and releases associated resources.
   *
   * <p>After calling close(), this ComponentTypedFunc instance should not be used. Implementations
   * should ensure this method is idempotent.
   */
  @Override
  void close();

  /**
   * Creates a ComponentTypedFunc from a ComponentFunc with the specified signature.
   *
   * <p>This is a factory method that creates the appropriate implementation based on the current
   * runtime (JNI or Panama). The actual implementation is delegated to the ComponentFunc itself.
   *
   * @param function the function to wrap with type information
   * @param signature the signature string (e.g., "s32,s32->s32" for (s32, s32) -> s32)
   * @return a new ComponentTypedFunc instance
   * @throws IllegalArgumentException if function or signature is invalid
   * @throws UnsupportedOperationException if the function implementation doesn't support typed
   *     functions
   */
  static ComponentTypedFunc create(final ComponentFunc function, final String signature) {
    if (function == null) {
      throw new IllegalArgumentException("Function cannot be null");
    }
    if (signature == null || signature.isEmpty()) {
      throw new IllegalArgumentException("Signature cannot be null or empty");
    }

    // Check if the function supports creating typed wrappers
    if (function instanceof TypedComponentFunctionSupport) {
      return ((TypedComponentFunctionSupport) function).asTyped(signature);
    } else {
      throw new UnsupportedOperationException(
          "Function implementation does not support typed functions: "
              + function.getClass().getName());
    }
  }

  /**
   * Internal interface for ComponentFunc implementations that support creating ComponentTypedFunc
   * wrappers.
   *
   * <p>This is an internal SPI interface - end users should use {@link #create(ComponentFunc,
   * String)} instead.
   */
  interface TypedComponentFunctionSupport {
    /**
     * Creates a typed component function wrapper with the specified signature.
     *
     * @param signature the signature string
     * @return a new ComponentTypedFunc instance
     */
    ComponentTypedFunc asTyped(String signature);
  }
}
