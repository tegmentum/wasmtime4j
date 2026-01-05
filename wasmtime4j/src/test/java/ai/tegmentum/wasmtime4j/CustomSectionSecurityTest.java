package ai.tegmentum.wasmtime4j;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/** Tests for CustomSectionSecurity functionality. */
final class CustomSectionSecurityTest {

  @Test
  void testValidCustomSection() {
    final byte[] data = "Hello, World!".getBytes(StandardCharsets.UTF_8);
    final CustomSection section = new CustomSection("greeting", data, CustomSectionType.UNKNOWN);

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
    assertTrue(result.getWarnings().isEmpty());
  }

  @Test
  void testOversizedCustomSection() {
    // Create a section that exceeds the maximum size
    final byte[] data = new byte[CustomSectionSecurity.MAX_CUSTOM_SECTION_SIZE + 1];
    final CustomSection section = new CustomSection("oversized", data, CustomSectionType.UNKNOWN);

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());

    final CustomSectionValidationResult.ValidationIssue error = result.getErrors().get(0);
    assertEquals("oversized", error.getSectionName());
    assertTrue(error.getMessage().contains("exceeds maximum allowed size"));
  }

  @Test
  void testSuspiciousSectionName() {
    final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
    final CustomSection section = new CustomSection("eval_script", data, CustomSectionType.UNKNOWN);

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
    assertTrue(result.isValid()); // Should be valid but with warnings
    assertTrue(result.getErrors().isEmpty());
    assertFalse(result.getWarnings().isEmpty());

    final CustomSectionValidationResult.ValidationIssue warning = result.getWarnings().get(0);
    assertEquals("eval_script", warning.getSectionName());
    assertTrue(warning.getMessage().contains("suspicious"));
  }

  @Test
  void testLongSectionName() {
    final String longName = "a".repeat(CustomSectionSecurity.MAX_SECTION_NAME_LENGTH + 1);
    final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
    final CustomSection section = new CustomSection(longName, data, CustomSectionType.UNKNOWN);

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());

    final CustomSectionValidationResult.ValidationIssue error = result.getErrors().get(0);
    assertTrue(error.getMessage().contains("length"));
    assertTrue(error.getMessage().contains("exceeds"));
  }

  @Test
  void testSectionNameWithControlCharacters() {
    final byte[] data = "data".getBytes(StandardCharsets.UTF_8);
    final CustomSection section = new CustomSection("test\0name", data, CustomSectionType.UNKNOWN);

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());

    final CustomSectionValidationResult.ValidationIssue error = result.getErrors().get(0);
    assertTrue(error.getMessage().contains("invalid control characters"));
  }

  @Test
  void testEmptySection() {
    final byte[] data = {};
    final CustomSection section = new CustomSection("empty", data, CustomSectionType.UNKNOWN);

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(section);
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
    assertFalse(result.getWarnings().isEmpty());

    final CustomSectionValidationResult.ValidationIssue warning = result.getWarnings().get(0);
    assertTrue(warning.getMessage().contains("empty"));
  }

  @Test
  void testMultipleSectionsValidation() {
    final List<CustomSection> sections = new ArrayList<>();

    // Add normal sections
    for (int i = 0; i < 5; i++) {
      sections.add(
          new CustomSection(
              "section" + i,
              ("data" + i).getBytes(StandardCharsets.UTF_8),
              CustomSectionType.UNKNOWN));
    }

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(sections);
    assertTrue(result.isValid());
    assertTrue(result.getErrors().isEmpty());
  }

  @Test
  void testTooManySections() {
    final List<CustomSection> sections = new ArrayList<>();

    // Add too many sections
    for (int i = 0; i < CustomSectionSecurity.MAX_CUSTOM_SECTION_COUNT + 1; i++) {
      sections.add(
          new CustomSection(
              "section" + i, "data".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN));
    }

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(sections);
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());

    boolean foundCountError =
        result.getErrors().stream()
            .anyMatch(error -> error.getMessage().contains("exceeds maximum allowed count"));
    assertTrue(foundCountError);
  }

  @Test
  void testTotalSizeLimit() {
    final List<CustomSection> sections = new ArrayList<>();
    final int sectionSize = 1024 * 1024; // 1MB each
    final int sectionCount =
        (int) (CustomSectionSecurity.MAX_TOTAL_CUSTOM_SECTIONS_SIZE / sectionSize) + 1;

    // Add sections that exceed total size limit
    for (int i = 0; i < sectionCount; i++) {
      sections.add(
          new CustomSection("section" + i, new byte[sectionSize], CustomSectionType.UNKNOWN));
    }

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(sections);
    assertFalse(result.isValid());
    assertFalse(result.getErrors().isEmpty());

    boolean foundSizeError =
        result.getErrors().stream()
            .anyMatch(error -> error.getMessage().contains("Total custom sections size"));
    assertTrue(foundSizeError);
  }

  @Test
  void testDuplicateSectionNames() {
    final List<CustomSection> sections = new ArrayList<>();
    sections.add(
        new CustomSection(
            "duplicate", "data1".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN));
    sections.add(
        new CustomSection(
            "duplicate", "data2".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN));
    sections.add(
        new CustomSection(
            "unique", "data3".getBytes(StandardCharsets.UTF_8), CustomSectionType.UNKNOWN));

    final CustomSectionValidationResult result = CustomSectionSecurity.validateSecurity(sections);
    assertTrue(result.isValid()); // Valid but with warnings
    assertTrue(result.getErrors().isEmpty());
    assertFalse(result.getWarnings().isEmpty());

    boolean foundDuplicateWarning =
        result.getWarnings().stream()
            .anyMatch(warning -> warning.getMessage().contains("duplicated"));
    assertTrue(foundDuplicateWarning);
  }

  @Test
  void testSuspiciousSectionNames() {
    final String[] suspiciousNames = {
      "eval", "script", "exec", "shell", ".exe", ".dll", "javascript"
    };

    for (final String name : suspiciousNames) {
      assertTrue(
          CustomSectionSecurity.isSuspiciousSectionName(name),
          "Name should be suspicious: " + name);
      assertTrue(
          CustomSectionSecurity.isSuspiciousSectionName(name.toUpperCase()),
          "Name should be suspicious (case insensitive): " + name.toUpperCase());
    }

    final String[] normalNames = {"name", "producers", "data", "metadata"};
    for (final String name : normalNames) {
      assertFalse(
          CustomSectionSecurity.isSuspiciousSectionName(name),
          "Name should not be suspicious: " + name);
    }
  }

  @Test
  void testSuspiciousPatterns() {
    // Test executable file headers
    assertTrue(CustomSectionSecurity.containsSuspiciousPatterns(new byte[] {0x4D, 0x5A})); // PE
    assertTrue(
        CustomSectionSecurity.containsSuspiciousPatterns(
            new byte[] {0x7F, 0x45, 0x4C, 0x46})); // ELF
    assertTrue(
        CustomSectionSecurity.containsSuspiciousPatterns(
            new byte[] {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE})); // Mach-O

    // Test script patterns
    assertTrue(
        CustomSectionSecurity.containsSuspiciousPatterns(
            "eval(malicious)".getBytes(StandardCharsets.UTF_8)));
    assertTrue(
        CustomSectionSecurity.containsSuspiciousPatterns(
            "system('rm -rf /')".getBytes(StandardCharsets.UTF_8)));
    assertTrue(
        CustomSectionSecurity.containsSuspiciousPatterns(
            "<script>alert()</script>".getBytes(StandardCharsets.UTF_8)));

    // Test normal data
    assertFalse(
        CustomSectionSecurity.containsSuspiciousPatterns(
            "normal data".getBytes(StandardCharsets.UTF_8)));
    assertFalse(
        CustomSectionSecurity.containsSuspiciousPatterns(
            "function_names".getBytes(StandardCharsets.UTF_8)));
  }

  @Test
  void testSanitize() {
    final byte[] data = "normal data".getBytes(StandardCharsets.UTF_8);
    final CustomSection section = new CustomSection("test", data, CustomSectionType.UNKNOWN);

    final CustomSection sanitized = CustomSectionSecurity.sanitize(section);

    // For now, sanitization doesn't change anything for normal data
    assertEquals(section, sanitized);
  }

  @Test
  void testSecurityConfig() {
    final CustomSectionSecurity.SecurityConfig config = CustomSectionSecurity.getSecurityConfig();

    // Test default values
    assertTrue(config.isStrictValidation());
    assertFalse(config.isAllowSuspiciousNames());
    assertTrue(config.isSanitizeContent());
    assertEquals(CustomSectionSecurity.MAX_CUSTOM_SECTION_SIZE, config.getMaxSectionSize());
    assertEquals(CustomSectionSecurity.MAX_TOTAL_CUSTOM_SECTIONS_SIZE, config.getMaxTotalSize());
    assertEquals(CustomSectionSecurity.MAX_CUSTOM_SECTION_COUNT, config.getMaxSectionCount());

    // Test configuration changes
    config
        .setStrictValidation(false)
        .setAllowSuspiciousNames(true)
        .setSanitizeContent(false)
        .setMaxSectionSize(1000)
        .setMaxTotalSize(5000)
        .setMaxSectionCount(10);

    assertFalse(config.isStrictValidation());
    assertTrue(config.isAllowSuspiciousNames());
    assertFalse(config.isSanitizeContent());
    assertEquals(1000, config.getMaxSectionSize());
    assertEquals(5000, config.getMaxTotalSize());
    assertEquals(10, config.getMaxSectionCount());
  }

  @Test
  void testSecurityConfigInvalidValues() {
    final CustomSectionSecurity.SecurityConfig config = CustomSectionSecurity.getSecurityConfig();

    assertThrows(IllegalArgumentException.class, () -> config.setMaxSectionSize(-1));
    assertThrows(IllegalArgumentException.class, () -> config.setMaxTotalSize(-1));
    assertThrows(IllegalArgumentException.class, () -> config.setMaxSectionCount(-1));
  }

  @Test
  void testNullArgumentValidation() {
    assertThrows(
        IllegalArgumentException.class,
        () -> CustomSectionSecurity.validateSecurity((CustomSection) null));
    assertThrows(
        IllegalArgumentException.class,
        () -> CustomSectionSecurity.validateSecurity((List<CustomSection>) null));
    assertThrows(IllegalArgumentException.class, () -> CustomSectionSecurity.sanitize(null));
    assertThrows(
        IllegalArgumentException.class, () -> CustomSectionSecurity.isSuspiciousSectionName(null));
    assertThrows(
        IllegalArgumentException.class,
        () -> CustomSectionSecurity.containsSuspiciousPatterns(null));
  }
}
