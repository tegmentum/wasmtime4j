package ai.tegmentum.wasmtime4j.wit;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Result of a WIT interface evolution operation.
 *
 * <p>This class encapsulates the complete result of evolving one WIT interface to another,
 * including migration information, type mappings, and evolution metadata.
 *
 * @since 1.0.0
 */
public final class WitEvolutionResult {

  private final WitInterfaceDefinition sourceInterface;
  private final WitInterfaceDefinition targetInterface;
  private final boolean successful;
  private final List<WitEvolutionChange> changes;
  private final Map<String, WitTypeAdapter> typeAdapters;
  private final WitInterfaceBindings bindings;
  private final Instant evolutionTime;
  private final Optional<String> errorMessage;
  private final WitEvolutionMetrics metrics;

  /**
   * Creates a new WIT evolution result.
   *
   * @param sourceInterface the source interface
   * @param targetInterface the target interface
   * @param successful whether evolution was successful
   * @param changes list of evolution changes
   * @param typeAdapters map of type adapters
   * @param bindings interface bindings
   * @param evolutionTime time of evolution
   * @param errorMessage error message if evolution failed
   * @param metrics evolution metrics
   */
  public WitEvolutionResult(
      final WitInterfaceDefinition sourceInterface,
      final WitInterfaceDefinition targetInterface,
      final boolean successful,
      final List<WitEvolutionChange> changes,
      final Map<String, WitTypeAdapter> typeAdapters,
      final WitInterfaceBindings bindings,
      final Instant evolutionTime,
      final Optional<String> errorMessage,
      final WitEvolutionMetrics metrics) {
    this.sourceInterface = sourceInterface;
    this.targetInterface = targetInterface;
    this.successful = successful;
    this.changes = List.copyOf(changes);
    this.typeAdapters = Map.copyOf(typeAdapters);
    this.bindings = bindings;
    this.evolutionTime = evolutionTime;
    this.errorMessage = errorMessage;
    this.metrics = metrics;
  }

  /**
   * Creates a successful evolution result.
   *
   * @param sourceInterface the source interface
   * @param targetInterface the target interface
   * @param changes list of evolution changes
   * @param typeAdapters map of type adapters
   * @param bindings interface bindings
   * @param metrics evolution metrics
   * @return successful evolution result
   */
  public static WitEvolutionResult success(
      final WitInterfaceDefinition sourceInterface,
      final WitInterfaceDefinition targetInterface,
      final List<WitEvolutionChange> changes,
      final Map<String, WitTypeAdapter> typeAdapters,
      final WitInterfaceBindings bindings,
      final WitEvolutionMetrics metrics) {
    return new WitEvolutionResult(
        sourceInterface,
        targetInterface,
        true,
        changes,
        typeAdapters,
        bindings,
        Instant.now(),
        Optional.empty(),
        metrics);
  }

  /**
   * Creates a failed evolution result.
   *
   * @param sourceInterface the source interface
   * @param targetInterface the target interface
   * @param errorMessage error message
   * @return failed evolution result
   */
  public static WitEvolutionResult failure(
      final WitInterfaceDefinition sourceInterface,
      final WitInterfaceDefinition targetInterface,
      final String errorMessage) {
    return new WitEvolutionResult(
        sourceInterface,
        targetInterface,
        false,
        List.of(),
        Map.of(),
        null,
        Instant.now(),
        Optional.of(errorMessage),
        WitEvolutionMetrics.empty());
  }

  /**
   * Gets the source interface.
   *
   * @return source interface
   */
  public WitInterfaceDefinition getSourceInterface() {
    return sourceInterface;
  }

  /**
   * Gets the target interface.
   *
   * @return target interface
   */
  public WitInterfaceDefinition getTargetInterface() {
    return targetInterface;
  }

  /**
   * Checks if evolution was successful.
   *
   * @return true if successful
   */
  public boolean isSuccessful() {
    return successful;
  }

  /**
   * Gets the evolution changes.
   *
   * @return list of changes
   */
  public List<WitEvolutionChange> getChanges() {
    return changes;
  }

  /**
   * Gets the type adapters created during evolution.
   *
   * @return map of type adapters
   */
  public Map<String, WitTypeAdapter> getTypeAdapters() {
    return typeAdapters;
  }

  /**
   * Gets the interface bindings.
   *
   * @return interface bindings
   */
  public WitInterfaceBindings getBindings() {
    return bindings;
  }

  /**
   * Gets the evolution time.
   *
   * @return evolution time
   */
  public Instant getEvolutionTime() {
    return evolutionTime;
  }

  /**
   * Gets the error message if evolution failed.
   *
   * @return error message
   */
  public Optional<String> getErrorMessage() {
    return errorMessage;
  }

  /**
   * Gets evolution metrics.
   *
   * @return evolution metrics
   */
  public WitEvolutionMetrics getMetrics() {
    return metrics;
  }

  /**
   * Checks if there are breaking changes.
   *
   * @return true if there are breaking changes
   */
  public boolean hasBreakingChanges() {
    return changes.stream().anyMatch(WitEvolutionChange::isBreaking);
  }

  /**
   * Gets only breaking changes.
   *
   * @return list of breaking changes
   */
  public List<WitEvolutionChange> getBreakingChanges() {
    return changes.stream().filter(WitEvolutionChange::isBreaking).toList();
  }

  /**
   * Gets only non-breaking changes.
   *
   * @return list of non-breaking changes
   */
  public List<WitEvolutionChange> getNonBreakingChanges() {
    return changes.stream().filter(change -> !change.isBreaking()).toList();
  }

  /**
   * Gets type adapter for a specific type.
   *
   * @param typeName the type name
   * @return type adapter if available
   */
  public Optional<WitTypeAdapter> getTypeAdapter(final String typeName) {
    return Optional.ofNullable(typeAdapters.get(typeName));
  }

  /**
   * Checks if a type has an adapter.
   *
   * @param typeName the type name
   * @return true if type has adapter
   */
  public boolean hasTypeAdapter(final String typeName) {
    return typeAdapters.containsKey(typeName);
  }

  @Override
  public String toString() {
    return "WitEvolutionResult{"
        + "sourceInterface="
        + sourceInterface.getName()
        + ", targetInterface="
        + targetInterface.getName()
        + ", successful="
        + successful
        + ", changes="
        + changes.size()
        + ", typeAdapters="
        + typeAdapters.size()
        + ", evolutionTime="
        + evolutionTime
        + '}';
  }
}
