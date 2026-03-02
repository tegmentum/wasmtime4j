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
package ai.tegmentum.wasmtime4j.component;

/**
 * Represents an export discovered on a component instance via {@link
 * ComponentInstance#getExport(String)}.
 *
 * <p>Contains the export's kind (function, module, resource, etc.) and a pre-computed {@link
 * ComponentExportIndex} for efficient subsequent lookups.
 *
 * @since 1.1.0
 */
public final class ComponentExportItem implements AutoCloseable {

  private final ComponentItemKind kind;
  private final ComponentExportIndex exportIndex;

  /**
   * Creates a new component export item.
   *
   * @param kind the kind of the export
   * @param exportIndex the pre-computed export index for efficient lookups
   * @throws IllegalArgumentException if kind or exportIndex is null
   */
  public ComponentExportItem(final ComponentItemKind kind, final ComponentExportIndex exportIndex) {
    if (kind == null) {
      throw new IllegalArgumentException("kind cannot be null");
    }
    if (exportIndex == null) {
      throw new IllegalArgumentException("exportIndex cannot be null");
    }
    this.kind = kind;
    this.exportIndex = exportIndex;
  }

  /**
   * Gets the kind of this export (function, module, resource, etc.).
   *
   * @return the component item kind
   */
  public ComponentItemKind getKind() {
    return kind;
  }

  /**
   * Gets the pre-computed export index for efficient subsequent lookups.
   *
   * <p>This index can be passed to methods like {@link
   * ComponentInstance#getFunc(ComponentExportIndex)} for O(1) lookups.
   *
   * @return the export index
   */
  public ComponentExportIndex getExportIndex() {
    return exportIndex;
  }

  /**
   * Converts a native kind code to a {@link ComponentItemKind}.
   *
   * @param code the native kind code (0-6)
   * @return the corresponding ComponentItemKind
   * @throws IllegalArgumentException if the code is not recognized
   */
  public static ComponentItemKind kindFromCode(final int code) {
    switch (code) {
      case 0:
        return ComponentItemKind.COMPONENT_FUNC;
      case 1:
        return ComponentItemKind.CORE_FUNC;
      case 2:
        return ComponentItemKind.MODULE;
      case 3:
        return ComponentItemKind.COMPONENT;
      case 4:
        return ComponentItemKind.COMPONENT_INSTANCE;
      case 5:
        return ComponentItemKind.TYPE;
      case 6:
        return ComponentItemKind.RESOURCE;
      default:
        throw new IllegalArgumentException("Unknown component export kind code: " + code);
    }
  }

  @Override
  public void close() {
    exportIndex.close();
  }

  @Override
  public String toString() {
    return "ComponentExportItem{kind=" + kind + "}";
  }
}
