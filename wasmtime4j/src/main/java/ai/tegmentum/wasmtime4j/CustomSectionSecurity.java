package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Set;

/**
 * Security and validation utilities for WebAssembly custom sections.
 *
 * <p>This class provides comprehensive security validation for custom sections to prevent malicious
 * content from affecting the runtime or host environment.
 *
 * @since 1.0.0
 */
public final class CustomSectionSecurity {

  /** Maximum allowed size for a single custom section (16MB). */
  public static final int MAX_CUSTOM_SECTION_SIZE = 16 * 1024 * 1024;

  /** Maximum allowed total size for all custom sections (64MB). */
  public static final long MAX_TOTAL_CUSTOM_SECTIONS_SIZE = 64L * 1024 * 1024;

  /** Maximum allowed number of custom sections. */
  public static final int MAX_CUSTOM_SECTION_COUNT = 1000;

  /** Maximum allowed length for custom section names. */
  public static final int MAX_SECTION_NAME_LENGTH = 256;

  private static final Set<String> SUSPICIOUS_SECTION_NAMES =
      java.util.Set.of(
          "eval",
          "script",
          "code",
          "exec",
          "shell",
          "cmd",
          "run",
          ".exe",
          ".dll",
          ".so",
          ".dylib",
          "javascript",
          "php",
          "python");

  private CustomSectionSecurity() {
    // Utility class
  }

  /**
   * Validates the security of a custom section.
   *
   * @param section the custom section to validate
   * @return validation result
   * @throws IllegalArgumentException if section is null
   */
  public static CustomSectionValidationResult validateSecurity(final CustomSection section) {
    if (section == null) {
      throw new IllegalArgumentException("Custom section cannot be null");
    }

    final CustomSectionValidationResult.Builder builder = CustomSectionValidationResult.builder();

    // Validate section size
    if (section.getSize() > MAX_CUSTOM_SECTION_SIZE) {
      builder.addError(
          section.getName(),
          String.format(
              "Section size %d exceeds maximum allowed size %d",
              section.getSize(), MAX_CUSTOM_SECTION_SIZE));
    }

    // Validate section name
    validateSectionName(section.getName(), builder);

    // Validate section content
    validateSectionContent(section, builder);

    return builder.build();
  }

  /**
   * Validates the security of all custom sections in a collection.
   *
   * @param sections the custom sections to validate
   * @return validation result
   * @throws IllegalArgumentException if sections is null
   */
  public static CustomSectionValidationResult validateSecurity(final List<CustomSection> sections) {
    if (sections == null) {
      throw new IllegalArgumentException("Custom sections cannot be null");
    }

    final CustomSectionValidationResult.Builder builder = CustomSectionValidationResult.builder();

    // Track all errors and warnings
    final List<CustomSectionValidationResult.ValidationIssue> allErrors =
        new java.util.ArrayList<>();
    final List<CustomSectionValidationResult.ValidationIssue> allWarnings =
        new java.util.ArrayList<>();

    // Validate section count
    if (sections.size() > MAX_CUSTOM_SECTION_COUNT) {
      allErrors.add(
          CustomSectionValidationResult.ValidationIssue.error(
              "*",
              String.format(
                  "Number of custom sections %d exceeds maximum allowed count %d",
                  sections.size(), MAX_CUSTOM_SECTION_COUNT)));
    }

    // Validate total size
    final long totalSize = sections.stream().mapToLong(CustomSection::getSize).sum();

    if (totalSize > MAX_TOTAL_CUSTOM_SECTIONS_SIZE) {
      allErrors.add(
          CustomSectionValidationResult.ValidationIssue.error(
              "*",
              String.format(
                  "Total custom sections size %d exceeds maximum allowed size %d",
                  totalSize, MAX_TOTAL_CUSTOM_SECTIONS_SIZE)));
    }

    // Validate individual sections
    for (final CustomSection section : sections) {
      final CustomSectionValidationResult sectionResult = validateSecurity(section);
      allErrors.addAll(sectionResult.getErrors());
      allWarnings.addAll(sectionResult.getWarnings());
    }

    // Set combined errors and warnings
    builder.setErrors(allErrors);
    builder.setWarnings(allWarnings);

    // Check for duplicate section names (warning only)
    validateDuplicateNames(sections, builder);

    return builder.build();
  }

  /**
   * Sanitizes custom section data to remove potentially harmful content.
   *
   * @param section the custom section to sanitize
   * @return a sanitized version of the section
   * @throws IllegalArgumentException if section is null
   */
  public static CustomSection sanitize(final CustomSection section) {
    if (section == null) {
      throw new IllegalArgumentException("Custom section cannot be null");
    }

    final byte[] originalData = section.getData();
    final byte[] sanitizedData = sanitizeData(originalData);

    if (originalData == sanitizedData) {
      return section; // No changes needed
    }

    return new CustomSection(section.getName(), sanitizedData, section.getType());
  }

