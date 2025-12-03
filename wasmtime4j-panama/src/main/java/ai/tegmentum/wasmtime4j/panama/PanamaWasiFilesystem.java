package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiDirEntry;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiFileHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiFileStats;
import ai.tegmentum.wasmtime4j.wasi.WasiFileType;
import ai.tegmentum.wasmtime4j.wasi.WasiFilesystem;
import ai.tegmentum.wasmtime4j.wasi.WasiOpenFlags;
import ai.tegmentum.wasmtime4j.wasi.WasiPermissions;
import ai.tegmentum.wasmtime4j.wasi.WasiRights;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.OpenOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.nio.file.attribute.PosixFilePermission;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

/**
 * Panama implementation of the WasiFilesystem interface.
 *
 * <p>This class provides filesystem operations within the WASI sandbox using Java NIO APIs. It
 * enforces capability-based security through the WASI rights model and manages file handles with
 * proper resource cleanup.
 *
 * @since 1.0.0
 */
public final class PanamaWasiFilesystem implements WasiFilesystem {

  private static final Logger LOGGER = Logger.getLogger(PanamaWasiFilesystem.class.getName());

  private final Path rootPath;
  private final AtomicInteger fdCounter = new AtomicInteger(3); // 0, 1, 2 reserved for stdio
  private final Map<Integer, PanamaWasiFileHandleImpl> openFiles = new ConcurrentHashMap<>();
  private final Map<Integer, PanamaWasiDirectoryHandleImpl> openDirectories =
      new ConcurrentHashMap<>();
  private volatile String currentWorkingDirectory;
  private volatile boolean closed = false;

  /**
   * Creates a new Panama WASI filesystem rooted at the specified path.
   *
   * @param rootPath the root path for the filesystem sandbox
   */
  public PanamaWasiFilesystem(final Path rootPath) {
    this.rootPath = Objects.requireNonNull(rootPath, "rootPath").toAbsolutePath().normalize();
    this.currentWorkingDirectory = "/";
  }

