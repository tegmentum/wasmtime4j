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

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Optional;
import ai.tegmentum.wasmtime4j.config.OptimizationLevel;

/**
 * Provides low-level access to compiled WebAssembly module data.
 *
 * <p>CompiledModule represents a WebAssembly module that has been compiled to native code by the
 * Cranelift compiler. This interface provides access to the compiled code artifacts for advanced
 * use cases such as:
 *
 * <ul>
 *   <li>Module caching and serialization
 *   <li>Code analysis and inspection
 *   <li>Custom loading and memory management
 *   <li>Debugging and profiling
 * </ul>
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Module module = engine.compileModule(wasmBytes);
 * CompiledModule compiled = module.getCompiledModule().orElseThrow();
 *
 * // Get code segment information
 * long codeSize = compiled.getCodeSize();
 * System.out.println("Compiled code size: " + codeSize + " bytes");
 *
 * // Serialize for caching
 * byte[] serialized = compiled.serialize();
 * Files.write(cachePath, serialized);
 * }</pre>
 *
 * @since 1.1.0
 */
public interface CompiledModule {

  /**
   * Gets the total size of the compiled code in bytes.
   *
   * @return the code size
   */
  long getCodeSize();

  /**
   * Gets the size of read-only data in bytes.
   *
   * @return the read-only data size
   */
  long getReadOnlyDataSize();

  /**
   * Gets the total number of compiled functions.
   *
   * @return the function count
   */
  int getFunctionCount();

  /**
   * Serializes the compiled module to bytes.
   *
   * <p>The serialized format can be later deserialized using {@link Module#deserialize(Engine,
   * byte[])} for faster loading, avoiding the need for recompilation.
   *
   * @return the serialized bytes
   * @throws WasmException if serialization fails
   */
  byte[] serialize() throws WasmException;

  /**
   * Gets information about a specific compiled function.
   *
   * @param functionIndex the function index
   * @return information about the compiled function
   * @throws IndexOutOfBoundsException if the index is invalid
   */
  CompiledFunction getFunction(int functionIndex);

  /**
   * Gets information about all compiled functions.
   *
   * @return a list of compiled function information
   */
  List<CompiledFunction> getFunctions();

  /**
   * Gets the memory image for this compiled module.
   *
   * @return an optional containing the memory image, or empty if not available
   */
  Optional<byte[]> getMemoryImage();

  /**
   * Gets the address range where the compiled code is loaded.
   *
   * @return the code address range
   */
  AddressRange getCodeRange();

  /**
   * Gets metadata about the compilation.
   *
   * @return the compilation metadata
   */
  CompilationMetadata getMetadata();

  /** Information about a compiled function. */
  interface CompiledFunction {

    /**
     * Gets the function index.
     *
     * @return the function index
     */
    int getIndex();

    /**
     * Gets the function name if available.
     *
     * @return an optional containing the function name
     */
    Optional<String> getName();

    /**
     * Gets the size of the compiled code for this function.
     *
     * @return the code size in bytes
     */
    long getCodeSize();

    /**
     * Gets the starting address of this function in the code segment.
     *
     * @return the start address
     */
    long getStartAddress();

    /**
     * Gets the ending address of this function in the code segment.
     *
     * @return the end address
     */
    long getEndAddress();

    /**
     * Gets the number of stack slots used by this function.
     *
     * @return the stack slot count
     */
    int getStackSlots();
  }

  /** Represents an address range in memory. */
  interface AddressRange {

    /**
     * Gets the start address.
     *
     * @return the start address
     */
    long getStart();

    /**
     * Gets the end address.
     *
     * @return the end address
     */
    long getEnd();

    /**
     * Gets the size of the range.
     *
     * @return the size in bytes
     */
    default long getSize() {
      return getEnd() - getStart();
    }

    /**
     * Checks if an address is within this range.
     *
     * @param address the address to check
     * @return true if the address is within the range
     */
    default boolean contains(final long address) {
      return address >= getStart() && address < getEnd();
    }
  }

  /** Metadata about the compilation process. */
  interface CompilationMetadata {

    /**
     * Gets the wasmtime version used for compilation.
     *
     * @return the wasmtime version
     */
    String getWasmtimeVersion();

    /**
     * Gets the target architecture.
     *
     * @return the target triple (e.g., "x86_64-unknown-linux-gnu")
     */
    String getTarget();

    /**
     * Gets the optimization level used.
     *
     * @return the optimization level
     */
    OptimizationLevel getOptimizationLevel();

    /**
     * Checks if debug info was included.
     *
     * @return true if debug info is included
     */
    boolean hasDebugInfo();

    /**
     * Gets the compilation timestamp.
     *
     * @return the timestamp in milliseconds since epoch
     */
    long getTimestamp();
  }
}
