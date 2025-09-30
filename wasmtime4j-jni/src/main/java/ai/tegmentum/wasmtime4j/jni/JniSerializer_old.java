package ai.tegmentum.wasmtime4j.jni;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.Serializer;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.jni.util.JniResource;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * JNI implementation of the Serializer interface.
 *
 * <p>This class provides WebAssembly module serialization functionality using Java Native Interface
 * (JNI) to communicate with the native Wasmtime library. It manages serialization cache and
 * compression settings.
 *
 * <p>Key features:
 *
 * <ul>
 *   <li>Efficient module serialization and deserialization
 *   <li>Configurable compression and caching
 *   <li>Automatic resource cleanup and management
 *   <li>Thread-safe operations
 * </ul>
 *
 * @since 1.0.0
 */
public final class JniSerializer extends JniResource implements Serializer {

  private static final Logger LOGGER = Logger.getLogger(JniSerializer.class.getName());

  private final long maxModules;
  private final boolean enableCompression;
  private final int compressionLevel;
  private final AtomicLong totalOperations;
  private final AtomicLong cacheHits;

  /**
   * Creates a new JNI serializer with the specified configuration.
   *
   * @param nativeHandle the native serializer handle
   * @param maxModules maximum number of modules to cache
   * @param enableCompression whether to enable compression
   * @param compressionLevel compression level (0-9)
   */
  public JniSerializer(
      final long nativeHandle,
      final long maxModules,
      final boolean enableCompression,
      final int compressionLevel) {
    super(nativeHandle);
    this.maxModules = maxModules;
    this.enableCompression = enableCompression;
    this.compressionLevel = compressionLevel;
    this.totalOperations = new AtomicLong(0);
    this.cacheHits = new AtomicLong(0);
    LOGGER.fine(
        "Created JNI serializer with handle: 0x"
            + Long.toHexString(nativeHandle)
            + ", compression: "
            + enableCompression);
  }

  @Override
  public byte[] serialize(final Engine engine, final byte[] moduleBytes) throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonNull(moduleBytes, "moduleBytes");
    ensureNotClosed();

    if (moduleBytes.length == 0) {
      throw new IllegalArgumentException("Module bytes cannot be empty");
    }

    try {
      totalOperations.incrementAndGet();
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JNI engine instance");
      }

      final JniEngine jniEngine = (JniEngine) engine;
      final byte[] result =
          nativeSerialize(getNativeHandle(), jniEngine.getNativeHandle(), moduleBytes);
      if (result == null) {
        throw new WasmException("Serialization failed");
      }

      LOGGER.fine("Serialized module: " + moduleBytes.length + " -> " + result.length + " bytes");
      return result;
    } catch (final Exception e) {
      throw new WasmException("Failed to serialize module", e);
    }
  }

  @Override
  public Module deserialize(final Engine engine, final byte[] serializedBytes)
      throws WasmException {
    JniValidation.requireNonNull(engine, "engine");
    JniValidation.requireNonNull(serializedBytes, "serializedBytes");
    ensureNotClosed();

    if (serializedBytes.length == 0) {
      throw new IllegalArgumentException("Serialized bytes cannot be empty");
    }

    try {
      totalOperations.incrementAndGet();
      if (!(engine instanceof JniEngine)) {
        throw new IllegalArgumentException("Engine must be a JNI engine instance");
      }

      final JniEngine jniEngine = (JniEngine) engine;
      final long moduleHandle =
          nativeDeserialize(getNativeHandle(), jniEngine.getNativeHandle(), serializedBytes);
      if (moduleHandle == 0) {
        throw new WasmException("Deserialization failed");
      }

      LOGGER.fine("Deserialized module from " + serializedBytes.length + " bytes");
      return new JniModule(moduleHandle, engine);
    } catch (final Exception e) {
      throw new WasmException("Failed to deserialize module", e);
    }
  }

  @Override
  public boolean clearCache() throws WasmException {
    ensureNotClosed();

    try {
      final boolean result = nativeClearCache(getNativeHandle());
      if (result) {
        LOGGER.fine("Cleared serialization cache");
      }
      return result;
    } catch (final Exception e) {
      throw new WasmException("Failed to clear cache", e);
    }
  }

  @Override
  public int getCacheEntryCount() {
    ensureNotClosed();
    return nativeGetCacheEntryCount(getNativeHandle());
  }

  @Override
  public long getCacheTotalSize() {
    ensureNotClosed();
    return nativeGetCacheTotalSize(getNativeHandle());
  }

  @Override
  public double getCacheHitRate() {
    final long total = totalOperations.get();
    if (total == 0) {
      return 0.0;
    }
    return (double) cacheHits.get() / total;
  }

  @Override
  protected void doClose() throws Exception {
    if (getNativeHandle() != 0) {
      nativeDestroySerializer(getNativeHandle());
      LOGGER.fine("Destroyed JNI serializer with handle: 0x" + Long.toHexString(getNativeHandle()));
    }
  }

  @Override
  protected String getResourceType() {
    return "Serializer";
  }

  // Native method declarations

  /**
   * Serializes a module to bytes.
   *
   * @param serializerHandle the native serializer handle
   * @param engineHandle the native engine handle
   * @param moduleBytes the module bytecode
   * @return the serialized data or null on failure
   */
  private static native byte[] nativeSerialize(
      long serializerHandle, long engineHandle, byte[] moduleBytes);

  /**
   * Deserializes a module from bytes.
   *
   * @param serializerHandle the native serializer handle
   * @param engineHandle the native engine handle
   * @param serializedBytes the serialized data
   * @return the module handle or 0 on failure
   */
  private static native long nativeDeserialize(
      long serializerHandle, long engineHandle, byte[] serializedBytes);

  /**
   * Clears the serialization cache.
   *
   * @param serializerHandle the native serializer handle
   * @return true if cache was cleared, false if already empty
   */
  private static native boolean nativeClearCache(long serializerHandle);

  /**
   * Gets the cache entry count.
   *
   * @param serializerHandle the native serializer handle
   * @return the number of cache entries
   */
  private static native int nativeGetCacheEntryCount(long serializerHandle);

  /**
   * Gets the total cache size.
   *
   * @param serializerHandle the native serializer handle
   * @return the cache size in bytes
   */
  private static native long nativeGetCacheTotalSize(long serializerHandle);

  /**
   * Destroys a native serializer.
   *
   * @param serializerHandle the native serializer handle
   */
  private static native void nativeDestroySerializer(long serializerHandle);
}
