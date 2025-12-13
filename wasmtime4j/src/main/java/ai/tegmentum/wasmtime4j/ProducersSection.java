package ai.tegmentum.wasmtime4j;

import java.util.List;
import java.util.Map;

/**
 * Represents a WebAssembly "producers" custom section.
 *
 * <p>The producers section contains metadata about the tools and compilers used to generate the
 * WebAssembly module. This information is useful for debugging, optimization, and compatibility
 * analysis.
 *
 * @since 1.0.0
 */
public final class ProducersSection {

  private final List<ProducerEntry> languages;
  private final List<ProducerEntry> processedBy;
  private final List<ProducerEntry> sdk;

  private ProducersSection(final Builder builder) {
    this.languages =
        builder.languages == null
            ? java.util.Collections.emptyList()
            : java.util.Collections.unmodifiableList(builder.languages);
    this.processedBy =
        builder.processedBy == null
            ? java.util.Collections.emptyList()
            : java.util.Collections.unmodifiableList(builder.processedBy);
    this.sdk =
        builder.sdk == null
            ? java.util.Collections.emptyList()
            : java.util.Collections.unmodifiableList(builder.sdk);
  }

  /**
   * Gets the programming languages used to create this module.
   *
   * @return an immutable list of language producer entries
   */
  public List<ProducerEntry> getLanguages() {
    return languages;
  }

  /**
   * Gets the tools that processed this module.
   *
   * @return an immutable list of tool producer entries
   */
  public List<ProducerEntry> getProcessedBy() {
    return processedBy;
  }

  /**
   * Gets the SDKs used to create this module.
   *
   * @return an immutable list of SDK producer entries
   */
  public List<ProducerEntry> getSdk() {
    return sdk;
  }

  /**
   * Checks if this producers section is empty.
   *
   * @return true if no producer information is available
   */
  public boolean isEmpty() {
    return languages.isEmpty() && processedBy.isEmpty() && sdk.isEmpty();
  }

  /**
   * Gets a summary of this producers section.
   *
   * @return a human-readable summary
   */
  public String getSummary() {
    return String.format(
        "ProducersSection{languages=%d, processedBy=%d, sdk=%d}",
        languages.size(), processedBy.size(), sdk.size());
  }

  /**
   * Gets all producer entries as a flat list.
   *
   * @return all producer entries combined
   */
  public List<ProducerEntry> getAllEntries() {
    final List<ProducerEntry> allEntries = new java.util.ArrayList<>();
    allEntries.addAll(languages);
    allEntries.addAll(processedBy);
    allEntries.addAll(sdk);
    return java.util.Collections.unmodifiableList(allEntries);
  }

  /**
   * Finds a producer entry by name.
   *
   * @param name the producer name to find
   * @return the first matching producer entry, or empty if not found
   * @throws IllegalArgumentException if name is null
   */
  public java.util.Optional<ProducerEntry> findProducerByName(final String name) {
    if (name == null) {
      throw new IllegalArgumentException("Producer name cannot be null");
    }

    return getAllEntries().stream().filter(entry -> name.equals(entry.getName())).findFirst();
  }

  /**
   * Finds all producer entries of a specific type.
   *
   * @param type the producer type to find
   * @return list of matching producer entries
   * @throws IllegalArgumentException if type is null
   */
  public List<ProducerEntry> findProducersByType(final ProducerType type) {
    if (type == null) {
      throw new IllegalArgumentException("Producer type cannot be null");
    }

    switch (type) {
      case LANGUAGE:
        return new java.util.ArrayList<>(languages);
      case PROCESSED_BY:
        return new java.util.ArrayList<>(processedBy);
      case SDK:
        return new java.util.ArrayList<>(sdk);
      default:
        return java.util.Collections.emptyList();
    }
  }

  /**
   * Creates a new builder for constructing a ProducersSection.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  /** Builder for constructing ProducersSection instances. */
  public static final class Builder {
    private List<ProducerEntry> languages;
    private List<ProducerEntry> processedBy;
    private List<ProducerEntry> sdk;

    private Builder() {}

    /**
     * Sets the language producer entries.
     *
     * @param languages list of language entries
     * @return this builder
     */
    public Builder setLanguages(final List<ProducerEntry> languages) {
      this.languages = languages == null ? null : new java.util.ArrayList<>(languages);
      return this;
    }

    /**
     * Sets the processed-by producer entries.
     *
     * @param processedBy list of tool entries
     * @return this builder
     */
    public Builder setProcessedBy(final List<ProducerEntry> processedBy) {
      this.processedBy = processedBy == null ? null : new java.util.ArrayList<>(processedBy);
      return this;
    }

