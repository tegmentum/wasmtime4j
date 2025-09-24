/*
 * Copyright 2024 Tegmentum AI
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

package ai.tegmentum.wasmtime4j.serialization.optimization;

import java.util.Objects;

/**
 * Configuration settings for serialization optimization.
 *
 * <p>This class defines the optimization strategy and parameters used by the
 * SerializationOptimizer to tune performance based on system conditions and requirements.
 *
 * @since 1.0.0
 */
public final class OptimizationSettings {

  private final int baseBufferSize;
  private final boolean performanceOptimized;
  private final boolean alwaysVerifyIntegrity;
  private final boolean enableAdaptiveOptimization;
  private final double memoryPressureThreshold;
  private final double cpuUsageThreshold;
  private final int maxParallelTasks;
  private final boolean enableMachineLearning;
  private final long performanceHistorySize;

  /**
   * Creates optimization settings with the specified builder.
   *
   * @param builder the settings builder
   */
  private OptimizationSettings(final Builder builder) {
    this.baseBufferSize = builder.baseBufferSize;
    this.performanceOptimized = builder.performanceOptimized;
    this.alwaysVerifyIntegrity = builder.alwaysVerifyIntegrity;
    this.enableAdaptiveOptimization = builder.enableAdaptiveOptimization;
    this.memoryPressureThreshold = builder.memoryPressureThreshold;
    this.cpuUsageThreshold = builder.cpuUsageThreshold;
    this.maxParallelTasks = builder.maxParallelTasks;
    this.enableMachineLearning = builder.enableMachineLearning;
    this.performanceHistorySize = builder.performanceHistorySize;
  }

  // Getter methods

  public int getBaseBufferSize() {
    return baseBufferSize;
  }

  public boolean isPerformanceOptimized() {
    return performanceOptimized;
  }

  public boolean isAlwaysVerifyIntegrity() {
    return alwaysVerifyIntegrity;
  }

  public boolean isEnableAdaptiveOptimization() {
    return enableAdaptiveOptimization;
  }

  public double getMemoryPressureThreshold() {
    return memoryPressureThreshold;
  }

  public double getCpuUsageThreshold() {
    return cpuUsageThreshold;
  }

  public int getMaxParallelTasks() {
    return maxParallelTasks;
  }

  public boolean isEnableMachineLearning() {
    return enableMachineLearning;
  }

  public long getPerformanceHistorySize() {
    return performanceHistorySize;
  }

  /**
   * Creates default optimization settings suitable for most use cases.
   *
   * @return default optimization settings
   */
  public static OptimizationSettings createDefault() {
    return new Builder().build();
  }

  /**
   * Creates performance-optimized settings for maximum speed.
   *
   * @return performance-optimized settings
   */
  public static OptimizationSettings createPerformanceOptimized() {
    return new Builder()
        .setBaseBufferSize(256 * 1024) // 256KB
        .setPerformanceOptimized(true)
        .setAlwaysVerifyIntegrity(false) // Skip for speed
        .setEnableAdaptiveOptimization(true)
        .setMemoryPressureThreshold(0.9) // Higher threshold
        .setCpuUsageThreshold(0.8) // Higher threshold
        .setMaxParallelTasks(Runtime.getRuntime().availableProcessors() * 2)
        .setEnableMachineLearning(true)
        .build();
  }

  /**
   * Creates security-focused settings with integrity verification.
   *
   * @return security-focused settings
   */
  public static OptimizationSettings createSecurityFocused() {
    return new Builder()
        .setBaseBufferSize(64 * 1024) // Smaller buffer for security
        .setPerformanceOptimized(false)
        .setAlwaysVerifyIntegrity(true) // Always verify
        .setEnableAdaptiveOptimization(false) // Consistent behavior
        .setMemoryPressureThreshold(0.7) // Lower threshold
        .setCpuUsageThreshold(0.6) // Lower threshold
        .setMaxParallelTasks(2) // Limited parallelism
        .setEnableMachineLearning(false) // No learning for predictable behavior
        .build();
  }

