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

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Describes a single item in a component's imports or exports.
 *
 * <p>This interface corresponds to Wasmtime's {@code ComponentItem} enum and provides type-safe
 * access to the different kinds of items a component can import or export.
 *
 * <p>Use {@link #kind()} to determine the item type, then cast or use the appropriate accessor
 * methods. Alternatively, use {@code instanceof} checks against the specific record types.
 *
 * @since 1.1.0
 */
public interface ComponentItemInfo {

  /**
   * Gets the kind of this component item.
   *
   * @return the item kind
   */
  ComponentItemKind kind();

  /**
   * A component model function with typed parameters and results.
   *
   * @param params the function parameters as name-type pairs
   * @param results the function result types
   * @param isAsync whether the function is async
   */
  record ComponentFuncInfo(
      List<NamedType> params, List<ComponentTypeDescriptor> results, boolean isAsync)
      implements ComponentItemInfo {
    /** Creates a ComponentFuncInfo with defensive copies. */
    public ComponentFuncInfo {
      params = List.copyOf(params);
      results = List.copyOf(results);
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.COMPONENT_FUNC;
    }
  }

  /**
   * A core WebAssembly function with core value types.
   *
   * @param params the core parameter types as strings (e.g., "i32", "i64")
   * @param results the core result types as strings
   */
  record CoreFuncInfo(List<String> params, List<String> results) implements ComponentItemInfo {
    /** Creates a CoreFuncInfo with defensive copies. */
    public CoreFuncInfo {
      params = List.copyOf(params);
      results = List.copyOf(results);
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.CORE_FUNC;
    }
  }

  /**
   * A WebAssembly core module.
   *
   * @param name the module name (may be null)
   */
  record ModuleInfo(String name) implements ComponentItemInfo {
    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.MODULE;
    }
  }

  /**
   * A nested component with its own imports and exports.
   *
   * @param imports the nested component's imports
   * @param exports the nested component's exports
   */
  record ComponentInfo(
      Map<String, ComponentItemInfo> imports, Map<String, ComponentItemInfo> exports)
      implements ComponentItemInfo {
    /** Creates a ComponentInfo with defensive copies. */
    public ComponentInfo {
      imports = Map.copyOf(imports);
      exports = Map.copyOf(exports);
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.COMPONENT;
    }
  }

  /**
   * A component instance (a collection of named exports).
   *
   * @param exports the instance's exports
   */
  record ComponentInstanceInfo(Map<String, ComponentItemInfo> exports)
      implements ComponentItemInfo {
    /** Creates a ComponentInstanceInfo with defensive copies. */
    public ComponentInstanceInfo {
      exports = Map.copyOf(exports);
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.COMPONENT_INSTANCE;
    }
  }

  /**
   * A type definition.
   *
   * @param descriptor the type descriptor
   */
  record TypeInfo(ComponentTypeDescriptor descriptor) implements ComponentItemInfo {
    /** Creates a TypeInfo. */
    public TypeInfo {
      Objects.requireNonNull(descriptor, "descriptor cannot be null");
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.TYPE;
    }
  }

  /**
   * A resource type.
   *
   * @param name the resource name (may be null)
   * @param resourceTypeId a unique identifier for this resource type
   */
  record ResourceInfo(String name, long resourceTypeId) implements ComponentItemInfo {
    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.RESOURCE;
    }
  }

  /**
   * A named type, used for function parameters.
   *
   * @param name the parameter name
   * @param type the parameter type descriptor
   */
  record NamedType(String name, ComponentTypeDescriptor type) {
    /** Creates a NamedType. */
    public NamedType {
      Objects.requireNonNull(name, "name cannot be null");
      Objects.requireNonNull(type, "type cannot be null");
    }
  }
}