    /**
     * Sets the SDK producer entries.
     *
     * @param sdk list of SDK entries
     * @return this builder
     */
    public Builder setSdk(final List<ProducerEntry> sdk) {
      this.sdk = sdk == null ? null : new java.util.ArrayList<>(sdk);
      return this;
    }

    /**
     * Adds a language producer entry.
     *
     * @param entry the language entry to add
     * @return this builder
     * @throws IllegalArgumentException if entry is null
     */
    public Builder addLanguage(final ProducerEntry entry) {
      if (entry == null) {
        throw new IllegalArgumentException("Producer entry cannot be null");
      }
      if (languages == null) {
        languages = new java.util.ArrayList<>();
      }
      languages.add(entry);
      return this;
    }

    /**
     * Adds a processed-by producer entry.
     *
     * @param entry the tool entry to add
     * @return this builder
     * @throws IllegalArgumentException if entry is null
     */
    public Builder addProcessedBy(final ProducerEntry entry) {
      if (entry == null) {
        throw new IllegalArgumentException("Producer entry cannot be null");
      }
      if (processedBy == null) {
        processedBy = new java.util.ArrayList<>();
      }
      processedBy.add(entry);
      return this;
    }

    /**
     * Adds an SDK producer entry.
     *
     * @param entry the SDK entry to add
     * @return this builder
     * @throws IllegalArgumentException if entry is null
     */
    public Builder addSdk(final ProducerEntry entry) {
      if (entry == null) {
        throw new IllegalArgumentException("Producer entry cannot be null");
      }
      if (sdk == null) {
        sdk = new java.util.ArrayList<>();
      }
      sdk.add(entry);
      return this;
    }

    /**
     * Builds the ProducersSection.
     *
     * @return a new ProducersSection instance
     */
    public ProducersSection build() {
      return new ProducersSection(this);
    }
  }

  /** Represents a single producer entry in the producers section. */
  public static final class ProducerEntry {
    private final String name;
    private final String version;
    private final Map<String, String> metadata;

    /**
     * Creates a new producer entry.
     *
     * @param name the producer name
     * @param version the producer version
     * @param metadata additional metadata
     * @throws IllegalArgumentException if name is null or empty
     */
    public ProducerEntry(
        final String name, final String version, final Map<String, String> metadata) {
      if (name == null || name.trim().isEmpty()) {
        throw new IllegalArgumentException("Producer name cannot be null or empty");
      }
      this.name = name;
      this.version = version;
      this.metadata =
          metadata == null
              ? java.util.Collections.emptyMap()
              : java.util.Collections.unmodifiableMap(new java.util.HashMap<>(metadata));
    }

    /**
     * Creates a new producer entry with just name and version.
     *
     * @param name the producer name
     * @param version the producer version
     * @throws IllegalArgumentException if name is null or empty
     */
    public ProducerEntry(final String name, final String version) {
      this(name, version, null);
    }

    /**
     * Gets the producer name.
     *
     * @return the producer name
     */
    public String getName() {
      return name;
    }

    /**
     * Gets the producer version.
     *
     * @return the producer version, or null if not specified
     */
    public String getVersion() {
      return version;
    }

    /**
     * Gets additional metadata for this producer.
     *
     * @return an immutable map of metadata
     */
    public Map<String, String> getMetadata() {
      return metadata;
    }

    /**
     * Checks if this producer entry has version information.
     *
     * @return true if version is not null and not empty
     */
    public boolean hasVersion() {
      return version != null && !version.trim().isEmpty();
    }

    /**
     * Checks if this producer entry has metadata.
     *
     * @return true if metadata is not empty
     */
    public boolean hasMetadata() {
      return !metadata.isEmpty();
    }

    @Override
    public String toString() {
      final StringBuilder sb = new StringBuilder();
      sb.append("ProducerEntry{name='").append(name).append("'");
      if (hasVersion()) {
        sb.append(", version='").append(version).append("'");
      }
      if (hasMetadata()) {
        sb.append(", metadata=").append(metadata);
      }
      sb.append("}");
      return sb.toString();
    }

    @Override
    public boolean equals(final Object obj) {
      if (this == obj) {
        return true;
      }
      if (obj == null || getClass() != obj.getClass()) {
        return false;
      }
      final ProducerEntry that = (ProducerEntry) obj;
      return name.equals(that.name)
          && java.util.Objects.equals(version, that.version)
          && metadata.equals(that.metadata);
    }

    @Override
    public int hashCode() {
      return java.util.Objects.hash(name, version, metadata);
    }
  }

  /** Types of producer entries. */
  public enum ProducerType {
    /** Programming language used to create the module. */
    LANGUAGE,
    /** Tool that processed the module. */
    PROCESSED_BY,
    /** SDK used to create the module. */
    SDK
  }

  @Override
  public String toString() {
    return getSummary();
  }
}
