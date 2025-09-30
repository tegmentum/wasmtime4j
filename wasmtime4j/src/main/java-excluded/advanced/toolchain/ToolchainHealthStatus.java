package ai.tegmentum.wasmtime4j.toolchain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * Health status information for a WebAssembly toolchain.
 *
 * <p>Provides detailed information about the current state and health
 * of a toolchain, including availability, version, and any issues.
 *
 * @since 1.0.0
 */
public final class ToolchainHealthStatus {

  private final ToolchainType toolchainType;
  private final HealthLevel healthLevel;
  private final boolean available;
  private final Optional<String> version;
  private final List<String> issues;
  private final List<String> warnings;
  private final List<String> recommendations;
  private final Instant lastChecked;
  private final Optional<String> installationPath;

  private ToolchainHealthStatus(final Builder builder) {
    this.toolchainType = Objects.requireNonNull(builder.toolchainType);
    this.healthLevel = Objects.requireNonNull(builder.healthLevel);
    this.available = builder.available;
    this.version = Optional.ofNullable(builder.version);
    this.issues = Collections.unmodifiableList(new ArrayList<>(builder.issues));
    this.warnings = Collections.unmodifiableList(new ArrayList<>(builder.warnings));
    this.recommendations = Collections.unmodifiableList(new ArrayList<>(builder.recommendations));
    this.lastChecked = Objects.requireNonNull(builder.lastChecked);
    this.installationPath = Optional.ofNullable(builder.installationPath);
  }

  /**
   * Creates a new builder for toolchain health status.
   *
   * @param toolchainType the toolchain type
   * @return new builder instance
   */
  public static Builder builder(final ToolchainType toolchainType) {
    return new Builder(toolchainType);
  }

  /**
   * Creates a healthy status for an available toolchain.
   *
   * @param toolchainType the toolchain type
   * @param version the toolchain version
   * @param installationPath the installation path
   * @return healthy status
   */
  public static ToolchainHealthStatus healthy(final ToolchainType toolchainType,
                                              final String version,
                                              final String installationPath) {
    return builder(toolchainType)
        .healthLevel(HealthLevel.HEALTHY)
        .available(true)
        .version(version)
        .installationPath(installationPath)
        .build();
  }

  /**
   * Creates an unavailable status for a missing toolchain.
   *
   * @param toolchainType the toolchain type
   * @param issue the issue description
   * @return unavailable status
   */
  public static ToolchainHealthStatus unavailable(final ToolchainType toolchainType,
                                                  final String issue) {
    return builder(toolchainType)
        .healthLevel(HealthLevel.CRITICAL)
        .available(false)
        .addIssue(issue)
        .build();
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
   * Gets the overall health level.
   *
   * @return health level
   */
  public HealthLevel getHealthLevel() {
    return healthLevel;
  }

  /**
   * Checks if the toolchain is available.
   *
   * @return true if available, false otherwise
   */
  public boolean isAvailable() {
    return available;
  }

  /**
   * Gets the toolchain version.
   *
   * @return version, or empty if not available
   */
  public Optional<String> getVersion() {
    return version;
  }

  /**
   * Gets the list of issues.
   *
   * @return list of issues
   */
  public List<String> getIssues() {
    return issues;
  }

  /**
   * Gets the list of warnings.
   *
   * @return list of warnings
   */
  public List<String> getWarnings() {
    return warnings;
  }

  /**
   * Gets the list of recommendations.
   *
   * @return list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets when this status was last checked.
   *
   * @return last check timestamp
   */
  public Instant getLastChecked() {
    return lastChecked;
  }

  /**
   * Gets the installation path.
   *
   * @return installation path, or empty if not available
   */
  public Optional<String> getInstallationPath() {
    return installationPath;
  }

  /**
   * Checks if the toolchain is fully functional.
   *
   * @return true if healthy and available with no critical issues
   */
  public boolean isFullyFunctional() {
    return available && healthLevel == HealthLevel.HEALTHY && issues.isEmpty();
  }

  /**
   * Checks if there are any problems.
   *
   * @return true if there are issues or warnings
   */
  public boolean hasProblems() {
    return !issues.isEmpty() || !warnings.isEmpty();
  }

  /**
   * Gets a summary description of the status.
   *
   * @return status summary
   */
  public String getSummary() {
    if (!available) {
      return String.format("%s toolchain is not available", toolchainType.getIdentifier());
    }

    final String versionInfo = version.map(v -> " (version " + v + ")").orElse("");
    final String baseStatus = String.format("%s toolchain%s is %s",
        toolchainType.getIdentifier(), versionInfo, healthLevel.name().toLowerCase());

    if (issues.isEmpty() && warnings.isEmpty()) {
      return baseStatus;
    }

    return String.format("%s with %d issue(s) and %d warning(s)",
        baseStatus, issues.size(), warnings.size());
  }

  /**
   * Builder for toolchain health status.
   */
  public static final class Builder {
    private final ToolchainType toolchainType;
    private HealthLevel healthLevel = HealthLevel.UNKNOWN;
    private boolean available = false;
    private String version;
    private final List<String> issues = new ArrayList<>();
    private final List<String> warnings = new ArrayList<>();
    private final List<String> recommendations = new ArrayList<>();
    private Instant lastChecked = Instant.now();
    private String installationPath;

    private Builder(final ToolchainType toolchainType) {
      this.toolchainType = toolchainType;
    }

    public Builder healthLevel(final HealthLevel healthLevel) {
      this.healthLevel = Objects.requireNonNull(healthLevel);
      return this;
    }

    public Builder available(final boolean available) {
      this.available = available;
      return this;
    }

    public Builder version(final String version) {
      this.version = version;
      return this;
    }

    public Builder addIssue(final String issue) {
      this.issues.add(Objects.requireNonNull(issue));
      return this;
    }

    public Builder addWarning(final String warning) {
      this.warnings.add(Objects.requireNonNull(warning));
      return this;
    }

    public Builder addRecommendation(final String recommendation) {
      this.recommendations.add(Objects.requireNonNull(recommendation));
      return this;
    }

    public Builder lastChecked(final Instant lastChecked) {
      this.lastChecked = Objects.requireNonNull(lastChecked);
      return this;
    }

    public Builder installationPath(final String installationPath) {
      this.installationPath = installationPath;
      return this;
    }

    public ToolchainHealthStatus build() {
      return new ToolchainHealthStatus(this);
    }
  }

  /**
   * Overall health levels for toolchains.
   */
  public enum HealthLevel {
    /** Toolchain is fully functional with no issues */
    HEALTHY,

    /** Toolchain is functional but has minor issues or warnings */
    WARNING,

    /** Toolchain has significant issues that may affect functionality */
    DEGRADED,

    /** Toolchain is not functional or has critical issues */
    CRITICAL,

    /** Health status is unknown or could not be determined */
    UNKNOWN
  }

  @Override
  public String toString() {
    return String.format("ToolchainHealthStatus{type=%s, level=%s, available=%s, version=%s}",
        toolchainType.getIdentifier(), healthLevel, available, version.orElse("unknown"));
  }
}