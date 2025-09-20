package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.aot.AotExecutableMetadata;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.serialization.CompressionType;
import ai.tegmentum.wasmtime4j.serialization.SerializationFormat;
import ai.tegmentum.wasmtime4j.serialization.SerializationOptions;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import java.util.logging.Logger;

/**
 * JNI implementation of serialized module.
 *
 * <p>This class wraps a native serialized module and provides Java access to the serialized
 * WebAssembly data. It implements defensive programming patterns to ensure safe access to native
 * resources.
 *
 * @since 1.0.0
 */
public final class JniSerializedModule implements SerializedModule {

  private static final Logger LOGGER = Logger.getLogger(JniSerializedModule.class.getName());

  // Native library loading
  static {
    NativeLibraryLoader.loadNativeLibrary();
  }

  // Native serialized module handle
  private final long nativeHandle;

  // Serialization options used
  private final SerializationOptions options;

  // Cached data to avoid repeated native calls
  private volatile byte[] cachedData;

  // Flag to track if this serialized module has been closed
  private volatile boolean closed = false;

  /**
   * Creates a new JNI serialized module from a native handle.
   *
   * @param nativeHandle the native serialized module handle
   * @param options the serialization options used
   * @throws IllegalArgumentException if options is null
   */
  public JniSerializedModule(final long nativeHandle, final SerializationOptions options) {
    if (options == null) {
      throw new IllegalArgumentException("Options cannot be null");
    }

    this.nativeHandle = nativeHandle;
    this.options = options;

    LOGGER.fine("JNI serialized module created with handle: " + this.nativeHandle);
  }

