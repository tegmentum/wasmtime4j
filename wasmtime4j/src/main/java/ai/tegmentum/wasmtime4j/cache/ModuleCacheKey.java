package ai.tegmentum.wasmtime4j.cache;

import ai.tegmentum.wasmtime4j.EngineConfig;
import java.util.Objects;

/**
 * Represents a unique key for identifying cached WebAssembly modules.
 *
 * <p>ModuleCacheKey combines various factors that affect module compilation to create a unique
 * identifier. Modules with the same cache key should be functionally equivalent and
 * interchangeable.
 *
 * <p>Cache keys include the original WebAssembly bytecode hash, engine configuration, compilation
 * settings, and other factors that influence the compilation result.
 *
 * @since 1.0.0
 */
public interface ModuleCacheKey {

  /**
   * Gets the hash of the original WebAssembly bytecode.
   *
   * <p>This hash uniquely identifies the source WebAssembly module that was compiled. Modules
   * compiled from the same bytecode will have the same hash.
   *
   * @return the WebAssembly bytecode hash as a byte array
   */
  byte[] getWasmHash();

  /**
   * Gets the engine configuration used for compilation.
   *
   * <p>The engine configuration affects compilation behavior and the resulting module. Different
   * engine configurations will produce different cache keys even for the same WebAssembly
   * bytecode.
   *
   * @return the engine configuration
   */
  EngineConfig getEngineConfig();

  /**
   * Gets the compilation settings used for this module.
   *
   * <p>Compilation settings include optimization levels, feature flags, and other parameters
   * that affect the compilation process.
   *
   * @return the compilation settings
   */
  CompilationSettings getCompilationSettings();

  /**
   * Gets the timestamp when this cache key was created.
   *
   * <p>The timestamp can be used for cache expiration and invalidation policies. It represents
   * when the module was compiled, not when the cache key was created.
   *
   * @return the creation timestamp in milliseconds since epoch
   */
  long getTimestamp();

  /**
   * Gets the target platform for this cached module.
   *
   * <p>Modules compiled for different platforms are not interchangeable and must have different
   * cache keys.
   *
   * @return the target platform identifier
   */
  String getTargetPlatform();

  /**
   * Gets the version of the compiler/runtime used.
   *
   * <p>Modules compiled with different versions may not be compatible and should have different
   * cache keys.
   *
   * @return the compiler/runtime version
   */
  String getCompilerVersion();

  /**
   * Gets a string representation of this cache key suitable for use as a filename or identifier.
   *
   * <p>The string representation is deterministic and unique for each distinct cache key. It
   * should be safe for use in file systems and URLs.
   *
   * @return a string representation of the cache key
   */
  String toStringRepresentation();

  /**
   * Gets a short hash representation of this cache key.
   *
   * <p>The short hash provides a compact representation useful for logging and debugging. It
   * should be reasonably unique but may have collisions.
   *
   * @return a short hash string (typically 8-16 characters)
   */
  String getShortHash();

  /**
   * Checks if this cache key is compatible with another key.
   *
   * <p>Compatible keys represent modules that can be used interchangeably. This is typically
   * true only when the keys are exactly equal, but some implementations may allow for compatible
   * differences.
   *
   * @param other the other cache key to check compatibility with
   * @return true if the keys are compatible, false otherwise
   */
  boolean isCompatibleWith(final ModuleCacheKey other);

  /**
   * Creates a ModuleCacheKey from the given parameters.
   *
   * @param wasmHash the hash of the WebAssembly bytecode
   * @param engineConfig the engine configuration
   * @param compilationSettings the compilation settings
   * @param targetPlatform the target platform
   * @param compilerVersion the compiler version
   * @return a new ModuleCacheKey instance
   * @throws IllegalArgumentException if any required parameter is null
   */
  static ModuleCacheKey create(
      final byte[] wasmHash,
      final EngineConfig engineConfig,
      final CompilationSettings compilationSettings,
      final String targetPlatform,
      final String compilerVersion) {
    return new ModuleCacheKeyImpl(
        wasmHash, engineConfig, compilationSettings, targetPlatform, compilerVersion);
  }

  /**
   * Creates a ModuleCacheKey from the given parameters with current timestamp.
   *
   * @param wasmHash the hash of the WebAssembly bytecode
   * @param engineConfig the engine configuration
   * @param compilationSettings the compilation settings
   * @return a new ModuleCacheKey instance with current timestamp and platform
   * @throws IllegalArgumentException if any required parameter is null
   */
  static ModuleCacheKey create(
      final byte[] wasmHash,
      final EngineConfig engineConfig,
      final CompilationSettings compilationSettings) {
    return create(
        wasmHash,
        engineConfig,
        compilationSettings,
        System.getProperty("os.name") + "-" + System.getProperty("os.arch"),
        "wasmtime4j-1.0.0");
  }

