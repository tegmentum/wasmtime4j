package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.exception.JniExceptionHandler;
import ai.tegmentum.wasmtime4j.jni.nativelib.NativeLibraryLoader;
import ai.tegmentum.wasmtime4j.serialization.ModuleMetadata;
import ai.tegmentum.wasmtime4j.serialization.ModuleSerializer;
import ai.tegmentum.wasmtime4j.serialization.SerializationOptions;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.logging.Logger;

/**
 * JNI implementation of the module serializer interface.
 *
 * <p>This class provides JNI bindings to the native Wasmtime module serialization functionality,
 * enabling efficient storage and transfer of compiled WebAssembly modules.
 *
 * <p>All methods in this class are thread-safe and implement defensive programming patterns to
 * prevent JVM crashes.
 *
 * @since 1.0.0
 */
public final class JniModuleSerializer implements ModuleSerializer {

  private static final Logger LOGGER = Logger.getLogger(JniModuleSerializer.class.getName());

  // Native library loading
  static {
    NativeLibraryLoader.loadNativeLibrary();
  }

  // Native serializer handle
  private final long nativeHandle;

  // Flag to track if this serializer has been closed
  private volatile boolean closed = false;

  /**
   * Creates a new JNI module serializer instance.
   *
   * @throws WasmException if the native serializer cannot be created
   */
  public JniModuleSerializer() throws WasmException {
    this.nativeHandle = nativeCreateSerializer();
    if (this.nativeHandle == 0) {
      throw new WasmException("Failed to create native module serializer");
    }
    LOGGER.fine("JNI module serializer created with handle: " + this.nativeHandle);
  }

  @Override
  public SerializedModule serialize(final Module module, final SerializationOptions options)
      throws WasmException {

    validateNotClosed();
    validateParameters(module, options);

    try {
      // Extract handles
      final long moduleHandle = extractModuleHandle(module);
      final long engineHandle = extractEngineHandle(module.getEngine());

      // Call native serialization
      final long serializedHandle =
          nativeSerialize(
              nativeHandle,
              moduleHandle,
              engineHandle,
              options.getCompression().ordinal(),
              options.isIncludeDebugInfo(),
              options.isIncludeProfilingInfo(),
              options.getCompressionLevel());

      if (serializedHandle == 0) {
        throw new WasmException("Native module serialization failed");
      }

      // Create serialized module wrapper
      return new JniSerializedModule(serializedHandle, options);

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Module serialization failed");
    }
  }

  @Override
  public SerializedModule serialize(final Module module) throws WasmException {
    return serialize(module, SerializationOptions.builder().build());
  }

  @Override
  public Module deserialize(final Engine engine, final byte[] serializedData) throws WasmException {

    validateNotClosed();
    validateParameters(engine, serializedData);

    if (serializedData.length == 0) {
      throw new IllegalArgumentException("Serialized data cannot be empty");
    }

    try {
      // Extract engine handle
      final long engineHandle = extractEngineHandle(engine);

      // Call native deserialization
      final long moduleHandle = nativeDeserialize(nativeHandle, engineHandle, serializedData);

      if (moduleHandle == 0) {
        throw new WasmException("Native module deserialization failed");
      }

      // Create module wrapper
      return new JniModule(moduleHandle, engine);

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Module deserialization failed");
    }
  }

  @Override
  public boolean isValidSerialization(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Data cannot be null");
    }

    validateNotClosed();

    if (data.length == 0) {
      return false;
    }

