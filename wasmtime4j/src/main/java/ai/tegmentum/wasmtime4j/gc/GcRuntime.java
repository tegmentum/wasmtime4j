package ai.tegmentum.wasmtime4j.gc;

import java.util.List;
import java.util.Map;
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

  // ========== Field and Element Access ==========

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

  // ========== Complex Type Operations ==========

  /**
   * Creates a struct instance with packed fields and custom alignment.
   *
   * @param structType the struct type with packed field definitions
   * @param fieldValues the initial field values
   * @param customAlignment custom field alignment requirements
   * @return the new struct instance
   * @throws GcException if creation fails
   */
  StructInstance createPackedStruct(StructType structType, List<GcValue> fieldValues,
                                   Map<Integer, Integer> customAlignment);

  /**
   * Creates a variable-length array with flexible members.
   *
   * @param arrayType the base array type
   * @param baseLength the fixed portion length
   * @param flexibleElements the flexible portion elements
   * @return the new variable-length array instance
   * @throws GcException if creation fails
   */
  ArrayInstance createVariableLengthArray(ArrayType arrayType, int baseLength,
                                         List<GcValue> flexibleElements);

  /**
   * Creates an array with nested struct or array elements.
   *
   * @param arrayType the array type
   * @param nestedElements the nested elements (structs or arrays)
   * @return the new nested array instance
   * @throws GcException if creation fails
   */
  ArrayInstance createNestedArray(ArrayType arrayType, List<GcObject> nestedElements);

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
  void copyArrayElements(ArrayInstance sourceArray, int sourceIndex,
                        ArrayInstance destArray, int destIndex, int length);

  /**
   * Fills array elements with a specific value.
   *
   * @param array the array to fill
   * @param startIndex the start index
   * @param length the number of elements to fill
   * @param value the fill value
   * @throws GcException if fill operation fails
   */
  void fillArrayElements(ArrayInstance array, int startIndex, int length, GcValue value);

  // ========== Type Registration and Management ==========

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
   * Registers a recursive type definition.
   *
   * @param typeName the type name
   * @param typeDefinition the recursive type definition
   * @return the assigned type ID
   * @throws GcException if registration fails
   */
  int registerRecursiveType(String typeName, Object typeDefinition);

  /**
   * Creates a type hierarchy with inheritance relationships.
   *
   * @param baseType the base type
   * @param derivedTypes the derived types
   * @return the type hierarchy mapping
   * @throws GcException if hierarchy creation fails
   */
  Map<String, Integer> createTypeHierarchy(Object baseType, List<Object> derivedTypes);

  // ========== Garbage Collection Control ==========

  /**
   * Triggers garbage collection.
   *
   * @return collection statistics
   */
  GcStats collectGarbage();

  /**
   * Triggers incremental garbage collection.
   *
   * @param maxPauseMillis maximum pause time in milliseconds
   * @return collection statistics
   */
  GcStats collectGarbageIncremental(long maxPauseMillis);

  /**
   * Triggers concurrent garbage collection.
   *
   * @return collection statistics
   */
  GcStats collectGarbageConcurrent();

  /**
   * Gets current garbage collection statistics.
   *
   * @return GC statistics
   */
  GcStats getGcStats();

  /**
   * Configures garbage collection strategy.
   *
   * @param strategy the GC strategy to use
   * @param parameters strategy-specific parameters
   * @throws GcException if configuration fails
   */
  void configureGcStrategy(String strategy, Map<String, Object> parameters);

  /**
   * Monitors GC pressure and triggers collection when necessary.
   *
   * @param pressureThreshold the pressure threshold (0.0 to 1.0)
   * @return true if collection was triggered
   */
  boolean monitorGcPressure(double pressureThreshold);

  // ========== Advanced Memory Management ==========

  /**
   * Creates a weak reference to an object.
   *
   * @param object the object to reference weakly
   * @param finalizationCallback callback to invoke when object is finalized
   * @return the weak reference
   */
  WeakGcReference createWeakReference(GcObject object, Runnable finalizationCallback);

  /**
   * Registers a finalization callback for an object.
   *
   * @param object the object
   * @param callback the finalization callback
   */
  void registerFinalizationCallback(GcObject object, Runnable callback);

  /**
   * Forces finalization of all pending objects.
   *
   * @return the number of objects finalized
   */
  int runFinalization();

  // ========== Host Integration ==========

  /**
   * Integrates a host-managed object with the GC heap.
   *
   * @param hostObject the host object
   * @param gcType the corresponding GC type
   * @return the GC wrapper object
   * @throws GcException if integration fails
   */
  GcObject integrateHostObject(Object hostObject, GcReferenceType gcType);

  /**
   * Extracts the host object from a GC-managed wrapper.
   *
   * @param gcObject the GC wrapper object
   * @return the host object
   * @throws GcException if extraction fails
   */
  Object extractHostObject(GcObject gcObject);

  /**
   * Creates a cross-language object sharing bridge.
   *
   * @param objects objects to share across languages
   * @return the sharing bridge
   * @throws GcException if bridge creation fails
   */
  Object createSharingBridge(List<GcObject> objects);

  // ========== Debugging and Profiling ==========

  /**
   * Inspects the GC heap and returns analysis information.
   *
   * @return heap inspection results
   */
  GcHeapInspection inspectHeap();

  /**
   * Tracks the lifecycle of specific objects for debugging.
   *
   * @param objects objects to track
   * @return lifecycle tracking handle
   */
  ObjectLifecycleTracker trackObjectLifecycles(List<GcObject> objects);

  /**
   * Detects potential memory leaks in the GC heap.
   *
   * @return leak detection results
   */
  MemoryLeakAnalysis detectMemoryLeaks();

  /**
   * Starts performance profiling of GC operations.
   *
   * @return profiling session handle
   */
  GcProfiler startProfiling();

  // ========== Safety and Validation ==========

  /**
   * Validates reference safety for complex object graphs.
   *
   * @param rootObjects the root objects to validate
   * @return validation results
   */
  ReferenceSafetyResult validateReferenceSafety(List<GcObject> rootObjects);

  /**
   * Enforces type safety in complex scenarios.
   *
   * @param operation the operation to check
   * @param operands the operation operands
   * @return true if operation is type-safe
   */
  boolean enforceTypeSafety(String operation, List<Object> operands);

  /**
   * Detects potential memory corruption.
   *
   * @return corruption detection results
   */
  MemoryCorruptionAnalysis detectMemoryCorruption();

  /**
   * Validates GC invariants and consistency.
   *
   * @return invariant validation results
   */
  GcInvariantValidation validateInvariants();

  // ========== Advanced GC Features from Task #307 Integration ==========

  /**
   * Creates a weak reference with finalization callback support.
   *
   * @param object the object to reference weakly
   * @param finalizationCallback optional callback to invoke when object is finalized
   * @return the weak reference
   */
  WeakGcReference createWeakReferenceAdvanced(GcObject object, Runnable finalizationCallback);

  /**
   * Performs advanced garbage collection with incremental and concurrent support.
   *
   * @param maxPauseMillis maximum pause time in milliseconds (null for no limit)
   * @param concurrent whether to perform concurrent collection
   * @return collection statistics
   */
  GcStats collectGarbageAdvanced(Long maxPauseMillis, boolean concurrent);

  /**
   * Pins an object to prevent it from being moved during GC (future GC proposal support).
   *
   * @param object the object to pin
   */
  void pinObject(GcObject object);

  /**
   * Unpins an object to allow it to be moved during GC (future GC proposal support).
   *
   * @param object the object to unpin
   */
  void unpinObject(GcObject object);

  /**
   * Performs optimized reference type casting with caching support.
   *
   * @param object the object to cast
   * @param targetType the target reference type
   * @param enableCaching whether to enable cast result caching
   * @return the cast object
   * @throws ClassCastException if the cast is invalid
   */
  GcObject refCastOptimized(GcObject object, GcReferenceType targetType, boolean enableCaching);

  /**
   * Creates SIMD values with advanced vector types from Task #307.
   *
   * @param v256Data the 256-bit vector data (32 bytes)
   * @return the V256 GC value
   */
  default GcValue createV256(byte[] v256Data) {
    return GcValue.v256(v256Data);
  }

  /**
   * Creates SIMD values with AVX-512 support from Task #307.
   *
   * @param v512Data the 512-bit vector data (64 bytes)
   * @return the V512 GC value
   */
  default GcValue createV512(byte[] v512Data) {
    return GcValue.v512(v512Data);
  }
}
