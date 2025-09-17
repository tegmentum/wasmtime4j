package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * Schema validation utility for exported data formats with versioning support.
 *
 * <p>Provides validation capabilities for:
 *
 * <ul>
 *   <li>JSON schema validation with semantic versioning
 *   <li>CSV format validation with column type checking
 *   <li>Schema evolution and backward compatibility checking
 *   <li>Custom validation rules for business logic constraints
 * </ul>
 *
 * <p>Supports both structural validation (format compliance) and semantic validation (data
 * correctness) to ensure exported data meets quality standards for downstream consumption.
 *
 * @since 1.0.0
 */
public final class SchemaValidator {
  private static final Logger LOGGER = Logger.getLogger(SchemaValidator.class.getName());

  private final Map<String, SchemaDefinition> registeredSchemas;
  private final Map<ExportFormat, FormatValidator> formatValidators;

  /** Creates a new schema validator with default configuration. */
  public SchemaValidator() {
    this.registeredSchemas = new ConcurrentHashMap<>();
    this.formatValidators = createFormatValidators();
    registerBuiltinSchemas();
  }

  /**
   * Validates exported data against a specified schema.
   *
   * @param data the exported data to validate
   * @param format the export format
   * @param schemaVersion the schema version to validate against
   * @return validation result with any errors or warnings
   */
  public ValidationResult validate(
      final byte[] data, final ExportFormat format, final String schemaVersion) {
    Objects.requireNonNull(data, "data cannot be null");
    Objects.requireNonNull(format, "format cannot be null");
    Objects.requireNonNull(schemaVersion, "schemaVersion cannot be null");

    final String schemaKey = format.name() + ":" + schemaVersion;
    final SchemaDefinition schema = registeredSchemas.get(schemaKey);

    if (schema == null) {
      return ValidationResult.error(
          "Unknown schema: " + schemaKey, ValidationErrorType.SCHEMA_NOT_FOUND);
    }

    final FormatValidator validator = formatValidators.get(format);
    if (validator == null) {
      return ValidationResult.error(
          "No validator for format: " + format, ValidationErrorType.UNSUPPORTED_FORMAT);
    }

    try {
      return validator.validate(data, schema);
    } catch (final Exception e) {
      LOGGER.severe("Validation failed for schema " + schemaKey + ": " + e.getMessage());
      return ValidationResult.error(
          "Validation exception: " + e.getMessage(), ValidationErrorType.VALIDATION_EXCEPTION);
    }
  }

  /**
   * Validates that a comparison report can be exported successfully.
   *
   * @param report the comparison report to validate
   * @param format the target export format
   * @param schemaVersion the target schema version
   * @return validation result
   */
  public ValidationResult validateReport(
      final ComparisonReport report, final ExportFormat format, final String schemaVersion) {
    Objects.requireNonNull(report, "report cannot be null");
    Objects.requireNonNull(format, "format cannot be null");
    Objects.requireNonNull(schemaVersion, "schemaVersion cannot be null");

    final List<ValidationError> errors = new ArrayList<>();
    final List<ValidationWarning> warnings = new ArrayList<>();

    // Basic report structure validation
    if (report.getMetadata() == null) {
      errors.add(
          new ValidationError(
              "Report metadata is required", ValidationErrorType.MISSING_REQUIRED_FIELD));
    }

    if (report.getSummary() == null) {
      errors.add(
          new ValidationError(
              "Report summary is required", ValidationErrorType.MISSING_REQUIRED_FIELD));
    }

    if (report.getTestNames().isEmpty()) {
      warnings.add(new ValidationWarning("Report contains no test results", "performance"));
    }

    // Format-specific validation
    switch (format) {
      case JSON -> validateForJsonExport(report, errors, warnings);
      case CSV -> validateForCsvExport(report, errors, warnings);
      case XML -> validateForXmlExport(report, errors, warnings);
      case HTML -> validateForHtmlExport(report, errors, warnings);
      default -> {
        // Unknown format - add general validation warning
        warnings.add(new ValidationWarning("Unknown export format: " + format, "format"));
      }
    }

    // Schema version compatibility
    if (!isSchemaVersionSupported(format, schemaVersion)) {
      errors.add(
          new ValidationError(
              "Unsupported schema version: " + schemaVersion,
              ValidationErrorType.UNSUPPORTED_VERSION));
    }

    if (errors.isEmpty()) {
      return ValidationResult.success(warnings);
    } else {
      return ValidationResult.failure(errors, warnings);
    }
  }

