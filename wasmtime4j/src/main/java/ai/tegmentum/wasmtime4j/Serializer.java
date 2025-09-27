package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.factory.WasmRuntimeFactory;
import java.io.Closeable;

/**
 * WebAssembly module serialization interface.
 *
 * <p>A Serializer provides efficient serialization and deserialization of compiled WebAssembly
 * modules. Serialization allows compiled modules to be cached to disk, transmitted over network, or
 * stored for later use without requiring recompilation.
 *
 * <p>Serialized modules contain the compiled code and metadata in an optimized format that can be
 * quickly deserialized back into executable modules. The serialization format is specific to the
 * engine configuration and target architecture.
 *
 * <p>Serializers support caching with configurable compression and size limits to optimize
 * performance for repeated serialization operations.
 *
 * @since 1.0.0
 */
public interface Serializer extends Closeable {

  /**
   * Serializes a compiled WebAssembly module to bytes.
   *
   * <p>The module must have been compiled with an engine that has compatible configuration with
   * this serializer. The resulting bytes can be stored and later deserialized to recreate the
   * module without recompilation.
   *
   * @param engine the engine used to compile the module
   * @param moduleBytes the original WebAssembly bytecode
   * @return the serialized module data
   * @throws WasmException if serialization fails
   * @throws IllegalArgumentException if engine or moduleBytes is null
   * @since 1.0.0
   */
  byte[] serialize(final Engine engine, final byte[] moduleBytes) throws WasmException;

  /**
   * Deserializes previously serialized module data back into a compiled module.
   *
   * <p>The serialized data must have been created with compatible engine configuration.
   * Deserialization is typically much faster than compilation from WebAssembly bytecode.
   *
   * @param engine the engine to use for deserialization (must match original engine config)
   * @param serializedBytes the serialized module data
   * @return the deserialized module ready for instantiation
   * @throws WasmException if deserialization fails
   * @throws IllegalArgumentException if engine or serializedBytes is null
   * @since 1.0.0
   */
  Module deserialize(final Engine engine, final byte[] serializedBytes) throws WasmException;

  /**
   * Clears the internal serialization cache.
   *
   * <p>This operation removes all cached serialization data, freeing memory but potentially
   * impacting performance for subsequent serialization operations.
   *
   * @return true if the cache was successfully cleared, false if already empty
   * @throws WasmException if cache clearing fails
   * @since 1.0.0
   */
  boolean clearCache() throws WasmException;

  /**
   * Gets the number of entries in the serialization cache.
   *
   * <p>Returns the count of cached serialization operations that can be reused for performance
   * optimization.
   *
   * @return the number of cache entries
   * @since 1.0.0
   */
  int getCacheEntryCount();

  /**
   * Gets the total size of the serialization cache in bytes.
   *
   * <p>Returns the memory usage of all cached serialization data.
   *
   * @return the cache size in bytes
   * @since 1.0.0
   */
  long getCacheTotalSize();

  /**
   * Gets the cache hit rate as a percentage.
   *
   * <p>Returns the ratio of cache hits to total cache operations, indicating the effectiveness of
   * the caching strategy.
   *
   * @return the cache hit rate between 0.0 and 1.0
   * @since 1.0.0
   */
  double getCacheHitRate();

  /**
   * Closes the serializer and releases associated resources.
   *
   * <p>After closing, the serializer becomes invalid and should not be used. All cached data will
   * be released.
   */
  @Override
  void close();

  /**
   * Creates a new Serializer with default configuration.
   *
   * <p>The default configuration provides reasonable caching and compression settings for most use
   * cases.
   *
   * @return a new Serializer instance
   * @throws WasmException if the serializer cannot be created
   * @since 1.0.0
   */
  static Serializer create() throws WasmException {
    return WasmRuntimeFactory.create().createSerializer();
  }

  /**
   * Creates a new Serializer with custom configuration.
   *
   * <p>This factory method allows customizing cache size, compression settings, and other
   * serialization parameters for optimal performance.
   *
   * @param maxCacheSize the maximum cache size in bytes (0 for unlimited)
   * @param enableCompression whether to enable compression of serialized data
   * @param compressionLevel the compression level (0-9, higher is better compression)
   * @return a new Serializer instance with the specified configuration
   * @throws WasmException if the serializer cannot be created
   * @throws IllegalArgumentException if parameters are invalid
   * @since 1.0.0
   */
  static Serializer create(
      final long maxCacheSize, final boolean enableCompression, final int compressionLevel)
      throws WasmException {
    return WasmRuntimeFactory.create()
        .createSerializer(maxCacheSize, enableCompression, compressionLevel);
  }
}
