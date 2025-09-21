package ai.tegmentum.wasmtime4j.security;

import java.time.Instant;
import java.util.Map;
import java.util.Objects;

/**
 * Metadata for security policies.
 *
 * @since 1.0.0
 */
public final class PolicyMetadata {

  private final String version;
  private final String name;
  private final String description;
  private final String author;
  private final Instant createdAt;
  private final Instant lastModified;
  private final Map<String, String> tags;

  private PolicyMetadata(final Builder builder) {
    this.version = builder.version;
    this.name = builder.name;
    this.description = builder.description;
    this.author = builder.author;
    this.createdAt = builder.createdAt;
    this.lastModified = builder.lastModified;
    this.tags = Map.copyOf(builder.tags);
  }

  public static Builder builder() {
    return new Builder();
  }

  public static PolicyMetadata defaultMetadata() {
    return builder()
        .withName("default-policy")
        .withVersion("1.0.0")
        .withDescription("Default security policy")
        .build();
  }

  public String getVersion() {
    return version;
  }

  public String getName() {
    return name;
  }

  public String getDescription() {
    return description;
  }

  public String getAuthor() {
    return author;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getLastModified() {
    return lastModified;
  }

  public Map<String, String> getTags() {
    return tags;
  }

  @Override
  public boolean equals(final Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;
    final PolicyMetadata that = (PolicyMetadata) o;
    return Objects.equals(version, that.version) && Objects.equals(name, that.name);
  }

  @Override
  public int hashCode() {
    return Objects.hash(version, name);
  }

  public static final class Builder {
    private String version = "1.0.0";
    private String name = "unnamed-policy";
    private String description = "";
    private String author = "system";
    private Instant createdAt = Instant.now();
    private Instant lastModified = Instant.now();
    private Map<String, String> tags = Map.of();

    public Builder withVersion(final String version) {
      this.version = version;
      return this;
    }

    public Builder withName(final String name) {
      this.name = name;
      return this;
    }

    public Builder withDescription(final String description) {
      this.description = description;
      return this;
    }

    public Builder withAuthor(final String author) {
      this.author = author;
      return this;
    }

    public Builder withCreatedAt(final Instant createdAt) {
      this.createdAt = createdAt;
      return this;
    }

    public Builder withLastModified(final Instant lastModified) {
      this.lastModified = lastModified;
      return this;
    }

    public Builder withTags(final Map<String, String> tags) {
      this.tags = tags != null ? tags : Map.of();
      return this;
    }

    public PolicyMetadata build() {
      Objects.requireNonNull(version, "Version cannot be null");
      Objects.requireNonNull(name, "Name cannot be null");
      return new PolicyMetadata(this);
    }
  }
}
