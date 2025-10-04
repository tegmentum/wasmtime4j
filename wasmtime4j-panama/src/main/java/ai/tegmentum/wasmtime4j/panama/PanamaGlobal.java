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

  private final Arena arena;
  private final MemorySegment nativeGlobal;
  private final WasmValueType type;
  private final boolean mutable;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama global.
   *
   * @param type the value type of this global
   * @param mutable whether this global is mutable
   * @param initialValue the initial value
   */
  public PanamaGlobal(final WasmValueType type, final boolean mutable, final WasmValue initialValue) {
    if (type == null) {
      throw new IllegalArgumentException("Type cannot be null");
    }
    if (initialValue == null) {
      throw new IllegalArgumentException("Initial value cannot be null");
    }
    this.type = type;
    this.mutable = mutable;
    this.arena = Arena.ofShared();

    // TODO: Create native global via Panama FFI
    this.nativeGlobal = MemorySegment.NULL;

    LOGGER.fine("Created Panama global");
  }

  @Override
  public WasmValue get() {
    ensureNotClosed();
    // TODO: Implement value get
    return WasmValue.i32(0);
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
    // TODO: Implement value set
  }

  @Override
  public WasmValueType getType() {
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

  /**
   * Closes the global and releases resources.
   */
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
