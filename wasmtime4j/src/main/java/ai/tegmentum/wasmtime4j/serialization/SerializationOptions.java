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

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Configuration options for WebAssembly module serialization operations.
 *
 * <p>This class provides fine-grained control over serialization behavior including
 * performance optimizations, security settings, and metadata preservation options.
 *
 * @since 1.0.0
 */
public final class SerializationOptions {

  // Core serialization options
  private final boolean includeChecksum;
  private final boolean preserveDebugInfo;
  private final boolean preserveCustomSections;
  private final boolean preserveNameSection;

  // Performance options
  private final int bufferSize;
  private final boolean useParallelCompression;
  private final int compressionLevel;
  private final boolean enableStreaming;
  private final long streamingThreshold;

  // Security options
  private final boolean encryptSerialization;
  private final String encryptionAlgorithm;
  private final byte[] encryptionKey;
  private final boolean verifyIntegrity;

  // Metadata options
  private final boolean includePerformanceMetrics;
  private final boolean includeCompilerInfo;
  private final boolean includePlatformInfo;
  private final Map<String, String> customMetadata;

  // File system options
  private final boolean cleanupTempFiles;
  private final String tempFilePrefix;
  private final boolean useMemoryMappedFiles;

  /**
   * Creates serialization options with the specified builder.
   *
   * @param builder the options builder
   */
  private SerializationOptions(final Builder builder) {
    this.includeChecksum = builder.includeChecksum;
    this.preserveDebugInfo = builder.preserveDebugInfo;
    this.preserveCustomSections = builder.preserveCustomSections;
    this.preserveNameSection = builder.preserveNameSection;
    this.bufferSize = builder.bufferSize;
    this.useParallelCompression = builder.useParallelCompression;
    this.compressionLevel = builder.compressionLevel;
    this.enableStreaming = builder.enableStreaming;
    this.streamingThreshold = builder.streamingThreshold;
    this.encryptSerialization = builder.encryptSerialization;
    this.encryptionAlgorithm = builder.encryptionAlgorithm;
    this.encryptionKey = builder.encryptionKey != null ? builder.encryptionKey.clone() : null;
    this.verifyIntegrity = builder.verifyIntegrity;
    this.includePerformanceMetrics = builder.includePerformanceMetrics;
    this.includeCompilerInfo = builder.includeCompilerInfo;
    this.includePlatformInfo = builder.includePlatformInfo;
    this.customMetadata = Collections.unmodifiableMap(new LinkedHashMap<>(builder.customMetadata));
    this.cleanupTempFiles = builder.cleanupTempFiles;
    this.tempFilePrefix = builder.tempFilePrefix;
    this.useMemoryMappedFiles = builder.useMemoryMappedFiles;
  }

  // Getter methods

  public boolean isIncludeChecksum() {
    return includeChecksum;
  }

  public boolean isPreserveDebugInfo() {
    return preserveDebugInfo;
  }

  public boolean isPreserveCustomSections() {
    return preserveCustomSections;
  }

  public boolean isPreserveNameSection() {
    return preserveNameSection;
  }

  public int getBufferSize() {
    return bufferSize;
  }

  public boolean isUseParallelCompression() {
    return useParallelCompression;
  }

  public int getCompressionLevel() {
    return compressionLevel;
  }

  public boolean isEnableStreaming() {
    return enableStreaming;
  }

  public long getStreamingThreshold() {
    return streamingThreshold;
  }

  public boolean isEncryptSerialization() {
    return encryptSerialization;
  }

  public String getEncryptionAlgorithm() {
    return encryptionAlgorithm;
  }

  public byte[] getEncryptionKey() {
    return encryptionKey != null ? encryptionKey.clone() : null;
  }

  public boolean isVerifyIntegrity() {
    return verifyIntegrity;
  }

  public boolean isIncludePerformanceMetrics() {
    return includePerformanceMetrics;
  }

  public boolean isIncludeCompilerInfo() {
    return includeCompilerInfo;
  }

  public boolean isIncludePlatformInfo() {
    return includePlatformInfo;
  }

  public Map<String, String> getCustomMetadata() {
    return customMetadata;
  }

  public boolean isCleanupTempFiles() {
    return cleanupTempFiles;
  }

