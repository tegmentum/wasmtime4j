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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.WeakEngine;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import java.lang.foreign.MemorySegment;
import java.util.Optional;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of {@link WeakEngine}.
 *
 * <p>Wraps a native WeakEngine handle that holds a weak reference to the underlying Wasmtime
 * engine. The weak reference does not prevent the engine from being freed.
 *
 * @since 1.1.0
 */
final class PanamaWeakEngine implements WeakEngine {

  private static final Logger LOGGER = Logger.getLogger(PanamaWeakEngine.class.getName());
  private static final NativeEngineBindings NATIVE_BINDINGS = NativeEngineBindings.getInstance();

  private final MemorySegment nativeWeakEngine;
  private final PanamaEngine sourceEngine;
  private final NativeResourceHandle resourceHandle;

  /**
   * Creates a new Panama weak engine wrapper.
   *
   * @param nativeWeakEngine the native weak engine pointer
   * @param sourceEngine the engine that created this weak reference
   */
  PanamaWeakEngine(final MemorySegment nativeWeakEngine, final PanamaEngine sourceEngine) {
    this.nativeWeakEngine = nativeWeakEngine;
    this.sourceEngine = sourceEngine;

    // Capture pointer for safety net (must not capture 'this')
    final MemorySegment weakPtr = nativeWeakEngine;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaWeakEngine",
            () -> NATIVE_BINDINGS.weakEngineDestroy(weakPtr),
            this,
            () -> {
              LOGGER.warning("PanamaWeakEngine not explicitly closed, destroying via safety net");
              NATIVE_BINDINGS.weakEngineDestroy(weakPtr);
            });
  }

  @Override
  public Optional<Engine> upgrade() {
    if (resourceHandle.isClosed()) {
      return Optional.empty();
    }
    final MemorySegment enginePtr = NATIVE_BINDINGS.weakEngineUpgrade(nativeWeakEngine);
    if (enginePtr == null || enginePtr.equals(MemorySegment.NULL)) {
      return Optional.empty();
    }
    try {
      return Optional.of(
          new PanamaEngine(sourceEngine.getConfig(), sourceEngine.getRuntime(), enginePtr));
    } catch (final Exception e) {
      LOGGER.warning("Failed to create PanamaEngine from upgraded weak reference: " + e);
      return Optional.empty();
    }
  }

  @Override
  public boolean isValid() {
    return !resourceHandle.isClosed();
  }

  @Override
  public void close() {
    resourceHandle.close();
  }
}
