package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
import ai.tegmentum.wasmtime4j.gc.FieldDefinition;
import ai.tegmentum.wasmtime4j.gc.FieldType;
import ai.tegmentum.wasmtime4j.gc.GcException;
import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcStats;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.gc.I31Instance;
import ai.tegmentum.wasmtime4j.gc.StructInstance;
import ai.tegmentum.wasmtime4j.gc.StructType;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * JNI implementation of WebAssembly GC runtime operations.
 *
 * <p>This class provides actual WebAssembly GC functionality through JNI bindings to the native
 * Wasmtime GC implementation. It supports all GC operations including struct creation, array
 * manipulation, reference type conversions, and heap management.
 *
 * @since 1.0.0
 */
public final class JniGcRuntime implements GcRuntime {

  private static final Logger LOGGER = Logger.getLogger(JniGcRuntime.class.getName());

  private final long nativeHandle;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private volatile boolean disposed = false;

  static {
    NativeLibraryLoader.loadLibrary();
  }

  /**
   * Creates a new JNI GC runtime with the specified engine handle.
   *
   * @param engineHandle the native engine handle
   * @throws JniException if runtime creation fails
   */
  public JniGcRuntime(final long engineHandle) {
    if (engineHandle == 0) {
      throw new JniException("Invalid engine handle");
    }

    this.nativeHandle = createRuntimeNative(engineHandle);
    if (this.nativeHandle == 0) {
      throw new JniException("Failed to create GC runtime");
    }

    LOGGER.fine("Created JNI GC runtime with handle: " + this.nativeHandle);
  }

  /** Javadoc placeholder. */
  public StructInstance createStruct(final StructType structType, final List<GcValue> fieldValues)
      throws GcException {
    validateNotDisposed();
    validateNotNull(structType, "structType");
    validateNotNull(fieldValues, "fieldValues");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      // Register struct type if needed
      final int typeId = registerStructTypeInternal(structType);

      // Convert field values to native format
      final Object[] nativeValues = convertGcValuesToNative(fieldValues);

      // Create struct instance
      final long objectId = structNewNative(nativeHandle, typeId, nativeValues);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create struct instance");
      }

