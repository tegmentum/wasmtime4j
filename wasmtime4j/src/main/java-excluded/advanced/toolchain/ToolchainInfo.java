package ai.tegmentum.wasmtime4j.toolchain;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Information about an available toolchain.
 *
 * <p>Provides details about toolchain capabilities, version, installation
 * path, and supported features.
 *
 * @since 1.0.0
 */
public final class ToolchainInfo {

  private final ToolchainType toolchainType;
  private final String version;
  private final String installationPath;
  private final List<String> supportedTargets;
  private final Map<String, String> capabilities;
  private final boolean isAvailable;
  private final Optional<String> description;
  private final Instant lastUpdated;
  private final Map<String, Object> metadata;

  private ToolchainInfo(final ToolchainType toolchainType,
                        final String version,
                        final String installationPath,
                        final List<String> supportedTargets,
                        final Map<String, String> capabilities,
                        final boolean isAvailable,
                        final String description,
                        final Instant lastUpdated,
                        final Map<String, Object> metadata) {
    this.toolchainType = Objects.requireNonNull(toolchainType);
    this.version = Objects.requireNonNull(version);
    this.installationPath = Objects.requireNonNull(installationPath);
    this.supportedTargets = List.copyOf(supportedTargets);
    this.capabilities = Map.copyOf(capabilities);
    this.isAvailable = isAvailable;
    this.description = Optional.ofNullable(description);
    this.lastUpdated = Objects.requireNonNull(lastUpdated);
    this.metadata = Map.copyOf(metadata);
  }

  /**
   * Creates a new builder for toolchain info.
   *
   * @param toolchainType the toolchain type
   * @return new builder
   */
  public static Builder builder(final ToolchainType toolchainType) {
    return new Builder(toolchainType);
  }

  /**
   * Gets the toolchain type.
   *
   * @return toolchain type
   */
  public ToolchainType getToolchainType() {
    return toolchainType;
  }

  /**
   * Gets the toolchain version.
   *
   * @return version string
   */
  public String getVersion() {
    return version;
  }

  /**
   * Gets the installation path.
   *
   * @return installation path
   */
  public String getInstallationPath() {
    return installationPath;
  }

  /**
   * Gets the supported target architectures.
   *
   * @return list of supported targets
   */
  public List<String> getSupportedTargets() {
    return supportedTargets;
  }

  /**
   * Gets the toolchain capabilities.
   *
   * @return map of capability names to descriptions
   */
  public Map<String, String> getCapabilities() {
    return capabilities;
  }

  /**
   * Checks if the toolchain is available for use.
   *
   * @return true if available
   */
  public boolean isAvailable() {
    return isAvailable;
  }

  /**
   * Gets the toolchain description.
   *
   * @return description, or empty if not available
   */
  public Optional<String> getDescription() {
    return description;
  }

  /**
   * Gets when this information was last updated.
   *
   * @return last update timestamp
   */
  public Instant getLastUpdated() {
    return lastUpdated;
  }

  /**
   * Gets additional metadata.
   *
   * @return metadata map
   */
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  /**
   * Checks if the toolchain supports a specific target.
   *
   * @param target the target to check
   * @return true if target is supported
   */
  public boolean supportsTarget(final String target) {
    return supportedTargets.contains(target);
  }

  /**
   * Checks if the toolchain has a specific capability.
   *
   * @param capability the capability to check
   * @return true if capability is supported
   */
  public boolean hasCapability(final String capability) {
    return capabilities.containsKey(capability);
  }

  /**
   * Gets a capability description.
   *
   * @param capability the capability name
   * @return capability description, or null if not supported
   */
  public String getCapabilityDescription(final String capability) {
    return capabilities.get(capability);
  }

  /**
   * Builder for toolchain information.
   */
  public static final class Builder {
    private final ToolchainType toolchainType;
    private String version = "unknown";
    private String installationPath = "";
    private List<String> supportedTargets = List.of();
    private Map<String, String> capabilities = Map.of();
    private boolean isAvailable = false;
    private String description;
    private Instant lastUpdated = Instant.now();
    private Map<String, Object> metadata = Map.of();

    private Builder(final ToolchainType toolchainType) {
      this.toolchainType = toolchainType;
    }

    public Builder version(final String version) {
      this.version = Objects.requireNonNull(version);
      return this;
    }

    public Builder installationPath(final String installationPath) {
      this.installationPath = Objects.requireNonNull(installationPath);
      return this;
    }

    public Builder supportedTargets(final List<String> supportedTargets) {
      this.supportedTargets = List.copyOf(Objects.requireNonNull(supportedTargets));
      return this;
    }

    public Builder addSupportedTarget(final String target) {
      this.supportedTargets = List.copyOf(
          java.util.stream.Stream.concat(
              supportedTargets.stream(),
              java.util.stream.Stream.of(Objects.requireNonNull(target))
          ).toList()
      );
      return this;
    }

    public Builder capabilities(final Map<String, String> capabilities) {
      this.capabilities = Map.copyOf(Objects.requireNonNull(capabilities));
      return this;
    }

    public Builder addCapability(final String name, final String description) {
      final var newCapabilities = new java.util.HashMap<>(capabilities);
      newCapabilities.put(Objects.requireNonNull(name), Objects.requireNonNull(description));
      this.capabilities = Map.copyOf(newCapabilities);
      return this;
    }

    public Builder available(final boolean isAvailable) {
      this.isAvailable = isAvailable;
      return this;
    }

    public Builder description(final String description) {
      this.description = description;
      return this;
    }

    public Builder lastUpdated(final Instant lastUpdated) {
      this.lastUpdated = Objects.requireNonNull(lastUpdated);
      return this;
    }

    public Builder metadata(final Map<String, Object> metadata) {
      this.metadata = Map.copyOf(Objects.requireNonNull(metadata));
      return this;
    }

    public Builder addMetadata(final String key, final Object value) {
      final var newMetadata = new java.util.HashMap<>(metadata);
      newMetadata.put(Objects.requireNonNull(key), Objects.requireNonNull(value));
      this.metadata = Map.copyOf(newMetadata);
      return this;
    }

    public ToolchainInfo build() {
      return new ToolchainInfo(toolchainType, version, installationPath, supportedTargets,
                               capabilities, isAvailable, description, lastUpdated, metadata);
    }
  }

  @Override
  public String toString() {
    return String.format("ToolchainInfo{type=%s, version='%s', available=%s, targets=%d, capabilities=%d}",
        toolchainType.getIdentifier(), version, isAvailable, supportedTargets.size(), capabilities.size());
  }
}