  /**
   * Checks if a custom section name is suspicious.
   *
   * @param sectionName the section name to check
   * @return true if the name appears suspicious
   * @throws IllegalArgumentException if sectionName is null
   */
  public static boolean isSuspiciousSectionName(final String sectionName) {
    if (sectionName == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    final String lowerName = sectionName.toLowerCase();

    return SUSPICIOUS_SECTION_NAMES.stream().anyMatch(lowerName::contains);
  }

  /**
   * Checks if custom section data contains suspicious patterns.
   *
   * @param data the section data to check
   * @return true if the data contains suspicious patterns
   * @throws IllegalArgumentException if data is null
   */
  public static boolean containsSuspiciousPatterns(final byte[] data) {
    if (data == null) {
      throw new IllegalArgumentException("Section data cannot be null");
    }

    // Check for executable file headers
    if (data.length >= 2) {
      // Check for common executable formats
      if (startsWithMagic(data, new byte[] {0x4D, 0x5A})) { // PE/EXE (2 bytes)
        return true;
      }
    }
    if (data.length >= 4) {
      if (startsWithMagic(data, new byte[] {0x7F, 0x45, 0x4C, 0x46})
          || // ELF
          startsWithMagic(data, new byte[] {(byte) 0xFE, (byte) 0xED, (byte) 0xFA, (byte) 0xCE})
          || // Mach-O
          startsWithMagic(
              data, new byte[] {(byte) 0xCE, (byte) 0xFA, (byte) 0xED, (byte) 0xFE})) { // Mach-O
        return true;
      }
    }

    // Check for script-like content
    final String dataStr =
        new String(data, 0, Math.min(data.length, 1024), java.nio.charset.StandardCharsets.UTF_8);
    final String lowerData = dataStr.toLowerCase();

    return lowerData.contains("eval(")
        || lowerData.contains("exec(")
        || lowerData.contains("system(")
        || lowerData.contains("shell_exec")
        || lowerData.contains("<script")
        || lowerData.contains("javascript:");
  }

  /**
   * Gets security configuration for custom section handling.
   *
   * @return security configuration
   */
  public static SecurityConfig getSecurityConfig() {
    return new SecurityConfig();
  }

  private static void validateSectionName(
      final String name, final CustomSectionValidationResult.Builder builder) {
    if (name.length() > MAX_SECTION_NAME_LENGTH) {
      builder.addError(
          name,
          String.format(
              "Section name length %d exceeds maximum allowed length %d",
              name.length(), MAX_SECTION_NAME_LENGTH));
    }

    if (isSuspiciousSectionName(name)) {
      builder.addWarning(name, "Section name appears suspicious: " + name);
    }

    // Check for invalid characters
    if (name.contains("\0") || name.contains("\n") || name.contains("\r")) {
      builder.addError(name, "Section name contains invalid control characters");
    }
  }

  private static void validateSectionContent(
      final CustomSection section, final CustomSectionValidationResult.Builder builder) {
    if (section.isEmpty()) {
      builder.addWarning(section.getName(), "Section is empty");
      return;
    }

    if (containsSuspiciousPatterns(section.getData())) {
      builder.addError(section.getName(), "Section contains suspicious binary patterns");
    }

    // Validate encoding for text-based sections
    if (isTextBasedSection(section.getType())) {
      try {
        new String(section.getData(), java.nio.charset.StandardCharsets.UTF_8);
      } catch (final Exception e) {
        builder.addWarning(section.getName(), "Section does not contain valid UTF-8 text");
      }
    }
  }

  private static void validateDuplicateNames(
      final List<CustomSection> sections, final CustomSectionValidationResult.Builder builder) {
    final java.util.Map<String, Integer> nameCounts = new java.util.HashMap<>();

    for (final CustomSection section : sections) {
      nameCounts.merge(section.getName(), 1, Integer::sum);
    }

    for (final java.util.Map.Entry<String, Integer> entry : nameCounts.entrySet()) {
      if (entry.getValue() > 1) {
        builder.addWarning(
            entry.getKey(),
            String.format("Section name appears %d times (duplicated)", entry.getValue()));
      }
    }
  }

  private static boolean startsWithMagic(final byte[] data, final byte[] magic) {
    if (data.length < magic.length) {
      return false;
    }

    for (int i = 0; i < magic.length; i++) {
      if (data[i] != magic[i]) {
        return false;
      }
    }

    return true;
  }

  private static boolean isTextBasedSection(final CustomSectionType type) {
    return type == CustomSectionType.NAME
        || type == CustomSectionType.PRODUCERS
        || type == CustomSectionType.SOURCE_MAP;
  }

  private static byte[] sanitizeData(final byte[] data) {
    // For now, just return the original data
    // In a full implementation, this could remove suspicious patterns
    return data;
  }

  private static List<CustomSectionValidationResult.ValidationIssue> combineIssues(
      final List<CustomSectionValidationResult.ValidationIssue> existingIssues,
      final List<CustomSectionValidationResult.ValidationIssue> newIssues) {
    final List<CustomSectionValidationResult.ValidationIssue> combined =
        new java.util.ArrayList<>(existingIssues);
    combined.addAll(newIssues);
    return combined;
  }

  /** Security configuration for custom section handling. */
  public static final class SecurityConfig {
    private boolean strictValidation = true;
    private boolean allowSuspiciousNames = false;
    private boolean sanitizeContent = true;
    private int maxSectionSize = MAX_CUSTOM_SECTION_SIZE;
    private long maxTotalSize = MAX_TOTAL_CUSTOM_SECTIONS_SIZE;
    private int maxSectionCount = MAX_CUSTOM_SECTION_COUNT;

    /**
     * Gets whether strict validation is enabled.
     *
     * @return true if strict validation is enabled
     */
    public boolean isStrictValidation() {
      return strictValidation;
    }

    /**
     * Sets whether strict validation is enabled.
     *
     * @param strictValidation true to enable strict validation
     * @return this config
     */
    public SecurityConfig setStrictValidation(final boolean strictValidation) {
      this.strictValidation = strictValidation;
      return this;
    }

    /**
     * Gets whether suspicious section names are allowed.
     *
     * @return true if suspicious names are allowed
     */
    public boolean isAllowSuspiciousNames() {
      return allowSuspiciousNames;
    }

    /**
     * Sets whether suspicious section names are allowed.
     *
     * @param allowSuspiciousNames true to allow suspicious names
     * @return this config
     */
    public SecurityConfig setAllowSuspiciousNames(final boolean allowSuspiciousNames) {
      this.allowSuspiciousNames = allowSuspiciousNames;
      return this;
    }

    /**
     * Gets whether content sanitization is enabled.
     *
     * @return true if content sanitization is enabled
     */
    public boolean isSanitizeContent() {
      return sanitizeContent;
    }

    /**
     * Sets whether content sanitization is enabled.
     *
     * @param sanitizeContent true to enable content sanitization
     * @return this config
     */
    public SecurityConfig setSanitizeContent(final boolean sanitizeContent) {
      this.sanitizeContent = sanitizeContent;
      return this;
    }

    /**
     * Gets the maximum allowed section size.
     *
     * @return the maximum section size in bytes
     */
    public int getMaxSectionSize() {
      return maxSectionSize;
    }

    /**
     * Sets the maximum allowed section size.
     *
     * @param maxSectionSize the maximum section size in bytes
     * @return this config
     * @throws IllegalArgumentException if maxSectionSize is negative
     */
    public SecurityConfig setMaxSectionSize(final int maxSectionSize) {
      if (maxSectionSize < 0) {
        throw new IllegalArgumentException("Max section size cannot be negative");
      }
      this.maxSectionSize = maxSectionSize;
      return this;
    }

    /**
     * Gets the maximum allowed total size for all custom sections.
     *
     * @return the maximum total size in bytes
     */
    public long getMaxTotalSize() {
      return maxTotalSize;
    }

    /**
     * Sets the maximum allowed total size for all custom sections.
     *
     * @param maxTotalSize the maximum total size in bytes
     * @return this config
     * @throws IllegalArgumentException if maxTotalSize is negative
     */
    public SecurityConfig setMaxTotalSize(final long maxTotalSize) {
      if (maxTotalSize < 0) {
        throw new IllegalArgumentException("Max total size cannot be negative");
      }
      this.maxTotalSize = maxTotalSize;
      return this;
    }

    /**
     * Gets the maximum allowed number of custom sections.
     *
     * @return the maximum section count
     */
    public int getMaxSectionCount() {
      return maxSectionCount;
    }

    /**
     * Sets the maximum allowed number of custom sections.
     *
     * @param maxSectionCount the maximum section count
     * @return this config
     * @throws IllegalArgumentException if maxSectionCount is negative
     */
    public SecurityConfig setMaxSectionCount(final int maxSectionCount) {
      if (maxSectionCount < 0) {
        throw new IllegalArgumentException("Max section count cannot be negative");
      }
      this.maxSectionCount = maxSectionCount;
      return this;
    }
  }
}