  /**
   * Creates memory-constrained settings for resource-limited environments.
   *
   * @return memory-constrained settings
   */
  public static OptimizationSettings createMemoryConstrained() {
    return new Builder()
        .setBaseBufferSize(16 * 1024) // 16KB small buffer
        .setPerformanceOptimized(false)
        .setAlwaysVerifyIntegrity(true)
        .setEnableAdaptiveOptimization(true)
        .setMemoryPressureThreshold(0.6) // Conservative threshold
        .setCpuUsageThreshold(0.7)
        .setMaxParallelTasks(1) // No parallelism
        .setEnableMachineLearning(false) // Avoid ML overhead
        .setPerformanceHistorySize(50) // Small history
        .build();
  }

  @Override
  public String toString() {
    return String.format(
        "OptimizationSettings{buffer=%dKB, performance=%s, integrity=%s, adaptive=%s, " +
        "memory_threshold=%.1f%%, cpu_threshold=%.1f%%, parallel=%d, ml=%s}",
        baseBufferSize / 1024,
        performanceOptimized,
        alwaysVerifyIntegrity,
        enableAdaptiveOptimization,
        memoryPressureThreshold * 100,
        cpuUsageThreshold * 100,
        maxParallelTasks,
        enableMachineLearning);
  }

  /**
   * Builder for creating OptimizationSettings instances.
   */
  public static final class Builder {
    // Default values
    private int baseBufferSize = 64 * 1024; // 64KB
    private boolean performanceOptimized = false;
    private boolean alwaysVerifyIntegrity = true;
    private boolean enableAdaptiveOptimization = true;
    private double memoryPressureThreshold = 0.8; // 80%
    private double cpuUsageThreshold = 0.7; // 70%
    private int maxParallelTasks = Runtime.getRuntime().availableProcessors();
    private boolean enableMachineLearning = false;
    private long performanceHistorySize = 1000;

    public Builder setBaseBufferSize(final int size) {
      this.baseBufferSize = requirePositive(size, "Base buffer size must be positive");
      return this;
    }

    public Builder setPerformanceOptimized(final boolean optimized) {
      this.performanceOptimized = optimized;
      return this;
    }

    public Builder setAlwaysVerifyIntegrity(final boolean verify) {
      this.alwaysVerifyIntegrity = verify;
      return this;
    }

    public Builder setEnableAdaptiveOptimization(final boolean enable) {
      this.enableAdaptiveOptimization = enable;
      return this;
    }

    public Builder setMemoryPressureThreshold(final double threshold) {
      this.memoryPressureThreshold = requireInRange(threshold, 0.0, 1.0,
          "Memory pressure threshold must be between 0 and 1");
      return this;
    }

    public Builder setCpuUsageThreshold(final double threshold) {
      this.cpuUsageThreshold = requireInRange(threshold, 0.0, 1.0,
          "CPU usage threshold must be between 0 and 1");
      return this;
    }

    public Builder setMaxParallelTasks(final int tasks) {
      this.maxParallelTasks = requirePositive(tasks, "Max parallel tasks must be positive");
      return this;
    }

    public Builder setEnableMachineLearning(final boolean enable) {
      this.enableMachineLearning = enable;
      return this;
    }

    public Builder setPerformanceHistorySize(final long size) {
      this.performanceHistorySize = requirePositive(size, "Performance history size must be positive");
      return this;
    }

    public OptimizationSettings build() {
      return new OptimizationSettings(this);
    }

    private static int requirePositive(final int value, final String message) {
      if (value <= 0) {
        throw new IllegalArgumentException(message + " (was: " + value + ")");
      }
      return value;
    }

    private static long requirePositive(final long value, final String message) {
      if (value <= 0) {
        throw new IllegalArgumentException(message + " (was: " + value + ")");
      }
      return value;
    }

    private static double requireInRange(final double value, final double min, final double max,
                                        final String message) {
      if (value < min || value > max) {
        throw new IllegalArgumentException(message + " (was: " + value + ", expected: " + min + "-" + max + ")");
      }
      return value;
    }
  }
}