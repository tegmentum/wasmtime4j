package ai.tegmentum.wasmtime4j.wit;
import ai.tegmentum.wasmtime4j.WasmValue;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Interface bindings for evolved WIT interfaces.
 *
 * <p>This interface provides comprehensive binding capabilities for evolved WIT interfaces,
 * enabling automatic code generation, runtime adaptation, and type-safe invocation of evolved
 * interface methods.
 *
 * @since 1.0.0
 */
public interface WitInterfaceBindings {

  /**
   * Gets the source interface definition.
   *
   * @return source interface
   */
  WitInterfaceDefinition getSourceInterface();

  /**
   * Gets the target interface definition.
   *
   * @return target interface
   */
  WitInterfaceDefinition getTargetInterface();

  /**
   * Gets all function bindings.
   *
   * @return map of function name to binding
   */
  Map<String, FunctionBinding> getFunctionBindings();

  /**
   * Gets all type bindings.
   *
   * @return map of type name to binding
   */
  Map<String, TypeBinding> getTypeBindings();

  /**
   * Gets all import bindings.
   *
   * @return map of import name to binding
   */
  Map<String, ImportBinding> getImportBindings();

  /**
   * Gets all export bindings.
   *
   * @return map of export name to binding
   */
  Map<String, ExportBinding> getExportBindings();

  /**
   * Gets binding for a specific function.
   *
   * @param functionName function name
   * @return function binding if available
   */
  Optional<FunctionBinding> getFunctionBinding(String functionName);

  /**
   * Gets binding for a specific type.
   *
   * @param typeName type name
   * @return type binding if available
   */
  Optional<TypeBinding> getTypeBinding(String typeName);

  /**
   * Checks if a function is bound.
   *
   * @param functionName function name
   * @return true if function is bound
   */
  boolean isFunctionBound(String functionName);

  /**
   * Checks if a type is bound.
   *
   * @param typeName type name
   * @return true if type is bound
   */
  boolean isTypeBound(String typeName);

  /**
   * Gets all unbound functions.
   *
   * @return set of unbound function names
   */
  Set<String> getUnboundFunctions();

  /**
   * Gets all unbound types.
   *
   * @return set of unbound type names
   */
  Set<String> getUnboundTypes();

  /**
   * Invokes a bound function with automatic adaptation.
   *
   * @param functionName function name
   * @param arguments function arguments
   * @return function result
   * @throws WasmRuntimeException if invocation fails
   */
  WasmValue invoke(String functionName, WasmValue... arguments);

  /**
   * Creates an instance of a bound type.
   *
   * @param typeName type name
   * @param arguments constructor arguments
   * @return type instance
   * @throws WasmRuntimeException if creation fails
   */
  WasmValue createInstance(String typeName, WasmValue... arguments);

  /**
   * Converts a value using type bindings.
   *
   * @param value source value
   * @param targetTypeName target type name
   * @return converted value
   * @throws WasmRuntimeException if conversion fails
   */
  WasmValue convertType(WasmValue value, String targetTypeName);

  /**
   * Gets binding statistics.
   *
   * @return binding statistics
   */
  BindingStatistics getStatistics();

  /**
   * Validates all bindings.
   *
   * @return validation result
   */
  BindingValidationResult validateBindings();

  /**
   * Gets binding metadata.
   *
   * @return binding metadata
   */
  BindingMetadata getMetadata();

  /** Function binding information. */
  interface FunctionBinding {
    /**
     * Gets the source function name.
     *
     * @return source function name
     */
    String getSourceFunctionName();

    /**
     * Gets the target function name.
     *
     * @return target function name
     */
    String getTargetFunctionName();

    /**
     * Gets parameter adapters.
     *
     * @return list of parameter adapters
     */
    List<WitTypeAdapter> getParameterAdapters();

    /**
     * Gets return value adapter.
     *
     * @return return value adapter if applicable
     */
    Optional<WitTypeAdapter> getReturnValueAdapter();

    /**
     * Gets binding type.
     *
     * @return binding type
     */
    BindingType getBindingType();

    /**
     * Checks if this is a direct binding.
     *
     * @return true if direct binding
     */
    boolean isDirect();

    /**
     * Gets adaptation metadata.
     *
     * @return adaptation metadata
     */
    AdaptationMetadata getAdaptationMetadata();

    /**
     * Invokes the bound function.
     *
     * @param arguments function arguments
     * @return function result
     * @throws WasmRuntimeException if invocation fails
     */
    WasmValue invoke(WasmValue... arguments);

    /**
     * Validates function binding.
     *
     * @return validation result
     */
    BindingValidationResult validate();
  }

  /** Type binding information. */
  interface TypeBinding {
    /**
     * Gets the source type name.
     *
     * @return source type name
     */
    String getSourceTypeName();

    /**
     * Gets the target type name.
     *
     * @return target type name
     */
    String getTargetTypeName();

    /**
     * Gets the type adapter.
     *
     * @return type adapter
     */
    WitTypeAdapter getTypeAdapter();

    /**
     * Gets binding type.
     *
     * @return binding type
     */
    BindingType getBindingType();

    /**
     * Checks if this is a direct binding.
     *
     * @return true if direct binding
     */
    boolean isDirect();

    /**
     * Gets constructor bindings.
     *
     * @return map of constructor bindings
     */
    Map<String, FunctionBinding> getConstructorBindings();

    /**
     * Gets method bindings.
     *
     * @return map of method bindings
     */
    Map<String, FunctionBinding> getMethodBindings();

    /**
     * Converts a value using this binding.
     *
     * @param value source value
     * @param direction conversion direction
     * @return converted value
     * @throws WasmRuntimeException if conversion fails
     */
    WasmValue convert(WasmValue value, ConversionDirection direction);

