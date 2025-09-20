package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.io.Closeable;
import java.util.Map;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model resource instances.
 *
 * <p>ComponentResource represents a runtime instance of a resource type with its own state and
 * capabilities. Resources provide controlled access to stateful objects and enable capability-
 * based security in the Component Model.
 *
 * <p>Resource instances maintain their lifecycle according to their resource type's policy and
 * provide access to the methods and properties defined by their type.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentResource resource = export.asResource();
 * ComponentFunction method = resource.getMethod("read").orElseThrow();
 * ComponentValue result = method.call();
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentResource extends Closeable {

  /**
   * Gets the resource type of this instance.
   *
   * @return the resource type
   */
  ComponentResourceType getResourceType();

  /**
   * Gets the handle for this resource instance.
   *
   * <p>Returns a handle that can be used to reference this resource in component function calls
   * and resource management operations.
   *
   * @return the resource handle
   */
  ComponentResourceHandle getHandle();

  /**
   * Gets a method from this resource instance.
   *
   * <p>Returns a bound method that can be called on this specific resource instance.
   *
   * @param name the method name to retrieve
   * @return the bound method, or empty if not found
   * @throws WasmException if the resource is invalid or method retrieval fails
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentFunction> getMethod(final String name) throws WasmException;

  /**
   * Gets the names of all available methods on this resource.
   *
   * @return list of method names
   * @throws WasmException if the resource is invalid
   */
  java.util.List<String> getMethodNames() throws WasmException;

  /**
   * Checks if this resource has a specific method.
   *
   * @param name the method name to check
   * @return true if the method exists, false otherwise
   * @throws WasmException if the resource is invalid
   * @throws IllegalArgumentException if name is null or empty
   */
  boolean hasMethod(final String name) throws WasmException;

  /**
   * Gets a property value from this resource instance.
   *
   * @param name the property name to retrieve
   * @return the property value, or empty if not found
   * @throws WasmException if the resource is invalid or property access fails
   * @throws IllegalArgumentException if name is null or empty
   */
  Optional<ComponentValue> getProperty(final String name) throws WasmException;

  /**
   * Sets a property value on this resource instance.
   *
   * @param name the property name to set
   * @param value the property value
   * @throws WasmException if the resource is invalid or property setting fails
   * @throws IllegalArgumentException if name is null/empty or value is invalid
   */
  void setProperty(final String name, final ComponentValue value) throws WasmException;

  /**
   * Gets all property values from this resource instance.
   *
   * @return map of property names to their values
   * @throws WasmException if the resource is invalid
   */
  Map<String, ComponentValue> getProperties() throws WasmException;

  /**
   * Gets the current state of this resource instance.
   *
   * @return the resource state
   */
  ComponentResourceState getState();

  /**
   * Checks if this resource supports a specific capability.
   *
   * @param capability the capability name to check
   * @return true if the capability is supported and available, false otherwise
   * @throws WasmException if the resource is invalid
   * @throws IllegalArgumentException if capability is null or empty
   */
  boolean hasCapability(final String capability) throws WasmException;

  /**
   * Gets all available capabilities for this resource instance.
   *
   * @return list of capability names
   * @throws WasmException if the resource is invalid
   */
  java.util.List<String> getCapabilities() throws WasmException;

  /**
   * Gets usage statistics for this resource instance.
   *
   * <p>Returns information about method calls, property access, and resource utilization.
   *
   * @return resource usage statistics
   */
  ComponentResourceStats getStats();

  /**
   * Checks if this resource instance is still valid and usable.
   *
   * <p>Resources become invalid when closed, when their parent component instance is closed,
   * or when their lifecycle policy determines they should be destroyed.
   *
   * @return true if the resource is valid and usable, false otherwise
   */
  boolean isValid();

  /**
   * Closes this resource instance and releases associated resources.
   *
   * <p>After calling this method, the resource becomes invalid and should not be used. The
   * behavior of this method depends on the resource type's lifecycle policy.
   */
  @Override
  void close();
}