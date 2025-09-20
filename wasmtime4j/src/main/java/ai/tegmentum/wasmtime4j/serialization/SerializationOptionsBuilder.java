package ai.tegmentum.wasmtime4j.serialization;

import java.util.HashSet;
import java.util.Set;

/**
 * Builder for constructing SerializationOptions instances.
 *
 * <p>This builder provides a fluent interface for configuring serialization options with
 * validation and sensible defaults.
 *
 * @since 1.0.0
 */
public final class SerializationOptionsBuilder {

  private CompressionType compression = CompressionType.NONE;
  private boolean includeDebugInfo = false;
  private boolean includeSourceMap = false;
  private Set<OptimizationLevel> optimizations = new HashSet<>();
  private TargetPlatform targetPlatform = null; // null means current platform
  private boolean strictValidation = false;
  private long maxSize = -1; // -1 means no limit
  private boolean includeChecksum = true;

  SerializationOptionsBuilder() {
    // Package-private constructor
    optimizations.add(OptimizationLevel.BASIC);
  }

  /**
   * Sets the compression type for serialization.
   *
   * @param compression the compression type to use
   * @return this builder for method chaining
   * @throws IllegalArgumentException if compression is null
   */
  public SerializationOptionsBuilder compression(final CompressionType compression) {
    if (compression == null) {
      throw new IllegalArgumentException("Compression type cannot be null");
    }
    this.compression = compression;
    return this;
  }

  /**
   * Enables or disables inclusion of debug information.
   *
   * @param includeDebugInfo true to include debug information, false otherwise
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder includeDebugInfo(final boolean includeDebugInfo) {
    this.includeDebugInfo = includeDebugInfo;
    return this;
  }

  /**
   * Enables inclusion of debug information.
   *
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder includeDebugInfo() {
    return includeDebugInfo(true);
  }

  /**
   * Enables or disables inclusion of source map information.
   *
   * @param includeSourceMap true to include source map information, false otherwise
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder includeSourceMap(final boolean includeSourceMap) {
    this.includeSourceMap = includeSourceMap;
    return this;
  }

  /**
   * Enables inclusion of source map information.
   *
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder includeSourceMap() {
    return includeSourceMap(true);
  }

  /**
   * Sets the optimization levels to apply.
   *
   * @param optimizations the optimization levels to apply
   * @return this builder for method chaining
   * @throws IllegalArgumentException if optimizations is null
   */
  public SerializationOptionsBuilder optimizations(final Set<OptimizationLevel> optimizations) {
    if (optimizations == null) {
      throw new IllegalArgumentException("Optimizations set cannot be null");
    }
    this.optimizations = new HashSet<>(optimizations);
    return this;
  }

  /**
   * Adds an optimization level to apply.
   *
   * @param optimization the optimization level to add
   * @return this builder for method chaining
   * @throws IllegalArgumentException if optimization is null
   */
  public SerializationOptionsBuilder addOptimization(final OptimizationLevel optimization) {
    if (optimization == null) {
      throw new IllegalArgumentException("Optimization level cannot be null");
    }
    this.optimizations.add(optimization);
    return this;
  }

  /**
   * Removes an optimization level.
   *
   * @param optimization the optimization level to remove
   * @return this builder for method chaining
   * @throws IllegalArgumentException if optimization is null
   */
  public SerializationOptionsBuilder removeOptimization(final OptimizationLevel optimization) {
    if (optimization == null) {
      throw new IllegalArgumentException("Optimization level cannot be null");
    }
    this.optimizations.remove(optimization);
    return this;
  }

  /**
   * Clears all optimization levels.
   *
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder clearOptimizations() {
    this.optimizations.clear();
    return this;
  }

  /**
   * Sets the target platform for serialization.
   *
   * @param targetPlatform the target platform, or null for current platform
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder targetPlatform(final TargetPlatform targetPlatform) {
    this.targetPlatform = targetPlatform;
    return this;
  }

  /**
   * Enables or disables strict validation.
   *
   * @param strictValidation true to enable strict validation, false otherwise
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder strictValidation(final boolean strictValidation) {
    this.strictValidation = strictValidation;
    return this;
  }

  /**
   * Enables strict validation.
   *
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder strictValidation() {
    return strictValidation(true);
  }

  /**
   * Sets the maximum size limit for serialized modules.
   *
   * @param maxSize the maximum size in bytes, or -1 for no limit
   * @return this builder for method chaining
   * @throws IllegalArgumentException if maxSize is negative and not -1
   */
  public SerializationOptionsBuilder maxSize(final long maxSize) {
    if (maxSize < -1) {
      throw new IllegalArgumentException("Max size must be positive or -1 for no limit");
    }
    this.maxSize = maxSize;
    return this;
  }

  /**
   * Enables or disables inclusion of checksums.
   *
   * @param includeChecksum true to include checksums, false otherwise
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder includeChecksum(final boolean includeChecksum) {
    this.includeChecksum = includeChecksum;
    return this;
  }

  /**
   * Disables inclusion of checksums.
   *
   * @return this builder for method chaining
   */
  public SerializationOptionsBuilder noChecksum() {
    return includeChecksum(false);
  }

  /**
   * Builds the SerializationOptions instance.
   *
   * @return a new SerializationOptions instance with the configured settings
   */
  public SerializationOptions build() {
    return new SerializationOptionsImpl(
        compression,
        includeDebugInfo,
        includeSourceMap,
        new HashSet<>(optimizations),
        targetPlatform,
        strictValidation,
        maxSize,
        includeChecksum);
  }

  /**
   * Implementation of SerializationOptions.
   */
  private static final class SerializationOptionsImpl implements SerializationOptions {
    private final CompressionType compression;
    private final boolean includeDebugInfo;
    private final boolean includeSourceMap;
    private final Set<OptimizationLevel> optimizations;
    private final TargetPlatform targetPlatform;
    private final boolean strictValidation;
    private final long maxSize;
    private final boolean includeChecksum;

    SerializationOptionsImpl(
        final CompressionType compression,
        final boolean includeDebugInfo,
        final boolean includeSourceMap,
        final Set<OptimizationLevel> optimizations,
        final TargetPlatform targetPlatform,
        final boolean strictValidation,
        final long maxSize,
        final boolean includeChecksum) {
      this.compression = compression;
      this.includeDebugInfo = includeDebugInfo;
      this.includeSourceMap = includeSourceMap;
      this.optimizations = Set.copyOf(optimizations);
      this.targetPlatform = targetPlatform;
      this.strictValidation = strictValidation;
      this.maxSize = maxSize;
      this.includeChecksum = includeChecksum;
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
    public String toString() {
      return "SerializationOptions{"
          + "compression="
          + compression
          + ", includeDebugInfo="
          + includeDebugInfo
          + ", includeSourceMap="
          + includeSourceMap
          + ", optimizations="
          + optimizations
          + ", targetPlatform="
          + targetPlatform
          + ", strictValidation="
          + strictValidation
          + ", maxSize="
          + maxSize
          + ", includeChecksum="
          + includeChecksum
          + '}';
    }
  }
}