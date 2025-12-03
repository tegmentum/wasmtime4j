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

package ai.tegmentum.wasmtime4j.coredump;

import java.util.Optional;

/**
 * Represents information about a WebAssembly instance captured in a coredump.
 *
 * <p>Each instance corresponds to an instantiated module that was active at the time of the trap.
 *
 * @since 1.0.0
 */
public interface CoreDumpInstance {

  /**
   * Returns the index of this instance in the coredump.
   *
   * @return the instance index
   */
  int getIndex();

  /**
   * Returns the index of the module this instance was created from.
   *
   * @return the module index
   */
  int getModuleIndex();

  /**
   * Returns the name of this instance, if available.
   *
   * @return an Optional containing the instance name, or empty if not available
   */
  Optional<String> getName();

  /**
   * Returns the number of memories in this instance.
   *
   * @return the memory count
   */
  int getMemoryCount();

  /**
   * Returns the number of globals in this instance.
   *
   * @return the global count
   */
  int getGlobalCount();

  /**
   * Returns the number of tables in this instance.
   *
   * @return the table count
   */
  int getTableCount();
}
