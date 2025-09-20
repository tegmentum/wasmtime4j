package ai.tegmentum.wasmtime4j.component;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

/**
 * Metadata and statistics for component linker operations.
 *
 * <p>ComponentLinkerMetadata provides detailed information about linker configuration, import
 * resolution performance, and linking statistics. This information is useful for debugging
 * linking issues, optimizing import resolution, and monitoring component composition.
 *
 * @since 1.0.0
 */
public interface ComponentLinkerMetadata {

  /**
   * Gets the timestamp when this linker was created.
   *
   * <p>Returns the time when the linker instance was first created.
   *
   * @return linker creation timestamp
   */
  Instant getCreationTime();

  /**
   * Gets the number of imports defined in this linker.
   *
   * <p>Returns the total count of import definitions that have been added to this linker.
   *
   * @return number of defined imports
   */
  int getDefinedImportCount();

  /**
   * Gets the number of successful instantiations performed.
   *
   * <p>Returns the count of component instantiations that completed successfully using this
   * linker.
   *
   * @return successful instantiation count
   */
  long getSuccessfulInstantiations();

  /**
   * Gets the number of failed instantiations.
   *
   * <p>Returns the count of component instantiation attempts that failed due to linking errors,
   * missing imports, or type mismatches.
   *
   * @return failed instantiation count
   */
  long getFailedInstantiations();

  /**
   * Gets the total time spent in linking operations.
   *
   * <p>Returns the cumulative time spent resolving imports and performing linking operations
   * across all instantiation attempts.
   *
   * @return total linking time
   */
  Duration getTotalLinkingTime();

  /**
   * Gets the average linking time per instantiation.
   *
   * <p>Returns the mean time required to resolve imports and link a component using this linker.
   *
   * @return average linking time
   */
  Duration getAverageLinkingTime();

  /**
   * Gets import resolution statistics by import name.
   *
   * <p>Returns detailed statistics about how often each import has been resolved and the
   * performance characteristics of each resolution.
   *
   * @return map of import names to their resolution statistics
   */
  Map<String, ImportResolutionStats> getImportResolutionStats();

  /**
   * Gets the most frequently used imports.
   *
   * <p>Returns a list of import names ordered by frequency of use during instantiation
   * operations.
   *
   * @return list of import names ordered by usage frequency
   */
  List<String> getMostFrequentlyUsedImports();

  /**
   * Gets the imports that cause the most linking overhead.
   *
   * <p>Returns a list of import names that require the most time to resolve during linking
   * operations.
   *
   * @return list of import names ordered by resolution time
   */
  List<String> getSlowestResolvingImports();

  /**
   * Gets the number of import definition conflicts detected.
   *
   * <p>Returns the count of times an import definition was redefined or conflicted with an
   * existing definition.
   *
   * @return import conflict count
   */
  long getImportConflictCount();

  /**
   * Gets the number of missing import errors.
   *
   * <p>Returns the count of instantiation failures caused by missing required imports.
   *
   * @return missing import error count
   */
  long getMissingImportErrors();

  /**
   * Gets the number of type mismatch errors.
   *
   * <p>Returns the count of instantiation failures caused by import implementations that don't
   * match the expected types.
   *
   * @return type mismatch error count
   */
  long getTypeMismatchErrors();

  /**
   * Gets caching effectiveness metrics.
   *
   * <p>Returns information about how effectively the linker caches resolved imports and linking
   * results for improved performance.
   *
   * @return caching effectiveness metrics
   */
  LinkerCacheMetrics getCacheMetrics();

  /**
   * Gets memory usage information for this linker.
   *
   * <p>Returns details about memory consumption for storing import definitions, cached
   * resolutions, and other linker state.
   *
   * @return linker memory usage information
   */
  LinkerMemoryUsage getMemoryUsage();

  /**
   * Gets optimization suggestions based on usage patterns.
   *
   * <p>Returns recommendations for improving linking performance based on observed usage
   * patterns and bottlenecks.
   *
   * @return list of optimization suggestions
   */
  List<LinkerOptimizationSuggestion> getOptimizationSuggestions();

  /**
   * Gets the linker configuration summary.
   *
   * <p>Returns information about how this linker is configured including caching settings,
   * optimization levels, and other configuration parameters.
   *
   * @return linker configuration summary
   */
  LinkerConfiguration getConfiguration();

  /**
   * Gets compatibility analysis results.
   *
   * <p>Returns information about component compatibility issues discovered during linking
   * operations.
   *
   * @return compatibility analysis results
   */
  ComponentCompatibilityAnalysis getCompatibilityAnalysis();

  /**
   * Resets all statistics and metrics.
   *
   * <p>Clears all accumulated statistics while preserving the linker configuration and import
   * definitions. Timestamps are reset and counters are cleared.
   */
  void resetStatistics();
}