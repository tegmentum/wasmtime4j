package ai.tegmentum.wasmtime4j;

/**
 * Enumeration of WebAssembly SIMD instructions supported by the runtime.
 *
 * <p>This enum covers the complete set of SIMD (Single Instruction, Multiple Data) instructions
 * available in the WebAssembly SIMD proposal. These instructions enable high-performance vector
 * operations on 128-bit vectors with various lane configurations.
 *
 * @since 1.0.0
 */
public enum SimdInstruction {

  // Load and Store Operations
  /** Load a 128-bit vector from memory. */
  V128_LOAD,
  /** Store a 128-bit vector to memory. */
  V128_STORE,
  /** Load a value and splat it across all lanes of a 128-bit vector. */
  V128_LOAD_SPLAT,
  /** Load 8 bytes and zero-extend to create a 128-bit vector. */
  V128_LOAD8X8_S,
  /** Load 8 bytes unsigned and zero-extend to create a 128-bit vector. */
  V128_LOAD8X8_U,
  /** Load 4 16-bit values and sign-extend to create a 128-bit vector. */
  V128_LOAD16X4_S,
  /** Load 4 16-bit values unsigned and zero-extend to create a 128-bit vector. */
  V128_LOAD16X4_U,
  /** Load 2 32-bit values and sign-extend to create a 128-bit vector. */
  V128_LOAD32X2_S,
  /** Load 2 32-bit values unsigned and zero-extend to create a 128-bit vector. */
  V128_LOAD32X2_U,

  // Lane Access Operations
  /** Extract a lane from an i8x16 vector. */
  I8X16_EXTRACT_LANE_S,
  /** Extract a lane from an i8x16 vector (unsigned). */
  I8X16_EXTRACT_LANE_U,
  /** Replace a lane in an i8x16 vector. */
  I8X16_REPLACE_LANE,
  /** Extract a lane from an i16x8 vector. */
  I16X8_EXTRACT_LANE_S,
  /** Extract a lane from an i16x8 vector (unsigned). */
  I16X8_EXTRACT_LANE_U,
  /** Replace a lane in an i16x8 vector. */
  I16X8_REPLACE_LANE,
  /** Extract a lane from an i32x4 vector. */
  I32X4_EXTRACT_LANE,
  /** Replace a lane in an i32x4 vector. */
  I32X4_REPLACE_LANE,
  /** Extract a lane from an i64x2 vector. */
  I64X2_EXTRACT_LANE,
  /** Replace a lane in an i64x2 vector. */
  I64X2_REPLACE_LANE,
  /** Extract a lane from an f32x4 vector. */
  F32X4_EXTRACT_LANE,
  /** Replace a lane in an f32x4 vector. */
  F32X4_REPLACE_LANE,
  /** Extract a lane from an f64x2 vector. */
  F64X2_EXTRACT_LANE,
  /** Replace a lane in an f64x2 vector. */
  F64X2_REPLACE_LANE,

  // Arithmetic Operations - i8x16
  /** Add corresponding lanes of two i8x16 vectors. */
  I8X16_ADD,
  /** Subtract corresponding lanes of two i8x16 vectors. */
  I8X16_SUB,
  /** Multiply corresponding lanes of two i8x16 vectors. */
  I8X16_MUL,
  /** Negate all lanes of an i8x16 vector. */
  I8X16_NEG,
  /** Add corresponding lanes of two i8x16 vectors with saturation (signed). */
  I8X16_ADD_SAT_S,
  /** Add corresponding lanes of two i8x16 vectors with saturation (unsigned). */
  I8X16_ADD_SAT_U,
  /** Subtract corresponding lanes of two i8x16 vectors with saturation (signed). */
  I8X16_SUB_SAT_S,
  /** Subtract corresponding lanes of two i8x16 vectors with saturation (unsigned). */
  I8X16_SUB_SAT_U,

  // Arithmetic Operations - i16x8
  /** Add corresponding lanes of two i16x8 vectors. */
  I16X8_ADD,
  /** Subtract corresponding lanes of two i16x8 vectors. */
  I16X8_SUB,
  /** Multiply corresponding lanes of two i16x8 vectors. */
  I16X8_MUL,
  /** Negate all lanes of an i16x8 vector. */
  I16X8_NEG,
  /** Add corresponding lanes of two i16x8 vectors with saturation (signed). */
  I16X8_ADD_SAT_S,
  /** Add corresponding lanes of two i16x8 vectors with saturation (unsigned). */
  I16X8_ADD_SAT_U,
  /** Subtract corresponding lanes of two i16x8 vectors with saturation (signed). */
  I16X8_SUB_SAT_S,
  /** Subtract corresponding lanes of two i16x8 vectors with saturation (unsigned). */
  I16X8_SUB_SAT_U,

