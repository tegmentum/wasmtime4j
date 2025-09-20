package ai.tegmentum.wasmtime4j.wasi.impl;

import ai.tegmentum.wasmtime4j.exception.WasiResourceException;
import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiResourceConfig;
import ai.tegmentum.wasmtime4j.wasi.WasiResourcePermissions;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Set;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

/**
 * Implementation of WasiResource for filesystem resources.
 *
 * <p>This implementation provides WASI Preview 2 filesystem resource management including file
 * operations, directory access, and path validation with proper sandboxing and permission
 * enforcement.
 *
 * @since 1.0.0
 */
public final class WasiFileResourceImpl extends WasiGenericResourceImpl {

  private static final Logger LOGGER = Logger.getLogger(WasiFileResourceImpl.class.getName());

  private final Path rootPath;
  private final Set<WasiResourcePermissions> permissions;
  private final AtomicLong bytesRead = new AtomicLong(0);
  private final AtomicLong bytesWritten = new AtomicLong(0);
  private final AtomicLong fileOperations = new AtomicLong(0);

  /**
   * Creates a new filesystem resource.
   *
   * @param id the unique resource identifier
   * @param name the resource name
   * @param config the resource configuration
   * @throws IllegalArgumentException if any parameter is null or invalid
   * @throws WasiResourceException if filesystem setup fails
   */
  public WasiFileResourceImpl(final long id, final String name, final WasiResourceConfig config) {
    super(id, name, "filesystem", config);

    if (config == null) {
      throw new IllegalArgumentException("Config cannot be null");
    }

    // Extract filesystem-specific configuration
    final String rootPathStr =
        (String)
            config
                .getProperty("root_path")
                .orElseThrow(
                    () -> new IllegalArgumentException("Filesystem root path must be specified"));

    this.rootPath = validateAndNormalizePath(rootPathStr);
    this.permissions =
        config.getPermissions().getClass().isAssignableFrom(Set.class)
            ? (Set<WasiResourcePermissions>) config.getPermissions()
            : WasiResourcePermissions.READ_ONLY;

    // Validate root path exists and is accessible
    if (!Files.exists(rootPath)) {
      throw new WasiResourceException("Root path does not exist: " + rootPath);
    }

    if (!Files.isDirectory(rootPath)) {
      throw new WasiResourceException("Root path is not a directory: " + rootPath);
    }

    LOGGER.fine("Created filesystem resource '" + name + "' with root: " + rootPath);
  }

