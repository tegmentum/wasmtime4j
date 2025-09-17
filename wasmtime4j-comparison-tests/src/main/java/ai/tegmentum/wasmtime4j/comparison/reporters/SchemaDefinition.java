package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Map;
import java.util.Objects;

/** Schema definition with metadata. */
public final class SchemaDefinition {
  private final ExportFormat format;
  private final String version;
  private final String description;
  private final String schemaContent;
  private final Map<String, String> properties;

  /**
   * Creates a new schema definition.
   *
   * @param format the export format
   * @param version the schema version
   * @param description the schema description
   * @param schemaContent the schema content
   * @param properties additional properties
   */
  public SchemaDefinition(
      final ExportFormat format,
      final String version,
      final String description,
      final String schemaContent,
      final Map<String, String> properties) {
    this.format = Objects.requireNonNull(format, "format cannot be null");
    this.version = Objects.requireNonNull(version, "version cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.schemaContent = Objects.requireNonNull(schemaContent, "schemaContent cannot be null");
    this.properties = Map.copyOf(properties);
  }

  /**
   * Gets the export format.
   *
   * @return the export format
   */
  public ExportFormat getFormat() {
    return format;
  }

  /**
   * Gets the schema version.
   *
   * @return the schema version
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the schema description.
   *
   * @return the schema description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the schema content.
   *
   * @return the schema content
   */
  public String getSchemaContent() {
    return schemaContent;
  }

  /**
   * Gets the additional properties.
   *
   * @return the additional properties
   */
  public Map<String, String> getProperties() {
    return properties;
  }
}
