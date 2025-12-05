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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.ComponentResourceLimits;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Panama FFI implementation of ComponentResourceLimits.
 *
 * <p>This class provides resource limiting functionality for WebAssembly components. By default,
 * all limits are set to maximum values (unlimited).
 *
 * @since 1.0.0
 */
final class PanamaComponentResourceLimits implements ComponentResourceLimits {

  private final MemoryLimits memoryLimits;
  private final ExecutionLimits executionLimits;
  private final IoLimits ioLimits;
  private final NetworkLimits networkLimits;
  private final FileSystemLimits fileSystemLimits;

  /** Creates a new Panama component resource limits instance with default (unlimited) values. */
  PanamaComponentResourceLimits() {
    this.memoryLimits = new DefaultMemoryLimits();
    this.executionLimits = new DefaultExecutionLimits();
    this.ioLimits = new DefaultIoLimits();
    this.networkLimits = new DefaultNetworkLimits();
    this.fileSystemLimits = new DefaultFileSystemLimits();
  }

  @Override
  public MemoryLimits getMemoryLimits() {
    return memoryLimits;
  }

  @Override
  public ExecutionLimits getExecutionLimits() {
    return executionLimits;
  }

  @Override
  public IoLimits getIoLimits() {
    return ioLimits;
  }

  @Override
  public NetworkLimits getNetworkLimits() {
    return networkLimits;
  }

  @Override
  public FileSystemLimits getFileSystemLimits() {
    return fileSystemLimits;
  }

  @Override
  public ValidationResult validate(final ResourceUsage usage) {
    if (usage == null) {
      return new DefaultValidationResult(true, Collections.emptyList());
    }

    final List<LimitViolation> violations = new ArrayList<>();

    // Check memory limits
    if (usage.getCurrentMemoryUsage() > memoryLimits.getMaxTotalMemory()) {
      violations.add(
          new DefaultLimitViolation(
              ViolationType.MEMORY_LIMIT,
              "maxTotalMemory",
              usage.getCurrentMemoryUsage(),
              memoryLimits.getMaxTotalMemory(),
              "Memory usage exceeds limit"));
    }

    // Check execution limits
    if (usage.getCurrentExecutionTime() > executionLimits.getMaxExecutionTime()) {
      violations.add(
          new DefaultLimitViolation(
              ViolationType.EXECUTION_TIME,
              "maxExecutionTime",
              usage.getCurrentExecutionTime(),
              executionLimits.getMaxExecutionTime(),
              "Execution time exceeds limit"));
    }

    if (usage.getCurrentFuelConsumption() > executionLimits.getMaxFuel()) {
      violations.add(
          new DefaultLimitViolation(
              ViolationType.FUEL_LIMIT,
              "maxFuel",
              usage.getCurrentFuelConsumption(),
              executionLimits.getMaxFuel(),
              "Fuel consumption exceeds limit"));
    }

    if (usage.getCurrentInstructionCount() > executionLimits.getMaxInstructions()) {
      violations.add(
          new DefaultLimitViolation(
              ViolationType.INSTRUCTION_LIMIT,
              "maxInstructions",
              usage.getCurrentInstructionCount(),
              executionLimits.getMaxInstructions(),
              "Instruction count exceeds limit"));
    }

    return new DefaultValidationResult(violations.isEmpty(), violations);
  }

  /** Default implementation of MemoryLimits. */
  private static final class DefaultMemoryLimits implements MemoryLimits {

    @Override
    public long getMaxHeapSize() {
      return Long.MAX_VALUE;
    }

    @Override
    public long getMaxStackSize() {
      return Long.MAX_VALUE;
    }

    @Override
    public long getMaxTotalMemory() {
      return Long.MAX_VALUE;
    }
  }

  /** Default implementation of ExecutionLimits. */
  private static final class DefaultExecutionLimits implements ExecutionLimits {

    @Override
    public long getMaxExecutionTime() {
      return Long.MAX_VALUE;
    }

    @Override
    public long getMaxFuel() {
      return Long.MAX_VALUE;
    }

    @Override
    public long getMaxInstructions() {
      return Long.MAX_VALUE;
    }
  }

  /** Default implementation of IoLimits. */
  private static final class DefaultIoLimits implements IoLimits {

    @Override
    public int getMaxReadOpsPerSecond() {
      return Integer.MAX_VALUE;
    }

    @Override
    public int getMaxWriteOpsPerSecond() {
      return Integer.MAX_VALUE;
    }

    @Override
    public long getMaxBytesReadPerSecond() {
      return Long.MAX_VALUE;
    }

    @Override
    public long getMaxBytesWrittenPerSecond() {
      return Long.MAX_VALUE;
    }
  }

  /** Default implementation of NetworkLimits. */
  private static final class DefaultNetworkLimits implements NetworkLimits {

    @Override
    public int getMaxConnections() {
      return Integer.MAX_VALUE;
    }

    @Override
    public long getMaxBandwidth() {
      return Long.MAX_VALUE;
    }

    @Override
    public int getMaxRequestsPerSecond() {
      return Integer.MAX_VALUE;
    }
  }

  /** Default implementation of FileSystemLimits. */
  private static final class DefaultFileSystemLimits implements FileSystemLimits {

    @Override
    public int getMaxOpenFiles() {
      return Integer.MAX_VALUE;
    }

    @Override
    public long getMaxDiskUsage() {
      return Long.MAX_VALUE;
    }

    @Override
    public long getMaxFileSize() {
      return Long.MAX_VALUE;
    }
  }

  /** Default implementation of ValidationResult. */
  private static final class DefaultValidationResult implements ValidationResult {

    private final boolean valid;
    private final List<LimitViolation> violations;

    DefaultValidationResult(final boolean valid, final List<LimitViolation> violations) {
      this.valid = valid;
      this.violations =
          violations != null ? Collections.unmodifiableList(violations) : Collections.emptyList();
    }

    @Override
    public boolean isValid() {
      return valid;
    }

    @Override
    public List<LimitViolation> getViolations() {
      return violations;
    }
  }

  /** Default implementation of LimitViolation. */
  private static final class DefaultLimitViolation implements LimitViolation {

    private final ViolationType type;
    private final String limitName;
    private final long currentValue;
    private final long limitValue;
    private final String message;

    DefaultLimitViolation(
        final ViolationType type,
        final String limitName,
        final long currentValue,
        final long limitValue,
        final String message) {
      this.type = type;
      this.limitName = limitName;
      this.currentValue = currentValue;
      this.limitValue = limitValue;
      this.message = message;
    }

    @Override
    public ViolationType getType() {
      return type;
    }

    @Override
    public String getLimitName() {
      return limitName;
    }

    @Override
    public long getCurrentValue() {
      return currentValue;
    }

    @Override
    public long getLimitValue() {
      return limitValue;
    }

    @Override
    public String getMessage() {
      return message;
    }
  }
}
