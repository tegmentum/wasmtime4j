package ai.tegmentum.wasmtime4j.serialization.impl;

import ai.tegmentum.wasmtime4j.serialization.CompressionType;
import ai.tegmentum.wasmtime4j.serialization.OptimizationLevel;
import ai.tegmentum.wasmtime4j.serialization.SerializationOptions;
import ai.tegmentum.wasmtime4j.serialization.TargetPlatform;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

/**
 * Default implementation of SerializationOptions.
 *
 * <p>This implementation provides immutable serialization configuration with validation and
 * consistent behavior across all use cases.
 *
 * @since 1.0.0
 */
public final class SerializationOptionsImpl implements SerializationOptions {

  private final CompressionType compression;
  private final boolean includeDebugInfo;
  private final boolean includeSourceMap;
  private final Set<OptimizationLevel> optimizations;
  private final TargetPlatform targetPlatform;
  private final boolean strictValidation;
  private final long maxSize;
  private final boolean includeChecksum;

  /**
   * Creates a new SerializationOptionsImpl.
   *
   * @param compression the compression type
   * @param includeDebugInfo whether to include debug information
   * @param includeSourceMap whether to include source map information
   * @param optimizations the optimization levels to apply
   * @param targetPlatform the target platform (null for current platform)
   * @param strictValidation whether to enable strict validation
   * @param maxSize the maximum size limit (-1 for no limit)
   * @param includeChecksum whether to include checksums
   * @throws IllegalArgumentException if any required parameter is null or invalid
   */
  public SerializationOptionsImpl(
      final CompressionType compression,
      final boolean includeDebugInfo,
      final boolean includeSourceMap,
      final Set<OptimizationLevel> optimizations,
      final TargetPlatform targetPlatform,
      final boolean strictValidation,
      final long maxSize,
      final boolean includeChecksum) {

    this.compression = Objects.requireNonNull(compression, "Compression type cannot be null");
    this.includeDebugInfo = includeDebugInfo;
    this.includeSourceMap = includeSourceMap;
    this.optimizations = Collections.unmodifiableSet(
        Objects.requireNonNull(optimizations, "Optimizations set cannot be null"));
    this.targetPlatform = targetPlatform; // null is allowed for current platform
    this.strictValidation = strictValidation;

    if (maxSize < -1) {
      throw new IllegalArgumentException("Max size must be -1 (no limit) or positive: " + maxSize);
    }
    this.maxSize = maxSize;
    this.includeChecksum = includeChecksum;

    // Validate optimization levels
    if (optimizations.isEmpty()) {
      throw new IllegalArgumentException("At least one optimization level must be specified");
    }
  }

  @Override
  public CompressionType getCompression() {
    return compression;
  }

  @Override
  public boolean isIncludeDebugInfo() {
    return includeDebugInfo;
  }

  @Override
  public boolean isIncludeSourceMap() {
    return includeSourceMap;
  }

  @Override
  public Set<OptimizationLevel> getOptimizations() {
    return optimizations;
  }

  @Override
  public TargetPlatform getTargetPlatform() {
    return targetPlatform;
  }

  @Override
  public boolean isStrictValidation() {
    return strictValidation;
  }

  @Override
  public long getMaxSize() {
    return maxSize;
  }

  @Override
  public boolean isIncludeChecksum() {
    return includeChecksum;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SerializationOptionsImpl other = (SerializationOptionsImpl) obj;
    return includeDebugInfo == other.includeDebugInfo
        && includeSourceMap == other.includeSourceMap
        && strictValidation == other.strictValidation
        && maxSize == other.maxSize
        && includeChecksum == other.includeChecksum
        && compression == other.compression
        && Objects.equals(optimizations, other.optimizations)
        && Objects.equals(targetPlatform, other.targetPlatform);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        compression,
        includeDebugInfo,
        includeSourceMap,
        optimizations,
        targetPlatform,
        strictValidation,
        maxSize,
        includeChecksum);
  }

  @Override
  public String toString() {
    return "SerializationOptions{"
        + "compression=" + compression
        + ", includeDebugInfo=" + includeDebugInfo
        + ", includeSourceMap=" + includeSourceMap
        + ", optimizations=" + optimizations
        + ", targetPlatform=" + targetPlatform
        + ", strictValidation=" + strictValidation
        + ", maxSize=" + maxSize
        + ", includeChecksum=" + includeChecksum
        + '}';
  }
}