package ai.tegmentum.wasmtime4j;

/**
 * Supported WIT interface evolution operations.
 *
 * <p>This enum defines the types of evolution operations that can be performed on WIT interfaces
 * while maintaining compatibility and semantic correctness.
 *
 * @since 1.0.0
 */
public enum WitEvolutionOperation {

  /** Add a new function to the interface. */
  ADD_FUNCTION("Add new function", false, OperationComplexity.LOW),

  /** Remove an existing function from the interface. */
  REMOVE_FUNCTION("Remove existing function", true, OperationComplexity.MEDIUM),

  /** Modify function signature (parameters or return type). */
  MODIFY_FUNCTION_SIGNATURE("Modify function signature", true, OperationComplexity.HIGH),

  /** Rename a function. */
  RENAME_FUNCTION("Rename function", true, OperationComplexity.MEDIUM),

  /** Add a new type definition. */
  ADD_TYPE("Add new type", false, OperationComplexity.LOW),

  /** Remove an existing type definition. */
  REMOVE_TYPE("Remove existing type", true, OperationComplexity.HIGH),

  /** Modify type definition (fields, variants, etc.). */
  MODIFY_TYPE("Modify type definition", true, OperationComplexity.HIGH),

  /** Rename a type. */
  RENAME_TYPE("Rename type", true, OperationComplexity.MEDIUM),

  /** Add optional field to record type. */
  ADD_OPTIONAL_FIELD("Add optional field to record", false, OperationComplexity.LOW),

  /** Add required field to record type. */
  ADD_REQUIRED_FIELD("Add required field to record", true, OperationComplexity.HIGH),

  /** Remove field from record type. */
  REMOVE_FIELD("Remove field from record", true, OperationComplexity.HIGH),

  /** Make required field optional. */
  MAKE_FIELD_OPTIONAL("Make field optional", false, OperationComplexity.MEDIUM),

  /** Make optional field required. */
  MAKE_FIELD_REQUIRED("Make field required", true, OperationComplexity.HIGH),

  /** Add variant case. */
  ADD_VARIANT_CASE("Add variant case", false, OperationComplexity.LOW),

  /** Remove variant case. */
  REMOVE_VARIANT_CASE("Remove variant case", true, OperationComplexity.MEDIUM),

  /** Add enum value. */
  ADD_ENUM_VALUE("Add enum value", false, OperationComplexity.LOW),

  /** Remove enum value. */
  REMOVE_ENUM_VALUE("Remove enum value", true, OperationComplexity.MEDIUM),

  /** Change interface version. */
  UPDATE_VERSION("Update interface version", false, OperationComplexity.LOW),

  /** Change package name. */
  CHANGE_PACKAGE("Change package name", true, OperationComplexity.MEDIUM),

  /** Add import dependency. */
  ADD_IMPORT("Add import dependency", false, OperationComplexity.LOW),

  /** Remove import dependency. */
  REMOVE_IMPORT("Remove import dependency", true, OperationComplexity.MEDIUM),

  /** Add export. */
  ADD_EXPORT("Add export", false, OperationComplexity.LOW),

  /** Remove export. */
  REMOVE_EXPORT("Remove export", true, OperationComplexity.MEDIUM),

  /** Convert type to more general form. */
  GENERALIZE_TYPE("Generalize type", false, OperationComplexity.HIGH),

  /** Convert type to more specific form. */
  SPECIALIZE_TYPE("Specialize type", true, OperationComplexity.HIGH),

  /** Merge multiple types into one. */
  MERGE_TYPES("Merge types", true, OperationComplexity.HIGH),

  /** Split one type into multiple. */
  SPLIT_TYPE("Split type", true, OperationComplexity.HIGH),

  /** Add default parameter value. */
  ADD_DEFAULT_PARAMETER("Add default parameter value", false, OperationComplexity.MEDIUM),

  /** Remove default parameter value. */
  REMOVE_DEFAULT_PARAMETER("Remove default parameter value", true, OperationComplexity.MEDIUM),

  /** Change parameter order. */
  REORDER_PARAMETERS("Reorder parameters", true, OperationComplexity.HIGH),

