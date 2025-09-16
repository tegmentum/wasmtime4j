package ai.tegmentum.wasmtime4j.comparison.analyzers;

/**
 * Types of comparisons that can be performed on values during behavioral analysis.
 *
 * @since 1.0.0
 */
public enum ComparisonType {
  /** Values are identical references or primitives. */
  EXACT_MATCH("Exact match"),

  /** Numeric comparison with tolerance. */
  NUMERIC("Numeric comparison"),

  /** String comparison with semantic analysis. */
  STRING("String comparison"),

  /** Temporal value comparison. */
  TEMPORAL("Temporal comparison"),

  /** Array element-by-element comparison. */
  ARRAY("Array comparison"),

  /** Collection comparison. */
  COLLECTION("Collection comparison"),

  /** Map key-value comparison. */
  MAP("Map comparison"),

  /** Enum value comparison. */
  ENUM("Enum comparison"),

  /** ByteBuffer content comparison. */
  BYTE_BUFFER("ByteBuffer comparison"),

  /** Object field reflection comparison. */
  OBJECT_REFLECTION("Object reflection comparison"),

  /** Reference/identity comparison. */
  REFERENCE("Reference comparison"),

  /** One value is null, other is not. */
  NULL_MISMATCH("Null mismatch"),

  /** Values have incompatible types. */
  TYPE_MISMATCH("Type mismatch"),

  /** Comparison depth limit exceeded. */
  DEPTH_LIMIT_EXCEEDED("Depth limit exceeded"),

  /** Unknown or unhandled comparison type. */
  UNKNOWN("Unknown comparison");

  private final String description;

  ComparisonType(final String description) {
    this.description = description;
  }

  public String getDescription() {
    return description;
  }
}
