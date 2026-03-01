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
package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.WasmValueType;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.panama.util.NativeResourceHandle;
import ai.tegmentum.wasmtime4j.panama.util.PanamaErrorMapper;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Table.
 *
 * @since 1.0.0
 */
public final class PanamaTable implements WasmTable {
  private static final Logger LOGGER = Logger.getLogger(PanamaTable.class.getName());
  private static final NativeMemoryBindings NATIVE_BINDINGS = NativeMemoryBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeTable;
  private final PanamaInstance instance;
  private final PanamaStore store; // For tables created directly by store (instance will be null)
  private final WasmValueType elementType;
  private final NativeResourceHandle resourceHandle;

  /**
   * Package-private constructor for wrapping an existing native table pointer.
   *
   * @param nativeTable the native table pointer from Wasmtime
   * @param instance the instance that owns this table
   */
  PanamaTable(final MemorySegment nativeTable, final PanamaInstance instance) {
    if (nativeTable == null || nativeTable.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native table pointer cannot be null");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    this.arena = Arena.ofShared();
    this.nativeTable = nativeTable;
    this.instance = instance;
    this.store = null;
    // Query element type from native table metadata
    this.elementType = queryElementTypeFromNative(nativeTable, arena);
    final MemorySegment capturedNativeTable = this.nativeTable;
    final Arena capturedArena = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaTable",
            () -> {
              try {
                final MethodHandle deleteHandle = NATIVE_BINDINGS.getPanamaTableDelete();
                if (deleteHandle != null) {
                  deleteHandle.invoke(nativeTable);
                }
                arena.close();
                LOGGER.fine("Closed Panama table");
              } catch (final Throwable e) {
                LOGGER.warning("Error closing table: " + e.getMessage());
              }
            },
            this,
            () -> {
              try {
                final MethodHandle deleteHandle = NATIVE_BINDINGS.getPanamaTableDelete();
                if (deleteHandle != null) {
                  deleteHandle.invoke(capturedNativeTable);
                }
              } catch (final Throwable e) {
                // Safety net — best effort
              }
              if (capturedArena != null && capturedArena.scope().isAlive()) {
                capturedArena.close();
              }
            });
    LOGGER.fine("Wrapped native table pointer with element type: " + this.elementType);
  }

