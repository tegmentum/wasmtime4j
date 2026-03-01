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
package ai.tegmentum.wasmtime4j.wit;

import ai.tegmentum.wasmtime4j.exception.ValidationException;
import java.util.Objects;

/**
 * Represents a WIT character value (Unicode scalar value).
 *
 * <p>WIT char represents a valid Unicode scalar value (U+0000 to U+D7FF and U+E000 to U+10FFFF).
 * This excludes surrogate code points (U+D800 to U+DFFF). Values are immutable and thread-safe.
 *
 * @since 1.0.0
 */
public final class WitChar extends WitPrimitiveValue {

  private static final WitType CHAR_TYPE = WitType.createChar();

  private static final int MIN_CODEPOINT = 0x0000;
  private static final int MAX_CODEPOINT = 0x10FFFF;
  private static final int SURROGATE_MIN = 0xD800;
  private static final int SURROGATE_MAX = 0xDFFF;

  private final int codepoint;

  /**
   * Creates a new WIT char value.
   *
   * @param codepoint the Unicode codepoint
   * @throws ValidationException if codepoint is not a valid Unicode scalar value
   */
  private WitChar(final int codepoint) throws ValidationException {
    super(CHAR_TYPE);
    this.codepoint = codepoint;
    validate();
  }

  /**
   * Creates a WIT char value from a Unicode codepoint.
   *
   * @param codepoint the Unicode codepoint
   * @return a WIT char value
   * @throws ValidationException if codepoint is not a valid Unicode scalar value
   */
  public static WitChar of(final int codepoint) throws ValidationException {
    return new WitChar(codepoint);
  }

  /**
   * Gets the Unicode codepoint value.
   *
   * @return the Unicode codepoint
   */
  public int getCodepoint() {
    return codepoint;
  }

  @Override
  public Character toJava() {
    return Character.valueOf((char) codepoint);
  }

  @Override
  protected void validate() throws ValidationException {
    if (codepoint < MIN_CODEPOINT || codepoint > MAX_CODEPOINT) {
      throw new ValidationException(
          String.format("Invalid codepoint 0x%X: not a valid Unicode scalar value", codepoint));
    }
    if (codepoint >= SURROGATE_MIN && codepoint <= SURROGATE_MAX) {
      throw new ValidationException(
          String.format("Invalid codepoint 0x%X: not a valid Unicode scalar value", codepoint));
    }
  }

  @Override
  public String toString() {
    return String.format("WitChar{codepoint=U+%04X}", codepoint);
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof WitChar)) {
      return false;
    }
    final WitChar other = (WitChar) obj;
    return codepoint == other.codepoint;
  }

  @Override
  public int hashCode() {
    return Objects.hash(getType(), codepoint);
  }
}
