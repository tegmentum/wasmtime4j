package ai.tegmentum.wasmtime4j.component;

import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model typed values.
 *
 * <p>ComponentValue represents a typed value that can be passed to and returned from component
 * functions. Values maintain type safety and support the full range of Component Model types
 * including primitives, structured types, and resources.
 *
 * <p>Component values provide type-safe access to their underlying data and support conversion
 * between different representations as needed.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentValue value = ComponentValue.string("hello");
 * ComponentValueType type = value.getType();
 * String stringValue = value.asString();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentValue {

  /**
   * Gets the type of this value.
   *
   * @return the component value type
   */
  ComponentValueType getType();

  /**
   * Gets the raw underlying value.
   *
   * <p>Returns the Java object representation of this component value. The type of the returned
   * object depends on the component value type.
   *
   * @return the underlying value
   */
  Object getValue();

  /**
   * Checks if this value is null or represents an optional value that is not present.
   *
   * @return true if the value is null or absent, false otherwise
   */
  boolean isNull();

  // Primitive type accessors

  /**
   * Returns this value as a boolean.
   *
   * @return the boolean value
   * @throws IllegalStateException if this value is not a boolean
   */
  boolean asBoolean();

  /**
   * Returns this value as a signed 8-bit integer.
   *
   * @return the byte value
   * @throws IllegalStateException if this value is not an S8
   */
  byte asByte();

  /**
   * Returns this value as an unsigned 8-bit integer.
   *
   * @return the unsigned byte value as short
   * @throws IllegalStateException if this value is not a U8
   */
  short asUByte();

  /**
   * Returns this value as a signed 16-bit integer.
   *
   * @return the short value
   * @throws IllegalStateException if this value is not an S16
   */
  short asShort();

  /**
   * Returns this value as an unsigned 16-bit integer.
   *
   * @return the unsigned short value as int
   * @throws IllegalStateException if this value is not a U16
   */
  int asUShort();

  /**
   * Returns this value as a signed 32-bit integer.
   *
   * @return the int value
   * @throws IllegalStateException if this value is not an S32
   */
  int asInt();

  /**
   * Returns this value as an unsigned 32-bit integer.
   *
   * @return the unsigned int value as long
   * @throws IllegalStateException if this value is not a U32
   */
  long asUInt();

  /**
   * Returns this value as a signed 64-bit integer.
   *
   * @return the long value
   * @throws IllegalStateException if this value is not an S64
   */
  long asLong();

  /**
   * Returns this value as an unsigned 64-bit integer.
   *
   * @return the unsigned long value as BigInteger
   * @throws IllegalStateException if this value is not a U64
   */
  java.math.BigInteger asULong();

  /**
   * Returns this value as a 32-bit floating point number.
   *
   * @return the float value
   * @throws IllegalStateException if this value is not an F32
   */
  float asFloat();

  /**
   * Returns this value as a 64-bit floating point number.
   *
   * @return the double value
   * @throws IllegalStateException if this value is not an F64
   */
  double asDouble();

  /**
   * Returns this value as a Unicode character.
   *
   * @return the char value
   * @throws IllegalStateException if this value is not a CHAR
   */
  char asChar();

  /**
   * Returns this value as a UTF-8 string.
   *
   * @return the string value
   * @throws IllegalStateException if this value is not a STRING
   */
  String asString();

  // Structured type accessors

  /**
   * Returns this value as a list.
   *
   * @return the list of component values
   * @throws IllegalStateException if this value is not a LIST
   */
  List<ComponentValue> asList();

  /**
   * Returns this value as a record.
   *
   * @return the record value
   * @throws IllegalStateException if this value is not a RECORD
   */
  ComponentRecordValue asRecord();

  /**
   * Returns this value as a variant.
   *
   * @return the variant value
   * @throws IllegalStateException if this value is not a VARIANT
   */
  ComponentVariantValue asVariant();

  /**
   * Returns this value as a tuple.
   *
   * @return the tuple values
   * @throws IllegalStateException if this value is not a TUPLE
   */
  List<ComponentValue> asTuple();

  /**
   * Returns this value as flags.
   *
   * @return the flags value
   * @throws IllegalStateException if this value is not FLAGS
   */
  ComponentFlagsValue asFlags();

  /**
   * Returns this value as an enum.
   *
   * @return the enum value
   * @throws IllegalStateException if this value is not an ENUM
   */
  ComponentEnumValue asEnum();

  // Optional and result type accessors

  /**
   * Returns this value as an optional.
   *
   * @return the optional value
   * @throws IllegalStateException if this value is not an OPTION
   */
  Optional<ComponentValue> asOptional();

  /**
   * Returns this value as a result.
   *
   * @return the result value
   * @throws IllegalStateException if this value is not a RESULT
   */
  ComponentResultValue asResult();

  // Resource type accessors

  /**
   * Returns this value as a resource handle.
   *
   * @return the resource handle
   * @throws IllegalStateException if this value is not a RESOURCE
   */
  ComponentResourceHandle asResource();

  // Safe conversion methods

  /**
   * Safely attempts to convert this value to a boolean.
   *
   * @return the boolean value, or empty if not a boolean
   */
  Optional<Boolean> tryAsBoolean();

  /**
   * Safely attempts to convert this value to a string.
   *
   * @return the string value, or empty if not a string
   */
  Optional<String> tryAsString();

  /**
   * Safely attempts to convert this value to an integer.
   *
   * @return the integer value, or empty if not convertible to int
   */
  Optional<Integer> tryAsInt();

  /**
   * Safely attempts to convert this value to a list.
   *
   * @return the list value, or empty if not a list
   */
  Optional<List<ComponentValue>> tryAsList();

  // Serialization methods

  /**
   * Serializes this value to its binary representation.
   *
   * @return the serialized value as bytes
   */
  ByteBuffer serialize();

  /**
   * Serializes this value to its JSON representation if supported.
   *
   * @return the JSON representation, or empty if not serializable to JSON
   */
  Optional<String> toJson();

  // Static factory methods

  /**
   * Creates a boolean component value.
   *
   * @param value the boolean value
   * @return a new component value
   */
  static ComponentValue bool(final boolean value) {
    throw new UnsupportedOperationException("Component value creation not yet implemented");
  }

  /**
   * Creates a string component value.
   *
   * @param value the string value
   * @return a new component value
   * @throws IllegalArgumentException if value is null
   */
  static ComponentValue string(final String value) {
    throw new UnsupportedOperationException("Component value creation not yet implemented");
  }

  /**
   * Creates a 32-bit signed integer component value.
   *
   * @param value the integer value
   * @return a new component value
   */
  static ComponentValue s32(final int value) {
    throw new UnsupportedOperationException("Component value creation not yet implemented");
  }

  /**
   * Creates a 32-bit unsigned integer component value.
   *
   * @param value the unsigned integer value
   * @return a new component value
   */
  static ComponentValue u32(final long value) {
    throw new UnsupportedOperationException("Component value creation not yet implemented");
  }

  /**
   * Creates a list component value.
   *
   * @param values the list of component values
   * @return a new component value
   * @throws IllegalArgumentException if values is null
   */
  static ComponentValue list(final List<ComponentValue> values) {
    throw new UnsupportedOperationException("Component value creation not yet implemented");
  }

  /**
   * Creates an optional component value.
   *
   * @param value the optional value, or null for empty
   * @return a new component value
   */
  static ComponentValue option(final ComponentValue value) {
    throw new UnsupportedOperationException("Component value creation not yet implemented");
  }

  /**
   * Creates a record component value.
   *
   * @param fields map of field names to their values
   * @return a new component value
   * @throws IllegalArgumentException if fields is null
   */
  static ComponentValue record(final Map<String, ComponentValue> fields) {
    throw new UnsupportedOperationException("Component value creation not yet implemented");
  }

  /**
   * Validates this value against its declared type.
   *
   * @throws IllegalStateException if the value is invalid for its type
   */
  void validate();

  /**
   * Checks if this value is compatible with the specified type.
   *
   * @param type the type to check compatibility with
   * @return true if the value is compatible with the type, false otherwise
   * @throws IllegalArgumentException if type is null
   */
  boolean isCompatibleWith(final ComponentValueType type);
}