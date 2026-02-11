package ai.tegmentum.wasmtime4j.metadata;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for accessing and manipulating WebAssembly custom section metadata.
 *
 * <p>This interface provides a unified API for working with custom sections in WebAssembly modules,
 * including both standard sections like "name" and "producers", as well as arbitrary custom
 * sections.
 *
 * @since 1.0.0
 */
public interface CustomSectionMetadata {

  /**
   * Gets all custom sections present in the module.
   *
   * @return an immutable list of custom sections
   */
  List<CustomSection> getAllCustomSections();

  /**
   * Gets custom sections by name.
   *
   * @param name the name of the sections to retrieve
   * @return an immutable list of matching custom sections
   * @throws IllegalArgumentException if name is null
   */
  List<CustomSection> getCustomSectionsByName(final String name);

  /**
   * Gets custom sections by type.
   *
   * @param type the type of sections to retrieve
   * @return an immutable list of matching custom sections
   * @throws IllegalArgumentException if type is null
   */
  List<CustomSection> getCustomSectionsByType(final CustomSectionType type);

  /**
   * Gets the first custom section with the specified name.
   *
   * @param name the name of the section to retrieve
   * @return the first matching section, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  Optional<CustomSection> getFirstCustomSection(final String name);

  /**
   * Checks if the module contains any custom section with the specified name.
   *
   * @param name the section name to check
   * @return true if at least one section with this name exists
   * @throws IllegalArgumentException if name is null
   */
  boolean hasCustomSection(final String name);

  /**
   * Checks if the module contains any custom section of the specified type.
   *
   * @param type the section type to check
   * @return true if at least one section of this type exists
   * @throws IllegalArgumentException if type is null
   */
  boolean hasCustomSection(final CustomSectionType type);

  /**
   * Gets the total number of custom sections in the module.
   *
   * @return the count of custom sections
   */
  int getCustomSectionCount();

  /**
   * Gets the total size of all custom sections in bytes.
   *
   * @return the total size of custom sections
   */
  long getCustomSectionsTotalSize();

  /**
   * Gets all unique custom section names present in the module.
   *
   * @return an immutable set of section names
   */
  java.util.Set<String> getCustomSectionNames();

  /**
   * Gets all custom section types present in the module.
   *
   * @return an immutable set of section types
   */
  java.util.Set<CustomSectionType> getCustomSectionTypes();

  /**
   * Gets custom sections grouped by their type.
   *
   * @return an immutable map of section types to their sections
   */
  Map<CustomSectionType, List<CustomSection>> getCustomSectionsByTypeMap();

  /**
   * Gets custom sections grouped by their name.
   *
   * @return an immutable map of section names to their sections
   */
  Map<String, List<CustomSection>> getCustomSectionsByNameMap();

  /**
   * Gets the name section if present.
   *
   * @return the name section, or empty if not found
   */
  Optional<NameSection> getNameSection();

  /**
   * Gets the producers section if present.
   *
   * @return the producers section, or empty if not found
   */
  Optional<ProducersSection> getProducersSection();

  /**
   * Gets the target features section if present.
   *
   * @return the target features section, or empty if not found
   */
  Optional<TargetFeaturesSection> getTargetFeaturesSection();

  /**
   * Gets debugging sections (DWARF) if present.
   *
   * @return list of debugging sections
   */
  List<CustomSection> getDebuggingSections();

  /**
   * Checks if the module has debugging information.
   *
   * @return true if debugging sections are present
   */
  boolean hasDebuggingInfo();

  /**
   * Gets a summary of all custom sections for diagnostic purposes.
   *
   * @return a human-readable summary of custom sections
   */
  String getCustomSectionsSummary();

  /**
   * Validates the integrity and format of all custom sections.
   *
   * @return validation result with any errors or warnings
   */
  CustomSectionValidationResult validateCustomSections();
}
