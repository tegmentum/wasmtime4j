package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.TypedFunc;
import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of typed WebAssembly function calls.
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
 *   <li>(i64) -> i64
 *   <li>(i64, i64) -> i64
 *   <li>(f32) -> f32
 *   <li>(f64) -> f64
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasmFunction func = instance.getFunction("add").get();
 * JniTypedFunc typedAdd = new JniTypedFunc(store, func, "ii->i");
 * int result = typedAdd.callI32I32ToI32(10, 20); // Returns 30
 * }</pre>
 *
 * @since 1.0.0
 */
public final class JniTypedFunc extends JniResource implements TypedFunc {

  private static final Logger LOGGER = Logger.getLogger(JniTypedFunc.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniTypedFunc: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /** Store context required for function calls. */
  private final JniStore store;

  /** Function signature string (e.g., "ii->i" for (i32, i32) -> i32). */
  private final String signature;

  /** The original function being wrapped. */
  private final WasmFunction function;

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
   * @param store the store context
   * @param func the function to wrap with type information
   * @param signature the signature string encoding parameter and return types
   * @throws JniResourceException if the function signature doesn't match or native call fails
   */
  public JniTypedFunc(final JniStore store, final WasmFunction func, final String signature) {
    super(createHandle(store, func, signature));

    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (signature == null || signature.isEmpty()) {
      throw new IllegalArgumentException("Signature cannot be null or empty");
    }

    this.store = store;
    this.signature = signature;
    this.function = func;

    LOGGER.log(Level.FINE, "Created TypedFunc with signature: {0}", signature);
  }

  /**
   * Creates the native handle for the typed function.
   *
   * @param store the store context
   * @param func the function to wrap
   * @param signature the signature string
   * @return the native handle
   */
  private static long createHandle(
      final JniStore store, final WasmFunction func, final String signature) {
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    if (func == null) {
      throw new IllegalArgumentException("Function cannot be null");
    }
    if (signature == null || signature.isEmpty()) {
      throw new IllegalArgumentException("Signature cannot be null or empty");
    }

    // Get native function pointer - assumes JniFunction implementation
    final long funcPtr;
    if (func instanceof JniFunction) {
      funcPtr = ((JniFunction) func).getNativeHandle();
    } else {
      throw new IllegalArgumentException("Function must be a JniFunction instance");
    }

    // Create native typed function handle
    final long handle = nativeCreate(store.getNativeHandle(), funcPtr, signature);
    if (handle == 0) {
      throw new JniResourceException("Failed to create typed function");
    }

    return handle;
  }

  /**
   * Calls a typed function with no parameters and no return value: () -> ().
   *
   * @throws WasmException if function execution fails
   */
  public void callVoidToVoid() throws WasmException {
    ensureNotClosed();
    nativeCallVoidToVoid(getNativeHandle(), store.getNativeHandle());
  }

  /**
   * Calls a typed function with i32 parameter and void result: (i32) -> ().
   *
   * @param param the i32 parameter
   * @throws WasmException if function execution fails
   */
  public void callI32ToVoid(final int param) throws WasmException {
    ensureNotClosed();
    nativeCallI32ToVoid(getNativeHandle(), store.getNativeHandle(), param);
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
    nativeCallI32I32ToVoid(getNativeHandle(), store.getNativeHandle(), param1, param2);
  }

  /**
   * Calls a typed function with i64 parameter and void result: (i64) -> ().
   *
   * @param param the i64 parameter
   * @throws WasmException if function execution fails
   */
  public void callI64ToVoid(final long param) throws WasmException {
    ensureNotClosed();
    nativeCallI64ToVoid(getNativeHandle(), store.getNativeHandle(), param);
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
    nativeCallI64I64ToVoid(getNativeHandle(), store.getNativeHandle(), param1, param2);
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
    return nativeCallI32ToI32(getNativeHandle(), store.getNativeHandle(), param);
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
    return nativeCallI32I32ToI32(getNativeHandle(), store.getNativeHandle(), param1, param2);
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
    return nativeCallI64ToI64(getNativeHandle(), store.getNativeHandle(), param);
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
    return nativeCallI64I64ToI64(getNativeHandle(), store.getNativeHandle(), param1, param2);
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
    return nativeCallF32ToF32(getNativeHandle(), store.getNativeHandle(), param);
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
    return nativeCallF64ToF64(getNativeHandle(), store.getNativeHandle(), param);
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
    return nativeCallF32F32ToF32(getNativeHandle(), store.getNativeHandle(), param1, param2);
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
    return nativeCallF64F64ToF64(getNativeHandle(), store.getNativeHandle(), param1, param2);
  }

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
  public int callI32I32I32ToI32(final int param1, final int param2, final int param3)
      throws WasmException {
    ensureNotClosed();
    return nativeCallI32I32I32ToI32(
        getNativeHandle(), store.getNativeHandle(), param1, param2, param3);
  }

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
  public long callI64I64I64ToI64(final long param1, final long param2, final long param3)
      throws WasmException {
    ensureNotClosed();
    return nativeCallI64I64I64ToI64(
        getNativeHandle(), store.getNativeHandle(), param1, param2, param3);
  }

  @Override
  public String getSignature() {
    return signature;
  }

  @Override
  public WasmFunction getFunction() {
    return function;
  }

  @Override
  protected String getResourceType() {
    return "TypedFunc[" + signature + "]";
  }

  @Override
  protected void doClose() throws Exception {
    nativeDestroy(getNativeHandle());
    LOGGER.log(Level.FINE, "Closed TypedFunc with signature: {0}", signature);
  }

  // Native method declarations

  private static native long nativeCreate(long storePtr, long funcPtr, String signature);

  private static native void nativeCallVoidToVoid(long handlePtr, long storePtr);

  private static native void nativeCallI32ToVoid(long handlePtr, long storePtr, int param);

  private static native void nativeCallI32I32ToVoid(
      long handlePtr, long storePtr, int param1, int param2);

  private static native void nativeCallI64ToVoid(long handlePtr, long storePtr, long param);

  private static native void nativeCallI64I64ToVoid(
      long handlePtr, long storePtr, long param1, long param2);

  private static native int nativeCallI32ToI32(long handlePtr, long storePtr, int param);

  private static native int nativeCallI32I32ToI32(
      long handlePtr, long storePtr, int param1, int param2);

  private static native long nativeCallI64ToI64(long handlePtr, long storePtr, long param);

  private static native long nativeCallI64I64ToI64(
      long handlePtr, long storePtr, long param1, long param2);

  private static native float nativeCallF32ToF32(long handlePtr, long storePtr, float param);

  private static native double nativeCallF64ToF64(long handlePtr, long storePtr, double param);

  private static native float nativeCallF32F32ToF32(
      long handlePtr, long storePtr, float param1, float param2);

  private static native double nativeCallF64F64ToF64(
      long handlePtr, long storePtr, double param1, double param2);

  private static native int nativeCallI32I32I32ToI32(
      long handlePtr, long storePtr, int param1, int param2, int param3);

  private static native long nativeCallI64I64I64ToI64(
      long handlePtr, long storePtr, long param1, long param2, long param3);

  private static native void nativeDestroy(long handlePtr);
}
