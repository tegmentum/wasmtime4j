package ai.tegmentum.wasmtime4j.component;

import ai.tegmentum.wasmtime4j.exception.WasmException;
import java.util.Optional;

/**
 * Interface for WebAssembly Component Model component exports.
 *
 * <p>ComponentExport represents an exported entity from a component instance. Exports can be
 * functions, interfaces, resources, or other component-defined entities that are made available to
 * external consumers.
 *
 * <p>Component exports provide typed access to component functionality and maintain the structured
 * interface contracts defined by the Component Model specification.
 *
 * <p>Example usage:
 *
 * <pre>{@code
 * ComponentExport export = instance.getExport("my-interface").orElseThrow();
 * ComponentExportKind kind = export.getKind();
 *
 * if (kind == ComponentExportKind.FUNCTION) {
 *     ComponentFunction func = export.asFunction();
 *     ComponentValue result = func.call(args);
 * } else if (kind == ComponentExportKind.INTERFACE) {
 *     ComponentInterface iface = export.asInterface();
 *     // Use interface methods
 * }
 * }</pre>
 *
 * @since 1.0.0
 */
public interface ComponentExport {

  /**
   * Gets the name of this export.
   *
   * @return the export name
   */
  String getName();

  /**
   * Gets the kind of this export.
   *
   * <p>Returns the type of entity being exported (function, interface, resource, etc.).
   *
   * @return the export kind
   */
  ComponentExportKind getKind();

  /**
   * Gets the type information for this export.
   *
   * <p>Returns detailed type information including function signatures, interface definitions, or
   * resource type specifications.
   *
   * @return the export type information
   */
  ComponentExportType getType();

  /**
   * Converts this export to a ComponentFunction if it is a function export.
   *
   * @return the export as a ComponentFunction
   * @throws WasmException if this export is not a function
   */
  ComponentFunction asFunction() throws WasmException;

  /**
   * Converts this export to a ComponentInterface if it is an interface export.
   *
   * @return the export as a ComponentInterface
   * @throws WasmException if this export is not an interface
   */
  ComponentInterface asInterface() throws WasmException;

  /**
   * Converts this export to a ComponentResource if it is a resource export.
   *
   * @return the export as a ComponentResource
   * @throws WasmException if this export is not a resource
   */
  ComponentResource asResource() throws WasmException;

  /**
   * Safely attempts to convert this export to a ComponentFunction.
   *
   * @return the export as a ComponentFunction, or empty if not a function
   */
  Optional<ComponentFunction> tryAsFunction();

  /**
   * Safely attempts to convert this export to a ComponentInterface.
   *
   * @return the export as a ComponentInterface, or empty if not an interface
   */
  Optional<ComponentInterface> tryAsInterface();

  /**
   * Safely attempts to convert this export to a ComponentResource.
   *
   * @return the export as a ComponentResource, or empty if not a resource
   */
  Optional<ComponentResource> tryAsResource();

  /**
   * Gets documentation for this export if available.
   *
   * @return export documentation, or empty if not available
   */
  Optional<String> getDocumentation();

  /**
   * Gets metadata about this export.
   *
   * <p>Returns information about performance characteristics, usage patterns, and other
   * export-specific metadata.
   *
   * @return export metadata
   */
  ComponentExportMetadata getMetadata();

  /**
   * Checks if this export is still valid and usable.
   *
   * <p>Exports become invalid when their parent component instance is closed or becomes invalid.
   *
   * @return true if the export is valid and usable, false otherwise
   */
  boolean isValid();
}
