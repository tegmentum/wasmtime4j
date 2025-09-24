package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Default implementation of CustomSectionMetadata.
 *
 * <p>This implementation provides comprehensive custom section metadata access and management
 * for WebAssembly modules.
 *
 * @since 1.0.0
 */
final class DefaultCustomSectionMetadata implements CustomSectionMetadata {

  private final List<CustomSection> customSections;
  private final CustomSectionParser parser;
  private final Map<String, List<CustomSection>> sectionsByName;
  private final Map<CustomSectionType, List<CustomSection>> sectionsByType;
  private volatile NameSection nameSection;
  private volatile ProducersSection producersSection;
  private volatile TargetFeaturesSection targetFeaturesSection;

  /**
   * Creates a new default custom section metadata instance.
   *
   * @param customSections the list of custom sections
   * @param parser the custom section parser
   * @throws IllegalArgumentException if customSections or parser is null
   */
  DefaultCustomSectionMetadata(final List<CustomSection> customSections,
                              final CustomSectionParser parser) {
    if (customSections == null) {
      throw new IllegalArgumentException("Custom sections cannot be null");
    }
    if (parser == null) {
      throw new IllegalArgumentException("Custom section parser cannot be null");
    }

    this.customSections = java.util.Collections.unmodifiableList(new java.util.ArrayList<>(customSections));
    this.parser = parser;

    // Pre-compute lookup maps for efficiency
    this.sectionsByName = this.customSections.stream()
        .collect(Collectors.groupingBy(
            CustomSection::getName,
            Collectors.collectingAndThen(
                Collectors.toList(),
                java.util.Collections::unmodifiableList)));

    this.sectionsByType = this.customSections.stream()
        .collect(Collectors.groupingBy(
            CustomSection::getType,
            Collectors.collectingAndThen(
                Collectors.toList(),
                java.util.Collections::unmodifiableList)));
  }

  @Override
  public List<CustomSection> getAllCustomSections() {
    return customSections;
  }

  @Override
  public List<CustomSection> getCustomSectionsByName(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    return sectionsByName.getOrDefault(name, java.util.Collections.emptyList());
  }

  @Override
  public List<CustomSection> getCustomSectionsByType(final CustomSectionType type) {
    if (type == null) {
      throw new IllegalArgumentException("Section type cannot be null");
    }

    return sectionsByType.getOrDefault(type, java.util.Collections.emptyList());
  }

  @Override
  public Optional<CustomSection> getFirstCustomSection(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    final List<CustomSection> sections = sectionsByName.get(name);
    return sections != null && !sections.isEmpty()
        ? Optional.of(sections.get(0))
        : Optional.empty();
  }

  @Override
  public boolean hasCustomSection(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    return sectionsByName.containsKey(name);
  }

  @Override
  public boolean hasCustomSection(final CustomSectionType type) {
    if (type == null) {
      throw new IllegalArgumentException("Section type cannot be null");
    }

    return sectionsByType.containsKey(type);
  }

  @Override
  public int getCustomSectionCount() {
    return customSections.size();
  }

  @Override
  public long getCustomSectionsTotalSize() {
    return customSections.stream()
        .mapToLong(CustomSection::getSize)
        .sum();
  }

  @Override
  public Set<String> getCustomSectionNames() {
    return java.util.Collections.unmodifiableSet(sectionsByName.keySet());
  }

  @Override
  public Set<CustomSectionType> getCustomSectionTypes() {
    return java.util.Collections.unmodifiableSet(sectionsByType.keySet());
  }

  @Override
  public Map<CustomSectionType, List<CustomSection>> getCustomSectionsByTypeMap() {
    return java.util.Collections.unmodifiableMap(sectionsByType);
  }

  @Override
  public Map<String, List<CustomSection>> getCustomSectionsByNameMap() {
    return java.util.Collections.unmodifiableMap(sectionsByName);
  }

  @Override
  public Optional<NameSection> getNameSection() {
    if (nameSection == null) {
      synchronized (this) {
        if (nameSection == null) {
          nameSection = parseNameSection();
        }
      }
    }
    return Optional.ofNullable(nameSection);
  }

