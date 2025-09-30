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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

/**
 * Result of a WebAssembly module serialization operation.
 *
 * <p>This class encapsulates the serialized module data and its associated metadata, providing
 * convenient methods for saving, loading, and analyzing serialization results.
 *
 * @since 1.0.0
 */
public final class SerializationResult {

  private final byte[] serializedData;
  private final SerializedModuleMetadata metadata;

  /**
   * Creates a new serialization result with the specified data and metadata.
   *
   * @param serializedData the serialized module data
   * @param metadata the serialization metadata
   * @throws IllegalArgumentException if parameters are null
   */
  public SerializationResult(final byte[] serializedData, final SerializedModuleMetadata metadata) {
    this.serializedData =
        Objects.requireNonNull(serializedData, "Serialized data cannot be null").clone();
    this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
  }

  /**
   * Gets the serialized module data.
   *
   * @return a defensive copy of the serialized data
   */
  public byte[] getSerializedData() {
    return serializedData.clone();
  }

  /**
   * Gets the serialization metadata.
   *
   * @return the metadata object
   */
  public SerializedModuleMetadata getMetadata() {
    return metadata;
  }

  /**
   * Gets the size of the serialized data in bytes.
   *
   * @return the size in bytes
   */
  public long getSize() {
    return serializedData.length;
  }

  /**
   * Gets the size of the serialized data in megabytes.
   *
   * @return the size in megabytes
   */
  public double getSizeMB() {
    return serializedData.length / (1024.0 * 1024.0);
  }

  /**
   * Gets the compression ratio achieved during serialization.
   *
   * @return the compression ratio (original size / compressed size)
   */
  public double getCompressionRatio() {
    return metadata.getCompressionRatio();
  }

  /**
   * Checks if the serialization was successful and met performance targets.
   *
   * @return true if serialization was optimal
   */
  public boolean isOptimalPerformance() {
    final SerializationPerformanceMetrics metrics = metadata.getPerformanceMetrics();
    return metrics != null && metrics.isOptimalPerformance();
  }

  /**
   * Gets a human-readable summary of the serialization result.
   *
   * @return the serialization summary
   */
  public String getSummary() {
    final StringBuilder summary = new StringBuilder();
    summary.append("Serialization Result:\n");
    summary.append("  Format: ").append(metadata.getFormat().getIdentifier()).append("\n");
    summary.append("  Size: ").append(String.format("%.2f MB", getSizeMB())).append("\n");
    summary
        .append("  Compression: ")
        .append(String.format("%.2fx", getCompressionRatio()))
        .append("\n");
    summary.append("  Duration: ").append(metadata.getSerializationDurationMs()).append("ms\n");

    if (metadata.getPerformanceMetrics() != null) {
      summary
          .append("  Performance: ")
          .append(metadata.getPerformanceMetrics().getPerformanceSummary())
          .append("\n");
    }

    return summary.toString();
  }

  /**
   * Saves the serialized module to a file.
   *
   * @param filePath the file path to save to
   * @throws IOException if the save operation fails
   * @throws IllegalArgumentException if filePath is null
   */
  public void saveToFile(final Path filePath) throws IOException {
    Objects.requireNonNull(filePath, "File path cannot be null");

    // Ensure parent directories exist
    final Path parentDir = filePath.getParent();
    if (parentDir != null && !Files.exists(parentDir)) {
      Files.createDirectories(parentDir);
    }

    // Write serialized data
    Files.write(filePath, serializedData);

    // Write metadata to a companion file
    final Path metadataPath = getMetadataPath(filePath);
    final String metadataJson = serializeMetadata(metadata);
    Files.write(metadataPath, metadataJson.getBytes("UTF-8"));
  }

  /**
   * Loads a serialization result from a file.
   *
   * @param filePath the file path to load from
   * @return the loaded serialization result
   * @throws IOException if the load operation fails
   * @throws IllegalArgumentException if filePath is null or file doesn't exist
   */
  public static SerializationResult loadFromFile(final Path filePath) throws IOException {
    Objects.requireNonNull(filePath, "File path cannot be null");

    if (!Files.exists(filePath)) {
      throw new IOException("Serialized module file not found: " + filePath);
    }

    // Load serialized data
    final byte[] serializedData = Files.readAllBytes(filePath);

    // Load metadata from companion file
    final Path metadataPath = getMetadataPath(filePath);
    SerializedModuleMetadata metadata;

    if (Files.exists(metadataPath)) {
      final String metadataJson = new String(Files.readAllBytes(metadataPath), "UTF-8");
      metadata = deserializeMetadata(metadataJson);
    } else {
      // Create minimal metadata if companion file is missing
      metadata = createMinimalMetadata(serializedData);
    }

    return new SerializationResult(serializedData, metadata);
  }

  /**
   * Validates the integrity of this serialization result.
   *
   * @return true if integrity validation passes
   */
  public boolean validateIntegrity() {
    return metadata.validateIntegrity(serializedData);
  }

  /**
   * Checks if this serialization result is compatible with the current environment.
   *
   * @return true if compatible
   */
  public boolean isCompatibleWithCurrentEnvironment() {
    return metadata.isCompatibleWithCurrentEnvironment();
  }

  /**
   * Gets the recommended deserialization time based on performance metrics.
   *
   * @return estimated deserialization time in milliseconds
   */
  public long getEstimatedDeserializationTime() {
    return metadata.getEstimatedDeserializationTimeMs();
  }

  /**
   * Creates a copy of this result with updated metadata.
   *
   * @param newMetadata the new metadata
   * @return a new serialization result with the updated metadata
   */
  public SerializationResult withMetadata(final SerializedModuleMetadata newMetadata) {
    Objects.requireNonNull(newMetadata, "New metadata cannot be null");
    return new SerializationResult(serializedData, newMetadata);
  }

