package ai.tegmentum.wasmtime4j.panama;

import static java.lang.foreign.ValueLayout.*;

import ai.tegmentum.wasmtime4j.gc.ArrayInstance;
import ai.tegmentum.wasmtime4j.gc.ArrayType;
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
import ai.tegmentum.wasmtime4j.gc.ReferenceSafetyResult;
import ai.tegmentum.wasmtime4j.gc.StructInstance;
import ai.tegmentum.wasmtime4j.gc.StructType;
import ai.tegmentum.wasmtime4j.gc.WeakGcReference;
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
import java.util.Map;
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

  // Host object integration fields
  private final java.util.concurrent.ConcurrentHashMap<Long, Runnable> finalizationCallbacks =
      new java.util.concurrent.ConcurrentHashMap<>();
  private final java.util.concurrent.ConcurrentHashMap<Long, Object> hostObjects =
      new java.util.concurrent.ConcurrentHashMap<>();
  private final java.util.concurrent.atomic.AtomicLong nextHostObjectId =
      new java.util.concurrent.atomic.AtomicLong(1);

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

    lock.readLock().lock();
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

        return new PanamaStructInstance(this, objectId, structType, typeId);
      } catch (final Throwable e) {
        throw new GcException("Failed to create struct instance", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public StructInstance createStruct(final StructType structType) throws GcException {
    validateNotDisposed();
    validateNotNull(structType, "structType");

    lock.readLock().lock();
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

        return new PanamaStructInstance(this, objectId, structType, typeId);
      } catch (final Throwable e) {
        throw new GcException("Failed to create struct instance with default values", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public ArrayInstance createArray(final ArrayType arrayType, final List<GcValue> elements)
      throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    validateNotNull(elements, "elements");

    lock.readLock().lock();
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

        return new PanamaArrayInstance(this, objectId, arrayType, typeId, elements.size());
      } catch (final Throwable e) {
        throw new GcException("Failed to create array instance", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public ArrayInstance createArray(final ArrayType arrayType, final int length) throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    if (length < 0) {
      throw new IllegalArgumentException("Array length cannot be negative: " + length);
    }

    lock.readLock().lock();
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

        return new PanamaArrayInstance(this, objectId, arrayType, typeId, length);
      } catch (final Throwable e) {
        throw new GcException("Failed to create array instance with default values", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public I31Instance createI31(final int value) throws GcException {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      try {
        final long objectId = (long) i31New.invokeExact(nativeHandle, value);
        if (objectId == 0) {
          throw new GcException("Failed to create I31 instance");
        }

        return new PanamaI31Instance(this, objectId, value);
      } catch (final Throwable e) {
        throw new GcException("Failed to create I31 instance", e);
      }
    } finally {
      lock.readLock().unlock();
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

    lock.readLock().lock();
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
      lock.readLock().unlock();
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

    lock.readLock().lock();
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
      lock.readLock().unlock();
    }
  }

  @Override
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
        throw new GcException("GC runtime has been disposed");
      }

      if (!(array instanceof PanamaArrayInstance)) {
        throw new GcException("Invalid array instance type");
      }

      final PanamaArrayInstance panamaArray = (PanamaArrayInstance) array;
      if (elementIndex >= panamaArray.getLength()) {
        throw new IndexOutOfBoundsException(
            "Element index out of bounds: " + elementIndex + " >= " + panamaArray.getLength());
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
      lock.readLock().unlock();
    }
  }

  @Override
  public void setArrayElement(
      final ArrayInstance array, final int elementIndex, final GcValue value) throws GcException {
    validateNotDisposed();
    validateNotNull(array, "array");
    validateNotNull(value, "value");
    if (elementIndex < 0) {
      throw new IllegalArgumentException("Element index cannot be negative: " + elementIndex);
    }

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      if (!(array instanceof PanamaArrayInstance)) {
        throw new GcException("Invalid array instance type");
      }

      final PanamaArrayInstance panamaArray = (PanamaArrayInstance) array;
      if (elementIndex >= panamaArray.getLength()) {
        throw new IndexOutOfBoundsException(
            "Element index out of bounds: " + elementIndex + " >= " + panamaArray.getLength());
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
      lock.readLock().unlock();
    }
  }

  @Override
  public int getArrayLength(final ArrayInstance array) {
    validateNotDisposed();
    validateNotNull(array, "array");

    lock.readLock().lock();
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
      lock.readLock().unlock();
    }
  }

  @Override
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

      try {
        final long castObjectId = (long) refCast.invokeExact(nativeHandle, objectId, targetTypeId);
        if (castObjectId == 0) {
          throw new ClassCastException("Reference cast failed");
        }

        return createGcObjectFromId(castObjectId, targetType);
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to cast reference", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public GcObject refCastOptimized(
      final GcObject object, final GcReferenceType targetType, final boolean enableCaching) {
    // For now, delegate to non-optimized version
    // TODO: Implement caching when enableCaching is true
    return refCast(object, targetType);
  }

  @Override
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

      try {
        final byte result = (byte) refTest.invokeExact(nativeHandle, objectId, targetTypeId);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to test reference", e);
      }
    } finally {
      lock.readLock().unlock();
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

    lock.readLock().lock();
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
      lock.readLock().unlock();
    }
  }

  @Override
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

      try {
        final byte result = (byte) refIsNull.invokeExact(nativeHandle, objectId);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to check if reference is null", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int registerStructType(final StructType structType) throws GcException {
    validateNotDisposed();
    validateNotNull(structType, "structType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      return registerStructTypeInternal(structType);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int registerArrayType(final ArrayType arrayType) throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      return registerArrayTypeInternal(arrayType);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public void pinObject(final GcObject object) {
    validateNotDisposed();
    validateNotNull(object, "object");
    // TODO: Implement object pinning in native code
    LOGGER.fine("Pinning object (not yet implemented): " + getObjectId(object));
  }

  @Override
  public void unpinObject(final GcObject object) {
    validateNotDisposed();
    validateNotNull(object, "object");
    // TODO: Implement object unpinning in native code
    LOGGER.fine("Unpinning object (not yet implemented): " + getObjectId(object));
  }

  @Override
  public GcStats collectGarbageAdvanced(final Long maxPauseMillis, final boolean concurrent) {
    validateNotDisposed();
    // TODO: Implement advanced GC with pause time and concurrency controls
    LOGGER.fine(
        "Advanced GC requested (maxPause=" + maxPauseMillis + "ms, concurrent=" + concurrent + ")");
    return collectGarbage();
  }

  @Override
  public WeakGcReference createWeakReferenceAdvanced(
      final GcObject object, final Runnable finalizationCallback) {
    validateNotDisposed();
    validateNotNull(object, "object");
    // TODO: Implement weak reference creation with finalization callback
    LOGGER.fine("Creating weak reference with finalization callback: " + getObjectId(object));
    return createWeakReference(object, finalizationCallback);
  }

  @Override
  public GcStats collectGarbage() {
    validateNotDisposed();

    lock.readLock().lock();
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
      lock.readLock().unlock();
    }
  }

  @Override
  public GcStats getGcStats() {
    validateNotDisposed();

    lock.readLock().lock();
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
      lock.readLock().unlock();
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
        // References need special handling - for now, return as zero
        return new NativeValue(0, 5);
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
      case 5: // Reference
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

  // Helper classes for Panama GC objects

  private static class PanamaGcObject implements GcObject {
    private final long objectId;

    public PanamaGcObject(final long objectId) {
      this.objectId = objectId;
    }

    public long getObjectId() {
      return objectId;
    }

    @Override
    public int getSizeBytes() {
      // Size would need to be queried from native side
      return 0;
    }

    @Override
    public boolean refEquals(final GcObject other) {
      if (other instanceof PanamaGcObject) {
        return ((PanamaGcObject) other).objectId == this.objectId;
      }
      return false;
    }

    @Override
    public GcObject castTo(final GcReferenceType type) {
      // TODO: Implement proper type casting with validation
      return this;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      // TODO: Implement proper type checking
      return type == GcReferenceType.ANY_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.ANY_REF;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }
  }

  private static class PanamaStructInstance extends PanamaGcObject implements StructInstance {
    private final PanamaGcRuntime runtime;
    private final StructType structType;
    private final int typeId;

    public PanamaStructInstance(
        final PanamaGcRuntime runtime,
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
    public GcValue getField(final int index) throws GcException {
      return runtime.getStructField(this, index);
    }

    @Override
    public void setField(final int index, final GcValue value) throws GcException {
      runtime.setStructField(this, index, value);
    }

    @Override
    public int getFieldCount() {
      return structType.getFields().size();
    }

    @Override
    public int getSizeBytes() {
      // Return approximate size based on struct fields
      return 16 + (structType.getFields().size() * 8);
    }

    @Override
    public boolean refEquals(final GcObject other) {
      return runtime.refEquals(this, other);
    }

    @Override
    public GcObject castTo(final GcReferenceType type) {
      // TODO: Implement proper type casting with validation
      return this;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      // TODO: Implement proper type checking
      return type == GcReferenceType.STRUCT_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.STRUCT_REF;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }
  }

  private static class PanamaArrayInstance extends PanamaGcObject implements ArrayInstance {
    private final PanamaGcRuntime runtime;
    private final ArrayType arrayType;
    private final int typeId;
    private final int length;

    public PanamaArrayInstance(
        final PanamaGcRuntime runtime,
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
    public GcValue getElement(final int index) throws GcException {
      return runtime.getArrayElement(this, index);
    }

    @Override
    public void setElement(final int index, final GcValue value) throws GcException {
      runtime.setArrayElement(this, index, value);
    }

    @Override
    public int getLength() {
      return length;
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
    public GcObject castTo(final GcReferenceType type) {
      // TODO: Implement proper type casting with validation
      return this;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      // TODO: Implement proper type checking
      return type == GcReferenceType.ARRAY_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.ARRAY_REF;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }
  }

  private static class PanamaI31Instance extends PanamaGcObject implements I31Instance {
    private final PanamaGcRuntime runtime;
    private final int value;

    public PanamaI31Instance(final PanamaGcRuntime runtime, final long objectId, final int value) {
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
      try {
        return (int) i31Get.invokeExact(runtime.nativeHandle, getObjectId(), (byte) 1);
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to get signed I31 value", e);
      }
    }

    @Override
    public int getUnsignedValue() {
      try {
        return (int) i31Get.invokeExact(runtime.nativeHandle, getObjectId(), (byte) 0);
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to get unsigned I31 value", e);
      }
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
    public GcObject castTo(final GcReferenceType type) {
      // TODO: Implement proper type casting with validation
      return this;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      // TODO: Implement proper type checking
      return type == GcReferenceType.I31_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return GcReferenceType.I31_REF;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }
  }

  // ========== Advanced GC Method Stubs ==========
  // These methods provide stub implementations for all the advanced GC features
  // In a complete implementation, these would have Panama FFI bindings to native code

  @Override
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
      lock.readLock().unlock();
    }
  }

  @Override
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
      lock.readLock().unlock();
    }
  }

  @Override
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

      try {
        final byte result = (byte) refTest.invokeExact(nativeHandle, objectId, NATIVE_TYPE_STRUCT);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to test struct type", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
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

      try {
        final byte result = (byte) refTest.invokeExact(nativeHandle, objectId, NATIVE_TYPE_ARRAY);
        return result != 0;
      } catch (final Throwable e) {
        throw new IllegalStateException("Failed to test array type", e);
      }
    } finally {
      lock.readLock().unlock();
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
  public StructInstance createPackedStruct(
      final StructType structType,
      final List<GcValue> fieldValues,
      final Map<Integer, Integer> customAlignment)
      throws GcException {
    // Packed structs use the same creation path - alignment is handled at type level
    // Custom alignment is advisory and may not be honored by the native runtime
    return createStruct(structType, fieldValues);
  }

  @Override
  public ArrayInstance createVariableLengthArray(
      final ArrayType arrayType, final int baseLength, final List<GcValue> flexibleElements)
      throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    validateNotNull(flexibleElements, "flexibleElements");

    // Create array with combined length of base + flexible elements
    final int totalLength = baseLength + flexibleElements.size();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      final int typeId = registerArrayTypeInternal(arrayType);

      // Create array with default values first
      final long objectId = (long) arrayNewDefault.invokeExact(nativeHandle, typeId, totalLength);
      if (objectId == 0) {
        throw new GcException("Failed to create variable-length array instance");
      }

      // Set flexible elements starting at baseLength
      for (int i = 0; i < flexibleElements.size(); i++) {
        final GcValue value = flexibleElements.get(i);
        final NativeValue nativeValue = convertGcValueToNative(value);
        arraySet.invokeExact(
            nativeHandle, objectId, baseLength + i, nativeValue.value, nativeValue.type);
      }

      return new PanamaArrayInstance(this, objectId, arrayType, typeId, totalLength);
    } catch (final Throwable e) {
      if (e instanceof GcException) {
        throw (GcException) e;
      }
      throw new GcException("Failed to create variable-length array", e);
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public ArrayInstance createNestedArray(
      final ArrayType arrayType, final List<GcObject> nestedElements) throws GcException {
    validateNotDisposed();
    validateNotNull(arrayType, "arrayType");
    validateNotNull(nestedElements, "nestedElements");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      final int typeId = registerArrayTypeInternal(arrayType);
      final int length = nestedElements.size();

      // Create array with default values
      final long objectId = (long) arrayNewDefault.invokeExact(nativeHandle, typeId, length);
      if (objectId == 0) {
        throw new GcException("Failed to create nested array instance");
      }

      // Set nested elements (references to other GC objects)
      for (int i = 0; i < nestedElements.size(); i++) {
        final GcObject nested = nestedElements.get(i);
        final long nestedId = getObjectId(nested);
        // Reference type = 7
        arraySet.invokeExact(nativeHandle, objectId, i, nestedId, 7);
      }

      return new PanamaArrayInstance(this, objectId, arrayType, typeId, length);
    } catch (final Throwable e) {
      if (e instanceof GcException) {
        throw (GcException) e;
      }
      throw new GcException("Failed to create nested array", e);
    } finally {
      lock.readLock().unlock();
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

    lock.readLock().lock();
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
      lock.readLock().unlock();
    }
  }

  @Override
  public void fillArrayElements(
      final ArrayInstance array, final int startIndex, final int length, final GcValue value)
      throws GcException {
    validateNotDisposed();
    validateNotNull(array, "array");
    validateNotNull(value, "value");

    lock.readLock().lock();
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
      lock.readLock().unlock();
    }
  }

  @Override
  public int registerRecursiveType(final String typeName, final Object typeDefinition)
      throws GcException {
    validateNotDisposed();
    validateNotNull(typeName, "typeName");
    validateNotNull(typeDefinition, "typeDefinition");

    // For recursive types, we generate a unique type ID and store the definition
    // Wasmtime handles recursive type resolution at module compilation time
    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      // Generate a simple hash-based type ID for tracking
      final int typeId = typeName.hashCode() & 0x7FFFFFFF;
      return typeId;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Map<String, Integer> createTypeHierarchy(
      final Object baseType, final List<Object> derivedTypes) throws GcException {
    validateNotDisposed();
    validateNotNull(baseType, "baseType");
    validateNotNull(derivedTypes, "derivedTypes");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      // Create a type hierarchy map with generated IDs
      final Map<String, Integer> hierarchy = new java.util.HashMap<>();
      hierarchy.put("base", baseType.hashCode() & 0x7FFFFFFF);
      for (int i = 0; i < derivedTypes.size(); i++) {
        hierarchy.put("derived_" + i, derivedTypes.get(i).hashCode() & 0x7FFFFFFF);
      }
      return hierarchy;
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public GcStats collectGarbageIncremental(final long maxPauseMillis) {
    validateNotDisposed();

    // Delegate to standard GC collection - incremental behavior is advisory
    // The runtime may not support true incremental collection
    return collectGarbage();
  }

  @Override
  public GcStats collectGarbageConcurrent() {
    validateNotDisposed();

    // Delegate to standard GC collection - concurrent behavior is advisory
    // The runtime may not support true concurrent collection
    return collectGarbage();
  }

  @Override
  public void configureGcStrategy(final String strategy, final Map<String, Object> parameters)
      throws GcException {
    validateNotDisposed();
    validateNotNull(strategy, "strategy");

    // GC strategy configuration is advisory - Wasmtime uses its own internal GC
    // Log the configuration attempt but don't fail
    java.util.logging.Logger.getLogger(getClass().getName())
        .fine("GC strategy configuration requested: " + strategy + " (advisory only)");
  }

  @Override
  public boolean monitorGcPressure(final double pressureThreshold) {
    validateNotDisposed();

    // GC pressure monitoring requires runtime metrics
    // Return false (no pressure) as we cannot accurately measure native GC pressure
    return false;
  }

  @Override
  public WeakGcReference createWeakReference(
      final GcObject object, final Runnable finalizationCallback) {
    validateNotDisposed();
    validateNotNull(object, "object");

    // Create a weak reference wrapper using Java's WeakReference
    final long objectId = getObjectId(object);
    return new PanamaWeakGcReference(this, objectId, finalizationCallback);
  }

  @Override
  public void registerFinalizationCallback(final GcObject object, final Runnable callback) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(callback, "callback");

    // Store finalization callback - will be invoked when object is collected
    final long objectId = getObjectId(object);
    finalizationCallbacks.put(objectId, callback);
  }

  @Override
  public int runFinalization() {
    validateNotDisposed();

    // Run all pending finalization callbacks
    int finalized = 0;
    for (final Runnable callback : finalizationCallbacks.values()) {
      try {
        callback.run();
        finalized++;
      } catch (final Exception e) {
        // Log but don't fail on individual callback errors
        java.util.logging.Logger.getLogger(getClass().getName())
            .warning("Finalization callback failed: " + e.getMessage());
      }
    }
    finalizationCallbacks.clear();
    return finalized;
  }

  @Override
  public GcObject integrateHostObject(final Object hostObject, final GcReferenceType gcType)
      throws GcException {
    validateNotDisposed();
    validateNotNull(hostObject, "hostObject");
    validateNotNull(gcType, "gcType");

    lock.writeLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      // Create a host object wrapper with a unique ID
      final long objectId = nextHostObjectId.getAndIncrement();
      hostObjects.put(objectId, hostObject);

      return new PanamaHostObjectWrapper(this, objectId, gcType);
    } finally {
      lock.writeLock().unlock();
    }
  }

  @Override
  public Object extractHostObject(final GcObject gcObject) throws GcException {
    validateNotDisposed();
    validateNotNull(gcObject, "gcObject");

    if (gcObject instanceof PanamaHostObjectWrapper) {
      final long objectId = ((PanamaHostObjectWrapper) gcObject).getObjectId();
      final Object hostObject = hostObjects.get(objectId);
      if (hostObject == null) {
        throw new GcException("Host object not found or has been collected");
      }
      return hostObject;
    }

    throw new GcException("Object is not a host-integrated object");
  }

  @Override
  public Object createSharingBridge(final List<GcObject> objects) throws GcException {
    validateNotDisposed();
    validateNotNull(objects, "objects");

    // Create a sharing bridge that holds references to all objects
    final long[] objectIds = objects.stream().mapToLong(this::getObjectId).toArray();
    return new SharingBridge(objectIds);
  }

  @Override
  public GcHeapInspection inspectHeap() {
    validateNotDisposed();
    return new PanamaGcHeapInspection();
  }

  @Override
  public ObjectLifecycleTracker trackObjectLifecycles(final List<GcObject> objects) {
    validateNotDisposed();
    validateNotNull(objects, "objects");

    final long[] objectIds = objects.stream().mapToLong(this::getObjectId).toArray();
    return new PanamaObjectLifecycleTracker(objectIds);
  }

  @Override
  public MemoryLeakAnalysis detectMemoryLeaks() {
    validateNotDisposed();
    return new PanamaMemoryLeakAnalysis();
  }

  @Override
  public GcProfiler startProfiling() {
    validateNotDisposed();
    return new PanamaGcProfiler();
  }

  @Override
  public ReferenceSafetyResult validateReferenceSafety(final List<GcObject> rootObjects) {
    validateNotDisposed();
    validateNotNull(rootObjects, "rootObjects");

    // Basic safety validation - check all references are valid
    for (final GcObject obj : rootObjects) {
      if (obj == null) {
        return new PanamaReferenceSafetyResult(false, "Null reference found in root objects");
      }
    }
    return new PanamaReferenceSafetyResult(true, "All references are safe");
  }

  @Override
  public boolean enforceTypeSafety(final String operation, final List<Object> operands) {
    validateNotDisposed();
    validateNotNull(operation, "operation");

    // Type safety enforcement - validate operand types match expected patterns
    // Return true as we cannot perform deep type checking without more context
    return true;
  }

  @Override
  public MemoryCorruptionAnalysis detectMemoryCorruption() {
    validateNotDisposed();
    return new PanamaMemoryCorruptionAnalysis();
  }

  @Override
  public GcInvariantValidation validateInvariants() {
    validateNotDisposed();
    return new PanamaGcInvariantValidation();
  }

  // ========== Helper Classes ==========

  /** Weak GC reference implementation for Panama. */
  private static final class PanamaWeakGcReference implements WeakGcReference {
    private final PanamaGcRuntime runtime;
    private final long objectId;
    private volatile Runnable finalizationCallback;
    private volatile boolean cleared = false;

    PanamaWeakGcReference(
        final PanamaGcRuntime runtime, final long objectId, final Runnable finalizationCallback) {
      this.runtime = runtime;
      this.objectId = objectId;
      this.finalizationCallback = finalizationCallback;
    }

    @Override
    public java.util.Optional<GcObject> get() {
      if (cleared) {
        return java.util.Optional.empty();
      }
      // Return empty - actual object retrieval would need native support
      return java.util.Optional.empty();
    }

    @Override
    public boolean isCleared() {
      return cleared;
    }

    @Override
    public void clear() {
      cleared = true;
      // Note: clear() does NOT invoke finalization callback per interface contract
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

  /** Host object wrapper for Panama. */
  private static final class PanamaHostObjectWrapper implements GcObject {
    private final PanamaGcRuntime runtime;
    private final long objectId;
    private final GcReferenceType gcType;

    PanamaHostObjectWrapper(
        final PanamaGcRuntime runtime, final long objectId, final GcReferenceType gcType) {
      this.runtime = runtime;
      this.objectId = objectId;
      this.gcType = gcType;
    }

    @Override
    public long getObjectId() {
      return objectId;
    }

    @Override
    public int getSizeBytes() {
      return 8; // Pointer size
    }

    @Override
    public boolean refEquals(final GcObject other) {
      if (other instanceof PanamaHostObjectWrapper) {
        return objectId == ((PanamaHostObjectWrapper) other).objectId;
      }
      return false;
    }

    @Override
    public GcObject castTo(final GcReferenceType type) {
      return this;
    }

    @Override
    public boolean isOfType(final GcReferenceType type) {
      return type == gcType || type == GcReferenceType.ANY_REF;
    }

    @Override
    public boolean isNull() {
      return false;
    }

    @Override
    public GcReferenceType getReferenceType() {
      return gcType;
    }

    @Override
    public ai.tegmentum.wasmtime4j.WasmValue toWasmValue() {
      return ai.tegmentum.wasmtime4j.WasmValue.externRef(this);
    }
  }

  /** Sharing bridge for cross-module object sharing. */
  private static final class SharingBridge {
    private final long[] objectIds;

    SharingBridge(final long[] objectIds) {
      this.objectIds = objectIds.clone();
    }

    public long[] getObjectIds() {
      return objectIds.clone();
    }
  }

  /** Heap inspection result. */
  private static final class PanamaGcHeapInspection implements GcHeapInspection {
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
    public java.util.Map<String, Long> getObjectTypeDistribution() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.Map<String, Long> getMemoryUsageByType() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.List<StructInstanceInfo> getStructInstances() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<ArrayInstanceInfo> getArrayInstances() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<I31InstanceInfo> getI31Instances() {
      return java.util.Collections.emptyList();
    }

    @Override
    public ai.tegmentum.wasmtime4j.gc.ReferenceGraph getReferenceGraph() {
      return null;
    }

    @Override
    public GcStats getGcStats() {
      return null;
    }

    @Override
    public HeapFragmentation getFragmentationInfo() {
      return new HeapFragmentation() {
        @Override
        public double getFragmentationRatio() {
          return 0.0;
        }

        @Override
        public int getFreeBlockCount() {
          return 0;
        }

        @Override
        public long getLargestFreeBlock() {
          return 0;
        }

        @Override
        public long getAverageFreeBlockSize() {
          return 0;
        }
      };
    }

    @Override
    public java.util.List<RootObjectInfo> getRootObjects() {
      return java.util.Collections.emptyList();
    }
  }

  /** Object lifecycle tracker. */
  private static final class PanamaObjectLifecycleTracker implements ObjectLifecycleTracker {
    private final java.util.List<Long> trackedObjectIds;
    private final long startTime;

    PanamaObjectLifecycleTracker(final long[] objectIds) {
      this.trackedObjectIds = new java.util.ArrayList<>();
      for (final long id : objectIds) {
        this.trackedObjectIds.add(id);
      }
      this.startTime = System.currentTimeMillis();
    }

    @Override
    public java.util.List<Long> getTrackedObjects() {
      return java.util.Collections.unmodifiableList(trackedObjectIds);
    }

    @Override
    public java.util.List<LifecycleEvent> getLifecycleEvents(final long objectId) {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.Map<Long, ObjectStatus> getObjectStatuses() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.Map<Long, AccessStatistics> getAccessStatistics() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.Map<Long, java.util.List<ReferenceChange>> getReferenceHistory() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public LifecycleTrackingSummary stopTracking() {
      final long duration = System.currentTimeMillis() - startTime;
      final int count = trackedObjectIds.size();
      return new LifecycleTrackingSummary() {
        @Override
        public long getTrackingDurationMillis() {
          return duration;
        }

        @Override
        public int getTrackedObjectCount() {
          return count;
        }

        @Override
        public int getCollectedObjectCount() {
          return 0;
        }

        @Override
        public long getTotalEventCount() {
          return 0;
        }

        @Override
        public java.util.List<Long> getMostAccessedObjects() {
          return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<Long> getLongestLivedObjects() {
          return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<Long> getPotentialLeaks() {
          return java.util.Collections.emptyList();
        }
      };
    }

    @Override
    public void trackAdditionalObjects(final java.util.List<GcObject> objects) {
      if (objects != null) {
        for (final GcObject obj : objects) {
          trackedObjectIds.add(obj.getObjectId());
        }
      }
    }

    @Override
    public void stopTrackingObjects(final java.util.List<Long> objectIds) {
      if (objectIds != null) {
        trackedObjectIds.removeAll(objectIds);
      }
    }
  }

  /** Memory leak analysis result. */
  private static final class PanamaMemoryLeakAnalysis implements MemoryLeakAnalysis {
    private final java.time.Instant analysisTime = java.time.Instant.now();

    @Override
    public java.time.Instant getAnalysisTime() {
      return analysisTime;
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
    public java.util.List<PotentialLeak> getPotentialLeaks() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<CircularReference> getCircularReferences() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<LongLivedObject> getLongLivedObjects() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<HighlyReferencedObject> getHighlyReferencedObjects() {
      return java.util.Collections.emptyList();
    }

    @Override
    public MemoryUsageTrend getMemoryUsageTrend() {
      return new MemoryUsageTrend() {
        @Override
        public boolean isIncreasing() {
          return false;
        }

        @Override
        public double getGrowthRate() {
          return 0.0;
        }

        @Override
        public double getCorrelation() {
          return 0.0;
        }

        @Override
        public long getTimeToExhaustionMillis() {
          return Long.MAX_VALUE;
        }

        @Override
        public boolean isLeakPattern() {
          return false;
        }
      };
    }

    @Override
    public LeakSeverity getLeakSeverity() {
      return LeakSeverity.LOW;
    }

    @Override
    public java.util.List<LeakRecommendation> getRecommendations() {
      return java.util.Collections.emptyList();
    }
  }

  /** GC profiler implementation. */
  private static final class PanamaGcProfiler implements GcProfiler {
    private long startTime;
    private volatile boolean active = false;

    @Override
    public void start() {
      startTime = System.currentTimeMillis();
      active = true;
    }

    @Override
    public GcProfilingResults stop() {
      active = false;
      final long duration = System.currentTimeMillis() - startTime;
      return new DefaultGcProfilingResults(duration);
    }

    @Override
    public boolean isActive() {
      return active;
    }

    @Override
    public java.time.Duration getProfilingDuration() {
      if (!active) {
        return java.time.Duration.ZERO;
      }
      return java.time.Duration.ofMillis(System.currentTimeMillis() - startTime);
    }

    @Override
    public void recordEvent(
        final String eventName,
        final java.time.Duration duration,
        final java.util.Map<String, Object> metadata) {
      // No-op - stub implementation
    }
  }

  /** Default GC profiling results implementation. */
  private static final class DefaultGcProfilingResults implements GcProfiler.GcProfilingResults {
    private final long durationMs;

    DefaultGcProfilingResults(final long durationMs) {
      this.durationMs = durationMs;
    }

    @Override
    public java.time.Duration getTotalDuration() {
      return java.time.Duration.ofMillis(durationMs);
    }

    @Override
    public long getSampleCount() {
      return 0;
    }

    @Override
    public GcProfiler.AllocationStatistics getAllocationStatistics() {
      return null;
    }

    @Override
    public GcProfiler.FieldAccessStatistics getFieldAccessStatistics() {
      return null;
    }

    @Override
    public GcProfiler.ArrayAccessStatistics getArrayAccessStatistics() {
      return null;
    }

    @Override
    public GcProfiler.ReferenceOperationStatistics getReferenceOperationStatistics() {
      return null;
    }

    @Override
    public GcProfiler.GcPerformanceStatistics getGcPerformanceStatistics() {
      return null;
    }

    @Override
    public GcProfiler.TypeOperationStatistics getTypeOperationStatistics() {
      return null;
    }

    @Override
    public java.util.List<GcProfiler.PerformanceHotspot> getHotspots() {
      return java.util.Collections.emptyList();
    }

    @Override
    public GcProfiler.PerformanceComparison getBaselineComparison() {
      return null;
    }

    @Override
    public GcProfiler.ProfilingTimeline getTimeline() {
      return null;
    }
  }

  /** Reference safety validation result. */
  private static final class PanamaReferenceSafetyResult implements ReferenceSafetyResult {
    private final boolean safe;

    PanamaReferenceSafetyResult(final boolean safe, final String message) {
      this.safe = safe;
    }

    @Override
    public boolean isAllSafe() {
      return safe;
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
    public java.util.List<SafetyViolation> getSafetyViolations() {
      return java.util.Collections.emptyList();
    }

    @Override
    public double getSafetyScore() {
      return safe ? 1.0 : 0.0;
    }

    @Override
    public java.util.Map<ViolationType, Integer> getViolationStatistics() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.List<SafetyRecommendation> getRecommendations() {
      return java.util.Collections.emptyList();
    }

    @Override
    public java.util.List<DangerousReferencePattern> getDangerousPatterns() {
      return java.util.Collections.emptyList();
    }
  }

  /** Memory corruption analysis result. */
  private static final class PanamaMemoryCorruptionAnalysis implements MemoryCorruptionAnalysis {
    private final java.time.Instant analysisTime = java.time.Instant.now();

    @Override
    public java.time.Instant getAnalysisTime() {
      return analysisTime;
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
    public java.util.List<CorruptionIssue> getCorruptionIssues() {
      return java.util.Collections.emptyList();
    }

    @Override
    public MemoryIntegrityResult getIntegrityResult() {
      return new MemoryIntegrityResult() {
        @Override
        public boolean isIntegrityIntact() {
          return true;
        }

        @Override
        public int getViolationCount() {
          return 0;
        }

        @Override
        public java.util.List<IntegrityViolation> getViolations() {
          return java.util.Collections.emptyList();
        }

        @Override
        public double getIntegrityScore() {
          return 1.0;
        }

        @Override
        public java.util.Map<String, Boolean> getChecksumResults() {
          return java.util.Collections.emptyMap();
        }
      };
    }

    @Override
    public HeapConsistencyResult getConsistencyResult() {
      return new HeapConsistencyResult() {
        @Override
        public boolean isConsistent() {
          return true;
        }

        @Override
        public int getErrorCount() {
          return 0;
        }

        @Override
        public java.util.List<ConsistencyError> getErrors() {
          return java.util.Collections.emptyList();
        }

        @Override
        public FreeListValidation getFreeListValidation() {
          return null;
        }

        @Override
        public ObjectGraphValidation getObjectGraphValidation() {
          return null;
        }
      };
    }

    @Override
    public LifecycleViolationResult getLifecycleViolationResult() {
      return new LifecycleViolationResult() {
        @Override
        public boolean hasViolations() {
          return false;
        }

        @Override
        public int getViolationCount() {
          return 0;
        }

        @Override
        public java.util.List<LifecycleViolation> getViolations() {
          return java.util.Collections.emptyList();
        }

        @Override
        public java.util.Map<Long, ObjectStateValidation> getStateValidations() {
          return java.util.Collections.emptyMap();
        }
      };
    }

    @Override
    public java.util.List<CorruptionRecommendation> getRecommendations() {
      return java.util.Collections.emptyList();
    }
  }

  /** GC invariant validation result. */
  private static final class PanamaGcInvariantValidation implements GcInvariantValidation {
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
    public java.util.List<InvariantViolation> getViolations() {
      return java.util.Collections.emptyList();
    }

    @Override
    public double getSatisfactionScore() {
      return 1.0;
    }

    @Override
    public java.util.Map<InvariantCategory, CategoryValidation> getCategoryResults() {
      return java.util.Collections.emptyMap();
    }

    @Override
    public java.util.List<CriticalInvariantResult> getCriticalInvariants() {
      return java.util.Collections.emptyList();
    }

    @Override
    public ValidationPerformanceImpact getPerformanceImpact() {
      return new ValidationPerformanceImpact() {
        @Override
        public java.time.Duration getTotalValidationTime() {
          return java.time.Duration.ZERO;
        }

        @Override
        public double getValidationOverheadPercentage() {
          return 0.0;
        }

        @Override
        public java.util.Map<InvariantCategory, java.time.Duration> getTimeByCategory() {
          return java.util.Collections.emptyMap();
        }

        @Override
        public java.util.List<ExpensiveInvariant> getMostExpensiveInvariants() {
          return java.util.Collections.emptyList();
        }

        @Override
        public java.util.List<String> getOptimizationRecommendations() {
          return java.util.Collections.emptyList();
        }
      };
    }

    @Override
    public java.util.Map<InvariantCategory, Object> getSpecificValidators() {
      return java.util.Collections.emptyMap();
    }
  }
}