  /**
   * Reads data from a file within this filesystem.
   *
   * @param relativePath the path relative to the root
   * @param buffer the buffer to read into
   * @param offset the offset in the file to start reading
   * @return the number of bytes read
   * @throws WasmException if the operation fails
   */
  public int readFile(final String relativePath, final byte[] buffer, final long offset)
      throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.READ);
    recordAccess();

    final Path targetPath = validateAndResolvePath(relativePath);

    try {
      if (!Files.exists(targetPath)) {
        throw new WasiResourceException("File does not exist: " + relativePath);
      }

      if (!Files.isRegularFile(targetPath)) {
        throw new WasiResourceException("Path is not a regular file: " + relativePath);
      }

      // In a real implementation, this would use proper file I/O with offset support
      final byte[] fileData = Files.readAllBytes(targetPath);
      final int availableData = Math.max(0, fileData.length - (int) offset);
      final int bytesToRead = Math.min(buffer.length, availableData);

      if (bytesToRead > 0) {
        System.arraycopy(fileData, (int) offset, buffer, 0, bytesToRead);
      }

      bytesRead.addAndGet(bytesToRead);
      fileOperations.incrementAndGet();

      LOGGER.fine("Read " + bytesToRead + " bytes from " + relativePath);
      return bytesToRead;
    } catch (final IOException e) {
      throw new WasiResourceException("Failed to read file: " + relativePath, e);
    }
  }

  /**
   * Writes data to a file within this filesystem.
   *
   * @param relativePath the path relative to the root
   * @param buffer the data to write
   * @param offset the offset in the file to start writing
   * @return the number of bytes written
   * @throws WasmException if the operation fails
   */
  public int writeFile(final String relativePath, final byte[] buffer, final long offset)
      throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.WRITE);
    recordAccess();

    final Path targetPath = validateAndResolvePath(relativePath);

    try {
      // Create parent directories if they don't exist and we have create permission
      final Path parentPath = targetPath.getParent();
      if (parentPath != null && !Files.exists(parentPath)) {
        ensurePermission(WasiResourcePermissions.CREATE);
        Files.createDirectories(parentPath);
      }

      // In a real implementation, this would support proper offset-based writing
      if (offset == 0) {
        Files.write(targetPath, buffer);
      } else {
        throw new WasiResourceException("Offset-based writing not yet implemented");
      }

      bytesWritten.addAndGet(buffer.length);
      fileOperations.incrementAndGet();

      LOGGER.fine("Wrote " + buffer.length + " bytes to " + relativePath);
      return buffer.length;
    } catch (final IOException e) {
      throw new WasiResourceException("Failed to write file: " + relativePath, e);
    }
  }

  /**
   * Deletes a file or directory within this filesystem.
   *
   * @param relativePath the path relative to the root
   * @throws WasmException if the operation fails
   */
  public void deleteFile(final String relativePath) throws WasmException {
    ensureValid();
    ensurePermission(WasiResourcePermissions.DELETE);
    recordAccess();

    final Path targetPath = validateAndResolvePath(relativePath);

    try {
      if (!Files.exists(targetPath)) {
        throw new WasiResourceException("Path does not exist: " + relativePath);
      }

      if (Files.isDirectory(targetPath)) {
        // Only delete empty directories for safety
        try {
          Files.delete(targetPath);
        } catch (final IOException e) {
          throw new WasiResourceException(
              "Directory not empty or deletion failed: " + relativePath, e);
        }
      } else {
        Files.delete(targetPath);
      }

      fileOperations.incrementAndGet();
      LOGGER.fine("Deleted " + relativePath);
    } catch (final IOException e) {
      throw new WasiResourceException("Failed to delete: " + relativePath, e);
    }
  }

  /**
   * Gets filesystem-specific statistics.
   *
   * @return filesystem usage statistics
   */
  public FileSystemStats getFileSystemStats() {
    return new FileSystemStats() {
      @Override
      public long getBytesRead() {
        return bytesRead.get();
      }

      @Override
      public long getBytesWritten() {
        return bytesWritten.get();
      }

      @Override
      public long getFileOperations() {
        return fileOperations.get();
      }

      @Override
      public String getRootPath() {
        return rootPath.toString();
      }
    };
  }

  @Override
  public Object invoke(final String operation, final Object... parameters) throws WasmException {
    if (operation == null) {
      throw new IllegalArgumentException("Operation cannot be null");
    }

    // Handle filesystem-specific operations first
    switch (operation.toLowerCase()) {
      case "read_file":
        if (parameters.length < 3) {
          throw new IllegalArgumentException(
              "read_file requires path, buffer, and offset parameters");
        }
        return readFile((String) parameters[0], (byte[]) parameters[1], (Long) parameters[2]);

      case "write_file":
        if (parameters.length < 3) {
          throw new IllegalArgumentException(
              "write_file requires path, buffer, and offset parameters");
        }
        return writeFile((String) parameters[0], (byte[]) parameters[1], (Long) parameters[2]);

      case "delete_file":
        if (parameters.length < 1) {
          throw new IllegalArgumentException("delete_file requires path parameter");
        }
        deleteFile((String) parameters[0]);
        return null;

      case "get_stats":
        return getFileSystemStats();

      case "get_root_path":
        return rootPath.toString();

      default:
        // Delegate to parent for common operations
        return super.invoke(operation, parameters);
    }
  }

  @Override
  protected void performCleanup() {
    super.performCleanup();
    // Additional filesystem-specific cleanup could go here
    LOGGER.fine("Filesystem resource cleanup completed for " + getName());
  }

  /**
   * Validates and normalizes a path string.
   *
   * @param pathStr the path string to validate
   * @return the normalized path
   * @throws IllegalArgumentException if the path is invalid
   */
  private Path validateAndNormalizePath(final String pathStr) {
    if (pathStr == null || pathStr.trim().isEmpty()) {
      throw new IllegalArgumentException("Path cannot be null or empty");
    }

    final Path path = Paths.get(pathStr.trim()).toAbsolutePath().normalize();

    // Basic path traversal protection
    if (path.toString().contains("..")) {
      throw new IllegalArgumentException("Path traversal not allowed: " + pathStr);
    }

    return path;
  }

  /**
   * Validates and resolves a relative path against the root.
   *
   * @param relativePath the relative path to resolve
   * @return the resolved absolute path
   * @throws WasiResourceException if the path is invalid or unsafe
   */
  private Path validateAndResolvePath(final String relativePath) throws WasiResourceException {
    if (relativePath == null || relativePath.trim().isEmpty()) {
      throw new WasiResourceException("Relative path cannot be null or empty");
    }

    // Remove leading slashes to ensure relative resolution
    final String cleanPath = relativePath.replaceAll("^/+", "");

    final Path resolved = rootPath.resolve(cleanPath).normalize();

    // Ensure the resolved path is still within the root directory (sandbox validation)
    if (!resolved.startsWith(rootPath)) {
      throw new WasiResourceException("Path escapes sandbox: " + relativePath);
    }

    return resolved;
  }

  /**
   * Ensures the resource has the specified permission.
   *
   * @param required the required permission
   * @throws WasiResourceException if permission is not granted
   */
  private void ensurePermission(final WasiResourcePermissions required)
      throws WasiResourceException {
    if (!permissions.contains(required)) {
      throw new WasiResourceException("Permission denied: " + required.getName());
    }
  }

  /** Interface for filesystem-specific statistics. */
  public interface FileSystemStats {
    long getBytesRead();

    long getBytesWritten();

    long getFileOperations();

    String getRootPath();
  }
}
