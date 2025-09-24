package ai.tegmentum.wasmtime4j.gc;

import ai.tegmentum.wasmtime4j.WasmValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * WebAssembly GC array instance.
 *
 * <p>Represents an instance of an array type with element values. Provides indexed access to
 * elements with bounds checking and type safety.
 *
 * <p>Usage example:
 *
 * <pre>{@code
 * ArrayInstance intArray = runtime.createArray(intArrayType, 10);
 * intArray.setElement(0, GcValue.i32(42));
 * intArray.setElement(1, GcValue.i32(100));
 *
 * int value = intArray.getElement(0).asI32();
 * int length = intArray.getLength();
 * }</pre>
 *
 * @since 1.0.0
 */
public final class ArrayInstance implements GcObject {
  private final long objectId;
  private final ArrayType arrayType;
  private final GcRuntime runtime;

  /**
   * Create a new array instance.
   *
   * @param objectId the object ID
   * @param arrayType the array type
   * @param runtime the GC runtime
   */
  public ArrayInstance(final long objectId, final ArrayType arrayType, final GcRuntime runtime) {
    this.objectId = objectId;
    this.arrayType = Objects.requireNonNull(arrayType, "Array type cannot be null");
    this.runtime = Objects.requireNonNull(runtime, "Runtime cannot be null");
  }

  @Override
  public long getObjectId() {
    return objectId;
  }

  @Override
  public GcReferenceType getReferenceType() {
    return GcReferenceType.ARRAY_REF;
  }

  @Override
  public boolean isNull() {
    return false; // Array instances are never null
  }

  @Override
  public boolean isOfType(final GcReferenceType type) {
    return type.equals(GcReferenceType.ARRAY_REF)
        || type.equals(GcReferenceType.EQ_REF)
        || type.equals(GcReferenceType.ANY_REF);
  }

  @Override
  public GcObject castTo(final GcReferenceType type) {
    if (!isOfType(type)) {
      throw new ClassCastException("Cannot cast array to " + type);
    }
    return this;
  }

  @Override
  public boolean refEquals(final GcObject other) {
    return other != null && this.objectId == other.getObjectId();
  }

  @Override
  public int getSizeBytes() {
    return (int) arrayType.getArraySizeBytes(getLength());
  }

  @Override
  public WasmValue toWasmValue() {
    return WasmValue.externRef(this);
  }

  /**
   * Gets the array type of this instance.
   *
   * @return the array type
   */
  public ArrayType getArrayType() {
    return arrayType;
  }

  /**
   * Gets the length of this array.
   *
   * @return the array length
   */
  public int getLength() {
    return runtime.getArrayLength(this);
  }

  /**
   * Gets an element value by index.
   *
   * @param index the element index
   * @return the element value
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws GcException if element access fails
   */
  public GcValue getElement(final int index) {
    if (index < 0 || index >= getLength()) {
      throw new IndexOutOfBoundsException(
          "Index " + index + " out of bounds for array of length " + getLength());
    }

    return runtime.getArrayElement(this, index);
  }

  /**
   * Sets an element value by index.
   *
   * @param index the element index
   * @param value the new value
   * @return this instance for method chaining
   * @throws IndexOutOfBoundsException if index is invalid
   * @throws IllegalArgumentException if array is immutable or value type is incompatible
   * @throws GcException if element assignment fails
   */
  public ArrayInstance setElement(final int index, final GcValue value) {
    if (index < 0 || index >= getLength()) {
      throw new IndexOutOfBoundsException(
          "Index " + index + " out of bounds for array of length " + getLength());
    }

    if (!arrayType.isMutable()) {
      throw new IllegalArgumentException("Array is immutable");
    }

    runtime.setArrayElement(this, index, value);
    return this;
  }

  /**
   * Sets an element value by index with automatic type conversion.
   *
   * @param index the element index
   * @param value the new value (will be converted to appropriate GcValue)
   * @return this instance for method chaining
   */
  public ArrayInstance setElement(final int index, final Object value) {
    return setElement(index, GcValue.fromObject(value));
  }

  /**
   * Gets all elements as a list.
   *
   * @return list of all elements
   */
  public List<GcValue> getAllElements() {
    final int length = getLength();
    final List<GcValue> elements = new ArrayList<>(length);
    for (int i = 0; i < length; i++) {
      elements.add(getElement(i));
    }
    return elements;
  }

