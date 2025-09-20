package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model variant types.
 *
 * <p>ComponentVariant represents a discriminated union type that can hold one of several possible
 * types at runtime. Variants enable type-safe representation of data that can be one of multiple
 * alternatives, similar to tagged unions or sum types.
 *
 * <p>Variants support optional payload types for each case and comprehensive type validation
 * according to the Component Model specification.
 *
 * <p>Example variant definition:
 *
 * <pre>{@code
 * variant result {
 *   ok(string),
 *   error(error-code)
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentVariant {

  /**
   * Gets the name of this variant type.
   *
   * @return the variant type name, or empty if anonymous
   */
  Optional<String> getName();

  /**
   * Gets all cases defined in this variant.
   *
   * <p>Returns an ordered list of variant cases including their names, payload types, and metadata.
   *
   * @return list of variant cases
   */
  List<ComponentCase> getCases();

  /**
   * Gets a specific case by name.
   *
   * @param name the case name to look up
   * @return the case definition, or empty if not found
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentCase> getCase(final String name);

  /**
   * Gets the names of all cases in declaration order.
   *
   * @return list of case names
   */
  List<String> getCaseNames();

  /**
   * Checks if a case with the specified name exists.
   *
   * @param name the case name to check
   * @return true if the case exists, false otherwise
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean hasCase(final String name);

  /**
   * Gets the payload type for a specific case.
   *
   * @param name the case name
   * @return the payload type, or empty if case has no payload or doesn't exist
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentValueType> getCasePayloadType(final String name);

  /**
   * Gets the number of cases in this variant.
   *
   * @return the case count
   */
  int getCaseCount();

  /**
   * Checks if this variant has any cases with payload types.
   *
   * @return true if any cases have payloads, false otherwise
   */
  boolean hasPayloadCases();

  /**
   * Gets all cases that have payload types.
   *
   * @return list of cases with payloads
   */
  List<ComponentCase> getPayloadCases();

  /**
   * Gets all cases that have no payload (unit cases).
   *
   * @return list of unit cases
   */
  List<ComponentCase> getUnitCases();

  /**
   * Gets the discriminant type used for this variant.
   *
   * <p>Returns the integer type used to represent which case is active in the variant's runtime
   * representation.
   *
   * @return the discriminant type
   */
  ComponentValueType getDiscriminantType();

  /**
   * Creates a new variant value for the specified case.
   *
   * <p>Creates a variant value with the specified case active and optional payload value.
   *
   * @param caseName the name of the case to create
   * @param payload the payload value, or null for unit cases
   * @return a new variant value
   * @throws IllegalArgumentException if case doesn't exist or payload is invalid
   */
  ComponentVariantValue createValue(final String caseName, final Object payload);

  /**
   * Creates a new variant value for a unit case (no payload).
   *
   * @param caseName the name of the unit case to create
   * @return a new variant value
   * @throws IllegalArgumentException if case doesn't exist or case requires a payload
   */
  ComponentVariantValue createUnitValue(final String caseName);

  /**
   * Validates a case name and payload value against this variant definition.
   *
   * <p>Checks that the case exists and that the payload value is compatible with the case's payload
   * type.
   *
   * @param caseName the case name
   * @param payload the payload value
   * @return true if the case and payload are valid, false otherwise
   * @throws IllegalArgumentException if caseName is null or empty
   */
  boolean isValidValue(final String caseName, final Object payload);

  /**
   * Gets documentation for this variant type.
   *
   * @return variant documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets additional attributes/annotations for this variant.
   *
   * <p>Returns metadata such as serialization hints, performance annotations, and tool-specific
   * attributes.
   *
   * @return variant attributes
   */
  Map<String, Object> getAttributes();

  /**
   * Validates this variant definition for correctness.
   *
   * <p>Validation includes case name uniqueness, payload type validity, and structural consistency.
   *
   * @throws IllegalArgumentException if validation fails with details about specific issues
   */
  void validate();

  /**
   * Checks if this variant is compatible with another variant definition.
   *
   * <p>Compatibility checking considers case names, payload types, and structural equivalence.
   *
   * @param other the variant to check compatibility with
   * @return true if variants are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentVariant other);

  /**
   * Gets metadata about this variant type.
   *
   * <p>Returns information about size, complexity, and performance characteristics.
   *
   * @return variant metadata
   */
  ComponentVariantMetadata getMetadata();
}
