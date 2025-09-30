package ai.tegmentum.wasmtime4j;

/**
 * Component backup interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentBackup {

  /**
   * Gets the backup ID.
   *
   * @return backup ID
   */
  String getBackupId();

  /**
   * Gets the component ID.
   *
   * @return component ID
   */
  String getComponentId();

  /**
   * Gets the backup timestamp.
   *
   * @return backup timestamp
   */
  long getTimestamp();

  /**
   * Gets the backup type.
   *
   * @return backup type
   */
  BackupType getType();

  /**
   * Gets the backup size in bytes.
   *
   * @return backup size
   */
  long getSize();

  /**
   * Gets the backup checksum.
   *
   * @return backup checksum
   */
  String getChecksum();

  /**
   * Gets backup metadata.
   *
   * @return metadata map
   */
  java.util.Map<String, Object> getMetadata();

  /**
   * Gets the backup status.
   *
   * @return backup status
   */
  BackupStatus getStatus();

  /**
   * Gets the backup location.
   *
   * @return backup location
   */
  String getLocation();

  /**
   * Verifies backup integrity.
   *
   * @return verification result
   */
  VerificationResult verify();

  /**
   * Restores from this backup.
   *
   * @param options restore options
   * @return restore result
   */
  RestoreResult restore(ComponentRestoreOptions options);

  /**
   * Deletes this backup.
   *
   * @return true if deleted successfully
   */
  boolean delete();

  /**
   * Gets backup compression info.
   *
   * @return compression info
   */
  CompressionInfo getCompressionInfo();

  /**
   * Gets backup encryption info.
   *
   * @return encryption info, or null if not encrypted
   */
  EncryptionInfo getEncryptionInfo();

  /** Backup type enumeration. */
  enum BackupType {
    /** Full backup. */
    FULL,
    /** Incremental backup. */
    INCREMENTAL,
    /** Differential backup. */
    DIFFERENTIAL,
    /** Snapshot backup. */
    SNAPSHOT
  }

  /** Backup status enumeration. */
  enum BackupStatus {
    /** Backup in progress. */
    IN_PROGRESS,
    /** Backup completed. */
    COMPLETED,
    /** Backup failed. */
    FAILED,
    /** Backup corrupted. */
    CORRUPTED,
    /** Backup archived. */
    ARCHIVED
  }

  /** Verification result interface. */
  interface VerificationResult {
    /**
     * Checks if verification passed.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Gets verification errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets verification timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets the checksum match result.
     *
     * @return true if checksum matches
     */
    boolean isChecksumValid();
  }

  /** Restore result interface. */
  interface RestoreResult {
    /**
     * Checks if restore was successful.
     *
     * @return true if successful
     */
    boolean isSuccessful();

    /**
     * Gets restore errors.
     *
     * @return list of errors
     */
    java.util.List<String> getErrors();

    /**
     * Gets restore timestamp.
     *
     * @return timestamp
     */
    long getTimestamp();

    /**
     * Gets the restored component.
     *
     * @return restored component
     */
    Component getRestoredComponent();
  }

  /** Compression info interface. */
  interface CompressionInfo {
    /**
     * Gets the compression algorithm.
     *
     * @return algorithm name
     */
    String getAlgorithm();

    /**
     * Gets the original size.
     *
     * @return original size in bytes
     */
    long getOriginalSize();

    /**
     * Gets the compressed size.
     *
     * @return compressed size in bytes
     */
    long getCompressedSize();

    /**
     * Gets the compression ratio.
     *
     * @return compression ratio
     */
    double getCompressionRatio();
  }

  /** Encryption info interface. */
  interface EncryptionInfo {
    /**
     * Gets the encryption algorithm.
     *
     * @return algorithm name
     */
    String getAlgorithm();

    /**
     * Gets the key derivation function.
     *
     * @return KDF name
     */
    String getKeyDerivationFunction();

    /**
     * Checks if backup is encrypted.
     *
     * @return true if encrypted
     */
    boolean isEncrypted();

    /**
     * Gets encryption parameters.
     *
     * @return encryption parameters
     */
    java.util.Map<String, Object> getParameters();
  }
}
