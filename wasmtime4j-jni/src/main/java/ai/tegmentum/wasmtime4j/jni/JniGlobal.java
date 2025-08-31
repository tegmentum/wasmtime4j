package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.logging.Logger;

/**
 * JNI implementation of the Global interface.
 *
 * <p>This class provides access to WebAssembly global variables through JNI calls to the native
 * Wasmtime library. Globals can store various value types and may be mutable or immutable.
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive type checking for global variable access using JniValidation and the JniResource
 * base class.
 */
public final class JniGlobal extends JniResource implements WasmGlobal {

  private static final Logger LOGGER = Logger.getLogger(JniGlobal.class.getName());

  // Load native library when this class is first loaded
  static {
    try {
      ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader.loadLibrary();
    } catch (final RuntimeException e) {
      LOGGER.severe("Failed to load native library for JniGlobal: " + e.getMessage());
      throw new ExceptionInInitializerError(e);
    }
  }

  /**
   * Creates a new JNI global with the given native handle.
   *
   * @param nativeHandle the native global handle
   * @throws JniResourceException if nativeHandle is invalid
   */
  JniGlobal(final long nativeHandle) {
    super(nativeHandle);
    LOGGER.fine("Created JNI global with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Gets the value type of this global.
   *
   * @return the value type name (e.g., "i32", "i64", "f32", "f64")
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the type cannot be retrieved
   */
  public String getValueType() {
    ensureNotClosed();
    try {
      final String type = nativeGetValueType(getNativeHandle());
      return type != null ? type : "unknown";
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global value type", e);
    }
  }

  /**
   * Checks if this global is mutable.
   *
   * @return true if the global is mutable, false if immutable
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the mutability cannot be determined
   */
  public boolean isMutable() {
    ensureNotClosed();
    try {
      return nativeIsMutable(getNativeHandle());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error checking global mutability", e);
    }
  }

  /**
   * Gets the current value of this global as a generic Object.
   *
   * @return the global value (Integer, Long, Float, or Double)
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the value cannot be retrieved
   */
  public Object getValue() {
    ensureNotClosed();
    try {
      return nativeGetValue(getNativeHandle());
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global value", e);
    }
  }

  /**
   * Gets the current value of this global as an integer.
   *
   * @return the global value as an integer
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the value cannot be retrieved or is not an integer
   */
  public int getIntValue() {
    ensureNotClosed();
    try {
      return nativeGetIntValue(getNativeHandle());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global int value", e);
    }
  }

  /**
   * Gets the current value of this global as a long.
   *
   * @return the global value as a long
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the value cannot be retrieved or is not a long
   */
  public long getLongValue() {
    ensureNotClosed();
    try {
      return nativeGetLongValue(getNativeHandle());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global long value", e);
    }
  }

  /**
   * Gets the current value of this global as a float.
   *
   * @return the global value as a float
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the value cannot be retrieved or is not a float
   */
  public float getFloatValue() {
    ensureNotClosed();
    try {
      return nativeGetFloatValue(getNativeHandle());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global float value", e);
    }
  }

  /**
   * Gets the current value of this global as a double.
   *
   * @return the global value as a double
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the value cannot be retrieved or is not a double
   */
  public double getDoubleValue() {
    ensureNotClosed();
    try {
      return nativeGetDoubleValue(getNativeHandle());
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global double value", e);
    }
  }

  /**
   * Sets the value of this global (only if mutable).
   *
   * @param value the new value (must match the global's type)
   * @throws JniResourceException if value is null, wrong type, this global is closed, or immutable
   * @throws RuntimeException if the value cannot be set
   */
  public void setValue(final Object value) {
    JniValidation.requireNonNull(value, "value");
    ensureNotClosed();
    validateMutable();

    try {
      final boolean success = nativeSetValue(getNativeHandle(), value);
      if (!success) {
        throw new RuntimeException("Failed to set global value");
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting global value", e);
    }
  }

  /**
   * Sets the value of this global to an integer (only if mutable and compatible type).
   *
   * @param value the new integer value
   * @throws JniResourceException if this global is closed or immutable
   * @throws RuntimeException if the value cannot be set or type is incompatible
   */
  public void setIntValue(final int value) {
    ensureNotClosed();
    validateMutable();

    try {
      final boolean success = nativeSetIntValue(getNativeHandle(), value);
      if (!success) {
        throw new RuntimeException("Failed to set global int value");
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting global int value", e);
    }
  }

  /**
   * Sets the value of this global to a long (only if mutable and compatible type).
   *
   * @param value the new long value
   * @throws JniResourceException if this global is closed or immutable
   * @throws RuntimeException if the value cannot be set or type is incompatible
   */
  public void setLongValue(final long value) {
    ensureNotClosed();
    validateMutable();

    try {
      final boolean success = nativeSetLongValue(getNativeHandle(), value);
      if (!success) {
        throw new RuntimeException("Failed to set global long value");
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting global long value", e);
    }
  }

  /**
   * Sets the value of this global to a float (only if mutable and compatible type).
   *
   * @param value the new float value
   * @throws JniResourceException if this global is closed or immutable
   * @throws RuntimeException if the value cannot be set or type is incompatible
   */
  public void setFloatValue(final float value) {
    ensureNotClosed();
    validateMutable();

    try {
      final boolean success = nativeSetFloatValue(getNativeHandle(), value);
      if (!success) {
        throw new RuntimeException("Failed to set global float value");
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting global float value", e);
    }
  }

  /**
   * Sets the value of this global to a double (only if mutable and compatible type).
   *
   * @param value the new double value
   * @throws JniResourceException if this global is closed or immutable
   * @throws RuntimeException if the value cannot be set or type is incompatible
   */
  public void setDoubleValue(final double value) {
    ensureNotClosed();
    validateMutable();

    try {
      final boolean success = nativeSetDoubleValue(getNativeHandle(), value);
      if (!success) {
        throw new RuntimeException("Failed to set global double value");
      }
    } catch (final RuntimeException e) {
      throw e;
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error setting global double value", e);
    }
  }

  // Interface implementation methods for WasmGlobal

  @Override
  public WasmValue get() {
    ensureNotClosed();
    try {
      final Object value = getValue();
      final String typeString = getValueType();
      
      // Convert Object value and type string to WasmValue
      return convertToWasmValue(value, typeString);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to get global value as WasmValue", e);
    }
  }

  @Override
  public void set(final WasmValue value) {
    JniValidation.requireNonNull(value, "value");
    ensureNotClosed();
    validateMutable();
    
    try {
      final Object objectValue = convertFromWasmValue(value);
      setValue(objectValue);
    } catch (final Exception e) {
      throw new RuntimeException("Failed to set global value from WasmValue", e);
    }
  }

  @Override
  public WasmValueType getType() {
    final String typeString = getValueType();
    // Convert string type to WasmValueType enum
    switch (typeString.toLowerCase()) {
      case "i32":
        return WasmValueType.I32;
      case "i64":
        return WasmValueType.I64;
      case "f32":
        return WasmValueType.F32;
      case "f64":
        return WasmValueType.F64;
      case "v128":
        return WasmValueType.V128;
      case "funcref":
        return WasmValueType.FUNCREF;
      case "externref":
        return WasmValueType.EXTERNREF;
      default:
        return WasmValueType.I32; // Default fallback
    }
  }

  private WasmValue convertToWasmValue(final Object value, final String typeString) {
    if (value == null) {
      return WasmValue.i32(0); // Default for null
    }
    
    switch (typeString.toLowerCase()) {
      case "i32":
        return WasmValue.i32(((Number) value).intValue());
      case "i64":
        return WasmValue.i64(((Number) value).longValue());
      case "f32":
        return WasmValue.f32(((Number) value).floatValue());
      case "f64":
        return WasmValue.f64(((Number) value).doubleValue());
      default:
        return WasmValue.i32(0); // Default fallback
    }
  }

  private Object convertFromWasmValue(final WasmValue value) {
    switch (value.getType()) {
      case I32:
        return value.asInt();
      case I64:
        return value.asLong();
      case F32:
        return value.asFloat();
      case F64:
        return value.asDouble();
      default:
        return value.getValue();
    }
  }

  /**
   * Gets the resource type name for logging and error messages.
   *
   * @return the resource type name
   */
  @Override
  protected String getResourceType() {
    return "Global";
  }

  /**
   * Validates that this global is mutable.
   *
   * @throws JniResourceException if this global is immutable
   */
  private void validateMutable() {
    if (!isMutable()) {
      throw new JniResourceException("Global is immutable");
    }
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    nativeDestroyGlobal(nativeHandle);
  }

  // Native method declarations

  /**
   * Gets the value type of a global.
   *
   * @param globalHandle the native global handle
   * @return the value type name or null on error
   */
  private static native String nativeGetValueType(long globalHandle);

  /**
   * Checks if a global is mutable.
   *
   * @param globalHandle the native global handle
   * @return true if mutable, false if immutable
   */
  private static native boolean nativeIsMutable(long globalHandle);

  /**
   * Gets the value of a global as a generic Object.
   *
   * @param globalHandle the native global handle
   * @return the global value or null on error
   */
  private static native Object nativeGetValue(long globalHandle);

  /**
   * Gets the value of a global as an integer.
   *
   * @param globalHandle the native global handle
   * @return the global value as an integer
   */
  private static native int nativeGetIntValue(long globalHandle);

  /**
   * Gets the value of a global as a long.
   *
   * @param globalHandle the native global handle
   * @return the global value as a long
   */
  private static native long nativeGetLongValue(long globalHandle);

  /**
   * Gets the value of a global as a float.
   *
   * @param globalHandle the native global handle
   * @return the global value as a float
   */
  private static native float nativeGetFloatValue(long globalHandle);

  /**
   * Gets the value of a global as a double.
   *
   * @param globalHandle the native global handle
   * @return the global value as a double
   */
  private static native double nativeGetDoubleValue(long globalHandle);

  /**
   * Sets the value of a global with a generic Object.
   *
   * @param globalHandle the native global handle
   * @param value the new value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetValue(long globalHandle, Object value);

  /**
   * Sets the value of a global to an integer.
   *
   * @param globalHandle the native global handle
   * @param value the new integer value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetIntValue(long globalHandle, int value);

  /**
   * Sets the value of a global to a long.
   *
   * @param globalHandle the native global handle
   * @param value the new long value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetLongValue(long globalHandle, long value);

  /**
   * Sets the value of a global to a float.
   *
   * @param globalHandle the native global handle
   * @param value the new float value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetFloatValue(long globalHandle, float value);

  /**
   * Sets the value of a global to a double.
   *
   * @param globalHandle the native global handle
   * @param value the new double value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetDoubleValue(long globalHandle, double value);

  /**
   * Destroys a native global.
   *
   * @param globalHandle the native global handle
   */
  private static native void nativeDestroyGlobal(long globalHandle);
}
