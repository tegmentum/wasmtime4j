package ai.tegmentum.wasmtime4j.wasi;

/**
 * Represents a WASI process identifier.
 *
 * <p>Process IDs are unique identifiers assigned to WASI processes and are used for process
 * management operations such as waiting and signaling.
 *
 * @since 1.0.0
 */
public final class WasiProcessId {
  private final long id;

  private WasiProcessId(final long id) {
    this.id = id;
  }

  /**
   * Creates a new process ID from a numeric value.
   *
   * @param id the numeric process ID
   * @return a new WasiProcessId instance
   * @throws IllegalArgumentException if id is negative
   */
  public static WasiProcessId of(final long id) {
    if (id < 0) {
      throw new IllegalArgumentException("Process ID cannot be negative: " + id);
    }
    return new WasiProcessId(id);
  }

  /**
   * Gets the numeric value of this process ID.
   *
   * @return the process ID value
   */
  public long getId() {
    return id;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final WasiProcessId that = (WasiProcessId) obj;
    return id == that.id;
  }

  @Override
  public int hashCode() {
    return Long.hashCode(id);
  }

  @Override
  public String toString() {
    return "WasiProcessId{id=" + id + "}";
  }
}