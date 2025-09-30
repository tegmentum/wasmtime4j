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
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

/**
 * Advanced component linking system supporting dynamic loading, composition, and hot-swapping.
 *
 * <p>The ComponentLinker enables sophisticated component orchestration patterns including:
 * <ul>
 *   <li>Dynamic component loading and hot-swapping without service interruption</li>
 *   <li>Component versioning and compatibility checking</li>
 *   <li>Conditional component loading based on runtime capabilities</li>
 *   <li>Component dependency injection and service discovery</li>
 *   <li>Shared-everything dynamic linking for efficient resource usage</li>
 * </ul>
 *
 * @since 1.0.0
 */
public interface ComponentLinker {

  /**
   * Links multiple components into a composite component with dynamic resolution.
   *
   * <p>This method performs advanced component linking using the WebAssembly Component Model's
   * shared-everything dynamic linking capabilities, allowing common modules to be shared
   * between components for efficient resource usage.
   *
   * @param components the components to link together
   * @param linkingConfig configuration for the linking operation
   * @return the linked composite component
   * @throws WasmException if linking fails due to incompatible interfaces or other issues
   */
  ComponentSimple linkComponents(List<ComponentSimple> components, ComponentLinkingConfig linkingConfig)
      throws WasmException;

  /**
   * Performs hot-swapping of a component in a running application.
   *
   * <p>Hot-swapping allows replacing a component with a new version without stopping
   * the application. The linker ensures that all interface contracts are maintained
   * and that the swap happens atomically.
   *
   * @param oldComponent the component to replace
   * @param newComponent the replacement component
   * @param swapConfig configuration for the hot-swap operation
   * @return future that completes when the swap is finished
   * @throws WasmException if the swap cannot be performed safely
   */
  CompletableFuture<ComponentSwapResult> hotSwapComponent(
      ComponentSimple oldComponent,
      ComponentSimple newComponent,
      ComponentSwapConfig swapConfig) throws WasmException;

  /**
   * Loads a component dynamically with conditional loading based on capabilities.
   *
   * <p>Dynamic loading allows components to be loaded at runtime based on available
   * capabilities, system resources, or other runtime conditions.
   *
   * @param componentSpec specification of the component to load
   * @param loadConditions conditions that must be met for loading
   * @return the loaded component, or empty if conditions are not met
   * @throws WasmException if loading fails
   */
  Optional<ComponentSimple> loadComponentDynamic(
      ComponentSpecification componentSpec,
      ComponentLoadConditions loadConditions) throws WasmException;

  /**
   * Performs comprehensive compatibility checking between components.
   *
   * <p>This includes interface compatibility, version compatibility, resource requirements,
   * and security policy compatibility.
   *
   * @param source the source component
   * @param target the target component
   * @return detailed compatibility analysis result
   */
  ComponentCompatibilityResult checkCompatibility(ComponentSimple source, ComponentSimple target);

  /**
   * Injects dependencies into a component using service discovery.
   *
   * <p>Dependency injection resolves component imports by finding compatible components
   * in the registry or loading them dynamically if needed.
   *
   * @param component the component needing dependency injection
   * @param injectionConfig configuration for dependency injection
   * @return the component with resolved dependencies
   * @throws WasmException if dependency injection fails
   */
  ComponentSimple injectDependencies(ComponentSimple component, DependencyInjectionConfig injectionConfig)
      throws WasmException;

  /**
   * Creates a component pipeline for data flow composition.
   *
   * <p>Pipelines allow components to be chained together for data processing,
   * with type-safe interface matching between pipeline stages.
   *
   * @param pipeline the pipeline specification
   * @return the composite pipeline component
   * @throws WasmException if pipeline creation fails
   */
  ComponentPipeline createPipeline(ComponentPipelineSpec pipeline) throws WasmException;

  /**
   * Sets up event-driven communication between components.
   *
   * <p>Event-driven communication allows components to communicate asynchronously
   * through publish-subscribe patterns or direct event routing.
   *
   * @param eventConfig event communication configuration
   * @return the event communication system
   * @throws WasmException if event setup fails
   */
  ComponentEventSystem setupEventCommunication(ComponentEventConfig eventConfig) throws WasmException;

  /**
   * Validates a linking configuration before execution.
   *
   * <p>Pre-validation helps catch linking errors early and provides detailed
   * diagnostic information about potential issues.
   *
   * @param linkingConfig the linking configuration to validate
   * @return validation result with any issues found
   */
  LinkingValidationResult validateLinking(ComponentLinkingConfig linkingConfig);

  /**
   * Gets the component registry associated with this linker.
   *
   * @return the component registry
   */
  ComponentRegistry getRegistry();

  /**
   * Sets the component registry for this linker.
   *
   * @param registry the component registry to use
   */
  void setRegistry(ComponentRegistry registry);

  /**
   * Gets linking statistics and metrics.
   *
   * @return current linking statistics
   */
  ComponentLinkingStatistics getStatistics();

  /**
   * Cleans up unused linked components and resources.
   *
   * @return number of components cleaned up
   * @throws WasmException if cleanup fails
   */
  int cleanup() throws WasmException;

  /**
   * Gets information about active component links.
   *
   * @return list of active component links
   */
  List<ComponentLinkInfo> getActiveLinks();

  /**
   * Shuts down the linker and releases all resources.
   */
  void shutdown();
}