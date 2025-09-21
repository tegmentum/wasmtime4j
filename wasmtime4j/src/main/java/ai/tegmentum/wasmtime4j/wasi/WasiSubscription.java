package ai.tegmentum.wasmtime4j.wasi;

import java.time.Duration;

/**
 * Represents a subscription for WASI polling operations.
 *
 * <p>A subscription describes an event that the application wants to wait for using the
 * poll_oneoff operation. Subscriptions can wait for file descriptor readiness, timer expiration,
 * or other asynchronous events.
 *
 * @since 1.0.0
 */
public final class WasiSubscription {

  /**
   * Type of subscription event.
   */
  public enum Type {
    /** Subscription for file descriptor read readiness. */
    FD_READ,
    /** Subscription for file descriptor write readiness. */
    FD_WRITE,
    /** Subscription for timer expiration (absolute time). */
    CLOCK_ABSOLUTE,
    /** Subscription for timer expiration (relative time). */
    CLOCK_RELATIVE
  }

  private final Type type;
  private final long userdata;
  private final int fileDescriptor;
  private final WasiClockType clockType;
  private final long timeout;
  private final Duration precision;

  /**
   * Creates a file descriptor read subscription.
   *
   * @param userdata user-defined data associated with this subscription
   * @param fileDescriptor the file descriptor to wait for
   * @return a new FD_READ subscription
   */
  public static WasiSubscription fdRead(final long userdata, final int fileDescriptor) {
    return new WasiSubscription(Type.FD_READ, userdata, fileDescriptor, null, 0, null);
  }

  /**
   * Creates a file descriptor write subscription.
   *
   * @param userdata user-defined data associated with this subscription
   * @param fileDescriptor the file descriptor to wait for
   * @return a new FD_WRITE subscription
   */
  public static WasiSubscription fdWrite(final long userdata, final int fileDescriptor) {
    return new WasiSubscription(Type.FD_WRITE, userdata, fileDescriptor, null, 0, null);
  }

  /**
   * Creates an absolute timer subscription.
   *
   * @param userdata user-defined data associated with this subscription
   * @param clockType the clock type to use for the timer
   * @param timeout the absolute timeout in nanoseconds
   * @param precision the timer precision (accuracy)
   * @return a new CLOCK_ABSOLUTE subscription
   */
  public static WasiSubscription clockAbsolute(
      final long userdata,
      final WasiClockType clockType,
      final long timeout,
      final Duration precision) {
    return new WasiSubscription(Type.CLOCK_ABSOLUTE, userdata, -1, clockType, timeout, precision);
  }

  /**
   * Creates a relative timer subscription.
   *
   * @param userdata user-defined data associated with this subscription
   * @param clockType the clock type to use for the timer
   * @param timeout the relative timeout in nanoseconds
   * @param precision the timer precision (accuracy)
   * @return a new CLOCK_RELATIVE subscription
   */
  public static WasiSubscription clockRelative(
      final long userdata,
      final WasiClockType clockType,
      final long timeout,
      final Duration precision) {
    return new WasiSubscription(Type.CLOCK_RELATIVE, userdata, -1, clockType, timeout, precision);
  }

  private WasiSubscription(
      final Type type,
      final long userdata,
      final int fileDescriptor,
      final WasiClockType clockType,
      final long timeout,
      final Duration precision) {
    this.type = type;
    this.userdata = userdata;
    this.fileDescriptor = fileDescriptor;
    this.clockType = clockType;
    this.timeout = timeout;
    this.precision = precision;
  }

  /**
   * Gets the type of this subscription.
   *
   * @return the subscription type
   */
  public Type getType() {
    return type;
  }

  /**
   * Gets the user-defined data associated with this subscription.
   *
   * <p>This data is returned in the corresponding event when the subscription triggers.
   *
   * @return the user data
   */
  public long getUserdata() {
    return userdata;
  }

  /**
   * Gets the file descriptor for FD_READ and FD_WRITE subscriptions.
   *
   * @return the file descriptor, or -1 if not applicable
   */
  public int getFileDescriptor() {
    return fileDescriptor;
  }

  /**
   * Gets the clock type for timer subscriptions.
   *
   * @return the clock type, or null if not applicable
   */
  public WasiClockType getClockType() {
    return clockType;
  }

  /**
   * Gets the timeout value for timer subscriptions.
   *
   * <p>For absolute timers, this is the absolute time in nanoseconds. For relative timers, this
   * is the relative time in nanoseconds.
   *
   * @return the timeout value
   */
  public long getTimeout() {
    return timeout;
  }

  /**
   * Gets the timer precision for timer subscriptions.
   *
   * <p>This indicates the acceptable accuracy for the timer. The actual timer may fire anywhere
   * within the precision window around the specified timeout.
   *
   * @return the timer precision, or null if not applicable
   */
  public Duration getPrecision() {
    return precision;
  }

  /**
   * Checks if this is a file descriptor subscription.
   *
   * @return true if this is an FD_READ or FD_WRITE subscription, false otherwise
   */
  public boolean isFileDescriptor() {
    return type == Type.FD_READ || type == Type.FD_WRITE;
  }

  /**
   * Checks if this is a timer subscription.
   *
   * @return true if this is a timer subscription, false otherwise
   */
  public boolean isTimer() {
    return type == Type.CLOCK_ABSOLUTE || type == Type.CLOCK_RELATIVE;
  }

  @Override
  public String toString() {
    switch (type) {
      case FD_READ:
        return String.format(
            "WasiSubscription{type=FD_READ, userdata=%d, fd=%d}", userdata, fileDescriptor);
      case FD_WRITE:
        return String.format(
            "WasiSubscription{type=FD_WRITE, userdata=%d, fd=%d}", userdata, fileDescriptor);
      case CLOCK_ABSOLUTE:
        return String.format(
            "WasiSubscription{type=CLOCK_ABSOLUTE, userdata=%d, clock=%s, timeout=%d, precision=%s}",
            userdata, clockType, timeout, precision);
      case CLOCK_RELATIVE:
        return String.format(
            "WasiSubscription{type=CLOCK_RELATIVE, userdata=%d, clock=%s, timeout=%d, precision=%s}",
            userdata, clockType, timeout, precision);
      default:
        return String.format("WasiSubscription{type=%s, userdata=%d}", type, userdata);
    }
  }
}