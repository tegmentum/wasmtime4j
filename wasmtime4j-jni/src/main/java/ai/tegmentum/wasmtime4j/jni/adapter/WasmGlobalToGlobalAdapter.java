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

package ai.tegmentum.wasmtime4j.jni.adapter;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.memory.Global;

/**
 * Adapter class that wraps a WasmGlobal instance to implement the Global interface.
 *
 * <p>This adapter bridges the gap between the comprehensive WasmGlobal interface and the simpler
 * Global interface used by the Caller interface. It handles type conversions and method signature
 * differences between the two interfaces.
 *
 * @since 1.0.0
 */
public final class WasmGlobalToGlobalAdapter implements Global {

  private final WasmGlobal delegate;

  /**
   * Creates a new adapter wrapping the given WasmGlobal instance.
   *
   * @param delegate the WasmGlobal instance to wrap
   * @throws IllegalArgumentException if delegate is null
   */
  public WasmGlobalToGlobalAdapter(final WasmGlobal delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("WasmGlobal delegate cannot be null");
    }
    this.delegate = delegate;
  }

  @Override
  public Object getValue() throws WasmException {
    try {
      final WasmValue value = delegate.get();
      return value != null ? value.getValue() : null;
    } catch (final RuntimeException e) {
      throw new WasmException("Failed to get global value: " + e.getMessage(), e);
    }
  }

  @Override
  public void setValue(final Object value) throws WasmException {
    if (!isMutable()) {
      throw new WasmException("Cannot set value of immutable global");
    }

    try {
      final WasmValue wasmValue = convertToWasmValue(value);
      delegate.set(wasmValue);
    } catch (final RuntimeException e) {
      throw new WasmException("Failed to set global value: " + e.getMessage(), e);
    }
  }

  @Override
  public GlobalValueType getValueType() {
    final WasmValueType wasmType = delegate.getType();
    if (wasmType == null) {
      return GlobalValueType.I32; // Default
    }

    // Map WasmValueType to GlobalValueType
    switch (wasmType) {
      case I32:
        return GlobalValueType.I32;
      case I64:
        return GlobalValueType.I64;
      case F32:
        return GlobalValueType.F32;
      case F64:
        return GlobalValueType.F64;
      case V128:
        return GlobalValueType.V128;
      case FUNCREF:
        return GlobalValueType.FUNCREF;
      case EXTERNREF:
        return GlobalValueType.EXTERNREF;
      default:
        return GlobalValueType.I32;
    }
  }

  @Override
  public boolean isMutable() {
    return delegate.isMutable();
  }

  @Override
  public boolean isValid() {
    // WasmGlobal doesn't have a direct isValid method, so we try a safe operation
    try {
      delegate.get();
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  @Override
  public int getIntValue() throws WasmException {
    final Object value = getValue();
    if (value instanceof Number) {
      return ((Number) value).intValue();
    }
    throw new WasmException("Global value is not an integer");
  }

  @Override
  public long getLongValue() throws WasmException {
    final Object value = getValue();
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    throw new WasmException("Global value is not a long");
  }

  @Override
  public float getFloatValue() throws WasmException {
    final Object value = getValue();
    if (value instanceof Number) {
      return ((Number) value).floatValue();
    }
    throw new WasmException("Global value is not a float");
  }

  @Override
  public double getDoubleValue() throws WasmException {
    final Object value = getValue();
    if (value instanceof Number) {
      return ((Number) value).doubleValue();
    }
    throw new WasmException("Global value is not a double");
  }

  @Override
  public void setIntValue(final int value) throws WasmException {
    setValue(value);
  }

  @Override
  public void setLongValue(final long value) throws WasmException {
    setValue(value);
  }

  @Override
  public void setFloatValue(final float value) throws WasmException {
    setValue(value);
  }

  @Override
  public void setDoubleValue(final double value) throws WasmException {
    setValue(value);
  }

  /**
   * Converts a Java object to a WasmValue based on the global's type.
   *
   * @param value the Java value to convert
   * @return the converted WasmValue
   */
  private WasmValue convertToWasmValue(final Object value) {
    if (value == null) {
      return WasmValue.externref(null);
    }

    final WasmValueType type = delegate.getType();
    if (type == null) {
      // Infer type from value
      if (value instanceof Integer) {
        return WasmValue.i32((Integer) value);
      } else if (value instanceof Long) {
        return WasmValue.i64((Long) value);
      } else if (value instanceof Float) {
        return WasmValue.f32((Float) value);
      } else if (value instanceof Double) {
        return WasmValue.f64((Double) value);
      } else {
        return WasmValue.externref(value);
      }
    }

    // Convert based on expected type
    switch (type) {
      case I32:
        if (value instanceof Number) {
          return WasmValue.i32(((Number) value).intValue());
        }
        break;
      case I64:
        if (value instanceof Number) {
          return WasmValue.i64(((Number) value).longValue());
        }
        break;
      case F32:
        if (value instanceof Number) {
          return WasmValue.f32(((Number) value).floatValue());
        }
        break;
      case F64:
        if (value instanceof Number) {
          return WasmValue.f64(((Number) value).doubleValue());
        }
        break;
      case FUNCREF:
        return WasmValue.funcref(value);
      case EXTERNREF:
      default:
        return WasmValue.externref(value);
    }

    // Fallback to externref
    return WasmValue.externref(value);
  }

  /**
   * Gets the underlying WasmGlobal delegate.
   *
   * @return the delegate WasmGlobal instance
   */
  public WasmGlobal getDelegate() {
    return delegate;
  }
}
