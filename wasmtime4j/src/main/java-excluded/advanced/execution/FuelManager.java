package ai.tegmentum.wasmtime4j.execution;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.Set;

/**
 * Advanced fuel management system for WebAssembly execution control.
 *
 * <p>The FuelManager provides hierarchical fuel allocation, consumption tracking,
 * dynamic adjustment capabilities, and comprehensive fuel analytics for fine-grained
 * control over WebAssembly execution resources.
 *
 * @since 1.0.0
 */
public interface FuelManager {

  /**
   * Allocates fuel for a specific execution context.
   *
   * @param contextId unique identifier for the execution context
   * @param amount amount of fuel to allocate
   * @param priority execution priority level
   * @throws WasmException if fuel allocation fails
   * @throws IllegalArgumentException if contextId is null or amount is negative
   */
  void allocateFuel(String contextId, long amount, FuelPriority priority) throws WasmException;

  /**
   * Allocates fuel with hierarchical inheritance from parent context.
   *
   * @param contextId unique identifier for the execution context
   * @param parentContextId parent context to inherit fuel policies from
   * @param amount amount of fuel to allocate
   * @param inheritanceRatio ratio of fuel to inherit from parent (0.0-1.0)
   * @throws WasmException if fuel allocation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void allocateHierarchicalFuel(String contextId, String parentContextId, long amount, double inheritanceRatio) throws WasmException;

  /**
   * Consumes fuel from a specific execution context.
   *
   * @param contextId execution context identifier
   * @param amount amount of fuel to consume
   * @return actual amount of fuel consumed
   * @throws WasmException if fuel consumption fails
   * @throws IllegalArgumentException if contextId is null or amount is negative
   */
  long consumeFuel(String contextId, long amount) throws WasmException;

  /**
   * Consumes fuel with per-function tracking.
   *
   * @param contextId execution context identifier
   * @param functionName name of the WebAssembly function consuming fuel
   * @param amount amount of fuel to consume
   * @return actual amount of fuel consumed
   * @throws WasmException if fuel consumption fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  long consumeFunctionFuel(String contextId, String functionName, long amount) throws WasmException;

  /**
   * Consumes fuel with per-instruction granularity tracking.
   *
   * @param contextId execution context identifier
   * @param instructionCount number of instructions executed
   * @param fuelPerInstruction fuel cost per instruction
   * @return actual amount of fuel consumed
   * @throws WasmException if fuel consumption fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  long consumeInstructionFuel(String contextId, long instructionCount, double fuelPerInstruction) throws WasmException;

  /**
   * Gets remaining fuel for a specific execution context.
   *
   * @param contextId execution context identifier
   * @return remaining fuel amount, or -1 if unlimited
   * @throws WasmException if query fails
   * @throws IllegalArgumentException if contextId is null
   */
  long getRemainingFuel(String contextId) throws WasmException;

  /**
   * Gets comprehensive fuel statistics for a context.
   *
   * @param contextId execution context identifier
   * @return fuel statistics including consumption patterns and efficiency metrics
   * @throws WasmException if query fails
   * @throws IllegalArgumentException if contextId is null
   */
  FuelStatistics getFuelStatistics(String contextId) throws WasmException;

  /**
   * Dynamically adjusts fuel allocation based on execution patterns.
   *
   * @param contextId execution context identifier
   * @param adjustment fuel adjustment parameters
   * @throws WasmException if adjustment fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void adjustFuelAllocation(String contextId, FuelAdjustment adjustment) throws WasmException;

  /**
   * Delegates fuel from one context to another.
   *
   * @param sourceContextId source execution context
   * @param targetContextId target execution context
   * @param amount amount of fuel to delegate
   * @param conditions delegation conditions
   * @throws WasmException if delegation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void delegateFuel(String sourceContextId, String targetContextId, long amount, DelegationConditions conditions) throws WasmException;

  /**
   * Creates a fuel budget for managing multiple execution contexts.
   *
   * @param budgetId unique budget identifier
   * @param totalFuel total fuel available for the budget
   * @param allocationStrategy strategy for allocating fuel to contexts
   * @throws WasmException if budget creation fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void createFuelBudget(String budgetId, long totalFuel, FuelAllocationStrategy allocationStrategy) throws WasmException;

  /**
   * Transfers fuel between contexts within a budget.
   *
   * @param budgetId budget identifier
   * @param fromContext source execution context
   * @param toContext target execution context
   * @param amount amount of fuel to transfer
   * @throws WasmException if transfer fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void transferFuel(String budgetId, String fromContext, String toContext, long amount) throws WasmException;

  /**
   * Gets fuel consumption patterns for optimization analysis.
   *
   * @param contextId execution context identifier
   * @param timeWindow time window for pattern analysis
   * @return consumption patterns including frequency, spikes, and trends
   * @throws WasmException if analysis fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  FuelConsumptionPattern getConsumptionPattern(String contextId, Duration timeWindow) throws WasmException;

  /**
   * Sets fuel consumption limits with automatic enforcement.
   *
   * @param contextId execution context identifier
   * @param limits consumption limits configuration
   * @throws WasmException if limit setting fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void setConsumptionLimits(String contextId, FuelConsumptionLimits limits) throws WasmException;

  /**
   * Gets all active execution contexts managed by this fuel manager.
   *
   * @return set of active context identifiers
   */
  Set<String> getActiveContexts();

  /**
   * Gets fuel budget information for all managed budgets.
   *
   * @return map of budget IDs to their current status
   * @throws WasmException if query fails
   */
  Map<String, FuelBudgetStatus> getFuelBudgets() throws WasmException;

  /**
   * Resets fuel allocation for a specific context.
   *
   * @param contextId execution context identifier
   * @param newAmount new fuel amount to allocate
   * @throws WasmException if reset fails
   * @throws IllegalArgumentException if parameters are invalid
   */
  void resetFuelAllocation(String contextId, long newAmount) throws WasmException;

  /**
   * Cleans up resources for terminated execution contexts.
   *
   * @param contextId execution context identifier to clean up
   * @throws WasmException if cleanup fails
   */
  void cleanupContext(String contextId) throws WasmException;

  /**
   * Gets global fuel manager statistics across all contexts.
   *
   * @return comprehensive fuel manager statistics
   * @throws WasmException if query fails
   */
  GlobalFuelStatistics getGlobalStatistics() throws WasmException;

  /**
   * Validates fuel manager state and consistency.
   *
   * @return validation result
   * @throws WasmException if validation fails
   */
  FuelValidationResult validate() throws WasmException;
}