  /**
   * Package-private constructor for tables created directly by a store.
   *
   * @param nativeTable the native table pointer from Wasmtime
   * @param elementType the type of elements in this table
   * @param store the store that owns this table
   */
  PanamaTable(
      final MemorySegment nativeTable, final WasmValueType elementType, final PanamaStore store) {
    if (nativeTable == null || nativeTable.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native table pointer cannot be null");
    }
    if (elementType == null) {
      throw new IllegalArgumentException("Element type cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    this.arena = Arena.ofShared();
    this.nativeTable = nativeTable;
    this.elementType = elementType;
    this.instance = null; // Tables created by store don't have an instance
    this.store = store;
    final MemorySegment capturedNativeTable2 = this.nativeTable;
    final Arena capturedArena2 = this.arena;
    this.resourceHandle =
        new NativeResourceHandle(
            "PanamaTable",
            () -> {
              try {
                final MethodHandle deleteHandle = NATIVE_BINDINGS.getPanamaTableDelete();
                if (deleteHandle != null) {
                  deleteHandle.invoke(nativeTable);
                }
                arena.close();
                LOGGER.fine("Closed Panama table");
              } catch (final Throwable e) {
                LOGGER.warning("Error closing table: " + e.getMessage());
              }
            },
            this,
            () -> {
              try {
                final MethodHandle deleteHandle = NATIVE_BINDINGS.getPanamaTableDelete();
                if (deleteHandle != null) {
                  deleteHandle.invoke(capturedNativeTable2);
                }
              } catch (final Throwable e) {
                // Safety net — best effort
              }
              if (capturedArena2 != null && capturedArena2.scope().isAlive()) {
                capturedArena2.close();
              }
            });
    LOGGER.fine("Created table from store with type: " + elementType);
  }

  /**
   * Package-private constructor for extern tables with automatic element type detection.
   *
   * <p>This constructor queries the native table handle to determine the element type, similar to
   * the instance-based constructor but without requiring a PanamaInstance.
   *
   * @param nativeTable the native table pointer from Wasmtime
   * @param store the store that owns this table
   */
  PanamaTable(final MemorySegment nativeTable, final PanamaStore store) {
    this(nativeTable, queryElementTypeWithTempArena(nativeTable), store);
  }

  /**
   * Queries element type using a temporary arena that is properly closed.
   *
   * @param nativeTable the native table pointer
   * @return the element type
   */
  private static WasmValueType queryElementTypeWithTempArena(final MemorySegment nativeTable) {
    try (Arena tempArena = Arena.ofConfined()) {
      return queryElementTypeFromNative(nativeTable, tempArena);
    }
  }

  @Override
  public int getSize() {
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot get size: table not associated with an instance");
    }

    try {
      final MethodHandle getSizeHandle = NATIVE_BINDINGS.getPanamaTableSize();
      if (getSizeHandle == null) {
        throw new IllegalStateException("Panama table size function not available");
      }

      final MemorySegment sizeSegment = arena.allocate(ValueLayout.JAVA_INT);
      final int result =
          (int) getSizeHandle.invoke(nativeTable, getNativeStorePointer(), sizeSegment);

      if (result != 0) {
        throw new IllegalStateException("Failed to get table size");
      }

      return sizeSegment.get(ValueLayout.JAVA_INT, 0);
    } catch (final Throwable e) {
      throw new IllegalStateException("Error getting table size: " + e.getMessage(), e);
    }
  }

  @Override
  public WasmValueType getType() {
    return elementType;
  }

  @Override
  public ai.tegmentum.wasmtime4j.type.TableType getTableType() {
    ensureNotClosed();

    try {
      final MethodHandle metadataHandle = NATIVE_BINDINGS.getPanamaTableMetadata();
      if (metadataHandle == null) {
        throw new IllegalStateException("Panama table metadata function not available");
      }

      final MemorySegment elementTypeSegment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment initialSizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment hasMaximumSegment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment maximumSizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment is64Segment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment nameSegment = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              metadataHandle.invoke(
                  nativeTable,
                  elementTypeSegment,
                  initialSizeSegment,
                  hasMaximumSegment,
                  maximumSizeSegment,
                  is64Segment,
                  nameSegment);

      if (result != 0) {
        throw new IllegalStateException("Failed to get table metadata");
      }

      final int elementTypeCode = elementTypeSegment.get(ValueLayout.JAVA_INT, 0);
      final WasmValueType element = WasmValueType.fromNativeTypeCode(elementTypeCode);
      final long minimum = initialSizeSegment.get(ValueLayout.JAVA_LONG, 0);
      final int hasMaximum = hasMaximumSegment.get(ValueLayout.JAVA_INT, 0);
      final Long maximum =
          hasMaximum != 0 ? Long.valueOf(maximumSizeSegment.get(ValueLayout.JAVA_LONG, 0)) : null;
      final boolean is64 = is64Segment.get(ValueLayout.JAVA_INT, 0) != 0;

      return new ai.tegmentum.wasmtime4j.panama.type.PanamaTableType(
          element, minimum, maximum, is64, arena, nativeTable);
    } catch (final Throwable e) {
      throw new IllegalStateException("Error getting table type: " + e.getMessage(), e);
    }
  }

  @Override
  public int grow(final int elements, final Object initValue) {
    if (elements < 0) {
      throw new IllegalArgumentException("Elements cannot be negative");
    }
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot grow table: table not associated with an instance");
    }

    try {
      final MethodHandle growHandle = NATIVE_BINDINGS.getPanamaTableGrow();
      if (growHandle == null) {
        throw new IllegalStateException("Panama table grow function not available");
      }

      // Determine element type based on table's element type
      final int elementTypeCode;
      if (elementType == WasmValueType.FUNCREF) {
        elementTypeCode = 5; // FUNCREF
      } else if (elementType == WasmValueType.EXTERNREF) {
        elementTypeCode = 6; // EXTERNREF
      } else {
        throw new IllegalArgumentException("Unsupported element type: " + elementType);
      }

      // Handle init value
      final long[] refPair = objectToRefIdPair(initValue);

      final MemorySegment oldSizeSegment = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          (int)
              growHandle.invoke(
                  nativeTable,
                  getNativeStorePointer(),
                  elements,
                  elementTypeCode,
                  (int) refPair[0],
                  refPair[1],
                  oldSizeSegment);

      if (result != 0) {
        return -1; // Growth failed
      }

      return oldSizeSegment.get(ValueLayout.JAVA_INT, 0);
    } catch (final Throwable e) {
      throw new IllegalStateException("Error growing table: " + e.getMessage(), e);
    }
  }

