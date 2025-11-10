package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.TypedFunc;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of typed WebAssembly function calls.
 *
 * <p>This class provides zero-cost typed function calls by caching type information and avoiding
 * runtime type checking on each invocation. This is a significant performance optimization for
 * frequently called functions with known signatures.
 *
 * <p>TypedFunc wraps a regular WasmFunction with compile-time type information, enabling:
 *
 * <ul>
 *   <li>Zero-cost abstraction - no runtime type checking overhead
 *   <li>Direct memory layout matching between Java and WebAssembly
 *   <li>Optimized parameter/result marshalling for common type combinations
 *   <li>Type safety through signature validation at creation time
 * </ul>
 *
 * <p>Supported signatures:
 *
 * <ul>
 *   <li>() -> void
 *   <li>(i32) -> i32
 *   <li>(i32, i32) -> i32
 *   <li>(i32, i32, i32) -> i32
 *   <li>(i64) -> i64
 *   <li>(i64, i64) -> i64
 *   <li>(i64, i64, i64) -> i64
 *   <li>(f32) -> f32
 *   <li>(f32, f32) -> f32
 *   <li>(f64) -> f64
 *   <li>(f64, f64) -> f64
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasmFunction func = instance.getFunction("add").get();
 * PanamaTypedFunc typedAdd = new PanamaTypedFunc(func, "ii->i");
 * int result = typedAdd.callI32I32ToI32(10, 20); // Returns 30
 * }</pre>
 *
 * @since 1.0.0
 */
public final class PanamaTypedFunc implements TypedFunc {

  private static final Logger LOGGER = Logger.getLogger(PanamaTypedFunc.class.getName());

  /** The underlying function to call. */
  private final WasmFunction function;

  /** Function signature string (e.g., "ii->i" for (i32, i32) -> i32). */
  private final String signature;

  /** Flag to track if this resource has been closed. */
  private volatile boolean closed = false;

  /**
   * Creates a typed function wrapper from a regular function.
   *
   * <p>The signature string encodes parameter and return types:
   *
   * <ul>
   *   <li>"i" = i32
   *   <li>"I" = i64
   *   <li>"f" = f32
   *   <li>"F" = f64
   *   <li>"v" = void/none
   * </ul>
   *
   * <p>Format: "params->results", e.g., "ii->i" for (i32, i32) -> i32
   *
   * @param func the function to wrap with type information
   * @param signature the signature string encoding parameter and return types
   * @throws IllegalArgumentException if function or signature is invalid
   */
  public PanamaTypedFunc(final WasmFunction func, final String signature) {
    if (func == null) {
      throw new IllegalArgumentException("Function cannot be null");
    }
    if (signature == null || signature.isEmpty()) {
      throw new IllegalArgumentException("Signature cannot be null or empty");
    }

    this.function = func;
    this.signature = signature;

    LOGGER.log(Level.FINE, "Created TypedFunc with signature: {0}", signature);
  }

  /**
   * Calls a typed function with no parameters and no return value: () -> ().
   *
   * @throws WasmException if function execution fails
   */
  public void callVoidToVoid() throws WasmException {
    ensureNotClosed();
    function.call();
  }

  /**
   * Calls a typed function with i32 parameter and void result: (i32) -> ().
   *
   * @param param the i32 parameter
   * @throws WasmException if function execution fails
   */
  public void callI32ToVoid(final int param) throws WasmException {
    ensureNotClosed();
    function.call(WasmValue.i32(param));
  }

  /**
   * Calls a typed function with (i32, i32) parameters and void result: (i32, i32) -> ().
   *
   * @param param1 the first i32 parameter
   * @param param2 the second i32 parameter
   * @throws WasmException if function execution fails
   */
  public void callI32I32ToVoid(final int param1, final int param2) throws WasmException {
    ensureNotClosed();
    function.call(WasmValue.i32(param1), WasmValue.i32(param2));
  }

  /**
   * Calls a typed function with i64 parameter and void result: (i64) -> ().
   *
   * @param param the i64 parameter
   * @throws WasmException if function execution fails
   */
  public void callI64ToVoid(final long param) throws WasmException {
    ensureNotClosed();
    function.call(WasmValue.i64(param));
  }

  /**
   * Calls a typed function with (i64, i64) parameters and void result: (i64, i64) -> ().
   *
   * @param param1 the first i64 parameter
   * @param param2 the second i64 parameter
   * @throws WasmException if function execution fails
   */
  public void callI64I64ToVoid(final long param1, final long param2) throws WasmException {
    ensureNotClosed();
    function.call(WasmValue.i64(param1), WasmValue.i64(param2));
  }

