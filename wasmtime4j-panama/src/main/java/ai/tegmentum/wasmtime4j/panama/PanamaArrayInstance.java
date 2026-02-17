/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
import ai.tegmentum.wasmtime4j.gc.GcException;
import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.GcValue;

/**
 * Panama implementation of a GC array instance.
 *
 * @since 1.0.0
 */
class PanamaArrayInstance extends PanamaGcObject implements ArrayInstance {

  private final PanamaGcRuntime runtime;
  private final ArrayType arrayType;
  private final int typeId;
  private final int length;

  PanamaArrayInstance(
      final PanamaGcRuntime runtime,
      final long objectId,
      final ArrayType arrayType,
      final int typeId,
      final int length) {
    super(objectId);
    this.runtime = runtime;
    this.arrayType = arrayType;
    this.typeId = typeId;
    this.length = length;
  }

  @Override
  public ArrayType getType() {
    return arrayType;
  }

  @Override
  public GcValue getElement(final int index) throws GcException {
    return runtime.getArrayElement(this, index);
  }

  @Override
  public void setElement(final int index, final GcValue value) throws GcException {
    runtime.setArrayElement(this, index, value);
  }

  @Override
  public int getLength() {
    return length;
  }

  @Override
  public int getSizeBytes() {
    // Return approximate size based on array length and element size
    return 16 + (length * 8);
  }

  @Override
  public boolean refEquals(final GcObject other) {
    return runtime.refEquals(this, other);
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.ARRAY_REF;
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.externref(this);
  }
}
