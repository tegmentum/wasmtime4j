package ai.tegmentum.wasmtime4j.component;

import java.util.List;
import java.util.Optional;

/**
 * Type information for a WebAssembly component export.
 *
 * <p>ComponentExportType provides detailed type information about component exports including
 * function signatures, interface definitions, resource types, and other type metadata. This
 * information enables type-safe interaction with component exports and runtime validation.
 *
 * <p>Export types encompass the full range of component model types including functions,
 * interfaces, resources, and composite types like records and variants.
 *
 * @since 1.0.0
 */
public interface ComponentExportType {

  /**
   * Gets the name of this export.
   *
   * <p>Returns the export name as declared in the component's interface definition.
   *
   * @return the export name
   */
  String getName();

  /**
   * Gets the kind of this export.
   *
   * <p>Returns the category of export (function, interface, resource, etc.) that this type
   * represents.
   *
   * @return the export kind
   */
  ComponentExportKind getKind();

  /**
   * Gets the value type for this export.
   *
   * <p>Returns the component value type that describes the structure and constraints of this
   * export's type.
   *
   * @return the component value type
   */
  ComponentValueType getValueType();

  /**
   * Gets function type information if this export is a function.
   *
   * <p>Returns detailed function signature information including parameter types, return type, and
   * calling conventions. Returns empty if this export is not a function.
   *
   * @return function type information, or empty if not a function
   */
  Optional<ComponentFunctionType> getFunctionType();

  /**
   * Gets interface type information if this export is an interface.
   *
   * <p>Returns complete interface definition including methods, resources, and type definitions.
   * Returns empty if this export is not an interface.
   *
   * @return interface type information, or empty if not an interface
   */
  Optional<InterfaceType> getInterfaceType();

  /**
   * Gets resource type information if this export is a resource.
   *
   * <p>Returns resource type definition including resource methods and lifecycle information.
   * Returns empty if this export is not a resource.
   *
   * @return resource type information, or empty if not a resource
   */
  Optional<ComponentResourceType> getResourceType();

  /**
   * Gets record type information if this export is a record.
   *
   * <p>Returns record structure definition including field names, types, and layout information.
   * Returns empty if this export is not a record type.
   *
   * @return record type information, or empty if not a record
   */
  Optional<ComponentRecord> getRecordType();

  /**
   * Gets variant type information if this export is a variant.
   *
   * <p>Returns variant definition including case names, associated types, and discriminant
   * information. Returns empty if this export is not a variant type.
   *
   * @return variant type information, or empty if not a variant
   */
  Optional<ComponentVariant> getVariantType();

  /**
   * Checks if this export type is compatible with another type.
   *
   * <p>Determines if values of this export type can be safely passed to or used where the other
   * type is expected, following component model subtyping rules.
   *
   * @param other the type to check compatibility with
   * @return true if types are compatible, false otherwise
   * @throws IllegalArgumentException if other is null
   */
  boolean isCompatibleWith(final ComponentExportType other);

  /**
   * Gets the type constraints for this export.
   *
   * <p>Returns any additional constraints or validation rules that apply to values of this export
   * type beyond the basic type structure.
   *
   * @return list of type constraints
   */
  List<ComponentTypeConstraint> getConstraints();

  /**
   * Gets the nullability information for this export type.
   *
   * <p>Returns whether values of this type can be null or must always have a valid value. This
   * is relevant for optional types and resource handles.
   *
   * @return true if the type is nullable, false otherwise
   */
  boolean isNullable();

  /**
   * Gets the mutability information for this export type.
   *
   * <p>Returns whether values of this type can be modified after creation or are immutable. This
   * affects how values can be used and shared across component boundaries.
   *
   * @return true if the type is mutable, false if immutable
   */
  boolean isMutable();

  /**
   * Gets type parameters if this is a generic type.
   *
   * <p>Returns type parameter information for generic or parameterized types. Returns empty list
   * for non-generic types.
   *
   * @return list of type parameters
   */
  List<ComponentTypeParameter> getTypeParameters();

  /**
   * Gets the size information for this type.
   *
   * <p>Returns size characteristics including fixed size (for value types), alignment
   * requirements, and memory layout information.
   *
   * @return type size information
   */
  ComponentTypeSize getTypeSize();

  /**
   * Gets documentation or description for this export type.
   *
   * <p>Returns human-readable description or documentation that was embedded with this type
   * definition, if available.
   *
   * @return type documentation, or empty if none available
   */
  Optional<String> getDocumentation();

  /**
   * Validates a value against this export type.
   *
   * <p>Checks if the provided component value conforms to this export type's constraints and
   * structure. This can be used for runtime type checking and validation.
   *
   * @param value the component value to validate
   * @return true if the value is valid for this type, false otherwise
   * @throws IllegalArgumentException if value is null
   */
  boolean validateValue(final ComponentValue value);
}