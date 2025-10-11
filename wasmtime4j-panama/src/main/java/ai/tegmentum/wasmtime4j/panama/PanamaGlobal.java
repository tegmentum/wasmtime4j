package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
import java.lang.foreign.Arena;
import java.lang.foreign.MemorySegment;
import java.util.logging.Logger;

/**
 * Panama FFI implementation of WebAssembly Global.
 *
 * @since 1.0.0
 */
public final class PanamaGlobal implements WasmGlobal {
  private static final Logger LOGGER = Logger.getLogger(PanamaGlobal.class.getName());

  private static final NativeFunctionBindings NATIVE_BINDINGS =
      NativeFunctionBindings.getInstance();

  private final Arena arena;
  private final MemorySegment nativeGlobal;
  private final PanamaStore store;
  private WasmValueType type;
  private boolean mutable;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama global.
   *
   * @param type the value type of this global
   * @param mutable whether this global is mutable
   * @param initialValue the initial value
   */
  public PanamaGlobal(
      final WasmValueType type, final boolean mutable, final WasmValue initialValue) {
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null");
    }
    if (initialValue == null) {
      throw new IllegalArgumentException("Initial value cannot be null");
    }
    this.type = type;
    this.mutable = mutable;
    this.arena = Arena.ofShared();
    this.store = null;

    // TODO: Create native global via Panama FFI
    this.nativeGlobal = MemorySegment.NULL;

