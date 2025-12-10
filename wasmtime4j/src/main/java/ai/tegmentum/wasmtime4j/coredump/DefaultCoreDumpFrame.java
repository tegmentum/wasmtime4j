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

package ai.tegmentum.wasmtime4j.coredump;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * Default implementation of {@link CoreDumpFrame}.
 *
 * @since 1.0.0
 */
public final class DefaultCoreDumpFrame implements CoreDumpFrame {

  private final int funcIndex;
  private final String funcName;
  private final int moduleIndex;
  private final String moduleName;
  private final int offset;
  private final List<byte[]> locals;
  private final List<byte[]> stack;
  private final boolean trapFrame;

  private DefaultCoreDumpFrame(final Builder builder) {
    this.funcIndex = builder.funcIndex;
    this.funcName = builder.funcName;
    this.moduleIndex = builder.moduleIndex;
    this.moduleName = builder.moduleName;
    this.offset = builder.offset;
    this.locals = copyByteArrayList(builder.locals);
    this.stack = copyByteArrayList(builder.stack);
    this.trapFrame = builder.trapFrame;
  }

  private static List<byte[]> copyByteArrayList(final List<byte[]> source) {
    final List<byte[]> copy = new ArrayList<>(source.size());
    for (final byte[] arr : source) {
      copy.add(arr != null ? arr.clone() : null);
    }
    return Collections.unmodifiableList(copy);
  }

  /**
   * Creates a new builder for constructing a CoreDumpFrame.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public int getFuncIndex() {
    return funcIndex;
  }

  @Override
  public Optional<String> getFuncName() {
    return Optional.ofNullable(funcName);
  }

  @Override
  public int getModuleIndex() {
    return moduleIndex;
  }

  @Override
  public Optional<String> getModuleName() {
    return Optional.ofNullable(moduleName);
  }

  @Override
  public int getOffset() {
    return offset;
  }

  @Override
  public List<byte[]> getLocals() {
    return locals;
  }

  @Override
  public List<byte[]> getStack() {
    return stack;
  }

  @Override
  public boolean isTrapFrame() {
    return trapFrame;
  }

  @Override
  public String toString() {
    return "CoreDumpFrame{"
        + "funcIndex="
        + funcIndex
        + ", funcName='"
        + funcName
        + '\''
        + ", moduleIndex="
        + moduleIndex
        + ", moduleName='"
        + moduleName
        + '\''
        + ", offset="
        + offset
        + ", trapFrame="
        + trapFrame
        + '}';
  }

  /** Builder for constructing {@link DefaultCoreDumpFrame} instances. */
  public static final class Builder {

    private int funcIndex;
    private String funcName;
    private int moduleIndex;
    private String moduleName;
    private int offset;
    private final List<byte[]> locals = new ArrayList<>();
    private final List<byte[]> stack = new ArrayList<>();
    private boolean trapFrame;

    private Builder() {}

    public Builder funcIndex(final int funcIndex) {
      this.funcIndex = funcIndex;
      return this;
    }

    public Builder funcName(final String funcName) {
      this.funcName = funcName;
      return this;
    }

    public Builder moduleIndex(final int moduleIndex) {
      this.moduleIndex = moduleIndex;
      return this;
    }

    public Builder moduleName(final String moduleName) {
      this.moduleName = moduleName;
      return this;
    }

    public Builder offset(final int offset) {
      this.offset = offset;
      return this;
    }

    public Builder addLocal(final byte[] value) {
      this.locals.add(value != null ? value.clone() : null);
      return this;
    }

    /**
     * Adds multiple local variable values to this frame.
     *
     * @param values the list of local values to add
     * @return this builder
     */
    public Builder addLocals(final List<byte[]> values) {
      for (final byte[] value : values) {
        this.locals.add(value != null ? value.clone() : null);
      }
      return this;
    }

    public Builder addStackValue(final byte[] value) {
      this.stack.add(value != null ? value.clone() : null);
      return this;
    }

    /**
     * Adds multiple operand stack values to this frame.
     *
     * @param values the list of stack values to add
     * @return this builder
     */
    public Builder addStackValues(final List<byte[]> values) {
      for (final byte[] value : values) {
        this.stack.add(value != null ? value.clone() : null);
      }
      return this;
    }

    public Builder trapFrame(final boolean trapFrame) {
      this.trapFrame = trapFrame;
      return this;
    }

    public DefaultCoreDumpFrame build() {
      return new DefaultCoreDumpFrame(this);
    }
  }
}
