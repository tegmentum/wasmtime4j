package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Optional;

/**
 * Represents a type parameter in generic component types.
 *
 * <p>ComponentTypeParameter defines parameterized types in the component model, allowing for
 * generic interfaces and functions that can work with multiple concrete types. This enables
 * type-safe polymorphism in component interfaces.
 *
 * @since 1.0.0
 */
public interface ComponentTypeParameter {

  /**
   * Gets the name of this type parameter.
   *
   * <p>Returns the identifier used for this type parameter in type definitions and signatures.
   *
   * @return the type parameter name
   */
  String getName();

  /**
   * Gets the bounds for this type parameter.
   *
   * <p>Returns the constraints that concrete types must satisfy to be used as this type
   * parameter. These define the upper and lower bounds for valid substitutions.
   *
   * @return list of type bounds
   */
  List<ComponentTypeBound> getBounds();

  /**
   * Gets the default type for this parameter.
   *
   * <p>Returns the type that should be used if no explicit type is provided for this parameter.
   * Returns empty if no default is specified.
   *
   * @return default type, or empty if none specified
   */
  Optional<ComponentValueType> getDefaultType();

  /**
   * Gets the variance of this type parameter.
   *
   * <p>Returns whether this type parameter is covariant, contravariant, or invariant with
   * respect to subtyping relationships.
   *
   * @return type variance
   */
  TypeVariance getVariance();

  /**
   * Checks if this type parameter has a specific bound.
   *
   * <p>Determines if this type parameter is constrained by the specified bound type.
   *
   * @param boundType the bound type to check for
   * @return true if the bound exists, false otherwise
   * @throws IllegalArgumentException if boundType is null
   */
  boolean hasBound(final ComponentValueType boundType);

  /**
   * Validates a concrete type against this parameter's constraints.
   *
   * <p>Checks if the provided type can be used as a concrete substitution for this type
   * parameter.
   *
   * @param concreteType the concrete type to validate
   * @return true if the type satisfies all bounds, false otherwise
   * @throws IllegalArgumentException if concreteType is null
   */
  boolean validateConcreteType(final ComponentValueType concreteType);

  /**
   * Gets documentation for this type parameter.
   *
   * <p>Returns human-readable description of this type parameter's purpose and constraints.
   *
   * @return type parameter documentation, or empty if none available
   */
  Optional<String> getDocumentation();

  /**
   * Type variance options for generic type parameters.
   */
  enum TypeVariance {
    /** Type parameter is covariant (can be substituted with subtypes) */
    COVARIANT,
    /** Type parameter is contravariant (can be substituted with supertypes) */
    CONTRAVARIANT,
    /** Type parameter is invariant (exact type match required) */
    INVARIANT
  }
}