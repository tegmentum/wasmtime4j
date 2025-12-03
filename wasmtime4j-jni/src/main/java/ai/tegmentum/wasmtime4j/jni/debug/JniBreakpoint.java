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

package ai.tegmentum.wasmtime4j.jni.debug;

import ai.tegmentum.wasmtime4j.debug.Breakpoint;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * JNI implementation of a WebAssembly breakpoint.
 *
 * <p>This class represents a breakpoint that can be set at a specific location in WebAssembly code.
 * Breakpoints can be conditional and track how many times they have been hit.
 *
 * @since 1.0.0
 */
public final class JniBreakpoint implements Breakpoint {

  private final String breakpointId;
  private final String functionName;
  private final int lineNumber;
  private final int columnNumber;
  private final long instructionOffset;
  private final AtomicBoolean enabled;
  private final AtomicInteger hitCount;
  private volatile String condition;

  /**
   * Creates a new JNI breakpoint.
   *
   * @param breakpointId the unique breakpoint identifier
   * @param functionName the function name where the breakpoint is set
   * @param lineNumber the line number in source code
   * @param columnNumber the column number in source code
   * @param instructionOffset the instruction offset in WebAssembly bytecode
   */
  public JniBreakpoint(
      final String breakpointId,
      final String functionName,
      final int lineNumber,
      final int columnNumber,
      final long instructionOffset) {
    this.breakpointId = Objects.requireNonNull(breakpointId, "breakpointId cannot be null");
    this.functionName = functionName;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
    this.instructionOffset = instructionOffset;
    this.enabled = new AtomicBoolean(true);
    this.hitCount = new AtomicInteger(0);
    this.condition = null;
  }

  /**
   * Creates a new JNI breakpoint from native data.
   *
   * @param nativeId the native breakpoint ID
   * @param functionName the function name
   * @param lineNumber the line number
   * @param columnNumber the column number
   * @param instructionOffset the instruction offset
   * @return a new JniBreakpoint instance
   */
  public static JniBreakpoint fromNative(
      final long nativeId,
      final String functionName,
      final int lineNumber,
      final int columnNumber,
      final long instructionOffset) {
    return new JniBreakpoint(
        "bp-" + nativeId, functionName, lineNumber, columnNumber, instructionOffset);
  }

  @Override
  public String getBreakpointId() {
    return breakpointId;
  }

  @Override
  public String getFunctionName() {
    return functionName;
  }

  @Override
  public int getLineNumber() {
    return lineNumber;
  }

  @Override
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * Gets the instruction offset in WebAssembly bytecode.
   *
   * @return the instruction offset
   */
  public long getInstructionOffset() {
    return instructionOffset;
  }

  @Override
  public boolean isEnabled() {
    return enabled.get();
  }

  @Override
  public void setEnabled(final boolean enabledState) {
    enabled.set(enabledState);
  }

  @Override
  public String getCondition() {
    return condition;
  }

  @Override
  public void setCondition(final String conditionExpr) {
    this.condition = conditionExpr;
  }

  @Override
  public int getHitCount() {
    return hitCount.get();
  }

  @Override
  public void resetHitCount() {
    hitCount.set(0);
  }

  /**
   * Increments the hit count and returns the new value.
   *
   * @return the new hit count after incrementing
   */
  public int incrementHitCount() {
    return hitCount.incrementAndGet();
  }

  @Override
  public String toString() {
    return "JniBreakpoint{"
        + "id='"
        + breakpointId
        + '\''
        + ", function='"
        + functionName
        + '\''
        + ", line="
        + lineNumber
        + ", column="
        + columnNumber
        + ", offset="
        + instructionOffset
        + ", enabled="
        + enabled.get()
        + ", hits="
        + hitCount.get()
        + (condition != null ? ", condition='" + condition + '\'' : "")
        + '}';
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JniBreakpoint)) {
      return false;
    }
    final JniBreakpoint other = (JniBreakpoint) obj;
    return breakpointId.equals(other.breakpointId);
  }

  @Override
  public int hashCode() {
    return breakpointId.hashCode();
  }
}
