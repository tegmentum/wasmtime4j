/*
 * Copyright 2025 Tegmentum AI
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.tegmentum.wasmtime4j.simd;

import ai.tegmentum.wasmtime4j.WasmMemory;
import ai.tegmentum.wasmtime4j.exception.WasmException;

/**
 * SIMD (Single Instruction, Multiple Data) operations for WebAssembly.
 *
 * <p>Provides vectorized operations on 128-bit SIMD vectors following the WebAssembly SIMD proposal
 * specification.
 *
 * <p>All operations are immutable - they return new {@link SimdVector} instances rather than
 * modifying existing vectors.
 *
 * @since 1.0.0
 */
public interface SimdOperations {

  // ===== Arithmetic Operations =====

  /**
   * Adds two SIMD vectors lane-wise.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with lane-wise sum
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector add(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Subtracts two SIMD vectors lane-wise.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with lane-wise difference
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector subtract(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Multiplies two SIMD vectors lane-wise.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with lane-wise product
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector multiply(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Divides two SIMD vectors lane-wise.
   *
   * @param a first vector (dividend)
   * @param b second vector (divisor)
   * @return new vector with lane-wise quotient
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails or division by zero
   */
  SimdVector divide(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Adds two SIMD vectors with saturating arithmetic.
   *
   * <p>Values are clamped to the min/max representable values instead of wrapping.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with saturated lane-wise sum
   * @throws IllegalArgumentException if vectors have different lane types or are not integer types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector addSaturated(SimdVector a, SimdVector b) throws WasmException;

  // ===== Bitwise Operations =====

  /**
   * Performs bitwise AND of two SIMD vectors.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with bitwise AND result
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector and(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Performs bitwise OR of two SIMD vectors.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with bitwise OR result
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector or(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Performs bitwise XOR of two SIMD vectors.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with bitwise XOR result
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector xor(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Performs bitwise NOT of a SIMD vector.
   *
   * @param a the vector to negate
   * @return new vector with bitwise NOT result
   * @throws WasmException if SIMD operation fails
   */
  SimdVector not(SimdVector a) throws WasmException;

  // ===== Comparison Operations =====

  /**
   * Compares two SIMD vectors for equality lane-wise.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with all-ones in lanes where equal, all-zeros otherwise
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector equals(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Compares two SIMD vectors for less-than lane-wise.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with all-ones in lanes where a &lt; b, all-zeros otherwise
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector lessThan(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Compares two SIMD vectors for greater-than lane-wise.
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with all-ones in lanes where a &gt; b, all-zeros otherwise
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector greaterThan(SimdVector a, SimdVector b) throws WasmException;

  // ===== Memory Operations =====

  /**
   * Loads a SIMD vector from WebAssembly memory.
   *
   * @param memory the memory instance
   * @param offset byte offset in memory
   * @param lane the lane type to load
   * @return new vector loaded from memory
   * @throws WasmException if memory access fails or offset is out of bounds
   */
  SimdVector load(WasmMemory memory, int offset, SimdLane lane) throws WasmException;

  /**
   * Loads a SIMD vector from aligned WebAssembly memory.
   *
   * <p>The offset must be aligned to 16 bytes for optimal performance.
   *
   * @param memory the memory instance
   * @param offset byte offset in memory (should be 16-byte aligned)
   * @param lane the lane type to load
   * @return new vector loaded from memory
   * @throws WasmException if memory access fails or offset is out of bounds
   */
  SimdVector loadAligned(WasmMemory memory, int offset, SimdLane lane) throws WasmException;

  /**
   * Stores a SIMD vector to WebAssembly memory.
   *
   * @param memory the memory instance
   * @param offset byte offset in memory
   * @param vector the vector to store
   * @throws WasmException if memory access fails or offset is out of bounds
   */
  void store(WasmMemory memory, int offset, SimdVector vector) throws WasmException;

  /**
   * Stores a SIMD vector to aligned WebAssembly memory.
   *
   * <p>The offset must be aligned to 16 bytes for optimal performance.
   *
   * @param memory the memory instance
   * @param offset byte offset in memory (should be 16-byte aligned)
   * @param vector the vector to store
   * @throws WasmException if memory access fails or offset is out of bounds
   */
  void storeAligned(WasmMemory memory, int offset, SimdVector vector) throws WasmException;

  // ===== Lane Manipulation =====

  /**
   * Extracts a 32-bit integer lane from a vector.
   *
   * @param vector the source vector
   * @param laneIndex the lane index (0-based)
   * @return the lane value as a 32-bit integer
   * @throws IllegalArgumentException if lane index is out of bounds or vector is not integer type
   * @throws WasmException if SIMD operation fails
   */
  int extractLaneI32(SimdVector vector, int laneIndex) throws WasmException;

  /**
   * Replaces a 32-bit integer lane in a vector.
   *
   * @param vector the source vector
   * @param laneIndex the lane index (0-based)
   * @param value the new lane value
   * @return new vector with replaced lane
   * @throws IllegalArgumentException if lane index is out of bounds or vector is not integer type
   * @throws WasmException if SIMD operation fails
   */
  SimdVector replaceLaneI32(SimdVector vector, int laneIndex, int value) throws WasmException;

  // ===== Conversion Operations =====

  /**
   * Converts 32-bit integer lanes to 32-bit float lanes.
   *
   * @param vector the source vector (must be I32X4)
   * @return new F32X4 vector with converted values
   * @throws IllegalArgumentException if vector is not I32X4
   * @throws WasmException if SIMD operation fails
   */
  SimdVector convertI32ToF32(SimdVector vector) throws WasmException;

  /**
   * Converts 32-bit float lanes to 32-bit integer lanes.
   *
   * @param vector the source vector (must be F32X4)
   * @return new I32X4 vector with converted values
   * @throws IllegalArgumentException if vector is not F32X4
   * @throws WasmException if SIMD operation fails
   */
  SimdVector convertF32ToI32(SimdVector vector) throws WasmException;

  // ===== Advanced Operations =====

  /**
   * Shuffles lanes from two vectors according to an index array.
   *
   * @param a first vector
   * @param b second vector
   * @param indices 16-element array specifying which bytes to select
   * @return new vector with shuffled lanes
   * @throws IllegalArgumentException if vectors have different lane types or indices length != 16
   * @throws WasmException if SIMD operation fails
   */
  SimdVector shuffle(SimdVector a, SimdVector b, int[] indices) throws WasmException;

  /**
   * Fused multiply-add operation: (a * b) + c.
   *
   * @param a first vector
   * @param b second vector
   * @param c third vector
   * @return new vector with FMA result
   * @throws IllegalArgumentException if vectors have different lane types or are not float types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector fma(SimdVector a, SimdVector b, SimdVector c) throws WasmException;

  /**
   * Fused multiply-subtract operation: (a * b) - c.
   *
   * @param a first vector
   * @param b second vector
   * @param c third vector
   * @return new vector with FMS result
   * @throws IllegalArgumentException if vectors have different lane types or are not float types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector fms(SimdVector a, SimdVector b, SimdVector c) throws WasmException;

  /**
   * Computes reciprocal of each lane.
   *
   * @param vector the source vector (must be float type)
   * @return new vector with reciprocal values
   * @throws IllegalArgumentException if vector is not float type
   * @throws WasmException if SIMD operation fails
   */
  SimdVector reciprocal(SimdVector vector) throws WasmException;

  /**
   * Computes square root of each lane.
   *
   * @param vector the source vector (must be float type)
   * @return new vector with square root values
   * @throws IllegalArgumentException if vector is not float type
   * @throws WasmException if SIMD operation fails
   */
  SimdVector sqrt(SimdVector vector) throws WasmException;

  /**
   * Computes reciprocal square root of each lane.
   *
   * @param vector the source vector (must be float type)
   * @return new vector with reciprocal square root values
   * @throws IllegalArgumentException if vector is not float type
   * @throws WasmException if SIMD operation fails
   */
  SimdVector rsqrt(SimdVector vector) throws WasmException;

  /**
   * Counts set bits in each lane.
   *
   * @param vector the source vector (must be integer type)
   * @return new vector with popcount values
   * @throws IllegalArgumentException if vector is not integer type
   * @throws WasmException if SIMD operation fails
   */
  SimdVector popcount(SimdVector vector) throws WasmException;

  /**
   * Variable-length left shift of lanes.
   *
   * @param a vector to shift
   * @param b shift amounts per lane
   * @return new vector with shifted values
   * @throws IllegalArgumentException if vectors have different lane types or are not integer types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector shlVariable(SimdVector a, SimdVector b) throws WasmException;

  /**
   * Variable-length right shift of lanes.
   *
   * @param a vector to shift
   * @param b shift amounts per lane
   * @return new vector with shifted values
   * @throws IllegalArgumentException if vectors have different lane types or are not integer types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector shrVariable(SimdVector a, SimdVector b) throws WasmException;

  // ===== Reduction Operations =====

  /**
   * Computes horizontal sum of all lanes.
   *
   * @param vector the source vector (must be float type)
   * @return sum of all lanes
   * @throws IllegalArgumentException if vector is not float type
   * @throws WasmException if SIMD operation fails
   */
  float horizontalSum(SimdVector vector) throws WasmException;

  /**
   * Computes horizontal minimum of all lanes.
   *
   * @param vector the source vector (must be float type)
   * @return minimum of all lanes
   * @throws IllegalArgumentException if vector is not float type
   * @throws WasmException if SIMD operation fails
   */
  float horizontalMin(SimdVector vector) throws WasmException;

  /**
   * Computes horizontal maximum of all lanes.
   *
   * @param vector the source vector (must be float type)
   * @return maximum of all lanes
   * @throws IllegalArgumentException if vector is not float type
   * @throws WasmException if SIMD operation fails
   */
  float horizontalMax(SimdVector vector) throws WasmException;

  // ===== Select/Blend Operations =====

  /**
   * Selects lanes from two vectors based on a mask.
   *
   * @param mask selection mask (non-zero = select from a, zero = select from b)
   * @param a first source vector
   * @param b second source vector
   * @return new vector with selected lanes
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector select(SimdVector mask, SimdVector a, SimdVector b) throws WasmException;

  /**
   * Blends two vectors based on a bit mask.
   *
   * @param a first source vector
   * @param b second source vector
   * @param mask bit mask (1 = select from a, 0 = select from b)
   * @return new vector with blended lanes
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector blend(SimdVector a, SimdVector b, int mask) throws WasmException;

  // ===== Relaxed SIMD Operations =====

  /**
   * Relaxed SIMD addition with potential performance optimizations.
   *
   * <p>May have slightly different rounding behavior than strict add().
   *
   * @param a first vector
   * @param b second vector
   * @return new vector with relaxed lane-wise sum
   * @throws IllegalArgumentException if vectors have different lane types
   * @throws WasmException if SIMD operation fails
   */
  SimdVector relaxedAdd(SimdVector a, SimdVector b) throws WasmException;

  // ===== Capability Queries =====

  /**
   * Checks if SIMD operations are supported by this runtime.
   *
   * @return true if SIMD is supported, false otherwise
   */
  boolean isSimdSupported();

  /**
   * Gets a string describing the SIMD capabilities of this runtime.
   *
   * @return capabilities description
   */
  String getSimdCapabilities();
}
