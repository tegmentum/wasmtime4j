package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryEntry;
import ai.tegmentum.wasmtime4j.wasi.WasiFileMetadata;
import ai.tegmentum.wasmtime4j.wasi.WasiFileOperation;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.AccessDeniedException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.logging.Logger;

/**
 * JNI implementation of WASI file system operations with comprehensive sandbox security.
 *
 * <p>This class provides production-ready WASI file system operations through JNI integration,
 * including:
 *
 * <ul>
 *   <li>Java NIO integration for efficient file operations
 *   <li>Comprehensive sandbox security validation
 *   <li>Configurable directory access controls
 *   <li>Proper file handle management and resource cleanup
 *   <li>Support for both blocking and non-blocking I/O operations
 *   <li>Thread-safe file system operations
 * </ul>
 *
 * <p>All file operations are validated against sandbox permissions and security policies before
 * execution to prevent unauthorized system access.
 *
 * @since 1.0.0
 */
public final class WasiFileSystem {

  private static final Logger LOGGER = Logger.getLogger(WasiFileSystem.class.getName());

  /** Maximum number of open file handles per WASI context. */
  private static final int MAX_OPEN_FILES = 1024;

  /** Default buffer size for I/O operations. */
  private static final int DEFAULT_BUFFER_SIZE = 8192;

  /** The WASI context that owns this file system. */
  private final WasiContext wasiContext;

  /** Lock for thread-safe file handle management. */
  private final ReadWriteLock fileHandleLock = new ReentrantReadWriteLock();

  /** Open file handles tracking for resource management. */
  private final Map<Integer, WasiFileHandle> openHandles = new ConcurrentHashMap<>();

  /** Next file descriptor number. */
  private int nextFileDescriptor = 3; // Start after stdin/stdout/stderr

  /**
   * Creates a new WASI file system for the specified context.
   *
   * @param wasiContext the WASI context that owns this file system
   * @throws JniException if the file system cannot be initialized
   */
  public WasiFileSystem(final WasiContext wasiContext) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    this.wasiContext = wasiContext;

