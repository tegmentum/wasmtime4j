package ai.tegmentum.wasmtime4j.config;

/**
 * Tri-state enum for configuration options that support auto-detection.
 *
 * <p>This corresponds to Wasmtime's {@code wasmtime::Enabled} enum, which allows configuration
 * options to be explicitly enabled, explicitly disabled, or set to auto-detect based on other
 * configuration settings.
 *
 * <p>Used by configuration options such as GC support, shared memory, memory protection keys, and
 * pagemap scanning.
 *
 * @since 1.1.0
 */
public enum Enabled {
  /**
   * Automatic detection based on other configuration.
   *
   * <p>The runtime will determine the appropriate setting based on related configuration options
   * and platform capabilities.
   */
  AUTO,

  /** Explicitly enabled. */
  YES,

  /** Explicitly disabled. */
  NO;

  /**
   * Converts this value to a JSON-compatible string representation.
   *
   * @return "auto", "yes", or "no"
   */
  public String toJsonValue() {
    switch (this) {
      case AUTO:
        return "auto";
      case YES:
        return "yes";
      case NO:
        return "no";
      default:
        return "auto";
    }
  }

  /**
   * Converts a boolean to an Enabled value.
   *
   * @param value the boolean value
   * @return {@link #YES} if true, {@link #NO} if false
   */
  public static Enabled fromBoolean(final boolean value) {
    return value ? YES : NO;
  }

  /**
   * Parses a string to an Enabled value.
   *
   * @param value the string value ("auto", "yes", "no", "true", "false")
   * @return the corresponding Enabled value
   * @throws IllegalArgumentException if the string is not a valid Enabled value
   */
  public static Enabled fromString(final String value) {
    if (value == null) {
      throw new IllegalArgumentException("Enabled value cannot be null");
    }
    switch (value.toLowerCase(java.util.Locale.ROOT)) {
      case "auto":
        return AUTO;
      case "yes":
      case "true":
        return YES;
      case "no":
      case "false":
        return NO;
      default:
        throw new IllegalArgumentException("Unknown Enabled value: " + value);
    }
  }
}
