package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.ArrayList;
import java.util.List;

/**
 * HTML format validator.
 */
final class HtmlFormatValidator implements FormatValidator {
  @Override
  public ValidationResult validate(final byte[] data, final SchemaDefinition schema) {
    final String content = new String(data, java.nio.charset.StandardCharsets.UTF_8);
    final List<ValidationError> errors = new ArrayList<>();
    final List<ValidationWarning> warnings = new ArrayList<>();

    // Basic HTML structure check
    if (!content.toLowerCase().contains("<html") && !content.toLowerCase().contains("<!doctype")) {
      warnings.add(new ValidationWarning("No HTML doctype or html element found", "structure"));
    }

    return ValidationResult.success(warnings);
  }
}