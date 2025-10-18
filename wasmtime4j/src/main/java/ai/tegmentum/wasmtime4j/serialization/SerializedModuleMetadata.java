/*
 * Copyright 2024 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.serialization;

import java.security.MessageDigest;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata associated with serialized WebAssembly modules.
 *
 * <p>This class contains comprehensive metadata about serialized modules including format
 * information, integrity data, performance metrics, and compatibility information. It enables
 * efficient cache validation, version checking, and deserialization optimization.
 *
 * @since 1.0.0
 */
public final class SerializedModuleMetadata {

  // Core metadata
  private final String formatVersion;
  private final ModuleSerializationFormat format;
  private final Instant serializationTimestamp;
  private final long serializedSize;
  private final long originalSize;

  // Integrity and security
  private final String sha256Hash;
  private final String sha1Hash; // For backward compatibility
  private final boolean isEncrypted;
  private final String encryptionAlgorithm;

  // Module characteristics
  private final String moduleName;
  private final String moduleVersion;
  private final int importCount;
  private final int exportCount;
  private final int functionCount;
  private final int globalCount;
  private final int memoryCount;
  private final int tableCount;

  // Performance metrics
  private final long serializationDurationMs;
  private final double compressionRatio;
  private final SerializationPerformanceMetrics performanceMetrics;

  // Compatibility information
  private final String wasmtimeVersion;
  private final String javaVersion;
  private final String platformArch;
  private final String platformOs;

  // Custom metadata
  private final Map<String, String> customMetadata;

  // Debug information preservation
  private final boolean hasSourceMaps;
  private final boolean hasDebugSymbols;
  private final boolean hasNameSection;

