package ai.tegmentum.wasmtime4j.serialization.impl;

import ai.tegmentum.wasmtime4j.serialization.CompressionType;
import ai.tegmentum.wasmtime4j.serialization.ModuleMetadata;
import ai.tegmentum.wasmtime4j.serialization.OptimizationLevel;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of ModuleMetadata.
 *
 * <p>This implementation provides comprehensive metadata for serialized modules including version
 * information, compilation settings, and compatibility data.
 *
 * @since 1.0.0
 */
public final class ModuleMetadataImpl implements ModuleMetadata {

  private final String formatVersion;
  private final String wasmtimeVersion;
  private final TargetPlatform targetPlatform;
  private final Instant compilationTimestamp;
  private final Set<OptimizationLevel> optimizationLevels;
  private final CompressionType compressionType;
  private final boolean hasDebugInfo;
  private final boolean hasSourceMap;
  private final String originalModuleHash;
  private final long dataSize;
  private final long originalSize;
  private final Map<String, String> customProperties;
  private final String checksum;

  /**
   * Creates a new ModuleMetadataImpl.
   *
   * @param formatVersion the serialization format version
   * @param wasmtimeVersion the Wasmtime version used for compilation
   * @param targetPlatform the target platform
   * @param compilationTimestamp the compilation timestamp
   * @param optimizationLevels the optimization levels applied
   * @param compressionType the compression type used
   * @param hasDebugInfo whether debug information is included
   * @param hasSourceMap whether source map information is included
   * @param originalModuleHash the hash of the original WebAssembly module
   * @param dataSize the size of the serialized data
   * @param originalSize the size of the original module
   * @param customProperties custom metadata properties
   * @param checksum the checksum of the serialized data
   */
  public ModuleMetadataImpl(
      final String formatVersion,
      final String wasmtimeVersion,
      final TargetPlatform targetPlatform,
      final Instant compilationTimestamp,
      final Set<OptimizationLevel> optimizationLevels,
      final CompressionType compressionType,
      final boolean hasDebugInfo,
      final boolean hasSourceMap,
      final String originalModuleHash,
      final long dataSize,
      final long originalSize,
      final Map<String, String> customProperties,
      final String checksum) {
    this.formatVersion = Objects.requireNonNull(formatVersion, "Format version cannot be null");
    this.wasmtimeVersion =
        Objects.requireNonNull(wasmtimeVersion, "Wasmtime version cannot be null");
    this.targetPlatform = Objects.requireNonNull(targetPlatform, "Target platform cannot be null");
    this.compilationTimestamp =
        Objects.requireNonNull(compilationTimestamp, "Compilation timestamp cannot be null");
    this.optimizationLevels =
        Set.copyOf(Objects.requireNonNull(optimizationLevels, "Optimization levels cannot be null"));
    this.compressionType =
        Objects.requireNonNull(compressionType, "Compression type cannot be null");
    this.hasDebugInfo = hasDebugInfo;
    this.hasSourceMap = hasSourceMap;
    this.originalModuleHash =
        Objects.requireNonNull(originalModuleHash, "Original module hash cannot be null");
    this.dataSize = dataSize;
    this.originalSize = originalSize;
    this.customProperties =
        Map.copyOf(
            Objects.requireNonNullElse(customProperties, Map.of()));
    this.checksum = checksum; // Can be null if no checksum was calculated
  }

  @Override
  public String getFormatVersion() {
    return formatVersion;
  }

  @Override
  public String getWasmtimeVersion() {
    return wasmtimeVersion;
  }

  @Override
  public TargetPlatform getTargetPlatform() {
    return targetPlatform;
  }

  @Override
  public Instant getCompilationTimestamp() {
    return compilationTimestamp;
  }

  @Override
  public Set<OptimizationLevel> getOptimizationLevels() {
    return optimizationLevels;
  }

  @Override
  public CompressionType getCompressionType() {
    return compressionType;
  }

  @Override
  public boolean hasDebugInfo() {
    return hasDebugInfo;
  }

  @Override
  public boolean hasSourceMap() {
    return hasSourceMap;
  }

  @Override
  public String getOriginalModuleHash() {
    return originalModuleHash;
  }

  @Override
  public long getDataSize() {
    return dataSize;
  }

  @Override
  public long getOriginalSize() {
    return originalSize;
  }

  @Override
  public double getCompressionRatio() {
    if (dataSize <= 0) {
      return 1.0;
    }
    return (double) originalSize / (double) dataSize;
  }

  @Override
  public Map<String, String> getCustomProperties() {
    return customProperties;
  }

  @Override
  public String getChecksum() {
    return checksum;
  }

  @Override
  public boolean isCompatibleWith(final TargetPlatform platform) {
    if (platform == null) {
      throw new IllegalArgumentException("Platform cannot be null");
    }
    return targetPlatform.isCompatibleWith(platform);
  }