    LOGGER.info("Created WASI file system for context");
  }

  /**
   * Opens a file for reading and/or writing with the specified options.
   *
   * @param path the file path to open
   * @param operation the file operation type
   * @param createIfNotExists whether to create the file if it doesn't exist
   * @param truncate whether to truncate the file on open
   * @return the file descriptor for the opened file
   * @throws WasiFileSystemException if the file cannot be opened
   */
  public int openFile(
      final String path,
      final WasiFileOperation operation,
      final boolean createIfNotExists,
      final boolean truncate) {
    JniValidation.requireNonEmpty(path, "path");
    JniValidation.requireNonNull(operation, "operation");

    LOGGER.fine(
        String.format(
            "Opening file: %s, operation: %s, create: %s, truncate: %s",
            path, operation, createIfNotExists, truncate));

    try {
      // Validate path access through WASI context
      final Path validatedPath = wasiContext.validatePath(path, operation);

      // Check file handle limits
      fileHandleLock.writeLock().lock();
      try {
        if (openHandles.size() >= MAX_OPEN_FILES) {
          throw new WasiFileSystemException("Too many open files", "EMFILE");
        }

        // Build open options based on operation and flags
        final Set<StandardOpenOption> openOptions =
            buildOpenOptions(operation, createIfNotExists, truncate);

        // Open the file using Java NIO
        final SeekableByteChannel channel = Files.newByteChannel(validatedPath, openOptions);
        final FileChannel fileChannel =
            channel instanceof FileChannel ? (FileChannel) channel : null;

        // Create file handle wrapper
        final WasiFileHandle handle =
            new WasiFileHandle(nextFileDescriptor, validatedPath, channel, fileChannel, operation);

        openHandles.put(nextFileDescriptor, handle);
        final int fileDescriptor = nextFileDescriptor++;

        LOGGER.fine(
            String.format("File opened successfully: %s, fd: %d", validatedPath, fileDescriptor));

        return fileDescriptor;

      } finally {
        fileHandleLock.writeLock().unlock();
      }

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to open file: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to open file: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation opening file: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Reads data from an open file into a buffer.
   *
   * @param fileDescriptor the file descriptor
   * @param buffer the buffer to read data into
   * @param offset the offset in the buffer to start reading
   * @param length the maximum number of bytes to read
   * @return the number of bytes actually read, or -1 if end of file
   * @throws WasiFileSystemException if the read operation fails
   */
  public int readFile(
      final int fileDescriptor, final byte[] buffer, final int offset, final int length) {
    JniValidation.requireNonNull(buffer, "buffer");
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requirePositive(length, "length");

    if (offset + length > buffer.length) {
      throw new IllegalArgumentException("Buffer overflow: offset + length > buffer.length");
    }

    LOGGER.fine(
        String.format("Reading from file descriptor: %d, length: %d", fileDescriptor, length));

    final WasiFileHandle handle = getFileHandle(fileDescriptor);
    if (!handle.getOperation().requiresReadAccess()) {
      throw new WasiFileSystemException("File not open for reading", "EBADF");
    }

    try {
      final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
      final int bytesRead = handle.getChannel().read(byteBuffer);

      LOGGER.fine(
          String.format("Read %d bytes from file descriptor: %d", bytesRead, fileDescriptor));

      return bytesRead;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to read from file: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to read file: " + e.getMessage(), mapIoExceptionToWasiError(e));
    }
  }

  /**
   * Writes data from a buffer to an open file.
   *
   * @param fileDescriptor the file descriptor
   * @param buffer the buffer containing data to write
   * @param offset the offset in the buffer to start writing from
   * @param length the number of bytes to write
   * @return the number of bytes actually written
   * @throws WasiFileSystemException if the write operation fails
   */
  public int writeFile(
      final int fileDescriptor, final byte[] buffer, final int offset, final int length) {
    JniValidation.requireNonNull(buffer, "buffer");
    JniValidation.requireNonNegative(offset, "offset");
    JniValidation.requirePositive(length, "length");

    if (offset + length > buffer.length) {
      throw new IllegalArgumentException("Buffer overflow: offset + length > buffer.length");
    }

    LOGGER.fine(
        String.format("Writing to file descriptor: %d, length: %d", fileDescriptor, length));

    final WasiFileHandle handle = getFileHandle(fileDescriptor);
    if (!handle.getOperation().requiresWriteAccess()) {
      throw new WasiFileSystemException("File not open for writing", "EBADF");
    }

    try {
      final ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, offset, length);
      final int bytesWritten = handle.getChannel().write(byteBuffer);

      LOGGER.fine(
          String.format("Wrote %d bytes to file descriptor: %d", bytesWritten, fileDescriptor));

      return bytesWritten;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to write to file: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to write file: " + e.getMessage(), mapIoExceptionToWasiError(e));
    }
  }

  /**
   * Seeks to a specific position in an open file.
   *
   * @param fileDescriptor the file descriptor
   * @param position the position to seek to
   * @param whence the seek origin (0=start, 1=current, 2=end)
   * @return the new absolute position in the file
   * @throws WasiFileSystemException if the seek operation fails
   */
  public long seekFile(final int fileDescriptor, final long position, final int whence) {
    LOGGER.fine(
        String.format(
            "Seeking file descriptor: %d, position: %d, whence: %d",
            fileDescriptor, position, whence));

    final WasiFileHandle handle = getFileHandle(fileDescriptor);
    final FileChannel fileChannel = handle.getFileChannel();

    if (fileChannel == null) {
      throw new WasiFileSystemException("File does not support seeking", "ESPIPE");
    }

    try {
      final long newPosition;
      switch (whence) {
        case 0: // SEEK_SET
          newPosition = fileChannel.position(position).position();
          break;
        case 1: // SEEK_CUR
          newPosition = fileChannel.position(fileChannel.position() + position).position();
          break;
        case 2: // SEEK_END
          newPosition = fileChannel.position(fileChannel.size() + position).position();
          break;
        default:
          throw new WasiFileSystemException("Invalid seek whence: " + whence, "EINVAL");
      }

      LOGGER.fine(
          String.format(
              "Seeked to position: %d in file descriptor: %d", newPosition, fileDescriptor));

      return newPosition;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to seek file: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to seek file: " + e.getMessage(), mapIoExceptionToWasiError(e));
    }
  }

  /**
   * Synchronizes file contents to disk.
   *
   * @param fileDescriptor the file descriptor to sync
   * @param dataOnly whether to sync only data (not metadata)
   * @throws WasiFileSystemException if the sync operation fails
   */
  public void syncFile(final int fileDescriptor, final boolean dataOnly) {
    LOGGER.fine(
        String.format("Syncing file descriptor: %d, data only: %s", fileDescriptor, dataOnly));

    final WasiFileHandle handle = getFileHandle(fileDescriptor);
    final FileChannel fileChannel = handle.getFileChannel();

    if (fileChannel == null) {
      // For non-FileChannel implementations, we can't sync
      LOGGER.fine("File does not support syncing, operation ignored");
      return;
    }

    try {
      fileChannel.force(!dataOnly); // force(false) syncs data only, force(true) syncs metadata too

      LOGGER.fine(String.format("File synced successfully: %d", fileDescriptor));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to sync file: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to sync file: " + e.getMessage(), mapIoExceptionToWasiError(e));
    }
  }

  /**
   * Truncates a file to the specified size.
   *
   * @param fileDescriptor the file descriptor
   * @param size the new file size
   * @throws WasiFileSystemException if the truncate operation fails
   */
  public void truncateFile(final int fileDescriptor, final long size) {
    JniValidation.requireNonNegative(size, "size");

    LOGGER.fine(String.format("Truncating file descriptor: %d to size: %d", fileDescriptor, size));

    final WasiFileHandle handle = getFileHandle(fileDescriptor);
    if (!handle.getOperation().requiresWriteAccess()) {
      throw new WasiFileSystemException("File not open for writing", "EBADF");
    }

    final FileChannel fileChannel = handle.getFileChannel();
    if (fileChannel == null) {
      throw new WasiFileSystemException("File does not support truncation", "EINVAL");
    }

    try {
      fileChannel.truncate(size);

      LOGGER.fine(String.format("File truncated successfully: %d", fileDescriptor));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to truncate file: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to truncate file: " + e.getMessage(), mapIoExceptionToWasiError(e));
    }
  }

  /**
   * Closes an open file and releases its resources.
   *
   * @param fileDescriptor the file descriptor to close
   * @throws WasiFileSystemException if the close operation fails
   */
  public void closeFile(final int fileDescriptor) {
    LOGGER.fine(String.format("Closing file descriptor: %d", fileDescriptor));

    fileHandleLock.writeLock().lock();
    try {
      final WasiFileHandle handle = openHandles.remove(fileDescriptor);
      if (handle == null) {
        throw new WasiFileSystemException("Invalid file descriptor", "EBADF");
      }

      handle.close();

      LOGGER.fine(String.format("File closed successfully: %d", fileDescriptor));

    } finally {
      fileHandleLock.writeLock().unlock();
    }
  }

  /**
   * Gets file metadata (stat) for the specified path.
   *
   * @param path the file path
   * @return the file metadata
   * @throws WasiFileSystemException if the metadata cannot be retrieved
   */
  public WasiFileMetadata getFileMetadata(final String path) {
    JniValidation.requireNonEmpty(path, "path");

    LOGGER.fine(String.format("Getting metadata for path: %s", path));

    try {
      // Validate path access
      final Path validatedPath = wasiContext.validatePath(path, WasiFileOperation.METADATA);

      // Read basic file attributes
      final BasicFileAttributes attributes =
          Files.readAttributes(validatedPath, BasicFileAttributes.class);

      final WasiFileMetadata metadata =
          new WasiFileMetadata(
              attributes.size(),
              attributes.lastModifiedTime(),
              attributes.lastAccessTime(),
              attributes.creationTime(),
              attributes.isRegularFile(),
              attributes.isDirectory(),
              attributes.isSymbolicLink(),
              Files.isReadable(validatedPath),
              Files.isWritable(validatedPath),
              Files.isExecutable(validatedPath));

      LOGGER.fine(String.format("Retrieved metadata for: %s", validatedPath));

      return metadata;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to get file metadata: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to get file metadata: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation getting metadata: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Lists the contents of a directory.
   *
   * @param path the directory path
   * @return list of directory entries
   * @throws WasiFileSystemException if the directory cannot be listed
   */
  public List<WasiDirectoryEntry> listDirectory(final String path) {
    JniValidation.requireNonEmpty(path, "path");

    LOGGER.fine(String.format("Listing directory: %s", path));

    try {
      // Validate path access
      final Path validatedPath = wasiContext.validatePath(path, WasiFileOperation.LIST_DIRECTORY);

      if (!Files.isDirectory(validatedPath)) {
        throw new WasiFileSystemException("Path is not a directory", "ENOTDIR");
      }

      final List<WasiDirectoryEntry> entries = new ArrayList<>();

      try (final DirectoryStream<Path> stream = Files.newDirectoryStream(validatedPath)) {
        for (final Path entry : stream) {
          final String entryName = entry.getFileName().toString();
          final BasicFileAttributes attributes =
              Files.readAttributes(entry, BasicFileAttributes.class);

          final WasiDirectoryEntry directoryEntry =
              new WasiDirectoryEntry(
                  entryName,
                  attributes.isRegularFile(),
                  attributes.isDirectory(),
                  attributes.isSymbolicLink(),
                  attributes.size(),
                  attributes.lastModifiedTime());

          entries.add(directoryEntry);
        }
      }

      LOGGER.fine(
          String.format("Listed %d entries in directory: %s", entries.size(), validatedPath));

      return entries;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to list directory: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to list directory: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation listing directory: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Creates a new directory.
   *
   * @param path the directory path to create
   * @throws WasiFileSystemException if the directory cannot be created
   */
  public void createDirectory(final String path) {
    JniValidation.requireNonEmpty(path, "path");

    LOGGER.fine(String.format("Creating directory: %s", path));

    try {
      // Validate path access
      final Path validatedPath = wasiContext.validatePath(path, WasiFileOperation.CREATE_DIRECTORY);

      Files.createDirectory(validatedPath);

      LOGGER.fine(String.format("Directory created successfully: %s", validatedPath));

    } catch (final FileAlreadyExistsException e) {
      LOGGER.fine(String.format("Directory already exists: %s", path));
      throw new WasiFileSystemException("Directory already exists", "EEXIST");
    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to create directory: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to create directory: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation creating directory: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Removes a file or empty directory.
   *
   * @param path the path to remove
   * @throws WasiFileSystemException if the file or directory cannot be removed
   */
  public void removeFileOrDirectory(final String path) {
    JniValidation.requireNonEmpty(path, "path");

    LOGGER.fine(String.format("Removing file or directory: %s", path));

    try {
      // Validate path access
      final Path validatedPath = wasiContext.validatePath(path, WasiFileOperation.DELETE);

      Files.delete(validatedPath);

      LOGGER.fine(String.format("File or directory removed successfully: %s", validatedPath));

    } catch (final NoSuchFileException e) {
      LOGGER.fine(String.format("File or directory does not exist: %s", path));
      throw new WasiFileSystemException("File or directory does not exist", "ENOENT");
    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to remove file or directory: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to remove file or directory: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation removing file: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Renames or moves a file or directory.
   *
   * @param oldPath the current path
   * @param newPath the new path
   * @throws WasiFileSystemException if the rename operation fails
   */
  public void renameFileOrDirectory(final String oldPath, final String newPath) {
    JniValidation.requireNonEmpty(oldPath, "oldPath");
    JniValidation.requireNonEmpty(newPath, "newPath");

    LOGGER.fine(String.format("Renaming from: %s to: %s", oldPath, newPath));

    try {
      // Validate both paths
      final Path validatedOldPath = wasiContext.validatePath(oldPath, WasiFileOperation.RENAME);
      final Path validatedNewPath = wasiContext.validatePath(newPath, WasiFileOperation.RENAME);

      Files.move(validatedOldPath, validatedNewPath);

      LOGGER.fine(
          String.format(
              "File renamed successfully from: %s to: %s", validatedOldPath, validatedNewPath));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to rename file: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to rename file: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation renaming file: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Sets file timestamps.
   *
   * @param path the file path
   * @param lastAccessTime the last access time (null to leave unchanged)
   * @param lastModifiedTime the last modified time (null to leave unchanged)
   * @throws WasiFileSystemException if the timestamps cannot be set
   */
  public void setFileTimes(
      final String path, final FileTime lastAccessTime, final FileTime lastModifiedTime) {
    JniValidation.requireNonEmpty(path, "path");

    LOGGER.fine(String.format("Setting file times for: %s", path));

    try {
      // Validate path access
      final Path validatedPath = wasiContext.validatePath(path, WasiFileOperation.SET_TIMES);

      if (lastAccessTime != null) {
        Files.setAttribute(validatedPath, "basic:lastAccessTime", lastAccessTime);
      }

      if (lastModifiedTime != null) {
        Files.setAttribute(validatedPath, "basic:lastModifiedTime", lastModifiedTime);
      }

      LOGGER.fine(String.format("File times set successfully for: %s", validatedPath));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to set file times: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to set file times: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation setting file times: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /** Closes all open file handles and releases resources. */
  public void closeAll() {
    LOGGER.fine("Closing all file handles");

    fileHandleLock.writeLock().lock();
    try {
      for (final WasiFileHandle handle : openHandles.values()) {
        try {
          handle.close();
        } catch (final Exception e) {
          LOGGER.warning(String.format("Error closing file handle: %s", e.getMessage()));
        }
      }
      openHandles.clear();
      nextFileDescriptor = 3;

      LOGGER.info("All file handles closed successfully");

    } finally {
      fileHandleLock.writeLock().unlock();
    }
  }

  /**
   * Gets the number of currently open file handles.
   *
   * @return the number of open file handles
   */
  public int getOpenFileCount() {
    fileHandleLock.readLock().lock();
    try {
      return openHandles.size();
    } finally {
      fileHandleLock.readLock().unlock();
    }
  }

  /** Gets a file handle by descriptor, throwing an exception if not found. */
  private WasiFileHandle getFileHandle(final int fileDescriptor) {
    fileHandleLock.readLock().lock();
    try {
      final WasiFileHandle handle = openHandles.get(fileDescriptor);
      if (handle == null) {
        throw new WasiFileSystemException("Invalid file descriptor: " + fileDescriptor, "EBADF");
      }
      return handle;
    } finally {
      fileHandleLock.readLock().unlock();
    }
  }

  /** Builds StandardOpenOptions based on operation and flags. */
  private Set<StandardOpenOption> buildOpenOptions(
      final WasiFileOperation operation, final boolean createIfNotExists, final boolean truncate) {
    final Set<StandardOpenOption> options = ConcurrentHashMap.newKeySet();

    if (operation.requiresReadAccess()) {
      options.add(StandardOpenOption.READ);
    }

    if (operation.requiresWriteAccess()) {
      options.add(StandardOpenOption.WRITE);
    }

    if (createIfNotExists) {
      options.add(StandardOpenOption.CREATE);
    }

    if (truncate) {
      options.add(StandardOpenOption.TRUNCATE_EXISTING);
    }

    return options;
  }

  /** Maps IOException to appropriate WASI error codes. */
  private String mapIoExceptionToWasiError(final IOException e) {
    if (e instanceof NoSuchFileException) {
      return "ENOENT";
    } else if (e instanceof FileAlreadyExistsException) {
      return "EEXIST";
    } else if (e instanceof AccessDeniedException) {
      return "EACCES";
    } else {
      return "EIO";
    }
  }
}
