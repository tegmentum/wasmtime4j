package ai.tegmentum.wasmtime4j.util;

import ai.tegmentum.wasmtime4j.Linker.LinkerDefinition;
import ai.tegmentum.wasmtime4j.Module;
import ai.tegmentum.wasmtime4j.type.ExternType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import ai.tegmentum.wasmtime4j.validation.ImportIssue;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Shared utility methods for Linker import tracking and validation.
 *
 * <p>Provides pure-Java logic used by both JNI and Panama Linker implementations to avoid code
 * duplication. All methods are static and operate on provided collections.
 *
 * @since 1.0.0
 */
public final class LinkerSupport {

  private LinkerSupport() {
    // Utility class
  }

  /**
   * Builds the import key from module name and import name.
   *
   * @param moduleName the module name
   * @param name the import name
   * @return the combined key
   */
  public static String importKey(final String moduleName, final String name) {
    return moduleName + "::" + name;
  }

  /**
   * Checks if an import is defined in the tracking set.
   *
   * @param imports the import tracking set
   * @param moduleName the module name
   * @param name the import name
   * @return true if the import is defined
   * @throws IllegalArgumentException if moduleName or name is null or empty
   */
  public static boolean hasImport(
      final Set<String> imports, final String moduleName, final String name) {
    if (moduleName == null || moduleName.isEmpty()) {
      throw new IllegalArgumentException("Module name cannot be null or empty");
    }
    if (name == null || name.isEmpty()) {
      throw new IllegalArgumentException("Import name cannot be null or empty");
    }
    return imports.contains(importKey(moduleName, name));
  }

  /**
   * Adds an import to the tracking set.
   *
   * @param imports the import tracking set
   * @param moduleName the module name
   * @param name the import name
   */
  public static void addImport(
      final Set<String> imports, final String moduleName, final String name) {
    imports.add(importKey(moduleName, name));
  }

  /**
   * Adds an import with full metadata to the tracking registry.
   *
   * @param imports the import tracking set
   * @param importRegistry the import metadata registry
   * @param moduleName the module name
   * @param name the import name
   * @param importKind the import kind
   * @param typeSignature the type signature (may be null)
   */
  public static void addImportWithMetadata(
      final Set<String> imports,
      final Map<String, ImportInfo> importRegistry,
      final String moduleName,
      final String name,
      final ImportInfo.ImportKind importKind,
      final String typeSignature) {
    final String key = importKey(moduleName, name);
    imports.add(key);
    final ImportInfo info =
        new ImportInfo(
            moduleName,
            name,
            importKind,
            Optional.ofNullable(typeSignature),
            Instant.now(),
            true, // All imports registered via define* methods are host-provided
            Optional.of("Host-provided import"));
    importRegistry.put(key, info);
  }

  /**
   * Maps an ImportType to the corresponding ImportInfo.ImportKind.
   *
   * @param importType the import type from module
   * @return the corresponding ImportInfo.ImportKind
   */
  public static ImportInfo.ImportKind mapImportTypeToImportKind(final ImportType importType) {
    final WasmTypeKind kind = importType.getType().getKind();
    switch (kind) {
      case FUNCTION:
        return ImportInfo.ImportKind.FUNCTION;
      case MEMORY:
        return ImportInfo.ImportKind.MEMORY;
      case TABLE:
        return ImportInfo.ImportKind.TABLE;
      case GLOBAL:
        return ImportInfo.ImportKind.GLOBAL;
      default:
        return ImportInfo.ImportKind.FUNCTION;
    }
  }

  /**
   * Validates that all imports required by the given modules are defined in the linker.
   *
   * @param imports the import tracking set
   * @param modules the modules whose imports to validate
   * @return the validation result
   * @throws IllegalArgumentException if modules is null or empty
   */
  public static ImportValidation validateImports(
      final Set<String> imports, final Module... modules) {
    if (modules == null) {
      throw new IllegalArgumentException("Modules cannot be null");
    }
    if (modules.length == 0) {
      throw new IllegalArgumentException("At least one module must be provided");
    }

    final List<ImportIssue> issues = new ArrayList<>();
    final List<ImportInfo> validatedImports = new ArrayList<>();

    int totalImports = 0;
    int validImportCount = 0;

    for (final Module module : modules) {
      final List<ImportType> moduleImports = module.getImports();
      totalImports += moduleImports.size();

      for (final ImportType importType : moduleImports) {
        final String moduleName = importType.getModuleName();
        final String importName = importType.getName();

        final boolean isDefined = hasImport(imports, moduleName, importName);

        if (isDefined) {
          validImportCount++;

          final ImportInfo.ImportKind infoType = mapImportTypeToImportKind(importType);
          final Optional<String> typeSignature =
              Optional.of(importType.getType().toString());

          final ImportInfo info =
              new ImportInfo(
                  moduleName,
                  importName,
                  infoType,
                  typeSignature,
                  Instant.now(),
                  true,
                  Optional.of("Defined in linker"));

          validatedImports.add(info);
        } else {
          final ImportIssue issue =
              new ImportIssue(
                  ImportIssue.Severity.ERROR,
                  ImportIssue.Type.MISSING_IMPORT,
                  moduleName,
                  importName,
                  "Import not defined in linker",
                  importType.getType().toString(),
                  null);

          issues.add(issue);
        }
      }
    }

    final boolean valid = issues.isEmpty();

    return new ImportValidation(
        valid, issues, validatedImports, totalImports, validImportCount);
  }

  /**
   * Converts the import registry to an iterable of LinkerDefinition objects.
   *
   * @param importRegistry the import metadata registry
   * @return list of LinkerDefinition objects
   */
  public static List<LinkerDefinition> iterDefinitions(
      final Map<String, ImportInfo> importRegistry) {
    final List<LinkerDefinition> definitions = new ArrayList<>();

    for (final ImportInfo info : importRegistry.values()) {
      final ExternType externType;
      switch (info.getImportKind()) {
        case FUNCTION:
          externType = ExternType.FUNC;
          break;
        case MEMORY:
          externType = ExternType.MEMORY;
          break;
        case TABLE:
          externType = ExternType.TABLE;
          break;
        case GLOBAL:
          externType = ExternType.GLOBAL;
          break;
        default:
          externType = ExternType.FUNC;
      }

      definitions.add(new LinkerDefinition(info.getModuleName(), info.getImportName(), externType));
    }

    return definitions;
  }
}
