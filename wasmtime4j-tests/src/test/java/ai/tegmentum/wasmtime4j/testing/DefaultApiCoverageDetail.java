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

/**
 * Default implementation of ApiCoverageDetail.
 *
 * <p>Provides detailed information about API coverage including implementation status, native
 * backing, and test coverage for a specific API.
 */
final class DefaultApiCoverageDetail implements ApiCoverageDetail {

  private final String apiName;
  private final boolean isImplemented;
  private final boolean hasNativeBacking;
  private final boolean hasJniImplementation;
  private final boolean hasPanamaImplementation;
  private final List<String> missingMethods;
  private final TestCoverageInfo testCoverage;

  DefaultApiCoverageDetail(
      final String apiName,
      final boolean isImplemented,
      final boolean hasNativeBacking,
      final boolean hasJniImplementation,
      final boolean hasPanamaImplementation,
      final List<String> missingMethods,
      final TestCoverageInfo testCoverage) {
    this.apiName = apiName;
    this.isImplemented = isImplemented;
    this.hasNativeBacking = hasNativeBacking;
    this.hasJniImplementation = hasJniImplementation;
    this.hasPanamaImplementation = hasPanamaImplementation;
    this.missingMethods = new java.util.ArrayList<>(missingMethods);
    this.testCoverage = testCoverage;
  }

  @Override
  public String getApiName() {
    return apiName;
  }

  @Override
  public boolean isImplemented() {
    return isImplemented;
  }

  @Override
  public boolean hasNativeBacking() {
    return hasNativeBacking;
  }

  @Override
  public boolean hasJniImplementation() {
    return hasJniImplementation;
  }

  @Override
  public boolean hasPanamaImplementation() {
    return hasPanamaImplementation;
  }

  @Override
  public List<String> getMissingMethods() {
    return new java.util.ArrayList<>(missingMethods);
  }

  @Override
  public TestCoverageInfo getTestCoverage() {
    return testCoverage;
  }

  @Override
  public String toString() {
    return String.format(
        "ApiCoverageDetail{api='%s', implemented=%s, nativeBacking=%s, jni=%s, panama=%s,"
            + " missingMethods=%d}",
        apiName,
        isImplemented,
        hasNativeBacking,
        hasJniImplementation,
        hasPanamaImplementation,
        missingMethods.size());
  }
}
