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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * Represents the type-level description of a WebAssembly component.
 *
 * <p>This class corresponds to Wasmtime's {@code types::Component} type and provides introspection
 * into a component's imports and exports. It is obtained via {@link Component#componentType()}.
 *
 * <p>A {@code ComponentTypeInfo} provides a frozen snapshot of the component's type information at
 * the time it was compiled. The imports and exports represent the component's interface contract.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * Component component = engine.compileComponent(wasmBytes);
 * ComponentTypeInfo typeInfo = component.componentType();
 *
 * // Iterate over imports and exports
 * for (String importName : typeInfo.imports()) {
 *     System.out.println("Requires: " + importName);
 * }
 * for (String exportName : typeInfo.exports()) {
 *     System.out.println("Provides: " + exportName);
 * }
 *
 * // Check specific imports/exports
 * if (typeInfo.hasExport("run")) {
 *     System.out.println("Component has a 'run' export");
 * }
 * }</pre>
 *
 * @since 1.1.0
 */
public final class ComponentTypeInfo {

  private final Set<String> imports;
  private final Set<String> exports;
  private final Map<String, ComponentItemInfo> importItems;
  private final Map<String, ComponentItemInfo> exportItems;

  /**
   * Creates a new ComponentTypeInfo with the given imports and exports (name-only).
   *
   * @param imports the set of import names
   * @param exports the set of export names
   * @throws IllegalArgumentException if imports or exports is null
   */
  public ComponentTypeInfo(final Set<String> imports, final Set<String> exports) {
    Objects.requireNonNull(imports, "imports cannot be null");
    Objects.requireNonNull(exports, "exports cannot be null");
    this.imports = Collections.unmodifiableSet(new LinkedHashSet<>(imports));
    this.exports = Collections.unmodifiableSet(new LinkedHashSet<>(exports));
    this.importItems = Collections.emptyMap();
    this.exportItems = Collections.emptyMap();
  }

  /**
   * Creates a new ComponentTypeInfo with full type information for imports and exports.
   *
   * <p>The import and export names are derived from the map keys. The typed maps provide full
   * {@link ComponentItemInfo} for each item, enabling deep type introspection.
   *
   * @param importItems the typed import items keyed by name
   * @param exportItems the typed export items keyed by name
   * @throws IllegalArgumentException if importItems or exportItems is null
   */
  public ComponentTypeInfo(
      final Map<String, ComponentItemInfo> importItems,
      final Map<String, ComponentItemInfo> exportItems) {
    Objects.requireNonNull(importItems, "importItems cannot be null");
    Objects.requireNonNull(exportItems, "exportItems cannot be null");
    this.importItems = Collections.unmodifiableMap(new LinkedHashMap<>(importItems));
    this.exportItems = Collections.unmodifiableMap(new LinkedHashMap<>(exportItems));
    this.imports = Collections.unmodifiableSet(new LinkedHashSet<>(importItems.keySet()));
    this.exports = Collections.unmodifiableSet(new LinkedHashSet<>(exportItems.keySet()));
  }

  /**
   * Gets all import names for this component type.
   *
   * <p>Each name represents a component import that must be satisfied at instantiation time. Import
   * names follow WIT identifier conventions (e.g., "wasi:cli/stdout@0.2.0").
   *
   * @return an unmodifiable set of import names
   */
  public Set<String> imports() {
    return imports;
  }

  /**
   * Gets all export names for this component type.
   *
   * <p>Each name represents a component export that is available after instantiation. Export names
   * follow WIT identifier conventions.
   *
   * @return an unmodifiable set of export names
   */
  public Set<String> exports() {
    return exports;
  }

  /**
   * Checks if this component type has a specific import.
   *
   * @param name the import name to check
   * @return true if the import exists, false otherwise
   * @throws IllegalArgumentException if name is null
   */
  public boolean hasImport(final String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return imports.contains(name);
  }

  /**
   * Checks if this component type has a specific export.
   *
   * @param name the export name to check
   * @return true if the export exists, false otherwise
   * @throws IllegalArgumentException if name is null
   */
  public boolean hasExport(final String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return exports.contains(name);
  }

  /**
   * Looks up a specific import by name.
   *
   * @param name the import name to look up
   * @return an Optional containing the import name if found, or empty
   * @throws IllegalArgumentException if name is null
   */
  public Optional<String> getImport(final String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return imports.contains(name) ? Optional.of(name) : Optional.empty();
  }

  /**
   * Looks up a specific export by name.
   *
   * @param name the export name to look up
   * @return an Optional containing the export name if found, or empty
   * @throws IllegalArgumentException if name is null
   */
  public Optional<String> getExport(final String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return exports.contains(name) ? Optional.of(name) : Optional.empty();
  }

  /**
   * Gets the typed import items for this component type.
   *
   * <p>Each entry maps an import name to its full {@link ComponentItemInfo} type descriptor. This
   * map is empty if this instance was created with the name-only constructor.
   *
   * @return an unmodifiable map of import names to their type descriptors
   */
  public Map<String, ComponentItemInfo> importItems() {
    return importItems;
  }

  /**
   * Gets the typed export items for this component type.
   *
   * <p>Each entry maps an export name to its full {@link ComponentItemInfo} type descriptor. This
   * map is empty if this instance was created with the name-only constructor.
   *
   * @return an unmodifiable map of export names to their type descriptors
   */
  public Map<String, ComponentItemInfo> exportItems() {
    return exportItems;
  }

  /**
   * Gets the typed descriptor for a specific import.
   *
   * @param name the import name to look up
   * @return an Optional containing the import's type descriptor, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  public Optional<ComponentItemInfo> getImportItem(final String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return Optional.ofNullable(importItems.get(name));
  }

  /**
   * Gets the typed descriptor for a specific export.
   *
   * @param name the export name to look up
   * @return an Optional containing the export's type descriptor, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  public Optional<ComponentItemInfo> getExportItem(final String name) {
    Objects.requireNonNull(name, "name cannot be null");
    return Optional.ofNullable(exportItems.get(name));
  }

  @Override
  public String toString() {
    return "ComponentTypeInfo{imports=" + imports.size() + ", exports=" + exports.size() + "}";
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) {
      return true;
    }
    if (!(o instanceof ComponentTypeInfo)) {
      return false;
    }
    final ComponentTypeInfo that = (ComponentTypeInfo) o;
    return imports.equals(that.imports) && exports.equals(that.exports);
  }

  @Override
  public int hashCode() {
    return Objects.hash(imports, exports);
  }
}
