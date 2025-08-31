package ai.tegmentum.wasmtime4j.wasi;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.List;
import java.util.Map;

/**
 * Interface for resolving WASI component imports.
 *
 * <p>Import resolvers provide implementations for component interface imports. When a component
 * requires external functionality, resolvers supply the necessary implementations, whether as
 * host functions, other components, or resource providers.
 *
 * <p>Resolvers can be used to inject custom functionality, provide mock implementations for
 * testing, or create bridges to existing Java code.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * WasiImportResolver resolver = new WasiImportResolver() {
 *     public Object resolveFunction(String functionName, List<Object> parameters) {
 *         if ("get-current-time".equals(functionName)) {
 *             return System.currentTimeMillis();
 *         }
 *         throw new WasmException("Unknown function: " + functionName);
 *     }
 * };
 * }</pre>
 *
 * @since 1.0.0
 */
public interface WasiImportResolver {

  /**
   * Gets the name of the interface this resolver handles.
   *
   * @return the interface name
   */
  String getInterfaceName();

  /**
   * Gets the version of the interface this resolver supports.
   *
   * @return the interface version, or null if version-agnostic
   */
  String getInterfaceVersion();

  /**
   * Lists all functions provided by this resolver.
   *
   * @return list of function names this resolver can handle
   */
  List<String> getProvidedFunctions();

  /**
   * Lists all resource types provided by this resolver.
   *
   * @return list of resource type names this resolver can create
   */
  List<String> getProvidedResourceTypes();

  /**
   * Resolves a function call from the component.
   *
   * <p>This method is called when the component invokes an imported function. The resolver should
   * execute the appropriate logic and return the result in the expected format.
   *
   * @param functionName the name of the function being called
   * @param parameters the function parameters in order
   * @return the function result, or null for void functions
   * @throws WasmException if the function call fails or function is not supported
   * @throws IllegalArgumentException if functionName is null or parameters are invalid
   */
  Object resolveFunction(final String functionName, final List<Object> parameters)
      throws WasmException;

  /**
   * Creates a resource of the specified type.
   *
   * <p>This method is called when the component needs to create a resource through an imported
   * interface. The resolver should create and return an appropriate resource implementation.
   *
   * @param resourceType the type of resource to create
   * @param parameters creation parameters
   * @return the created resource
   * @throws WasmException if resource creation fails or type is not supported
   * @throws IllegalArgumentException if resourceType is null or parameters are invalid
   */
  WasiResource createResource(final String resourceType, final Object... parameters)
      throws WasmException;

  /**
   * Gets metadata about a function provided by this resolver.
   *
   * <p>Returns information about function signatures, parameter types, and return types.
   *
   * @param functionName the function name to inspect
   * @return function metadata
   * @throws WasmException if function doesn't exist or metadata cannot be retrieved
   * @throws IllegalArgumentException if functionName is null or empty
   */
  WasiFunctionMetadata getFunctionMetadata(final String functionName) throws WasmException;

  /**
   * Gets metadata about a resource type provided by this resolver.
   *
   * @param resourceType the resource type name to inspect
   * @return resource type metadata
   * @throws WasmException if resource type doesn't exist or metadata cannot be retrieved
   * @throws IllegalArgumentException if resourceType is null or empty
   */
  WasiResourceTypeMetadata getResourceTypeMetadata(final String resourceType) throws WasmException;

  /**
   * Checks if this resolver can handle the specified function.
   *
   * @param functionName the function name to check
   * @return true if the function is supported, false otherwise
   */
  boolean canResolveFunction(final String functionName);

  /**
   * Checks if this resolver can create the specified resource type.
   *
   * @param resourceType the resource type to check
   * @return true if the resource type is supported, false otherwise
   */
  boolean canCreateResourceType(final String resourceType);

  /**
   * Gets custom properties and configuration for this resolver.
   *
   * <p>Properties can include resolver-specific settings, debugging information, or metadata
   * useful for management and monitoring.
   *
   * @return map of property names to values
   */
  Map<String, Object> getProperties();

  /**
   * Sets a custom property on this resolver.
   *
   * @param key the property key
   * @param value the property value
   * @throws IllegalArgumentException if key is null or empty
   */
  void setProperty(final String key, final Object value);

  /**
   * Validates this resolver for correctness and completeness.
   *
   * <p>Validation checks include function availability, resource type consistency, and
   * configuration correctness.
   *
   * @throws WasmException if validation fails with details about specific issues
   */
  void validate() throws WasmException;

  /**
   * Initializes this resolver with the specified component context.
   *
   * <p>This method is called before the resolver is used, allowing it to prepare for operation
   * with knowledge of the requesting component.
   *
   * @param component the component that will use this resolver
   * @throws WasmException if initialization fails
   * @throws IllegalArgumentException if component is null
   */
  void initialize(final WasiComponent component) throws WasmException;

  /**
   * Cleans up resources used by this resolver.
   *
   * <p>This method is called when the resolver is no longer needed, allowing it to release any
   * held resources or perform cleanup operations.
   *
   * @throws WasmException if cleanup encounters errors
   */
  void cleanup() throws WasmException;
}