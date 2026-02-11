package ai.tegmentum.wasmtime4j.wit;
import ai.tegmentum.wasmtime4j.component.Component;

import java.util.List;
import java.util.Set;

/**
 * Represents a WebAssembly Interface Type (WIT) interface definition.
 *
 * <p>WIT interfaces define the contract between WebAssembly components, providing type-safe
 * interaction and composition capabilities in the Component Model.
 *
 * <p>This interface provides access to interface metadata, type definitions, function signatures,
 * and validation capabilities.
 *
 * @since 1.0.0
 */
public interface WitInterfaceDefinition {

  /**
   * Gets the name of this WIT interface.
   *
   * @return the interface name
   */
  String getName();

  /**
   * Gets the version of this WIT interface.
   *
   * @return the interface version as a string
   */
  String getVersion();

  /**
   * Gets the package name containing this interface.
   *
   * @return the package name
   */
  String getPackageName();

  /**
   * Gets all function names in this interface.
   *
   * @return list of function names
   */
  List<String> getFunctionNames();

  /**
   * Gets all type names in this interface.
   *
   * @return list of type names
   */
  List<String> getTypeNames();

  /**
   * Gets the interfaces that this interface depends on.
   *
   * @return set of interface dependencies
   */
  Set<String> getDependencies();

  /**
   * Checks if this interface is compatible with another interface.
   *
   * @param other the other interface to check compatibility with
   * @return compatibility result
   */
  WitCompatibilityResult isCompatibleWith(WitInterfaceDefinition other);

  /**
   * Gets the raw WIT definition text.
   *
   * @return the WIT definition as text
   */
  String getWitText();

  /**
   * Gets imports required by this interface.
   *
   * @return list of import names
   */
  List<String> getImportNames();

  /**
   * Gets exports provided by this interface.
   *
   * @return list of export names
   */
  List<String> getExportNames();
}