  /**
   * Creates comprehensive serialized module metadata.
   *
   * @param builder the metadata builder
   */
  private SerializedModuleMetadata(final Builder builder) {
    this.formatVersion = builder.formatVersion;
    this.format = builder.format;
    this.serializationTimestamp = builder.serializationTimestamp;
    this.serializedSize = builder.serializedSize;
    this.originalSize = builder.originalSize;
    this.sha256Hash = builder.sha256Hash;
    this.sha1Hash = builder.sha1Hash;
    this.isEncrypted = builder.isEncrypted;
    this.encryptionAlgorithm = builder.encryptionAlgorithm;
    this.moduleName = builder.moduleName;
    this.moduleVersion = builder.moduleVersion;
    this.importCount = builder.importCount;
    this.exportCount = builder.exportCount;
    this.functionCount = builder.functionCount;
    this.globalCount = builder.globalCount;
    this.memoryCount = builder.memoryCount;
    this.tableCount = builder.tableCount;
    this.serializationDurationMs = builder.serializationDurationMs;
    this.compressionRatio = builder.compressionRatio;
    this.performanceMetrics = builder.performanceMetrics;
    this.wasmtimeVersion = builder.wasmtimeVersion;
    this.javaVersion = builder.javaVersion;
    this.platformArch = builder.platformArch;
    this.platformOs = builder.platformOs;
    this.customMetadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.customMetadata));
    this.hasSourceMaps = builder.hasSourceMaps;
    this.hasDebugSymbols = builder.hasDebugSymbols;
    this.hasNameSection = builder.hasNameSection;
  }

  // Getters for all fields

  public String getFormatVersion() {
    return formatVersion;
  }

  public ModuleSerializationFormat getFormat() {
    return format;
  }

  public Instant getSerializationTimestamp() {
    return serializationTimestamp;
  }

  public long getSerializedSize() {
    return serializedSize;
  }

  public long getOriginalSize() {
    return originalSize;
  }

  public String getSha256Hash() {
    return sha256Hash;
  }

  public String getSha1Hash() {
    return sha1Hash;
  }

  public boolean isEncrypted() {
    return isEncrypted;
  }

  public String getEncryptionAlgorithm() {
    return encryptionAlgorithm;
  }

  public String getModuleName() {
    return moduleName;
  }

  public String getModuleVersion() {
    return moduleVersion;
  }

  public int getImportCount() {
    return importCount;
  }

  public int getExportCount() {
    return exportCount;
  }

  public int getFunctionCount() {
    return functionCount;
  }

  public int getGlobalCount() {
    return globalCount;
  }

  public int getMemoryCount() {
    return memoryCount;
  }

  public int getTableCount() {
    return tableCount;
  }

  public long getSerializationDurationMs() {
    return serializationDurationMs;
  }

  public double getCompressionRatio() {
    return compressionRatio;
  }

  public SerializationPerformanceMetrics getPerformanceMetrics() {
    return performanceMetrics;
  }

  public String getWasmtimeVersion() {
    return wasmtimeVersion;
  }

  public String getJavaVersion() {
    return javaVersion;
  }

  public String getPlatformArch() {
    return platformArch;
  }

  public String getPlatformOs() {
    return platformOs;
  }

  public Map<String, String> getCustomMetadata() {
    return customMetadata;
  }

  public boolean hasSourceMaps() {
    return hasSourceMaps;
  }

  public boolean hasDebugSymbols() {
    return hasDebugSymbols;
  }

  public boolean hasNameSection() {
    return hasNameSection;
  }

  /**
   * Validates the integrity of serialized data against this metadata.
   *
   * @param serializedData the serialized module data
   * @return true if integrity check passes
   * @throws IllegalArgumentException if serializedData is null
   */
  public boolean validateIntegrity(final byte[] serializedData) {
    Objects.requireNonNull(serializedData, "Serialized data cannot be null");

    try {
      // Validate size
      if (serializedData.length != serializedSize) {
        return false;
      }

      // Validate SHA-256 hash
      final MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
      final byte[] computedHash = sha256.digest(serializedData);
      final String computedHashHex = bytesToHex(computedHash);

      return computedHashHex.equals(sha256Hash);
    } catch (Exception e) {
      return false;
    }
  }

  /**
   * Checks if this metadata is compatible with the current runtime environment.
   *
   * @return true if compatible
   */
  public boolean isCompatibleWithCurrentEnvironment() {
    // Check Java version compatibility
    final String currentJavaVersion = System.getProperty("java.version");
    if (!isJavaVersionCompatible(currentJavaVersion, javaVersion)) {
      return false;
    }

    // Check platform architecture
    final String currentArch = System.getProperty("os.arch");
    if (!isPlatformArchCompatible(currentArch, platformArch)) {
      return false;
    }

    // Check operating system
    final String currentOs = System.getProperty("os.name");
    if (!isPlatformOsCompatible(currentOs, platformOs)) {
      return false;
    }

    return true;
  }

  /**
   * Gets the estimated deserialization time based on performance metrics.
   *
   * @return estimated deserialization time in milliseconds
   */
  public long getEstimatedDeserializationTimeMs() {
    if (performanceMetrics != null) {
      // Use historical performance data to estimate
      final double deserializationRatio = performanceMetrics.getDeserializationSpeedRatio();
      return Math.round(serializationDurationMs * deserializationRatio);
    }

    // Fallback estimation based on size and format
    final double sizeFactorMs = serializedSize / 1024.0; // 1ms per KB baseline
    final double formatFactor = format.supportsCompression() ? 1.5 : 1.0;
    return Math.round(sizeFactorMs * formatFactor);
  }

  /**
   * Converts byte array to hexadecimal string.
   *
   * @param bytes the byte array
   * @return hexadecimal representation
   */
  private String bytesToHex(final byte[] bytes) {
    final StringBuilder result = new StringBuilder();
    for (final byte b : bytes) {
      result.append(String.format("%02x", b));
    }
    return result.toString();
  }

  /**
   * Checks Java version compatibility.
   *
   * @param current the current Java version
   * @param serialized the Java version used for serialization
   * @return true if compatible
   */
  private boolean isJavaVersionCompatible(final String current, final String serialized) {
    // Simplified version check - in practice would need more sophisticated logic
    return current.equals(serialized) || current.startsWith(extractMajorVersion(serialized));
  }

  /**
   * Checks platform architecture compatibility.
   *
   * @param current the current architecture
   * @param serialized the architecture used for serialization
   * @return true if compatible
   */
  private boolean isPlatformArchCompatible(final String current, final String serialized) {
    // Architecture compatibility matrix
    return current.equals(serialized)
        || (current.contains("x86_64") && serialized.contains("amd64"))
        || (current.contains("amd64") && serialized.contains("x86_64"));
  }

  /**
   * Checks platform OS compatibility.
   *
   * @param current the current OS
   * @param serialized the OS used for serialization
   * @return true if compatible
   */
  private boolean isPlatformOsCompatible(final String current, final String serialized) {
    // OS compatibility check
    final String currentLower = current.toLowerCase(Locale.ROOT);
    final String serializedLower = serialized.toLowerCase(Locale.ROOT);

    return currentLower.equals(serializedLower)
        || (currentLower.contains("windows") && serializedLower.contains("windows"))
        || (currentLower.contains("linux") && serializedLower.contains("linux"))
        || (currentLower.contains("mac") && serializedLower.contains("mac"));
  }

  /**
   * Extracts major version from version string.
   *
   * @param version the full version string
   * @return the major version
   */
  private String extractMajorVersion(final String version) {
    if (version == null) {
      return "";
    }
    final int dotIndex = version.indexOf('.');
    return dotIndex > 0 ? version.substring(0, dotIndex) : version;
  }

  @Override
  public String toString() {
    return String.format(
        "SerializedModuleMetadata{format=%s, size=%d bytes, compression=%.2fx, "
            + "imports=%d, exports=%d, timestamp=%s}",
        format.getIdentifier(),
        serializedSize,
        compressionRatio,
        importCount,
        exportCount,
        serializationTimestamp);
  }

  /** Builder for creating SerializedModuleMetadata instances. */
  public static final class Builder {
    // Required fields
    private String formatVersion = "1.0";
    private ModuleSerializationFormat format = ModuleSerializationFormat.COMPACT_BINARY_LZ4;
    private Instant serializationTimestamp = Instant.now();
    private long serializedSize;
    private long originalSize;
    private String sha256Hash = "";

    // Optional fields with defaults
    private String sha1Hash = "";
    private boolean isEncrypted = false;
    private String encryptionAlgorithm = null;
    private String moduleName = null;
    private String moduleVersion = null;
    private int importCount = 0;
    private int exportCount = 0;
    private int functionCount = 0;
    private int globalCount = 0;
    private int memoryCount = 0;
    private int tableCount = 0;
    private long serializationDurationMs = 0;
    private double compressionRatio = 1.0;
    private SerializationPerformanceMetrics performanceMetrics = null;
    private String wasmtimeVersion = "36.0.2";
    private String javaVersion = System.getProperty("java.version");
    private String platformArch = System.getProperty("os.arch");
    private String platformOs = System.getProperty("os.name");
    private Map<String, String> customMetadata = new LinkedHashMap<>();
    private boolean hasSourceMaps = false;
    private boolean hasDebugSymbols = false;
    private boolean hasNameSection = false;

    public Builder setFormatVersion(final String formatVersion) {
      this.formatVersion = Objects.requireNonNull(formatVersion, "Format version cannot be null");
      return this;
    }

    public Builder setFormat(final ModuleSerializationFormat format) {
      this.format = Objects.requireNonNull(format, "Format cannot be null");
      return this;
    }

    public Builder setSerializationTimestamp(final Instant timestamp) {
      this.serializationTimestamp = Objects.requireNonNull(timestamp, "Timestamp cannot be null");
      return this;
    }

    public Builder setSerializedSize(final long size) {
      this.serializedSize = requireNonNegative(size, "Serialized size cannot be negative");
      return this;
    }

    public Builder setOriginalSize(final long size) {
      this.originalSize = requireNonNegative(size, "Original size cannot be negative");
      return this;
    }

    public Builder setSha256Hash(final String hash) {
      this.sha256Hash = Objects.requireNonNull(hash, "SHA-256 hash cannot be null");
      return this;
    }

    public Builder setSha1Hash(final String hash) {
      this.sha1Hash = hash;
      return this;
    }

    /**
     * Sets encryption information.
     *
     * @param encrypted whether the module is encrypted
     * @param algorithm encryption algorithm used
     * @return this builder
     */
    public Builder setEncrypted(final boolean encrypted, final String algorithm) {
      this.isEncrypted = encrypted;
      this.encryptionAlgorithm = algorithm;
      return this;
    }

    /**
     * Sets the module name.
     *
     * @param name the module name
     * @return this builder
     */
    public Builder setModuleName(final String name) {
      this.moduleName = name;
      return this;
    }

    /**
     * Sets the module version.
     *
     * @param version the module version
     * @return this builder
     */
    public Builder setModuleVersion(final String version) {
      this.moduleVersion = version;
      return this;
    }

    /**
     * Sets entity counts for the module.
     *
     * @param imports number of imports
     * @param exports number of exports
     * @param functions number of functions
     * @param globals number of globals
     * @param memories number of memories
     * @param tables number of tables
     * @return this builder
     */
    public Builder setCounts(
        final int imports,
        final int exports,
        final int functions,
        final int globals,
        final int memories,
        final int tables) {
      this.importCount = requireNonNegative(imports, "Import count cannot be negative");
      this.exportCount = requireNonNegative(exports, "Export count cannot be negative");
      this.functionCount = requireNonNegative(functions, "Function count cannot be negative");
      this.globalCount = requireNonNegative(globals, "Global count cannot be negative");
      this.memoryCount = requireNonNegative(memories, "Memory count cannot be negative");
      this.tableCount = requireNonNegative(tables, "Table count cannot be negative");
      return this;
    }

    public Builder setSerializationDuration(final long durationMs) {
      this.serializationDurationMs = requireNonNegative(durationMs, "Duration cannot be negative");
      return this;
    }

    /**
     * Sets the compression ratio achieved.
     *
     * @param ratio compression ratio (must be positive)
     * @return this builder
     */
    public Builder setCompressionRatio(final double ratio) {
      this.compressionRatio = requirePositive(ratio, "Compression ratio must be positive");
      return this;
    }

    /**
     * Sets performance metrics for serialization.
     *
     * @param metrics serialization performance metrics
     * @return this builder
     */
    public Builder setPerformanceMetrics(final SerializationPerformanceMetrics metrics) {
      this.performanceMetrics = metrics;
      return this;
    }

    /**
     * Sets the Wasmtime version used.
     *
     * @param version Wasmtime version
     * @return this builder
     */
    public Builder setWasmtimeVersion(final String version) {
      this.wasmtimeVersion = version;
      return this;
    }

    /**
     * Sets the Java version used.
     *
     * @param version Java version
     * @return this builder
     */
    public Builder setJavaVersion(final String version) {
      this.javaVersion = version;
      return this;
    }

    /**
     * Sets platform information.
     *
     * @param arch platform architecture
     * @param os operating system
     * @return this builder
     */
    public Builder setPlatformInfo(final String arch, final String os) {
      this.platformArch = arch;
      this.platformOs = os;
      return this;
    }

    /**
     * Adds custom metadata to the serialized module.
     *
     * @param key metadata key
     * @param value metadata value
     * @return this builder
     */
    public Builder addCustomMetadata(final String key, final String value) {
      Objects.requireNonNull(key, "Metadata key cannot be null");
      this.customMetadata.put(key, value);
      return this;
    }

    /**
     * Sets custom metadata, replacing any existing metadata.
     *
     * @param metadata metadata map to set
     * @return this builder
     */
    public Builder setCustomMetadata(final Map<String, String> metadata) {
      this.customMetadata.clear();
      if (metadata != null) {
        this.customMetadata.putAll(metadata);
      }
      return this;
    }

    /**
     * Sets debug information flags.
     *
     * @param sourceMaps whether source maps are present
     * @param debugSymbols whether debug symbols are present
     * @param nameSection whether name section is present
     * @return this builder
     */
    public Builder setDebugInfo(
        final boolean sourceMaps, final boolean debugSymbols, final boolean nameSection) {
      this.hasSourceMaps = sourceMaps;
      this.hasDebugSymbols = debugSymbols;
      this.hasNameSection = nameSection;
      return this;
    }

    /**
     * Builds the serialized module metadata.
     *
     * @return configured serialized module metadata
     */
    public SerializedModuleMetadata build() {
      // Validation
      if (originalSize > 0 && serializedSize > 0) {
        compressionRatio = (double) originalSize / serializedSize;
      }

      return new SerializedModuleMetadata(this);
    }

    private static long requireNonNegative(final long value, final String message) {
      if (value < 0) {
        throw new IllegalArgumentException(message);
      }
      return value;
    }

    private static int requireNonNegative(final int value, final String message) {
      if (value < 0) {
        throw new IllegalArgumentException(message);
      }
      return value;
    }

    private static double requirePositive(final double value, final String message) {
      if (value <= 0) {
        throw new IllegalArgumentException(message);
      }
      return value;
    }
  }
}
