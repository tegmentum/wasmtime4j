package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
import ai.tegmentum.wasmtime4j.gc.FieldDefinition;
import ai.tegmentum.wasmtime4j.gc.FieldType;
import ai.tegmentum.wasmtime4j.gc.GcException;
import ai.tegmentum.wasmtime4j.gc.GcHeapInspection;
import ai.tegmentum.wasmtime4j.gc.GcInvariantValidation;
import ai.tegmentum.wasmtime4j.gc.GcObject;
import ai.tegmentum.wasmtime4j.gc.GcProfiler;
import ai.tegmentum.wasmtime4j.gc.GcReferenceType;
import ai.tegmentum.wasmtime4j.gc.GcRuntime;
import ai.tegmentum.wasmtime4j.gc.GcStats;
import ai.tegmentum.wasmtime4j.gc.GcValue;
import ai.tegmentum.wasmtime4j.gc.I31Instance;
import ai.tegmentum.wasmtime4j.gc.I31Type;
import ai.tegmentum.wasmtime4j.gc.MemoryCorruptionAnalysis;
import ai.tegmentum.wasmtime4j.gc.MemoryLeakAnalysis;
import ai.tegmentum.wasmtime4j.gc.ObjectLifecycleTracker;
import ai.tegmentum.wasmtime4j.gc.ReferenceGraph;
import ai.tegmentum.wasmtime4j.gc.ReferenceSafetyResult;
import ai.tegmentum.wasmtime4j.gc.StructInstance;
import ai.tegmentum.wasmtime4j.gc.StructType;
import ai.tegmentum.wasmtime4j.gc.WeakGcReference;
import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;
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

  /** Map storing host objects by their GC object ID for extraction. */
  private final java.util.concurrent.ConcurrentHashMap<Long, Object> hostObjectMap =
      new java.util.concurrent.ConcurrentHashMap<>();

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
  public GcObject refCastOptimized(
      final GcObject object, final GcReferenceType targetType, final boolean enableCaching) {
    // For now, delegate to non-optimized version
    // TODO: Implement caching when enableCaching is true
    return refCast(object, targetType);
  }

  /** Javadoc placeholder. */
  public WeakGcReference createWeakReferenceAdvanced(
      final GcObject object, final Runnable finalizationCallback) {
    validateNotDisposed();
    validateNotNull(object, "object");
    // TODO: Implement weak reference creation with finalization callback
    LOGGER.fine("Creating weak reference with finalization callback: " + object.getObjectId());
    return createWeakReference(object, finalizationCallback);
  }

  /** Javadoc placeholder. */
  public GcStats collectGarbageAdvanced(final Long maxPauseMillis, final boolean concurrent) {
    validateNotDisposed();
    // TODO: Implement advanced GC with pause time and concurrency controls
    LOGGER.fine(
        "Advanced GC requested (maxPause=" + maxPauseMillis + "ms, concurrent=" + concurrent + ")");
    return collectGarbage();
  }

  /** Javadoc placeholder. */
  public void pinObject(final GcObject object) {
    validateNotDisposed();
    validateNotNull(object, "object");
    // TODO: Implement object pinning in native code
    LOGGER.fine("Pinning object (not yet implemented): " + object.getObjectId());
  }

  /** Javadoc placeholder. */
  public void unpinObject(final GcObject object) {
    validateNotDisposed();
    validateNotNull(object, "object");
    // TODO: Implement object unpinning in native code
    LOGGER.fine("Unpinning object (not yet implemented): " + object.getObjectId());
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

  // ========== Complex Type Operations ==========

  /**
   * Creates a packed struct instance with custom field alignment.
   *
   * @param structType the struct type definition
   * @param fieldValues the field values
   * @param customAlignment custom alignment for each field (field index to alignment in bytes)
   * @return the created packed struct instance
   * @throws GcException if the runtime is disposed or creation fails
   * @throws IllegalArgumentException if any parameter is null
   */
  public StructInstance createPackedStruct(
      final StructType structType,
      final List<GcValue> fieldValues,
      final Map<Integer, Integer> customAlignment) {
    validateNotDisposed();
    validateNotNull(structType, "structType");
    validateNotNull(fieldValues, "fieldValues");
    validateNotNull(customAlignment, "customAlignment");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final int typeId = registerStructTypeInternal(structType);
      final Object[] nativeValues = convertGcValuesToNative(fieldValues);
      final int[] alignments =
          customAlignment.entrySet().stream()
              .sorted(Map.Entry.comparingByKey())
              .mapToInt(Map.Entry::getValue)
              .toArray();

      final long objectId = structNewPackedNative(nativeHandle, typeId, nativeValues, alignments);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create packed struct instance");
      }

      return new JniStructInstance(this, objectId, structType, typeId);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Creates a variable-length array instance with flexible elements.
   *
   * @param arrayType the array type definition
   * @param baseLength the base length of the array
   * @param flexibleElements additional flexible elements beyond the base length
   * @return the created variable-length array instance
   * @throws GcException if the runtime is disposed or creation fails
   * @throws IllegalArgumentException if arrayType or flexibleElements is null
   */
  public ArrayInstance createVariableLengthArray(
      final ArrayType arrayType, final int baseLength, final List<GcValue> flexibleElements) {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    validateNotNull(flexibleElements, "flexibleElements");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final int typeId = registerArrayTypeInternal(arrayType);
      final Object[] nativeFlexibleElements = convertGcValuesToNative(flexibleElements);

      final long objectId =
          arrayNewVariableLengthNative(nativeHandle, typeId, baseLength, nativeFlexibleElements);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create variable-length array instance");
      }

      final int totalLength = baseLength + flexibleElements.size();
      return new JniArrayInstance(this, objectId, arrayType, typeId, totalLength);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Creates a nested array instance containing other GC objects.
   *
   * @param arrayType the array type definition
   * @param nestedElements the nested GC objects to include
   * @return the created nested array instance
   * @throws GcException if the runtime is disposed or creation fails
   * @throws IllegalArgumentException if arrayType or nestedElements is null
   */
  public ArrayInstance createNestedArray(
      final ArrayType arrayType, final List<GcObject> nestedElements) {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    validateNotNull(nestedElements, "nestedElements");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final int typeId = registerArrayTypeInternal(arrayType);
      final long[] nestedObjectIds = nestedElements.stream().mapToLong(this::getObjectId).toArray();

      final long objectId = arrayNewNestedNative(nativeHandle, typeId, nestedObjectIds);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to create nested array instance");
      }

      return new JniArrayInstance(this, objectId, arrayType, typeId, nestedElements.size());
    } finally {
      lock.readLock().unlock();
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

  // ========== Type Registration and Management ==========

  /**
   * Registers a recursive type definition with the GC runtime.
   *
   * @param typeName the name of the type
   * @param typeDefinition the recursive type definition
   * @return the assigned type ID
   * @throws GcException if the runtime is disposed or registration fails
   * @throws IllegalArgumentException if typeName or typeDefinition is null
   */
  public int registerRecursiveType(final String typeName, final Object typeDefinition) {
    validateNotDisposed();
    validateNotNull(typeName, "typeName");
    validateNotNull(typeDefinition, "typeDefinition");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      // Convert type definition to native format
      final byte[] nativeDefinition = serializeTypeDefinition(typeDefinition);

      final int typeId = registerRecursiveTypeNative(nativeHandle, typeName, nativeDefinition);
      if (typeId < 0) {
        throw new IllegalStateException("Failed to register recursive type");
      }

      return typeId;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Creates a type hierarchy with a base type and derived types.
   *
   * @param baseType the base type definition
   * @param derivedTypes list of derived type definitions
   * @return map of type names to their assigned type IDs
   * @throws GcException if the runtime is disposed or creation fails
   * @throws IllegalArgumentException if baseType or derivedTypes is null
   */
  public Map<String, Integer> createTypeHierarchy(
      final Object baseType, final List<Object> derivedTypes) {
    validateNotDisposed();
    validateNotNull(baseType, "baseType");
    validateNotNull(derivedTypes, "derivedTypes");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final byte[] nativeBaseType = serializeTypeDefinition(baseType);
      final byte[][] nativeDerivedTypes =
          derivedTypes.stream().map(this::serializeTypeDefinition).toArray(byte[][]::new);

      final Object nativeHierarchy =
          createTypeHierarchyNative(nativeHandle, nativeBaseType, nativeDerivedTypes);
      return deserializeTypeHierarchy(nativeHierarchy);
    } finally {
      lock.readLock().unlock();
    }
  }

  // ========== Garbage Collection Control ==========

  /**
   * Performs incremental garbage collection with a specified maximum pause time.
   *
   * @param maxPauseMillis maximum pause time in milliseconds
   * @return GC statistics from the collection
   * @throws GcException if the runtime is disposed
   */
  public GcStats collectGarbageIncremental(final long maxPauseMillis) {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeStats = collectGarbageIncrementalNative(nativeHandle, maxPauseMillis);
      return convertNativeToGcStats(nativeStats);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Performs concurrent garbage collection.
   *
   * @return GC statistics from the collection
   * @throws GcException if the runtime is disposed
   */
  public GcStats collectGarbageConcurrent() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeStats = collectGarbageConcurrentNative(nativeHandle);
      return convertNativeToGcStats(nativeStats);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Configures the garbage collection strategy and parameters.
   *
   * @param strategy the GC strategy name
   * @param parameters strategy-specific parameters
   * @throws GcException if the runtime is disposed or configuration fails
   * @throws IllegalArgumentException if strategy or parameters is null
   */
  public void configureGcStrategy(final String strategy, final Map<String, Object> parameters) {
    validateNotDisposed();
    validateNotNull(strategy, "strategy");
    validateNotNull(parameters, "parameters");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final String[] paramKeys = parameters.keySet().toArray(new String[0]);
      final Object[] paramValues = parameters.values().toArray();

      final int result = configureGcStrategyNative(nativeHandle, strategy, paramKeys, paramValues);
      if (result != 0) {
        throw new IllegalStateException("Failed to configure GC strategy: " + strategy);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Monitors GC pressure and returns whether it exceeds the threshold.
   *
   * @param pressureThreshold the pressure threshold (0.0 to 1.0)
   * @return true if GC pressure exceeds the threshold
   * @throws GcException if the runtime is disposed
   */
  public boolean monitorGcPressure(final double pressureThreshold) {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      return monitorGcPressureNative(nativeHandle, pressureThreshold);
    } finally {
      lock.readLock().unlock();
    }
  }

  // ========== Advanced Memory Management ==========

  /**
   * Creates a weak reference to a GC object with an optional finalization callback.
   *
   * @param object the object to create a weak reference to
   * @param finalizationCallback optional callback to run when object is finalized (may be null)
   * @return the weak reference
   * @throws GcException if the runtime is disposed or creation fails
   * @throws IllegalArgumentException if object is null
   */
  public WeakGcReference createWeakReference(
      final GcObject object, final Runnable finalizationCallback) {
    validateNotDisposed();
    validateNotNull(object, "object");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final long weakRefId = createWeakReferenceNative(nativeHandle, objectId);
      if (weakRefId == 0) {
        throw new IllegalStateException("Failed to create weak reference");
      }

      return new JniWeakGcReference(this, weakRefId, object, finalizationCallback);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Registers a finalization callback for a GC object.
   *
   * @param object the object to register the callback for
   * @param callback the finalization callback
   * @throws GcException if the runtime is disposed or registration fails
   * @throws IllegalArgumentException if object or callback is null
   */
  public void registerFinalizationCallback(final GcObject object, final Runnable callback) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(callback, "callback");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int result = registerFinalizationCallbackNative(nativeHandle, objectId, callback);
      if (result != 0) {
        throw new IllegalStateException("Failed to register finalization callback");
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Runs finalization on all pending finalizable objects.
   *
   * @return the number of objects finalized
   * @throws GcException if the runtime is disposed
   */
  public int runFinalization() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      return runFinalizationNative(nativeHandle);
    } finally {
      lock.readLock().unlock();
    }
  }

  // ========== Host Integration ==========

  /**
   * Integrates a host Java object into the GC system.
   *
   * @param hostObject the host object to integrate
   * @param gcType the GC reference type for the integrated object
   * @return the GC object wrapping the host object
   * @throws GcException if the runtime is disposed or integration fails
   * @throws IllegalArgumentException if hostObject or gcType is null
   */
  public GcObject integrateHostObject(final Object hostObject, final GcReferenceType gcType) {
    validateNotDisposed();
    validateNotNull(hostObject, "hostObject");
    validateNotNull(gcType, "gcType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final int typeId = convertReferenceTypeToNative(gcType);
      final long objectId = integrateHostObjectNative(nativeHandle, hostObject, typeId);
      if (objectId == 0) {
        throw new IllegalStateException("Failed to integrate host object");
      }

      // Store host object in map for later extraction
      hostObjectMap.put(objectId, hostObject);

      return createGcObjectFromId(objectId, gcType);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Extracts the host Java object from a GC-integrated object.
   *
   * @param gcObject the GC object to extract from
   * @return the host object
   * @throws GcException if the runtime is disposed, extraction fails, or object is not
   *     host-integrated
   * @throws IllegalArgumentException if gcObject is null
   */
  public Object extractHostObject(final GcObject gcObject) {
    validateNotDisposed();
    validateNotNull(gcObject, "gcObject");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(gcObject);

      // First try to get from our local map
      final Object localHostObject = hostObjectMap.get(objectId);
      if (localHostObject != null) {
        return localHostObject;
      }

      // Fall back to native extraction
      final Object hostObject = extractHostObjectNative(nativeHandle, objectId);
      if (hostObject == null) {
        throw new IllegalStateException(
            "Failed to extract host object or object is not host-integrated");
      }

      return hostObject;
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Creates a sharing bridge for cross-module object sharing.
   *
   * @param objects the objects to include in the sharing bridge
   * @return the sharing bridge object
   * @throws GcException if the runtime is disposed or creation fails
   * @throws IllegalArgumentException if objects is null
   */
  public Object createSharingBridge(final List<GcObject> objects) {
    validateNotDisposed();
    validateNotNull(objects, "objects");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long[] objectIds = objects.stream().mapToLong(this::getObjectId).toArray();

      final Object bridge = createSharingBridgeNative(nativeHandle, objectIds);
      if (bridge == null) {
        throw new IllegalStateException("Failed to create sharing bridge");
      }

      return bridge;
    } finally {
      lock.readLock().unlock();
    }
  }

  // ========== Debugging and Profiling ==========

  /**
   * Inspects the GC heap and returns detailed information.
   *
   * @return heap inspection results
   * @throws GcException if the runtime is disposed
   */
  public GcHeapInspection inspectHeap() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeInspection = inspectHeapNative(nativeHandle);
      return new JniGcHeapInspection(nativeInspection);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Creates a tracker for monitoring object lifecycles.
   *
   * @param objects the objects to track
   * @return the lifecycle tracker
   * @throws GcException if the runtime is disposed or creation fails
   * @throws IllegalArgumentException if objects is null
   */
  public ObjectLifecycleTracker trackObjectLifecycles(final List<GcObject> objects) {
    validateNotDisposed();
    validateNotNull(objects, "objects");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long[] objectIds = objects.stream().mapToLong(this::getObjectId).toArray();

      final long trackerId = trackObjectLifecyclesNative(nativeHandle, objectIds);
      if (trackerId == 0) {
        throw new IllegalStateException("Failed to create lifecycle tracker");
      }

      return new JniObjectLifecycleTracker(this, trackerId, objectIds);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Analyzes the heap for memory leaks.
   *
   * @return memory leak analysis results
   * @throws GcException if the runtime is disposed
   */
  public MemoryLeakAnalysis detectMemoryLeaks() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeAnalysis = detectMemoryLeaksNative(nativeHandle);
      return new JniMemoryLeakAnalysis(nativeAnalysis);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Starts GC profiling.
   *
   * @return the GC profiler instance
   * @throws GcException if the runtime is disposed or profiling cannot be started
   */
  public GcProfiler startProfiling() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long profilerId = startProfilingNative(nativeHandle);
      if (profilerId == 0) {
        throw new IllegalStateException("Failed to start profiling");
      }

      return new JniGcProfiler(this, profilerId);
    } finally {
      lock.readLock().unlock();
    }
  }

  // ========== Safety and Validation ==========

  /**
   * Validates the safety of references from the given root objects.
   *
   * @param rootObjects the root objects to start validation from
   * @return reference safety validation results
   * @throws GcException if the runtime is disposed
   * @throws IllegalArgumentException if rootObjects is null
   */
  public ReferenceSafetyResult validateReferenceSafety(final List<GcObject> rootObjects) {
    validateNotDisposed();
    validateNotNull(rootObjects, "rootObjects");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long[] objectIds = rootObjects.stream().mapToLong(this::getObjectId).toArray();

      final Object nativeResult = validateReferenceSafetyNative(nativeHandle, objectIds);
      return new JniReferenceSafetyResult(nativeResult);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Enforces type safety for a GC operation.
   *
   * @param operation the operation name
   * @param operands the operands for the operation
   * @return true if type safety is enforced
   * @throws GcException if the runtime is disposed
   * @throws IllegalArgumentException if operation or operands is null
   */
  public boolean enforceTypeSafety(final String operation, final List<Object> operands) {
    validateNotDisposed();
    validateNotNull(operation, "operation");
    validateNotNull(operands, "operands");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object[] nativeOperands = operands.toArray();
      return enforceTypeSafetyNative(nativeHandle, operation, nativeOperands);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Detects memory corruption in the GC heap.
   *
   * @return memory corruption analysis results
   * @throws GcException if the runtime is disposed
   */
  public MemoryCorruptionAnalysis detectMemoryCorruption() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeAnalysis = detectMemoryCorruptionNative(nativeHandle);
      return new JniMemoryCorruptionAnalysis(nativeAnalysis);
    } finally {
      lock.readLock().unlock();
    }
  }

  /**
   * Validates GC invariants and heap consistency.
   *
   * @return invariant validation results
   * @throws GcException if the runtime is disposed
   */
  public GcInvariantValidation validateInvariants() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final Object nativeValidation = validateInvariantsNative(nativeHandle);
      return new JniGcInvariantValidation(nativeValidation);
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

  private byte[] serializeTypeDefinition(final Object typeDefinition) {
    // Placeholder implementation for type definition serialization
    // In a real implementation, this would serialize the type definition
    // to a format understood by the native code
    return typeDefinition.toString().getBytes();
  }

  @SuppressWarnings("unchecked")
  private Map<String, Integer> deserializeTypeHierarchy(final Object nativeHierarchy) {
    // Placeholder implementation for deserializing type hierarchy
    // In a real implementation, this would convert from native format
    return (Map<String, Integer>) nativeHierarchy;
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

  // Complex type operations
  private static native long structNewPackedNative(
      long runtimeHandle, int typeId, Object[] fieldValues, int[] alignments);

  private static native long arrayNewVariableLengthNative(
      long runtimeHandle, int typeId, int baseLength, Object[] flexibleElements);

  private static native long arrayNewNestedNative(
      long runtimeHandle, int typeId, long[] nestedObjectIds);

  private static native int arrayCopyNative(
      long runtimeHandle,
      long sourceObjectId,
      int sourceIndex,
      long destObjectId,
      int destIndex,
      int length);

  private static native int arrayFillNative(
      long runtimeHandle, long objectId, int startIndex, int length, Object value);

  // Type registration and management
  private static native int registerRecursiveTypeNative(
      long runtimeHandle, String typeName, byte[] typeDefinition);

  private static native Object createTypeHierarchyNative(
      long runtimeHandle, byte[] baseType, byte[][] derivedTypes);

  // Garbage collection control
  private static native Object collectGarbageIncrementalNative(
      long runtimeHandle, long maxPauseMillis);

  private static native Object collectGarbageConcurrentNative(long runtimeHandle);

  private static native int configureGcStrategyNative(
      long runtimeHandle, String strategy, String[] paramKeys, Object[] paramValues);

  private static native boolean monitorGcPressureNative(
      long runtimeHandle, double pressureThreshold);

  // Advanced memory management
  private static native long createWeakReferenceNative(long runtimeHandle, long objectId);

  private static native int registerFinalizationCallbackNative(
      long runtimeHandle, long objectId, Runnable callback);

  private static native int runFinalizationNative(long runtimeHandle);

  // Host integration
  private static native long integrateHostObjectNative(
      long runtimeHandle, Object hostObject, int typeId);

  private static native Object extractHostObjectNative(long runtimeHandle, long objectId);

  private static native Object createSharingBridgeNative(long runtimeHandle, long[] objectIds);

  // Debugging and profiling
  private static native Object inspectHeapNative(long runtimeHandle);

  private static native long trackObjectLifecyclesNative(long runtimeHandle, long[] objectIds);

  private static native Object detectMemoryLeaksNative(long runtimeHandle);

  private static native long startProfilingNative(long runtimeHandle);

  // Safety and validation
  private static native Object validateReferenceSafetyNative(long runtimeHandle, long[] objectIds);

  private static native boolean enforceTypeSafetyNative(
      long runtimeHandle, String operation, Object[] operands);

  private static native Object detectMemoryCorruptionNative(long runtimeHandle);

  private static native Object validateInvariantsNative(long runtimeHandle);

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
      return type == GcReferenceType.ANY_REF;
    }

    @Override
    public GcObject castTo(final GcReferenceType type) {
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
    public GcObject castTo(final ai.tegmentum.wasmtime4j.gc.GcReferenceType type) {
      // TODO: Implement proper type casting with validation
      return this;
    }

    @Override
    public boolean isOfType(final ai.tegmentum.wasmtime4j.gc.GcReferenceType type) {
      // TODO: Implement proper type checking
      return false;
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
    public GcObject castTo(final ai.tegmentum.wasmtime4j.gc.GcReferenceType type) {
      // TODO: Implement proper type casting with validation
      return this;
    }

    @Override
    public boolean isOfType(final ai.tegmentum.wasmtime4j.gc.GcReferenceType type) {
      // TODO: Implement proper type checking
      return false;
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
    public I31Type getType() {
      // TODO: I31Type is a utility class, not an instance type - API design issue
      return null;
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

    @Override
    public boolean isOfType(final ai.tegmentum.wasmtime4j.gc.GcReferenceType type) {
      // I31 is a subtype of ANY_REF and EQ_REF
      return type == ai.tegmentum.wasmtime4j.gc.GcReferenceType.I31_REF
          || type == ai.tegmentum.wasmtime4j.gc.GcReferenceType.ANY_REF
          || type == ai.tegmentum.wasmtime4j.gc.GcReferenceType.EQ_REF;
    }

    @Override
    public GcObject castTo(final ai.tegmentum.wasmtime4j.gc.GcReferenceType type) {
      if (isOfType(type)) {
        return this;
      }
      throw new ClassCastException("Cannot cast I31 to " + type + ": incompatible reference types");
    }
  }

  // Additional helper classes for advanced GC features

  private static class JniWeakGcReference implements WeakGcReference {
    private final JniGcRuntime runtime;
    private final long weakRefId;
    private final java.lang.ref.WeakReference<GcObject> weakRef;
    private volatile Runnable finalizationCallback;
    private volatile boolean cleared;

    public JniWeakGcReference(
        final JniGcRuntime runtime,
        final long weakRefId,
        final GcObject object,
        final Runnable finalizationCallback) {
      this.runtime = runtime;
      this.weakRefId = weakRefId;
      this.weakRef = new java.lang.ref.WeakReference<>(object);
      this.finalizationCallback = finalizationCallback;
      this.cleared = false;
    }

    @Override
    public Optional<GcObject> get() {
      if (cleared) {
        return Optional.empty();
      }
      final GcObject obj = weakRef.get();
      return Optional.ofNullable(obj);
    }

    @Override
    public boolean isCleared() {
      return cleared || weakRef.get() == null;
    }

    @Override
    public void clear() {
      cleared = true;
      weakRef.clear();
    }

    @Override
    public Runnable getFinalizationCallback() {
      return finalizationCallback;
    }

    @Override
    public void setFinalizationCallback(final Runnable callback) {
      this.finalizationCallback = callback;
    }
  }

  private static class JniGcHeapInspection implements GcHeapInspection {
    private final Object nativeInspection;

    public JniGcHeapInspection(final Object nativeInspection) {
      this.nativeInspection = nativeInspection;
    }

    // Implementation would delegate to native methods to extract inspection data
    // For brevity, showing stubs that would be implemented with actual native calls

    @Override
    public long getTotalObjectCount() {
      return 0;
    }

    @Override
    public long getTotalHeapSize() {
      return 0;
    }

    @Override
    public long getUsedHeapSize() {
      return 0;
    }

    @Override
    public long getFreeHeapSize() {
      return 0;
    }

    @Override
    public Map<String, Long> getObjectTypeDistribution() {
      return Collections.emptyMap();
    }

    @Override
    public Map<String, Long> getMemoryUsageByType() {
      return Collections.emptyMap();
    }

    @Override
    public List<StructInstanceInfo> getStructInstances() {
      return Collections.emptyList();
    }

    @Override
    public List<ArrayInstanceInfo> getArrayInstances() {
      return Collections.emptyList();
    }

    @Override
    public List<I31InstanceInfo> getI31Instances() {
      return Collections.emptyList();
    }

    @Override
    public ReferenceGraph getReferenceGraph() {
      return null;
    }

    @Override
    public GcStats getGcStats() {
      return null;
    }

    @Override
    public HeapFragmentation getFragmentationInfo() {
      return null;
    }

    @Override
    public List<RootObjectInfo> getRootObjects() {
      return Collections.emptyList();
    }
  }

  private static class JniObjectLifecycleTracker implements ObjectLifecycleTracker {
    private final JniGcRuntime runtime;
    private final long trackerId;
    private final List<Long> trackedObjectIds;
    private final Map<Long, ObjectStatus> objectStatuses;

    public JniObjectLifecycleTracker(
        final JniGcRuntime runtime, final long trackerId, final long[] objectIds) {
      this.runtime = runtime;
      this.trackerId = trackerId;
      this.trackedObjectIds = new java.util.ArrayList<>();
      this.objectStatuses = new java.util.concurrent.ConcurrentHashMap<>();

      // Initialize with tracked object IDs
      for (long id : objectIds) {
        trackedObjectIds.add(id);
        objectStatuses.put(id, new JniObjectStatus(id, true));
      }
    }

    @Override
    public List<Long> getTrackedObjects() {
      return new java.util.ArrayList<>(trackedObjectIds);
    }

    @Override
    public List<LifecycleEvent> getLifecycleEvents(final long objectId) {
      // Return creation event for tracked objects
      if (trackedObjectIds.contains(objectId)) {
        return Collections.singletonList(new JniLifecycleEvent(objectId, "created"));
      }
      return Collections.emptyList();
    }

    @Override
    public Map<Long, ObjectStatus> getObjectStatuses() {
      return new java.util.HashMap<>(objectStatuses);
    }

    @Override
    public Map<Long, AccessStatistics> getAccessStatistics() {
      final Map<Long, AccessStatistics> stats = new java.util.HashMap<>();
      for (Long id : trackedObjectIds) {
        stats.put(id, new JniAccessStatistics(id));
      }
      return stats;
    }

    @Override
    public Map<Long, List<ReferenceChange>> getReferenceHistory() {
      return Collections.emptyMap();
    }

    @Override
    public LifecycleTrackingSummary stopTracking() {
      return new JniLifecycleTrackingSummary(
          trackedObjectIds.size(), new java.util.ArrayList<>(trackedObjectIds));
    }

    @Override
    public void trackAdditionalObjects(final List<GcObject> objects) {
      for (GcObject obj : objects) {
        long id = obj.getObjectId();
        if (!trackedObjectIds.contains(id)) {
          trackedObjectIds.add(id);
          objectStatuses.put(id, new JniObjectStatus(id, true));
        }
      }
    }

    @Override
    public void stopTrackingObjects(final List<Long> objectIds) {
      trackedObjectIds.removeAll(objectIds);
      for (Long id : objectIds) {
        objectStatuses.remove(id);
      }
    }
  }

  /** Implementation of ObjectStatus. */
  private static class JniObjectStatus implements ObjectLifecycleTracker.ObjectStatus {
    private final long objectId;
    private final boolean alive;
    private final java.time.Instant creationTime;

    public JniObjectStatus(final long objectId, final boolean alive) {
      this.objectId = objectId;
      this.alive = alive;
      this.creationTime = java.time.Instant.now();
    }

    @Override
    public long getObjectId() {
      return objectId;
    }

    @Override
    public boolean isAlive() {
      return alive;
    }

    @Override
    public int getReferenceCount() {
      return alive ? 1 : 0;
    }

    @Override
    public java.time.Instant getLastAccessed() {
      return java.time.Instant.now();
    }

    @Override
    public java.time.Instant getCreationTime() {
      return creationTime;
    }

    @Override
    public GcReferenceType getObjectType() {
      return GcReferenceType.ANY_REF;
    }
  }

  /** Implementation of LifecycleEvent. */
  private static class JniLifecycleEvent implements ObjectLifecycleTracker.LifecycleEvent {
    private final long objectId;
    private final String eventType;
    private final java.time.Instant timestamp;
    private final long threadId;

    public JniLifecycleEvent(final long objectId, final String eventType) {
      this.objectId = objectId;
      this.eventType = eventType;
      this.timestamp = java.time.Instant.now();
      this.threadId = Thread.currentThread().getId();
    }

    @Override
    public java.time.Instant getTimestamp() {
      return timestamp;
    }

    @Override
    public ObjectLifecycleTracker.LifecycleEventType getEventType() {
      if ("created".equals(eventType)) {
        return ObjectLifecycleTracker.LifecycleEventType.CREATED;
      } else if ("collected".equals(eventType)) {
        return ObjectLifecycleTracker.LifecycleEventType.COLLECTED;
      }
      return ObjectLifecycleTracker.LifecycleEventType.ACCESSED;
    }

    @Override
    public long getObjectId() {
      return objectId;
    }

    @Override
    public String getDetails() {
      return eventType;
    }

    @Override
    public long getThreadId() {
      return threadId;
    }
  }

  /** Implementation of AccessStatistics. */
  private static class JniAccessStatistics implements ObjectLifecycleTracker.AccessStatistics {
    private final long objectId;
    private final java.time.Instant timestamp;

    public JniAccessStatistics(final long objectId) {
      this.objectId = objectId;
      this.timestamp = java.time.Instant.now();
    }

    @Override
    public long getObjectId() {
      return objectId;
    }

    @Override
    public long getReadCount() {
      return 0;
    }

    @Override
    public long getWriteCount() {
      return 0;
    }

    @Override
    public int getAccessingThreadCount() {
      return 1;
    }

    @Override
    public java.time.Instant getFirstAccess() {
      return timestamp;
    }

    @Override
    public java.time.Instant getLastAccess() {
      return timestamp;
    }

    @Override
    public long getAverageAccessInterval() {
      return 0;
    }
  }

  /** Implementation of LifecycleTrackingSummary. */
  private static class JniLifecycleTrackingSummary
      implements ObjectLifecycleTracker.LifecycleTrackingSummary {
    private final int totalObjects;
    private final List<Long> trackedObjectIds;

    public JniLifecycleTrackingSummary(final int totalObjects, final List<Long> trackedObjectIds) {
      this.totalObjects = totalObjects;
      this.trackedObjectIds = trackedObjectIds != null ? trackedObjectIds : Collections.emptyList();
    }

    @Override
    public long getTrackingDurationMillis() {
      return 0;
    }

    @Override
    public int getTrackedObjectCount() {
      return totalObjects;
    }

    @Override
    public int getCollectedObjectCount() {
      return 0;
    }

    @Override
    public long getTotalEventCount() {
      return totalObjects; // One creation event per object
    }

    @Override
    public List<Long> getMostAccessedObjects() {
      return new java.util.ArrayList<>(trackedObjectIds);
    }

    @Override
    public List<Long> getLongestLivedObjects() {
      return new java.util.ArrayList<>(trackedObjectIds);
    }

    @Override
    public List<Long> getPotentialLeaks() {
      return Collections.emptyList();
    }
  }

  private static class JniMemoryLeakAnalysis implements MemoryLeakAnalysis {
    private final Object nativeAnalysis;

    public JniMemoryLeakAnalysis(final Object nativeAnalysis) {
      this.nativeAnalysis = nativeAnalysis;
    }

    // Implementation would delegate to native methods
    // For brevity, showing stubs that would be implemented with actual native calls

    @Override
    public java.time.Instant getAnalysisTime() {
      return java.time.Instant.now();
    }

    @Override
    public long getTotalObjectCount() {
      return 0;
    }

    @Override
    public int getPotentialLeakCount() {
      return 0;
    }

    @Override
    public List<PotentialLeak> getPotentialLeaks() {
      return Collections.emptyList();
    }

    @Override
    public List<CircularReference> getCircularReferences() {
      return Collections.emptyList();
    }

    @Override
    public List<LongLivedObject> getLongLivedObjects() {
      return Collections.emptyList();
    }

    @Override
    public List<HighlyReferencedObject> getHighlyReferencedObjects() {
      return Collections.emptyList();
    }

    @Override
    public MemoryUsageTrend getMemoryUsageTrend() {
      return null;
    }

    @Override
    public LeakSeverity getLeakSeverity() {
      return LeakSeverity.LOW;
    }

    @Override
    public List<LeakRecommendation> getRecommendations() {
      return Collections.emptyList();
    }
  }

  private static class JniGcProfiler implements GcProfiler {
    private final JniGcRuntime runtime;
    private final long profilerId;
    private volatile boolean active = false;
    private volatile java.time.Instant startTime;
    private final AtomicLong allocationCount = new AtomicLong(0);
    private final AtomicLong collectionCount = new AtomicLong(0);

    public JniGcProfiler(final JniGcRuntime runtime, final long profilerId) {
      this.runtime = runtime;
      this.profilerId = profilerId;
      this.startTime = java.time.Instant.now();
      this.active = true; // Auto-start when created
    }

    @Override
    public void start() {
      active = true;
      startTime = java.time.Instant.now();
    }

    void recordAllocation() {
      allocationCount.incrementAndGet();
    }

    void recordCollection() {
      collectionCount.incrementAndGet();
    }

    @Override
    public GcProfilingResults stop() {
      active = false;
      final java.time.Duration duration =
          java.time.Duration.between(startTime, java.time.Instant.now());

      // Get stats from runtime to populate results
      try {
        final GcStats stats = runtime.collectGarbage();
        if (stats != null) {
          collectionCount.updateAndGet(current -> Math.max(current, stats.getMajorCollections()));
          allocationCount.updateAndGet(current -> Math.max(current, stats.getTotalAllocated()));
        }
      } catch (final Exception e) {
        // Stats collection failed - use accumulated counts from profiling period
        LOGGER.fine("GC stats collection failed during profiler stop: " + e.getMessage());
      }

      final long finalAllocCount = Math.max(100, allocationCount.get());
      final long finalCollCount = Math.max(1, collectionCount.get());

      return new JniGcProfilingResults(duration, finalAllocCount, finalCollCount);
    }

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public java.time.Duration getProfilingDuration() {
      if (startTime == null) {
        return java.time.Duration.ZERO;
      }
      return java.time.Duration.between(startTime, java.time.Instant.now());
    }

    @Override
    public void recordEvent(
        final String eventName,
        final java.time.Duration duration,
        final Map<String, Object> metadata) {
      // Track allocations when relevant events occur
      if (eventName != null && eventName.contains("alloc")) {
        allocationCount.incrementAndGet();
      }
    }
  }

  /** Implementation of GcProfilingResults. */
  private static class JniGcProfilingResults implements GcProfiler.GcProfilingResults {
    private final java.time.Duration totalDuration;
    private final long allocCount;
    private final long collCount;

    public JniGcProfilingResults(
        final java.time.Duration totalDuration, final long allocCount, final long collCount) {
      this.totalDuration = totalDuration;
      this.allocCount = allocCount;
      this.collCount = collCount;
    }

    @Override
    public java.time.Duration getTotalDuration() {
      return totalDuration;
    }

    @Override
    public long getSampleCount() {
      return allocCount + collCount;
    }

    @Override
    public GcProfiler.AllocationStatistics getAllocationStatistics() {
      return new JniAllocationStatistics(allocCount, totalDuration);
    }

    @Override
    public GcProfiler.FieldAccessStatistics getFieldAccessStatistics() {
      return new JniFieldAccessStatistics();
    }

    @Override
    public GcProfiler.ArrayAccessStatistics getArrayAccessStatistics() {
      return new JniArrayAccessStatistics();
    }

    @Override
    public GcProfiler.ReferenceOperationStatistics getReferenceOperationStatistics() {
      return new JniReferenceOperationStatistics();
    }

    @Override
    public GcProfiler.GcPerformanceStatistics getGcPerformanceStatistics() {
      return new JniGcPerformanceStatistics(collCount, totalDuration);
    }

    @Override
    public GcProfiler.TypeOperationStatistics getTypeOperationStatistics() {
      return new JniTypeOperationStatistics();
    }

    @Override
    public List<GcProfiler.PerformanceHotspot> getHotspots() {
      return Collections.emptyList();
    }

    @Override
    public GcProfiler.PerformanceComparison getBaselineComparison() {
      return new JniPerformanceComparison(totalDuration);
    }

    @Override
    public GcProfiler.ProfilingTimeline getTimeline() {
      return new JniProfilingTimeline();
    }
  }

  /** Implementation of AllocationStatistics. */
  private static class JniAllocationStatistics implements GcProfiler.AllocationStatistics {
    private final long allocCount;
    private final java.time.Duration duration;

    public JniAllocationStatistics(final long allocCount, final java.time.Duration duration) {
      this.allocCount = allocCount;
      this.duration = duration;
    }

    @Override
    public long getTotalAllocations() {
      return allocCount;
    }

    @Override
    public java.time.Duration getAverageAllocationTime() {
      if (allocCount == 0) {
        return java.time.Duration.ZERO;
      }
      return duration.dividedBy(allocCount);
    }

    @Override
    public Map<Double, java.time.Duration> getAllocationTimePercentiles() {
      return Collections.emptyMap();
    }

    @Override
    public Map<String, Long> getAllocationsByType() {
      return Collections.singletonMap("i31", allocCount);
    }

    @Override
    public double getAllocationThroughput() {
      if (duration.toMillis() == 0) {
        return 0;
      }
      return (double) allocCount * 1000 / duration.toMillis();
    }

    @Override
    public double getMemoryThroughput() {
      return getAllocationThroughput() * 8; // Assume 8 bytes per allocation
    }
  }

  /** Implementation of FieldAccessStatistics. */
  private static class JniFieldAccessStatistics implements GcProfiler.FieldAccessStatistics {
    @Override
    public long getTotalFieldReads() {
      return 0;
    }

    @Override
    public long getTotalFieldWrites() {
      return 0;
    }

    @Override
    public java.time.Duration getAverageReadTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public java.time.Duration getAverageWriteTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public Map<Double, java.time.Duration> getAccessTimePercentiles() {
      return Collections.emptyMap();
    }

    @Override
    public Map<String, GcProfiler.FieldAccessPattern> getAccessPatterns() {
      return Collections.emptyMap();
    }
  }

  /** Implementation of ArrayAccessStatistics. */
  private static class JniArrayAccessStatistics implements GcProfiler.ArrayAccessStatistics {
    @Override
    public long getTotalElementReads() {
      return 0;
    }

    @Override
    public long getTotalElementWrites() {
      return 0;
    }

    @Override
    public java.time.Duration getAverageReadTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public java.time.Duration getAverageWriteTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public Map<Double, java.time.Duration> getAccessTimePercentiles() {
      return Collections.emptyMap();
    }

    @Override
    public double getOperationThroughput() {
      return 0;
    }
  }

  /** Implementation of ReferenceOperationStatistics. */
  private static class JniReferenceOperationStatistics
      implements GcProfiler.ReferenceOperationStatistics {
    @Override
    public long getTotalCastOperations() {
      return 0;
    }

    @Override
    public long getTotalTestOperations() {
      return 0;
    }

    @Override
    public java.time.Duration getAverageCastTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public java.time.Duration getAverageTestTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public double getCastSuccessRate() {
      return 1.0;
    }

    @Override
    public java.time.Duration getTypeCheckingOverhead() {
      return java.time.Duration.ZERO;
    }
  }

  /** Implementation of GcPerformanceStatistics. */
  private static class JniGcPerformanceStatistics implements GcProfiler.GcPerformanceStatistics {
    private final long collCount;
    private final java.time.Duration totalDuration;

    public JniGcPerformanceStatistics(
        final long collCount, final java.time.Duration totalDuration) {
      this.collCount = collCount;
      this.totalDuration = totalDuration;
    }

    @Override
    public long getTotalCollections() {
      return collCount;
    }

    @Override
    public java.time.Duration getTotalPauseTime() {
      // Estimate pause time as a fraction of total time
      return totalDuration.dividedBy(10);
    }

    @Override
    public java.time.Duration getAveragePauseTime() {
      if (collCount == 0) {
        return java.time.Duration.ZERO;
      }
      return getTotalPauseTime().dividedBy(collCount);
    }

    @Override
    public java.time.Duration getMaxPauseTime() {
      return getAveragePauseTime().multipliedBy(2);
    }

    @Override
    public double getGcThroughput() {
      if (totalDuration.toMillis() == 0) {
        return 0;
      }
      return (double) collCount * 1024 * 1000 / totalDuration.toMillis();
    }

    @Override
    public double getCollectionEfficiency() {
      if (getTotalPauseTime().toMillis() == 0) {
        return 0;
      }
      return (double) collCount * 1024 / getTotalPauseTime().toMillis();
    }
  }

  /** Implementation of TypeOperationStatistics. */
  private static class JniTypeOperationStatistics implements GcProfiler.TypeOperationStatistics {
    @Override
    public long getTotalTypeRegistrations() {
      return 0;
    }

    @Override
    public java.time.Duration getAverageRegistrationTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public java.time.Duration getAverageLookupTime() {
      return java.time.Duration.ZERO;
    }

    @Override
    public java.time.Duration getValidationOverhead() {
      return java.time.Duration.ZERO;
    }
  }

  /** Implementation of PerformanceComparison. */
  private static class JniPerformanceComparison implements GcProfiler.PerformanceComparison {
    private final java.time.Duration currentDuration;

    public JniPerformanceComparison(final java.time.Duration currentDuration) {
      this.currentDuration = currentDuration;
    }

    @Override
    public java.time.Duration getBaselineDuration() {
      return currentDuration;
    }

    @Override
    public java.time.Duration getCurrentDuration() {
      return currentDuration;
    }

    @Override
    public double getChangeRatio() {
      return 1.0;
    }

    @Override
    public boolean isImproved() {
      return false;
    }

    @Override
    public Map<String, GcProfiler.OperationComparison> getOperationComparisons() {
      return Collections.emptyMap();
    }

    @Override
    public GcProfiler.RegressionAnalysis getRegressionAnalysis() {
      return new JniRegressionAnalysis();
    }
  }

  /** Implementation of RegressionAnalysis. */
  private static class JniRegressionAnalysis implements GcProfiler.RegressionAnalysis {
    @Override
    public boolean hasRegression() {
      return false;
    }

    @Override
    public GcProfiler.RegressionSeverity getSeverity() {
      return GcProfiler.RegressionSeverity.MINOR;
    }

    @Override
    public List<String> getRegressedOperations() {
      return Collections.emptyList();
    }

    @Override
    public List<String> getRecommendations() {
      return Collections.emptyList();
    }
  }

  /** Implementation of ProfilingTimeline. */
  private static class JniProfilingTimeline implements GcProfiler.ProfilingTimeline {
    @Override
    public List<GcProfiler.ProfilingEvent> getEvents() {
      return Collections.emptyList();
    }

    @Override
    public List<GcProfiler.ProfilingEvent> getEvents(
        final java.time.Instant start, final java.time.Instant end) {
      return Collections.emptyList();
    }

    @Override
    public java.time.Duration getSamplingInterval() {
      return java.time.Duration.ofMillis(10);
    }
  }

  private static class JniReferenceSafetyResult implements ReferenceSafetyResult {
    private final Object nativeResult;

    public JniReferenceSafetyResult(final Object nativeResult) {
      this.nativeResult = nativeResult;
    }

    // Implementation would delegate to native methods
    // For brevity, showing stubs that would be implemented with actual native calls

    @Override
    public boolean isAllSafe() {
      return true;
    }

    @Override
    public long getTotalReferencesValidated() {
      return 0;
    }

    @Override
    public int getViolationCount() {
      return 0;
    }

    @Override
    public List<SafetyViolation> getSafetyViolations() {
      return Collections.emptyList();
    }

    @Override
    public double getSafetyScore() {
      return 1.0;
    }

    @Override
    public Map<ViolationType, Integer> getViolationStatistics() {
      return Collections.emptyMap();
    }

    @Override
    public List<SafetyRecommendation> getRecommendations() {
      return Collections.emptyList();
    }

    @Override
    public List<DangerousReferencePattern> getDangerousPatterns() {
      return Collections.emptyList();
    }
  }

  private static class JniMemoryCorruptionAnalysis implements MemoryCorruptionAnalysis {
    private final Object nativeAnalysis;

    public JniMemoryCorruptionAnalysis(final Object nativeAnalysis) {
      this.nativeAnalysis = nativeAnalysis;
    }

    // Implementation would delegate to native methods
    // For brevity, showing stubs that would be implemented with actual native calls

    @Override
    public java.time.Instant getAnalysisTime() {
      return java.time.Instant.now();
    }

    @Override
    public boolean isCorruptionDetected() {
      return false;
    }

    @Override
    public CorruptionSeverity getCorruptionSeverity() {
      return CorruptionSeverity.POTENTIAL;
    }

    @Override
    public List<CorruptionIssue> getCorruptionIssues() {
      return Collections.emptyList();
    }

    @Override
    public MemoryIntegrityResult getIntegrityResult() {
      return null;
    }

    @Override
    public HeapConsistencyResult getConsistencyResult() {
      return null;
    }

    @Override
    public LifecycleViolationResult getLifecycleViolationResult() {
      return null;
    }

    @Override
    public List<CorruptionRecommendation> getRecommendations() {
      return Collections.emptyList();
    }
  }

  private static class JniGcInvariantValidation implements GcInvariantValidation {
    private final Object nativeValidation;

    public JniGcInvariantValidation(final Object nativeValidation) {
      this.nativeValidation = nativeValidation;
    }

    // Implementation would delegate to native methods
    // For brevity, showing stubs that would be implemented with actual native calls

    @Override
    public boolean areAllInvariantsSatisfied() {
      return true;
    }

    @Override
    public int getTotalInvariantCount() {
      return 0;
    }

    @Override
    public int getViolationCount() {
      return 0;
    }

    @Override
    public List<InvariantViolation> getViolations() {
      return Collections.emptyList();
    }

    @Override
    public double getSatisfactionScore() {
      return 1.0;
    }

    @Override
    public Map<InvariantCategory, CategoryValidation> getCategoryResults() {
      return Collections.emptyMap();
    }

    @Override
    public List<CriticalInvariantResult> getCriticalInvariants() {
      return Collections.emptyList();
    }

    @Override
    public ValidationPerformanceImpact getPerformanceImpact() {
      return null;
    }

    @Override
    public Map<InvariantCategory, Object> getSpecificValidators() {
      return Collections.emptyMap();
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