  @Override
  public boolean isCompatibleWith(final String wasmtimeVersion) {
    if (wasmtimeVersion == null) {
      throw new IllegalArgumentException("Wasmtime version cannot be null");
    }

    // For now, we do exact version matching
    // In a more sophisticated implementation, we might support version ranges
    return this.wasmtimeVersion.equals(wasmtimeVersion);
  }

  @Override
  public String toSummaryString() {
    return String.format(
        "ModuleMetadata{format=%s, wasmtime=%s, platform=%s, optimizations=%s, "
            + "compression=%s, debugInfo=%s, sourceMap=%s, dataSize=%d, originalSize=%d, "
            + "compressionRatio=%.2f, timestamp=%s}",
        formatVersion,
        wasmtimeVersion,
        targetPlatform,
        optimizationLevels,
        compressionType,
        hasDebugInfo,
        hasSourceMap,
        dataSize,
        originalSize,
        getCompressionRatio(),
        compilationTimestamp);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ModuleMetadataImpl other = (ModuleMetadataImpl) obj;
    return hasDebugInfo == other.hasDebugInfo
        && hasSourceMap == other.hasSourceMap
        && dataSize == other.dataSize
        && originalSize == other.originalSize
        && Objects.equals(formatVersion, other.formatVersion)
        && Objects.equals(wasmtimeVersion, other.wasmtimeVersion)
        && Objects.equals(targetPlatform, other.targetPlatform)
        && Objects.equals(compilationTimestamp, other.compilationTimestamp)
        && Objects.equals(optimizationLevels, other.optimizationLevels)
        && Objects.equals(compressionType, other.compressionType)
        && Objects.equals(originalModuleHash, other.originalModuleHash)
        && Objects.equals(customProperties, other.customProperties)
        && Objects.equals(checksum, other.checksum);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        formatVersion,
        wasmtimeVersion,
        targetPlatform,
        compilationTimestamp,
        optimizationLevels,
        compressionType,
        hasDebugInfo,
        hasSourceMap,
        originalModuleHash,
        dataSize,
        originalSize,
        customProperties,
        checksum);
  }

  @Override
  public String toString() {
    return toSummaryString();
  }

  /**
   * Builder for creating ModuleMetadataImpl instances.
   */
  public static final class Builder {
    private String formatVersion = "1.0";
    private String wasmtimeVersion = "unknown";
    private TargetPlatform targetPlatform = TargetPlatform.current();
    private Instant compilationTimestamp = Instant.now();
    private Set<OptimizationLevel> optimizationLevels = Set.of(OptimizationLevel.BASIC);
    private CompressionType compressionType = CompressionType.NONE;
    private boolean hasDebugInfo = false;
    private boolean hasSourceMap = false;
    private String originalModuleHash = "";
    private long dataSize = 0;
    private long originalSize = 0;
    private Map<String, String> customProperties = new HashMap<>();
    private String checksum = null;

    public Builder formatVersion(final String formatVersion) {
      this.formatVersion = formatVersion;
      return this;
    }

    public Builder wasmtimeVersion(final String wasmtimeVersion) {
      this.wasmtimeVersion = wasmtimeVersion;
      return this;
    }

    public Builder targetPlatform(final TargetPlatform targetPlatform) {
      this.targetPlatform = targetPlatform;
      return this;
    }

    public Builder compilationTimestamp(final Instant compilationTimestamp) {
      this.compilationTimestamp = compilationTimestamp;
      return this;
    }

    public Builder optimizationLevels(final Set<OptimizationLevel> optimizationLevels) {
      this.optimizationLevels = optimizationLevels;
      return this;
    }

    public Builder compressionType(final CompressionType compressionType) {
      this.compressionType = compressionType;
      return this;
    }

    public Builder hasDebugInfo(final boolean hasDebugInfo) {
      this.hasDebugInfo = hasDebugInfo;
      return this;
    }

    public Builder hasSourceMap(final boolean hasSourceMap) {
      this.hasSourceMap = hasSourceMap;
      return this;
    }

    public Builder originalModuleHash(final String originalModuleHash) {
      this.originalModuleHash = originalModuleHash;
      return this;
    }

    public Builder dataSize(final long dataSize) {
      this.dataSize = dataSize;
      return this;
    }

    public Builder originalSize(final long originalSize) {
      this.originalSize = originalSize;
      return this;
    }

    public Builder customProperty(final String key, final String value) {
      this.customProperties.put(key, value);
      return this;
    }

    public Builder customProperties(final Map<String, String> customProperties) {
      this.customProperties = new HashMap<>(customProperties);
      return this;
    }

    public Builder checksum(final String checksum) {
      this.checksum = checksum;
      return this;
    }

    public ModuleMetadataImpl build() {
      return new ModuleMetadataImpl(
          formatVersion,
          wasmtimeVersion,
          targetPlatform,
          compilationTimestamp,
          optimizationLevels,
          compressionType,
          hasDebugInfo,
          hasSourceMap,
          originalModuleHash,
          dataSize,
          originalSize,
          customProperties,
          checksum);
    }
  }

  /**
   * Creates a new builder for ModuleMetadataImpl.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }
}