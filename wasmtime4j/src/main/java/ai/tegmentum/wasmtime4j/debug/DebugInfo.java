package ai.tegmentum.wasmtime4j.debug;

/**
 * Debug information interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface DebugInfo {

  /**
   * Gets the module name.
   *
   * @return module name
   */
  String getModuleName();

  /**
   * Gets function debug information.
   *
   * @return list of function debug info
   */
  java.util.List<FunctionDebugInfo> getFunctionDebugInfo();

  /**
   * Gets global variable information.
   *
   * @return list of global variables
   */
  java.util.List<GlobalVariableInfo> getGlobalVariables();

  /**
   * Gets type information.
   *
   * @return list of type info
   */
  java.util.List<TypeInfo> getTypeInfo();

  /**
   * Finds debug info by address.
   *
   * @param address instruction address
   * @return debug info or null
   */
  AddressDebugInfo getDebugInfoAtAddress(long address);

  /** Function debug information interface. */
  interface FunctionDebugInfo {
    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();

    /**
     * Gets the start address.
     *
     * @return start address
     */
    long getStartAddress();

    /**
     * Gets the end address.
     *
     * @return end address
     */
    long getEndAddress();

    /**
     * Gets local variables.
     *
     * @return list of local variables
     */
    java.util.List<VariableDebugInfo> getLocalVariables();

    /**
     * Gets parameters.
     *
     * @return list of parameters
     */
    java.util.List<VariableDebugInfo> getParameters();
  }

  /** Variable debug information interface. */
  interface VariableDebugInfo {
    /**
     * Gets the variable name.
     *
     * @return variable name
     */
    String getName();

    /**
     * Gets the variable type.
     *
     * @return variable type
     */
    String getType();

    /**
     * Gets the storage location.
     *
     * @return storage location
     */
    String getLocation();
  }

  /** Global variable information interface. */
  interface GlobalVariableInfo {
    /**
     * Gets the variable name.
     *
     * @return variable name
     */
    String getName();

    /**
     * Gets the variable type.
     *
     * @return variable type
     */
    String getType();

    /**
     * Gets the memory address.
     *
     * @return memory address
     */
    long getAddress();
  }

  /** Type information interface. */
  interface TypeInfo {
    /**
     * Gets the type name.
     *
     * @return type name
     */
    String getTypeName();

    /**
     * Gets the type size.
     *
     * @return size in bytes
     */
    int getSize();

    /**
     * Gets the type kind.
     *
     * @return type kind
     */
    TypeKind getTypeKind();
  }

  /** Type kind enumeration. */
  enum TypeKind {
    /** Primitive type. */
    PRIMITIVE,
    /** Struct type. */
    STRUCT,
    /** Array type. */
    ARRAY,
    /** Pointer type. */
    POINTER,
    /** Function type. */
    FUNCTION
  }

  /** Address debug information interface. */
  interface AddressDebugInfo {
    /**
     * Gets the source file.
     *
     * @return source file path
     */
    String getSourceFile();

    /**
     * Gets the line number.
     *
     * @return line number
     */
    int getLineNumber();

    /**
     * Gets the column number.
     *
     * @return column number
     */
    int getColumnNumber();

    /**
     * Gets the function name.
     *
     * @return function name
     */
    String getFunctionName();
  }
}
