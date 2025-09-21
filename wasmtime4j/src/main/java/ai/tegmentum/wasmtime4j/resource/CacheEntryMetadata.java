package ai.tegmentum.wasmtime4j.resource;

import java.time.Instant;

/**
 * Metadata for individual cache entries.
 *
 * <p>CacheEntryMetadata provides detailed information about cached entries including timestamps,
 * access patterns, and resource characteristics.
 *
 * @since 1.0.0
 */
public final class CacheEntryMetadata {

  private final String key;
  private final ResourceType resourceType;
  private final CachePolicy policy;
  private final Instant creationTime;
  private final Instant lastAccessTime;
  private final Instant lastModificationTime;
  private final Instant expirationTime;
  private final long accessCount;
  private final long estimatedSize;
  private final CachePriority priority;
  private final double evictionWeight;
  private final boolean isExpired;
  private final boolean isPersistent;

  private CacheEntryMetadata(final Builder builder) {
    this.key = builder.key;
    this.resourceType = builder.resourceType;
    this.policy = builder.policy;
    this.creationTime = builder.creationTime;
    this.lastAccessTime = builder.lastAccessTime;
    this.lastModificationTime = builder.lastModificationTime;
    this.expirationTime = builder.expirationTime;
    this.accessCount = builder.accessCount;
    this.estimatedSize = builder.estimatedSize;
    this.priority = builder.priority;
    this.evictionWeight = builder.evictionWeight;
    this.isExpired = builder.isExpired;
    this.isPersistent = builder.isPersistent;
  }

  /**
   * Creates a new metadata builder.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the cache key.
   *
   * @return the cache key
   */
  public String getKey() {
    return key;
  }

  /**
   * Gets the resource type.
   *
   * @return the resource type
   */
  public ResourceType getResourceType() {
    return resourceType;
  }

  /**
   * Gets the cache policy.
   *
   * @return the cache policy
   */
  public CachePolicy getPolicy() {
    return policy;
  }

  /**
   * Gets the creation time.
   *
   * @return the creation time
   */
  public Instant getCreationTime() {
    return creationTime;
  }

  /**
   * Gets the last access time.
   *
   * @return the last access time
   */
  public Instant getLastAccessTime() {
    return lastAccessTime;
  }

  /**
   * Gets the last modification time.
   *
   * @return the last modification time
   */
  public Instant getLastModificationTime() {
    return lastModificationTime;
  }

  /**
   * Gets the expiration time.
   *
   * @return the expiration time, or null if no expiration
   */
  public Instant getExpirationTime() {
    return expirationTime;
  }

  /**
   * Gets the access count.
   *
   * @return the number of times this entry has been accessed
   */
  public long getAccessCount() {
    return accessCount;
  }

  /**
   * Gets the estimated size in bytes.
   *
   * @return the estimated size
   */
  public long getEstimatedSize() {
    return estimatedSize;
  }

  /**
   * Gets the cache priority.
   *
   * @return the cache priority
   */
  public CachePriority getPriority() {
    return priority;
  }

  /**
   * Gets the eviction weight.
   *
   * @return the eviction weight
   */
  public double getEvictionWeight() {
    return evictionWeight;
  }

  /**
   * Checks if the entry has expired.
   *
   * @return true if the entry has expired
   */
  public boolean isExpired() {
    return isExpired;
  }

  /**
   * Checks if the entry is persistent.
   *
   * @return true if the entry is stored persistently
   */
  public boolean isPersistent() {
    return isPersistent;
  }

  /**
   * Calculates the age of the entry.
   *
   * @return the age in milliseconds
   */
  public long getAge() {
    return Instant.now().toEpochMilli() - creationTime.toEpochMilli();
  }

  /**
   * Calculates the time since last access.
   *
   * @return the idle time in milliseconds
   */
  public long getIdleTime() {
    return Instant.now().toEpochMilli() - lastAccessTime.toEpochMilli();
  }

  /**
   * Calculates the remaining time to live.
   *
   * @return the remaining TTL in milliseconds, or -1 if no expiration
   */
  public long getRemainingTtl() {
    if (expirationTime == null) {
      return -1;
    }
    return Math.max(0, expirationTime.toEpochMilli() - Instant.now().toEpochMilli());
  }

  /**
   * Calculates the access frequency (accesses per minute).
   *
   * @return the access frequency
   */
  public double getAccessFrequency() {
    final long ageMinutes = Math.max(1, getAge() / (60 * 1000));
    return (double) accessCount / ageMinutes;
  }

  /** Builder for creating cache entry metadata. */
  public static final class Builder {
    private String key;
    private ResourceType resourceType;
    private CachePolicy policy;
    private Instant creationTime = Instant.now();
    private Instant lastAccessTime = Instant.now();
    private Instant lastModificationTime = Instant.now();
    private Instant expirationTime;
    private long accessCount = 0;
    private long estimatedSize = 0;
    private CachePriority priority = CachePriority.NORMAL;
    private double evictionWeight = 1.0;
    private boolean isExpired = false;
    private boolean isPersistent = false;

    private Builder() {}

    public Builder withKey(final String key) {
      this.key = key;
      return this;
    }

    public Builder withResourceType(final ResourceType resourceType) {
      this.resourceType = resourceType;
      return this;
    }

    public Builder withPolicy(final CachePolicy policy) {
      this.policy = policy;
      return this;
    }

    public Builder withCreationTime(final Instant creationTime) {
      this.creationTime = creationTime;
      return this;
    }

    public Builder withLastAccessTime(final Instant lastAccessTime) {
      this.lastAccessTime = lastAccessTime;
      return this;
    }

    public Builder withLastModificationTime(final Instant lastModificationTime) {
      this.lastModificationTime = lastModificationTime;
      return this;
    }

    public Builder withExpirationTime(final Instant expirationTime) {
      this.expirationTime = expirationTime;
      return this;
    }

    public Builder withAccessCount(final long accessCount) {
      this.accessCount = accessCount;
      return this;
    }

    public Builder withEstimatedSize(final long estimatedSize) {
      this.estimatedSize = estimatedSize;
      return this;
    }

    public Builder withPriority(final CachePriority priority) {
      this.priority = priority;
      return this;
    }

    public Builder withEvictionWeight(final double evictionWeight) {
      this.evictionWeight = evictionWeight;
      return this;
    }

    public Builder withExpired(final boolean isExpired) {
      this.isExpired = isExpired;
      return this;
    }

    public Builder withPersistent(final boolean isPersistent) {
      this.isPersistent = isPersistent;
      return this;
    }

    public CacheEntryMetadata build() {
      return new CacheEntryMetadata(this);
    }
  }

  @Override
  public String toString() {
    return String.format(
        "CacheEntryMetadata{key='%s', resourceType=%s, priority=%s, "
            + "accessCount=%d, estimatedSize=%d, age=%dms, idleTime=%dms, "
            + "remainingTtl=%dms, isExpired=%s}",
        key,
        resourceType,
        priority,
        accessCount,
        estimatedSize,
        getAge(),
        getIdleTime(),
        getRemainingTtl(),
        isExpired);
  }
}
