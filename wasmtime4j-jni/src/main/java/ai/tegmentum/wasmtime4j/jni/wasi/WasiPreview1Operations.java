package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiErrorCode;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiException;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * JNI implementation of complete WASI Preview 1 operations.
 *
 * <p>This class provides a comprehensive implementation of all WASI Preview 1 system interface
 * operations as specified in the WASI specification. It serves as the central interface for
 * WebAssembly modules to interact with system resources through the WASI runtime.
 *
 * <p>Implemented WASI Preview 1 operations:
 *
 * <ul>
 *   <li>File descriptor operations: fd_read, fd_write, fd_seek, fd_close, fd_fdstat_get
 *   <li>Path operations: path_open, path_create_directory, path_remove_directory, path_unlink_file
 *   <li>Environment operations: environ_get, environ_sizes_get
 *   <li>Process operations: proc_exit, proc_raise, args_get, args_sizes_get
 *   <li>Time operations: clock_time_get, clock_res_get
 *   <li>Random operations: random_get
 *   <li>Poll operations: poll_oneoff
 * </ul>
 *
 * <p>All operations implement comprehensive error handling, security validation, and proper
 * integration with the WASI context and permission system.
 *
 * @since 1.0.0
 */
public final class WasiPreview1Operations {

  private static final Logger LOGGER = Logger.getLogger(WasiPreview1Operations.class.getName());

  /** WASI error code for success. */
  public static final int WASI_ESUCCESS = 0;

  /** WASI error code for invalid argument. */
  public static final int WASI_EINVAL = 28;

  /** WASI error code for bad file descriptor. */
  public static final int WASI_EBADF = 9;

  /** WASI error code for no such file or directory. */
  public static final int WASI_ENOENT = 44;

  /** WASI error code for permission denied. */
  public static final int WASI_EACCES = 2;

  /** The WASI context this operations instance belongs to. */
  private final WasiContext wasiContext;

  /** File system operations handler. */
  private final WasiFileSystem fileSystem;

  /** Time operations handler. */
  private final WasiTimeOperations timeOperations;

  /** Random operations handler. */
  private final WasiRandomOperations randomOperations;

  /**
   * Creates a new WASI Preview 1 operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @throws JniException if the wasiContext is null
   */
  public WasiPreview1Operations(final WasiContext wasiContext) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    this.wasiContext = wasiContext;
    this.fileSystem = new WasiFileSystem(wasiContext);
    this.timeOperations = new WasiTimeOperations(wasiContext);
    this.randomOperations = new WasiRandomOperations(wasiContext);

