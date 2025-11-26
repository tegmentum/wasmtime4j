package ai.tegmentum.wasmtime4j.wasi.clocks;

/**
 * Represents timezone information for a specific point in time.
 *
 * <p>Contains the UTC offset, timezone name abbreviation, and daylight saving time status for
 * displaying time information to users.
 *
 * <p>WASI Preview 2 specification: wasi:clocks/timezone@0.2.8
 *
 * @unstable This is marked as unstable in WASI (feature = clocks-timezone)
 */
public final class TimezoneDisplay {

  private final int utcOffsetSeconds;
  private final String name;
  private final boolean inDaylightSavingTime;

  /**
   * Creates a new TimezoneDisplay.
   *
   * @param utcOffsetSeconds offset from UTC in seconds (must be less than 86,400)
   * @param name timezone name abbreviation (e.g., "PST", "UTC", "EST")
   * @param inDaylightSavingTime whether daylight saving time is currently active
   * @throws IllegalArgumentException if utcOffsetSeconds is invalid or name is null
   */
  public TimezoneDisplay(
      final int utcOffsetSeconds, final String name, final boolean inDaylightSavingTime) {
    if (Math.abs(utcOffsetSeconds) >= 86_400) {
      throw new IllegalArgumentException(
          "utcOffsetSeconds must be less than 86,400, got: " + utcOffsetSeconds);
    }
    if (name == null) {
      throw new IllegalArgumentException("name cannot be null");
    }
    this.utcOffsetSeconds = utcOffsetSeconds;
    this.name = name;
    this.inDaylightSavingTime = inDaylightSavingTime;
  }

  /**
   * Gets the UTC offset in seconds.
   *
   * <p>This is the number of seconds to add to UTC to get local time. Positive values are east of
   * UTC, negative values are west.
   *
   * @return UTC offset in seconds (less than 86,400)
   */
  public int getUtcOffsetSeconds() {
    return utcOffsetSeconds;
  }

  /**
   * Gets the timezone name abbreviation.
   *
   * <p>This is typically a short abbreviation like "PST", "UTC", "EST", etc. The exact format may
   * vary by region and implementation.
   *
   * @return timezone name abbreviation
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if daylight saving time is currently active.
   *
   * @return true if daylight saving time is active, false otherwise
   */
  public boolean isInDaylightSavingTime() {
    return inDaylightSavingTime;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof TimezoneDisplay)) {
      return false;
    }
    final TimezoneDisplay other = (TimezoneDisplay) obj;
    return utcOffsetSeconds == other.utcOffsetSeconds
        && name.equals(other.name)
        && inDaylightSavingTime == other.inDaylightSavingTime;
  }

  @Override
  public int hashCode() {
    int result = Integer.hashCode(utcOffsetSeconds);
    result = 31 * result + name.hashCode();
    result = 31 * result + Boolean.hashCode(inDaylightSavingTime);
    return result;
  }

  @Override
  public String toString() {
    return "TimezoneDisplay{"
        + "utcOffsetSeconds="
        + utcOffsetSeconds
        + ", name='"
        + name
        + '\''
        + ", inDaylightSavingTime="
        + inDaylightSavingTime
        + '}';
  }
}
