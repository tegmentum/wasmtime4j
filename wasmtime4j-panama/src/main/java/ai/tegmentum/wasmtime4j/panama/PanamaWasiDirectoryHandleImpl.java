package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiDirectoryHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiRights;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Panama implementation of WasiDirectoryHandle.
 *
 * @since 1.0.0
 */
final class PanamaWasiDirectoryHandleImpl implements WasiDirectoryHandle {

  private final int fileDescriptor;
  private final String path;
  private final Path resolvedPath;
  private final WasiRights rights;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private final AtomicLong position = new AtomicLong(0);

  PanamaWasiDirectoryHandleImpl(
      final int fileDescriptor,
      final String path,
      final Path resolvedPath,
      final WasiRights rights) {
    this.fileDescriptor = fileDescriptor;
    this.path = path;
    this.resolvedPath = resolvedPath;
    this.rights = rights;
  }

  @Override
  public int getFileDescriptor() {
    return fileDescriptor;
  }

  @Override
  public String getPath() {
    return path;
  }

  @Override
  public WasiRights getRights() {
    return rights;
  }

  @Override
  public boolean isValid() {
    return !closed.get();
  }

  @Override
  public long getPosition() throws WasmException {
    ensureValid();
    return position.get();
  }

  @Override
  public void setPosition(final long newPosition) throws WasmException {
    if (newPosition < 0) {
      throw new IllegalArgumentException("Position cannot be negative");
    }
    ensureValid();
    position.set(newPosition);
  }

  @Override
  public void rewind() throws WasmException {
    ensureValid();
    position.set(0);
  }

  @Override
  public void close() {
    closed.set(true);
  }

  Path getResolvedPath() {
    return resolvedPath;
  }

  private void ensureValid() throws WasmException {
    if (!isValid()) {
      throw new WasmException("Directory handle is not valid");
    }
  }
}