  public String getTempFilePrefix() {
    return tempFilePrefix;
  }

  public boolean isUseMemoryMappedFiles() {
    return useMemoryMappedFiles;
  }

  /**
   * Creates default serialization options suitable for most use cases.
   *
   * @return default serialization options
   */
  public static SerializationOptions createDefault() {
    return new Builder().build();
  }

  /**
   * Creates production-optimized serialization options.
   *
   * @return production serialization options
   */
  public static SerializationOptions createProduction() {
    return new Builder()
        .includeChecksum(true)
        .preserveCustomSections(false) // Optimize for size
        .preserveDebugInfo(false) // Optimize for size
        .setBufferSize(128 * 1024) // 128KB for better I/O performance
        .useParallelCompression(true)
        .setCompressionLevel(6) // Balanced compression
        .enableStreaming(true)
        .setStreamingThreshold(10 * 1024 * 1024) // 10MB threshold
        .verifyIntegrity(true)
        .includePerformanceMetrics(true)
        .includePlatformInfo(true)
        .cleanupTempFiles(true)
        .build();
  }

  /**
   * Creates development-friendly serialization options.
   *
   * @return development serialization options
   */
  public static SerializationOptions createDevelopment() {
    return new Builder()
        .includeChecksum(true)
        .preserveCustomSections(true) // Keep all debug info
        .preserveDebugInfo(true)
        .preserveNameSection(true)
        .setBufferSize(64 * 1024) // Smaller buffer for faster feedback
        .useParallelCompression(false) // Simpler debugging
        .setCompressionLevel(1) // Fast compression
        .enableStreaming(false)
        .verifyIntegrity(true)
        .includePerformanceMetrics(true)
        .includeCompilerInfo(true)
        .includePlatformInfo(true)
        .cleanupTempFiles(false) // Keep files for debugging
        .build();
  }

  /**
   * Creates high-performance serialization options.
   *
   * @return high-performance serialization options
   */
  public static SerializationOptions createHighPerformance() {
    return new Builder()
        .includeChecksum(false) // Skip for maximum speed
        .preserveCustomSections(false)
        .preserveDebugInfo(false)
        .setBufferSize(1024 * 1024) // 1MB buffer for high throughput
        .useParallelCompression(true)
        .setCompressionLevel(1) // Fast compression
        .enableStreaming(true)
        .setStreamingThreshold(1024 * 1024) // 1MB threshold
        .verifyIntegrity(false) // Skip for maximum speed
        .includePerformanceMetrics(false)
        .useMemoryMappedFiles(true)
        .cleanupTempFiles(true)
        .build();
  }

  /**
   * Creates secure serialization options with encryption.
   *
   * @param encryptionKey the encryption key
   * @return secure serialization options
   */
  public static SerializationOptions createSecure(final byte[] encryptionKey) {
    Objects.requireNonNull(encryptionKey, "Encryption key cannot be null");

    return new Builder()
        .includeChecksum(true)
        .setBufferSize(64 * 1024)
        .setCompressionLevel(9) // Maximum compression for security
        .encryptSerialization(true)
        .setEncryptionKey(encryptionKey)
        .verifyIntegrity(true)
        .includePerformanceMetrics(true)
        .cleanupTempFiles(true)
        .build();
  }

  @Override
  public String toString() {
    return String.format(
        "SerializationOptions{checksum=%s, debug=%s, compression=%d, streaming=%s, " +
        "buffer=%dKB, encryption=%s, integrity=%s}",
        includeChecksum,
        preserveDebugInfo,
        compressionLevel,
        enableStreaming,
        bufferSize / 1024,
        encryptSerialization,
        verifyIntegrity);
  }

  /**
   * Builder for creating SerializationOptions instances.
   */
  public static final class Builder {
    // Default values
    private boolean includeChecksum = true;
    private boolean preserveDebugInfo = false;
    private boolean preserveCustomSections = false;
    private boolean preserveNameSection = false;
    private int bufferSize = 64 * 1024; // 64KB
    private boolean useParallelCompression = false;
    private int compressionLevel = 6;
    private boolean enableStreaming = false;
    private long streamingThreshold = 5 * 1024 * 1024; // 5MB
    private boolean encryptSerialization = false;
    private String encryptionAlgorithm = "AES/GCM/NoPadding";
    private byte[] encryptionKey = null;
    private boolean verifyIntegrity = true;
    private boolean includePerformanceMetrics = false;
    private boolean includeCompilerInfo = false;
    private boolean includePlatformInfo = false;
    private Map<String, String> customMetadata = new LinkedHashMap<>();
    private boolean cleanupTempFiles = true;
    private String tempFilePrefix = "wasmtime4j-";
    private boolean useMemoryMappedFiles = false;

