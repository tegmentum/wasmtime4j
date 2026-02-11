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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Specification for a WebAssembly component.
 *
 * <p>This class defines the structure and requirements of a component including its imports,
 * exports, and dependencies.
 *
 * @since 1.0.0
 */
public final class ComponentSpecification {

  private final String componentName;
  private final String version;
  private final List<String> imports;
  private final List<String> exports;
  private final Map<String, String> metadata;

  /**
   * Creates a new component specification.
   *
   * @param componentName the component name
   * @param version the component version
   * @param imports the list of required imports
   * @param exports the list of provided exports
   * @param metadata additional component metadata
   */
  public ComponentSpecification(
      final String componentName,
      final String version,
      final List<String> imports,
      final List<String> exports,
      final Map<String, String> metadata) {
    this.componentName = Objects.requireNonNull(componentName, "componentName cannot be null");
    this.version = Objects.requireNonNull(version, "version cannot be null");
    this.imports = List.copyOf(Objects.requireNonNull(imports, "imports cannot be null"));
    this.exports = List.copyOf(Objects.requireNonNull(exports, "exports cannot be null"));
    this.metadata = Map.copyOf(Objects.requireNonNull(metadata, "metadata cannot be null"));
  }

  /**
   * Gets the component name.
   *
   * @return component name
   */
  public String getComponentName() {
    return componentName;
  }

  /**
   * Gets the component version.
   *
   * @return component version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the list of required imports.
   *
   * @return immutable list of imports
   */
  public List<String> getImports() {
    return imports;
  }

  /**
   * Gets the list of provided exports.
   *
   * @return immutable list of exports
   */
  public List<String> getExports() {
    return exports;
  }

  /**
   * Gets the component metadata.
   *
   * @return immutable map of metadata
   */
  public Map<String, String> getMetadata() {
    return metadata;
  }
}
