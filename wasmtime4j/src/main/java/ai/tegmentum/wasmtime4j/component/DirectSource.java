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

/**
 * A direct source that provides synchronous access to values in addition to async reads.
 *
 * <p>DirectSource extends {@link Source} with the ability to provide values directly without going
 * through the asynchronous read path. This is useful for sources that already have values available
 * in memory.
 *
 * @since 1.1.0
 */
public interface DirectSource extends Source {

  /**
   * Directly reads available values into the buffer without suspending.
   *
   * <p>Returns the number of values read. If no values are immediately available, returns 0 without
   * blocking.
   *
   * @param buffer the buffer to read values into
   * @return the number of values read
   * @throws IllegalArgumentException if buffer is null
   */
  int readDirect(VecBuffer buffer);
}
