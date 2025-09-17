package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.ArrayList;
import java.util.List;

/**
 * XML format validator.
 */
final class XmlFormatValidator implements FormatValidator {
  @Override
  public ValidationResult validate(final byte[] data, final SchemaDefinition schema) {
    final String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
    final List<ValidationError> errors = new ArrayList<>();
    final List<ValidationWarning> warnings = new ArrayList<>();

    // Basic XML structure check
    if (!content.trim().startsWith("<?xml") && !content.trim().startsWith("<")) {
      errors.add(new ValidationError("Invalid XML structure", ValidationErrorType.INVALID_FORMAT));
    }

    return errors.isEmpty()
        ? ValidationResult.success(warnings)
        : ValidationResult.failure(errors, warnings);
  }
}