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

import java.util.Objects;

/**
 * Represents a stack frame in WebAssembly execution.
 *
 * <p>This class provides information about a single frame in the call stack during debugging.
 *
 * @since 1.0.0
 */
public final class PanamaStackFrame {

  private final int frameIndex;
  private final String functionName;
  private final int functionIndex;
  private final int moduleIndex;
  private final String moduleName;
  private final long instructionOffset;
  private final int lineNumber;
  private final int columnNumber;
  private final String sourceFile;

  private PanamaStackFrame(final Builder builder) {
    this.frameIndex = builder.frameIndex;
    this.functionName = builder.functionName;
    this.functionIndex = builder.functionIndex;
    this.moduleIndex = builder.moduleIndex;
    this.moduleName = builder.moduleName;
    this.instructionOffset = builder.instructionOffset;
    this.lineNumber = builder.lineNumber;
    this.columnNumber = builder.columnNumber;
    this.sourceFile = builder.sourceFile;
  }

  /**
   * Creates a builder for a stack frame.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  /**
   * Gets the frame index (0 = top of stack).
   *
   * @return the frame index
   */
  public int getFrameIndex() {
    return frameIndex;
  }

  /**
   * Gets the function name.
   *
   * @return the function name, or null if not available
   */
  public String getFunctionName() {
    return functionName;
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
   * Gets the module index.
   *
   * @return the module index
   */
  public int getModuleIndex() {
    return moduleIndex;
  }

  /**
   * Gets the module name.
   *
   * @return the module name, or null if not available
   */
  public String getModuleName() {
    return moduleName;
  }

  /**
   * Gets the instruction offset within the function.
   *
   * @return the instruction offset
   */
  public long getInstructionOffset() {
    return instructionOffset;
  }

  /**
   * Gets the source line number.
   *
   * @return the line number, or -1 if not available
   */
  public int getLineNumber() {
    return lineNumber;
  }

  /**
   * Gets the source column number.
   *
   * @return the column number, or -1 if not available
   */
  public int getColumnNumber() {
    return columnNumber;
  }

  /**
   * Gets the source file name.
   *
   * @return the source file, or null if not available
   */
  public String getSourceFile() {
    return sourceFile;
  }

  /**
   * Checks if source location information is available.
   *
   * @return true if source info is available
   */
  public boolean hasSourceInfo() {
    return sourceFile != null && lineNumber >= 0;
  }

  @Override
  public String toString() {
    final StringBuilder sb = new StringBuilder();
    sb.append("#").append(frameIndex).append(" ");
    if (functionName != null) {
      sb.append(functionName);
    } else {
      sb.append("<function ").append(functionIndex).append(">");
    }
    sb.append(" at offset ").append(instructionOffset);
    if (hasSourceInfo()) {
      sb.append(" (").append(sourceFile);
      if (lineNumber >= 0) {
        sb.append(":").append(lineNumber);
        if (columnNumber >= 0) {
          sb.append(":").append(columnNumber);
        }
      }
      sb.append(")");
    }
    return sb.toString();
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof PanamaStackFrame)) {
      return false;
    }
    final PanamaStackFrame other = (PanamaStackFrame) obj;
    return frameIndex == other.frameIndex
        && functionIndex == other.functionIndex
        && moduleIndex == other.moduleIndex
        && instructionOffset == other.instructionOffset
        && Objects.equals(functionName, other.functionName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(frameIndex, functionName, functionIndex, moduleIndex, instructionOffset);
  }

  /** Builder for creating stack frames. */
  public static final class Builder {
    private int frameIndex;
    private String functionName;
    private int functionIndex;
    private int moduleIndex;
    private String moduleName;
    private long instructionOffset;
    private int lineNumber = -1;
    private int columnNumber = -1;
    private String sourceFile;

    private Builder() {}

    /**
     * Sets the frame index.
     *
     * @param index the frame index
     * @return this builder
     */
    public Builder frameIndex(final int index) {
      this.frameIndex = index;
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
     * Sets the function index.
     *
     * @param index the function index
     * @return this builder
     */
    public Builder functionIndex(final int index) {
      this.functionIndex = index;
      return this;
    }

    /**
     * Sets the module index.
     *
     * @param index the module index
     * @return this builder
     */
    public Builder moduleIndex(final int index) {
      this.moduleIndex = index;
      return this;
    }

    /**
     * Sets the module name.
     *
     * @param name the module name
     * @return this builder
     */
    public Builder moduleName(final String name) {
      this.moduleName = name;
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
     * @param file the source file
     * @return this builder
     */
    public Builder sourceFile(final String file) {
      this.sourceFile = file;
      return this;
    }

    /**
     * Builds the stack frame.
     *
     * @return the built stack frame
     */
    public PanamaStackFrame build() {
      return new PanamaStackFrame(this);
    }
  }
}
