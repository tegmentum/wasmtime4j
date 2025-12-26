/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.jni.wasi;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.SeekableByteChannel;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Factory for creating test-friendly WasiFileHandle instances.
 *
 * <p>This factory creates WasiFileHandle instances with in-memory byte channels, avoiding the need
 * for mocking or actual file system access.
 *
 * <p>Note: Test handles created by this factory should not be used for actual file operations. They
 * are suitable for testing Java-side logic that requires a WasiFileHandle instance.
 */
public final class TestWasiFileHandleFactory {

  private TestWasiFileHandleFactory() {
    // Utility class
  }

  /**
   * Creates a test WasiFileHandle with the specified file descriptor.
   *
   * @param fileDescriptor the file descriptor number
   * @return a test WasiFileHandle instance
   */
  public static WasiFileHandle createTestHandle(final int fileDescriptor) {
    return createTestHandle(fileDescriptor, WasiFileOperation.READ);
  }

  /**
   * Creates a test WasiFileHandle with the specified file descriptor and operation.
   *
   * @param fileDescriptor the file descriptor number
   * @param operation the file operation type
   * @return a test WasiFileHandle instance
   */
  public static WasiFileHandle createTestHandle(
      final int fileDescriptor, final WasiFileOperation operation) {
    return createTestHandle(fileDescriptor, operation, "test-file-" + fileDescriptor + ".txt");
  }

  /**
   * Creates a test WasiFileHandle with the specified parameters.
   *
   * @param fileDescriptor the file descriptor number
   * @param operation the file operation type
   * @param fileName the file name for the path
   * @return a test WasiFileHandle instance
   */
  public static WasiFileHandle createTestHandle(
      final int fileDescriptor, final WasiFileOperation operation, final String fileName) {
    final Path path = Paths.get(System.getProperty("java.io.tmpdir"), fileName);
    final SeekableByteChannel channel = new InMemorySeekableByteChannel();

    return new WasiFileHandle(
        fileDescriptor,
        path,
        channel,
        null, // FileChannel not available for in-memory channel
        operation);
  }

  /** An in-memory implementation of SeekableByteChannel for testing. */
  private static final class InMemorySeekableByteChannel implements SeekableByteChannel {
    private final ByteArrayOutputStream buffer = new ByteArrayOutputStream();
    private long position = 0;
    private boolean open = true;

    @Override
    public int read(final ByteBuffer dst) throws IOException {
      ensureOpen();
      if (position >= buffer.size()) {
        return -1;
      }
      final byte[] data = buffer.toByteArray();
      final int remaining = (int) (data.length - position);
      final int toRead = Math.min(remaining, dst.remaining());
      dst.put(data, (int) position, toRead);
      position += toRead;
      return toRead;
    }

    @Override
    public int write(final ByteBuffer src) throws IOException {
      ensureOpen();
      final int toWrite = src.remaining();
      final byte[] data = new byte[toWrite];
      src.get(data);
      buffer.write(data);
      position += toWrite;
      return toWrite;
    }

    @Override
    public long position() throws IOException {
      ensureOpen();
      return position;
    }

    @Override
    public SeekableByteChannel position(final long newPosition) throws IOException {
      ensureOpen();
      this.position = newPosition;
      return this;
    }

    @Override
    public long size() throws IOException {
      ensureOpen();
      return buffer.size();
    }

    @Override
    public SeekableByteChannel truncate(final long size) throws IOException {
      ensureOpen();
      // Simplified implementation - just reset if truncating to 0
      if (size == 0) {
        buffer.reset();
        position = 0;
      }
      return this;
    }

    @Override
    public boolean isOpen() {
      return open;
    }

    @Override
    public void close() throws IOException {
      open = false;
    }

    private void ensureOpen() throws IOException {
      if (!open) {
        throw new IOException("Channel is closed");
      }
    }
  }
}
