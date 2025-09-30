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

import java.util.Objects;

/**
 * Defines the serialization format for WebAssembly modules.
 *
 * <p>This enum specifies the different serialization formats available for WebAssembly modules,
 * each optimized for specific use cases and performance characteristics.
 *
 * @since 1.0.0
 */
public enum ModuleSerializationFormat {
  /**
   * Compact binary format with LZ4 compression.
   *
   * <p>Optimized for storage efficiency and fast compression/decompression. Best for caching
   * scenarios where disk space is important but serialization speed is also critical.
   */
  COMPACT_BINARY_LZ4("compact-binary-lz4", "cbz4", true, true, false),

  /**
   * Compact binary format with GZIP compression.
   *
   * <p>Optimized for maximum compression ratio. Best for long-term storage or network transmission
   * where bandwidth is limited.
   */
  COMPACT_BINARY_GZIP("compact-binary-gzip", "cbgz", true, true, true),

  /**
   * Raw binary format without compression.
   *
   * <p>Optimized for maximum serialization/deserialization speed. Best for in-memory caching or
   * scenarios where CPU is more constrained than storage.
   */
  RAW_BINARY("raw-binary", "bin", false, false, false),

  /**
   * Streaming binary format for incremental serialization.
   *
   * <p>Supports progressive serialization and deserialization of large modules. Best for scenarios
   * with memory constraints or very large modules.
   */
  STREAMING_BINARY("streaming-binary", "stream", true, false, false),

  /**
   * Memory-mapped format optimized for zero-copy operations.
   *
   * <p>Uses memory-mapped files for direct access without loading entire module into memory. Best
   * for very large modules or scenarios requiring minimal memory footprint.
   */
  MEMORY_MAPPED("memory-mapped", "mmap", false, false, false);

  private final String identifier;
  private final String fileExtension;
  private final boolean supportsStreaming;
  private final boolean supportsCompression;
  private final boolean supportsHighCompression;

  ModuleSerializationFormat(
      final String identifier,
      final String fileExtension,
      final boolean supportsStreaming,
      final boolean supportsCompression,
      final boolean supportsHighCompression) {
    this.identifier = Objects.requireNonNull(identifier, "Identifier cannot be null");
    this.fileExtension = Objects.requireNonNull(fileExtension, "File extension cannot be null");
    this.supportsStreaming = supportsStreaming;
    this.supportsCompression = supportsCompression;
    this.supportsHighCompression = supportsHighCompression;
  }

  /**
   * Gets the unique identifier for this serialization format.
   *
   * @return the format identifier
   */
  public String getIdentifier() {
    return identifier;
  }

  /**
   * Gets the recommended file extension for this format.
   *
   * @return the file extension (without leading dot)
   */
  public String getFileExtension() {
    return fileExtension;
  }

  /**
   * Checks if this format supports streaming serialization.
   *
   * @return true if streaming is supported
   */
  public boolean supportsStreaming() {
    return supportsStreaming;
  }

  /**
   * Checks if this format supports compression.
   *
   * @return true if compression is supported
   */
  public boolean supportsCompression() {
    return supportsCompression;
  }

  /**
   * Checks if this format supports high compression ratios.
   *
   * @return true if high compression is available
   */
  public boolean supportsHighCompression() {
    return supportsHighCompression;
  }

  /**
   * Gets the optimal format for a given use case.
   *
   * @param useCase the serialization use case
   * @return the recommended format
   * @throws IllegalArgumentException if useCase is null
   */
  public static ModuleSerializationFormat getOptimalFormat(final SerializationUseCase useCase) {
    Objects.requireNonNull(useCase, "Use case cannot be null");

    switch (useCase) {
      case MEMORY_CACHE:
        return RAW_BINARY;
      case DISK_CACHE:
        return COMPACT_BINARY_LZ4;
      case NETWORK_TRANSMISSION:
        return COMPACT_BINARY_GZIP;
      case LONG_TERM_STORAGE:
        return COMPACT_BINARY_GZIP;
      case LARGE_MODULES:
        return STREAMING_BINARY;
      case MEMORY_CONSTRAINED:
        return MEMORY_MAPPED;
      default:
        return COMPACT_BINARY_LZ4; // Safe default
    }
  }

  /**
   * Parses a format from its identifier string.
   *
   * @param identifier the format identifier
   * @return the matching format
   * @throws IllegalArgumentException if identifier is invalid
   */
  public static ModuleSerializationFormat fromIdentifier(final String identifier) {
    Objects.requireNonNull(identifier, "Identifier cannot be null");

    for (final ModuleSerializationFormat format : values()) {
      if (format.identifier.equals(identifier)) {
        return format;
      }
    }

    throw new IllegalArgumentException("Unknown serialization format: " + identifier);
  }

  /** Serialization use cases for format selection. */
  public enum SerializationUseCase {
    /** In-memory caching for fast access. */
    MEMORY_CACHE,
    /** Disk-based caching with moderate compression. */
    DISK_CACHE,
    /** Network transmission requiring high compression. */
    NETWORK_TRANSMISSION,
    /** Long-term archival storage. */
    LONG_TERM_STORAGE,
    /** Very large modules requiring streaming. */
    LARGE_MODULES,
    /** Memory-constrained environments. */
    MEMORY_CONSTRAINED
  }
}
