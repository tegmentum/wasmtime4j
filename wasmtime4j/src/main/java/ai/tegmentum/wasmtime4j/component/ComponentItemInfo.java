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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
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

  /** A component model function with typed parameters and results. */
  public static final class ComponentFuncInfo implements ComponentItemInfo {
    private final List<NamedType> params;
    private final List<ComponentTypeDescriptor> results;
    private final boolean isAsync;

    /**
     * Creates a ComponentFuncInfo with defensive copies.
     *
     * @param params the function parameters as name-type pairs
     * @param results the function result types
     * @param isAsync whether the function is async
     */
    public ComponentFuncInfo(
        List<NamedType> params, List<ComponentTypeDescriptor> results, boolean isAsync) {
      this.params = Collections.unmodifiableList(new ArrayList<>(params));
      this.results = Collections.unmodifiableList(new ArrayList<>(results));
      this.isAsync = isAsync;
    }

    /**
     * Gets the function parameters.
     *
     * @return the function parameters as name-type pairs
     */
    public List<NamedType> params() {
      return params;
    }

    /**
     * Gets the function result types.
     *
     * @return the function result types
     */
    public List<ComponentTypeDescriptor> results() {
      return results;
    }

    /**
     * Returns whether the function is async.
     *
     * @return whether the function is async
     */
    public boolean isAsync() {
      return isAsync;
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.COMPONENT_FUNC;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ComponentFuncInfo)) {
        return false;
      }
      ComponentFuncInfo that = (ComponentFuncInfo) o;
      return isAsync == that.isAsync
          && Objects.equals(params, that.params)
          && Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
      return Objects.hash(params, results, isAsync);
    }

    @Override
    public String toString() {
      return "ComponentFuncInfo["
          + "params="
          + params
          + ", results="
          + results
          + ", isAsync="
          + isAsync
          + ']';
    }
  }

  /** A core WebAssembly function with core value types. */
  public static final class CoreFuncInfo implements ComponentItemInfo {
    private final List<String> params;
    private final List<String> results;

    /**
     * Creates a CoreFuncInfo with defensive copies.
     *
     * @param params the core parameter types as strings (e.g., "i32", "i64")
     * @param results the core result types as strings
     */
    public CoreFuncInfo(List<String> params, List<String> results) {
      this.params = Collections.unmodifiableList(new ArrayList<>(params));
      this.results = Collections.unmodifiableList(new ArrayList<>(results));
    }

    /**
     * Gets the core parameter types.
     *
     * @return the core parameter types as strings (e.g., "i32", "i64")
     */
    public List<String> params() {
      return params;
    }

    /**
     * Gets the core result types.
     *
     * @return the core result types as strings
     */
    public List<String> results() {
      return results;
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.CORE_FUNC;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof CoreFuncInfo)) {
        return false;
      }
      CoreFuncInfo that = (CoreFuncInfo) o;
      return Objects.equals(params, that.params) && Objects.equals(results, that.results);
    }

    @Override
    public int hashCode() {
      return Objects.hash(params, results);
    }

    @Override
    public String toString() {
      return "CoreFuncInfo[" + "params=" + params + ", results=" + results + ']';
    }
  }

  /** A WebAssembly core module. */
  public static final class ModuleInfo implements ComponentItemInfo {
    private final String name;

    /**
     * Creates a ModuleInfo.
     *
     * @param name the module name (may be null)
     */
    public ModuleInfo(String name) {
      this.name = name;
    }

    /**
     * Gets the module name.
     *
     * @return the module name (may be null)
     */
    public String name() {
      return name;
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.MODULE;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ModuleInfo)) {
        return false;
      }
      ModuleInfo that = (ModuleInfo) o;
      return Objects.equals(name, that.name);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name);
    }

    @Override
    public String toString() {
      return "ModuleInfo[" + "name=" + name + ']';
    }
  }

  /** A nested component with its own imports and exports. */
  public static final class ComponentInfo implements ComponentItemInfo {
    private final Map<String, ComponentItemInfo> imports;
    private final Map<String, ComponentItemInfo> exports;

    /**
     * Creates a ComponentInfo with defensive copies.
     *
     * @param imports the nested component's imports
     * @param exports the nested component's exports
     */
    public ComponentInfo(
        Map<String, ComponentItemInfo> imports, Map<String, ComponentItemInfo> exports) {
      this.imports = Collections.unmodifiableMap(new HashMap<>(imports));
      this.exports = Collections.unmodifiableMap(new HashMap<>(exports));
    }

    /**
     * Gets the nested component's imports.
     *
     * @return the nested component's imports
     */
    public Map<String, ComponentItemInfo> imports() {
      return imports;
    }

    /**
     * Gets the nested component's exports.
     *
     * @return the nested component's exports
     */
    public Map<String, ComponentItemInfo> exports() {
      return exports;
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.COMPONENT;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ComponentInfo)) {
        return false;
      }
      ComponentInfo that = (ComponentInfo) o;
      return Objects.equals(imports, that.imports) && Objects.equals(exports, that.exports);
    }

    @Override
    public int hashCode() {
      return Objects.hash(imports, exports);
    }

    @Override
    public String toString() {
      return "ComponentInfo[" + "imports=" + imports + ", exports=" + exports + ']';
    }
  }

  /** A component instance (a collection of named exports). */
  public static final class ComponentInstanceInfo implements ComponentItemInfo {
    private final Map<String, ComponentItemInfo> exports;

    /**
     * Creates a ComponentInstanceInfo with defensive copies.
     *
     * @param exports the instance's exports
     */
    public ComponentInstanceInfo(Map<String, ComponentItemInfo> exports) {
      this.exports = Collections.unmodifiableMap(new HashMap<>(exports));
    }

    /**
     * Gets the instance's exports.
     *
     * @return the instance's exports
     */
    public Map<String, ComponentItemInfo> exports() {
      return exports;
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.COMPONENT_INSTANCE;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ComponentInstanceInfo)) {
        return false;
      }
      ComponentInstanceInfo that = (ComponentInstanceInfo) o;
      return Objects.equals(exports, that.exports);
    }

    @Override
    public int hashCode() {
      return Objects.hash(exports);
    }

    @Override
    public String toString() {
      return "ComponentInstanceInfo[" + "exports=" + exports + ']';
    }
  }

  /** A type definition. */
  public static final class TypeInfo implements ComponentItemInfo {
    private final ComponentTypeDescriptor descriptor;

    /**
     * Creates a TypeInfo.
     *
     * @param descriptor the type descriptor
     */
    public TypeInfo(ComponentTypeDescriptor descriptor) {
      Objects.requireNonNull(descriptor, "descriptor cannot be null");
      this.descriptor = descriptor;
    }

    /**
     * Gets the type descriptor.
     *
     * @return the type descriptor
     */
    public ComponentTypeDescriptor descriptor() {
      return descriptor;
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.TYPE;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof TypeInfo)) {
        return false;
      }
      TypeInfo that = (TypeInfo) o;
      return Objects.equals(descriptor, that.descriptor);
    }

    @Override
    public int hashCode() {
      return Objects.hash(descriptor);
    }

    @Override
    public String toString() {
      return "TypeInfo[" + "descriptor=" + descriptor + ']';
    }
  }

  /** A resource type. */
  public static final class ResourceInfo implements ComponentItemInfo {
    private final String name;
    private final long resourceTypeId;
    private final String debugIdentity;
    private final boolean hostDefined;

    /**
     * Creates a ResourceInfo.
     *
     * @param name the resource name (may be null)
     * @param resourceTypeId a unique identifier for this resource type
     * @param debugIdentity the Wasmtime debug identity string for diagnostic correlation (may be
     *     null)
     * @param hostDefined whether this resource type was defined by the host (as opposed to a guest
     *     component)
     */
    public ResourceInfo(
        String name, long resourceTypeId, String debugIdentity, boolean hostDefined) {
      this.name = name;
      this.resourceTypeId = resourceTypeId;
      this.debugIdentity = debugIdentity;
      this.hostDefined = hostDefined;
    }

    /**
     * Creates a ResourceInfo with only name and id (backward-compatible).
     *
     * @param name the resource name
     * @param resourceTypeId the resource type id
     */
    public ResourceInfo(String name, long resourceTypeId) {
      this(name, resourceTypeId, null, false);
    }

    /**
     * Gets the resource name.
     *
     * @return the resource name (may be null)
     */
    public String name() {
      return name;
    }

    /**
     * Gets the resource type identifier.
     *
     * @return a unique identifier for this resource type
     */
    public long resourceTypeId() {
      return resourceTypeId;
    }

    /**
     * Gets the debug identity string.
     *
     * @return the Wasmtime debug identity string for diagnostic correlation (may be null)
     */
    public String debugIdentity() {
      return debugIdentity;
    }

    /**
     * Returns whether this resource type was defined by the host.
     *
     * @return whether this resource type was defined by the host
     */
    public boolean hostDefined() {
      return hostDefined;
    }

    @Override
    public ComponentItemKind kind() {
      return ComponentItemKind.RESOURCE;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof ResourceInfo)) {
        return false;
      }
      ResourceInfo that = (ResourceInfo) o;
      return resourceTypeId == that.resourceTypeId
          && hostDefined == that.hostDefined
          && Objects.equals(name, that.name)
          && Objects.equals(debugIdentity, that.debugIdentity);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, resourceTypeId, debugIdentity, hostDefined);
    }

    @Override
    public String toString() {
      return "ResourceInfo["
          + "name="
          + name
          + ", resourceTypeId="
          + resourceTypeId
          + ", debugIdentity="
          + debugIdentity
          + ", hostDefined="
          + hostDefined
          + ']';
    }
  }

  /** A named type, used for function parameters. */
  public static final class NamedType {
    private final String name;
    private final ComponentTypeDescriptor type;

    /**
     * Creates a NamedType.
     *
     * @param name the parameter name
     * @param type the parameter type descriptor
     */
    public NamedType(String name, ComponentTypeDescriptor type) {
      Objects.requireNonNull(name, "name cannot be null");
      Objects.requireNonNull(type, "type cannot be null");
      this.name = name;
      this.type = type;
    }

    /**
     * Gets the parameter name.
     *
     * @return the parameter name
     */
    public String name() {
      return name;
    }

    /**
     * Gets the parameter type descriptor.
     *
     * @return the parameter type descriptor
     */
    public ComponentTypeDescriptor type() {
      return type;
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) {
        return true;
      }
      if (!(o instanceof NamedType)) {
        return false;
      }
      NamedType that = (NamedType) o;
      return Objects.equals(name, that.name) && Objects.equals(type, that.type);
    }

    @Override
    public int hashCode() {
      return Objects.hash(name, type);
    }

    @Override
    public String toString() {
      return "NamedType[" + "name=" + name + ", type=" + type + ']';
    }
  }
}
