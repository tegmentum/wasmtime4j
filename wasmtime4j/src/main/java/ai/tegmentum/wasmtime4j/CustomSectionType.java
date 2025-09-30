package ai.tegmentum.wasmtime4j;

/**
 * Enumeration of standard WebAssembly custom section types.
 *
 * <p>This enum defines the well-known custom section types standardized by the WebAssembly
 * specification and commonly used by toolchains and runtimes.
 *
 * @since 1.0.0
 */
public enum CustomSectionType {

  /**
   * The "name" section containing symbolic names for module definitions.
   *
   * <p>The name section provides human-readable names for functions, locals, types, modules, and
   * other WebAssembly constructs. This is primarily used for debugging and development tools.
   */
  NAME("name"),

  /**
   * The "producers" section containing toolchain and compiler information.
   *
   * <p>The producers section stores metadata about the tools and compilers used to generate the
   * WebAssembly module, including version information and compilation parameters.
   */
  PRODUCERS("producers"),

  /**
   * The "target_features" section containing platform feature requirements.
   *
   * <p>The target features section specifies WebAssembly features required by the module and
   * provides compatibility information for runtime engines.
   */
  TARGET_FEATURES("target_features"),

  /**
   * DWARF debugging information sections.
   *
   * <p>These sections contain DWARF debugging metadata including source line information, variable
   * locations, and type information for debugging tools.
   */
  DWARF(".debug_info", ".debug_line", ".debug_abbrev", ".debug_str", ".debug_ranges", ".debug_loc"),

  /**
   * Source mapping information for WebAssembly text format.
   *
   * <p>Source map sections provide mappings between compiled WebAssembly and original source code
   * locations for development and debugging purposes.
   */
  SOURCE_MAP("sourceMappingURL"),

  /**
   * Linking metadata for WebAssembly object files.
   *
   * <p>Linking sections contain relocation information, symbol tables, and other metadata required
   * for linking WebAssembly object files into complete modules.
   */
  LINKING("linking"),

  /**
   * Relocation information for WebAssembly object files.
   *
   * <p>Relocation sections specify how addresses and symbols should be resolved during the linking
   * process.
   */
  RELOC("reloc.CODE", "reloc.DATA"),

  /**
   * Unknown or unrecognized custom section type.
   *
   * <p>This type is used for custom sections that don't match any of the well-known types defined
   * above.
   */
  UNKNOWN();

  private final String[] names;

  CustomSectionType(final String... names) {
    this.names = names;
  }

  /**
   * Gets the standard names associated with this custom section type.
   *
   * @return array of section names, or empty array for UNKNOWN type
   */
  public String[] getNames() {
    return names.clone();
  }

  /**
   * Gets the primary name for this custom section type.
   *
   * @return the primary section name, or null for UNKNOWN type
   */
  public String getPrimaryName() {
    return names.length > 0 ? names[0] : null;
  }

  /**
   * Checks if this type matches the given section name.
   *
   * @param sectionName the name to check
   * @return true if the name matches this type
   * @throws IllegalArgumentException if sectionName is null
   */
  public boolean matches(final String sectionName) {
    if (sectionName == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    for (final String name : names) {
      if (name.equals(sectionName)) {
        return true;
      }
    }
    return false;
  }

  /**
   * Determines the custom section type from a section name.
   *
   * @param sectionName the name of the section
   * @return the corresponding CustomSectionType, or UNKNOWN if not recognized
   * @throws IllegalArgumentException if sectionName is null
   */
  public static CustomSectionType fromName(final String sectionName) {
    if (sectionName == null) {
      throw new IllegalArgumentException("Section name cannot be null");
    }

    for (final CustomSectionType type : values()) {
      if (type != UNKNOWN && type.matches(sectionName)) {
        return type;
      }
    }
    return UNKNOWN;
  }

  /**
   * Checks if this is a debugging-related section type.
   *
   * @return true if this section contains debugging information
   */
  public boolean isDebuggingSection() {
    return this == DWARF || this == SOURCE_MAP || this == NAME;
  }

  /**
   * Checks if this is a toolchain-related section type.
   *
   * @return true if this section contains toolchain metadata
   */
  public boolean isToolchainSection() {
    return this == PRODUCERS || this == TARGET_FEATURES;
  }

  /**
   * Checks if this is a linking-related section type.
   *
   * @return true if this section contains linking metadata
   */
  public boolean isLinkingSection() {
    return this == LINKING || this == RELOC;
  }
}