      return new JniStructInstance(this, objectId, structType, typeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public StructInstance createStruct(final StructType structType) throws GcException {
    validateNotDisposed();
    validateNotNull(structType, "structType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      // Register struct type if needed
      final int typeId = registerStructTypeInternal(structType);

      // Create struct with default values
      final long objectId = structNewDefaultNative(nativeHandle, typeId);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create struct instance with default values");
      }

      return new JniStructInstance(this, objectId, structType, typeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public ArrayInstance createArray(final ArrayType arrayType, final List<GcValue> elements)
      throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    validateNotNull(elements, "elements");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      // Register array type if needed
      final int typeId = registerArrayTypeInternal(arrayType);

      // Convert elements to native format
      final Object[] nativeElements = convertGcValuesToNative(elements);

      // Create array instance
      final long objectId = arrayNewNative(nativeHandle, typeId, nativeElements);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create array instance");
      }

      return new JniArrayInstance(this, objectId, arrayType, typeId, elements.size());
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public ArrayInstance createArray(final ArrayType arrayType, final int length) throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    if (length < 0) {
      throw new IllegalArgumentException("Array length cannot be negative: " + length);
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      // Register array type if needed
      final int typeId = registerArrayTypeInternal(arrayType);

      // Create array with default values
      final long objectId = arrayNewDefaultNative(nativeHandle, typeId, length);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create array instance with default values");
      }

      return new JniArrayInstance(this, objectId, arrayType, typeId, length);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public I31Instance createI31(final int value) throws GcException {
    validateNotDisposed();

    // I31 values must fit in 31 bits (signed)
    // Range: -(2^30) to (2^30 - 1) = -1073741824 to 1073741823
    final int minI31 = -(1 << 30); // -1073741824
    final int maxI31 = (1 << 30) - 1; // 1073741823
    if (value < minI31 || value > maxI31) {
      throw new IllegalArgumentException(
          "I31 value out of range: "
              + value
              + " (must be between "
              + minI31
              + " and "
              + maxI31
              + ")");
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = i31NewNative(nativeHandle, value);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create I31 instance");
      }

      return new JniI31Instance(this, objectId, value);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public GcValue getStructField(final StructInstance struct, final int fieldIndex)
      throws GcException {
    validateNotDisposed();
    validateNotNull(struct, "struct");
    if (fieldIndex < 0) {
      throw new IllegalArgumentException("Field index cannot be negative: " + fieldIndex);
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      if (!(struct instanceof JniStructInstance)) {
        throw new IllegalStateException("Invalid struct instance type");
      }

      final JniStructInstance jniStruct = (JniStructInstance) struct;
      final Object nativeValue = structGetNative(nativeHandle, jniStruct.getObjectId(), fieldIndex);

      return convertNativeToGcValue(nativeValue);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public void setStructField(final StructInstance struct, final int fieldIndex, final GcValue value)
      throws GcException {
    validateNotDisposed();
    validateNotNull(struct, "struct");
    validateNotNull(value, "value");
    if (fieldIndex < 0) {
      throw new IllegalArgumentException("Field index cannot be negative: " + fieldIndex);
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      if (!(struct instanceof JniStructInstance)) {
        throw new IllegalStateException("Invalid struct instance type");
      }

      final JniStructInstance jniStruct = (JniStructInstance) struct;
      final Object nativeValue = convertGcValueToNative(value);

      final int result =
          structSetNative(nativeHandle, jniStruct.getObjectId(), fieldIndex, nativeValue);
      if (result != 0) {
        throw new IllegalStateException("Failed to set struct field");
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public GcValue getArrayElement(final ArrayInstance array, final int elementIndex)
      throws GcException {
    validateNotDisposed();
    validateNotNull(array, "array");
    if (elementIndex < 0) {
      throw new IllegalArgumentException("Element index cannot be negative: " + elementIndex);
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      if (!(array instanceof JniArrayInstance)) {
        throw new IllegalStateException("Invalid array instance type");
      }

      final JniArrayInstance jniArray = (JniArrayInstance) array;
      if (elementIndex >= jniArray.getLength()) {
        throw new IndexOutOfBoundsException(
            "Element index out of bounds: " + elementIndex + " >= " + jniArray.getLength());
      }

      final Object nativeValue = arrayGetNative(nativeHandle, jniArray.getObjectId(), elementIndex);
      return convertNativeToGcValue(nativeValue);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public void setArrayElement(
      final ArrayInstance array, final int elementIndex, final GcValue value) throws GcException {
    validateNotDisposed();
    validateNotNull(array, "array");
    validateNotNull(value, "value");
    if (elementIndex < 0) {
      throw new IndexOutOfBoundsException("Element index cannot be negative: " + elementIndex);
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      if (!(array instanceof JniArrayInstance)) {
        throw new IllegalStateException("Invalid array instance type");
      }

      final JniArrayInstance jniArray = (JniArrayInstance) array;
      if (elementIndex >= jniArray.getLength()) {
        throw new IndexOutOfBoundsException(
            "Element index out of bounds: " + elementIndex + " >= " + jniArray.getLength());
      }

      final Object nativeValue = convertGcValueToNative(value);
      final int result =
          arraySetNative(nativeHandle, jniArray.getObjectId(), elementIndex, nativeValue);
      if (result != 0) {
        throw new IllegalStateException("Failed to set array element");
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public int getArrayLength(final ArrayInstance array) {
    validateNotDisposed();
    validateNotNull(array, "array");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      if (!(array instanceof JniArrayInstance)) {
        throw new IllegalArgumentException("Invalid array instance type");
      }

      final JniArrayInstance jniArray = (JniArrayInstance) array;
      final int length = arrayLenNative(nativeHandle, jniArray.getObjectId());
      if (length < 0) {
        throw new IllegalStateException("Failed to get array length");
      }

      return length;
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public GcObject refCast(final GcObject object, final GcReferenceType targetType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetType, "targetType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = convertReferenceTypeToNative(targetType);

      final long castObjectId = refCastNative(nativeHandle, objectId, targetTypeId);
      if (castObjectId == 0) {
        throw new ClassCastException("Reference cast failed");
      }

      return createGcObjectFromId(castObjectId, targetType);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public boolean refTest(final GcObject object, final GcReferenceType targetType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetType, "targetType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = convertReferenceTypeToNative(targetType);

      return refTestNative(nativeHandle, objectId, targetTypeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public boolean refEquals(final GcObject obj1, final GcObject obj2) {
    validateNotDisposed();

    if (obj1 == null && obj2 == null) {
      return true;
    }
    if (obj1 == null || obj2 == null) {
      return false;
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId1 = getObjectId(obj1);
      final long objectId2 = getObjectId(obj2);

      return refEqNative(nativeHandle, objectId1, objectId2);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public boolean isNull(final GcObject object) {
    if (object == null) {
      return true;
    }

    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      return refIsNullNative(nativeHandle, objectId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public int registerStructType(final StructType structType) {
    validateNotDisposed();
    validateNotNull(structType, "structType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      return registerStructTypeInternal(structType);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public int registerArrayType(final ArrayType arrayType) {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      return registerArrayTypeInternal(arrayType);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public GcStats collectGarbage() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeStats = collectGarbageNative(nativeHandle);
      return convertNativeToGcStats(nativeStats);
    } finally {
      lock.readLock().unlock();
    }
  }

  /** Javadoc placeholder. */
  public GcStats getGcStats() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeStats = getGcStatsNative(nativeHandle);
      return convertNativeToGcStats(nativeStats);
    } finally {
      lock.readLock().unlock();
    }
  }

  // ========== Advanced Reference Type Operations ==========

  /**
   * Casts a GC object to a struct instance of the specified type.
   *
   * @param object the object to cast
   * @param targetStructType the target struct type
   * @return the cast struct instance
   * @throws GcException if the runtime is disposed
   * @throws ClassCastException if the cast fails
   * @throws IllegalArgumentException if object or targetStructType is null
   */
  public StructInstance refCastStruct(final GcObject object, final StructType targetStructType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetStructType, "targetStructType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = registerStructTypeInternal(targetStructType);

      final long castObjectId = refCastStructNative(nativeHandle, objectId, targetTypeId);
      if (castObjectId == 0) {
        throw new ClassCastException("Struct cast failed");
      }

      return new JniStructInstance(this, castObjectId, targetStructType, targetTypeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Casts a GC object to an array instance of the specified type.
   *
   * @param object the object to cast
   * @param targetArrayType the target array type
   * @return the cast array instance
   * @throws GcException if the runtime is disposed
   * @throws ClassCastException if the cast fails
   * @throws IllegalArgumentException if object or targetArrayType is null
   */
  public ArrayInstance refCastArray(final GcObject object, final ArrayType targetArrayType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetArrayType, "targetArrayType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = registerArrayTypeInternal(targetArrayType);

      final long castObjectId = refCastArrayNative(nativeHandle, objectId, targetTypeId);
      if (castObjectId == 0) {
        throw new ClassCastException("Array cast failed");
      }

      final int length = arrayLenNative(nativeHandle, castObjectId);
      return new JniArrayInstance(this, castObjectId, targetArrayType, targetTypeId, length);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Tests if a GC object can be cast to the specified struct type.
   *
   * @param object the object to test
   * @param targetStructType the target struct type
   * @return true if the object can be cast to the target type
   * @throws GcException if the runtime is disposed
   * @throws IllegalArgumentException if object or targetStructType is null
   */
  public boolean refTestStruct(final GcObject object, final StructType targetStructType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetStructType, "targetStructType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = registerStructTypeInternal(targetStructType);

      return refTestStructNative(nativeHandle, objectId, targetTypeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Tests if a GC object can be cast to the specified array type.
   *
   * @param object the object to test
   * @param targetArrayType the target array type
   * @return true if the object can be cast to the target type
   * @throws GcException if the runtime is disposed
   * @throws IllegalArgumentException if object or targetArrayType is null
   */
  public boolean refTestArray(final GcObject object, final ArrayType targetArrayType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetArrayType, "targetArrayType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = registerArrayTypeInternal(targetArrayType);

      return refTestArrayNative(nativeHandle, objectId, targetTypeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Gets the runtime type of a GC object.
   *
   * @param object the object to query
   * @return the runtime reference type of the object
   * @throws GcException if the runtime is disposed
   * @throws IllegalArgumentException if object is null
   */
  public GcReferenceType getRuntimeType(final GcObject object) {
    validateNotDisposed();
    validateNotNull(object, "object");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int typeId = getRuntimeTypeNative(nativeHandle, objectId);

      return convertNativeToReferenceType(typeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Casts a GC object to the specified type, returning empty if the object is null.
   *
   * @param object the object to cast (may be null)
   * @param targetType the target reference type
   * @return an Optional containing the cast object, or empty if the object is null
   * @throws ClassCastException if the cast fails for non-null objects
   */
  public Optional<GcObject> refCastNullable(
      final GcObject object, final GcReferenceType targetType) {
    if (object == null || isNull(object)) {
      return Optional.empty();
    }

    try {
      return Optional.of(refCast(object, targetType));
    } catch (ClassCastException e) {
      throw e; // Re-throw cast exceptions for non-null objects
    }
  }

  /**
   * Copies elements from one array to another.
   *
   * @param sourceArray the source array
   * @param sourceIndex the starting index in the source array
   * @param destArray the destination array
   * @param destIndex the starting index in the destination array
   * @param length the number of elements to copy
   * @throws GcException if the runtime is disposed or the operation fails
   * @throws IllegalArgumentException if sourceArray or destArray is null
   */
  public void copyArrayElements(
      final ArrayInstance sourceArray,
      final int sourceIndex,
      final ArrayInstance destArray,
      final int destIndex,
      final int length) {
    validateNotDisposed();
    validateNotNull(sourceArray, "sourceArray");
    validateNotNull(destArray, "destArray");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long sourceObjectId = getObjectId(sourceArray);
      final long destObjectId = getObjectId(destArray);

      final int result =
          arrayCopyNative(
              nativeHandle, sourceObjectId, sourceIndex, destObjectId, destIndex, length);
      if (result != 0) {
        throw new IllegalStateException("Failed to copy array elements");
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Fills array elements with a specific value.
   *
   * @param array the array to fill
   * @param startIndex the starting index
   * @param length the number of elements to fill
   * @param value the value to fill with
   * @throws GcException if the runtime is disposed or the operation fails
   * @throws IllegalArgumentException if array or value is null
   */
  public void fillArrayElements(
      final ArrayInstance array, final int startIndex, final int length, final GcValue value) {
    validateNotDisposed();
    validateNotNull(array, "array");
    validateNotNull(value, "value");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(array);
      final Object nativeValue = convertGcValueToNative(value);

      final int result = arrayFillNative(nativeHandle, objectId, startIndex, length, nativeValue);
      if (result != 0) {
        throw new IllegalStateException("Failed to fill array elements");
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Disposes this GC runtime and releases native resources.
   *
   * @throws JniException if disposal fails
   */
  public void dispose() {
    lock.writeLock().lock();
    try {
      if (!disposed) {
        destroyRuntimeNative(nativeHandle);
        disposed = true;
        LOGGER.fine("Disposed JNI GC runtime with handle: " + nativeHandle);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Private helper methods

  private void validateNotDisposed() {
    if (disposed) {
      throw new IllegalStateException("GC runtime has been disposed");
    }
  }

  private void validateNotNull(final Object obj, final String paramName) {
    if (obj == null) {
      throw new IllegalArgumentException(paramName + " cannot be null");
    }
  }

  private int registerStructTypeInternal(final StructType structType) {
    final String[] fieldNames =
        structType.getFields().stream()
            .map(field -> field.getName() != null ? field.getName() : "")
            .toArray(String[]::new);

    final String[] fieldTypes =
        structType.getFields().stream()
            .map(field -> field.getFieldType().toString())
            .toArray(String[]::new);

    final Boolean[] fieldMutabilities =
        structType.getFields().stream().map(FieldDefinition::isMutable).toArray(Boolean[]::new);

    final int typeId =
        registerStructTypeNative(
            nativeHandle,
            structType.getName() != null ? structType.getName() : "",
            fieldNames,
            fieldTypes,
            fieldMutabilities);

    if (typeId < 0) {
      throw new IllegalStateException("Failed to register struct type");
    }

    return typeId;
  }

  private int registerArrayTypeInternal(final ArrayType arrayType) {
    final int elementTypeId = convertFieldTypeToNative(arrayType.getElementType());

    final int typeId =
        registerArrayTypeNative(
            nativeHandle,
            arrayType.getName() != null ? arrayType.getName() : "",
            elementTypeId,
            arrayType.isMutable());

    if (typeId < 0) {
      throw new IllegalStateException("Failed to register array type");
    }

    return typeId;
  }

  private Object[] convertGcValuesToNative(final List<GcValue> values) {
    return values.stream().map(this::convertGcValueToNative).toArray();
  }

  private Object convertGcValueToNative(final GcValue value) {
    final GcValue.Type valueType = value.getType();
    switch (valueType) {
      case I32:
        return value.asI32();
      case I64:
        return value.asI64();
      case F32:
        return value.asF32();
      case F64:
        return value.asF64();
      case V128:
        return value.asV128();
      case NULL:
        // Explicit null value
        return null;
      case REFERENCE:
        // Extract object ID from the GcObject reference
        final ai.tegmentum.wasmtime4j.gc.GcObject gcObject = value.asReference();
        if (gcObject == null) {
          // Null reference is valid
          return null;
        }
        // Check if it's a JniGcObject and extract the object ID
        if (gcObject instanceof JniGcObject) {
          // CRITICAL: Must return Long object, not long primitive, so JNI can convert to jobject
          return Long.valueOf(((JniGcObject) gcObject).getObjectId());
        }
        // If it's not a JniGcObject, this is a critical error
        throw new IllegalStateException(
            "BUG: convertGcValueToNative received GcObject that is not JniGcObject! "
                + "Type="
                + gcObject.getClass().getName()
                + ", Superclass="
                + (gcObject.getClass().getSuperclass() != null
                    ? gcObject.getClass().getSuperclass().getName()
                    : "null")
                + ", Implemented interfaces="
                + Arrays.toString(gcObject.getClass().getInterfaces()));
      default:
        throw new IllegalStateException("Unsupported GC value type: " + valueType);
    }
  }

  private GcValue convertNativeToGcValue(final Object nativeValue) {
    if (nativeValue == null) {
      return GcValue.nullValue();
    }

    // Check if this is a GC object reference marker
    if (nativeValue instanceof GcReferenceMarker) {
      final GcReferenceMarker marker = (GcReferenceMarker) nativeValue;
      final long objectId = marker.getObjectId();
      // Create a generic GC object wrapper for the reference
      // We don't know if it's a struct or array at this point, so create a generic wrapper
      final JniGcObject gcObject =
          new JniGcObject(objectId) {
            @Override
            public ai.tegmentum.wasmtime4j.gc.GcReferenceType getReferenceType() {
              // Return ANYREF as we don't know the specific type
              return ai.tegmentum.wasmtime4j.gc.GcReferenceType.ANY_REF;
            }
          };
      return GcValue.reference(gcObject);
    }

    if (nativeValue instanceof Integer) {
      return GcValue.i32((Integer) nativeValue);
    } else if (nativeValue instanceof Long) {
      return GcValue.i64((Long) nativeValue);
    } else if (nativeValue instanceof Float) {
      return GcValue.f32((Float) nativeValue);
    } else if (nativeValue instanceof Double) {
      return GcValue.f64((Double) nativeValue);
    } else if (nativeValue instanceof byte[]) {
      return GcValue.v128((byte[]) nativeValue);
    } else {
      return GcValue.nullValue();
    }
  }

  private int convertFieldTypeToNative(final FieldType fieldType) {
    switch (fieldType.getKind()) {
      case I32:
        return 0;
      case I64:
        return 1;
      case F32:
        return 2;
      case F64:
        return 3;
      case V128:
        return 4;
      case PACKED_I8:
        return 5;
      case PACKED_I16:
        return 6;
      case REFERENCE:
        return 7;
      default:
        throw new IllegalStateException("Unsupported field type: " + fieldType.getKind());
    }
  }

  private int convertReferenceTypeToNative(final GcReferenceType refType) {
    switch (refType) {
      case ANY_REF:
        return 0;
      case EQ_REF:
        return 1;
      case I31_REF:
        return 2;
      case STRUCT_REF:
        return 3;
      case ARRAY_REF:
        return 4;
      default:
        throw new IllegalStateException("Unsupported reference type: " + refType);
    }
  }

  private long getObjectId(final GcObject object) {
    // All GcObject implementations have getObjectId() method
    return object.getObjectId();
  }

  private GcObject createGcObjectFromId(final long objectId, final GcReferenceType type) {
    switch (type) {
      case I31_REF:
        final int value = i31GetNative(nativeHandle, objectId, true);
        return new JniI31Instance(this, objectId, value);
      case STRUCT_REF:
      case ARRAY_REF:
        // Would need type information to create proper struct/array instance
        return new JniGcObject(objectId);
      default:
        return new JniGcObject(objectId);
    }
  }

  private GcStats convertNativeToGcStats(final Object nativeStats) {
    if (nativeStats == null) {
      // Return default stats if native stats unavailable
      return GcStats.builder()
          .totalAllocated(0)
          .totalCollected(0)
          .bytesAllocated(0)
          .bytesCollected(0)
          .minorCollections(0)
          .majorCollections(0)
          .currentHeapSize(0)
          .peakHeapSize(0)
          .maxHeapSize(0)
          .build();
    }

    // Handle GcCollectionResult (from collectGarbage operations)
    if (nativeStats instanceof ai.tegmentum.wasmtime4j.gc.GcCollectionResult) {
      final ai.tegmentum.wasmtime4j.gc.GcCollectionResult collectionResult =
          (ai.tegmentum.wasmtime4j.gc.GcCollectionResult) nativeStats;

      return GcStats.builder()
          .totalAllocated(0) // Not tracked in collection result
          .totalCollected(collectionResult.getObjectsCollected())
          .bytesAllocated(0) // Not tracked in collection result
          .bytesCollected(collectionResult.getBytesCollected())
          .minorCollections(0) // Not tracked in collection result
          .majorCollections(1) // Count this collection
          .currentHeapSize(0) // Not tracked in collection result
          .peakHeapSize(0) // Not tracked in collection result
          .maxHeapSize(0) // Not tracked in collection result
          .build();
    }

    // Handle GcHeapStats (from getGcStats operations)
    if (nativeStats instanceof ai.tegmentum.wasmtime4j.gc.GcHeapStats) {
      final ai.tegmentum.wasmtime4j.gc.GcHeapStats heapStats =
          (ai.tegmentum.wasmtime4j.gc.GcHeapStats) nativeStats;

      long totalAlloc = heapStats.getTotalAllocated();
      long currentHeap = heapStats.getCurrentHeapSize();
      long majorColl = heapStats.getMajorCollections();

      return GcStats.builder()
          .totalAllocated(totalAlloc)
          .totalCollected(0) // Not tracked by GcHeapStats
          .bytesAllocated(totalAlloc) // Use totalAllocated as bytes
          .bytesCollected(0) // Not tracked by GcHeapStats
          .minorCollections(0) // Not tracked by GcHeapStats
          .majorCollections(majorColl)
          .currentHeapSize((int) currentHeap)
          .peakHeapSize(0) // Not tracked by GcHeapStats
          .maxHeapSize(0) // Not tracked by GcHeapStats
          .build();
    }

    // Handle HashMap (from native JNI implementation)
    if (nativeStats instanceof java.util.Map) {
      @SuppressWarnings("unchecked")
      final java.util.Map<String, Object> statsMap = (java.util.Map<String, Object>) nativeStats;

      // Handle both collection result (objectsCollected) and stats (totalAllocated)
      final long totalAlloc = getLongValue(statsMap, "totalAllocated", 0L);
      final long objCollected = getLongValue(statsMap, "objectsCollected", 0L);
      final long bytesCollected = getLongValue(statsMap, "bytesCollected", 0L);
      final long majorColl = getLongValue(statsMap, "majorCollections", 0L);
      final long currentHeap = getLongValue(statsMap, "currentHeapSize", 0L);

      return GcStats.builder()
          .totalAllocated(totalAlloc)
          .totalCollected(objCollected)
          .bytesAllocated(totalAlloc) // Use totalAllocated as bytesAllocated
          .bytesCollected(bytesCollected)
          .minorCollections(getLongValue(statsMap, "minorCollections", 0L))
          .majorCollections(majorColl > 0 ? majorColl : (objCollected > 0 ? 1 : 0))
          .currentHeapSize((int) currentHeap)
          .peakHeapSize((int) getLongValue(statsMap, "peakHeapSize", 0L))
          .maxHeapSize((int) getLongValue(statsMap, "maxHeapSize", 0L))
          .build();
    }

    // Unknown type - return defaults
    return GcStats.builder()
        .totalAllocated(0)
        .totalCollected(0)
        .bytesAllocated(0)
        .bytesCollected(0)
        .minorCollections(0)
        .majorCollections(0)
        .currentHeapSize(0)
        .peakHeapSize(0)
        .maxHeapSize(0)
        .build();
  }

  private static long getLongValue(
      final java.util.Map<String, Object> map, final String key, final long defaultValue) {
    final Object value = map.get(key);
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return defaultValue;
  }

  private GcReferenceType convertNativeToReferenceType(final int typeId) {
    switch (typeId) {
      case 0:
        return GcReferenceType.ANY_REF;
      case 1:
        return GcReferenceType.EQ_REF;
      case 2:
        return GcReferenceType.I31_REF;
      case 3:
        return GcReferenceType.STRUCT_REF;
      case 4:
        return GcReferenceType.ARRAY_REF;
      default:
        throw new IllegalStateException("Unknown reference type ID: " + typeId);
    }
  }

  // Native method declarations

  private static native long createRuntimeNative(long engineHandle);

  private static native void destroyRuntimeNative(long runtimeHandle);

  private static native int registerStructTypeNative(
      long runtimeHandle,
      String name,
      String[] fieldNames,
      String[] fieldTypes,
      Boolean[] fieldMutabilities);

  private static native int registerArrayTypeNative(
      long runtimeHandle, String name, int elementType, boolean mutable);

  private static native long structNewNative(long runtimeHandle, int typeId, Object[] fieldValues);

  private static native long structNewDefaultNative(long runtimeHandle, int typeId);

  private static native Object structGetNative(long runtimeHandle, long objectId, int fieldIndex);

  private static native int structSetNative(
      long runtimeHandle, long objectId, int fieldIndex, Object value);

  private static native long arrayNewNative(long runtimeHandle, int typeId, Object[] elements);

  private static native long arrayNewDefaultNative(long runtimeHandle, int typeId, int length);

  private static native Object arrayGetNative(long runtimeHandle, long objectId, int elementIndex);

  private static native int arraySetNative(
      long runtimeHandle, long objectId, int elementIndex, Object value);

  private static native int arrayLenNative(long runtimeHandle, long objectId);

  private static native long i31NewNative(long runtimeHandle, int value);

  private static native int i31GetNative(long runtimeHandle, long objectId, boolean signed);

  private static native long refCastNative(long runtimeHandle, long objectId, int targetType);

  private static native boolean refTestNative(long runtimeHandle, long objectId, int targetType);

  private static native boolean refEqNative(long runtimeHandle, long objectId1, long objectId2);

  private static native boolean refIsNullNative(long runtimeHandle, long objectId);

  private static native Object collectGarbageNative(long runtimeHandle);

  private static native Object getGcStatsNative(long runtimeHandle);

  // Advanced reference type operations
  private static native long refCastStructNative(
      long runtimeHandle, long objectId, int targetTypeId);

  private static native long refCastArrayNative(
      long runtimeHandle, long objectId, int targetTypeId);

  private static native boolean refTestStructNative(
      long runtimeHandle, long objectId, int targetTypeId);

  private static native boolean refTestArrayNative(
      long runtimeHandle, long objectId, int targetTypeId);

  private static native int getRuntimeTypeNative(long runtimeHandle, long objectId);

  private static native int arrayCopyNative(
      long runtimeHandle,
      long sourceObjectId,
      int sourceIndex,
      long destObjectId,
      int destIndex,
      int length);

  private static native int arrayFillNative(
      long runtimeHandle, long objectId, int startIndex, int length, Object value);

  // Helper classes for JNI GC objects

  private static class JniGcObject implements GcObject {
    private final long objectId;

    public JniGcObject(final long objectId) {
      this.objectId = objectId;
    }

    @Override
    public long getObjectId() {
      return objectId;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.ANY_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      return getReferenceType().isSubtypeOf(type);
    }

    @Override
    public GcObject castTo(final GcReferenceType type) {
      if (!isOfType(type)) {
        throw new ClassCastException(
            "Cannot cast " + getReferenceType() + " to " + type
                + ": incompatible reference types");
      }
      return this;
    }

    @Override
    public boolean refEquals(final GcObject other) {
      if (other instanceof JniGcObject) {
        return ((JniGcObject) other).objectId == this.objectId;
      }
      return false;
    }

    @Override
    public int getSizeBytes() {
      return 0; // Size would need to be queried from native side
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      // GC objects are reference types in WebAssembly
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }
  }

  /** JNI implementation of struct instance for GC runtime. */
  public static class JniStructInstance extends JniGcObject implements StructInstance {
    private final JniGcRuntime runtime;
    private final StructType structType;
    private final int typeId;

    /**
     * Creates a new JNI struct instance.
     *
     * @param runtime the GC runtime
     * @param objectId the object ID
     * @param structType the struct type
     * @param typeId the type ID
     */
    public JniStructInstance(
        final JniGcRuntime runtime,
        final long objectId,
        final StructType structType,
        final int typeId) {
      super(objectId);
      this.runtime = runtime;
      this.structType = structType;
      this.typeId = typeId;
    }

    @Override
    public StructType getType() {
      return structType;
    }

    @Override
    public int getFieldCount() {
      return structType.getFieldCount();
    }

    @Override
    public GcValue getField(final int index) throws GcException {
      return runtime.getStructField(this, index);
    }

    @Override
    public void setField(final int index, final GcValue value) throws GcException {
      runtime.setStructField(this, index, value);
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }

    @Override
    public int getSizeBytes() {
      // Return approximate size based on struct fields
      return 16 + (structType.getFieldCount() * 8);
    }

    @Override
    public boolean refEquals(final GcObject other) {
      return runtime.refEquals(this, other);
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.STRUCT_REF;
    }
  }

  /** JNI implementation of array instance for GC runtime. */
  public static class JniArrayInstance extends JniGcObject implements ArrayInstance {
    private final JniGcRuntime runtime;
    private final ArrayType arrayType;
    private final int typeId;
    private final int length;

    /**
     * Creates a new JNI array instance.
     *
     * @param runtime the GC runtime
     * @param objectId the object ID
     * @param arrayType the array type
     * @param typeId the type ID
     * @param length the array length
     */
    public JniArrayInstance(
        final JniGcRuntime runtime,
        final long objectId,
        final ArrayType arrayType,
        final int typeId,
        final int length) {
      super(objectId);
      this.runtime = runtime;
      this.arrayType = arrayType;
      this.typeId = typeId;
      this.length = length;
    }

    @Override
    public ArrayType getType() {
      return arrayType;
    }

    @Override
    public int getLength() {
      return length;
    }

    @Override
    public GcValue getElement(final int index) throws GcException {
      return runtime.getArrayElement(this, index);
    }

    @Override
    public void setElement(final int index, final GcValue value) throws GcException {
      runtime.setArrayElement(this, index, value);
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }

    @Override
    public int getSizeBytes() {
      // Return approximate size based on array length and element size
      return 16 + (length * 8);
    }

    @Override
    public boolean refEquals(final GcObject other) {
      return runtime.refEquals(this, other);
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.ARRAY_REF;
    }
  }

  /** JNI implementation of i31 instance for GC runtime. */
  public static class JniI31Instance extends JniGcObject implements I31Instance {
    private final JniGcRuntime runtime;
    private final int value;

    /**
     * Creates a new JNI i31 instance.
     *
     * @param runtime the GC runtime
     * @param objectId the object ID
     * @param value the i31 value
     */
    public JniI31Instance(final JniGcRuntime runtime, final long objectId, final int value) {
      super(objectId);
      this.runtime = runtime;
      this.value = value;
    }

    @Override
    public int getValue() {
      return value;
    }

    @Override
    public int getSignedValue() {
      return value;
    }

    @Override
    public int getUnsignedValue() {
      return value & 0x7FFFFFFF;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }

    @Override
    public int getSizeBytes() {
      // I31 values are stored inline, minimal overhead
      return 4;
    }

    @Override
    public boolean refEquals(final GcObject other) {
      return runtime.refEquals(this, other);
    }

    @Override
    public ai.tegmentum.wasmtime4j.gc.GcReferenceType getReferenceType() {
      return ai.tegmentum.wasmtime4j.gc.GcReferenceType.I31_REF;
    }

  }

  /**
   * Internal marker class used by JNI to indicate that a returned value is a GC object reference
   * (object ID) rather than a primitive value. This class is package-private and only used
   * internally by the JNI bindings to distinguish between i64 values and object IDs.
   */
  static class GcReferenceMarker {
    private final long objectId;

    GcReferenceMarker(final long objectId) {
      this.objectId = objectId;
    }

    long getObjectId() {
      return objectId;
    }
  }
}
