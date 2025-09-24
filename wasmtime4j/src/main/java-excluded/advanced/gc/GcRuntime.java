package ai.tegmentum.wasmtime4j.gc;

import java.util.List;

/**
 * Interface for WebAssembly GC runtime operations.
 *
 * <p>Defines the contract for garbage collection runtime implementations, providing methods for
 * object creation, field access, and runtime management.
 *
 * @since 1.0.0
 */
public interface GcRuntime {

  /**
   * Creates a new struct instance.
   *
   * @param structType the struct type
   * @param fieldValues the initial field values
   * @return the new struct instance
   * @throws GcException if creation fails
   */
  StructInstance createStruct(StructType structType, List<GcValue> fieldValues);

  /**
   * Creates a new struct instance with default values.
   *
   * @param structType the struct type
   * @return the new struct instance with default field values
   * @throws GcException if creation fails
   */
  StructInstance createStruct(StructType structType);

  /**
   * Creates a new array instance.
   *
   * @param arrayType the array type
   * @param elements the array elements
   * @return the new array instance
   * @throws GcException if creation fails
   */
  ArrayInstance createArray(ArrayType arrayType, List<GcValue> elements);

  /**
   * Creates a new array instance with default values.
   *
   * @param arrayType the array type
   * @param length the array length
   * @return the new array instance with default element values
   * @throws GcException if creation fails
   */
  ArrayInstance createArray(ArrayType arrayType, int length);

  /**
   * Creates a new I31 value.
   *
   * @param value the integer value
   * @return the I31 instance
   * @throws GcException if creation fails
   */
  I31Instance createI31(int value);

  /**
   * Gets a struct field value.
   *
   * @param struct the struct instance
   * @param fieldIndex the field index
   * @return the field value
   * @throws GcException if field access fails
   */
  GcValue getStructField(StructInstance struct, int fieldIndex);

  /**
   * Sets a struct field value.
   *
   * @param struct the struct instance
   * @param fieldIndex the field index
   * @param value the new value
   * @throws GcException if field assignment fails
   */
  void setStructField(StructInstance struct, int fieldIndex, GcValue value);

  /**
   * Gets an array element value.
   *
   * @param array the array instance
   * @param elementIndex the element index
   * @return the element value
   * @throws GcException if element access fails
   */
  GcValue getArrayElement(ArrayInstance array, int elementIndex);

  /**
   * Sets an array element value.
   *
   * @param array the array instance
   * @param elementIndex the element index
   * @param value the new value
   * @throws GcException if element assignment fails
   */
  void setArrayElement(ArrayInstance array, int elementIndex, GcValue value);

  /**
   * Gets the length of an array.
   *
   * @param array the array instance
   * @return the array length
   */
  int getArrayLength(ArrayInstance array);

  /**
   * Performs a reference type cast.
   *
   * @param object the object to cast
   * @param targetType the target type
   * @return the cast object
   * @throws ClassCastException if the cast is invalid
   */
  GcObject refCast(GcObject object, GcReferenceType targetType);

  /**
   * Tests if an object is of a specific type.
   *
   * @param object the object to test
   * @param targetType the target type
   * @return true if the object is of the target type
   */
  boolean refTest(GcObject object, GcReferenceType targetType);

  /**
   * Compares two objects for reference equality.
   *
   * @param obj1 the first object
   * @param obj2 the second object
   * @return true if the objects are the same reference
   */
  boolean refEquals(GcObject obj1, GcObject obj2);

  /**
   * Checks if an object is null.
   *
   * @param object the object to check
   * @return true if the object is null
   */
  boolean isNull(GcObject object);

  /**
   * Registers a struct type with the runtime.
   *
   * @param structType the struct type to register
   * @return the assigned type ID
   * @throws GcException if registration fails
   */
  int registerStructType(StructType structType);

  /**
   * Registers an array type with the runtime.
   *
   * @param arrayType the array type to register
   * @return the assigned type ID
   * @throws GcException if registration fails
   */
  int registerArrayType(ArrayType arrayType);

  /**
   * Triggers garbage collection.
   *
   * @return collection statistics
   */
  GcStats collectGarbage();

  /**
   * Gets current garbage collection statistics.
   *
   * @return GC statistics
   */
  GcStats getGcStats();
}
