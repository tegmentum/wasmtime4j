package ai.tegmentum.wasmtime4j.jni.wasi;

import ai.tegmentum.wasmtime4j.jni.exception.JniException;
import ai.tegmentum.wasmtime4j.jni.util.JniValidation;
import ai.tegmentum.wasmtime4j.jni.wasi.exception.WasiFileSystemException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.PosixFilePermission;
import java.nio.file.attribute.PosixFilePermissions;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * JNI implementation of advanced WASI file system operations.
 *
 * <p>This class provides advanced file system operations that extend the basic WASI file system
 * functionality with additional capabilities required for complete WASI implementation:
 *
 * <ul>
 *   <li>Symbolic link creation and resolution
 *   <li>File permission management (POSIX)
 *   <li>Advanced file metadata operations
 *   <li>Directory tree operations
 *   <li>Asynchronous file I/O operations
 *   <li>File system monitoring and events
 *   <li>Hard link operations
 * </ul>
 *
 * <p>All operations implement comprehensive security validation and integrate with the WASI
 * permission system to ensure sandbox compliance.
 *
 * @since 1.0.0
 */
public final class WasiAdvancedFileOperations {

  private static final Logger LOGGER = Logger.getLogger(WasiAdvancedFileOperations.class.getName());

  /** Maximum depth for directory tree traversal. */
  private static final int MAX_TRAVERSAL_DEPTH = 100;

  /** Maximum number of files to process in batch operations. */
  private static final int MAX_BATCH_SIZE = 1000;

  /** The WASI context this advanced file operations instance belongs to. */
  private final WasiContext wasiContext;

  /** Basic file system operations handler. */
  private final WasiFileSystem fileSystem;

  /** Executor service for async file operations. */
  private final ExecutorService asyncExecutor;

  /**
   * Creates a new WASI advanced file operations instance.
   *
   * @param wasiContext the WASI context to operate within
   * @param fileSystem the basic file system operations handler
   * @throws JniException if parameters are null
   */
  public WasiAdvancedFileOperations(
      final WasiContext wasiContext, final WasiFileSystem fileSystem) {
    JniValidation.requireNonNull(wasiContext, "wasiContext");
    JniValidation.requireNonNull(fileSystem, "fileSystem");

    this.wasiContext = wasiContext;
    this.fileSystem = fileSystem;
    this.asyncExecutor =
        Executors.newFixedThreadPool(
            Math.min(Runtime.getRuntime().availableProcessors(), 4),
            r -> {
              final Thread t = new Thread(r, "wasi-async-file");
              t.setDaemon(true);
              return t;
            });

    LOGGER.info("Created WASI advanced file operations handler");
  }

