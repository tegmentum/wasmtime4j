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

package ai.tegmentum.wasmtime4j.execution;

/**
 * Statistics tracked by a resource limiter.
 *
 * <p>This class contains metrics about resource allocation, including total memory and table
 * elements allocated, the number of grow requests, and the number of denials.
 *
 * @since 1.0.0
 */
public final class ResourceLimiterStats {

  private final long totalMemoryBytes;
  private final long totalTableElements;
  private final long memoryGrowRequests;
  private final long memoryGrowDenials;
  private final long tableGrowRequests;
  private final long tableGrowDenials;

  /**
   * Creates a new resource limiter statistics instance.
   *
   * @param totalMemoryBytes the total memory allocated in bytes
   * @param totalTableElements the total table elements allocated
   * @param memoryGrowRequests the number of memory grow requests
   * @param memoryGrowDenials the number of memory grow denials
   * @param tableGrowRequests the number of table grow requests
   * @param tableGrowDenials the number of table grow denials
   */
  public ResourceLimiterStats(
      final long totalMemoryBytes,
      final long totalTableElements,
      final long memoryGrowRequests,
      final long memoryGrowDenials,
      final long tableGrowRequests,
      final long tableGrowDenials) {
    this.totalMemoryBytes = totalMemoryBytes;
    this.totalTableElements = totalTableElements;
    this.memoryGrowRequests = memoryGrowRequests;
    this.memoryGrowDenials = memoryGrowDenials;
    this.tableGrowRequests = tableGrowRequests;
    this.tableGrowDenials = tableGrowDenials;
  }

  /**
   * Gets the total memory allocated in bytes.
   *
   * @return the total memory bytes
   */
  public long getTotalMemoryBytes() {
    return totalMemoryBytes;
  }

  /**
   * Gets the total table elements allocated.
   *
   * @return the total table elements
   */
  public long getTotalTableElements() {
    return totalTableElements;
  }

  /**
   * Gets the number of memory grow requests.
   *
   * @return the memory grow request count
   */
  public long getMemoryGrowRequests() {
    return memoryGrowRequests;
  }

  /**
   * Gets the number of memory grow denials.
   *
   * @return the memory grow denial count
   */
  public long getMemoryGrowDenials() {
    return memoryGrowDenials;
  }

  /**
   * Gets the number of table grow requests.
   *
   * @return the table grow request count
   */
  public long getTableGrowRequests() {
    return tableGrowRequests;
  }

  /**
   * Gets the number of table grow denials.
   *
   * @return the table grow denial count
   */
  public long getTableGrowDenials() {
    return tableGrowDenials;
  }

  /**
   * Gets the memory denial rate as a ratio of denials to requests.
   *
   * @return the denial rate, or 0.0 if no requests have been made
   */
  public double getMemoryDenialRate() {
    if (memoryGrowRequests == 0) {
      return 0.0;
    }
    return (double) memoryGrowDenials / memoryGrowRequests;
  }

  /**
   * Gets the table denial rate as a ratio of denials to requests.
   *
   * @return the denial rate, or 0.0 if no requests have been made
   */
  public double getTableDenialRate() {
    if (tableGrowRequests == 0) {
      return 0.0;
    }
    return (double) tableGrowDenials / tableGrowRequests;
  }

  @Override
  public String toString() {
    return "ResourceLimiterStats{"
        + "totalMemoryBytes="
        + totalMemoryBytes
        + ", totalTableElements="
        + totalTableElements
        + ", memoryGrowRequests="
        + memoryGrowRequests
        + ", memoryGrowDenials="
        + memoryGrowDenials
        + ", tableGrowRequests="
        + tableGrowRequests
        + ", tableGrowDenials="
        + tableGrowDenials
        + '}';
  }
}