    /**
     * Creates an instance using this binding.
     *
     * @param arguments constructor arguments
     * @return type instance
     * @throws WasmRuntimeException if creation fails
     */
    WasmValue createInstance(WasmValue... arguments);

    /**
     * Validates type binding.
     *
     * @return validation result
     */
    BindingValidationResult validate();
  }

  /** Import binding information. */
  interface ImportBinding {
    /**
     * Gets the import name.
     *
     * @return import name
     */
    String getImportName();

    /**
     * Gets the resolved import.
     *
     * @return resolved import
     */
    WasmValue getResolvedImport();

    /**
     * Gets binding type.
     *
     * @return binding type
     */
    BindingType getBindingType();

    /**
     * Checks if import is resolved.
     *
     * @return true if resolved
     */
    boolean isResolved();

    /**
     * Gets resolution metadata.
     *
     * @return resolution metadata
     */
    ResolutionMetadata getResolutionMetadata();
  }

  /** Export binding information. */
  interface ExportBinding {
    /**
     * Gets the export name.
     *
     * @return export name
     */
    String getExportName();

    /**
     * Gets the exported value.
     *
     * @return exported value
     */
    WasmValue getExportedValue();

    /**
     * Gets binding type.
     *
     * @return binding type
     */
    BindingType getBindingType();

    /**
     * Gets export metadata.
     *
     * @return export metadata
     */
    ExportMetadata getExportMetadata();
  }

  /** Binding statistics. */
  interface BindingStatistics {
    /**
     * Gets total number of bindings.
     *
     * @return total bindings
     */
    int getTotalBindings();

    /**
     * Gets number of function bindings.
     *
     * @return function bindings count
     */
    int getFunctionBindings();

    /**
     * Gets number of type bindings.
     *
     * @return type bindings count
     */
    int getTypeBindings();

    /**
     * Gets number of direct bindings.
     *
     * @return direct bindings count
     */
    int getDirectBindings();

    /**
     * Gets number of adapted bindings.
     *
     * @return adapted bindings count
     */
    int getAdaptedBindings();

    /**
     * Gets binding success rate.
     *
     * @return success rate (0.0 to 1.0)
     */
    double getSuccessRate();

    /**
     * Gets average binding time.
     *
     * @return average binding time in nanoseconds
     */
    double getAverageBindingTime();

    /**
     * Gets detailed statistics.
     *
     * @return detailed statistics map
     */
    Map<String, Object> getDetailedStatistics();
  }

  /** Binding validation result. */
  interface BindingValidationResult {
    /**
     * Checks if validation passed.
     *
     * @return true if valid
     */
    boolean isValid();

    /**
     * Gets validation errors.
     *
     * @return list of errors
     */
    List<String> getErrors();

    /**
     * Gets validation warnings.
     *
     * @return list of warnings
     */
    List<String> getWarnings();

    /**
     * Gets binding coverage.
     *
     * @return coverage percentage (0.0 to 1.0)
     */
    double getCoverage();

    /**
     * Gets unbound items.
     *
     * @return list of unbound item names
     */
    List<String> getUnboundItems();
  }

  /** Binding metadata. */
  interface BindingMetadata {
    /**
     * Gets binding creation time.
     *
     * @return creation timestamp
     */
    java.time.Instant getCreationTime();

    /**
     * Gets binding version.
     *
     * @return binding version
     */
    String getVersion();

    /**
     * Gets generator information.
     *
     * @return generator info
     */
    String getGenerator();

    /**
     * Gets additional properties.
     *
     * @return properties map
     */
    Map<String, Object> getProperties();
  }

  /** Adaptation metadata. */
  interface AdaptationMetadata {
    /**
     * Gets adaptation type.
     *
     * @return adaptation type
     */
    String getAdaptationType();

    /**
     * Gets adaptation complexity.
     *
     * @return complexity level
     */
    ComplexityLevel getComplexity();

    /**
     * Checks if adaptation is lossy.
     *
     * @return true if lossy
     */
    boolean isLossy();

    /**
     * Gets performance impact.
     *
     * @return performance impact level
     */
    PerformanceImpact getPerformanceImpact();
  }

  /** Resolution metadata. */
  interface ResolutionMetadata {
    /**
     * Gets resolution strategy.
     *
     * @return resolution strategy
     */
    String getStrategy();

    /**
     * Gets resolution time.
     *
     * @return resolution timestamp
     */
    java.time.Instant getResolutionTime();

    /**
     * Gets fallback information.
     *
     * @return fallback info if applicable
     */
    Optional<String> getFallbackInfo();
  }

  /** Export metadata. */
  interface ExportMetadata {
    /**
     * Gets export visibility.
     *
     * @return visibility level
     */
    VisibilityLevel getVisibility();

    /**
     * Gets export documentation.
     *
     * @return documentation if available
     */
    Optional<String> getDocumentation();

    /**
     * Gets export attributes.
     *
     * @return attributes map
     */
    Map<String, String> getAttributes();
  }

  // Enums for categorization
  /** Type of binding between WIT and Java. */
  enum BindingType {
    DIRECT,
    ADAPTED,
    GENERATED,
    CUSTOM
  }

  /** Direction of type conversion. */
  enum ConversionDirection {
    FORWARD,
    REVERSE
  }

  /** Complexity level of binding operations. */
  enum ComplexityLevel {
    LOW,
    MEDIUM,
    HIGH
  }

  /** Performance impact level of conversions. */
  enum PerformanceImpact {
    MINIMAL,
    LOW,
    MEDIUM,
    HIGH
  }

  /** Visibility level for generated bindings. */
  enum VisibilityLevel {
    PRIVATE,
    PACKAGE,
    PROTECTED,
    PUBLIC
  }
}
