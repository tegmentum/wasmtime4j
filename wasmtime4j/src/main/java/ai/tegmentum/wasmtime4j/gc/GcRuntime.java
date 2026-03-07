/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ai.tegmentum.wasmtime4j.gc;

import java.util.List;
import java.util.Optional;

/**
 * Interface for WebAssembly GC runtime operations.
 *
 * <p>Defines the contract for garbage collection runtime implementations, providing methods for
 * object creation, field access, runtime management, and advanced WebAssembly 3.0 GC features
 * including complex reference type operations, incremental collection, and debugging support.
 *
 * @since 1.0.0
 */
public interface GcRuntime {

  // ========== Basic Object Creation ==========

  /**
   * Creates a new struct instance.
   *
   * @param structType the struct type
   * @param fieldValues the initial field values
   * @return the new struct instance
   * @throws GcException if creation fails
   */
  StructInstance createStruct(StructType structType, List<GcValue> fieldValues) throws GcException;

  /**
   * Creates a new struct instance with default values.
   *
   * @param structType the struct type
   * @return the new struct instance with default field values
   * @throws GcException if creation fails
   */
  StructInstance createStruct(StructType structType) throws GcException;

  /**
   * Creates a new array instance.
   *
   * @param arrayType the array type
   * @param elements the array elements
   * @return the new array instance
   * @throws GcException if creation fails
   */
  ArrayInstance createArray(ArrayType arrayType, List<GcValue> elements) throws GcException;

  /**
   * Creates a new array instance with default values.
   *
   * @param arrayType the array type
   * @param length the array length
   * @return the new array instance with default element values
   * @throws GcException if creation fails
   */
  ArrayInstance createArray(ArrayType arrayType, int length) throws GcException;

  /**
   * Creates a new I31 value.
   *
   * @param value the integer value
   * @return the I31 instance
   * @throws GcException if creation fails
   */
  I31Instance createI31(int value) throws GcException;

  /**
   * Creates a new I31 from an unsigned value (checked).
   *
   * <p>Returns the I31 instance if the value fits in 31 unsigned bits (0 to 2^31-1), otherwise
   * throws.
   *
   * @param value the unsigned value (interpreted as unsigned, must be non-negative)
   * @return the I31 instance
   * @throws GcException if the value is out of range or creation fails
   * @since 1.1.0
   */
  I31Instance createI31Unsigned(int value) throws GcException;

  /**
   * Creates a new I31 from a signed value with wrapping (truncates to 31 bits).
   *
   * @param value the signed integer value
   * @return the I31 instance with the value truncated to 31 bits
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  I31Instance createI31Wrapping(int value) throws GcException;

  /**
   * Creates a new I31 from an unsigned value with wrapping (truncates to 31 bits).
   *
   * @param value the unsigned integer value (interpreted as unsigned)
   * @return the I31 instance with the value truncated to 31 bits
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  I31Instance createI31WrappingUnsigned(int value) throws GcException;

  // ========== Async Object Creation ==========

  /**
   * Asynchronously creates a new struct instance.
   *
   * <p>Uses Wasmtime's async resource limiter path for allocation. The default implementation
   * delegates to the synchronous method. Implementations should override this to use the native
   * async API when available.
   *
   * @param structType the struct type
   * @param fieldValues the initial field values
   * @return the new struct instance
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  default StructInstance createStructAsync(StructType structType, List<GcValue> fieldValues)
      throws GcException {
    return createStruct(structType, fieldValues);
  }

  /**
   * Asynchronously creates a new array instance.
   *
   * <p>Uses Wasmtime's async resource limiter path for allocation. The default implementation
   * delegates to the synchronous method. Implementations should override this to use the native
   * async API when available.
   *
   * @param arrayType the array type
   * @param elements the array elements
   * @return the new array instance
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  default ArrayInstance createArrayAsync(ArrayType arrayType, List<GcValue> elements)
      throws GcException {
    return createArray(arrayType, elements);
  }

  /**
   * Creates a new fixed-length array instance with the given elements.
   *
   * <p>Unlike {@link #createArray(ArrayType, List)}, this method creates an immutable array where
   * all elements are set at creation time and cannot be modified afterward. This corresponds to
   * Wasmtime's {@code ArrayRef::new_fixed} operation.
   *
   * <p>The default implementation delegates to {@link #createArray(ArrayType, List)} since the
   * distinction between fixed and dynamic arrays is a native-level optimization.
   *
   * @param arrayType the array type
   * @param elements the fixed array elements (must match the array type's element type)
   * @return the new fixed array instance
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  default ArrayInstance createArrayFixed(ArrayType arrayType, List<GcValue> elements)
      throws GcException {
    return createArray(arrayType, elements);
  }

  // ========== Field and Element Access ==========

  /**
   * Gets a struct field value.
   *
   * @param struct the struct instance
   * @param fieldIndex the field index
   * @return the field value
   * @throws GcException if field access fails
   */
  GcValue getStructField(StructInstance struct, int fieldIndex) throws GcException;