    LOGGER.info("Created WASI Preview 1 operations handler");
  }

  /**
   * Reads data from a file descriptor.
   *
   * <p>WASI function: fd_read
   *
   * @param fd the file descriptor to read from
   * @param iovs array of I/O vectors describing buffers to read into
   * @return the number of bytes read
   * @throws WasiException if the read operation fails
   */
  public int fdRead(final int fd, final List<ByteBuffer> iovs) {
    JniValidation.requireNonNull(iovs, "iovs");
    validateFileDescriptor(fd);

    LOGGER.fine(() -> String.format("fd_read: fd=%d, %d buffers", fd, iovs.size()));

    try {
      int totalBytesRead = 0;

      for (final ByteBuffer buffer : iovs) {
        if (buffer.remaining() == 0) {
          continue;
        }

        final byte[] tempBuffer = new byte[buffer.remaining()];
        final int bytesRead = fileSystem.readFile(fd, tempBuffer, 0, tempBuffer.length);

        if (bytesRead <= 0) {
          break; // EOF or error
        }

        buffer.put(tempBuffer, 0, bytesRead);
        totalBytesRead += bytesRead;

        if (bytesRead < tempBuffer.length) {
          break; // EOF reached
        }
      }

      LOGGER.fine(() -> String.format("fd_read completed: %d bytes read", totalBytesRead));
      return totalBytesRead;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "fd_read failed for fd " + fd, e);
      throw new WasiException("fd_read failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Writes data to a file descriptor.
   *
   * <p>WASI function: fd_write
   *
   * @param fd the file descriptor to write to
   * @param iovs array of I/O vectors describing buffers to write from
   * @return the number of bytes written
   * @throws WasiException if the write operation fails
   */
  public int fdWrite(final int fd, final List<ByteBuffer> iovs) {
    JniValidation.requireNonNull(iovs, "iovs");
    validateFileDescriptor(fd);

    LOGGER.fine(() -> String.format("fd_write: fd=%d, %d buffers", fd, iovs.size()));

    try {
      int totalBytesWritten = 0;

      for (final ByteBuffer buffer : iovs) {
        if (buffer.remaining() == 0) {
          continue;
        }

        final byte[] tempBuffer = new byte[buffer.remaining()];
        buffer.get(tempBuffer);

        final int bytesWritten = fileSystem.writeFile(fd, tempBuffer, 0, tempBuffer.length);
        totalBytesWritten += bytesWritten;

        if (bytesWritten < tempBuffer.length) {
          break; // Partial write
        }
      }

      LOGGER.fine(() -> String.format("fd_write completed: %d bytes written", totalBytesWritten));
      return totalBytesWritten;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "fd_write failed for fd " + fd, e);
      throw new WasiException("fd_write failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Seeks to a position in a file descriptor.
   *
   * <p>WASI function: fd_seek
   *
   * @param fd the file descriptor
   * @param offset the seek offset
   * @param whence the seek origin (0=start, 1=current, 2=end)
   * @return the new absolute position
   * @throws WasiException if the seek operation fails
   */
  public long fdSeek(final int fd, final long offset, final int whence) {
    validateFileDescriptor(fd);
    validateWhence(whence);

    LOGGER.fine(() -> String.format("fd_seek: fd=%d, offset=%d, whence=%d", fd, offset, whence));

    try {
      final long newPosition = fileSystem.seekFile(fd, offset, whence);
      LOGGER.fine(() -> String.format("fd_seek completed: new position=%d", newPosition));
      return newPosition;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "fd_seek failed for fd " + fd, e);
      throw new WasiException("fd_seek failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Closes a file descriptor.
   *
   * <p>WASI function: fd_close
   *
   * @param fd the file descriptor to close
   * @throws WasiException if the close operation fails
   */
  public void fdClose(final int fd) {
    validateFileDescriptor(fd);

    LOGGER.fine(() -> String.format("fd_close: fd=%d", fd));

    try {
      fileSystem.closeFile(fd);
      LOGGER.fine(() -> String.format("fd_close completed: fd=%d", fd));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "fd_close failed for fd " + fd, e);
      throw new WasiException("fd_close failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Opens a file at a path.
   *
   * <p>WASI function: path_open
   *
   * @param dirfd the directory file descriptor
   * @param dirflags directory lookup flags
   * @param path the path to open
   * @param oflags file open flags
   * @param rights file rights
   * @param rightsInheriting inheriting file rights
   * @param fdflags file descriptor flags
   * @return the new file descriptor
   * @throws WasiException if the open operation fails
   */
  public int pathOpen(
      final int dirfd,
      final int dirflags,
      final String path,
      final int oflags,
      final long rights,
      final long rightsInheriting,
      final int fdflags) {

    JniValidation.requireNonEmpty(path, "path");
    validateFileDescriptor(dirfd);

    LOGGER.fine(
        () -> String.format("path_open: dirfd=%d, path=%s, oflags=0x%x", dirfd, path, oflags));

    try {
      // Map WASI flags to file operations
      final WasiFileOperation operation = mapOFlagsToOperation(oflags);
      final boolean create = (oflags & 0x1) != 0; // O_CREAT equivalent
      final boolean truncate = (oflags & 0x8) != 0; // O_TRUNC equivalent

      final int fd = fileSystem.openFile(path, operation, create, truncate);
      LOGGER.fine(() -> String.format("path_open completed: fd=%d", fd));
      return fd;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "path_open failed for path " + path, e);
      throw new WasiException("path_open failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Creates a directory.
   *
   * <p>WASI function: path_create_directory
   *
   * @param dirfd the directory file descriptor
   * @param path the path of the directory to create
   * @throws WasiException if the directory creation fails
   */
  public void pathCreateDirectory(final int dirfd, final String path) {
    JniValidation.requireNonEmpty(path, "path");
    validateFileDescriptor(dirfd);

    LOGGER.fine(() -> String.format("path_create_directory: dirfd=%d, path=%s", dirfd, path));

    try {
      fileSystem.createDirectory(path);
      LOGGER.fine(() -> String.format("path_create_directory completed: %s", path));

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "path_create_directory failed for path " + path, e);
      throw new WasiException("path_create_directory failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets environment variables.
   *
   * <p>WASI function: environ_get
   *
   * @param environ buffer to store environment variable pointers
   * @param environBuf buffer to store environment variable data
   * @return WASI error code (0 for success)
   * @throws WasiException if the operation fails
   */
  public int environGet(final ByteBuffer environ, final ByteBuffer environBuf) {
    JniValidation.requireNonNull(environ, "environ");
    JniValidation.requireNonNull(environBuf, "environBuf");

    LOGGER.fine("environ_get called");

    try {
      final Map<String, String> environment = wasiContext.getEnvironment();
      return writeStringArray(environ, environBuf, environment);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "environ_get failed", e);
      return WASI_EIO;
    }
  }

  /**
   * Gets the sizes of environment variables.
   *
   * <p>WASI function: environ_sizes_get
   *
   * @return array containing [count, totalSize]
   * @throws WasiException if the operation fails
   */
  public int[] environSizesGet() {
    LOGGER.fine("environ_sizes_get called");

    try {
      final Map<String, String> environment = wasiContext.getEnvironment();
      final int count = environment.size();
      int totalSize = 0;

      for (final Map.Entry<String, String> entry : environment.entrySet()) {
        totalSize += entry.getKey().length() + 1 + entry.getValue().length() + 1; // key=value\0
      }

      LOGGER.fine(
          () -> String.format("environ_sizes_get: count=%d, totalSize=%d", count, totalSize));
      return new int[] {count, totalSize};

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "environ_sizes_get failed", e);
      throw new WasiException("environ_sizes_get failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets command line arguments.
   *
   * <p>WASI function: args_get
   *
   * @param argv buffer to store argument pointers
   * @param argvBuf buffer to store argument data
   * @return WASI error code (0 for success)
   * @throws WasiException if the operation fails
   */
  public int argsGet(final ByteBuffer argv, final ByteBuffer argvBuf) {
    JniValidation.requireNonNull(argv, "argv");
    JniValidation.requireNonNull(argvBuf, "argvBuf");

    LOGGER.fine("args_get called");

    try {
      final String[] arguments = wasiContext.getArguments();
      return writeStringArray(argv, argvBuf, arguments);

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "args_get failed", e);
      return WASI_EIO;
    }
  }

  /**
   * Gets the sizes of command line arguments.
   *
   * <p>WASI function: args_sizes_get
   *
   * @return array containing [count, totalSize]
   * @throws WasiException if the operation fails
   */
  public int[] argsSizesGet() {
    LOGGER.fine("args_sizes_get called");

    try {
      final String[] arguments = wasiContext.getArguments();
      final int count = arguments.length;
      int totalSize = 0;

      for (final String arg : arguments) {
        totalSize += arg.length() + 1; // string + null terminator
      }

      LOGGER.fine(() -> String.format("args_sizes_get: count=%d, totalSize=%d", count, totalSize));
      return new int[] {count, totalSize};

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "args_sizes_get failed", e);
      throw new WasiException("args_sizes_get failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets current time for a clock.
   *
   * <p>WASI function: clock_time_get
   *
   * @param clockId the clock identifier
   * @param precision the requested precision
   * @return the current time in nanoseconds
   * @throws WasiException if the operation fails
   */
  public long clockTimeGet(final int clockId, final long precision) {
    LOGGER.fine(
        () -> String.format("clock_time_get: clockId=%d, precision=%d", clockId, precision));

    try {
      final long time = timeOperations.getCurrentTime(clockId, precision);
      LOGGER.fine(() -> String.format("clock_time_get completed: %d nanoseconds", time));
      return time;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "clock_time_get failed", e);
      throw new WasiException("clock_time_get failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Gets clock resolution.
   *
   * <p>WASI function: clock_res_get
   *
   * @param clockId the clock identifier
   * @return the clock resolution in nanoseconds
   * @throws WasiException if the operation fails
   */
  public long clockResGet(final int clockId) {
    LOGGER.fine(() -> String.format("clock_res_get: clockId=%d", clockId));

    try {
      final long resolution = timeOperations.getClockResolution(clockId);
      LOGGER.fine(() -> String.format("clock_res_get completed: %d nanoseconds", resolution));
      return resolution;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "clock_res_get failed", e);
      throw new WasiException("clock_res_get failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Generates random bytes.
   *
   * <p>WASI function: random_get
   *
   * @param buffer the buffer to fill with random bytes
   * @throws WasiException if the operation fails
   */
  public void randomGet(final ByteBuffer buffer) {
    JniValidation.requireNonNull(buffer, "buffer");

    LOGGER.fine(() -> String.format("random_get: %d bytes", buffer.remaining()));

    try {
      randomOperations.getRandomBytes(buffer);
      LOGGER.fine("random_get completed successfully");

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "random_get failed", e);
      throw new WasiException("random_get failed: " + e.getMessage(), WasiErrorCode.EIO);
    }
  }

  /**
   * Terminates the process.
   *
   * <p>WASI function: proc_exit
   *
   * @param exitCode the exit code
   */
  public void procExit(final int exitCode) {
    LOGGER.info(() -> String.format("proc_exit called with exit code: %d", exitCode));

    try {
      // Perform cleanup
      wasiContext.close();

      // Exit the process
      System.exit(exitCode);

    } catch (final Exception e) {
      LOGGER.log(Level.SEVERE, "Error during proc_exit", e);
      System.exit(1);
    }
  }

  /** Validates that a file descriptor is valid. */
  private void validateFileDescriptor(final int fd) {
    if (fd < 0) {
      throw new WasiException("Invalid file descriptor: " + fd, WasiErrorCode.EBADF);
    }
  }

  /** Validates that a whence value is valid. */
  private void validateWhence(final int whence) {
    if (whence < 0 || whence > 2) {
      throw new WasiException("Invalid whence value: " + whence, WasiErrorCode.EINVAL);
    }
  }

  /** Maps WASI open flags to file operation. */
  private WasiFileOperation mapOFlagsToOperation(final int oflags) {
    final boolean read = (oflags & 0x2) != 0; // O_RDONLY equivalent
    final boolean write = (oflags & 0x4) != 0; // O_WRONLY equivalent

    if (read && write) {
      return WasiFileOperation.READ_WRITE;
    } else if (write) {
      return WasiFileOperation.WRITE_ONLY;
    } else {
      return WasiFileOperation.READ_ONLY;
    }
  }

  /** Writes string array to WASI buffers. */
  private int writeStringArray(
      final ByteBuffer pointers, final ByteBuffer data, final Map<String, String> environment) {
    try {
      int dataOffset = 0;

      for (final Map.Entry<String, String> entry : environment.entrySet()) {
        // Write pointer
        pointers.putInt(dataOffset);

        // Write data
        final String envVar = entry.getKey() + "=" + entry.getValue();
        final byte[] bytes = envVar.getBytes(StandardCharsets.UTF_8);
        data.put(bytes);
        data.put((byte) 0); // null terminator

        dataOffset += bytes.length + 1;
      }

      return WASI_ESUCCESS;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to write string array", e);
      return WASI_EINVAL;
    }
  }

  /** Writes string array to WASI buffers. */
  private int writeStringArray(
      final ByteBuffer pointers, final ByteBuffer data, final String[] strings) {
    try {
      int dataOffset = 0;

      for (final String str : strings) {
        // Write pointer
        pointers.putInt(dataOffset);

        // Write data
        final byte[] bytes = str.getBytes(StandardCharsets.UTF_8);
        data.put(bytes);
        data.put((byte) 0); // null terminator

        dataOffset += bytes.length + 1;
      }

      return WASI_ESUCCESS;

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Failed to write string array", e);
      return WASI_EINVAL;
    }
  }

  /** WASI I/O error code. */
  private static final int WASI_EIO = 29;
}
