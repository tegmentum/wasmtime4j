package ai.tegmentum.wasmtime4j;

/**
 * WIT interface introspection interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface WitInterfaceIntrospection {

  /**
   * Gets the interface name.
   *
   * @return interface name
   */
  String getInterfaceName();

  /**
   * Gets the interface version.
   *
   * @return interface version
   */
  String getVersion();

  /**
   * Gets interface functions.
   *
   * @return list of functions
   */
  java.util.List<FunctionInfo> getFunctions();

  /**
   * Gets interface types.
   *
   * @return list of types
   */
  java.util.List<TypeInfo> getTypes();

  /**
   * Gets interface resources.
   *
   * @return list of resources
   */
  java.util.List<ResourceInfo> getResources();

  /**
   * Gets interface documentation.
   *
   * @return documentation string
   */
  String getDocumentation();

  /**
   * Gets interface metadata.
   *
   * @return metadata map
   */
  java.util.Map<String, Object> getMetadata();

  /**
   * Checks if interface is compatible with another version.
   *
   * @param other other interface version
   * @return compatibility result
   */
  CompatibilityResult isCompatibleWith(WitInterfaceIntrospection other);

  /**
   * Gets interface dependencies.
   *
   * @return list of dependencies
   */
  java.util.List<DependencyInfo> getDependencies();

  /**
   * Gets interface exports.
   *
   * @return list of exports
   */
  java.util.List<ExportInfo> getExports();

  /**
   * Gets interface imports.
   *
   * @return list of imports
   */
  java.util.List<ImportInfo> getImports();

  /** Function information interface. */
  interface FunctionInfo {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getName();

    /**
     * Gets function parameters.
     *
     * @return list of parameters
     */
    java.util.List<ParameterInfo> getParameters();

    /**
     * Gets function return types.
     *
     * @return list of return types
     */
    java.util.List<TypeInfo> getReturnTypes();

    /**
     * Gets function documentation.
     *
     * @return documentation
     */
    String getDocumentation();

    /**
     * Checks if function is async.
     *
     * @return true if async
     */
    boolean isAsync();
  }

  /** Type information interface. */
  interface TypeInfo {
    /**
     * Gets the type name.
     *
     * @return type name
     */
    String getName();

    /**
     * Gets the type kind.
     *
     * @return type kind
     */
    TypeKind getKind();

    /**
     * Gets type definition.
     *
     * @return type definition
     */
    String getDefinition();

    /**
     * Gets type documentation.
     *
     * @return documentation
     */
    String getDocumentation();
  }

  /** Parameter information interface. */
  interface ParameterInfo {
    /**
     * Gets the parameter name.
     *
     * @return parameter name
     */
    String getName();

    /**
     * Gets the parameter type.
     *
     * @return parameter type
     */
    TypeInfo getType();

    /**
     * Checks if parameter is optional.
     *
     * @return true if optional
     */
    boolean isOptional();

    /**
     * Gets parameter documentation.
     *
     * @return documentation
     */
    String getDocumentation();
  }

  /** Resource information interface. */
  interface ResourceInfo {
    /**
     * Gets the resource name.
     *
     * @return resource name
     */
    String getName();

    /**
     * Gets resource methods.
     *
     * @return list of methods
     */
    java.util.List<MethodInfo> getMethods();

    /**
     * Gets resource constructor.
     *
     * @return constructor info, or null if none
     */
    ConstructorInfo getConstructor();

    /**
     * Gets resource documentation.
     *
     * @return documentation
     */
    String getDocumentation();
  }

  /** Method information interface. */
  interface MethodInfo {
    /**
     * Gets the method name.
     *
     * @return method name
     */
    String getName();

    /**
     * Gets method parameters.
     *
     * @return list of parameters
     */
    java.util.List<ParameterInfo> getParameters();

    /**
     * Gets method return types.
     *
     * @return list of return types
     */
    java.util.List<TypeInfo> getReturnTypes();

    /**
     * Gets method kind.
     *
     * @return method kind
     */
    MethodKind getKind();

    /**
     * Gets method documentation.
     *
     * @return documentation
     */
    String getDocumentation();
  }

  /** Constructor information interface. */
  interface ConstructorInfo {
    /**
     * Gets constructor parameters.
     *
     * @return list of parameters
     */
    java.util.List<ParameterInfo> getParameters();

    /**
     * Gets constructor documentation.
     *
     * @return documentation
     */
    String getDocumentation();
  }

  /** Dependency information interface. */
  interface DependencyInfo {
    /**
     * Gets the dependency name.
     *
     * @return dependency name
     */
    String getName();

    /**
     * Gets the dependency version constraint.
     *
     * @return version constraint
     */
    String getVersionConstraint();

    /**
     * Checks if dependency is optional.
     *
     * @return true if optional
     */
    boolean isOptional();
  }

  /** Export information interface. */
  interface ExportInfo {
    /**
     * Gets the export name.
     *
     * @return export name
     */
    String getName();

    /**
     * Gets the export type.
     *
     * @return export type
     */
    ExportType getType();

    /**
     * Gets export target.
     *
     * @return target information
     */
    String getTarget();
  }

  /** Import information interface. */
  interface ImportInfo {
    /**
     * Gets the import name.
     *
     * @return import name
     */
    String getName();

    /**
     * Gets the import type.
     *
     * @return import type
     */
    ImportType getType();

    /**
     * Gets import source.
     *
     * @return source information
     */
    String getSource();
  }

  /** Compatibility result interface. */
  interface CompatibilityResult {
    /**
     * Checks if interfaces are compatible.
     *
     * @return true if compatible
     */
    boolean isCompatible();

    /**
     * Gets compatibility issues.
     *
     * @return list of issues
     */
    java.util.List<CompatibilityIssue> getIssues();

    /**
     * Gets compatibility score.
     *
     * @return compatibility score (0.0-1.0)
     */
    double getScore();
  }

  /** Compatibility issue interface. */
  interface CompatibilityIssue {
    /**
     * Gets the issue type.
     *
     * @return issue type
     */
    IssueType getType();

    /**
     * Gets the issue message.
     *
     * @return issue message
     */
    String getMessage();

    /**
     * Gets the issue severity.
     *
     * @return severity level
     */
    IssueSeverity getSeverity();

    /**
     * Gets affected element.
     *
     * @return affected element name
     */
    String getAffectedElement();
  }

  /** Type kind enumeration. */
  enum TypeKind {
    /** Primitive type. */
    PRIMITIVE,
    /** Record type. */
    RECORD,
    /** Variant type. */
    VARIANT,
    /** Enum type. */
    ENUM,
    /** List type. */
    LIST,
    /** Option type. */
    OPTION,
    /** Result type. */
    RESULT,
    /** Tuple type. */
    TUPLE,
    /** Flags type. */
    FLAGS,
    /** Resource type. */
    RESOURCE
  }

  /** Method kind enumeration. */
  enum MethodKind {
    /** Instance method. */
    INSTANCE,
    /** Static method. */
    STATIC,
    /** Constructor. */
    CONSTRUCTOR,
    /** Destructor. */
    DESTRUCTOR
  }

  /** Export type enumeration. */
  enum ExportType {
    /** Function export. */
    FUNCTION,
    /** Type export. */
    TYPE,
    /** Resource export. */
    RESOURCE,
    /** Interface export. */
    INTERFACE
  }

  /** Import type enumeration. */
  enum ImportType {
    /** Function import. */
    FUNCTION,
    /** Type import. */
    TYPE,
    /** Resource import. */
    RESOURCE,
    /** Interface import. */
    INTERFACE
  }

  /** Issue type enumeration. */
  enum IssueType {
    /** Missing function. */
    MISSING_FUNCTION,
    /** Function signature mismatch. */
    FUNCTION_SIGNATURE_MISMATCH,
    /** Missing type. */
    MISSING_TYPE,
    /** Type definition mismatch. */
    TYPE_DEFINITION_MISMATCH,
    /** Version mismatch. */
    VERSION_MISMATCH,
    /** Breaking change. */
    BREAKING_CHANGE
  }

  /** Issue severity enumeration. */
  enum IssueSeverity {
    /** Information. */
    INFO,
    /** Warning. */
    WARNING,
    /** Error. */
    ERROR,
    /** Critical. */
    CRITICAL
  }
}
