package ai.tegmentum.wasmtime4j.panama;

import static java.lang.foreign.ValueLayout.*;

import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
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
import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemoryLayout;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.VarHandle;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly GC runtime operations.
 *
 * <p>This class provides actual WebAssembly GC functionality through Panama FFI bindings to the
 * native Wasmtime GC implementation. It supports all GC operations including struct creation, array
 * manipulation, reference type conversions, and heap management.
 *
 * @since 1.0.0
 */
public final class PanamaGcRuntime implements GcRuntime {

  private static final Logger LOGGER = Logger.getLogger(PanamaGcRuntime.class.getName());

  // Native type IDs matching GcReferenceType enum ordinals
  private static final int NATIVE_TYPE_ANY = 0;
  private static final int NATIVE_TYPE_EQ = 1;
  private static final int NATIVE_TYPE_I31 = 2;
  private static final int NATIVE_TYPE_STRUCT = 3;
  private static final int NATIVE_TYPE_ARRAY = 4;

  private final long nativeHandle;
  private final Arena arena;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private volatile boolean disposed = false;

  // Registry to track GC objects by their native object ID
  private final java.util.concurrent.ConcurrentHashMap<Long, PanamaGcObject> objectRegistry =
      new java.util.concurrent.ConcurrentHashMap<>();

  // Native function handles
  private static final MethodHandle createRuntime;
  private static final MethodHandle destroyRuntime;
  private static final MethodHandle registerStructType;
  private static final MethodHandle registerArrayType;
  private static final MethodHandle structNew;
  private static final MethodHandle structNewDefault;
  private static final MethodHandle structGet;
  private static final MethodHandle structSet;
  private static final MethodHandle arrayNew;
  private static final MethodHandle arrayNewDefault;
  private static final MethodHandle arrayGet;
  private static final MethodHandle arraySet;
  private static final MethodHandle arrayLen;
  private static final MethodHandle i31New;
  private static final MethodHandle i31Get;
  private static final MethodHandle refCast;
  private static final MethodHandle refTest;
  private static final MethodHandle refEq;
  private static final MethodHandle refIsNull;
  private static final MethodHandle collectGarbage;
  private static final MethodHandle getGcStats;

  // GC Stats structure layout
  private static final MemoryLayout GC_STATS_LAYOUT =
      MemoryLayout.structLayout(
              JAVA_LONG.withName("total_allocated"),
              JAVA_LONG.withName("total_collected"),
              JAVA_LONG.withName("bytes_allocated"),
              JAVA_LONG.withName("bytes_collected"),
              JAVA_LONG.withName("minor_collections"),
              JAVA_LONG.withName("major_collections"),
              JAVA_LONG.withName("total_gc_time_nanos"),
              JAVA_INT.withName("current_heap_size"),
              JAVA_INT.withName("peak_heap_size"),
              JAVA_INT.withName("max_heap_size"))
          .withName("GcStatsFFI");

