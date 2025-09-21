package ai.tegmentum.wasmtime4j.wasi;

/**
 * Represents an event returned by WASI polling operations.
 *
 * <p>A WasiEvent is returned when a subscription becomes ready during a poll_oneoff operation.
 * It contains the user data from the original subscription and information about what happened.
 *
 * @since 1.0.0
 */
public final class WasiEvent {

  /**
   * Type of event that occurred.
   */
  public enum Type {
    /** File descriptor became ready for reading. */
    FD_READ,
    /** File descriptor became ready for writing. */
    FD_WRITE,
    /** Timer expired. */
    CLOCK,
    /** An error occurred. */
    ERROR
  }

  private final Type type;
  private final long userdata;
  private final WasiEventError error;
  private final long fdReadWriteFlags;
  private final long fdReadWriteBytes;

  /**
   * Creates a new file descriptor read event.
   *
   * @param userdata the user data from the original subscription
   * @param flags flags indicating the readiness state
   * @param bytes number of bytes available for reading
   * @return a new FD_READ event
   */
  public static WasiEvent fdRead(final long userdata, final long flags, final long bytes) {
    return new WasiEvent(Type.FD_READ, userdata, null, flags, bytes);
  }

  /**
   * Creates a new file descriptor write event.
   *
   * @param userdata the user data from the original subscription
   * @param flags flags indicating the readiness state
   * @param bytes number of bytes that can be written
   * @return a new FD_WRITE event
   */
  public static WasiEvent fdWrite(final long userdata, final long flags, final long bytes) {
    return new WasiEvent(Type.FD_WRITE, userdata, null, flags, bytes);
  }

  /**
   * Creates a new clock (timer) event.
   *
   * @param userdata the user data from the original subscription
   * @return a new CLOCK event
   */
  public static WasiEvent clock(final long userdata) {
    return new WasiEvent(Type.CLOCK, userdata, null, 0, 0);
  }

  /**
   * Creates a new error event.
   *
   * @param userdata the user data from the original subscription
   * @param error the error that occurred
   * @return a new ERROR event
   */
  public static WasiEvent error(final long userdata, final WasiEventError error) {
    return new WasiEvent(Type.ERROR, userdata, error, 0, 0);
  }

  private WasiEvent(
      final Type type,
      final long userdata,
      final WasiEventError error,
      final long fdReadWriteFlags,
      final long fdReadWriteBytes) {
    this.type = type;
    this.userdata = userdata;
    this.error = error;
    this.fdReadWriteFlags = fdReadWriteFlags;
    this.fdReadWriteBytes = fdReadWriteBytes;
  }

  /**
   * Gets the type of this event.
   *
   * @return the event type
   */
  public Type getType() {
    return type;
  }

  /**
   * Gets the user data from the original subscription.
   *
   * <p>This is the same value that was provided when creating the subscription that triggered
   * this event.
   *
   * @return the user data
   */
  public long getUserdata() {
    return userdata;
  }

  /**
   * Gets the error information for ERROR events.
   *
   * @return the error information, or null if this is not an ERROR event
   */
  public WasiEventError getError() {
    return error;
  }

  /**
   * Gets the flags for file descriptor events.
   *
   * <p>These flags provide additional information about the readiness state of the file
   * descriptor.
   *
   * @return the flags for FD_READ/FD_WRITE events, or 0 for other event types
   */
  public long getFdReadWriteFlags() {
    return fdReadWriteFlags;
  }

  /**
   * Gets the byte count for file descriptor events.
   *
   * <p>For FD_READ events, this indicates the number of bytes available for reading. For FD_WRITE
   * events, this indicates the number of bytes that can be written without blocking.
   *
   * @return the byte count for FD_READ/FD_WRITE events, or 0 for other event types
   */
  public long getFdReadWriteBytes() {
    return fdReadWriteBytes;
  }

  /**
   * Checks if this is a file descriptor event.
   *
   * @return true if this is an FD_READ or FD_WRITE event, false otherwise
   */
  public boolean isFileDescriptor() {
    return type == Type.FD_READ || type == Type.FD_WRITE;
  }

  /**
   * Checks if this is a timer event.
   *
   * @return true if this is a CLOCK event, false otherwise
   */
  public boolean isTimer() {
    return type == Type.CLOCK;
  }

  /**
   * Checks if this is an error event.
   *
   * @return true if this is an ERROR event, false otherwise
   */
  public boolean isError() {
    return type == Type.ERROR;
  }

  @Override
  public String toString() {
    switch (type) {
      case FD_READ:
        return String.format(
            "WasiEvent{type=FD_READ, userdata=%d, flags=0x%x, bytes=%d}",
            userdata, fdReadWriteFlags, fdReadWriteBytes);
      case FD_WRITE:
        return String.format(
            "WasiEvent{type=FD_WRITE, userdata=%d, flags=0x%x, bytes=%d}",
            userdata, fdReadWriteFlags, fdReadWriteBytes);
      case CLOCK:
        return String.format("WasiEvent{type=CLOCK, userdata=%d}", userdata);
      case ERROR:
        return String.format("WasiEvent{type=ERROR, userdata=%d, error=%s}", userdata, error);
      default:
        return String.format("WasiEvent{type=%s, userdata=%d}", type, userdata);
    }
  }
}