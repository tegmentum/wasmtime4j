package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * An opaque handle representing a Component Model async stream.
 *
 * <p>StreamAny is the type-erased counterpart to typed {@link StreamReader}/{@link StreamWriter}
 * pairs. It allows streams to be passed through component boundaries without static type
 * information, similar to how {@link ResourceAny} works for resources.
 *
 * @since 1.1.0
 */
public interface StreamAny extends AutoCloseable {

  /**
   * Gets the opaque native handle for this stream.
   *
   * @return the native handle
   */
  long getHandle();

  /**
   * Attempts to convert this StreamAny into a typed StreamReader.
   *
   * @return a typed StreamReader view of this stream
   * @throws WasmException if the conversion fails
   */
  StreamReader tryIntoStreamReader() throws WasmException;

  /** Closes this stream, releasing associated native resources. */
  @Override
  void close();

  /**
   * Creates a StreamAny from a typed StreamReader.
   *
   * @param reader the typed stream reader
   * @return an opaque StreamAny wrapping the reader
   * @throws IllegalArgumentException if reader is null
   */
  static StreamAny tryFromStreamReader(final StreamReader reader) {
    if (reader == null) {
      throw new IllegalArgumentException("reader cannot be null");
    }
    return new DefaultStreamAny(reader.getHandle(), reader);
  }

  /** Default implementation wrapping an opaque handle. */
  final class DefaultStreamAny implements StreamAny {

    private final long handle;
    private final StreamReader reader;
    private volatile boolean closed;

    DefaultStreamAny(final long handle, final StreamReader reader) {
      this.handle = handle;
      this.reader = reader;
    }

    @Override
    public long getHandle() {
      return handle;
    }

    @Override
    public StreamReader tryIntoStreamReader() throws WasmException {
      if (closed) {
        throw new WasmException("StreamAny is already closed");
      }
      if (reader == null) {
        throw new WasmException("StreamAny does not wrap a typed reader");
      }
      return reader;
    }

    @Override
    public void close() {
      if (!closed) {
        closed = true;
        if (reader != null) {
          reader.close();
        }
      }
    }
  }
}
