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

import ai.tegmentum.wasmtime4j.debug.DebugCapabilities;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * JNI implementation of debug capabilities.
 *
 * <p>This class reports the debugging capabilities available through the native Wasmtime runtime.
 *
 * @since 1.0.0
 */
public final class JniDebugCapabilities implements DebugCapabilities {

  private final boolean breakpointsSupported;
  private final boolean stepDebuggingSupported;
  private final boolean variableInspectionSupported;
  private final boolean memoryInspectionSupported;
  private final boolean profilingSupported;
  private final boolean sourceMapSupported;
  private final boolean dwarfSupported;
  private final int maxBreakpoints;
  private final List<String> supportedFormats;
  private final String debuggerVersion;

  private JniDebugCapabilities(final Builder builder) {
    this.breakpointsSupported = builder.breakpointsSupported;
    this.stepDebuggingSupported = builder.stepDebuggingSupported;
    this.variableInspectionSupported = builder.variableInspectionSupported;
    this.memoryInspectionSupported = builder.memoryInspectionSupported;
    this.profilingSupported = builder.profilingSupported;
    this.sourceMapSupported = builder.sourceMapSupported;
    this.dwarfSupported = builder.dwarfSupported;
    this.maxBreakpoints = builder.maxBreakpoints;
    this.supportedFormats = Collections.unmodifiableList(new ArrayList<>(builder.supportedFormats));
    this.debuggerVersion = builder.debuggerVersion;
  }

  /**
   * Creates capabilities with default values for Wasmtime debugging.
   *
   * @return default capabilities
   */
  public static JniDebugCapabilities getDefault() {
    return builder()
        .breakpointsSupported(true)
        .stepDebuggingSupported(true)
        .variableInspectionSupported(true)
        .memoryInspectionSupported(true)
        .profilingSupported(true)
        .sourceMapSupported(true)
        .dwarfSupported(true)
        .maxBreakpoints(1024)
        .supportedFormats(Arrays.asList("DWARF", "SourceMap"))
        .debuggerVersion("1.0.0")
        .build();
  }

  /**
   * Creates a builder for debug capabilities.
   *
   * @return a new builder
   */
  public static Builder builder() {
    return new Builder();
  }

  @Override
  public boolean supportsBreakpoints() {
    return breakpointsSupported;
  }

  @Override
  public boolean supportsStepDebugging() {
    return stepDebuggingSupported;
  }

  @Override
  public boolean supportsVariableInspection() {
    return variableInspectionSupported;
  }

  @Override
  public boolean supportsMemoryInspection() {
    return memoryInspectionSupported;
  }

  @Override
  public boolean supportsProfiling() {
    return profilingSupported;
  }

  @Override
  public boolean supportsSourceMap() {
    return sourceMapSupported;
  }

  @Override
  public boolean supportsDwarf() {
    return dwarfSupported;
  }

  @Override
  public int getMaxBreakpoints() {
    return maxBreakpoints;
  }

  @Override
  public List<String> getSupportedFormats() {
    return supportedFormats;
  }

  @Override
  public String getDebuggerVersion() {
    return debuggerVersion;
  }

  @Override
  public String toString() {
    return "JniDebugCapabilities{"
        + "breakpoints="
        + breakpointsSupported
        + ", step="
        + stepDebuggingSupported
        + ", variables="
        + variableInspectionSupported
        + ", memory="
        + memoryInspectionSupported
        + ", profiling="
        + profilingSupported
        + ", sourceMap="
        + sourceMapSupported
        + ", dwarf="
        + dwarfSupported
        + ", maxBreakpoints="
        + maxBreakpoints
        + ", formats="
        + supportedFormats
        + ", version='"
        + debuggerVersion
        + '\''
        + '}';
  }

  /** Builder for debug capabilities. */
  public static final class Builder {
    private boolean breakpointsSupported = true;
    private boolean stepDebuggingSupported = true;
    private boolean variableInspectionSupported = true;
    private boolean memoryInspectionSupported = true;
    private boolean profilingSupported = true;
    private boolean sourceMapSupported = true;
    private boolean dwarfSupported = true;
    private int maxBreakpoints = 1024;
    private List<String> supportedFormats = new ArrayList<>();
    private String debuggerVersion = "1.0.0";

    private Builder() {}

    /**
     * Sets whether breakpoints are supported.
     *
     * @param supported true if supported
     * @return this builder
     */
    public Builder breakpointsSupported(final boolean supported) {
      this.breakpointsSupported = supported;
      return this;
    }

    /**
     * Sets whether step debugging is supported.
     *
     * @param supported true if supported
     * @return this builder
     */
    public Builder stepDebuggingSupported(final boolean supported) {
      this.stepDebuggingSupported = supported;
      return this;
    }

    /**
     * Sets whether variable inspection is supported.
     *
     * @param supported true if supported
     * @return this builder
     */
    public Builder variableInspectionSupported(final boolean supported) {
      this.variableInspectionSupported = supported;
      return this;
    }

    /**
     * Sets whether memory inspection is supported.
     *
     * @param supported true if supported
     * @return this builder
     */
    public Builder memoryInspectionSupported(final boolean supported) {
      this.memoryInspectionSupported = supported;
      return this;
    }

    /**
     * Sets whether profiling is supported.
     *
     * @param supported true if supported
     * @return this builder
     */
    public Builder profilingSupported(final boolean supported) {
      this.profilingSupported = supported;
      return this;
    }

    /**
     * Sets whether source map debugging is supported.
     *
     * @param supported true if supported
     * @return this builder
     */
    public Builder sourceMapSupported(final boolean supported) {
      this.sourceMapSupported = supported;
      return this;
    }

    /**
     * Sets whether DWARF debugging is supported.
     *
     * @param supported true if supported
     * @return this builder
     */
    public Builder dwarfSupported(final boolean supported) {
      this.dwarfSupported = supported;
      return this;
    }

    /**
     * Sets the maximum number of breakpoints.
     *
     * @param max the maximum
     * @return this builder
     */
    public Builder maxBreakpoints(final int max) {
      this.maxBreakpoints = max;
      return this;
    }

    /**
     * Sets the supported formats.
     *
     * @param formats the formats
     * @return this builder
     */
    public Builder supportedFormats(final List<String> formats) {
      this.supportedFormats = formats != null ? new ArrayList<>(formats) : new ArrayList<>();
      return this;
    }

    /**
     * Sets the debugger version.
     *
     * @param version the version string
     * @return this builder
     */
    public Builder debuggerVersion(final String version) {
      this.debuggerVersion = version;
      return this;
    }

    /**
     * Builds the capabilities.
     *
     * @return the built capabilities
     */
    public JniDebugCapabilities build() {
      return new JniDebugCapabilities(this);
    }
  }
}
