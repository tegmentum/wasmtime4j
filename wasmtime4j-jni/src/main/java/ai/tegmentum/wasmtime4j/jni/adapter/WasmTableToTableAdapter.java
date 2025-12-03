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

import ai.tegmentum.wasmtime4j.Table;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Adapter class that wraps a WasmTable instance to implement the Table interface.
 *
 * <p>This adapter bridges the gap between the comprehensive WasmTable interface and the simpler
 * Table interface used by the Caller interface. It handles type conversions and method signature
 * differences between the two interfaces.
 *
 * @since 1.0.0
 */
public final class WasmTableToTableAdapter implements Table {

  private final WasmTable delegate;

  /**
   * Creates a new adapter wrapping the given WasmTable instance.
   *
   * @param delegate the WasmTable instance to wrap
   * @throws IllegalArgumentException if delegate is null
   */
  public WasmTableToTableAdapter(final WasmTable delegate) {
    if (delegate == null) {
      throw new IllegalArgumentException("WasmTable delegate cannot be null");
    }
    this.delegate = delegate;
  }

  @Override
  public long getSize() {
    return delegate.getSize();
  }

  @Override
  public long grow(final long deltaElements, final Object initValue) throws WasmException {
    if (deltaElements > Integer.MAX_VALUE) {
      throw new WasmException("Cannot grow by more than " + Integer.MAX_VALUE + " elements");
    }

    try {
      final int result = delegate.grow((int) deltaElements, initValue);
      return result;
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Table grow failed: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Table grow failed: " + e.getMessage(), e);
    }
  }

  @Override
  public Object get(final long index) throws WasmException {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    if (index > Integer.MAX_VALUE) {
      throw new WasmException("Index exceeds maximum supported value");
    }

    try {
      return delegate.get((int) index);
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Table get out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Table get failed: " + e.getMessage(), e);
    }
  }

  @Override
  public void set(final long index, final Object value) throws WasmException {
    if (index < 0) {
      throw new IllegalArgumentException("Index cannot be negative");
    }
    if (index > Integer.MAX_VALUE) {
      throw new WasmException("Index exceeds maximum supported value");
    }

    try {
      delegate.set((int) index, value);
    } catch (final IndexOutOfBoundsException e) {
      throw new WasmException("Table set out of bounds: " + e.getMessage(), e);
    } catch (final RuntimeException e) {
      throw new WasmException("Table set failed: " + e.getMessage(), e);
    }
  }

  @Override
  public TableElementType getElementType() {
    final WasmValueType wasmType = delegate.getElementType();
    if (wasmType == null) {
      return TableElementType.EXTERNREF; // Default
    }

    // Map WasmValueType to TableElementType
    switch (wasmType) {
      case FUNCREF:
        return TableElementType.FUNCREF;
      case EXTERNREF:
      default:
        return TableElementType.EXTERNREF;
    }
  }

  @Override
  public long getMaxSize() {
    return delegate.getMaxSize();
  }

  @Override
  public boolean isValid() {
    // WasmTable doesn't have a direct isValid method, so we try a safe operation
    try {
      delegate.getSize();
      return true;
    } catch (final Exception e) {
      return false;
    }
  }

  /**
   * Gets the underlying WasmTable delegate.
   *
   * @return the delegate WasmTable instance
   */
  public WasmTable getDelegate() {
    return delegate;
  }
}
