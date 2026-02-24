package ai.tegmentum.wasmtime4j.util;

import ai.tegmentum.wasmtime4j.WasmFunction;
import ai.tegmentum.wasmtime4j.WasmGlobal;
import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.WasmTable;
import ai.tegmentum.wasmtime4j.type.GlobalType;
import ai.tegmentum.wasmtime4j.type.ImportType;
import ai.tegmentum.wasmtime4j.type.MemoryType;
import ai.tegmentum.wasmtime4j.type.TableType;
import ai.tegmentum.wasmtime4j.type.WasmType;
import ai.tegmentum.wasmtime4j.type.WasmTypeKind;
import ai.tegmentum.wasmtime4j.validation.ImportInfo;
import ai.tegmentum.wasmtime4j.validation.ImportIssue;
import ai.tegmentum.wasmtime4j.validation.ImportMap;
import ai.tegmentum.wasmtime4j.validation.ImportValidation;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Shared import validation logic for Module implementations.
 *
 * <p>Both JniModule and PanamaModule delegate their {@code validateImportsDetailed} logic to this
 * utility to avoid code duplication.
 *
 * @since 1.0.0
 */
public final class ModuleValidationSupport {

  private ModuleValidationSupport() {
    // Utility class
  }

  /**
   * Validates imports against a module's required import types.
   *
   * @param importTypes the module's required import types (from {@code Module.getImports()})
   * @param imports the import map to validate against
   * @return the validation result
   */
  public static ImportValidation validateImportsDetailed(
      final List<ImportType> importTypes, final ImportMap imports) {
    final List<ImportIssue> issues = new ArrayList<>();
    final List<ImportInfo> validatedImports = new ArrayList<>();
    final Map<String, Map<String, Object>> importsMap = imports.getImports();

    int validCount = 0;

    for (final ImportType importType : importTypes) {
      final String moduleName = importType.getModuleName();
      final String fieldName = importType.getName();
      final WasmType expectedType = importType.getType();

      // Check if import exists
      if (!imports.contains(moduleName, fieldName)) {
        issues.add(
            new ImportIssue(
                ImportIssue.Severity.ERROR,
                ImportIssue.Type.MISSING_IMPORT,
                moduleName,
                fieldName,
                "Required import is missing from ImportMap"));
        continue;
      }

      // Get actual import object and validate type
      final Map<String, Object> moduleMap = importsMap.get(moduleName);
      if (moduleMap == null) {
        issues.add(
            new ImportIssue(
                ImportIssue.Severity.ERROR,
                ImportIssue.Type.MODULE_NOT_FOUND,
                moduleName,
                fieldName,
                "Module not found in ImportMap"));
        continue;
      }

      final Object actualImport = moduleMap.get(fieldName);
      if (actualImport == null) {
        issues.add(
            new ImportIssue(
                ImportIssue.Severity.ERROR,
                ImportIssue.Type.EXPORT_NOT_FOUND,
                moduleName,
                fieldName,
                "Import field not found in module"));
        continue;
      }

      // Type check based on expected type kind
      final WasmTypeKind expectedKind = expectedType.getKind();
      boolean typeMatches = true;
      String expectedTypeStr = expectedKind.toString();
      String actualTypeStr = actualImport.getClass().getSimpleName();

      switch (expectedKind) {
        case GLOBAL:
          if (actualImport instanceof WasmGlobal) {
            final WasmGlobal global = (WasmGlobal) actualImport;
            final GlobalType actualGlobalType = global.getGlobalType();
            final GlobalType expectedGlobalType = (GlobalType) expectedType;

            if (!globalTypesMatch(expectedGlobalType, actualGlobalType)) {
              typeMatches = false;
              expectedTypeStr = formatGlobalType(expectedGlobalType);
              actualTypeStr = formatGlobalType(actualGlobalType);
            }
          } else {
            typeMatches = false;
          }
          break;

        case TABLE:
          if (actualImport instanceof WasmTable) {
            final WasmTable table = (WasmTable) actualImport;
            final TableType actualTableType = table.getTableType();
            final TableType expectedTableType = (TableType) expectedType;

            if (!tableTypesMatch(expectedTableType, actualTableType)) {
              typeMatches = false;
              expectedTypeStr = formatTableType(expectedTableType);
              actualTypeStr = formatTableType(actualTableType);
            }
          } else {
            typeMatches = false;
          }
          break;

        case MEMORY:
          if (actualImport instanceof WasmMemory) {
            final WasmMemory memory = (WasmMemory) actualImport;
            final MemoryType actualMemoryType = memory.getMemoryType();
            final MemoryType expectedMemoryType = (MemoryType) expectedType;

            if (!memoryTypesMatch(expectedMemoryType, actualMemoryType)) {
              typeMatches = false;
              expectedTypeStr = formatMemoryType(expectedMemoryType);
              actualTypeStr = formatMemoryType(actualMemoryType);
            }
          } else {
            typeMatches = false;
          }
          break;

        case FUNCTION:
          if (actualImport instanceof WasmFunction) {
            // Function type checking would require FunctionType comparison
            // For now, accept any WasmFunction as matching
            typeMatches = true;
          } else {
            typeMatches = false;
          }
          break;

        default:
          typeMatches = false;
          expectedTypeStr = "Unknown type: " + expectedKind;
      }

      if (!typeMatches) {
        issues.add(
            new ImportIssue(
                ImportIssue.Severity.ERROR,
                ImportIssue.Type.TYPE_MISMATCH,
                moduleName,
                fieldName,
                "Import type does not match expected type",
                expectedTypeStr,
                actualTypeStr));
      } else {
        validCount++;
        // Determine ImportInfo.ImportKind from WasmTypeKind
        final ImportInfo.ImportKind infoType;
        switch (expectedKind) {
          case GLOBAL:
            infoType = ImportInfo.ImportKind.GLOBAL;
            break;
          case TABLE:
            infoType = ImportInfo.ImportKind.TABLE;
            break;
          case MEMORY:
            infoType = ImportInfo.ImportKind.MEMORY;
            break;
          case FUNCTION:
            infoType = ImportInfo.ImportKind.FUNCTION;
            break;
          default:
            infoType = ImportInfo.ImportKind.FUNCTION;
        }

        validatedImports.add(
            new ImportInfo(
                moduleName,
                fieldName,
                infoType,
                Optional.of(actualTypeStr),
                Instant.now(),
                actualImport instanceof WasmFunction,
                Optional.of("Provided via ImportMap")));
      }
    }

    return new ImportValidation(
        issues.isEmpty(), issues, validatedImports, importTypes.size(), validCount);
  }

