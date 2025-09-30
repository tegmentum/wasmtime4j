package ai.tegmentum.wasmtime4j;

/**
 * Component backup configuration interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface ComponentBackupConfig {

  /**
   * Checks if backup is enabled.
   *
   * @return true if enabled
   */
  boolean isEnabled();

  /**
   * Sets backup enabled state.
   *
   * @param enabled enabled state
   */
  void setEnabled(boolean enabled);

  /**
   * Gets the backup strategy.
   *
   * @return backup strategy
   */
  BackupStrategy getStrategy();

  /**
   * Sets the backup strategy.
   *
   * @param strategy backup strategy
   */
  void setStrategy(BackupStrategy strategy);

  /**
   * Gets the backup location.
   *
   * @return backup location
   */
  String getBackupLocation();

  /**
   * Sets the backup location.
   *
   * @param location backup location
   */
  void setBackupLocation(String location);

  /**
   * Gets the backup frequency.
   *
   * @return backup frequency
   */
  BackupFrequency getFrequency();

  /**
   * Sets the backup frequency.
   *
   * @param frequency backup frequency
   */
  void setFrequency(BackupFrequency frequency);

  /**
   * Gets the retention policy.
   *
   * @return retention policy
   */
  RetentionPolicy getRetentionPolicy();

  /**
   * Sets the retention policy.
   *
   * @param policy retention policy
   */
  void setRetentionPolicy(RetentionPolicy policy);

  /**
   * Gets compression settings.
   *
   * @return compression settings
   */
  CompressionSettings getCompressionSettings();

  /**
   * Sets compression settings.
   *
   * @param settings compression settings
   */
  void setCompressionSettings(CompressionSettings settings);

  /**
   * Gets encryption settings.
   *
   * @return encryption settings
   */
  EncryptionSettings getEncryptionSettings();

  /**
   * Sets encryption settings.
   *
   * @param settings encryption settings
   */
  void setEncryptionSettings(EncryptionSettings settings);

  /**
   * Gets backup triggers.
   *
   * @return set of backup triggers
   */
  java.util.Set<BackupTrigger> getTriggers();

  /**
   * Adds a backup trigger.
   *
   * @param trigger backup trigger
   */
  void addTrigger(BackupTrigger trigger);

  /**
   * Removes a backup trigger.
   *
   * @param trigger backup trigger
   */
  void removeTrigger(BackupTrigger trigger);

  /**
   * Gets verification settings.
   *
   * @return verification settings
   */
  VerificationSettings getVerificationSettings();

  /**
   * Sets verification settings.
   *
   * @param settings verification settings
   */
  void setVerificationSettings(VerificationSettings settings);

  /** Backup strategy enumeration. */
  enum BackupStrategy {
    /** Full backup only. */
    FULL_ONLY,
    /** Incremental backups. */
    INCREMENTAL,
    /** Differential backups. */
    DIFFERENTIAL,
    /** Mixed strategy. */
    MIXED,
    /** Snapshot based. */
    SNAPSHOT
  }

  /** Backup frequency enumeration. */
  enum BackupFrequency {
    /** Manual backup only. */
    MANUAL,
    /** On component change. */
    ON_CHANGE,
    /** Hourly backups. */
    HOURLY,
    /** Daily backups. */
    DAILY,
    /** Weekly backups. */
    WEEKLY,
    /** Monthly backups. */
    MONTHLY
  }

  /** Backup trigger enumeration. */
  enum BackupTrigger {
    /** Before component update. */
    BEFORE_UPDATE,
    /** After component update. */
    AFTER_UPDATE,
    /** On component instantiation. */
    ON_INSTANTIATION,
    /** On significant state change. */
    ON_STATE_CHANGE,
    /** On error occurrence. */
    ON_ERROR,
    /** Scheduled trigger. */
    SCHEDULED
  }

  /** Retention policy interface. */
  interface RetentionPolicy {
    /**
     * Gets the maximum number of backups to keep.
     *
     * @return max backup count
     */
    int getMaxBackups();

    /**
     * Gets the maximum age of backups in milliseconds.
     *
     * @return max age
     */
    long getMaxAge();

    /**
     * Gets the maximum total backup size.
     *
     * @return max size in bytes
     */
    long getMaxTotalSize();

    /**
     * Gets the cleanup strategy.
     *
     * @return cleanup strategy
     */
    CleanupStrategy getCleanupStrategy();
  }

  /** Compression settings interface. */
  interface CompressionSettings {
    /**
     * Checks if compression is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the compression algorithm.
     *
     * @return compression algorithm
     */
    CompressionAlgorithm getAlgorithm();

    /**
     * Gets the compression level.
     *
     * @return compression level (1-9)
     */
    int getLevel();

    /**
     * Gets compression options.
     *
     * @return compression options
     */
    java.util.Map<String, Object> getOptions();
  }

  /** Encryption settings interface. */
  interface EncryptionSettings {
    /**
     * Checks if encryption is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the encryption algorithm.
     *
     * @return encryption algorithm
     */
    EncryptionAlgorithm getAlgorithm();

    /**
     * Gets the key derivation function.
     *
     * @return KDF
     */
    String getKeyDerivationFunction();

    /**
     * Gets encryption parameters.
     *
     * @return encryption parameters
     */
    java.util.Map<String, Object> getParameters();
  }

  /** Verification settings interface. */
  interface VerificationSettings {
    /**
     * Checks if verification is enabled.
     *
     * @return true if enabled
     */
    boolean isEnabled();

    /**
     * Gets the checksum algorithm.
     *
     * @return checksum algorithm
     */
    ChecksumAlgorithm getChecksumAlgorithm();

    /**
     * Checks if integrity verification is enabled.
     *
     * @return true if enabled
     */
    boolean isIntegrityVerificationEnabled();

    /**
     * Gets verification frequency.
     *
     * @return verification frequency
     */
    VerificationFrequency getFrequency();
  }

  /** Cleanup strategy enumeration. */
  enum CleanupStrategy {
    /** Oldest first. */
    OLDEST_FIRST,
    /** Largest first. */
    LARGEST_FIRST,
    /** Least used first. */
    LEAST_USED_FIRST,
    /** Keep full backups. */
    KEEP_FULL_BACKUPS
  }

  /** Compression algorithm enumeration. */
  enum CompressionAlgorithm {
    /** GZIP compression. */
    GZIP,
    /** ZIP compression. */
    ZIP,
    /** BZIP2 compression. */
    BZIP2,
    /** LZ4 compression. */
    LZ4,
    /** ZSTD compression. */
    ZSTD
  }

  /** Encryption algorithm enumeration. */
  enum EncryptionAlgorithm {
    /** AES-256-GCM. */
    AES_256_GCM,
    /** AES-128-GCM. */
    AES_128_GCM,
    /** ChaCha20-Poly1305. */
    CHACHA20_POLY1305
  }

  /** Checksum algorithm enumeration. */
  enum ChecksumAlgorithm {
    /** SHA-256. */
    SHA256,
    /** SHA-512. */
    SHA512,
    /** BLAKE2b. */
    BLAKE2B,
    /** CRC32. */
    CRC32
  }

  /** Verification frequency enumeration. */
  enum VerificationFrequency {
    /** Never verify. */
    NEVER,
    /** On backup creation. */
    ON_CREATION,
    /** On backup restore. */
    ON_RESTORE,
    /** Periodic verification. */
    PERIODIC
  }
}
