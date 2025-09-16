package ai.tegmentum.wasmtime4j.comparison.reporters;

import static org.junit.jupiter.api.Assertions.*;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;

/**
 * Comprehensive tests for SchemaValidator functionality including validation,
 * schema registration, and error reporting.
 */
class SchemaValidatorTest {

  private SchemaValidator schemaValidator;
  private ComparisonReport testReport;

  @BeforeEach
  void setUp() {
    schemaValidator = new SchemaValidator();
    testReport = createTestReport();
  }

  @Test
  @DisplayName("Should validate valid JSON data successfully")
  void shouldValidateValidJsonDataSuccessfully() {
    // Given
    final String validJson = """
        {
          "schema": {"version": "1.0.0"},
          "metadata": {
            "suiteName": "Test Suite",
            "generatedAt": "2023-09-15T10:30:00Z",
            "runtimesCompared": ["JNI", "PANAMA"]
          },
          "summary": {
            "totalTests": 5,
            "overallCompatibilityScore": 0.95
          },
          "results": {
            "test1": {"behavioral": {}, "performance": {}}
          }
        }
        """;
    final byte[] data = validJson.getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(data, ExportFormat.JSON, "1.0.0");

    // Then
    assertNotNull(result);
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
  }

  @Test
  @DisplayName("Should detect missing required fields in JSON")
  void shouldDetectMissingRequiredFieldsInJson() {
    // Given
    final String invalidJson = """
        {
          "metadata": {
            "suiteName": "Test Suite"
          }
        }
        """;
    final byte[] data = invalidJson.getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(data, ExportFormat.JSON, "1.0.0");

    // Then
    assertNotNull(result);
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());

