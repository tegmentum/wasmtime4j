package ai.tegmentum.wasmtime4j.serialization;

import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Metadata associated with a serialized WebAssembly module.
 *
 * <p>ModuleMetadata contains information about the serialized module including version information,
 * compilation settings, target platform, and other details required for validation and
 * compatibility checking.
 *
 * <p>This metadata is embedded in serialized modules and can be extracted without full
 * deserialization to enable efficient compatibility checking and caching decisions.
 *
 * @since 1.0.0
 */
public interface ModuleMetadata {

  /**
   * Gets the format version of the serialized module.
   *
   * <p>The format version indicates the serialization format and compatibility level. Different
   * versions may not be compatible with each other.
   *
   * @return the serialization format version
   */
  String getFormatVersion();

  /**
   * Gets the version of Wasmtime used to compile this module.
   *
   * <p>This information is crucial for compatibility checking as modules compiled with different
   * Wasmtime versions may not be compatible.
   *
   * @return the Wasmtime version string
   */
  String getWasmtimeVersion();

  /**
   * Gets the target platform this module was compiled for.
   *
   * <p>Modules are typically optimized for specific platforms and may not work correctly on
   * different operating systems or CPU architectures.
   *
   * @return the target platform
   */
  TargetPlatform getTargetPlatform();

  /**
   * Gets the compilation timestamp when this module was serialized.
   *
   * <p>This timestamp can be used for cache invalidation and version tracking.
   *
   * @return the compilation timestamp
   */
  Instant getCompilationTimestamp();

  /**
   * Gets the optimization levels that were applied during compilation.
   *
   * <p>This information affects compatibility and performance characteristics of the serialized
   * module.
   *
   * @return the set of optimization levels applied
   */
  Set<OptimizationLevel> getOptimizationLevels();

  /**
   * Gets the compression type used for this serialized module.
   *
   * @return the compression type
   */
  CompressionType getCompressionType();

  /**
   * Checks if debug information was included in the serialized module.
   *
   * @return true if debug information is included, false otherwise
   */
  boolean hasDebugInfo();

  /**
   * Checks if source map information was included in the serialized module.
   *
   * @return true if source map information is included, false otherwise
   */
  boolean hasSourceMap();

  /**
   * Gets the original WebAssembly module hash.
   *
   * <p>This hash can be used to verify that the serialized module corresponds to the expected
   * original WebAssembly bytecode.
   *
   * @return the original module hash as a hex string
   */
  String getOriginalModuleHash();

  /**
   * Gets the size of the serialized module data in bytes.
   *
   * @return the serialized data size
   */
  long getDataSize();

  /**
   * Gets the size of the original WebAssembly module in bytes.
   *
   * @return the original module size
   */
  long getOriginalSize();

  /**
   * Gets the compression ratio achieved during serialization.
   *
   * <p>This is calculated as originalSize / dataSize. A value greater than 1 indicates compression,
   * while 1 indicates no size change.
   *
   * @return the compression ratio
   */
  double getCompressionRatio();

  /**
   * Gets custom metadata properties.
   *
   * <p>This map contains additional metadata properties that may be specific to certain use cases
   * or implementations.
   *
   * @return an immutable map of custom metadata properties
   */
  Map<String, String> getCustomProperties();

  /**
   * Gets the checksum of the serialized data.
   *
   * <p>This checksum can be used to verify data integrity and detect corruption.
   *
   * @return the checksum as a hex string, or null if no checksum was calculated
   */
  String getChecksum();

  /**
   * Checks if this metadata indicates compatibility with the given platform.
   *
   * @param platform the platform to check compatibility with
   * @return true if compatible, false otherwise
   * @throws IllegalArgumentException if platform is null
   */
  boolean isCompatibleWith(final TargetPlatform platform);

  /**
   * Checks if this metadata indicates compatibility with the given Wasmtime version.
   *
   * @param wasmtimeVersion the Wasmtime version to check compatibility with
   * @return true if compatible, false otherwise
   * @throws IllegalArgumentException if wasmtimeVersion is null
   */
  boolean isCompatibleWith(final String wasmtimeVersion);

  /**
   * Gets a string representation of this metadata for logging and debugging.
   *
   * @return a formatted string containing key metadata information
   */
  String toSummaryString();
}
