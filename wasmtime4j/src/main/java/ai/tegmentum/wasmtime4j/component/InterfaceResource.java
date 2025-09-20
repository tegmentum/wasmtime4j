package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model interface resource definitions.
 *
 * <p>InterfaceResource represents a resource type definition within a WIT interface. Resources
 * provide stateful, capability-based abstractions that enable controlled access to component
 * functionality and external system resources.
 *
 * <p>Resources support methods (both instance and static), constructors, and lifecycle management
 * according to the Component Model specification.
 *
 * <p>Example resource definition:
 *
 * <pre>{@code
 * resource file {
 *   constructor open(path: string)
 *   read: func() -> list<u8>
 *   write: func(data: list<u8>)
 *   static create-temp: func() -> file
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface InterfaceResource {

  /**
   * Gets the name of this resource type.
   *
   * @return the resource type name
   */
  String getName();

  /**
   * Gets documentation for this resource type.
   *
   * @return resource documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets all methods defined on this resource type.
   *
   * <p>Returns both instance methods and static methods defined for this resource.
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
   * Gets all instance methods for this resource.
   *
   * <p>Returns methods that require a resource instance to be called.
   *
   * @return list of instance methods
   */
  List<InterfaceFunction> getInstanceMethods();

  /**
   * Gets all static methods for this resource.
   *
   * <p>Returns methods that can be called on the resource type without requiring an instance.
   *
   * @return list of static methods
   */
  List<InterfaceFunction> getStaticMethods();

  /**
   * Gets all constructor functions for this resource.
   *
   * <p>Returns functions that create and return new instances of this resource type.
   *
   * @return list of constructor functions
   */
  List<InterfaceFunction> getConstructors();

  /**
   * Gets the primary constructor if defined.
   *
   * <p>Returns the main constructor function for this resource, or empty if no constructors are
   * defined or if there are multiple constructors without a designated primary.
   *
   * @return the primary constructor, or empty if not available
   */
  Optional<InterfaceFunction> getPrimaryConstructor();

  /**
   * Gets the destructor function if defined.
   *
   * <p>Returns the function responsible for cleaning up resources when the resource instance is no
   * longer needed.
   *
   * @return the destructor function, or empty if not defined
   */
  Optional<InterfaceFunction> getDestructor();

  /**
   * Gets custom properties/fields associated with this resource.
   *
   * <p>Returns resource-specific data or configuration that is part of the resource definition.
   *
   * @return map of property names to their types
   */
  Map<String, ComponentValueType> getProperties();

  /**
   * Gets a specific property by name.
   *
   * @param name the property name to look up
   * @return the property type, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentValueType> getProperty(final String name);

  /**
   * Gets the interfaces that this resource depends on.
   *
   * <p>Returns interfaces whose types are used in this resource's method signatures or properties.
   *
   * @return list of interface names this resource depends on
   */
  List<String> getDependencies();

  /**
   * Checks if this resource supports a specific capability.
   *
   * <p>Capabilities define what operations or features are available on this resource type.
   *
   * @param capability the capability name to check
   * @return true if the resource supports the capability, false otherwise
   * @throws IllegalArgumentException if capability is null or empty
   */
  boolean hasCapability(final String capability);

  /**
   * Gets all capabilities supported by this resource.
   *
   * @return list of capability names
   */
  List<String> getCapabilities();

  /**
   * Gets the resource's lifecycle policy.
   *
   * <p>Returns information about how instances of this resource are managed, including ownership
   * semantics and cleanup behavior.
   *
   * @return the lifecycle policy
   */
  ResourceLifecyclePolicy getLifecyclePolicy();

  /**
   * Gets security attributes for this resource.
   *
   * <p>Returns security-related metadata such as required permissions, access controls, and
   * isolation requirements.
   *
   * @return resource security attributes
   */
  ResourceSecurityAttributes getSecurityAttributes();

  /**
   * Validates this resource definition for correctness.
   *
   * <p>Validation includes method signature checking, property type validation, and lifecycle
   * consistency.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Checks if this resource is compatible with another resource definition.
   *
   * <p>Compatibility checking considers method signatures, properties, capabilities, and lifecycle
   * policies.
   *
   * @param other the resource to check compatibility with
   * @return true if resources are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final InterfaceResource other);

  /**
   * Gets additional attributes/annotations for this resource.
   *
   * <p>Returns tool-specific metadata, performance hints, and custom annotations.
   *
   * @return resource attributes
   */
  Map<String, Object> getAttributes();

  /**
   * Gets metadata about this resource.
   *
   * <p>Returns information about complexity, performance characteristics, and usage patterns.
   *
   * @return resource metadata
   */
  InterfaceResourceMetadata getMetadata();
}
