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
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.stream.Collectors;

/**
 * Basic implementation of ComponentRegistry for foundational Component Model support.
 *
 * <p>This implementation provides core component registry functionality as part of
 * Task #304 to stabilize the Component Model foundation. It focuses on essential
 * component management operations while maintaining compatibility with the advanced
 * ComponentRegistry interface.
 *
 * @since 1.0.0
 */
public final class BasicComponentRegistry implements ComponentRegistry {

  private static final Logger LOGGER = Logger.getLogger(BasicComponentRegistry.class.getName());

  private final Map<String, ComponentSimple> componentsById;
  private final Map<String, ComponentSimple> componentsByName;
  private final Map<String, String> nameToIdMapping;

  /**
   * Creates a new basic component registry.
   */
  public BasicComponentRegistry() {
    this.componentsById = new ConcurrentHashMap<>();
    this.componentsByName = new ConcurrentHashMap<>();
    this.nameToIdMapping = new ConcurrentHashMap<>();
  }

  @Override
  public void register(final ComponentSimple component) throws WasmException {
    Objects.requireNonNull(component, "component");

    final String componentId = component.getId();
    if (componentId == null || componentId.trim().isEmpty()) {
      throw new WasmException("Component ID cannot be null or empty");
    }

    try {
      if (componentsById.containsKey(componentId)) {
        throw new WasmException("Component already registered with ID: " + componentId);
      }

      componentsById.put(componentId, component);
      LOGGER.fine("Registered component with ID: " + componentId);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to register component", e);
    }
  }

  @Override
  public void register(final String name, final ComponentSimple component) throws WasmException {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(component, "component");

    if (name.trim().isEmpty()) {
      throw new IllegalArgumentException("Component name cannot be empty");
    }

    final String componentId = component.getId();
    if (componentId == null || componentId.trim().isEmpty()) {
      throw new WasmException("Component ID cannot be null or empty");
    }

    try {
      if (componentsByName.containsKey(name)) {
        throw new WasmException("Component already registered with name: " + name);
      }

      if (componentsById.containsKey(componentId)) {
        throw new WasmException("Component already registered with ID: " + componentId);
      }

      componentsById.put(componentId, component);
      componentsByName.put(name, component);
      nameToIdMapping.put(name, componentId);

      LOGGER.fine("Registered component with name: " + name + " and ID: " + componentId);

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to register component with name: " + name, e);
    }
  }

  @Override
  public void unregister(final String componentId) throws WasmException {
    Objects.requireNonNull(componentId, "componentId");

    try {
      final ComponentSimple component = componentsById.remove(componentId);
      if (component == null) {
        LOGGER.fine("Component not found for unregistration: " + componentId);
        return;
      }

      // Remove from name mapping if present
      nameToIdMapping.entrySet().removeIf(entry -> entry.getValue().equals(componentId));
      componentsByName.entrySet().removeIf(entry -> entry.getValue().getId().equals(componentId));

      LOGGER.fine("Unregistered component: " + componentId);

    } catch (final Exception e) {
      throw new WasmException("Failed to unregister component: " + componentId, e);
    }
  }

  @Override
  public Optional<ComponentSimple> findById(final String componentId) {
    if (componentId == null || componentId.trim().isEmpty()) {
      return Optional.empty();
    }

    return Optional.ofNullable(componentsById.get(componentId));
  }

  @Override
  public Optional<ComponentSimple> findByName(final String name) {
    if (name == null || name.trim().isEmpty()) {
      return Optional.empty();
    }

    return Optional.ofNullable(componentsByName.get(name));
  }

  @Override
  public Optional<ComponentSimple> findComponent(final String name, final ComponentVersion version) {
    if (name == null || name.trim().isEmpty() || version == null) {
      return Optional.empty();
    }

    // First try to find by exact name
    final Optional<ComponentSimple> namedComponent = findByName(name);
    if (namedComponent.isPresent() && namedComponent.get().getVersion().equals(version)) {
      return namedComponent;
    }

    // Search through all components for name and version match
    return componentsById.values().stream()
        .filter(component -> {
          try {
            final String componentName = component.getMetadata().getName();
            return name.equals(componentName) && version.equals(component.getVersion());
          } catch (Exception e) {
            return false;
          }
        })
        .findFirst();
  }

  @Override
  public List<ComponentSimple> findByVersion(final ComponentVersion version) {
    if (version == null) {
      return Collections.emptyList();
    }

    return componentsById.values().stream()
        .filter(component -> version.equals(component.getVersion()))
        .collect(Collectors.toList());
  }