  /**
   * Creates a new JNI serialized module from raw data.
   *
   * @param data the serialized data
   * @param metadata the metadata (unused in this simple implementation)
   */
  public JniSerializedModule(final byte[] data, final AotExecutableMetadata metadata) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }

    this.nativeHandle = 0; // No native handle for raw data
    this.options = SerializationOptions.builder().build(); // Default options
    this.cachedData = data.clone(); // Defensive copy

    LOGGER.fine("JNI serialized module created from raw data: " + data.length + " bytes");
  }

  @Override
  public byte[] getData() throws WasmException {
    validateNotClosed();

    // Return cached data if available
    if (cachedData != null) {
      return cachedData.clone(); // Defensive copy
    }

    if (nativeHandle == 0) {
      throw new WasmException("Cannot get data from invalid serialized module");
    }

    try {
      final byte[] data = nativeGetData(nativeHandle);
      if (data == null) {
        throw new WasmException("Failed to retrieve serialized data");
      }

      // Cache the data for future calls
      cachedData = data.clone();
      return data.clone(); // Defensive copy
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to get serialized data");
    }
  }

  @Override
  public SerializationFormat getFormat() {
    validateNotClosed();

    if (nativeHandle == 0) {
      return SerializationFormat.WASMTIME4J_V1; // Default format
    }

    try {
      final String formatString = nativeGetFormat(nativeHandle);
      return SerializationFormat.fromString(formatString);
    } catch (final Exception e) {
      LOGGER.warning("Failed to get serialization format: " + e.getMessage());
      return SerializationFormat.WASMTIME4J_V1; // Default fallback
    }
  }

  @Override
  public CompressionType getCompression() {
    return options.getCompression();
  }

  @Override
  public String getChecksum() throws WasmException {
    validateNotClosed();

    if (nativeHandle == 0) {
      // Calculate checksum for raw data
      if (cachedData != null) {
        return calculateChecksum(cachedData);
      }
      throw new WasmException("Cannot calculate checksum without data");
    }

    try {
      final String checksum = nativeGetChecksum(nativeHandle);
      if (checksum == null || checksum.isEmpty()) {
        throw new WasmException("Failed to get checksum");
      }
      return checksum;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to get checksum");
    }
  }

  @Override
  public boolean isCompatibleWith(final Engine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    validateNotClosed();

    if (nativeHandle == 0) {
      // For raw data, we can't check compatibility without deserialization
      return true; // Optimistic assumption
    }

    try {
      final long engineHandle = extractEngineHandle(engine);
      return nativeIsCompatibleWith(nativeHandle, engineHandle);
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to check compatibility");
    }
  }

  @Override
  public long getSize() {
    validateNotClosed();

    if (cachedData != null) {
      return cachedData.length;
    }

    if (nativeHandle == 0) {
      return 0;
    }

    try {
      return nativeGetSize(nativeHandle);
    } catch (final Exception e) {
      LOGGER.warning("Failed to get size: " + e.getMessage());
      return 0;
    }
  }

  @Override
  public long getOriginalSize() throws WasmException {
    validateNotClosed();

    if (nativeHandle == 0) {
      // For raw data, we don't know the original size
      return getSize();
    }

    try {
      return nativeGetOriginalSize(nativeHandle);
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to get original size");
    }
  }

  @Override
  public boolean isValid() {
    validateNotClosed();

    if (nativeHandle == 0) {
      return cachedData != null && cachedData.length > 0;
    }

    try {
      return nativeIsValid(nativeHandle);
    } catch (final Exception e) {
      LOGGER.warning("Failed to validate serialized module: " + e.getMessage());
      return false;
    }
  }

  @Override
  public String getMetadataString() throws WasmException {
    validateNotClosed();

    if (nativeHandle == 0) {
      // Return minimal metadata for raw data
      return String.format(
          "{\"size\":%d,\"compression\":\"%s\"}", getSize(), getCompression().name());
    }

    try {
      final String metadata = nativeGetMetadata(nativeHandle);
      if (metadata == null) {
        throw new WasmException("Failed to get metadata");
      }
      return metadata;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Failed to get metadata");
    }
  }

  /**
   * Gets the serialization options used for this module.
   *
   * @return the serialization options
   */
  public SerializationOptions getOptions() {
    return options;
  }

  /**
   * Gets the native handle for this serialized module.
   *
   * <p>This method is intended for internal use by other JNI components.
   *
   * @return the native handle, or 0 if this module was created from raw data
   */
  public long getNativeHandle() {
    return nativeHandle;
  }

  /** Closes this serialized module and releases native resources. */
  public void close() {
    if (!closed) {
      closed = true;
      cachedData = null; // Clear cache
      if (nativeHandle != 0) {
        nativeDestroy(nativeHandle);
      }
      LOGGER.fine("JNI serialized module closed");
    }
  }

  // Private helper methods

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Serialized module has been closed");
    }
  }

  private long extractEngineHandle(final Engine engine) throws WasmException {
    if (engine instanceof JniEngine) {
      return ((JniEngine) engine).getNativeHandle();
    }
    throw new WasmException("Engine is not a JNI engine implementation");
  }

  private String calculateChecksum(final byte[] data) {
    // Simple checksum calculation using Java's built-in hash
    int hash = java.util.Arrays.hashCode(data);
    return String.format("%08x", hash);
  }

  // Native method declarations

  /**
   * Gets the serialized data.
   *
   * @param handle the serialized module handle
   * @return serialized data, or null on failure
   */
  private static native byte[] nativeGetData(long handle);

  /**
   * Gets the serialization format.
   *
   * @param handle the serialized module handle
   * @return format string, or null on failure
   */
  private static native String nativeGetFormat(long handle);

  /**
   * Gets the checksum.
   *
   * @param handle the serialized module handle
   * @return checksum string, or null on failure
   */
  private static native String nativeGetChecksum(long handle);

  /**
   * Checks compatibility with an engine.
   *
   * @param handle the serialized module handle
   * @param engineHandle the engine handle
   * @return true if compatible, false otherwise
   */
  private static native boolean nativeIsCompatibleWith(long handle, long engineHandle);

  /**
   * Gets the size of the serialized data.
   *
   * @param handle the serialized module handle
   * @return size in bytes
   */
  private static native long nativeGetSize(long handle);

  /**
   * Gets the original (uncompressed) size.
   *
   * @param handle the serialized module handle
   * @return original size in bytes
   */
  private static native long nativeGetOriginalSize(long handle);

  /**
   * Validates the serialized module.
   *
   * @param handle the serialized module handle
   * @return true if valid, false otherwise
   */
  private static native boolean nativeIsValid(long handle);

  /**
   * Gets metadata as a JSON string.
   *
   * @param handle the serialized module handle
   * @return metadata JSON string, or null on failure
   */
  private static native String nativeGetMetadata(long handle);

  /**
   * Destroys the native serialized module.
   *
   * @param handle the handle to destroy
   */
  private static native void nativeDestroy(long handle);
}