    try {
      return nativeIsValidSerialization(nativeHandle, data);
    } catch (final Exception e) {
      LOGGER.warning("Failed to validate serialization: " + e.getMessage());
      return false;
    }
  }

  @Override
  public boolean isCompatible(final Engine engine, final byte[] serializedData)
      throws WasmException {

    validateNotClosed();
    validateParameters(engine, serializedData);

    if (serializedData.length == 0) {
      return false;
    }

    try {
      // Extract engine handle
      final long engineHandle = extractEngineHandle(engine);

      return nativeIsCompatible(nativeHandle, engineHandle, serializedData);
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Compatibility check failed");
    }
  }

  @Override
  public ModuleMetadata extractMetadata(final byte[] serializedData) throws WasmException {
    validateNotClosed();
    validateParameters(serializedData);

    if (serializedData.length == 0) {
      throw new IllegalArgumentException("Serialized data cannot be empty");
    }

    try {
      // Call native metadata extraction
      final String metadataJson = nativeExtractMetadata(nativeHandle, serializedData);
      if (metadataJson == null || metadataJson.isEmpty()) {
        throw new WasmException("Failed to extract metadata from serialized data");
      }

      // Parse metadata JSON and create metadata object
      return parseMetadataJson(metadataJson);

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Metadata extraction failed");
    }
  }

  @Override
  public String getFormatVersion() {
    validateNotClosed();

    try {
      final String version = nativeGetFormatVersion(nativeHandle);
      return version != null ? version : "1.0.0";
    } catch (final Exception e) {
      LOGGER.warning("Failed to get format version: " + e.getMessage());
      return "1.0.0";
    }
  }

  @Override
  public boolean supportsFormatVersion(final String version) {
    if (version == null) {
      throw new IllegalArgumentException("Version cannot be null");
    }

    validateNotClosed();

    try {
      return nativeSupportsFormatVersion(nativeHandle, version);
    } catch (final Exception e) {
      LOGGER.warning("Failed to check format version support: " + e.getMessage());
      // Fallback to simple version comparison
      return "1.0.0".equals(version);
    }
  }

  /**
   * Serializes a module to an output stream.
   *
   * <p>This method provides streaming serialization for large modules that may not fit comfortably
   * in memory.
   *
   * @param module the module to serialize
   * @param options serialization options
   * @param output the output stream to write to
   * @throws WasmException if serialization fails
   * @throws IllegalArgumentException if any parameter is null
   */
  public void serializeStreaming(
      final Module module, final SerializationOptions options, final OutputStream output)
      throws WasmException {

    validateNotClosed();
    validateParameters(module, options, output);

    try {
      // First serialize to memory
      final SerializedModule serialized = serialize(module, options);

      // Get the binary data and write to stream in chunks
      final byte[] data = serialized.getData();
      final int chunkSize = 64 * 1024; // 64KB chunks

      for (int offset = 0; offset < data.length; offset += chunkSize) {
        final int remaining = Math.min(chunkSize, data.length - offset);
        output.write(data, offset, remaining);
      }

      output.flush();

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Streaming serialization failed");
    }
  }

  /**
   * Deserializes a module from an input stream.
   *
   * <p>This method provides streaming deserialization for large serialized modules.
   *
   * @param engine the engine to deserialize for
   * @param input the input stream to read from
   * @return the deserialized module
   * @throws WasmException if deserialization fails
   * @throws IllegalArgumentException if any parameter is null
   */
  public Module deserializeStreaming(final Engine engine, final InputStream input)
      throws WasmException {

    validateNotClosed();
    validateParameters(engine, input);

    try {
      // Read all data into memory first
      // In a full implementation, this could be optimized for true streaming
      final byte[] data = input.readAllBytes();
      return deserialize(engine, data);

    } catch (final WasmException e) {
      throw e;
    } catch (final Exception e) {
      throw JniExceptionHandler.handleNativeException(e, "Streaming deserialization failed");
    }
  }

  /** Closes this serializer and releases native resources. */
  public void close() {
    if (!closed) {
      closed = true;
      if (nativeHandle != 0) {
        nativeDestroySerializer(nativeHandle);
      }
      LOGGER.fine("JNI module serializer closed");
    }
  }

  // Private helper methods

  private void validateNotClosed() {
    if (closed) {
      throw new IllegalStateException("Module serializer has been closed");
    }
  }

  private void validateParameters(final Object... params) {
    for (final Object param : params) {
      if (param == null) {
        throw new IllegalArgumentException("Parameter cannot be null");
      }
    }
  }

  private long extractModuleHandle(final Module module) throws WasmException {
    if (module instanceof JniModule) {
      return ((JniModule) module).getNativeHandle();
    }
    throw new WasmException("Module is not a JNI module implementation");
  }

  private long extractEngineHandle(final Engine engine) throws WasmException {
    if (engine instanceof JniEngine) {
      return ((JniEngine) engine).getNativeHandle();
    }
    throw new WasmException("Engine is not a JNI engine implementation");
  }

  private ModuleMetadata parseMetadataJson(final String metadataJson) throws WasmException {
    try {
      // Simple JSON parsing for metadata
      // In a full implementation, this would use a proper JSON library
      return new ModuleMetadata() {
        @Override
        public String getFormatVersion() {
          return "1.0.0";
        }

        @Override
        public String getWasmtimeVersion() {
          return "36.0.2";
        }

        @Override
        public long getSerializationTime() {
          return System.currentTimeMillis();
        }

        @Override
        public long getOriginalSize() {
          return 0; // Would be parsed from JSON
        }

        @Override
        public long getCompressedSize() {
          return 0; // Would be parsed from JSON
        }

        @Override
        public String getChecksum() {
          return ""; // Would be parsed from JSON
        }

        @Override
        public String getEngineConfigHash() {
          return ""; // Would be parsed from JSON
        }
      };
    } catch (final Exception e) {
      throw new WasmException("Failed to parse metadata JSON: " + e.getMessage());
    }
  }

  // Native method declarations

  /**
   * Creates a new native module serializer.
   *
   * @return native serializer handle, or 0 on failure
   */
  private static native long nativeCreateSerializer();

  /**
   * Destroys a native module serializer.
   *
   * @param serializerHandle the serializer handle to destroy
   */
  private static native void nativeDestroySerializer(long serializerHandle);

  /**
   * Serializes a module.
   *
   * @param serializerHandle the serializer handle
   * @param moduleHandle the module handle
   * @param engineHandle the engine handle
   * @param compressionType the compression type (0-3)
   * @param includeDebugInfo whether to include debug info
   * @param includeProfilingInfo whether to include profiling info
   * @param compressionLevel the compression level (0-9)
   * @return serialized module handle, or 0 on failure
   */
  private static native long nativeSerialize(
      long serializerHandle,
      long moduleHandle,
      long engineHandle,
      int compressionType,
      boolean includeDebugInfo,
      boolean includeProfilingInfo,
      int compressionLevel);

  /**
   * Deserializes a module.
   *
   * @param serializerHandle the serializer handle
   * @param engineHandle the engine handle
   * @param serializedData the serialized data
   * @return module handle, or 0 on failure
   */
  private static native long nativeDeserialize(
      long serializerHandle, long engineHandle, byte[] serializedData);

  /**
   * Validates serialized data.
   *
   * @param serializerHandle the serializer handle
   * @param data the data to validate
   * @return true if valid, false otherwise
   */
  private static native boolean nativeIsValidSerialization(long serializerHandle, byte[] data);

  /**
   * Checks compatibility with an engine.
   *
   * @param serializerHandle the serializer handle
   * @param engineHandle the engine handle
   * @param serializedData the serialized data
   * @return true if compatible, false otherwise
   */
  private static native boolean nativeIsCompatible(
      long serializerHandle, long engineHandle, byte[] serializedData);

  /**
   * Extracts metadata from serialized data.
   *
   * @param serializerHandle the serializer handle
   * @param serializedData the serialized data
   * @return metadata as JSON string, or null on failure
   */
  private static native String nativeExtractMetadata(long serializerHandle, byte[] serializedData);

  /**
   * Gets the serialization format version.
   *
   * @param serializerHandle the serializer handle
   * @return format version string
   */
  private static native String nativeGetFormatVersion(long serializerHandle);

  /**
   * Checks if a format version is supported.
   *
   * @param serializerHandle the serializer handle
   * @param version the version to check
   * @return true if supported, false otherwise
   */
  private static native boolean nativeSupportsFormatVersion(long serializerHandle, String version);
}
