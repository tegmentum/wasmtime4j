package ai.tegmentum.wasmtime4j.gc;

/**
 * WebAssembly GC I31 instance.
 *
 * <p>Represents an immediate 31-bit signed integer stored as a reference. I31 values provide
 * efficient storage for small integers without heap allocation while maintaining reference
 * semantics for equality comparison.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * I31Instance value = runtime.createI31(42);
 * int intValue = value.getValue();
 * int unsignedValue = value.getUnsignedValue();
 *
 * boolean equal = value.refEquals(runtime.createI31(42)); // false (different objects)
 * boolean valueEqual = value.getValue() == 42; // true (same value)
 * }</pre>
 *
 * @since 1.0.0
 */
public interface I31Instance extends GcObject {

  /**
   * Gets the I31 type.
   *
   * @return the I31 type
   */
  I31Type getType();

  /**
   * Gets the signed 31-bit integer value.
   *
   * @return the signed value
   */
  int getValue();

  /**
   * Gets the signed 31-bit integer value.
   *
   * @return the signed value
   */
  int getSignedValue();

  /**
   * Gets the unsigned 31-bit integer value.
   *
   * @return the unsigned value
   */
  int getUnsignedValue();
}
