package ai.tegmentum.wasmtime4j;

import java.util.Objects;

/**
 * Represents a contiguous segment of WebAssembly linear memory.
 *
 * <p>Memory segments provide detailed information about memory layout and usage patterns.
 * This is particularly useful for memory analysis, debugging, and optimization in
 * enterprise applications that require fine-grained memory management.
 *
 * @since 1.0.0
 */
public final class MemorySegment {

  private final long startOffset;
  private final long size;
  private final boolean isActive;
  private final boolean isReadOnly;
  private final boolean isExecutable;
  private final String description;
  private final long creationTimestamp;
  private final long lastAccessTimestamp;

  /**
   * Creates a new memory segment descriptor.
   *
   * @param startOffset the starting byte offset of this segment
   * @param size the size of this segment in bytes
   * @param isActive whether this segment is currently active
   * @param isReadOnly whether this segment is read-only
   * @param isExecutable whether this segment is executable
   * @param description a human-readable description of this segment
   * @param creationTimestamp when this segment was created (milliseconds since epoch)
   * @param lastAccessTimestamp when this segment was last accessed (milliseconds since epoch)
   * @throws IllegalArgumentException if startOffset or size is negative, or description is null
   */
  public MemorySegment(
      final long startOffset,
      final long size,
      final boolean isActive,
      final boolean isReadOnly,
      final boolean isExecutable,
      final String description,
      final long creationTimestamp,
      final long lastAccessTimestamp) {
    if (startOffset < 0) {
      throw new IllegalArgumentException("startOffset cannot be negative: " + startOffset);
    }
    if (size < 0) {
      throw new IllegalArgumentException("size cannot be negative: " + size);
    }
    if (description == null) {
      throw new IllegalArgumentException("description cannot be null");
    }

    this.startOffset = startOffset;
    this.size = size;
    this.isActive = isActive;
    this.isReadOnly = isReadOnly;
    this.isExecutable = isExecutable;
    this.description = description;
    this.creationTimestamp = creationTimestamp;
    this.lastAccessTimestamp = lastAccessTimestamp;
  }

  /**
   * Gets the starting byte offset of this memory segment.
   *
   * @return the starting offset in bytes
   */
  public long getStartOffset() {
    return startOffset;
  }

  /**
   * Gets the size of this memory segment in bytes.
   *
   * @return the segment size in bytes
   */
  public long getSize() {
    return size;
  }

  /**
   * Gets the ending byte offset of this memory segment (exclusive).
   *
   * @return the ending offset in bytes
   */
  public long getEndOffset() {
    return startOffset + size;
  }

  /**
   * Checks if this memory segment is currently active.
   *
   * @return true if the segment is active, false otherwise
   */
  public boolean isActive() {
    return isActive;
  }

  /**
   * Checks if this memory segment is read-only.
   *
   * @return true if the segment is read-only, false otherwise
   */
  public boolean isReadOnly() {
    return isReadOnly;
  }

  /**
   * Checks if this memory segment is executable.
   *
   * @return true if the segment is executable, false otherwise
   */
  public boolean isExecutable() {
    return isExecutable;
  }

  /**
   * Gets a human-readable description of this memory segment.
   *
   * @return the segment description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the timestamp when this segment was created.
   *
   * @return the creation timestamp in milliseconds since epoch
   */
  public long getCreationTimestamp() {
    return creationTimestamp;
  }

  /**
   * Gets the timestamp when this segment was last accessed.
   *
   * @return the last access timestamp in milliseconds since epoch
   */
  public long getLastAccessTimestamp() {
    return lastAccessTimestamp;
  }

  /**
   * Checks if the given offset falls within this memory segment.
   *
   * @param offset the byte offset to check
   * @return true if the offset is within this segment, false otherwise
   */
  public boolean contains(final long offset) {
    return offset >= startOffset && offset < getEndOffset();
  }

  /**
   * Checks if this segment overlaps with another segment.
   *
   * @param other the other segment to check for overlap
   * @return true if the segments overlap, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  public boolean overlaps(final MemorySegment other) {
    if (other == null) {
      throw new IllegalArgumentException("other segment cannot be null");
    }
    return startOffset < other.getEndOffset() && getEndOffset() > other.startOffset;
  }

  /**
   * Gets the age of this segment in milliseconds.
   *
   * @return the age since creation in milliseconds
   */
  public long getAge() {
    return System.currentTimeMillis() - creationTimestamp;
  }

  /**
   * Gets the time since last access in milliseconds.
   *
   * @return the time since last access in milliseconds
   */
  public long getTimeSinceLastAccess() {
    return System.currentTimeMillis() - lastAccessTimestamp;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final MemorySegment that = (MemorySegment) obj;
    return startOffset == that.startOffset
        && size == that.size
        && isActive == that.isActive
        && isReadOnly == that.isReadOnly
        && isExecutable == that.isExecutable
        && creationTimestamp == that.creationTimestamp
        && lastAccessTimestamp == that.lastAccessTimestamp
        && Objects.equals(description, that.description);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        startOffset,
        size,
        isActive,
        isReadOnly,
        isExecutable,
        description,
        creationTimestamp,
        lastAccessTimestamp);
  }

  @Override
  public String toString() {
    return "MemorySegment{"
        + "startOffset="
        + startOffset
        + ", size="
        + size
        + ", endOffset="
        + getEndOffset()
        + ", isActive="
        + isActive
        + ", isReadOnly="
        + isReadOnly
        + ", isExecutable="
        + isExecutable
        + ", description='"
        + description
        + '\''
        + ", age="
        + getAge()
        + "ms"
        + ", timeSinceLastAccess="
        + getTimeSinceLastAccess()
        + "ms"
        + '}';
  }
}