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

/**
 * Describes the resources required to instantiate a WebAssembly module.
 *
 * <p>This class provides information about the memory, table, and other resources that will be
 * needed when instantiating a module. This can be used for resource planning and validation before
 * attempting instantiation.
 *
 * <p><b>Upstream mapping:</b> The following fields correspond to Wasmtime's {@code
 * ResourcesRequired} struct: {@link #getNumMemories()}, {@link #getMaximumMemoryBytes()}, {@link
 * #getNumTables()}, {@link #getMaximumTableElements()}. The remaining fields ({@link
 * #getMinimumMemoryBytes()}, {@link #getMinimumTableElements()}, {@link #getNumGlobals()}, {@link
 * #getNumFunctions()}) are custom extensions derived from module analysis.
 *
 * @since 1.1.0
 */
public final class ResourcesRequired {

  private final long minimumMemoryBytes;
  private final long maximumMemoryBytes;
  private final int minimumTableElements;
  private final int maximumTableElements;
  private final int numMemories;
  private final int numTables;
  private final int numGlobals;
  private final int numFunctions;

  /**
   * Creates a new ResourcesRequired instance.
   *
   * @param minimumMemoryBytes minimum memory required in bytes
   * @param maximumMemoryBytes maximum memory that may be allocated in bytes
   * @param minimumTableElements minimum table elements required
   * @param maximumTableElements maximum table elements that may be allocated
   * @param numMemories number of memory instances
   * @param numTables number of table instances
   * @param numGlobals number of global variables
   * @param numFunctions number of functions
   */
  public ResourcesRequired(
      final long minimumMemoryBytes,
      final long maximumMemoryBytes,
      final int minimumTableElements,
      final int maximumTableElements,
      final int numMemories,
      final int numTables,
      final int numGlobals,
      final int numFunctions) {
    this.minimumMemoryBytes = minimumMemoryBytes;
    this.maximumMemoryBytes = maximumMemoryBytes;
    this.minimumTableElements = minimumTableElements;
    this.maximumTableElements = maximumTableElements;
    this.numMemories = numMemories;
    this.numTables = numTables;
    this.numGlobals = numGlobals;
    this.numFunctions = numFunctions;
  }

  /**
   * Gets the minimum memory required in bytes.
   *
   * @return minimum memory in bytes
   */
  public long getMinimumMemoryBytes() {
    return minimumMemoryBytes;
  }

  /**
   * Gets the maximum memory that may be allocated in bytes.
   *
   * @return maximum memory in bytes, or -1 if unlimited
   */
  public long getMaximumMemoryBytes() {
    return maximumMemoryBytes;
  }

  /**
   * Gets the minimum number of table elements required.
   *
   * @return minimum table elements
   */
  public int getMinimumTableElements() {
    return minimumTableElements;
  }

  /**
   * Gets the maximum number of table elements that may be allocated.
   *
   * @return maximum table elements, or -1 if unlimited
   */
  public int getMaximumTableElements() {
    return maximumTableElements;
  }

  /**
   * Gets the number of memory instances in the module.
   *
   * @return number of memories
   */
  public int getNumMemories() {
    return numMemories;
  }

  /**
   * Gets the number of table instances in the module.
   *
   * @return number of tables
   */
  public int getNumTables() {
    return numTables;
  }

  /**
   * Gets the number of global variables in the module.
   *
   * @return number of globals
   */
  public int getNumGlobals() {
    return numGlobals;
  }

  /**
   * Gets the number of functions in the module.
   *
   * @return number of functions
   */
  public int getNumFunctions() {
    return numFunctions;
  }

  /**
   * Creates an empty ResourcesRequired for modules with no significant resource requirements.
   *
   * @return an empty ResourcesRequired instance
   */
  public static ResourcesRequired empty() {
    return new ResourcesRequired(0, 0, 0, 0, 0, 0, 0, 0);
  }

  @Override
  public String toString() {
    return "ResourcesRequired{"
        + "minimumMemoryBytes="
        + minimumMemoryBytes
        + ", maximumMemoryBytes="
        + maximumMemoryBytes
        + ", minimumTableElements="
        + minimumTableElements
        + ", maximumTableElements="
        + maximumTableElements
        + ", numMemories="
        + numMemories
        + ", numTables="
        + numTables
        + ", numGlobals="
        + numGlobals
        + ", numFunctions="
        + numFunctions
        + '}';
  }
}