    public Builder includeChecksum(final boolean include) {
      this.includeChecksum = include;
      return this;
    }

    public Builder preserveDebugInfo(final boolean preserve) {
      this.preserveDebugInfo = preserve;
      return this;
    }

    public Builder preserveCustomSections(final boolean preserve) {
      this.preserveCustomSections = preserve;
      return this;
    }

    public Builder preserveNameSection(final boolean preserve) {
      this.preserveNameSection = preserve;
      return this;
    }

    public Builder setBufferSize(final int size) {
      this.bufferSize = requirePositive(size, "Buffer size must be positive");
      return this;
    }

    public Builder useParallelCompression(final boolean useParallel) {
      this.useParallelCompression = useParallel;
      return this;
    }

    public Builder setCompressionLevel(final int level) {
      this.compressionLevel = requireInRange(level, 0, 9, "Compression level must be between 0 and 9");
      return this;
    }

    public Builder enableStreaming(final boolean enable) {
      this.enableStreaming = enable;
      return this;
    }

    public Builder setStreamingThreshold(final long threshold) {
      this.streamingThreshold = requireNonNegative(threshold, "Streaming threshold must be non-negative");
      return this;
    }

    public Builder encryptSerialization(final boolean encrypt) {
      this.encryptSerialization = encrypt;
      return this;
    }

    public Builder setEncryptionAlgorithm(final String algorithm) {
      this.encryptionAlgorithm = Objects.requireNonNull(algorithm, "Encryption algorithm cannot be null");
      return this;
    }

    public Builder setEncryptionKey(final byte[] key) {
      this.encryptionKey = key != null ? key.clone() : null;
      return this;
    }

    public Builder verifyIntegrity(final boolean verify) {
      this.verifyIntegrity = verify;
      return this;
    }

    public Builder includePerformanceMetrics(final boolean include) {
      this.includePerformanceMetrics = include;
      return this;
    }

    public Builder includeCompilerInfo(final boolean include) {
      this.includeCompilerInfo = include;
      return this;
    }

    public Builder includePlatformInfo(final boolean include) {
      this.includePlatformInfo = include;
      return this;
    }

    public Builder addCustomMetadata(final String key, final String value) {
      Objects.requireNonNull(key, "Metadata key cannot be null");
      this.customMetadata.put(key, value);
      return this;
    }

    public Builder setCustomMetadata(final Map<String, String> metadata) {
      this.customMetadata.clear();
      if (metadata != null) {
        this.customMetadata.putAll(metadata);
      }
      return this;
    }

    public Builder cleanupTempFiles(final boolean cleanup) {
      this.cleanupTempFiles = cleanup;
      return this;
    }

    public Builder setTempFilePrefix(final String prefix) {
      this.tempFilePrefix = Objects.requireNonNull(prefix, "Temp file prefix cannot be null");
      return this;
    }

    public Builder useMemoryMappedFiles(final boolean useMmap) {
      this.useMemoryMappedFiles = useMmap;
      return this;
    }

    public SerializationOptions build() {
      // Validation
      if (encryptSerialization && encryptionKey == null) {
        throw new IllegalArgumentException("Encryption key required when encryption is enabled");
      }

      return new SerializationOptions(this);
    }

    private static int requirePositive(final int value, final String message) {
      if (value <= 0) {
        throw new IllegalArgumentException(message + " (was: " + value + ")");
      }
      return value;
    }

    private static long requireNonNegative(final long value, final String message) {
      if (value < 0) {
        throw new IllegalArgumentException(message + " (was: " + value + ")");
      }
      return value;
    }

    private static int requireInRange(final int value, final int min, final int max, final String message) {
      if (value < min || value > max) {
        throw new IllegalArgumentException(message + " (was: " + value + ", expected: " + min + "-" + max + ")");
      }
      return value;
    }
  }
}