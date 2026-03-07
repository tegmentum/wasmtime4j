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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmTypeException;
import ai.tegmentum.wasmtime4j.jni.exception.JniResourceException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.util.Validation;
import java.util.logging.Logger;

/**
 * JNI implementation of the Global interface.
 *
 * <p>This class provides access to WebAssembly global variables through JNI calls to the native
 * Wasmtime library. Globals can store various value types and may be mutable or immutable.
 *
 * <p>This implementation ensures defensive programming to prevent JVM crashes and provides
 * comprehensive type checking for global variable access using Validation and the JniResource base
 * class.
 */
public final class JniGlobal extends JniResource implements WasmGlobal {

  private static final Logger LOGGER = Logger.getLogger(JniGlobal.class.getName());

  /** Store context required for global operations. */
  private final JniStore store;

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
   * Creates a new JNI global with the given native handle and store context.
   *
   * @param nativeHandle the native global handle
   * @param store the store context for this global
   * @throws JniResourceException if nativeHandle is invalid
   * @throws IllegalArgumentException if store is null
   */
  JniGlobal(final long nativeHandle, final JniStore store) {
    super(nativeHandle);
    Validation.requireNonNull(store, "store");
    this.store = store;
    LOGGER.fine("Created JNI global with handle: 0x" + Long.toHexString(nativeHandle));
  }

  /**
   * Ensures this global and its owning store are still usable.
   *
   * @throws JniResourceException if this global or its store has been closed
   */
  private void ensureUsable() {
    beginOperation();
    try {
      if (store.isClosed()) {
        throw new JniResourceException("Store is closed");
      }
    } finally {
      endOperation();
    }
  }