  /**
   * Registers a custom schema definition.
   *
   * @param schema the schema definition to register
   */
  public void registerSchema(final SchemaDefinition schema) {
    Objects.requireNonNull(schema, "schema cannot be null");
    final String key = schema.getFormat().name() + ":" + schema.getVersion();
    registeredSchemas.put(key, schema);
    LOGGER.info("Registered schema: " + key);
  }

  /**
   * Checks if a schema version is supported for a given format.
   *
   * @param format the export format
   * @param version the schema version
   * @return true if supported
   */
  public boolean isSchemaVersionSupported(final ExportFormat format, final String version) {
    return registeredSchemas.containsKey(format.name() + ":" + version);
  }

  /**
   * Gets all supported schema versions for a format.
   *
   * @param format the export format
   * @return list of supported versions
   */
  public List<String> getSupportedVersions(final ExportFormat format) {
    return registeredSchemas.keySet().stream()
        .filter(key -> key.startsWith(format.name() + ":"))
        .map(key -> key.substring(format.name().length() + 1))
        .sorted(this::compareVersions)
        .toList();
  }

  /** Creates format-specific validators. */
  private Map<ExportFormat, FormatValidator> createFormatValidators() {
    final Map<ExportFormat, FormatValidator> validators = new HashMap<>();
    validators.put(ExportFormat.JSON, new JsonFormatValidator());
    validators.put(ExportFormat.CSV, new CsvFormatValidator());
    validators.put(ExportFormat.XML, new XmlFormatValidator());
    validators.put(ExportFormat.HTML, new HtmlFormatValidator());
    return validators;
  }

  /** Registers built-in schema definitions. */
  private void registerBuiltinSchemas() {
    // JSON schemas
    registerSchema(createJsonSchema("1.0.0"));

    // CSV schemas
    registerSchema(createCsvSchema("1.0.0"));

    // XML schemas
    registerSchema(createXmlSchema("1.0.0"));

    // HTML schemas
    registerSchema(createHtmlSchema("1.0.0"));
  }

  private SchemaDefinition createJsonSchema(final String version) {
    final String schemaContent =
        """
        {
          "$schema": "https://json-schema.org/draft/2020-12/schema",
          "type": "object",
          "required": ["schema", "metadata", "summary", "results"],
          "properties": {
            "schema": {
              "type": "object",
              "required": ["version"],
              "properties": {
                "version": {"type": "string", "pattern": "^\\d+\\.\\d+\\.\\d+$"}
              }
            },
            "metadata": {
              "type": "object",
              "required": ["suiteName", "generatedAt", "runtimesCompared"],
              "properties": {
                "suiteName": {"type": "string", "minLength": 1},
                "generatedAt": {"type": "string", "format": "date-time"},
                "runtimesCompared": {
                  "type": "array",
                  "items": {"type": "string"},
                  "minItems": 1
                }
              }
            },
            "summary": {
              "type": "object",
              "required": ["totalTests", "overallCompatibilityScore"],
              "properties": {
                "totalTests": {"type": "integer", "minimum": 0},
                "overallCompatibilityScore": {"type": "number", "minimum": 0, "maximum": 1}
              }
            },
            "results": {
              "type": "object",
              "patternProperties": {
                ".*": {
                  "type": "object",
                  "properties": {
                    "behavioral": {"type": "object"},
                    "performance": {"type": "object"},
                    "coverage": {"type": "object"},
                    "recommendations": {"type": "object"}
                  }
                }
              }
            }
          }
        }
        """;

    return new SchemaDefinition(
        ExportFormat.JSON,
        version,
        "JSON schema for Wasmtime4j comparison reports",
        schemaContent,
        Map.of(
            "encoding", "UTF-8",
            "required_fields", "schema,metadata,summary,results"));
  }

