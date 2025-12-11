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

import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Contains profiling data collected during WebAssembly execution.
 *
 * @since 1.0.0
 */
public final class ProfileData {

  private final Duration totalDuration;
  private final long totalFunctionCalls;
  private final long totalInstructions;
  private final int maxStackDepth;
  private final List<FunctionProfile> functionProfiles;
  private final Map<String, Long> customMetrics;

  /**
   * Creates a new ProfileData instance.
   *
   * @param totalDuration the total profiling duration
   * @param totalFunctionCalls the total number of function calls
   * @param totalInstructions the total instruction count
   * @param maxStackDepth the maximum stack depth observed
   * @param functionProfiles per-function profile data
   * @param customMetrics custom metrics
   */
  public ProfileData(
      final Duration totalDuration,
      final long totalFunctionCalls,
      final long totalInstructions,
      final int maxStackDepth,
      final List<FunctionProfile> functionProfiles,
      final Map<String, Long> customMetrics) {
    this.totalDuration = totalDuration;
    this.totalFunctionCalls = totalFunctionCalls;
    this.totalInstructions = totalInstructions;
    this.maxStackDepth = maxStackDepth;
    this.functionProfiles = Collections.unmodifiableList(functionProfiles);
    this.customMetrics = Collections.unmodifiableMap(customMetrics);
  }

  /**
   * Gets the total profiling duration.
   *
   * @return the duration
   */
  public Duration getTotalDuration() {
    return totalDuration;
  }

  /**
   * Gets the total function call count.
   *
   * @return the call count
   */
  public long getTotalFunctionCalls() {
    return totalFunctionCalls;
  }

  /**
   * Gets the total instruction count.
   *
   * @return the instruction count
   */
  public long getTotalInstructions() {
    return totalInstructions;
  }

  /**
   * Gets the maximum stack depth observed.
   *
   * @return the max stack depth
   */
  public int getMaxStackDepth() {
    return maxStackDepth;
  }

  /**
   * Gets the per-function profile data.
   *
   * @return the function profiles
   */
  public List<FunctionProfile> getFunctionProfiles() {
    return functionProfiles;
  }

  /**
   * Gets custom metrics.
   *
   * @return the custom metrics map
   */
  public Map<String, Long> getCustomMetrics() {
    return customMetrics;
  }

  /**
   * Profile data for a single function.
   */
  public static final class FunctionProfile {
    private final String functionName;
    private final int functionIndex;
    private final long callCount;
    private final Duration totalTime;
    private final Duration selfTime;

    /**
     * Creates a new FunctionProfile.
     *
     * @param functionName the function name
     * @param functionIndex the function index
     * @param callCount the number of times called
     * @param totalTime total time including callees
     * @param selfTime time spent in this function only
     */
    public FunctionProfile(
        final String functionName,
        final int functionIndex,
        final long callCount,
        final Duration totalTime,
        final Duration selfTime) {
      this.functionName = functionName;
      this.functionIndex = functionIndex;
      this.callCount = callCount;
      this.totalTime = totalTime;
      this.selfTime = selfTime;
    }

    public String getFunctionName() {
      return functionName;
    }

    public int getFunctionIndex() {
      return functionIndex;
    }

    public long getCallCount() {
      return callCount;
    }

    public Duration getTotalTime() {
      return totalTime;
    }

    public Duration getSelfTime() {
      return selfTime;
    }

    @Override
    public String toString() {
      return "FunctionProfile{name='" + functionName + "', index=" + functionIndex
          + ", calls=" + callCount + ", total=" + totalTime + ", self=" + selfTime + "}";
    }
  }
}