  @Override
  public List<ComponentSimple> findByInterface(final String interfaceName) {
    if (interfaceName == null || interfaceName.trim().isEmpty()) {
      return Collections.emptyList();
    }

    return componentsById.values().stream()
        .filter(component -> {
          try {
            return component.exportsInterface(interfaceName) || component.importsInterface(interfaceName);
          } catch (Exception e) {
            return false;
          }
        })
        .collect(Collectors.toList());
  }

  @Override
  public Set<ComponentSimple> getAllComponents() {
    return Collections.unmodifiableSet(new HashSet<>(componentsById.values()));
  }

  @Override
  public Set<String> getAllComponentIds() {
    return Collections.unmodifiableSet(new HashSet<>(componentsById.keySet()));
  }

  @Override
  public boolean isRegistered(final String componentId) {
    return componentId != null && componentsById.containsKey(componentId);
  }

  @Override
  public int getComponentCount() {
    return componentsById.size();
  }

  @Override
  public Set<ComponentSimple> resolveDependencies(final ComponentSimple component) throws WasmException {
    Objects.requireNonNull(component, "component");

    try {
      final Set<ComponentSimple> dependencies = new HashSet<>();

      // Get component's imported interfaces
      final Set<String> importedInterfaces = component.getImportedInterfaces();

      // Find components that export these interfaces
      for (final String interfaceName : importedInterfaces) {
        final List<ComponentSimple> providers = findByInterface(interfaceName);

        // Add the first compatible provider found
        for (final ComponentSimple provider : providers) {
          if (!provider.getId().equals(component.getId()) && provider.exportsInterface(interfaceName)) {
            dependencies.add(provider);
            break; // Only add the first compatible provider
          }
        }
      }

      return dependencies;

    } catch (final Exception e) {
      throw new WasmException("Failed to resolve dependencies for component: " + component.getId(), e);
    }
  }

  @Override
  public ComponentRegistryStatistics getStatistics() {
    final int totalComponents = componentsById.size();
    final int namedComponents = componentsByName.size();
    final Map<ComponentVersion, Integer> versionCounts = new HashMap<>();

    // Count components by version
    for (final ComponentSimple component : componentsById.values()) {
      final ComponentVersion version = component.getVersion();
      versionCounts.put(version, versionCounts.getOrDefault(version, 0) + 1);
    }

    return new ComponentRegistryStatistics(
        totalComponents,
        namedComponents,
        versionCounts.size(),
        versionCounts
    );
  }

  @Override
  public void validateRegistry() throws WasmException {
    try {
      final List<String> issues = new ArrayList<>();

      // Check for consistency between ID and name mappings
      for (final Map.Entry<String, String> entry : nameToIdMapping.entrySet()) {
        final String name = entry.getKey();
        final String expectedId = entry.getValue();

        final ComponentSimple namedComponent = componentsByName.get(name);
        final ComponentSimple idComponent = componentsById.get(expectedId);

        if (namedComponent == null) {
          issues.add("Name mapping exists but component not found by name: " + name);
        }

        if (idComponent == null) {
          issues.add("Name mapping exists but component not found by ID: " + expectedId);
        }

        if (namedComponent != null && idComponent != null && namedComponent != idComponent) {
          issues.add("Name mapping inconsistency for name: " + name);
        }
      }

      // Check for invalid components
      for (final Map.Entry<String, ComponentSimple> entry : componentsById.entrySet()) {
        final String id = entry.getKey();
        final ComponentSimple component = entry.getValue();

        if (!component.isValid()) {
          issues.add("Invalid component registered with ID: " + id);
        }
      }

      if (!issues.isEmpty()) {
        throw new WasmException("Registry validation failed: " + String.join("; ", issues));
      }

      LOGGER.fine("Registry validation passed");

    } catch (final Exception e) {
      if (e instanceof WasmException) {
        throw e;
      }
      throw new WasmException("Failed to validate registry", e);
    }
  }

  @Override
  public void clear() {
    try {
      // Close all components before clearing
      for (final ComponentSimple component : componentsById.values()) {
        try {
          component.close();
        } catch (Exception e) {
          LOGGER.warning("Error closing component during clear: " + e.getMessage());
        }
      }

      componentsById.clear();
      componentsByName.clear();
      nameToIdMapping.clear();

      LOGGER.fine("Registry cleared successfully");

    } catch (final Exception e) {
      LOGGER.warning("Error during registry clear: " + e.getMessage());
    }
  }

  @Override
  public void close() {
    clear();
    LOGGER.fine("Component registry closed");
  }

  @Override
  public String toString() {
    return "BasicComponentRegistry{" +
        "componentCount=" + componentsById.size() +
        ", namedComponentCount=" + componentsByName.size() +
        '}';
  }
}