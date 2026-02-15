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
import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.I31Instance;

/**
 * Panama implementation of a GC I31 instance.
 *
 * @since 1.0.0
 */
class PanamaI31Instance extends PanamaGcObject implements I31Instance {

  private final PanamaGcRuntime runtime;
  private final int value;

  PanamaI31Instance(final PanamaGcRuntime runtime, final long objectId, final int value) {
    super(objectId);
    this.runtime = runtime;
    this.value = value;
  }

  @Override
  public int getValue() {
    return value;
  }

  @Override
  public int getSignedValue() {
    return runtime.getI31SignedValue(getObjectId());
  }

  @Override
  public int getUnsignedValue() {
    return runtime.getI31UnsignedValue(getObjectId());
  }

  @Override
  public int getSizeBytes() {
    // I31 values are stored inline, minimal overhead
    return 4;
  }

  @Override
  public boolean refEquals(final GcObject other) {
    return runtime.refEquals(this, other);
  }

  @Override
  public GcObject castTo(final GcReferenceType type) {
    throw new UnsupportedOperationException(
        "not yet implemented: GC I31 type casting with validation");
  }

  @Override
  public boolean isOfType(final GcReferenceType type) {
    throw new UnsupportedOperationException("not yet implemented: GC I31 type checking");
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.I31_REF;
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.externRef(this);
  }
}