  @Override
  public int getMaxSize() {
    ensureNotClosed();

    try {
      final MethodHandle metadataHandle = NATIVE_BINDINGS.getPanamaTableMetadata();
      if (metadataHandle == null) {
        throw new IllegalStateException("Panama table metadata function not available");
      }

      final MemorySegment elementTypeSegment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment initialSizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment hasMaximumSegment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment maximumSizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment is64Segment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment nameSegment = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              metadataHandle.invoke(
                  nativeTable,
                  elementTypeSegment,
                  initialSizeSegment,
                  hasMaximumSegment,
                  maximumSizeSegment,
                  is64Segment,
                  nameSegment);

      if (result != 0) {
        throw new IllegalStateException("Failed to get table metadata");
      }

      final int hasMaximum = hasMaximumSegment.get(ValueLayout.JAVA_INT, 0);
      if (hasMaximum == 0) {
        return -1; // No maximum
      }

      return (int) maximumSizeSegment.get(ValueLayout.JAVA_LONG, 0);
    } catch (final Throwable e) {
      throw new IllegalStateException("Error getting table max size: " + e.getMessage(), e);
    }
  }

  @Override
  public Object get(final int index) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative");
    }
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot get element: table not associated with an instance");
    }

    try {
      final MethodHandle getHandle = NATIVE_BINDINGS.getPanamaTableGet();
      if (getHandle == null) {
        throw new IllegalStateException("Panama table get function not available");
      }

      final MemorySegment refIdPresentSegment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment refIdSegment = arena.allocate(ValueLayout.JAVA_LONG);

      final int result =
          (int)
              getHandle.invoke(
                  nativeTable, getNativeStorePointer(), index, refIdPresentSegment, refIdSegment);

      if (result != 0) {
        throw new IndexOutOfBoundsException("Failed to get table element at index " + index);
      }

      final int refIdPresent = refIdPresentSegment.get(ValueLayout.JAVA_INT, 0);
      if (refIdPresent == 0) {
        return null; // No reference present
      }

      final long refId = refIdSegment.get(ValueLayout.JAVA_LONG, 0);
      return Long.valueOf(refId);
    } catch (final IndexOutOfBoundsException e) {
      throw e;
    } catch (final Throwable e) {
      throw new IllegalStateException("Error getting table element: " + e.getMessage(), e);
    }
  }

  @Override
  public void set(final int index, final Object value) {
    if (index < 0) {
      throw new IndexOutOfBoundsException("Index cannot be negative");
    }
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot set element: table not associated with an instance");
    }

    try {
      final MethodHandle setHandle = NATIVE_BINDINGS.getPanamaTableSet();
      if (setHandle == null) {
        throw new IllegalStateException("Panama table set function not available");
      }

      // Determine element type based on table's element type
      final int elementTypeCode;
      if (elementType == WasmValueType.FUNCREF) {
        elementTypeCode = 5; // FUNCREF
      } else if (elementType == WasmValueType.EXTERNREF) {
        elementTypeCode = 6; // EXTERNREF
      } else {
        throw new IllegalArgumentException("Unsupported element type: " + elementType);
      }

      // Handle value conversion
      final long[] refPair = objectToRefIdPair(value);

      final int result =
          (int)
              setHandle.invoke(
                  nativeTable,
                  getNativeStorePointer(),
                  index,
                  elementTypeCode,
                  (int) refPair[0],
                  refPair[1]);

      if (result != 0) {
        throw new IndexOutOfBoundsException("Failed to set table element at index " + index);
      }
    } catch (final IndexOutOfBoundsException e) {
      throw e;
    } catch (final IllegalArgumentException e) {
      throw e;
    } catch (final Throwable e) {
      throw new IllegalStateException("Error setting table element: " + e.getMessage(), e);
    }
  }

  @Override
  public WasmValueType getElementType() {
    return elementType;
  }

  /**
   * Converts an object value to a (refIdPresent, refId) pair for native FFI calls.
   *
   * <p>Handles null (no ref), Long (raw ID), PanamaHostFunction (funcRefId), and
   * PanamaFunctionReference (nativeRegistryId).
   *
   * @param value the value to convert
   * @return a two-element long array: [0] = refIdPresent (0 or 1), [1] = refId
   * @throws IllegalArgumentException if the value type is unsupported
   */
  private long[] objectToRefIdPair(final Object value) {
    if (value == null) {
      return new long[] {0, 0L};
    }
    if (value instanceof Long) {
      return new long[] {1, ((Long) value).longValue()};
    }
    if (value instanceof PanamaHostFunction) {
      final PanamaHostFunction hostFunc = (PanamaHostFunction) value;
      final long funcRefIdVal = hostFunc.getFuncRefId();
      if (funcRefIdVal == 0) {
        throw new IllegalArgumentException(
            "Host function has no native registry ID - it may not have been created with a store");
      }
      LOGGER.fine("Converting host function to ref ID: " + funcRefIdVal);
      return new long[] {1, funcRefIdVal};
    }
    if (value instanceof PanamaFunctionReference) {
      final PanamaFunctionReference funcRef = (PanamaFunctionReference) value;
      final long registryId = funcRef.getNativeRegistryId();
      if (registryId < 0) {
        throw new IllegalArgumentException(
            "Function reference has no native registry ID - it may not have been registered");
      }
      LOGGER.fine("Converting function reference to ref ID: " + registryId);
      return new long[] {1, registryId};
    }
    if (value instanceof ai.tegmentum.wasmtime4j.WasmFunction) {
      throw new IllegalArgumentException(
          "Unsupported WasmFunction type: "
              + value.getClass().getName()
              + ". Only PanamaHostFunction and PanamaFunctionReference are supported.");
    }
    throw new IllegalArgumentException("Unsupported value type: " + value.getClass());
  }

  @Override
  public void fill(final int start, final int count, final Object value) {
    if (start < 0) {
      throw new IllegalArgumentException("Start index cannot be negative");
    }
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative");
    }
    ensureNotClosed();
    if (instance == null && store == null) {
      throw new IllegalStateException(
          "Cannot fill table: table not associated with an instance or store");
    }

    try {
      final MethodHandle fillHandle = NATIVE_BINDINGS.getPanamaTableFill();
      if (fillHandle == null) {
        throw new IllegalStateException("Panama table fill function not available");
      }

      // Determine element type based on table's element type
      final int elementTypeCode;
      if (elementType == WasmValueType.FUNCREF) {
        elementTypeCode = 5; // FUNCREF
      } else if (elementType == WasmValueType.EXTERNREF) {
        elementTypeCode = 6; // EXTERNREF
      } else {
        throw new IllegalArgumentException("Unsupported element type: " + elementType);
      }

      // Handle fill value
      final long[] refPair = objectToRefIdPair(value);

      final int result =
          (int)
              fillHandle.invoke(
                  nativeTable,
                  getNativeStorePointer(),
                  start,
                  count,
                  elementTypeCode,
                  (int) refPair[0],
                  refPair[1]);

      if (result != 0) {
        throw new IllegalStateException("Failed to fill table");
      }
    } catch (final Throwable e) {
      throw new IllegalStateException("Error filling table: " + e.getMessage(), e);
    }
  }

  /**
   * Fills a region of this table with the specified value.
   *
   * @param dstIndex the starting index in this table
   * @param value the value to fill with
   * @param length the number of elements to fill
   * @throws WasmException if the operation fails or indices are out of bounds
   */
  public void fill(final long dstIndex, final Object value, final long length)
      throws WasmException {
    if (dstIndex < 0) {
      throw new IllegalArgumentException("dstIndex cannot be negative");
    }
    if (length < 0) {
      throw new IllegalArgumentException("length cannot be negative");
    }
    // Delegate to existing fill method with int parameters
    fill((int) dstIndex, (int) length, value);
  }

  @Override
  public void copy(final int dst, final int src, final int count) {
    if (dst < 0) {
      throw new IllegalArgumentException("Destination index cannot be negative");
    }
    if (src < 0) {
      throw new IllegalArgumentException("Source index cannot be negative");
    }
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative");
    }
    ensureNotClosed();

    if (count == 0) {
      return; // Nothing to do
    }

    final int result =
        NATIVE_BINDINGS.panamaTableCopy(nativeTable, getNativeStorePointer(), dst, src, count);

    if (result != 0) {
      throw new IllegalStateException(
          "Failed to copy table elements: " + PanamaErrorMapper.getErrorDescription(result));
    }
  }

  @Override
  public void copy(final int dst, final WasmTable src, final int srcIndex, final int count) {
    if (dst < 0) {
      throw new IllegalArgumentException("Destination index cannot be negative");
    }
    if (src == null) {
      throw new IllegalArgumentException("Source table cannot be null");
    }
    if (srcIndex < 0) {
      throw new IllegalArgumentException("Source index cannot be negative");
    }
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative");
    }
    ensureNotClosed();

    if (count == 0) {
      return; // Nothing to do
    }

    if (!(src instanceof PanamaTable)) {
      throw new IllegalArgumentException("Source table must be a PanamaTable instance");
    }

    final PanamaTable srcTable = (PanamaTable) src;
    final MemorySegment srcTablePtr = srcTable.getNativeTable();

    final int result =
        NATIVE_BINDINGS.panamaTableCopyFrom(
            nativeTable, getNativeStorePointer(), dst, srcTablePtr, srcIndex, count);

    if (result != 0) {
      throw new IllegalStateException(
          "Failed to copy table elements from source: "
              + PanamaErrorMapper.getErrorDescription(result));
    }
  }

  /**
   * Copies elements from another table to this table.
   *
   * @param dstIndex the starting index in this table
   * @param srcTable the source table to copy from
   * @param srcIndex the starting index in the source table
   * @param length the number of elements to copy
   * @throws WasmException if the operation fails or indices are out of bounds
   */
  public void copy(
      final long dstIndex, final WasmTable srcTable, final long srcIndex, final long length)
      throws WasmException {
    if (srcTable == null) {
      throw new IllegalArgumentException("srcTable cannot be null");
    }
    if (dstIndex < 0) {
      throw new IllegalArgumentException("dstIndex cannot be negative");
    }
    if (srcIndex < 0) {
      throw new IllegalArgumentException("srcIndex cannot be negative");
    }
    if (length < 0) {
      throw new IllegalArgumentException("length cannot be negative");
    }

    // Handle self-copy case
    if (this.equals(srcTable)) {
      copy((int) dstIndex, (int) srcIndex, (int) length);
      return;
    }

    copy((int) dstIndex, srcTable, (int) srcIndex, (int) length);
  }

  @Override
  public void init(
      final int destIndex, final int elementSegmentIndex, final int srcIndex, final int count) {
    if (destIndex < 0) {
      throw new IndexOutOfBoundsException("Destination index cannot be negative");
    }
    if (elementSegmentIndex < 0) {
      throw new IllegalArgumentException("Element segment index cannot be negative");
    }
    if (srcIndex < 0) {
      throw new IndexOutOfBoundsException("Source index cannot be negative");
    }
    if (count < 0) {
      throw new IllegalArgumentException("Count cannot be negative");
    }
    ensureNotClosed();

    if (count == 0) {
      return; // Nothing to do
    }

    if (instance == null) {
      throw new IllegalStateException("Cannot init: table not associated with an instance");
    }

    final PanamaStore actualStore = getActualStore();
    if (actualStore == null) {
      throw new IllegalStateException("Store is not available");
    }

    final MemorySegment storePtr = actualStore.getNativeStore();
    final MemorySegment instancePtr = instance.getNativeInstance();

    if (nativeTable == null || nativeTable.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Table pointer is null");
    }

    if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Instance pointer is null");
    }

    final int result =
        NATIVE_BINDINGS.panamaTableInit(
            nativeTable, storePtr, instancePtr, destIndex, srcIndex, count, elementSegmentIndex);

    if (result != 0) {
      throw new RuntimeException(
          "Failed to initialize table from element segment: "
              + PanamaErrorMapper.getErrorDescription(result));
    }

    LOGGER.fine(
        "Initialized table range ["
            + destIndex
            + ", "
            + (destIndex + count)
            + ") from element segment "
            + elementSegmentIndex
            + " at offset "
            + srcIndex);
  }

  @Override
  public void dropElementSegment(final int elementSegmentIndex) {
    if (elementSegmentIndex < 0) {
      throw new IllegalArgumentException("Element segment index cannot be negative");
    }
    ensureNotClosed();

    if (instance == null) {
      throw new IllegalStateException(
          "Cannot drop element segment: table not associated with an instance");
    }

    final MemorySegment instancePtr = instance.getNativeInstance();

    if (instancePtr == null || instancePtr.equals(MemorySegment.NULL)) {
      throw new IllegalStateException("Instance pointer is null");
    }

    final int result = NATIVE_BINDINGS.elemDrop(instancePtr, elementSegmentIndex);

    if (result != 0) {
      throw new RuntimeException(
          "Failed to drop element segment: " + PanamaErrorMapper.getErrorDescription(result));
    }

    LOGGER.fine("Dropped element segment: " + elementSegmentIndex);
  }

  /**
   * Helper method to get the actual store from either store or instance.
   *
   * @return PanamaStore instance
   */
  private PanamaStore getActualStore() {
    if (store != null) {
      return store;
    }
    if (instance != null && instance.getStore() instanceof PanamaStore) {
      return (PanamaStore) instance.getStore();
    }
    return null;
  }

  /** Closes the table and releases resources. */
  public void close() {
    resourceHandle.close();
  }

  /**
   * Gets the native table pointer.
   *
   * @return native table segment
   */
  public MemorySegment getNativeTable() {
    return nativeTable;
  }

  /**
   * Ensures the table is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    resourceHandle.ensureNotClosed();
  }

  /**
   * Gets the native store pointer from either the instance or direct store reference.
   *
   * @return native store segment
   * @throws IllegalStateException if store is not available or not a PanamaStore
   */
  private MemorySegment getNativeStorePointer() {
    if (store != null) {
      // Table was created directly by store
      return store.getNativeStore();
    }
    if (instance == null || instance.getStore() == null) {
      throw new IllegalStateException("Instance or store is null");
    }
    if (!(instance.getStore() instanceof PanamaStore)) {
      throw new IllegalStateException("Store is not a PanamaStore");
    }
    return ((PanamaStore) instance.getStore()).getNativeStore();
  }

  @Override
  public int growAsync(final int elements, final Object initValue)
      throws ai.tegmentum.wasmtime4j.exception.WasmException {
    if (elements < 0) {
      throw new IllegalArgumentException("Elements cannot be negative");
    }
    ensureNotClosed();

    try {
      // Determine element type based on table's element type
      final int elementTypeCode;
      if (elementType == WasmValueType.FUNCREF) {
        elementTypeCode = 5; // FUNCREF
      } else if (elementType == WasmValueType.EXTERNREF) {
        elementTypeCode = 6; // EXTERNREF
      } else {
        throw new IllegalArgumentException("Unsupported element type: " + elementType);
      }

      // Handle init value
      final long[] refPair = objectToRefIdPair(initValue);

      final MemorySegment oldSizeSegment = arena.allocate(ValueLayout.JAVA_INT);

      final int result =
          NATIVE_BINDINGS.panamaTableGrowAsync(
              nativeTable,
              getNativeStorePointer(),
              elements,
              elementTypeCode,
              (int) refPair[0],
              refPair[1],
              oldSizeSegment);

      if (result != 0) {
        return -1; // Growth failed
      }

      return oldSizeSegment.get(ValueLayout.JAVA_INT, 0);
    } catch (final Throwable e) {
      throw new ai.tegmentum.wasmtime4j.exception.WasmException(
          "Async table growth failed: " + e.getMessage(), e);
    }
  }

  @Override
  public boolean supports64BitAddressing() {
    if (resourceHandle.isClosed()) {
      return false;
    }
    try {
      final MemorySegment storePtr = getNativeStorePointer();
      return NATIVE_BINDINGS.tableSupports64BitAddressing(nativeTable, storePtr);
    } catch (final Exception e) {
      LOGGER.fine("Error checking 64-bit addressing support: " + e.getMessage());
      return false;
    }
  }

  /**
   * Query the element type from native table metadata.
   *
   * @param nativeTable the native table pointer
   * @param arena the memory arena to use for allocations
   * @return the element type of the table
   */
  private static WasmValueType queryElementTypeFromNative(
      final MemorySegment nativeTable, final Arena arena) {
    try {
      final MethodHandle metadataHandle = NATIVE_BINDINGS.getPanamaTableMetadata();
      if (metadataHandle == null) {
        LOGGER.warning("Panama table metadata function not available, defaulting to FUNCREF");
        return WasmValueType.FUNCREF;
      }

      final MemorySegment elementTypeSegment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment initialSizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment hasMaximumSegment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment maximumSizeSegment = arena.allocate(ValueLayout.JAVA_LONG);
      final MemorySegment is64Segment = arena.allocate(ValueLayout.JAVA_INT);
      final MemorySegment nameSegment = arena.allocate(ValueLayout.ADDRESS);

      final int result =
          (int)
              metadataHandle.invoke(
                  nativeTable,
                  elementTypeSegment,
                  initialSizeSegment,
                  hasMaximumSegment,
                  maximumSizeSegment,
                  is64Segment,
                  nameSegment);

      if (result != 0) {
        LOGGER.warning("Failed to get table metadata, defaulting to FUNCREF");
        return WasmValueType.FUNCREF;
      }

      final int elementTypeCode = elementTypeSegment.get(ValueLayout.JAVA_INT, 0);
      return WasmValueType.fromNativeTypeCode(elementTypeCode);
    } catch (final Throwable e) {
      LOGGER.warning("Error querying element type from native: " + e.getMessage());
      return WasmValueType.FUNCREF;
    }
  }
}
