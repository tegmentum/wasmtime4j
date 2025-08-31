package ai.tegmentum.wasmtime4j.wasi;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Metadata information about a WASI component interface.
 *
 * <p>Interface metadata provides comprehensive information about component interfaces including
 * function signatures, resource types, and type definitions. This information is essential for
 * dynamic interaction with components and tooling support.
 *
 * <p>Metadata is immutable and reflects the interface definition at the time of component loading.
 *
 * @since 1.0.0
 */
public interface WasiInterfaceMetadata {

  /**
   * Gets the name of the interface.
   *
   * @return the interface name
   */
  String getName();

  /**
   * Gets the version of the interface if specified.
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
   * Gets all functions defined in this interface.
   *
   * @return list of function metadata
   */
  List<WasiFunctionMetadata> getFunctions();

  /**
   * Gets metadata for a specific function.
   *
   * @param functionName the function name to look up
   * @return function metadata, or empty if function doesn't exist
   * @throws IllegalArgumentException if functionName is null or empty
   */
  Optional<WasiFunctionMetadata> getFunction(final String functionName);

  /**
   * Gets all resource types defined in this interface.
   *
   * @return list of resource type metadata
   */
  List<WasiResourceTypeMetadata> getResourceTypes();

  /**
   * Gets metadata for a specific resource type.
   *
   * @param typeName the resource type name to look up
   * @return resource type metadata, or empty if type doesn't exist
   * @throws IllegalArgumentException if typeName is null or empty
   */
  Optional<WasiResourceTypeMetadata> getResourceType(final String typeName);

  /**
   * Gets all custom types defined in this interface.
   *
   * <p>Custom types include records, variants, enums, and other user-defined types used in
   * function signatures.
   *
   * @return map of type names to their definitions
   */
  Map<String, WasiTypeDefinition> getCustomTypes();

  /**
   * Gets a specific custom type definition.
   *
   * @param typeName the type name to look up
   * @return type definition, or empty if type doesn't exist
   * @throws IllegalArgumentException if typeName is null or empty
   */
  Optional<WasiTypeDefinition> getCustomType(final String typeName);

  /**
   * Gets all constants defined in this interface.
   *
   * @return map of constant names to their values
   */
  Map<String, Object> getConstants();

  /**
   * Gets a specific constant value.
   *
   * @param constantName the constant name to look up
   * @return constant value, or empty if constant doesn't exist
   * @throws IllegalArgumentException if constantName is null or empty
   */
  Optional<Object> getConstant(final String constantName);

  /**
   * Gets dependency information for this interface.
   *
   * <p>Dependencies include other interfaces that this interface imports or requires.
   *
   * @return list of interface names that this interface depends on
   */
  List<String> getDependencies();

  /**
   * Checks if this interface is compatible with another interface.
   *
   * <p>Compatibility checking considers function signatures, type definitions, and semantic
   * versioning if available.
   *
   * @param other the interface to check compatibility with
   * @return true if interfaces are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final WasiInterfaceMetadata other);

  /**
   * Gets additional metadata properties specific to this interface.
   *
   * <p>This includes tool-specific annotations, custom attributes, and extension metadata not
   * covered by standard fields.
   *
   * @return map of property names to values
   */
  Map<String, Object> getProperties();

  /**
   * Validates this interface metadata for completeness and consistency.
   *
   * <p>Validation checks include type reference consistency, function signature validity, and
   * dependency resolution.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();
}