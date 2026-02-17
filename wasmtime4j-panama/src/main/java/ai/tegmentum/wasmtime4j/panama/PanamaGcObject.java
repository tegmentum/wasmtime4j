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

/**
 * Base GC object implementation for Panama FFI.
 *
 * <p>Wraps a native GC object ID and provides basic {@link GcObject} operations.
 *
 * @since 1.0.0
 */
class PanamaGcObject implements GcObject {

  private final long objectId;

  PanamaGcObject(final long objectId) {
    this.objectId = objectId;
  }

  @Override
  public long getObjectId() {
    return objectId;
  }

  @Override
  public int getSizeBytes() {
    // Size would need to be queried from native side
    return 0;
  }

  @Override
  public boolean refEquals(final GcObject other) {
    if (other instanceof PanamaGcObject) {
      return ((PanamaGcObject) other).objectId == this.objectId;
    }
    return false;
  }

  @Override
  public GcObject castTo(final GcReferenceType type) {
    if (!isOfType(type)) {
      throw new ClassCastException(
          "Cannot cast " + getReferenceType() + " to " + type + ": incompatible reference types");
    }
    return this;
  }

  @Override
  public boolean isOfType(final GcReferenceType type) {
    return getReferenceType().isSubtypeOf(type);
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.ANY_REF;
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.externref(this);
  }
}
