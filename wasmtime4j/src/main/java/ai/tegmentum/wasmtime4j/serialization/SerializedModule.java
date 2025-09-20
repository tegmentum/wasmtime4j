package ai.tegmentum.wasmtime4j.serialization;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * Represents a serialized WebAssembly module that can be persisted and later deserialized.
 *
 * <p>SerializedModule contains the compiled module bytecode along with metadata required for
 * validation and compatibility checking. It provides efficient storage and transfer of compiled
 * modules without requiring recompilation.
 *
 * <p>Serialized modules include compatibility information to ensure they can only be deserialized
 * with compatible engines and configurations.
 *
 * @since 1.0.0
 */
public interface SerializedModule {

  /**
   * Gets the serialized module data as a byte array.
   *
   * <p>The data includes the compiled module bytecode, metadata, and any additional information
   * required for deserialization. The format is specific to the Wasmtime runtime and may include
   * compression.
   *
   * @return the serialized module data
   */
  byte[] getData();

  /**
   * Gets metadata about the serialized module.
   *
   * <p>Metadata includes version information, compilation settings, target platform details, and
   * other information required for compatibility validation.
   *
   * @return the module metadata
   */
  ModuleMetadata getMetadata();

  /**
   * Checks if this serialized module is compatible with the given engine.
   *
   * <p>Compatibility is determined by comparing the engine configuration, target platform, Wasmtime
   * version, and other relevant settings used during serialization.
   *
   * @param engine the engine to check compatibility with
   * @return true if the module is compatible with the engine, false otherwise
   * @throws IllegalArgumentException if engine is null
   */
  boolean isCompatible(final Engine engine);

  /**
   * Deserializes this module for use with the given engine.
   *
   * <p>The deserialization process validates compatibility and reconstructs the module for
   * execution. This is significantly faster than recompiling from WebAssembly bytecode.
   *
   * @param engine the engine to deserialize the module for
   * @return the deserialized Module ready for instantiation
   * @throws WasmException if deserialization fails or compatibility check fails
   * @throws IllegalArgumentException if engine is null
   */
  Module deserialize(final Engine engine) throws WasmException;

  /**
   * Gets the size of the serialized data in bytes.
   *
   * @return the size of the serialized data
   */
  long getSize();

  /**
   * Gets the compression type used for this serialized module.
   *
   * @return the compression type, or NONE if no compression was used
   */
  CompressionType getCompressionType();

  /**
   * Checks if this serialized module includes debug information.
   *
   * @return true if debug information is included, false otherwise
   */
  boolean hasDebugInfo();

  /**
   * Checks if this serialized module includes source map information.
   *
   * @return true if source map information is included, false otherwise
   */
  boolean hasSourceMap();

  /**
   * Gets the checksum of the serialized data for integrity validation.
   *
   * @return the checksum as a hex string
   */
  String getChecksum();
}
