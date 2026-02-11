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

/**
 * Represents a WebAssembly coredump generated when a trap occurs.
 *
 * <p>A coredump captures the complete state of a WebAssembly execution at the point of a trap,
 * including stack frames, global values, and memory contents. This information is useful for
 * post-mortem debugging and analysis of WebAssembly execution failures.
 *
 * <p>Coredumps follow the WebAssembly Coredump specification and can be serialized to a standard
 * format for analysis with external tools.
 *
 * @see ai.tegmentum.wasmtime4j.config.EngineConfig#setCoredumpOnTrap(boolean)
 * @since 1.0.0
 */
public interface WasmCoreDump {

  /**
   * Returns the name of the coredump.
   *
   * <p>This is typically derived from the module name or set during creation.
   *
   * @return the coredump name, or null if not available
   */
  String getName();

  /**
   * Returns the list of stack frames captured in this coredump.
   *
   * <p>The frames are ordered from the most recent (top of stack) to the oldest (bottom of stack).
   * Each frame represents a function call that was active at the time of the trap.
   *
   * @return an unmodifiable list of stack frames
   */
  List<CoreDumpFrame> getFrames();

  /**
   * Returns the list of module names that were loaded at the time of the trap.
   *
   * @return an unmodifiable list of module names
   */
  List<String> getModules();

  /**
   * Returns the list of instance information captured in this coredump.
   *
   * <p>Each instance represents a WebAssembly module instance that was active at the time of the
   * trap. This includes information about the instance's index and associated module.
   *
   * @return an unmodifiable list of instance information
   */
  List<CoreDumpInstance> getInstances();

  /**
   * Returns the list of global values captured in this coredump.
   *
   * <p>Global values represent the state of all WebAssembly globals at the time of the trap,
   * including both mutable and immutable globals.
   *
   * @return an unmodifiable list of global values
   */
  List<CoreDumpGlobal> getGlobals();

  /**
   * Returns the list of memory snapshots captured in this coredump.
   *
   * <p>Each memory snapshot contains the address ranges and data of WebAssembly linear memories at
   * the time of the trap.
   *
   * @return an unmodifiable list of memory snapshots
   */
  List<CoreDumpMemory> getMemories();

  /**
   * Serializes this coredump to a byte array in the standard WebAssembly coredump format.
   *
   * <p>The serialized format follows the WebAssembly Coredump specification and can be analyzed
   * with external debugging tools.
   *
   * @return the serialized coredump as a byte array
   */
  byte[] serialize();

  /**
   * Returns the total size of the coredump in bytes.
   *
   * <p>This includes all captured data: frames, globals, and memory snapshots.
   *
   * @return the size of the coredump in bytes
   */
  long getSize();

  /**
   * Returns the trap message that caused this coredump to be generated.
   *
   * @return the trap message, or null if not available
   */
  String getTrapMessage();
}
