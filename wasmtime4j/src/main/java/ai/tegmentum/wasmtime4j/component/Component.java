/*
 * Copyright 2024 Tegmentum AI
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

import ai.tegmentum.wasmtime4j.ResourcesRequired;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.nio.file.Path;
import java.util.Optional;
import java.util.Set;

/**
 * Core WebAssembly Component Model component interface.
 *
 * <p>This interface provides the fundamental component management features:
 *
 * <ul>
 *   <li>Component instantiation and lifecycle management
 *   <li>WIT interface introspection and validation
 * </ul>
 *
 * <p>Components are created through the {@link ComponentEngine} factory and represent compiled
 * WebAssembly components that can be instantiated and executed.
 *
 * @since 1.0.0
 */
public interface Component extends AutoCloseable {

  /**
   * Gets the unique identifier for this component.
   *
   * @return the component identifier
   */
  String getId();

  /**
   * Gets the size of the component in bytes.
   *
   * @return the component size in bytes
   * @throws WasmException if the operation fails
   */
  long getSize() throws WasmException;

  /**
   * Checks if the component exports the specified interface.
   *
   * @param interfaceName the interface name to check
   * @return true if the interface is exported, false otherwise
   * @throws WasmException if the operation fails
   */
  boolean exportsInterface(String interfaceName) throws WasmException;

  /**
   * Checks if the component imports the specified interface.
   *
   * @param interfaceName the interface name to check
   * @return true if the interface is imported, false otherwise
   * @throws WasmException if the operation fails
   */
  boolean importsInterface(String interfaceName) throws WasmException;

  /**
   * Gets all exported interfaces from this component.
   *
   * @return set of exported interface names
   * @throws WasmException if the operation fails
   */
  Set<String> getExportedInterfaces() throws WasmException;

  /**
   * Gets all imported interfaces required by this component.
   *
   * @return set of imported interface names
   * @throws WasmException if the operation fails
   */
  Set<String> getImportedInterfaces() throws WasmException;

  /**
   * Creates a new instance of this component.
   *
   * @return a new component instance
   * @throws WasmException if instantiation fails
   */
  ComponentInstance instantiate() throws WasmException;

  /**
   * Creates a new instance of this component with configuration.
   *
   * @param config the instance configuration
   * @return a new component instance
   * @throws WasmException if instantiation fails
   */
  ComponentInstance instantiate(ComponentInstanceConfig config) throws WasmException;

  /**
   * Gets the WIT interface definition for this component.
   *
   * @return the WIT interface definition
   * @throws WasmException if WIT interface retrieval fails
   */
  WitInterfaceDefinition getWitInterface() throws WasmException;

  /**
   * Validates WIT interface compatibility with another component.
   *
   * @param other the other component
   * @return the WIT compatibility result
   * @throws WasmException if compatibility check fails
   */
  WitCompatibilityResult checkWitCompatibility(Component other) throws WasmException;

  /**
   * Gets a pre-computed export index for efficient repeated lookups.
   *
   * <p>The returned index can be passed to {@link ComponentInstance#getFunc(ComponentExportIndex)}
   * for O(1) function lookup instead of string-based O(n) lookup.
   *
   * @param instanceIndex an optional parent instance export index for nested lookups, or null for
   *     root-level exports
   * @param name the name of the export to look up
   * @return an Optional containing the export index if found, or empty if not found
   * @throws WasmException if the lookup fails due to an error
   * @throws IllegalArgumentException if name is null or empty
   * @since 1.0.0
   */
  Optional<ComponentExportIndex> exportIndex(ComponentExportIndex instanceIndex, String name)
      throws WasmException;

  /**
   * Serializes this compiled component to a byte array for caching or distribution.
   *
   * <p>Serialized components can be stored to disk, sent over the network, or cached for faster
   * startup times. The serialized data includes the compiled code and all necessary metadata for
   * instantiation.
   *
   * @return the serialized component data
   * @throws WasmException if serialization fails
   * @since 1.0.0
   */
  byte[] serialize() throws WasmException;

