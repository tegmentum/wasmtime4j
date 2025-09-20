package ai.tegmentum.wasmtime4j.serialization;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Provides functionality to serialize and deserialize WebAssembly modules.
 *
 * <p>ModuleSerializer enables efficient storage and transfer of compiled modules without requiring
 * recompilation. It supports various serialization options including compression, debug information
 * inclusion, and optimization settings.
 *
 * <p>Serialization is platform and engine-specific, ensuring compatibility when deserializing
 * modules on the same or compatible configurations.
 *
 * @since 1.0.0
 */
public interface ModuleSerializer {

  /**
   * Serializes a compiled module with the specified options.
   *
   * <p>The serialization process captures the compiled module state along with metadata required
   * for later deserialization. The resulting SerializedModule can be stored persistently or
   * transferred between compatible environments.
   *
   * @param module the compiled module to serialize
   * @param options serialization options controlling the output format
   * @return a SerializedModule containing the serialized data and metadata
   * @throws WasmException if serialization fails
   * @throws IllegalArgumentException if module or options is null
   */
  SerializedModule serialize(final Module module, final SerializationOptions options)
      throws WasmException;

  /**
   * Serializes a compiled module with default options.
   *
   * <p>Uses default serialization settings: no compression, no debug information, standard
   * optimization level.
   *
   * @param module the compiled module to serialize
   * @return a SerializedModule containing the serialized data and metadata
   * @throws WasmException if serialization fails
   * @throws IllegalArgumentException if module is null
   */
  SerializedModule serialize(final Module module) throws WasmException;

  /**
   * Deserializes a module from serialized data.
   *
   * <p>The deserialization process validates the data format, checks compatibility with the
   * provided engine, and reconstructs the module for execution.
   *
   * @param engine the engine to deserialize the module for
   * @param serializedData the serialized module data
   * @return the deserialized Module ready for instantiation
   * @throws WasmException if deserialization fails or data is invalid
   * @throws IllegalArgumentException if engine or serializedData is null
   */
  Module deserialize(final Engine engine, final byte[] serializedData) throws WasmException;

  /**
   * Validates that the provided data represents a valid serialized module.
   *
   * <p>This method performs format validation and integrity checks without fully deserializing the
   * module. It's useful for validating cached or stored serialized modules.
   *
   * @param data the serialized module data to validate
   * @return true if the data represents a valid serialized module, false otherwise
   * @throws IllegalArgumentException if data is null
   */
  boolean isValidSerialization(final byte[] data);

  /**
   * Checks if the serialized data is compatible with the given engine.
   *
   * <p>Compatibility is determined by examining metadata in the serialized data and comparing it
   * with the engine configuration, without fully deserializing the module.
   *
   * @param engine the engine to check compatibility with
   * @param serializedData the serialized module data
   * @return true if the data is compatible with the engine, false otherwise
   * @throws WasmException if the serialized data is invalid or corrupted
   * @throws IllegalArgumentException if engine or serializedData is null
   */
  boolean isCompatible(final Engine engine, final byte[] serializedData) throws WasmException;

  /**
   * Extracts metadata from serialized module data without full deserialization.
   *
   * <p>This method efficiently reads the metadata header from serialized data, providing
   * information about the module without the overhead of full deserialization.
   *
   * @param serializedData the serialized module data
   * @return the module metadata
   * @throws WasmException if the serialized data is invalid or corrupted
   * @throws IllegalArgumentException if serializedData is null
   */
  ModuleMetadata extractMetadata(final byte[] serializedData) throws WasmException;

  /**
   * Gets the format version supported by this serializer.
   *
   * @return the serialization format version
   */
  String getFormatVersion();

  /**
   * Checks if this serializer supports the given format version.
   *
   * @param version the format version to check
   * @return true if the version is supported, false otherwise
   * @throws IllegalArgumentException if version is null
   */
  boolean supportsFormatVersion(final String version);
}
