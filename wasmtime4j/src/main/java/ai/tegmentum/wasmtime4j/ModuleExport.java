package ai.tegmentum.wasmtime4j;

import ai.tegmentum.wasmtime4j.type.ExportType;

/**
 * Represents a WebAssembly module export with complete type information.
 *
 * <p>This class provides comprehensive export metadata including name and specific type information
 * for introspection purposes.
 *
 * @since 1.0.0
 */
public final class ModuleExport {

  private final String name;
  private final ExportType exportType;

  /**
   * Creates a new module export.
   *
   * @param name the name of the export
   * @param exportType the type information of the export
   * @throws IllegalArgumentException if any parameter is null
   */
  public ModuleExport(final String name, final ExportType exportType) {
    if (name == null) {
      throw new IllegalArgumentException("Export name cannot be null");
    }
    if (exportType == null) {
      throw new IllegalArgumentException("Export type cannot be null");
    }
    this.name = name;
    this.exportType = exportType;
  }

  /**
   * Gets the name of the export.
   *
   * @return the export name
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the complete export type information.
   *
   * @return the export type
   */
  public ExportType getExportType() {
    return exportType;
  }

  @Override
  public String toString() {
    return String.format("ModuleExport{name='%s', type=%s}", name, exportType);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ModuleExport that = (ModuleExport) obj;
    return name.equals(that.name) && exportType.equals(that.exportType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(name, exportType);
  }
}
