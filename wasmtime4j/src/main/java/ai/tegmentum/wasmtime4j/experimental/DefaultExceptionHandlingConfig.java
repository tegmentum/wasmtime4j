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

package ai.tegmentum.wasmtime4j.experimental;

import java.util.Objects;

/**
 * Default implementation of {@link ExceptionHandler.ExceptionHandlingConfig}.
 *
 * <p>Provides sensible defaults for WebAssembly exception handling configuration.
 *
 * @since 1.0.0
 */
public final class DefaultExceptionHandlingConfig
    implements ExceptionHandler.ExceptionHandlingConfig {

  /** Default maximum unwind depth. */
  public static final int DEFAULT_MAX_UNWIND_DEPTH = 1000;

  private final boolean nestedTryCatchEnabled;
  private final boolean exceptionUnwindingEnabled;
  private final int maxUnwindDepth;
  private final boolean exceptionTypeValidationEnabled;
  private final boolean stackTracesEnabled;
  private final boolean exceptionPropagationEnabled;
  private final boolean gcIntegrationEnabled;

  private DefaultExceptionHandlingConfig(final ConfigBuilder builder) {
    this.nestedTryCatchEnabled = builder.nestedTryCatchEnabled;
    this.exceptionUnwindingEnabled = builder.exceptionUnwindingEnabled;
    this.maxUnwindDepth = builder.maxUnwindDepth;
    this.exceptionTypeValidationEnabled = builder.exceptionTypeValidationEnabled;
    this.stackTracesEnabled = builder.stackTracesEnabled;
    this.exceptionPropagationEnabled = builder.exceptionPropagationEnabled;
    this.gcIntegrationEnabled = builder.gcIntegrationEnabled;
  }

  /**
   * Creates a new builder for exception handling configuration.
   *
   * @return a new builder
   */
  public static ConfigBuilder builder() {
    return new ConfigBuilder();
  }

  /**
   * Creates a default configuration with sensible defaults.
   *
   * @return a default configuration
   */
  public static DefaultExceptionHandlingConfig getDefault() {
    return builder().build();
  }

  @Override
  public boolean isNestedTryCatchEnabled() {
    return nestedTryCatchEnabled;
  }

  @Override
  public boolean isExceptionUnwindingEnabled() {
    return exceptionUnwindingEnabled;
  }

  @Override
  public int getMaxUnwindDepth() {
    return maxUnwindDepth;
  }

  @Override
  public boolean isExceptionTypeValidationEnabled() {
    return exceptionTypeValidationEnabled;
  }

  @Override
  public boolean isStackTracesEnabled() {
    return stackTracesEnabled;
  }

  @Override
  public boolean isExceptionPropagationEnabled() {
    return exceptionPropagationEnabled;
  }

  @Override
  public boolean isGcIntegrationEnabled() {
    return gcIntegrationEnabled;
  }

  @Override
  public boolean equals(final Object obj) {
    if (this == obj) {
      return true;
    }
    if (!(obj instanceof DefaultExceptionHandlingConfig)) {
      return false;
    }
    final DefaultExceptionHandlingConfig other = (DefaultExceptionHandlingConfig) obj;
    return nestedTryCatchEnabled == other.nestedTryCatchEnabled
        && exceptionUnwindingEnabled == other.exceptionUnwindingEnabled
        && maxUnwindDepth == other.maxUnwindDepth
        && exceptionTypeValidationEnabled == other.exceptionTypeValidationEnabled
        && stackTracesEnabled == other.stackTracesEnabled
        && exceptionPropagationEnabled == other.exceptionPropagationEnabled
        && gcIntegrationEnabled == other.gcIntegrationEnabled;
  }

  @Override
  public int hashCode() {
    return Objects.hash(
        nestedTryCatchEnabled,
        exceptionUnwindingEnabled,
        maxUnwindDepth,
        exceptionTypeValidationEnabled,
        stackTracesEnabled,
        exceptionPropagationEnabled,
        gcIntegrationEnabled);
  }

  @Override
  public String toString() {
    return "DefaultExceptionHandlingConfig{"
        + "nestedTryCatch="
        + nestedTryCatchEnabled
        + ", exceptionUnwinding="
        + exceptionUnwindingEnabled
        + ", maxUnwindDepth="
        + maxUnwindDepth
        + ", typeValidation="
        + exceptionTypeValidationEnabled
        + ", stackTraces="
        + stackTracesEnabled
        + ", exceptionPropagation="
        + exceptionPropagationEnabled
        + ", gcIntegration="
        + gcIntegrationEnabled
        + '}';
  }

  /** Builder for {@link DefaultExceptionHandlingConfig}. */
  public static final class ConfigBuilder
      implements ExceptionHandler.ExceptionHandlingConfig.Builder {

    private boolean nestedTryCatchEnabled = true;
    private boolean exceptionUnwindingEnabled = true;
    private int maxUnwindDepth = DEFAULT_MAX_UNWIND_DEPTH;
    private boolean exceptionTypeValidationEnabled = true;
    private boolean stackTracesEnabled = true;
    private boolean exceptionPropagationEnabled = true;
    private boolean gcIntegrationEnabled = false;

    private ConfigBuilder() {}

    @Override
    public ConfigBuilder nestedTryCatch(final boolean enabled) {
      this.nestedTryCatchEnabled = enabled;
      return this;
    }

    @Override
    public ConfigBuilder exceptionUnwinding(final boolean enabled) {
      this.exceptionUnwindingEnabled = enabled;
      return this;
    }

    @Override
    public ConfigBuilder maxUnwindDepth(final int maxDepth) {
      if (maxDepth < 0) {
        throw new IllegalArgumentException("Max unwind depth cannot be negative: " + maxDepth);
      }
      this.maxUnwindDepth = maxDepth;
      return this;
    }

    @Override
    public ConfigBuilder typeValidation(final boolean enabled) {
      this.exceptionTypeValidationEnabled = enabled;
      return this;
    }

    @Override
    public ConfigBuilder stackTraces(final boolean enabled) {
      this.stackTracesEnabled = enabled;
      return this;
    }

    @Override
    public ConfigBuilder exceptionPropagation(final boolean enabled) {
      this.exceptionPropagationEnabled = enabled;
      return this;
    }

    @Override
    public ConfigBuilder gcIntegration(final boolean enabled) {
      this.gcIntegrationEnabled = enabled;
      return this;
    }

    @Override
    public DefaultExceptionHandlingConfig build() {
      return new DefaultExceptionHandlingConfig(this);
    }
  }
}
