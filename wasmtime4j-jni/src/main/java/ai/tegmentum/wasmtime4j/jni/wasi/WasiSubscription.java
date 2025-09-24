package ai.tegmentum.wasmtime4j.jni.wasi;

/**
 * WASI event subscription for polling operations.
 *
 * <p>This class represents a subscription to events for use with poll_oneoff.
 *
 * @since 1.0.0
 */
public final class WasiSubscription {
  private long userData;
  private int type;
  private int fd;
  private int flags;

  /** Creates a new WASI subscription. */
  public WasiSubscription() {
    // Default constructor
  }

  /**
   * Gets the user data associated with this subscription.
   *
   * @return the user data
   */
  public long getUserData() {
    return userData;
  }

  /**
   * Sets the user data for this subscription.
   *
   * @param userData the user data
   */
  public void setUserData(final long userData) {
    this.userData = userData;
  }

  /**
   * Gets the event type for this subscription.
   *
   * @return the event type
   */
  public int getType() {
    return type;
  }

  /**
   * Sets the event type for this subscription.
   *
   * @param type the event type
   */
  public void setType(final int type) {
    this.type = type;
  }

  /**
   * Gets the file descriptor for this subscription.
   *
   * @return the file descriptor
   */
  public int getFd() {
    return fd;
  }

  /**
   * Sets the file descriptor for this subscription.
   *
   * @param fd the file descriptor
   */
  public void setFd(final int fd) {
    this.fd = fd;
  }

  /**
   * Gets the flags for this subscription.
   *
   * @return the flags
   */
  public int getFlags() {
    return flags;
  }

  /**
   * Sets the flags for this subscription.
   *
   * @param flags the flags
   */
  public void setFlags(final int flags) {
    this.flags = flags;
  }

  /**
   * Sets up an fd_readwrite subscription.
   *
   * @param fd the file descriptor to monitor
   * @param flags the readwrite flags
   */
  public void setFdReadwrite(final int fd, final int flags) {
    this.fd = fd;
    this.flags = flags;
  }

  @Override
  public String toString() {
    return String.format(
        "WasiSubscription{userData=%d, type=%d, fd=%d, flags=%d}", userData, type, fd, flags);
  }
}