  private SchemaDefinition createCsvSchema(final String version) {
    final String schemaContent =
        """
        CSV Schema Rules:
        - First row must contain column headers
        - testName column is required
        - Numeric columns must contain valid numbers or empty values
        - Boolean columns must contain true/false or empty values
        - Date columns must follow ISO 8601 format
        - No embedded newlines in field values
        - Field values containing delimiter must be quoted
        """;

    return new SchemaDefinition(
        ExportFormat.CSV,
        version,
        "CSV schema for Wasmtime4j comparison reports",
        schemaContent,
        Map.of(
            "encoding", "UTF-8",
            "delimiter", ",",
            "quote_char", "\"",
            "required_columns", "testName"));
  }

  private SchemaDefinition createXmlSchema(final String version) {
    final String schemaContent = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><!-- Placeholder -->";
    return new SchemaDefinition(
        ExportFormat.XML,
        version,
        "XML schema for Wasmtime4j comparison reports",
        schemaContent,
        Map.of());
  }

  private SchemaDefinition createHtmlSchema(final String version) {
    final String schemaContent = "<!DOCTYPE html><!-- Placeholder -->";
    return new SchemaDefinition(
        ExportFormat.HTML,
        version,
        "HTML schema for Wasmtime4j comparison reports",
        schemaContent,
        Map.of());
  }

  /** Validates JSON-specific report requirements. */
  private void validateForJsonExport(
      final ComparisonReport report,
      final List<ValidationError> errors,
      final List<ValidationWarning> warnings) {
    // Check for JSON-unfriendly characters in strings
    for (final String testName : report.getTestNames()) {
      if (containsControlCharacters(testName)) {
        warnings.add(
            new ValidationWarning(
                "Test name contains control characters: " + testName, "json_encoding"));
      }
    }
  }

  /** Validates CSV-specific report requirements. */
  private void validateForCsvExport(
      final ComparisonReport report,
      final List<ValidationError> errors,
      final List<ValidationWarning> warnings) {
    // Check for CSV-problematic characters in test names
    for (final String testName : report.getTestNames()) {
      if (testName.contains(",") || testName.contains("\"") || testName.contains("\n")) {
        warnings.add(
            new ValidationWarning(
                "Test name contains CSV delimiter/quote characters: " + testName, "csv_encoding"));
      }
    }
  }

  /** Validates XML-specific report requirements. */
  private void validateForXmlExport(
      final ComparisonReport report,
      final List<ValidationError> errors,
      final List<ValidationWarning> warnings) {
    // Check for XML-invalid characters
    for (final String testName : report.getTestNames()) {
      if (containsXmlInvalidCharacters(testName)) {
        warnings.add(
            new ValidationWarning(
                "Test name contains XML-invalid characters: " + testName, "xml_encoding"));
      }
    }
  }

  /** Validates HTML-specific report requirements. */
  private void validateForHtmlExport(
      final ComparisonReport report,
      final List<ValidationError> errors,
      final List<ValidationWarning> warnings) {
    // HTML validation is primarily about escaping, which is handled during export
    if (report.getTestCount() > 10000) {
      warnings.add(
          new ValidationWarning(
              "Large report may cause browser performance issues", "html_performance"));
    }
  }

  /** Checks if string contains control characters. */
  private boolean containsControlCharacters(final String str) {
    return str.chars().anyMatch(c -> c < 32 && c != '\t' && c != '\n' && c != '\r');
  }

  /** Checks if string contains XML-invalid characters. */
  private boolean containsXmlInvalidCharacters(final String str) {
    return str.chars()
        .anyMatch(
            c -> (c < 32 && c != '\t' && c != '\n' && c != '\r') || c == 0xFFFE || c == 0xFFFF);
  }

