package ai.tegmentum.wasmtime4j.jni.wasi;

/**
 * WASI event returned by polling operations.
 *
 * <p>This class represents an event returned by poll_oneoff operations.
 *
 * @since 1.0.0
 */
public final class WasiEvent {
  private final long userData;
  private final int error;
  private final int type;
  private final int nbytes;

  /**
   * Creates a new WASI event.
   *
   * @param userData the user data from the subscription
   * @param error the error code (0 if no error)
   * @param type the event type
   * @param nbytes the number of bytes available for read/write
   */
  public WasiEvent(final long userData, final int error, final int type, final int nbytes) {
    this.userData = userData;
    this.error = error;
    this.type = type;
    this.nbytes = nbytes;
  }

  /**
   * Gets the user data associated with this event.
   *
   * @return the user data
   */
  public long getUserData() {
    return userData;
  }

  /**
   * Gets the error code for this event.
   *
   * @return the error code (0 if no error)
   */
  public int getError() {
    return error;
  }

  /**
   * Gets the event type.
   *
   * @return the event type
   */
  public int getType() {
    return type;
  }

  /**
   * Gets the number of bytes available for read/write operations.
   *
   * @return the number of bytes
   */
  public int getNbytes() {
    return nbytes;
  }

  /**
   * Checks if this event indicates an error.
   *
   * @return true if there was an error, false otherwise
   */
  public boolean hasError() {
    return error != 0;
  }

  @Override
  public String toString() {
    return String.format(
        "WasiEvent{userData=%d, error=%d, type=%d, nbytes=%d}", userData, error, type, nbytes);
  }
}
