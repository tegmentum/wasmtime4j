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

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WeakEngine;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import java.util.Optional;

/**
 * JNI implementation of {@link WeakEngine}.
 *
 * <p>Wraps a native WeakEngine handle that holds a weak reference to the underlying Wasmtime
 * engine. The weak reference does not prevent the engine from being freed.
 *
 * @since 1.1.0
 */
final class JniWeakEngine extends JniResource implements WeakEngine {

  /** The engine that created this weak reference, used for upgrade(). */
  private final JniEngine sourceEngine;

  /**
   * Creates a new JNI weak engine wrapper.
   *
   * @param nativeHandle the native weak engine handle
   * @param sourceEngine the engine that created this weak reference
   */
  JniWeakEngine(final long nativeHandle, final JniEngine sourceEngine) {
    super(nativeHandle);
    this.sourceEngine = sourceEngine;
  }

  @Override
  public Optional<Engine> upgrade() {
    if (isClosed()) {
      return Optional.empty();
    }
    final long enginePtr = nativeUpgrade(nativeHandle);
    if (enginePtr == 0) {
      return Optional.empty();
    }
    return Optional.of(
        new JniEngine(enginePtr, sourceEngine.getRuntime(), sourceEngine.getConfig()));
  }

  @Override
  public boolean isValid() {
    return !isClosed();
  }

  @Override
  protected void doClose() {
    nativeDestroyWeakEngine(nativeHandle);
  }

  @Override
  protected String getResourceType() {
    return "JniWeakEngine";
  }

  // --- Native methods ---

  private static native long nativeUpgrade(long weakHandle);

  private static native void nativeDestroyWeakEngine(long weakHandle);
}