  // Arithmetic Operations - i32x4
  /** Add corresponding lanes of two i32x4 vectors. */
  I32X4_ADD,
  /** Subtract corresponding lanes of two i32x4 vectors. */
  I32X4_SUB,
  /** Multiply corresponding lanes of two i32x4 vectors. */
  I32X4_MUL,
  /** Negate all lanes of an i32x4 vector. */
  I32X4_NEG,

  // Arithmetic Operations - i64x2
  /** Add corresponding lanes of two i64x2 vectors. */
  I64X2_ADD,
  /** Subtract corresponding lanes of two i64x2 vectors. */
  I64X2_SUB,
  /** Multiply corresponding lanes of two i64x2 vectors. */
  I64X2_MUL,
  /** Negate all lanes of an i64x2 vector. */
  I64X2_NEG,

  // Arithmetic Operations - f32x4
  /** Add corresponding lanes of two f32x4 vectors. */
  F32X4_ADD,
  /** Subtract corresponding lanes of two f32x4 vectors. */
  F32X4_SUB,
  /** Multiply corresponding lanes of two f32x4 vectors. */
  F32X4_MUL,
  /** Divide corresponding lanes of two f32x4 vectors. */
  F32X4_DIV,
  /** Negate all lanes of an f32x4 vector. */
  F32X4_NEG,
  /** Square root of all lanes of an f32x4 vector. */
  F32X4_SQRT,
  /** Absolute value of all lanes of an f32x4 vector. */
  F32X4_ABS,
  /** Minimum of corresponding lanes of two f32x4 vectors. */
  F32X4_MIN,
  /** Maximum of corresponding lanes of two f32x4 vectors. */
  F32X4_MAX,

  // Arithmetic Operations - f64x2
  /** Add corresponding lanes of two f64x2 vectors. */
  F64X2_ADD,
  /** Subtract corresponding lanes of two f64x2 vectors. */
  F64X2_SUB,
  /** Multiply corresponding lanes of two f64x2 vectors. */
  F64X2_MUL,
  /** Divide corresponding lanes of two f64x2 vectors. */
  F64X2_DIV,
  /** Negate all lanes of an f64x2 vector. */
  F64X2_NEG,
  /** Square root of all lanes of an f64x2 vector. */
  F64X2_SQRT,
  /** Absolute value of all lanes of an f64x2 vector. */
  F64X2_ABS,
  /** Minimum of corresponding lanes of two f64x2 vectors. */
  F64X2_MIN,
  /** Maximum of corresponding lanes of two f64x2 vectors. */
  F64X2_MAX,

  // Comparison Operations - i8x16
  /** Compare lanes for equality in i8x16 vectors. */
  I8X16_EQ,
  /** Compare lanes for inequality in i8x16 vectors. */
  I8X16_NE,
  /** Compare lanes for less than (signed) in i8x16 vectors. */
  I8X16_LT_S,
  /** Compare lanes for less than (unsigned) in i8x16 vectors. */
  I8X16_LT_U,
  /** Compare lanes for greater than (signed) in i8x16 vectors. */
  I8X16_GT_S,
  /** Compare lanes for greater than (unsigned) in i8x16 vectors. */
  I8X16_GT_U,
  /** Compare lanes for less than or equal (signed) in i8x16 vectors. */
  I8X16_LE_S,
  /** Compare lanes for less than or equal (unsigned) in i8x16 vectors. */
  I8X16_LE_U,
  /** Compare lanes for greater than or equal (signed) in i8x16 vectors. */
  I8X16_GE_S,
  /** Compare lanes for greater than or equal (unsigned) in i8x16 vectors. */
  I8X16_GE_U,

  // Comparison Operations - i16x8
  /** Compare lanes for equality in i16x8 vectors. */
  I16X8_EQ,
  /** Compare lanes for inequality in i16x8 vectors. */
  I16X8_NE,
  /** Compare lanes for less than (signed) in i16x8 vectors. */
  I16X8_LT_S,
  /** Compare lanes for less than (unsigned) in i16x8 vectors. */
  I16X8_LT_U,
  /** Compare lanes for greater than (signed) in i16x8 vectors. */
  I16X8_GT_S,
  /** Compare lanes for greater than (unsigned) in i16x8 vectors. */
  I16X8_GT_U,
  /** Compare lanes for less than or equal (signed) in i16x8 vectors. */
  I16X8_LE_S,
  /** Compare lanes for less than or equal (unsigned) in i16x8 vectors. */
  I16X8_LE_U,
  /** Compare lanes for greater than or equal (signed) in i16x8 vectors. */
  I16X8_GE_S,
  /** Compare lanes for greater than or equal (unsigned) in i16x8 vectors. */
  I16X8_GE_U,

