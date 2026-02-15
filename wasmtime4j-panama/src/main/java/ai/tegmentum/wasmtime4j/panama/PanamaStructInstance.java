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
import ai.tegmentum.wasmtime4j.gc.GcException;
import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.gc.StructInstance;
import ai.tegmentum.wasmtime4j.gc.StructType;

/**
 * Panama implementation of a GC struct instance.
 *
 * @since 1.0.0
 */
class PanamaStructInstance extends PanamaGcObject implements StructInstance {

  private final PanamaGcRuntime runtime;
  private final StructType structType;
  private final int typeId;

  PanamaStructInstance(
      final PanamaGcRuntime runtime,
      final long objectId,
      final StructType structType,
      final int typeId) {
    super(objectId);
    this.runtime = runtime;
    this.structType = structType;
    this.typeId = typeId;
  }

  @Override
  public StructType getType() {
    return structType;
  }

  @Override
  public GcValue getField(final int index) throws GcException {
    return runtime.getStructField(this, index);
  }

  @Override
  public void setField(final int index, final GcValue value) throws GcException {
    runtime.setStructField(this, index, value);
  }

  @Override
  public int getFieldCount() {
    return structType.getFields().size();
  }

  @Override
  public int getSizeBytes() {
    // Return approximate size based on struct fields
    return 16 + (structType.getFields().size() * 8);
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
    return GcReferenceType.STRUCT_REF;
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.externRef(this);
  }
}
