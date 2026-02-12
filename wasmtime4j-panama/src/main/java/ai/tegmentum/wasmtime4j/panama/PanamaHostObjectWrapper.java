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
 * Panama implementation of a host object wrapper for GC integration.
 *
 * @since 1.0.0
 */
final class PanamaHostObjectWrapper implements GcObject {

  private final PanamaGcRuntime runtime;
  private final long objectId;
  private final GcReferenceType gcType;

  PanamaHostObjectWrapper(
      final PanamaGcRuntime runtime, final long objectId, final GcReferenceType gcType) {
    this.runtime = runtime;
    this.objectId = objectId;
    this.gcType = gcType;
  }

  @Override
  public long getObjectId() {
    return objectId;
  }

  @Override
  public int getSizeBytes() {
    return 8; // Pointer size
  }

  @Override
  public boolean refEquals(final GcObject other) {
    if (other instanceof PanamaHostObjectWrapper) {
      return objectId == ((PanamaHostObjectWrapper) other).objectId;
    }
    return false;
  }

  @Override
  public GcObject castTo(final GcReferenceType type) {
    return this;
  }

  @Override
  public boolean isOfType(final GcReferenceType type) {
    return type == gcType || type == GcReferenceType.ANY_REF;
  }

  @Override
  public boolean isNull() {
    return false;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return gcType;
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.externRef(this);
  }
}