  /**
   * Sets a struct field value.
   *
   * @param struct the struct instance
   * @param fieldIndex the field index
   * @param value the new value
   * @throws GcException if field assignment fails
   */
  void setStructField(StructInstance struct, int fieldIndex, GcValue value) throws GcException;

  /**
   * Gets an array element value.
   *
   * @param array the array instance
   * @param elementIndex the element index
   * @return the element value
   * @throws GcException if element access fails
   */
  GcValue getArrayElement(ArrayInstance array, int elementIndex) throws GcException;

  /**
   * Sets an array element value.
   *
   * @param array the array instance
   * @param elementIndex the element index
   * @param value the new value
   * @throws GcException if element assignment fails
   */
  void setArrayElement(ArrayInstance array, int elementIndex, GcValue value) throws GcException;

  /**
   * Gets the length of an array.
   *
   * @param array the array instance
   * @return the array length
   */
  int getArrayLength(ArrayInstance array);

  // ========== Advanced Reference Type Operations ==========

  /**
   * Performs a reference type cast with runtime type checking.
   *
   * @param object the object to cast
   * @param targetType the target reference type
   * @return the cast object
   * @throws ClassCastException if the cast is invalid
   */
  GcObject refCast(GcObject object, GcReferenceType targetType);

  /**
   * Performs a reference type cast to a specific struct type.
   *
   * @param object the object to cast
   * @param targetStructType the target struct type
   * @return the cast struct instance
   * @throws ClassCastException if the cast is invalid
   */
  StructInstance refCastStruct(GcObject object, StructType targetStructType);

  /**
   * Performs a reference type cast to a specific array type.
   *
   * @param object the object to cast
   * @param targetArrayType the target array type
   * @return the cast array instance
   * @throws ClassCastException if the cast is invalid
   */
  ArrayInstance refCastArray(GcObject object, ArrayType targetArrayType);

  /**
   * Tests if an object is of a specific reference type.
   *
   * @param object the object to test
   * @param targetType the target type
   * @return true if the object is of the target type
   */
  boolean refTest(GcObject object, GcReferenceType targetType);

  /**
   * Tests if an object is of a specific struct type.
   *
   * @param object the object to test
   * @param targetStructType the target struct type
   * @return true if the object is of the target struct type
   */
  boolean refTestStruct(GcObject object, StructType targetStructType);

  /**
   * Tests if an object is of a specific array type.
   *
   * @param object the object to test
   * @param targetArrayType the target array type
   * @return true if the object is of the target array type
   */
  boolean refTestArray(GcObject object, ArrayType targetArrayType);

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
   * Gets the runtime type information for an object.
   *
   * @param object the object
   * @return the runtime type information
   */
  GcReferenceType getRuntimeType(GcObject object);

  /**
   * Performs a nullable reference cast.
   *
   * @param object the object to cast (may be null)
   * @param targetType the target type
   * @return the cast object or null if input is null
   * @throws ClassCastException if the cast is invalid for non-null objects
   */
  Optional<GcObject> refCastNullable(GcObject object, GcReferenceType targetType);

  /**
   * Copies array elements between arrays with type compatibility checking.
   *
   * @param sourceArray the source array
   * @param sourceIndex the source start index
   * @param destArray the destination array
   * @param destIndex the destination start index
   * @param length the number of elements to copy
   * @throws GcException if copy operation fails
   */
  void copyArrayElements(
      ArrayInstance sourceArray,
      int sourceIndex,
      ArrayInstance destArray,
      int destIndex,
      int length)
      throws GcException;

  /**
   * Fills array elements with a specific value.
   *
   * @param array the array to fill
   * @param startIndex the start index
   * @param length the number of elements to fill
   * @param value the fill value
   * @throws GcException if fill operation fails
   */
  void fillArrayElements(ArrayInstance array, int startIndex, int length, GcValue value)
      throws GcException;

  // ========== Type Registration and Management ==========

  /**
   * Registers a struct type with the runtime.
   *
   * @param structType the struct type to register
   * @return the assigned type ID
   * @throws GcException if registration fails
   */
  int registerStructType(StructType structType) throws GcException;

  /**
   * Registers an array type with the runtime.
   *
   * @param arrayType the array type to register
   * @return the assigned type ID
   * @throws GcException if registration fails
   */
  int registerArrayType(ArrayType arrayType) throws GcException;

  // ========== Raw GC Heap Conversions ==========

