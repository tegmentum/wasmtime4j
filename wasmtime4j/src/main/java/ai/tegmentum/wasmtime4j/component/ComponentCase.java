package ai.tegmentum.wasmtime4j.component;

import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model variant case definitions.
 *
 * <p>ComponentCase represents a case within a variant type, including its name, optional payload
 * type, and metadata. Cases define the possible alternatives that a variant value can represent.
 *
 * @since 1.0.0
 */
public interface ComponentCase {

  /**
   * Gets the name of this case.
   *
   * @return the case name
   */
  String getName();

  /**
   * Gets the discriminant value for this case.
   *
   * <p>Returns the integer value used to identify this case in the variant's runtime
   * representation.
   *
   * @return the discriminant value
   */
  int getDiscriminant();

  /**
   * Gets the payload type for this case.
   *
   * <p>Returns the type of data that this case can carry, or empty if this is a unit case with no
   * payload.
   *
   * @return the payload type, or empty if no payload
   */
  Optional<ComponentValueType> getPayloadType();

  /**
   * Checks if this case has a payload.
   *
   * <p>Returns true if this case carries data, false if it's a unit case.
   *
   * @return true if the case has a payload, false otherwise
   */
  boolean hasPayload();

  /**
   * Checks if this is a unit case (no payload).
   *
   * @return true if this is a unit case, false otherwise
   */
  boolean isUnit();

  /**
   * Gets documentation for this case.
   *
   * @return case documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets additional attributes/annotations for this case.
   *
   * @return case attributes
   */
  Map<String, Object> getAttributes();

  /**
   * Validates a payload value against this case's payload type.
   *
   * @param payload the payload value to validate
   * @return true if the payload is valid for this case, false otherwise
   */
  boolean isValidPayload(final Object payload);

  /**
   * Checks if this case is compatible with another case definition.
   *
   * @param other the case to check compatibility with
   * @return true if cases are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentCase other);
}
