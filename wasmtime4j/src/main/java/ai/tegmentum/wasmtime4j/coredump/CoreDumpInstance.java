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

import java.util.Optional;

/**
 * Represents a WebAssembly instance snapshot in a core dump.
 *
 * @since 1.0.0
 */
public final class CoreDumpInstance {

  private final int index;
  private final int moduleIndex;
  private final String name;
  private final int memoryCount;
  private final int globalCount;
  private final int tableCount;

  private CoreDumpInstance(final Builder builder) {
    this.index = builder.index;
    this.moduleIndex = builder.moduleIndex;
    this.name = builder.name;
    this.memoryCount = builder.memoryCount;
    this.globalCount = builder.globalCount;
    this.tableCount = builder.tableCount;
  }

  /**
   * Creates a new builder for constructing a CoreDumpInstance.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public int getIndex() {
    return index;
  }

  public int getModuleIndex() {
    return moduleIndex;
  }

  public Optional<String> getName() {
    return Optional.ofNullable(name);
  }

  public int getMemoryCount() {
    return memoryCount;
  }

  public int getGlobalCount() {
    return globalCount;
  }

  public int getTableCount() {
    return tableCount;
  }

  @Override
  public String toString() {
    return "CoreDumpInstance{"
        + "index="
        + index
        + ", moduleIndex="
        + moduleIndex
        + ", name='"
        + name
        + '\''
        + ", memoryCount="
        + memoryCount
        + ", globalCount="
        + globalCount
        + ", tableCount="
        + tableCount
        + '}';
  }

  /** Builder for constructing {@link CoreDumpInstance} instances. */
  public static final class Builder {

    private int index;
    private int moduleIndex;
    private String name;
    private int memoryCount;
    private int globalCount;
    private int tableCount;

    private Builder() {}

    public Builder index(final int index) {
      this.index = index;
      return this;
    }

    public Builder moduleIndex(final int moduleIndex) {
      this.moduleIndex = moduleIndex;
      return this;
    }

    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    public Builder memoryCount(final int memoryCount) {
      this.memoryCount = memoryCount;
      return this;
    }

    public Builder globalCount(final int globalCount) {
      this.globalCount = globalCount;
      return this;
    }

    public Builder tableCount(final int tableCount) {
      this.tableCount = tableCount;
      return this;
    }

    public CoreDumpInstance build() {
      return new CoreDumpInstance(this);
    }
  }
}
