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
package ai.tegmentum.wasmtime4j.component;

import java.util.List;

/**
 * A direct destination that provides synchronous access for writing values.
 *
 * <p>DirectDestination extends {@link Destination} with the ability to write values directly
 * without going through the asynchronous write path. This is useful for destinations that can
 * immediately accept values without buffering.
 *
 * @since 1.1.0
 */
public interface DirectDestination extends Destination {

  /**
   * Directly writes values to the destination without suspending.
   *
   * <p>Returns the number of values actually written. If the destination cannot accept any values
   * immediately, returns 0 without blocking.
   *
   * @param values the values to write
   * @return the number of values written
   * @throws IllegalArgumentException if values is null
   */
  int writeDirect(List<ComponentVal> values);
}
