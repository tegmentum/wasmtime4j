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

import ai.tegmentum.wasmtime4j.WasmValueType;
import java.util.Optional;

/**
 * Represents a global variable value captured in a WebAssembly coredump.
 *
 * <p>Global values represent the state of WebAssembly globals at the time of the trap.
 *
 * @since 1.0.0
 */
public interface CoreDumpGlobal {

  /**
   * Returns the index of the instance containing this global.
   *
   * @return the instance index
   */
  int getInstanceIndex();

  /**
   * Returns the index of this global within its instance.
   *
   * @return the global index
   */
  int getGlobalIndex();

  /**
   * Returns the name of this global, if available.
   *
   * @return an Optional containing the global name, or empty if not available
   */
  Optional<String> getName();

  /**
   * Returns the WebAssembly value type of this global.
   *
   * @return the value type
   */
  WasmValueType getValueType();

  /**
   * Returns whether this global is mutable.
   *
   * @return true if the global is mutable
   */
  boolean isMutable();

  /**
   * Returns the raw value of this global as a byte array.
   *
   * <p>The encoding follows WebAssembly's binary format for the value type.
   *
   * @return the raw value bytes
   */
  byte[] getRawValue();

  /**
   * Returns the value as a 32-bit integer.
   *
   * <p>This method should only be called if the value type is I32.
   *
   * @return the integer value
   * @throws IllegalStateException if the value type is not I32
   */
  int getI32Value();

  /**
   * Returns the value as a 64-bit integer.
   *
   * <p>This method should only be called if the value type is I64.
   *
   * @return the long value
   * @throws IllegalStateException if the value type is not I64
   */
  long getI64Value();

  /**
   * Returns the value as a 32-bit float.
   *
   * <p>This method should only be called if the value type is F32.
   *
   * @return the float value
   * @throws IllegalStateException if the value type is not F32
   */
  float getF32Value();

  /**
   * Returns the value as a 64-bit float.
   *
   * <p>This method should only be called if the value type is F64.
   *
   * @return the double value
   * @throws IllegalStateException if the value type is not F64
   */
  double getF64Value();
}