  /**
   * Deserializes a component from previously serialized bytes.
   *
   * <p>This method can be used to quickly load a previously compiled component without going
   * through the compilation process again. The bytes must have been created by a compatible version
   * of the same engine.
   *
   * @param engine the component engine to use for deserialization
   * @param bytes the serialized component data
   * @return the deserialized Component
   * @throws WasmException if deserialization fails or data is invalid
   * @throws IllegalArgumentException if engine or bytes is null
   * @since 1.0.0
   */
  static Component deserialize(final ComponentEngine engine, final byte[] bytes)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (bytes == null || bytes.length == 0) {
      throw new IllegalArgumentException("bytes cannot be null or empty");
    }
    return engine.deserializeComponent(bytes);
  }

  /**
   * Gets the resources required to instantiate this component.
   *
   * <p>Returns information about the memory and table resources the component needs. This can be
   * used for resource planning and validation before attempting instantiation.
   *
   * <p>Returns empty if the component imports other modules or components whose resource
   * requirements cannot be statically determined.
   *
   * @return an Optional containing ResourcesRequired if available, empty otherwise
   * @throws WasmException if the operation fails
   * @since 1.0.0
   */
  Optional<ResourcesRequired> resourcesRequired() throws WasmException;

  /**
   * Deserializes a component from a previously serialized file.
   *
   * <p>This is more efficient than reading the file into memory and then calling {@link
   * #deserialize(ComponentEngine, byte[])} because it uses memory-mapped I/O to avoid copying the
   * file contents into memory.
   *
   * <p>The file must have been created by a previous call to {@link #serialize()} on a component
   * compiled with a compatible engine configuration.
   *
   * @param engine the component engine to use for deserialization
   * @param path the path to the serialized component file
   * @return the deserialized Component
   * @throws WasmException if deserialization fails or the file is invalid
   * @throws IllegalArgumentException if engine or path is null
   * @since 1.0.0
   */
  static Component deserializeFile(final ComponentEngine engine, final Path path)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return engine.deserializeComponentFile(path.toString());
  }

  /**
   * Compiles a WebAssembly component from a file on disk.
   *
   * <p>This is a convenience method that reads the file and compiles it. The file can contain
   * either binary WebAssembly (.wasm) or WebAssembly text format (.wat).
   *
   * @param engine the component engine to use for compilation
   * @param path the path to the WebAssembly component file
   * @return a compiled Component
   * @throws WasmException if compilation fails or the file cannot be read
   * @throws IllegalArgumentException if engine or path is null
   * @since 1.1.0
   */
  static Component fromFile(final ComponentEngine engine, final Path path) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (path == null) {
      throw new IllegalArgumentException("path cannot be null");
    }
    return engine.compileComponentFile(path);
  }

  /**
   * Compiles a WebAssembly component from binary bytes.
   *
   * <p>This method validates that the provided bytes are in binary WebAssembly format (not WAT text
   * format) by checking the WebAssembly magic number ({@code \0asm}), then compiles the component.
   *
   * <p>Unlike {@link ComponentEngine#compileComponent(byte[])} which accepts both binary and text
   * formats, this method strictly requires binary format.
   *
   * @param engine the component engine to use for compilation
   * @param wasmBytes the binary WebAssembly component bytes
   * @return a compiled Component
   * @throws WasmException if compilation fails
   * @throws IllegalArgumentException if engine or wasmBytes is null, or if the bytes are not in
   *     binary format
   * @since 1.1.0
   */
  static Component fromBinary(final ComponentEngine engine, final byte[] wasmBytes)
      throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("engine cannot be null");
    }
    if (wasmBytes == null) {
      throw new IllegalArgumentException("wasmBytes cannot be null");
    }
    if (wasmBytes.length < 4) {
      throw new IllegalArgumentException(
          "Binary wasm bytes too short (minimum 4 bytes for magic number)");
    }
    // Validate WebAssembly binary magic number: \0asm (0x00 0x61 0x73 0x6D)
    if (wasmBytes[0] != 0x00
        || wasmBytes[1] != 0x61
        || wasmBytes[2] != 0x73
        || wasmBytes[3] != 0x6D) {
      throw new IllegalArgumentException(
          "Not binary wasm: missing WebAssembly magic number. "
              + "Use ComponentEngine.compileComponent() for WAT text format.");
    }
    return engine.compileComponent(wasmBytes);
  }

  /**
   * Gets the memory address range of the compiled image for this component.
   *
   * <p>This returns the range in the process's virtual address space where the compiled machine
   * code for this component resides. This is useful for tools that need to identify which addresses
   * belong to JIT-compiled WebAssembly code.
   *
   * @return the image range containing start and end addresses
   * @throws WasmException if the component is no longer valid or the operation fails
   * @since 1.1.0
   */
  ai.tegmentum.wasmtime4j.ImageRange imageRange() throws WasmException;

  /**
   * Pre-initializes this component's copy-on-write image for faster instantiation.
   *
   * <p>When using copy-on-write memory initialization (the default), this method eagerly creates
   * the memory-mapped image used to initialize linear memories during instantiation. Without this
   * call, the image is created lazily on first instantiation.
   *
   * <p>Calling this is beneficial for server-side use cases where the first instantiation latency
   * matters and you want to front-load the cost during component compilation/loading.
   *
   * @throws WasmException if creating the copy-on-write image fails
   * @since 1.1.0
   */
  default void initializeCopyOnWriteImage() throws WasmException {
    // Default no-op; runtime implementations override with native calls
  }

  /**
   * Gets the component type description for this component.
   *
   * <p>This method corresponds to Wasmtime's {@code Component::component_type()} and returns a
   * frozen snapshot of the component's type-level information, including all imports and exports.
   *
   * <p>The returned {@link ComponentTypeInfo} can be used for programmatic introspection, type
   * compatibility checking, and interface discovery.
   *
   * @return the component type information
   * @throws WasmException if type introspection fails
   * @since 1.1.0
   */
  default ComponentTypeInfo componentType() throws WasmException {
    return new ComponentTypeInfo(getImportedInterfaces(), getExportedInterfaces());
  }

  /**
   * Gets the engine that was used to compile this component.
   *
   * @return the ComponentEngine used for compilation
   * @since 1.1.0
   */
  ComponentEngine getComponentEngine();

  /**
   * Checks if this component is still valid and usable.
   *
   * @return true if the component is valid, false otherwise
   */
  boolean isValid();

  @Override
  void close();
}
