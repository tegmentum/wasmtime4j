package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model resource type definitions.
 *
 * <p>ComponentResourceType represents the type definition for a resource, including its methods,
 * properties, lifecycle semantics, and security characteristics. Resource types enable stateful,
 * capability-based abstractions in the Component Model.
 *
 * <p>Resource types define the interface contract for resource instances and provide the foundation
 * for resource management and access control.
 *
 * @since 1.0.0
 */
public interface ComponentResourceType {

  /**
   * Gets the name of this resource type.
   *
   * @return the resource type name
   */
  String getName();

  /**
   * Gets documentation for this resource type.
   *
   * @return resource type documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets all methods defined on this resource type.
   *
   * @return list of resource methods
   */
  List<InterfaceFunction> getMethods();

  /**
   * Gets a specific method by name.
   *
   * @param name the method name to look up
   * @return the method definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<InterfaceFunction> getMethod(final String name);

  /**
   * Gets all constructor functions for this resource type.
   *
   * @return list of constructor functions
   */
  List<InterfaceFunction> getConstructors();

  /**
   * Gets the destructor function if defined.
   *
   * @return the destructor function, or empty if not defined
   */
  Optional<InterfaceFunction> getDestructor();

  /**
   * Gets custom properties associated with this resource type.
   *
   * @return map of property names to their types
   */
  Map<String, ComponentValueType> getProperties();

  /**
   * Gets the lifecycle policy for instances of this resource type.
   *
   * @return the lifecycle policy
   */
  ResourceLifecyclePolicy getLifecyclePolicy();

  /**
   * Gets security attributes for this resource type.
   *
   * @return resource security attributes
   */
  ResourceSecurityAttributes getSecurityAttributes();

  /**
   * Gets the capabilities supported by this resource type.
   *
   * @return list of capability names
   */
  List<String> getCapabilities();

  /**
   * Checks if this resource type supports a specific capability.
   *
   * @param capability the capability name to check
   * @return true if the capability is supported, false otherwise
   * @throws IllegalArgumentException if capability is null or empty
   */
  boolean hasCapability(final String capability);

  /**
   * Gets additional attributes/annotations for this resource type.
   *
   * @return resource type attributes
   */
  Map<String, Object> getAttributes();

  /**
   * Validates this resource type definition for correctness.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Checks if this resource type is compatible with another resource type.
   *
   * @param other the resource type to check compatibility with
   * @return true if resource types are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentResourceType other);

  /**
   * Gets metadata about this resource type.
   *
   * @return resource type metadata
   */
  ComponentResourceTypeMetadata getMetadata();
}
