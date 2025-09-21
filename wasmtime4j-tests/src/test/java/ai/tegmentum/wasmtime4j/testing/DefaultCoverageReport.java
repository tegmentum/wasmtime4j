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
 * Default implementation of CoverageReport.
 *
 * <p>Provides comprehensive API coverage analysis including overall coverage percentages,
 * module-specific coverage, and detailed implementation status.
 */
final class DefaultCoverageReport implements CoverageReport {

  private final double totalCoveragePercentage;
  private final Map<String, Double> coverageByModule;
  private final List<String> implementedApis;
  private final List<String> missingApis;
  private final List<String> partiallyImplementedApis;
  private final Map<String, ApiCoverageDetail> detailedCoverage;

  DefaultCoverageReport(
      final double totalCoveragePercentage,
      final Map<String, Double> coverageByModule,
      final List<String> implementedApis,
      final List<String> missingApis,
      final List<String> partiallyImplementedApis,
      final Map<String, ApiCoverageDetail> detailedCoverage) {
    this.totalCoveragePercentage = totalCoveragePercentage;
    this.coverageByModule = new java.util.HashMap<>(coverageByModule);
    this.implementedApis = new java.util.ArrayList<>(implementedApis);
    this.missingApis = new java.util.ArrayList<>(missingApis);
    this.partiallyImplementedApis = new java.util.ArrayList<>(partiallyImplementedApis);
    this.detailedCoverage = new java.util.HashMap<>(detailedCoverage);
  }

  @Override
  public double getTotalCoveragePercentage() {
    return totalCoveragePercentage;
  }

  @Override
  public Map<String, Double> getCoverageByModule() {
    return new java.util.HashMap<>(coverageByModule);
  }

  @Override
  public List<String> getImplementedApis() {
    return new java.util.ArrayList<>(implementedApis);
  }

  @Override
  public List<String> getMissingApis() {
    return new java.util.ArrayList<>(missingApis);
  }

  @Override
  public List<String> getPartiallyImplementedApis() {
    return new java.util.ArrayList<>(partiallyImplementedApis);
  }

  @Override
  public Map<String, ApiCoverageDetail> getDetailedCoverage() {
    return new java.util.HashMap<>(detailedCoverage);
  }

  @Override
  public int getTotalApiCount() {
    return implementedApis.size() + missingApis.size() + partiallyImplementedApis.size();
  }

  @Override
  public int getImplementedApiCount() {
    return implementedApis.size();
  }

  @Override
  public int getMissingApiCount() {
    return missingApis.size();
  }

  @Override
  public String toString() {
    return String.format(
        "CoverageReport{totalCoverage=%.2f%%, implementedApis=%d, missingApis=%d, partialApis=%d}",
        totalCoveragePercentage,
        implementedApis.size(),
        missingApis.size(),
        partiallyImplementedApis.size());
  }
}
