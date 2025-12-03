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

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

/**
 * Represents a stack frame in WebAssembly execution.
 *
 * <p>A stack frame contains information about a function call during execution, including the
 * function name, instruction offset, source location, and local variables.
 *
 * @since 1.0.0
 */
public final class JniStackFrame {

  private final int functionIndex;
  private final String functionName;
  private final long instructionOffset;
  private final int lineNumber;
  private final int columnNumber;
  private final String sourceFile;
  private final List<JniVariable> variables;

  /**
   * Creates a new stack frame.
   *
   * @param functionIndex the function index in the module
   * @param functionName the function name (may be null for unnamed functions)
   * @param instructionOffset the instruction offset in WebAssembly bytecode
   * @param lineNumber the source line number (0 if unknown)
   * @param columnNumber the source column number (0 if unknown)
   * @param sourceFile the source file path (may be null)
   * @param variables the local variables in this frame
   */
  public JniStackFrame(
      final int functionIndex,
      final String functionName,
      final long instructionOffset,
      final int lineNumber,
      final int columnNumber,
      final String sourceFile,
      final List<JniVariable> variables) {
    this.functionIndex = functionIndex;
    this.functionName = functionName;
    this.instructionOffset = instructionOffset;
    this.lineNumber = lineNumber;
    this.columnNumber = columnNumber;
    this.sourceFile = sourceFile;
    this.variables = variables != null ? new ArrayList<>(variables) : new ArrayList<>();
  }

  /**
   * Creates a stack frame from native data.
   *
   * @param functionIndex the function index
   * @param functionName the function name
   * @param instructionOffset the instruction offset
   * @param lineNumber the line number
   * @param columnNumber the column number
   * @param sourceFile the source file
   * @return a new stack frame
   */
  public static JniStackFrame fromNative(
      final int functionIndex,
      final String functionName,
      final long instructionOffset,
      final int lineNumber,
      final int columnNumber,
      final String sourceFile) {
    return new JniStackFrame(
        functionIndex,
        functionName,
        instructionOffset,
        lineNumber,
        columnNumber,
        sourceFile,
        Collections.emptyList());
  }

  /**
   * Gets the function index in the module.
   *
   * @return the function index
   */
  public int getFunctionIndex() {
    return functionIndex;
  }

  /**
   * Gets the function name.
   *
   * @return the function name, or null if unnamed
   */
  public String getFunctionName() {
    return functionName;
  }

  /**
   * Gets the instruction offset in WebAssembly bytecode.
   *
   * @return the instruction offset
   */
  public long getInstructionOffset() {
    return instructionOffset;
  }

  /**
   * Gets the source line number.
   *
   * @return the line number, or 0 if unknown
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Gets the source column number.
   *
   * @return the column number, or 0 if unknown
   */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * Gets the source file path.
   *
   * @return the source file path, or null if unknown
   */
  public String getSourceFile() {
    return sourceFile;
  }

  /**
   * Gets the variables in this frame.
   *
   * @return an unmodifiable list of variables
   */
  public List<JniVariable> getVariables() {
    return Collections.unmodifiableList(variables);
  }

  /**
   * Creates a builder for constructing stack frames with additional data.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder("JniStackFrame{");
    sb.append("functionIndex=").append(functionIndex);
    if (functionName != null) {
      sb.append(", functionName='").append(functionName).append('\'');
    }
    sb.append(", offset=").append(instructionOffset);
    if (sourceFile != null) {
      sb.append(", source='").append(sourceFile).append(':').append(lineNumber);
      if (columnNumber > 0) {
        sb.append(':').append(columnNumber);
      }
      sb.append('\'');
    }
    if (!variables.isEmpty()) {
      sb.append(", vars=").append(variables.size());
    }
    sb.append('}');
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof JniStackFrame)) {
      return false;
    }
    final JniStackFrame other = (JniStackFrame) obj;
    return functionIndex == other.functionIndex
        && instructionOffset == other.instructionOffset
        && lineNumber == other.lineNumber
        && columnNumber == other.columnNumber
        && Objects.equals(functionName, other.functionName)
        && Objects.equals(sourceFile, other.sourceFile);
  }

  @Override
  public int hashCode() {
    return Objects.hash(functionIndex, functionName, instructionOffset, lineNumber, columnNumber);
  }

  /** Builder for constructing stack frames. */
  public static final class Builder {
    private int functionIndex;
    private String functionName;
    private long instructionOffset;
    private int lineNumber;
    private int columnNumber;
    private String sourceFile;
    private List<JniVariable> variables = new ArrayList<>();

    private Builder() {}

    /**
     * Sets the function index.
     *
     * @param idx the function index
     * @return this builder
     */
    public Builder functionIndex(final int idx) {
      this.functionIndex = idx;
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
     * Sets the source file.
     *
     * @param file the source file path
     * @return this builder
     */
    public Builder sourceFile(final String file) {
      this.sourceFile = file;
      return this;
    }

    /**
     * Adds a variable to the frame.
     *
     * @param variable the variable to add
     * @return this builder
     */
    public Builder addVariable(final JniVariable variable) {
      this.variables.add(variable);
      return this;
    }

    /**
     * Sets the variables list.
     *
     * @param vars the variables
     * @return this builder
     */
    public Builder variables(final List<JniVariable> vars) {
      this.variables = vars != null ? new ArrayList<>(vars) : new ArrayList<>();
      return this;
    }

    /**
     * Builds the stack frame.
     *
     * @return the constructed stack frame
     */
    public JniStackFrame build() {
      return new JniStackFrame(
          functionIndex,
          functionName,
          instructionOffset,
          lineNumber,
          columnNumber,
          sourceFile,
          variables);
    }
  }
}
