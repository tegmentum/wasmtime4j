package ai.tegmentum.wasmtime4j.comparison.analyzers;

import ai.tegmentum.wasmtime4j.RuntimeType;
import java.util.Set;

/**
 * Represents a coverage gap specific to Wasmtime test suite analysis.
 *
 * @since 1.0.0
 */
public final class WasmtimeCoverageGap {
  private final WasmtimeGapType gapType;
  private final String description;
  private final Set<String> affectedFeatures;
  private final Set<RuntimeType> affectedRuntimes;
  private final GapSeverity severity;

  /**
   * Creates a Wasmtime coverage gap with affected features and runtimes.
   *
   * @param gapType type of coverage gap
   * @param description detailed description of the gap
   * @param affectedFeatures features affected by this gap
   * @param affectedRuntimes runtimes affected by this gap
   * @param severity severity level of the gap
   */
  public WasmtimeCoverageGap(
      final WasmtimeGapType gapType,
      final String description,
      final Set<String> affectedFeatures,
      final Set<RuntimeType> affectedRuntimes,
      final GapSeverity severity) {
    this.gapType = gapType;
    this.description = description;
    this.affectedFeatures = Set.copyOf(affectedFeatures);
    this.affectedRuntimes = Set.copyOf(affectedRuntimes);
    this.severity = severity;
  }

  public WasmtimeGapType getGapType() {
    return gapType;
  }

  public String getDescription() {
    return description;
  }

  public Set<String> getAffectedFeatures() {
    return affectedFeatures;
  }

  public Set<RuntimeType> getAffectedRuntimes() {
    return affectedRuntimes;
  }

  public GapSeverity getSeverity() {
    return severity;
  }
}
