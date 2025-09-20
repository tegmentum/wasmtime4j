package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Optional;

/**
 * Type information for WebAssembly component imports.
 *
 * <p>ComponentImportType describes the expected type and constraints for component imports,
 * enabling type checking during linking and import resolution. This ensures that provided
 * implementations match the component's requirements.
 *
 * @since 1.0.0
 */
public interface ComponentImportType {

  /**
   * Gets the name of this import.
   *
   * <p>Returns the import name as declared in the component's interface definition.
   *
   * @return the import name
   */
  String getName();

  /**
   * Gets the kind of this import.
   *
   * <p>Returns the category of import (function, interface, resource, component, etc.) that this
   * type represents.
   *
   * @return the import kind
   */
  ComponentImportKind getKind();

  /**
   * Gets the value type for this import.
   *
   * <p>Returns the component value type that describes the structure and constraints of the
   * expected import implementation.
   *
   * @return the component value type
   */
  ComponentValueType getValueType();

  /**
   * Gets function type information if this import is a function.
   *
   * <p>Returns detailed function signature information that the import implementation must match.
   * Returns empty if this import is not a function.
   *
   * @return function type information, or empty if not a function
   */
  Optional<ComponentFunctionType> getFunctionType();

  /**
   * Gets interface type information if this import is an interface.
   *
   * <p>Returns complete interface definition that the import implementation must satisfy. Returns
   * empty if this import is not an interface.
   *
   * @return interface type information, or empty if not an interface
   */
  Optional<InterfaceType> getInterfaceType();

  /**
   * Gets resource type information if this import is a resource.
   *
   * <p>Returns resource type definition that the import implementation must provide. Returns
   * empty if this import is not a resource.
   *
   * @return resource type information, or empty if not a resource
   */
  Optional<ComponentResourceType> getResourceType();

  /**
   * Gets component type information if this import is a component.
   *
   * <p>Returns component type definition that the imported component must satisfy. Returns empty
   * if this import is not a component.
   *
   * @return component type information, or empty if not a component
   */
  Optional<ComponentType> getComponentType();

  /**
   * Checks if this import is required or optional.
   *
   * <p>Returns true if this import must be satisfied for the component to instantiate
   * successfully, false if it's optional.
   *
   * @return true if the import is required, false if optional
   */
  boolean isRequired();

  /**
   * Gets the default implementation if this import is optional.
   *
   * <p>Returns a default implementation that will be used if no explicit import is provided for
   * this optional import. Returns empty if no default is available or if the import is required.
   *
   * @return default implementation, or empty if none available
   */
  Optional<Object> getDefaultImplementation();

  /**
   * Validates an implementation against this import type.
   *
   * <p>Checks if the provided implementation satisfies this import's type requirements and
   * constraints.
   *
   * @param implementation the implementation to validate
   * @return true if the implementation is compatible, false otherwise
   * @throws IllegalArgumentException if implementation is null
   */
  boolean validateImplementation(final Object implementation);

  /**
   * Gets the type constraints for this import.
   *
   * <p>Returns any additional constraints or validation rules that apply to implementations of
   * this import beyond the basic type structure.
   *
   * @return list of type constraints
   */
  List<ComponentTypeConstraint> getConstraints();

  /**
   * Gets version requirements for this import.
   *
   * <p>Returns version constraints that import implementations must satisfy, useful for ensuring
   * compatibility across component versions.
   *
   * @return version requirements, or empty if no version constraints
   */
  Optional<ComponentVersionRequirements> getVersionRequirements();

  /**
   * Gets security requirements for this import.
   *
   * <p>Returns security constraints and permissions that import implementations must satisfy or
   * possess.
   *
   * @return security requirements
   */
  ComponentSecurityRequirements getSecurityRequirements();

  /**
   * Gets documentation for this import.
   *
   * <p>Returns human-readable description that explains the purpose and requirements of this
   * import.
   *
   * @return import documentation, or empty if none available
   */
  Optional<String> getDocumentation();

  /**
   * Checks if this import type is compatible with another import type.
   *
   * <p>Determines if implementations that satisfy one import type can also satisfy the other,
   * following component model subtyping rules.
   *
   * @param other the import type to check compatibility with
   * @return true if types are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentImportType other);
}