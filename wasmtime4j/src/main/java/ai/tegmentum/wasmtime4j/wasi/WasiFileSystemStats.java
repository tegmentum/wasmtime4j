package ai.tegmentum.wasmtime4j.wasi;

/**
 * File system operation statistics for WASI instances.
 *
 * @since 1.0.0
 */
public interface WasiFileSystemStats {

  /**
   * Gets the number of file read operations performed.
   *
   * @return read operation count
   */
  long getReadOperations();

  /**
   * Gets the number of file write operations performed.
   *
   * @return write operation count
   */
  long getWriteOperations();

  /**
   * Gets the total bytes read from files.
   *
   * @return bytes read
   */
  long getBytesRead();

  /**
   * Gets the total bytes written to files.
   *
   * @return bytes written
   */
  long getBytesWritten();

  /**
   * Gets the number of file open operations.
   *
   * @return file open count
   */
  long getFileOpenCount();

  /**
   * Gets the current number of open file handles.
   *
   * @return current open file count
   */
  int getCurrentOpenFiles();
}
