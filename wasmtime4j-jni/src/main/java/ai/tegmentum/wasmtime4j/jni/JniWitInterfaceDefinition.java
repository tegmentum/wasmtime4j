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
package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.wit.AbstractWitInterfaceDefinition;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * JNI implementation of WIT interface definition.
 *
 * <p>This class provides a concrete implementation of the WitInterfaceDefinition interface for use
 * with JNI-based component operations.
 *
 * <p>Function and type names are derived synthetically from export metadata by appending {@code
 * -func} and {@code -type} suffixes respectively. These are approximations, not actual WIT
 * introspection data, because wasmtime's C/Rust API does not expose WIT-level metadata directly.
 * The synthetic names are suitable for informational display but should not be used for dispatch or
 * name-based matching.
 *
 * @since 1.0.0
 */
public final class JniWitInterfaceDefinition extends AbstractWitInterfaceDefinition {

  /**
   * Creates a new JNI WIT interface definition.
   *
   * @param name the interface name
   * @param version the interface version
   * @param packageName the package name
   * @param exportNames the list of export names
   * @param importNames the list of import names
   */
  public JniWitInterfaceDefinition(
      final String name,
      final String version,
      final String packageName,
      final Set<String> exportNames,
      final Set<String> importNames) {
    super(
        name,
        version,
        packageName,
        buildFunctionNames(exportNames),
        buildTypeNames(exportNames),
        importNames != null ? importNames : Collections.emptySet(),
        new ArrayList<>(importNames != null ? importNames : Collections.emptySet()),
        new ArrayList<>(exportNames != null ? exportNames : Collections.emptySet()));
  }

  private static List<String> buildFunctionNames(final Set<String> exportNames) {
    if (exportNames == null || exportNames.isEmpty()) {
      return Collections.emptyList();
    }
    final List<String> names = new ArrayList<>();
    for (final String export : exportNames) {
      names.add(export + "-func");
    }
    return names;
  }

  private static List<String> buildTypeNames(final Set<String> exportNames) {
    if (exportNames == null || exportNames.isEmpty()) {
      return Collections.emptyList();
    }
    final List<String> names = new ArrayList<>();
    for (final String export : exportNames) {
      names.add(export + "-type");
    }
    return names;
  }

  @Override
  public String getWitText() {
    final StringBuilder wit = new StringBuilder();
    wit.append("interface ").append(getName()).append(" {\n");

    for (final String func : getFunctionNames()) {
      wit.append("  ").append(func).append("() -> ();\n");
    }

    for (final String type : getTypeNames()) {
      wit.append("  type ").append(type).append(" = string;\n");
    }

    wit.append("}\n");
    return wit.toString();
  }
}