    // Should have errors for missing fields
    final List<ValidationError> errors = result.getErrors();
    assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("schema")));
    assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("summary")));
    assertTrue(errors.stream().anyMatch(e -> e.getMessage().contains("results")));
  }

  @Test
  @DisplayName("Should validate valid CSV data successfully")
  void shouldValidateValidCsvDataSuccessfully() {
    // Given
    final String validCsv = """
        testName,verdict,consistencyScore
        test1,CONSISTENT,0.95
        test2,MOSTLY_CONSISTENT,0.87
        """;
    final byte[] data = validCsv.getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(data, ExportFormat.CSV, "1.0.0");

    // Then
    assertNotNull(result);
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
  }

  @Test
  @DisplayName("Should detect missing required columns in CSV")
  void shouldDetectMissingRequiredColumnsInCsv() {
    // Given
    final String invalidCsv = """
        verdict,consistencyScore
        CONSISTENT,0.95
        """;
    final byte[] data = invalidCsv.getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(data, ExportFormat.CSV, "1.0.0");

    // Then
    assertNotNull(result);
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());

    // Should have error for missing testName column
    assertTrue(result.getErrors().stream()
        .anyMatch(e -> e.getMessage().contains("testName")));
  }

  @Test
  @DisplayName("Should warn about inconsistent CSV structure")
  void shouldWarnAboutInconsistentCsvStructure() {
    // Given
    final String inconsistentCsv = """
        testName,verdict,consistencyScore
        test1,CONSISTENT,0.95
        test2,MOSTLY_CONSISTENT
        test3,CONSISTENT,0.87,extra_column
        """;
    final byte[] data = inconsistentCsv.getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(data, ExportFormat.CSV, "1.0.0");

    // Then
    assertNotNull(result);
    assertTrue(result.isValid()); // Structure warnings don't fail validation
    assertTrue(result.hasWarnings());

    // Should have warnings about column count mismatches
    assertTrue(result.getWarnings().stream()
        .anyMatch(w -> w.getMessage().contains("columns")));
  }

  @Test
  @DisplayName("Should validate report structure for JSON export")
  void shouldValidateReportStructureForJsonExport() {
    // When
    final ValidationResult result = schemaValidator.validateReport(
        testReport, ExportFormat.JSON, "1.0.0");

    // Then
    assertNotNull(result);
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
  }

  @Test
  @DisplayName("Should detect missing report metadata")
  void shouldDetectMissingReportMetadata() {
    // Given
    final ComparisonReport invalidReport = new ComparisonReport.Builder()
        .metadata(null)
        .summary(testReport.getSummary())
        .testExecutionOrder(testReport.getTestNames())
        .build();

    // This should fail at build time due to null check, so let's test with reflection
    // or create a report that bypasses the null check for testing
    final ReportSummary summary = new ReportSummary(
        0, 0, 0, 0, 0, 0, 0.0, Map.of()
    );

    final ComparisonReport reportWithoutMetadata = new ComparisonReport.Builder()
        .summary(summary)
        .testExecutionOrder(Collections.emptyList())
        .build();

    // When & Then - This should fail during build due to validation
    assertThrows(IllegalStateException.class, () ->
        new ComparisonReport.Builder()
            .summary(summary)
            .testExecutionOrder(Collections.emptyList())
            .build());
  }

  @Test
  @DisplayName("Should warn about empty test results")
  void shouldWarnAboutEmptyTestResults() {
    // Given
    final ComparisonReport emptyReport = createEmptyReport();

    // When
    final ValidationResult result = schemaValidator.validateReport(
        emptyReport, ExportFormat.JSON, "1.0.0");

    // Then
    assertNotNull(result);
    assertTrue(result.isValid());
    assertTrue(result.hasWarnings());

    // Should warn about no test results
    assertTrue(result.getWarnings().stream()
        .anyMatch(w -> w.getMessage().contains("no test results")));
  }

  @Test
  @DisplayName("Should detect unsupported schema versions")
  void shouldDetectUnsupportedSchemaVersions() {
    // When
    final ValidationResult result = schemaValidator.validateReport(
        testReport, ExportFormat.JSON, "99.99.99");

    // Then
    assertNotNull(result);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().stream()
        .anyMatch(e -> e.getMessage().contains("Unsupported schema version")));
  }

  @Test
  @DisplayName("Should register and validate custom schemas")
  void shouldRegisterAndValidateCustomSchemas() {
    // Given
    final SchemaDefinition customSchema = new SchemaDefinition(
        ExportFormat.XML,
        "2.0.0",
        "Custom XML schema",
        "<?xml version=\"1.0\"?>",
        Map.of("custom", "true")
    );

    // When
    schemaValidator.registerSchema(customSchema);

    // Then
    assertTrue(schemaValidator.isSchemaVersionSupported(ExportFormat.XML, "2.0.0"));

    final List<String> versions = schemaValidator.getSupportedVersions(ExportFormat.XML);
    assertTrue(versions.contains("2.0.0"));
  }

  @Test
  @DisplayName("Should return supported schema versions sorted")
  void shouldReturnSupportedSchemaVersionsSorted() {
    // Given
    schemaValidator.registerSchema(new SchemaDefinition(
        ExportFormat.JSON, "2.0.0", "Version 2", "{}", Map.of()));
    schemaValidator.registerSchema(new SchemaDefinition(
        ExportFormat.JSON, "1.5.0", "Version 1.5", "{}", Map.of()));

    // When
    final List<String> versions = schemaValidator.getSupportedVersions(ExportFormat.JSON);

    // Then
    assertNotNull(versions);
    assertTrue(versions.size() >= 3); // Built-in 1.0.0 + our 2 custom versions

    // Should be sorted
    final int index100 = versions.indexOf("1.0.0");
    final int index150 = versions.indexOf("1.5.0");
    final int index200 = versions.indexOf("2.0.0");

    assertTrue(index100 < index150);
    assertTrue(index150 < index200);
  }

  @Test
  @DisplayName("Should handle unknown schema gracefully")
  void shouldHandleUnknownSchemaGracefully() {
    // Given
    final byte[] data = "test data".getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(
        data, ExportFormat.JSON, "unknown-version");

    // Then
    assertNotNull(result);
    assertFalse(result.isValid());
    assertEquals(1, result.getErrors().size());
    assertEquals(ValidationErrorType.SCHEMA_NOT_FOUND,
        result.getErrors().get(0).getType());
  }

  @Test
  @DisplayName("Should handle invalid JSON structure")
  void shouldHandleInvalidJsonStructure() {
    // Given
    final String invalidJson = "not valid json {";
    final byte[] data = invalidJson.getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(data, ExportFormat.JSON, "1.0.0");

    // Then
    assertNotNull(result);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().stream()
        .anyMatch(e -> e.getType() == ValidationErrorType.INVALID_FORMAT));
  }

  @Test
  @DisplayName("Should handle empty CSV file")
  void shouldHandleEmptyCsvFile() {
    // Given
    final byte[] data = "".getBytes(StandardCharsets.UTF_8);

    // When
    final ValidationResult result = schemaValidator.validate(data, ExportFormat.CSV, "1.0.0");

    // Then
    assertNotNull(result);
    assertFalse(result.isValid());
    assertTrue(result.getErrors().stream()
        .anyMatch(e -> e.getMessage().contains("Empty CSV file")));
  }

  @Test
  @DisplayName("Should warn about special characters for different formats")
  void shouldWarnAboutSpecialCharactersForDifferentFormats() {
    // Given
    final ComparisonReport reportWithSpecialChars = createReportWithSpecialCharacters();

    // When - JSON validation
    final ValidationResult jsonResult = schemaValidator.validateReport(
        reportWithSpecialChars, ExportFormat.JSON, "1.0.0");

    // Then
    assertTrue(jsonResult.isValid());
    assertTrue(jsonResult.hasWarnings());
    assertTrue(jsonResult.getWarnings().stream()
        .anyMatch(w -> w.getCategory().equals("json_encoding")));

    // When - CSV validation
    final ValidationResult csvResult = schemaValidator.validateReport(
        reportWithSpecialChars, ExportFormat.CSV, "1.0.0");

    // Then
    assertTrue(csvResult.isValid());
    assertTrue(csvResult.hasWarnings());
    assertTrue(csvResult.getWarnings().stream()
        .anyMatch(w -> w.getCategory().equals("csv_encoding")));
  }

  @Test
  @DisplayName("Should validate XML and HTML formats")
  void shouldValidateXmlAndHtmlFormats() {
    // Given
    final String validXml = "<?xml version=\"1.0\"?><root></root>";
    final String validHtml = "<!DOCTYPE html><html><head></head><body></body></html>";

    // When
    final ValidationResult xmlResult = schemaValidator.validate(
        validXml.getBytes(StandardCharsets.UTF_8), ExportFormat.XML, "1.0.0");
    final ValidationResult htmlResult = schemaValidator.validate(
        validHtml.getBytes(StandardCharsets.UTF_8), ExportFormat.HTML, "1.0.0");

    // Then
    assertTrue(xmlResult.isValid());
    assertTrue(htmlResult.isValid());
  }

  @Test
  @DisplayName("Should handle null validation inputs gracefully")
  void shouldHandleNullValidationInputsGracefully() {
    // When & Then
    assertThrows(NullPointerException.class, () ->
        schemaValidator.validate(null, ExportFormat.JSON, "1.0.0"));

    assertThrows(NullPointerException.class, () ->
        schemaValidator.validate("{}".getBytes(), null, "1.0.0"));

    assertThrows(NullPointerException.class, () ->
        schemaValidator.validate("{}".getBytes(), ExportFormat.JSON, null));

    assertThrows(NullPointerException.class, () ->
        schemaValidator.validateReport(null, ExportFormat.JSON, "1.0.0"));

    assertThrows(NullPointerException.class, () ->
        schemaValidator.registerSchema(null));
  }

  // Helper methods for creating test data

  private ComparisonReport createTestReport() {
    final ReportMetadata metadata = new ReportMetadata(
        "Test Suite",
        "1.0.0",
        Instant.now(),
        Duration.ofMinutes(5),
        List.of("JNI", "PANAMA"),
        Map.of("timeout", "30s"),
        "wasmtime4j-1.0.0"
    );

    final ReportSummary summary = new ReportSummary(
        2, 2, 2, 2, 1, 1, 0.95,
        Map.of("CONSISTENT", 1, "MOSTLY_CONSISTENT", 1)
    );

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(List.of("test1", "test2"))
        .build();
  }

  private ComparisonReport createEmptyReport() {
    final ReportMetadata metadata = new ReportMetadata(
        "Empty Suite",
        "1.0.0",
        Instant.now(),
        Duration.ZERO,
        List.of("JNI"),
        Map.of(),
        "wasmtime4j-1.0.0"
    );

    final ReportSummary summary = new ReportSummary(
        0, 0, 0, 0, 0, 0, 0.0, Map.of()
    );

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(Collections.emptyList())
        .build();
  }

  private ComparisonReport createReportWithSpecialCharacters() {
    final ReportMetadata metadata = new ReportMetadata(
        "Test\nSuite\twith\rspecial\u0001chars",
        "1.0.0",
        Instant.now(),
        Duration.ofMinutes(1),
        List.of("JNI"),
        Map.of("key,with,commas", "value\"with\"quotes\nand\nnewlines"),
        "wasmtime4j-1.0.0"
    );

    final ReportSummary summary = new ReportSummary(
        1, 1, 1, 1, 0, 0, 1.0, Map.of("CONSISTENT", 1)
    );

    return new ComparisonReport.Builder()
        .metadata(metadata)
        .summary(summary)
        .testExecutionOrder(List.of(
            "test,with,commas",
            "test\"with\"quotes",
            "test\nwith\nnewlines",
            "test\u0001with\u0002control\u0003chars"
        ))
        .build();
  }
}