  // Comparison Operations - i32x4
  /** Compare lanes for equality in i32x4 vectors. */
  I32X4_EQ,
  /** Compare lanes for inequality in i32x4 vectors. */
  I32X4_NE,
  /** Compare lanes for less than (signed) in i32x4 vectors. */
  I32X4_LT_S,
  /** Compare lanes for less than (unsigned) in i32x4 vectors. */
  I32X4_LT_U,
  /** Compare lanes for greater than (signed) in i32x4 vectors. */
  I32X4_GT_S,
  /** Compare lanes for greater than (unsigned) in i32x4 vectors. */
  I32X4_GT_U,
  /** Compare lanes for less than or equal (signed) in i32x4 vectors. */
  I32X4_LE_S,
  /** Compare lanes for less than or equal (unsigned) in i32x4 vectors. */
  I32X4_LE_U,
  /** Compare lanes for greater than or equal (signed) in i32x4 vectors. */
  I32X4_GE_S,
  /** Compare lanes for greater than or equal (unsigned) in i32x4 vectors. */
  I32X4_GE_U,

  // Comparison Operations - f32x4
  /** Compare lanes for equality in f32x4 vectors. */
  F32X4_EQ,
  /** Compare lanes for inequality in f32x4 vectors. */
  F32X4_NE,
  /** Compare lanes for less than in f32x4 vectors. */
  F32X4_LT,
  /** Compare lanes for greater than in f32x4 vectors. */
  F32X4_GT,
  /** Compare lanes for less than or equal in f32x4 vectors. */
  F32X4_LE,
  /** Compare lanes for greater than or equal in f32x4 vectors. */
  F32X4_GE,

  // Comparison Operations - f64x2
  /** Compare lanes for equality in f64x2 vectors. */
  F64X2_EQ,
  /** Compare lanes for inequality in f64x2 vectors. */
  F64X2_NE,
  /** Compare lanes for less than in f64x2 vectors. */
  F64X2_LT,
  /** Compare lanes for greater than in f64x2 vectors. */
  F64X2_GT,
  /** Compare lanes for less than or equal in f64x2 vectors. */
  F64X2_LE,
  /** Compare lanes for greater than or equal in f64x2 vectors. */
  F64X2_GE,

  // Bitwise Operations
  /** Bitwise AND of two 128-bit vectors. */
  V128_AND,
  /** Bitwise OR of two 128-bit vectors. */
  V128_OR,
  /** Bitwise XOR of two 128-bit vectors. */
  V128_XOR,
  /** Bitwise NOT of a 128-bit vector. */
  V128_NOT,
  /** Bitwise AND NOT of two 128-bit vectors. */
  V128_ANDNOT,

  // Shuffle and Select Operations
  /** Shuffle bytes of two i8x16 vectors according to indices. */
  I8X16_SHUFFLE,
  /** Select bits from two 128-bit vectors based on a mask. */
  V128_BITSELECT,

  // Conversion Operations
  /** Convert signed i32x4 to f32x4. */
  F32X4_CONVERT_I32X4_S,
  /** Convert unsigned i32x4 to f32x4. */
  F32X4_CONVERT_I32X4_U,
  /** Convert f32x4 to signed i32x4 with saturation. */
  I32X4_TRUNC_SAT_F32X4_S,
  /** Convert f32x4 to unsigned i32x4 with saturation. */
  I32X4_TRUNC_SAT_F32X4_U,

  // Splat Operations
  /** Create an i8x16 vector with all lanes set to the same value. */
  I8X16_SPLAT,
  /** Create an i16x8 vector with all lanes set to the same value. */
  I16X8_SPLAT,
  /** Create an i32x4 vector with all lanes set to the same value. */
  I32X4_SPLAT,
  /** Create an i64x2 vector with all lanes set to the same value. */
  I64X2_SPLAT,
  /** Create an f32x4 vector with all lanes set to the same value. */
  F32X4_SPLAT,
  /** Create an f64x2 vector with all lanes set to the same value. */
  F64X2_SPLAT,

  // Shift Operations
  /** Left shift lanes of an i8x16 vector. */
  I8X16_SHL,
  /** Right shift lanes of an i8x16 vector (signed). */
  I8X16_SHR_S,
  /** Right shift lanes of an i8x16 vector (unsigned). */
  I8X16_SHR_U,
  /** Left shift lanes of an i16x8 vector. */
  I16X8_SHL,
  /** Right shift lanes of an i16x8 vector (signed). */
  I16X8_SHR_S,
  /** Right shift lanes of an i16x8 vector (unsigned). */
  I16X8_SHR_U,
  /** Left shift lanes of an i32x4 vector. */
  I32X4_SHL,
  /** Right shift lanes of an i32x4 vector (signed). */
  I32X4_SHR_S,
  /** Right shift lanes of an i32x4 vector (unsigned). */
  I32X4_SHR_U,
  /** Left shift lanes of an i64x2 vector. */
  I64X2_SHL,
  /** Right shift lanes of an i64x2 vector (signed). */
  I64X2_SHR_S,
  /** Right shift lanes of an i64x2 vector (unsigned). */
  I64X2_SHR_U
}
