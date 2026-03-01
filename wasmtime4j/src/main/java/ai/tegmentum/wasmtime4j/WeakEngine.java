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
package ai.tegmentum.wasmtime4j;

import java.io.Closeable;
import java.util.Optional;

/**
 * A weak reference to an {@link Engine} that does not prevent the engine from being freed.
 *
 * <p>A {@code WeakEngine} is obtained by calling {@link Engine#weak()} and can be used to check
 * whether the engine is still alive without preventing garbage collection. The weak reference can
 * be upgraded to a strong {@link Engine} reference using {@link #upgrade()}.
 *
 * <p>This is useful for caches and other data structures that want to reference an engine without
 * preventing it from being cleaned up.
 *
 * @since 1.1.0
 */
public interface WeakEngine extends Closeable {

  /**
   * Attempts to upgrade this weak reference to a strong {@link Engine} reference.
   *
   * <p>If the underlying engine has not yet been closed or garbage collected, this returns the
   * engine wrapped in an {@link Optional}. If the engine has been dropped, returns {@link
   * Optional#empty()}.
   *
   * @return an {@link Optional} containing the engine if it is still alive, or empty if it has been
   *     dropped
   */
  Optional<Engine> upgrade();

  /**
   * Checks if this weak reference is still valid (has not been closed).
   *
   * @return true if this weak reference can still be used
   */
  boolean isValid();

  /**
   * Closes this weak reference, releasing its native resources.
   *
   * <p>After closing, {@link #upgrade()} will always return empty and {@link #isValid()} will
   * return false.
   */
  @Override
  void close();
}
