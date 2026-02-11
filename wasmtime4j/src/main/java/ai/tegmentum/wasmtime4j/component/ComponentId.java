package ai.tegmentum.wasmtime4j.component;

/**
 * Unique identifier for WebAssembly components.
 *
 * @since 1.0.0
 */
public final class ComponentId {
  private final String id;
  private final String name;

  /**
   * Creates a new component ID.
   *
   * @param id the unique identifier
   * @param name the component name
   */
  public ComponentId(final String id, final String name) {
    if (id == null || id.isEmpty()) {
      throw new IllegalArgumentException("Component ID cannot be null or empty");
    }
    this.id = id;
    this.name = name;
  }

  /**
   * Creates a new component ID with generated ID.
   *
   * @param name the component name
   */
  public ComponentId(final String name) {
    this(java.util.UUID.randomUUID().toString(), name);
  }

  /**
   * Gets the unique identifier.
   *
   * @return the unique identifier
   */
  public String getId() {
    return id;
  }

  /**
   * Gets the component name.
   *
   * @return the component name
   */
  public String getName() {
    return name;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ComponentId other = (ComponentId) obj;
    return id.equals(other.id);
  }

  @Override
  public int hashCode() {
    return id.hashCode();
  }

  @Override
  public String toString() {
    return "ComponentId{id='" + id + "', name='" + name + "'}";
  }
}