  private static final VarHandle totalAllocatedHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("total_allocated"));
  private static final VarHandle totalCollectedHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("total_collected"));
  private static final VarHandle bytesAllocatedHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bytes_allocated"));
  private static final VarHandle bytesCollectedHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("bytes_collected"));
  private static final VarHandle minorCollectionsHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("minor_collections"));
  private static final VarHandle majorCollectionsHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("major_collections"));
  private static final VarHandle totalGcTimeNanosHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("total_gc_time_nanos"));
  private static final VarHandle currentHeapSizeHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("current_heap_size"));
  private static final VarHandle peakHeapSizeHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("peak_heap_size"));
  private static final VarHandle maxHeapSizeHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("max_heap_size"));

  static {
    // Ensure native library is loaded before trying to find symbols
    final NativeLibraryLoader loader = NativeLibraryLoader.getInstance();
    final Linker linker = Linker.nativeLinker();
    final SymbolLookup lookup = loader.getSymbolLookup();

    try {
      createRuntime =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_create_runtime").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));

      destroyRuntime =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_destroy_runtime").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG));

      registerStructType =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_register_struct_type").orElseThrow(),
              FunctionDescriptor.of(
                  JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS,
                  ADDRESS));

      registerArrayType =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_register_array_type").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_BYTE));

      structNew =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_struct_new").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT));

      structNewDefault =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_struct_new_default").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));

      structGet =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_struct_get").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, ADDRESS));

      structSet =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_struct_set").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT));

      arrayNew =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_array_new").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT));

      arrayNewDefault =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_array_new_default").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT));

      arrayGet =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_array_get").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, ADDRESS));

      arraySet =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_array_set").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT));

      arrayLen =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_array_len").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG));

      i31New =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_i31_new").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));

      i31Get =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_i31_get").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_BYTE));

      refCast =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_ref_cast").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_INT));

      refTest =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_ref_test").orElseThrow(),
              FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG, JAVA_INT));

      refEq =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_ref_eq").orElseThrow(),
              FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG, JAVA_LONG));

      refIsNull =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_ref_is_null").orElseThrow(),
              FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG));

      collectGarbage =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_collect_garbage").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));

      getGcStats =
          linker.downcallHandle(
              lookup.find("wasmtime4j_gc_get_stats").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));

    } catch (final Throwable e) {
      throw new ExceptionInInitializerError(
          "Failed to initialize Panama GC bindings: " + e.getMessage());
    }
  }

  /**
   * Creates a new Panama GC runtime with the specified engine handle.
   *
   * @param engineHandle the native engine handle
   * @throws PanamaException if runtime creation fails
   */
  public PanamaGcRuntime(final long engineHandle) throws PanamaException {
    if (engineHandle == 0) {
      throw new PanamaException("Invalid engine handle");
    }

    this.arena = Arena.ofConfined();

    try {
      this.nativeHandle = (long) createRuntime.invokeExact(engineHandle);
      if (this.nativeHandle == 0) {
        throw new PanamaException("Failed to create GC runtime");
      }
    } catch (final Throwable e) {
      arena.close();
      throw new PanamaException("Failed to create GC runtime", e);
    }

    LOGGER.fine("Created Panama GC runtime with handle: " + this.nativeHandle);
  }

  @Override
  public StructInstance createStruct(final StructType structType, final List<GcValue> fieldValues)
      throws GcException {
    validateNotDisposed();
    validateNotNull(structType, "structType");
    validateNotNull(fieldValues, "fieldValues");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      // Register struct type if needed
      final int typeId = registerStructTypeInternal(structType);

      // Convert field values to native format
      final MemorySegment valuesSegment = convertGcValuesToNative(fieldValues);

      try {
        final long objectId =
            (long) structNew.invokeExact(nativeHandle, typeId, valuesSegment, fieldValues.size());

        if (objectId == 0) {
          throw new GcException("Failed to create struct instance");
        }

        final PanamaStructInstance instance =
            new PanamaStructInstance(this, objectId, structType, typeId);
        objectRegistry.put(objectId, instance);
        return instance;
      } catch (final Throwable e) {
        throw new GcException("Failed to create struct instance", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public StructInstance createStruct(final StructType structType) throws GcException {
    validateNotDisposed();
    validateNotNull(structType, "structType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      // Register struct type if needed
      final int typeId = registerStructTypeInternal(structType);

      try {
        final long objectId = (long) structNewDefault.invokeExact(nativeHandle, typeId);
        if (objectId == 0) {
          throw new GcException("Failed to create struct instance with default values");
        }

        final PanamaStructInstance instance =
            new PanamaStructInstance(this, objectId, structType, typeId);
        objectRegistry.put(objectId, instance);
        return instance;
      } catch (final Throwable e) {
        throw new GcException("Failed to create struct instance with default values", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public ArrayInstance createArray(final ArrayType arrayType, final List<GcValue> elements)
      throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    validateNotNull(elements, "elements");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      // Register array type if needed
      final int typeId = registerArrayTypeInternal(arrayType);

      // Convert elements to native format
      final MemorySegment elementsSegment = convertGcValuesToNative(elements);

      try {
        final long objectId =
            (long) arrayNew.invokeExact(nativeHandle, typeId, elementsSegment, elements.size());

        if (objectId == 0) {
          throw new GcException("Failed to create array instance");
        }

        final PanamaArrayInstance instance =
            new PanamaArrayInstance(this, objectId, arrayType, typeId, elements.size());
        objectRegistry.put(objectId, instance);
        return instance;
      } catch (final Throwable e) {
        throw new GcException("Failed to create array instance", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public ArrayInstance createArray(final ArrayType arrayType, final int length) throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    if (length < 0) {
      throw new IllegalArgumentException("Array length cannot be negative: " + length);
    }

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      // Register array type if needed
      final int typeId = registerArrayTypeInternal(arrayType);

      try {
        final long objectId = (long) arrayNewDefault.invokeExact(nativeHandle, typeId, length);
        if (objectId == 0) {
          throw new GcException("Failed to create array instance with default values");
        }

        final PanamaArrayInstance instance =
            new PanamaArrayInstance(this, objectId, arrayType, typeId, length);
        objectRegistry.put(objectId, instance);
        return instance;
      } catch (final Throwable e) {
        throw new GcException("Failed to create array instance with default values", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public I31Instance createI31(final int value) throws GcException {
    // Validate I31 range: 31-bit signed integer (-2^30 to 2^30-1)
    // I31 uses 31 bits for the value (1 bit reserved for tagging)
    final int i31Min = -(1 << 30); // -1073741824
    final int i31Max = (1 << 30) - 1; // 1073741823
    if (value < i31Min || value > i31Max) {
      throw new IllegalArgumentException(
          "I31 value out of range: "
              + value
              + " (must be between "
              + i31Min
              + " and "
              + i31Max
              + ")");
    }

    validateNotDisposed();

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      try {
        final long objectId = (long) i31New.invokeExact(nativeHandle, value);
        if (objectId == 0) {
          throw new GcException("Failed to create I31 instance");
        }

        final PanamaI31Instance instance = new PanamaI31Instance(this, objectId, value);
        objectRegistry.put(objectId, instance);
        return instance;
      } catch (final Throwable e) {
        throw new GcException("Failed to create I31 instance", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public GcValue getStructField(final StructInstance struct, final int fieldIndex)
      throws GcException {
    validateNotDisposed();
    validateNotNull(struct, "struct");
    if (fieldIndex < 0) {
      throw new IllegalArgumentException("Field index cannot be negative: " + fieldIndex);
    }

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      if (!(struct instanceof PanamaStructInstance)) {
        throw new GcException("Invalid struct instance type");
      }

      final PanamaStructInstance panamaStruct = (PanamaStructInstance) struct;

      final MemorySegment resultValue = arena.allocate(JAVA_LONG);
      final MemorySegment resultType = arena.allocate(JAVA_INT);

      try {
        final int result =
            (int)
                structGet.invokeExact(
                    nativeHandle, panamaStruct.getObjectId(), fieldIndex, resultValue, resultType);

        if (result != 0) {
          throw new GcException("Failed to get struct field");
        }

        final long value = resultValue.get(JAVA_LONG, 0);
        final int type = resultType.get(JAVA_INT, 0);

        return convertNativeToGcValue(value, type);
      } catch (final Throwable e) {
        throw new GcException("Failed to get struct field", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void setStructField(final StructInstance struct, final int fieldIndex, final GcValue value)
      throws GcException {
    validateNotDisposed();
    validateNotNull(struct, "struct");
    validateNotNull(value, "value");
    if (fieldIndex < 0) {
      throw new IllegalArgumentException("Field index cannot be negative: " + fieldIndex);
    }

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      if (!(struct instanceof PanamaStructInstance)) {
        throw new GcException("Invalid struct instance type");
      }

      final PanamaStructInstance panamaStruct = (PanamaStructInstance) struct;
      final NativeValue nativeValue = convertGcValueToNative(value);

      try {
        final int result =
            (int)
                structSet.invokeExact(
                    nativeHandle,
                    panamaStruct.getObjectId(),
                    fieldIndex,
                    nativeValue.value,
                    nativeValue.type);

        if (result != 0) {
          throw new GcException("Failed to set struct field");
        }
      } catch (final Throwable e) {
        throw new GcException("Failed to set struct field", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public GcValue getArrayElement(final ArrayInstance array, final int elementIndex)
      throws GcException {
    validateNotDisposed();
    validateNotNull(array, "array");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      if (!(array instanceof PanamaArrayInstance)) {
        throw new GcException("Invalid array instance type");
      }

      final PanamaArrayInstance panamaArray = (PanamaArrayInstance) array;
      if (elementIndex < 0 || elementIndex >= panamaArray.getLength()) {
        throw new IndexOutOfBoundsException(
            "Element index out of bounds: "
                + elementIndex
                + " (array length: "
                + panamaArray.getLength()
                + ")");
      }

      final MemorySegment resultValue = arena.allocate(JAVA_LONG);
      final MemorySegment resultType = arena.allocate(JAVA_INT);

      try {
        final int result =
            (int)
                arrayGet.invokeExact(
                    nativeHandle, panamaArray.getObjectId(), elementIndex, resultValue, resultType);

        if (result != 0) {
          throw new GcException("Failed to get array element");
        }

        final long value = resultValue.get(JAVA_LONG, 0);
        final int type = resultType.get(JAVA_INT, 0);

        return convertNativeToGcValue(value, type);
      } catch (final Throwable e) {
        throw new GcException("Failed to get array element", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void setArrayElement(
      final ArrayInstance array, final int elementIndex, final GcValue value) throws GcException {
    validateNotDisposed();
    validateNotNull(array, "array");
    validateNotNull(value, "value");
    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      if (!(array instanceof PanamaArrayInstance)) {
        throw new GcException("Invalid array instance type");
      }

      final PanamaArrayInstance panamaArray = (PanamaArrayInstance) array;
      if (elementIndex < 0 || elementIndex >= panamaArray.getLength()) {
        throw new IndexOutOfBoundsException(
            "Element index out of bounds: "
                + elementIndex
                + " (array length: "
                + panamaArray.getLength()
                + ")");
      }

      final NativeValue nativeValue = convertGcValueToNative(value);

      try {
        final int result =
            (int)
                arraySet.invokeExact(
                    nativeHandle,
                    panamaArray.getObjectId(),
                    elementIndex,
                    nativeValue.value,
                    nativeValue.type);

        if (result != 0) {
          throw new GcException("Failed to set array element");
        }
      } catch (final Throwable e) {
        throw new GcException("Failed to set array element", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public int getArrayLength(final ArrayInstance array) {
    validateNotDisposed();
    validateNotNull(array, "array");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      if (!(array instanceof PanamaArrayInstance)) {
        throw new IllegalArgumentException("Invalid array instance type");
      }

      final PanamaArrayInstance panamaArray = (PanamaArrayInstance) array;

      try {
        final int length = (int) arrayLen.invokeExact(nativeHandle, panamaArray.getObjectId());
        if (length < 0) {
          throw new IllegalStateException("Failed to get array length");
        }

        return length;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to get array length", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public GcObject refCast(final GcObject object, final GcReferenceType targetType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetType, "targetType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = convertReferenceTypeToNative(targetType);

      try {
        final long castObjectId = (long) refCast.invokeExact(nativeHandle, objectId, targetTypeId);
        if (castObjectId == 0) {
          throw new ClassCastException("Reference cast failed");
        }

        return createGcObjectFromId(castObjectId, targetType);
      } catch (final ClassCastException e) {
        throw e; // Let ClassCastException propagate
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to cast reference", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean refTest(final GcObject object, final GcReferenceType targetType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetType, "targetType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = convertReferenceTypeToNative(targetType);

      try {
        final byte result = (byte) refTest.invokeExact(nativeHandle, objectId, targetTypeId);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to test reference", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean refEquals(final GcObject obj1, final GcObject obj2) {
    validateNotDisposed();

    if (obj1 == null && obj2 == null) {
      return true;
    }
    if (obj1 == null || obj2 == null) {
      return false;
    }

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId1 = getObjectId(obj1);
      final long objectId2 = getObjectId(obj2);

      try {
        final byte result = (byte) refEq.invokeExact(nativeHandle, objectId1, objectId2);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to compare references", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean isNull(final GcObject object) {
    if (object == null) {
      return true;
    }

    validateNotDisposed();

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);

      try {
        final byte result = (byte) refIsNull.invokeExact(nativeHandle, objectId);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to check if reference is null", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public int registerStructType(final StructType structType) throws GcException {
    validateNotDisposed();
    validateNotNull(structType, "structType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      return registerStructTypeInternal(structType);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public int registerArrayType(final ArrayType arrayType) throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      return registerArrayTypeInternal(arrayType);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public GcStats collectGarbage() {
    validateNotDisposed();

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final MemorySegment statsSegment = arena.allocate(GC_STATS_LAYOUT);

      try {
        final int result = (int) collectGarbage.invokeExact(nativeHandle, statsSegment);
        if (result != 0) {
          throw new IllegalStateException("Failed to collect garbage");
        }

        return convertNativeToGcStats(statsSegment);
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to collect garbage", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public GcStats getGcStats() {
    validateNotDisposed();

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final MemorySegment statsSegment = arena.allocate(GC_STATS_LAYOUT);

      try {
        final int result = (int) getGcStats.invokeExact(nativeHandle, statsSegment);
        if (result != 0) {
          throw new IllegalStateException("Failed to get GC stats");
        }

        return convertNativeToGcStats(statsSegment);
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to get GC stats", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  /**
   * Disposes this GC runtime and releases native resources.
   *
   * @throws PanamaException if disposal fails
   */
  public void dispose() {
    lock.writeLock().lock();
    try {
      if (!disposed) {
        try {
          destroyRuntime.invokeExact(nativeHandle);
        } catch (final Throwable e) {
          LOGGER.warning("Failed to destroy GC runtime: " + e.getMessage());
        }
        arena.close();
        disposed = true;
        LOGGER.fine("Disposed Panama GC runtime with handle: " + nativeHandle);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  // Package-private helper methods for extracted inner classes

  /**
   * Gets the signed value of an I31 object by its native object ID.
   *
   * @param objectId the native object ID
   * @return the signed I31 value
   * @throws IllegalStateException if the native call fails
   */
  int getI31SignedValue(final long objectId) {
    try {
      return (int) i31Get.invokeExact(nativeHandle, objectId, (byte) 1);
    } catch (final Throwable e) {
      throw new IllegalStateException("Failed to get signed I31 value", e);
    }
  }

  /**
   * Gets the unsigned value of an I31 object by its native object ID.
   *
   * @param objectId the native object ID
   * @return the unsigned I31 value
   * @throws IllegalStateException if the native call fails
   */
  int getI31UnsignedValue(final long objectId) {
    try {
      return (int) i31Get.invokeExact(nativeHandle, objectId, (byte) 0);
    } catch (final Throwable e) {
      throw new IllegalStateException("Failed to get unsigned I31 value", e);
    }
  }

  /**
   * Looks up a GC object in the registry by its native object ID.
   *
   * @param objectId the native object ID
   * @return the GC object, or null if not found
   */
  PanamaGcObject lookupGcObject(final long objectId) {
    return objectRegistry.get(objectId);
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

  private int registerStructTypeInternal(final StructType structType) throws GcException {
    final String name = structType.getName() != null ? structType.getName() : "";
    final List<ai.tegmentum.wasmtime4j.gc.FieldDefinition> fields = structType.getFields();

    final MemorySegment nameSegment = arena.allocateFrom(name);
    final MemorySegment fieldNamesSegment = arena.allocate(ADDRESS, fields.size());
    final MemorySegment fieldNameLensSegment = arena.allocate(JAVA_INT, fields.size());
    final MemorySegment fieldTypesSegment = arena.allocate(JAVA_INT, fields.size());
    final MemorySegment fieldMutabilitiesSegment = arena.allocate(JAVA_BYTE, fields.size());

    for (int i = 0; i < fields.size(); i++) {
      final ai.tegmentum.wasmtime4j.gc.FieldDefinition field = fields.get(i);

      // Field name
      final String fieldName = field.getName() != null ? field.getName() : "";
      final MemorySegment fieldNameSegment = arena.allocateFrom(fieldName);
      fieldNamesSegment.setAtIndex(ADDRESS, i, fieldNameSegment);
      fieldNameLensSegment.setAtIndex(
          JAVA_INT, i, fieldName.getBytes(StandardCharsets.UTF_8).length);

      // Field type
      fieldTypesSegment.setAtIndex(JAVA_INT, i, convertFieldTypeToNative(field.getFieldType()));

      // Field mutability
      fieldMutabilitiesSegment.setAtIndex(JAVA_BYTE, i, (byte) (field.isMutable() ? 1 : 0));
    }

    try {
      final int typeId =
          (int)
              registerStructType.invokeExact(
                  nativeHandle,
                  nameSegment,
                  name.getBytes(StandardCharsets.UTF_8).length,
                  fields.size(),
                  fieldNamesSegment,
                  fieldNameLensSegment,
                  fieldTypesSegment,
                  fieldMutabilitiesSegment);

      if (typeId < 0) {
        throw new GcException("Failed to register struct type");
      }

      return typeId;
    } catch (final Throwable e) {
      throw new GcException("Failed to register struct type", e);
    }
  }

  private int registerArrayTypeInternal(final ArrayType arrayType) throws GcException {
    final String name = arrayType.getName() != null ? arrayType.getName() : "";
    final MemorySegment nameSegment = arena.allocateFrom(name);
    final int elementTypeId = convertFieldTypeToNative(arrayType.getElementType());

    try {
      final int typeId =
          (int)
              registerArrayType.invokeExact(
                  nativeHandle,
                  nameSegment,
                  name.getBytes(StandardCharsets.UTF_8).length,
                  elementTypeId,
                  (byte) (arrayType.isMutable() ? 1 : 0));

      if (typeId < 0) {
        throw new GcException("Failed to register array type");
      }

      return typeId;
    } catch (final Throwable e) {
      throw new GcException("Failed to register array type", e);
    }
  }

  private MemorySegment convertGcValuesToNative(final List<GcValue> values) {
    final MemorySegment segment = arena.allocate(JAVA_LONG, values.size());

    for (int i = 0; i < values.size(); i++) {
      final NativeValue nativeValue = convertGcValueToNative(values.get(i));
      segment.setAtIndex(JAVA_LONG, i, nativeValue.value);
    }

    return segment;
  }

  private static class NativeValue {
    final long value;
    final int type;

    NativeValue(final long value, final int type) {
      this.value = value;
      this.type = type;
    }
  }

  private NativeValue convertGcValueToNative(final GcValue value) {
    switch (value.getType()) {
      case I32:
        return new NativeValue(value.asI32(), 0);
      case I64:
        return new NativeValue(value.asI64(), 1);
      case F32:
        return new NativeValue(Float.floatToIntBits(value.asF32()), 2);
      case F64:
        return new NativeValue(Double.doubleToLongBits(value.asF64()), 3);
      case V128:
        // V128 values need special handling - for now, return as zero
        return new NativeValue(0, 4);
      case REFERENCE:
        // Get object ID from reference
        final GcObject refObject = value.asReference();
        if (refObject != null) {
          final long refObjectId = getObjectId(refObject);
          return new NativeValue(refObjectId, 5);
        }
        return new NativeValue(0, 5); // Null reference
      case NULL:
        // Null GC value - return 0 with null type indicator
        return new NativeValue(0, 6);
      default:
        throw new IllegalArgumentException("Unsupported GC value type: " + value.getType());
    }
  }

  private GcValue convertNativeToGcValue(final long value, final int type) {
    switch (type) {
      case 0: // I32
        return GcValue.i32((int) value);
      case 1: // I64
        return GcValue.i64(value);
      case 2: // F32
        return GcValue.f32(Float.intBitsToFloat((int) value));
      case 3: // F64
        return GcValue.f64(Double.longBitsToDouble(value));
      case 4: // V128
        // V128 values need special handling - for now, return zero bytes
        return GcValue.v128(new byte[16]);
      case 5: // Reference - value contains the object_id
        final PanamaGcObject referencedObject = objectRegistry.get(value);
        if (referencedObject != null) {
          return GcValue.reference(referencedObject);
        }
        // Object not in registry - create a wrapper with the object_id for operations
        final PanamaGcObject newRef = new PanamaGcObject(value);
        objectRegistry.put(value, newRef);
        return GcValue.reference(newRef);
      case 6: // Null
        return GcValue.nullValue();
      default:
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
        throw new IllegalArgumentException("Unsupported field type: " + fieldType.getKind());
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
        throw new IllegalArgumentException("Unsupported reference type: " + refType);
    }
  }

  private long getObjectId(final GcObject object) {
    if (object instanceof PanamaGcObject) {
      return ((PanamaGcObject) object).getObjectId();
    } else {
      throw new IllegalArgumentException("Invalid GC object type");
    }
  }

  private GcObject createGcObjectFromId(final long objectId, final GcReferenceType type) {
    switch (type) {
      case I31_REF:
        try {
          final int value = (int) i31Get.invokeExact(nativeHandle, objectId, (byte) 1);
          return new PanamaI31Instance(this, objectId, value);
        } catch (final Throwable e) {
          return new PanamaGcObject(objectId);
        }
      default:
        return new PanamaGcObject(objectId);
    }
  }

  private GcStats convertNativeToGcStats(final MemorySegment statsSegment) {
    final long totalAllocated = (long) totalAllocatedHandle.get(statsSegment, 0L);
    final long totalCollected = (long) totalCollectedHandle.get(statsSegment, 0L);
    final long bytesAllocated = (long) bytesAllocatedHandle.get(statsSegment, 0L);
    final long bytesCollected = (long) bytesCollectedHandle.get(statsSegment, 0L);
    final long minorCollections = (long) minorCollectionsHandle.get(statsSegment, 0L);
    final long majorCollections = (long) majorCollectionsHandle.get(statsSegment, 0L);
    final int currentHeapSize = (int) currentHeapSizeHandle.get(statsSegment, 0L);
    final int peakHeapSize = (int) peakHeapSizeHandle.get(statsSegment, 0L);
    final int maxHeapSize = (int) maxHeapSizeHandle.get(statsSegment, 0L);

    return GcStats.builder()
        .totalAllocated(totalAllocated)
        .totalCollected(totalCollected)
        .bytesAllocated(bytesAllocated)
        .bytesCollected(bytesCollected)
        .minorCollections(minorCollections)
        .majorCollections(majorCollections)
        .currentHeapSize(currentHeapSize)
        .peakHeapSize(peakHeapSize)
        .maxHeapSize(maxHeapSize)
        .build();
  }

  @Override
  public StructInstance refCastStruct(final GcObject object, final StructType targetStructType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetStructType, "targetStructType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = registerStructTypeInternal(targetStructType);

      try {
        final long castObjectId =
            (long) refCast.invokeExact(nativeHandle, objectId, NATIVE_TYPE_STRUCT);
        if (castObjectId == 0) {
          throw new ClassCastException("Struct cast failed");
        }

        return new PanamaStructInstance(this, castObjectId, targetStructType, targetTypeId);
      } catch (final Throwable e) {
        if (e instanceof ClassCastException) {
          throw (ClassCastException) e;
        }
        throw new IllegalStateException("Failed to cast to struct", e);
      }
    } catch (final GcException e) {
      throw new IllegalStateException("Failed to register struct type", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public ArrayInstance refCastArray(final GcObject object, final ArrayType targetArrayType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetArrayType, "targetArrayType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = registerArrayTypeInternal(targetArrayType);

      try {
        final long castObjectId =
            (long) refCast.invokeExact(nativeHandle, objectId, NATIVE_TYPE_ARRAY);
        if (castObjectId == 0) {
          throw new ClassCastException("Array cast failed");
        }

        final int length = (int) arrayLen.invokeExact(nativeHandle, castObjectId);
        return new PanamaArrayInstance(this, castObjectId, targetArrayType, targetTypeId, length);
      } catch (final Throwable e) {
        if (e instanceof ClassCastException) {
          throw (ClassCastException) e;
        }
        throw new IllegalStateException("Failed to cast to array", e);
      }
    } catch (final GcException e) {
      throw new IllegalStateException("Failed to register array type", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean refTestStruct(final GcObject object, final StructType targetStructType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetStructType, "targetStructType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);

      try {
        final byte result = (byte) refTest.invokeExact(nativeHandle, objectId, NATIVE_TYPE_STRUCT);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to test struct type", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public boolean refTestArray(final GcObject object, final ArrayType targetArrayType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetArrayType, "targetArrayType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new IllegalStateException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);

      try {
        final byte result = (byte) refTest.invokeExact(nativeHandle, objectId, NATIVE_TYPE_ARRAY);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to test array type", e);
      }
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public GcReferenceType getRuntimeType(final GcObject object) {
    validateNotDisposed();
    validateNotNull(object, "object");

    // Determine type based on object instance type
    if (object instanceof PanamaStructInstance) {
      return GcReferenceType.STRUCT_REF;
    } else if (object instanceof PanamaArrayInstance) {
      return GcReferenceType.ARRAY_REF;
    } else if (object instanceof PanamaI31Instance) {
      return GcReferenceType.I31_REF;
    } else {
      return GcReferenceType.ANY_REF;
    }
  }

  @Override
  public Optional<GcObject> refCastNullable(
      final GcObject object, final GcReferenceType targetType) {
    if (object == null || isNull(object)) {
      return Optional.empty();
    }

    try {
      return Optional.of(refCast(object, targetType));
    } catch (final ClassCastException e) {
      throw e; // Re-throw cast exceptions for non-null objects
    }
  }

  @Override
  public void copyArrayElements(
      final ArrayInstance sourceArray,
      final int sourceIndex,
      final ArrayInstance destArray,
      final int destIndex,
      final int length)
      throws GcException {
    validateNotDisposed();
    validateNotNull(sourceArray, "sourceArray");
    validateNotNull(destArray, "destArray");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      final long sourceId = getObjectId(sourceArray);
      final long destId = getObjectId(destArray);

      // Copy element by element using get/set since no native copy exists
      for (int i = 0; i < length; i++) {
        try (final java.lang.foreign.Arena arena = java.lang.foreign.Arena.ofConfined()) {
          final java.lang.foreign.MemorySegment resultValue =
              arena.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);
          final java.lang.foreign.MemorySegment resultType =
              arena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);

          final int getResult =
              (int)
                  arrayGet.invokeExact(
                      nativeHandle, sourceId, sourceIndex + i, resultValue, resultType);
          if (getResult != 0) {
            throw new GcException("Failed to get array element at index " + (sourceIndex + i));
          }

          final long value = resultValue.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
          final int valueType = resultType.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);

          final int setResult =
              (int) arraySet.invokeExact(nativeHandle, destId, destIndex + i, value, valueType);
          if (setResult != 0) {
            throw new GcException("Failed to set array element at index " + (destIndex + i));
          }
        }
      }
    } catch (final Throwable e) {
      if (e instanceof GcException) {
        throw (GcException) e;
      }
      throw new GcException("Failed to copy array elements", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public void fillArrayElements(
      final ArrayInstance array, final int startIndex, final int length, final GcValue value)
      throws GcException {
    validateNotDisposed();
    validateNotNull(array, "array");
    validateNotNull(value, "value");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(array);
      final NativeValue nativeValue = convertGcValueToNative(value);

      // Fill element by element since no native fill exists
      for (int i = 0; i < length; i++) {
        final int result =
            (int)
                arraySet.invokeExact(
                    nativeHandle, objectId, startIndex + i, nativeValue.value, nativeValue.type);
        if (result != 0) {
          throw new GcException("Failed to fill array element at index " + (startIndex + i));
        }
      }
    } catch (final Throwable e) {
      if (e instanceof GcException) {
        throw (GcException) e;
      }
      throw new GcException("Failed to fill array elements", e);
    } finally {
      lock.writeLock().unlock();
    }
  }

}