  /**
   * Compares this result with another for performance analysis.
   *
   * @param other the other serialization result
   * @return a comparison summary string
   */
  public String compareWith(final SerializationResult other) {
    Objects.requireNonNull(other, "Other result cannot be null");

    final StringBuilder comparison = new StringBuilder();
    comparison.append("Serialization Comparison:\n");

    // Size comparison
    final long sizeDiff = this.getSize() - other.getSize();
    final double sizePercent = ((double) sizeDiff / other.getSize()) * 100;
    comparison
        .append("  Size: ")
        .append(String.format("%+.2f%% (%+d bytes)", sizePercent, sizeDiff))
        .append("\n");

    // Compression comparison
    final double compressionDiff = this.getCompressionRatio() - other.getCompressionRatio();
    comparison
        .append("  Compression: ")
        .append(String.format("%+.2fx", compressionDiff))
        .append("\n");

    // Duration comparison
    final long durationDiff =
        this.metadata.getSerializationDurationMs() - other.metadata.getSerializationDurationMs();
    final double durationPercent =
        other.metadata.getSerializationDurationMs() > 0
            ? ((double) durationDiff / other.metadata.getSerializationDurationMs()) * 100
            : 0;
    comparison
        .append("  Duration: ")
        .append(String.format("%+.1f%% (%+dms)", durationPercent, durationDiff))
        .append("\n");

    return comparison.toString();
  }

  @Override
  public String toString() {
    return String.format(
        "SerializationResult{format=%s, size=%d bytes, compression=%.2fx, duration=%dms}",
        metadata.getFormat().getIdentifier(),
        serializedData.length,
        getCompressionRatio(),
        metadata.getSerializationDurationMs());
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SerializationResult other = (SerializationResult) obj;
    return java.util.Arrays.equals(serializedData, other.serializedData)
        && Objects.equals(metadata, other.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(java.util.Arrays.hashCode(serializedData), metadata);
  }

  // Private helper methods

  /**
   * Gets the metadata file path for a given module file path.
   *
   * @param modulePath the module file path
   * @return the metadata file path
   */
  private static Path getMetadataPath(final Path modulePath) {
    final String fileName = modulePath.getFileName().toString();
    final String metadataFileName = fileName + ".meta";
    return modulePath.resolveSibling(metadataFileName);
  }

  /**
   * Serializes metadata to JSON string (simplified implementation).
   *
   * @param metadata the metadata to serialize
   * @return JSON string representation
   */
  private static String serializeMetadata(final SerializedModuleMetadata metadata) {
    // Simplified JSON serialization - in production would use Jackson or similar
    return String.format(
        "{\n"
            + "  \"format\": \"%s\",\n"
            + "  \"formatVersion\": \"%s\",\n"
            + "  \"serializedSize\": %d,\n"
            + "  \"originalSize\": %d,\n"
            + "  \"sha256Hash\": \"%s\",\n"
            + "  \"timestamp\": \"%s\",\n"
            + "  \"serializationDurationMs\": %d,\n"
            + "  \"compressionRatio\": %.4f,\n"
            + "  \"wasmtimeVersion\": \"%s\",\n"
            + "  \"javaVersion\": \"%s\",\n"
            + "  \"platformArch\": \"%s\",\n"
            + "  \"platformOs\": \"%s\"\n"
            + "}",
        metadata.getFormat().getIdentifier(),
        metadata.getFormatVersion(),
        metadata.getSerializedSize(),
        metadata.getOriginalSize(),
        metadata.getSha256Hash(),
        metadata.getSerializationTimestamp().toString(),
        metadata.getSerializationDurationMs(),
        metadata.getCompressionRatio(),
        metadata.getWasmtimeVersion(),
        metadata.getJavaVersion(),
        metadata.getPlatformArch(),
        metadata.getPlatformOs());
  }

  /**
   * Deserializes metadata from JSON string (simplified implementation).
   *
   * @param json the JSON string
   * @return the deserialized metadata
   */
  private static SerializedModuleMetadata deserializeMetadata(final String json) {
    // Simplified JSON deserialization - in production would use Jackson or similar
    // For now, return basic metadata with default values
    return new SerializedModuleMetadata.Builder()
        .setFormat(ModuleSerializationFormat.COMPACT_BINARY_LZ4)
        .setSerializedSize(0)
        .setOriginalSize(0)
        .setSha256Hash("")
        .build();
  }

  /**
   * Creates minimal metadata for cases where metadata file is missing.
   *
   * @param serializedData the serialized data
   * @return minimal metadata
   */
  private static SerializedModuleMetadata createMinimalMetadata(final byte[] serializedData) {
    return new SerializedModuleMetadata.Builder()
        .setFormat(ModuleSerializationFormat.RAW_BINARY) // Safe assumption
        .setSerializedSize(serializedData.length)
        .setOriginalSize(serializedData.length)
        .setSha256Hash(calculateSha256Hash(serializedData))
        .build();
  }

  /**
   * Calculates SHA-256 hash of data.
   *
   * @param data the data to hash
   * @return the hash as hex string
   */
  private static String calculateSha256Hash(final byte[] data) {
    try {
      final java.security.MessageDigest digest = java.security.MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = digest.digest(data);
      final StringBuilder result = new StringBuilder();
      for (final byte b : hashBytes) {
        result.append(String.format("%02x", b));
      }
      return result.toString();
    } catch (java.security.NoSuchAlgorithmException e) {
      return ""; // Fallback for missing SHA-256
    }
  }
}
