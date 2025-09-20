package ai.tegmentum.wasmtime4j.component;

import java.time.Instant;
import java.util.Map;

/**
 * Metadata information about a WebAssembly component.
 *
 * <p>ComponentMetadata provides comprehensive information about a component including its size,
 * complexity, compilation details, and performance characteristics. This information is useful for
 * tooling, optimization, and runtime monitoring.
 *
 * <p>The metadata includes both static information (size, type counts) and dynamic information that
 * may change based on runtime behavior and optimization.
 *
 * @since 1.0.0
 */
public interface ComponentMetadata {

  /**
   * Gets the size of the component in bytes.
   *
   * <p>Returns the total size of the component binary representation including all sections,
   * metadata, and embedded data.
   *
   * @return the component size in bytes
   */
  long getSize();

  /**
   * Gets the number of exports in the component.
   *
   * <p>Returns the count of all exported interfaces, functions, resources, and other entities
   * provided by this component.
   *
   * @return the number of exports
   */
  int getExportCount();

  /**
   * Gets the number of imports required by the component.
   *
   * <p>Returns the count of all imports that must be satisfied for this component to be
   * instantiated successfully.
   *
   * @return the number of imports
   */
  int getImportCount();

  /**
   * Gets the number of interfaces defined or used by the component.
   *
   * <p>Returns the count of all interface types referenced by this component, including both
   * imported and exported interfaces.
   *
   * @return the number of interfaces
   */
  int getInterfaceCount();

  /**
   * Gets the number of resource types defined by the component.
   *
   * <p>Returns the count of all resource types declared in this component's interface definitions.
   *
   * @return the number of resource types
   */
  int getResourceTypeCount();

  /**
   * Gets the estimated instantiation complexity.
   *
   * <p>Returns a metric indicating the expected computational cost of instantiating this component.
   * Higher values indicate more complex instantiation processes.
   *
   * @return complexity score (0-100 scale)
   */
  int getComplexityScore();

  /**
   * Gets the timestamp when this component was compiled.
   *
   * <p>Returns the time when the component was last compiled or processed by the runtime.
   *
   * @return compilation timestamp
   */
  Instant getCompilationTime();

  /**
   * Gets the version of the component model specification used.
   *
   * <p>Returns the version string of the WebAssembly Component Model specification that this
   * component was compiled against.
   *
   * @return component model version string
   */
  String getComponentModelVersion();

  /**
   * Gets the runtime engine information.
   *
   * <p>Returns information about the WebAssembly runtime engine that compiled or is hosting this
   * component.
   *
   * @return engine information string
   */
  String getEngineInfo();

  /**
   * Gets optimization level used during compilation.
   *
   * <p>Returns the optimization level that was applied when this component was compiled.
   *
   * @return optimization level description
   */
  String getOptimizationLevel();

  /**
   * Gets custom metadata properties.
   *
   * <p>Returns a map of custom metadata properties that may have been embedded in the component
   * during compilation or added by tooling.
   *
   * @return map of custom metadata properties
   */
  Map<String, Object> getCustomProperties();

  /**
   * Gets the estimated memory usage for instantiation.
   *
   * <p>Returns an estimate of the memory required to instantiate this component, including space
   * for component state, export tables, and runtime structures.
   *
   * @return estimated memory usage in bytes
   */
  long getEstimatedMemoryUsage();

  /**
   * Checks if the component supports asynchronous execution.
   *
   * <p>Returns true if this component is designed to support async execution patterns and non-blocking operations.
   *
   * @return true if async execution is supported, false otherwise
   */
  boolean supportsAsyncExecution();

  /**
   * Checks if the component uses WASI Preview 2 features.
   *
   * <p>Returns true if this component requires or uses WASI Preview 2 interfaces and capabilities.
   *
   * @return true if WASI P2 features are used, false otherwise
   */
  boolean usesWasiP2Features();

  /**
   * Gets the list of WASI interfaces used by the component.
   *
   * <p>Returns names of all WASI interfaces that this component imports or depends on.
   *
   * @return list of WASI interface names
   */
  java.util.List<String> getWasiInterfaces();

  /**
   * Gets performance hints for this component.
   *
   * <p>Returns optimization hints and performance characteristics that can be used by the runtime
   * for better execution planning.
   *
   * @return performance hints
   */
  ComponentPerformanceHints getPerformanceHints();
}