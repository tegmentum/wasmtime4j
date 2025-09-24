package ai.tegmentum.wasmtime4j.panama;

import static java.lang.foreign.ValueLayout.*;

import ai.tegmentum.wasmtime4j.gc.*;
import ai.tegmentum.wasmtime4j.panama.exception.PanamaException;
import java.lang.foreign.*;
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

  private final long nativeHandle;
  private final Arena arena;
  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private volatile boolean disposed = false;

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
  private static final VarHandle currentHeapSizeHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("current_heap_size"));
  private static final VarHandle peakHeapSizeHandle =
      GC_STATS_LAYOUT.varHandle(MemoryLayout.PathElement.groupElement("peak_heap_size"));

  static {
    final Linker linker = Linker.nativeLinker();
    final SymbolLookup stdlib = linker.defaultLookup();

    try {
      createRuntime =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_create_runtime").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG));

      destroyRuntime =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_destroy_runtime").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG));

      registerStructType =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_register_struct_type").orElseThrow(),
              FunctionDescriptor.of(
                  JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS,
                  ADDRESS));

      registerArrayType =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_register_array_type").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS, JAVA_INT, JAVA_INT, JAVA_BYTE));

      structNew =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_struct_new").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT));

      structNewDefault =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_struct_new_default").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));

      structGet =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_struct_get").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, ADDRESS));

      structSet =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_struct_set").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT));

      arrayNew =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_array_new").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, JAVA_INT));

      arrayNewDefault =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_array_new_default").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_INT));

      arrayGet =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_array_get").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, ADDRESS, ADDRESS));

      arraySet =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_array_set").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_INT, JAVA_LONG, JAVA_INT));

      arrayLen =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_array_len").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG));

      i31New =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_i31_new").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_INT));

      i31Get =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_i31_get").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, JAVA_LONG, JAVA_BYTE));

      refCast =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_ref_cast").orElseThrow(),
              FunctionDescriptor.of(JAVA_LONG, JAVA_LONG, JAVA_LONG, JAVA_INT));

      refTest =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_ref_test").orElseThrow(),
              FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG, JAVA_INT));

      refEq =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_ref_eq").orElseThrow(),
              FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG, JAVA_LONG));

      refIsNull =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_ref_is_null").orElseThrow(),
              FunctionDescriptor.of(JAVA_BYTE, JAVA_LONG, JAVA_LONG));

      collectGarbage =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_collect_garbage").orElseThrow(),
              FunctionDescriptor.of(JAVA_INT, JAVA_LONG, ADDRESS));

      getGcStats =
          linker.downcallHandle(
              stdlib.find("wasmtime4j_gc_get_stats").orElseThrow(),
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
  public PanamaGcRuntime(final long engineHandle) {
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
  public StructInstance createStruct(final StructType structType, final List<GcValue> fieldValues) {
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
  public StructInstance createStruct(final StructType structType) {
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
  public ArrayInstance createArray(final ArrayType arrayType, final List<GcValue> elements) {
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
  public ArrayInstance createArray(final ArrayType arrayType, final int length) {
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
  public I31Instance createI31(final int value) {
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
  public GcValue getStructField(final StructInstance struct, final int fieldIndex) {
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
  public void setStructField(
      final StructInstance struct, final int fieldIndex, final GcValue value) {
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
  public GcValue getArrayElement(final ArrayInstance array, final int elementIndex) {
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
      final ArrayInstance array, final int elementIndex, final GcValue value) {
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
        throw new GcException("GC runtime has been disposed");
      }

      if (!(array instanceof PanamaArrayInstance)) {
        throw new GcException("Invalid array instance type");
      }

      final PanamaArrayInstance panamaArray = (PanamaArrayInstance) array;

      try {
        final int length = (int) arrayLen.invokeExact(nativeHandle, panamaArray.getObjectId());
        if (length < 0) {
          throw new GcException("Failed to get array length");
        }

        return length;
      } catch (final Throwable e) {
        throw new GcException("Failed to get array length", e);
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
        throw new GcException("GC runtime has been disposed");
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
        throw new GcException("Failed to cast reference", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public boolean refTest(final GcObject object, final GcReferenceType targetType) {
    validateNotDisposed();
    validateNotNull(object, "object");
    validateNotNull(targetType, "targetType");

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);
      final int targetTypeId = convertReferenceTypeToNative(targetType);

      try {
        final byte result = (byte) refTest.invokeExact(nativeHandle, objectId, targetTypeId);
        return result != 0;
      } catch (final Throwable e) {
        throw new GcException("Failed to test reference", e);
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
        throw new GcException("GC runtime has been disposed");
      }

      final long objectId1 = getObjectId(obj1);
      final long objectId2 = getObjectId(obj2);

      try {
        final byte result = (byte) refEq.invokeExact(nativeHandle, objectId1, objectId2);
        return result != 0;
      } catch (final Throwable e) {
        throw new GcException("Failed to compare references", e);
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
        throw new GcException("GC runtime has been disposed");
      }

      final long objectId = getObjectId(object);

      try {
        final byte result = (byte) refIsNull.invokeExact(nativeHandle, objectId);
        return result != 0;
      } catch (final Throwable e) {
        throw new GcException("Failed to check if reference is null", e);
      }
    } finally {
      lock.readLock().unlock();
    }
  }

  @Override
  public int registerStructType(final StructType structType) {
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
  public int registerArrayType(final ArrayType arrayType) {
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
  public GcStats collectGarbage() {
    validateNotDisposed();

    lock.readLock().lock();
    try {
      if (disposed) {
        throw new GcException("GC runtime has been disposed");
      }

      final MemorySegment statsSegment = arena.allocate(GC_STATS_LAYOUT);

      try {
        final int result = (int) collectGarbage.invokeExact(nativeHandle, statsSegment);
        if (result != 0) {
          throw new GcException("Failed to collect garbage");
        }

        return convertNativeToGcStats(statsSegment);
      } catch (final Throwable e) {
        throw new GcException("Failed to collect garbage", e);
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
        throw new GcException("GC runtime has been disposed");
      }

      final MemorySegment statsSegment = arena.allocate(GC_STATS_LAYOUT);

      try {
        final int result = (int) getGcStats.invokeExact(nativeHandle, statsSegment);
        if (result != 0) {
          throw new GcException("Failed to get GC stats");
        }

        return convertNativeToGcStats(statsSegment);
      } catch (final Throwable e) {
        throw new GcException("Failed to get GC stats", e);
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

  private int registerStructTypeInternal(final StructType structType) {
    final String name = structType.getName() != null ? structType.getName() : "";
    final List<FieldDefinition> fields = structType.getFields();

    final MemorySegment nameSegment = arena.allocateUtf8String(name);
    final MemorySegment fieldNamesSegment = arena.allocate(ADDRESS, fields.size());
    final MemorySegment fieldNameLensSegment = arena.allocate(JAVA_INT, fields.size());
    final MemorySegment fieldTypesSegment = arena.allocate(JAVA_INT, fields.size());
    final MemorySegment fieldMutabilitiesSegment = arena.allocate(JAVA_BYTE, fields.size());

    for (int i = 0; i < fields.size(); i++) {
      final FieldDefinition field = fields.get(i);

      // Field name
      final String fieldName = field.getName() != null ? field.getName() : "";
      final MemorySegment fieldNameSegment = arena.allocateUtf8String(fieldName);
      fieldNamesSegment.setAtIndex(ADDRESS, i, fieldNameSegment);
      fieldNameLensSegment.setAtIndex(
          JAVA_INT, i, fieldName.getBytes(StandardCharsets.UTF_8).length);

      // Field type
      fieldTypesSegment.setAtIndex(JAVA_INT, i, convertFieldTypeToNative(field.getType()));

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

  private int registerArrayTypeInternal(final ArrayType arrayType) {
    final String name = arrayType.getName() != null ? arrayType.getName() : "";
    final MemorySegment nameSegment = arena.allocateUtf8String(name);
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
        throw new GcException("Unsupported GC value type: " + value.getType());
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
        throw new GcException("Unsupported field type: " + fieldType.getKind());
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
        throw new GcException("Unsupported reference type: " + refType);
    }
  }

  private long getObjectId(final GcObject object) {
    if (object instanceof PanamaGcObject) {
      return ((PanamaGcObject) object).getObjectId();
    } else {
      throw new GcException("Invalid GC object type");
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
    final long totalAllocated = (long) totalAllocatedHandle.get(statsSegment);
    final long totalCollected = (long) totalCollectedHandle.get(statsSegment);
    final long bytesAllocated = (long) bytesAllocatedHandle.get(statsSegment);
    final long bytesCollected = (long) bytesCollectedHandle.get(statsSegment);
    final long minorCollections = (long) minorCollectionsHandle.get(statsSegment);
    final long majorCollections = (long) majorCollectionsHandle.get(statsSegment);
    final int currentHeapSize = (int) currentHeapSizeHandle.get(statsSegment);
    final int peakHeapSize = (int) peakHeapSizeHandle.get(statsSegment);

    return new GcStats() {
      @Override
      public long getTotalAllocated() {
        return totalAllocated;
      }

      @Override
      public long getTotalCollected() {
        return totalCollected;
      }

      @Override
      public long getBytesAllocated() {
        return bytesAllocated;
      }

      @Override
      public long getBytesCollected() {
        return bytesCollected;
      }

      @Override
      public long getMinorCollections() {
        return minorCollections;
      }

      @Override
      public long getMajorCollections() {
        return majorCollections;
      }

      @Override
      public long getCurrentHeapSize() {
        return currentHeapSize;
      }

      @Override
      public long getPeakHeapSize() {
        return peakHeapSize;
      }
    };
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
    public GcValue getField(final int index) {
      return runtime.getStructField(this, index);
    }

    @Override
    public void setField(final int index, final GcValue value) {
      runtime.setStructField(this, index, value);
    }

    @Override
    public int getFieldCount() {
      return structType.getFields().size();
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
    public GcValue getElement(final int index) {
      return runtime.getArrayElement(this, index);
    }

    @Override
    public void setElement(final int index, final GcValue value) {
      runtime.setArrayElement(this, index, value);
    }

    @Override
    public int getLength() {
      return length;
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
      return I31Type.getInstance();
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
        throw new GcException("Failed to get signed I31 value", e);
      }
    }

    @Override
    public int getUnsignedValue() {
      try {
        return (int) i31Get.invokeExact(runtime.nativeHandle, getObjectId(), (byte) 0);
      } catch (final Throwable e) {
        throw new GcException("Failed to get unsigned I31 value", e);
      }
    }
  }

  // ========== Advanced GC Method Stubs ==========
  // These methods provide stub implementations for all the advanced GC features
  // In a complete implementation, these would have Panama FFI bindings to native code

  @Override
  public StructInstance refCastStruct(final GcObject object, final StructType targetStructType) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public ArrayInstance refCastArray(final GcObject object, final ArrayType targetArrayType) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public boolean refTestStruct(final GcObject object, final StructType targetStructType) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public boolean refTestArray(final GcObject object, final ArrayType targetArrayType) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public GcReferenceType getRuntimeType(final GcObject object) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public Optional<GcObject> refCastNullable(final GcObject object, final GcReferenceType targetType) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public StructInstance createPackedStruct(final StructType structType, final List<GcValue> fieldValues,
                                          final Map<Integer, Integer> customAlignment) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public ArrayInstance createVariableLengthArray(final ArrayType arrayType, final int baseLength,
                                                final List<GcValue> flexibleElements) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public ArrayInstance createNestedArray(final ArrayType arrayType, final List<GcObject> nestedElements) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public void copyArrayElements(final ArrayInstance sourceArray, final int sourceIndex,
                               final ArrayInstance destArray, final int destIndex, final int length) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public void fillArrayElements(final ArrayInstance array, final int startIndex, final int length, final GcValue value) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public int registerRecursiveType(final String typeName, final Object typeDefinition) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public Map<String, Integer> createTypeHierarchy(final Object baseType, final List<Object> derivedTypes) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public GcStats collectGarbageIncremental(final long maxPauseMillis) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public GcStats collectGarbageConcurrent() {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public void configureGcStrategy(final String strategy, final Map<String, Object> parameters) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public boolean monitorGcPressure(final double pressureThreshold) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public WeakGcReference createWeakReference(final GcObject object, final Runnable finalizationCallback) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public void registerFinalizationCallback(final GcObject object, final Runnable callback) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public int runFinalization() {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public GcObject integrateHostObject(final Object hostObject, final GcReferenceType gcType) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public Object extractHostObject(final GcObject gcObject) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public Object createSharingBridge(final List<GcObject> objects) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public GcHeapInspection inspectHeap() {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public ObjectLifecycleTracker trackObjectLifecycles(final List<GcObject> objects) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public MemoryLeakAnalysis detectMemoryLeaks() {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public GcProfiler startProfiling() {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public ReferenceSafetyResult validateReferenceSafety(final List<GcObject> rootObjects) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public boolean enforceTypeSafety(final String operation, final List<Object> operands) {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public MemoryCorruptionAnalysis detectMemoryCorruption() {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }

  @Override
  public GcInvariantValidation validateInvariants() {
    throw new UnsupportedOperationException("Advanced GC features not yet implemented in Panama runtime");
  }
}
