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

package ai.tegmentum.wasmtime4j;

/**
 * Statistics and metrics for component linking operations.
 *
 * <p>This class provides information about linking performance and activity,
 * useful for monitoring and debugging component linking operations.
 *
 * @since 1.0.0
 */
public final class ComponentLinkingStatistics {

  private final int totalLinksCreated;
  private final int totalSwapsPerformed;
  private final int activeLinksCount;

  /**
   * Creates new component linking statistics.
   *
   * @param totalLinksCreated total number of links created
   * @param totalSwapsPerformed total number of hot swaps performed
   * @param activeLinksCount current number of active links
   */
  public ComponentLinkingStatistics(final int totalLinksCreated,
      final int totalSwapsPerformed,
      final int activeLinksCount) {
    this.totalLinksCreated = totalLinksCreated;
    this.totalSwapsPerformed = totalSwapsPerformed;
    this.activeLinksCount = activeLinksCount;
  }

  /**
   * Gets the total number of links created.
   *
   * @return total links created
   */
  public int getTotalLinksCreated() {
    return totalLinksCreated;
  }

  /**
   * Gets the total number of hot swaps performed.
   *
   * @return total swaps performed
   */
  public int getTotalSwapsPerformed() {
    return totalSwapsPerformed;
  }

  /**
   * Gets the current number of active links.
   *
   * @return active links count
   */
  public int getActiveLinksCount() {
    return activeLinksCount;
  }

  @Override
  public String toString() {
    return "ComponentLinkingStatistics{" +
        "totalLinksCreated=" + totalLinksCreated +
        ", totalSwapsPerformed=" + totalSwapsPerformed +
        ", activeLinksCount=" + activeLinksCount +
        '}';
  }
}