  /**
   * Implementation of ModuleCacheKey.
   */
  final class ModuleCacheKeyImpl implements ModuleCacheKey {
    private final byte[] wasmHash;
    private final EngineConfig engineConfig;
    private final CompilationSettings compilationSettings;
    private final long timestamp;
    private final String targetPlatform;
    private final String compilerVersion;
    private final String stringRepresentation;
    private final String shortHash;

    ModuleCacheKeyImpl(
        final byte[] wasmHash,
        final EngineConfig engineConfig,
        final CompilationSettings compilationSettings,
        final String targetPlatform,
        final String compilerVersion) {
      this.wasmHash = Objects.requireNonNull(wasmHash, "WebAssembly hash cannot be null").clone();
      this.engineConfig = Objects.requireNonNull(engineConfig, "Engine config cannot be null");
      this.compilationSettings =
          Objects.requireNonNull(compilationSettings, "Compilation settings cannot be null");
      this.targetPlatform =
          Objects.requireNonNull(targetPlatform, "Target platform cannot be null");
      this.compilerVersion =
          Objects.requireNonNull(compilerVersion, "Compiler version cannot be null");
      this.timestamp = System.currentTimeMillis();

      // Generate string representation and short hash
      this.stringRepresentation = generateStringRepresentation();
      this.shortHash = generateShortHash();
    }

    @Override
    public byte[] getWasmHash() {
      return wasmHash.clone();
    }

    @Override
    public EngineConfig getEngineConfig() {
      return engineConfig;
    }

    @Override
    public CompilationSettings getCompilationSettings() {
      return compilationSettings;
    }

    @Override
    public long getTimestamp() {
      return timestamp;
    }

    @Override
    public String getTargetPlatform() {
      return targetPlatform;
    }

    @Override
    public String getCompilerVersion() {
      return compilerVersion;
    }

    @Override
    public String toStringRepresentation() {
      return stringRepresentation;
    }

    @Override
    public String getShortHash() {
      return shortHash;
    }

    @Override
    public boolean isCompatibleWith(final ModuleCacheKey other) {
      if (other == null || !(other instanceof ModuleCacheKeyImpl)) {
        return false;
      }

      final ModuleCacheKeyImpl otherImpl = (ModuleCacheKeyImpl) other;
      return java.util.Arrays.equals(wasmHash, otherImpl.wasmHash)
          && Objects.equals(engineConfig, otherImpl.engineConfig)
          && Objects.equals(compilationSettings, otherImpl.compilationSettings)
          && Objects.equals(targetPlatform, otherImpl.targetPlatform)
          && Objects.equals(compilerVersion, otherImpl.compilerVersion);
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }

      final ModuleCacheKeyImpl other = (ModuleCacheKeyImpl) obj;
      return isCompatibleWith(other);
    }

    @Override
    public int hashCode() {
      return Objects.hash(
          java.util.Arrays.hashCode(wasmHash),
          engineConfig,
          compilationSettings,
          targetPlatform,
          compilerVersion);
    }

    @Override
    public String toString() {
      return "ModuleCacheKey{"
          + "shortHash='"
          + shortHash
          + "', platform='"
          + targetPlatform
          + "', version='"
          + compilerVersion
          + "'}";
    }

    private String generateStringRepresentation() {
      final StringBuilder sb = new StringBuilder();
      sb.append(bytesToHex(wasmHash, 16)); // First 16 bytes of hash
      sb.append('_');
      sb.append(targetPlatform.replace('-', '_'));
      sb.append('_');
      sb.append(compilerVersion.replace('.', '_').replace('-', '_'));
      sb.append('_');
      sb.append(Integer.toHexString(engineConfig.hashCode()));
      sb.append('_');
      sb.append(Integer.toHexString(compilationSettings.hashCode()));
      return sb.toString();
    }

    private String generateShortHash() {
      final int combinedHash =
          Objects.hash(
              java.util.Arrays.hashCode(wasmHash),
              engineConfig,
              compilationSettings,
              targetPlatform,
              compilerVersion);
      return Integer.toHexString(Math.abs(combinedHash)).substring(0, 8);
    }

    private static String bytesToHex(final byte[] bytes, final int maxLength) {
      final StringBuilder sb = new StringBuilder();
      final int length = Math.min(bytes.length, maxLength);
      for (int i = 0; i < length; i++) {
        sb.append(String.format("%02x", bytes[i]));
      }
      return sb.toString();
    }
  }
}