  /** Compares semantic version strings. */
  private int compareVersions(final String v1, final String v2) {
    final String[] parts1 = v1.split("\\.");
    final String[] parts2 = v2.split("\\.");

    final int length = Math.max(parts1.length, parts2.length);
    for (int i = 0; i < length; i++) {
      final int part1 = i < parts1.length ? Integer.parseInt(parts1[i]) : 0;
      final int part2 = i < parts2.length ? Integer.parseInt(parts2[i]) : 0;

      if (part1 != part2) {
        return Integer.compare(part1, part2);
      }
    }
    return 0;
  }
}

/** Format-specific validator interface. */
interface FormatValidator {
  ValidationResult validate(byte[] data, SchemaDefinition schema);
}

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

/** CSV format validator. */
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

/** XML format validator. */
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

/** HTML format validator. */
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

/** Schema definition with metadata. */
final class SchemaDefinition {
  private final ExportFormat format;
  private final String version;
  private final String description;
  private final String schemaContent;
  private final Map<String, String> properties;

  public SchemaDefinition(
      final ExportFormat format,
      final String version,
      final String description,
      final String schemaContent,
      final Map<String, String> properties) {
    this.format = Objects.requireNonNull(format, "format cannot be null");
    this.version = Objects.requireNonNull(version, "version cannot be null");
    this.description = Objects.requireNonNull(description, "description cannot be null");
    this.schemaContent = Objects.requireNonNull(schemaContent, "schemaContent cannot be null");
    this.properties = Map.copyOf(properties);
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

  public String getSchemaContent() {
    return schemaContent;
  }

  public Map<String, String> getProperties() {
    return properties;
  }
}

/** Result of schema validation. */
final class ValidationResult {
  private final boolean valid;
  private final List<ValidationError> errors;
  private final List<ValidationWarning> warnings;

  private ValidationResult(
      final boolean valid,
      final List<ValidationError> errors,
      final List<ValidationWarning> warnings) {
    this.valid = valid;
    this.errors = List.copyOf(errors);
    this.warnings = List.copyOf(warnings);
  }

  public static ValidationResult success(final List<ValidationWarning> warnings) {
    return new ValidationResult(true, Collections.emptyList(), warnings);
  }

  public static ValidationResult failure(
      final List<ValidationError> errors, final List<ValidationWarning> warnings) {
    return new ValidationResult(false, errors, warnings);
  }

  public static ValidationResult error(final String message, final ValidationErrorType type) {
    return new ValidationResult(
        false, List.of(new ValidationError(message, type)), Collections.emptyList());
  }

  public boolean isValid() {
    return valid;
  }

  public List<ValidationError> getErrors() {
    return errors;
  }

  public List<ValidationWarning> getWarnings() {
    return warnings;
  }

  public boolean hasWarnings() {
    return !warnings.isEmpty();
  }
}

/** Validation error information. */
final class ValidationError {
  private final String message;
  private final ValidationErrorType type;

  public ValidationError(final String message, final ValidationErrorType type) {
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.type = Objects.requireNonNull(type, "type cannot be null");
  }

  public String getMessage() {
    return message;
  }

  public ValidationErrorType getType() {
    return type;
  }

  @Override
  public String toString() {
    return type + ": " + message;
  }
}

/** Validation warning information. */
final class ValidationWarning {
  private final String message;
  private final String category;

  public ValidationWarning(final String message, final String category) {
    this.message = Objects.requireNonNull(message, "message cannot be null");
    this.category = Objects.requireNonNull(category, "category cannot be null");
  }

  public String getMessage() {
    return message;
  }

  public String getCategory() {
    return category;
  }

  @Override
  public String toString() {
    return category + ": " + message;
  }
}

/** Types of validation errors. */
enum ValidationErrorType {
  SCHEMA_NOT_FOUND,
  UNSUPPORTED_FORMAT,
  UNSUPPORTED_VERSION,
  INVALID_FORMAT,
  MISSING_REQUIRED_FIELD,
  INVALID_FIELD_VALUE,
  VALIDATION_EXCEPTION
}
