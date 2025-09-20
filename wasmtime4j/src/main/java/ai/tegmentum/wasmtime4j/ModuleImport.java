package ai.tegmentum.wasmtime4j;

/**
 * Represents a WebAssembly module import with complete type information.
 *
 * <p>This class provides comprehensive import metadata including module name, field name, and
 * specific type information for introspection purposes.
 *
 * @since 1.0.0
 */
public final class ModuleImport {

  private final String moduleName;
  private final String fieldName;
  private final ImportType importType;

  /**
   * Creates a new module import.
   *
   * @param moduleName the module name of the import
   * @param fieldName the field name of the import
   * @param importType the type information of the import
   * @throws IllegalArgumentException if any parameter is null
   */
  public ModuleImport(
      final String moduleName, final String fieldName, final ImportType importType) {
    if (moduleName == null) {
      throw new IllegalArgumentException("Module name cannot be null");
    }
    if (fieldName == null) {
      throw new IllegalArgumentException("Field name cannot be null");
    }
    if (importType == null) {
      throw new IllegalArgumentException("Import type cannot be null");
    }
    this.moduleName = moduleName;
    this.fieldName = fieldName;
    this.importType = importType;
  }

  /**
   * Gets the module name of the import.
   *
   * @return the module name
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Gets the field name of the import.
   *
   * @return the field name
   */
  public String getFieldName() {
    return fieldName;
  }

  /**
   * Gets the complete import type information.
   *
   * @return the import type
   */
  public ImportType getImportType() {
    return importType;
  }

  @Override
  public String toString() {
    return String.format(
        "ModuleImport{module='%s', field='%s', type=%s}", moduleName, fieldName, importType);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ModuleImport that = (ModuleImport) obj;
    return moduleName.equals(that.moduleName)
        && fieldName.equals(that.fieldName)
        && importType.equals(that.importType);
  }

  @Override
  public int hashCode() {
    return java.util.Objects.hash(moduleName, fieldName, importType);
  }
}
