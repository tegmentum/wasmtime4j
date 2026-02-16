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
import java.util.Objects;

/**
 * Represents a WebAssembly core dump containing trap diagnostics and debugging information.
 *
 * <p>This class provides a complete implementation that can be constructed from native data and
 * used by both JNI and Panama implementations.
 *
 * @since 1.0.0
 */
public final class WasmCoreDump {

  private final String name;
  private final List<CoreDumpFrame> frames;
  private final List<String> modules;
  private final List<CoreDumpInstance> instances;
  private final List<CoreDumpGlobal> globals;
  private final List<CoreDumpMemory> memories;
  private final String trapMessage;
  private final byte[] serializedData;

  private WasmCoreDump(final Builder builder) {
    this.name = builder.name;
    this.frames = Collections.unmodifiableList(new ArrayList<>(builder.frames));
    this.modules = Collections.unmodifiableList(new ArrayList<>(builder.modules));
    this.instances = Collections.unmodifiableList(new ArrayList<>(builder.instances));
    this.globals = Collections.unmodifiableList(new ArrayList<>(builder.globals));
    this.memories = Collections.unmodifiableList(new ArrayList<>(builder.memories));
    this.trapMessage = builder.trapMessage;
    this.serializedData = builder.serializedData != null ? builder.serializedData.clone() : null;
  }

  /**
   * Creates a new builder for constructing a WasmCoreDump.
   *
   * @return a new builder instance
   */
  public static Builder builder() {
    return new Builder();
  }

  public String getName() {
    return name;
  }

  public List<CoreDumpFrame> getFrames() {
    return frames;
  }

  public List<String> getModules() {
    return modules;
  }

  public List<CoreDumpInstance> getInstances() {
    return instances;
  }

  public List<CoreDumpGlobal> getGlobals() {
    return globals;
  }

  public List<CoreDumpMemory> getMemories() {
    return memories;
  }

  public byte[] serialize() {
    if (serializedData == null) {
      throw new UnsupportedOperationException("Serialization not available for this coredump");
    }
    return serializedData.clone();
  }

  public long getSize() {
    long size = 0;
    if (serializedData != null) {
      size += serializedData.length;
    }
    for (final CoreDumpMemory memory : memories) {
      for (final CoreDumpMemory.MemorySegment segment : memory.getSegments()) {
        size += segment.getSize();
      }
    }
    return size;
  }

  public String getTrapMessage() {
    return trapMessage;
  }

  @Override
  public String toString() {
    return "WasmCoreDump{"
        + "name='"
        + name
        + '\''
        + ", frames="
        + frames.size()
        + ", modules="
        + modules.size()
        + ", instances="
        + instances.size()
        + ", globals="
        + globals.size()
        + ", memories="
        + memories.size()
        + ", trapMessage='"
        + trapMessage
        + '\''
        + '}';
  }

  /** Builder for constructing {@link WasmCoreDump} instances. */
  public static final class Builder {

    private String name;
    private final List<CoreDumpFrame> frames = new ArrayList<>();
    private final List<String> modules = new ArrayList<>();
    private final List<CoreDumpInstance> instances = new ArrayList<>();
    private final List<CoreDumpGlobal> globals = new ArrayList<>();
    private final List<CoreDumpMemory> memories = new ArrayList<>();
    private String trapMessage;
    private byte[] serializedData;

    private Builder() {}

    /**
     * Sets the name of the coredump.
     *
     * @param name the name
     * @return this builder
     */
    public Builder name(final String name) {
      this.name = name;
      return this;
    }

    /**
     * Adds a stack frame to the coredump.
     *
     * @param frame the frame to add
     * @return this builder
     */
    public Builder addFrame(final CoreDumpFrame frame) {
      Objects.requireNonNull(frame, "Frame cannot be null");
      this.frames.add(frame);
      return this;
    }

    /**
     * Adds all frames from the given list.
     *
     * @param frames the frames to add
     * @return this builder
     */
    public Builder addFrames(final List<CoreDumpFrame> frames) {
      Objects.requireNonNull(frames, "Frames cannot be null");
      this.frames.addAll(frames);
      return this;
    }

    /**
     * Adds a module name to the coredump.
     *
     * @param module the module name
     * @return this builder
     */
    public Builder addModule(final String module) {
      Objects.requireNonNull(module, "Module cannot be null");
      this.modules.add(module);
      return this;
    }

    /**
     * Adds all modules from the given list.
     *
     * @param modules the modules to add
     * @return this builder
     */
    public Builder addModules(final List<String> modules) {
      Objects.requireNonNull(modules, "Modules cannot be null");
      this.modules.addAll(modules);
      return this;
    }

    /**
     * Adds an instance to the coredump.
     *
     * @param instance the instance to add
     * @return this builder
     */
    public Builder addInstance(final CoreDumpInstance instance) {
      Objects.requireNonNull(instance, "Instance cannot be null");
      this.instances.add(instance);
      return this;
    }

    /**
     * Adds all instances from the given list.
     *
     * @param instances the instances to add
     * @return this builder
     */
    public Builder addInstances(final List<CoreDumpInstance> instances) {
      Objects.requireNonNull(instances, "Instances cannot be null");
      this.instances.addAll(instances);
      return this;
    }

    /**
     * Adds a global to the coredump.
     *
     * @param global the global to add
     * @return this builder
     */
    public Builder addGlobal(final CoreDumpGlobal global) {
      Objects.requireNonNull(global, "Global cannot be null");
      this.globals.add(global);
      return this;
    }

    /**
     * Adds all globals from the given list.
     *
     * @param globals the globals to add
     * @return this builder
     */
    public Builder addGlobals(final List<CoreDumpGlobal> globals) {
      Objects.requireNonNull(globals, "Globals cannot be null");
      this.globals.addAll(globals);
      return this;
    }

    /**
     * Adds a memory snapshot to the coredump.
     *
     * @param memory the memory to add
     * @return this builder
     */
    public Builder addMemory(final CoreDumpMemory memory) {
      Objects.requireNonNull(memory, "Memory cannot be null");
      this.memories.add(memory);
      return this;
    }

    /**
     * Adds all memories from the given list.
     *
     * @param memories the memories to add
     * @return this builder
     */
    public Builder addMemories(final List<CoreDumpMemory> memories) {
      Objects.requireNonNull(memories, "Memories cannot be null");
      this.memories.addAll(memories);
      return this;
    }

    /**
     * Sets the trap message.
     *
     * @param trapMessage the trap message
     * @return this builder
     */
    public Builder trapMessage(final String trapMessage) {
      this.trapMessage = trapMessage;
      return this;
    }

    /**
     * Sets the serialized coredump data.
     *
     * @param serializedData the serialized data
     * @return this builder
     */
    public Builder serializedData(final byte[] serializedData) {
      this.serializedData = serializedData != null ? serializedData.clone() : null;
      return this;
    }

    /**
     * Builds the WasmCoreDump.
     *
     * @return the constructed WasmCoreDump
     */
    public WasmCoreDump build() {
      return new WasmCoreDump(this);
    }
  }
}
