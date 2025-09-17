package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Objects;

/**
 * Schema definition for export formats with versioning support.
 *
 * @since 1.0.0
 */
public final class ExportSchema {
  private final ExportFormat format;
  private final String version;
  private final String description;
  private final String schemaDefinition;

  /**
   * Constructs a new ExportSchema with the specified properties.
   *
   * @param format the export format
   * @param version the schema version
   * @param description the schema description
   * @param schemaDefinition the schema definition content
   */
  public ExportSchema(
      final ExportFormat format,
      final String version,
      final String description,
      final String schemaDefinition) {
    this.format = Objects.requireNonNull(format, "format cannot be null");
    this.version = Objects.requireNonNull(version, "version cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.schemaDefinition =
        Objects.requireNonNull(schemaDefinition, "schemaDefinition cannot be null");
  }

  public ExportFormat getFormat() {
    return format;
  }

  public String getVersion() {
    return version;
  }

  public String getDescription() {
    return description;
  }

  public String getSchemaDefinition() {
    return schemaDefinition;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final ExportSchema that = (ExportSchema) obj;
    return format == that.format
        && Objects.equals(version, that.version)
        && Objects.equals(description, that.description)
        && Objects.equals(schemaDefinition, that.schemaDefinition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(format, version, description, schemaDefinition);
  }

  @Override
  public String toString() {
    return "ExportSchema{"
        + "format="
        + format
        + ", version='"
        + version
        + '\''
        + ", description='"
        + description
        + '\''
        + '}';
  }
}
