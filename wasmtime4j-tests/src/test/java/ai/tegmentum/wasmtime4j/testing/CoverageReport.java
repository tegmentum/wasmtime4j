/*
 * Copyright 2024 Tegmentum AI Inc.
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

package ai.tegmentum.wasmtime4j.testing;

import java.util.List;
import java.util.Map;

/**
 * Comprehensive API coverage report providing detailed analysis of implementation completeness.
 *
 * <p>This report provides accurate measurement of API coverage, breaking down coverage by module
 * and identifying areas requiring attention for complete implementation.
 */
public interface CoverageReport {

  /**
   * Gets the total API coverage percentage across all modules.
   *
   * @return coverage percentage from 0.0 to 100.0
   */
  double getTotalCoveragePercentage();

  /**
   * Gets coverage percentage broken down by module.
   *
   * @return map of module names to their coverage percentages
   */
  Map<String, Double> getCoverageByModule();

  /**
   * Gets list of all implemented APIs.
   *
   * @return list of implemented API names
   */
  List<String> getImplementedApis();

  /**
   * Gets list of APIs that are completely missing.
   *
   * @return list of missing API names
   */
  List<String> getMissingApis();

  /**
   * Gets list of APIs that are partially implemented.
   *
   * @return list of partially implemented API names
   */
  List<String> getPartiallyImplementedApis();

  /**
   * Gets detailed coverage information for each API.
   *
   * @return map of API names to their detailed coverage information
   */
  Map<String, ApiCoverageDetail> getDetailedCoverage();

  /**
   * Gets the total number of APIs that should be implemented.
   *
   * @return total API count
   */
  int getTotalApiCount();

  /**
   * Gets the number of implemented APIs.
   *
   * @return implemented API count
   */
  int getImplementedApiCount();

  /**
   * Gets the number of missing APIs.
   *
   * @return missing API count
   */
  int getMissingApiCount();

  /**
   * Checks if coverage meets production readiness threshold (95%+).
   *
   * @return true if coverage is sufficient for production
   */
  default boolean isProductionReady() {
    return getTotalCoveragePercentage() >= 95.0 && getMissingApis().isEmpty();
  }

  /**
   * Gets coverage summary as formatted string.
   *
   * @return human-readable coverage summary
   */
  default String getSummary() {
    return String.format(
        "API Coverage: %.2f%% (%d/%d APIs implemented, %d missing, %d partial)",
        getTotalCoveragePercentage(),
        getImplementedApiCount(),
        getTotalApiCount(),
        getMissingApiCount(),
        getPartiallyImplementedApis().size());
  }
}
