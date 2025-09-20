package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model component type information.
 *
 * <p>ComponentType provides comprehensive type information about a component including its
 * imports, exports, and interface definitions. This information is essential for validation,
 * tooling, and dynamic interaction with components.
 *
 * <p>Component types are immutable and reflect the component definition at compile time.
 *
 * @since 1.0.0
 */
public interface ComponentType {

  /**
   * Gets the name of the component type if specified.
   *
   * @return the component type name, or empty if not specified
   */
  Optional<String> getName();

  /**
   * Gets all import declarations for this component.
   *
   * <p>Returns a list of all interfaces, components, functions, and resources that this component
   * requires to be provided during instantiation.
   *
   * @return list of component imports
   */
  List<ComponentImport> getImports();

  /**
   * Gets a specific import by name.
   *
   * @param name the import name to look up
   * @return the component import, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentImport> getImport(final String name);

  /**
   * Gets all export declarations for this component.
   *
   * <p>Returns a list of all interfaces, functions, and resources that this component provides
   * after instantiation.
   *
   * @return list of component exports
   */
  List<ComponentExportDeclaration> getExports();

  /**
   * Gets a specific export declaration by name.
   *
   * @param name the export name to look up
   * @return the component export declaration, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentExportDeclaration> getExport(final String name);

  /**
   * Gets all interface definitions used by this component.
   *
   * <p>Returns interfaces that are either imported, exported, or used internally by the
   * component. This includes both component-defined and referenced external interfaces.
   *
   * @return map of interface names to their definitions
   */
  Map<String, InterfaceType> getInterfaces();

  /**
   * Gets a specific interface definition.
   *
   * @param name the interface name to look up
   * @return the interface definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<InterfaceType> getInterface(final String name);

  /**
   * Gets all resource types defined or used by this component.
   *
   * <p>Returns resource types that are imported, exported, or used internally within component
   * interfaces.
   *
   * @return map of resource type names to their definitions
   */
  Map<String, ComponentResourceType> getResourceTypes();

  /**
   * Gets a specific resource type definition.
   *
   * @param name the resource type name to look up
   * @return the resource type definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentResourceType> getResourceType(final String name);

  /**
   * Gets all custom value types defined by this component.
   *
   * <p>Returns record types, variant types, and other custom types defined within component
   * interfaces.
   *
   * @return map of type names to their definitions
   */
  Map<String, ComponentValueType> getCustomTypes();

  /**
   * Gets a specific custom value type definition.
   *
   * @param name the type name to look up
   * @return the value type definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentValueType> getCustomType(final String name);

  /**
   * Checks if this component type is compatible with another component type.
   *
   * <p>Compatibility checking considers interface definitions, import/export signatures, and
   * structural compatibility for composition.
   *
   * @param other the component type to check compatibility with
   * @return true if component types are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentType other);

  /**
   * Validates this component type for completeness and consistency.
   *
   * <p>Validation includes type reference checking, import/export consistency, and interface
   * definition validation.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Gets additional metadata about this component type.
   *
   * <p>Returns information such as complexity metrics, dependency analysis, and tool-specific
   * annotations.
   *
   * @return component type metadata
   */
  ComponentTypeMetadata getMetadata();
}