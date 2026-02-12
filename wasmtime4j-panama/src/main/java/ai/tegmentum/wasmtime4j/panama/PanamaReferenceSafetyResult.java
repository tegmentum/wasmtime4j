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

package ai.tegmentum.wasmtime4j.panama;

import ai.tegmentum.wasmtime4j.gc.ReferenceSafetyResult;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Panama implementation of reference safety validation result.
 *
 * @since 1.0.0
 */
final class PanamaReferenceSafetyResult implements ReferenceSafetyResult {

  private final boolean safe;

  PanamaReferenceSafetyResult(final boolean safe, final String message) {
    this.safe = safe;
  }

  @Override
  public boolean isAllSafe() {
    return safe;
  }

  @Override
  public long getTotalReferencesValidated() {
    return 0;
  }

  @Override
  public int getViolationCount() {
    return 0;
  }

  @Override
  public List<SafetyViolation> getSafetyViolations() {
    return Collections.emptyList();
  }

  @Override
  public double getSafetyScore() {
    return safe ? 1.0 : 0.0;
  }

  @Override
  public Map<ViolationType, Integer> getViolationStatistics() {
    return Collections.emptyMap();
  }

  @Override
  public List<SafetyRecommendation> getRecommendations() {
    return Collections.emptyList();
  }

  @Override
  public List<DangerousReferencePattern> getDangerousPatterns() {
    return Collections.emptyList();
  }
}
