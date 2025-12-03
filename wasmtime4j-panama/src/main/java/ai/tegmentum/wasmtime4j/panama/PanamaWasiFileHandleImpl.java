package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import ai.tegmentum.wasmtime4j.wasi.WasiFileHandle;
import ai.tegmentum.wasmtime4j.wasi.WasiOpenFlags;
import ai.tegmentum.wasmtime4j.wasi.WasiRights;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.nio.file.Path;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Panama implementation of WasiFileHandle.
 *
 * @since 1.0.0
 */
final class PanamaWasiFileHandleImpl implements WasiFileHandle {

  private final int fileDescriptor;
  private final String path;
  private final Path resolvedPath;
  private final FileChannel channel;
  private final WasiOpenFlags openFlags;
  private final WasiRights rights;
  private final AtomicBoolean closed = new AtomicBoolean(false);
  private volatile long position = 0;

  PanamaWasiFileHandleImpl(
      final int fileDescriptor,
      final String path,
      final Path resolvedPath,
      final FileChannel channel,
      final WasiOpenFlags openFlags,
      final WasiRights rights) {
    this.fileDescriptor = fileDescriptor;
    this.path = path;
    this.resolvedPath = resolvedPath;
    this.channel = channel;
    this.openFlags = openFlags;
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
  public WasiOpenFlags getOpenFlags() {
    return openFlags;
  }

  @Override
  public boolean isValid() {
    return !closed.get() && channel.isOpen();
  }

  @Override
  public long getPosition() throws WasmException {
    ensureValid();
    try {
      return channel.position();
    } catch (final IOException e) {
      throw new WasmException("Failed to get file position", e);
    }
  }

  @Override
  public void setPosition(final long position) throws WasmException {
    if (position < 0) {
      throw new IllegalArgumentException("Position cannot be negative");
    }
    ensureValid();
    try {
      channel.position(position);
      this.position = position;
    } catch (final IOException e) {
      throw new WasmException("Failed to set file position", e);
    }
  }

  @Override
  public long getSize() throws WasmException {
    ensureValid();
    try {
      return channel.size();
    } catch (final IOException e) {
      throw new WasmException("Failed to get file size", e);
    }
  }

  @Override
  public void setSize(final long size) throws WasmException {
    if (size < 0) {
      throw new IllegalArgumentException("Size cannot be negative");
    }
    ensureValid();
    try {
      channel.truncate(size);
    } catch (final IOException e) {
      throw new WasmException("Failed to set file size", e);
    }
  }

  @Override
  public void close() {
    if (closed.compareAndSet(false, true)) {
      try {
        channel.close();
      } catch (final IOException e) {
        // Log and continue
      }
    }
  }

  FileChannel getChannel() {
    return channel;
  }

  Path getResolvedPath() {
    return resolvedPath;
  }

  private void ensureValid() throws WasmException {
    if (!isValid()) {
      throw new WasmException("File handle is not valid");
    }
  }
}