    LOGGER.fine("Created Panama global");
  }

  /**
   * Package-private constructor for wrapping an existing native global pointer.
   *
   * @param nativeGlobal the native global pointer from Wasmtime
   * @param store the store associated with this global
   */
  PanamaGlobal(final MemorySegment nativeGlobal, final PanamaStore store) {
    if (nativeGlobal == null || nativeGlobal.equals(MemorySegment.NULL)) {
      throw new IllegalArgumentException("Native global pointer cannot be null");
    }
    if (store == null) {
      throw new IllegalArgumentException("Store cannot be null");
    }
    this.arena = Arena.ofShared();
    this.nativeGlobal = nativeGlobal;
    this.store = store;

    // Query type and mutability from native global
    queryTypeAndMutability();

    LOGGER.fine("Wrapped native global pointer");
  }

  @Override
  public WasmValue get() {
    ensureNotClosed();
    if (store == null) {
      throw new IllegalStateException("Cannot get value: global not associated with a store");
    }

    try (final Arena tempArena = Arena.ofConfined()) {
      // Allocate output parameters
      final MemorySegment i32Out = tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);
      final MemorySegment i64Out = tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);
      final MemorySegment f32Out = tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_DOUBLE);
      final MemorySegment f64Out = tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_DOUBLE);
      final MemorySegment refIdPresentOut =
          tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_INT);
      final MemorySegment refIdOut = tempArena.allocate(java.lang.foreign.ValueLayout.JAVA_LONG);

      // Call native function
      final int result =
          NATIVE_BINDINGS.panamaGlobalGet(
              nativeGlobal,
              store.getNativeStore(),
              i32Out,
              i64Out,
              f32Out,
              f64Out,
              refIdPresentOut,
              refIdOut);

      if (result != 0) {
        throw new RuntimeException("Failed to get global value: error code " + result);
      }

      // If type is not yet determined, try all types and cache the result
      if (type == null) {
        // Try I32 first (most common)
        final int i32Val = i32Out.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);
        final long i64Val = i64Out.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0);
        final double f32Val = f32Out.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 0);
        final double f64Val = f64Out.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 0);

        // Heuristic: if i64 != i32 (sign-extended), it's probably I64
        // If f32 != 0.0 or f64 != 0.0, it's probably float
        if (i64Val != (long) i32Val) {
          type = WasmValueType.I64;
        } else if (f32Val != 0.0) {
          type = WasmValueType.F32;
        } else if (f64Val != 0.0 && f64Val != (double) (float) f32Val) {
          type = WasmValueType.F64;
        } else {
          // Default to I32
          type = WasmValueType.I32;
        }
        LOGGER.fine("Auto-detected global type: " + type);
      }

      // Extract value based on type
      switch (type) {
        case I32:
          return WasmValue.i32(i32Out.get(java.lang.foreign.ValueLayout.JAVA_INT, 0));
        case I64:
          return WasmValue.i64(i64Out.get(java.lang.foreign.ValueLayout.JAVA_LONG, 0));
        case F32:
          return WasmValue.f32((float) f32Out.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 0));
        case F64:
          return WasmValue.f64(f64Out.get(java.lang.foreign.ValueLayout.JAVA_DOUBLE, 0));
        case FUNCREF:
          final int refPresent = refIdPresentOut.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);
          return WasmValue.funcref(refPresent != 0 ? new Object() : null);
        case EXTERNREF:
          final int extRefPresent = refIdPresentOut.get(java.lang.foreign.ValueLayout.JAVA_INT, 0);
          return WasmValue.externref(extRefPresent != 0 ? new Object() : null);
        default:
          throw new IllegalStateException("Unsupported global type: " + type);
      }
    }
  }

  @Override
  public void set(final WasmValue value) {
    if (value == null) {
      throw new IllegalArgumentException("Value cannot be null");
    }
    if (!mutable) {
      throw new IllegalStateException("Cannot set value of immutable global");
    }
    ensureNotClosed();
    if (store == null) {
      throw new IllegalStateException("Cannot set value: global not associated with a store");
    }

    // Validate type matches
    if (value.getType() != type) {
      throw new IllegalArgumentException(
          "Value type " + value.getType() + " does not match global type " + type);
    }

    // Convert WasmValue to native parameters
    int valueTypeCode;
    int i32Value = 0;
    long i64Value = 0;
    double f32Value = 0.0;
    double f64Value = 0.0;
    int refIdPresent = 0;
    long refId = 0;

    switch (type) {
      case I32:
        valueTypeCode = 0;
        i32Value = value.asI32();
        break;
      case I64:
        valueTypeCode = 1;
        i64Value = value.asI64();
        break;
      case F32:
        valueTypeCode = 2;
        f32Value = value.asF32();
        break;
      case F64:
        valueTypeCode = 3;
        f64Value = value.asF64();
        break;
      case FUNCREF:
        valueTypeCode = 5;
        final Object funcRef = value.asFuncref();
        refIdPresent = (funcRef != null) ? 1 : 0;
        break;
      case EXTERNREF:
        valueTypeCode = 6;
        final Object extRef = value.asExternref();
        refIdPresent = (extRef != null) ? 1 : 0;
        break;
      default:
        throw new IllegalStateException("Unsupported global type: " + type);
    }

    // Call native function
    final int result =
        NATIVE_BINDINGS.panamaGlobalSet(
            nativeGlobal,
            store.getNativeStore(),
            valueTypeCode,
            i32Value,
            i64Value,
            f32Value,
            f64Value,
            refIdPresent,
            refId);

    if (result != 0) {
      throw new RuntimeException("Failed to set global value: error code " + result);
    }
  }

  @Override
  public WasmValueType getType() {
    // Determine type lazily if not yet known
    if (type == null) {
      // Call get() to auto-detect the type
      get();
    }
    return type;
  }

  @Override
  public boolean isMutable() {
    return mutable;
  }

  /**
   * Checks if the global has been closed.
   *
   * @return true if closed, false otherwise
   */
  public boolean isClosed() {
    return closed;
  }

  /** Closes the global and releases resources. */
  public void close() {
    if (closed) {
      return;
    }

    try {
      // TODO: Destroy native global
      arena.close();
      closed = true;
      LOGGER.fine("Closed Panama global");
    } catch (final Exception e) {
      LOGGER.warning("Error closing global: " + e.getMessage());
    }
  }

  /**
   * Queries type and mutability information from the native global.
   *
   * <p>This method is called during construction to initialize type and mutability fields.
   */
  private void queryTypeAndMutability() {
    // Type will be determined lazily during first get() call
    // Panama FFI version of globalGetTypeInfo is not yet implemented
    this.type = null; // Will be auto-detected
    this.mutable = true; // Assume mutable, set() will fail if not
    LOGGER.fine("Global type will be determined on first access");
  }

  /**
   * Gets the native global pointer.
   *
   * @return native global segment
   */
  public MemorySegment getNativeGlobal() {
    return nativeGlobal;
  }

  /**
   * Gets the global handle for native operations.
   *
   * @return native global handle
   */
  public MemorySegment getGlobalHandle() {
    return nativeGlobal;
  }

  /**
   * Ensures the global is not closed.
   *
   * @throws IllegalStateException if closed
   */
  private void ensureNotClosed() {
    if (closed) {
      throw new IllegalStateException("Global has been closed");
    }
  }
}