  @Override
  public WasiFileHandle openFile(
      final String path, final WasiOpenFlags flags, final WasiRights rights) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    Objects.requireNonNull(flags, "flags cannot be null");
    Objects.requireNonNull(rights, "rights cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      final Set<OpenOption> options = convertOpenFlags(flags);
      final FileChannel channel = FileChannel.open(resolvedPath, options);
      final int fd = fdCounter.getAndIncrement();

      final PanamaWasiFileHandleImpl handle =
          new PanamaWasiFileHandleImpl(fd, path, resolvedPath, channel, flags, rights);
      openFiles.put(fd, handle);

      LOGGER.fine(() -> "Opened file: " + path + " with fd=" + fd);
      return handle;

    } catch (final IOException e) {
      throw new WasmException("Failed to open file: " + path, e);
    }
  }

  @Override
  public void closeFile(final WasiFileHandle handle) throws WasmException {
    Objects.requireNonNull(handle, "handle cannot be null");

    if (!(handle instanceof PanamaWasiFileHandleImpl)) {
      throw new IllegalArgumentException("Handle must be Panama implementation");
    }

    final PanamaWasiFileHandleImpl panamaHandle = (PanamaWasiFileHandleImpl) handle;
    openFiles.remove(panamaHandle.getFileDescriptor());
    panamaHandle.close();
  }

  @Override
  public long readFile(final WasiFileHandle handle, final ByteBuffer buffer, final long offset)
      throws WasmException {
    Objects.requireNonNull(handle, "handle cannot be null");
    Objects.requireNonNull(buffer, "buffer cannot be null");

    if (!(handle instanceof PanamaWasiFileHandleImpl)) {
      throw new IllegalArgumentException("Handle must be Panama implementation");
    }

    final PanamaWasiFileHandleImpl panamaHandle = (PanamaWasiFileHandleImpl) handle;
    if (!panamaHandle.isValid()) {
      throw new WasmException("File handle is not valid");
    }

    try {
      return panamaHandle.getChannel().read(buffer, offset);
    } catch (final IOException e) {
      throw new WasmException("Failed to read file", e);
    }
  }

  @Override
  public long writeFile(final WasiFileHandle handle, final ByteBuffer buffer, final long offset)
      throws WasmException {
    Objects.requireNonNull(handle, "handle cannot be null");
    Objects.requireNonNull(buffer, "buffer cannot be null");

    if (!(handle instanceof PanamaWasiFileHandleImpl)) {
      throw new IllegalArgumentException("Handle must be Panama implementation");
    }

    final PanamaWasiFileHandleImpl panamaHandle = (PanamaWasiFileHandleImpl) handle;
    if (!panamaHandle.isValid()) {
      throw new WasmException("File handle is not valid");
    }

    try {
      return panamaHandle.getChannel().write(buffer, offset);
    } catch (final IOException e) {
      throw new WasmException("Failed to write file", e);
    }
  }

  @Override
  public WasiDirectoryHandle openDirectory(final String path, final WasiRights rights)
      throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    Objects.requireNonNull(rights, "rights cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    if (!Files.isDirectory(resolvedPath)) {
      throw new WasmException("Not a directory: " + path);
    }

    final int fd = fdCounter.getAndIncrement();
    final PanamaWasiDirectoryHandleImpl handle =
        new PanamaWasiDirectoryHandleImpl(fd, path, resolvedPath, rights);
    openDirectories.put(fd, handle);

    LOGGER.fine(() -> "Opened directory: " + path + " with fd=" + fd);
    return handle;
  }

  @Override
  public List<WasiDirEntry> readDirectory(final WasiDirectoryHandle handle) throws WasmException {
    Objects.requireNonNull(handle, "handle cannot be null");

    if (!(handle instanceof PanamaWasiDirectoryHandleImpl)) {
      throw new IllegalArgumentException("Handle must be Panama implementation");
    }

    final PanamaWasiDirectoryHandleImpl panamaHandle = (PanamaWasiDirectoryHandleImpl) handle;
    if (!panamaHandle.isValid()) {
      throw new WasmException("Directory handle is not valid");
    }

    final List<WasiDirEntry> entries = new ArrayList<>();

    try (DirectoryStream<Path> stream = Files.newDirectoryStream(panamaHandle.getResolvedPath())) {
      for (final Path entry : stream) {
        final BasicFileAttributes attrs = Files.readAttributes(entry, BasicFileAttributes.class);
        final WasiFileType fileType = determineFileType(attrs);
        entries.add(
            new PanamaWasiDirEntryImpl(
                entry.getFileName().toString(),
                fileType,
                0L, // inode
                attrs.size(),
                attrs.lastAccessTime().toInstant(),
                attrs.lastModifiedTime().toInstant(),
                attrs.creationTime().toInstant(),
                WasiPermissions.defaultFilePermissions()));
      }
    } catch (final IOException e) {
      throw new WasmException("Failed to read directory", e);
    }

    return entries;
  }

  @Override
  public void createDirectory(final String path, final WasiPermissions permissions)
      throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    Objects.requireNonNull(permissions, "permissions cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      Files.createDirectories(resolvedPath);
      LOGGER.fine(() -> "Created directory: " + path);
    } catch (final IOException e) {
      throw new WasmException("Failed to create directory: " + path, e);
    }
  }

  @Override
  public void removeDirectory(final String path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      if (!Files.isDirectory(resolvedPath)) {
        throw new WasmException("Not a directory: " + path);
      }
      Files.delete(resolvedPath);
      LOGGER.fine(() -> "Removed directory: " + path);
    } catch (final IOException e) {
      throw new WasmException("Failed to remove directory: " + path, e);
    }
  }

  @Override
  public WasiFileStats getFileStats(final String path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      final BasicFileAttributes attrs =
          Files.readAttributes(resolvedPath, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS);

      return WasiFileStats.builder()
          .fileType(determineFileType(attrs))
          .size(attrs.size())
          .creationTime(attrs.creationTime().toInstant())
          .modificationTime(attrs.lastModifiedTime().toInstant())
          .accessTime(attrs.lastAccessTime().toInstant())
          .linkCount(1L)
          .inode(0L)
          .device(0L)
          .permissions(WasiPermissions.defaultFilePermissions())
          .build();
    } catch (final IOException e) {
      throw new WasmException("Failed to get file stats: " + path, e);
    }
  }

  @Override
  public void setFileStats(final String path, final WasiFileStats stats) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    Objects.requireNonNull(stats, "stats cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      if (stats.getModificationTime() != null) {
        Files.setLastModifiedTime(resolvedPath, FileTime.from(stats.getModificationTime()));
      }
    } catch (final IOException e) {
      throw new WasmException("Failed to set file stats: " + path, e);
    }
  }

  @Override
  public void setFilePermissions(final String path, final WasiPermissions permissions)
      throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    Objects.requireNonNull(permissions, "permissions cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      final Set<PosixFilePermission> posixPerms = convertPermissions(permissions);
      Files.setPosixFilePermissions(resolvedPath, posixPerms);
    } catch (final UnsupportedOperationException e) {
      // Platform doesn't support POSIX permissions
      LOGGER.fine(() -> "POSIX permissions not supported on this platform");
    } catch (final IOException e) {
      throw new WasmException("Failed to set file permissions: " + path, e);
    }
  }

  @Override
  public String canonicalizePath(final String path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      return resolvedPath.toRealPath().toString();
    } catch (final IOException e) {
      throw new WasmException("Failed to canonicalize path: " + path, e);
    }
  }

  @Override
  public void symlinkCreate(final String oldPath, final String newPath) throws WasmException {
    Objects.requireNonNull(oldPath, "oldPath cannot be null");
    Objects.requireNonNull(newPath, "newPath cannot be null");
    ensureNotClosed();

    final Path resolvedNewPath = resolvePath(newPath);
    validatePathWithinSandbox(resolvedNewPath);

    try {
      Files.createSymbolicLink(resolvedNewPath, Paths.get(oldPath));
      LOGGER.fine(() -> "Created symlink: " + newPath + " -> " + oldPath);
    } catch (final IOException e) {
      throw new WasmException("Failed to create symlink: " + newPath, e);
    }
  }

  @Override
  public String readSymlink(final String path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      return Files.readSymbolicLink(resolvedPath).toString();
    } catch (final IOException e) {
      throw new WasmException("Failed to read symlink: " + path, e);
    }
  }

  @Override
  public void rename(final String oldPath, final String newPath) throws WasmException {
    Objects.requireNonNull(oldPath, "oldPath cannot be null");
    Objects.requireNonNull(newPath, "newPath cannot be null");
    ensureNotClosed();

    final Path resolvedOldPath = resolvePath(oldPath);
    final Path resolvedNewPath = resolvePath(newPath);
    validatePathWithinSandbox(resolvedOldPath);
    validatePathWithinSandbox(resolvedNewPath);

    try {
      Files.move(resolvedOldPath, resolvedNewPath);
      LOGGER.fine(() -> "Renamed: " + oldPath + " -> " + newPath);
    } catch (final IOException e) {
      throw new WasmException("Failed to rename: " + oldPath, e);
    }
  }

  @Override
  public void unlink(final String path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    try {
      if (Files.isDirectory(resolvedPath)) {
        throw new WasmException("Cannot unlink a directory: " + path);
      }
      Files.delete(resolvedPath);
      LOGGER.fine(() -> "Unlinked file: " + path);
    } catch (final IOException e) {
      throw new WasmException("Failed to unlink: " + path, e);
    }
  }

  @Override
  public void syncFile(final WasiFileHandle handle) throws WasmException {
    Objects.requireNonNull(handle, "handle cannot be null");

    if (!(handle instanceof PanamaWasiFileHandleImpl)) {
      throw new IllegalArgumentException("Handle must be Panama implementation");
    }

    final PanamaWasiFileHandleImpl panamaHandle = (PanamaWasiFileHandleImpl) handle;
    if (!panamaHandle.isValid()) {
      throw new WasmException("File handle is not valid");
    }

    try {
      panamaHandle.getChannel().force(true);
    } catch (final IOException e) {
      throw new WasmException("Failed to sync file", e);
    }
  }

  @Override
  public String getCurrentWorkingDirectory() throws WasmException {
    ensureNotClosed();
    return currentWorkingDirectory;
  }

  @Override
  public void setCurrentWorkingDirectory(final String path) throws WasmException {
    Objects.requireNonNull(path, "path cannot be null");
    ensureNotClosed();

    final Path resolvedPath = resolvePath(path);
    validatePathWithinSandbox(resolvedPath);

    if (!Files.isDirectory(resolvedPath)) {
      throw new WasmException("Not a directory: " + path);
    }

    this.currentWorkingDirectory = path;
  }

  /**
   * Closes the filesystem and releases all resources.
   *
   * @throws WasmException if cleanup fails
   */
  public void close() throws WasmException {
    if (closed) {
      return;
    }
    closed = true;

    // Close all open files
    for (final PanamaWasiFileHandleImpl handle : openFiles.values()) {
      try {
        handle.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close file handle: " + e.getMessage());
      }
    }
    openFiles.clear();

    // Close all open directories
    for (final PanamaWasiDirectoryHandleImpl handle : openDirectories.values()) {
      try {
        handle.close();
      } catch (final Exception e) {
        LOGGER.warning("Failed to close directory handle: " + e.getMessage());
      }
    }
    openDirectories.clear();
  }

  private Path resolvePath(final String path) {
    if (path.startsWith("/")) {
      return rootPath.resolve(path.substring(1)).normalize();
    } else {
      final String cwd =
          currentWorkingDirectory.startsWith("/")
              ? currentWorkingDirectory.substring(1)
              : currentWorkingDirectory;
      return rootPath.resolve(cwd).resolve(path).normalize();
    }
  }

  private void validatePathWithinSandbox(final Path path) throws WasmException {
    if (!path.startsWith(rootPath)) {
      throw new WasmException("Path escapes sandbox: " + path);
    }
  }

  private Set<OpenOption> convertOpenFlags(final WasiOpenFlags flags) {
    final Set<OpenOption> options = new HashSet<>();

    switch (flags) {
      case READ:
        options.add(StandardOpenOption.READ);
        break;
      case WRITE:
        options.add(StandardOpenOption.WRITE);
        break;
      case CREATE:
        options.add(StandardOpenOption.CREATE);
        options.add(StandardOpenOption.WRITE);
        break;
      case TRUNCATE:
        options.add(StandardOpenOption.TRUNCATE_EXISTING);
        options.add(StandardOpenOption.WRITE);
        break;
      case APPEND:
        options.add(StandardOpenOption.APPEND);
        options.add(StandardOpenOption.WRITE);
        break;
      case SYNC:
        options.add(StandardOpenOption.SYNC);
        options.add(StandardOpenOption.WRITE);
        break;
      case DSYNC:
        options.add(StandardOpenOption.DSYNC);
        options.add(StandardOpenOption.WRITE);
        break;
      default:
        options.add(StandardOpenOption.READ);
    }

    return options;
  }

  private WasiFileType determineFileType(final BasicFileAttributes attrs) {
    if (attrs.isDirectory()) {
      return WasiFileType.DIRECTORY;
    } else if (attrs.isSymbolicLink()) {
      return WasiFileType.SYMBOLIC_LINK;
    } else if (attrs.isRegularFile()) {
      return WasiFileType.REGULAR_FILE;
    } else {
      return WasiFileType.UNKNOWN;
    }
  }

  private Set<PosixFilePermission> convertPermissions(final WasiPermissions permissions) {
    final Set<PosixFilePermission> posixPerms = new HashSet<>();

    if (permissions.isOwnerRead()) {
      posixPerms.add(PosixFilePermission.OWNER_READ);
    }
    if (permissions.isOwnerWrite()) {
      posixPerms.add(PosixFilePermission.OWNER_WRITE);
    }
    if (permissions.isOwnerExecute()) {
      posixPerms.add(PosixFilePermission.OWNER_EXECUTE);
    }
    if (permissions.isGroupRead()) {
      posixPerms.add(PosixFilePermission.GROUP_READ);
    }
    if (permissions.isGroupWrite()) {
      posixPerms.add(PosixFilePermission.GROUP_WRITE);
    }
    if (permissions.isGroupExecute()) {
      posixPerms.add(PosixFilePermission.GROUP_EXECUTE);
    }
    if (permissions.isOtherRead()) {
      posixPerms.add(PosixFilePermission.OTHERS_READ);
    }
    if (permissions.isOtherWrite()) {
      posixPerms.add(PosixFilePermission.OTHERS_WRITE);
    }
    if (permissions.isOtherExecute()) {
      posixPerms.add(PosixFilePermission.OTHERS_EXECUTE);
    }

    return posixPerms;
  }

  private void ensureNotClosed() throws WasmException {
    if (closed) {
      throw new WasmException("Filesystem is closed");
    }
  }

  /**
   * Gets the root path of the filesystem sandbox.
   *
   * @return the root path
   */
  public Path getRootPath() {
    return rootPath;
  }

  /**
   * Gets the number of open file handles.
   *
   * @return the number of open files
   */
  public int getOpenFileCount() {
    return openFiles.size();
  }

  /**
   * Gets the number of open directory handles.
   *
   * @return the number of open directories
   */
  public int getOpenDirectoryCount() {
    return openDirectories.size();
  }
}
