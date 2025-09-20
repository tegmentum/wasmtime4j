package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model interface types.
 *
 * <p>InterfaceType represents a WIT (WebAssembly Interface Types) interface definition that
 * specifies a collection of functions, resources, and types. Interfaces enable structured
 * composition and type-safe interaction between components.
 *
 * <p>Interface types follow the WIT specification and provide the foundation for component model
 * composition, import/export resolution, and inter-component communication.
 *
 * <p>Example interface definition:
 *
 * <pre>{@code
 * interface filesystem {
 *   resource file {
 *     read: func() -> list<u8>
 *     write: func(data: list<u8>)
 *   }
 *
 *   open: func(path: string) -> file
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface InterfaceType {

  /**
   * Gets the name of this interface.
   *
   * @return the interface name
   */
  String getName();

  /**
   * Gets the version of this interface if specified.
   *
   * @return the interface version, or empty if not specified
   */
  Optional<String> getVersion();

  /**
   * Gets the documentation for this interface.
   *
   * @return interface documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets all function definitions in this interface.
   *
   * <p>Returns functions that are directly defined in this interface, including both standalone
   * functions and resource methods.
   *
   * @return list of interface functions
   */
  List<InterfaceFunction> getFunctions();

  /**
   * Gets a specific function by name.
   *
   * @param name the function name to look up
   * @return the interface function, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<InterfaceFunction> getFunction(final String name);

  /**
   * Gets all resource definitions in this interface.
   *
   * <p>Returns resource types that are defined within this interface, including their methods and
   * associated functionality.
   *
   * @return list of interface resources
   */
  List<InterfaceResource> getResources();

  /**
   * Gets a specific resource by name.
   *
   * @param name the resource name to look up
   * @return the interface resource, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<InterfaceResource> getResource(final String name);

  /**
   * Gets all type definitions in this interface.
   *
   * <p>Returns custom types (records, variants, enums, etc.) that are defined within this interface
   * and can be used in function signatures.
   *
   * @return map of type names to their definitions
   */
  Map<String, ComponentValueType> getTypes();

  /**
   * Gets a specific type definition by name.
   *
   * @param name the type name to look up
   * @return the type definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentValueType> getType(final String name);

  /**
   * Gets all constants defined in this interface.
   *
   * @return map of constant names to their values
   */
  Map<String, Object> getConstants();

  /**
   * Gets a specific constant by name.
   *
   * @param name the constant name to look up
   * @return the constant value, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<Object> getConstant(final String name);

  /**
   * Gets the names of all entities in this interface.
   *
   * <p>Returns names of functions, resources, types, and constants defined in this interface.
   *
   * @return list of all entity names
   */
  List<String> getEntityNames();

  /**
   * Checks if this interface depends on another interface.
   *
   * <p>Returns true if this interface imports or references types from the specified interface.
   *
   * @param interfaceName the interface name to check dependency for
   * @return true if this interface depends on the specified interface
   * @throws IllegalArgumentException if interfaceName is null or empty
   */
  boolean dependsOn(final String interfaceName);

  /**
   * Gets all interface dependencies.
   *
   * <p>Returns the names of all interfaces that this interface depends on through imports or type
   * references.
   *
   * @return list of interface names this interface depends on
   */
  List<String> getDependencies();

  /**
   * Checks if this interface is compatible with another interface.
   *
   * <p>Compatibility checking considers function signatures, resource definitions, type
   * compatibility, and semantic versioning if available.
   *
   * @param other the interface to check compatibility with
   * @return true if interfaces are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final InterfaceType other);

  /**
   * Validates this interface for completeness and consistency.
   *
   * <p>Validation includes type reference checking, function signature validation, resource
   * definition consistency, and dependency resolution.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Gets metadata about this interface.
   *
   * <p>Returns information about complexity, performance characteristics, and tool-specific
   * annotations.
   *
   * @return interface metadata
   */
  InterfaceMetadata getMetadata();
}
