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

import ai.tegmentum.wasmtime4j.gc.MemoryCorruptionAnalysis;
import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * Panama implementation of memory corruption analysis.
 *
 * @since 1.0.0
 */
final class PanamaMemoryCorruptionAnalysis implements MemoryCorruptionAnalysis {

  private final Instant analysisTime = Instant.now();

  @Override
  public Instant getAnalysisTime() {
    return analysisTime;
  }

  @Override
  public boolean isCorruptionDetected() {
    return false;
  }

  @Override
  public CorruptionSeverity getCorruptionSeverity() {
    return CorruptionSeverity.POTENTIAL;
  }

  @Override
  public List<CorruptionIssue> getCorruptionIssues() {
    return Collections.emptyList();
  }

  @Override
  public MemoryIntegrityResult getIntegrityResult() {
    return new MemoryIntegrityResult() {
      @Override
      public boolean isIntegrityIntact() {
        return true;
      }

      @Override
      public int getViolationCount() {
        return 0;
      }

      @Override
      public List<IntegrityViolation> getViolations() {
        return Collections.emptyList();
      }

      @Override
      public double getIntegrityScore() {
        return 1.0;
      }

      @Override
      public Map<String, Boolean> getChecksumResults() {
        return Collections.emptyMap();
      }
    };
  }

  @Override
  public HeapConsistencyResult getConsistencyResult() {
    return new HeapConsistencyResult() {
      @Override
      public boolean isConsistent() {
        return true;
      }

      @Override
      public int getErrorCount() {
        return 0;
      }

      @Override
      public List<ConsistencyError> getErrors() {
        return Collections.emptyList();
      }

      @Override
      public FreeListValidation getFreeListValidation() {
        return null;
      }

      @Override
      public ObjectGraphValidation getObjectGraphValidation() {
        return null;
      }
    };
  }

  @Override
  public LifecycleViolationResult getLifecycleViolationResult() {
    return new LifecycleViolationResult() {
      @Override
      public boolean hasViolations() {
        return false;
      }

      @Override
      public int getViolationCount() {
        return 0;
      }

      @Override
      public List<LifecycleViolation> getViolations() {
        return Collections.emptyList();
      }

      @Override
      public Map<Long, ObjectStateValidation> getStateValidations() {
        return Collections.emptyMap();
      }
    };
  }

  @Override
  public List<CorruptionRecommendation> getRecommendations() {
    return Collections.emptyList();
  }
}
