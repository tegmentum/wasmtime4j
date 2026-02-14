/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.wit;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * Base class for all WebAssembly Interface Type (WIT) values.
 *
 * <p>WIT values represent typed data that can be marshalled between Java and WebAssembly
 * components. This abstract base class provides common functionality for all WIT value types
 * including primitives, composites, and reference types.
 *
 * <p>All WIT values are immutable and thread-safe once constructed. Implementations must ensure
 * defensive copying of mutable parameters and validation of all inputs.
 *
 * @since 1.0.0
 */
public abstract class WitValue {

  private final WitType type;

  /**
   * Creates a new WIT value with the specified type.
   *
   * @param type the WIT type of this value (must not be null)
   * @throws IllegalArgumentException if type is null
   */
  @SuppressFBWarnings(
      value = "CT_CONSTRUCTOR_THROW",
      justification =
          "Constructor validates inputs before object construction; no subclass can bypass this")
  protected WitValue(final WitType type) {
    if (type == null) {
      throw new IllegalArgumentException("WIT type cannot be null");
    }
    this.type = type;
  }

  /**
   * Gets the WIT type of this value.
   *
   * @return the WIT type (never null)
   */
  public final WitType getType() {
    return type;
  }

  /**
   * Converts this WIT value to its Java representation.
   *
   * <p>The returned Java object type depends on the WIT type:
   *
   * <ul>
   *   <li>bool → Boolean
   *   <li>s8, s16, s32, u8, u16 → Integer
   *   <li>s64, u32, u64 → Long
   *   <li>float32, float64 → Double
   *   <li>char → Integer (Unicode codepoint)
   *   <li>string → String
   *   <li>record → {@code Map<String, Object>}
   *   <li>variant → custom variant wrapper
   *   <li>list → {@code List<Object>}
   *   <li>tuple → {@code List<Object>}
   * </ul>
   *
   * @return the Java representation of this WIT value (never null)
   */
  public abstract Object toJava();

  /**
   * Returns a string representation of this WIT value.
   *
   * <p>The format includes the type and value information suitable for debugging and logging.
   *
   * @return a string representation of this value
   */
  @Override
  public abstract String toString();

  /**
   * Indicates whether some other object is "equal to" this one.
   *
   * <p>Two WIT values are equal if they have the same type and the same value. Implementations must
   * ensure proper value comparison semantics for their specific type.
   *
   * @param obj the reference object with which to compare
   * @return true if this object is the same as the obj argument; false otherwise
   */
  @Override
  public abstract boolean equals(Object obj);

  /**
   * Returns a hash code value for this WIT value.
   *
   * <p>The hash code must be consistent with equals() and should incorporate both the type and
   * value information.
   *
   * @return a hash code value for this object
   */
  @Override
  public abstract int hashCode();

  /**
   * Validates that this WIT value conforms to its type constraints.
   *
   * <p>This method performs deep validation of the value, checking:
   *
   * <ul>
   *   <li>Type consistency
   *   <li>Range constraints
   *   <li>Format requirements
   *   <li>Structural constraints (for composite types)
   * </ul>
   *
   * <p>Implementations should call this method during construction to ensure values are always
   * valid.
   *
   * @throws ai.tegmentum.wasmtime4j.exception.ValidationException if validation fails
   */
  protected abstract void validate() throws ai.tegmentum.wasmtime4j.exception.ValidationException;

  /**
   * Checks if this value is compatible with the specified WIT type.
   *
   * <p>Compatibility means the value can be safely converted or assigned to the target type. This
   * may include subtyping relationships and safe numeric conversions.
   *
   * @param targetType the target WIT type to check compatibility with
   * @return true if this value is compatible with the target type
   */
  public boolean isCompatibleWith(final WitType targetType) {
    if (targetType == null) {
      return false;
    }
    // Default implementation: exact type match
    // Subclasses may override for more sophisticated compatibility checks
    return this.type.equals(targetType);
  }
}
