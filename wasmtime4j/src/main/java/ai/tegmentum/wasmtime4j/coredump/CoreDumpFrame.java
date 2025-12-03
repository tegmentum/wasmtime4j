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

import java.util.List;
import java.util.Optional;

/**
 * Represents a single stack frame captured in a WebAssembly coredump.
 *
 * <p>Each frame corresponds to a function call that was active at the time of the trap. The frame
 * contains information about the function, its location, and the values of local variables.
 *
 * @since 1.0.0
 */
public interface CoreDumpFrame {

  /**
   * Returns the index of the function within its module.
   *
   * @return the function index
   */
  int getFuncIndex();

  /**
   * Returns the name of the function, if available.
   *
   * <p>Function names are typically available when debug information is present in the module.
   *
   * @return an Optional containing the function name, or empty if not available
   */
  Optional<String> getFuncName();

  /**
   * Returns the index of the module containing this function.
   *
   * @return the module index
   */
  int getModuleIndex();

  /**
   * Returns the name of the module containing this function, if available.
   *
   * @return an Optional containing the module name, or empty if not available
   */
  Optional<String> getModuleName();

  /**
   * Returns the byte offset within the function where execution was at the time of the trap.
   *
   * @return the offset in bytes from the start of the function
   */
  int getOffset();

  /**
   * Returns the values of local variables in this frame.
   *
   * <p>The list includes both function parameters and local variables. Values are represented as
   * raw byte arrays in their WebAssembly encoding.
   *
   * @return an unmodifiable list of local variable values
   */
  List<byte[]> getLocals();

  /**
   * Returns the values on the operand stack in this frame.
   *
   * <p>Stack values are represented as raw byte arrays in their WebAssembly encoding.
   *
   * @return an unmodifiable list of stack values
   */
  List<byte[]> getStack();

  /**
   * Returns whether this is the trap frame (the frame where the trap occurred).
   *
   * @return true if this is the trap frame
   */
  boolean isTrapFrame();
}
