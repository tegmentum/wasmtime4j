package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmValue;
import ai.tegmentum.wasmtime4j.WasmValueType;
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

  private final String globalName;
  private final PanamaInstance instance;
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
    this.instance = null;
    this.globalName = null;

    // TODO: Create native global via Panama FFI
    throw new UnsupportedOperationException("Creating new globals not yet implemented");
  }

  /**
   * Package-private constructor for wrapping an exported global.
   *
   * @param globalName the name of the global export
   * @param instance the instance that owns this global
   */
  PanamaGlobal(final String globalName, final PanamaInstance instance) {
    if (globalName == null || globalName.isEmpty()) {
      throw new IllegalArgumentException("Global name cannot be null or empty");
    }
    if (instance == null) {
      throw new IllegalArgumentException("Instance cannot be null");
    }
    this.globalName = globalName;
    this.instance = instance;

    // Query type and mutability from native global
    queryTypeAndMutability();

    LOGGER.fine("Created global wrapper for export: " + globalName);
  }

  @Override
  public WasmValue get() {
    ensureNotClosed();
    if (instance == null) {
      throw new IllegalStateException("Cannot get value: global not associated with an instance");
    }
    return instance.getGlobalValue(this);
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
    if (instance == null) {
      throw new IllegalStateException("Cannot set value: global not associated with an instance");
    }

    // Validate type matches
    if (value.getType() != type) {
      throw new IllegalArgumentException(
          "Value type " + value.getType() + " does not match global type " + type);
    }

    instance.setGlobalValue(this, value);
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

    closed = true;
    LOGGER.fine("Closed Panama global");
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
   * Gets the global name.
   *
   * @return global export name
   */
  String getGlobalName() {
    return globalName;
  }

  /**
   * Sets the type of this global.
   *
   * @param type the value type
   */
  void setType(final WasmValueType type) {
    this.type = type;
  }

  /**
   * Sets the mutability of this global.
   *
   * @param mutable whether this global is mutable
   */
  void setMutable(final boolean mutable) {
    this.mutable = mutable;
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
