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

import ai.tegmentum.wasmtime4j.gc.GcInvariantValidation;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Panama implementation of GC invariant validation.
 *
 * @since 1.0.0
 */
final class PanamaGcInvariantValidation implements GcInvariantValidation {

  @Override
  public boolean areAllInvariantsSatisfied() {
    return true;
  }

  @Override
  public int getTotalInvariantCount() {
    return 0;
  }

  @Override
  public int getViolationCount() {
    return 0;
  }

  @Override
  public List<InvariantViolation> getViolations() {
    return Collections.emptyList();
  }

  @Override
  public double getSatisfactionScore() {
    return 1.0;
  }

  @Override
  public Map<InvariantCategory, CategoryValidation> getCategoryResults() {
    return Collections.emptyMap();
  }

  @Override
  public List<CriticalInvariantResult> getCriticalInvariants() {
    return Collections.emptyList();
  }

  @Override
  public ValidationPerformanceImpact getPerformanceImpact() {
    return new ValidationPerformanceImpact() {
      @Override
      public Duration getTotalValidationTime() {
        return Duration.ZERO;
      }

      @Override
      public double getValidationOverheadPercentage() {
        return 0.0;
      }

      @Override
      public Map<InvariantCategory, Duration> getTimeByCategory() {
        return Collections.emptyMap();
      }

      @Override
      public List<ExpensiveInvariant> getMostExpensiveInvariants() {
        return Collections.emptyList();
      }

      @Override
      public List<String> getOptimizationRecommendations() {
        return Collections.emptyList();
      }
    };
  }

  @Override
  public Map<InvariantCategory, Object> getSpecificValidators() {
    return Collections.emptyMap();
  }
}
