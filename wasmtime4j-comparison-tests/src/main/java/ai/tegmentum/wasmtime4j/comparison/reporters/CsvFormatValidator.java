package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.ArrayList;
import java.util.List;

/**
 * CSV format validator.
 */
final class CsvFormatValidator implements FormatValidator {
  @Override
  public ValidationResult validate(final byte[] data, final SchemaDefinition schema) {
    final String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
    final List<ValidationError> errors = new ArrayList<>();
    final List<ValidationWarning> warnings = new ArrayList<>();

    final String[] lines = content.split("\n");
    if (lines.length == 0) {
      errors.add(new ValidationError("Empty CSV file", ValidationErrorType.INVALID_FORMAT));
      return ValidationResult.failure(errors, warnings);
    }

    // Check header row
    final String header = lines[0];
    if (!header.contains("testName")) {
      errors.add(
          new ValidationError(
              "Missing required column: testName", ValidationErrorType.MISSING_REQUIRED_FIELD));
    }

    // Basic structure validation
    final int expectedColumnCount = header.split(",").length;
    for (int i = 1; i < lines.length; i++) {
      final String line = lines[i].trim();
      if (!line.isEmpty()) {
        final int columnCount = line.split(",").length;
        if (columnCount != expectedColumnCount) {
          warnings.add(
              new ValidationWarning(
                  "Line "
                      + (i + 1)
                      + " has "
                      + columnCount
                      + " columns, expected "
                      + expectedColumnCount,
                  "structure"));
        }
      }
    }

    return errors.isEmpty()
        ? ValidationResult.success(warnings)
        : ValidationResult.failure(errors, warnings);
  }
}