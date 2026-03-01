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
package ai.tegmentum.wasmtime4j.pool;

/**
 * Shared base class for {@link PoolStatistics} implementations.
 *
 * <p>This class holds all fields, provides the complete getter implementations, and a {@code
 * toString()} that uses {@code getClass().getSimpleName()} so subclasses get the correct name
 * automatically.
 *
 * @since 1.0.0
 */
public abstract class AbstractPoolStatistics implements PoolStatistics {

  private final long coreInstances;
  private final long componentInstances;
  private final long memories;
  private final long tables;
  private final long stacks;
  private final long gcHeaps;
  private final long unusedWarmMemories;
  private final long unusedMemoryBytesResident;
  private final long unusedWarmTables;
  private final long unusedTableBytesResident;
  private final long unusedWarmStacks;
  private final long unusedStackBytesResident;

  /**
   * Creates a new AbstractPoolStatistics from a metrics array.
   *
   * <p>The array must contain exactly 12 elements in the order defined by the native FFI.
   *
   * @param metrics the 12-element metrics array from native code
   */
  protected AbstractPoolStatistics(final long[] metrics) {
    if (metrics == null || metrics.length != 12) {
      throw new IllegalArgumentException("metrics array must have exactly 12 elements");
    }
    this.coreInstances = metrics[0];
    this.componentInstances = metrics[1];
    this.memories = metrics[2];
    this.tables = metrics[3];
    this.stacks = metrics[4];
    this.gcHeaps = metrics[5];
    this.unusedWarmMemories = metrics[6];
    this.unusedMemoryBytesResident = metrics[7];
    this.unusedWarmTables = metrics[8];
    this.unusedTableBytesResident = metrics[9];
    this.unusedWarmStacks = metrics[10];
    this.unusedStackBytesResident = metrics[11];
  }

  /** Creates empty statistics with all values set to zero. */
  protected AbstractPoolStatistics() {
    this(new long[12]);
  }

  @Override
  public long getCoreInstances() {
    return coreInstances;
  }

  @Override
  public long getComponentInstances() {
    return componentInstances;
  }

  @Override
  public long getMemories() {
    return memories;
  }

  @Override
  public long getTables() {
    return tables;
  }

  @Override
  public long getStacks() {
    return stacks;
  }

  @Override
  public long getGcHeaps() {
    return gcHeaps;
  }

  @Override
  public long getUnusedWarmMemories() {
    return unusedWarmMemories;
  }

  @Override
  public long getUnusedMemoryBytesResident() {
    return unusedMemoryBytesResident;
  }

  @Override
  public long getUnusedWarmTables() {
    return unusedWarmTables;
  }

  @Override
  public long getUnusedTableBytesResident() {
    return unusedTableBytesResident;
  }

  @Override
  public long getUnusedWarmStacks() {
    return unusedWarmStacks;
  }

  @Override
  public long getUnusedStackBytesResident() {
    return unusedStackBytesResident;
  }

  @Override
  public String toString() {
    return getClass().getSimpleName()
        + "{"
        + "coreInstances="
        + coreInstances
        + ", componentInstances="
        + componentInstances
        + ", memories="
        + memories
        + ", tables="
        + tables
        + ", stacks="
        + stacks
        + ", gcHeaps="
        + gcHeaps
        + ", unusedWarmMemories="
        + unusedWarmMemories
        + ", unusedMemoryBytesResident="
        + unusedMemoryBytesResident
        + ", unusedWarmTables="
        + unusedWarmTables
        + ", unusedTableBytesResident="
        + unusedTableBytesResident
        + ", unusedWarmStacks="
        + unusedWarmStacks
        + ", unusedStackBytesResident="
        + unusedStackBytesResident
        + ", totalInstances="
        + getTotalInstances()
        + '}';
  }
}
