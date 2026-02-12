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

import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.WeakGcReference;
import java.util.Optional;

/**
 * Panama implementation of a weak GC reference.
 *
 * @since 1.0.0
 */
final class PanamaWeakGcReference implements WeakGcReference {

  private final PanamaGcRuntime runtime;
  private final long objectId;
  private volatile Runnable finalizationCallback;
  private volatile boolean cleared = false;

  PanamaWeakGcReference(
      final PanamaGcRuntime runtime, final long objectId, final Runnable finalizationCallback) {
    this.runtime = runtime;
    this.objectId = objectId;
    this.finalizationCallback = finalizationCallback;
  }

  @Override
  public Optional<GcObject> get() {
    if (cleared) {
      return Optional.empty();
    }
    // Look up the object in the registry by its ID
    final PanamaGcObject object = runtime.lookupGcObject(objectId);
    return Optional.ofNullable(object);
  }

  @Override
  public boolean isCleared() {
    return cleared;
  }

  @Override
  public void clear() {
    cleared = true;
    // Note: clear() does NOT invoke finalization callback per interface contract
  }

  @Override
  public Runnable getFinalizationCallback() {
    return finalizationCallback;
  }

  @Override
  public void setFinalizationCallback(final Runnable callback) {
    this.finalizationCallback = callback;
  }
}
