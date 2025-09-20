package ai.tegmentum.wasmtime4j.serialization;

import java.util.Set;

/**
 * Configuration options for module serialization.
 *
 * <p>SerializationOptions control various aspects of the serialization process including
 * compression, debug information inclusion, optimization settings, and metadata preservation.
 *
 * <p>These options affect both the size and compatibility of the resulting serialized module.
 * Different option combinations may produce serialized modules that are only compatible with
 * engines configured with matching settings.
 *
 * @since 1.0.0
 */
public interface SerializationOptions {

  /**
   * Gets the compression type to use for serialization.
   *
   * <p>Compression reduces the size of serialized modules but adds processing overhead during
   * serialization and deserialization. Different compression types provide different trade-offs
   * between size reduction and performance.
   *
   * @return the compression type
   */
  CompressionType getCompression();

  /**
   * Checks if debug information should be included in the serialized module.
   *
   * <p>Debug information includes source locations, function names, and other metadata useful for
   * debugging and profiling. Including debug information increases the size of serialized modules
   * but enables better error reporting and profiling capabilities.
   *
   * @return true if debug information should be included, false otherwise
   */
  boolean isIncludeDebugInfo();

  /**
   * Checks if source map information should be included in the serialized module.
   *
   * <p>Source maps provide mapping between compiled WebAssembly and original source code,
   * enabling source-level debugging. This option is independent of debug information and
   * specifically relates to source mapping data.
   *
   * @return true if source map information should be included, false otherwise
   */
  boolean isIncludeSourceMap();

  /**
   * Gets the optimization levels to apply during serialization.
   *
   * <p>Optimization levels control various compiler optimizations that may be applied to the
   * module during serialization. Higher optimization levels may produce faster code but can
   * increase compilation time and reduce debugging capabilities.
   *
   * @return the set of optimization levels to apply
   */
  Set<OptimizationLevel> getOptimizations();

  /**
   * Gets the target platform for serialization.
   *
   * <p>The target platform affects code generation and optimization decisions. Serialized modules
   * are typically optimized for specific platforms and may not be compatible across different
   * architectures.
   *
   * @return the target platform, or null for current platform
   */
  TargetPlatform getTargetPlatform();

  /**
   * Checks if metadata validation should be strict.
   *
   * <p>Strict validation performs additional checks during serialization to ensure compatibility
   * and correctness. This may catch potential issues early but adds processing overhead.
   *
   * @return true if strict validation is enabled, false otherwise
   */
  boolean isStrictValidation();

  /**
   * Gets the maximum size limit for serialized modules.
   *
   * <p>This limit prevents serialization of extremely large modules that might cause memory
   * issues. Serialization will fail if the resulting data would exceed this limit.
   *
   * @return the maximum size in bytes, or -1 for no limit
   */
  long getMaxSize();

  /**
   * Checks if checksums should be included for integrity validation.
   *
   * <p>Checksums enable detection of data corruption in serialized modules. They add a small
   * amount of overhead but provide strong integrity guarantees.
   *
   * @return true if checksums should be included, false otherwise
   */
  boolean isIncludeChecksum();

  /**
   * Creates a builder for constructing SerializationOptions.
   *
   * @return a new SerializationOptions builder
   */
  static SerializationOptionsBuilder builder() {
    return new SerializationOptionsBuilder();
  }

  /**
   * Creates SerializationOptions with default settings.
   *
   * <p>Default settings: no compression, no debug info, no source maps, basic optimization,
   * current platform, non-strict validation, no size limit, include checksums.
   *
   * @return SerializationOptions with default settings
   */
  static SerializationOptions defaults() {
    return builder().build();
  }
}