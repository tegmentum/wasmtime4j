package ai.tegmentum.wasmtime4j.wasi;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Statistics and metrics for WASI components.
 *
 * <p>Component statistics provide insight into component characteristics, resource usage, and
 * performance metrics. This information is useful for monitoring, optimization, and debugging.
 *
 * <p>Statistics are snapshot-based and reflect the state at the time they were collected.
 *
 * @since 1.0.0
 */
public interface WasiComponentStats {

  /**
   * Gets the timestamp when these statistics were collected.
   *
   * @return the statistics collection timestamp
   */
  Instant getCollectedAt();

  /**
   * Gets the component name if available.
   *
   * @return the component name, or null if not specified
   */
  String getComponentName();

  /**
   * Gets the size of the original component bytecode in bytes.
   *
   * @return component bytecode size in bytes
   */
  long getBytecodeSize();

  /**
   * Gets the size of the compiled component in memory.
   *
   * <p>This includes the size of compiled machine code, metadata, and internal data structures
   * after compilation and optimization.
   *
   * @return compiled component size in bytes
   */
  long getCompiledSize();

  /**
   * Gets the number of exported interfaces.
   *
   * @return count of exported interfaces
   */
  int getExportedInterfaceCount();

  /**
   * Gets the number of exported functions across all interfaces.
   *
   * @return count of exported functions
   */
  int getExportedFunctionCount();

  /**
   * Gets the number of imported interfaces required by this component.
   *
   * @return count of imported interfaces
   */
  int getImportedInterfaceCount();

  /**
   * Gets the number of imported functions across all interfaces.
   *
   * @return count of imported functions
   */
  int getImportedFunctionCount();

  /**
   * Gets the number of resource types defined by this component.
   *
   * @return count of resource types
   */
  int getResourceTypeCount();

  /**
   * Gets the number of custom types defined by this component.
   *
   * <p>Custom types include records, variants, enums, and other user-defined types.
   *
   * @return count of custom types
   */
  int getCustomTypeCount();

  /**
   * Gets the compilation time for this component.
   *
   * <p>This is the time taken to compile the component from bytecode to the internal
   * representation.
   *
   * @return compilation time in milliseconds
   */
  long getCompilationTimeMs();

  /**
   * Gets the estimated memory overhead for this component.
   *
   * <p>This is the base memory usage for the component itself, not including instance-specific
   * memory.
   *
   * @return memory overhead in bytes
   */
  long getMemoryOverhead();

  /**
   * Gets the number of active instances of this component.
   *
   * @return count of active instances
   */
  int getActiveInstanceCount();

  /**
   * Gets the total number of instances ever created from this component.
   *
   * @return total instance count
   */
  long getTotalInstanceCount();

  /**
   * Gets the total number of function calls made across all instances.
   *
   * @return total function call count
   */
  long getTotalFunctionCalls();

  /**
   * Gets function call statistics broken down by function name.
   *
   * <p>The map contains function names as keys and call counts as values.
   *
   * @return map of function names to call counts
   */
  Map<String, Long> getFunctionCallStats();

  /**
   * Gets the total execution time across all instances and function calls.
   *
   * @return total execution time in milliseconds
   */
  long getTotalExecutionTimeMs();

  /**
   * Gets execution time statistics broken down by function name.
   *
   * <p>The map contains function names as keys and total execution times as values.
   *
   * @return map of function names to execution times in milliseconds
   */
  Map<String, Long> getFunctionExecutionTimeStats();

  /**
   * Gets error statistics for this component.
   *
   * <p>This includes compilation errors, instantiation failures, and runtime errors across all
   * instances.
   *
   * @return error statistics
   */
  WasiErrorStats getErrorStats();

  /**
   * Gets resource usage statistics for this component.
   *
   * <p>This includes resource creation, destruction, and usage patterns across all instances.
   *
   * @return resource usage statistics
   */
  WasiResourceUsageStats getResourceUsageStats();

  /**
   * Gets performance metrics for this component.
   *
   * <p>Performance metrics include throughput, latency percentiles, and other performance
   * indicators useful for optimization.
   *
   * @return performance metrics
   */
  WasiPerformanceMetrics getPerformanceMetrics();

  /**
   * Gets a list of all interfaces exported by this component.
   *
   * @return list of exported interface names
   */
  List<String> getExportedInterfaces();

  /**
   * Gets a list of all interfaces imported by this component.
   *
   * @return list of imported interface names
   */
  List<String> getImportedInterfaces();

  /**
   * Gets additional custom properties and metrics.
   *
   * <p>This includes implementation-specific metrics and custom statistics that may be useful for
   * debugging or monitoring.
   *
   * @return map of property names to values
   */
  Map<String, Object> getCustomProperties();

  /**
   * Creates a summary string representation of these statistics.
   *
   * <p>The summary includes key metrics in a human-readable format suitable for logging or display.
   *
   * @return formatted summary string
   */
  String getSummary();
}
