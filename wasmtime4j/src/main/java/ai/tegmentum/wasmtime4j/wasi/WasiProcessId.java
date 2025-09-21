package ai.tegmentum.wasmtime4j.wasi;

/**
 * Represents a process identifier in the WASI environment.
 *
 * <p>A WasiProcessId uniquely identifies a process within the WASI context. It can be used to wait
 * for process completion, send signals, or perform other process management operations.
 *
 * <p>Process IDs are opaque identifiers that should not be interpreted as having any particular
 * format or meaning beyond identity comparison.
 *
 * @since 1.0.0
 */
public final class WasiProcessId {

  private final long id;
  private final String name;

  /**
   * Creates a new process ID.
   *
   * @param id the numeric process identifier
   * @param name the process name or command (optional, may be null)
   */
  public WasiProcessId(final long id, final String name) {
    this.id = id;
    this.name = name;
  }

  /**
   * Creates a new process ID with only a numeric identifier.
   *
   * @param id the numeric process identifier
   */
  public WasiProcessId(final long id) {
    this(id, null);
  }

  /**
   * Gets the numeric process identifier.
   *
   * <p>This is the primary identifier for the process. The exact meaning and format depend on the
   * host system and WASI implementation.
   *
   * @return the numeric process ID
   */
  public long getId() {
    return id;
  }

  /**
   * Gets the process name if available.
   *
   * <p>This is an optional human-readable name for the process, which might be the command name or
   * executable path. May be null if not available.
   *
   * @return the process name, or null if not available
   */
  public String getName() {
    return name;
  }

  /**
   * Checks if this process ID represents a valid, active process.
   *
   * <p>This method can be used to determine if the process is still running or has been terminated.
   * The exact semantics depend on the WASI implementation.
   *
   * @return true if the process is valid and active, false otherwise
   */
  public boolean isValid() {
    // Basic validation - non-negative ID
    return id >= 0;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final WasiProcessId other = (WasiProcessId) obj;
    return id == other.id;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  @Override
  public String toString() {
    if (name != null) {
      return String.format("WasiProcessId{id=%d, name='%s'}", id, name);
    } else {
      return String.format("WasiProcessId{id=%d}", id);
    }
  }
}
