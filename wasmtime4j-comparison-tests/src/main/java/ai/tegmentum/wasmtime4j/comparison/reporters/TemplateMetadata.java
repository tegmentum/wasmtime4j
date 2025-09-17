package ai.tegmentum.wasmtime4j.comparison.reporters;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata about a template.
 *
 * @since 1.0.0
 */
public final class TemplateMetadata {
  private final String version;
  private final String author;
  private final String description;
  private final List<String> supportedFormats;
  private final Map<String, String> customMetadata;

  private TemplateMetadata(final Builder builder) {
    this.version = Objects.requireNonNull(builder.version, "version cannot be null");
    this.author = Objects.requireNonNull(builder.author, "author cannot be null");
    this.description = Objects.requireNonNull(builder.description, "description cannot be null");
    this.supportedFormats = List.copyOf(builder.supportedFormats);
    this.customMetadata = Map.copyOf(builder.customMetadata);
  }

  public String getVersion() {
    return version;
  }

  public String getAuthor() {
    return author;
  }

  public String getDescription() {
    return description;
  }

  public List<String> getSupportedFormats() {
    return supportedFormats;
  }

  public Map<String, String> getCustomMetadata() {
    return customMetadata;
  }

  /** Creates default template metadata. */
  public static TemplateMetadata defaultMetadata() {
    return new Builder()
        .version("1.0.0")
        .author("Wasmtime4j Comparison Suite")
        .description("Default template for comparison reports")
        .supportedFormats(List.of("HTML", "JSON", "CSV", "CONSOLE"))
        .build();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (obj == null || getClass() != obj.getClass()) {
      return false;
    }

    final TemplateMetadata that = (TemplateMetadata) obj;
    return Objects.equals(version, that.version)
        && Objects.equals(author, that.author)
        && Objects.equals(description, that.description)
        && Objects.equals(supportedFormats, that.supportedFormats)
        && Objects.equals(customMetadata, that.customMetadata);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, author, description, supportedFormats, customMetadata);
  }

  @Override
  public String toString() {
    return "TemplateMetadata{"
        + "version='"
        + version
        + '\''
        + ", author='"
        + author
        + '\''
        + ", formats="
        + supportedFormats
        + '}';
  }

  /** Builder for TemplateMetadata. */
  public static final class Builder {
    private String version = "1.0.0";
    private String author = "Unknown";
    private String description = "";
    private List<String> supportedFormats = Collections.emptyList();
    private Map<String, String> customMetadata = Collections.emptyMap();

    public Builder version(final String version) {
      this.version = Objects.requireNonNull(version, "version cannot be null");
      return this;
    }

    public Builder author(final String author) {
      this.author = Objects.requireNonNull(author, "author cannot be null");
      return this;
    }

    public Builder description(final String description) {
      this.description = Objects.requireNonNull(description, "description cannot be null");
      return this;
    }

    /**
     * Sets the supported formats.
     *
     * @param supportedFormats the supported formats
     * @return this builder
     */
    public Builder supportedFormats(final List<String> supportedFormats) {
      this.supportedFormats =
          Objects.requireNonNull(supportedFormats, "supportedFormats cannot be null");
      return this;
    }

    public Builder customMetadata(final Map<String, String> customMetadata) {
      this.customMetadata = Objects.requireNonNull(customMetadata, "customMetadata cannot be null");
      return this;
    }

    /**
     * Builds the TemplateMetadata instance.
     *
     * @return the built TemplateMetadata
     */
    public TemplateMetadata build() {
      return new TemplateMetadata(this);
    }
  }
}
