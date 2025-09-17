package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

/** JSON format validator. */
final class JsonFormatValidator implements FormatValidator {
  private static final Pattern JSON_STRUCTURE_PATTERN =
      Pattern.compile("^\\s*\\{.*\\}\\s*$", Pattern.DOTALL);

  @Override
  public ValidationResult validate(final byte[] data, final SchemaDefinition schema) {
    final String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
    final List<ValidationError> errors = new ArrayList<>();
    final List<ValidationWarning> warnings = new ArrayList<>();

    // Basic JSON structure check
    if (!JSON_STRUCTURE_PATTERN.matcher(content).matches()) {
      errors.add(new ValidationError("Invalid JSON structure", ValidationErrorType.INVALID_FORMAT));
      return ValidationResult.failure(errors, warnings);
    }

    // Check for required top-level fields
    if (!content.contains("\"schema\"")) {
      errors.add(
          new ValidationError(
              "Missing required field: schema", ValidationErrorType.MISSING_REQUIRED_FIELD));
    }
    if (!content.contains("\"metadata\"")) {
      errors.add(
          new ValidationError(
              "Missing required field: metadata", ValidationErrorType.MISSING_REQUIRED_FIELD));
    }
    if (!content.contains("\"summary\"")) {
      errors.add(
          new ValidationError(
              "Missing required field: summary", ValidationErrorType.MISSING_REQUIRED_FIELD));
    }
    if (!content.contains("\"results\"")) {
      errors.add(
          new ValidationError(
              "Missing required field: results", ValidationErrorType.MISSING_REQUIRED_FIELD));
    }

    // Check for valid UTF-8 encoding - looking for replacement character (Unicode FFFD)
    if (content.contains("�")) {
      warnings.add(new ValidationWarning("Potential encoding issues detected", "encoding"));
    }

    return errors.isEmpty()
        ? ValidationResult.success(warnings)
        : ValidationResult.failure(errors, warnings);
  }
}
