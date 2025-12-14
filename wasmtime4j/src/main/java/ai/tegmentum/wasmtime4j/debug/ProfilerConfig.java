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

package ai.tegmentum.wasmtime4j.debug;

/**
 * Configuration for the guest profiler.
 *
 * <p>This class allows fine-grained control over what data the profiler collects.
 *
 * @since 1.0.0
 */
public final class ProfilerConfig {

  private final boolean trackFunctionCalls;
  private final boolean trackMemoryOperations;
  private final boolean trackInstructionCount;
  private final boolean trackStackDepth;
  private final long samplingIntervalNanos;
  private final int maxStackFrames;

  private ProfilerConfig(final Builder builder) {
    this.trackFunctionCalls = builder.trackFunctionCalls;
    this.trackMemoryOperations = builder.trackMemoryOperations;
    this.trackInstructionCount = builder.trackInstructionCount;
    this.trackStackDepth = builder.trackStackDepth;
    this.samplingIntervalNanos = builder.samplingIntervalNanos;
    this.maxStackFrames = builder.maxStackFrames;
  }

  /**
   * Creates a default configuration.
   *
   * @return a default ProfilerConfig
   */
  public static ProfilerConfig defaults() {
    return builder().build();
  }

  /**
   * Creates a new builder.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  public boolean isTrackFunctionCalls() {
    return trackFunctionCalls;
  }

  public boolean isTrackMemoryOperations() {
    return trackMemoryOperations;
  }

  public boolean isTrackInstructionCount() {
    return trackInstructionCount;
  }

  public boolean isTrackStackDepth() {
    return trackStackDepth;
  }

  public long getSamplingIntervalNanos() {
    return samplingIntervalNanos;
  }

  public int getMaxStackFrames() {
    return maxStackFrames;
  }

  /** Builder for ProfilerConfig. */
  public static final class Builder {
    private boolean trackFunctionCalls = true;
    private boolean trackMemoryOperations = false;
    private boolean trackInstructionCount = false;
    private boolean trackStackDepth = true;
    private long samplingIntervalNanos = 1_000_000; // 1ms
    private int maxStackFrames = 128;

    private Builder() {}

    public Builder trackFunctionCalls(final boolean track) {
      this.trackFunctionCalls = track;
      return this;
    }

    public Builder trackMemoryOperations(final boolean track) {
      this.trackMemoryOperations = track;
      return this;
    }

    public Builder trackInstructionCount(final boolean track) {
      this.trackInstructionCount = track;
      return this;
    }

    public Builder trackStackDepth(final boolean track) {
      this.trackStackDepth = track;
      return this;
    }

    public Builder samplingIntervalNanos(final long nanos) {
      this.samplingIntervalNanos = nanos;
      return this;
    }

    public Builder maxStackFrames(final int max) {
      this.maxStackFrames = max;
      return this;
    }

    public ProfilerConfig build() {
      return new ProfilerConfig(this);
    }
  }
}