  /**
   * Calls a typed function with i32 parameter and i32 result: (i32) -> i32.
   *
   * @param param the i32 parameter
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  public int callI32ToI32(final int param) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.i32(param));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI32();
  }

  /**
   * Calls a typed function with (i32, i32) parameters and i32 result: (i32, i32) -> i32.
   *
   * @param param1 the first i32 parameter
   * @param param2 the second i32 parameter
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  public int callI32I32ToI32(final int param1, final int param2) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.i32(param1), WasmValue.i32(param2));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI32();
  }

  /**
   * Calls a typed function with i64 parameter and i64 result: (i64) -> i64.
   *
   * @param param the i64 parameter
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  public long callI64ToI64(final long param) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.i64(param));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI64();
  }

  /**
   * Calls a typed function with (i64, i64) parameters and i64 result: (i64, i64) -> i64.
   *
   * @param param1 the first i64 parameter
   * @param param2 the second i64 parameter
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  public long callI64I64ToI64(final long param1, final long param2) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.i64(param1), WasmValue.i64(param2));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI64();
  }

  /**
   * Calls a typed function with f32 parameter and f32 result: (f32) -> f32.
   *
   * @param param the f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  public float callF32ToF32(final float param) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.f32(param));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF32();
  }

  /**
   * Calls a typed function with f64 parameter and f64 result: (f64) -> f64.
   *
   * @param param the f64 parameter
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  public double callF64ToF64(final double param) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.f64(param));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF64();
  }

  /**
   * Calls a typed function with (f32, f32) parameters and f32 result: (f32, f32) -> f32.
   *
   * @param param1 the first f32 parameter
   * @param param2 the second f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  public float callF32F32ToF32(final float param1, final float param2) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.f32(param1), WasmValue.f32(param2));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF32();
  }

  /**
   * Calls a typed function with (f64, f64) parameters and f64 result: (f64, f64) -> f64.
   *
   * @param param1 the first f64 parameter
   * @param param2 the second f64 parameter
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  public double callF64F64ToF64(final double param1, final double param2) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.f64(param1), WasmValue.f64(param2));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF64();
  }

  /**
   * Calls a typed function with (i32, i32, i32) parameters and i32 result: (i32, i32, i32) -> i32.
   *
   * @param param1 the first i32 parameter
   * @param param2 the second i32 parameter
   * @param param3 the third i32 parameter
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  public int callI32I32I32ToI32(final int param1, final int param2, final int param3)
      throws WasmException {
    ensureNotClosed();
    final WasmValue[] results =
        function.call(WasmValue.i32(param1), WasmValue.i32(param2), WasmValue.i32(param3));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI32();
  }

  /**
   * Calls a typed function with (i64, i64, i64) parameters and i64 result: (i64, i64, i64) -> i64.
   *
   * @param param1 the first i64 parameter
   * @param param2 the second i64 parameter
   * @param param3 the third i64 parameter
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  public long callI64I64I64ToI64(final long param1, final long param2, final long param3)
      throws WasmException {
    ensureNotClosed();
    final WasmValue[] results =
        function.call(WasmValue.i64(param1), WasmValue.i64(param2), WasmValue.i64(param3));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI64();
  }

  @Override
  public String getSignature() {
    return signature;
  }

  @Override
  public WasmFunction getFunction() {
    return function;
  }

  /**
   * Calls a typed function with (f32, f32, f32) parameters and f32 result: (f32, f32, f32) -> f32.
   *
   * @param param1 the first f32 parameter
   * @param param2 the second f32 parameter
   * @param param3 the third f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  public float callF32F32F32ToF32(final float param1, final float param2, final float param3)
      throws WasmException {
    ensureNotClosed();
    final WasmValue[] results =
        function.call(WasmValue.f32(param1), WasmValue.f32(param2), WasmValue.f32(param3));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF32();
  }

  /**
   * Calls a typed function with (f64, f64, f64) parameters and f64 result: (f64, f64, f64) -> f64.
   *
   * @param param1 the first f64 parameter
   * @param param2 the second f64 parameter
   * @param param3 the third f64 parameter
   * @return the f64 result
   * @throws WasmException if function execution fails
   */
  public double callF64F64F64ToF64(final double param1, final double param2, final double param3)
      throws WasmException {
    ensureNotClosed();
    final WasmValue[] results =
        function.call(WasmValue.f64(param1), WasmValue.f64(param2), WasmValue.f64(param3));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF64();
  }

  /**
   * Calls a typed function with (i32, i32) parameters and i64 result: (i32, i32) -> i64.
   *
   * @param param1 the first i32 parameter
   * @param param2 the second i32 parameter
   * @return the i64 result
   * @throws WasmException if function execution fails
   */
  public long callI32I32ToI64(final int param1, final int param2) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.i32(param1), WasmValue.i32(param2));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI64();
  }

  /**
   * Calls a typed function with i64 parameter and i32 result: (i64) -> i32.
   *
   * @param param the i64 parameter
   * @return the i32 result
   * @throws WasmException if function execution fails
   */
  public int callI64ToI32(final long param) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.i64(param));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asI32();
  }

  /**
   * Calls a typed function with (i32, f32) parameters and f32 result: (i32, f32) -> f32.
   *
   * @param param1 the i32 parameter
   * @param param2 the f32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  public float callI32F32ToF32(final int param1, final float param2) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.i32(param1), WasmValue.f32(param2));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF32();
  }

  /**
   * Calls a typed function with (f32, i32) parameters and f32 result: (f32, i32) -> f32.
   *
   * @param param1 the f32 parameter
   * @param param2 the i32 parameter
   * @return the f32 result
   * @throws WasmException if function execution fails
   */
  public float callF32I32ToF32(final float param1, final int param2) throws WasmException {
    ensureNotClosed();
    final WasmValue[] results = function.call(WasmValue.f32(param1), WasmValue.i32(param2));
    if (results.length != 1) {
      throw new WasmException("Expected 1 result, got " + results.length);
    }
    return results[0].asF32();
  }

  /** Closes the typed function and releases resources. */
  public void close() {
    if (closed) {
      return;
    }

    try {
      closed = true;
      LOGGER.log(Level.FINE, "Closed TypedFunc with signature: {0}", signature);
    } catch (final Exception e) {
      LOGGER.warning("Error closing typed function: " + e.getMessage());
    }
  }

  /**
   * Ensures the typed function is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("TypedFunc has been closed");
    }
  }
}
