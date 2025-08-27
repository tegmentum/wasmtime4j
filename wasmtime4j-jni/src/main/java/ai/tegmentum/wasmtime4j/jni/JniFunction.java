package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the Function interface.
 *
 * <p>This class provides access to WebAssembly functions through JNI calls to the native Wasmtime
 * library. It supports calling functions with various parameter and return types.
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive type checking for function calls using JniValidation and the JniResource base class.
 */
public final class JniFunction extends JniResource {

  private static final Logger LOGGER = Logger.getLogger(JniFunction.class.getName());

  /** Function name for debugging. */
  private final String name;

  /**
   * Creates a new JNI function with the given native handle and name.
   *
   * @param nativeHandle the native function handle
   * @param name the function name
   * @throws JniResourceException if nativeHandle is invalid or name is null
   */
  JniFunction(final long nativeHandle, final String name) {
    super(nativeHandle);
    JniValidation.requireNonNull(name, "name");
    this.name = name;
    LOGGER.fine("Created JNI function '" + name + "' with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Gets the name of this function.
   *
   * @return the function name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the parameter types for this function.
   *
   * @return array of parameter type names
   * @throws JniResourceException if this function is closed
   * @throws RuntimeException if the types cannot be retrieved
   */
  public String[] getParameterTypes() {
    ensureNotClosed();
    try {
      final String[] types = nativeGetParameterTypes(getNativeHandle());
      return types != null ? types : new String[0];
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting parameter types", e);
    }
  }

  /**
   * Gets the return types for this function.
   *
   * @return array of return type names
   * @throws JniResourceException if this function is closed
   * @throws RuntimeException if the types cannot be retrieved
   */
  public String[] getReturnTypes() {
    ensureNotClosed();
    try {
      final String[] types = nativeGetReturnTypes(getNativeHandle());
      return types != null ? types : new String[0];
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting return types", e);
    }
  }

  /**
   * Calls this function with no parameters.
   *
   * @return the return value (null if function returns void)
   * @throws JniResourceException if this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public Object call() {
    return call(new Object[0]);
  }

  /**
   * Calls this function with the given parameters.
   *
   * @param parameters the function parameters
   * @return the return value (null if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public Object call(final Object... parameters) {
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

    try {
      return nativeCall(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with integer parameters (optimized path).
   *
   * @param parameters the integer parameters
   * @return the return value as an integer (0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public int callInt(final int... parameters) {
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

    try {
      return nativeCallInt(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with long parameters (optimized path).
   *
   * @param parameters the long parameters
   * @return the return value as a long (0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public long callLong(final long... parameters) {
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

    try {
      return nativeCallLong(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with float parameters (optimized path).
   *
   * @param parameters the float parameters
   * @return the return value as a float (0.0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public float callFloat(final float... parameters) {
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

    try {
      return nativeCallFloat(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Calls this function with double parameters (optimized path).
   *
   * @param parameters the double parameters
   * @return the return value as a double (0.0 if function returns void)
   * @throws JniResourceException if parameters is null or this function is closed
   * @throws RuntimeException if the call fails or types don't match
   */
  public double callDouble(final double... parameters) {
    JniValidation.requireNonNull(parameters, "parameters");
    ensureNotClosed();

    try {
      return nativeCallDouble(getNativeHandle(), parameters);
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error calling function '" + name + "'", e);
    }
  }

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name
   */
  @Override
  protected String getResourceType() {
    return "Function[" + name + "]";
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    nativeDestroyFunction(nativeHandle);
  }

  // Native method declarations

  /**
   * Gets the parameter types for a function.
   *
   * @param functionHandle the native function handle
   * @return array of parameter type names or null on error
   */
  private static native String[] nativeGetParameterTypes(long functionHandle);

  /**
   * Gets the return types for a function.
   *
   * @param functionHandle the native function handle
   * @return array of return type names or null on error
   */
  private static native String[] nativeGetReturnTypes(long functionHandle);

  /**
   * Calls a function with generic parameters.
   *
   * @param functionHandle the native function handle
   * @param parameters the function parameters
   * @return the return value or null
   */
  private static native Object nativeCall(long functionHandle, Object[] parameters);

  /**
   * Calls a function with integer parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the integer parameters
   * @return the return value as an integer
   */
  private static native int nativeCallInt(long functionHandle, int[] parameters);

  /**
   * Calls a function with long parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the long parameters
   * @return the return value as a long
   */
  private static native long nativeCallLong(long functionHandle, long[] parameters);

  /**
   * Calls a function with float parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the float parameters
   * @return the return value as a float
   */
  private static native float nativeCallFloat(long functionHandle, float[] parameters);

  /**
   * Calls a function with double parameters (optimized).
   *
   * @param functionHandle the native function handle
   * @param parameters the double parameters
   * @return the return value as a double
   */
  private static native double nativeCallDouble(long functionHandle, double[] parameters);

  /**
   * Destroys a native function.
   *
   * @param functionHandle the native function handle
   */
  private static native void nativeDestroyFunction(long functionHandle);
}
