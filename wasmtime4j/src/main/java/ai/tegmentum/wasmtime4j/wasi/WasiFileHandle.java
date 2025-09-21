package ai.tegmentum.wasmtime4j.wasi;

import java.io.Closeable;

/**
 * Handle representing an open file in the WASI filesystem.
 *
 * <p>A WasiFileHandle represents an open file descriptor that can be used for I/O operations.
 * Handles are obtained by calling {@link WasiFilesystem#openFile} and should be closed when no
 * longer needed to free system resources.
 *
 * <p>File handles are not thread-safe and should not be shared between threads without external
 * synchronization.
 *
 * @since 1.0.0
 */
public interface WasiFileHandle extends Closeable {

  /**
   * Gets the unique identifier for this file handle.
   *
   * <p>The file descriptor is a unique integer that identifies this open file within the WASI
   * context. It can be used for debugging and logging purposes.
   *
   * @return the file descriptor number
   */
  int getFileDescriptor();

  /**
   * Gets the rights (capabilities) available on this file handle.
   *
   * <p>The rights determine what operations can be performed on this file handle. These are set
   * when the file is opened and cannot be changed.
   *
   * @return the rights bitmask for this handle
   */
  WasiRights getRights();

  /**
   * Gets the current file position for read/write operations.
   *
   * <p>The file position determines where the next read or write operation will occur. It is
   * automatically advanced by read/write operations or can be set explicitly.
   *
   * @return the current file position in bytes from the start of the file
   */
  long getPosition();

  /**
   * Sets the current file position for read/write operations.
   *
   * <p>This method changes the file position where the next read or write operation will occur.
   * The position must be non-negative and within the bounds of the file.
   *
   * @param position the new file position in bytes from the start of the file
   * @throws IllegalArgumentException if position is negative
   * @throws IllegalStateException if the handle is closed
   */
  void setPosition(final long position);

  /**
   * Gets the size of the file in bytes.
   *
   * <p>This method returns the current size of the file. The size may change if the file is
   * modified by write operations or truncation.
   *
   * @return the file size in bytes
   * @throws IllegalStateException if the handle is closed
   */
  long getSize();

  /**
   * Checks if this file handle is still valid and open.
   *
   * <p>A file handle becomes invalid when it is closed either explicitly via {@link #close()} or
   * implicitly when the WASI context is destroyed.
   *
   * @return true if the handle is valid and can be used for operations, false otherwise
   */
  boolean isValid();

  /**
   * Checks if this file handle supports reading.
   *
   * <p>Read capability depends on the rights that were specified when the file was opened.
   *
   * @return true if reading is supported, false otherwise
   */
  boolean canRead();

  /**
   * Checks if this file handle supports writing.
   *
   * <p>Write capability depends on the rights that were specified when the file was opened.
   *
   * @return true if writing is supported, false otherwise
   */
  boolean canWrite();

  /**
   * Checks if this file handle supports seeking (changing position).
   *
   * <p>Seek capability depends on the rights that were specified when the file was opened and
   * the underlying file type (regular files typically support seeking, pipes do not).
   *
   * @return true if seeking is supported, false otherwise
   */
  boolean canSeek();

  /**
   * Flushes any buffered data to the underlying storage.
   *
   * <p>This method ensures that any data written to the file is actually stored on the
   * underlying storage device. This is important for data durability and consistency.
   *
   * @throws IllegalStateException if the handle is closed or does not support writing
   */
  void flush();

  /**
   * Truncates the file to the specified size.
   *
   * <p>If the file is larger than the specified size, the excess data is discarded. If the file
   * is smaller, it is extended with zero bytes (behavior may be filesystem-dependent).
   *
   * @param size the new size for the file in bytes
   * @throws IllegalArgumentException if size is negative
   * @throws IllegalStateException if the handle is closed or does not support truncation
   */
  void truncate(final long size);

  /**
   * Closes the file handle and releases associated resources.
   *
   * <p>After calling this method, the handle becomes invalid and should not be used for further
   * operations. This method is idempotent - calling it multiple times has no additional effect.
   */
  @Override
  void close();
}