  @Override
  public Optional<ProducersSection> getProducersSection() {
    if (producersSection == null) {
      synchronized (this) {
        if (producersSection == null) {
          producersSection = parseProducersSection();
        }
      }
    }
    return Optional.ofNullable(producersSection);
  }

  @Override
  public Optional<TargetFeaturesSection> getTargetFeaturesSection() {
    if (targetFeaturesSection == null) {
      synchronized (this) {
        if (targetFeaturesSection == null) {
          targetFeaturesSection = parseTargetFeaturesSection();
        }
      }
    }
    return Optional.ofNullable(targetFeaturesSection);
  }

  @Override
  public List<CustomSection> getDebuggingSections() {
    return customSections.stream()
        .filter(section -> section.getType().isDebuggingSection())
        .collect(Collectors.collectingAndThen(
            Collectors.toList(),
            java.util.Collections::unmodifiableList));
  }

  @Override
  public boolean hasDebuggingInfo() {
    return customSections.stream()
        .anyMatch(section -> section.getType().isDebuggingSection());
  }

  @Override
  public String getCustomSectionsSummary() {
    final StringBuilder sb = new StringBuilder();
    sb.append("Custom Sections Summary:\n");
    sb.append("  Total sections: ").append(getCustomSectionCount()).append("\n");
    sb.append("  Total size: ").append(getCustomSectionsTotalSize()).append(" bytes\n");

    if (hasCustomSection(CustomSectionType.NAME)) {
      sb.append("  Name section: present\n");
    }
    if (hasCustomSection(CustomSectionType.PRODUCERS)) {
      sb.append("  Producers section: present\n");
    }
    if (hasCustomSection(CustomSectionType.TARGET_FEATURES)) {
      sb.append("  Target features section: present\n");
    }
    if (hasDebuggingInfo()) {
      sb.append("  Debugging sections: ").append(getDebuggingSections().size()).append("\n");
    }

    final Map<CustomSectionType, List<CustomSection>> typeMap = getCustomSectionsByTypeMap();
    if (typeMap.containsKey(CustomSectionType.UNKNOWN)) {
      final List<CustomSection> unknownSections = typeMap.get(CustomSectionType.UNKNOWN);
      sb.append("  Unknown sections: ").append(unknownSections.size()).append("\n");
    }

    return sb.toString();
  }

  @Override
  public CustomSectionValidationResult validateCustomSections() {
    final CustomSectionValidationResult.Builder builder = CustomSectionValidationResult.builder();

    // Basic structural validation
    if (customSections.isEmpty()) {
      builder.addWarning("*", "No custom sections found");
      return builder.build();
    }

    // Security validation
    final CustomSectionValidationResult securityResult = CustomSectionSecurity.validateSecurity(customSections);
    builder.setErrors(securityResult.getErrors());
    builder.setWarnings(securityResult.getWarnings());

    // Validate individual sections based on their type
    for (final CustomSection section : customSections) {
      final CustomSectionValidationResult sectionResult = parser.validateSection(section.getName(), section.getData());

      // Combine results
      for (final CustomSectionValidationResult.ValidationIssue error : sectionResult.getErrors()) {
        builder.addError(error);
      }
      for (final CustomSectionValidationResult.ValidationIssue warning : sectionResult.getWarnings()) {
        builder.addWarning(warning);
      }
    }

    return builder.build();
  }

  private NameSection parseNameSection() {
    final Optional<CustomSection> section = getFirstCustomSection("name");
    if (!section.isPresent()) {
      return null;
    }

    return parser.parseNameSection(section.get().getData()).orElse(null);
  }

  private ProducersSection parseProducersSection() {
    final Optional<CustomSection> section = getFirstCustomSection("producers");
    if (!section.isPresent()) {
      return null;
    }

    return parser.parseProducersSection(section.get().getData()).orElse(null);
  }

  private TargetFeaturesSection parseTargetFeaturesSection() {
    final Optional<CustomSection> section = getFirstCustomSection("target_features");
    if (!section.isPresent()) {
      return null;
    }

    return parser.parseTargetFeaturesSection(section.get().getData()).orElse(null);
  }
}