  /**
   * Converts an AnyRef (identified by its GC object ID) to a raw GC heap index.
   *
   * <p>This is the host-side equivalent of getting the raw representation of an anyref for use with
   * ValRaw and typed function calls.
   *
   * @param objectId the GC object ID (from {@link GcObject#getObjectId()})
   * @return the raw u32 representation as a long
   * @throws GcException if conversion fails
   * @since 1.1.0
   */
  long anyRefToRaw(long objectId) throws GcException;

  /**
   * Creates an AnyRef from a raw GC heap index.
   *
   * <p>This is the host-side equivalent of creating an anyref from its raw representation. A raw
   * value that decodes to null returns -1.
   *
   * @param raw the raw u32 representation
   * @return the new GC object ID, or -1 if the raw value is null/invalid
   * @throws GcException if creation fails
   * @since 1.1.0
   */
  long anyRefFromRaw(long raw) throws GcException;

  /**
   * Checks if an AnyRef matches a given heap type.
   *
   * @param objectId the GC object ID (from {@link GcObject#getObjectId()})
   * @param heapTypeOrdinal the ordinal of the HeapType enum
   * @return true if the AnyRef matches the heap type
   * @throws GcException if the check fails
   * @since 1.1.0
   */
  boolean anyRefMatchesTy(long objectId, int heapTypeOrdinal) throws GcException;

  /**
   * Converts an AnyRef to an ExternRef via the {@code extern.convert_any} instruction.
   *
   * <p>This is the host-side equivalent of the WebAssembly {@code extern.convert_any} instruction.
   *
   * @param objectId the GC object ID of the AnyRef (from {@link GcObject#getObjectId()})
   * @return the ExternRef's i64 data, or {@link Long#MIN_VALUE} if the conversion yields null
   * @throws GcException if conversion fails
   * @since 1.1.0
   */
  long externRefConvertAny(long objectId) throws GcException;

  /**
   * Converts an ExternRef to an AnyRef via the {@code any.convert_extern} instruction.
   *
   * <p>This is the host-side equivalent of the WebAssembly {@code any.convert_extern} instruction.
   *
   * @param externRefData the ExternRef's i64 data (the Java ExternRef id)
   * @return the new GC object ID for the resulting AnyRef
   * @throws GcException if conversion fails
   * @since 1.1.0
   */
  long anyRefConvertExtern(long externRefData) throws GcException;

  /**
   * Gets the abstract HeapType code for an EqRef.
   *
   * <p>Returns the ordinal of the {@link ai.tegmentum.wasmtime4j.type.HeapType} enum that
   * corresponds to the concrete type of the EqRef (e.g., I31, STRUCT, ARRAY).
   *
   * @param objectId the GC object ID (from {@link GcObject#getObjectId()})
   * @return the HeapType ordinal code
   * @throws GcException if the type check fails
   * @since 1.1.0
   */
  int eqRefTy(long objectId) throws GcException;

  /**
   * Checks if an EqRef matches a given heap type.
   *
   * <p>EqRef can wrap I31, StructRef, or ArrayRef values. This method handles all variants and
   * delegates to the appropriate Wasmtime type check.
   *
   * @param objectId the GC object ID (from {@link GcObject#getObjectId()})
   * @param heapTypeOrdinal the ordinal of the HeapType enum
   * @return true if the EqRef matches the heap type
   * @throws GcException if the check fails
   * @since 1.1.0
   */
  boolean eqRefMatchesTy(long objectId, int heapTypeOrdinal) throws GcException;

  /**
   * Checks if a StructRef matches a given heap type.
   *
   * @param objectId the GC object ID (from {@link GcObject#getObjectId()})
   * @param heapTypeOrdinal the ordinal of the HeapType enum
   * @return true if the StructRef matches the heap type
   * @throws GcException if the check fails
   * @since 1.1.0
   */
  boolean structRefMatchesTy(long objectId, int heapTypeOrdinal) throws GcException;

  /**
   * Checks if an ArrayRef matches a given heap type.
   *
   * @param objectId the GC object ID (from {@link GcObject#getObjectId()})
   * @param heapTypeOrdinal the ordinal of the HeapType enum
   * @return true if the ArrayRef matches the heap type
   * @throws GcException if the check fails
   * @since 1.1.0
   */
  boolean arrayRefMatchesTy(long objectId, int heapTypeOrdinal) throws GcException;

  // ========== Object Lifecycle ==========

  /**
   * Releases a GC object by its ID, allowing the underlying native GC to collect it.
   *
   * <p>This should be called when the Java side no longer needs a reference to a GC object. Without
   * explicit release, GC objects persist in native memory for the lifetime of the {@code
   * GcRuntime}, which can cause memory leaks in long-running processes.
   *
   * @param objectId the object ID to release (from {@link GcObject#getObjectId()})
   * @return {@code true} if the object existed and was released, {@code false} if not found
   * @since 1.1.0
   */
  boolean releaseObject(long objectId);

  // ========== Garbage Collection Control ==========

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
