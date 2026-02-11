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

package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wit.WitCompatibilityResult;
import ai.tegmentum.wasmtime4j.wit.WitInterfaceDefinition;
import java.util.Set;

/**
 * Core WebAssembly Component Model component interface.
 *
 * <p>This interface provides the fundamental component management features:
 *
 * <ul>
 *   <li>Component instantiation and lifecycle management
 *   <li>WIT interface introspection and validation
 *   <li>Component metadata and resource management
 *   <li>Basic dependency resolution
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
   * Gets the version of this component.
   *
   * @return the component version
   */
  ComponentVersion getVersion();

  /**
   * Gets the size of the component in bytes.
   *
   * @return the component size in bytes
   * @throws WasmException if the operation fails
   */
  long getSize() throws WasmException;

  /**
   * Gets metadata about this component.
   *
   * @return the component metadata
   */
  ComponentMetadata getMetadata();

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
   * Gets the dependency graph for this component.
   *
   * @return the component dependency graph
   * @throws WasmException if the operation fails
   */
  ComponentDependencyGraph getDependencyGraph() throws WasmException;

  /**
   * Resolves dependencies for this component.
   *
   * @param registry the component registry for dependency resolution
   * @return the resolved dependency set
   * @throws WasmException if dependency resolution fails
   */
  Set<Component> resolveDependencies(ComponentRegistry registry) throws WasmException;

  /**
   * Checks if this component is compatible with another component version.
   *
   * @param other the other component version
   * @return the compatibility result
   * @throws WasmException if compatibility check fails
   */
  ComponentCompatibility checkCompatibility(Component other) throws WasmException;

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
   * Gets resource usage information for this component.
   *
   * @return the resource usage information
   */
  ComponentResourceUsage getResourceUsage();

  /**
   * Gets the current lifecycle state of this component.
   *
   * @return the component lifecycle state
   */
  ComponentLifecycleState getLifecycleState();

  /**
   * Checks if this component is still valid and usable.
   *
   * @return true if the component is valid, false otherwise
   */
  boolean isValid();

  /**
   * Validates the integrity and health of this component.
   *
   * @param validationConfig the validation configuration
   * @return the validation result
   * @throws WasmException if validation fails
   */
  ComponentValidationResult validate(ComponentValidationConfig validationConfig)
      throws WasmException;

  @Override
  void close();
}