  /**
   * Checks if two GlobalType instances are compatible.
   *
   * @param expected the expected global type
   * @param actual the actual global type
   * @return true if the types match
   */
  static boolean globalTypesMatch(final GlobalType expected, final GlobalType actual) {
    return expected.getValueType() == actual.getValueType()
        && expected.isMutable() == actual.isMutable();
  }

  /**
   * Checks if two TableType instances are compatible.
   *
   * @param expected the expected table type
   * @param actual the actual table type
   * @return true if the types match
   */
  static boolean tableTypesMatch(final TableType expected, final TableType actual) {
    return expected.getElementType() == actual.getElementType()
        && expected.getMinimum() <= actual.getMinimum()
        && (!expected.getMaximum().isPresent()
            || (actual.getMaximum().isPresent()
                && expected.getMaximum().get() >= actual.getMaximum().get()));
  }

  /**
   * Checks if two MemoryType instances are compatible.
   *
   * @param expected the expected memory type
   * @param actual the actual memory type
   * @return true if the types match
   */
  static boolean memoryTypesMatch(final MemoryType expected, final MemoryType actual) {
    return expected.getMinimum() <= actual.getMinimum()
        && expected.is64Bit() == actual.is64Bit()
        && expected.isShared() == actual.isShared()
        && (!expected.getMaximum().isPresent()
            || (actual.getMaximum().isPresent()
                && expected.getMaximum().get() >= actual.getMaximum().get()));
  }

  /**
   * Formats a GlobalType for error messages.
   *
   * @param type the global type
   * @return formatted string
   */
  static String formatGlobalType(final GlobalType type) {
    return String.format(
        "Global(%s, %s)", type.getValueType(), type.isMutable() ? "mutable" : "immutable");
  }

  /**
   * Formats a TableType for error messages.
   *
   * @param type the table type
   * @return formatted string
   */
  static String formatTableType(final TableType type) {
    return String.format(
        "Table(%s, min=%d, max=%s)",
        type.getElementType(),
        type.getMinimum(),
        type.getMaximum().map(String::valueOf).orElse("none"));
  }

  /**
   * Formats a MemoryType for error messages.
   *
   * @param type the memory type
   * @return formatted string
   */
  static String formatMemoryType(final MemoryType type) {
    return String.format(
        "Memory(min=%d, max=%s, %s, %s)",
        type.getMinimum(),
        type.getMaximum().map(String::valueOf).orElse("none"),
        type.is64Bit() ? "64-bit" : "32-bit",
        type.isShared() ? "shared" : "not-shared");
  }
}