  /**
   * Gets the value type of this global.
   *
   * @return the value type name (e.g., "i32", "i64", "f32", "f64")
   * @throws JniResourceException if this global is closed
   * @throws RuntimeException if the type cannot be retrieved
   */
  public String getValueType() {
    ensureUsable();
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
    ensureUsable();
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
  public WasmValue getValue() {
    ensureUsable();
    try {
      final Object value = nativeGetValue(getNativeHandle(), store.getNativeHandle());
      final String typeString = getValueType();
      // Convert Object value and type string to WasmValue
      return convertToWasmValue(value, typeString);
    } catch (final RuntimeException e) {
      throw e;
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
    ensureUsable();
    try {
      return nativeGetIntValue(getNativeHandle(), store.getNativeHandle());
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
    ensureUsable();
    try {
      return nativeGetLongValue(getNativeHandle(), store.getNativeHandle());
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
    ensureUsable();
    try {
      return nativeGetFloatValue(getNativeHandle(), store.getNativeHandle());
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
    ensureUsable();
    try {
      return nativeGetDoubleValue(getNativeHandle(), store.getNativeHandle());
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
    Validation.requireNonNull(value, "value");
    ensureUsable();
    validateMutable();

    try {
      final boolean success = nativeSetValue(getNativeHandle(), store.getNativeHandle(), value);
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
    ensureUsable();
    validateMutable();

    try {
      final boolean success = nativeSetIntValue(getNativeHandle(), store.getNativeHandle(), value);
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
    ensureUsable();
    validateMutable();

    try {
      final boolean success = nativeSetLongValue(getNativeHandle(), store.getNativeHandle(), value);
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
    ensureUsable();
    validateMutable();

    try {
      final boolean success =
          nativeSetFloatValue(getNativeHandle(), store.getNativeHandle(), value);
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
    ensureUsable();
    validateMutable();

    try {
      final boolean success =
          nativeSetDoubleValue(getNativeHandle(), store.getNativeHandle(), value);
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
    return getValue();
  }

  @Override
  public void set(final WasmValue value) {
    Validation.requireNonNull(value, "value");
    ensureUsable();
    validateMutable();

    // Validate type compatibility
    final WasmValueType globalType = getType();
    final WasmValueType valueType = value.getType();
    if (globalType != valueType) {
      throw new WasmTypeException(
          "Type mismatch: cannot set " + valueType + " value on " + globalType + " global");
    }

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
      case "anyref":
        return WasmValueType.ANYREF;
      case "eqref":
        return WasmValueType.EQREF;
      case "i31ref":
        return WasmValueType.I31REF;
      case "structref":
        return WasmValueType.STRUCTREF;
      case "arrayref":
        return WasmValueType.ARRAYREF;
      case "nullref":
        return WasmValueType.NULLREF;
      case "nullfuncref":
        return WasmValueType.NULLFUNCREF;
      case "nullexternref":
        return WasmValueType.NULLEXTERNREF;
      case "exnref":
        return WasmValueType.EXNREF;
      case "nullexnref":
        return WasmValueType.NULLEXNREF;
      case "contref":
        return WasmValueType.CONTREF;
      case "nullcontref":
        return WasmValueType.NULLCONTREF;
      default:
        throw new IllegalStateException("Unknown global type: " + typeString);
    }
  }

  @Override
  public ai.tegmentum.wasmtime4j.type.GlobalType getGlobalType() {
    ensureUsable();
    try {
      final long[] typeInfo = nativeGetGlobalTypeInfo(getNativeHandle());
      if (typeInfo.length < 2) {
        throw new IllegalStateException("Invalid global type info from native");
      }
      final WasmValueType valueType = WasmValueType.fromNativeTypeCode((int) typeInfo[0]);
      final boolean isMutable = typeInfo[1] != 0;
      return new ai.tegmentum.wasmtime4j.jni.type.JniGlobalType(valueType, isMutable);
    } catch (final Exception e) {
      throw new RuntimeException("Unexpected error getting global type", e);
    }
  }

  private WasmValue convertToWasmValue(final Object value, final String typeString) {
    switch (typeString.toLowerCase()) {
      case "i32":
        return WasmValue.i32(((Number) value).intValue());
      case "i64":
        return WasmValue.i64(((Number) value).longValue());
      case "f32":
        return WasmValue.f32(((Number) value).floatValue());
      case "f64":
        return WasmValue.f64(((Number) value).doubleValue());
      case "v128":
        if (value instanceof byte[]) {
          return WasmValue.v128((byte[]) value);
        }
        throw new WasmTypeException("Expected byte array for v128, got " + value.getClass());
      case "funcref":
        if (value == null) {
          return WasmValue.funcref(null);
        }
        if (value instanceof Long) {
          return WasmValue.funcref(value);
        }
        throw new WasmTypeException("Expected Long or null for funcref, got " + value.getClass());
      case "externref":
        if (value == null) {
          return WasmValue.externref(null);
        }
        if (value instanceof Long) {
          return WasmValue.externref(value);
        }
        throw new WasmTypeException("Expected Long or null for externref, got " + value.getClass());
      case "anyref":
        if (value == null) {
          return WasmValue.nullAnyRef();
        }
        return WasmValue.anyref(value);
      case "eqref":
        if (value == null) {
          return WasmValue.nullEqRef();
        }
        return WasmValue.eqref(value);
      case "i31ref":
        if (value == null) {
          return WasmValue.nullI31Ref();
        }
        if (value instanceof Number) {
          return WasmValue.i31ref(((Number) value).intValue());
        }
        throw new WasmTypeException("Expected Number or null for i31ref, got " + value.getClass());
      case "structref":
        if (value == null) {
          return WasmValue.nullStructRef();
        }
        return WasmValue.structref(value);
      case "arrayref":
        if (value == null) {
          return WasmValue.nullArrayRef();
        }
        return WasmValue.arrayref(value);
      case "nullref":
        return WasmValue.nullRef();
      case "nullfuncref":
        return WasmValue.nullNullFuncRef();
      case "nullexternref":
        return WasmValue.nullNullExternRef();
      default:
        throw new WasmTypeException("Unknown type: " + typeString);
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
      case FUNCREF:
        {
          final Object funcrefValue = value.asFuncref();
          if (funcrefValue == null) {
            return null;
          }
          if (funcrefValue instanceof ai.tegmentum.wasmtime4j.func.FunctionReference) {
            // Get the Rust registry ID for the function reference
            if (funcrefValue instanceof JniFunctionReference) {
              return ((JniFunctionReference) funcrefValue).longValue();
            }
            throw new WasmTypeException(
                "Funcref must be a JniFunctionReference, got: " + funcrefValue.getClass());
          }
          // Already a handle (Long)
          return funcrefValue;
        }
      case EXTERNREF:
        {
          final Object externrefValue = value.asExternref();
          if (externrefValue == null) {
            return null;
          }
          if (externrefValue instanceof Long) {
            return externrefValue;
          }
          throw new WasmTypeException(
              "Externref must be a Long handle or null, got: " + externrefValue.getClass());
        }
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
   * @throws UnsupportedOperationException if this global is immutable
   */
  private void validateMutable() {
    if (!isMutable()) {
      throw new UnsupportedOperationException("Cannot modify immutable global");
    }
  }

  /**
   * Performs the actual native resource cleanup.
   *
   * <p>Note: In wasmtime, Globals are owned by the Store. Destroying a Global while the Store still
   * exists can corrupt the Store's internal slab state. We mark the Global as closed but don't
   * destroy it - the Store will handle cleanup.
   *
   * @throws Exception if there's an error during cleanup
   */
  @Override
  protected void doClose() throws Exception {
    // Note: Do NOT call nativeDestroyGlobal here. Globals are Store-owned resources.
    // The Store will clean up all its Globals when it is destroyed.
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
   * @param storeHandle the native store handle
   * @return the global value or null on error
   */
  private static native Object nativeGetValue(long globalHandle, long storeHandle);

  /**
   * Gets the value of a global as an integer.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @return the global value as an integer
   */
  private static native int nativeGetIntValue(long globalHandle, long storeHandle);

  /**
   * Gets the value of a global as a long.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @return the global value as a long
   */
  private static native long nativeGetLongValue(long globalHandle, long storeHandle);

  /**
   * Gets the value of a global as a float.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @return the global value as a float
   */
  private static native float nativeGetFloatValue(long globalHandle, long storeHandle);

  /**
   * Gets the value of a global as a double.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @return the global value as a double
   */
  private static native double nativeGetDoubleValue(long globalHandle, long storeHandle);

  /**
   * Sets the value of a global with a generic Object.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @param value the new value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetValue(long globalHandle, long storeHandle, Object value);

  /**
   * Sets the value of a global to an integer.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @param value the new integer value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetIntValue(long globalHandle, long storeHandle, int value);

  /**
   * Sets the value of a global to a long.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @param value the new long value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetLongValue(long globalHandle, long storeHandle, long value);

  /**
   * Sets the value of a global to a float.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @param value the new float value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetFloatValue(
      long globalHandle, long storeHandle, float value);

  /**
   * Sets the value of a global to a double.
   *
   * @param globalHandle the native global handle
   * @param storeHandle the native store handle
   * @param value the new double value
   * @return true on success, false on failure
   */
  private static native boolean nativeSetDoubleValue(
      long globalHandle, long storeHandle, double value);

  /**
   * Gets global type information directly from the global.
   *
   * @param globalHandle the native global handle
   * @return array containing [valueTypeCode, isMutable(0/1)]
   */
  private static native long[] nativeGetGlobalTypeInfo(long globalHandle);
}
