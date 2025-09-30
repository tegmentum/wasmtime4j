package ai.tegmentum.wasmtime4j;

import java.util.Optional;

/**
 * Interface for parsing WebAssembly custom sections.
 *
 * <p>This interface defines the contract for parsing custom sections from WebAssembly modules.
 * Implementations provide support for different custom section formats including standard sections
 * like "name", "producers", and "target_features", as well as arbitrary custom sections.
 *
 * @since 1.0.0
 */
public interface CustomSectionParser {

  /**
   * Parses a custom section from raw binary data.
   *
   * @param name the name of the custom section
   * @param data the raw binary data of the section
   * @return the parsed custom section, or empty if parsing fails
   * @throws IllegalArgumentException if name or data is null
   */
  Optional<CustomSection> parseCustomSection(final String name, final byte[] data);

  /**
   * Parses a name section from raw binary data.
   *
   * @param data the raw binary data of the name section
   * @return the parsed name section, or empty if parsing fails
   * @throws IllegalArgumentException if data is null
   */
  Optional<NameSection> parseNameSection(final byte[] data);

  /**
   * Parses a producers section from raw binary data.
   *
   * @param data the raw binary data of the producers section
   * @return the parsed producers section, or empty if parsing fails
   * @throws IllegalArgumentException if data is null
   */
  Optional<ProducersSection> parseProducersSection(final byte[] data);

  /**
   * Parses a target features section from raw binary data.
   *
   * @param data the raw binary data of the target features section
   * @return the parsed target features section, or empty if parsing fails
   * @throws IllegalArgumentException if data is null
   */
  Optional<TargetFeaturesSection> parseTargetFeaturesSection(final byte[] data);

  /**
   * Checks if this parser supports a specific custom section type.
   *
   * @param sectionName the name of the section to check
   * @return true if the parser can handle this section type
   * @throws IllegalArgumentException if sectionName is null
   */
  boolean supports(final String sectionName);

  /**
   * Gets the custom section types supported by this parser.
   *
   * @return an immutable set of supported section types
   */
  java.util.Set<CustomSectionType> getSupportedTypes();

  /**
   * Validates the format of a custom section without full parsing.
   *
   * @param name the name of the custom section
   * @param data the raw binary data of the section
   * @return validation result
   * @throws IllegalArgumentException if name or data is null
   */
  CustomSectionValidationResult validateSection(final String name, final byte[] data);

  /**
   * Creates a custom section from structured data.
   *
   * @param name the section name
   * @param type the section type
   * @param structuredData the structured data to serialize
   * @return the created custom section, or empty if creation fails
   * @throws IllegalArgumentException if name, type, or structuredData is null
   */
  Optional<CustomSection> createCustomSection(
      final String name, final CustomSectionType type, final Object structuredData);

  /**
   * Serializes a name section to binary data.
   *
   * @param nameSection the name section to serialize
   * @return the serialized binary data, or empty if serialization fails
   * @throws IllegalArgumentException if nameSection is null
   */
  Optional<byte[]> serializeNameSection(final NameSection nameSection);

  /**
   * Serializes a producers section to binary data.
   *
   * @param producersSection the producers section to serialize
   * @return the serialized binary data, or empty if serialization fails
   * @throws IllegalArgumentException if producersSection is null
   */
  Optional<byte[]> serializeProducersSection(final ProducersSection producersSection);

  /**
   * Serializes a target features section to binary data.
   *
   * @param targetFeaturesSection the target features section to serialize
   * @return the serialized binary data, or empty if serialization fails
   * @throws IllegalArgumentException if targetFeaturesSection is null
   */
  Optional<byte[]> serializeTargetFeaturesSection(
      final TargetFeaturesSection targetFeaturesSection);
}
