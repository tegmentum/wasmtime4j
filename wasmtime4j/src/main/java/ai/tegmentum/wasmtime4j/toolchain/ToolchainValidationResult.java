package ai.tegmentum.wasmtime4j.toolchain;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

/**
 * Result of toolchain validation operations.
 *
 * <p>Contains information about the validity and availability of toolchain
 * components, along with any issues or recommendations.
 *
 * @since 1.0.0
 */
public final class ToolchainValidationResult {

  private final boolean valid;
  private final ToolchainType toolchainType;
  private final List<String> validationErrors;
  private final List<String> validationWarnings;
  private final List<String> recommendations;
  private final Instant validationTimestamp;
  private final String toolchainVersion;

  private ToolchainValidationResult(final boolean valid,
                                    final ToolchainType toolchainType,
                                    final List<String> validationErrors,
                                    final List<String> validationWarnings,
                                    final List<String> recommendations,
                                    final Instant validationTimestamp,
                                    final String toolchainVersion) {
    this.valid = valid;
    this.toolchainType = Objects.requireNonNull(toolchainType);
    this.validationErrors = List.copyOf(validationErrors);
    this.validationWarnings = List.copyOf(validationWarnings);
    this.recommendations = List.copyOf(recommendations);
    this.validationTimestamp = Objects.requireNonNull(validationTimestamp);
    this.toolchainVersion = toolchainVersion;
  }

  /**
   * Creates a valid toolchain result.
   *
   * @param toolchainType the toolchain type
   * @param toolchainVersion the toolchain version
   * @return valid result
   */
  public static ToolchainValidationResult valid(final ToolchainType toolchainType,
                                                final String toolchainVersion) {
    return new ToolchainValidationResult(true, toolchainType, List.of(), List.of(),
                                         List.of(), Instant.now(), toolchainVersion);
  }

  /**
   * Creates an invalid toolchain result.
   *
   * @param toolchainType the toolchain type
   * @param validationErrors the validation errors
   * @return invalid result
   */
  public static ToolchainValidationResult invalid(final ToolchainType toolchainType,
                                                  final List<String> validationErrors) {
    return new ToolchainValidationResult(false, toolchainType, validationErrors, List.of(),
                                         List.of(), Instant.now(), null);
  }

  /**
   * Creates a toolchain result with warnings.
   *
   * @param toolchainType the toolchain type
   * @param toolchainVersion the toolchain version
   * @param validationWarnings the validation warnings
   * @param recommendations the recommendations
   * @return result with warnings
   */
  public static ToolchainValidationResult withWarnings(final ToolchainType toolchainType,
                                                       final String toolchainVersion,
                                                       final List<String> validationWarnings,
                                                       final List<String> recommendations) {
    return new ToolchainValidationResult(true, toolchainType, List.of(), validationWarnings,
                                         recommendations, Instant.now(), toolchainVersion);
  }

  /**
   * Checks if the toolchain is valid.
   *
   * @return true if valid
   */
  public boolean isValid() {
    return valid;
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
   * Gets the validation errors.
   *
   * @return list of validation errors
   */
  public List<String> getValidationErrors() {
    return validationErrors;
  }

  /**
   * Gets the validation warnings.
   *
   * @return list of validation warnings
   */
  public List<String> getValidationWarnings() {
    return validationWarnings;
  }

  /**
   * Gets the recommendations.
   *
   * @return list of recommendations
   */
  public List<String> getRecommendations() {
    return recommendations;
  }

  /**
   * Gets the validation timestamp.
   *
   * @return validation timestamp
   */
  public Instant getValidationTimestamp() {
    return validationTimestamp;
  }

  /**
   * Gets the toolchain version.
   *
   * @return toolchain version, or null if not available
   */
  public String getToolchainVersion() {
    return toolchainVersion;
  }

  /**
   * Checks if there are validation errors.
   *
   * @return true if errors exist
   */
  public boolean hasErrors() {
    return !validationErrors.isEmpty();
  }

  /**
   * Checks if there are validation warnings.
   *
   * @return true if warnings exist
   */
  public boolean hasWarnings() {
    return !validationWarnings.isEmpty();
  }

  /**
   * Checks if there are recommendations.
   *
   * @return true if recommendations exist
   */
  public boolean hasRecommendations() {
    return !recommendations.isEmpty();
  }

  @Override
  public String toString() {
    return String.format("ToolchainValidationResult{valid=%s, type=%s, errors=%d, warnings=%d}",
        valid, toolchainType.getIdentifier(), validationErrors.size(), validationWarnings.size());
  }
}