  /**
   * Sets multiple elements starting at the given index.
   *
   * @param startIndex the starting index
   * @param values the values to set
   * @return this instance for method chaining
   * @throws IndexOutOfBoundsException if any index is invalid
   */
  public ArrayInstance setElements(final int startIndex, final List<GcValue> values) {
    if (startIndex < 0 || startIndex + values.size() > getLength()) {
      throw new IndexOutOfBoundsException(
          "Range ["
              + startIndex
              + ", "
              + (startIndex + values.size())
              + ") out of bounds for array of length "
              + getLength());
    }

    for (int i = 0; i < values.size(); i++) {
      setElement(startIndex + i, values.get(i));
    }
    return this;
  }

  /**
   * Fills the array with the given value.
   *
   * @param value the value to fill with
   * @return this instance for method chaining
   */
  public ArrayInstance fill(final GcValue value) {
    final int length = getLength();
    for (int i = 0; i < length; i++) {
      setElement(i, value);
    }
    return this;
  }

  /**
   * Fills a range of the array with the given value.
   *
   * @param startIndex the starting index (inclusive)
   * @param endIndex the ending index (exclusive)
   * @param value the value to fill with
   * @return this instance for method chaining
   */
  public ArrayInstance fill(final int startIndex, final int endIndex, final GcValue value) {
    if (startIndex < 0 || endIndex > getLength() || startIndex > endIndex) {
      throw new IndexOutOfBoundsException(
          "Invalid range ["
              + startIndex
              + ", "
              + endIndex
              + ") for array of length "
              + getLength());
    }

    for (int i = startIndex; i < endIndex; i++) {
      setElement(i, value);
    }
    return this;
  }

  /**
   * Copies elements from another array.
   *
   * @param source the source array
   * @param sourceIndex the starting index in the source array
   * @param destIndex the starting index in this array
   * @param length the number of elements to copy
   * @return this instance for method chaining
   */
  public ArrayInstance copyFrom(
      final ArrayInstance source, final int sourceIndex, final int destIndex, final int length) {
    if (sourceIndex < 0 || sourceIndex + length > source.getLength()) {
      throw new IndexOutOfBoundsException("Source range out of bounds");
    }
    if (destIndex < 0 || destIndex + length > getLength()) {
      throw new IndexOutOfBoundsException("Destination range out of bounds");
    }

    // Check element type compatibility
    if (!arrayType.getElementType().isCompatibleWith(source.arrayType.getElementType())) {
      throw new IllegalArgumentException("Incompatible element types");
    }

    for (int i = 0; i < length; i++) {
      setElement(destIndex + i, source.getElement(sourceIndex + i));
    }
    return this;
  }

  /**
   * Checks if this array is an instance of the specified array type.
   *
   * @param type the array type to check
   * @return true if this array is an instance of the type
   */
  public boolean isInstanceOf(final ArrayType type) {
    return arrayType.isSubtypeOf(type);
  }

  /**
   * Creates a shallow copy of this array with the same element values.
   *
   * @return a new array instance with copied values
   */
  public ArrayInstance copy() {
    return runtime.createArray(arrayType, getAllElements());
  }

  /**
   * Gets a subarray view (creates a new array with copied elements).
   *
   * @param startIndex the starting index (inclusive)
   * @param endIndex the ending index (exclusive)
   * @return a new array with the subrange
   */
  public ArrayInstance subArray(final int startIndex, final int endIndex) {
    if (startIndex < 0 || endIndex > getLength() || startIndex > endIndex) {
      throw new IndexOutOfBoundsException(
          "Invalid range ["
              + startIndex
              + ", "
              + endIndex
              + ") for array of length "
              + getLength());
    }

    final List<GcValue> subElements = new ArrayList<>();
    for (int i = startIndex; i < endIndex; i++) {
      subElements.add(getElement(i));
    }

    return runtime.createArray(arrayType, subElements);
  }

  /**
   * Finds the first index of the given value.
   *
   * @param value the value to search for
   * @return the index of the first occurrence, or -1 if not found
   */
  public int indexOf(final GcValue value) {
    final int length = getLength();
    for (int i = 0; i < length; i++) {
      if (Objects.equals(getElement(i), value)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Checks if the array contains the given value.
   *
   * @param value the value to search for
   * @return true if the value is found
   */
  public boolean contains(final GcValue value) {
    return indexOf(value) != -1;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ArrayInstance that = (ArrayInstance) obj;
    return objectId == that.objectId;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(objectId);
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append(arrayType.getName()).append("[").append(getLength()).append("] {");

    final int length = Math.min(getLength(), 10); // Show first 10 elements
    for (int i = 0; i < length; i++) {
      if (i > 0) {
        sb.append(", ");
      }

      try {
        sb.append(getElement(i));
      } catch (final Exception e) {
        sb.append("<error>");
      }
    }

    if (getLength() > 10) {
      sb.append(", ...");
    }

    sb.append("}");
    return sb.toString();
  }
}