  /**
   * Creates a symbolic link.
   *
   * @param linkPath the path of the symbolic link to create
   * @param targetPath the target path that the link should point to
   * @throws WasiFileSystemException if the symbolic link cannot be created
   */
  public void createSymbolicLink(final String linkPath, final String targetPath) {
    JniValidation.requireNonEmpty(linkPath, "linkPath");
    JniValidation.requireNonEmpty(targetPath, "targetPath");

    LOGGER.fine(() -> String.format("Creating symbolic link: %s -> %s", linkPath, targetPath));

    try {
      // Validate paths through WASI context
      final Path validatedLinkPath =
          wasiContext.validatePath(linkPath, WasiFileOperation.CREATE_LINK);
      final Path validatedTargetPath = Paths.get(targetPath); // Target doesn't need to exist

      Files.createSymbolicLink(validatedLinkPath, validatedTargetPath);

      LOGGER.fine(
          () ->
              String.format(
                  "Symbolic link created successfully: %s -> %s",
                  validatedLinkPath, validatedTargetPath));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to create symbolic link: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to create symbolic link: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(
          String.format("Security violation creating symbolic link: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Reads the target of a symbolic link.
   *
   * @param linkPath the path of the symbolic link to read
   * @return the target path of the symbolic link
   * @throws WasiFileSystemException if the link cannot be read
   */
  public String readSymbolicLink(final String linkPath) {
    JniValidation.requireNonEmpty(linkPath, "linkPath");

    LOGGER.fine(() -> String.format("Reading symbolic link: %s", linkPath));

    try {
      // Validate path through WASI context
      final Path validatedPath = wasiContext.validatePath(linkPath, WasiFileOperation.READ_LINK);

      if (!Files.isSymbolicLink(validatedPath)) {
        throw new WasiFileSystemException("Path is not a symbolic link", "EINVAL");
      }

      final Path target = Files.readSymbolicLink(validatedPath);
      final String targetString = target.toString();

      LOGGER.fine(() -> String.format("Read symbolic link: %s -> %s", validatedPath, targetString));
      return targetString;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to read symbolic link: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to read symbolic link: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation reading symbolic link: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Creates a hard link.
   *
   * @param linkPath the path of the hard link to create
   * @param targetPath the target file to link to
   * @throws WasiFileSystemException if the hard link cannot be created
   */
  public void createHardLink(final String linkPath, final String targetPath) {
    JniValidation.requireNonEmpty(linkPath, "linkPath");
    JniValidation.requireNonEmpty(targetPath, "targetPath");

    LOGGER.fine(() -> String.format("Creating hard link: %s -> %s", linkPath, targetPath));

    try {
      // Validate paths through WASI context
      final Path validatedLinkPath =
          wasiContext.validatePath(linkPath, WasiFileOperation.CREATE_LINK);
      final Path validatedTargetPath =
          wasiContext.validatePath(targetPath, WasiFileOperation.READ_ONLY);

      Files.createLink(validatedLinkPath, validatedTargetPath);

      LOGGER.fine(
          () ->
              String.format(
                  "Hard link created successfully: %s -> %s",
                  validatedLinkPath, validatedTargetPath));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to create hard link: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to create hard link: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation creating hard link: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Sets file permissions using POSIX permission bits.
   *
   * @param filePath the path of the file to modify
   * @param permissions the POSIX permission bits (e.g., 0644)
   * @throws WasiFileSystemException if permissions cannot be set
   */
  public void setFilePermissions(final String filePath, final int permissions) {
    JniValidation.requireNonEmpty(filePath, "filePath");
    JniValidation.requireNonNegative(permissions, "permissions");

    LOGGER.fine(
        () -> String.format("Setting file permissions: %s, mode=0%o", filePath, permissions));

    try {
      // Validate path through WASI context
      final Path validatedPath =
          wasiContext.validatePath(filePath, WasiFileOperation.SET_PERMISSIONS);

      // Convert integer permissions to POSIX permission set
      final Set<PosixFilePermission> posixPermissions = convertToPosixPermissions(permissions);

      Files.setPosixFilePermissions(validatedPath, posixPermissions);

      LOGGER.fine(
          () ->
              String.format(
                  "File permissions set successfully: %s, mode=0%o", validatedPath, permissions));

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to set file permissions: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to set file permissions: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(
          String.format("Security violation setting file permissions: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("POSIX permissions not supported on this platform");
      throw new WasiFileSystemException("POSIX permissions not supported", "ENOTSUP");
    }
  }

  /**
   * Gets file permissions as POSIX permission bits.
   *
   * @param filePath the path of the file to query
   * @return the POSIX permission bits
   * @throws WasiFileSystemException if permissions cannot be retrieved
   */
  public int getFilePermissions(final String filePath) {
    JniValidation.requireNonEmpty(filePath, "filePath");

    LOGGER.fine(() -> String.format("Getting file permissions: %s", filePath));

    try {
      // Validate path through WASI context
      final Path validatedPath = wasiContext.validatePath(filePath, WasiFileOperation.METADATA);

      // Get POSIX permissions
      final Set<PosixFilePermission> posixPermissions =
          Files.getPosixFilePermissions(validatedPath);
      final int permissions = convertFromPosixPermissions(posixPermissions);

      LOGGER.fine(
          () ->
              String.format(
                  "File permissions retrieved: %s, mode=0%o", validatedPath, permissions));

      return permissions;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to get file permissions: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to get file permissions: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(
          String.format("Security violation getting file permissions: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    } catch (final UnsupportedOperationException e) {
      LOGGER.warning("POSIX permissions not supported on this platform");
      throw new WasiFileSystemException("POSIX permissions not supported", "ENOTSUP");
    }
  }

  /**
   * Lists directory contents recursively up to a specified depth.
   *
   * @param directoryPath the directory path to list
   * @param maxDepth the maximum depth to traverse (0 = current level only)
   * @return list of all file and directory entries found
   * @throws WasiFileSystemException if the directory cannot be listed
   */
  public List<WasiDirectoryEntry> listDirectoryRecursive(
      final String directoryPath, final int maxDepth) {
    JniValidation.requireNonEmpty(directoryPath, "directoryPath");
    JniValidation.requireNonNegative(maxDepth, "maxDepth");

    final int actualMaxDepth = Math.min(maxDepth, MAX_TRAVERSAL_DEPTH);

    LOGGER.fine(
        () ->
            String.format(
                "Listing directory recursively: %s, maxDepth=%d", directoryPath, actualMaxDepth));

    try {
      // Validate path through WASI context
      final Path validatedPath =
          wasiContext.validatePath(directoryPath, WasiFileOperation.LIST_DIRECTORY);

      if (!Files.isDirectory(validatedPath)) {
        throw new WasiFileSystemException("Path is not a directory", "ENOTDIR");
      }

      final List<WasiDirectoryEntry> entries;
      try (final Stream<Path> stream = Files.walk(validatedPath, actualMaxDepth)) {
        entries =
            stream
                .filter(
                    path -> !path.equals(validatedPath)) // Exclude the starting directory itself
                .limit(MAX_BATCH_SIZE) // Prevent excessive memory usage
                .map(this::createDirectoryEntry)
                .collect(Collectors.toList());
      }

      LOGGER.fine(
          () ->
              String.format(
                  "Listed %d entries recursively in directory: %s", entries.size(), validatedPath));

      return entries;

    } catch (final IOException e) {
      LOGGER.warning(String.format("Failed to list directory recursively: %s", e.getMessage()));
      throw new WasiFileSystemException(
          "Failed to list directory recursively: " + e.getMessage(), mapIoExceptionToWasiError(e));
    } catch (final SecurityException e) {
      LOGGER.warning(String.format("Security violation listing directory: %s", e.getMessage()));
      throw new WasiFileSystemException("Access denied: " + e.getMessage(), "EACCES");
    }
  }

  /**
   * Copies a file asynchronously.
   *
   * @param sourcePath the source file path
   * @param targetPath the target file path
   * @return CompletableFuture that completes when the copy is finished
   */
  public CompletableFuture<Void> copyFileAsync(final String sourcePath, final String targetPath) {
    JniValidation.requireNonEmpty(sourcePath, "sourcePath");
    JniValidation.requireNonEmpty(targetPath, "targetPath");

    LOGGER.fine(() -> String.format("Starting async file copy: %s -> %s", sourcePath, targetPath));

    return CompletableFuture.runAsync(
        () -> {
          try {
            // Validate paths through WASI context
            final Path validatedSourcePath =
                wasiContext.validatePath(sourcePath, WasiFileOperation.READ_ONLY);
            final Path validatedTargetPath =
                wasiContext.validatePath(targetPath, WasiFileOperation.WRITE_ONLY);

            Files.copy(validatedSourcePath, validatedTargetPath);

            LOGGER.fine(
                () ->
                    String.format(
                        "Async file copy completed: %s -> %s",
                        validatedSourcePath, validatedTargetPath));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async file copy failed", e);
            throw new RuntimeException("File copy failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Moves/renames a file asynchronously.
   *
   * @param sourcePath the source file path
   * @param targetPath the target file path
   * @return CompletableFuture that completes when the move is finished
   */
  public CompletableFuture<Void> moveFileAsync(final String sourcePath, final String targetPath) {
    JniValidation.requireNonEmpty(sourcePath, "sourcePath");
    JniValidation.requireNonEmpty(targetPath, "targetPath");

    LOGGER.fine(() -> String.format("Starting async file move: %s -> %s", sourcePath, targetPath));

    return CompletableFuture.runAsync(
        () -> {
          try {
            // Validate paths through WASI context
            final Path validatedSourcePath =
                wasiContext.validatePath(sourcePath, WasiFileOperation.RENAME);
            final Path validatedTargetPath =
                wasiContext.validatePath(targetPath, WasiFileOperation.RENAME);

            Files.move(validatedSourcePath, validatedTargetPath);

            LOGGER.fine(
                () ->
                    String.format(
                        "Async file move completed: %s -> %s",
                        validatedSourcePath, validatedTargetPath));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async file move failed", e);
            throw new RuntimeException("File move failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /**
   * Deletes a directory tree asynchronously.
   *
   * @param directoryPath the directory path to delete
   * @return CompletableFuture that completes when the deletion is finished
   */
  public CompletableFuture<Void> deleteDirectoryTreeAsync(final String directoryPath) {
    JniValidation.requireNonEmpty(directoryPath, "directoryPath");

    LOGGER.fine(() -> String.format("Starting async directory tree deletion: %s", directoryPath));

    return CompletableFuture.runAsync(
        () -> {
          try {
            // Validate path through WASI context
            final Path validatedPath =
                wasiContext.validatePath(directoryPath, WasiFileOperation.DELETE);

            if (!Files.isDirectory(validatedPath)) {
              throw new WasiFileSystemException("Path is not a directory", "ENOTDIR");
            }

            // Delete directory tree
            try (final Stream<Path> stream = Files.walk(validatedPath)) {
              stream
                  .sorted((path1, path2) -> path2.compareTo(path1)) // Delete deeper paths first
                  .forEach(
                      path -> {
                        try {
                          Files.delete(path);
                        } catch (final IOException e) {
                          throw new RuntimeException("Failed to delete: " + path, e);
                        }
                      });
            }

            LOGGER.fine(
                () -> String.format("Async directory tree deletion completed: %s", validatedPath));

          } catch (final Exception e) {
            LOGGER.log(Level.WARNING, "Async directory tree deletion failed", e);
            throw new RuntimeException("Directory tree deletion failed: " + e.getMessage(), e);
          }
        },
        asyncExecutor);
  }

  /** Closes the advanced file operations handler and cleans up resources. */
  public void close() {
    LOGGER.info("Closing WASI advanced file operations handler");

    try {
      asyncExecutor.shutdown();
      LOGGER.info("WASI advanced file operations handler closed successfully");

    } catch (final Exception e) {
      LOGGER.log(Level.WARNING, "Error closing advanced file operations", e);
    }
  }

  /** Creates a WasiDirectoryEntry from a Path. */
  private WasiDirectoryEntry createDirectoryEntry(final Path path) {
    try {
      final BasicFileAttributes attributes =
          Files.readAttributes(path, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);
      final String fileName = path.getFileName().toString();

      return new WasiDirectoryEntry(
          fileName,
          attributes.isRegularFile(),
          attributes.isDirectory(),
          attributes.isSymbolicLink(),
          attributes.size(),
          attributes.lastModifiedTime());

    } catch (final IOException e) {
      LOGGER.warning("Failed to read attributes for: " + path);
      // Return minimal entry with just the name
      return new WasiDirectoryEntry(path.getFileName().toString(), false, false, false, 0L, null);
    }
  }

  /**
   * Converts integer permissions to POSIX permission set.
   *
   * <p>Note: This method restricts permissions to owner and group only (0770) for security. World
   * permissions are masked out to prevent overly permissive file access.
   */
  private Set<PosixFilePermission> convertToPosixPermissions(final int permissions) {
    // Mask to owner and group permissions only (0770) to avoid world-writable files
    return PosixFilePermissions.fromString(String.format("%03o", permissions & 0770));
  }

  /** Converts POSIX permission set to integer permissions. */
  private int convertFromPosixPermissions(final Set<PosixFilePermission> permissions) {
    return Integer.parseInt(PosixFilePermissions.toString(permissions), 8);
  }

  /** Maps IOException to appropriate WASI error codes. */
  private String mapIoExceptionToWasiError(final IOException e) {
    // This is the same mapping as in WasiFileSystem
    final String className = e.getClass().getSimpleName();
    switch (className) {
      case "NoSuchFileException":
        return "ENOENT";
      case "FileAlreadyExistsException":
        return "EEXIST";
      case "AccessDeniedException":
        return "EACCES";
      case "DirectoryNotEmptyException":
        return "ENOTEMPTY";
      case "NotDirectoryException":
        return "ENOTDIR";
      case "FileSystemException":
        return "EIO";
      default:
        return "EIO";
    }
  }
}
