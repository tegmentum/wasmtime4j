package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Objects;
import java.util.function.Consumer;

/** Wrapper for output stream that tracks bytes written and provides buffering. */
final class StreamingOutputStream extends OutputStream {
  private final OutputStream delegate;
  private final byte[] buffer;
  private final Consumer<Long> bytesWrittenCallback;
  private int bufferPosition = 0;
  private long totalBytesWritten = 0;

  public StreamingOutputStream(
      final OutputStream delegate,
      final StreamingConfiguration config,
      final Consumer<Long> bytesWrittenCallback) {
    this.delegate = Objects.requireNonNull(delegate, "delegate cannot be null");
    this.buffer = new byte[config.getBufferSize()];
    this.bytesWrittenCallback = bytesWrittenCallback;
  }

  @Override
  public void write(final int b) throws IOException {
    buffer[bufferPosition++] = (byte) b;
    if (bufferPosition >= buffer.length) {
      flush();
    }
  }

  @Override
  public void write(final byte[] b, final int off, final int len) throws IOException {
    if (len >= buffer.length) {
      // Large write - flush buffer and write directly
      flush();
      delegate.write(b, off, len);
      updateBytesWritten(len);
    } else if (bufferPosition + len >= buffer.length) {
      // Would overflow buffer - flush first
      flush();
      System.arraycopy(b, off, buffer, bufferPosition, len);
      bufferPosition += len;
    } else {
      // Fits in buffer
      System.arraycopy(b, off, buffer, bufferPosition, len);
      bufferPosition += len;
    }
  }

  @Override
  public void flush() throws IOException {
    if (bufferPosition > 0) {
      delegate.write(buffer, 0, bufferPosition);
      updateBytesWritten(bufferPosition);
      bufferPosition = 0;
    }
    delegate.flush();
  }

  @Override
  public void close() throws IOException {
    flush();
    delegate.close();
  }

  public long getBytesWritten() {
    return totalBytesWritten;
  }

  private void updateBytesWritten(final long bytes) {
    totalBytesWritten += bytes;
    if (bytesWrittenCallback != null) {
      bytesWrittenCallback.accept(bytes);
    }
  }
}