  /** Add parameter to function. */
  ADD_PARAMETER("Add function parameter", true, OperationComplexity.HIGH),

  /** Remove parameter from function. */
  REMOVE_PARAMETER("Remove function parameter", true, OperationComplexity.HIGH),

  /** Change return type. */
  CHANGE_RETURN_TYPE("Change return type", true, OperationComplexity.HIGH),

  /** Add error to result type. */
  ADD_ERROR_TYPE("Add error to result type", false, OperationComplexity.MEDIUM),

  /** Remove error from result type. */
  REMOVE_ERROR_TYPE("Remove error from result type", true, OperationComplexity.MEDIUM),

  /** Convert synchronous function to asynchronous. */
  MAKE_ASYNC("Make function asynchronous", true, OperationComplexity.HIGH),

  /** Convert asynchronous function to synchronous. */
  MAKE_SYNC("Make function synchronous", true, OperationComplexity.HIGH);

  private final String description;
  private final boolean breaking;
  private final OperationComplexity complexity;

  /**
   * Creates a WIT evolution operation.
   *
   * @param description operation description
   * @param breaking whether this is a breaking change
   * @param complexity operation complexity
   */
  WitEvolutionOperation(
      final String description, final boolean breaking, final OperationComplexity complexity) {
    this.description = description;
    this.breaking = breaking;
    this.complexity = complexity;
  }

  /**
   * Gets the operation description.
   *
   * @return operation description
   */
  public String getDescription() {
    return description;
  }

  /**
   * Checks if this is a breaking change operation.
   *
   * @return true if breaking
   */
  public boolean isBreaking() {
    return breaking;
  }

  /**
   * Gets the operation complexity.
   *
   * @return operation complexity
   */
  public OperationComplexity getComplexity() {
    return complexity;
  }

  /**
   * Checks if the operation is safe (non-breaking).
   *
   * @return true if safe
   */
  public boolean isSafe() {
    return !breaking;
  }

  /**
   * Gets the operation category.
   *
   * @return operation category
   */
  public OperationCategory getCategory() {
    final String name = name();
    if (name.contains("FUNCTION")) {
      return OperationCategory.FUNCTION;
    } else if (name.contains("TYPE")
        || name.contains("FIELD")
        || name.contains("VARIANT")
        || name.contains("ENUM")) {
      return OperationCategory.TYPE;
    } else if (name.contains("IMPORT") || name.contains("EXPORT")) {
      return OperationCategory.INTERFACE;
    } else if (name.contains("PARAMETER") || name.contains("RETURN")) {
      return OperationCategory.SIGNATURE;
    } else {
      return OperationCategory.METADATA;
    }
  }

  /**
   * Gets the estimated migration effort for this operation.
   *
   * @return migration effort
   */
  public MigrationEffort getEstimatedEffort() {
    if (complexity == OperationComplexity.LOW) {
      return breaking ? MigrationEffort.MEDIUM : MigrationEffort.LOW;
    } else if (complexity == OperationComplexity.MEDIUM) {
      return breaking ? MigrationEffort.HIGH : MigrationEffort.MEDIUM;
    } else if (complexity == OperationComplexity.HIGH) {
      return breaking ? MigrationEffort.VERY_HIGH : MigrationEffort.HIGH;
    } else {
      throw new IllegalArgumentException("Unknown complexity: " + complexity);
    }
  }

  /** Operation complexity levels. */
  public enum OperationComplexity {
    /** Low complexity - simple operations. */
    LOW,
    /** Medium complexity - moderate changes. */
    MEDIUM,
    /** High complexity - significant changes. */
    HIGH
  }

  /** Operation categories for grouping related operations. */
  public enum OperationCategory {
    /** Function-related operations. */
    FUNCTION,
    /** Type-related operations. */
    TYPE,
    /** Interface structure operations. */
    INTERFACE,
    /** Function signature operations. */
    SIGNATURE,
    /** Metadata operations. */
    METADATA
  }

  /** Migration effort levels. */
  public enum MigrationEffort {
    /** Low effort required. */
    LOW,
    /** Medium effort required. */
    MEDIUM,
    /** High effort required. */
    HIGH,
    /** Very high effort required. */
    VERY_HIGH
  }
}
