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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.wit.AbstractWitInterfaceDefinition;

/**
 * Panama implementation of WIT interface definition.
 *
 * @since 1.0.0
 */
final class PanamaWitInterfaceDefinition extends AbstractWitInterfaceDefinition {

  private final String witText;

  /**
   * Creates a new PanamaWitInterfaceDefinition.
   *
   * @param name the interface name
   * @param version the interface version
   * @param packageName the package name
   * @param witText the raw WIT definition text
   */
  PanamaWitInterfaceDefinition(
      final String name, final String version, final String packageName, final String witText) {
    super(name, version, packageName, null, null, null, null, null);
    this.witText = witText != null ? witText : "";

    if (witText != null && !witText.isEmpty()) {
      parseWitText(witText);
    }
  }

  private void parseWitText(final String text) {
    for (final String line : text.split("\n")) {
      final String trimmed = line.trim();

      if (trimmed.contains(": func(") || trimmed.matches("^[a-z][a-z0-9-]*\\s*:\\s*func.*")) {
        final int colonIdx = trimmed.indexOf(':');
        if (colonIdx > 0) {
          mutableFunctionNames().add(trimmed.substring(0, colonIdx).trim());
        }
      }

      if (trimmed.startsWith("type ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          mutableTypeNames().add(parts[1]);
        }
      }

      if (trimmed.startsWith("record ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          mutableTypeNames().add(parts[1].replace("{", "").trim());
        }
      }

      if (trimmed.startsWith("enum ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          mutableTypeNames().add(parts[1].replace("{", "").trim());
        }
      }

      if (trimmed.startsWith("variant ")) {
        final String[] parts = trimmed.split("\\s+");
        if (parts.length >= 2) {
          mutableTypeNames().add(parts[1].replace("{", "").trim());
        }
      }

      if (trimmed.startsWith("use ")) {
        mutableDependencies().add(trimmed.substring(4).replace(";", "").trim());
      }

      if (trimmed.startsWith("import ")) {
        mutableImportNames().add(trimmed.substring(7).replace(";", "").trim());
      }

      if (trimmed.startsWith("export ")) {
        mutableExportNames().add(trimmed.substring(7).replace(";", "").trim());
      }
    }
  }

  @Override
  public String getWitText() {
    return witText;
  }

  void addFunction(final String functionName) {
    if (functionName != null && !functionName.isEmpty()) {
      mutableFunctionNames().add(functionName);
    }
  }

  void addType(final String typeName) {
    if (typeName != null && !typeName.isEmpty()) {
      mutableTypeNames().add(typeName);
    }
  }

  void addDependency(final String dependency) {
    if (dependency != null && !dependency.isEmpty()) {
      mutableDependencies().add(dependency);
    }
  }
}
