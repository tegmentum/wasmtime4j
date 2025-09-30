package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.Objects;

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
public final class I31Instance implements GcObject {
  private final long objectId;
  private final int value;

  /**
   * Create a new I31 instance.
   *
   * @param objectId the object ID
   * @param value the 31-bit signed integer value
   */
  public I31Instance(final long objectId, final int value) {
    this.objectId = objectId;
    this.value = I31Type.validateValue(value);
  }

  @Override
  public long getObjectId() {
    return objectId;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.I31_REF;
  }

  @Override
  public boolean isNull() {
    return false; // I31 instances are never null
  }

  @Override
  public boolean isOfType(final GcReferenceType type) {
    return type.equals(GcReferenceType.I31_REF)
        || type.equals(GcReferenceType.EQ_REF)
        || type.equals(GcReferenceType.ANY_REF);
  }

  @Override
  public GcObject castTo(final GcReferenceType type) {
    if (!isOfType(type)) {
      throw new ClassCastException("Cannot cast I31 to " + type);
    }
    return this;
  }

  @Override
  public boolean refEquals(final GcObject other) {
    return other != null && this.objectId == other.getObjectId();
  }

  @Override
  public int getSizeBytes() {
    return I31Type.BIT_WIDTH / 8; // Conceptual size, actually stored inline
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.i32(value);
  }

  /**
   * Gets the signed 31-bit integer value.
   *
   * @return the signed value
   */
  public int getValue() {
    return value;
  }

  /**
   * Gets the unsigned 31-bit integer value.
   *
   * @return the unsigned value
   */
  public int getUnsignedValue() {
    return I31Type.toUnsigned(value);
  }

  /**
   * Checks if this I31 value equals another I31 value by integer value (not reference identity).
   *
   * @param other the other I31 instance
   * @return true if the integer values are equal
   */
  public boolean valueEquals(final I31Instance other) {
    return other != null && this.value == other.value;
  }

  /**
   * Compares this I31 value with another I31 value.
   *
   * @param other the other I31 instance
   * @return negative if this < other, zero if equal, positive if this > other
   */
  public int compareTo(final I31Instance other) {
    Objects.requireNonNull(other, "Other I31 instance cannot be null");
    return I31Type.compare(this.value, other.value);
  }

  /**
   * Checks if this value is in the valid I31 range.
   *
   * @return true if the value is valid (always true for existing instances)
   */
  public boolean isValidValue() {
    return I31Type.isValidValue(value);
  }

  /**
   * Gets the minimum value that can be stored in an I31.
   *
   * @return the minimum I31 value
   */
  public static int getMinValue() {
    return I31Type.getMinValue();
  }

  /**
   * Gets the maximum value that can be stored in an I31.
   *
   * @return the maximum I31 value
   */
  public static int getMaxValue() {
    return I31Type.getMaxValue();
  }

  /**
   * Converts this I31 to a GcValue.
   *
   * @return the corresponding GcValue
   */
  public GcValue toGcValue() {
    return GcValue.i32(value);
  }

  /**
   * Performs arithmetic addition with another I31 value.
   *
   * <p>Note: This creates a new I31 instance and does not modify this instance. The result is
   * clamped to the valid I31 range.
   *
   * @param other the other I31 instance
   * @param runtime the GC runtime for creating the result
   * @return a new I31 instance with the sum
   */
  public I31Instance add(final I31Instance other, final GcRuntime runtime) {
    Objects.requireNonNull(other, "Other I31 instance cannot be null");
    Objects.requireNonNull(runtime, "Runtime cannot be null");

    final long sum = (long) this.value + other.value;
    final int clampedSum = I31Type.clampValue(sum);
    return runtime.createI31(clampedSum);
  }

  /**
   * Performs arithmetic subtraction with another I31 value.
   *
   * <p>Note: This creates a new I31 instance and does not modify this instance. The result is
   * clamped to the valid I31 range.
   *
   * @param other the other I31 instance
   * @param runtime the GC runtime for creating the result
   * @return a new I31 instance with the difference
   */
  public I31Instance subtract(final I31Instance other, final GcRuntime runtime) {
    Objects.requireNonNull(other, "Other I31 instance cannot be null");
    Objects.requireNonNull(runtime, "Runtime cannot be null");

    final long diff = (long) this.value - other.value;
    final int clampedDiff = I31Type.clampValue(diff);
    return runtime.createI31(clampedDiff);
  }

  /**
   * Performs arithmetic multiplication with another I31 value.
   *
   * <p>Note: This creates a new I31 instance and does not modify this instance. The result is
   * clamped to the valid I31 range.
   *
   * @param other the other I31 instance
   * @param runtime the GC runtime for creating the result
   * @return a new I31 instance with the product
   */
  public I31Instance multiply(final I31Instance other, final GcRuntime runtime) {
    Objects.requireNonNull(other, "Other I31 instance cannot be null");
    Objects.requireNonNull(runtime, "Runtime cannot be null");

    final long product = (long) this.value * other.value;
    final int clampedProduct = I31Type.clampValue(product);
    return runtime.createI31(clampedProduct);
  }

  /**
   * Performs bitwise AND with another I31 value.
   *
   * @param other the other I31 instance
   * @param runtime the GC runtime for creating the result
   * @return a new I31 instance with the result
   */
  public I31Instance and(final I31Instance other, final GcRuntime runtime) {
    Objects.requireNonNull(other, "Other I31 instance cannot be null");
    Objects.requireNonNull(runtime, "Runtime cannot be null");

    final int result = this.value & other.value;
    return runtime.createI31(result);
  }

  /**
   * Performs bitwise OR with another I31 value.
   *
   * @param other the other I31 instance
   * @param runtime the GC runtime for creating the result
   * @return a new I31 instance with the result
   */
  public I31Instance or(final I31Instance other, final GcRuntime runtime) {
    Objects.requireNonNull(other, "Other I31 instance cannot be null");
    Objects.requireNonNull(runtime, "Runtime cannot be null");

    final int result = this.value | other.value;
    return runtime.createI31(I31Type.clampValue(result));
  }

  /**
   * Performs bitwise XOR with another I31 value.
   *
   * @param other the other I31 instance
   * @param runtime the GC runtime for creating the result
   * @return a new I31 instance with the result
   */
  public I31Instance xor(final I31Instance other, final GcRuntime runtime) {
    Objects.requireNonNull(other, "Other I31 instance cannot be null");
    Objects.requireNonNull(runtime, "Runtime cannot be null");

    final int result = this.value ^ other.value;
    return runtime.createI31(I31Type.clampValue(result));
  }

  /**
   * Performs bitwise NOT (complement).
   *
   * @param runtime the GC runtime for creating the result
   * @return a new I31 instance with the result
   */
  public I31Instance not(final GcRuntime runtime) {
    Objects.requireNonNull(runtime, "Runtime cannot be null");

    final int result = ~this.value;
    return runtime.createI31(I31Type.clampValue(result));
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final I31Instance that = (I31Instance) obj;
    return objectId == that.objectId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(objectId);
  }

  @Override
  public String toString() {
    return I31Type.toString(value) + "@" + objectId;
  }
}
