package ai.tegmentum.wasmtime4j.serialization.impl;

import ai.tegmentum.wasmtime4j.Engine;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.serialization.CompressionType;
import ai.tegmentum.wasmtime4j.serialization.ModuleMetadata;
import ai.tegmentum.wasmtime4j.serialization.SerializedModule;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Objects;

/**
 * Default implementation of SerializedModule.
 *
 * <p>This implementation provides serialized module functionality with integrity checking,
 * compression support, and metadata management.
 *
 * @since 1.0.0
 */
public final class SerializedModuleImpl implements SerializedModule {

  private final byte[] data;
  private final ModuleMetadata metadata;
  private final String checksum;

  /**
   * Creates a new SerializedModuleImpl.
   *
   * @param data the serialized module data
   * @param metadata the module metadata
   * @throws IllegalArgumentException if data or metadata is null
   */
  public SerializedModuleImpl(final byte[] data, final ModuleMetadata metadata) {
    this.data = Objects.requireNonNull(data, "Serialized data cannot be null").clone();
    this.metadata = Objects.requireNonNull(metadata, "Metadata cannot be null");
    this.checksum = calculateChecksum(this.data);
  }

  @Override
  public byte[] getData() {
    return data.clone();
  }

  @Override
  public ModuleMetadata getMetadata() {
    return metadata;
  }

  @Override
  public boolean isCompatible(final Engine engine) {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    // Check basic compatibility requirements
    if (!engine.supportsModuleSerialization()) {
      return false;
    }

    // Check runtime version compatibility
    final String engineVersion = engine.getRuntimeVersion();
    if (!metadata.isCompatibleWith(engineVersion)) {
      return false;
    }

    // Check platform compatibility
    try {
      final ai.tegmentum.wasmtime4j.serialization.TargetPlatform currentPlatform =
          ai.tegmentum.wasmtime4j.serialization.TargetPlatform.current();
      if (!metadata.isCompatibleWith(currentPlatform)) {
        return false;
      }
    } catch (final UnsupportedOperationException e) {
      // Current platform not supported
      return false;
    }

    return true;
  }

  @Override
  public Module deserialize(final Engine engine) throws WasmException {
    if (engine == null) {
      throw new IllegalArgumentException("Engine cannot be null");
    }

    if (!isCompatible(engine)) {
      throw new WasmException(
          "Serialized module is not compatible with the provided engine. "
              + "Engine version: "
              + engine.getRuntimeVersion()
              + ", Module version: "
              + metadata.getWasmtimeVersion()
              + ", Target platform: "
              + metadata.getTargetPlatform());
    }

    // Verify data integrity
    final String currentChecksum = calculateChecksum(data);
    if (!checksum.equals(currentChecksum)) {
      throw new WasmException(
          "Serialized module data corruption detected. Checksums do not match.");
    }

    return engine.deserializeModule(data);
  }

  @Override
  public long getSize() {
    return data.length;
  }

  @Override
  public CompressionType getCompressionType() {
    return metadata.getCompressionType();
  }

  @Override
  public boolean hasDebugInfo() {
    return metadata.hasDebugInfo();
  }

  @Override
  public boolean hasSourceMap() {
    return metadata.hasSourceMap();
  }

  @Override
  public String getChecksum() {
    return checksum;
  }

  /**
   * Calculates the SHA-256 checksum of the given data.
   *
   * @param data the data to calculate checksum for
   * @return the checksum as a hex string
   */
  private String calculateChecksum(final byte[] data) {
    try {
      final MessageDigest md = MessageDigest.getInstance("SHA-256");
      final byte[] hashBytes = md.digest(data);
      final StringBuilder sb = new StringBuilder();
      for (final byte b : hashBytes) {
        sb.append(String.format("%02x", b));
      }
      return sb.toString();
    } catch (final NoSuchAlgorithmException e) {
      throw new RuntimeException("SHA-256 algorithm not available", e);
    }
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final SerializedModuleImpl other = (SerializedModuleImpl) obj;
    return Objects.equals(checksum, other.checksum) && Objects.equals(metadata, other.metadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(checksum, metadata);
  }

  @Override
  public String toString() {
    return "SerializedModule{"
        + "size="
        + data.length
        + ", compression="
        + metadata.getCompressionType()
        + ", platform="
        + metadata.getTargetPlatform()
        + ", checksum="
        + checksum.substring(0, 8)
        + "...}";
  }
}
