package ai.tegmentum.wasmtime4j.validation;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * Information about an import definition in a linker.
 *
 * <p>This class provides comprehensive metadata about registered imports, including their types,
 * signatures, and registration details.
 *
 * @since 1.0.0
 */
public final class ImportInfo {

  private final String moduleName;
  private final String importName;
  private final ImportKind importKind;
  private final Optional<String> typeSignature;
  private final Instant definedAt;
  private final boolean isHostFunction;
  private final Optional<String> sourceDescription;

  /**
   * Creates a new import information record.
   *
   * @param moduleName the module name for the import
   * @param importName the import name
   * @param importKind the kind of import
   * @param typeSignature the type signature (if applicable)
   * @param definedAt when this import was defined
   * @param isHostFunction whether this is a host function
   * @param sourceDescription description of the import source
   */
  public ImportInfo(
      final String moduleName,
      final String importName,
      final ImportKind importKind,
      final Optional<String> typeSignature,
      final Instant definedAt,
      final boolean isHostFunction,
      final Optional<String> sourceDescription) {
    this.moduleName = Objects.requireNonNull(moduleName, "moduleName");
    this.importName = Objects.requireNonNull(importName, "importName");
    this.importKind = Objects.requireNonNull(importKind, "importKind");
    this.typeSignature = Objects.requireNonNull(typeSignature, "typeSignature");
    this.definedAt = Objects.requireNonNull(definedAt, "definedAt");
    this.isHostFunction = isHostFunction;
    this.sourceDescription = Objects.requireNonNull(sourceDescription, "sourceDescription");
  }

  /**
   * Gets the module name for this import.
   *
   * @return the module name
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Gets the import name.
   *
   * @return the import name
   */
  public String getImportName() {
    return importName;
  }

  /**
   * Gets the type of this import.
   *
   * @return the import type
   */
  public ImportKind getImportKind() {
    return importKind;
  }

  /**
   * Gets the type signature for this import.
   *
   * <p>For functions, this includes parameter and return types. For other types, this may include
   * size or other type information.
   *
   * @return the type signature if available
   */
  public Optional<String> getTypeSignature() {
    return typeSignature;
  }

  /**
   * Gets when this import was defined.
   *
   * @return the definition timestamp
   */
  public Instant getDefinedAt() {
    return definedAt;
  }

  /**
   * Checks whether this is a host function.
   *
   * @return true if this is a host function, false otherwise
   */
  public boolean isHostFunction() {
    return isHostFunction;
  }

  /**
   * Gets a description of the import source.
   *
   * <p>This might indicate whether the import comes from a host function, another module instance,
   * or a direct definition.
   *
   * @return the source description if available
   */
  public Optional<String> getSourceDescription() {
    return sourceDescription;
  }

  /**
   * Gets the full import identifier.
   *
   * @return a string in the format "moduleName::importName"
   */
  public String getImportIdentifier() {
    return moduleName + "::" + importName;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("ImportInfo{");
    sb.append(getImportIdentifier());
    sb.append(", type=").append(importKind);

    if (typeSignature.isPresent()) {
      sb.append(", signature=").append(typeSignature.get());
    }

    if (isHostFunction) {
      sb.append(", hostFunction");
    }

    if (sourceDescription.isPresent()) {
      sb.append(", source=").append(sourceDescription.get());
    }

    sb.append(", definedAt=").append(definedAt);
    sb.append("}");

    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }
    final ImportInfo that = (ImportInfo) obj;
    return isHostFunction == that.isHostFunction
        && Objects.equals(moduleName, that.moduleName)
        && Objects.equals(importName, that.importName)
        && importKind == that.importKind
        && Objects.equals(typeSignature, that.typeSignature)
        && Objects.equals(definedAt, that.definedAt)
        && Objects.equals(sourceDescription, that.sourceDescription);
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        moduleName,
        importName,
        importKind,
        typeSignature,
        definedAt,
        isHostFunction,
        sourceDescription);
  }

  /** Types of imports that can be defined in a linker. */
  public enum ImportKind {
    /** Function import. */
    FUNCTION,
    /** Memory import. */
    MEMORY,
    /** Table import. */
    TABLE,
    /** Global import. */
    GLOBAL,
    /** Instance import (all exports from an instance). */
    INSTANCE
  }
}
