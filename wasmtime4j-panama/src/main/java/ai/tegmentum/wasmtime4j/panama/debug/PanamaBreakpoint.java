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

package ai.tegmentum.wasmtime4j.panama.debug;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Panama implementation of a WebAssembly breakpoint.
 *
 * <p>This class represents a breakpoint that can be set in WebAssembly code at a specific location.
 *
 * @since 1.0.0
 */
public final class PanamaBreakpoint implements Breakpoint {

  private final String breakpointId;
  private final String functionName;
  private final int lineNumber;
  private final int columnNumber;
  private final long instructionOffset;
  private final AtomicBoolean enabled;
  private final AtomicInteger hitCount;
  private volatile String condition;

  private PanamaBreakpoint(final Builder builder) {
    this.breakpointId =
        builder.breakpointId != null ? builder.breakpointId : "bp-" + UUID.randomUUID().toString();
    this.functionName = builder.functionName;
    this.lineNumber = builder.lineNumber;
    this.columnNumber = builder.columnNumber;
    this.instructionOffset = builder.instructionOffset;
    this.enabled = new AtomicBoolean(builder.enabled);
    this.hitCount = new AtomicInteger(0);
    this.condition = builder.condition;
  }

  /**
   * Creates a builder for a breakpoint.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Creates a breakpoint at a function entry.
   *
   * @param functionName the function name
   * @return a new breakpoint
   */
  public static PanamaBreakpoint atFunction(final String functionName) {
    return builder().functionName(functionName).build();
  }

  /**
   * Creates a breakpoint at a specific line.
   *
   * @param lineNumber the line number
   * @return a new breakpoint
   */
  public static PanamaBreakpoint atLine(final int lineNumber) {
    return builder().lineNumber(lineNumber).build();
  }

  /**
   * Creates a breakpoint at a specific instruction offset.
   *
   * @param offset the instruction offset
   * @return a new breakpoint
   */
  public static PanamaBreakpoint atOffset(final long offset) {
    return builder().instructionOffset(offset).build();
  }

  @Override
  public String getBreakpointId() {
    return breakpointId;
  }

  @Override
  public boolean isEnabled() {
    return enabled.get();
  }

  @Override
  public void setEnabled(final boolean enabled) {
    this.enabled.set(enabled);
  }

  @Override
  public int getHitCount() {
    return hitCount.get();
  }

  /**
   * Gets the function name where the breakpoint is set.
   *
   * @return the function name, may be null
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the line number of the breakpoint.
   *
   * @return the line number, or -1 if not set
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Gets the column number of the breakpoint.
   *
   * @return the column number, or -1 if not set
   */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * Gets the instruction offset of the breakpoint.
   *
   * @return the instruction offset, or -1 if not set
   */
  public long getInstructionOffset() {
    return instructionOffset;
  }

  /**
   * Gets the condition expression for this breakpoint.
   *
   * @return the condition, or null if unconditional
   */
  public String getCondition() {
    return condition;
  }

  /**
   * Increments the hit count. Called when the breakpoint is hit.
   *
   * @return the new hit count
   */
  public int incrementHitCount() {
    return hitCount.incrementAndGet();
  }

  /** Resets the hit count to zero. */
  public void resetHitCount() {
    hitCount.set(0);
  }

  @Override
  public void setCondition(final String condition) {
    this.condition = condition;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("PanamaBreakpoint{");
    sb.append("id='").append(breakpointId).append('\'');
    if (functionName != null) {
      sb.append(", function='").append(functionName).append('\'');
    }
    if (lineNumber >= 0) {
      sb.append(", line=").append(lineNumber);
    }
    if (columnNumber >= 0) {
      sb.append(", column=").append(columnNumber);
    }
    if (instructionOffset >= 0) {
      sb.append(", offset=").append(instructionOffset);
    }
    sb.append(", enabled=").append(enabled.get());
    sb.append(", hits=").append(hitCount.get());
    if (condition != null) {
      sb.append(", condition='").append(condition).append('\'');
    }
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PanamaBreakpoint)) {
      return false;
    }
    final PanamaBreakpoint other = (PanamaBreakpoint) obj;
    return Objects.equals(breakpointId, other.breakpointId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(breakpointId);
  }

  /** Builder for creating breakpoints. */
  public static final class Builder {
    private String breakpointId;
    private String functionName;
    private int lineNumber = -1;
    private int columnNumber = -1;
    private long instructionOffset = -1;
    private boolean enabled = true;
    private String condition;

    private Builder() {}

    /**
     * Sets the breakpoint ID.
     *
     * @param id the breakpoint ID
     * @return this builder
     */
    public Builder breakpointId(final String id) {
      this.breakpointId = id;
      return this;
    }

    /**
     * Sets the function name.
     *
     * @param name the function name
     * @return this builder
     */
    public Builder functionName(final String name) {
      this.functionName = name;
      return this;
    }

    /**
     * Sets the line number.
     *
     * @param line the line number
     * @return this builder
     */
    public Builder lineNumber(final int line) {
      this.lineNumber = line;
      return this;
    }

    /**
     * Sets the column number.
     *
     * @param column the column number
     * @return this builder
     */
    public Builder columnNumber(final int column) {
      this.columnNumber = column;
      return this;
    }

    /**
     * Sets the instruction offset.
     *
     * @param offset the instruction offset
     * @return this builder
     */
    public Builder instructionOffset(final long offset) {
      this.instructionOffset = offset;
      return this;
    }

    /**
     * Sets whether the breakpoint is enabled.
     *
     * @param enabled true if enabled
     * @return this builder
     */
    public Builder enabled(final boolean enabled) {
      this.enabled = enabled;
      return this;
    }

    /**
     * Sets the condition expression.
     *
     * @param condition the condition
     * @return this builder
     */
    public Builder condition(final String condition) {
      this.condition = condition;
      return this;
    }

    /**
     * Builds the breakpoint.
     *
     * @return the built breakpoint
     */
    public PanamaBreakpoint build() {
      return new PanamaBreakpoint(this);
    }
  }
}
