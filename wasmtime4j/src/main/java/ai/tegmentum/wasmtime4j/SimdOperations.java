package ai.tegmentum.wasmtime4j;

/**
 * SIMD operations interface for WebAssembly components.
 *
 * @since 1.0.0
 */
public interface SimdOperations {

  /** SIMD lane operation enumeration. */
  enum LaneOperation {
    /** Extract lane. */
    EXTRACT,
    /** Replace lane. */
    REPLACE,
    /** Splat value across lanes. */
    SPLAT
  }

  /** SIMD arithmetic operation enumeration. */
  enum ArithmeticOperation {
    /** Addition. */
    ADD,
    /** Subtraction. */
    SUB,
    /** Multiplication. */
    MUL,
    /** Division. */
    DIV
  }

  /** SIMD comparison operation enumeration. */
  enum ComparisonOperation {
    /** Equal. */
    EQ,
    /** Not equal. */
    NE,
    /** Less than. */
    LT,
    /** Greater than. */
    GT,
    /** Less than or equal. */
    LE,
    /** Greater than or equal. */
    GE
  }

  /**
   * Gets the SIMD lane count.
   *
   * @return the lane count
   */
  int getLaneCount();

  /**
   * Gets the SIMD element type.
   *
   * @return the element type
   */
  String getElementType();

  /**
   * Checks if SIMD operations are supported.
   *
   * @return true if SIMD is supported
   */
  boolean isSimdSupported();

  /** 128-bit SIMD value interface. */
  interface V128 {
    /**
     * Gets the value as byte array.
     *
     * @return byte array representation
     */
    byte[] toByteArray();

    /**
     * Gets the value as integer array.
     *
     * @return integer array representation
     */
    int[] toIntArray();

    /**
     * Gets the value as float array.
     *
     * @return float array representation
     */
    float[] toFloatArray();

    /**
     * Gets the value as long array.
     *
     * @return long array representation
     */
    long[] toLongArray();

    /**
     * Gets the value as double array.
     *
     * @return double array representation
     */
    double[] toDoubleArray();
  }
}
