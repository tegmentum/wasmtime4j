package ai.tegmentum.wasmtime4j;

import java.util.Map;

/**
 * Metadata information for a WebAssembly component.
 *
 * <p>This class contains descriptive information about a component including its name, version,
 * description, and custom properties.
 *
 * @since 1.0.0
 */
public final class ComponentMetadata {

  private final String name;
  private final ComponentVersion version;
  private final String description;
  private final String author;
  private final String license;
  private final Map<String, String> properties;
  private final long createdTimestamp;

  /**
   * Creates new component metadata.
   *
   * @param name the component name
   * @param version the component version
   * @param description the component description
   */
  public ComponentMetadata(
      final String name, final ComponentVersion version, final String description) {
    this(name, version, description, null, null, Map.of());
  }

  /**
   * Creates new component metadata with all fields.
   *
   * @param name the component name
   * @param version the component version
   * @param description the component description
   * @param author the component author
   * @param license the component license
   * @param properties custom properties
   */
  public ComponentMetadata(
      final String name,
      final ComponentVersion version,
      final String description,
      final String author,
      final String license,
      final Map<String, String> properties) {
    this.name = name != null ? name : "unknown";
    this.version = version != null ? version : new ComponentVersion(1, 0, 0);
    this.description = description != null ? description : "";
    this.author = author;
    this.license = license;
    this.properties = properties != null ? Map.copyOf(properties) : Map.of();
    this.createdTimestamp = System.currentTimeMillis();
  }

  public String getName() {
    return name;
  }

  public ComponentVersion getVersion() {
    return version;
  }

  public String getDescription() {
    return description;
  }

  public String getAuthor() {
    return author;
  }

  public String getLicense() {
    return license;
  }

  public Map<String, String> getProperties() {
    return properties;
  }

  public long getCreatedTimestamp() {
    return createdTimestamp;
  }

  @Override
  public String toString() {
    return String.format(
        "ComponentMetadata{name='%s', version=%s, description='%s'}", name, version, description);
  }
}
